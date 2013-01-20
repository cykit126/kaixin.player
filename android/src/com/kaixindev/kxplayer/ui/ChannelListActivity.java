package com.kaixindev.kxplayer.ui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.kaixindev.android.Log;
import com.kaixindev.core.IOUtil;
import com.kaixindev.core.ThreadWorker;
import com.kaixindev.kxplayer.Channel;
import com.kaixindev.kxplayer.ChannelListAdapter;
import com.kaixindev.kxplayer.R;
import com.kaixindev.kxplayer.service.PlayerService;
import com.kaixindev.serialize.XMLSerializer;

public class ChannelListActivity extends Activity implements
		AdapterView.OnItemClickListener {

	public static final int MSG_CHANNELS_OK = 1;
	public static final int MSG_CHANNELS_ERROR = 2;

	private class LocalHandler extends Handler {
		@Override
		public void handleMessage(Message message) {
			ListAdapter adapter = new ChannelListAdapter(mChannels,
					ChannelListActivity.this);
			ListView listView = (ListView) findViewById(R.id.listview);
			listView.setOnItemClickListener(ChannelListActivity.this);
			listView.setAdapter(adapter);
			hideLoading();
			ChannelListActivity.this.handleMessage(message);
		}
	}

	private ThreadWorker mThreadWorker = new ThreadWorker();
	private Messenger mMessenger;
	private List<Channel> mChannels;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mThreadWorker.start();
		mMessenger = new Messenger(new LocalHandler());
		bindPlayerService();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindPlayerService();
	}

	protected void loadChannels(final String path) {
		showLoading();
		mThreadWorker.pushJob(new Runnable() {
			public void run() {
				InputStream input = IOUtil.open(ChannelListActivity.this, path);
				byte[] content = IOUtil.read(input);

				XMLSerializer serializer = new XMLSerializer();
				Channel[] channels = (Channel[]) serializer
						.unserialize(content);
				mChannels = new ArrayList<Channel>();
				for (Channel ch : channels) {
					mChannels.add(ch);
				}

				Message message = Message.obtain(null,
						mChannels == null ? MSG_CHANNELS_ERROR
								: MSG_CHANNELS_OK);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
	}

	protected void handleMessage(Message message) {

	}

	protected void showLoading() {
		findViewById(R.id.loading).setVisibility(View.VISIBLE);
		findViewById(R.id.listview).setVisibility(View.GONE);
	}

	protected void hideLoading() {
		findViewById(R.id.loading).setVisibility(View.GONE);
		findViewById(R.id.listview).setVisibility(View.VISIBLE);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		ListView listView = (ListView) findViewById(R.id.listview);
		ChannelListAdapter adapter = (ChannelListAdapter) listView.getAdapter();
		Channel channel = (Channel) adapter.getItem(position);

		if (listView.isItemChecked(position)) {
			return;
		}

		if (mPlayerCLI != null) {
			mPlayerCLI.open(channel.uri);
		}

		listView.clearChoices();
		listView.setItemChecked(position, true);
	}

	// Player Service

	protected PlayerService.Client mPlayerCLI;
	private ServiceConnection mPlayerConn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mPlayerCLI = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mPlayerCLI = (PlayerService.Client) service;
		}
	};

	private void bindPlayerService() {
		Intent intent = new Intent(PlayerService.ACTION);
		if (!getApplicationContext().bindService(intent, mPlayerConn,
				BIND_AUTO_CREATE)) {
			Log.e("unable to bind Player Service.");
		}
	}

	private void unbindPlayerService() {
		if (mPlayerCLI != null) {
			getApplicationContext().unbindService(mPlayerConn);
		}
	}
}

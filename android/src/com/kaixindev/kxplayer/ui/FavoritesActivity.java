package com.kaixindev.kxplayer.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.widget.ListView;

import com.kaixindev.android.Log;
import com.kaixindev.kxplayer.Channel;
import com.kaixindev.kxplayer.ChannelListAdapter;
import com.kaixindev.kxplayer.FavoritesManager;
import com.kaixindev.kxplayer.R;
import com.kaixindev.kxplayer.service.ChannelSourceService;
import com.kaixindev.kxplayer.service.FavoritesService;

public class FavoritesActivity extends ChannelListActivity {

	private class LocalHandler extends Handler {

		private boolean mFavLoaded = false;
		private boolean mChLoaded = false;

		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case FavoritesManager.MSG_LOAD_OK: {
				mFavLoaded = true;
				if (mChLoaded) {
					loadFavorites();
					hideLoading();
				}
				break;
			}
			case ChannelSourceService.MSG_OK:
			case ChannelSourceService.MSG_ERROR: {
				mChLoaded = true;
				if (mFavLoaded) {
					loadFavorites();
					hideLoading();
				}
				break;
			}
			default:
				super.handleMessage(msg);
			}
			hideLoading();
		}
	}

	private Handler mHandler = new LocalHandler();
	private Messenger mMessenger;

	// Favorites Service
	private FavoritesService.Client mFavClient;
	private ServiceConnection mFavConn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mFavClient = (FavoritesService.Client) service;
			mFavClient.loadFavorites(mMessenger);
		}
	};

	private void bindFavoritesService() {
		Intent intent = new Intent(FavoritesService.ACTION);
		if (!getApplicationContext().bindService(intent, mFavConn,
				BIND_AUTO_CREATE)) {
			Log.e("Failed to bind Favorites Service.");
		}
	}

	private void unbindFavoritesService() {
		if (mFavClient != null) {
			unbindService(mFavConn);
		}
	}

	// Channel Source Service
	private ChannelSourceService.Client mChClient;
	private ServiceConnection mChConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mChClient = (ChannelSourceService.Client) service;
			mChClient.loadChannels(false, mMessenger);
		}
	};

	private void bindChannelSourceService() {
		Intent intent = new Intent(ChannelSourceService.ACTION);
		if (!getApplicationContext().bindService(intent, mChConn,
				BIND_AUTO_CREATE)) {
			Log.e("Failed to bind Favorites Service.");
		}
	}

	private void unbindChannelSourceService() {
		if (mChClient != null) {
			unbindService(mChConn);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadFavorites();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_list_activity);

		showLoading();

		mMessenger = new Messenger(mHandler);
		bindFavoritesService();
		bindChannelSourceService();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindFavoritesService();
		unbindChannelSourceService();
	}

	private void loadFavorites() {
		if (mFavClient == null) {
			return;
		}
		List<Channel> channels = new ArrayList<Channel>();
		Collection<String> chs = mFavClient.getFavorites();
		if (chs != null) {
			Log.i("Channels:" + chs.size());
			for (String ch : chs) {
				Log.i("favor:" + ch);
				channels.add(mChClient.get(ch));
			}
		} else {
			Log.i("no channel.");
		}

		ChannelListAdapter adapter = new FavoritesAdapter(channels,
				FavoritesActivity.this);
		ListView listView = (ListView) findViewById(R.id.listview);
		listView.setOnItemClickListener(FavoritesActivity.this);
		listView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}

	public void removeFavor(String uri) {
		mFavClient.removeFavor(uri);
		mFavClient.flushFavorites(null);
		ChannelListAdapter adapter = getAdapter();
		if (adapter != null) {
			adapter.removeChannel(uri);
			adapter.notifyDataSetChanged();
		}
	}

	public ChannelListAdapter getAdapter() {
		ListView listView = (ListView) findViewById(R.id.listview);
		return (ChannelListAdapter) listView.getAdapter();
	}
}

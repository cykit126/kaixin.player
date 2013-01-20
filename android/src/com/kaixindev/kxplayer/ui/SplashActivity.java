package com.kaixindev.kxplayer.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

import com.kaixindev.android.Log;
import com.kaixindev.kxplayer.R;
import com.kaixindev.kxplayer.service.ChannelSourceService;

public class SplashActivity extends Activity {
	
	private Messenger mMessenger;
	private class LocalHandler extends Handler {
		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
			case ChannelSourceService.MSG_OK:
				Intent intent = new Intent();
				intent.setClass(SplashActivity.this, MainActivity.class);
				startActivity(intent);
				break;
			case ChannelSourceService.MSG_ERROR:
				Log.e("unable to load channel source.");
				finish();
				break;
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		mMessenger = new Messenger(new LocalHandler());
		bindChannelSourceService();
	}

	public void onDestroy() {
		super.onDestroy();
		unbindChannelSourceService();
	}
	
	private ChannelSourceService.Client mChannelSource;
	private ServiceConnection mChannelSourceConn = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mChannelSource = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mChannelSource = (ChannelSourceService.Client)service;
			mChannelSource.loadChannels(false, mMessenger);
		}
	};
	private void bindChannelSourceService() {
		Intent intent = new Intent(ChannelSourceService.ACTION);
		if (!bindService(intent, mChannelSourceConn, BIND_AUTO_CREATE)) {
			Log.e("unable to bind ChannelSourceService.");
		}
	}
	private void unbindChannelSourceService() {
		if (mChannelSource != null) {
			unbindService(mChannelSourceConn);
		}
	}
}

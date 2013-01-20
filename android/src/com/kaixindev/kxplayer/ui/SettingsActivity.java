package com.kaixindev.kxplayer.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.widget.Toast;

import com.kaixindev.android.FileSystem;
import com.kaixindev.android.Log;
import com.kaixindev.core.ThreadWorker;
import com.kaixindev.kxplayer.R;
import com.kaixindev.kxplayer.UpdateInfo;
import com.kaixindev.kxplayer.service.UpdateService;

public class SettingsActivity extends Activity {

	private ThreadWorker mWorker = new ThreadWorker();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		View checkUpdate = findViewById(R.id.update);
		checkUpdate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mUpdateCLI != null) {
					mUpdateCLI.checkUpdate(SettingsActivity.this,  mMessenger);
				}
			}
		});
		
		View clearCache = findViewById(R.id.clear_cache);
		clearCache.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				String message = getResources().getString(R.string.dialog_wait);
				final ProgressDialog dialog = ProgressDialog.show(
						SettingsActivity.this, null, message);
				dialog.setCancelable(false);
				mWorker.pushJob(new Runnable(){
					@Override
					public void run() {
						FileSystem.clearCache(SettingsActivity.this);
						dialog.dismiss();
					}});
			}
		});
		
		mMessenger = new Messenger(new LocalHandler());
		bindUpdateService();
		
		mWorker.start();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindUpdateService();
	}
	
	// update
    private UpdateService.Client mUpdateCLI;
    private ServiceConnection mUpdateConn = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mUpdateCLI = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mUpdateCLI = (UpdateService.Client)service;
		}
	};
	private void bindUpdateService() {
		Intent intent = new Intent(UpdateService.ACTION);
		if (!getApplicationContext().bindService(intent, mUpdateConn, BIND_AUTO_CREATE)) {
			Log.e("unable to bind Update Service");
		}
	}
	private void unbindUpdateService() {
		if (mUpdateCLI != null) {
			unbindService(mUpdateConn);
		}
	}	
	
	private Messenger mMessenger;
	private class LocalHandler extends Handler {
		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
			case UpdateService.MSG_UPDATE_AVAILABLE:
				mUpdateCLI.askForUpdate(SettingsActivity.this, (UpdateInfo)message.obj);
				break;
			case UpdateService.MSG_UPDATE_TO_DATE:
				Toast.makeText(SettingsActivity.this, 
						R.string.settings_update_to_date, 
						Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	}
}








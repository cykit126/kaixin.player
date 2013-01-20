package com.kaixindev.kxplayer.ui;

import java.util.Locale;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;
import com.kaixindev.android.Application;
import com.kaixindev.android.Log;
import com.kaixindev.kxplayer.Channel;
import com.kaixindev.kxplayer.Player;
import com.kaixindev.kxplayer.PlayerNotificationReceiver;
import com.kaixindev.kxplayer.R;
import com.kaixindev.kxplayer.UpdateInfo;
import com.kaixindev.kxplayer.config.Config;
import com.kaixindev.kxplayer.service.ChannelSourceService;
import com.kaixindev.kxplayer.service.PlayerService;
import com.kaixindev.kxplayer.service.UpdateService;

public class MainActivity extends TabActivity {

	public static final String LOGTAG = "KaixinPlayerAndroidActivity";

	static {
		System.loadLibrary("kxplayer");
	}

	private PlayerNotificationReceiver mPCBReceiver;

	private TabHost.TabSpec createTabSpec(TabHost tabHost, String tag,
			int indicatorId, Intent content) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.tab_item, null);
		ImageView indicator = (ImageView) view.findViewById(R.id.indicator);
		indicator.setImageResource(indicatorId);
		return tabHost.newTabSpec(tag).setContent(content).setIndicator(view);
	}

	private void createTabs() {
		TabHost tabHost = getTabHost(); // The activity TabHost
		Intent intent; // Reusable Intent for each tab

		intent = new Intent().setClass(this, RecommActivity.class);
		tabHost.addTab(createTabSpec(tabHost, "recomm", R.drawable.tab_recomm,
				intent));

		intent = new Intent().setClass(this, CategoriesActivity.class);
		tabHost.addTab(createTabSpec(tabHost, "list", R.drawable.tab_list,
				intent));

		intent = new Intent().setClass(this, FavoritesActivity.class);
		tabHost.addTab(createTabSpec(tabHost, "favorites",
				R.drawable.tab_favorites, intent));

		intent = new Intent().setClass(this, SettingsActivity.class);
		tabHost.addTab(createTabSpec(tabHost, "settings",
				R.drawable.tab_settings, intent));

		tabHost.setCurrentTab(0);
	}

	private void initButtons() {
		final ImageButton btnStart = (ImageButton) findViewById(R.id.player_control_start);
		btnStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (mPlayerCLI.getState()) {
				case Player.STATE_IDLE:
				case Player.STATE_PAUSED:
				case Player.STATE_ERROR:
					mPlayerCLI.resume();
					updatePlayerState(Player.STATE_PLAYING);
					break;
				}
			}
		});

		final ImageButton btnPause = (ImageButton) findViewById(R.id.player_control_pause);
		btnPause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (mPlayerCLI.getState()) {
				case Player.STATE_BUFFERING:
				case Player.STATE_CONNECTING:
				case Player.STATE_PLAYING:
					mPlayerCLI.pause();
					updatePlayerState(Player.STATE_PAUSED);
					break;
				}
			}
		});

		final ImageButton btnOpen = (ImageButton) findViewById(R.id.player_control_open_player);
		btnOpen.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, PlayerActivity.class);
				startActivity(intent);
			}
		});

		if (mPlayerCLI != null) {
			updatePlayerState(mPlayerCLI.getState());
		} else {
			updatePlayerState(Player.STATE_IDLE);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		com.kaixindev.android.Log.ENABLED = true;
		initButtons();
		createTabs();

		Log.d("density:" + getResources().getDisplayMetrics().density);
		Log.d("dpi:" + getResources().getDisplayMetrics().densityDpi);

		mMessenger = new Messenger(new LocalHandler());
		mPCBReceiver = new PlayerNotificationReceiver(mMessenger);

		bindUpdateService();
		bindPlayerService();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			AlertDialog dialog = builder.setMessage(R.string.quit_alert)
					.setPositiveButton(R.string.quit_yes, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Application.quit();
							finish();
						}
					})
					.setNegativeButton(R.string.quit_no, null)
					.create();
			dialog.show();
			return false;
		default:
			return false;
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(PlayerService.PLAYER_NOTICE);
		registerReceiver(mPCBReceiver, filter);
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(mPCBReceiver);
	}

	@Override
	public void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, Config.FLURRY_API_KEY);
	}

	@Override
	public void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindUpdateService();
		unbindPlayerService();
	}

	public void updatePlayerDisplay(String content) {
		TextView displayView = (TextView) findViewById(R.id.player_control_display);
		displayView.setText(content);
	}

	private Messenger mMessenger;

	private class LocalHandler extends Handler {
		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
			case UpdateService.MSG_UPDATE_AVAILABLE:
				mUpdateCLI.askForUpdate(MainActivity.this,
						(UpdateInfo) message.obj);
				break;
			case PlayerNotificationReceiver.MSG_NOTICE:
				updatePlayerState(message.arg1);
				break;
			}
		}
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
			mUpdateCLI = (UpdateService.Client) service;
			mUpdateCLI.checkUpdate(MainActivity.this, mMessenger);
		}
	};

	private void bindUpdateService() {
		Intent intent = new Intent(UpdateService.ACTION);
		if (!bindService(intent, mUpdateConn, BIND_AUTO_CREATE)) {
			Log.e("unable to bind Update Service");
		}
	}

	private void unbindUpdateService() {
		if (mUpdateCLI != null) {
			unbindService(mUpdateConn);
		}
	}

	// Player Service

	private PlayerService.Client mPlayerCLI;
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
		if (!bindService(intent, mPlayerConn, BIND_AUTO_CREATE)) {
			Log.e("unable to bind Player Service.");
		}
	}

	private void unbindPlayerService() {
		if (mPlayerCLI != null) {
			unbindService(mPlayerConn);
		}
	}

	public void updatePlayerState(int state) {
		String uri = mPlayerCLI != null ? mPlayerCLI.getPlayingUri() : null;
		Channel channel = ChannelSourceService.getChannel(uri);
		Resources r = getResources();
		String channelName = r.getString(R.string.unknown_channel);
		if (channel != null && channel.name != null) {
			channelName = channel.name.get(Locale.getDefault().toString());
		}

		switch (state) {
		case Player.STATE_CONNECTING: {
			showPauseButton();
			String content = r.getString(R.string.player_connecting,
					channelName);
			updatePlayerDisplay(content);
			break;
		}
		case Player.STATE_PLAYING: {
			showPauseButton();
			String content = r.getString(R.string.player_control_playing,
					channelName);
			updatePlayerDisplay(content);
			break;
		}
		case Player.STATE_IDLE:
			showStartButton();
			updatePlayerDisplay(null);
			break;
		case Player.STATE_ERROR: {
			showStartButton();
			String content = r.getString(R.string.player_control_error,
					channelName);
			updatePlayerDisplay(content);
			break;
		}
		case Player.STATE_PAUSED: {
			showStartButton();
			String content = r.getString(R.string.player_paused, channelName);
			updatePlayerDisplay(content);
			break;
		}
		case Player.STATE_BUFFERING: {
			showPauseButton();
			String content = r.getString(R.string.player_control_buffering,
					channelName);
			updatePlayerDisplay(content);
			break;
		}
		default:
			break;
		}

	}

	private void showPauseButton() {
		ImageButton btnResume = (ImageButton) findViewById(R.id.player_control_start);
		ImageButton btnPause = (ImageButton) findViewById(R.id.player_control_pause);
		btnResume.setVisibility(View.GONE);
		btnPause.setVisibility(View.VISIBLE);
	}

	private void showStartButton() {
		ImageButton btnResume = (ImageButton) findViewById(R.id.player_control_start);
		ImageButton btnPause = (ImageButton) findViewById(R.id.player_control_pause);
		btnResume.setVisibility(View.VISIBLE);
		btnPause.setVisibility(View.GONE);
	}
}

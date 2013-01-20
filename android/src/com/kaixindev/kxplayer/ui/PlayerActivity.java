package com.kaixindev.kxplayer.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.kaixindev.android.Log;
import com.kaixindev.android.service.IntentDispatcher;
import com.kaixindev.android.service.IntentHandler;
import com.kaixindev.core.StringUtil;
import com.kaixindev.kxplayer.FavoritesManager;
import com.kaixindev.kxplayer.Player;
import com.kaixindev.kxplayer.R;
import com.kaixindev.kxplayer.service.FavoritesService;
import com.kaixindev.kxplayer.service.PlayerService;

public class PlayerActivity extends Activity {
	
	protected class BroadcastHandler {
		
		@IntentHandler(action=PlayerService.PLAYER_NOTICE)
		public void updatePlayer(final Intent intent, final Context context, final Object unused) {
			int state = intent.getIntExtra(PlayerService.PROPERTY_STATE, Player.STATE_IDLE);
			updatePlayerState(state);
		}
		
		@IntentHandler(action=Intent.ACTION_MEDIA_BUTTON)
		public void handleMediaButton(final Intent intent, final Context context, final Object unused) {
			SeekBar volSeekbar = (SeekBar)findViewById(R.id.control_volume);
			KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_VOLUME_DOWN:
			case KeyEvent.KEYCODE_VOLUME_UP:
				final AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
				volSeekbar.setProgress(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
				break;
			}
		}
	}
	
	private class BroReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			mIntentDispatcher.handle(intent, PlayerActivity.this, null);
		}
	}
	
	private BroReceiver mRecv = new BroReceiver();
	private IntentDispatcher mIntentDispatcher = new IntentDispatcher();
	
	private class MessengerHandler extends Handler {
		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
			case FavoritesManager.MSG_LOAD_OK:
				String uri = mPlayerCLI.getPlayingUri();
				ImageView btnFavor = (ImageView)findViewById(R.id.control_favor);
				if (mFavClient.hasFavorite(uri)) {
					btnFavor.setBackgroundResource(R.drawable.btn_favor_active);
				} else {
					btnFavor.setBackgroundResource(R.drawable.btn_favor_normal);
				}
				break;
			}
		}
	}
	private FavoritesService.Client mFavClient;
	private Messenger mMessenger;
	private ServiceConnection mServConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mFavClient = (FavoritesService.Client)service;
			mFavClient.loadFavorites(mMessenger);
		}
	};
	
	private void bindFavoritesService() {
		mMessenger = new Messenger(new MessengerHandler());
		if (!bindService(new Intent(FavoritesService.ACTION), mServConn, BIND_AUTO_CREATE)) {
			Log.e("unable to bind Favorites Service.");
		}
	}
	
	private void unbindFavoritesService() {
		if (mFavClient != null) {
			unbindService(mServConn);
		}
	}
	
	private void initButtons() {
		final AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
		final int totalVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int currentVolumn = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		SeekBar volSeekbar = (SeekBar)findViewById(R.id.control_volume);
		volSeekbar.setMax(totalVolumn);
		volSeekbar.setProgress(currentVolumn);
		volSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {	
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				am.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
			}
		});
		
		ImageButton btnBack = (ImageButton) findViewById(R.id.go_back);
		btnBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
        final ImageButton btnStart = (ImageButton)findViewById(R.id.control_play);
        btnStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int state = mPlayerCLI.getState();
				switch (state) {
				case Player.STATE_IDLE:
				case Player.STATE_PAUSED:
				case Player.STATE_ERROR:
					mPlayerCLI.resume();
					updatePlayerState(Player.STATE_PLAYING);
					break;
				}
			}
		});
        
        final ImageButton btnPause = (ImageButton)findViewById(R.id.control_pause);
        btnPause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int state = mPlayerCLI.getState();
				switch (state) {
				case Player.STATE_CONNECTING:
				case Player.STATE_BUFFERING:
				case Player.STATE_PLAYING:
					mPlayerCLI.pause();
					updatePlayerState(Player.STATE_PAUSED);
					break;
				}
			}
		});		
        
        final ImageButton btnFavor = (ImageButton)findViewById(R.id.control_favor);
        btnFavor.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				String uri = mPlayerCLI.getPlayingUri();
				if (!StringUtil.isEmpty(uri)) {
					if (mFavClient != null) {
						if (!mFavClient.hasFavorite(uri)) {
							mFavClient.addFavor(uri);
							mFavClient.flushFavorites(null);
							btnFavor.setBackgroundResource(R.drawable.btn_favor_active);
						} else {
							mFavClient.removeFavor(uri);
							mFavClient.flushFavorites(null);
							btnFavor.setBackgroundResource(R.drawable.btn_favor_normal);
						}
					}
				}
			}
		});
        
        if (mPlayerCLI != null) {
        	updatePlayerState(mPlayerCLI.getState());
        } else {
        	updatePlayerState(Player.STATE_IDLE);
        }
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);
		initButtons();
		mIntentDispatcher.registerHandlers(new BroadcastHandler());
		bindFavoritesService();
		bindPlayerService();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindFavoritesService();
		unbindPlayerService();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(PlayerService.PLAYER_NOTICE);
		registerReceiver(mRecv, filter);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mRecv);
	}
	
	private void updatePlayerState(int state) {
		ImageButton btnPlay = (ImageButton)findViewById(R.id.control_play);
		ImageButton btnPause = (ImageButton)findViewById(R.id.control_pause);
		switch (state) {
		case Player.STATE_IDLE:
		case Player.STATE_ERROR:
			btnPlay.setVisibility(View.VISIBLE);
			btnPause.setVisibility(View.GONE);
			break;
		case Player.STATE_BUFFERING:
		case Player.STATE_CONNECTING:
		case Player.STATE_PLAYING:
			btnPlay.setVisibility(View.GONE);
			btnPause.setVisibility(View.VISIBLE);
			break;
		case Player.STATE_PAUSED:
			btnPlay.setVisibility(View.VISIBLE);
			btnPause.setVisibility(View.GONE);
			break;
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
			mPlayerCLI = (PlayerService.Client)service;
			updatePlayerState(mPlayerCLI.getState());
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

}








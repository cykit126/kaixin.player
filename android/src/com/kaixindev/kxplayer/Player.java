package com.kaixindev.kxplayer;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.os.Handler;

import com.kaixindev.android.Log;
import com.kaixindev.core.StringUtil;
import com.kaixindev.kxplayer.service.PlayerService;

public class Player implements Agent.OnStartListener, 
							Agent.OnReceiveListener, 
							Agent.OnFinishListener, 
							OnPlaybackPositionUpdateListener 
{	
	public static final int STATE_IDLE = 1;
	public static final int STATE_CONNECTING = 2;
	public static final int STATE_BUFFERING = 3;
	public static final int STATE_PLAYING = 4;
	public static final int STATE_PAUSED = 5;
	public static final int STATE_ERROR = 6;
	
	private static final int NOTIFY_PERIOD = 4096;
	private static final int BUFFER_SIZE = 32 * 1024;
	
	private int mState = STATE_IDLE;
	
	private AudioTrack mAudioTrack;
	private Agent mAgent;
	private Context mContext;
	private String mUri;
	private int mBufferSize = 0;
	
	private Handler mHandler = new Handler();
		
	public Player(Context context) {
		mAgent = Agent.create(this, this, this);
		mContext = context;
	}
	
	public void finalize() {
		stop();
	}
	
	synchronized public int getState() {
		return mState;
	}
	
	synchronized public void setState(int state) {
		mState = state;
		notify(state);
	}
	
	synchronized public boolean open(String uri) {
		if (StringUtil.isEmpty(uri)) {
			Log.e("uri is empty.");
			return false;
		}
		abort();
		mUri = uri;
		if (mAgent.open(uri) == 0) {
			setState(STATE_CONNECTING);
			return true;
		} else {
			setState(STATE_ERROR);
			return false;
		}
	}
	
	synchronized public void resume() {
		if (getState() == STATE_PAUSED) {
			mAgent.resume();
			mAudioTrack.play();
			setState(STATE_PLAYING);
		}
	}
	
	synchronized public void stop() {
		mAudioTrack.release();
		mAudioTrack = null;
		mAgent.release();
		mAgent = null;
	}
	
	synchronized public void pause() {
		int state = getState();
		if (state == STATE_BUFFERING || 
				state == STATE_PLAYING ||
				state == STATE_CONNECTING) 
		{
			if (mAgent != null) {
				mAgent.pause();
			}
			if (mAudioTrack != null) {
				mAudioTrack.pause();
			}
			setState(Player.STATE_PAUSED);
		}
	}
	
	synchronized public void abort() {
		int state = getState();
		if (state == STATE_BUFFERING ||
				state == STATE_PLAYING ||
				state == STATE_CONNECTING) 
		{
			if (mAudioTrack != null) {
				mAudioTrack.release();
				mAudioTrack = null;
			}
			if (mAgent != null) {
				mAgent.abort();
			}
			setState(Player.STATE_IDLE);
		}
	}
	
	@Override
	synchronized public void onReceive(byte[] data) {
		mBufferSize += data.length;
		//Log.d("onReceive # sPlayer buffer size:" + mBufferSize);
		if (getState() == STATE_BUFFERING) {
			if (mBufferSize >= BUFFER_SIZE) {
				mAudioTrack.play();
				setState(Player.STATE_PLAYING);
			}
		}
		if (mAudioTrack != null) {
			mAudioTrack.write(data, 0, data.length);
		}
	}

	@Override
	synchronized public int onStart(AVContext context) {
        int channels = context.mAudioChannels == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        int bufferSize = AudioTrack.getMinBufferSize(
        		context.mAudioSampleRate, 
        		channels, 
        		AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(
				AudioManager.STREAM_MUSIC,
				context.mAudioSampleRate,
				channels,
				AudioFormat.ENCODING_PCM_16BIT,
				bufferSize,
				AudioTrack.MODE_STREAM);
        mAudioTrack.setPositionNotificationPeriod(NOTIFY_PERIOD);
        mAudioTrack.setPlaybackPositionUpdateListener(this);
        //mAudioTrack.play();
        //setState(STATE_PLAYING);
        setState(STATE_BUFFERING);  
       
        return 0;
	}
	
	synchronized public String getPlayingUri() {
		return mUri;
	}

	@Override
	public void onFinish(int status) {
		setState(status == Agent.STATUS_ERROR ? Player.STATE_ERROR : Player.STATE_IDLE);
	}
	
	public void notify(int status) {
		Intent intent = new Intent(PlayerService.PLAYER_NOTICE);
		intent.putExtra(PlayerService.PROPERTY_STATE, status);
		intent.putExtra(PlayerService.PROPERTY_URI, mUri);
		mContext.sendBroadcast(intent);
	}

	@Override
	public void onMarkerReached(AudioTrack track) {
	}

	@Override
	public void onPeriodicNotification(final AudioTrack track) {
		mHandler.post(new Runnable() {
			public void run() {
				int format = track.getAudioFormat();
				int frameSizeInBytes = track.getChannelCount()
						* (format == AudioFormat.ENCODING_PCM_8BIT ? 1 : 2);
				mBufferSize -= frameSizeInBytes * NOTIFY_PERIOD;
				//Log.d("onNotify # Player buffer size:" + mBufferSize);
				if (mBufferSize <= 0) {
					track.pause();
					setState(STATE_BUFFERING);
					mBufferSize = 0;
				}
			}
		});
	}
}

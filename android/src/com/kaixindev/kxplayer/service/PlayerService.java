package com.kaixindev.kxplayer.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.kaixindev.android.Log;
import com.kaixindev.core.IOUtil;
import com.kaixindev.kxplayer.Player;

public class PlayerService extends Service {
	public static final String PLAYER_NOTICE = "com.kaixindev.kxplayer.PLAYER_NOTICE";
	public static final String PROPERTY_STATE = "state";
	public static final String PROPERTY_URI = "uri";
	
	public static final String ACTION = "com.kaixindev.kxplayer.PLAYER_SERVICE";

	public class Client extends Binder {
		public boolean open(String uri) {
			String resolved = uri;
			String proto = IOUtil.getProtocol(uri);
			if (proto.equals(IOUtil.PROTOCOL_HTTP)) {
				try {
					URL oUrl = new URL(uri);
					HttpURLConnection conn = (HttpURLConnection)oUrl.openConnection();
					conn.addRequestProperty("User-Agent", "Lavf54.2.100");
					conn.addRequestProperty("Accept", "*/*");
					conn.addRequestProperty("Range", "0-");
					conn.addRequestProperty("Connection", "close");
					conn.connect();
					BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					String firstLine = br.readLine();
					if (firstLine != null && firstLine.equalsIgnoreCase("[Reference]")) {
						Pattern p = Pattern.compile("\\w+=http://(.*)");
						Matcher matcher = p.matcher(br.readLine());
						if (matcher.matches() && matcher.groupCount() > 0) {
							resolved = "mmsh://" + matcher.group(1);
							Log.d("revoled url " + uri + "to " +resolved);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
			Log.d("open uri:" + resolved);
			return mPlayer.open(resolved);
		}
		
		public void pause() {
			mPlayer.pause();
		}
		
		public void abort() {
			mPlayer.abort();
		}
		
		public void resume() {
			mPlayer.resume();
		}
		
		public void stop() {
			mPlayer.stop();
		}
		
		public int getState() {
			return mPlayer.getState();
		}
		
		public String getPlayingUri() {
			return mPlayer.getPlayingUri();
		}
	}
	
	private Client mClient = new Client();
	private Player mPlayer;
	
	@Override
	public IBinder onBind(Intent intent) {
		mPlayer = new Player(this);
		return mClient;
	}
}

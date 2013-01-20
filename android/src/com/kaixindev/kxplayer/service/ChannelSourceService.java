package com.kaixindev.kxplayer.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.kaixindev.android.Log;
import com.kaixindev.core.IOUtil;
import com.kaixindev.core.ThreadWorker;
import com.kaixindev.kxplayer.Channel;
import com.kaixindev.kxplayer.config.Config;
import com.kaixindev.serialize.XMLSerializer;

public class ChannelSourceService extends Service {
	
	public static final String ACTION = "com.kaixindev.kxplayer.CHANNEL_SOURCE_SERVICE";
	
	public static final int MSG_OK 		= 201;
	public static final int MSG_ERROR 	= 202;

	private static Map<String,Channel> mChannels = new HashMap<String,Channel>();
	private ThreadWorker mWorker = new ThreadWorker();
	private boolean mLoaded = false;
	
	public static Channel getChannel(String uri) {
		return mChannels.get(uri);
	}
	
	public static boolean hasChannel(String uri) {
		return mChannels.containsKey(uri);
	}
	
	public static List<Channel> getChannels() {
		List<Channel> channels = new ArrayList<Channel>();
		for (Channel ch : mChannels.values()) {
			channels.add(ch);
		}
		return channels;
	}
	
	public class Client extends Binder {
		synchronized public Channel get(String uri) {
			return mChannels.get(uri);
		}
		
		synchronized public boolean has(String uri) {
			return mChannels.containsKey(uri);
		}
		
		synchronized public boolean isLoaded() {
			return mLoaded;
		}
		
		synchronized public void loadChannels(boolean forceReload, final Messenger messenger) {
			if (forceReload || !mLoaded) {
				mChannels.clear();
				mWorker.pushJob(new Runnable(){
					public void run() {
						InputStream input = IOUtil.open(ChannelSourceService.this, Config.FILE_CHANNELS);
						byte[] content = IOUtil.read(input);

						XMLSerializer serializer = new XMLSerializer();
						Channel[] channels = (Channel[]) serializer.unserialize(content);
						
						for (Channel ch : channels) {
							mChannels.put(ch.uri, ch);
							Log.i(ch.name.get("zh") + ":" + ch.uri);
						}	
						
						mLoaded = true;
						Log.i("Channels are loaded.");
						
						if (messenger != null) {
							Message message = Message.obtain(null, channels==null ? MSG_ERROR : MSG_OK);
							try {
								messenger.send(message);
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
					}
				});				
			}
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		mWorker.start();
		return new Client();
	}

}

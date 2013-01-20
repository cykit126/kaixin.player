package com.kaixindev.kxplayer.service;

import java.io.File;
import java.util.Collection;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.kaixindev.android.FileSystem;
import com.kaixindev.android.Log;
import com.kaixindev.core.ThreadWorker;
import com.kaixindev.kxplayer.FavoritesManager;
import com.kaixindev.kxplayer.config.Config;

public class FavoritesService extends Service {

	public static final String ACTION = "com.kaixindev.kxplayer.FAVORITES_SERVICE";
	private ThreadWorker mThreadWorker = new ThreadWorker();
	private FavoritesManager mFavorManager;
	private boolean mLoaded = false;
	
	public class Client extends Binder {
		synchronized public void addFavor(String channel) {
			mFavorManager.addChannel(channel);
		}
		
		synchronized public boolean hasFavorite(String uri) {
			return mFavorManager.hasChannel(uri);
		}
		
		synchronized public void removeFavor(String channel) {
			mFavorManager.removeChannel(channel);
		}
		
		synchronized public Collection<String> getFavorites() {
			return mFavorManager.getChannels();
		}
		
		synchronized public void flushFavorites(final Messenger messenger) {
			mThreadWorker.pushJob(new Runnable(){
				public void run() {
					int msgid = mFavorManager.flush() ? FavoritesManager.MSG_FLUSH_OK : FavoritesManager.MSG_FLUSH_ERROR;
					if (messenger != null) {
						Message message = Message.obtain(null, msgid);
						try {
							messenger.send(message);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}
			});
		}
		
		synchronized public boolean isLoaded() {
			return mLoaded;
		}
		
		public void loadFavorites(final Messenger messenger) {
			mThreadWorker.pushJob(new Runnable(){
				public void run() {
					int msgid = mFavorManager.load(false) ? FavoritesManager.MSG_LOAD_OK : FavoritesManager.MSG_LOAD_ERROR;
					if (messenger != null) {
						Message message = Message.obtain(null, msgid);
						try {
							messenger.send(message);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
					mLoaded = true;
				}
			});
		}
		
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		File file = FileSystem.getInternalFile(
				this, Config.DIR_DATA, Config.FILE_FAVORITES);
		mFavorManager = FavoritesManager.create(file);
		if (mFavorManager == null) {
			Log.e("unable to create FavoritesManager.");
		}
		mThreadWorker.start();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return new Client();
	}

}

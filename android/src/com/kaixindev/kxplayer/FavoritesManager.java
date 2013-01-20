package com.kaixindev.kxplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import com.kaixindev.android.Log;
import com.kaixindev.core.IOUtil;
import com.kaixindev.core.StringUtil;
import com.kaixindev.serialize.XMLSerializer;

public class FavoritesManager {

	public static final int MSG_LOAD_OK = 100;
	public static final int MSG_LOAD_ERROR = 101;
	public static final int MSG_FLUSH_OK = 102;
	public static final int MSG_FLUSH_ERROR = 103;

	private Collection<String> mChannels = new ArrayList<String>();
	private File mFile;
	private boolean mLoaded = false;

	private FavoritesManager(File file) {
		mFile = file;
	}

	public static FavoritesManager create(File file) {
		if (file == null) {
			return null;
		}
		return new FavoritesManager(file);
	}

	synchronized public boolean hasChannel(String uri) {
		return mChannels.contains(uri);
	}

	synchronized public boolean addChannel(String uri) {
		if (StringUtil.isEmpty(uri)) {
			return false;
		}

		if (!hasChannel(uri)) {
			mChannels.add(uri);
		}

		return true;
	}

	synchronized public void removeChannel(String uri) {
		if (StringUtil.isEmpty(uri)) {
			return;
		}
		mChannels.remove(uri);
	}

	synchronized public boolean load(boolean forceReload) {
		if (forceReload || !mLoaded) {
			mChannels.clear();
			if (mFile.exists()) {
				byte[] content = IOUtil.read(mFile);
				Log.i("load favorites:" + new String(content));
				XMLSerializer serializer = new XMLSerializer();
				try {
					String[] channels = (String[]) serializer.unserialize(content);
					if (channels != null) {
						Log.i("loaded favorites:" + channels.length);
						for (String ch : channels) {
							mChannels.add(ch);
						}
					} else {
						Log.i("can't load favorites.");
					}
				} catch (ClassCastException e) {
					e.printStackTrace();
				}
				mLoaded = true;
			}
		}

		return true;
	}

	synchronized public boolean flush() {
		if (!mLoaded) {
			load(true);
		}
		
		if (!mFile.exists() && !IOUtil.createFile(mFile)) {
			return false;
		}
		XMLSerializer serializer = new XMLSerializer();
		byte[] content = serializer.serialize(mChannels.toArray(new String[0]));
		if (content == null) {
			return false;
		}

		Log.i("flush favorites:" + new String(content));
		return IOUtil.write(mFile, content, 0, content.length);
	}

	synchronized public Collection<String> getChannels() {
		return mChannels;
	}
}

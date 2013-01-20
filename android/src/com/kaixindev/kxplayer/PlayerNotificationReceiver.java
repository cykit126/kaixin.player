package com.kaixindev.kxplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.kaixindev.kxplayer.service.PlayerService;

public class PlayerNotificationReceiver extends BroadcastReceiver {
	
	public static final int MSG_NOTICE = 401;
	
	private Messenger mMessenger = null;
	
	public PlayerNotificationReceiver(Messenger messenger) {
		mMessenger = messenger;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		int state = intent.getIntExtra(PlayerService.PROPERTY_STATE, Player.STATE_IDLE);
		String uri = intent.getStringExtra(PlayerService.PROPERTY_URI);
		Message message = Message.obtain(null, MSG_NOTICE, state, 0, uri);
		try {
			mMessenger.send(message);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

}

package com.kaixindev.kxplayer.service;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.widget.RemoteViews;

import com.flurry.android.FlurryAgent;
import com.kaixindev.android.Application;
import com.kaixindev.android.FileSystem;
import com.kaixindev.android.Log;
import com.kaixindev.core.Hash;
import com.kaixindev.core.IOUtil;
import com.kaixindev.core.ThreadWorker;
import com.kaixindev.io.Copier;
import com.kaixindev.io.FileOutputBuffer;
import com.kaixindev.io.InputBuffer;
import com.kaixindev.io.OutputBuffer;
import com.kaixindev.kxplayer.R;
import com.kaixindev.kxplayer.UpdateInfo;
import com.kaixindev.kxplayer.config.Config;
import com.kaixindev.net.HTTPDownloadBuilder;
import com.kaixindev.serialize.XMLSerializer;

public class UpdateService extends Service {
	
	public static final String LOGTAG = "CommonService";
	public static final String ACTION = "com.kaixindev.kxplayer.COMMON_SERVICE";
	
	public static final int MSG_UPDATE_AVAILABLE = 301;
	public static final int MSG_UPDATE_TO_DATE = 302;
	
	private Client mBinder = new Client();
	private ThreadWorker mThreadWorker = new ThreadWorker();
	
	public UpdateService() {
		mThreadWorker.start();
	}
	
	public class Client extends Binder {
		public void checkUpdate(final Activity activity, final Messenger messenger) {
			mThreadWorker.pushJob(new Runnable() {
				@Override
				public void run() {				
					URL oUrl = null;
			        try {
			            oUrl = new URL(Config.UPDATE_URI);
			            InputStream input = oUrl.openStream();
			            byte[] content = IOUtil.read(input);
			            XMLSerializer serializer = new XMLSerializer();
			            final UpdateInfo info = (UpdateInfo)serializer.unserialize(content);
				        if (info != null) {
							int versionCode = Application.getVersionCode(UpdateService.this);
							if (versionCode < info.versionCode) {
								Message message = Message.obtain(null, MSG_UPDATE_AVAILABLE, info);
								messenger.send(message);
							} else {
								Message message = Message.obtain(null, MSG_UPDATE_TO_DATE);
								messenger.send(message);
							}
				        }
			        }
			        catch (Exception e) {
			        	Log.w(e.getMessage());
			            e.printStackTrace();
			            return;
			        }
			    }
			});
		}
		
		public void downloadUpdate(final UpdateInfo info) {
			Map<String,String> args = new HashMap<String,String>();
			args.put("version", info.versionString);
			args.put("current version", Application.getVersionName(UpdateService.this));
			FlurryAgent.logEvent(Config.FLURRY_EVENT_UPDATE, args, true);
			mThreadWorker.pushJob(new Runnable(){
				@Override
				public void run() {
					String filename = Hash.MD5(info.packageUri.getBytes());
					File logFile = FileSystem.getProperCacheFile(
							UpdateService.this, Config.DIR_UPDATE, filename+".log");
					if (!IOUtil.createFile(logFile)) {
						Log.e(LOGTAG, "failed to create log file for update.");
						return;
					}
					File file = FileSystem.getProperCacheFile(
							UpdateService.this, Config.DIR_UPDATE, filename + ".apk");
					if (!IOUtil.createFile(file)) {
						Log.e(LOGTAG, "failed to create file for update.");
						return;
					}
					FileOutputBuffer buffer = FileOutputBuffer.newInstance(file, "rw");
					HTTPDownloadBuilder builder = new HTTPDownloadBuilder();
					Copier copier = builder.build(info.packageUri, buffer, logFile);
					copier.addOnProgressListener(new Copier.OnProgressListener() {
						@Override
						public boolean onProgress(InputBuffer input, OutputBuffer output, long current) {
							Resources r = getResources();
							String title = r.getString(R.string.update_notification_title, r.getText(R.string.app_name));
							RemoteViews view = new RemoteViews(getPackageName(), R.layout.update_notification);
							view.setProgressBar(R.id.progress, (int)input.getSize(), (int)current, input.getSize()<=0);
							view.setTextViewText(R.id.title, title);
							Notification not = new Notification(R.drawable.icon, title, System.currentTimeMillis());
							not.contentView = view;
							not.flags = Notification.FLAG_ONGOING_EVENT;
							not.contentIntent = PendingIntent.getActivity(UpdateService.this, 0, new Intent(), 0);
							getNotificationManager().notify(Config.UPDATE_NOTIFICATION_ID, not);
							return true;
						}
					});
					copier.addOnEndListener(new Copier.OnEndListener() {
						@Override
						public void onEnd(InputBuffer input, OutputBuffer output, long size) {
							Resources r = getResources();
							String title = r.getString(R.string.app_name);
							String content = r.getString(R.string.update_notification_confirm_installation);
							Notification not = new Notification(R.drawable.icon, title, System.currentTimeMillis());
							Intent intent = Application.buildInstallIntent(UpdateService.this, output.getPath());
							not.setLatestEventInfo(
									UpdateService.this, 
									title, 
									content, 
									PendingIntent.getActivity(UpdateService.this, 0, intent, 0));
							not.flags = Notification.FLAG_AUTO_CANCEL;
							getNotificationManager().notify(Config.UPDATE_NOTIFICATION_ID, not);
						}
					});
					if (copier.copy() < 0) {
						Log.e(LOGTAG, "failed to download update apk.");
					}
				}});
		}
		
		public void askForUpdate(Activity activity, final UpdateInfo info) {
			AlertDialog dialog = new AlertDialog.Builder(activity)
					.setMessage(R.string.comfirm_update)
					.setPositiveButton(
							R.string.update_yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									downloadUpdate(info);
								}
							})
					.setNegativeButton(R.string.update_no, null)
					.create();
			dialog.show();		
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	private NotificationManager getNotificationManager() {
		return (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}
}












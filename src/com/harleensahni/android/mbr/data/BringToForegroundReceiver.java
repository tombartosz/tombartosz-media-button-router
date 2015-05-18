package com.harleensahni.android.mbr.data;

import com.harleensahni.android.mbr.Constants;
import com.harleensahni.android.mbr.Utils;

import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

/**
 * 
 * @author bartosz.tomaszewski
 * 
 */
public class BringToForegroundReceiver extends Receiver {

	private final ApplicationInfo applicationInfo;
	private final Context context;
	private final SharedPreferences preferences;

	public BringToForegroundReceiver(final ApplicationInfo applicationInfo,
			final Context context) {
		super();

		this.applicationInfo = applicationInfo;
		this.context = context;
		this.preferences = PreferenceManager.getDefaultSharedPreferences(context);   

		final PackageManager packageManager = this.context.getPackageManager();

		setPrefix("Switch to:");
		setName(packageManager.getApplicationLabel(applicationInfo).toString());
		setIcon(packageManager.getApplicationIcon(applicationInfo));
	}


	@Override
	public void onSelect(int position) {
		Intent launchIntent = context.getPackageManager()
				.getLaunchIntentForPackage(applicationInfo.packageName);

		context.startActivity(launchIntent);
		
		/* unlock device */
		KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE); 
		final KeyguardManager.KeyguardLock kl = km .newKeyguardLock("MyKeyguardLock"); 
		kl.disableKeyguard(); 

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE); 
		WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
		                                 | PowerManager.ACQUIRE_CAUSES_WAKEUP
		                                 | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
		wakeLock.acquire(Constants.WAKE_TIME);


		
/*		String last_media_button_receiver = preferences.getString(Constants.LAST_MEDIA_BUTTON_RECEIVER,
                null);
		
		ComponentName cn = ComponentName.unflattenFromString(last_media_button_receiver);
		
       	//Set the current audio player name/package
    	preferences.edit().putString(Constants.CURRENT_AUDIO_PLAYER_NAME, cn.getShortClassName())
		  				  .putString(Constants.CURRENT_AUDIO_PLAYER_PACKAGE, cn.getPackageName())
		  				  .commit();*/
    	
    	Utils.forwardKeyCodeToComponent(context, null, false, KeyEvent.KEYCODE_MEDIA_PLAY, null, true);


	}

}

package com.harleensahni.android.mbr.utils;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.media.ToneGenerator;

import com.harleensahni.android.mbr.data.BringToForegroundReceiver;
import com.harleensahni.android.mbr.data.Receiver;

/**
 * 
 * @author bartosz.tomaszewski
 * 
 */
public class AndroidAppsUtils {

	private final Context context;

	public AndroidAppsUtils(final Context context) {
		this.context = context;
	}
	

	public List<Receiver> getRunningAppsBringToFrontReceivers() {
		final ActivityManager activityManager = (ActivityManager) this.context
				.getSystemService(Context.ACTIVITY_SERVICE);
		final PackageManager packageManager = this.context.getPackageManager();
		final List<RunningAppProcessInfo> procInfos = activityManager
				.getRunningAppProcesses();

		List<Receiver> ret = new ArrayList<Receiver>();
		for (RunningAppProcessInfo procInfo : procInfos) {

			ApplicationInfo ai;
			try {
				ai = packageManager.getApplicationInfo(procInfo.processName, 0);
			} catch (final NameNotFoundException e) {
				continue;
			}

			if (AndroidAppsUtils.isSystemApp(ai) == false) {
				ret.add(new BringToForegroundReceiver(ai, this.context));
			}

		}

		return ret;
	}

	public static boolean isSystemApp(ApplicationInfo applicationInfo) {
		
	    int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_PERSISTENT ;

		
		if ((applicationInfo.flags & mask) != 0 ) {
			return true;
		}
		else if (applicationInfo.packageName.startsWith("com.google.android") && !applicationInfo.packageName.contains("maps")) {
			return true;
		}
	
		else {
			return false;
		}
	}

}

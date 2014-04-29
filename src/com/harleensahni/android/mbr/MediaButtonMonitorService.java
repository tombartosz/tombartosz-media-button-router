/*
 * Copyright 2011 Peter Haight, Harleen Sahni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.harleensahni.android.mbr;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.harleensahni.android.mbr.receivers.MediaButtonReceiver;

/**
 * Monitors when the media button receiver registered with the audio manager changes, and sets 
 * it back to media button router's receiver. Allows media button router to correctly intercept
 * all media button presses.
 * 
 * @author Peter Haight
 */
 public class MediaButtonMonitorService extends Service {
    public static final String TAG = "MediaButtonMonitorService";
   // public SettingsObserver mSettingsObserver;
    public ComponentName mComponentName;
    public AudioManager mAudioManager;
    
    /**
     * Prevents executing thread for more than once 
     */
    public static boolean isRunning = false;

   /* private class SettingsObserver extends ContentObserver {
        ContentResolver mContentResolver;
        MediaButtonMonitorService mMonitorService;
        private static final String MEDIA_BUTTON_RECEIVER = "media_button_receiver";

        SettingsObserver(MediaButtonMonitorService monitorService) {

            super(new Handler());
            mMonitorService = monitorService;
            mContentResolver = mMonitorService.getContentResolver();
            mContentResolver.registerContentObserver(Settings.System.getUriFor(MEDIA_BUTTON_RECEIVER), false, this);
        }

        public void onChange(boolean selfChange) {
            Log.d("SettingsObserver", "onChange(" + selfChange + ")");
            String receiverName = Settings.System.getString(mContentResolver, MEDIA_BUTTON_RECEIVER);
            Log.i("SettingsObserver", "MEDIA_BUTTON_RECEIVER changed to " + receiverName);
            Log.d("SettingsObserver", "'" + receiverName + "' == '" + mMonitorService.mComponentName.flattenToString()
                    + "'");
            if (!selfChange
                    && !receiverName.equals(mMonitorService.mComponentName.flattenToString())
                    && !receiverName
                            .equals("com.harleensahni.android.mbr/com.harleensahni.android.mbr.ReceiverSelector$1")) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mMonitorService
                        .getApplicationContext());
                preferences.edit().putString(Constants.LAST_MEDIA_BUTTON_RECEIVER, receiverName).commit();
                Log.d("SettingsObserver", "Set LAST_MEDIA_BUTTON_RECEIVER to" + receiverName);
                mMonitorService.registerMediaButtonReceiver();
            }
        }
    } */

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        Log.d(TAG, "onCreate()");
       // mComponentName = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());
       // mSettingsObserver = new SettingsObserver(this);
       // mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand(" + intent + ", " + flags + ", " + startId);
        registerMediaButtonReceiver();
        return START_STICKY;
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy() called. Unregistering media button receiver.");
        mAudioManager.unregisterMediaButtonEventReceiver(mComponentName);
    }

    private static long reregisterCounter = 0;
    
    public void registerMediaButtonReceiver() {
		if (!isRunning) {
			isRunning = true;
			Runnable r = new Runnable() {

				@Override
				public void run() {
					while (true)
						try {
							Thread.sleep(15000);

							/*
							 * This registers MultimediaEventReciver to be sole
							 * bluetooth multimedia button receiver
							 */
							AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
							ComponentName cn = new ComponentName(
									getPackageName(),
									MediaButtonReceiver.class.getName());

							manager.registerMediaButtonEventReceiver(cn);

							Log.i("BT", "reregister! " + (++reregisterCounter));

						} catch (InterruptedException e) {
							e.printStackTrace();
						}
				}
			};
			Thread t = new Thread(r);
			t.start();
			
			Log.d(TAG, "registerMediaButtonReceiver()");
		}
		else {
    	
			Log.d(TAG, "registerMediaButtonReceiver() - not started");
		}
        
        //mAudioManager.registerMediaButtonEventReceiver(mComponentName);
    }
}

/*
 * Copyright 2011 Harleen Sahni
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
package com.harleensahni.android.mbr.receivers;

import static com.harleensahni.android.mbr.Constants.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.harleensahni.android.mbr.Constants;
import com.harleensahni.android.mbr.MediaButtonReceiverService;
import com.harleensahni.android.mbr.Utils;

/**
 * Handles routing media button intents to application that is playing music
 * 
 * @author Harleen Sahni
 */
public class MediaButtonReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        
    	if (!intent.getBooleanExtra("mbrIgnore", false) &&
    		preferences.getBoolean(Constants.ENABLED_PREF_KEY, true)) {
        
	        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	        if (telephony.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK ||
	        	telephony.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
	        		return;
	        }
	        
	        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
	        	
	            Log.i(TAG, "Media Button Receiver: received media button intent: " + intent);
		    	KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
		        int keyCode = Utils.getAdjustedKeyCode(keyEvent);        
		        
		        // Don't want to capture volume buttons
		        if (Utils.isMediaButton(keyCode)) {
		        	Intent receiver_service_intent = new Intent(context, MediaButtonReceiverService.class);
	                receiver_service_intent.putExtras(intent);
	                context.startService(receiver_service_intent);
					if (isOrderedBroadcast()) {
						abortBroadcast();
					}
		        }	        
	    	}
    	}
    }
}

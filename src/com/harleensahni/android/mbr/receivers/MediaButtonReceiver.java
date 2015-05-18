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
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.harleensahni.android.mbr.Constants;
import com.harleensahni.android.mbr.MediaButtonReceiverService;
import com.harleensahni.android.mbr.Utils;
import com.harleensahni.android.mbr.utils.Preferences;

/**
 * Handles routing media button intents to application that is playing music
 *
 * @author Harleen Sahni
 */
public class MediaButtonReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Preferences.init(preferences);

        boolean isAppEnabled = Preferences.isEnabled();

        if (!isAppEnabled) {
            /* if app is not enabled in Settings do nothing */
            return;
        }

        if (intent.getBooleanExtra("mbrIgnore", false)) {
			/* do nothing */
            return;
        }

        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK ||
                telephony.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
			/* in in call or ringing - do nothing */
            return;
        }

        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {

            Log.i(TAG, "Media Button Receiver: received media button intent: " + intent);
            KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
            int keyCode = Utils.getAdjustedKeyCode(keyEvent);

            /* Button press received here */
            Toast.makeText(context, "Starting ...", Toast.LENGTH_SHORT).show();

            Log.d(TAG, "Media button press received. Button code: " + keyCode);
            if (Utils.isMediaButton(keyCode)) {

                if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                    /* Start App only when button is pressed */
                    Intent mediaButtonRouterServiceIntent = new Intent(context,
                            MediaButtonReceiverService.class);

                    mediaButtonRouterServiceIntent.putExtras(intent);
                    mediaButtonRouterServiceIntent.putExtra(Constants.INTENT_KEY_CODE, keyCode);
                    context.startService(mediaButtonRouterServiceIntent);
                }

                /* Now event will be handled by the App */
                if (isOrderedBroadcast()) {
                    abortBroadcast();
                }
            }
        }
    }
}
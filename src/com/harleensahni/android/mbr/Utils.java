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
package com.harleensahni.android.mbr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.harleensahni.android.mbr.receivers.MediaButtonReceiver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class.
 *
 * @author Harleen Sahni
 */
public final class Utils {

    private static final String TAG = "MediaButtonRouter";
//    private static final String GOOGLE_MUSIC_RECEIVER = "com.google.blahdfdf";

    public static final int KEYCODE_MEDIA_PLAY = 126;
    public static final int KEYCODE_MEDIA_PAUSE = 127;
    public static final int ICS_API_LEVEL = 14;

    /**
     * Prevent instantiation.
     */
    private Utils() {
        // Intentionally blank
    }

    /**
     * Whether the keyCode represents a media button that we handle.
     *
     * @param keyCode
     * @return
     */
    public static boolean isMediaButton(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KEYCODE_MEDIA_PLAY || keyCode == KEYCODE_MEDIA_PAUSE
                || keyCode == KeyEvent.KEYCODE_HEADSETHOOK;
    }

    public static void forwardKeyCodeToComponent(Context context, ComponentName selectedReceiver, boolean launch,
                                                 int keyCode, BroadcastReceiver cleanUpReceiver) {
        forwardKeyCodeToComponent(context, selectedReceiver, launch, keyCode, cleanUpReceiver, false);
    }

    /**
     * Forwards {@code keyCode} to receiver specified as two key events, one for
     * up and one for down. Optionally launches the application for the
     * receiver.
     *
     * @param context
     * @param selectedReceiver
     * @param launch
     * @param keyCode
     * @param cleanUpReceiver
     */
    public static void forwardKeyCodeToComponent(final Context context, ComponentName selectedReceiver, boolean launch,
                                                 int keyCode, BroadcastReceiver cleanUpReceiver, boolean mbrIgnore) {

        String appPkg = selectedReceiver.getPackageName();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (launch) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appPkg);
            if (launchIntent != null) {
                try {
                    /* starts activity in one way ...*/
                    context.startActivity(launchIntent);


                } catch (ActivityNotFoundException e) {
                    //Alright. The straightforward approach didn't work.
                    //Try this way instead...
                    startApplication(context, selectedReceiver.getPackageName());
                }



                /*
                 * when selected activity is receiver of media button events - show main screen.
                 * it allows media button router still to be receiver
                 */

                if (isMediaReceiver(context, selectedReceiver)) {


                    forwardMediaKeyToPlayer(context, keyCode);

                    Intent lastAppIntent = null;

                    lastAppIntent = new Intent(Intent.ACTION_MAIN);
                    lastAppIntent.addCategory(Intent.CATEGORY_HOME);
                    lastAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(lastAppIntent);


                }

            }
        }

    }

    public static void forwardMediaKeyToPlayer(Context context, int keyCode) {
        Intent i = new Intent("com.android.music.musicservicecommand");

        keyCode = getAdjustedKeyCode(keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                i.putExtra("command", "togglepause");
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                i.putExtra("command", "stop");
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                i.putExtra("command", "pause");
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                i.putExtra("command", "play");
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                i.putExtra("command", "next");
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                i.putExtra("command", "previous");
                break;
            default:
                break;
        }

        context.sendBroadcast(i);
    }

    public static boolean isMediaReceiver(Context context, ComponentName componentName) {

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        String packageName = componentName.getPackageName();

        List<ResolveInfo> mediaReceivers = context.getPackageManager().queryBroadcastReceivers(mediaButtonIntent,
                PackageManager.GET_INTENT_FILTERS | PackageManager.GET_RESOLVED_FILTER);

        for (ResolveInfo info : mediaReceivers) {
            String infoPackage = info.activityInfo.packageName;
            if (infoPackage.equals(packageName)) {
                return true;
            }
        }

        return false;

    }

    /**
     * Gets the list of available media receivers, optionally filtering out ones
     * the user has indicated should be hidden in preferences.
     *
     * @param packageManager The {@code PackageManager} used to retrieve media button
     *                       receivers.
     * @param filterHidden   Whether user-hidden media receivers should be shown.
     * @return The list of {@code ResolveInfo} for different media button
     * receivers.
     */
    public static List<ResolveInfo> getMediaReceivers(PackageManager packageManager, boolean filterHidden,
                                                      Context context) {
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);

        List<ResolveInfo> mediaReceivers = packageManager.queryBroadcastReceivers(mediaButtonIntent,
                PackageManager.GET_INTENT_FILTERS | PackageManager.GET_RESOLVED_FILTER);

        String hiddenReceiverIdsString = "";
        List<String> hiddenIds = new ArrayList<String>();

        if (filterHidden) {
            hiddenReceiverIdsString = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.HIDDEN_APPS_KEY, "");
            hiddenIds = Arrays.asList(hiddenReceiverIdsString.split(","));
        }

        for (int i = mediaReceivers.size() - 1; i >= 0; i--) {

            ResolveInfo mediaReceiverResolveInfo = mediaReceivers.get(i);

            if (MediaButtonReceiver.class.getName().equals(mediaReceiverResolveInfo.activityInfo.name)) {
                mediaReceivers.remove(i);
                if (!filterHidden) {
                    break;
                }
            } else if (filterHidden) {

                // i have to be more exact than just application name because
                // the two versions (old and new) of google music
                // have the same classnames for their intent receivers. I need
                // to know where their apks live to be able to differentiate.
//	            String name = mediaReceiverResolveInfo.activityInfo.applicationInfo.sourceDir
//	                    + mediaReceiverResolveInfo.activityInfo.name;

                String receiverId = mediaReceiverResolveInfo.activityInfo.name;
                // Don't think the following is an issue anymore with the latest versions of google play, if it is i'll add back
//        if (GOOGLE_MUSIC_RECEIVER.contains(receiverId)) {
//        // i have to be more exact than just application name because
//        // the two versions (old and new) of google music
//        // have the same classnames for their intent receivers. I need
//        // to know where their apks live to be able to differentiate.
//            receiverId = resolveInfo.activityInfo.applicationInfo.sourceDir + receiverId;
//        }
                String name = receiverId;


                if (hiddenIds.contains(name)) {
                    mediaReceivers.remove(i);
                }
            }
        }

        return mediaReceivers;
    }

    /**
     * Gets the list of available apps, optionally filtering out ones
     * the user has indicated should be hidden in preferences.
     *
     * @param packageManager The {@code PackageManager} used to retrieve  apps.
     * @param filterHidden   Whether user-hidden apps should be shown.
     * @return The list of {@code ResolveInfo} for different voice command
     * apps.
     */
    public static List<ResolveInfo> getAllApps(PackageManager packageManager, boolean filterHidden,
                                               Context context) {

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent,
                PackageManager.GET_INTENT_FILTERS | PackageManager.GET_RESOLVED_FILTER);

        String hiddenReceiverIdsString = "";
        List<String> hiddenIds = new ArrayList<String>();

        if (filterHidden) {
            hiddenReceiverIdsString = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.HIDDEN_APPS_KEY, "");
            hiddenIds = Arrays.asList(hiddenReceiverIdsString.split(","));
        }

        for (int i = apps.size() - 1; i >= 0; i--) {

            ResolveInfo appResolveInfo = apps.get(i);

            if (ReceiverSelector.class.getName().equals(appResolveInfo.activityInfo.name)) {
                apps.remove(i);
                if (!filterHidden) {
                    break;
                }
            } else if (filterHidden) {

                // i have to be more exact than just application name because
                // the two versions (old and new) of google music
                // have the same classnames for their intent receivers. I need
                // to know where their apks live to be able to differentiate.

                //String name = voiceCommandAppResolveInfo.activityInfo.applicationInfo.sourceDir
                //        + voiceCommandAppResolveInfo.activityInfo.name;
                String receiverId = appResolveInfo.activityInfo.name;
                // Don't think the following is an issue anymore with the latest versions of google play, if it is i'll add back
//        if (GOOGLE_MUSIC_RECEIVER.contains(receiverId)) {
//        // i have to be more exact than just application name because
//        // the two versions (old and new) of google music
//        // have the same classnames for their intent receivers. I need
//        // to know where their apks live to be able to differentiate.
//            receiverId = resolveInfo.activityInfo.applicationInfo.sourceDir + receiverId;
//        }
                String name = receiverId;

                if (hiddenIds.contains(name)) {
                    apps.remove(i);
                }
            }
        }

        return apps;
    }


    public static List<ResolveInfo> getAllReceivers(PackageManager packageManager, boolean filterHidden,
                                                    boolean performSort, Context context) {

        String currAudioPlayerPkg = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.CURRENT_AUDIO_PLAYER_PACKAGE, null);
        String currAudioPlayerName = PreferenceManager.getDefaultSharedPreferences(context).getString(
                Constants.CURRENT_AUDIO_PLAYER_NAME, null);

        List<ResolveInfo> allReceivers = new ArrayList<ResolveInfo>();
        List<ResolveInfo> allApps = getAllApps(packageManager, filterHidden, context);

        if (performSort) {

            ResolveInfo currAudioPlayerRI = null;
            ResolveInfo highestPriorityVoiceCommandApp = null;

            if (allApps.size() > 0) {
                Collections.sort(allApps, new Comparator<ResolveInfo>() {

                    public int compare(ResolveInfo r1, ResolveInfo r2) {
                        return r2.priority - r1.priority;
                    }

                });

                highestPriorityVoiceCommandApp = allApps.get(0);
                allApps.remove(0);
            }

            for (ResolveInfo r1 : allApps) {
                if (r1.activityInfo.packageName.equals(currAudioPlayerPkg) &&
                        r1.activityInfo.name.equals(currAudioPlayerName)) {
                    currAudioPlayerRI = r1;
                    allApps.remove(currAudioPlayerRI);
                    break;
                }
            }

            //Join the rest and sort...
/*	    			mediaReceivers.addAll(allApps);
                    Collections.sort(mediaReceivers, new Comparator<ResolveInfo>(){
		   				 
	    	            public int compare(ResolveInfo r1, ResolveInfo r2) {
	    	               return r2.priority - r1.priority;
	    	            }
	    	 
	    	        });*/


            if (currAudioPlayerRI != null) {
                allReceivers.add(currAudioPlayerRI);
            }
            if (highestPriorityVoiceCommandApp != null) {
                allReceivers.add(highestPriorityVoiceCommandApp);
            }
            allReceivers.addAll(allApps);

        } else {
            //		allReceivers.addAll(mediaReceivers);
            allReceivers.addAll(allApps);
        }

        return allReceivers;
    }

    /**
     * Returns the name of the application of the broadcast receiver specified
     * by {@code resolveInfo}.
     *
     * @param resolveInfo The receiver.
     * @return The name of the application.
     */
    public static String getAppName(ResolveInfo resolveInfo, PackageManager packageManager) {
        return resolveInfo.activityInfo.applicationInfo.loadLabel(packageManager).toString();
    }

    public static AlertDialog showIntroifNeccessary(Context context) {
        final SharedPreferences preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferenceManager.getBoolean(Constants.INTRO_SHOWN_KEY, false)) {
            // TextView textview = new TextView(context);
            // textview.setText(context.getText(R.string.intro_text));
            Spanned s = Html.fromHtml(context.getString(R.string.intro_text));
            Builder alertDialog = new AlertDialog.Builder(context).setTitle("Introduction").setMessage(s);
            alertDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    // preferenceManager.edit().putBoolean(Constants.INTRO_SHOWN_KEY,
                    // true);
                    Log.d(TAG, "Intro cancelled. will show again.");
                }
            });
            alertDialog.setNegativeButton("Close", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {

                    preferenceManager.edit().putBoolean(Constants.INTRO_SHOWN_KEY, true).commit();

                    Log.d(TAG, "Intro closed. Will not show again.");
                }
            });
            return alertDialog.show();
        }
        return null;
    }

    public static int getAdjustedKeyCode(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        return getAdjustedKeyCode(keyCode);
    }

    public static int getAdjustedKeyCode(int keyCode) {
        if (keyCode == KEYCODE_MEDIA_PLAY || keyCode == KEYCODE_MEDIA_PAUSE) {
            keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
        }
        return keyCode;
    }


    /**
     * Whether we have to go through AudioManager's register media button
     * receiver where this is only a single media button receiver. See ticket
     * #10.
     *
     * @return
     */
    public static boolean isHandlingThroughSoleReceiver() {

        return android.os.Build.VERSION.SDK_INT >= ICS_API_LEVEL;
    }

    private static void launchComponent(Context context, String packageName, String name) {
        Intent launch_intent = new Intent("android.intent.action.VIEW");
        launch_intent.addCategory("android.intent.category.DEFAULT");
        launch_intent.setComponent(new ComponentName(packageName, name));
        launch_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        context.startActivity(launch_intent);
    }

    public static void startApplication(Context context, String application_name) {
        try {

            Intent intent = new Intent("android.intent.action.VIEW");
            intent.addCategory("android.intent.category.DEFAULT");

            //intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            List<ResolveInfo> resolveinfo_list = context.getPackageManager().queryIntentActivities(intent, 0);

            for (ResolveInfo info : resolveinfo_list) {
                if (info.activityInfo.packageName.equalsIgnoreCase(application_name)) {
                    launchComponent(context, info.activityInfo.packageName, info.activityInfo.name);
                    break;
                }
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context.getApplicationContext(), "There was a problem loading the application: " + application_name, Toast.LENGTH_SHORT).show();
        }
    }
}

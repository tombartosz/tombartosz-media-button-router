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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.harleensahni.android.mbr.receivers.MediaButtonReceiver;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

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
    public static void forwardKeyCodeToComponent(Context context, ComponentName selectedReceiver, boolean launch,
            int keyCode, BroadcastReceiver cleanUpReceiver,  boolean mbrIgnore) {

        Intent mediaButtonDownIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent downKe = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN,
                keyCode, 0);
        mediaButtonDownIntent.putExtra(Intent.EXTRA_KEY_EVENT, downKe);
        if (mbrIgnore) mediaButtonDownIntent.putExtra("mbrIgnore", true);

        Intent mediaButtonUpIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent upKe = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP,
                keyCode, 0);
        mediaButtonUpIntent.putExtra(Intent.EXTRA_KEY_EVENT, upKe);
        if (mbrIgnore) mediaButtonUpIntent.putExtra("mbrIgnore", true);

        mediaButtonDownIntent.setComponent(selectedReceiver);
        mediaButtonUpIntent.setComponent(selectedReceiver);

        /* COMMENTED OUT FOR MARKET RELEASE Log.i(TAG, "Forwarding Down and Up intent events to " + selectedReceiver + " Down Intent: "
                + mediaButtonDownIntent + " Down key:" + downKe + " Up Intent: " + mediaButtonUpIntent + " Up key:"
                + upKe); */
        // We start the selected application because some apps broadcast
        // receivers won't do anything with the intents unless the
        // application is open. (This this is only if the app isn't
        // playing music and you want it to play music now)
        // XXX Is that true? recheck..
        // Another reason to launch the app is that if the app does
        // AudioManager#registerMediaButtonEventReceiver
        // on load, and we are unable to tell when this app is playing music,
        // android's default behavior should be correct.
        if (launch) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(
                    selectedReceiver.getPackageName());
            if (launchIntent != null) {
//                context.startActivity(launchIntent);
            	try {
                context.startActivity(launchIntent);
            	} catch (ActivityNotFoundException e) {
            		//Alright. The straightforward approach didn't work.
            		//Try this way instead...
            		startApplication(context, selectedReceiver.getPackageName());
            	}
            
            }
        }

        context.sendOrderedBroadcast(mediaButtonDownIntent, null, cleanUpReceiver, null, Activity.RESULT_OK, null, null);
        context.sendOrderedBroadcast(mediaButtonUpIntent, null, cleanUpReceiver, null, Activity.RESULT_OK, null, null);

    }

    /**
     * Gets the list of available media receivers, optionally filtering out ones
     * the user has indicated should be hidden in preferences.
     * 
     * @param packageManager
     *            The {@code PackageManager} used to retrieve media button
     *            receivers.
     * 
     * @param filterHidden
     *            Whether user-hidden media receivers should be shown.
     * @return The list of {@code ResolveInfo} for different media button
     *         receivers.
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
	        }
	        else if (filterHidden) {
	
	            // i have to be more exact than just application name because
	            // the two versions (old and new) of google music
	            // have the same classnames for their intent receivers. I need
	            // to know where their apks live to be able to differentiate.
//	            String name = mediaReceiverResolveInfo.activityInfo.applicationInfo.sourceDir
//	                    + mediaReceiverResolveInfo.activityInfo.name;
	        	
           	 	String name = Utils.getMediaReceiverUniqueID(mediaReceiverResolveInfo, packageManager);

	        	
	            if (hiddenIds.contains(name)) {
                    mediaReceivers.remove(i);
                }
            }
        }

        return mediaReceivers;
    }
    
	/**
     * Gets the list of available voice command apps, optionally filtering out ones
     * the user has indicated should be hidden in preferences.
     * 
     * @param packageManager
     *            The {@code PackageManager} used to retrieve voice command
     *            apps.
     * 
     * @param filterHidden
     *            Whether user-hidden voice command apps should be shown.
     * @return The list of {@code ResolveInfo} for different voice command
     *         apps.
     */
    public static List<ResolveInfo> getVoiceCommandApps(PackageManager packageManager, boolean filterHidden,
            Context context) {
    	
        Intent voiceCommandIntent = new Intent(Intent.ACTION_VOICE_COMMAND);
        List<ResolveInfo> voiceCommandApps = packageManager.queryIntentActivities(voiceCommandIntent,
                PackageManager.GET_INTENT_FILTERS | PackageManager.GET_RESOLVED_FILTER);
        
        String hiddenReceiverIdsString = "";
        List<String> hiddenIds = new ArrayList<String>();
        
        if (filterHidden) {
        	hiddenReceiverIdsString = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.HIDDEN_APPS_KEY, "");
        	hiddenIds = Arrays.asList(hiddenReceiverIdsString.split(","));
        }
        
        for (int i = voiceCommandApps.size() - 1; i >= 0; i--) {
        	
            ResolveInfo voiceCommandAppResolveInfo = voiceCommandApps.get(i);
                        
            if (ReceiverSelector.class.getName().equals(voiceCommandAppResolveInfo.activityInfo.name)) {
            	voiceCommandApps.remove(i);
                if (!filterHidden) {
                	break;
                }
            }
            else if (filterHidden) {
            	
            	// i have to be more exact than just application name because
                // the two versions (old and new) of google music
                // have the same classnames for their intent receivers. I need
                // to know where their apks live to be able to differentiate.
                
            	//String name = voiceCommandAppResolveInfo.activityInfo.applicationInfo.sourceDir
                //        + voiceCommandAppResolveInfo.activityInfo.name;
            	 String name = Utils.getMediaReceiverUniqueID(voiceCommandAppResolveInfo, packageManager);
            	 
            	if (hiddenIds.contains(name)) {
            		voiceCommandApps.remove(i);
            	} 
            }
        }

        return voiceCommandApps;
    }
    
    
    public static List<ResolveInfo> getAllReceivers(PackageManager packageManager, boolean filterHidden,
            boolean performSort, Context context) {
    	
    			String currAudioPlayerPkg = PreferenceManager.getDefaultSharedPreferences(context).getString(
    					Constants.CURRENT_AUDIO_PLAYER_PACKAGE, null);
    			String currAudioPlayerName = PreferenceManager.getDefaultSharedPreferences(context).getString(
    					Constants.CURRENT_AUDIO_PLAYER_NAME, null);
    			
    			List<ResolveInfo> allReceivers = new ArrayList<ResolveInfo>();
    			List<ResolveInfo> mediaReceivers = getMediaReceivers(packageManager, filterHidden, context); 			
    			List<ResolveInfo> voiceCommandApps = getVoiceCommandApps(packageManager, filterHidden, context);
    			  			
    			if (performSort) {
    				
    				ResolveInfo currAudioPlayerRI = null;
        			ResolveInfo highestPriorityVoiceCommandApp = null;
    				
        			if (voiceCommandApps.size() > 0) {
		    			Collections.sort(voiceCommandApps, new Comparator<ResolveInfo>(){
		   				 
		    	            public int compare(ResolveInfo r1, ResolveInfo r2) {
		    	               return r2.priority - r1.priority;
		    	            }
		    	 
		    	        });
	    			
		    			highestPriorityVoiceCommandApp = voiceCommandApps.get(0);
		    			voiceCommandApps.remove(0);
        			}
    			
	    			for (ResolveInfo r1:mediaReceivers) {
						if (r1.activityInfo.packageName.equals(currAudioPlayerPkg) &&
							r1.activityInfo.name.equals(currAudioPlayerName)) {
							currAudioPlayerRI = r1;
							mediaReceivers.remove(currAudioPlayerRI);
							break;
						}
	    			}
	    			
	    			//Join the rest and sort...
	    			mediaReceivers.addAll(voiceCommandApps);
	    			Collections.sort(mediaReceivers, new Comparator<ResolveInfo>(){
		   				 
	    	            public int compare(ResolveInfo r1, ResolveInfo r2) {
	    	               return r2.priority - r1.priority;
	    	            }
	    	 
	    	        });
	    			
	    			
	    			if (currAudioPlayerRI != null) {
	    				allReceivers.add(currAudioPlayerRI);
	    			}
	    			if (highestPriorityVoiceCommandApp != null) {
	    				allReceivers.add(highestPriorityVoiceCommandApp);
	    			}
	    			allReceivers.addAll(mediaReceivers);
	    			
    			}
    			
    			else {
    				allReceivers.addAll(mediaReceivers);
    				allReceivers.addAll(voiceCommandApps);
    			}
    			
    			return allReceivers;    	
    }	
    
    public static String getMediaReceiverUniqueID(ResolveInfo resolveInfo, PackageManager packageManager) {
        String receiverId = resolveInfo.activityInfo.name;
        // Don't think the following is an issue anymore with the latest versions of google play, if it is i'll add back
//        if (GOOGLE_MUSIC_RECEIVER.contains(receiverId)) {
//        // i have to be more exact than just application name because
//        // the two versions (old and new) of google music
//        // have the same classnames for their intent receivers. I need
//        // to know where their apks live to be able to differentiate.
//            receiverId = resolveInfo.activityInfo.applicationInfo.sourceDir + receiverId;
//        }
        return receiverId;
    }

    /**
     * Returns the name of the application of the broadcast receiver specified
     * by {@code resolveInfo}.
     * 
     * @param resolveInfo
     *            The receiver.
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
    
    private static void launchComponent(Context context, String packageName, String name){
        Intent launch_intent = new Intent("android.intent.action.VIEW");
        launch_intent.addCategory("android.intent.category.DEFAULT");
        launch_intent.setComponent(new ComponentName(packageName, name));
        launch_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        context.startActivity(launch_intent);
    }

    public static void startApplication(Context context, String application_name){
        try{
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.addCategory("android.intent.category.DEFAULT");
            
            //intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            List<ResolveInfo> resolveinfo_list = context.getPackageManager().queryIntentActivities(intent, 0);

            for(ResolveInfo info:resolveinfo_list){
                if(info.activityInfo.packageName.equalsIgnoreCase(application_name)){
                    launchComponent(context, info.activityInfo.packageName, info.activityInfo.name);
                    break;
                }
            }
        }
        catch (ActivityNotFoundException e) {
            Toast.makeText(context.getApplicationContext(), "There was a problem loading the application: "+application_name,Toast.LENGTH_SHORT).show();
        }
    }    
}

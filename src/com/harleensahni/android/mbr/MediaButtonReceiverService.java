package com.harleensahni.android.mbr;

import static com.harleensahni.android.mbr.Constants.*;
import static com.harleensahni.android.mbr.Constants.TAG;

import java.util.ArrayList;
import java.util.List;


import android.app.ActivityManager;
import android.app.IntentService;
import android.app.KeyguardManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;


import android.util.Log;
import android.view.KeyEvent;

import com.harleensahni.android.mbr.utils.Preferences;

public class MediaButtonReceiverService extends IntentService {
	

	public MediaButtonReceiverService() {
	      super("MediaButtonReceiverService");
	}

    private boolean checkIsSelectorVisible() {
        ActivityManager activityManager = ((ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE));
        List<RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1);
        if (runningTasks.size() > 0) {
            String className = runningTasks.get(0).topActivity.getClassName();
            if (className.equals(ReceiverSelector.class.getName())
                    || className.equals(ReceiverSelectorLocked.class.getName())) {
                Log.d(TAG, "Media Button Receiver Service: Selector is already open, rebroadcasting for selector only.");

                return true;
            }
        }
        return false;
    }

    private boolean checkIsMusicActive() {
        AudioManager audioManager = ((AudioManager) this.getSystemService(Context.AUDIO_SERVICE));
        return audioManager.isMusicActive();
    }
	
	@Override
	protected void onHandleIntent(Intent intent) {	
		Context context = this;

        boolean isSelectorAlreadyVisible = checkIsSelectorVisible();

        if (isSelectorAlreadyVisible) {
            /* If selector is already visible - forward intent to it */
            Intent receiverSelectorIntent = new Intent(INTENT_ACTION_VIEW_MEDIA_LIST_KEYPRESS);
            receiverSelectorIntent.putExtras(intent);
            context.sendBroadcast(receiverSelectorIntent);
            return;
        }


        int keyCode = intent.getIntExtra(INTENT_KEY_CODE, 0);

        Log.i(TAG, "MediaButtonReceiverService: handling legitimate media key code: " + keyCode);


        //SCENARIO 1: Music is currently playing. 
        if (checkIsMusicActive()) {

            String lastMediaButtonReceiverName = Preferences.getLastMediaButtonReceiver();
            if (lastMediaButtonReceiverName == null) {

                Log.d(TAG, "Last media button receiver stored in preferences is null");

                // Try to best guess who is playing the music based off
                // of
                // running foreground services.

                // XXX Move stuff like receivers to service so we can
                // cache
                // it. Doing too much stuff here
                List<ResolveInfo> receivers = Utils.getMediaReceivers(context.getPackageManager(), false, null);

                // Remove our app from the list so users can't select
                // it.
                if (receivers != null) {
                    ActivityManager activityManager = ((ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE));
                    List<RunningServiceInfo> runningServices = activityManager
                            .getRunningServices(Integer.MAX_VALUE);
               

                    // Only need to look at services that are foreground
                    // and started            
                    
                    List<RunningServiceInfo> candidateServices = new ArrayList<ActivityManager.RunningServiceInfo>();
                    for (RunningServiceInfo runningService : runningServices) {
                        if (runningService.started && runningService.foreground) {
                            candidateServices.add(runningService);
                        }
                    }

                    boolean matched = false;
                    
                    for (ResolveInfo resolveInfo : receivers) {
                        
                        // Find any service that's package matches that
                        // of a receivers.
                        for (RunningServiceInfo candidateService : candidateServices) {

                            if (candidateService.foreground &&
                            	candidateService.started
                                && resolveInfo.activityInfo.packageName.equals(candidateService.service
                                            .getPackageName())) {
                            	
                            	//We found a match...

                                //Set the current audio player name/package
                                Preferences.getPreferences().edit().putString(CURRENT_AUDIO_PLAYER_NAME, resolveInfo.activityInfo.name)
                                                  .putString(CURRENT_AUDIO_PLAYER_PACKAGE, resolveInfo.activityInfo.packageName)
                                                  .commit();

                                String packageName = resolveInfo.activityInfo.packageName;
                                String activityName = resolveInfo.activityInfo.name;

                                Utils.forwardKeyCodeToComponent(context,
                                        new ComponentName(packageName,
                                                activityName), false, keyCode, null);
                                    

                             
                                matched = true;
                                Log.i(TAG, "Media Button Receiver Service: Music playing and passed on event : "
                                        + keyCode + " to " + resolveInfo.activityInfo.name);
                                break;
                            }
                        }
                        if (matched) {
                            // TODO Need to handle case with multiple
                            // matches, maybe by showing selector
                            break;
                        }
                    }
                    
                    if (!matched) {
                        if (Preferences.getPreferences().getBoolean(CONSERVATIVE_PREF_KEY, false)) {
                            //DAS - Don't set ANY CURRENT_AUDIO_PLAYER value. This means we'll
                        	//default to the Voice Command module, which isn't ideal, but it's
                        	//better than nothing.
                        	
                        	//If there's a previously set 'current audio player'
                        	//property, we'll use that in the selector.
                            
                        	Log.i(TAG, "Media Button Receiver Service: No Receivers found playing music. Intent broadcast will be aborted.");

                            showSelector(context, intent);


                        } else {
                            Log.i(TAG, "Media Button Receiver Service: No Receivers found playing music. Intent will use regular priorities.");
                            //DAS - Since we moved this code to a service, that means we have to resend 
                            //the ordered broadcast; so as to avoid an endless loop, we need to make
                            //sure we set a flag that tells our broadcast receiver to ignore this intent.

                            Utils.forwardKeyCodeToComponent(context, null, false, keyCode, null, true);

                        }
                    }
                }
                return;
                
            } else {
            	//SCENARIO 2: Audio is playing AND code is for ICS...

                	
                ComponentName cn = ComponentName.unflattenFromString(lastMediaButtonReceiverName);

                //Set the current audio player name/package
                Preferences.getPreferences().edit().putString(CURRENT_AUDIO_PLAYER_NAME, cn.getShortClassName())
                                  .putString(CURRENT_AUDIO_PLAYER_PACKAGE, cn.getPackageName())
                                  .commit();

                Utils.forwardKeyCodeToComponent(context,
                        ComponentName.unflattenFromString(lastMediaButtonReceiverName), false, keyCode, null);

                return;
            }
        }

        //SCENARIO 3: No music is playing. A music player might be paused (in which case, we'll
        //figure it out from the properties we previously set) or, if we don't have any info about
        //a music player, we'll assume voice commands.
        

        List<ResolveInfo> receivers = Utils.getAllReceivers(context.getPackageManager(), true, false, context);
        if (receivers.size() == 1) {
            // Not using last last_media_button_receiver
            // since we want this feature to work just as
            // well on Android version < 4.0
            ResolveInfo resolveInfo = receivers.get(0);
            Utils.forwardKeyCodeToComponent(context, new ComponentName(
                    resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name), false,
                    keyCode, null);
        } else {
            showSelector(context, intent);
        }

	}
	
	 /**
     * Shows the selector dialog that allows the user to decide which music
     * player should receiver the media button press intent.
     * 
     * @param context
     *            The context.
     * @param intent
     *            The intent to forward.
     */
    private void showSelector(Context context, Intent intent) {
        KeyguardManager manager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = manager.inKeyguardRestrictedInputMode();

        Intent showForwardView = new Intent(INTENT_ACTION_VIEW_MEDIA_BUTTON_LIST);
        showForwardView.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        showForwardView.putExtras(intent);
        showForwardView.setClassName(context,
                locked ? ReceiverSelectorLocked.class.getName() : ReceiverSelector.class.getName());


        if (locked) {

            // XXX See if this actually makes a difference, might
            // not be needed if we move more things to onCreate?
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            // acquire temp wake lock
            WakeLock wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
            wakeLock.setReferenceCounted(false);

            // Our app better display within 3 seconds or we have
            // bigger issues.
            wakeLock.acquire(Constants.WAKE_TIME);
        }
        context.startActivity(showForwardView);
    }

}

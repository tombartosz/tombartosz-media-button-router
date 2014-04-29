package com.harleensahni.android.mbr;

import static com.harleensahni.android.mbr.Constants.TAG;

import java.util.ArrayList;
import java.util.List;

import com.harleensahni.android.mbr.receivers.MediaButtonReceiver;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.KeyguardManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

public class MediaButtonReceiverService extends IntentService {

	public MediaButtonReceiverService() {
	      super("MediaButtonReceiverService");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		
		
		
		Context context = this;
		ActivityManager activityManager = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
		
        // Try to figure out if our selector is currently open; if so,
		// rebroadcast the keypress to it.
		
		//DAS NOTE: This only runs when the selector is actually on-screen. So this
		//code is fine.
		
        List<RunningTaskInfo> runningTasks = activityManager.getRunningTasks(1);
        if (runningTasks.size() > 0) {
            String className = runningTasks.get(0).topActivity.getClassName();
            if (className.equals(ReceiverSelector.class.getName())
                    || className.equals(ReceiverSelectorLocked.class.getName())) {
                Log.d(TAG, "Media Button Receiver Service: Selector is already open, rebroadcasting for selector only.");
                Intent receiver_selector_intent = new Intent(Constants.INTENT_ACTION_VIEW_MEDIA_LIST_KEYPRESS);
                receiver_selector_intent.putExtras(intent);
                context.sendBroadcast(receiver_selector_intent);
                return;
            }
        }
        
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);    	
		KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
        int keyCode = Utils.getAdjustedKeyCode(keyEvent);

        // TODO Handle the case where there is only 0 or 1 media receivers
        // besides ourself by disabling our media receiver
        
        Log.i(TAG, "Media Button Receiver Service: handling legitimate media key event: " + keyEvent);

        AudioManager audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
        
        //SCENARIO 1: Music is currently playing. 
        if (audioManager.isMusicActive()) {
        	
            String last_media_button_receiver = preferences.getString(Constants.LAST_MEDIA_BUTTON_RECEIVER,
                    null);

            if (last_media_button_receiver == null) {

                // XXX Need to improve this behavior, somethings doesn't
                // work. For instance, if you select "Listen" App, and
                // then
                // hit next,
                // the built in music app handles it because it has a
                // higher
                // priority. If we could change priorities on app
                // selection
                // and have it stick,
                // would probably be good enough to handle this.
                // One thing to do would be to add specific classes that
                // check for each knowhn app if our generic way doesn't
                // work
                // well for them
                Log.d(TAG, "Media Button Receiver Service: may pass on event because music is already playing: "
                        + keyEvent);

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
                            	
                                if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                	
                                	//Set the current audio player name/package
                                	preferences.edit().putString(Constants.CURRENT_AUDIO_PLAYER_NAME, resolveInfo.activityInfo.name)
    								  				  .putString(Constants.CURRENT_AUDIO_PLAYER_PACKAGE, resolveInfo.activityInfo.packageName)
    								  				  .commit();
                                	
                                	Utils.forwardKeyCodeToComponent(context,
                                            new ComponentName(resolveInfo.activityInfo.packageName,
                                                    resolveInfo.activityInfo.name), false, keyCode, null);
                                    
                                }
                             
                                matched = true;
                                Log.i(TAG, "Media Button Receiver Service: Music playing and passed on event : "
                                        + keyEvent + " to " + resolveInfo.activityInfo.name);
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
                        if (preferences.getBoolean(Constants.CONSERVATIVE_PREF_KEY, false)) {
                            //DAS - Don't set ANY CURRENT_AUDIO_PLAYER value. This means we'll
                        	//default to the Voice Command module, which isn't ideal, but it's
                        	//better than nothing.
                        	
                        	//If there's a previously set 'current audio player'
                        	//property, we'll use that in the selector.
                            
                        	Log.i(TAG, "Media Button Receiver Service: No Receivers found playing music. Intent broadcast will be aborted.");
                            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                showSelector(context, intent, keyEvent);
                            }

                        } else {
                            Log.i(TAG, "Media Button Receiver Service: No Receivers found playing music. Intent will use regular priorities.");
                            //DAS - Since we moved this code to a service, that means we have to resend 
                            //the ordered broadcast; so as to avoid an endless loop, we need to make
                            //sure we set a flag that tells our broadcast receiver to ignore this intent.
                            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                            	Utils.forwardKeyCodeToComponent(context, null, false, keyCode, null, true);
                            }
                        }
                    }
                }
                return;
                
            } else {
            	//SCENARIO 2: Audio is playing AND code is for ICS...
                if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                	
                	ComponentName cn = ComponentName.unflattenFromString(last_media_button_receiver);
                		
                   	//Set the current audio player name/package
                	preferences.edit().putString(Constants.CURRENT_AUDIO_PLAYER_NAME, cn.getShortClassName())
					  				  .putString(Constants.CURRENT_AUDIO_PLAYER_PACKAGE, cn.getPackageName())
					  				  .commit();
                	
					Utils.forwardKeyCodeToComponent(context,
							ComponentName.unflattenFromString(last_media_button_receiver), false, keyCode, null);
				}
                return;
            }
        }

        //SCENARIO 3: No music is playing. A music player might be paused (in which case, we'll
        //figure it out from the properties we previously set) or, if we don't have any info about
        //a music player, we'll assume voice commands.
        
        if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
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
                showSelector(context, intent, keyEvent);
            }
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
     * @param keyEvent
     *            The key event
     */
    private void showSelector(Context context, Intent intent, KeyEvent keyEvent) {
        KeyguardManager manager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = manager.inKeyguardRestrictedInputMode();

        Intent showForwardView = new Intent(Constants.INTENT_ACTION_VIEW_MEDIA_BUTTON_LIST);
        showForwardView.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        showForwardView.putExtras(intent);
        showForwardView.setClassName(context,
                locked ? ReceiverSelectorLocked.class.getName() : ReceiverSelector.class.getName());

        Log.i(TAG, "Media Button Receiver Service: starting selector activity for keyevent: " + keyEvent);

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
            wakeLock.acquire(3000);
        }
        context.startActivity(showForwardView);
    }

}

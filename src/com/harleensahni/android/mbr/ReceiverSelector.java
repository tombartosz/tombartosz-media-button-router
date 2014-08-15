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

import static com.harleensahni.android.mbr.Constants.TAG;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.harleensahni.android.mbr.data.Receiver;
import com.harleensahni.android.mbr.receivers.MediaButtonReceiver;
import com.harleensahni.android.mbr.utils.AndroidAppsUtils;
import com.harleensahni.android.mbr.utils.SimpleListener;
import com.harleensahni.android.mbr.utils.TTS;
import com.harleensahni.android.mbr.utils.Timer;

/**
 * Allows the user to choose which media receiver will handle a media button
 * press. Can be navigated via touch screen or media button keys. Provides voice
 * feedback.
 * 
 * @author Harleen Sahni
 */
public class ReceiverSelector extends ListActivity implements OnInitListener, AudioManager.OnAudioFocusChangeListener {

    private class SweepBroadcastReceiver extends BroadcastReceiver {
        String name;

        public SweepBroadcastReceiver(String name) {
            this.name = name;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
        }
    }

    /**
     * Temporary storage for the Notification stream volume.
     */
    private static int notificationStreamVolume;

    /**
     * Key used to store and retrieve last selected receiver.
     */
    private static final String SELECTION_KEY = "btButtonSelection";


    /**
     * The media button event that {@link MediaButtonReceiver} captured, and
     * that we will be forwarding to a music player's {@code BroadcastReceiver}
     * on selection.
     */
    private KeyEvent trappedKeyEvent;

    /**
     * The {@code BroadcastReceiver}'s registered in the system for *
     * {@link Intent.ACTION_MEDIA_BUTTON}.
     */
    private List<Receiver> allReceivers;

    /** The intent filter for registering our local {@code BroadcastReceiver}. */
    private IntentFilter uiIntentFilter;

    
    /**
     * The receiver currently selected by bluetooth next/prev navigation. We
     * track this ourselves because there isn't persisted selection w/ touch
     * screen interfaces.
     */
    private int btButtonSelection;

    /**
     * Whether we've done the start up announcement to the user using the text
     * to speech. Tracked so we don't repeat ourselves on orientation change.
     */
    private boolean announced;

    /**
     * The power manager used to wake the device with a wake lock so that we can
     * handle input. Allows us to have a regular activity life cycle when media
     * buttons are pressed when and the screen is off.
     */
    private PowerManager powerManager;

    /**
     * Used to wake up screen so that we can navigate our UI and select an app
     * to handle media button presses when display is off.
     */
    private WakeLock wakeLock;

    /**
     * Whether we've requested audio focus.
     */
    private boolean audioFocus;
    
    private Timer timer;
    private TTS tts;
    
    /** The header */
    private TextView header;

    /** The intro dialog. May be null if no dialog is being shown. */
    private AlertDialog introDialog;

    private boolean eulaAcceptedAlready;

    /**
     * Local broadcast receiver that allows us to handle media button events for
     * navigation inside the activity.
     */
    private BroadcastReceiver uiMediaReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
        	
        	boolean usingOneButton = preferences.getBoolean(Constants.ONE_BUTTON_MODE_PREF_KEY, false);
       	
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())
                    || Constants.INTENT_ACTION_VIEW_MEDIA_LIST_KEYPRESS.equals(intent.getAction())) {
                KeyEvent navigationKeyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
                int keyCode = navigationKeyEvent.getKeyCode();
                if (Utils.isMediaButton(keyCode)) {
                    if (navigationKeyEvent.getAction() == KeyEvent.ACTION_UP) {
						if (!usingOneButton) {
							switch (Utils
									.getAdjustedKeyCode(navigationKeyEvent)) {
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            moveSelection(1);
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            moveSelection(-1);
                            break;
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        	/*
                        	 * If user wants to confirm by timer, we should not allow
                        	 * to confirm by PLAY/PAUSE. In some bluetooth devices with one button
                        	 * i.e Scala Rider Q3, button function changes when playing music i.e:
                        	 * -> Music is stopped - Button act as PLAY/PAUSE (short click)
                        	 * -> Music is playing - Button act as NEXT (short click), PREV (double click) 
                        	 * or PAUSE (long click).
                        	 * 
                        	 * When button behavior changes - it takes while.
                        	 */
                        	if (preferences.getBoolean(Constants.CONFIRM_ACTION_PREF_KEY, true)) {
                        		select();
                        	}
                            break;
                        case KeyEvent.KEYCODE_MEDIA_STOP:
                            // just cancel
                            finish();
                            break;
                        default:
                            break;
                        }
                    }
						else {
                    		switch (Utils.getAdjustedKeyCode(navigationKeyEvent)) {
                    		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
	                            moveSelection(1);
	                            break;
	                        default:
	                        	break;
                    		}
                    	}
					}
                    if (isOrderedBroadcast()) {
                        abortBroadcast();
                    }
                }

            }

        }
    };

    /** Used to figure out if music is playing and handle audio focus. */
    private AudioManager audioManager;

    /** Preferences. */
    private SharedPreferences preferences;
    
    private AndroidAppsUtils androidAppsUtils;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInit(int status) {
    	
        // text to speech initialized
        // XXX This is where we announce to the user what we're handling. It's
        // not clear that this will always get called. I don't know how else to
        // query if the text to speech is started though.

        // Only announce if we haven't before
        if (!announced && trappedKeyEvent != null) {
            requestAudioFocus();

            String actionText = "";
            switch (Utils.getAdjustedKeyCode(trappedKeyEvent)) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                // This is just play even though the keycode is both play/pause,
                // the app shouldn't handle
                // pause if music is already playing, it should go to whoever is
            	// playing the music.... UNLESS, we're in one button mode.
                actionText = getString(audioManager.isMusicActive() ? R.string.pause_play_speak_text
                        : R.string.play_speak_text);
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                actionText = getString(R.string.next_speak_text);
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                actionText = getString(R.string.previous_speak_text);
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                actionText = getString(R.string.stop_speak_text);
                break;

            }
            String textToSpeak = null;
            if (btButtonSelection >= 0 && btButtonSelection < allReceivers.size()) {
                textToSpeak = String.format(getString(R.string.application_announce_speak_text), actionText,
                        allReceivers.get(btButtonSelection).getName(), getPackageManager());
            } else {
                textToSpeak = String.format(getString(R.string.announce_speak_text), actionText);
            }
            this.tts.say(textToSpeak);
            announced = true;
        }
    }
    
    private void createReceiverList() {
    	
    	final Context context = getApplicationContext();
    	final PackageManager packageManager = getPackageManager();
    	
    	int stringId = context.getApplicationInfo().labelRes;
        String myAppName = context.getString(stringId);
    	
    	androidAppsUtils = new AndroidAppsUtils(context);
    	
    	List<ResolveInfo >allResolveInfo = Utils.getAllReceivers(packageManager, true, true, context);
    	allReceivers = new ArrayList<Receiver>();
    	
    	for (final ResolveInfo resolveInfo : allResolveInfo) {
    		final String name = Utils.getAppName(resolveInfo, packageManager);
    		final Drawable icon = resolveInfo.loadIcon(packageManager);
			Receiver currReceiver = new Receiver(){
				@Override
				public void onSelect(int position) {
					
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ReceiverSelector.this);
	            	String currAudioPlayerName = prefs.getString(Constants.CURRENT_AUDIO_PLAYER_NAME, null);
	            	String currAudioPlayerPkg = prefs.getString(Constants.CURRENT_AUDIO_PLAYER_PACKAGE, null);
	            	
	            	if (!resolveInfo.activityInfo.packageName.equals(currAudioPlayerPkg)) {
	            		
	            		//Remove the current audio player properties if we selected something else.
	            		preferences.edit().remove(Constants.CURRENT_AUDIO_PLAYER_NAME)
	  				  					  .remove(Constants.CURRENT_AUDIO_PLAYER_PACKAGE)
	  				  					  .commit();
	            	}

	                ComponentName selectedReceiver = new ComponentName(resolveInfo.activityInfo.packageName,
	                        resolveInfo.activityInfo.name);
	                Utils.forwardKeyCodeToComponent(ReceiverSelector.this, selectedReceiver, true,
	                        Utils.getAdjustedKeyCode(trappedKeyEvent),
	                        new SweepBroadcastReceiver(selectedReceiver.toString()));
	                // save the last acted on app in case we have no idea who is
	                // playing music so we can make a guess
	            	preferences.edit().putString(Constants.CURRENT_AUDIO_PLAYER_NAME, resolveInfo.activityInfo.name)
	  								  .putString(Constants.CURRENT_AUDIO_PLAYER_PACKAGE, resolveInfo.activityInfo.packageName)
	  								  .commit();

				}
			};
			
			currReceiver.setIcon(icon);
			currReceiver.setName(name);
			
		
			if (!currReceiver.getName().trim().equals(myAppName)) {
				allReceivers.add(currReceiver);
			}

		}
    	
    	// Get running apps to the selector
    	List<Receiver> runningAppsReceivers = androidAppsUtils.getRunningAppsBringToFrontReceivers();
    	
    	//Add only this running apps which are not in configuration
    	for (Receiver nReceiver : runningAppsReceivers) {
    		boolean add = true;
			
    		if (nReceiver.getName().trim().equals(myAppName)) {
    			continue;
    		}
    		
    		for (Receiver aReceiver  : allReceivers) {
				if (aReceiver.getName().trim().equals(nReceiver.getName().trim())) {
					add = false;
					break;
				}
			}
			
			if (add) {
				allReceivers.add(nReceiver);
			}
    	}
    	
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Media Button Selector: On Create Called");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setContentView(R.layout.media_button_list);

        // Show eula
        eulaAcceptedAlready = Eula.show(this);

        uiIntentFilter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        uiIntentFilter.addAction(Constants.INTENT_ACTION_VIEW_MEDIA_LIST_KEYPRESS);
        uiIntentFilter.setPriority(Integer.MAX_VALUE);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        initializeTTS();	

        audioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        btButtonSelection = 0;
//        btButtonSelection = preferences.getInt(SELECTION_KEY, -1);

        createReceiverList();
        
        Receiver cancelReceiver = new Receiver() {
        	@Override
        	public void onSelect(int position) {
        		
        		finish();
     
        	}
        };
        cancelReceiver.setName("Cancel");
        Drawable cancelIcon = getResources().getDrawable(R.drawable.dagobert83_cancel);
        cancelReceiver.setIcon(cancelIcon);
        allReceivers.add(cancelReceiver);

        Boolean lastAnnounced = (Boolean) getLastNonConfigurationInstance();
        if (lastAnnounced != null) {
            announced = lastAnnounced;
        }

      
        setListAdapter(new BaseAdapter() {

            @Override
            public int getCount() {
                return allReceivers.size();
            }

            @Override
            public Object getItem(int position) {
                return allReceivers.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View view = convertView;
                if (view == null) {
                    LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = vi.inflate(R.layout.media_receiver_view, null);
                }

                Receiver receiver = allReceivers.get(position);
                view.findViewById(R.id.receiverSelectionIndicator).setVisibility(
                        btButtonSelection == position ? View.VISIBLE : view.INVISIBLE);

                ImageView imageView = (ImageView) view.findViewById(R.id.receiverAppImage);
                imageView.setImageDrawable(receiver.getIcon());

                TextView textView = (TextView) view.findViewById(R.id.receiverAppName);
                textView.setText(receiver.getName());
                return view;

            }
        });
        header = (TextView) findViewById(R.id.dialogHeader);
    }

	private void initializeTTS() {
		this.tts = new TTS(this, this);
        this.tts.setOnSpeechDone(new SimpleListener() {
			
			@Override
			public void execute() {
				if (timer != null) {
					timer.resetTimeout();
					}				
			}
		});
	}

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.tts.destroy();
        Log.d(TAG, "Media Button Selector: destroyed.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        btButtonSelection = position;
        getListView().invalidateViews();

        forwardToMediaReceiver(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Media Button Selector: onPause");
        Log.d(TAG, "Media Button Selector: unegistered UI receiver");
        unregisterReceiver(uiMediaReceiver);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        this.tts.pause();
        timer.shutDown();
        audioManager.abandonAudioFocus(this);
        preferences.edit().putInt(SELECTION_KEY, btButtonSelection).commit();
    }

    @Override
    protected void onStart() {

        super.onStart();
        Log.d(TAG, "Media Button Selector: On Start called");

        // TODO Originally thought most work should happen onResume and onPause.
        // I don't know if the onResume part is
        // right since you can't actually ever get back to this view, single
        // instance, and not shown in recents. Maybe it's possible if ANOTHER
        // dialog opens in front of ours?
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Media Button Selector: onResume");

        // Check to see if intro has been displayed before
        if (introDialog == null || !introDialog.isShowing()) {
            introDialog = Utils.showIntroifNeccessary(this);
        }
        requestAudioFocus();
        // TODO Clean this up, figure out which things need to be set on the
        // list view and which don't.
        if ((getIntent().getExtras() != null &&
           	 getIntent().getExtras().get(Intent.EXTRA_KEY_EVENT) != null) ||
           	 Intent.ACTION_VOICE_COMMAND.equals(getIntent().getAction())) {
           	
           	if (Intent.ACTION_VOICE_COMMAND.equals(getIntent().getAction())) {
           		
           		trappedKeyEvent = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP,
                           KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
           		
           		
           	}
           	else {
            trappedKeyEvent = (KeyEvent) getIntent().getExtras().get(Intent.EXTRA_KEY_EVENT);
           	}

            Log.i(TAG, "Media Button Selector: handling event: " + trappedKeyEvent + " from intent:" + getIntent()); 

            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            getListView().setClickable(true);
            getListView().setFocusable(true);
            getListView().setFocusableInTouchMode(true);

            String action = "";
            switch (Utils.getAdjustedKeyCode(trappedKeyEvent)) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                action = getString(audioManager.isMusicActive() ? R.string.pausePlay : R.string.play);
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                action = getString(R.string.next);
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                action = getString(R.string.prev);
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                action = getString(R.string.stop);
                break;
            }
            header.setText(String.format(getString(R.string.dialog_header_with_action), action));
            if (btButtonSelection >= 0 && btButtonSelection < allReceivers.size()) {
                // scroll to last selected item
                getListView().setSelection(btButtonSelection);
            }
        } else {
            Log.i(TAG, "Media Button Selector: launched without key event, started with intent: " + getIntent());

            trappedKeyEvent = null;
            getListView().setClickable(false);
            getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
            getListView().setFocusable(false);
            getListView().setFocusableInTouchMode(false);

        }
        Log.d(TAG, "Media Button Selector: Registered UI receiver");
        registerReceiver(uiMediaReceiver, uiIntentFilter);

        // power on device's screen so we can interact with it, otherwise on
        // pause gets called immediately.
        // alternative would be to change all of the selection logic to happen
        // in a service?? don't know if that process life cycle would fit well
        // -- look into
        // added On after release so screen stays on a little longer instead of
        // immediately to try and stop resume pause cycle that sometimes
        // happens.
        wakeLock = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG);
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
        
        int timeoutTime = Integer.valueOf(preferences.getString(Constants.TIMEOUT_KEY, "5"));        
        timer = new Timer(this, timeoutTime, onTimeout());
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        return announced;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // We're not supposed to show a menu since we show as a dialog,
        // according to google's ui guidelines. No other sane place to put this,
        // except maybe
        // a small configure button in the dialog header, but don't want users
        // to hit it by accident when selecting music app.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.selector_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, MediaButtonConfigure.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        // Reset timeout before we finish
        if (timer != null && !timer.isShutdown()) {
            timer.resetTimeout();
        }
    }

    /**
     * Forwards the {@code #trappedKeyEvent} to the receiver at specified
     * position.
     * 
     * @param position
     *            The index of the receiver to select. Must be in bounds.
     */
    private void forwardToMediaReceiver(int position) {
        Receiver receiver = allReceivers.get(position);
        if (receiver != null) {
            if (trappedKeyEvent != null) {

            	receiver.onSelect(position);
            	
            	finish();
            	
            	
            	/*SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            	String currAudioPlayerName = prefs.getString(Constants.CURRENT_AUDIO_PLAYER_NAME, null);
            	String currAudioPlayerPkg = prefs.getString(Constants.CURRENT_AUDIO_PLAYER_PACKAGE, null);
            	
            	if (!resolveInfo.activityInfo.packageName.equals(currAudioPlayerPkg)) {
            		
            		//Remove the current audio player properties if we selected something else.
            		preferences.edit().remove(Constants.CURRENT_AUDIO_PLAYER_NAME)
  				  					  .remove(Constants.CURRENT_AUDIO_PLAYER_PACKAGE)
  				  					  .commit();
            	}

                ComponentName selectedReceiver = new ComponentName(resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name);
                Utils.forwardKeyCodeToComponent(this, selectedReceiver, true,
                        Utils.getAdjustedKeyCode(trappedKeyEvent),
                        new SweepBroadcastReceiver(selectedReceiver.toString()));
                // save the last acted on app in case we have no idea who is
                // playing music so we can make a guess
            	preferences.edit().putString(Constants.CURRENT_AUDIO_PLAYER_NAME, resolveInfo.activityInfo.name)
  								  .putString(Constants.CURRENT_AUDIO_PLAYER_PACKAGE, resolveInfo.activityInfo.packageName)
  								  .commit();
                finish();*/
            }
        }
    }

    /**
     * Moves selection by the amount specified in the list. If we're already at
     * the last item and we're moving forward, wraps to the first item. If we're
     * already at the first item, and we're moving backwards, wraps to the last
     * item.
     * 
     * @param amount
     *            The amount to move, may be positive or negative.
     */
    private void moveSelection(int amount) {
    	if (timer != null) {
    		timer.stopTimer();
    	}

        btButtonSelection += amount;

        if (btButtonSelection >= allReceivers.size()) {
            // wrap
            btButtonSelection = 0;
        } else if (btButtonSelection < 0) {
            // wrap
            btButtonSelection = allReceivers.size() - 1;
        }

        // May not highlight, but will scroll to item
        getListView().invalidateViews();
        getListView().setSelection(btButtonSelection);

        String textToSay = allReceivers.get(btButtonSelection).getName();
        
       
        this.tts.say(textToSay);
        
    }
    


    /**
     * Select the currently selected receiver.
     */
    private void select() {
        if (btButtonSelection == -1 || btButtonSelection >= allReceivers.size()) {
            finish();
        } else {
            forwardToMediaReceiver(btButtonSelection);
        }

    }

    /**
     * Takes appropriate action to notify user and dismiss activity on timeout.
     */
	private Runnable onTimeout() {
		return new Runnable() {

			@Override
			public void run() {
				Log.d(TAG,
						"Media Button Selector: Timed out waiting for user interaction, finishing activity");
				final MediaPlayer timeoutPlayer = MediaPlayer.create(
						ReceiverSelector.this, R.raw.dismiss);
				timeoutPlayer.start();
				// not having an on error listener results in on completion
				// listener
				// being called anyway
				timeoutPlayer
						.setOnCompletionListener(new OnCompletionListener() {

							public void onCompletion(MediaPlayer mp) {
								timeoutPlayer.release();
							}
						});

				// If the user has set their preference not to confirm actions,
				// we'll
				// just forward automatically to whoever was last selected. If
				// no one is
				// selected, it just acts like finish anyway.
				if (preferences.getBoolean(Constants.CONFIRM_ACTION_PREF_KEY,
						true)) {
					finish();
				} else {
					select();
				}

			}
		};
	}

    /**
     * Requests audio focus if necessary.
     */
    private void requestAudioFocus() {
        if (!audioFocus) {
        	Log.d(TAG, "Request audio focus");
        	if (audioManager.isBluetoothScoOn()) {
        		audioManager.setBluetoothScoOn(false);
        	}
            audioFocus = audioManager.requestAudioFocus(this, AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN ||
        	focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT ||
        	focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) {

        		notificationStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        		audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 
        									 audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION),
        									 AudioManager.FLAG_SHOW_UI);
        }
        else {
        	audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 
					 					 notificationStreamVolume,
					 					 AudioManager.FLAG_SHOW_UI);
        }

    }
}

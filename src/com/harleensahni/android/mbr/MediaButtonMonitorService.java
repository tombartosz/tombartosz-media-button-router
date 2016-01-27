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

import static com.harleensahni.android.mbr.Constants.TAG;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;

import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import com.harleensahni.android.mbr.receivers.MediaButtonReceiver;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Monitors when the media button receiver registered with the audio manager changes, and sets
 * it back to media button router's receiver. Allows media button router to correctly intercept
 * all media button presses.
 *
 * @author Peter Haight
 */
public class MediaButtonMonitorService extends Service {

    /**
     * Prevents executing thread for more than once
     */
    public ComponentName mComponentName;
    public AudioManager mAudioManager;

    private static MediaButtonMonitorService _self;

    public static MediaButtonMonitorService getService() {
        return _self;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        Log.d(TAG, "onCreate()");
        _self = this;
        mComponentName = new ComponentName(getPackageName(), MediaButtonReceiver.class.getName());
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand(" + intent + ", " + flags + ", " + startId);
        registerMultimediaEventReceiver();


        return START_STICKY;
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy() called. Unregistering media button receiver.");
        mAudioManager.unregisterMediaButtonEventReceiver(mComponentName);
    }

    public void registerMultimediaEventReceiver() {
        registerAsReceiver(this);
    }

    public static int cnt = 0;

    public static void registerAsReceiver(final Context context) {
        /*
         * This registers MultimediaEventReciver to be sole
         * bluetooth multimedia button receiver
         */

        Log.d(TAG, "Registering Media Button Router to be Multimedia event receiver...");

        AudioManager manager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        ComponentName cn = new ComponentName(
                context.getPackageName(),
                MediaButtonReceiver.class.getName());

        manager.registerMediaButtonEventReceiver(cn);


        final MediaSession session = new MediaSession(context.getApplicationContext(), "MBR_MS_TAG");
        session.setCallback(new MediaSession.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                Log.i("TAG", "GOT EVENT");
                new MediaButtonReceiver().onReceive(context.getApplicationContext(), mediaButtonIntent);
                return true;
            }
        });



        int flags = MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS;

        session.setFlags(flags); // |
        //MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS | 1 << 16);

        session.setActive(true);

        PlaybackState state = new PlaybackState.Builder()
                .setActions(
                        PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE |
                                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID | PlaybackState.ACTION_PAUSE |
                                PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackState.STATE_PLAYING, 0, 0, SystemClock.elapsedRealtime())
                .build();

        session.setPlaybackState(state);

        Log.d(TAG, "Registering Media Button Router to be Multimedia event receiver finished. " + (cnt++));


    }

}

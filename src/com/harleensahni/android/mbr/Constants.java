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

/**
 * Constants.
 * 
 * @author Harleen Sahni
 */
public class Constants {

    private Constants() {
        // Intentionally blank
    }

    public static final String TAG = "MediaButtonRouter";

    public static final String INTENT_ACTION_VIEW_MEDIA_BUTTON_LIST = "com.harleensahni.android.VIEW_MEDIA_LIST";
    public static final String INTENT_ACTION_VIEW_MEDIA_LIST_KEYPRESS = "com.harleensahni.android.VIEW_MEDIA_LIST_KEYPRESS";
    public static final String INTENT_KEY_CODE = "com.harleensahni.android.mbr.KEY_CODE";
    public static final String ENABLED_PREF_KEY = "enable_receiver";
    public static final String INTRO_SHOWN_KEY = "intro_shown";
    public static final String TIMEOUT_KEY = "timeout";
    public static final String CONSERVATIVE_PREF_KEY = "conservative";
    public static final String CONFIRM_ACTION_PREF_KEY = "confirm_action";
    public static final String HIDDEN_APPS_KEY = "hidden_apps";
    public static final String LAST_MEDIA_BUTTON_RECEIVER = "last_media_button_receiver";
    public static final String DISABLE_TTS = "disable_tts";
    
    public static final String INTENT_MEDIA_BUTTON_RECEIVER_SERVICE = "com.harleensahni.android.MEDIA_BUTTON_RECEIVER_SERVICE";
    public static final String ONE_BUTTON_MODE_PREF_KEY = "one_button_mode";
    public static final String CURRENT_AUDIO_PLAYER_NAME = "current_audio_player_name";
    public static final String CURRENT_AUDIO_PLAYER_PACKAGE = "current_audio_player_pkg";
    public static final String CURRENT_AUDIO_PLAYER_IS_ACTIVE = "current_audio_player_is_active";
    public static final String SELECTION_ACTED_KEY = "btButtonSelectionActed";

    public static final int WAKE_TIME = 20000;


}

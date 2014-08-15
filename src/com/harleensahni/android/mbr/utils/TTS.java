package com.harleensahni.android.mbr.utils;

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;

import com.harleensahni.android.mbr.Constants;

public class TTS {

	private final TextToSpeech textToSpeech;
	private SimpleListener onSpeechDone;

	private final String KEY_PARAM_UTTERANCE_ID;

	public TTS(Context context, OnInitListener listener) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		boolean ttsDisabled = preferences.getBoolean(Constants.DISABLE_TTS, false);

		KEY_PARAM_UTTERANCE_ID = context.getPackageName();

		if (!ttsDisabled) {
			textToSpeech = new TextToSpeech(context, listener);
			textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {

				@Override
				public void onStart(String utteranceId) {
					/* nothing */
				}

				@Override
				public void onError(String utteranceId) {
					/* nothing */
				}

				@Override
				public void onDone(String utteranceId) {
					execOnSpeechDone();
				}
			});

		} else {
			textToSpeech = null;
		}

	}

	public void say(String whatToSay) {
		if (textToSpeech != null) {
			HashMap<String, String> ttsParams = new HashMap<String, String>();
			ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, KEY_PARAM_UTTERANCE_ID);

			textToSpeech.speak(whatToSay, TextToSpeech.QUEUE_FLUSH, ttsParams);
		} else {
			execOnSpeechDone();
		}
	}
	
	public void destroy() {
		if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
	}
	
	public void pause() {
		if (textToSpeech != null) {
			textToSpeech.stop();
		}
	}

	private void execOnSpeechDone() {
		if (onSpeechDone != null) {
			onSpeechDone.execute();
		}
	}

	public SimpleListener getOnSpeechDone() {
		return onSpeechDone;
	}

	public void setOnSpeechDone(SimpleListener onSpeechDone) {
		this.onSpeechDone = onSpeechDone;
	}

}

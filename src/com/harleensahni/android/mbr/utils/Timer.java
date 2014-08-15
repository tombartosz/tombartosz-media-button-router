package com.harleensahni.android.mbr.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.util.Log;

public class Timer {

	private static final String TAG = "TIMER";

	/**
	 * Number of seconds to wait before timing out and just cancelling.
	 */
	private final int timeoutTime;

	/**
	 * ScheduledExecutorService used to time out and close activity if the user
	 * doesn't make a selection within certain amount of time. Resets on user
	 * interaction.
	 */
	private final ScheduledExecutorService timeoutExecutor;

	/**
	 * ScheduledFuture of timeout.
	 */
	private ScheduledFuture<?> timeoutScheduledFuture;

	private final Activity activity;

	private final Runnable onTimeout;

	public Timer(Activity activity, int timeoutTime, Runnable onTimeout) {
		Log.d(TAG, "Timer constructor");
		this.activity = activity;
		this.timeoutTime = timeoutTime;
		this.onTimeout = onTimeout;

		this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
	}

	/**
	 * Stops timer
	 */
	public void stopTimer() {
		Log.d(TAG, "stop timer");
		if (timeoutScheduledFuture != null) {
			timeoutScheduledFuture.cancel(false);
		}
	}

	/**
	 * Resets the timeout before the application is automatically dismissed.
	 */
	public void resetTimeout() {

		Log.d(TAG, "reset timer");

		if (timeoutScheduledFuture != null) {
			timeoutScheduledFuture.cancel(false);
		}

		timeoutScheduledFuture = timeoutExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				activity.runOnUiThread(Timer.this.onTimeout);

			}
		}, timeoutTime, TimeUnit.SECONDS);

	}
	
	public boolean isShutdown() {
		return timeoutExecutor.isShutdown();
	}

	public void shutDown() {
		stopTimer();
		timeoutExecutor.shutdownNow();
	}
}

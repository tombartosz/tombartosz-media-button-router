package com.harleensahni.android.mbr.receivers;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class CallReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
				TelephonyManager.EXTRA_STATE_RINGING)) {

			// Phone number
			String incomingNumber = intent
					.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

			// Ringing state
			// This code will execute when the phone has an incoming call
		} else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
				TelephonyManager.EXTRA_STATE_IDLE)
		/*
		 * || intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
		 * TelephonyManager.EXTRA_STATE_OFFHOOK)
		 */) {

			// Disable bluetooth
			BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
					.getDefaultAdapter();
			if (mBluetoothAdapter.isEnabled()) {
				mBluetoothAdapter.disable();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mBluetoothAdapter.enable();
			final Toast toast = Toast.makeText(context,
					"This message will disappear     in half second",
					Toast.LENGTH_SHORT);
			toast.show();

			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					toast.cancel();
				}
			}, 500);
			// This code will execute when the call is answered or disconnected
		}

	}
}
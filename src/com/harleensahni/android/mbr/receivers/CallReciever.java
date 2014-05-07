package com.harleensahni.android.mbr.receivers;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class CallReciever extends BroadcastReceiver {

	private static boolean wasBluetoothEnabledDuringCall = false;

	@Override
	public void onReceive(Context context, Intent intent) {

		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();

		if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
				TelephonyManager.EXTRA_STATE_OFFHOOK)) {
			CallReciever.wasBluetoothEnabledDuringCall = mBluetoothAdapter
					.isEnabled();
			return;
		}

		if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(
				TelephonyManager.EXTRA_STATE_IDLE)) {

			// Disable and enable bluetooth bluetooth

			if (CallReciever.wasBluetoothEnabledDuringCall) {
				if (mBluetoothAdapter.isEnabled()) {
					mBluetoothAdapter.disable();
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mBluetoothAdapter.enable();
			}
			final Toast toast = Toast.makeText(context,
					"Restart bluetooth module ...",
					Toast.LENGTH_SHORT);
			toast.show();

			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					toast.cancel();
				}
			}, 1000);
		
		}

	}
}
package com.harleensahni.android.mbr;

import android.bluetooth.BluetoothAdapter;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class MyPhoneStateListener extends PhoneStateListener {

	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		switch (state) {
		// Hangup
		case TelephonyManager.CALL_STATE_IDLE:

			break;
		// Outgoing
		case TelephonyManager.CALL_STATE_OFFHOOK:
			break;
		// Incoming
		case TelephonyManager.CALL_STATE_RINGING:
			break;
		}
	}
}
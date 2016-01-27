package com.harleensahni.android.mbr.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.harleensahni.android.mbr.MediaButtonMonitorService;

import java.util.GregorianCalendar;

public class AlarmReciever extends BroadcastReceiver
{
    public final static String TAG = "MBR_AR";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");

        MediaButtonMonitorService.registerAsReceiver(context);
        scheduleAlarm(context);
    }

    public static void scheduleAlarm(Context context)
    {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Long time = new GregorianCalendar().getTimeInMillis()+1000;

        Intent intentAlarm = new Intent(context, AlarmReciever.class);

        alarmManager.set(AlarmManager.RTC_WAKEUP,time, PendingIntent.getBroadcast(context, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
        Log.i(TAG, "alarm scheduled");

    }


}
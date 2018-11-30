package ist.wirelessaccelerometer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class Alarm extends BroadcastReceiver

{
    private String logTag="log_placeholder";
    @Override
    public void onReceive(Context context, Intent intent)
    {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ist:wakelock1");
        wl.acquire();

        // Put here YOUR code.
        Log.d(logTag, "alarm triggered");
        // For example

        wl.release();
    }

    /*
    Function to set a repeating alarm based on the timebinsize of the connected device.
    The function will assume it receives time bin size in milliseconds
     */
    public void setAlarm(Context context,int timeBinSize)
    {
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, Alarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        Log.d(logTag, "alarm set");
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), timeBinSize, pi);
    }

    public void cancelAlarm(Context context)
    {
        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
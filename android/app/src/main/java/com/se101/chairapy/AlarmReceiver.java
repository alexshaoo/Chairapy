package com.se101.chairapy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "notifyMe")
                .setSmallIcon(R.drawable.chair)
                .setContentTitle("Reminder")
                .setContentText("go stretch or eat or smth")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // call notify - unique ID 1
        notificationManager.notify(1, builder.build());

        // generate text
        Toast.makeText(context,"text",Toast.LENGTH_SHORT).show();
    }
}
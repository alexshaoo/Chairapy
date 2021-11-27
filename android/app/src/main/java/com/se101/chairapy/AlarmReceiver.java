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
                .setContentTitle("NOTIFICATION")
                .setContentText("THIS IS A NOTIFICATION")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // create notification channel


        // call notify - unique ID 200

        notificationManager.notify(200, builder.build());

        // generate text
        Toast.makeText(context,"text",Toast.LENGTH_SHORT).show();
    }
}

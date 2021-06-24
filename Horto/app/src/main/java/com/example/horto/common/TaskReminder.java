package com.example.horto.common;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.horto.PlantListActivity;
import com.example.horto.R;

// Act upon receiving notification broadcast
public class TaskReminder extends BroadcastReceiver {

    // Notification Channel name
    public static final String CHANNEL_ID = "task notify";


    @Override
    public void onReceive(Context context, Intent intent) {
        //  Creation of dialog
        // Get the title and description of task
        String title = intent.getStringExtra("taskTitle");
        String desc = intent.getStringExtra("taskDesc");

        //  Define notification properties
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(desc)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Notification manager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(1, builder.build());
    }
}

package com.example.horto;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.horto.common.Garden;
import com.example.horto.common.TaskReminder;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmQuery;


// Activity that runs prerequisites for all other classes.
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_Horto);
        super.onCreate(savedInstanceState);

        // Initialise MongoDB Realm
        Realm.init(this);
        Log.v("REALM_CONTEXT_INTIATED", "Started the realm context");

        // Create the notification channel
        createNotificationChannel();

        // Create Realm Configuration
        RealmConfiguration config = new RealmConfiguration.Builder()
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .name("Garden")
                .build();
        Realm realm = Realm.getInstance(config);
        Log.v("REALM_INITIATE", "Successfully opened a realm at: " + realm.getPath());

        RealmQuery<Garden> gardenTest = realm.where(Garden.class);

        // Open CreateGarden if no garden exists...
        if (gardenTest.count() == 0) {
            Intent intent = new Intent(this, CreateGardenActivity.class);
            startActivityForResult(intent, 0);
        } else {
            // ...Otherwise open HomePage if there is.
            Intent intent = new Intent(this, HomePageActivity.class);
            startActivityForResult(intent, 0);
        }
    }

    // Initialise the notification channel.
    private void createNotificationChannel () {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Task Reminders";
            String description = "Reminders about plant tasks";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(TaskReminder.CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}

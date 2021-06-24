package com.example.horto;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.horto.common.Garden;
import com.example.horto.common.Task;
import com.example.horto.common.TaskActionDialog;
import com.example.horto.common.TaskReminder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import org.json.JSONException;
import org.json.JSONObject;


import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

// Homepage for application, launched when garden exists in database.
public class HomePageActivity extends AppCompatActivity implements TaskActionDialog.TaskDialogActionsListener {

    ListView taskWidgetList;
    SimpleAdapter taskWidgetArrayAdapter;
    ArrayList<Task> allTasksArray;

    TextView tempLabel;
    Toolbar toolbar;
    TextView dateLabel;
    TextView noTasksLabel;

    private CharSequence confirmText = "Task Completed: Refresh Page";
    private CharSequence deleteText = "Task Deleted: Refresh Page";
    private int duration = Toast.LENGTH_SHORT;
    Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // AR Screen button
        FloatingActionButton fab = findViewById(R.id.ar_button);
        fab.setOnClickListener(view -> {
            Intent myIntent = new Intent(view.getContext(), ArDesignActivity.class);
            startActivityForResult(myIntent, 0);
        });

        // Plant screen button
        FloatingActionButton fab2 = findViewById(R.id.plant_button);
        fab2.setOnClickListener(view -> {
            Intent myIntent = new Intent(view.getContext(), PlantListActivity.class);
            startActivityForResult(myIntent, 0);
        });

        // Temperature and Date Labels
        tempLabel = findViewById(R.id.tempLabel);
        taskWidgetList = findViewById(R.id.task_list_view);
        dateLabel = findViewById(R.id.dateLabel);
        noTasksLabel = findViewById(R.id.no_tasks_label);
        noTasksLabel.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        RealmConfiguration config = new RealmConfiguration.Builder()
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .name("Garden")
                .build();
        Realm realm = Realm.getInstance(config);
        Log.v("REALM_INITIATE", "Successfully opened a realm at: " + realm.getPath());
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        /*
        London - 2643743
        API Key - 83db5248255a64ca79ef958e57fd4e7a
         */
        String url = "https://api.openweathermap.org/data/2.5/weather?q=London&units=metric&appid=83db5248255a64ca79ef958e57fd4e7a";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, response -> {
                    try {
                        DecimalFormat df = new DecimalFormat("###.#");
                        tempLabel.setText(df.format(response.getJSONObject("main").getDouble("temp")) + "°");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> tempLabel.setText("Error"));
        // Access the RequestQueue through your singleton class.
        queue.add(jsonObjectRequest);
        // Date Label
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM");
        Date date = new Date();
        dateLabel.setText(sdf.format(date));

        // Setup task widget on the home screen.
        // Check for the tasks with the current date and not completed
        LocalDate today = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        RealmQuery<Task> tasksQuery = realm.where(Task.class);
        RealmResults<Task> tasksResults = tasksQuery.sort("dueDate", Sort.DESCENDING).findAll();
        allTasksArray = (ArrayList<Task>) realm.copyFromRealm(tasksResults);
        List<HashMap<String, String>> todayTaskArray = new ArrayList<>();
        for (Task task : allTasksArray) {
            System.out.println(task.getDueDate().toString());
            LocalDate taskDate = task.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalTime taskTime = task.getDueDate().toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
            String taskStatus = task.getStatus();
            System.out.println(taskDate.isEqual(today));
            if (taskDate.isEqual(today) && taskStatus.equals("waiting")) {
                HashMap<String, String> plantTime = new HashMap<>();
                plantTime.put("plant", task.getPlantName());
                plantTime.put("title", task.getTitle());
                plantTime.put("time", taskTime.toString());
                todayTaskArray.add(plantTime);
            }
        }
        if (todayTaskArray.isEmpty()) {
            noTasksLabel.setVisibility(View.VISIBLE);
        } else {
            // Display tasks if array not empty
            taskWidgetArrayAdapter = new SimpleAdapter(this, todayTaskArray, R.layout.single_item_list, new String[]{"plant", "title", "time"},
                    new int[]{R.id.item_task_plant, R.id.item_task_title, R.id.item_task_time});
            taskWidgetList.setAdapter(taskWidgetArrayAdapter);
            noTasksLabel.setVisibility(View.GONE);

            taskWidgetList.setOnItemClickListener((parent, view, position, id) -> {
                TaskActionDialog tad = new TaskActionDialog(allTasksArray.get(position), position);
                tad.show(getSupportFragmentManager(), "Task Action Dialog");
            });
            realm.close();
        }
        ;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // Confirm task button action.
    @Override
    public void confirmTask(Task task, int pos) {
        RealmConfiguration config = new RealmConfiguration.Builder()
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .name("Garden")
                .build();
        Realm realm = Realm.getInstance(config);

        // Update task details
        realm.executeTransaction(realm1 -> {
            Task myTask = realm1.where(Task.class).equalTo("_id", task.get_id()).findFirst();
            myTask.setStatus("completed");
            Date date = new Date();
            myTask.setCompletionDate(date);
        });
        allTasksArray.remove(pos);
        taskWidgetArrayAdapter.notifyDataSetChanged();
        realm.close();

        toast = Toast.makeText(HomePageActivity.this, confirmText, duration);
        toast.show();
    }

    // Delete the task from the database, Called in delete button listener
    @Override
    public void deleteTask(Task task, int pos) {
        RealmConfiguration config = new RealmConfiguration.Builder()
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .name("Garden")
                .build();
        Realm realm = Realm.getInstance(config);

        // Remove task from database
        realm.executeTransaction(realm1 -> {
            RealmResults<Task> results = realm1.where(Task.class).equalTo("_id", task.get_id()).findAll();
            results.deleteFirstFromRealm();
        });
        allTasksArray.remove(pos);
        taskWidgetArrayAdapter.notifyDataSetChanged();
        realm.close();

        toast = Toast.makeText(HomePageActivity.this, deleteText, duration);
        toast.show();
    }
}
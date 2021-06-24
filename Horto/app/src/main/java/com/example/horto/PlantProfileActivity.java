package com.example.horto;


import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.horto.common.Plant;
import com.example.horto.common.Task;
import com.example.horto.common.TaskReminder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;


// Plant profile page.
public class PlantProfileActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private Plant plant = new Plant();
    private Task task = new Task();
    private final String apiToken = "x0mkz8A-MjuYnK5_KZ8blzwgH_HiHhYBzYZu2iBIhw8";

    // Components of profile page
    private Button addTaskButton;
    private Button cancelTaskButton;
    private Button setDateButton;
    private Button setTimeButton;
    private Button confirmTaskButton;
    private FloatingActionButton deletePlantButton;

    private EditText taskTitle;
    private EditText taskDesc;

    private ImageView plantImage;
    private LocalDateTime localDateTime;
    private LocalDate localDate;
    private LocalTime localTime;
    private String getPlantsURL;

    private TextView scienceNameText;
    private TextView yearText;
    private TextView commonText;
    private TextView familyText;

    private CharSequence text = "Plant has been deleted";
    private int duration = Toast.LENGTH_SHORT;
    Toast toast;

    @SuppressLint("CutPasteId")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = this.getApplicationContext();

        String plantName = getIntent().getStringExtra("plantName");
        String plantID = getIntent().getStringExtra("plantID");

        plant.setName(plantName);
        plant.set_id(new ObjectId(plantID));

        String plantNameString = plant.getName().replaceAll("\\s+", "");
        getPlantsURL = "https://trefle.io/api/v1/plants/search?token=" + apiToken + "&q=" + plantNameString;

        System.out.println(getPlantsURL);

        setContentView(R.layout.plant_profile);

        plantImage = findViewById(R.id.plant_image);

        RequestQueue queue = Volley.newRequestQueue(this);
        //  Get plant image URL from API
        JsonObjectRequest getPlantImage = new JsonObjectRequest(Request.Method.GET, getPlantsURL, null, response -> {
            try {
                String imageURL = response.getJSONArray("data").getJSONObject(0).getString("image_url");
                System.out.println(imageURL);
                if (imageURL.equals("null")) {
                    // Placeholder image from - https://www.flaticon.com/free-icon/plant_628283?term=plant&page=1&position=15&page=1&position=15&related_id=628283&origin=search
                    // Credit - Freepik
                    plantImage.setImageResource(R.drawable.placeholder);
                } else {
                    // Attach the image onto the ImageView
                    Glide.with(context)
                            .load(imageURL)
                            .into(plantImage);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                CharSequence text = "Error retrieving plant";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }, error -> {
            CharSequence text = "Error retrieving plant";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        });
        queue.add(getPlantImage);

        // Set Back button
        ImageButton backButton = findViewById(R.id.profile_back_button);
        backButton.setOnClickListener(v -> {
            Intent myIntent = new Intent(v.getContext(), PlantListActivity.class);
            startActivityForResult(myIntent, 0);
        });

        FrameLayout addTaskFragment = findViewById(R.id.add_task_layout);
        addTaskFragment.setVisibility(FrameLayout.GONE);
        addTaskFragment.setAlpha(0f);

        // Delete plant button
        deletePlantButton = findViewById(R.id.delete_plant_button);
        deletePlantButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(PlantProfileActivity.this);

            builder.setPositiveButton("Delete", (dialog, id) -> {
                RealmConfiguration config = new RealmConfiguration.Builder()
                        .allowQueriesOnUiThread(true)
                        .allowWritesOnUiThread(true)
                        .name("Garden")
                        .build();
                Realm realm = Realm.getInstance(config);
                // Delete plant from the database when finished.
                realm.executeTransaction(realm1 -> {
                    RealmResults<Plant> results = realm1.where(Plant.class).equalTo("_id", plant.get_id()).findAll();
                    results.deleteFirstFromRealm();
                });

                toast = Toast.makeText(PlantProfileActivity.this, text, duration);
                toast.show();

                Intent myIntent = new Intent(v.getContext(), PlantListActivity.class);
                startActivityForResult(myIntent, 0);

                PlantProfileActivity.this.finish();
            });
            builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());

            builder.setTitle("Are you sure you want to delete this plant?");
            AlertDialog dialog = builder.create();
            dialog.show();
        });


        // Add a task button
        addTaskButton = findViewById(R.id.addTaskButton);
        addTaskButton.setOnClickListener(v -> {
            addTaskFragment.setVisibility(ConstraintLayout.VISIBLE);
            deletePlantButton.setVisibility(View.INVISIBLE);
            addTaskFragment.animate().alpha(1f).setDuration(200);
            deletePlantButton.animate().alpha(0f).setDuration(200);
            taskTitle.setText("");
            taskDesc.setText("");
        });

        // Cancel a task button
        cancelTaskButton = findViewById(R.id.cancel_task_button);
        cancelTaskButton.setOnClickListener(v -> {
            addTaskFragment.animate().alpha(0f).setDuration(200);
            deletePlantButton.animate().alpha(1f).setDuration(200);
            addTaskFragment.setVisibility(FrameLayout.INVISIBLE);
            deletePlantButton.setVisibility(View.VISIBLE);
        });

        // Set date button.
        setDateButton = findViewById(R.id.set_date_button);
        setDateButton.setOnClickListener(v -> {
            // Date picker dialog
            DatePickerDialog dateDialog = new DatePickerDialog(PlantProfileActivity.this,
                    PlantProfileActivity.this,
                    Calendar.getInstance().get(Calendar.YEAR),
                    Calendar.getInstance().get(Calendar.MONTH),
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
            dateDialog.show();
        });

        // Set time button
        setTimeButton = findViewById(R.id.set_time_button);
        setTimeButton.setOnClickListener(v -> {
            // Time picker dialog
            TimePickerDialog timeDialog = new TimePickerDialog(PlantProfileActivity.this,
                    PlantProfileActivity.this,
                    Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                    Calendar.getInstance().get(Calendar.MINUTE),
                    true);
            timeDialog.show();
        });
        taskTitle = findViewById(R.id.edit_task_name);
        taskDesc = findViewById(R.id.edit_task_desc);

        // Confirm the task button
        confirmTaskButton = findViewById(R.id.confirm_task_button);
        confirmTaskButton.setOnClickListener(v -> {
            System.out.println();
            if (taskTitle.getText().toString().isEmpty() || taskDesc.getText().toString().isEmpty()) {
                CharSequence text = "Please enter a name and description";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            } else {
                // Get component details
                Date date = new Date();
                localDateTime = LocalDateTime.of(localDate, localTime);
                ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
                String taskTitleString = taskTitle.getText().toString();
                String taskDescString = taskDesc.getText().toString();
                task.setTitle(taskTitleString);
                task.setDescription(taskDescString);
                task.setSetDate(date);
                task.setStatus("waiting");
                System.out.println(Date.from(zonedDateTime.toInstant()));
                task.setDueDate(Date.from(zonedDateTime.toInstant()));
                task.setPlantName(plant.getName());
                // Write the new task to the database.
                RealmConfiguration config = new RealmConfiguration.Builder()
                        .allowQueriesOnUiThread(true)
                        .allowWritesOnUiThread(true)
                        .name("Garden")
                        .build();
                Realm realm = Realm.getInstance(config);
                realm.executeTransaction(transactionRealm -> {
                    transactionRealm.insert(task);
                });
                // Close connection
                realm.close();

                // Create new alarm manager.
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                Intent intent = new Intent(PlantProfileActivity.this, TaskReminder.class);
                // Pass title and description to following intent
                intent.putExtra("taskTitle", taskTitleString);
                intent.putExtra("taskDesc", taskDescString);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(PlantProfileActivity.this, 0, intent, 0);
                am.set(AlarmManager.RTC_WAKEUP, zonedDateTime.toInstant().toEpochMilli(), pendingIntent);

                CharSequence text = "Task Added";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                addTaskFragment.setVisibility(FrameLayout.GONE);
            }
        });

        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        toolbar.setTitle(plant.getName());

        scienceNameText = findViewById(R.id.scientific_name_text);
        yearText = findViewById(R.id.year_text);
        commonText = findViewById(R.id.common_name_text);
        familyText = findViewById(R.id.family_text);
    }

    // On start of the activity...
    @Override
    protected void onStart() {
        super.onStart();
        taskTitle.setText("");
        taskDesc.setText("");

        RequestQueue queue = Volley.newRequestQueue(this);

        // Get all data from the plant.
        JsonObjectRequest getPlantImage = new JsonObjectRequest(Request.Method.GET, getPlantsURL, null, response -> {
            try {
                JSONObject plantObject = response.getJSONArray("data").getJSONObject(0);
                String scienceName = plantObject.getString("scientific_name");
                int year = plantObject.getInt("year");
                String commonName = plantObject.getString("common_name");
                String family = plantObject.getString("family");
                scienceNameText.setText(scienceName);
                yearText.setText(String.valueOf(year));
                commonText.setText(commonName);
                familyText.setText(family);
            } catch (JSONException e) {
                e.printStackTrace();
                CharSequence text = "Error retrieving plant";
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(PlantProfileActivity.this, text, duration);
                toast.show();
            }
        }, error -> {
            CharSequence text = "Plant details could not be found";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(this, text, duration);
            toast.show();
        });
        queue.add(getPlantImage);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        localDate = LocalDate.of(year, month + 1, dayOfMonth);
        TextView displayDateLabel = findViewById(R.id.display_date_label);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM, yyyy");
        displayDateLabel.setText(dtf.format(localDate));
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        localTime = LocalTime.of(hourOfDay, minute);
        TextView displayTimeLabel = findViewById(R.id.display_time_label);
        displayTimeLabel.setText(localTime.toString());
    }
}
package com.example.horto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.horto.common.Garden;
import com.example.horto.common.Plant;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmResults;

/*
* Create Garden page, found on application first boot
*/
public class CreateGardenActivity extends AppCompatActivity {

    // Trefle API token - x0mkz8A-MjuYnK5_KZ8blzwgH_HiHhYBzYZu2iBIhw8
    private final String apiToken = "x0mkz8A-MjuYnK5_KZ8blzwgH_HiHhYBzYZu2iBIhw8";

    private JsonObjectRequest plantsNamesObject;
    private RealmList<Plant> addedPlants = new RealmList<>();

    Context context;

    CharSequence text1 = "Plant not found";
    CharSequence text2 = "Garden Added";
    CharSequence text3 = "Plant Added";
    int duration = Toast.LENGTH_SHORT;
    Toast toast;
    Toast toastAdded;
    Toast toastPlant;

    /*
    * On create of the activity
    * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_garden);

        EditText gardenName = findViewById(R.id.edit_garden_name);
        AutoCompleteTextView plantName = findViewById(R.id.autoCompleteTextView);

        ArrayList<String> allNames = new ArrayList<>();
        try {
            allNames = getPlantNames();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ArrayAdapter<String> plantNamesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, allNames);
        plantName.setAdapter(plantNamesAdapter);

        ListView plantsList = findViewById(R.id.add_plant_list_view);
        ArrayAdapter<Plant> addedPlantsAdapter = new ArrayAdapter<>
                (this, android.R.layout.simple_list_item_1, addedPlants);

        plantsList.setAdapter(addedPlantsAdapter);
        context = getApplicationContext();

        toast = Toast.makeText(context, text1, duration);
        toastAdded = Toast.makeText(context, text2, duration);
        toastPlant = Toast.makeText(context, text3, duration);

        // Request queue instantiated.
        RequestQueue queue = Volley.newRequestQueue(context);

        // AddPlant Button
        Button addPlant = findViewById(R.id.add_plant_button);
        ArrayList<String> finalAllNames = allNames;
        // Define the on click listener
        addPlant.setOnClickListener(v -> {
            Plant plant = new Plant();
            String plantNameString = plantName.getText().toString().replaceAll("\\s+", "");
            String getPlantsURL = "https://trefle.io/api/v1/plants/search?token=" + apiToken + "&q=" + plantNameString;

            // Perform the GET Request to the API using the above URL.
            JsonObjectRequest getPlant = new JsonObjectRequest
                    (Request.Method.GET, getPlantsURL, null, response -> {
                        if (finalAllNames.contains(plantName.getText().toString())) {
                            try {
                                // Retrieve the Plant id.
                                int plantID = response.getJSONArray("data").getJSONObject(0).getInt("id");
                                Date date = new Date();
                                // Add details of plant to the plant object and store within list
                                plant.setApiID(plantID);
                                plant.setName(plantName.getText().toString());
                                plant.setGarden(gardenName.getText().toString());
                                plant.setDateAdded(date);
                                addedPlants.add(plant);
                                addedPlantsAdapter.notifyDataSetChanged();
                                toastPlant.show();
                            } catch (Exception e) {
                                e.printStackTrace();
                                toast.show();
                            }
                        } else {
                            toast.show();
                        }
                    }, new Response.ErrorListener() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            toast.show();
                        }
                    });
            // Access the RequestQueue through your singleton class.
            queue.add(getPlant);
        });
        // Define confirm garden creation button listener.
        Button confirmButton = findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(v -> {
            // Make config for realm transaction
            RealmConfiguration config = new RealmConfiguration.Builder()
                    .allowQueriesOnUiThread(true)
                    .allowWritesOnUiThread(true)
                    .name("Garden")
                    .build();
            Realm realm = Realm.getInstance(config);
            Garden garden = new Garden(gardenName.getText().toString(), addedPlants);

            // Add garden to database
            realm.executeTransaction(transactionRealm -> {
                transactionRealm.insert(garden);
            });
            toastAdded.show();

            // Close connection
            realm.close();

            Intent myIntent = new Intent(context, HomePageActivity.class);
            startActivityForResult(myIntent, 0);

            // Remove this activity from stack
            CreateGardenActivity.this.finish();
        });

    }

    // Method for getting plant names from "plant_names.txt" in assets folder.
    public ArrayList<String> getPlantNames() throws IOException {
        ArrayList<String> names = new ArrayList<>();
        InputStream fis = this.getAssets().open("plant_names.txt");
        InputStreamReader inputStreamReader =
                new InputStreamReader(fis, StandardCharsets.UTF_8);
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            String line = reader.readLine();
            while (line != null) {
                names.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return names;
    }

}

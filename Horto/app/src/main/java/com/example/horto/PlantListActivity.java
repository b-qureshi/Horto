package com.example.horto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.horto.common.AddPlantDialog;
import com.example.horto.common.Garden;
import com.example.horto.common.Plant;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.mongodb.App;

// Shows plants in list form. 
public class PlantListActivity extends AppCompatActivity implements AddPlantDialog.AddPlantDialogListener {

    private String plantName;
    private String gardenName;
    private RealmList<Plant> plants;
    private ArrayAdapter<Plant> gardenPlantsAdapter;
    private Plant newPlant;
    private Garden userGarden;
    private final String apiToken = "x0mkz8A-MjuYnK5_KZ8blzwgH_HiHhYBzYZu2iBIhw8";

    private CharSequence text = "Plant not added: Plant not found";
    private int duration = Toast.LENGTH_SHORT;
    Toast toast;

    Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Horto);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plants_list);

        Realm.init(this);

        Toolbar toolbar = findViewById(R.id.listHeader);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Get garden details
        RealmConfiguration config = new RealmConfiguration.Builder()
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .name("Garden")
                .build();

        realm = Realm.getInstance(config);
        RealmQuery<Garden> userGardenQuery = realm.where(Garden.class);
        userGarden = userGardenQuery.findFirst();
        gardenName = userGarden.getName();
        plants = userGarden.getPlants();

        ImageButton backButton = findViewById(R.id.plants_list_back_button);
        backButton.setOnClickListener(v -> {
            Intent myIntent = new Intent(v.getContext(), HomePageActivity.class);
            startActivityForResult(myIntent, 0);
        });

        FloatingActionButton addPlantFab = findViewById(R.id.add_plant_floating_button);
        addPlantFab.setOnClickListener(v -> openAddPlantDialog());

        Context context = this.getApplicationContext();

         toast = Toast.makeText(PlantListActivity.this, text, duration);

        TextView gardenListName = findViewById(R.id.list_title);

        // Populate the list view with the garden plants.
        ListView gardenList = findViewById(R.id.plant_listView);
        gardenPlantsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, plants);

        gardenListName.setText(gardenName);
        gardenList.setAdapter(gardenPlantsAdapter);

        // Set the listener for each plant in the list.
        gardenList.setOnItemClickListener((parent, view, position, id) -> {
            Intent myIntent = new Intent(context, PlantProfileActivity.class);
            myIntent.putExtra("plantName", gardenPlantsAdapter.getItem(position).getName());
            myIntent.putExtra("plantKey", gardenPlantsAdapter.getItem(position).getApiID());
            myIntent.putExtra("plantID", gardenPlantsAdapter.getItem(position).get_id().toHexString());
            startActivityForResult(myIntent, 0);

            PlantListActivity.this.finish();
        });
    }

    // Open the dialog for adding a plant.
    public void openAddPlantDialog() {
        AddPlantDialog apd = new AddPlantDialog();
        apd.show(getSupportFragmentManager(), "Add Plant Dialog");
    }

    // Add plant to the garden
    @Override
    public void AddPlant(String plantName) {
        this.plantName = plantName;
        if (!plantName.isEmpty()) {
            newPlant = new Plant();
            String plantNameString = this.plantName.replaceAll("\\s+", "");
            String getPlantsURL = "https://trefle.io/api/v1/plants/search?token=" + apiToken + "&q=" + plantNameString;

            System.out.println(getPlantsURL);

            RequestQueue queue = Volley.newRequestQueue(PlantListActivity.this);

            JsonObjectRequest getPlant = new JsonObjectRequest
                    (Request.Method.GET, getPlantsURL, null, response -> {
                        try {
                            int plantID = response.getJSONArray("data").getJSONObject(0).getInt("id");
                            System.out.println(plantID);
                            Date date = new Date();
                            newPlant.setApiID(plantID);
                            newPlant.setName(plantName);
                            newPlant.setGarden(gardenName);
                            newPlant.setDateAdded(date);
                            realm.executeTransaction(transactionRealm -> {
                                plants.add(newPlant);
                            });
                            gardenPlantsAdapter.notifyDataSetChanged();
                            CharSequence text = "Plant added";
                            int duration = Toast.LENGTH_SHORT;
                            Toast toast = Toast.makeText(PlantListActivity.this, text, duration);
                            toast.show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            toast.show();
                        }
                    }, error -> toast.show());
            // Access the RequestQueue through your singleton class.
            queue.add(getPlant);
        }
    }
}
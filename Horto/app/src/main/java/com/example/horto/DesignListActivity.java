package com.example.horto;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.horto.common.GardenDesign;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmQuery;
import io.realm.RealmResults;
// Garden design list activity
public class DesignListActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_Horto);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.garden_designs_list);

        Toolbar toolbar = findViewById(R.id.design_list_header);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Realm configuration file
        RealmConfiguration config = new RealmConfiguration.Builder()
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .name("Garden")
                .build();

        /*
        * Copy all results from the query to the database
        * Extract design names to be used with array adapter
        */
        Realm realm = Realm.getInstance(config);
        RealmQuery<GardenDesign> userGardenDesignQuery = realm.where(GardenDesign.class);
        RealmResults<GardenDesign> designResults = userGardenDesignQuery.findAll();
        ArrayList<GardenDesign> designs = (ArrayList<GardenDesign>) realm.copyFromRealm(designResults);
        ArrayList<String> designNames = new ArrayList<>();
        for (GardenDesign d : designResults){
            designNames.add(d.getTitle());
        }

        Context context = this.getApplicationContext();

        ListView designList = findViewById(R.id.design_listView);
        ArrayAdapter<String> gardenDesignArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, designNames);

        designList.setAdapter(gardenDesignArrayAdapter);

        // Back button to go back to AR View
        ImageButton backButton = findViewById(R.id.designs_back_button);
        backButton.setOnClickListener(v -> {
            Intent myIntent = new Intent(context, ArDesignActivity.class);
            startActivityForResult(myIntent, 0);
            DesignListActivity.this.finish();
        });

        // For each item in list:
        // Open the DesignViewActivity class
        designList.setOnItemClickListener((parent, view, position, id) -> {
            Intent myIntent = new Intent(context, DesignViewActivity.class);
            myIntent.putExtra("designName", gardenDesignArrayAdapter.getItem(position));
            myIntent.putExtra("designID", designs.get(position).get_id().toHexString());
            startActivityForResult(myIntent, 0);
            DesignListActivity.this.finish();
        });

    }
}

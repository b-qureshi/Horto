package com.example.horto.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.example.horto.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

// Dialog class used for an addition of a new plant
public class AddPlantDialog extends AppCompatDialogFragment {

    private AutoCompleteTextView plantNameText; // Plant Name
    private AddPlantDialogListener listener; // Listener

    @NonNull
    @Override
    // On creation of the dialog...
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        ArrayList<String> allNames = new ArrayList<>();
        try {
            // Prepare all plant names from text files
            allNames = getPlantNames();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Using the dialog builder, build a custom dialog using dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.add_plant_dialog, null);

        builder.setView(view)
                // Set the positive action of the dialog
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = plantNameText.getText().toString();
                    listener.AddPlant(name);
                    dialog.dismiss();
                })
                // Set the negative action of the dialog
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // Attach the plant names to an array and add to the AutoCompleteTextView component
        plantNameText = view.findViewById(R.id.add_plant_textview);
        ArrayAdapter<String> plantNamesAdapter = new ArrayAdapter<>(this.getContext(), android.R.layout.simple_dropdown_item_1line, allNames);
        plantNameText.setAdapter(plantNamesAdapter);
        return builder.create();
    }

    // Initialises the listener on attach of the dialog.
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (AddPlantDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "Must implement AddPlantDialogListener");
        }
    }

    // Dialog Listener Interface
    public interface AddPlantDialogListener{
        // Called within PlantListActivity
        void AddPlant(String plantName);
    }

    // Method to read all names within the "plant_names.txt" file found in the assets folder
    public ArrayList<String> getPlantNames() throws IOException {
        ArrayList<String> names = new ArrayList<>();
        InputStream fis = this.getContext().getAssets().open("plant_names.txt");
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



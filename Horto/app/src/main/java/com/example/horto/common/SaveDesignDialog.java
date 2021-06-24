package com.example.horto.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.example.horto.R;

import io.realm.Realm;
import io.realm.RealmConfiguration;

// Dialog used for Saving a design into the database.
public class SaveDesignDialog extends AppCompatDialogFragment {

    private EditText editTextDesignTitle; // Title of the design
    private SaveDialogListener listener; // Listener

    public SaveDesignDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        // Inflate dialog with the custom layout made
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.save_a_garden,null);
        builder.setView(view)
                // Set the positive button of the dialog
                .setPositiveButton("Save", (dialog, id) -> {
                    String title = editTextDesignTitle.getText().toString();
                    listener.SaveGarden(title);
                    dialog.dismiss();
                })
                // Set the negative button of the dialog.
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());
        editTextDesignTitle = view.findViewById(R.id.edit_design_name);
        return builder.create();
    }


    // Initialise the listener on dialog attach
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (SaveDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "Must implement SaveDialogListener");
        }
    }

    // Interface for saving dialog.
    public interface SaveDialogListener{
        // Initialised in ArDesignActivity
        void SaveGarden(String gardenTitle);
    }
}

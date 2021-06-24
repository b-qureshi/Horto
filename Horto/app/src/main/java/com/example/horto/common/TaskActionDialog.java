package com.example.horto.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.example.horto.HomePageActivity;
import com.example.horto.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

// Dialog used for when a task is being acted upon in the HomePage.
public class TaskActionDialog extends AppCompatDialogFragment {

    // Components of the custom dialog
    private Toolbar taskToolbar;
    private TextView taskTitleLabel;
    private TextView taskDescriptionLabel;
    private TextView dueDateLabel;
    private TextView setDateLabel;
    private Button completeButton;
    private Button deleteButton;
    private final Task task;
    private TaskDialogActionsListener listener;
    private int pos;

    public TaskActionDialog(Task task, int pos) {
        this.task = task;
        this.pos = pos;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.task_actions_dialog, null);
        builder.setView(view)
                // Set the negative button of the dialog
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Set the toolbar title to plant name
        taskToolbar = view.findViewById(R.id.task_toolbar);
        taskToolbar.setTitle(task.getPlantName());

        // Set the task title
        taskTitleLabel = view.findViewById(R.id.task_name_label);
        taskTitleLabel.setText(task.getTitle());

        // Set the task description
        taskDescriptionLabel = view.findViewById(R.id.show_task_description_label);
        taskDescriptionLabel.setText(task.getDescription());

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm");

        // Display the due date
        dueDateLabel = view.findViewById(R.id.task_due_date_label);
        dueDateLabel.setText(sdf.format(task.getDueDate()));

        // Display the set date
        setDateLabel = view.findViewById(R.id.task_set_date_label);
        setDateLabel.setText(sdf.format(task.getSetDate()));

        // Complete task button
        completeButton = view.findViewById(R.id.complete_task_button);
        completeButton.setOnClickListener(v -> {
            listener.confirmTask(task, pos);
            getDialog().dismiss();
        });
        // Delete task button
        deleteButton = view.findViewById(R.id.delete_task_button);
        deleteButton.setOnClickListener(v -> {
            listener.deleteTask(task, pos);
            getDialog().dismiss();
    });

        // Create Dialog
        return builder.create();
    }

    // Initialise listener
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (TaskActionDialog.TaskDialogActionsListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "Must implement TaskActionDialogListener");
        }
    }

    // Interface for dialog actions.
    public interface TaskDialogActionsListener{
        void confirmTask(Task task, int pos);
        void deleteTask(Task task, int pos);
    }

}

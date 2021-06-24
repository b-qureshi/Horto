package com.example.horto.common;

import org.bson.types.ObjectId;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

// Task RealmObject used for storing into the database
public class Task extends RealmObject {

    @PrimaryKey
    private ObjectId _id = new ObjectId(); // Primary key generated using ObjectID()
    private String title; // Title of the task
    private String description; // Description of the task
    private String status; // The task status
    private Date setDate; // The task set date
    private Date dueDate; // The task due date
    private Date completionDate; // The task completion date
    private String plantName; // The task plantname

    // Constructor
    public Task(String plantName, String title, String description, Date setDate, Date dueDate) {
        this.plantName = plantName;
        this.title = title;
        this.description = description;
        this.setDate = setDate;
        this.dueDate = dueDate;
    }

    public Task () {};

    public ObjectId get_id() {
        return _id;
    }

    public void set_id(ObjectId _id) {
        this._id = _id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getSetDate() {
        return setDate;
    }

    public void setSetDate(Date setDate) {
        this.setDate = setDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

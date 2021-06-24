package com.example.horto.common;

import androidx.annotation.NonNull;

import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

// Plant RealmObject use for storage in database.
public class Plant extends RealmObject {

    @PrimaryKey
    private ObjectId _id = new ObjectId(); // Primary Key generated using ObjectID()
    private int apiID; // ID used in API
    private String name; // Name of the plant
    private String garden; // Garden it is part of
    private Date dateAdded; // Date added to the garden

    // Plant Constructor
    public Plant(int apiID, String name, String garden, Date dateAdded) {
        this.apiID = apiID;
        this.name = name;
        this.garden = garden;
        this.dateAdded = dateAdded;
    }

    public Plant() {};

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    public String details(){
        return "Plant key:" + apiID + "\nPlant name:" + name;
    }

    public int getApiID() {
        return apiID;
    }

    public void setApiID(int apiID) {
        this.apiID = apiID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGarden() {
        return garden;
    }

    public void setGarden(String garden) {
        this.garden = garden;
    }

    public Date getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }

    public ObjectId get_id() {
        return _id;
    }

    public void set_id(ObjectId _id) {
        this._id = _id;
    }
}

package com.example.horto.common;

import androidx.annotation.NonNull;

import org.bson.types.ObjectId;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

// Garden RealmObject used for storing into the Database.
public class Garden extends RealmObject{

    @PrimaryKey
    private ObjectId _id = new ObjectId(); // Primary key, generated using ObjectID
    private String name; //  Name of the garden
    private RealmList<Plant> plants; // List of plants it contains


    // Garden Constructor
    public Garden(String name, RealmList<Plant> plants) {
        this.name = name;
        this.plants = plants;
    }

    public Garden () {};

    @NonNull
    @Override
    public String toString() {
        return "This garden has: " +
                "gardenName='" + name + '\'' +
                ", plants=" + plants +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RealmList<Plant> getPlants() {
        return plants;
    }

    public void setPlants(RealmList<Plant> plants) {
        this.plants = plants;
    }

    public ObjectId get_id() {
        return _id;
    }

    public void set_id(ObjectId _id) {
        this._id = _id;
    }
}

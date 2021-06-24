package com.example.horto.common;

import io.realm.RealmObject;

// Plant position RealmObject used for storing positions of models into the database.
public class PlantPosition extends RealmObject {
    float x; // X coordinate
    float y; // Y coordinate
    float z; // Z coordinate
    String plantName; // Name of plant

    // Constructor
    public PlantPosition(float x, float y, float z, String plantName) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.plantName = plantName;
    }

    public PlantPosition() {
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }
}

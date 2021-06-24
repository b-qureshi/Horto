package com.example.horto.common;

import org.bson.types.ObjectId;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;


// Garden Design RealmObject used for storing into the database.
public class GardenDesign extends RealmObject {

    @PrimaryKey
    private ObjectId _id = new ObjectId(); // Primary key generated using ObjectID()
    private String title; // Name of Garden Design
    private RealmList<PlantPosition> plantPositions; // List of PlantPositions along with type
    private Garden garden; // Garden specified for garden design

    // Garden Design Constructor
    public GardenDesign(String title, RealmList<PlantPosition> plantPositions, Garden garden){
        this.title = title;
        this.plantPositions = plantPositions;
        this.garden = garden;
    }

    public GardenDesign() {};

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

    public RealmList<PlantPosition> getPlantPositions() {
        return plantPositions;
    }

    public void setPlantPositions(RealmList<PlantPosition> plantAnchors) {
        this.plantPositions = plantAnchors;
    }

    public Garden getGarden() {
        return garden;
    }

    public void setGarden(Garden garden) {
        this.garden = garden;
    }
}

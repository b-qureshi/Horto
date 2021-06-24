package com.example.horto.common.arcore;

import com.google.ar.core.Anchor;

// This class stores the position with the model type
public class PlantAnchor extends Anchor {

    private String type; // Plant type
    private Anchor anchor; // Plant anchor

    public PlantAnchor(Anchor anchor, String type){
        super();
        this.type = type;
        this.anchor = anchor;
    }

    public String getType(){
        return type;
    }

    public void setType(String newType){
        type = newType;
    }

    public Anchor getAnchor(){
        return anchor;
    }

    public void setAnchor(Anchor newAnchor){
        anchor = newAnchor;
    }
}

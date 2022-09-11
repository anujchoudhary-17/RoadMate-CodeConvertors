package com.example.roadmapfinal.Model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {

    public String username;
    public String email;
    public String numberPlate;
    public int points;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String username, String email,String numberPlate, int points) {
        this.username = username;
        this.email = email;
        this.points = points;
        this.numberPlate = numberPlate;
    }

}
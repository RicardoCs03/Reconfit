package com.example.reconfit.model;

import com.google.firebase.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor//Anotación que nos hace un constructor vacío
@AllArgsConstructor//Anotación que nos hace un constructor con todos los atributos
public class Habit {
    private String id;
    private String name;
    private String description;
    private String contextPlace; // "Casa", "Trabajo", "Cualquiera"
    private String contextTime;  // "Mañana", "Tarde", "Noche"
    private Timestamp createdAt;
    private int goalFrequency;
    private boolean isCompleted;

}



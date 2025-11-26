package com.example.reconfit.model;

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
    private int goalFrequency;
    private boolean isCompleted;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}



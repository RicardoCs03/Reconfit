package com.example.reconfit.model;

import com.google.firebase.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Zone {
    private String id; // Id en firestore
    private String name; // Casa, Gym, Parque, Boulevard etc.
    private double latitude; // Latitud del centro de la zona
    private double longitude; // Longitud del centro de la zona
    private double radiusMeters; // Radio de la zona en metros
    private Timestamp creationDate; // Fecha de creación de la zona
    private String userId;//Relación con el usuario que creó el hábito
}

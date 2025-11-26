package com.example.reconfit.repository;

import com.example.reconfit.model.Habit;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor

public class HabitRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String COLLECTION_NAME = "habits";

    public void addHabit(Habit habit){
        db.collection(COLLECTION_NAME).add(habit)
                .addOnSuccessListener(documentReference -> {
                    habit.setId(documentReference.getId());
                    // Manejar la respuesta
                })
                .addOnFailureListener(e -> {
                    // Manejar el error
                });
    }

    //TODO: Agregar el resto de métodos CRUD


}

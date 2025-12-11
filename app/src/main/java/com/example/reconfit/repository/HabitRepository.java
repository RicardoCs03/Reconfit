package com.example.reconfit.repository;

import android.util.Log;

import com.example.reconfit.model.Habit;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor

public class HabitRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private final String COLLECTION_NAME = "habits";

    /*public void addHabit(Habit habit){
        db.collection(COLLECTION_NAME).add(habit)
                .addOnSuccessListener(documentReference -> {
                    habit.setId(documentReference.getId());
                    // Manejar la respuesta
                })
                .addOnFailureListener(e -> {
                    // Manejar el error
                });
    }*/

    //TODO: Agregar el resto de métodos CRUD

    // Metodo para guardar un hábito en la nube
    public Task<DocumentReference> saveHabit(Habit habit) {
        // Obtenemos el ID del usuario actual (Anónimo o Logueado)
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "invitado";

        // Referencia a la colección: users/{userId}/habits
        CollectionReference habitsRef = db.collection("users").document(userId).collection("habits");

        // Guardamos el objeto
        return habitsRef.add(habit);
    }

    // NUEVO: Metodo para OBTENER la referencia a la lista de hábitos
    public CollectionReference getHabitsCollection() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "invitado";
        return db.collection("users").document(userId).collection("habits");
    }

    // Método para borrar físicamente el hábito
    public void deleteHabit(String habitId) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "invitado";

        db.collection("users")
                .document(userId)
                .collection("habits")
                .document(habitId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("HabitRepository", "Hábito eliminado"))
                .addOnFailureListener(e -> Log.w("HabitRepository", "Error al eliminar hábito", e));
    }

}

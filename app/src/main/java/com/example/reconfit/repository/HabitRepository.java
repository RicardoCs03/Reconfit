package com.example.reconfit.repository;

import android.util.Log;

import com.example.reconfit.model.Habit;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
public class HabitRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();


    // Metodo para guardar un hábito en la nube
    public Task<DocumentReference> saveHabit(Habit habit) {
        // Obtenemos el ID del usuario actual
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "invitado";
        // Referencia a la colección: users/{userId}/habits
        CollectionReference habitsRef = db.collection("users").document(userId).collection("habits");
        // Guardamos el objeto
        return habitsRef.add(habit);
    }

    /**
     * Obtiene una colección de hábitos para el usuario actual.
     * @return
     */
    public CollectionReference getHabitsCollection() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "invitado";
        return db.collection("users").document(userId).collection("habits");
    }

    /**
     * Obtiene un hábito por su ID.
     * @param habitId
     */
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

    /**
     * Actualiza el estado de un hábito.
     * @param habitId
     * @param isCompleted
     */
    public void updateHabitStatus(String habitId, boolean isCompleted) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "invitado";

        db.collection("users")
                .document(userId)
                .collection("habits")
                .document(habitId)
                .update("completed", isCompleted)
                .addOnFailureListener(e -> System.err.println("Error actualizando hábito"));
    }

    /**
     * Obtiene una lista de hábitos públicos.
     * @return
     */
    public Task<QuerySnapshot> getPublicHabits() {
        return db.collection("public_habits").get();
    }

}

package com.example.reconfit.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.reconfit.model.Habit;
import com.example.reconfit.model.Zone;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ZoneRepository {

    private final FirebaseFirestore db;
    private FirebaseAuth auth;
    private static final String ZONES_COLLECTION = "zones";
    private static final String USERS_COLLECTION = "users";
    private final String userId;

    public ZoneRepository(){
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "invitado";
    }

    //TODO: Agregar el resto de métodos CRUD

    // Metodo para guardar una zona en la nube
    public Task<DocumentReference> saveZone(Zone zone) {
        // Referencia a la colección: users/{userId}/habits
        CollectionReference zonesRef = db.collection("users").document(userId).collection("zones");
        // Guardamos el objeto
        return zonesRef.add(zone);
    }

    // --- NUEVO MÉTODO: OBTENER TODAS LAS ZONAS COMO LiveData ---
    public LiveData<List<Zone>> getAllZones() {
        MutableLiveData<List<Zone>> zonesLiveData = new MutableLiveData<>();

        if (userId == null) {
            zonesLiveData.setValue(new ArrayList<>());
            return zonesLiveData;
        }

        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(ZONES_COLLECTION)
                // Opcional: ordenar por nombre o fecha
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Manejo de errores (ej. sin conexión, permisos)
                        System.err.println("Error al escuchar zonas: " + error);
                        zonesLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<Zone> zones = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            // Importante: Asume que el modelo Zone tiene un constructor o setters
                            // que coinciden con los campos de Firestore.
                            Zone zone = doc.toObject(Zone.class);

                            // Aseguramos que el ID de la zona esté en el objeto para poder eliminar/modificar
                            zone.setId(doc.getId());
                            zones.add(zone);
                        }
                    }
                    // Actualiza el LiveData, lo que notifica al ViewModel y a la Vista
                    zonesLiveData.setValue(zones);
                });

        return zonesLiveData;
    }

    // --- MÉTODO ADICIONAL REQUERIDO: ELIMINAR ZONA ---
    // (Necesario para el botón de eliminar que añadimos en ZonesAdapter)
    public void deleteZone(Zone zone) {
        if (userId == null || zone.getId() == null) {
            System.err.println("No se puede eliminar: ID de usuario o zona nulo.");
            return;
        }

        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(ZONES_COLLECTION)
                .document(zone.getId())
                .delete()
                .addOnSuccessListener(aVoid -> System.out.println("Zona eliminada con éxito: " + zone.getName()))
                .addOnFailureListener(e -> System.err.println("Error al eliminar zona: " + e.getMessage()));
    }


}

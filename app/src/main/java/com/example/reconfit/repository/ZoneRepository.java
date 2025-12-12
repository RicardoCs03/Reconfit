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

/**
 * Repositorio de zonas para Firebase Firestore.
 */
public class ZoneRepository {
    private final FirebaseFirestore db;
    private FirebaseAuth auth;
    private static final String ZONES_COLLECTION = "zones";
    private static final String USERS_COLLECTION = "users";
    private final String userId;

    /**
     * Constructor.
     * Crea una instancia del repositorio.
     */
    public ZoneRepository(){
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "invitado";
    }

    //TODO: Agregar el resto de métodos CRUD

    // Metodo para guardar una zona en la nube
    public Task<DocumentReference> saveZone(Zone zone) {
        CollectionReference zonesRef = db.collection("users").document(userId).collection("zones");
        return zonesRef.add(zone);
    }

    /**
     * Obtiene una colección de zonas para el usuario actual.
     * @return LiveData<List<Zone>> que notifica cuando cambian los datos.
     */
    public LiveData<List<Zone>> getAllZones() {
        MutableLiveData<List<Zone>> zonesLiveData = new MutableLiveData<>();
        if (userId == null) {
            zonesLiveData.setValue(new ArrayList<>());
            return zonesLiveData;
        }
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(ZONES_COLLECTION)
                .orderBy("name", Query.Direction.ASCENDING) // Ordenar por nombre
                .orderBy("creationDate", Query.Direction.DESCENDING) // Ordenar por fecha de creación
                .addSnapshotListener((value, error) -> {
                    if (error != null) {// Manejo de errores
                        System.err.println("Error al escuchar zonas: " + error);
                        zonesLiveData.setValue(new ArrayList<>());
                        return;
                    }
                    List<Zone> zones = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Zone zone = doc.toObject(Zone.class);
                            zone.setId(doc.getId());
                            zones.add(zone);
                        }
                    }
                    zonesLiveData.setValue(zones);
                });
        return zonesLiveData;
    }

    /**
     * Obtiene una lista de nombres de zonas.
     * @return LiveData<List<String>> que notifica cuando cambian los datos.
     */
    public LiveData<List<String>> getNombresDeZonas() {
        MutableLiveData<List<String>> nombresData = new MutableLiveData<>();
        if (userId == null) {// Si no hay usuario logueado, devolvemos lista vacía
            nombresData.setValue(new ArrayList<>());
            return nombresData;
        }
        // Consulta a Firestore
        db.collection("users").document(userId).collection("zones")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> nombres = new ArrayList<>();
                    nombres.add("Cualquiera");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String nombreZona = doc.getString("name");
                        if (nombreZona != null && !nombres.contains(nombreZona)) {
                            nombres.add(nombreZona);
                        }
                    }
                    nombresData.setValue(nombres);
                })
                .addOnFailureListener(e -> {
                    // En caso de error, devolvemos al menos los defaults
                    List<String> defaults = new ArrayList<>();
                    defaults.add("Cualquiera");
                    nombresData.setValue(defaults);
                });
        return nombresData;
    }


    /**
     * Elimina una zona.
     * @param zone La zona a eliminar.
     */
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

package com.example.reconfit.repository;

import com.example.reconfit.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;

public class UserRepository {

    private final FirebaseFirestore db;
    private static final String USERS_COLLECTION = "users";

    // Nota: Eliminamos la inicialización de FirebaseAuth aquí
    // El UserRepository solo maneja los datos de Firestore.

    public UserRepository(){
        db = FirebaseFirestore.getInstance();
        // Ya no necesitamos inicializar auth ni userId aquí
    }

    /**
     * Crea o actualiza un documento de usuario en la colección 'users'.
     * El ID del documento es establecido como el UID del usuario (obtenido de User.getId()).
     *
     * @param user El objeto User, que debe tener el UID de Firebase Auth ya asignado a su campo 'id'.
     * @return Task<Void> que representa la finalización de la operación SET.
     */
    public Task<Void> createUserDocument(User user){

        // 1. Obtenemos la referencia a la colección de usuarios
        CollectionReference usersRef = db.collection(USERS_COLLECTION);

        // 2. Apuntamos al documento con el ID que coincide con el UID del usuario
        // Utilizamos .set() en lugar de .add() para forzar el ID
        DocumentReference userDocRef = usersRef.document(user.getId());

        // 3. Devolvemos la tarea de escritura
        return userDocRef.set(user);
    }

    /**
     * Obtiene el documento de usuario. Útil para cargar el perfil después de un inicio de sesión.
     *
     * @param userId El UID del usuario autenticado.
     * @return Task<DocumentSnapshot> para obtener el documento.
     */
    public Task<com.google.firebase.firestore.DocumentSnapshot> getUser(String userId){
        return db.collection(USERS_COLLECTION).document(userId).get();
    }

    // Aquí podríamos añadir métodos como updateUserName, deleteUserDocument, etc.
}
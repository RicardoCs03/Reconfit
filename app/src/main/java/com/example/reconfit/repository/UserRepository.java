package com.example.reconfit.repository;

import com.example.reconfit.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;

import java.util.Map;

/**
 * Repositorio de usuarios para Firebase Firestore.
 */
public class UserRepository {

    private final FirebaseFirestore db;
    private static final String USERS_COLLECTION = "users";
    public UserRepository(){
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Crea o actualiza un documento de usuario en la colección 'users'.
     * El ID del documento es establecido como el UID del usuario (obtenido de User.getId()).
     * @param user El objeto User, que debe tener el UID de Firebase Auth ya asignado a su campo 'id'.
     * @return Task<Void> que representa la finalización de la operación SET.
     */
    public Task<Void> createUserDocument(User user){
        CollectionReference usersRef = db.collection(USERS_COLLECTION);
        DocumentReference userDocRef = usersRef.document(user.getId());
        return userDocRef.set(user);
    }

    /**
     * Obtiene el documento de usuario. Útil para cargar el perfil después de un inicio de sesión.
     * @param userId El UID del usuario autenticado.
     * @return Task<DocumentSnapshot> para obtener el documento.
     */
    public Task<com.google.firebase.firestore.DocumentSnapshot> getUser(String userId){
        return db.collection(USERS_COLLECTION).document(userId).get();
    }

    /**
     * Actualiza los campos del perfil del usuario.
     * @param uid
     * @param profileData
     * @return
     */
    public Task<Void> updateProfileFields(String uid, Map<String, Object> profileData) {
        // Usa .update() para modificar campos sin sobrescribir el documento completo
        return db.collection(USERS_COLLECTION)
                .document(uid)
                .update(profileData);
    }

    //TODO: Aquí podríamos añadir métodos como updateUserName, deleteUserDocument, etc.
}
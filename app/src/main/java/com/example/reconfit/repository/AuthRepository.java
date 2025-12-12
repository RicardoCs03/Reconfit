package com.example.reconfit.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.reconfit.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Repositorio de autenticación para Firebase Authentication y Firestore.
 */
public class AuthRepository {
    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;
    private final MutableLiveData<FirebaseUser> currentUserLiveData = new MutableLiveData<>(); // LiveData para observar el estado del usuario autenticado
    // Constructor
    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.userRepository = new UserRepository(); // Inicializa el repositorio de datos de Firestore
        // Inicializar el LiveData con el estado actual al arrancar
        currentUserLiveData.postValue(firebaseAuth.getCurrentUser());
        // Listener para actualizar el LiveData automáticamente cuando cambia el estado
        firebaseAuth.addAuthStateListener(firebaseAuth -> {
            currentUserLiveData.postValue(firebaseAuth.getCurrentUser());
        });
    }

    /**
     * Obtiene el LiveData del usuario actual.
     * @return LiveData<FirebaseUser> que notifica cuando cambia el estado del usuario autenticado.
     */
    public LiveData<FirebaseUser> getCurrentUserLiveData() {
        return currentUserLiveData;
    }

    /**
     * Registra un nuevo usuario con Firebase Auth y crea su documento de perfil en Firestore.
     * @param email Email del usuario.
     * @param password Contraseña del usuario.
     * @return Task<Void> que completa cuando la autenticación Y la escritura en Firestore han terminado.
     */
    public Task<Void> register(String email, String password) {
        Log.d("AuthRepository", "Intentando Registrar con: " + email);
        return firebaseAuth.createUserWithEmailAndPassword(email, password)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            Log.d("AuthRepository", "Intentando Registrar con: " + email);
                            User newUser = new User(
                                    firebaseUser.getUid(), // Usamos el UID como ID del documento
                                    "",//Nombre
                                    "",//Apellido P
                                    "",//Apellido M
                                    email,
                                    "",
                                    "Male",
                                    new Timestamp(new Date())
                            );
                            //UserRepository para crear el documento en Firestore
                            return userRepository.createUserDocument(newUser);
                        }
                    }
                    // Si falla la autenticación, propagar la excepción.
                    throw task.getException();
                });
    }

    /**
     * Inicia sesión con credenciales de email y contraseña.
     * @param email Email del usuario.
     * @param password Contraseña del usuario.
     * @return Task<AuthResult> que completa cuando el inicio de sesión ha terminado.
     */
    public Task<AuthResult> login(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    public void logout() {
        firebaseAuth.signOut();
    }

    public Task<Void> updateUserProfile(String uid, Map<String, Object> profileData) {
        return userRepository.updateProfileFields(uid, profileData);// Llama al método del repositorio para actualizar los campos del perfil
    }
}
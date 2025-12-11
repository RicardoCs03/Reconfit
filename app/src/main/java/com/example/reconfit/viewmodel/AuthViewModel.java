package com.example.reconfit.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.reconfit.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;


// El ViewModel es la clase que sobrevive a los cambios de configuración
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;

    // LiveData para observar el estado del usuario autenticado (si hay o no)
    private final LiveData<FirebaseUser> currentUserLiveData;

    // LiveData para comunicar mensajes de error específicos a la UI
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // LiveData para comunicar el éxito de una operación (registro/login)
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();

    public AuthViewModel() {
        this.authRepository = new AuthRepository();
        // Obtenemos el LiveData directamente del Repositorio
        this.currentUserLiveData = authRepository.getCurrentUserLiveData();
    }

    // ------------------------------------
    // Getters para la Interfaz de Usuario
    // ------------------------------------

    /** Expone el estado de autenticación de Firebase. */
    public LiveData<FirebaseUser> getCurrentUser() {
        return currentUserLiveData;
    }

    /** Expone mensajes de error específicos. */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /** Expone el resultado de éxito de la operación. */
    public LiveData<Boolean> getOperationSuccess() {
        return operationSuccess;
    }

    // ------------------------------------
    // Lógica de Negocio (Llamadas a Repository)
    // ------------------------------------

    /**
     * Intenta registrar un nuevo usuario y crear su documento de perfil.
     */
    public void signUp(String email, String password) {
        // Limpiar el estado anterior
        errorMessage.setValue(null);
        operationSuccess.setValue(false);
        authRepository.register(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Éxito: Auth y Firestore completados
                        operationSuccess.setValue(true);
                    } else {
                        // Fallo: Propagar el mensaje de error de Firebase
                        String error = task.getException() != null ?
                                task.getException().getMessage() :
                                "Error desconocido durante el registro.";
                        errorMessage.setValue(error);
                    }
                });
    }

    /**
     * Intenta iniciar sesión con credenciales.
     */
    public void signIn(String email, String password) {
        // Limpiar el estado anterior
        errorMessage.setValue(null);
        operationSuccess.setValue(false);

        authRepository.login(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Éxito: Inicio de sesión correcto
                        operationSuccess.setValue(true);
                    } else {
                        // Fallo: Propagar el mensaje de error de Firebase
                        String error;
                        if (task.getException() != null) {
                            // Intentamos obtener un mensaje amigable o genérico
                            error = getFriendlyErrorMessage(task.getException());
                        } else {
                            error = "Error desconocido al iniciar sesión.";
                        }

                        // ¡La notificación clave!
                        errorMessage.setValue(error);
                    }
                });
    }

    // Opcional: Función para traducir errores crudos de Firebase a mensajes amigables
    private String getFriendlyErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return "Por favor, revisa tu correo y contraseña.";
        }
        if (exception instanceof FirebaseAuthInvalidUserException) {
            return "No existe ninguna cuenta con este correo electrónico.";
        }
        // Puedes añadir más tipos de errores (ej. red)
        return "Error al iniciar sesión: " + exception.getMessage();
    }

    // Getter para la vista


    /**
     * Cierra la sesión del usuario actual.
     */
    public void logout() {
        authRepository.logout();
    }

    public void clearErrorState(){
        errorMessage.setValue(null);
    }

    public void completeProfile(String uid, String name, String lastName1, String lastName2, String fecnac, String genero) {
        // Limpiar el estado anterior
        errorMessage.setValue(null);
        operationSuccess.setValue(false);

        // 1. Crear el mapa de datos a actualizar
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", name);
        profileData.put("lastName1", lastName1);
        profileData.put("lastName2", lastName2); // Puede ser vacío, pero se guarda
        profileData.put("fecnac", fecnac);
        profileData.put("genero", genero);

        // 2. Llamar al repositorio para ejecutar la actualización
        authRepository.updateUserProfile(uid, profileData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Éxito: Perfil actualizado en Firestore
                        operationSuccess.setValue(true);
                    } else {
                        // Fallo: Propagar el mensaje de error de Firestore
                        String error = task.getException() != null ?
                                task.getException().getMessage() :
                                "Error desconocido al completar el perfil.";
                        errorMessage.setValue(error);
                    }
                });
    }

}
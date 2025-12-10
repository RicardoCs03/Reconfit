package com.example.reconfit.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.reconfit.repository.AuthRepository;
import com.google.firebase.auth.FirebaseUser;


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
                        String error = task.getException() != null ?
                                task.getException().getMessage() :
                                "Error desconocido al iniciar sesión.";
                        errorMessage.setValue(error);
                    }
                });
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    public void logout() {
        authRepository.logout();
    }

    public void clearErrorState(){
        errorMessage.setValue(null);
    }
}
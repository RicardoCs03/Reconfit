package com.example.reconfit.view;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.reconfit.MainActivity;
import com.example.reconfit.R;
import com.example.reconfit.viewmodel.AuthViewModel;

public class RegisterFragment extends Fragment {

    // Componentes de UI
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registerButton;
    private TextView loginTextView;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inicializar componentes
        emailEditText = view.findViewById(R.id.et_email_register);
        passwordEditText = view.findViewById(R.id.et_password_register);
        confirmPasswordEditText = view.findViewById(R.id.et_confirm_password);
        registerButton = view.findViewById(R.id.btn_register);
        loginTextView = view.findViewById(R.id.tv_go_to_login);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 3. Implementar Navegación (Volver a Login)
        loginTextView.setOnClickListener(v -> {
            // Usa el método del AuthActivity para volver al Login
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).showLoginFragment();
            }
        });

        // 4. Implementar el intento de Registro (La lógica va en la Fase 2)
        registerButton.setOnClickListener(v -> {
            attemptRegistration();
        });

        // 5. Observar el estado del ViewModel (Fase 2)
        // observeAuthState();
    }

    private void attemptRegistration() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        // Validaciones básicas (se mejorarán en la Fase 2)
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Las contraseñas no coinciden.", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getContext(), "Intentando registrar usuario: " + email, Toast.LENGTH_SHORT).show();
        registerButton.setEnabled(false); // Para evitar doble click
        authViewModel.signUp(email, password);
    }

    private void observeAuthState() {
        // Observa el resultado de éxito de la operación (registro)
        authViewModel.getOperationSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            registerButton.setEnabled(true); // Re-habilitar botón siempre al finalizar
            if (isSuccess != null && isSuccess) {
                // Si es exitoso (true), el usuario está creado y logueado
                Toast.makeText(getContext(), "¡Registro exitoso! Iniciando...", Toast.LENGTH_SHORT).show();
                // Redirigir a MainActivity
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                // Finalizar AuthActivity para evitar volver a la pantalla de login/registro
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });

        // Observa los mensajes de error
        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                registerButton.setEnabled(true); // Re-habilitar botón
                // Mostrar el error de Firebase (ej: "La dirección de correo electrónico ya está en uso...")
                Toast.makeText(getContext(), "Error de Registro: " + error, Toast.LENGTH_LONG).show();
                // Es importante limpiar el mensaje de error en el LiveData
                // para que no se dispare de nuevo al rotar el dispositivo, por ejemplo.
                authViewModel.clearErrorState();
            }
        });
    } // Lógica de observación (Fase 2)
}
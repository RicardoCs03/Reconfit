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

public class LoginFragment extends Fragment {
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private AuthViewModel authViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inicializar componentes
        emailEditText = view.findViewById(R.id.et_email);
        passwordEditText = view.findViewById(R.id.et_password);
        loginButton = view.findViewById(R.id.btn_login);
        registerTextView = view.findViewById(R.id.tv_go_to_register);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 3. Implementar Navegación (Ir a Registro)
        registerTextView.setOnClickListener(v -> {
            // Aseguramos que el host sea AuthActivity y tenga el método
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).showRegisterFragment();
            }
        });

        // 4. Implementar el intento de Login (La lógica va en la Fase 2)
        loginButton.setOnClickListener(v -> {
            attemptLogin();
        });
        observeAuthState();
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        // Validaciones básicas (se mejorarán en la Fase 2)
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, completa ambos campos.", Toast.LENGTH_SHORT).show();
            return;
        }
        //Toast.makeText(getContext(), "Intentando iniciar sesión con: " + email, Toast.LENGTH_SHORT).show();
        authViewModel.signIn(email, password);
    }

    private void observeAuthState() {
        // Observa el resultado de la operación (login)
        authViewModel.getOperationSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            loginButton.setEnabled(true); // Re-habilitar botón
            if (isSuccess != null && isSuccess) {
                // Inicio de sesión exitoso: Redirigir a la aplicación principal
                Toast.makeText(getContext(), "¡Bienvenido! Iniciando...", Toast.LENGTH_SHORT).show();
                // Revisa el Paso 3, esta redirección pronto cambiará.
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                loginButton.setEnabled(true); // Re-habilitar botón
                Toast.makeText(getContext(), "Error al iniciar sesión: " + error, Toast.LENGTH_LONG).show();
                // Llama al método de limpieza que creamos para evitar el error de 'protected access'
                authViewModel.clearErrorState();
            }
        });
    }
}
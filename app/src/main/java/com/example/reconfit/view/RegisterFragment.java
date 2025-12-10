package com.example.reconfit.view;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.reconfit.R;

public class RegisterFragment extends Fragment {

    // Componentes de UI
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registerButton;
    private TextView loginTextView;

    // ViewModel (se inicializará en la Fase 2)
    // private AuthViewModel authViewModel;

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

        // 2. Inicializar ViewModel (Fase 2)
        // authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

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

        // Lógica de autenticación del ViewModel (Fase 2)
        // authViewModel.signUp(email, password);
    }

    // private void observeAuthState() { ... } // Lógica de observación (Fase 2)
}
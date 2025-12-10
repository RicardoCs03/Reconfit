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

public class LoginFragment extends Fragment {

    // Componentes de UI
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;

    // ViewModel (se inicializará en la próxima fase)
    // private AuthViewModel authViewModel;

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

        // 2. Inicializar ViewModel (Fase 2)
        // authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

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

        // 5. Observar el estado del ViewModel (Fase 2)
        // observeAuthState();
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        // Validaciones básicas (se mejorarán en la Fase 2)
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, completa ambos campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Intentando iniciar sesión con: " + email, Toast.LENGTH_SHORT).show();

        // Lógica de autenticación del ViewModel (Fase 2)
        // authViewModel.signIn(email, password);
    }

    // private void observeAuthState() { ... } // Lógica de observación (Fase 2)
}
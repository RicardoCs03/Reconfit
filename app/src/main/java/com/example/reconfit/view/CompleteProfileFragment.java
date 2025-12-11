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
import android.widget.Toast;

import com.example.reconfit.MainActivity;
import com.example.reconfit.R; // Asegúrate de que esta importación apunte a tu paquete base
import com.example.reconfit.viewmodel.AuthViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CompleteProfileFragment extends Fragment {

    // Componentes de UI (asegúrate de que los IDs coincidan con fragment_complete_profile.xml)
    private AuthViewModel authViewModel;
    private EditText etName, etLastName1, etLastName2, etFecnac, etGenero;
    private Button btnFinishProfile;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Carga el layout del formulario
        return inflater.inflate(R.layout.fragment_complete_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inicializar ViewModel y UI
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Inicializar vistas con sus IDs (CRÍTICO: Deben coincidir con el XML)
        etName = view.findViewById(R.id.et_name);
        etLastName1 = view.findViewById(R.id.et_last_name_1);
        etLastName2 = view.findViewById(R.id.et_last_name_2);
        etFecnac = view.findViewById(R.id.et_fecnac);
        etGenero = view.findViewById(R.id.et_genero);
        btnFinishProfile = view.findViewById(R.id.btn_finish_profile);

        // 2. Implementar Observación del resultado de guardado
        observeProfileCompletion();

        // 3. Lógica del Botón: Intentar guardar el perfil
        btnFinishProfile.setOnClickListener(v -> attemptProfileCompletion());
    }

    private void attemptProfileCompletion() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Debe haber un usuario logueado para llegar aquí
        if (user == null) {
            Toast.makeText(getContext(), "Error de sesión. Intenta iniciar de nuevo.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Obtener datos
        String name = etName.getText().toString().trim();
        String lastName1 = etLastName1.getText().toString().trim();
        String lastName2 = etLastName2.getText().toString().trim();
        String fecnac = etFecnac.getText().toString().trim();
        String genero = etGenero.getText().toString().trim();

        // 2. Validaciones (Mínimas para campos obligatorios)
        // Asumimos que name, lastName1, fecnac y genero son obligatorios (ajusta según tu necesidad)
        if (name.isEmpty() || lastName1.isEmpty() || fecnac.isEmpty() || genero.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Llamada al ViewModel para actualizar Firestore
        btnFinishProfile.setEnabled(false); // Deshabilitar para evitar spam
        authViewModel.completeProfile(user.getUid(), name, lastName1, lastName2, fecnac, genero);
    }

    private void observeProfileCompletion() {
        // Observar si la actualización de Firestore fue exitosa
        authViewModel.getOperationSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            btnFinishProfile.setEnabled(true);
            if (isSuccess != null && isSuccess) {

                // Éxito: Redirigir a MainActivity
                Toast.makeText(getContext(), "Perfil completado. ¡Bienvenido a ReconFit!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                // 🚨 Cierra la AuthActivity para que el usuario no pueda volver al login/registro
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        // Observar si ocurrió algún error al guardar
        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                btnFinishProfile.setEnabled(true);
                Toast.makeText(getContext(), "Error al guardar el perfil: " + error, Toast.LENGTH_LONG).show();
                authViewModel.clearErrorState();
            }
        });
    }
}
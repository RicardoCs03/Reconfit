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

/**
 * Fragmento para completar el perfil del usuario.
 */
public class CompleteProfileFragment extends Fragment {
    private AuthViewModel authViewModel;
    private EditText etName, etLastName1, etLastName2, etFecnac, etGenero;
    private Button btnFinishProfile;

    /**
     * Se llama cuando se crea la vista del fragmento.
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return La vista del fragmento.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_complete_profile, container, false);
    }

    /**
     * Se llama cuando la vista del fragmento ha sido creada.
     * @param view
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);// Instanciar el ViewModel
        etName = view.findViewById(R.id.et_name);
        etLastName1 = view.findViewById(R.id.et_last_name_1);
        etLastName2 = view.findViewById(R.id.et_last_name_2);
        etFecnac = view.findViewById(R.id.et_fecnac);
        etGenero = view.findViewById(R.id.et_genero);
        btnFinishProfile = view.findViewById(R.id.btn_finish_profile);
        observeProfileCompletion();
        btnFinishProfile.setOnClickListener(v -> attemptProfileCompletion());
    }

    /**
     * Intenta completar el perfil del usuario.
     * Verifica y guarda los datos en Firestore.
     */
    private void attemptProfileCompletion() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Error de sesión. Intenta iniciar de nuevo.", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = etName.getText().toString().trim();
        String lastName1 = etLastName1.getText().toString().trim();
        String lastName2 = etLastName2.getText().toString().trim();
        String fecnac = etFecnac.getText().toString().trim();
        String genero = etGenero.getText().toString().trim();
        if (name.isEmpty() || lastName1.isEmpty() || fecnac.isEmpty() || genero.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, complete todos los campos obligatorios.", Toast.LENGTH_LONG).show();
            return;
        }
        btnFinishProfile.setEnabled(false); // Deshabilitar para evitar spam
        authViewModel.completeProfile(user.getUid(), name, lastName1, lastName2, fecnac, genero);
    }

    /**
     * Método para observar si la actualización de Firestore fue exitosa.
     */
    private void observeProfileCompletion() {
        authViewModel.getOperationSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            btnFinishProfile.setEnabled(true);
            if (isSuccess != null && isSuccess) {// Éxito: Redirigir a MainActivity
                Toast.makeText(getContext(), "Perfil completado. ¡Bienvenido a ReconFit!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
                if (getActivity() != null) {//Cierra la AuthActivity para que el usuario no pueda volver al login/registro
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
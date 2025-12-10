package com.example.reconfit;

import android.os.Bundle;
import android.content.Intent; // Importación necesaria para Intents
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
// ... (otras importaciones) ...

import com.example.reconfit.view.AuthActivity; // Importación necesaria
// ... (otras importaciones de fragmentos) ...

import com.example.reconfit.view.HabitsFragment;
import com.example.reconfit.view.HomeFragment;
import com.example.reconfit.view.ZonesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Inicializar Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Ejecutar la lógica de autenticación al inicio
        checkAuthenticationAndRedirect();
    }

    // --- NUEVO MÉTODO PARA CENTRALIZAR LA VERIFICACIÓN ---
    private void checkAuthenticationAndRedirect() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // USUARIO NO AUTENTICADO: Redirigir a AuthActivity

            // 1. Crear el Intent
            Intent intent = new Intent(this, AuthActivity.class);
            // 2. Iniciar la nueva Activity
            startActivity(intent);
            // 3. Finalizar MainActivity para que el usuario no pueda volver con el botón Atrás
            finish();

        } else {
            // USUARIO AUTENTICADO: Cargar la UI principal
            Log.d("AUTH", "Usuario ya conectado: " + currentUser.getUid());

            // Si el usuario está conectado, cargamos la vista principal
            loadMainUI();
        }
    }

    private void loadMainUI() {
        setContentView(R.layout.activity_main); // Carga el layout con la BottomNavigationView

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if(getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null){
            loadFragment(new HomeFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if(itemId == R.id.nav_home){
                selectedFragment = new HomeFragment();
            }else if(itemId == R.id.nav_habits){
                selectedFragment = new HabitsFragment();
            }
            else if(itemId == R.id.nav_zones){
                selectedFragment = new ZonesFragment();
            }
            if(selectedFragment != null){
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}
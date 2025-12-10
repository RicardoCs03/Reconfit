package com.example.reconfit;

import android.os.Bundle;
import android.content.Intent; // Importación necesaria para Intents
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
// ... (otras importaciones) ...

import com.example.reconfit.view.AuthActivity; // Importación necesaria
// ... (otras importaciones de fragmentos) ...

import com.example.reconfit.view.HabitsFragment;
import com.example.reconfit.view.HomeFragment;
import com.example.reconfit.view.ZonesFragment;
import com.example.reconfit.viewmodel.AuthViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        // 2. Ejecutar la lógica de autenticación al inicio
        observeAuthState();
    }

    // --- NUEVO MÉTODO PARA CENTRALIZAR LA VERIFICACIÓN ---
    private void checkAuthenticationAndRedirect() {
        // Si el usuario no está conectado (esto será llamado tras el logout)
        if (authViewModel.getCurrentUser().getValue() == null) {
            Intent intent = new Intent(this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void loadMainUI() {
        if(findViewById(R.id.bottom_navigation)==null){
            setContentView(R.layout.activity_main);
        }
        //setContentView(R.layout.activity_main); // Carga el layout con la BottomNavigationView

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
            else if(itemId == R.id.nav_logout){
                authViewModel.logout();
                checkAuthenticationAndRedirect();
                return true;
            }
            if(selectedFragment != null){
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void observeAuthState() {
        authViewModel.getCurrentUser().observe(this, firebaseUser -> {

            // El observador se dispara cada vez que el estado de Firebase cambia.

            if (firebaseUser != null) {
                // Usuario logueado o sesión persistente detectada
                Log.d("AUTH", "Observador: Sesión activa. Cargando UI principal.");

                // Cargar la UI principal solo si aún no está cargada (evitar recarga en rotación)
                if (findViewById(R.id.bottom_navigation) == null) {
                    loadMainUI();
                }
            } else {
                // Usuario deslogueado (o sesión no detectada)
                // Esto solo se ejecutará cuando el usuario haga Logout (nav_logout).
                // Si findViewById(R.id.bottom_navigation) es != null, significa que estamos
                // en la MainActivity y necesitamos salir.

                if (findViewById(R.id.bottom_navigation) != null) {
                    // Redirigir y cerrar MainActivity
                    checkAuthenticationAndRedirect();
                } else {
                    // Si la app inicia y no detecta sesión, debemos forzar la redirección.
                    // Esta es la lógica que faltaba al inicio.
                    // Usamos un control para evitar un loop en AuthActivity,
                    // pero si estamos en MainActivity y no hay usuario, vamos a AuthActivity.

                    // Si llegamos a este punto y el usuario es nulo,
                    // pero la UI no se ha cargado (es un inicio de app fallido),
                    // debemos redirigir al Login.

                    // 🚨 Para evitar el rebote, solo llamaremos a checkAuthenticationAndRedirect
                    // cuando sea forzado por el logout.
                }
            }
        });
    }

    private void loadFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}
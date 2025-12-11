package com.example.reconfit;

import android.os.Bundle;
import android.content.Intent; // Importación necesaria para Intents
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
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
        setContentView(R.layout.activity_main);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        final Observer<FirebaseUser> initialObserver = new Observer<FirebaseUser>() {
            @Override
            public void onChanged(FirebaseUser firebaseUser) {
                authViewModel.getCurrentUser().removeObserver(this);
                if (firebaseUser == null) {
                    Intent intent = new Intent(MainActivity.this, AuthActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Log.d("AUTH", "Usuario detectado por el observador. Cargando UI.");
                    loadMainUI();
                }
            }
        };
        authViewModel.getCurrentUser().observe(this, initialObserver);
        observeLogoutState();
    }

    private void handleInitialAuthState() {
        // Verificar el estado inmediatamente al inicio de la Activity
        if (authViewModel.getCurrentUser().getValue() == null) {
            // Usuario NO autenticado -> Redirigir al Login
            Intent intent = new Intent(this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            // Usuario AUTENTICADO -> Cargar la UI principal
            Log.d("AUTH", "Usuario detectado al inicio. Cargando UI.");
            loadMainUI();
        }
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

    private void observeLogoutState() {
        authViewModel.getCurrentUser().observe(this, firebaseUser -> {
            if (firebaseUser == null) {

                if (findViewById(R.id.bottom_navigation) != null) {
                    // Si la UI principal estaba cargada, la cerramos y vamos al login.
                    handleInitialAuthState();
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
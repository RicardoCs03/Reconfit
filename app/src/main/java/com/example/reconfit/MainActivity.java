package com.example.reconfit;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

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
        setContentView(R.layout.activity_main);

        // 1. Inicializar Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Verificar si ya estaba conectado
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // 3. Si no hay usuario, iniciar sesión anónima
            mAuth.signInAnonymously();
        } else {
            Log.d("AUTH", "Usuario ya conectado: " + currentUser.getUid());
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if(savedInstanceState == null){
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

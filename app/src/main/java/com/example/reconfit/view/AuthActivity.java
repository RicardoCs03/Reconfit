package com.example.reconfit.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.example.reconfit.R;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Si es la primera vez que se carga (savedInstanceState == null),
        // cargamos el LoginFragment por defecto.
        if (savedInstanceState == null) {
            showLoginFragment();
        }
    }

    // Método público para mostrar el LoginFragment (será llamado por el RegisterFragment)
    public void showLoginFragment() {
        replaceFragment(new LoginFragment(), false); // No lo añade al back stack
    }

    // Método público para mostrar el RegisterFragment (será llamado por el LoginFragment)
    public void showRegisterFragment() {
        // Lo añade al back stack para que el botón "Atrás" lleve al Login
        replaceFragment(new RegisterFragment(), true);
    }

    public void showCompleteProfileFragment() {
        replaceFragment(new CompleteProfileFragment(), false);
    }

    // Método utilitario centralizado para cambiar de fragmento
    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // Animaciones sutiles para una mejor experiencia de usuario
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
        transaction.replace(R.id.auth_fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}
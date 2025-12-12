package com.example.reconfit.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.os.Bundle;
import com.example.reconfit.R;

/**
 * Actividad de autenticación.
 * Aquí se muestra el LoginFragment por defecto.
 */
public class AuthActivity extends AppCompatActivity {

    /**
     * Se llama cuando la actividad es creada.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        if (savedInstanceState == null) {
            showLoginFragment();// Muestra el LoginFragment por defecto
        }
    }

    /**
     * Muestra el LoginFragment.
     * No añade el fragmento al back stack.
     */
    public void showLoginFragment() {
        replaceFragment(new LoginFragment(), false);
    }

    /**
     * Muestra el RegisterFragment.
     * Añade el fragmento al back stack.
     */
    public void showRegisterFragment() {
        // Lo añade al back stack para que el botón "Atrás" lleve al Login
        replaceFragment(new RegisterFragment(), true);
    }

    /**
     * Muestra el CompleteProfileFragment.
     * No añade el fragmento al back stack.
     */
    public void showCompleteProfileFragment() {
        replaceFragment(new CompleteProfileFragment(), false);
    }

    /**
     * Reemplaza el fragmento actual.
     * @param fragment
     * @param addToBackStack
     */
    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(// Animaciones de entrada y salida
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
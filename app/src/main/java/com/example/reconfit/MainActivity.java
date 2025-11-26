package com.example.reconfit;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.reconfit.view.HabitsFragment;
import com.example.reconfit.view.HomeFragment;
import com.example.reconfit.view.ZonesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

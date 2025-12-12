package com.example.reconfit.view;


import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.reconfit.R; // Asegúrate de que tu R sea correcto
import com.example.reconfit.model.Habit;
import com.example.reconfit.repository.HabitRepository;
import com.example.reconfit.viewmodel.ZonesViewModel;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Actividad para crear un hábito.
 */
public class CreateHabitActivity extends AppCompatActivity {
    private EditText etName, etDescr;
    private RadioGroup rgTime;
    private Spinner spinnerLugar;
    private Button btnSave;
    private HabitRepository repository;
    private ZonesViewModel zonesViewModel;
    private List<String> zonasNombres = new ArrayList<>();
    private ArrayAdapter<String> adapterSpinner;

    /**
     * Se llama cuando la actividad es creada.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_habit);
        repository = new HabitRepository();
        zonesViewModel = new ViewModelProvider(this).get(ZonesViewModel.class);
        // Conectamos con la UI
        etName = findViewById(R.id.et_habit_name);
        etDescr = findViewById(R.id.et_habit_description);
        spinnerLugar = findViewById(R.id.spinner_lugar);
        rgTime = findViewById(R.id.rg_time);
        btnSave = findViewById(R.id.btn_save_habit);
        zonesViewModel.getNombresDeZonas().observe(this, listaNombres -> {
            adapterSpinner = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    listaNombres
            );
            adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerLugar.setAdapter(adapterSpinner);
        });
        btnSave.setOnClickListener(v -> guardarHabito());
    }

    /**
     * Guarda un hábito en Firebase.
     */
    private void guardarHabito() {
        String nombre = etName.getText().toString().trim();
        String descr = etDescr.getText().toString().trim();
        if (nombre.isEmpty()) {
            etName.setError("Escribe un nombre");
            return;
        }
        if (descr.isEmpty()) {
            descr = "";
        }
        String lugarSeleccionado = spinnerLugar.getSelectedItem().toString();
        String tiempo = "Cualquiera";
        if (rgTime.getCheckedRadioButtonId() == R.id.rb_morning) tiempo = "Mañana";
        else if (rgTime.getCheckedRadioButtonId() == R.id.rb_afternoon) tiempo = "Tarde";
        else if (rgTime.getCheckedRadioButtonId() == R.id.rb_night) tiempo = "Noche";
        Habit nuevoHabito = new Habit();
        nuevoHabito.setName(nombre);
        nuevoHabito.setContextPlace(lugarSeleccionado);
        nuevoHabito.setContextTime(tiempo);
        nuevoHabito.setDescription(descr);
        nuevoHabito.setUserId(null);
        // Habilitado por defecto
        nuevoHabito.setCompleted(false);
        nuevoHabito.setGoalFrequency(1);
        nuevoHabito.setCreatedAt(new Timestamp(new Date()));
        btnSave.setEnabled(false); // Evitar doble click
        repository.saveHabit(nuevoHabito)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "¡Hábito Guardado!", Toast.LENGTH_SHORT).show();
                    finish(); // Cierra la pantalla y vuelve atrás
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

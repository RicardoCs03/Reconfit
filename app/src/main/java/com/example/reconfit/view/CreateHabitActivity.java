package com.example.reconfit.view;


import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.reconfit.R; // Asegúrate de que tu R sea correcto
import com.example.reconfit.model.Habit;
import com.example.reconfit.repository.HabitRepository;

import com.google.firebase.Timestamp;
import java.util.Date;

public class CreateHabitActivity extends AppCompatActivity {

    private EditText etName, etDescr;
    private RadioGroup rgPlace, rgTime;
    private Button btnSave;
    private HabitRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_habit);

        // Inicializamos el repositorio
        repository = new HabitRepository();

        // Conectamos con la UI
        etName = findViewById(R.id.et_habit_name);
        etDescr = findViewById(R.id.et_habit_description);
        rgPlace = findViewById(R.id.rg_place);
        rgTime = findViewById(R.id.rg_time);
        btnSave = findViewById(R.id.btn_save_habit);

        btnSave.setOnClickListener(v -> guardarHabito());
    }

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

        // Obtener valor seleccionado de Lugar
        String lugar = "Cualquiera";
        if (rgPlace.getCheckedRadioButtonId() == R.id.rb_home) lugar = "Casa";
        else if (rgPlace.getCheckedRadioButtonId() == R.id.rb_work) lugar = "Trabajo";

        // Obtener valor seleccionado de Tiempo
        String tiempo = "Cualquiera";
        if (rgTime.getCheckedRadioButtonId() == R.id.rb_morning) tiempo = "Mañana";
        else if (rgTime.getCheckedRadioButtonId() == R.id.rb_night) tiempo = "Noche";

        // Crear el objeto
        Habit nuevoHabito = new Habit();
        nuevoHabito.setName(nombre);
        nuevoHabito.setContextPlace(lugar);
        nuevoHabito.setContextTime(tiempo);
        nuevoHabito.setDescription(descr);
        nuevoHabito.setUserId(null);

        // Valores por defecto importantes
        nuevoHabito.setCompleted(false);
        nuevoHabito.setGoalFrequency(1);

        // new Date() obtiene la fecha/hora actual del teléfono
        // new Timestamp() la convierte al formato que le gusta a Firebase
        nuevoHabito.setCreatedAt(new Timestamp(new Date()));

        // Enviar a Firebase usando el repositorio
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

package com.example.reconfit.view;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.reconfit.R;
import com.example.reconfit.viewmodel.HomeViewModel;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment implements SensorEventListener {
    //Variables/Objetos/Constantes del sensor:
    private TextView dayNightStatusTextView;
    private TextView tvPasos;
    private android.widget.ProgressBar progressBarPasos;
    private CardView cardPasos;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor stepSensor;
    private boolean isSensorPresent = false;
    private int ultimoValorSensor = 0;
    private HomeViewModel homeViewModel;// Variable para la manipulación del ViewModel

    // Constante: Nivel de luz (en lux) para considerar "oscuridad"
    private static final float LIGHT_THRESHOLD = 20.0f; // Puedes ajustar este valor
    // Constante: Rango de horas para considerar "noche" por defecto (20:00 a 07:00)
    private static final int NIGHT_START_HOUR = 20; // 8 PM
    private static final int NIGHT_END_HOUR = 7;    // 7 AM

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onResume(){
        super.onResume();
        if (lightSensor != null && sensorManager != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(isSensorPresent && sensorManager != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    //
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // Vincular vistas
        dayNightStatusTextView = view.findViewById(R.id.tv_day_night_status);
        tvPasos = view.findViewById(R.id.tv_steps_count);
        progressBarPasos = view.findViewById(R.id.progress_bar_steps);
        cardPasos = view.findViewById(R.id.card_steps);

        //Inicializamos el Gestor de Sensores
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);

        // Inicializamos el sensor de luz
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if(lightSensor == null){
            //En dado caso que el dispositivo no tenga sensor de luz
            dayNightStatusTextView.setText("Sensor de luz no disponible en el dispositivo");
            dayNightStatusTextView.setVisibility(View.VISIBLE);//Por defecto está invisible entonces se muestra el mensaje
        }

        // Verificar si existe el sensor de pasos
        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            // Se inicializa sensor de pasos
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isSensorPresent = true;
        } else {
            // Muestra un aviso si estás en emulador o dispositivo sin sensor
            Toast.makeText(getContext(), "Sensor de pasos no disponible", Toast.LENGTH_SHORT).show();
            isSensorPresent = false;
        }
        // Pedir Permiso en tiempo de ejecución (Necesario para Android 10/Q +)
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            // Pedir permiso (puedes usar el request code que quieras, ej. 100)
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 100);
        }

        // Observar el LiveData del ViewModel
        homeViewModel.getDayNightStatus().observe(getViewLifecycleOwner(), status -> {
            String statusText;
            int bgColor;

            if("NIGHT_SCHEDULED_DARK".equals(status)){
                statusText="Es de noche, modo descanso sugerido!";
                bgColor = getResources().getColor(android.R.color.holo_blue_dark);
            }
            else if("DARK_INTERIOR".equals(status)){
                statusText="Oscuridad detectada, estás en un interior.";
                bgColor = getResources().getColor(android.R.color.holo_orange_dark);
            }
            else {
                statusText = "¡Es de Día! ¡A entrenar!";
                bgColor = getResources().getColor(android.R.color.holo_green_dark);
            }
            dayNightStatusTextView.setText(statusText);
            dayNightStatusTextView.setBackgroundColor(bgColor);
            dayNightStatusTextView.setVisibility(View.VISIBLE);

        });

        // Observar el LiveData del ViewModel
        homeViewModel.getSteps().observe(getViewLifecycleOwner(), currentSteps -> {
            if (currentSteps != null) {
                // Actualizamos el texto
                tvPasos.setText(currentSteps + " / 5,000");  // Hardcode de la meta de pasos diario

                // Actualizamos la barra de progreso
                progressBarPasos.setProgress(currentSteps);
            }
        });

        // Cheat mode code (Habilitar contador de pasos al tocar en la tarjeta)
        cardPasos.setOnClickListener(v -> {
            // Abrimos los SP
            SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);

            // Obtenemos los datos actuales y los actualizamos
            int extraActuales = prefs.getInt("pasos_extra_simulados", 0);
            int nuevosExtra = extraActuales + 500;

            // Guardar en memoria permanente
            prefs.edit().putInt("pasos_extra_simulados", nuevosExtra).apply();

            actualizarPasosTotales();

        });

        // Cheat mode code (Reiniciar el contador de pasos volutariamente con un click largo)
        cardPasos.setOnLongClickListener(v -> {
            // 1. Borrar la preferencia
            SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);
            prefs.edit().remove("pasos_base_dia").apply();
            prefs.edit().remove("pasos_extra_simulados").apply();

            // 2. Avisar al ViewModel que es 0
            homeViewModel.updateSteps(0);

            Toast.makeText(getContext(), "Contador de pasos reiniciado", Toast.LENGTH_SHORT).show();
            return true; // Indica que consumimos el evento
        });

        actualizarPasosTotales();
    }

    //Implementación de los métodos de SensorEventListener

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // LOGICA SENSORES
        // Sensor de luz
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            // Pasamos el valor del sensor al ViewModel
            homeViewModel.processLightData(event.values[0]);
        }
        // Sensor de pasos
        else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            ultimoValorSensor = (int) event.values[0];
            int totalStepsSensor = (int) event.values[0];

            // Usamos SharedPreferences para guardar el "Punto de Inicio" de forma permanente
            SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);

            // Logica para reinicio diario
            // Obtener la fecha de hoy (Ej: "20251210")
            String fechaHoy = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
            // Obtener la última fecha guardada
            String ultimaFechaGuardada = prefs.getString("fecha_ultimo_conteo", "");

            // Si las fechas son diferentes se Reinicia todo
            if (!ultimaFechaGuardada.equals(fechaHoy)) {
                // Guardamos la nueva fecha
                prefs.edit().putString("fecha_ultimo_conteo", fechaHoy).apply();

                // Reiniciamos la base al valor actual del sensor
                prefs.edit().putInt("pasos_base_dia", totalStepsSensor).apply();

                // Reiniciamos los pasos simulados (la trampa) a 0
                prefs.edit().putInt("pasos_extra_simulados", 0).apply();
            }

            // Obtenemos la base (ahora actualizada si cambió el día)
            int pasosBaseGuardados = prefs.getInt("pasos_base_dia", -1);

            // Si es la primera vez que corremos la app hoy (o se borraron los datos)
            if (pasosBaseGuardados == -1) {
                pasosBaseGuardados = totalStepsSensor;
                prefs.edit().putInt("pasos_base_dia", pasosBaseGuardados).apply();
                prefs.edit().putString("fecha_ultimo_conteo", fechaHoy).apply(); // Guardamos fecha también
            }

            // Protección: Si se reinicia el celular, el sensor vuelve a 0 y podría dar negativos
            if (totalStepsSensor < pasosBaseGuardados) {
                // Reseteamos la base
                pasosBaseGuardados = totalStepsSensor;
                prefs.edit().putInt("pasos_base_dia", pasosBaseGuardados).apply();
            }

            // Mandamos el dato al ViewModel
            actualizarPasosTotales();
        }
    }

    private void actualizarPasosTotales() {
        // Obtener datos de SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);
        int pasosExtra = prefs.getInt("pasos_extra_simulados", 0);
        int pasosBase = prefs.getInt("pasos_base_dia", -1);

        // Obtener dato crudo del sensor (necesitas una variable global para esto)
        int pasosRealesHoy = 0;
        if (pasosBase != -1 && ultimoValorSensor > 0) {
            pasosRealesHoy = ultimoValorSensor - pasosBase;
            if (pasosRealesHoy < 0) pasosRealesHoy = 0;
        }

        // Sumar todo
        int totalAVisualizar = pasosRealesHoy + pasosExtra;

        // 4. Mandar al ViewModel
        homeViewModel.updateSteps(totalAVisualizar);
    }
}
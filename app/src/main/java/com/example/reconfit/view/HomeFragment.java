package com.example.reconfit.view;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.reconfit.R;
import com.example.reconfit.viewmodel.HomeViewModel;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment implements SensorEventListener {
    //Variables/Objetos/Constantes del sensor:
    private TextView dayNightStatusTextView;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private HomeViewModel homeViewModel;// Variable para la manipulación del ViewModel

    // Constante: Nivel de luz (en lux) para considerar "oscuridad"
    private static final float LIGHT_THRESHOLD = 20.0f; // Puedes ajustar este valor
    // Constante: Rango de horas para considerar "noche" por defecto (20:00 a 07:00)
    private static final int NIGHT_START_HOUR = 20; // 8 PM
    private static final int NIGHT_END_HOUR = 7;    // 7 AM

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if (lightSensor != null && sensorManager != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
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
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);


        dayNightStatusTextView = view.findViewById(R.id.tv_day_night_status);
        //Inicializamos el Gestor de Sensores
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        //Inicializamos el sensor de luz
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if(lightSensor == null){
            //En dado caso que el dispositivo no tenga sensor de luz
            dayNightStatusTextView.setText("Sensor de luz no disponible en el dispositivo");
            dayNightStatusTextView.setVisibility(View.VISIBLE);//Por defecto está invisible entonces se muestra el mensaje
        }

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



    }

    //Implementación de los métodos de SensorEventListener

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            // Pasamos el valor del sensor al ViewModel
            homeViewModel.processLightData(event.values[0]);
        }
    }
}
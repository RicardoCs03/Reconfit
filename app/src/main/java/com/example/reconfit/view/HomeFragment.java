package com.example.reconfit.view;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.android.gms.location.FusedLocationProviderClient;

import com.example.reconfit.R;
import com.example.reconfit.viewmodel.HomeViewModel;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

/**
 * Fragmento para mostrar el Home.
 */
public class HomeFragment extends Fragment implements SensorEventListener {
    private TextView dayNightStatusTextView;
    private TextView tvHabitDayName;
    private TextView tvHabitDayDesc;
    private TextView tvPasos;
    private ProgressBar progressBarPasos;
    private CardView cardPasos;
    private TextView tvTituloContexto;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor stepSensor;
    private boolean isSensorPresent = false;
    private Sensor accelerometer;
    private double magnitudePrevious = 0;
    private static final double MAGNITUDE_THRESHOLD = 13.0; // Sensibilidad (Ajústalo si cuenta mucho o poco)
    private long lastStepTime = 0; // Para evitar contar doble (Debounce)
    private int pasosAcumuladosAcelerometro = 0; // Contador manual
    private int ultimoValorSensor = 0;
    private HomeViewModel homeViewModel;// Variable para la manipulación del ViewModel

    private RecyclerView rvSugerencias;
    private HabitsAdapter habitsAdapter;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    /**
     * Constructor.
     */
    public HomeFragment() {

    }

    /**
     * Se llama cuando el fragmento se crea.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    /**
     * Receptor de tiempo.
     */
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, android.content.Intent intent) {
            // Cada minuto (TIME_TICK) o si cambias la hora manual (TIME_CHANGED)
            if (homeViewModel != null) {
                homeViewModel.actualizarMomentoPorHora();
            }
        }
    };

    /**
     * Se llama cuando el fragmento se reanuda.
     */
    @Override
    public void onResume(){
        super.onResume();
        if (lightSensor != null && sensorManager != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        iniciarGPS();
        if (homeViewModel != null) {
            homeViewModel.actualizarMomentoPorHora();
        }
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(android.content.Intent.ACTION_TIME_TICK);    // Cada minuto
        filter.addAction(android.content.Intent.ACTION_TIME_CHANGED); // Cambio manual
        filter.addAction(android.content.Intent.ACTION_TIMEZONE_CHANGED); // Cambio de zona
        requireContext().registerReceiver(timeReceiver, filter);
    }

    /**
     * Se llama cuando el fragmento se pausa.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        detenerGPS();
        // Dejar de escuchar el tiempo para ahorrar batería
        try {
            requireContext().unregisterReceiver(timeReceiver);
        } catch (IllegalArgumentException e) {
            // Evitamos errores si por alguna razón no estaba registrado
            e.printStackTrace();
        }
    }

    /**
     * Se llama cuando el fragmento se crea la vista.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    /**
     * Se llama cuando la vista del fragmento ha sido creada.
     * @param view
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireActivity());
        // Lista de permisos que necesitamos
        List<String> permisosNecesarios = new ArrayList<>();
        // Checar Ubicación
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // Checar Actividad Física (Solo si es Android 10/Q o superior)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(Manifest.permission.ACTIVITY_RECOGNITION);
            }
        }
        // Si falta alguno, pedirlos todos de golpe
        if (!permisosNecesarios.isEmpty()) {
            requestPermissions(permisosNecesarios.toArray(new String[0]), 100);
        }
        // Vincular vistas
        dayNightStatusTextView = view.findViewById(R.id.tv_day_night_status);
        tvHabitDayName = view.findViewById(R.id.tv_habit_day_name);
        tvHabitDayDesc = view.findViewById(R.id.tv_habit_day_desc);
        tvPasos = view.findViewById(R.id.tv_steps_count);
        progressBarPasos = view.findViewById(R.id.progress_bar_steps);
        cardPasos = view.findViewById(R.id.card_steps);
        tvTituloContexto = view.findViewById(R.id.tv_context_label);
        //Inicializamos el Gestor de Sensores
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        // Inicializamos el sensor de luz
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if(lightSensor == null){
            //En dado caso que el dispositivo no tenga sensor de luz
            dayNightStatusTextView.setText("Sensor de luz no disponible en el dispositivo");
            dayNightStatusTextView.setVisibility(View.VISIBLE);//Por defecto está invisible entonces se muestra el mensaje
        }
        // Verificar si existe el sensor de pasos // LÓGICA DE PRIORIDAD DE SENSORES
        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            android.util.Log.d("RECONFIT_DEBUG", "SENSOR: ¡Tenemos Chip de Pasos (Hardware)!");
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isSensorPresent = true;
            accelerometer = null; // No usamos acelerómetro
        } else {
            android.util.Log.d("RECONFIT_DEBUG", "SENSOR: No hay chip. Usando Acelerómetro (Plan B)");
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            stepSensor = null;
            isSensorPresent = false; // "False" porque no es el sensor de pasos dedicado
            Toast.makeText(getContext(), "Usando Acelerómetro (Modo Compatibilidad)", Toast.LENGTH_SHORT).show();
        }
        // Pedir Permiso en tiempo de ejecución (Necesario para Android 10/Q +)
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            // Pedir permiso (puedes usar el request code que quieras, ej. 100)
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 100);
        }

        // Observar el LiveData del ViewModel
        homeViewModel.getDayNightStatus().observe(getViewLifecycleOwner(), statusKey -> {
            int colorResId;
            // Asignamos solo color basado en la clave técnica
            if("NIGHT_SCHEDULED_DARK".equals(statusKey)){
                colorResId = R.color.translucent_blue_night;
            }
            else if("DARK_INTERIOR".equals(statusKey)){
                colorResId = R.color.translucent_orange_dark;
            }
            else {
                colorResId = R.color.translucent_green_day;
            }

            // Usamos ContextCompat para sacar el color real de forma segura
            int colorFinal = ContextCompat.getColor(requireContext(), colorResId);

            // Obtenemos el fondo redondo y lo "pintamos" (Tint)
            Drawable background = dayNightStatusTextView.getBackground();
            background.mutate().setTint(colorFinal);
            dayNightStatusTextView.setVisibility(View.VISIBLE);
        });

        // Observar el TEXTO (La frase inteligente de la matriz)
        homeViewModel.getRecommendationText().observe(getViewLifecycleOwner(), frase -> {
            dayNightStatusTextView.setText(frase);
        });

        // Observar el habito del dia
        homeViewModel.getHabitOfTheDay().observe(getViewLifecycleOwner(), habito -> {
            if (habito != null) {
                tvHabitDayName.setText(habito.getName());
                if (tvHabitDayDesc != null) {
                    tvHabitDayDesc.setText(habito.getDescription());
                }
            }
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

        cardPasos.setOnClickListener(v -> {
            verificarReinicioDiario();
            SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);
            int extraActuales = prefs.getInt("pasos_extra_simulados", 0);
            int nuevosExtra = extraActuales + 1;
            prefs.edit().putInt("pasos_extra_simulados", nuevosExtra).apply();
            actualizarPasosTotales();
        });
        cardPasos.setOnLongClickListener(v -> {
            SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);
            prefs.edit().remove("pasos_base_dia").apply();
            prefs.edit().remove("pasos_extra_simulados").apply();
            homeViewModel.updateSteps(0);
            Toast.makeText(getContext(), "Contador de pasos reiniciado", Toast.LENGTH_SHORT).show();
            return true; // Indica que consumimos el evento
        });
        actualizarPasosTotales();
        rvSugerencias = view.findViewById(R.id.rv_habits_focused);
        rvSugerencias.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        habitsAdapter = new HabitsAdapter(new ArrayList<>(), new HabitsAdapter.OnHabitActionListener() {
            /**
             * Eliminar un hábito.
             */
            @Override
            public void onDelete(String habitId) {
                // Lógica de borrar (permitir borrar desde el Home)
                // homeViewModel.deleteHabit(habitId); (Si agregas ese método al VM)
            }

            /**
             * Cambiar el estado de un hábito.
             */
            @Override
            public void onToggle(String habitId, boolean isCompleted) {
                //Actualizamos en Firebase
                homeViewModel.updateHabitStatus(habitId, isCompleted);
                // Feedback opcional
                if (isCompleted) {
                    Toast.makeText(getContext(), "¡Bien hecho!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvSugerencias.setAdapter(habitsAdapter);
        homeViewModel.getFocusedHabits().observe(getViewLifecycleOwner(), listaFiltrada -> {
            // Si la lista es null o está vacía, podrías mostrar un texto de "No hay sugerencias"
            // Pero por ahora, solo actualizamos el adaptador
            if (listaFiltrada != null) {
                habitsAdapter.setHabits(listaFiltrada);
                habitsAdapter.notifyDataSetChanged();
            }
        });

        // Observa los cambios de lugar
        homeViewModel.getLugarDetectado().observe(getViewLifecycleOwner(), nombreLugar -> {
            if (nombreLugar != null && tvTituloContexto != null) {
                if (nombreLugar.equals("Cualquiera") || nombreLugar.equals("Escuchando...")) {
                    tvTituloContexto.setText("En Foco (Sugerencias)");
                } else {
                    tvTituloContexto.setText("En Foco (Estás en '" + nombreLugar + "')");
                }
            }
        });


        // Configurar la petición de GPS (Qué tan rápido queremos actualizaciones)
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // Actualizar cada 10 segundos
        locationRequest.setFastestInterval(5000); // O mínimo cada 5 segundos
        // Definir qué hacer cuando llega una nueva ubicación
        locationCallback = new LocationCallback() {
            /**
             * Se llama cuando llega una nueva ubicación.
             * @param locationResult
             */
            @Override
            public void onLocationResult(@androidx.annotation.NonNull com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult == null) {
                    android.util.Log.w("RECONFIT_DEBUG", "GPS: Callback ejecutado pero result es NULL");
                    return;
                }
                android.util.Log.d("RECONFIT_DEBUG", "GPS: ¡Llegaron " + locationResult.getLocations().size() + " ubicaciones!");
                for (android.location.Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        float acc = location.getAccuracy();
                        android.util.Log.d("RECONFIT_DEBUG", "GPS DATO: Lat=" + lat + ", Lon=" + lon + " (Precisión: " + acc + "m)");
                        // Se lo mandamos al cerebro (ViewModel)
                        homeViewModel.verificarUbicacionReal(lat, lon);
                        // Opcional: Para depurar y ver que funciona
                        // Toast.makeText(getContext(), "GPS Actualizado", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

    }

    /**
     * Se llama cuando el sensor cambia.OR_STATUS_*}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Se llama cuando el sensor cambia.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            android.util.Log.d("RECONFIT_DEBUG", "PASOS (HW): Valor recibido del chip = " + event.values[0]);
        }

        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Solo imprime si el movimiento es fuerte para no saturar el log
            double magnitude = Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]);
            if (magnitude > 11.0) {
                android.util.Log.d("RECONFIT_DEBUG", "ACELERÓMETRO: ¡Movimiento detectado! Mag=" + magnitude);
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            // Pasamos el valor del sensor al ViewModel
            homeViewModel.processLightData(event.values[0]);
        }

        else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            verificarReinicioDiario();

            int totalStepsSensor = (int) event.values[0];
            ultimoValorSensor = totalStepsSensor; // Actualizamos global

            SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);
            int pasosBaseGuardados = prefs.getInt("pasos_base_dia", -1);
            if (pasosBaseGuardados == -1) {
                pasosBaseGuardados = totalStepsSensor;
                prefs.edit().putInt("pasos_base_dia", pasosBaseGuardados).apply();
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
        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Obtenemos x, y, z
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double magnitude = Math.sqrt(x * x + y * y + z * z);
            if (magnitude > MAGNITUDE_THRESHOLD) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastStepTime > 500) {
                    lastStepTime = currentTime;
                    verificarReinicioDiario();
                    pasosAcumuladosAcelerometro++;
                    guardarYActualizarPasosManuales();
                }
            }
        }
    }

    /**
     * Guardar y actualizar los pasos "reales" acumulados hoy.
     */
    private void guardarYActualizarPasosManuales() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);
        // Recuperar los pasos "reales" acumulados hoy (si ya había)
        // en el caso del acelerómetro, para no complicar la lógica híbrida.
        int pasosGuardados = prefs.getInt("pasos_acelerometro_hoy", 0);
        // Sumar el nuevo paso
        int nuevosPasos = pasosGuardados + 1;
        prefs.edit().putInt("pasos_acelerometro_hoy", nuevosPasos).apply();
        // Mandar a la pantalla (Sumando los simulados del cheat mode)
        int pasosSimulados = prefs.getInt("pasos_extra_simulados", 0);
        homeViewModel.updateSteps(nuevosPasos + pasosSimulados);
    }

    /**
     * Actualizar los pasos totales.
     */
    private void actualizarPasosTotales() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);
        // Pasos Manuales/Simulados
        int pasosExtra = prefs.getInt("pasos_extra_simulados", 0);
        // Pasos Acelerómetro
        int pasosAcelerometro = prefs.getInt("pasos_acelerometro_hoy", 0);
        // Pasos Hardware (Solo si existe el sensor)
        int pasosRealesHoy = 0;
        int pasosBase = prefs.getInt("pasos_base_dia", -1);
        if (pasosBase != -1 && ultimoValorSensor > 0) {
            pasosRealesHoy = ultimoValorSensor - pasosBase;
            if (pasosRealesHoy < 0) pasosRealesHoy = 0;
        }
        int totalAVisualizar = pasosRealesHoy + pasosAcelerometro + pasosExtra;
        homeViewModel.updateSteps(totalAVisualizar);
    }

    /**
     * Verificar reinicio diario.
     */
    private void verificarReinicioDiario() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);
        String fechaHoy = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String ultimaFechaGuardada = prefs.getString("fecha_ultimo_conteo", "");
        if (!ultimaFechaGuardada.equals(fechaHoy)) {
            prefs.edit().putString("fecha_ultimo_conteo", fechaHoy).apply();
            prefs.edit().putInt("pasos_base_dia", -1).apply(); // Lo marcamos para recalcular
            prefs.edit().putInt("pasos_acelerometro_hoy", 0).apply();
            prefs.edit().putInt("pasos_extra_simulados", 0).apply();
            Toast.makeText(getContext(), "¡Nuevo día! Contadores a cero.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Iniciar GPS.
     */
    private void iniciarGPS() {
        android.util.Log.d("RECONFIT_DEBUG", "GPS: Intentando iniciar actualizaciones..."); // <--- LOG

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            android.util.Log.d("RECONFIT_DEBUG", "GPS: Permiso CONCEDIDO. Solicitando updates..."); // <--- LOG
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, android.os.Looper.getMainLooper());

        } else {
            android.util.Log.e("RECONFIT_DEBUG", "GPS: ¡Permiso DENEGADO! No puedo iniciar."); // <--- LOG DE ERROR
        }
    }

    /**
     * Detener GPS.
     */
    private void detenerGPS() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

}
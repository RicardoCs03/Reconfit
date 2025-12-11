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
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment implements SensorEventListener {
    //Variables/Objetos/Constantes del sensor:
    private TextView dayNightStatusTextView;
    private TextView tvHabitDayName;
    private TextView tvHabitDayDesc;
    private TextView tvPasos;
    private ProgressBar progressBarPasos;
    private CardView cardPasos;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor stepSensor;
    private boolean isSensorPresent = false;
    // Variables para Acelerómetro (Lógica de Respaldo)
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

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    // Receptor que escucha el reloj del sistema
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, android.content.Intent intent) {
            // Cada minuto (TIME_TICK) o si cambias la hora manual (TIME_CHANGED)
            if (homeViewModel != null) {
                homeViewModel.actualizarMomentoPorHora();
            }
        }
    };

    @Override
    public void onResume(){
        super.onResume();
        if (lightSensor != null && sensorManager != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Iniciar escaneo de GPS (cada 5 segundos)
        iniciarGPS();

        // Actualizacion inmediata (Manual)
        if (homeViewModel != null) {
            homeViewModel.actualizarMomentoPorHora();
        }

        // Registrar Sensor de Pasos (Si existe)
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        // O Registrar Acelerómetro (Si no hay sensor de pasos)
        else if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Registrar el receptor de tiempo
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(android.content.Intent.ACTION_TIME_TICK);    // Cada minuto
        filter.addAction(android.content.Intent.ACTION_TIME_CHANGED); // Cambio manual
        filter.addAction(android.content.Intent.ACTION_TIMEZONE_CHANGED); // Cambio de zona
        requireContext().registerReceiver(timeReceiver, filter);
    }

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
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireActivity());

        // Lista de permisos que necesitamos
        List<String> permisosNecesarios = new ArrayList<>();

        // 1. Checar Ubicación
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permisosNecesarios.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // 2. Checar Actividad Física (Solo si es Android 10/Q o superior)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                permisosNecesarios.add(Manifest.permission.ACTIVITY_RECOGNITION);
            }
        }

        // 3. Si falta alguno, pedirlos todos de golpe
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
            // Se inicializa sensor de pasos // Plan A: Usar el sensor dedicado (Más preciso)
            android.util.Log.d("RECONFIT_DEBUG", "SENSOR: ¡Tenemos Chip de Pasos (Hardware)!");
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isSensorPresent = true;
            accelerometer = null; // No usamos acelerómetro
        } else {
            // Plan B: Usar Acelerómetro (Matemáticas)
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

        // ---- OBSERVADORES ----
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

        // Cheat mode code (Habilitar contador de pasos al tocar en la tarjeta)
        cardPasos.setOnClickListener(v -> {
            // Si la fecha cambió, este metodo pondrá 'pasos_extra_simulados' en 0 internamente.
            verificarReinicioDiario();

            // Abrimos los SP
            SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);

            // Obtenemos los datos actuales y los actualizamos
            int extraActuales = prefs.getInt("pasos_extra_simulados", 0);
            int nuevosExtra = extraActuales + 1;

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

        // Vincular el RecyclerView del XML
        rvSugerencias = view.findViewById(R.id.rv_habits_focused);

        // Configurar cómo se ven los items (Lista vertical)
        rvSugerencias.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));

        // Inicializar el Adaptador (Empieza vacío)
        habitsAdapter = new HabitsAdapter(new ArrayList<>(), new HabitsAdapter.OnHabitActionListener() {
            @Override
            public void onDelete(String habitId) {
                // Lógica de borrar (permitir borrar desde el Home)
                // homeViewModel.deleteHabit(habitId); (Si agregas ese método al VM)
            }
            @Override
            public void onToggle(String habitId, boolean isCompleted) {
                //Actualizamos en Firebase
                homeViewModel.updateHabitStatus(habitId, isCompleted);
                // Feedback opcional
                if (isCompleted) {
                    Toast.makeText(getContext(), "¡Bien hecho!", Toast.LENGTH_SHORT).show();
                }
                // El cambio en Firebase disparará automáticamente el listener en HomeViewModel,
                // se recargarán los hábitos, se ejecutará filtrarHabitos() y
                // como ahora isCompleted es true, ¡desaparecerá de la lista sola!
            }
        });
        rvSugerencias.setAdapter(habitsAdapter);

        // EL CEREBRO: Observar la lista filtrada
        homeViewModel.getFocusedHabits().observe(getViewLifecycleOwner(), listaFiltrada -> {
            // Si la lista es null o está vacía, podrías mostrar un texto de "No hay sugerencias"
            // Pero por ahora, solo actualizamos el adaptador
            if (listaFiltrada != null) {
                habitsAdapter.setHabits(listaFiltrada);
                habitsAdapter.notifyDataSetChanged();
            }
        });

        // Configurar la petición de GPS (Qué tan rápido queremos actualizaciones)
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // Actualizar cada 10 segundos
        locationRequest.setFastestInterval(5000); // O mínimo cada 5 segundos

        // Definir qué hacer cuando llega una nueva ubicación
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@androidx.annotation.NonNull com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult == null) {
                    android.util.Log.w("RECONFIT_DEBUG", "GPS: Callback ejecutado pero result es NULL");
                    return;
                }
                android.util.Log.d("RECONFIT_DEBUG", "GPS: ¡Llegaron " + locationResult.getLocations().size() + " ubicaciones!");
                for (android.location.Location location : locationResult.getLocations()) {
                    if (location != null) {
                        // ¡AQUÍ LLEGA EL DATO FRESCO!
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        float acc = location.getAccuracy();

                        // LOG CRÍTICO CON DATOS
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

    //Implementación de los métodos de SensorEventListener

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // LOG PARA PASOS (HARDWARE)
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            android.util.Log.d("RECONFIT_DEBUG", "PASOS (HW): Valor recibido del chip = " + event.values[0]);
        }

        // LOG PARA ACELERÓMETRO
        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Solo imprime si el movimiento es fuerte para no saturar el log
            double magnitude = Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]);
            if (magnitude > 11.0) {
                android.util.Log.d("RECONFIT_DEBUG", "ACELERÓMETRO: ¡Movimiento detectado! Mag=" + magnitude);
            }
        }

        // LOGICA SENSORES
        // Sensor de luz
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            // Pasamos el valor del sensor al ViewModel
            homeViewModel.processLightData(event.values[0]);
        }
        // Sensor de pasos
        else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            verificarReinicioDiario();

            int totalStepsSensor = (int) event.values[0];
            ultimoValorSensor = totalStepsSensor; // Actualizamos global

            // Usamos SharedPreferences para guardar el "Punto de Inicio" de forma permanente
            SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);
            // Obtenemos la base (ahora actualizada si cambió el día)
            int pasosBaseGuardados = prefs.getInt("pasos_base_dia", -1);

            // Si es la primera vez que corremos la app hoy (o se borraron los datos)
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
        // ACELERÓMETRO (Software - Plan B)
        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Obtenemos x, y, z
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calculamos la Magnitud del vector
            double magnitude = Math.sqrt(x * x + y * y + z * z);

            // Lógica de Detección de Pico
            // Si el golpe supera el umbral (13.0) Y ha pasado tiempo suficiente desde el último paso
            if (magnitude > MAGNITUDE_THRESHOLD) {
                long currentTime = System.currentTimeMillis();

                // "Debounce": Solo contar si pasaron 500ms (medio segundo) entre pasos
                // Esto evita que cuente vibraciones
                if (currentTime - lastStepTime > 500) {
                    lastStepTime = currentTime;

                    // Antes de sumar el paso, verificamos si ya amaneció
                    verificarReinicioDiario();

                    // ¡Paso detectado!
                    pasosAcumuladosAcelerometro++;
                    // Guardar y Actualizar (Reusamos tu lógica de SharedPreferences)
                    guardarYActualizarPasosManuales();
                }
            }
        }
    }

    // Metodo auxiliar para guardar pasos con ACELEROMETRO
    private void guardarYActualizarPasosManuales() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);

        // Recuperar los pasos "reales" acumulados hoy (si ya había)
        // Nota: Reusamos la variable 'pasos_base_dia' pero ahora la usaremos como "Pasos Acumulados Hoy"
        // en el caso del acelerómetro, para no complicar la lógica híbrida.
        int pasosGuardados = prefs.getInt("pasos_acelerometro_hoy", 0);

        // Sumar el nuevo paso
        int nuevosPasos = pasosGuardados + 1;
        prefs.edit().putInt("pasos_acelerometro_hoy", nuevosPasos).apply();

        // 3. Mandar a la pantalla (Sumando los simulados del cheat mode)
        int pasosSimulados = prefs.getInt("pasos_extra_simulados", 0);
        homeViewModel.updateSteps(nuevosPasos + pasosSimulados);
    }

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

        // Si estamos usando Acelerómetro, 'pasosHardware' será 0 (porque no entra al if del sensor).
        // Si estamos usando Hardware, 'pasosAcelerometro' será 0 (porque no entra al if del acelerómetro).
        // Es seguro sumarlos todos.
        int totalAVisualizar = pasosRealesHoy + pasosAcelerometro + pasosExtra;

        // 4. Mandar al ViewModel
        homeViewModel.updateSteps(totalAVisualizar);
    }

    // Reinicio del conteo de pasos diario
    private void verificarReinicioDiario() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("ReconFitData", Context.MODE_PRIVATE);

        // Obtener fechas
        String fechaHoy = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String ultimaFechaGuardada = prefs.getString("fecha_ultimo_conteo", "");

        // 2. Comparar
        if (!ultimaFechaGuardada.equals(fechaHoy)) {
            // ¡ES UN NUEVO DÍA!

            // A. Guardamos la nueva fecha
            prefs.edit().putString("fecha_ultimo_conteo", fechaHoy).apply();

            // B. Reiniciamos TODOS los contadores
            // - Para el sensor de hardware: la base se vuelve el valor actual (se ajusta en el onSensorChanged)
            prefs.edit().putInt("pasos_base_dia", -1).apply(); // Lo marcamos para recalcular

            // - Para el acelerómetro y cheat mode: vuelven a cero absoluto
            prefs.edit().putInt("pasos_acelerometro_hoy", 0).apply();
            prefs.edit().putInt("pasos_extra_simulados", 0).apply();

            // Feedback (Opcional)
            Toast.makeText(getContext(), "¡Nuevo día! Contadores a cero.", Toast.LENGTH_SHORT).show();
        }
    }

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

    private void detenerGPS() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

}
package com.example.reconfit.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.reconfit.model.Habit;
import com.example.reconfit.model.Zone;
import com.example.reconfit.repository.HabitRepository;
import com.example.reconfit.repository.ZoneRepository;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {
    // Define un LiveData para exponer el estado final a la View
    private MutableLiveData<String> dayNightStatus = new MutableLiveData<>();
    private MutableLiveData<String> recommendationText = new MutableLiveData<>();

    // Definir listas para el contador de pasos
    private MutableLiveData<Integer> steps = new MutableLiveData<>(0);

    // Esta lista es la que observará el HomeFragment para llenar el RecyclerView
    private final MutableLiveData<List<Habit>> focusedHabits = new MutableLiveData<>();

    // Cache local: Guardamos todos los hábitos aquí para filtrarlos rápido en memoria
    private List<Habit> allHabitsCache = new ArrayList<>();
    private HabitRepository habitRepository;
    private List<Zone> allZonesCache = new ArrayList<>();
    private ZoneRepository zoneRepository;

    // Variables de contexto actual
    private String currentLugar = "Cualquiera"; // Por defecto lugar (GPS)
    private String currentMomento = "Mañana";   // Por defecto (Reloj)
    private boolean isEnvironmentDark = false; // Por defecto (Sensor Luz)

    // Las constantes de la lógica se mueven aquí
    private static final float LIGHT_THRESHOLD = 20.0f;

    // Rangos horarios (0-23h)
    private static final int HOUR_MORNING_START = 5;
    private static final int HOUR_AFTERNOON_START = 12;
    private static final int HOUR_NIGHT_START = 19; // 7 PM

    public HomeViewModel(@NonNull Application application) {
        super(application);
        habitRepository = new HabitRepository();
        // Al iniciar, descargamos todos los hábitos de Firebase
        cargarTodosLosHabitos();

        zoneRepository = new ZoneRepository();
        cargarZonas();

        actualizarMomentoPorHora();
    }

    // --- GETTERS (Lo que ve el Fragment) ---
    public LiveData<String> getDayNightStatus() { return dayNightStatus; }
    public LiveData<String> getRecommendationText() { return recommendationText; }
    public LiveData<Integer> getSteps() { return steps; }
    public LiveData<List<Habit>> getFocusedHabits() { return focusedHabits; }

    // LÓGICA 1: RELOJ (Define el "Momento" para filtrar hábitos)
    public void actualizarMomentoPorHora() {
        // Obtener hora actual (0 a 23)
        Calendar cal = Calendar.getInstance();
        int hora = cal.get(Calendar.HOUR_OF_DAY);

        String nuevoMomento;

        if (hora >= HOUR_MORNING_START && hora < HOUR_AFTERNOON_START) {
            nuevoMomento = "Mañana";
        } else if (hora >= HOUR_AFTERNOON_START && hora < HOUR_NIGHT_START) {
            nuevoMomento = "Tarde";
        } else {
            nuevoMomento = "Noche";
        }

        // Si cambió el momento (ej. pasamos de Tarde a Noche), actualizamos filtro
        if (!this.currentMomento.equalsIgnoreCase(nuevoMomento)) {
            this.currentMomento = nuevoMomento;
            filtrarHabitos(); // <--- El filtro depende SOLO de la hora y lugar
        }

        // Siempre recalculamos la matriz de recomendación (texto)
        calcularMatrizDeRecomendaciones();
    }

    // LÓGICA 2: SENSOR DE LUZ (Define "Oscuridad" para la recomendación)
    public void processLightData(float luxValue) {
        boolean isNowDark = luxValue < LIGHT_THRESHOLD;

        // Solo si la luz cambia drásticamente actualizamos la UI
        if (this.isEnvironmentDark != isNowDark) {
            this.isEnvironmentDark = isNowDark;
            calcularMatrizDeRecomendaciones();
        }

        // NOTA: La luz YA NO dispara filtrarHabitos(). Eso lo hace la hora.
    }

    //LÓGICA 3: MATRIZ DE RECOMENDACIONES (Tabla de Verdad)
    private void calcularMatrizDeRecomendaciones() {
        // Inputs: currentMomento (Mañana/Tarde/Noche) + isEnvironmentDark (Sí/No)

        String keyColor; // Para el color de fondo (NIGHT_SCHEDULED_DARK, etc)
        String frase;    // El texto personalizado

        if (currentMomento.equals("Mañana")) {
            if (isEnvironmentDark) {
                // Caso: Mañana pero oscuro (madrugada o cuarto cerrado)
                keyColor = "DARK_INTERIOR";
                frase = "Empieza tu día con calma.";
            } else {
                // Caso: Mañana y claro (Normal)
                keyColor = "DAYLIGHT";
                frase = "¡Buenos días! Es hora de activarse.";
            }
        }
        else if (currentMomento.equals("Tarde")) {
            if (isEnvironmentDark) {
                // Caso: Tarde pero oscuro (Nublado o cine/oficina cerrada)
                keyColor = "DARK_INTERIOR";
                frase = "Poca luz detectada. estás en un interior.";
            } else {
                // Caso: Tarde y claro (Normal)
                keyColor = "DAYLIGHT";
                frase = "Buenas tardes. Mantén el enfoque.";
            }
        }
        else { // Noche
            if (isEnvironmentDark) {
                // Caso: Noche y oscuro (Normal)
                keyColor = "NIGHT_SCHEDULED_DARK";
                frase = "Modo Descanso. Desconecta y relájate.";
            } else {
                // Caso: Noche pero mucha luz (Lámparas encendidas)
                keyColor = "DAYLIGHT"; // O un estado intermedio si quisieras
                frase = "Es de noche pero hay mucha luz. ¡A descansar pronto!";
            }
        }

        // Enviamos los resultados a la UI
        dayNightStatus.setValue(keyColor);
        recommendationText.setValue(frase);
    }

    // --- Metodos de Soporte ---
    private void cargarTodosLosHabitos() {
        // Usamos addSnapshotListener para tener actualizaciones en tiempo real
        habitRepository.getHabitsCollection().addSnapshotListener((value, error) -> {
            if (value != null) {
                allHabitsCache.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Habit h = doc.toObject(Habit.class);
                    h.setId(doc.getId()); // Guardamos ID por si acaso
                    allHabitsCache.add(h);
                }
                // Una vez que llegan los datos, aplicamos el filtro con el contexto actual
                filtrarHabitos();
            }
        });
    }

    private void cargarZonas() {
        zoneRepository.getAllZones().observeForever(zones -> {
            if (zones != null) {
                allZonesCache = zones;
            }
        });
    }

    //Metodo para actualizar los pasos
    public void updateSteps(int stepCount) {
        steps.setValue(stepCount);
    }

    public void updateHabitStatus(String habitId, boolean isCompleted) {
        habitRepository.updateHabitStatus(habitId, isCompleted);
    }

    public void setUbicacionActual(String nuevoLugar) {
        if (!this.currentLugar.equals(nuevoLugar)) {
            this.currentLugar = nuevoLugar;
            filtrarHabitos();
        }
    }

    // --- LÓGICA MATEMÁTICA REAL (GPS) ---
    public void verificarUbicacionReal(double latActual, double lonActual) {
        String lugarDetectado = "Cualquiera"; // Si no estoy cerca de nada, soy libre

        // Distancia mínima para considerar que "llegaste"
        float radioDeteccionMetros = 100.0f;

        for (Zone z : allZonesCache) {
            float[] results = new float[1];
            // Fórmula nativa de Android para distancia entre dos coordenadas
            android.location.Location.distanceBetween(
                    latActual, lonActual,
                    z.getLatitude(), z.getLongitude(),
                    results
            );

            float distanciaEnMetros = results[0];

            if (distanciaEnMetros <= radioDeteccionMetros) {
                lugarDetectado = z.getName(); // ¡Bingo! Estás en "Casa Kevin" o "Gym"
                break; // Ya encontramos uno, dejamos de buscar
            }
        }

        // Actualizamos la variable y filtramos
        // (Solo si cambió para no parpadear)
        if (!this.currentLugar.equalsIgnoreCase(lugarDetectado)) {
            this.currentLugar = lugarDetectado;
            filtrarHabitos(); // <--- El cerebro actualiza la lista
        }
    }

    // --- EL CEREBRO: FILTRADO ---
    private void filtrarHabitos() {
        List<Habit> sugerencias = new ArrayList<>();

        for (Habit h : allHabitsCache) {
            // Lógica de Coincidencia (Match)
            // A. LUGAR: ¿El hábito es para "Cualquiera" O coincide con donde estoy?
            // Usamos equalsIgnoreCase para evitar errores de mayúsculas/minúsculas
            boolean matchLugar = "Cualquiera".equalsIgnoreCase(h.getContextPlace()) ||
                    currentLugar.equalsIgnoreCase(h.getContextPlace());
            // B. MOMENTO: ¿El hábito es para "Cualquiera" O coincide con la Luz/Hora?
            boolean matchMomento = "Cualquiera".equalsIgnoreCase(h.getContextTime()) ||
                    currentMomento.equalsIgnoreCase(h.getContextTime());

            // Solo si cumple las condiciones, entra a la lista "En Foco"
            if (matchLugar && matchMomento && !h.isCompleted()) {
                // Solo si cumple AMBAS condiciones, entra a la lista "En Foco"
                sugerencias.add(h);
            }
        }
        // Publicamos la lista filtrada para que el Fragment la pinte
        focusedHabits.setValue(sugerencias);
    }

}

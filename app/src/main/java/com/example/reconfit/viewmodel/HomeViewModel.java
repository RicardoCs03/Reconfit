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
    private String currentLugar = "Cualquiera"; // Por defecto
    private String currentMomento = "Mañana";   // Por defecto

    // Las constantes de la lógica se mueven aquí
    private static final float LIGHT_THRESHOLD = 20.0f;
    private static final int NIGHT_START_HOUR = 19;
    private static final int NIGHT_END_HOUR = 5;

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
    public LiveData<Integer> getSteps() { return steps; }
    public LiveData<List<Habit>> getFocusedHabits() { return focusedHabits; }

    // --- LÓGICA DE CARGA DE DATOS ---
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

    public void processLightData(float luxValue) {

        // ******* TODA LA LÓGICA DEL MÉTODO updateDayNightStatus VA AQUÍ *******

        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean isDark = luxValue < LIGHT_THRESHOLD;
        boolean isScheduledNight = (currentHour >= NIGHT_START_HOUR || currentHour < NIGHT_END_HOUR);

        String statusKey;
        String nuevoMomentoDetectado;

        if (isDark && isScheduledNight) {
            statusKey = "NIGHT_SCHEDULED_DARK";
            nuevoMomentoDetectado = "Noche"; // Coincide con tu Spinner
        } else if (isDark && !isScheduledNight) {
            statusKey = "DARK_INTERIOR";
            nuevoMomentoDetectado = "Noche"; // Asumimos ambiente relajado
        } else {
            statusKey = "DAYLIGHT";
            nuevoMomentoDetectado = "Mañana"; // Coincide con tu Spinner
        }

        dayNightStatus.setValue(statusKey);

        // DETECTAR CAMBIO DE CONTEXTO
        // Solo refiltramos si la situación cambió para no gastar recursos
        if (!nuevoMomentoDetectado.equals(currentMomento)) {
            currentMomento = nuevoMomentoDetectado;
            filtrarHabitos();
        }

    }
    // Vamos a priorizar la Hora del Reloj y usar la luz solo como apoyo.
    public void actualizarMomentoPorHora() {
        // Obtener hora actual (0 a 23)
        Calendar cal = Calendar.getInstance();
        int hora = cal.get(Calendar.HOUR_OF_DAY);

        String nuevoMomento;

        if (hora >= 5 && hora < 12) {
            nuevoMomento = "Mañana";
        } else if (hora >= 12 && hora < 19) {
            nuevoMomento = "Tarde"; // <--- Agregamos Tarde para más precisión
        } else {
            nuevoMomento = "Noche";
        }

        // Actualizar y Filtrar
        if (!this.currentMomento.equalsIgnoreCase(nuevoMomento)) {
            this.currentMomento = nuevoMomento;
            filtrarHabitos();
        }
    }

    //Metodo para actualizar los pasos
    public void updateSteps(int stepCount) {
        steps.setValue(stepCount);
    }

    // Metodo para cambiar la ubicación (lo usaremos desde el GPS más adelante)
    public void setUbicacionActual(String nuevoLugar) {
        if (!this.currentLugar.equals(nuevoLugar)) {
            this.currentLugar = nuevoLugar;
            filtrarHabitos();
        }
    }

    public void updateHabitStatus(String habitId, boolean isCompleted) {
        habitRepository.updateHabitStatus(habitId, isCompleted);
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
                sugerencias.add(h);
            }
        }

        // Publicamos la lista filtrada para que el Fragment la pinte
        focusedHabits.setValue(sugerencias);
    }

}

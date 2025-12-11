package com.example.reconfit.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.reconfit.model.Zone;
import com.example.reconfit.repository.ZoneRepository;

import java.util.ArrayList;
import java.util.List;

public class ZonesViewModel extends ViewModel {
    private ZoneRepository repository;
    private LiveData<List<Zone>> zonesList = new MutableLiveData<>();
    // Estado para notificar si estamos en alguna de las zonas
    private MutableLiveData<String> zoneStatus = new MutableLiveData<>("Obteniendo Ubicación...");

    public LiveData<List<Zone>> getAllZones() {
        return zonesList;
    }

    public LiveData<String> getZoneStatus() {
        return zoneStatus;
    }

    public LiveData<List<String>> getNombresDeZonas() { return repository.getNombresDeZonas();
    }

    public ZonesViewModel() {
        repository = new ZoneRepository();
        zonesList = repository.getAllZones();
    }

    public void saveCurrentZone(Zone zone) {
        repository.saveZone(zone);
    }

    /**
     * Lógica clave: Calcula la distancia y actualiza el LiveData de estado.
     */
    public void checkDistanceToZones(double currentLat, double currentLong) {
        List<Zone> zones = zonesList.getValue();
        if (zones == null || zones.isEmpty()) {
            zoneStatus.setValue("No hay zonas guardadas.");
            return;
        }

        // CICLO CLAVE: Recorre la lista de zonas
        for (Zone zone : zones) {
            float[] results = new float[1];

            // Método estático de Android para calcular la distancia entre dos puntos (en metros)
            android.location.Location.distanceBetween(
                    currentLat, currentLong,
                    zone.getLatitude(), zone.getLongitude(),
                    results
            );

            float distanceInMeters = results[0];

            if (distanceInMeters < zone.getRadiusMeters()) {
                zoneStatus.setValue("¡Estás en " + zone.getName() + "!");
                return; // Encontró la zona más cercana, sale del ciclo
            }
        }

        zoneStatus.setValue("No estás cerca de ninguna zona guardada.");
    }


    public void deleteZone(Zone zone) {
        repository.deleteZone(zone);
    }
}

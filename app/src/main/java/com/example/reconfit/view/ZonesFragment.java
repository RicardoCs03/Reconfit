package com.example.reconfit.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reconfit.R;
import com.example.reconfit.model.Zone;
import com.example.reconfit.viewmodel.ZonesViewModel;
import com.google.firebase.Timestamp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener; // NUEVA IMPORTACIÓN
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

// Implementamos LocationListener
public class ZonesFragment extends Fragment implements LocationListener, ZonesAdapter.ZoneActionListener {
    private ZonesViewModel viewModel;
    private TextView statusTextView;
    private EditText zoneNameEditText;
    private Button saveLocationButton;
    private LocationManager locationManager;
    // Almacenamos la última ubicación obtenida
    private Location currentLocation = null;
    private RecyclerView zonesRecyclerView;
    private ZonesAdapter zonesAdapter;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "ZonesFragment";
    // ... Constructor y onCreateView iguales ...

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_zones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ZonesViewModel.class);

        // Inicializar LocationManager
        locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

        statusTextView = view.findViewById(R.id.tv_zone_status);
        saveLocationButton = view.findViewById(R.id.btn_save_location);
        zoneNameEditText = view.findViewById(R.id.et_zone_name);


        // 1. Observar el estado de la zona
        viewModel.getZoneStatus().observe(getViewLifecycleOwner(), status -> {
            statusTextView.setText(status);
        });

        // 2. Manejar el clic para guardar la ubicación
        saveLocationButton.setOnClickListener(v -> saveCurrentLocationAsZone());

        // 3. Verificar permisos y empezar a escuchar la ubicación
        checkLocationPermissionsAndStartListening();
        // --- INICIALIZACIÓN DEL RECYCLERVIEW (NUEVO BLOQUE) ---
        zonesRecyclerView = view.findViewById(R.id.rv_zones);
        // Inicializamos el adaptador con una lista vacía y el propio fragmento como listener
        zonesAdapter = new ZonesAdapter(new ArrayList<>(), this);
        zonesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        zonesRecyclerView.setAdapter(zonesAdapter);
        // 4. Observar la lista de zonas desde el ViewModel
        viewModel.getAllZones().observe(getViewLifecycleOwner(), zones -> {
            // Cuando la lista de zonas cambie, actualizamos el adaptador
            zonesAdapter.setZones(zones);
        });
    }

    // Nuevo método: Comprobar permisos e iniciar la escucha activa
    private void checkLocationPermissionsAndStartListening() {
        if (!checkLocationPermissions()) {
            requestLocationPermissions();
            return;
        }

        // Si los permisos están, iniciamos la escucha
        startLocationUpdates();
    }

    // Método para iniciar la escucha de actualizaciones de ubicación
    private void startLocationUpdates() {
        if (!checkLocationPermissions()) return;

        // Comprobación final de permisos (requerida por el IDE)
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            // Intentamos registrar al LocationListener usando el proveedor GPS
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000, // minTime: 2 segundos
                    10,   // minDistance: 10 metros
                    this // El fragmento es el listener
            );
            statusTextView.setText("Escuchando ubicación (GPS)...");

            // Intento secundario con el proveedor de Red, si el GPS no está activo
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        2000,
                        10,
                        this
                );
                statusTextView.setText("Escuchando ubicación (Red)...");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al solicitar actualizaciones de ubicación: " + e.getMessage());
            statusTextView.setText("Error al iniciar la ubicación.");
        }
    }

    // Detenemos las actualizaciones cuando el fragmento se destruye o se pausa
    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reiniciamos la escucha si volvemos al fragmento
        startLocationUpdates();
    }


    // Método que maneja la obtención de la ubicación y el guardado
    private void saveCurrentLocationAsZone() {
        if (currentLocation != null) {
            String zoneName = zoneNameEditText.getText().toString().trim();
            // Añadimos una validación para asegurar que el nombre no esté vacío**
            if (zoneName.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, ingresa un nombre para la zona.", Toast.LENGTH_SHORT).show();
                return;
            }
            Zone newZone = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                newZone = new Zone(
                        null, // Firestore generará el ID
                        zoneName,
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude(),
                        100.0, // 100 metros de radio
                        new Timestamp(new Date())
                );
            }
            viewModel.saveCurrentZone(newZone);
            viewModel.checkDistanceToZones(currentLocation.getLatitude(), currentLocation.getLongitude());
            Toast.makeText(requireContext(), "Zona '" + zoneName + "' guardada.", Toast.LENGTH_SHORT).show();

            // Opcional: detenemos la escucha una vez que guardamos la ubicación
            locationManager.removeUpdates(this);

        } else {
            statusTextView.setText("Ubicación actual no disponible. Esperando señal...");
            Toast.makeText(requireContext(), "Esperando ubicación, por favor espera.", Toast.LENGTH_SHORT).show();
        }
    }

    // Métodos de LocationListener
    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Esta función se llama CADA VEZ que llega una nueva ubicación
        this.currentLocation = location;

        statusTextView.setText(
                String.format("Ubicación lista: %.4f, %.4f", location.getLatitude(), location.getLongitude())
        );

        // Llamamos a la lógica de verificación tan pronto como recibimos una ubicación
        viewModel.checkDistanceToZones(location.getLatitude(), location.getLongitude());

        // Podemos detener la escucha después de la primera ubicación exitosa si solo la necesitamos una vez
        // locationManager.removeUpdates(this);
    }

    // Otros métodos de LocationListener (pueden dejarse vacíos)
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.i(TAG, provider + " habilitado.");
    }
    @Override
    public void onProviderDisabled(@NonNull String provider) {
        statusTextView.setText("Proveedor de ubicación (" + provider + ") desactivado.");
    }

    // ... checkLocationPermissions() y onRequestPermissionsResult() quedan iguales ...

    private boolean checkLocationPermissions() {
        return requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        requestPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permisos concedidos, intenta obtener la ubicación de nuevo
                startLocationUpdates();
            } else {
                statusTextView.setText("Permiso de ubicación denegado. Las Zonas no funcionarán.");
            }
        }
    }
    @Override
    public void onZoneModify(Zone zone) {
        // Lógica para modificar la zona
        Toast.makeText(requireContext(), "Modificar: " + zone.getName(), Toast.LENGTH_SHORT).show();
        // **PENDIENTE:** Aquí implementarás la lógica real de modificación (e.g., abrir un diálogo).
    }

    @Override
    public void onZoneDelete(Zone zone) {
        //TODO: Eliminar la zona
        //viewModel.deleteZone(zone);
        Toast.makeText(requireContext(), "Eliminada: " + zone.getName(), Toast.LENGTH_SHORT).show();
        // **NOTA:** La actualización es automática porque el adaptador observa getAllZones()
    }
}
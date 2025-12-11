package com.example.reconfit.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat; // Importante
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.reconfit.R;
import com.example.reconfit.model.Zone;
import com.example.reconfit.utils.MapUtils;
import com.example.reconfit.viewmodel.ZonesViewModel;
import com.google.android.gms.location.FusedLocationProviderClient; // NUEVO
import com.google.android.gms.location.LocationCallback; // NUEVO
import com.google.android.gms.location.LocationRequest; // NUEVO
import com.google.android.gms.location.LocationResult; // NUEVO
import com.google.android.gms.location.LocationServices; // NUEVO
import com.google.firebase.Timestamp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
// Borramos LocationListener antiguo
// import android.location.LocationListener;
// import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;

// YA NO IMPLEMENTAMOS LocationListener, el FusedLocation usa un Callback
public class ZonesFragment extends Fragment implements ZonesAdapter.ZoneActionListener {

    private ZonesViewModel viewModel;
    private TextView statusTextView;
    private EditText zoneNameEditText;
    private Button saveLocationButton;
    private ImageView mapImageView;

    // --- CAMBIO: MOTOR MODERNO ---
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    // -----------------------------

    private Location currentLocation = null;
    private RecyclerView zonesRecyclerView;
    private ZonesAdapter zonesAdapter;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "ZonesFragment";
    private static final String MY_API_KEY = "AIzaSyC8UJwBn3aDCbrHBg2O7teLMWcx6HlibTg";

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

        // --- 1. INICIALIZAR MOTOR MODERNO (Igual que en HomeFragment) ---
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Configurar la petición (Alta precisión, cada 5 seg)
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        // Configurar qué hacer cuando llega la ubicación
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        // ¡AQUÍ LLEGAN LOS DATOS!
                        actualizarUbicacionUI(location);
                    }
                }
            }
        };
        // -------------------------------------------------------------

        mapImageView = view.findViewById(R.id.iv_static_map);
        statusTextView = view.findViewById(R.id.tv_zone_status);
        saveLocationButton = view.findViewById(R.id.btn_save_location);
        zoneNameEditText = view.findViewById(R.id.et_zone_name);

        // Observar estado
        viewModel.getZoneStatus().observe(getViewLifecycleOwner(), status -> {
            statusTextView.setText(status); // Cuidado: esto podría sobrescribir nuestros mensajes de GPS
        });

        saveLocationButton.setOnClickListener(v -> saveCurrentLocationAsZone());

        zonesRecyclerView = view.findViewById(R.id.rv_zones);
        zonesAdapter = new ZonesAdapter(new ArrayList<>(), this);
        zonesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        zonesRecyclerView.setAdapter(zonesAdapter);

        viewModel.getAllZones().observe(getViewLifecycleOwner(), zones -> {
            zonesAdapter.setZones(zones);
        });

        // Iniciar GPS al crear la vista
        checkLocationPermissionsAndStartListening();
    }

    // Método auxiliar para manejar la llegada de datos
    private void actualizarUbicacionUI(Location location) {
        this.currentLocation = location;

        Log.d("RECONFIT_DEBUG", "ZONES: Ubicación recibida: " + location.getLatitude() + ", " + location.getLongitude());

        statusTextView.setText(
                String.format("Ubicación lista: %.4f, %.4f", location.getLatitude(), location.getLongitude())
        );

        loadStaticMap(location.getLatitude(), location.getLongitude());

        // Calcular distancias con las zonas existentes
        viewModel.checkDistanceToZones(location.getLatitude(), location.getLongitude());
    }

    private void checkLocationPermissionsAndStartListening() {
        if (!checkLocationPermissions()) {
            requestLocationPermissions();
            return;
        }
        startLocationUpdates();
    }

    private void loadStaticMap(double lat, double lng) {
        if (mapImageView == null) return;
        String mapUrl = MapUtils.buildStaticMapUrl(lat, lng, MY_API_KEY);
        Glide.with(this)
                .load(mapUrl)
                .placeholder(R.drawable.ic_map)
                .error(R.drawable.ic_error)
                .centerCrop()
                .into(mapImageView);
    }

    // --- CAMBIO: USAR FUSEDLOCATION ---
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Log.d("RECONFIT_DEBUG", "ZONES: Iniciando actualizaciones de GPS...");
        statusTextView.setText("Buscando señal GPS...");

        // Solicitamos actualizaciones al cliente moderno
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, android.os.Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // Ciclo de vida: Detener al salir, iniciar al volver
    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkLocationPermissions()) {
            startLocationUpdates();
        }
    }

    private void saveCurrentLocationAsZone() {
        if (currentLocation != null) {
            String zoneName = zoneNameEditText.getText().toString().trim();
            if (zoneName.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, ingresa un nombre para la zona.", Toast.LENGTH_SHORT).show();
                return;
            }

            Zone newZone = null;
            // Quitamos la validación de versión de Android, Zone siempre se puede crear
            newZone = new Zone(
                    null,
                    zoneName,
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    100.0,
                    new Timestamp(new Date()),
                    null
            );

            viewModel.saveCurrentZone(newZone);

            // Recalculamos inmediatamente
            viewModel.checkDistanceToZones(currentLocation.getLatitude(), currentLocation.getLongitude());

            Toast.makeText(requireContext(), "Zona '" + zoneName + "' guardada.", Toast.LENGTH_SHORT).show();
            zoneNameEditText.setText(""); // Limpiar campo

        } else {
            statusTextView.setText("Ubicación actual no disponible. Esperando señal...");
            Toast.makeText(requireContext(), "Esperando ubicación...", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
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
                startLocationUpdates();
            } else {
                statusTextView.setText("Permiso denegado.");
            }
        }
    }

    @Override
    public void onZoneDelete(Zone zone) {
        viewModel.deleteZone(zone);
        Toast.makeText(requireContext(), "Eliminada: " + zone.getName(), Toast.LENGTH_SHORT).show();
    }
}
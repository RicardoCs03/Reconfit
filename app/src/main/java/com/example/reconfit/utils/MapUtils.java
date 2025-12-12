package com.example.reconfit.utils;
import java.util.Locale;

/**
 * Clase de utilidad para construir URLs para Static Maps API.
 */
public class MapUtils {

    /**
     * Construye la URL para la Static Maps API.
     * @param lat La latitud del punto.
     * @param lng La longitud del punto.
     * @param apiKey La clave de API de Google Maps.
     * @return La URL completa de la imagen estática.
     */
    public static String buildStaticMapUrl(double lat, double lng, String apiKey) {
        // Formatear las coordenadas a String con Locale para asegurar el punto decimal
        String center = String.format(Locale.US, "%f,%f", lat, lng);
        // El formato de markers es: color:red|LAT,LNG
        // El '|' se reemplaza por %7C en la URL encoding
        String markers = String.format(Locale.US, "color:red%%7C%f,%f", lat, lng);
        String size = "600x300"; // Tamaño de la imagen
        int zoom = 15; // Nivel de zoom
        return "https://maps.googleapis.com/maps/api/staticmap" +
                "?center=" + center +
                "&zoom=" + zoom +
                "&size=" + size +
                "&markers=" + markers +
                "&key=" + apiKey;
    }
}
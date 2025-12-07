package com.example.reconfit.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Calendar;

public class HomeViewModel extends ViewModel {
    // Define un LiveData para exponer el estado final a la View
    private MutableLiveData<String> dayNightStatus = new MutableLiveData<>();
    public LiveData<String> getDayNightStatus() {
        return dayNightStatus;
    }

    // Las constantes de la lógica se mueven aquí
    private static final float LIGHT_THRESHOLD = 20.0f;
    private static final int NIGHT_START_HOUR = 20;
    private static final int NIGHT_END_HOUR = 7;

    public void processLightData(float luxValue) {

        // ******* TODA LA LÓGICA DEL MÉTODO updateDayNightStatus VA AQUÍ *******

        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean isDark = luxValue < LIGHT_THRESHOLD;
        boolean isScheduledNight = (currentHour >= NIGHT_START_HOUR || currentHour < NIGHT_END_HOUR);

        String statusKey;

        if (isDark && isScheduledNight) {
            statusKey = "NIGHT_SCHEDULED_DARK";
        } else if (isDark && !isScheduledNight) {
            statusKey = "DARK_INTERIOR";
        } else {
            statusKey = "DAYLIGHT";
        }

        // Establecer el valor, lo que notifica al Fragment
        dayNightStatus.setValue(statusKey);
    }
}

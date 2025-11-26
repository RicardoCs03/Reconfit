package com.example.reconfit.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.reconfit.model.Habit;
import com.example.reconfit.repository.HabitRepository;
import java.util.List;

public class HabitsViewModel extends ViewModel {

    // Variable LiveData que mantendrá la lista de hábitos
    private MutableLiveData<List<Habit>> habitsList;
    private HabitRepository repository;

    public HabitsViewModel() {
        repository = new HabitRepository();
        habitsList = new MutableLiveData<>();

        // Carga inicial de datos
        loadHabits();
    }

    // Expone el LiveData a la Vista (Fragment) para que pueda "observar" los cambios
    public LiveData<List<Habit>> getHabitsList() {
        return habitsList;
    }

    // Método que llama al Repositorio para cargar los datos
    private void loadHabits() {
        // Por ahora, cargaremos una lista de prueba
        // Más adelante, este método usará el repository.getHabits() para leer de Firebase

        // Lógica de prueba:
        // List<Habit> testList = // ... crear lista de prueba
        // habitsList.setValue(testList); 
    }

    // Método de acción de usuario (ej. añadir un hábito)
    public void addNewHabit(Habit newHabit) {
        repository.addHabit(newHabit);
        // Después de añadir, recargar o actualizar la lista de LiveData
        // loadHabits(); 
    }
}
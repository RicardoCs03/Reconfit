package com.example.reconfit.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.reconfit.model.Habit;
import com.example.reconfit.repository.HabitRepository;
import java.util.List;
import com.google.firebase.firestore.Query;

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

    // Metodo que llama al Repositorio para cargar los datos
    private void loadHabits() {
        // Obtenemos la colección del repositorio
        // Ordenamos por fecha de creación (descendente) para ver los nuevos arriba
        repository.getHabitsCollection()
                //.orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Hubo un error, aquí podrías loguearlo
                        error.printStackTrace();
                        return;
                    }

                    if (value != null) {
                        // ¡Magia! Convertimos los documentos de Firebase a objetos Java "Habit"
                        List<Habit> habits = value.toObjects(Habit.class);

                        // Guardamos los IDs de los documentos por si queremos borrar/editar luego
                        for (int i = 0; i < habits.size(); i++) {
                            habits.get(i).setId(value.getDocuments().get(i).getId());
                        }

                        // Actualizamos el LiveData. Esto avisará automáticamente al Fragment.
                        habitsList.setValue(habits);
                    }
                });
    }

    // Metodo de acción de usuario (ej. añadir un hábito)
    public void addNewHabit(Habit newHabit) {
        repository.saveHabit(newHabit);
        // Después de añadir, recargar o actualizar la lista de LiveData
        loadHabits();
    }
}
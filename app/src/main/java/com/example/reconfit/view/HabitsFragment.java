package com.example.reconfit.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reconfit.R; // Asegúrate de que tu R sea correcto
import com.example.reconfit.viewmodel.HabitsViewModel;
import com.example.reconfit.model.Habit;

import java.util.ArrayList;
import java.util.List;

public class HabitsFragment extends Fragment {

    private HabitsViewModel habitsViewModel;
    private RecyclerView recyclerView;
    // Debes crear un Adaptador personalizado (HabitsAdapter)
    private HabitsAdapter habitsAdapter;

    public HabitsFragment() {
        // Constructor público vacío requerido
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Infla el layout para este fragmento (fragment_habits.xml)
        return inflater.inflate(R.layout.fragment_habits, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inicializar el ViewModel
        // El ViewModelProvider asocia el ViewModel con el ciclo de vida del Fragment.
        habitsViewModel = new ViewModelProvider(this).get(HabitsViewModel.class);

        // 2. Inicializar el RecyclerView (de fragment_habits.xml)
        recyclerView = view.findViewById(R.id.recycler_view_habits);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 3. Inicializar el Adaptador
        habitsAdapter = new HabitsAdapter(new ArrayList<>(), new HabitsAdapter.OnHabitActionListener() {
            @Override
            public void onDelete(String habitId) {
                // Lógica de borrar (permitir borrar desde el Home)
                habitsViewModel.deleteHabit(habitId);
                Toast.makeText(getContext(), "Hábito eliminado", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onToggle(String habitId, boolean isCompleted) {
                // Solo avisamos al ViewModel que cambie el estado en Firebase.
                habitsViewModel.updateHabitStatus(habitId, isCompleted);
            }

        });

        recyclerView.setAdapter(habitsAdapter);

        // 4. Observar el LiveData
        habitsViewModel.getHabitsList().observe(getViewLifecycleOwner(), newHabitsList -> {
            // Este código se ejecuta CADA VEZ que la lista de hábitos cambie en el ViewModel

            // Actualizar la UI:
            if (newHabitsList != null) {
                habitsAdapter.setHabits(newHabitsList); // Metodo que crearás en el Adaptador
                // Por ahora, solo imprime para verificar la conexión
                Log.d("HabitsFragment", "Hábitos recibidos: " + newHabitsList.size());
            }
        });

        // Configurar el FloatingActionButton (fab_add_habit) para añadir un hábito
        view.findViewById(R.id.fab_add_habit).setOnClickListener(v -> {
        //     Aquí podrías abrir un diálogo para pedir el nombre del nuevo hábito
            startActivity(new android.content.Intent(getActivity(), CreateHabitActivity.class));
        //     Y luego llamar: habitsViewModel.addNewHabit(new Habit(...));
        });
    }
}
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

/**
 * Fragmento para mostrar la lista de hábitos.
 */
public class HabitsFragment extends Fragment {
    private HabitsViewModel habitsViewModel;
    private RecyclerView recyclerView;
    private HabitsAdapter habitsAdapter;

    public HabitsFragment() {

    }

    /**
     * Se llama cuando se crea la vista del fragmento.
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return La vista del fragmento.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Infla el layout para este fragmento (fragment_habits.xml)
        return inflater.inflate(R.layout.fragment_habits, container, false);
    }

    /**
     * Se llama cuando la vista del fragmento ha sido creada.
     * @param view
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        habitsViewModel = new ViewModelProvider(this).get(HabitsViewModel.class);
        recyclerView = view.findViewById(R.id.recycler_view_habits);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        habitsAdapter = new HabitsAdapter(new ArrayList<>(), new HabitsAdapter.OnHabitActionListener() {

            /**
             * Eliminar un hábito.
             * @param habitId
             */
            @Override
            public void onDelete(String habitId) {
                habitsViewModel.deleteHabit(habitId);
                Toast.makeText(getContext(), "Hábito eliminado", Toast.LENGTH_SHORT).show();
            }

            /**
             * Cambiar el estado de un hábito.
             * @param habitId
             * @param isCompleted
             */
            @Override
            public void onToggle(String habitId, boolean isCompleted) {
                // Solo avisamos al ViewModel que cambie el estado en Firebase.
                habitsViewModel.updateHabitStatus(habitId, isCompleted);
            }
        });
        recyclerView.setAdapter(habitsAdapter);
        habitsViewModel.getHabitsList().observe(getViewLifecycleOwner(), newHabitsList -> {
            if (newHabitsList != null) {
                habitsAdapter.setHabits(newHabitsList);
                Log.d("HabitsFragment", "Hábitos recibidos: " + newHabitsList.size());
            }
        });
        // Configurar el FloatingActionButton (fab_add_habit) para añadir un hábito
        view.findViewById(R.id.fab_add_habit).setOnClickListener(v -> {
        //     Aquí podrías abrir un diálogo para pedir el nombre del nuevo hábito
            startActivity(new android.content.Intent(getActivity(), CreateHabitActivity.class));
        });
    }
}
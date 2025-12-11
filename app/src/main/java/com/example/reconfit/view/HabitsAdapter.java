package com.example.reconfit.view;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.reconfit.R;
import com.example.reconfit.model.Habit;
import java.util.ArrayList;
import java.util.List;

public class HabitsAdapter extends RecyclerView.Adapter<HabitsAdapter.HabitViewHolder> {

    private List<Habit> habitList = new ArrayList<>();
    private OnHabitActionListener actionListener;

    // Metodo para actualizar la lista cuando el ViewModel nos mande datos nuevos
    public void setHabits(List<Habit> habits) {
        this.habitList = habits;
        notifyDataSetChanged(); // Refresca la pantalla
    }

    // 2. Interfaz para comunicar el evento
    public interface OnHabitActionListener {
        void onDelete(String habitId);
        void onToggle(String habitId, boolean isCompleted);
    }

    public HabitsAdapter(List<Habit> habitList, OnHabitActionListener listener) {
        this.habitList = habitList;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el diseño 'item_habit.xml' que creamos antes
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit_placeholder, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habitList.get(position);
        holder.bind(habit);

        // Esto evita que se dispare código fantasma mientras configuramos la vista.
        holder.cbCompleted.setOnCheckedChangeListener(null);
        holder.cbCompleted.setOnClickListener(null);
        //Esto ya no dispara nada porque quitamos los listeners arriba
        holder.cbCompleted.setChecked(habit.isCompleted());

        holder.cbCompleted.setOnClickListener(v -> {
            boolean isChecked = holder.cbCompleted.isChecked();
            if (actionListener != null) {
                // Enviamos el cambio a la lógica
                actionListener.onToggle(habit.getId(), isChecked);
                // Opcional: Actualizamos el modelo local inmediatamente para evitar parpadeos
                habit.setCompleted(isChecked);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Eliminar Hábito")
                    .setMessage("¿Deseas borrar " + habit.getName() + "?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        // AQUÍ ES EL CAMBIO: No borras, solo "avisas"
                        if (actionListener != null) {
                            actionListener.onDelete(habit.getId());
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    // Clase interna para manejar cada renglón
    class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvContext, tvDescription;
        CheckBox cbCompleted;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_habit_name);
            tvDescription = itemView.findViewById(R.id.tv_habit_description);
            tvContext = itemView.findViewById(R.id.tv_habit_context);
            cbCompleted = itemView.findViewById(R.id.cb_habit_completed);
        }

        void bind(Habit habit) {
            tvName.setText(habit.getName());
            tvDescription.setText(habit.getDescription());
            // Unimos Lugar y Momento con un punto separador
            String lugar = habit.getContextPlace();
            String momento = habit.getContextTime();
            String textoCombinado = lugar + " • " + momento;
            tvContext.setText(textoCombinado);
            cbCompleted.setChecked(habit.isCompleted());
        }
    }
}
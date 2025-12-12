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

/**
 * Adaptador para HabitList.
 */
public class HabitsAdapter extends RecyclerView.Adapter<HabitsAdapter.HabitViewHolder> {
    private List<Habit> habitList = new ArrayList<>();
    private OnHabitActionListener actionListener;


    /**
     * Constructor.
     * @param habitList
     * @param listener
     */
    public HabitsAdapter(List<Habit> habitList, OnHabitActionListener listener) {
        this.habitList = habitList;
        this.actionListener = listener;
    }

    /**
     * Actualiza la lista de hábitos.
     * @param habits
     */
    public void setHabits(List<Habit> habits) {
        this.habitList = habits;
        notifyDataSetChanged(); // Refresca la pantalla
    }

    /**
     * Interfaz para manejar los cambios en los hábitos.
     */
    public interface OnHabitActionListener {
        void onDelete(String habitId);
        void onToggle(String habitId, boolean isCompleted);
    }

    /**
     * Crea cada renglón.
     * @param parent
     * @param viewType
     * @return
     */
    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit_placeholder, parent, false);
        return new HabitViewHolder(view);
    }

    /**
     * Configura cada renglón.
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        Habit habit = habitList.get(position);
        holder.bind(habit);
        holder.cbCompleted.setOnCheckedChangeListener(null);
        holder.cbCompleted.setOnClickListener(null);
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
                        if (actionListener != null) {
                            actionListener.onDelete(habit.getId());
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
    }

    /**
     * Devuelve el número de hábitos.
     * @return
     */
    @Override
    public int getItemCount() {
        return habitList.size();
    }

    /**
     * ViewHolder para un hábito.
     */
    class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvContext, tvDescription;
        CheckBox cbCompleted;

        /**
         * Constructor.
         * @param itemView
         */
        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_habit_name);
            tvDescription = itemView.findViewById(R.id.tv_habit_description);
            tvContext = itemView.findViewById(R.id.tv_habit_context);
            cbCompleted = itemView.findViewById(R.id.cb_habit_completed);
        }

        /**
         * Configura el ViewHolder.
         * @param habit
         */
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
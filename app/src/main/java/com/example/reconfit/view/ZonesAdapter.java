package com.example.reconfit.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reconfit.R;
import com.example.reconfit.model.Zone;

import java.util.List;

public class ZonesAdapter extends RecyclerView.Adapter<ZonesAdapter.ZoneViewHolder> {

    // 1. Interfaz de Acción para comunicarnos con el Fragmento
    public interface ZoneActionListener {
        void onZoneModify(Zone zone);
        void onZoneDelete(Zone zone);
    }

    private List<Zone> zoneList;
    private final ZoneActionListener actionListener;

    public ZonesAdapter(List<Zone> zoneList, ZoneActionListener actionListener) {
        this.zoneList = zoneList;
        this.actionListener = actionListener;
    }

    // 2. ViewHolder: Mantiene las referencias a las vistas
    public static class ZoneViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView detailsTextView;
        ImageButton modifyButton;
        ImageButton deleteButton;

        public ZoneViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tv_zone_name);
            detailsTextView = itemView.findViewById(R.id.tv_zone_details);
            modifyButton = itemView.findViewById(R.id.btn_modify_zone);
            deleteButton = itemView.findViewById(R.id.btn_delete_zone);
        }
    }

    @NonNull
    @Override
    public ZoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_zone, parent, false);
        return new ZoneViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ZoneViewHolder holder, int position) {
        Zone zone = zoneList.get(position);

        // 3. Bindear datos
        holder.nameTextView.setText(zone.getName());
        String details = String.format("Lat: %.2f, Lon: %.2f (Radio: %.0fm)",
                zone.getLatitude(), zone.getLongitude(), zone.getRadiusMeters());
        holder.detailsTextView.setText(details);

        // 4. Configurar Click Listeners para los botones de acción
        holder.modifyButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onZoneModify(zone);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onZoneDelete(zone);
            }
        });
    }

    @Override
    public int getItemCount() {
        return zoneList != null ? zoneList.size() : 0;
    }

    // 5. Método para actualizar la lista de zonas desde el ViewModel
    public void setZones(List<Zone> newZones) {
        this.zoneList = newZones;
        notifyDataSetChanged();
    }
}
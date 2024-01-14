package com.atakmap.android.helloworld.adapter;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.helloworld.SquireDropDownReceiver;
import com.atakmap.android.helloworld.models.Device;
import com.atakmap.android.helloworld.plugin.R;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private static final String TAG = "DeviceAdapter";
    private Callable<List<Device>> getLatestDataset;
    private UUID patientUuid;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView descTextView;
        private Button button;
        private String deviceId;

        public ViewHolder(@NonNull View itemView, UUID patientUuid) {
            super(itemView);
            // click listener would go here when im ready for it

            titleTextView = itemView.findViewById(R.id.polar_device_title);
            descTextView = itemView.findViewById(R.id.polar_device_desc);
            button = itemView.findViewById(R.id.polar_device_button);

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Clicked. Patient uuid: " + patientUuid);
                    Gson gson = new Gson();
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.putExtra("startHR", deviceId);
                    intent.putExtra("patientUuid", gson.toJson(patientUuid));
                    v.getContext().sendBroadcast(intent);
                    SquireDropDownReceiver.closePolarDialog();
                }
            };

            titleTextView.setClickable(true);
            descTextView.setClickable(true);
            titleTextView.setOnClickListener(listener);
            descTextView.setOnClickListener(listener);
        }

        public Button getButton() {
            return button;
        }
        public TextView getDescTextView() {
            return descTextView;
        }
        public TextView getTitleTextView() {
            return titleTextView;
        }
    }

    public DeviceAdapter(Callable<List<Device>> getLatestDataset, UUID patientUuid) {
        this.getLatestDataset = getLatestDataset;
        this.patientUuid = patientUuid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(SquireDropDownReceiver.deviceAdapterView, patientUuid);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        List<Device> dataset = null;
        try {
            dataset = getLatestDataset.call();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Device device = dataset.get(position);
        holder.getTitleTextView().setText(device.getDeviceName());
        holder.getDescTextView().setText(device.getAddress());
        holder.getButton().setText(device.getStatusMessage());
        holder.deviceId = device.getDeviceId();

        if (!device.getConnected()) {
            holder.getButton().setOnClickListener(view -> {
                Log.d(TAG, "Selected a device");
                device.setStatusMessage("Info");
                holder.getButton().setText(device.getStatusMessage());

                //Gson gson = new Gson();
                //Intent intent = new Intent(view.getContext(), BluetoothConnectService.class);
                //intent.putExtra("device", gson.toJson(device));
                //view.getContext().startService(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        List<Device> dataset = null;
        try {
            dataset = getLatestDataset.call();
        } catch (Exception e) {
            e.printStackTrace();
            dataset = new ArrayList<>();
        }

        return dataset.size();
    }
}

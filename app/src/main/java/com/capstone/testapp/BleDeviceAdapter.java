package com.capstone.testapp;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BleDeviceAdapter extends RecyclerView.Adapter<BleDeviceAdapter.DeviceViewHolder> {

    private final List<BluetoothDevice> deviceList = new ArrayList<>();
    private final Map<String, BluetoothDevice> deviceMap = new HashMap<>(); // To prevent duplicates
    private final OnDeviceClickListener listener;
    private final Context context; // We need context for permission checks

    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }

    public BleDeviceAdapter(Context context, OnDeviceClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void addDevice(BluetoothDevice device) {
        if (device == null || device.getAddress() == null) return;
        if (!deviceMap.containsKey(device.getAddress())) {
            deviceMap.put(device.getAddress(), device);
            deviceList.add(device);
            notifyItemInserted(deviceList.size() - 1);
        }
    }

    public void clearDevices() {
        deviceList.clear();
        deviceMap.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDevice device = deviceList.get(position);

        // This is the safe way to get the device name
        String deviceName = "Unnamed Device";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // On API 31+, we MUST check for BLUETOOTH_CONNECT permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                deviceName = device.getName() != null ? device.getName() : "Unnamed Device";
            } else {
                deviceName = "Permission Needed";
            }
        } else {
            // On older versions, we can get the name directly (permission was for scanning)
            deviceName = device.getName() != null ? device.getName() : "Unnamed Device";
        }

        holder.deviceNameTextView.setText(deviceName);
        holder.deviceAddressTextView.setText(device.getAddress());
        holder.itemView.setOnClickListener(v -> listener.onDeviceClick(device));
    }


    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceNameTextView;
        TextView deviceAddressTextView;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceNameTextView = itemView.findViewById(R.id.deviceNameTextView);
            deviceAddressTextView = itemView.findViewById(R.id.deviceAddressTextView);
        }
    }
}


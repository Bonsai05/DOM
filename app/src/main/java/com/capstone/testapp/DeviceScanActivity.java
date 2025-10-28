package com.capstone.testapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DeviceScanActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE = "extra_device";

    private BleManager bleManager;
    private BleDeviceAdapter adapter;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                if (permissions.values().stream().allMatch(granted -> granted)) {
                    bleManager.startScan();
                } else {
                    Toast.makeText(this, "Required permissions denied.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            adapter.addDevice(result.getDevice());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        bleManager = new BleManager(this, scanCallback, null);

        RecyclerView recyclerView = findViewById(R.id.deviceRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BleDeviceAdapter(new ArrayList<>(), (device) -> {
            bleManager.stopScan();
            final Intent resultIntent = new Intent(this, ChatActivity.class);
            resultIntent.putExtra(EXTRA_DEVICE, device);
            startActivity(resultIntent);
            finish();
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.scanButton).setOnClickListener(v -> checkPermissionsAndScan());
    }

    private void checkPermissionsAndScan() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
    }

    @Override
    protected void onPause() {
        super.onPause();
        bleManager.stopScan();
    }
}

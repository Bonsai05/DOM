package com.capstone.testapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceScanActivity extends AppCompatActivity {

    private static final String TAG = "DeviceScanActivity";
    private static final long SCAN_PERIOD = 10000; // Scan for 10 seconds

    private BluetoothLeScanner scanner;

    private BluetoothAdapter bluetoothAdapter;
    private Handler scanHandler;
    private boolean isScanning = false;

    private RecyclerView deviceRecyclerView;
    private BleDeviceAdapter bleDeviceAdapter;
    private Button scanButton;
    private ProgressBar scanProgressBar;

    public static final String EXTRA_DEVICE_ADDRESS = "extra_device_address";
    public static final String EXTRA_DEVICE_NAME = "extra_device_name";

    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    startBleScan();
                } else {
                    Toast.makeText(this, "Bluetooth is required.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String[]> blePermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (boolean isGranted : result.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    checkBluetoothStateAndScan();
                } else {
                    Toast.makeText(this, "Permissions are required.", Toast.LENGTH_LONG).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        Toolbar toolbar = findViewById(R.id.scanToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Scan for Node");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        scanHandler = new Handler(Looper.getMainLooper());

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter != null) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        scanButton = findViewById(R.id.scanButton);
        scanProgressBar = findViewById(R.id.scanProgressBar);
        deviceRecyclerView = findViewById(R.id.deviceRecyclerView);

        bleDeviceAdapter = new BleDeviceAdapter(this, device -> {
            stopBleScan();
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                resultIntent.putExtra(EXTRA_DEVICE_NAME, device.getName());
            }

            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });

        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceRecyclerView.setAdapter(bleDeviceAdapter);

        scanButton.setOnClickListener(v -> {
            bleDeviceAdapter.clearDevices();
            checkPermissionsAndStartScan();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }


    private void checkPermissionsAndStartScan() {
        String[] permissionsToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else {
            permissionsToRequest = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }
        blePermissionsLauncher.launch(permissionsToRequest);
    }

    private void checkBluetoothStateAndScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return;
            }
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            startBleScan();
        }
    }


    private void startBleScan() {
        if (isScanning) return;

        if (scanner == null) {
            Log.e(TAG, "Failed to get BLE scanner.");
            Toast.makeText(this, "Cannot get BLE scanner", Toast.LENGTH_SHORT).show();
            return;
        }

        scanHandler.postDelayed(this::stopBleScan, SCAN_PERIOD);

        isScanning = true;
        scanButton.setEnabled(false);
        scanProgressBar.setVisibility(View.VISIBLE);
        bleDeviceAdapter.clearDevices();

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        scanner.startScan(null, scanSettings, scanCallback); // Scan without filters
        Log.d(TAG, "Scan started with native scanner...");
    }

    private void stopBleScan() {
        if (!isScanning) return;

        if (scanner != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            scanner.stopScan(scanCallback);
        }
        isScanning = false;
        scanButton.setEnabled(true);
        scanProgressBar.setVisibility(View.GONE);
        scanHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Scan stopped.");
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, @NonNull ScanResult result) {
            super.onScanResult(callbackType, result);
            processScanResult(result.getDevice());
        }

        @Override
        public void onBatchScanResults(@NonNull List<ScanResult> results) {
            super.onBatchScanResults(results);
            if (!isScanning) return;
            Log.d(TAG,"Batch Scan Results received: " + results.size());
            for (ScanResult result : results) {
                processScanResult(result.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Scan failed with error code: " + errorCode);
            stopBleScan();
        }

        private void processScanResult(BluetoothDevice device) {
            runOnUiThread(() -> bleDeviceAdapter.addDevice(device));
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
        stopBleScan();
    }
}

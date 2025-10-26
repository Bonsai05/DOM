package com.capstone.testapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final long SCAN_PERIOD = 10000; // Stop scanning after 10 seconds

    // --- SERVICE AND CHARACTERISTIC UUIDS ---
    private static final UUID SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID MESSAGE_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic messageCharacteristic;
    private Handler scanHandler;

    private Button scanButton;
    private ListView deviceListView;
    private EditText messageEditText;
    private Button sendButton;
    private ProgressBar scanProgressBar;
    private TextView statusTextView;

    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private ArrayAdapter<String> deviceListAdapter;

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null && !discoveredDevices.contains(device)) {
                discoveredDevices.add(device);
                deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
                deviceListAdapter.notifyDataSetChanged();
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(() -> {
                    statusTextView.setText("Status: Connected");
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                });
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(() -> {
                    statusTextView.setText("Status: Disconnected");
                    sendButton.setEnabled(false);
                    Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                });
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    messageCharacteristic = service.getCharacteristic(MESSAGE_CHARACTERISTIC_UUID);
                    if (messageCharacteristic != null) {
                        runOnUiThread(() -> sendButton.setEnabled(true));
                    } else {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Message characteristic not found", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Service not found", Toast.LENGTH_SHORT).show());
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanButton = findViewById(R.id.scanButton);
        deviceListView = findViewById(R.id.deviceListView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        scanProgressBar = findViewById(R.id.scanProgressBar);
        statusTextView = findViewById(R.id.statusTextView);

        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        deviceListView.setAdapter(deviceListAdapter);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        scanHandler = new Handler();

        scanButton.setOnClickListener(v -> checkPermissionsAndScan());

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = discoveredDevices.get(position);
            connectToDevice(selectedDevice);
        });

        sendButton.setOnClickListener(v -> sendMessage());

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void checkPermissionsAndScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                startScan();
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                startScan();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
                Toast.makeText(this, "Permissions are required for scanning.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothLeScanner == null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        statusTextView.setText("Status: Scanning...");
        scanProgressBar.setVisibility(View.VISIBLE);
        scanButton.setEnabled(false);
        discoveredDevices.clear();
        deviceListAdapter.clear();
        deviceListAdapter.notifyDataSetChanged();

        scanHandler.postDelayed(() -> {
            bluetoothLeScanner.stopScan(scanCallback);
            scanProgressBar.setVisibility(View.GONE);
            scanButton.setEnabled(true);
            statusTextView.setText("Status: Disconnected");
        }, SCAN_PERIOD);

        bluetoothLeScanner.startScan(scanCallback);
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        statusTextView.setText("Status: Connecting...");
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
        scanProgressBar.setVisibility(View.GONE);
        scanButton.setEnabled(true);
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    @SuppressLint("MissingPermission")
    private void sendMessage() {
        if (bluetoothGatt != null && messageCharacteristic != null) {
            String message = messageEditText.getText().toString();
            if (message.isEmpty()) {
                Toast.makeText(this, "Cannot send empty message", Toast.LENGTH_SHORT).show();
                return;
            }
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt.writeCharacteristic(messageCharacteristic, messageBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            } else {
                messageCharacteristic.setValue(messageBytes);
                bluetoothGatt.writeCharacteristic(messageCharacteristic);
            }
            messageEditText.setText(""); // Clear the message box
        }
    }
}

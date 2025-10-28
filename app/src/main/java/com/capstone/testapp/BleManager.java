package com.capstone.testapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class BleManager {

    private static final String TAG = "BleManager";

    // UUIDs
    private static final UUID SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID TX_CHARACTERISTIC_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID RX_CHARACTERISTIC_UUID = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScanner scanner;
    private BluetoothGatt gatt;

    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private final ScanCallback scanCallback;
    private final BluetoothGattCallback gattCallback;

    public BleManager(Context context, ScanCallback scanCallback, GattCallback gattCallback) {
        this.bluetoothAdapter = ((android.bluetooth.BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        this.scanner = bluetoothAdapter.getBluetoothLeScanner();
        this.scanCallback = scanCallback;
        this.gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server. Starting service discovery.");
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server.");
                }
                gattCallback.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                gattCallback.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                gattCallback.onCharacteristicChanged(gatt, characteristic);
            }
        };
    }

    public void startScan() {
        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder().build();
        scanner.startScan(filters, settings, scanCallback);
        scanHandler.postDelayed(this::stopScan, 10000);
    }

    public void stopScan() {
        scanner.stopScan(scanCallback);
    }

    public void connect(BluetoothDevice device) {
        gatt = device.connectGatt(null, false, gattCallback);
    }

    public void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
    }

    public void sendMessage(String message) {
        if (gatt == null) return;
        BluetoothGattService service = gatt.getService(SERVICE_UUID);
        if (service == null) return;
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(TX_CHARACTERISTIC_UUID);
        if (characteristic == null) return;

        characteristic.setValue(message.getBytes(StandardCharsets.UTF_8));
        gatt.writeCharacteristic(characteristic);
    }

    public void enableNotifications() {
        if (gatt == null) return;
        BluetoothGattService service = gatt.getService(SERVICE_UUID);
        if (service == null) return;
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(RX_CHARACTERISTIC_UUID);
        if (characteristic == null) return;

        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
    }

    public interface GattCallback {
        void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);
        void onServicesDiscovered(BluetoothGatt gatt, int status);
        void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
    }
}

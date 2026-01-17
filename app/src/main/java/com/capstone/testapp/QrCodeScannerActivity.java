package com.capstone.testapp;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.nio.charset.StandardCharsets;

public class QrCodeScannerActivity extends AppCompatActivity implements BleManager.GattCallback {

    private TextView qrCodeResult;
    private BleManager bleManager;
    private String scannedData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code_scanner);

        qrCodeResult = findViewById(R.id.qr_code_result);

        bleManager = new BleManager(this, null, this);

        if (DeviceScanActivity.connectedDevice == null) {
            Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        new IntentIntegrator(this).initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
                finish();
            } else {
                scannedData = result.getContents();
                qrCodeResult.setText("Scanned: " + scannedData);
                bleManager.connect(DeviceScanActivity.connectedDevice);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            runOnUiThread(() -> Toast.makeText(QrCodeScannerActivity.this, "Connected to device", Toast.LENGTH_SHORT).show());
            gatt.discoverServices();
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            runOnUiThread(() -> Toast.makeText(QrCodeScannerActivity.this, "Disconnected from device", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            bleManager.enableNotifications();
            bleManager.sendMessage(scannedData);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        String reply = new String(characteristic.getValue(), StandardCharsets.UTF_8);
        runOnUiThread(() -> qrCodeResult.append("\nReply from node: " + reply));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.disconnect();
    }
}

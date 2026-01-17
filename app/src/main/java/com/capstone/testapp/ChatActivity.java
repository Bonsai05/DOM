package com.capstone.testapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity implements BleManager.GattCallback {

    private BleManager bleManager;
    private MessageAdapter messageAdapter;
    private Button scanQrButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        BluetoothDevice device = getIntent().getParcelableExtra(DeviceScanActivity.EXTRA_DEVICE);
        bleManager = new BleManager(this, null, this);
        bleManager.connect(device);

        setupViews();
    }

    private void setupViews() {
        RecyclerView recyclerView = findViewById(R.id.chatRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter(new ArrayList<>());
        recyclerView.setAdapter(messageAdapter);

        scanQrButton = findViewById(R.id.scan_qr_button_chat);
        scanQrButton.setOnClickListener(v -> {
            new IntentIntegrator(this).initiateScan();
        });
        scanQrButton.setEnabled(false); // Disable until services are discovered
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
            } else {
                String scannedData = result.getContents();
                // Display scanned data in chat
                Message sentMessage = new Message(scannedData, System.currentTimeMillis(), true);
                messageAdapter.addMessage(sentMessage);
                // Send scanned data to the node
                bleManager.sendMessage(scannedData);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.disconnect();
    }

    // BleManager.GattCallback implementation
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        runOnUiThread(() -> {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Toast.makeText(this, "Connected. Discovering services...", Toast.LENGTH_SHORT).show();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
                scanQrButton.setEnabled(false);
            }
        });
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Services discovered. Ready to scan.", Toast.LENGTH_SHORT).show();
                scanQrButton.setEnabled(true);
                bleManager.enableNotifications();
            });
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        runOnUiThread(() -> {
            String receivedText = new String(characteristic.getValue());
            Message receivedMessage = new Message(receivedText, System.currentTimeMillis(), false);
            messageAdapter.addMessage(receivedMessage);
        });
    }
}

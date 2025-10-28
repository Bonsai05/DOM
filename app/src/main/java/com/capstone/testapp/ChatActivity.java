package com.capstone.testapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity implements BleManager.GattCallback {

    private BleManager bleManager;
    private MessageAdapter messageAdapter;
    private EditText messageEditText;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        BluetoothDevice device = getIntent().getParcelableExtra(DeviceScanActivity.EXTRA_DEVICE);
        bleManager = new BleManager(this, null, this);
        bleManager.connect(device);

        RecyclerView recyclerView = findViewById(R.id.chatRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(new ArrayList<>());
        recyclerView.setAdapter(messageAdapter);

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(v -> sendMessage());
        sendButton.setEnabled(false); // Disable send button until services are discovered
    }

    private void sendMessage() {
        String message = messageEditText.getText().toString();
        if (message.isEmpty()) return;

        bleManager.sendMessage(message);
        messageAdapter.addMessage("Me: " + message);
        messageEditText.setText("");
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
                sendButton.setEnabled(false);
            }
        });
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Services discovered. Ready to send.", Toast.LENGTH_SHORT).show();
                sendButton.setEnabled(true);
                bleManager.enableNotifications();
            });
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        runOnUiThread(() -> {
            messageAdapter.addMessage("Node: " + new String(characteristic.getValue()));
        });
    }
}

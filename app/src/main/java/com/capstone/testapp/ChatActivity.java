package com.capstone.testapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
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
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        BluetoothDevice device = getIntent().getParcelableExtra(DeviceScanActivity.EXTRA_DEVICE);
        bleManager = new BleManager(this, null, this);
        bleManager.connect(device);

        setupViews();
        setupKeyboardHandling();
    }

    private void setupViews() {
        recyclerView = findViewById(R.id.chatRecyclerView);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        recyclerView.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter(new ArrayList<>());
        recyclerView.setAdapter(messageAdapter);

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(v -> sendMessage());
        sendButton.setEnabled(false); // Disable send button until services are discovered
    }

    private void setupKeyboardHandling() {
        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) { // Keyboard is opened
                    // Scroll to bottom when keyboard opens
                    if (messageAdapter.getItemCount() > 0) {
                        recyclerView.post(() -> recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1));
                    }
                }
            }
        });

        // Auto-scroll when new messages are added
        messageAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                recyclerView.post(() -> {
                    if (layoutManager.findLastCompletelyVisibleItemPosition() >= positionStart - 1) {
                        recyclerView.smoothScrollToPosition(positionStart + itemCount - 1);
                    }
                });
            }
        });
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (messageText.isEmpty()) return;

        // Create and add sent message
        Message sentMessage = new Message(messageText, System.currentTimeMillis(), true);
        messageAdapter.addMessage(sentMessage);

        // Send via BLE
        bleManager.sendMessage(messageText);
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
            String receivedText = new String(characteristic.getValue());
            Message receivedMessage = new Message(receivedText, System.currentTimeMillis(), false);
            messageAdapter.addMessage(receivedMessage);
        });
    }
}

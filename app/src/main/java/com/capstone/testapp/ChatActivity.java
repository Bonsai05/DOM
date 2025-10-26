package com.capstone.testapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    // --- UI, Database, and Crypto variables ---
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private Button sendButton;
    private List<Message> messageList;
    private MessageAdapter messageAdapter;
    private AppDatabase db;
    private MessageDao messageDao;
    private ExecutorService executorService;
    private CryptoManager cryptoManager;
    private String contactPublicKey;

    // --- BLE variables ---
    private static final String TAG = "ChatActivity_BLE";
    private static final UUID SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID TX_CHARACTERISTIC_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID RX_CHARACTERISTIC_UUID = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt gatt;

    // --- REMOVED: All old scanning variables (scanner, isScanning, scanHandler) ---

    // --- NEW: Launcher to start the DeviceScanActivity ---
    private final ActivityResultLauncher<Intent> deviceScanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String deviceAddress = result.getData().getStringExtra(DeviceScanActivity.EXTRA_DEVICE_ADDRESS);
                    if (deviceAddress != null) {
                        Log.d(TAG, "Received device address: " + deviceAddress);
                        // We have the address, now connect to it.
                        connectToDevice(deviceAddress);
                    }
                } else {
                    Log.d(TAG, "Scan cancelled or failed.");
                    Toast.makeText(this, "Device selection cancelled.", Toast.LENGTH_SHORT).show();
                }
            });

    // --- REMOVED: Old enableBluetoothLauncher and blePermissionsLauncher ---
    // (This logic is now handled by DeviceScanActivity)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        cryptoManager = new CryptoManager(this);
        Intent intent = getIntent();
        String contactName = intent.getStringExtra("CONTACT_NAME");
        contactPublicKey = intent.getStringExtra("CONTACT_PUBLIC_KEY");
        if (contactPublicKey == null) {
            Toast.makeText(this, "Error: Contact's public key not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(contactName);
        }

        db = AppDatabase.getDatabase(this);
        messageDao = db.messageDao();
        executorService = Executors.newSingleThreadExecutor();
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(messageAdapter);

        loadMessages();

        sendButton.setOnClickListener(v -> {
            String messageText = messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            }
        });

        // --- SIMPLIFIED: Just get the adapter, don't start any processes ---
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    // --- NEW: Method to launch the DeviceScanActivity ---
    private void launchDeviceScanner() {
        // Before launching, check if Bluetooth is on
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please turn on Bluetooth first.", Toast.LENGTH_SHORT).show();
            // Optionally, you could request to turn it on here
            return;
        }

        Intent intent = new Intent(this, DeviceScanActivity.class);
        deviceScanLauncher.launch(intent);
    }

    // --- NEW: Method to connect to the selected device ---
    private void connectToDevice(String address) {
        if (bluetoothAdapter == null) return;

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.e(TAG, "Device not found with address: " + address);
            return;
        }

        Log.d(TAG, "Attempting to connect to device: " + address);

        // Close any existing connection first
        if (gatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // We'd need to ask for permission here, but DeviceScanActivity should have handled it.
                // For simplicity, we just check.
                Toast.makeText(this, "Missing Connect Permission", Toast.LENGTH_SHORT).show();
                return;
            }
            gatt.close();
            gatt = null;
        }

        // Connect using the existing gattCallback
        // This will trigger the gattCallback's onConnectionStateChange
        gatt = device.connectGatt(this, false, gattCallback);
    }

    // --- NEW: Methods to create and handle the "Connect" menu button ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_screen_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_connect) {
            launchDeviceScanner();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- This method handles the back button in the toolbar ---
    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    // --- This method is unchanged ---
    private void sendMessage(String messageText) {
        Message newMessage = new Message(messageText, System.currentTimeMillis(), true);
        executorService.execute(() -> {
            messageDao.insert(newMessage);
            runOnUiThread(() -> {
                messageList.add(newMessage);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                chatRecyclerView.scrollToPosition(messageList.size() - 1);
            });
        });

        String encryptedMessage = cryptoManager.encrypt(messageText, contactPublicKey);
        if (encryptedMessage != null) {
            sendBleMessage(encryptedMessage);
        } else {
            runOnUiThread(() -> Toast.makeText(this, "Encryption Failed!", Toast.LENGTH_SHORT).show());
        }
    }

    // --- This method is unchanged ---
    private void loadMessages() {
        executorService.execute(() -> {
            List<Message> loadedMessages = messageDao.getAllMessages();
            runOnUiThread(() -> {
                messageList.clear();
                messageList.addAll(loadedMessages);
                messageAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    chatRecyclerView.scrollToPosition(messageList.size() - 1);
                }
            });
        });
    }

    // --- This method is unchanged ---
    private void sendBleMessage(String encryptedText) {
        if (gatt == null) {
            Toast.makeText(this, "Not connected to LoRa Node", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;

        BluetoothGattService service = gatt.getService(SERVICE_UUID);
        if (service == null) { Log.e(TAG, "Service not found!"); return; }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(TX_CHARACTERISTIC_UUID);
        if (characteristic == null) { Log.e(TAG, "TX Characteristic not found!"); return; }

        byte[] messageBytes = encryptedText.getBytes(StandardCharsets.UTF_8);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, messageBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        } else {
            characteristic.setValue(messageBytes);
            gatt.writeCharacteristic(characteristic);
        }
        runOnUiThread(() -> messageEditText.setText(""));
    }

    // --- This callback is unchanged ---
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to LoRa Node.");
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Connected to Node!", Toast.LENGTH_SHORT).show());
                if (ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from LoRa Node.");
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Disconnected from Node.", Toast.LENGTH_SHORT).show());
                ChatActivity.this.gatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered. Enabling notifications...");
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service == null) return;
                BluetoothGattCharacteristic rxChar = service.getCharacteristic(RX_CHARACTERISTIC_UUID);
                if (rxChar == null) return;

                if (ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
                gatt.setCharacteristicNotification(rxChar, true);

                BluetoothGattDescriptor descriptor = rxChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                if (descriptor == null) return;
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
                Log.d(TAG, "Enabled notifications for RX characteristic.");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(RX_CHARACTERISTIC_UUID)) {
                String encryptedMessage = new String(characteristic.getValue(), StandardCharsets.UTF_8);
                Log.d(TAG, "Received encrypted message from node.");

                String decryptedMessage = cryptoManager.decrypt(encryptedMessage);
                if (decryptedMessage != null) {
                    Log.d(TAG, "Decryption successful.");
                    Message receivedMessage = new Message(decryptedMessage, System.currentTimeMillis(), false);
                    executorService.execute(() -> {
                        messageDao.insert(receivedMessage);
                        runOnUiThread(() -> {
                            messageList.add(receivedMessage);
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            chatRecyclerView.scrollToPosition(messageList.size() - 1);
                        });
                    });
                } else {
                    Log.e(TAG, "Decryption failed!");
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return;
            gatt.close();
            gatt = null;
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}


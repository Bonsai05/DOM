# DOM - BLE Messaging App

DOM is a simple Android application demonstrating Bluetooth Low Energy (BLE) communication. It allows users to scan for nearby BLE devices, establish a connection, and send text-based messages.

This project serves as a clear, straightforward example of using Android's native BLE APIs for basic data transfer.

## Features

- **BLE Device Scanning:** Scan for nearby BLE peripherals.
- **Device Connection:** Establish a connection with a selected device from the scan results.
- **Simple Messaging:** Send text messages to the connected device.
- **Modern UI:** Built with modern Material 3 components and a clean, user-friendly interface.
- **Runtime Permissions:** Correctly handles all necessary runtime permissions for Bluetooth functionality on modern Android versions.

## How to Use

1.  **Launch the App:** Open the DOM application.
2.  **Start the Process:** Tap the **"Start BLE Chat"** button.
3.  **Scan for Devices:** On the scan screen, tap the **"Scan for Devices"** button.
4.  **Grant Permissions:** If prompted, grant the necessary Bluetooth and Location permissions.
5.  **Connect to a Device:** A list of nearby devices will appear. Tap on a device to connect to it.
6.  **Chat:** You will be automatically taken to the chat screen. Once the "Services discovered" toast appears, you can send and receive messages.

## Building the Project

This is a standard Android Studio project with no external BLE dependencies.

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Let Gradle sync the project.
4.  Build and run the application on an Android device.

=======
=======

# Decentralized Off Grid Messenger

> Secure Offline Long-Range Messaging with BLE + LoRa 

---

```
Phone A <-> BLE <-> Node 1 <--- LoRa ---> Node 2 <-> BLE <-> Phone B
```
Offline peer‑to‑peer communication without internet or cell networks!

---

## 📌 Project Overview
LoRaBLE Messenger is an **end-to-end encrypted**, **offline messaging system** that uses:

- **Bluetooth Low Energy (BLE)** for phone ↔ hardware communication  
- **LoRa (Long Range Radio)** for long‑distance transmission  

This project was built as a **B.Tech CSE capstone** demonstrating skills in:  
Android Development • Embedded Systems • Wireless Protocols 

---

## 🧠 System Architecture


1️⃣ Send message over **BLE** to nearest node   
2️⃣ **LoRa radio** forwards packet   
3️⃣ Target node relays via **BLE** 

---

## ✅ Key Features
| Feature | Description |
|--------|-------------|
| 📡 Hardware‑extended Range | ESP32 + LoRa nodes |
| 🔄 BLE Relay | Low power short‑range communication |
| 📱 Local Chat Storage | Android Room database |
| LCD feedback for connection states |

---

## 🛠️ Technologies Used

### 📱 Android App
- Java + AndroidX
- Material UI | RecyclerView | ConstraintLayout
- BLE: `BluetoothLeScanner`
- Persistence: **Room DB**
- Gradle build system

### 📡 ESP32 Hardware Firmware
- C++ | Arduino Framework
- LoRa Library by Sandeep Mistry
- `BLEDevice.h` for BLE GATT
- LCD using `LiquidCrystal_I2C`

---

## 🔌 Hardware Requirements

| Component | Quantity | Purpose |
|----------|----------|---------|
| ESP32 Dev Board | 2 | Controller |
| SX1278 / Ra‑02 (433 MHz) | 2 | LoRa Communication |
| 16×2 I2C LCD | 2 | Status Updates |
| Jumper Wires + Breadboard | — | Prototyping |
| USB Power Source | 2 | Node Power |

📝 Wiring info included in `.ino` source code comments.

---

## 🚀 Installation & Setup

### Step 1️⃣ ESP32 Nodes
1. Install Arduino IDE + ESP32 support
2. Install Libraries:
   - **LoRa** (Sandeep Mistry)
   - **LiquidCrystal_I2C**
3. Flash Node 1 → `MY_ADDRESS 1`, `DESTINATION_ADDRESS 2`
4. Flash Node 2 → `MY_ADDRESS 2`, `DESTINATION_ADDRESS 1`

Expected LCD output: ✅ **BLE+LoRa Ready**

### Step 2️⃣ Android App
1. Clone repo & open in Android Studio
2. Let Gradle Sync
3. Install APK on two Android phones
4. On first launch:
   - Set Username
   - RSA keys auto‑generated

---

## 💬 Usage Guide

### 🗨️ Messaging
- Enable **Bluetooth + Location**
- Connect each phone to nearest LoRa Node  
- Wait for: ✅ *“Connected to Node!”*  
- Start Secure Chatting!

---

## 🧪 Challenges Overcome
| Challenge | Solution |
|----------|----------|
| BLE Scan inconsistencies | Event‑based scanning & filtering |
| LoRa packet loss | Structured packet format |
| DB migrations | Safe schema evolution |
| Hardware reliability | Signal and power debugging |

---

## 🔮 Future Roadmap
- Background notifications 🔔
- LoRa mesh for multi‑hop ✅
- Message buffering in nodes 🧳
- Delivery status & MAC authentication ✅

=======
=======


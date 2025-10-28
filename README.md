# DOM - Native BLE Messaging App

DOM is a focused Android application that demonstrates a clean and robust implementation of Bluetooth Low Energy (BLE) communication using the native Android SDK. It allows users to scan for nearby BLE devices, establish a connection, and engage in two-way messaging.

This project has been refactored to serve as a clear, reliable example of modern Android development practices for BLE, avoiding the complexities and dependency issues of third-party libraries.

## Core Features

- **Reliable BLE Scanning:** Scans for nearby BLE peripherals using the native `BluetoothLeScanner`.
- **Stable Device Connection:** Establishes and maintains a connection to a selected device.
- **Two-Way Messaging:** Supports both sending messages to and receiving messages from a connected BLE peripheral.
- **Modern Architecture:** Built with a clean, multi-screen architecture and a centralized `BleManager` to handle all Bluetooth logic.
- **Modern UI:** Features a user-friendly interface built with modern Material 3 components.
- **Correct Runtime Permissions:** Properly handles all necessary runtime permissions for `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, and `ACCESS_FINE_LOCATION` on all modern Android versions.

## Application Flow

The app follows a simple and intuitive multi-screen flow:

1.  **`MainActivity`**: The main entry point of the app, featuring a single button to begin the process.
2.  **`DeviceScanActivity`**: Launched from the main screen, this activity is dedicated to scanning for and displaying a list of available BLE devices.
3.  **`ChatActivity`**: After a device is selected from the scan list, this activity is launched, automatically establishing a connection and providing the interface for sending and receiving messages.

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


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

1.  **Launch the App:** Open the DOM application on your Android device.
2.  **Enable Bluetooth:** If Bluetooth is not already enabled, the app will prompt you to do so.
3.  **Scan for Devices:** Tap the **"Scan for Devices"** button to start searching for nearby BLE peripherals.
4.  **Grant Permissions:** If prompted, grant the necessary Bluetooth and Location permissions. Scanning will not work without them.
5.  **Connect to a Device:** Once the scan begins, a list of discovered devices will appear. Tap on a device from the list to initiate a connection.
6.  **Send a Message:** After a successful connection, the "Send" button will become active. Type your message in the text field and tap **"Send"** to transmit it to the connected device.

## Permissions Required

This application requires the following permissions to function correctly:

- `BLUETOOTH` and `BLUETOOTH_ADMIN` (for older Android versions)
- `BLUETOOTH_SCAN` (for scanning on Android 12+)
- `BLUETOOTH_CONNECT` (for connecting on Android 12+)
- `ACCESS_FINE_LOCATION` (required for BLE scanning on Android 6+)

## Building the Project

This is a standard Android Studio project. To build it:

1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Let Gradle sync the dependencies.
4.  Build and run the application on an Android device.


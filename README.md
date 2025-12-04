
# LCTerm Lucky Cat Terminal Point of Sale (POS) App

## Overview
The **LCTerm POS App** is a fully functional Android application designed to help merchants accept multiple cryptocurrencies, including Bitcoin (BTC), Ethereum (ETH), Litecoin (LTC), Dogecoin (DOGE), Dash (DASH), Bitcoin Cash (BCH), Tether (USDT), and Monero (XMR). This app supports seamless payment processing for various cryptocurrencies in a retail environment. It provides a simple interface for managing transactions, and supports QR code generation for payments.

## Features
- Accept payments in multiple cryptocurrencies: 
  - Bitcoin (BTC)
  - Ethereum (ETH)
  - Litecoin (LTC)
  - Dogecoin (DOGE)
  - Dash (DASH)
  - Bitcoin Cash (BCH)
  - Tether (USDT)
  - Monero (XMR)
- Real-time price conversion of cryptocurrencies
- Secure QR code generation for payment requests
- Transaction history and export feature for bookkeeping
- Integrated support for checking transaction status
- Compatible with hardware printers for receipt generation
- Pin protection for certain actions for enhanced security
- Easy API key management and configuration

## Prerequisites
- Android Studio installed (latest version recommended)
- Minimum Android SDK version 23
- Recommended: Android SDK version 33 (Android 13+)
- Basic knowledge of Android development

## Installation

### Clone the Repository
Clone this repository locally:

```bash
git https://github.com/Vermont-Secure-Computing/lucky-cat-terminal
```

### Open in Android Studio
1. Launch **Android Studio**.
2. Select **Open an existing project**.
3. Navigate to the cloned repository and open it.

### Build & Run
- Connect your Android device or configure an emulator.
- Open the project and click on **Run** to build and launch the app.

### APK Installation (For Non-Developers)
You can directly install the latest APK release from the [Releases](https://github.com/Vermont-Secure-Computing/lucky-cat-terminal/releases) page.

## Configuration


1. **API Key Configuration**:  
   Add your API key for cryptocurrency price conversion and transaction checking.
   - Navigate to the **API Key** in the app.
   - Enter your API key to enable the app’s conversion features.

2. **Supported Cryptocurrencies**:  
   The app supports several cryptocurrencies by default. You can add or configure more currencies by modifying the `cryptocurrencies.json` file in the `assets` directory.

## Screenshots
(Insert relevant screenshots of the app, such as the home screen, transaction screen, and QR code generation.)

## How It Works
1. **Accept Payment**: Click Accept Payment on the main menu.
2. **Enter Payment Amount**: Input the amount in your local currency.
3. **Select Cryptocurrency**: Choose the cryptocurrency you want to accept for payment.
4. **Generate QR Code**: The app converts the payment amount into cryptocurrency and generates a QR code for the customer to scan.
5. **Track Payment**: The app automatically checks the transaction status and confirms once the payment is received.
6. **Print Receipt**: You can print the receipt using the integrated Sunmi or ESC/POS printer functionality.

## Security
- The app supports **PIN protection** for high-risk operations (such as accessing sensitive settings).
- **Transaction confirmations** are automatically fetched from the blockchain network.

## Technologies Used
- **Kotlin**: Primary language for the app development.
- **ZXing**: For QR code generation.
- **Google ML Kit**: For scanning barcodes/QR codes.
- **SunmiPrinterSDK**: For printing receipts (optional).
- **ESC/POS Printer SDK**: For wider printer support.
- **SQLite**: For local storage of transactions and configuration.

## Contributions
We welcome contributions to improve the app. Please submit a pull request with a clear description of your changes.

## License

Lucky Cat Terminal (LCTerm)  
Copyright (c) 2024–2025 Vermont Secure Computing and contributors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this project except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

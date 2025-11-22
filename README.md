# 🚀 react-native-upi-launcher

A lightweight **React Native Android library** to launch UPI payment intents and fetch installed UPI apps on the device.

It supports:

- Direct launch of a specific UPI app (PhonePe, GPay, Paytm, etc.)
- Fallback to **Chooser** if no package is provided or if preferred app fails
- Fetching list of installed UPI apps that support `upi://pay`

> ⚠️ Android-only — iOS will simply open the UPI URL using `Linking.openURL`.

---

## 📦 Installation

````sh
npm install react-native-upi-launcher
# or
yarn add react-native-upi-launcher`


## Usage


```js
import { fetchUpiApps, openUpiIntent } from "react-native-upi-launcher";

// Fetch installed UPI apps
const apps = await fetchUpiApps();
console.log(apps);

// Launch UPI payment — direct to a selected app
await openUpiIntent(
  "upi://pay?pa=test@upi&pn=Test User&am=1&cu=INR&tn=Test Payment",
  "com.phonepe.app"
);
````

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)

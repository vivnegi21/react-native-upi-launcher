import { Platform, Linking } from "react-native";
import NativeUpiLauncher from "./NativeUpiLauncher";

export const openUpiIntent = async (upiUrl: string, packageName?: string) => {
  if (Platform.OS === "android") {
    try {
      if (packageName) {
        return NativeUpiLauncher.openUpiIntent(upiUrl, packageName);
      }
      return NativeUpiLauncher.openUpiIntent(upiUrl, "");
    } catch (error) {
      console.log("error in openUpiIntent--->", error);
      return Linking.openURL(upiUrl);
    }
  } else {
    if (await Linking.canOpenURL(upiUrl)) {
      return Linking.openURL(upiUrl);
    }
  }
};

export const fetchUpiApps = async () => await NativeUpiLauncher.fetchUpiApps();

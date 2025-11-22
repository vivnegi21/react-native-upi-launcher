import { NativeModules } from "react-native";

export type UpiApp = {
  name: string;
  package: string;
  [key: string]: any;
};

type Spec = {
  openUpiIntent(upiUrl: string, packageName?: string): Promise<any>;
  fetchUpiApps(): Promise<UpiApp[]>;
};

const LINKING_ERROR =
  "The native module 'UpiLauncher' doesn't seem to be linked.\n" +
  "- Make sure you rebuilt the app after installing the library.\n" +
  "- On Android, make sure Gradle synced correctly.\n";

const { UpiLauncher } = NativeModules;

// Use NativeModules instead of TurboModuleRegistry
const NativeUpiLauncher: Spec =
  UpiLauncher ??
  (new Proxy(
    {},
    {
      get() {
        throw new Error(LINKING_ERROR);
      },
    }
  ) as any);

export default NativeUpiLauncher;

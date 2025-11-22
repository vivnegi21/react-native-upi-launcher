import { useEffect, useState } from "react";
import {
  SafeAreaView,
  Text,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
} from "react-native";

// Import from your library
import { fetchUpiApps, openUpiIntent } from "react-native-upi-launcher";
import type { UpiApp } from "../../src/NativeUpiLauncher";

const TEST_UPI_URL =
  "upi://pay?pa=test@upi&pn=Test%20Merchant&am=1&cu=INR&tn=Test%20payment";

const App = () => {
  const [apps, setApps] = useState<UpiApp[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadApps = async () => {
      try {
        setLoading(true);
        setError(null);
        const list = await fetchUpiApps();
        console.log("---------------------", list);
        setApps(list || []);
      } catch (e: any) {
        console.log("Error fetching UPI apps", e);
        setError(e?.message || "Failed to fetch UPI apps");
      } finally {
        setLoading(false);
      }
    };

    loadApps();
  }, []);

  const handleTestPayment = async (item?: UpiApp) => {
    try {
      const res = await openUpiIntent(TEST_UPI_URL, item?.package);
      console.log("UPI result:", res);
    } catch (e) {
      console.log("error--->>>", e);
      console.log("UPI error:", e);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>Installed UPI Apps</Text>

      {loading && <ActivityIndicator size="large" />}

      {error && <Text style={styles.error}>{error}</Text>}

      {!loading && !error && apps.length === 0 && (
        <Text>No UPI apps found.</Text>
      )}

      <FlatList
        data={apps}
        keyExtractor={(item, index) => item.package + index}
        renderItem={({ item }) => (
          <TouchableOpacity
            onPress={handleTestPayment.bind(this, item)}
            style={styles.item}
          >
            <Text style={styles.appName}>{item.name}</Text>
            <Text style={styles.packageName}>{item.package}</Text>
          </TouchableOpacity>
        )}
      />

      <TouchableOpacity
        style={styles.button}
        onPress={() => handleTestPayment()}
      >
        <Text style={styles.buttonText}>Test Payment Intent</Text>
      </TouchableOpacity>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, backgroundColor: "white", paddingTop: 44 },
  title: { fontSize: 22, fontWeight: "600", marginBottom: 12 },
  error: { color: "red", marginBottom: 8 },
  item: {
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderColor: "#ccc",
  },
  appName: { fontSize: 16, fontWeight: "500" },
  packageName: { fontSize: 12, color: "#666" },
  button: {
    marginTop: 16,
    paddingVertical: 12,
    alignItems: "center",
    borderRadius: 8,
    borderWidth: 1,
  },
  buttonText: { fontSize: 16, fontWeight: "500" },
});

export default App;

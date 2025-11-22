package com.upilauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class UPIInAppActivity extends AppCompatActivity {

  public static final int UPI_REQUEST_CODE = 1111;

  public static final String TAG = "UPIInAppActivity";
  public static final String EXTRA_UPI_DEEP_LINK = "UPI_DEEP_LINK_URL";
  public static final String EXTRA_UPI_PACKAGE_NAME = "EXTRA_UPI_PACKAGE_NAME";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Get deep link URL and optional package name from Intent extras
    String deepLinkUrl = getIntent().getStringExtra(EXTRA_UPI_DEEP_LINK);
    String packageName = getIntent().getStringExtra(EXTRA_UPI_PACKAGE_NAME);

    if (deepLinkUrl == null || deepLinkUrl.isEmpty()) {
      Log.e(TAG, "No deep link URL provided");
      Intent result = new Intent();
      result.putExtra("Status", "ERROR");
      result.putExtra("ErrorMessage", "No deep link URL provided");
      setResult(Activity.RESULT_CANCELED, result);
      finish();
      return;
    }

    try {
      // Parse the deep link URL (handle URL encoding if needed)
      String decodedUrl = deepLinkUrl;
      try {
        if (deepLinkUrl.contains("%")) {
          decodedUrl = java.net.URLDecoder.decode(deepLinkUrl, "UTF-8");
          Log.d(TAG, "Decoded URL: " + decodedUrl);
        }
      } catch (Exception e) {
        Log.d(TAG, "URL decoding not needed or failed, using original");
      }

      Uri deepLinkUri = Uri.parse(decodedUrl);

      // Build a proper upi://pay URI from the query
      Uri upiUri = buildUpiUriFromQuery(deepLinkUri);
      if (upiUri == null && "upi".equalsIgnoreCase(deepLinkUri.getScheme())) {
        // If original is already a valid upi:// URI, use that
        upiUri = deepLinkUri;
      }

      // 1️⃣ FIRST TRY: if packageName exists, try direct launch with that package
      if (packageName != null && !packageName.isEmpty() && upiUri != null) {
        try {
          Intent upiIntent = new Intent(Intent.ACTION_VIEW);
          upiIntent.setData(upiUri);
          upiIntent.setPackage(packageName);
          Log.d(TAG, "Attempting direct launch with package: " + packageName +
              " and URI: " + upiUri.toString());
          startActivityForResult(upiIntent, UPI_REQUEST_CODE);
          return;
        } catch (ActivityNotFoundException e) {
          Log.d(TAG, "Direct launch with package failed: " + e.getMessage());
          // fall through to chooser
        }
      }

      // 2️⃣ FALLBACK: open chooser (let user pick any UPI app)
      if (upiUri != null) {
        Intent fallbackIntent = new Intent(Intent.ACTION_VIEW);
        fallbackIntent.setData(upiUri);
        Intent chooser = Intent.createChooser(fallbackIntent, "Pay with");
        try {
          Log.d(TAG, "Launching chooser for URI: " + upiUri.toString());
          startActivityForResult(chooser, UPI_REQUEST_CODE);
          return;
        } catch (ActivityNotFoundException e) {
          Log.d(TAG, "Chooser failed: " + e.getMessage());
          Intent result = new Intent();
          result.putExtra("Status", "APP_NOT_FOUND");
          result.putExtra("ErrorMessage", "No UPI app available to handle payment");
          setResult(Activity.RESULT_CANCELED, result);
          finish();
          return;
        }
      }

      // If nothing works, throw error
      throw new ActivityNotFoundException("No UPI app available to handle: " + deepLinkUrl);

    } catch (ActivityNotFoundException e) {
      Log.e(TAG, "No UPI app found to handle intent: " + deepLinkUrl, e);

      Intent result = new Intent();
      result.putExtra("Status", "APP_NOT_FOUND");
      result.putExtra("ErrorMessage", e.getMessage());
      setResult(Activity.RESULT_CANCELED, result);
      finish();
    } catch (Exception e) {
      Log.e(TAG, "Exception parsing deep link: " + deepLinkUrl, e);
      Intent result = new Intent();
      result.putExtra("Status", "ERROR");
      result.putExtra("ErrorMessage", e.getMessage());
      setResult(Activity.RESULT_CANCELED, result);
      finish();
    }
  }

  private Uri buildUpiUriFromQuery(Uri deepLinkUri) {
    try {
      Uri.Builder builder = new Uri.Builder()
        .scheme("upi")
        .authority("pay");

      // Copy all query parameters
      String query = deepLinkUri.getQuery();
      if (query != null && !query.isEmpty()) {
        for (String paramName : deepLinkUri.getQueryParameterNames()) {
          String paramValue = deepLinkUri.getQueryParameter(paramName);
          if (paramValue != null) {
            builder.appendQueryParameter(paramName, paramValue);
          }
        }
      } else {
        // If no query, check if the path contains parameters
        String path = deepLinkUri.getEncodedPath();
        if (path != null && path.contains("?")) {
          String[] parts = path.split("\\?", 2);
          if (parts.length == 2) {
            String[] params = parts[1].split("&");
            for (String param : params) {
              String[] keyValue = param.split("=", 2);
              if (keyValue.length == 2) {
                builder.appendQueryParameter(keyValue[0], keyValue[1]);
              }
            }
          }
        }
      }

      return builder.build();
    } catch (Exception e) {
      Log.e(TAG, "Error building UPI URI from: " + deepLinkUri.toString(), e);
      return null;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == UPI_REQUEST_CODE) {
      Intent result = new Intent();

      if (data != null && data.getExtras() != null) {
        String status = data.getStringExtra("Status");
        String txnRef = data.getStringExtra("txnId");
        String response = data.getStringExtra("response");

        result.putExtras(data.getExtras());
        result.putExtra("Status", status);
        result.putExtra("txnId", txnRef);
        result.putExtra("Response", response);
        setResult(resultCode == Activity.RESULT_OK ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
      } else {
        result.putExtra("Status", "NO_RESPONSE");
        setResult(Activity.RESULT_CANCELED, result);
      }
      finish();
    }
  }
}

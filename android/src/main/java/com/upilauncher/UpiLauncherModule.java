package com.upilauncher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import java.util.List;

public class UpiLauncherModule extends ReactContextBaseJavaModule {

  public static final String NAME = "UpiLauncher";
  private static final int RN_UPI_REQUEST = 5678;

  private final ReactApplicationContext reactContext;
  private Promise pendingPromise;
  private boolean isListenerRegistered = false;

  // Listener for activity result from UPIInAppActivity
  private final ActivityEventListener activityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, @Nullable Intent data) {
      if (requestCode != RN_UPI_REQUEST) {
        return;
      }

      if (pendingPromise == null) {
        // No pending promise, but still remove listener to be safe
        removeListenerIfNeeded();
        return;
      }

      try {
        if (data != null && data.getExtras() != null) {
          Bundle extras = data.getExtras();
          // Bridge will convert Bundle to JS object
          pendingPromise.resolve(Arguments.fromBundle(extras));
        } else {
          WritableMap map = new WritableNativeMap();
          map.putString("Status", "NO_RESPONSE");
          pendingPromise.resolve(map);
        }
      } catch (Exception e) {
        pendingPromise.reject("UPI_RESULT_ERROR", e.getMessage());
      } finally {
        pendingPromise = null;
        removeListenerIfNeeded();
      }
    }
  };

  public UpiLauncherModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    // ❌ No listener registration here anymore
  }

  @NonNull
  @Override
  public String getName() {
    return NAME;
  }

  private void addListenerIfNeeded() {
    if (!isListenerRegistered) {
      reactContext.addActivityEventListener(activityEventListener);
      isListenerRegistered = true;
    }
  }

  private void removeListenerIfNeeded() {
    if (isListenerRegistered) {
      reactContext.removeActivityEventListener(activityEventListener);
      isListenerRegistered = false;
    }
  }

  /**
   * Opens a UPI payment intent using the given deep link URL.
   *
   * @param deepLinkUrl UPI deep link.
   * @param packageName Preferred package name (optional from JS, but Java receives a string).
   * @param promise     Promise to resolve/reject with the result.
   */
  @ReactMethod
  public void openUpiIntent(String deepLinkUrl, String packageName, Promise promise) {
    Activity current = getCurrentActivity();
    if (current == null) {
      promise.reject("NO_ACTIVITY", "Current activity is null");
      return;
    }

    if (pendingPromise != null) {
      // prevent duplicate calls
      promise.reject("ALREADY_RUNNING", "A UPI request is already in progress");
      return;
    }

    try {
      Intent intent = new Intent(current, UPIInAppActivity.class);

      if (deepLinkUrl != null && !deepLinkUrl.isEmpty()) {
        intent.putExtra(UPIInAppActivity.EXTRA_UPI_DEEP_LINK, deepLinkUrl);
      }
      if (packageName != null && !packageName.isEmpty()) {
        intent.putExtra(UPIInAppActivity.EXTRA_UPI_PACKAGE_NAME, packageName);
      }

      pendingPromise = promise;
      addListenerIfNeeded();
      current.startActivityForResult(intent, RN_UPI_REQUEST);
    } catch (Exception e) {
      pendingPromise = null;
      removeListenerIfNeeded();
      promise.reject("INTENT_ERROR", e.getMessage());
    }
  }

  /**
   * Fetches the list of installed UPI applications on the device.
   *
   * @param promise Promise resolving to an array of apps with `name` and `package`.
   */
  @ReactMethod
  public void fetchUpiApps(Promise promise) {
    try {
      PackageManager pm = reactContext.getPackageManager();

      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse("upi://pay"));

      List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);

      WritableArray appsArray = Arguments.createArray();

      for (ResolveInfo resolveInfo : resolveInfos) {
        WritableMap appMap = Arguments.createMap();

        String packageName = resolveInfo.activityInfo.packageName;
        CharSequence label = resolveInfo.loadLabel(pm);

        appMap.putString("package", packageName);
        appMap.putString("name", label != null ? label.toString() : packageName);

        appsArray.pushMap(appMap);
      }
      Log.d("UPILauncherModule", appsArray.toString());

      promise.resolve(appsArray);
    } catch (Exception e) {
      promise.reject("UPI_APP_LIST_ERROR", e.getMessage());
    }
  }
}

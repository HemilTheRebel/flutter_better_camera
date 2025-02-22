package io.flutter.plugins.camera;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener;

final class CameraPermissions {
  interface PermissionsRegistry {
    void addListener(RequestPermissionsResultListener handler);
  }

  interface ResultCallback {
    void onResult(String errorCode, String errorDescription);
  }

  private static final int CAMERA_REQUEST_ID = 9796;
  private boolean ongoing = false;

  void requestPermissions(
      Activity activity,
      PermissionsRegistry permissionsRegistry,
      boolean enableAudio,
      ResultCallback callback) {
    if (ongoing) {
      callback.onResult("cameraPermission", "Camera permission request ongoing");
    }
    if (!hasCameraPermission(activity) || (enableAudio && !hasAudioPermission(activity))) {
      permissionsRegistry.addListener(
          new OneTimeCameraRequestPermissionsListener(
              (String errorCode, String errorDescription) -> {
                ongoing = false;
                callback.onResult(errorCode, errorDescription);
              }));
      ongoing = true;
      ActivityCompat.requestPermissions(
          activity,
          enableAudio
              ? new String[] {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}
              : new String[] {Manifest.permission.CAMERA},
          CAMERA_REQUEST_ID);
    } else {
      // Permissions already exist. Call the callback with success.
      callback.onResult(null, null);
    }
  }

  private boolean hasCameraPermission(Activity activity) {
    return ContextCompat.checkSelfPermission(activity, permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED;
  }

  private boolean hasAudioPermission(Activity activity) {
    return ContextCompat.checkSelfPermission(activity, permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED;
  }

  /// Handles a successful result only once. After that all calls to
  /// onRequestPermissionsResult will return false.
  private static class OneTimeCameraRequestPermissionsListener
      implements PluginRegistry.RequestPermissionsResultListener {

    final ResultCallback callback;
    boolean active = true;

    private OneTimeCameraRequestPermissionsListener(ResultCallback callback) {
      this.callback = callback;
    }

    @Override
    public boolean onRequestPermissionsResult(int id, String[] permissions, int[] grantResults) {
      if (!active || id != CAMERA_REQUEST_ID) {
        return false;
      }

      active = false;
      if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
        callback.onResult("cameraPermission", "MediaRecorderCamera permission not granted");
      } else if (grantResults.length > 1
          && grantResults[1] != PackageManager.PERMISSION_GRANTED) {
        callback.onResult("cameraPermission", "MediaRecorderAudio permission not granted");
      } else {
        callback.onResult(null, null);
      }
      return true;
    }
  }
}

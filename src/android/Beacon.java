package cordova.plugin.beacon;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static org.apache.cordova.device.Device.TAG;

/**
 * This class echoes a string called from JavaScript.
 */
public class Beacon extends CordovaPlugin {

  private Boolean etatAdapterBLE = true;
  private BluetoothAdapter bluetoothAdapter = null;
  private BluetoothLeScanner bluetoothLeScanner;

  // callbacks
  CallbackContext discoverBLECallbackContext;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("initAdapter")) {
      this.initAdapter(callbackContext);
      return true;
    }
    if (action.equals("scan")) {
      JSONArray macs = (JSONArray) args.get(0);
      this.scan(macs, callbackContext);
      return true;
    }
    if (action.equals("stopScan")) {
      this.stopScan(callbackContext);
      return true;
    }
    if (action.equals("enableBLE")) {
      boolean turnBLE = args.getJSONObject(0).getBoolean("turn");
      this.enableBLE(turnBLE, callbackContext);
      return true;
    }
    if (action.equals("purgeBLE")) {
      this.purgeBLE(callbackContext);
      return true;
    }
    return false;
  }

  private void initAdapter(CallbackContext callbackContext) throws JSONException {
    JSONObject jsonResponse = new JSONObject();
    if (this.bluetoothAdapter == null) {
      Activity activity = cordova.getActivity();
      final BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
      try {
        bluetoothAdapter = bluetoothManager.getAdapter();
        jsonResponse.put("error", false);
        if (this.bluetoothAdapter.isEnabled())
          jsonResponse.put("bleIsActived", true);
        else
          jsonResponse.put("bleIsActived", false);
        callbackContext.success(jsonResponse);
      } catch (Exception e) {
        jsonResponse.put("error", true);
        jsonResponse.put("bleIsSupported", false);
        jsonResponse.put("message", e.getMessage());
        callbackContext.success(jsonResponse);
      }
    } else {
      jsonResponse.put("error", false);
      if (this.bluetoothAdapter.isEnabled())
        jsonResponse.put("bleIsActived", true);
      else
        jsonResponse.put("bleIsActived", false);
      callbackContext.success(jsonResponse);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void scan(JSONArray macs, CallbackContext callbackContext) throws JSONException {
    JSONObject jsonResponse = new JSONObject();
    if (this.etatAdapterBLE) {
      if (this.bluetoothAdapter.isEnabled()) {
        try {
          jsonResponse.put("error", false);
          this.discoverBLECallbackContext = callbackContext;

          bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
          ScanFilter filter = getScanFilter();

          List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
          for (int i = 0; i < macs.length(); i++) {
            ScanFilter.Builder builder = new ScanFilter.Builder();
            builder.setDeviceAddress((String) macs.get(i));
            scanFilters.add(builder.build());
          }

          ScanSettings scanSettings = getScanSettings();
          bluetoothLeScanner.startScan(scanFilters, scanSettings, mScanCallback);
        } catch (Exception e) {
          jsonResponse.put("error", true);
          jsonResponse.put("message", "Probleme durant la procedure de scan : " + e.getMessage());
          PluginResult result = new PluginResult(PluginResult.Status.OK, jsonResponse);
          result.setKeepCallback(true);
          callbackContext.sendPluginResult(result);
        }
      } else {
        jsonResponse.put("error", true);
        jsonResponse.put("bleIsActived", false);
        PluginResult result = new PluginResult(PluginResult.Status.OK, jsonResponse);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
      }
    } else {
      jsonResponse.put("error", true);
      jsonResponse.put("initAdapter", false);
      PluginResult result = new PluginResult(PluginResult.Status.OK, jsonResponse);
      result.setKeepCallback(true);
      callbackContext.sendPluginResult(result);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void stopScan(CallbackContext callbackContext) throws JSONException {
    JSONObject jsonResponse = new JSONObject();
    if (this.etatAdapterBLE) {
      jsonResponse.put("error", false);
      if (this.bluetoothAdapter.isEnabled()) {
        this.bluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
        jsonResponse.put("blsIsActived", true);
      } else
        jsonResponse.put("blsIsActived", false);
    } else {
      jsonResponse.put("error", true);
      jsonResponse.put("initAdapter", false);
    }
    callbackContext.success(jsonResponse);
  }

  private void enableBLE(Boolean turnBLE, CallbackContext callbackContext) throws JSONException {
    JSONObject jsonResponse = new JSONObject();
    if (this.etatAdapterBLE) {
      jsonResponse.put("error", false);
      if (turnBLE) {
        if (!bluetoothAdapter.isEnabled())
          this.bluetoothAdapter.enable();
        jsonResponse.put("blsIsActived", true);
      } else {
        if (bluetoothAdapter.isEnabled())
          this.bluetoothAdapter.disable();
        jsonResponse.put("blsIsActived", false);
      }
    } else {
      jsonResponse.put("error", true);
      jsonResponse.put("initAdapter", false);
    }
    callbackContext.success(jsonResponse);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void purgeBLE(CallbackContext callbackContext) throws JSONException {
    JSONObject jsonResponse = new JSONObject();
    if (this.etatAdapterBLE) {
      jsonResponse.put("error", false);
      if (this.bluetoothAdapter.isEnabled()) {
        jsonResponse.put("purgeBLE", true);
        jsonResponse.put("blsIsActived", true);
        this.bluetoothAdapter.getBluetoothLeScanner().flushPendingScanResults(mScanCallback);
      } else {
        jsonResponse.put("purgeBLE", false);
        jsonResponse.put("blsIsActived", false);
      }
    } else {
      jsonResponse.put("error", true);
      jsonResponse.put("initAdapter", false);
    }
    PluginResult result = new PluginResult(PluginResult.Status.OK, jsonResponse);
    result.setKeepCallback(true);
    callbackContext.sendPluginResult(result);
  }

  private ScanCallback mScanCallback = new ScanCallback() {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      String mac = result.getDevice().getAddress();
      Log.d(TAG, mac);

      JSONObject jsonResponse = new JSONObject();

      JSONObject axes = new JSONObject();
      JSONObject axesN = new JSONObject();
      JSONObject axesN1 = new JSONObject();
      JSONObject axesN2 = new JSONObject();

      byte[] bytes = result.getScanRecord().getBytes();

      // Axex N
      int axeX = bytes[5];
      axeX += bytes[4] << 8;
      int axeY = bytes[7];
      axeY += bytes[6] << 8;
      int axeZ = bytes[9];
      axeZ += bytes[8] << 8;

      // AxesN-1
      int axeXn1 = bytes[11];
      axeXn1 += bytes[10] << 8;
      int axeYn1 = bytes[13];
      axeYn1 += bytes[12] << 8;
      int axeZn1 = bytes[15];
      axeZn1 += bytes[14] << 8;

      // AxesN-2
      int axeXn2 = bytes[17];
      axeXn2 += bytes[16] << 8;
      int axeYn2 = bytes[19];
      axeYn2 += bytes[18] << 8;
      int axeZn2 = bytes[21];
      axeZn2 += bytes[20] << 8;

      // Index
      byte[] bytesIndex = { bytes[25], bytes[24], bytes[23], bytes[22] };
      int index = ByteBuffer.wrap(bytesIndex, 0, 4).getInt();

      // mode
      int mode = bytes[26];
      try {
        axesN.put("x", axeX);
        axesN.put("y", axeY);
        axesN.put("z", axeZ);

        axesN1.put("xn1", axeXn1);
        axesN1.put("yn1", axeYn1);
        axesN1.put("zn1", axeZn1);

        axesN2.put("xn2", axeXn2);
        axesN2.put("yn2", axeYn2);
        axesN2.put("zn2", axeZn2);

        axes.put("N", axesN);
        axes.put("N1", axesN1);
        axes.put("N2", axesN2);
      } catch (JSONException e) {
        e.printStackTrace();
        PluginResult result1 = new PluginResult(PluginResult.Status.OK, "Erreur decode payload : " + e.getMessage());
        result1.setKeepCallback(true);
        discoverBLECallbackContext.sendPluginResult(result1);
      }
      try {
        jsonResponse.put("name", result.getDevice().getName());
        jsonResponse.put("rssi", result.getRssi());
        jsonResponse.put("id", result.getDevice().getAddress());
        jsonResponse.put("advertising", axes);
        jsonResponse.put("index", index);
        jsonResponse.put("mode", mode);
      } catch (JSONException e) {
        PluginResult result1 = new PluginResult(PluginResult.Status.OK, "Erreur create response : " + e.getMessage());
        result1.setKeepCallback(true);
        discoverBLECallbackContext.sendPluginResult(result1);
      }
      PluginResult result1 = new PluginResult(PluginResult.Status.OK, jsonResponse);
      result1.setKeepCallback(true);
      discoverBLECallbackContext.sendPluginResult(result1);
    }

  };

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private ScanSettings getScanSettings() {
    ScanSettings.Builder builder = new ScanSettings.Builder();
    builder.setReportDelay(0);
    builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
    return builder.build();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private ScanFilter getScanFilter() {
    ScanFilter.Builder builder = new ScanFilter.Builder();
    ByteBuffer manData = ByteBuffer.allocate(2); // The sensors only sends 6 bytes right now
    ByteBuffer manMask = ByteBuffer.allocate(2);
    manData.put(0, (byte) 0xFF);
    manData.put(1, (byte) 0xFE);
    for (int i = 0; i < 2; i++) {
      manMask.put((byte) 0x01);
    }
    builder.setManufacturerData(65534, manData.array(), manMask.array()); // Is this id correct?
    return builder.build();
  }
}

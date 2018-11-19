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
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Base64;

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

/**
 * This class echoes a string called from JavaScript.
 */
public class Beacon extends CordovaPlugin {

  private Boolean etatAdapterBLE = true;
  private BluetoothAdapter bluetoothAdapter;
  private BluetoothLeScanner bluetoothLeScanner;

  // callbacks
  CallbackContext discoverBLECallbackContext;

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("scan")) {
      String message = args.getString(0);
      this.scan(args, callbackContext);
      return true;
    }
    return false;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void scan(JSONArray args, CallbackContext callbackContext)
  {
    initBluetoothAdapter();
    if(this.etatAdapterBLE)
    {
      try{
        this.discoverBLECallbackContext = callbackContext;

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanFilter filter = getScanFilter();
        List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
        //scanFilters.add(filter);
        ScanSettings scanSettings = getScanSettings();

        bluetoothLeScanner.startScan(scanFilters, scanSettings, mScanCallback);

      } catch (Exception e){
        callbackContext.error("Probleme durant la procedure de scan : " + e.getMessage());
      }
    }
    else{
      callbackContext.error("Les conditions de demarrage de la BLE ne sont pas remplis");
    }
  }

  private ScanCallback mScanCallback = new ScanCallback() {
    @TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      String mac = result.getDevice().getAddress();
      if(mac.equals("03:80:E1:00:34:12") || mac.equals("03:80:E1:00:34:13") || mac.equals("03:80:E1:00:34:14")
          || mac.equals("03:80:E1:00:34:15") || mac.equals("03:80:E1:00:34:16") || mac.equals("03:80:E1:00:34:17")
          || mac.equals("03:80:E1:00:34:18") ||mac.equals("03:80:E1:00:34:19") || mac.equals("03:80:E1:00:34:20")
        )
      {

        JSONObject obj = new JSONObject();
        JSONObject axes = new JSONObject();
        JSONObject index = new JSONObject();

        byte[] bytes = result.getScanRecord().getBytes();
        int axeX = bytes[5];
        axeX += bytes[4] << 8;
        int axeY = bytes[7];
        axeY += bytes[6] << 8;
        int axeZ = bytes[9];
        axeZ += bytes[8] << 8;

        int index1 = (bytes[10] & 0xFF);
        int index2 = (bytes[11] & 0xFF);
        int index3 = (bytes[12] & 0xFF);
        int index4 = (bytes[13] & 0xFF);
        int res =  index1 + (index2 * 256) + (index3 * index2) + (index3 * index4);

        int i = bytes[10];
        i += bytes[11] << 8;
        i += bytes[12] << 16;
        i += bytes[13] << 24;

        try {
          axes.put("x", axeX);
          axes.put("y", axeY);
          axes.put("z", axeZ);
          index.put("index", i);

        } catch (JSONException e) {
          e.getMessage();
        }

        try {
          obj.put("name", result.getDevice().getName());
          obj.put("rssi", result.getRssi());
          obj.put("id", result.getDevice().getAddress());
          obj.put("advertising", axes);
          obj.put("index", res);
        } catch (JSONException e) {
          PluginResult result1 = new PluginResult(PluginResult.Status.OK, e.getMessage());
          result1.setKeepCallback(true);
          discoverBLECallbackContext.sendPluginResult(result1);
        }
        PluginResult result1 = new PluginResult(PluginResult.Status.OK, obj);
        result1.setKeepCallback(true);
        discoverBLECallbackContext.sendPluginResult(result1);
      }
    }
  };

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private ScanSettings getScanSettings(){
    ScanSettings.Builder builder = new ScanSettings.Builder();
    builder.setReportDelay(0);
    builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
    return builder.build();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private ScanFilter getScanFilter(){
    ScanFilter.Builder builder = new ScanFilter.Builder();
    ByteBuffer manData = ByteBuffer.allocate(2); //The sensors only sends 6 bytes right now
    ByteBuffer manMask = ByteBuffer.allocate(2);
    manData.put(0, (byte)0xFF);
    manData.put(1, (byte)0xFE);
    for(int i = 0; i < 2; i++){
      manMask.put((byte)0x01);
    }
    builder.setManufacturerData(65534, manData.array(), manMask.array()); //Is this id correct?
    return builder.build();
  }

  private void initBluetoothAdapter() {
    Activity activity = cordova.getActivity();
    final BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
    bluetoothAdapter = bluetoothManager.getAdapter();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  static JSONObject scanResultToJson(ScanResult result) throws JSONException {
    JSONObject object = new JSONObject();
    JSONObject subObject = new JSONObject();
    byte [] dataAdvertising = result.getScanRecord().getBytes();
    try {
      if(!result.getDevice().getName().equals(""))
        object.put("Name", result.getDevice().getName());
      object.put("id", result.getDevice().getAddress());
      object.put("axex", subObject);
    }catch (Exception e){

    }
    return object;
  }

}

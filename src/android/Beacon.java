package cordova.plugin.beacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.support.annotation.RequiresApi;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * This class echoes a string called from JavaScript.
 */
public class Beacon extends CordovaPlugin {

    private BluetoothAdapter bluetoothAdapter;
    private Boolean etatAdapterBLE = false;

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
        if(this.etatAdapterBLE)
        {
          try{
            this.discoverBLECallbackContext = callbackContext;

            BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            ScanFilter filter = getScanFilter();
            List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
            scanFilters.add(filter);
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
      @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
      @Override
      public void onScanResult(int callbackType, ScanResult result) {
        PluginResult result1 = new PluginResult(PluginResult.Status.OK, result.getScanRecord().getBytes());
        result1.setKeepCallback(true);
        discoverBLECallbackContext.sendPluginResult(result1);
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
}

package com.pranayankittiru.diabeat;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class DashboardActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ClientActivity";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // Bluetooth config and status settings
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private Map<String, BluetoothDevice> mScanResults;
    private ScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private String mBluetoothDeviceAddress = "30:AE:A4:13:C0:16";
    private BluetoothGatt mGatt;
    private BluetoothDevice device;
    private BluetoothGattDescriptor descriptor = null;

    // GATT connections states
    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private UUID device_service = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private UUID char_write = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M permission check
            if(this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        requestPermissions(new String[] {android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        //TODO - Check if ble device is already connected or not
        if(mBluetoothAdapter.isEnabled()) {
            TextView status = findViewById(R.id.ble_status);
            status.setText("On");
        }
        else {
            TextView status = findViewById(R.id.ble_status);
            status.setText("Off");
        }
    }

    // OnClick handlers for buttons
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if(i == R.id.ble_on) {
            Log.d(TAG,"Connect Triggered");
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                //TODO - Check if ble device is already connected or not
                //TextView status = findViewById(R.id.connect_status);
                //status.setText("Disconnected");
            }
        }
        if(i == R.id.search_device) {
            Log.d(TAG,"Search Triggered");
            scanLeDevice();
        }
        if(i == R.id.gatt_connect) {
            connectGattDevice(device);
        }
    }

    // Scan functions
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void scanLeDevice() {

        // Set scan mode to low power mode
        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        // Create hashmap to store scan results
        mScanResults = new HashMap<>();

        // Create a scan callback
        mScanCallback = new BtleScanCallback(mScanResults);

        // Get Bluetooth LE scanner
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        }, SCAN_PERIOD);

        mScanning = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopScan() {
        if(mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            scanComplete();
        }

        mScanCallback = null;
        mScanning = false;
        mHandler = null;
        Log.e(TAG,"Stopped scanning.");
    }

    private void scanComplete() {
        if (mScanResults.isEmpty()) {
            return;
        }
        for (String deviceAddress : mScanResults.keySet()) {
            //Toast.makeText(this, deviceAddress, Toast.LENGTH_LONG).show();
            if(deviceAddress.equals(mBluetoothDeviceAddress)) {
                Toast.makeText(this, deviceAddress, Toast.LENGTH_LONG).show();
                device = mScanResults.get(deviceAddress);
                break;
            }
        }
    }

    // GATT actions
    private void connectGattDevice(BluetoothDevice device) {
        mGatt = device.connectGatt(this, true, new BluetoothGattCallback() {
           @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
               if(newState == BluetoothProfile.STATE_CONNECTED) {
                   Log.i(TAG, "Connected to GATT server");
                   Log.i(TAG, "Attempting to start discovery: " + mGatt.discoverServices());
               }
           }

           @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
               if(status == BluetoothGatt.GATT_SUCCESS) {
                   BluetoothGattService service = gatt.getService(device_service);
                   List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                   Log.i(TAG, "Characteristic: " + characteristics.get(0).getUuid());
                   mGatt.setCharacteristicNotification(characteristics.get(1), true);
                   descriptor = characteristics.get(1).getDescriptor(characteristics.get(1).getUuid());
                   descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                   mGatt.writeDescriptor(descriptor);
               }
           }

           @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
               final byte[] data = characteristic.getValue();
               Log.i(TAG, new String(data) + "mg/dl");
               DashboardActivity.this.runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       TextView reading = findViewById(R.id.glucose_level);
                       String g_level = new String(data) + "mg/dl";
                       reading.setText(g_level);
                   }
               });
           }
        });
    }


    // Callbacks defined here!
    // ScanCallback
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class BtleScanCallback extends ScanCallback {

        private Map<String, BluetoothDevice> mScanResults;

        BtleScanCallback(Map<String, BluetoothDevice> scanResults) {
            mScanResults = scanResults;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errCode) {
            Log.e(TAG, "BLE Scan failed with code " + errCode);
        }

        private void addScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceAddress = device.getAddress();
            mScanResults.put(deviceAddress, device);
        }
    }

    // Request for permission - ACCESS_COARSE_LOCATION
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }
}

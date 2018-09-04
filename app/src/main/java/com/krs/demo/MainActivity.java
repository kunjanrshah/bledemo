package com.krs.demo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";
    BluetoothDevice bluetoothDevice;
    Button btnScan, btnTare;
    private BluetoothAdapter mBluetoothAdapter;
    private EditText edt_gross_wt = null, edt_tare_wt = null, edt_net_wt = null, edtLotNo = null, edtBaleNo = null;
    private TextView txtStatus = null, txtSrNo = null;
    private TextClock textClock = null;
    private BluetoothLEService mBluetoothLEService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLEService = ((BluetoothLEService.LocalBinder) service).getService();
            if (!mBluetoothLEService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            txtStatus.setText("Connecting device step2");
            btnTare.setEnabled(true);
            mBluetoothLEService.connect(bluetoothDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLEService = null;
            btnTare.setEnabled(false);
        }
    };
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLEService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState("connected");
                invalidateOptionsMenu();
            } else if (BluetoothLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                edt_gross_wt.setText("OFF");
                updateConnectionState("disconnected");
                //clearUI();
            } else if (BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLEService.getSupportedGattServices());
            } else if (BluetoothLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] dataInput = mNotifyCharacteristic.getValue();
                displayData(dataInput);
            }
        }
    };
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean mScanning;
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            txtStatus.setText("Finding device");
            // if(bluetoothDevice.getAddress().equalsIgnoreCase("C8:FD:19:4B:1F:09"))
            //{
            if (mScanning) {
                mScanning = false;
                bluetoothDevice = result.getDevice();
                Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLEService.class);
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

            }
            txtStatus.setText("Connecting device step1");
            //  startScanning(false);


            //}

            //  deviceAddress.setText(bluetoothDevice.getAddress());
            //  deviceName.setText(bluetoothDevice.getName());
            //  progressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "Scanning Failed " + errorCode);
            // progressBar.setVisibility(View.INVISIBLE);
        }
    };

    private static IntentFilter GattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void updateConnectionState(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(status);
            }
        });
    }

    private void displayData(byte[] data) {
        if (data != null) {
            String output = "";
            for (int i = 0; i < data.length; i++) {
                output = output + Character.toString((char) data[i]);
                edt_gross_wt.setText(output);
            }
        }
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String serviceString = "unknown service";
        String charaString = "unknown characteristic";

        for (BluetoothGattService gattService : gattServices) {

            uuid = gattService.getUuid().toString();

            serviceString = SampleGattAttributes.lookup(uuid);

            if (serviceString != null) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
                    uuid = gattCharacteristic.getUuid().toString();
                    charaString = SampleGattAttributes.lookup(uuid);
                    if (charaString != null) {
                        //  serviceName.setText(charaString);
                    }
                    mNotifyCharacteristic = gattCharacteristic;
                    if (mNotifyCharacteristic != null) {
                        final int charaProp = mNotifyCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            mBluetoothLEService.readCharacteristic(mNotifyCharacteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mBluetoothLEService.setCharacteristicNotification(mNotifyCharacteristic, true);
                        }
                    }
                    return;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        textClock = (TextClock) findViewById(R.id.textClock);
        txtSrNo = (TextView) findViewById(R.id.txtSrNo);
        edt_gross_wt = (EditText) findViewById(R.id.edt_gross_wt);
        edt_net_wt = (EditText) findViewById(R.id.edt_net_wt);
        edt_tare_wt = (EditText) findViewById(R.id.edt_tare_wt);
        edtLotNo = (EditText) findViewById(R.id.edtLotNo);
        edtBaleNo = (EditText) findViewById(R.id.edtBaleNo);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        btnScan = (Button) findViewById(R.id.btnScan);

        mBluetoothAdapter = BluetoothUtils.getBluetoothAdapter(MainActivity.this);
        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScanning(true);
            }
        });
        findViewById(R.id.btnTare).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLEService.writeCharacteristic(mNotifyCharacteristic, "T");
                } else {
                    Toast.makeText(MainActivity.this, "Please connect again!", Toast.LENGTH_SHORT).show();
                }

            }
        });
        findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject mjsonobject = new JSONObject();
                try {
                    mjsonobject.put("sr_no", txtSrNo.getText().toString());
                    mjsonobject.put("lot_no", edtLotNo.getText().toString());
                    mjsonobject.put("bale_no", edtBaleNo.getText().toString());
                    mjsonobject.put("gross_wt", edt_gross_wt.getText().toString());
                    mjsonobject.put("tare_wt", edt_tare_wt.getText().toString());
                    mjsonobject.put("net_wt", edt_net_wt.getText().toString());
                } catch (Exception e) {
                    e.getMessage().toString();
                }
                String filename = "superb_" + edtLotNo.getText().toString().trim() + "_lot.xls";
                if (txtSrNo.getText().toString().equalsIgnoreCase("1")) {
                    Constants.exportToExcel(mjsonobject, filename, true);
                } else {
                    Constants.exportToExcel(mjsonobject, filename, false);
                }
                int no = Integer.parseInt(txtSrNo.getText().toString()) + 1;
                txtSrNo.setText("" + no);
                int bale_no = Integer.parseInt(edtBaleNo.getText().toString()) + 1;
                edtBaleNo.setText("" + bale_no);
                Toast.makeText(MainActivity.this, "Written in " + filename, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtSrNo.setText("1");
                edtBaleNo.setText("1");
                if (!edtLotNo.getText().toString().isEmpty()) {
                    int lot = Integer.parseInt(edtLotNo.getText().toString()) + 1;
                    edtLotNo.setText("" + lot);
                } else {
                    edtLotNo.setText("1");
                }
                Log.d(TAG, "step btnSubmit");
            }
        });

        findViewById(R.id.btnExport1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LayoutInflater li = LayoutInflater.from(MainActivity.this);
                View promptsView = li.inflate(R.layout.prompts, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                alertDialogBuilder.setView(promptsView);
                final EditText userInput = (EditText) promptsView.findViewById(R.id.edtdlgLotNo);
                userInput.setText(edtLotNo.getText().toString());
                // set dialog message
                alertDialogBuilder.setCancelable(false).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String filename = "superb_" + userInput.getText().toString().trim() + "_lot.xls";
                        File file = Constants.getFile(filename);
                        ExportAlert(file);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        });

        textClock.setFormat12Hour("dd/MM/yyyy hh:mm:ss a EEE");
        textClock.setFormat24Hour(null);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, Constants.REQUEST_LOCATION_ENABLE_CODE);
        }
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Your devices that don't support BLE", Toast.LENGTH_LONG).show();
            finish();
        }
        if (!mBluetoothAdapter.enable()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, Constants.REQUEST_BLUETOOTH_ENABLE_CODE);
        }

        if (mBluetoothLEService != null) {
            final boolean result = mBluetoothLEService.connect(bluetoothDevice.getAddress());
            Log.d(TAG, "Connect request result=" + result);
        }

        registerReceiver(mGattUpdateReceiver, GattUpdateIntentFilter());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private void ExportAlert(final File file) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.app_name));

        builder.setMessage("Data Exported in a Excel Sheet");
        builder.setNegativeButton("Share", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@NonNull DialogInterface dialog, int which) {


                Uri photoURI = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + ".provider", file);

                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                intentShareFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (file.exists()) {
                    intentShareFile.setType("application/xls");
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, photoURI);
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT, "Sharing File...");
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");
                    startActivity(Intent.createChooser(intentShareFile, "Share File"));
                }
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(@NonNull DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }

    private void startScanning(final boolean enable) {

        Handler mHandler = new Handler();
        if (enable) {
            List<ScanFilter> scanFilters = new ArrayList<>();
            final ScanSettings settings = new ScanSettings.Builder().build();
            ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SampleGattAttributes.UUID_BATTERY_SERVICE)).build();
            scanFilters.add(scanFilter);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    btnScan.setText("Start");
                    txtStatus.setText("Scanning Stopped");
                    bluetoothLeScanner.stopScan(scanCallback);
                }
            }, Constants.SCAN_PERIOD);

            mScanning = true;
            btnScan.setText("Stop");
            txtStatus.setText("Scanning Started");
            bluetoothLeScanner.startScan(scanFilters, settings, scanCallback);
        } else {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    btnScan.setText("Start");
                    txtStatus.setText("Scanning Stopped");
                    bluetoothLeScanner.stopScan(scanCallback);
                }
            });

        }
    }
}

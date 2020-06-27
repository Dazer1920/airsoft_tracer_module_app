package com.example.tracer;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

import static com.example.tracer.Util.setListViewHeightBasedOnChildren;

public class Connect extends Activity {
    public ProgressDialog progressDialog;
    private String nameDevice;

    private static Connect connectInstance;

    public static Connect getInstance() {
        return connectInstance;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect);
        connectInstance = this;

        setupScaner();
        setupPairedList();
        checkBTPermissions();
    }

    public void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            }
        } else {
            Bluetooth.getInstance().startLeScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Bluetooth.getInstance().startLeScan();
            }
        }
    }

    private ProgressBar progressBar;
    private Button scanButton;
    private ListView deviceList;

    ArrayAdapter<String> scanDevicesArray;
    int countDevises = 0;

    public void OnStartScan() {
        scanButton.setEnabled(false);
        countDevises = 0;
        progressBar.setVisibility(View.VISIBLE);
        scanDevicesArray.clear();
    }

    public void OnStopScan() {
        progressBar.setVisibility(View.INVISIBLE);
        scanButton.setEnabled(true);
    }

    public void OnNewDevice(BluetoothDevice device) {
        for(int i = 0; i < scanDevicesArray.getCount(); i++) {
            String a = scanDevicesArray.getItem(i).substring(scanDevicesArray.getItem(i).length() - 17);
            if(a.equals(device.getAddress())) {
                return;
            }
        }

        countDevises++;
        scanDevicesArray.add(countDevises + ". " + device.getName() + '\n' + "    " + device.getAddress());
        setListViewHeightBasedOnChildren(deviceList);
    }

    private void setupScaner() {
        scanDevicesArray = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        scanButton = (Button) findViewById(R.id.scanButton);

        deviceList = (ListView) findViewById(R.id.listDevices);
        deviceList.setAdapter(scanDevicesArray);
        setOnClickItem(deviceList);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bluetooth.getInstance().startLeScan();
            }
        });
    }

    private void setupPairedList() {
        ListView pairedDevises = (ListView) findViewById(R.id.parDevices);
        int countPairedDevice = 0;

        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1);
        Set<BluetoothDevice> pairedDevices = Bluetooth.getInstance().bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                countPairedDevice++;
                pairedDevicesAdapter.add(countPairedDevice + ". " + device.getName() + '\n' + "    " + device.getAddress());
            }
        }

        if (pairedDevicesAdapter.getCount() == 0) {
            pairedDevicesAdapter.add("No devices");
        }

        pairedDevises.setAdapter(pairedDevicesAdapter);
        setListViewHeightBasedOnChildren(pairedDevises);
        setOnClickItem(pairedDevises);
    }

    public void StartConnect() {
        progressDialog = ProgressDialog.show(Connect.this,
                "Connect to " + Bluetooth.getInstance().device.getName(),
                "Connecting...");
    }

    public void Connected() {
        if(progressDialog != null) progressDialog.dismiss();
        finish();
    }

    private void setOnClickItem(ListView listView) {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(Bluetooth.getInstance().bluetoothAdapter.isEnabled()) {
                    String device = ((TextView) view).getText().toString();

                    if(device.equals("No devices")) return;

                    String address = device.substring(device.length() - 17);

                    BluetoothDevice bluetoothDevice = Bluetooth.getInstance().bluetoothAdapter.getRemoteDevice(address);
                    if(bluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {

                        Bluetooth.getInstance().Connect(bluetoothDevice, Connect.this);
                    }
                }
            }
        });
    }
}

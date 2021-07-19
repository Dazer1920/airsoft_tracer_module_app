package com.example.tracer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.UUID;

import static com.example.tracer.Tracer.setMessage;

public class Bluetooth {
        private static Bluetooth bluetooth = new Bluetooth();

    public static Bluetooth getInstance() {
        return bluetooth;
    }

    private Bluetooth() { }

    public static final int BLUETOOTH_STATUS = 1;

    public static final int STATUS_START_CONNECT = 0;
    public static final int STATUS_CONNECTED = 1;
    public static final int STATUS_DISCONNECTED = 2;

    public static final int STATUS_START_CSAN = 4;
    public static final int STATUS_NEW_DEVICE = 5;
    public static final int STATUS_STOP_SCAN = 6;

    public static final int SET_ERROR = 3;

    public static final int DEVICE_NOT_FOUND_1 = 133;
    public static final int DEVICE_NOT_FOUND_2 = 62;
    public static final int DISCONNECTED_BY_DEVICE = 19;
    public static final int DEVICE_WENT_OUT_OF_RANGE = 8;

    public static UUID HM_DEESCRIPTION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static UUID HM_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public static UUID HM_CHARACTERISTICS = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private boolean StateConnect = false;
    Handler handler;

    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService bluetoothGattServer;
    public BluetoothDevice device;
    public BluetoothAdapter bluetoothAdapter;

    public boolean isStateConnect() {
        return StateConnect;
    }

    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    setMessage(BLUETOOTH_STATUS, STATUS_NEW_DEVICE, 0, device);
                }
            };

    public void startLeScan() {
        setMessage(BLUETOOTH_STATUS, STATUS_START_CSAN, 0, null);
        handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setMessage(BLUETOOTH_STATUS, STATUS_STOP_SCAN, 0, null);
                        bluetoothAdapter.stopLeScan(leScanCallback);
                    }
                }, 20000);

                bluetoothAdapter.startLeScan(leScanCallback);
            }
        }).start();
    }

    public void stopLeScan() {
        setMessage(BLUETOOTH_STATUS, STATUS_STOP_SCAN, 0, null);
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    public void Connect(BluetoothDevice bluetoothDevice, Context c) {
        device = bluetoothDevice;
        setMessage(BLUETOOTH_STATUS, STATUS_START_CONNECT, 0, false);
        bluetoothGatt = device.connectGatt(c, false, bluetoothGattCallback);
    }

    public void Disconnect() {
        if(bluetoothGatt == null) return;
        bluetoothGatt.close();
        bluetoothGatt = null;
        StateConnect = false;

        setMessage(BLUETOOTH_STATUS, STATUS_DISCONNECTED, 0, true);
    }

    public void writeCharacteristic(byte[] value){
        if(!StateConnect) return;

        bluetoothGattServer = bluetoothGatt.getService(HM_SERVICE);
        characteristicTX = bluetoothGattServer.getCharacteristic(HM_CHARACTERISTICS);

        characteristicTX.setValue(value);
        bluetoothGatt.writeCharacteristic(characteristicTX);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void readData(BluetoothGattCharacteristic characteristic) {
        if(characteristic.getUuid().equals(HM_CHARACTERISTICS)) {
            for(int i = 0; i < characteristic.getValue().length; i++) {
                Tracer.getInstance().readData(characteristic.getValue()[i]);
            }
        }
    }

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if(newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                StateConnect = true;

            } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                StateConnect = false;

                setMessage(BLUETOOTH_STATUS, STATUS_DISCONNECTED, 0, false);
                setMessage(BLUETOOTH_STATUS, SET_ERROR, status, null);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(HM_SERVICE).getCharacteristic(HM_CHARACTERISTICS);
            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(HM_DEESCRIPTION);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            readData(characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            characteristicRX = bluetoothGatt.getService(HM_SERVICE).getCharacteristic(HM_CHARACTERISTICS);
            bluetoothGatt.setCharacteristicNotification(characteristicRX, true);
            bluetoothGatt.readCharacteristic(characteristicRX);

            setMessage(BLUETOOTH_STATUS, STATUS_CONNECTED, 0, null);
        }
    };
}

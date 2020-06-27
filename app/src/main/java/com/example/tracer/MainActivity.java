package com.example.tracer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import static com.example.tracer.Bluetooth.BLUETOOTH_STATUS;
import static com.example.tracer.Bluetooth.DEVICE_NOT_FOUND_1;
import static com.example.tracer.Bluetooth.DEVICE_NOT_FOUND_2;
import static com.example.tracer.Bluetooth.DEVICE_WENT_OUT_OF_RANGE;
import static com.example.tracer.Bluetooth.DISCONNECTED_BY_DEVICE;
import static com.example.tracer.Bluetooth.SET_ERROR;
import static com.example.tracer.Bluetooth.STATUS_CONNECTED;
import static com.example.tracer.Bluetooth.STATUS_DISCONNECTED;
import static com.example.tracer.Bluetooth.STATUS_NEW_DEVICE;
import static com.example.tracer.Bluetooth.STATUS_START_CONNECT;
import static com.example.tracer.Bluetooth.STATUS_START_CSAN;
import static com.example.tracer.Bluetooth.STATUS_STOP_SCAN;
import static com.example.tracer.Pref.getData;
import static com.example.tracer.Pref.saveData;
import static com.example.tracer.Tracer.SET_SPEED;
import static com.example.tracer.Tracer.SET_STATE_TRACER;
import static com.example.tracer.Tracer.TRACER;

public class MainActivity extends Activity {
    public static final int BLUETOOTH_ENABLE_CODE = 10;
    private static MainActivity mainActivity;

    private Button ConnectButton, CleareButton;
    private TextView SpeedText, EnergyText, ConnectText;
    private EditText MassBalText;
    private Switch stateTracer;

    public static MainActivity getInstance() {
        return mainActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mainActivity = this;

        Bluetooth.getInstance().bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Handler statusHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                processHandler(msg);
            }
        };
        Tracer.getInstance().setStatusHandler(statusHandler);

        stateTracer = (Switch) findViewById(R.id.enTracer);
        ConnectButton = (Button) findViewById(R.id.ConnectButt);
        SpeedText = (TextView) findViewById(R.id.speedText);
        EnergyText = (TextView) findViewById(R.id.EnergyText);
        ConnectText = (TextView) findViewById(R.id.ConnectText);
        MassBalText = (EditText) findViewById(R.id.massText);
        CleareButton = (Button) findViewById(R.id.CleareButt);

        stateTracer.setChecked(getData("EnableTracer", false));
        stateTracer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveData("EnableTracer", isChecked);
                Tracer.getInstance().TransmitFrame(new byte[] {4, (byte) ((isChecked)? 1 : 0)});
            }
        });

        CleareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EnergyText.setText("Muzzle energy:");
                SpeedText.setText("Speed:");
            }
        });

        ConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Bluetooth.getInstance().isStateConnect()) Bluetooth.getInstance().Disconnect();
                else {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, BLUETOOTH_ENABLE_CODE);
                }
            }
        });

        MassBalText.setText(String.valueOf(getData("massBallKey", 0.2f)));
        MassBalText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(s.toString() != null && !s.toString().equals("")) {
                    float data = Float.parseFloat(s.toString());
                    saveData("massBallKey", data);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == BLUETOOTH_ENABLE_CODE) {
            if(resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(MainActivity.this, Connect.class);
                startActivity(intent);
            }
        }
    }

    private void processTracer(Message msg) {
        switch (msg.arg1) {
            default: break;

            case SET_SPEED:
                float speed = (float) msg.obj;
                float mass = getData("massBallKey", 0.2f) / 1000;
                float energy = (speed * speed * mass) / 2;
                EnergyText.setText("Muzzle energy: " + String.format("%.2f", energy) + "J");
                SpeedText.setText("Speed: " + String.format("%.2f", speed) + "m/s");
                break;

            case SET_STATE_TRACER:
                stateTracer.setChecked((boolean) msg.obj);
                break;
        }
    }

    private void processHandler(Message msg) {
        switch (msg.what) {
            default:break;

            case BLUETOOTH_STATUS:  processBluetoothStatus(msg); break;
            case TRACER: processTracer(msg); break;
        }
    }

    @SuppressLint("SetTextI18n")
    private void processBluetoothStatus(android.os.Message msg) {
        switch (msg.arg1) {
            default:break;

            case STATUS_START_CONNECT:
                Connect.getInstance().StartConnect();
                break;

            case STATUS_DISCONNECTED:
                ConnectButton.setText("Connect");
                ConnectText.setText("No connection");
                if((boolean) msg.obj) Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
                break;

            case STATUS_CONNECTED:
                Tracer.getInstance().TransmitFrame(new byte[] {5});
                ConnectButton.setText("Disconnect");
                Connect.getInstance().Connected();
                ConnectText.setText("Connected to " + Bluetooth.getInstance().device.getName() + '\n' +
                        Bluetooth.getInstance().device.getAddress());
                Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
                break;

            case SET_ERROR: processErrors(msg); break;

            case STATUS_START_CSAN: Connect.getInstance().OnStartScan(); break;
            case STATUS_NEW_DEVICE: Connect.getInstance().OnNewDevice((BluetoothDevice) msg.obj); break;
            case STATUS_STOP_SCAN: Connect.getInstance().OnStopScan(); break;

        }
    }

    private void processErrors(android.os.Message msg) {
        switch (msg.arg2) {
            default:break;

            case DEVICE_NOT_FOUND_1: Toast.makeText(this, "Connecting failed", Toast.LENGTH_SHORT).show(); break;
            case DEVICE_NOT_FOUND_2: Toast.makeText(this, "Connecting failed", Toast.LENGTH_SHORT).show(); break;
            case DISCONNECTED_BY_DEVICE: Toast.makeText(this, "Error transmit data", Toast.LENGTH_SHORT).show(); break;
            case DEVICE_WENT_OUT_OF_RANGE: Toast.makeText(this, "Error transmit data", Toast.LENGTH_SHORT).show(); break;
        }
    }
}

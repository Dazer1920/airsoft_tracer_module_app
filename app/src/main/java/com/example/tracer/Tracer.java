package com.example.tracer;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Tracer {
    public final static int TRACER = 3;

    public final static int SET_SPEED = 1;
    public final static int SET_STATE_TRACER = 2;

    private static Tracer tracer = new Tracer();

    public static Tracer getInstance() {
        return tracer;
    }

    private Tracer() {}

    private Handler statusHandler;

    final static int SIZE_RX_BUFF = 128;

    final static byte START_BYTE = (byte)  0x94;
    final static byte STOP_BYTE = (byte)  0xA3;

    static public void setMessage(int what, int arg1, int arg2, Object object) {
        if(getInstance().statusHandler != null) {
            Message msg = getInstance().statusHandler.obtainMessage(what, arg1, arg2, object);
            getInstance().statusHandler.sendMessage(msg);
        }
    }

    public static void setMessage(int code, Object obj, Handler h) {
        Message msg;
        msg = h.obtainMessage(code, obj);
        h.sendMessage(msg);
    }

    public void setStatusHandler(Handler handler) {
        statusHandler = handler;
    }

    public synchronized void TransmitFrame(byte[] data) {
        int sum = 0;
        byte[] trans = new byte[data.length + 3];

        for(int i = 0; i < data.length; i++) {
            trans[i + 2] = data[i];
            sum += data[i];
        }

        trans[0] = START_BYTE;
        trans[1] = (byte) ((~(sum + 1)) & 0x007f);
        trans[trans.length - 1] = STOP_BYTE;

        Bluetooth.getInstance().writeCharacteristic(trans);
    }

    private byte[] buffData = new byte[SIZE_RX_BUFF];
    private byte sum, countBuff, CheckSum;
    private boolean triggerBuff = false;

    public void readData(byte data) {
        if((data & 0x80) > 0) {
            if(data == START_BYTE) {
                sum = 0;
                countBuff = 0;
                CheckSum = 0;
                buffData = new byte[SIZE_RX_BUFF];
                triggerBuff = true;
            } else if(data == STOP_BYTE) {
                triggerBuff = false;

                if(CheckSum == ((~(sum + 1)) & 0x007f)) {
                    readeData(buffData);
                    Log.d("Bluetooth", "read");
                }
            }
        } else {
            if(triggerBuff) {
                if(countBuff == 0) CheckSum = data;
                else {
                    buffData[countBuff - 1] = data;
                    sum += data;
                }

                countBuff = (countBuff < buffData.length)? (byte) (countBuff + 1) : countBuff;
            }
        }
    }

    private void readeData(byte data[]) {
        if(data[0] == 0x03) {
            float val = (float) (((data[1] << 14) | (data[2] << 7) | data[3]) / 1000.0);
            setMessage(TRACER, SET_SPEED, 0, val);
        } if(data[0] == 0x04) {
            setMessage(TRACER, SET_STATE_TRACER, 0, data[1] > 0);
        }
    }

}

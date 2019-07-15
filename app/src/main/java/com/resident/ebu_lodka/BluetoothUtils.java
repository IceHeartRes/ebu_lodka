package com.resident.ebu_lodka;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;

public class BluetoothUtils {
//    Thread workerThread;

    private static final int REQUEST_ENABLE_BT = 1234;
    private static final byte HEADER_LENGTH = 2;
    private static final byte LENGTH_POS = 1;
    private static final byte CRC_LENGTH = 1;
    private static final byte FIELD_LENGTH = 5;

    private static final byte STX = 0x02;

    private static OutputStream os;

    private static BluetoothDataListener listener;

    static public void updateListener(BluetoothDataListener listener) {
        BluetoothUtils.listener = listener;
    }

    static public void findBT(Activity activity, BluetoothDataListener listener) throws IOException {
        BluetoothUtils.listener = listener;

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(activity, "No bluetooth adapter available", Toast.LENGTH_SHORT).show();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-05")) {
                    beginListenForData(device);
                    break;
                }
            }
        }

    }

    private static void beginListenForData(BluetoothDevice device) throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        BluetoothSocket mmSocket = device.createRfcommSocketToServiceRecord(uuid);
//        if (mmSocket.isConnected()){
        mmSocket.close();
//        }
        mmSocket = device.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        InputStream mmInputStream = mmSocket.getInputStream();
        os = mmSocket.getOutputStream();
        final Handler handler = new Handler();

        new Thread(() -> readFromSocket(mmInputStream, handler)).start();
    }

    public static void writeToSocket(byte[] data) throws IOException {
        if (os != null) {
            os.write(data);
        }
    }

    private static void readFromSocket(final InputStream inputStream, Handler handler) {
        byte[] data = new byte[0];
        try {
            while (true) {
                int available = inputStream.available();
                if (available > 0) {
                    byte[] buff = new byte[available];
                    inputStream.read(buff);
                    data = concatArrays(data, buff);
                    byte[] packet = findPacket(data);
                    if (checkPacket(packet)) {
                        handler.post(() -> listener.onData(packet));
                        data = new byte[0];
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] findPacket(byte[] bytes) {
        int stxPos = 0;
        boolean stxFind = false;

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == STX) {
                stxPos = i;
                stxFind = true;
                break;
            }
        }

        if (stxFind && bytes.length >= stxPos + 2) {
            byte length = (byte) (bytes[stxPos + LENGTH_POS] + HEADER_LENGTH + CRC_LENGTH);
            if (length <= bytes.length - stxPos) {
                return Arrays.copyOfRange(bytes, stxPos, stxPos + length);
            }
        }

        return new byte[0];
    }

    private static byte[] concatArrays(byte[] ar1, byte[] ar2) {
        byte[] result = new byte[ar1.length + ar2.length];
        System.arraycopy(ar1, 0, result, 0, ar1.length);
        System.arraycopy(ar2, 0, result, ar1.length, ar2.length);
        return result;
    }

    private static boolean checkPacket(byte[] bytes) {
        return checkCRC(bytes);
    }

    private static byte getCRC(byte[] bytes) {
        return bytes[bytes.length - 1];
    }

    private static boolean checkCRC(byte[] bytes) {
        if (bytes.length < 3) {
            return false;
        }
        final byte[] data = Arrays.copyOfRange(bytes, HEADER_LENGTH, bytes.length - CRC_LENGTH);
        int result = 0;
        for (byte b : data) {
            result += b;
            System.out.println(result & 0xFF);
        }
        return (result & 0xFF) == (getCRC(bytes) & 0xFF);
    }

    private static byte getDataLength(byte[] bytes) {
        return bytes[2];
    }

    public interface BluetoothDataListener {
        void onData(byte[] data);
    }
}

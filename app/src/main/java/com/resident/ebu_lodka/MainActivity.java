package com.resident.ebu_lodka;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1234;
    public static final int HEADER_LENGTH = 3;
    public static final int CRC_LENGTH = 1;
    private static final byte STX = 0x02;

    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Observable.fromCallable(() -> {
            final String header = new String(new byte[]{2, 50, 40});
            final String crc = new String(new byte[]{76});
            final String data = "t292_t0_t0_t0_f0p400_f100p400_i1_i1_o0_o0";
            return header + data + crc;
        })
                .map(this::getData)
                .map(this::parseData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::displayData, Throwable::printStackTrace);


        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth != null) {
            if (bluetooth.isEnabled()) {
                // Bluetooth включен. Работаем.
            } else {
                // Bluetooth выключен. Предложим пользователю включить его.
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            BluetoothDevice pairedDevice = null;
            Set<BluetoothDevice> pairedDevices = bluetooth.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-05")) {
                    pairedDevice = device;
                    break;
                }
            }

            if (pairedDevice == null) {
                Toast.makeText(this, "Не найдено устройство. Соеденитесь с устройством", Toast.LENGTH_LONG).show();
            } else {
                /*Observable.just(pairedDevice)
                        .map(device -> {
                            try {
                                UUID uuid = device.getUuids()[0].getUuid();
                                return device.createRfcommSocketToServiceRecord(uuid);
                            } catch (IOException e) {
                                throw new RuntimeException("Не удалось соедениться с устройством");
                            }
                        })
                        .map(bluetoothSocket -> {
                            try {
                                return bluetoothSocket.getInputStream();
                            } catch (IOException e) {
                                throw new RuntimeException("Не удалось соедениться с устройством");
                            }
                        })
                        .flatMap(this::readFromSocket)
                        .map(this::getData)
                        .map(this::parseData)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::displayData, Throwable::printStackTrace);*/
            }
        }
    }

    @Override
    protected void onResume() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 10, 10, locationListener);
        super.onResume();
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            showLocation(location);
        }

        @Override
        public void onProviderEnabled(String provider) {
//            checkEnabled();
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            showLocation(locationManager.getLastKnownLocation(provider));
        }

        @Override
        public void onProviderDisabled(String s) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(MainActivity.this, "Статус GPS: " + String.valueOf(status), Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void showLocation(Location location) {
        TextView speed = findViewById(R.id.speed);
        speed.setText(String.format("Скорость: %s км/ч", location.getSpeed()));
        TextView altitude = findViewById(R.id.altitude);
        altitude.setText(String.format("Скорость: %s м", location.getAltitude()));
    }

    private int mcTokmh(int mc) {
        return mc*3600/1000;
    }

    private Observable<String> readFromSocket(InputStream inputStream) {
        return Observable.create(subscriber -> {
            byte[] packet = new byte[0];
            try {
                while (true) {
                    int available = inputStream.available();
                    if (available > 0) {
                        byte[] buff = new byte[available];
                        inputStream.read(buff);
                        packet = concatArrays(packet, buff);
                        if (checkPacket(packet)) {
                            subscriber.onNext(new String(packet));
                            packet = new byte[0];
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    private byte[] concatArrays(byte[] ar1, byte[] ar2) {
        byte[] result = new byte[ar1.length + ar2.length];
        System.arraycopy(ar1, 0, result, 0, ar1.length);
        System.arraycopy(ar2, 0, result, ar1.length, ar2.length);
        return result;
    }

    private boolean checkPacket(byte[] bytes) {
        return checkHeader(bytes) &&
                checkStartPacket(bytes) &&
                checkDataLength(bytes) &&
                checkCRC(bytes);
    }

    private boolean checkHeader(byte[] bytes) {
        return bytes.length > HEADER_LENGTH;
    }

    private boolean checkDataLength(byte[] bytes) {
        return getDataLength(bytes) <= bytes.length - HEADER_LENGTH - CRC_LENGTH;
    }

    private boolean checkStartPacket(byte[] bytes) {
        return bytes[0] == STX;
    }

    private byte getCRC(byte[] bytes) {
        return bytes[bytes.length - 1];
    }

    private boolean checkCRC(byte[] bytes) {
        final byte[] data = Arrays.copyOfRange(bytes, HEADER_LENGTH, bytes.length - CRC_LENGTH - 1);
        int result = 0;
        for (byte b : data) {
            result += b;
            System.out.println(result & 0xFF);
        }
        return (result & 0xFF) == getCRC(bytes);
    }

    private byte getDataLength(byte[] bytes) {
        return bytes[2];
    }

    private Response parseData(String data) {
        final String[] split = data.split("_");
        final Response response = new Response();
        for (String param : split) {
            final Prefix prefix = Prefix.valueOf((char) param.getBytes()[0]);
            switch (prefix) {
                case TEMP: {
                    final String paramData = param.substring(1, param.length());
                    final BigDecimal temp = new BigDecimal(paramData).divide(new BigDecimal(10), 1, BigDecimal.ROUND_HALF_UP);
                    response.addTemp(temp);
                    break;
                }
                case FREQ: {
                    final String[] dates = param.split(Prefix.PERIOD.getStringPrefix());
                    final String tickCount = dates[0].substring(1, dates[0].length());
                    final String time = dates[1];

                    final int tickCountInt = Integer.parseInt(tickCount);
                    final int timeInt = Integer.parseInt(time);
                    final int freq = tickCountInt * 1000 * 60 / timeInt;
                    response.addFreq(freq);
                    break;
                }
                case PINSIN: {
                    final String paramData = param.substring(1, 2);
                    response.addPinsIn(Byte.parseByte(paramData));
                    break;
                }
                case PINSOUT: {
                    final String paramData = param.substring(1, 2);
                    response.addPinsOut(Byte.parseByte(paramData));
                    break;
                }
            }
        }
        return response;
    }


    private String getData(String packet) {
        return packet.substring(HEADER_LENGTH, packet.length() - CRC_LENGTH);
    }

    private void displayData(Response response) {
        List<BigDecimal> temps = response.getTemps();

        TextView temp1 = findViewById(R.id.temp1);
        temp1.setText(String.format("Температура 1:   %s C", temps.get(0).toPlainString()));
        TextView temp2 = findViewById(R.id.temp2);
        temp2.setText(String.format("Температура 2:   %s C", temps.get(1).toPlainString()));
        TextView temp3 = findViewById(R.id.temp3);
        temp3.setText(String.format("Температура 3:   %s C", temps.get(2).toPlainString()));
        TextView temp4 = findViewById(R.id.temp4);
        temp4.setText(String.format("Температура 4:   %s C", temps.get(3).toPlainString()));

        List<Integer> freqs = response.getFreqs();

        TextView freq1 = findViewById(R.id.freq1);
        freq1.setText(String.format(Locale.getDefault(), "Обороты двигателя:   %d об/мин", freqs.get(0)));
        TextView freq2 = findViewById(R.id.freq2);
        freq2.setText(String.format(Locale.getDefault(), "Расход топлива:   %d л/100км", freqs.get(1)));

        List<Byte> pinsIn = response.getPinsIn();

        TextView pinsIn1 = findViewById(R.id.pinsin1);
        pinsIn1.setText(String.format(Locale.getDefault(), "Вход 1:   %s", pinsIn.get(0) == 0 ? "выкл" : "вкл"));
        TextView pinsIn2 = findViewById(R.id.pinsin2);
        pinsIn2.setText(String.format(Locale.getDefault(), "Вход 2:   %s", pinsIn.get(1) == 0 ? "выкл" : "вкл"));

        List<Byte> pinsOut = response.getPinsOut();

        TextView pinsOut1 = findViewById(R.id.pinsout1);
        pinsOut1.setText(String.format(Locale.getDefault(), "Выход 1:   %s", pinsOut.get(0) == 0 ? "выкл" : "вкл"));
        TextView pinsOut2 = findViewById(R.id.pinsout2);
        pinsOut2.setText(String.format(Locale.getDefault(), "Выход 2:   %s", pinsOut.get(1) == 0 ? "выкл" : "вкл"));


    }
}

package com.resident.ebu_lodka;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.anastr.speedviewlib.SpeedView;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1234;
    public static final byte HEADER_LENGTH = 2;
    public static final byte LENGTH_POS = 1;
    public static final byte CRC_LENGTH = 1;
    public static final byte FIELD_LENGTH = 5;

    private static final byte STX = 0x02;

    private LocationManager locationManager;

    BluetoothUtils.BluetoothDataListener listener = data -> {
        byte[] data1 = getData(data);
        Response response = parseData(data1);
        App.getInstance(MainActivity.this).setResponse(response);
        displayData(response);
    };

    private byte switchState = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        App app = App.getInstance(this);

        if (!app.isConnected().getAndSet(true)) {
            startConnect();
        } else {
            displayData(app.getResponse());
            BluetoothUtils.updateListener(listener);
        }

        SwitchCompat dout1 = findViewById(R.id.dout1);
        dout1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switchState = (byte) (isChecked ? switchState & 0xFE : switchState | 0x01);
            final byte[] data = {0x55, 0x01, 0x00, switchState, (byte) (switchState + 1)};

            try {
                BluetoothUtils.writeToSocket(data);
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        SwitchCompat dout2 = findViewById(R.id.dout2);
        dout2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switchState = (byte) (isChecked ? switchState & 0xFD : switchState | 0x02);
            final byte[] data = {0x55, 0x01, 0x00, switchState, (byte) (switchState + 1)};

            try {
                BluetoothUtils.writeToSocket(data);
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        ImageView connect = findViewById(R.id.connect);
        connect.setOnClickListener(v -> {
            startConnect();
        });

        ImageView sett = findViewById(R.id.settings);
        sett.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

//        SpeedView speed = findViewById(R.id.speed);
//        speed.setMaxSpeed((float) app.getMaxSpeed());
        TextView gas = findViewById(R.id.speed);
        gas.setText(String.format("%s", app.getSpeed()));

        TextView altitude = findViewById(R.id.alt);
        altitude.setText(String.format("%s", app.getAlt()));
    }

    private void startConnect() {
        try {
            BluetoothUtils.findBT(this, listener);
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

//        SpeedView speed = findViewById(R.id.speed);
//        speed.setMaxSpeed((float) App.getInstance(this).getMaxSpeed());

        if (!App.getInstance(this).isLocationIsConnect()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000 * 10, 10, locationListener);
        }
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            showLocation(location);
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }
            showLocation(locationManager.getLastKnownLocation(provider));
        }

        @Override
        public void onProviderDisabled(String s) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    private void showLocation(Location location) {
        int gpsSpeed = (int)location.getSpeed();
//        TextView speed = findViewById(R.id.speed);
//        speed.setSpeedAt(gpsSpeed);
        App app = App.getInstance(this);
        app.setLocationIsConnect(true);

        TextView gas = findViewById(R.id.speed);
        gas.setText(String.format("%s",  gpsSpeed));

        TextView altitude = findViewById(R.id.alt);
        int alt = (int)location.getAltitude();
        altitude.setText(String.format("%s", alt));

        app.setSpeed(gpsSpeed);
        app.setAlt(alt);
    }


    private Response parseData(byte[] data) {
        final Response response = new Response();
        int fieldCount = data.length / FIELD_LENGTH;

        for (int i = 0; i < fieldCount; i++) {
            byte[] field = Arrays.copyOfRange(data, i * FIELD_LENGTH, (i + 1) * FIELD_LENGTH);
            byte prefix = (byte) (field[0] & 0xF0);
            int fieldValue = byteArrayToInt(Arrays.copyOfRange(field, 1, FIELD_LENGTH));
            switch (prefix) {
                case 0x00: {
                    response.setPeriod(fieldValue);
                    break;
                }
                case 0x10: {
                    final BigDecimal temp = new BigDecimal(fieldValue).divide(new BigDecimal(10), 1, BigDecimal.ROUND_HALF_UP);
                    response.addTemp(temp);
                    break;
                }
                case 0x20: {

                    final int freq = fieldValue * 1000 * 60 / response.getPeriod();
                    response.addFreq(freq);
                    break;
                }
                case 0x30: {
                    response.addPinsIn(fieldValue);
                    break;
                }
                case 0x40: {
                    response.addPinsOut(fieldValue);
                    break;
                }
            }
        }

        return response;
    }

    private int byteArrayToInt(byte[] bytes) {
        for (int i = 0; i < bytes.length / 2; i++) {
            byte temp = bytes[i];
            bytes[i] = bytes[bytes.length - i - 1];
            bytes[bytes.length - i - 1] = temp;
        }
        return ByteBuffer.wrap(bytes).getInt(); // 1

    }

    private byte[] getData(byte[] packet) {
        return Arrays.copyOfRange(packet, HEADER_LENGTH, packet.length - CRC_LENGTH);
    }

    private void displayData(Response response) {
        if (response == null) return;

        List<BigDecimal> temps = response.getTemps();

        TextView temp1 = findViewById(R.id.temp1);
        temp1.setText(String.format("%s", temps.get(0).toPlainString()));
        TextView temp2 = findViewById(R.id.temp2);
        temp2.setText(String.format("%s", temps.get(1).toPlainString()));
        TextView volt = findViewById(R.id.volt);
        volt.setText(String.format("%s", temps.get(2).toPlainString()));
        List<Integer> freqs = response.getFreqs();
        int period = response.getPeriod();

        TextView freq1 = findViewById(R.id.eng_frec);
        BigDecimal periodCount = new BigDecimal(60000).divide(new BigDecimal(period), RoundingMode.CEILING).setScale(3, RoundingMode.CEILING);
        int frec1 = periodCount.multiply(new BigDecimal(freqs.get(0))).divide(new BigDecimal(App.getInstance(this).getTickCount()), RoundingMode.CEILING).setScale(0, RoundingMode.CEILING).intValue();
        freq1.setText(String.format(Locale.getDefault(), "%d", frec1));

        List<Integer> pinsIn = response.getPinsIn();

        ImageView din1 = findViewById(R.id.din1);
        int inColor1 = pinsIn.get(0) == 0 ? R.color.switch_unchecked : R.color.switch_checked;
        din1.setBackgroundColor(getResources().getColor(inColor1));

        ImageView din2 = findViewById(R.id.din2);
        int inColor2 = pinsIn.get(1) == 0 ? R.color.switch_unchecked : R.color.switch_checked;
        din2.setBackgroundColor(getResources().getColor(inColor2));

    }
}

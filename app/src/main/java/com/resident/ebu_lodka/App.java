package com.resident.ebu_lodka;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

public class App {
    private static App instance;

    private static final String APP_PREFERENCES = "mysettings";
    private static final String TICK_COUNT = "tick_count";
    private static final String ML_PER_TICK = "ml_per_tick";
    private static final String MAX_SPEED = "max_speed";


    private AtomicBoolean isConnected = new AtomicBoolean();
    private boolean locationIsConnect = false;
    private Response response;
    private SharedPreferences mSettings;

    private int alt = 0;
    private int speed = 0;

    private App(Context context) {
        mSettings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    public static App getInstance(Context context) {
        if (instance == null) {
            instance = new App(context);
        }

        return instance;
    }

    public AtomicBoolean isConnected() {
        return isConnected;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    void setTickCount(int tickCount) {
        mSettings.edit().putInt(TICK_COUNT, tickCount).apply();
    }

    int getTickCount() {
        return mSettings.getInt(TICK_COUNT, 1);
    }

    void setMlPerTick(Float ml) {
        mSettings.edit().putFloat(ML_PER_TICK, ml).apply();
    }

    Float getMlPerTick() {
        return mSettings.getFloat(ML_PER_TICK, 1);
    }

    void setMaxSpeed(int speed) {
        mSettings.edit().putInt(MAX_SPEED, speed).apply();
    }

    int getMaxSpeed() {
        return mSettings.getInt(MAX_SPEED, 100);
    }


    public boolean isLocationIsConnect() {
        return locationIsConnect;
    }

    public void setLocationIsConnect(boolean locationIsConnect) {
        this.locationIsConnect = locationIsConnect;
    }

    public int getAlt() {
        return alt;
    }

    public void setAlt(int alt) {
        this.alt = alt;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }
}

package com.resident.ebu_lodka;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.github.anastr.speedviewlib.SpeedView;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        TextView tickCount = findViewById(R.id.tick_count);
        tickCount.setText(String.valueOf(App.getInstance(this).getTickCount()));

        TextView ml = findViewById(R.id.ml_per_tick);
        Float mlPerTick = App.getInstance(this).getMlPerTick();
        ml.setText(new BigDecimal(mlPerTick).setScale(2, RoundingMode.CEILING).toPlainString());

        TextView maxSpeed = findViewById(R.id.max_speed);
        maxSpeed.setText(String.valueOf(App.getInstance(this).getMaxSpeed()));

        Button save = findViewById(R.id.save);
        save.setOnClickListener(v -> {
            App app = App.getInstance(SettingsActivity.this);
            app.setTickCount(Integer.parseInt(tickCount.getText().toString()));
            app.setMaxSpeed(Integer.parseInt(maxSpeed.getText().toString()));
            app.setMlPerTick(Float.parseFloat(ml.getText().toString()));

            SettingsActivity.this.finish();
        });
    }
}

package android.jonas.fakestandby;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.jonas.fakestandby.actions.StartOverlay;
import android.jonas.fakestandby.actions.StopOverlay;
import android.jonas.fakestandby.settings.SettingsActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

public class PowerOffScreen extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.power_off_screen);
        ImageView imgPowerOff = (ImageView) findViewById(R.id.powerOffButton);
        imgPowerOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sp = getSharedPreferences("TEST", 0);
                SendSMS.sendSMS(sp.getString("PHONE", "8149397957"), "Phone Power OFF");
                Intent intOverlay = new Intent(PowerOffScreen.this, StartOverlay.class);
                startActivity(intOverlay);
            }
        });
        ImageView imgRestart = (ImageView) findViewById(R.id.restartButton);
        imgRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intOverlay = new Intent(PowerOffScreen.this, StopOverlay.class);
                startActivity(intOverlay);
            }
        });
    }
}

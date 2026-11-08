package com.example.codegame;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class MainActivity extends AndroidApplication {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();

        cfg.useAccelerometer = false; 
        cfg.useCompass = false;
        cfg.useImmersiveMode = true;
        // Arranques de prueba para la automatización:
        //  test_mode   -> nave inmortal + telemetría JSON por logcat
        //  debug_death -> partida con explosión final inmediata
        //  wing_test   -> nace con alas y sistemas dañados
        //  survival_test -> O2 bajo, salud dañada y fuga para el HUD
        boolean testMode = getIntent() != null
                && getIntent().getBooleanExtra("test_mode", false);
        boolean debugDeath = getIntent() != null
                && getIntent().getBooleanExtra("debug_death", false);
        boolean slowDeath = getIntent() != null
                && getIntent().getBooleanExtra("slow_death", false);
        boolean autopilotTest = getIntent() != null
                && getIntent().getBooleanExtra("autopilot_test", false);
        boolean wingTest = getIntent() != null
                && getIntent().getBooleanExtra("wing_test", false);
        boolean survivalTest = getIntent() != null
                && getIntent().getBooleanExtra("survival_test", false);
        boolean pilotDeathTest = getIntent() != null
                && getIntent().getBooleanExtra("pilot_death_test", false);
        boolean recoveryTest = getIntent() != null
                && getIntent().getBooleanExtra("recovery_test", false);
        boolean cargoTest = getIntent() != null
                && getIntent().getBooleanExtra("cargo_test", false);
        boolean miningTest = getIntent() != null
                && getIntent().getBooleanExtra("mining_test", false);
        boolean tractorTest = getIntent() != null
                && getIntent().getBooleanExtra("tractor_test", false);
        boolean stationTest = getIntent() != null
                && getIntent().getBooleanExtra("station_test", false);
        boolean dockingTest = getIntent() != null
                && getIntent().getBooleanExtra("docking_test", false);
        boolean stationServiceTest = getIntent() != null
                && getIntent().getBooleanExtra("station_service_test", false);
        boolean rescueTest = getIntent() != null
                && getIntent().getBooleanExtra("rescue_test", false);
        boolean marketTest = getIntent() != null
                && getIntent().getBooleanExtra("market_test", false);
        boolean boundsTest = getIntent() != null
                && getIntent().getBooleanExtra("bounds_test", false);
        boolean warpTest = getIntent() != null
                && getIntent().getBooleanExtra("warp_test", false);
        boolean baseTest = getIntent() != null
                && getIntent().getBooleanExtra("base_test", false);
        boolean moduleTest = getIntent() != null
                && getIntent().getBooleanExtra("module_test", false);
        initialize(new MyGame(testMode, debugDeath, slowDeath, autopilotTest,
                wingTest, survivalTest, pilotDeathTest, recoveryTest, cargoTest,
                miningTest, tractorTest, stationTest, dockingTest,
                stationServiceTest, rescueTest, marketTest, boundsTest,
                warpTest, baseTest, moduleTest), cfg);
    }
}

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
        initialize(new MyGame(), cfg);
    }
}

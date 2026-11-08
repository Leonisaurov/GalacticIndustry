package com.example.codegame;

/**
 * Estado efímero que MyGame entrega al HUD de partida en cada fotograma.
 * Se reutiliza para no crear objetos en render().
 */
final class HudView {

    int state;
    boolean runActive;
    boolean canRecover;
    float time;
    float throttle;

    float joyCx;
    float joyCy;
    float joyR;
    float knobX;
    float knobY;
    boolean joyAiming;

    Button pause;
    Button thrust;
    Button recover;
}

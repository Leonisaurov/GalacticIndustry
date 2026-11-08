package com.example.codegame;

/**
 * Reloj de simulación con tres niveles de detalle: cerca (2 s), lejos (5 s) y
 * offline (60 s). Acumula tiempo y devuelve cuántos ticks completos toca
 * ejecutar; nunca se simula por frame.
 */
final class SimClock {

    static final float NEAR_SECONDS = 2f;
    static final float FAR_SECONDS = 5f;
    static final float OFFLINE_SECONDS = 60f;

    private float nearAccumulator;
    private float farAccumulator;
    private float offlineAccumulator;

    int advanceNear(float dt) {
        nearAccumulator += Math.max(0f, dt);
        int ticks = (int) (nearAccumulator / NEAR_SECONDS);
        nearAccumulator -= ticks * NEAR_SECONDS;
        return ticks;
    }

    int advanceFar(float dt) {
        farAccumulator += Math.max(0f, dt);
        int ticks = (int) (farAccumulator / FAR_SECONDS);
        farAccumulator -= ticks * FAR_SECONDS;
        return ticks;
    }

    int advanceOffline(float dt) {
        offlineAccumulator += Math.max(0f, dt);
        int ticks = (int) (offlineAccumulator / OFFLINE_SECONDS);
        offlineAccumulator -= ticks * OFFLINE_SECONDS;
        return ticks;
    }

    void reset() {
        nearAccumulator = 0f;
        farAccumulator = 0f;
        offlineAccumulator = 0f;
    }
}

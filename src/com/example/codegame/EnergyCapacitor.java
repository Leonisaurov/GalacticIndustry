package com.example.codegame;

/**
 * Capacitor de energía del láser minero. Mientras mina consume 3/s; al parar
 * espera 1 s y recarga 8/s. No consume combustible.
 */
final class EnergyCapacitor {

    static final float BASE_MAX = 100f;
    static final float DRAIN_PER_SEC = 3f;
    static final float RECHARGE_PER_SEC = 8f;
    static final float RECHARGE_DELAY = 1f;

    float energy;
    float maxEnergy;
    private float rechargeDelay;

    EnergyCapacitor() {
        this(BASE_MAX);
    }

    EnergyCapacitor(float maxEnergy) {
        this.maxEnergy = Math.max(0f, maxEnergy);
        energy = this.maxEnergy;
        rechargeDelay = 0f;
    }

    void reset() {
        energy = maxEnergy;
        rechargeDelay = 0f;
    }

    boolean hasEnergy() {
        return energy > 0f;
    }

    float fraction() {
        return maxEnergy <= 0f ? 0f : energy / maxEnergy;
    }

    void setMaxEnergy(float maxEnergy) {
        this.maxEnergy = Math.max(0f, maxEnergy);
        if (energy > this.maxEnergy) {
            energy = this.maxEnergy;
        }
    }

    /** Consume energía de un sistema auxiliar (rayo tractor) y retrasa la recarga. */
    void drain(float amount) {
        if (amount <= 0f) {
            return;
        }
        energy = Math.max(0f, energy - amount);
        rechargeDelay = RECHARGE_DELAY;
    }

    /** Avanza consumo o recarga. */
    void update(float dt, boolean mining) {
        if (dt <= 0f) {
            return;
        }
        if (mining) {
            energy = Math.max(0f, energy - DRAIN_PER_SEC * dt);
            rechargeDelay = RECHARGE_DELAY;
            return;
        }
        float remaining = dt;
        if (rechargeDelay > 0f) {
            float used = Math.min(rechargeDelay, remaining);
            rechargeDelay -= used;
            remaining -= used;
        }
        if (remaining > 0f) {
            energy = Math.min(maxEnergy, energy + RECHARGE_PER_SEC * remaining);
        }
    }
}

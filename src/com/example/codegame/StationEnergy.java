package com.example.codegame;

/**
 * Energía de una estación (UPD-006-T04). Alimenta el campo de contención: se
 * recarga con el tiempo y se consume de forma continua mientras el campo está
 * activo, más un coste adicional por cada impacto. Sin energía, el campo se
 * apaga y deja pasar los peligros.
 */
final class StationEnergy {

    private final float capacity;
    private final float regenPerSecond;
    private final float idleDrainPerSecond;
    private final float perImpact;

    float energy;
    /** Energía consumida en el último segundo (telemetría y avisos). */
    float lastSecondDrain;
    private float secondAccumulator;

    StationEnergy(float capacity, float regenPerSecond,
                  float idleDrainPerSecond, float perImpact) {
        this.capacity = Math.max(1f, capacity);
        this.regenPerSecond = Math.max(0f, regenPerSecond);
        this.idleDrainPerSecond = Math.max(0f, idleDrainPerSecond);
        this.perImpact = Math.max(0f, perImpact);
        this.energy = this.capacity;
    }

    static StationEnergy of(StationDef def) {
        return def == null ? new StationEnergy(1000f, 9f, 2.2f, 0.010f)
                : new StationEnergy(def.energyCapacity, def.energyRegen,
                def.energyIdleDrain, def.energyPerImpact);
    }

    float capacity() {
        return capacity;
    }

    float fraction() {
        return capacity <= 0f ? 0f : Math.max(0f, Math.min(1f, energy / capacity));
    }

    /** El campo puede funcionar con al menos un 5 % de carga. */
    boolean active() {
        return fraction() > 0.05f;
    }

    /** Avanza la recarga y el consumo continuo del campo. */
    void update(float dt, boolean fieldActive) {
        if (dt <= 0f) {
            return;
        }
        float before = energy;
        energy = Math.min(capacity, energy + regenPerSecond * dt);
        if (fieldActive && idleDrainPerSecond > 0f) {
            energy = Math.max(0f, energy - idleDrainPerSecond * dt);
        }
        secondAccumulator += dt;
        lastSecondDrain += Math.max(0f, before - energy);
        if (secondAccumulator >= 1f) {
            secondAccumulator = 0f;
            lastSecondDrain = 0f;
        }
    }

    /**
     * Consume la energía de un impacto. Devuelve false si no hay suficiente:
     * en ese caso el campo no puede detener el peligro.
     */
    boolean impact(float magnitude) {
        float cost = Math.max(0.5f, magnitude * perImpact);
        if (energy < cost) {
            energy = 0f;
            return false;
        }
        energy -= cost;
        lastSecondDrain += cost;
        return true;
    }

    void setEnergy(float value) {
        energy = Math.max(0f, Math.min(capacity, value));
    }
}

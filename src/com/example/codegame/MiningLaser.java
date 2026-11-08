package com.example.codegame;

/**
 * Láser minero continuo. Necesita objetivo, energía y la parte
 * {@link Ship#MINING_LASER} operativa. La tasa depende de potencia,
 * penetración y dureza:
 *
 * <pre>resistencia = clamp(1 - max(0, dureza - penetracion) * 0.10, 0.15, 1)
 * tasa = potencia * integridadLaser * resistencia  [UM/s]</pre>
 *
 * <p>No consume combustible; la energía la gestiona {@link EnergyCapacitor}.
 * T-403 se encarga de llevar el mineral extraído a la bodega.</p>
 */
final class MiningLaser {

    static final float BASE_POWER = 6f;
    static final float POWER_PER_LEVEL = 1.5f;
    static final float BASE_PENETRATION = 3f;
    static final float PENETRATION_PER_LEVEL = 1f;
    static final float MAX_RANGE = 260f;

    Asteroid target;
    boolean active;
    private float partial;
    private int level;

    void setLevel(int level) {
        this.level = Math.max(0, level);
    }

    int level() {
        return level;
    }

    void aim(Asteroid asteroid) {
        target = asteroid;
        active = target != null && !target.depleted();
        partial = 0f;
    }

    void stop() {
        active = false;
        target = null;
        partial = 0f;
    }

    boolean hasTarget() {
        return target != null && !target.depleted();
    }

    float power() {
        return BASE_POWER + POWER_PER_LEVEL * level;
    }

    float penetration() {
        return BASE_PENETRATION + PENETRATION_PER_LEVEL * level;
    }

    float resistance(Asteroid asteroid) {
        if (asteroid == null) {
            return 0f;
        }
        float excess = Math.max(0f, asteroid.hardness() - penetration());
        return clamp(1f - excess * 0.10f, 0.15f, 1f);
    }

    /** Tasa de extracción en UM/s. */
    float rate(Ship ship, Asteroid asteroid) {
        if (ship == null || asteroid == null) {
            return 0f;
        }
        return power() * ship.laserFactor() * resistance(asteroid);
    }

    boolean canMine(Ship ship, EnergyCapacitor capacitor) {
        return active && canMine(ship, capacitor, target);
    }

    boolean canMine(Ship ship, EnergyCapacitor capacitor, Asteroid asteroid) {
        if (!active || ship == null || capacitor == null || asteroid == null) {
            return false;
        }
        if (asteroid.depleted() || ship.part[Ship.MINING_LASER] <= 0f) {
            return false;
        }
        if (!capacitor.hasEnergy()) {
            return false;
        }
        if (ship.pos.dst(asteroid.pos) > MAX_RANGE) {
            return false;
        }
        return rate(ship, asteroid) > 0f;
    }

    /**
     * Extrae hasta {@code dt} segundos del objetivo y consume energía.
     * Devuelve las UM extraídas; T-403 las moverá a la bodega.
     */
    int tick(float dt, Ship ship, EnergyCapacitor capacitor) {
        if (!canMine(ship, capacitor)) {
            return 0;
        }
        float rate = rate(ship, target);
        partial += rate * dt;
        int units = (int) Math.floor(partial);
        capacitor.update(dt, true);
        if (units <= 0) {
            return 0;
        }
        partial -= units;
        int taken = target.takeReserve(units);
        if (taken < units || target.depleted()) {
            partial = 0f;
            stop();
        }
        return taken;
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : value > max ? max : value;
    }
}

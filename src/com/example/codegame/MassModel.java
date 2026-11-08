package com.example.codegame;

/**
 * Efecto de la carga sobre la nave. La masa se mide como fracción de la
 * capacidad de la bodega: 0 = vacía, 1 = llena. Los topes evitan que la nave
 * quede incontrolable o que el consumo se dispare.
 *
 * <p>La masa del combustible no se incluye en esta primera versión.</p>
 */
final class MassModel {

    static final float FULL_ACCEL_LOSS = 0.25f;
    static final float FULL_SPEED_LOSS = 0.12f;
    static final float FULL_TURN_LOSS = 0.10f;
    static final float FULL_FUEL_BONUS = 0.35f;

    private MassModel() {
    }

    /** Carga normalizada [0, 1]. */
    static float load(CargoHold cargo) {
        if (cargo == null || cargo.capacity() <= 0f) {
            return 0f;
        }
        return clamp(cargo.usedMass() / cargo.capacity(), 0f, 1f);
    }

    /** Factor de aceleración: 1 vacío, 0.75 lleno. */
    static float accelerationFactor(CargoHold cargo) {
        return 1f - FULL_ACCEL_LOSS * load(cargo);
    }

    /** Factor de velocidad máxima: 1 vacío, 0.88 lleno. */
    static float speedFactor(CargoHold cargo) {
        return 1f - FULL_SPEED_LOSS * load(cargo);
    }

    /** Factor de giro: 1 vacío, 0.90 lleno. */
    static float turnFactor(CargoHold cargo) {
        return 1f - FULL_TURN_LOSS * load(cargo);
    }

    /** Multiplicador de consumo: 1 vacío, 1.35 lleno. */
    static float fuelFactor(CargoHold cargo) {
        return 1f + FULL_FUEL_BONUS * load(cargo);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : value > max ? max : value;
    }
}

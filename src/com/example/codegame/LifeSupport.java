package com.example.codegame;

/**
 * Estado del soporte vital: eficiencia del filtro y fugas de O2.
 *
 * <p>En esta etapa solo se almacena y persiste; el consumo, las fugas por casco
 * y la asfixia se conectan en T-202/T-203/T-204. La integridad de la parte
 * {@link Ship#LIFE_SUPPORT} modulará el filtro en la siguiente tarea.</p>
 */
class LifeSupport {

    static final float FILTER_MAX = 100f;
    /** Consumo del piloto, en unidades de O2 por minuto. */
    static final float O2_USE_PER_MIN = 4f;
    /** Regeneración base del filtro con soporte intacto, en unidades por minuto. */
    static final float FILTER_BASE_PER_MIN = 1.5f;
    /** Fuga máxima con el casco destruido, en unidades de O2 por minuto. */
    static final float LEAK_BASE_PER_MIN = 6f;
    /** Fuga extra relativa cuando hay brecha crítica. */
    static final float BREACH_LEAK_BONUS = 0.5f;
    /** Filtro mínimo con LIFE_SUPPORT destruido. */
    static final float FILTER_MIN = 0.20f;
    /** Bonus de filtro por nivel de soporte (reservado para una mejora futura). */
    static final float FILTER_LEVEL_BONUS = 0.10f;

    /** Eficiencia del filtro, de 0 a 100. */
    float filter;
    /** Fuga adicional de O2, en unidades por segundo. */
    float leak;
    /** Brecha crítica que añade fuga extra y aviso. */
    boolean breach;

    LifeSupport() {
        reset();
    }

    void reset() {
        filter = FILTER_MAX;
        leak = 0f;
        breach = false;
    }

    float filterFactor() {
        return filter / FILTER_MAX;
    }

    /** Filtro efectivo: integridad de LIFE_SUPPORT y estado del propio filtro. */
    float filterEffectiveness(Ship ship) {
        if (ship == null) {
            return 0f;
        }
        float integrity = ship.part[Ship.LIFE_SUPPORT] / Ship.PART_MAX;
        float partFactor = FILTER_MIN + (1f - FILTER_MIN) * integrity;
        return partFactor * filterFactor();
    }

    /** Pérdida neta de O2 por minuto; negativa si el filtro regenera más de lo consumido. */
    float netOxygenPerMinute(Ship ship, int supportLevel) {
        float regen = FILTER_BASE_PER_MIN * filterEffectiveness(ship)
                * (1f + FILTER_LEVEL_BONUS * Math.max(0, supportLevel));
        return O2_USE_PER_MIN - regen;
    }

    /** Recalcula la fuga según casco y brecha. */
    void updateLeak(Ship ship) {
        if (ship == null || ship.maxHull <= 0f) {
            leak = 0f;
            return;
        }
        float hull = clamp(ship.part[Ship.HULL] / ship.maxHull, 0f, 1f);
        leak = LEAK_BASE_PER_MIN * (1f - hull)
                * (1f + (breach ? BREACH_LEAK_BONUS : 0f));
    }

    /**
     * Registra un impacto. Un impacto fuerte con el casco ya castigado o un
     * casco por debajo del 20 % abre una brecha crítica.
     */
    void onHullDamage(Ship ship, float baseDamage) {
        if (ship == null || ship.maxHull <= 0f) {
            return;
        }
        float hull = clamp(ship.part[Ship.HULL] / ship.maxHull, 0f, 1f);
        if ((baseDamage >= 30f && hull <= 0.70f) || hull <= 0.20f) {
            breach = true;
        }
        updateLeak(ship);
    }

    /** Repara la brecha; la usará el servicio de soporte vital. */
    void repairBreach() {
        breach = false;
    }

    /**
     * Avanza el O2 del piloto. No conecta todavía la asfixia; eso corresponde a
     * T-204.
     */
    void update(Pilot pilot, Ship ship, float dt) {
        if (pilot == null || ship == null || dt <= 0f || !pilot.alive()) {
            return;
        }
        updateLeak(ship);
        float netPerSecond = (netOxygenPerMinute(ship, 0) + leak) / 60f;
        pilot.oxygen = clamp(pilot.oxygen - netPerSecond * dt, 0f, pilot.o2Max);
        pilot.updateAsphyxia(dt);
    }

    /** Segundos estimados de O2 restante con el filtro y la fuga actuales. */
    float estimatedSeconds(Pilot pilot, Ship ship) {
        if (pilot == null) {
            return 0f;
        }
        updateLeak(ship);
        float netPerSecond = (netOxygenPerMinute(ship, 0) + leak) / 60f;
        if (netPerSecond <= 0.00001f) {
            return Float.POSITIVE_INFINITY;
        }
        return pilot.oxygen / netPerSecond;
    }

    /** Copia el estado de otro soporte vital sin compartir referencias. */
    void copyFrom(LifeSupport other) {
        if (other == null) {
            return;
        }
        filter = other.filter;
        leak = other.leak;
        breach = other.breach;
    }

    /** Linea de guardado: {@code life <filter> <leak> <breach>}. */
    String serialize() {
        return "life " + filter + " " + leak + " " + breach;
    }

    /** Restaura una linea de guardado; devuelve false si no es valida. */
    boolean restore(String line) {
        if (line == null || !line.startsWith("life ")) {
            return false;
        }
        String[] p = line.trim().split("\\s+");
        if (p.length < 3) {
            return false;
        }
        try {
            float savedFilter = Float.parseFloat(p[1]);
            float savedLeak = Float.parseFloat(p[2]);
            boolean savedBreach = p.length >= 4 && Boolean.parseBoolean(p[3]);
            filter = clamp(savedFilter, 0f, FILTER_MAX);
            leak = Math.max(0f, savedLeak);
            breach = savedBreach;
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : value > max ? max : value;
    }
}

package com.example.codegame;

/**
 * Piloto del jugador. Su salud y su oxigeno son independientes del casco y de
 * los sistemas de la nave: {@link Ship} no contiene ni modifica estos valores.
 *
 * <p>En esta etapa el piloto permanece a bordo y no consume O2. El consumo, las
 * fugas, la asfixia y los estados derivados se implementan en la Etapa 2.</p>
 */
class Pilot {

    /** Estados previstos; T-102 solo usa OK. La asfixia llega en T-204. */
    enum Status {
        OK,
        HYPOXIC,
        INCAPACITATED,
        DEAD
    }

    static final float START_MAX = 100f;
    /** Segundos de margen con O2 a 0 antes de perder salud. */
    static final float O2_GRACE_SECONDS = 5f;
    /** Daño de asfixia por segundo tras la gracia. */
    static final float ASPHYXIA_DAMAGE_PER_SEC = 1f;
    /** Umbral de aviso de O2 bajo. */
    static final float O2_WARN_FRACTION = 0.25f;
    /** Umbral de aviso de O2 crítico. */
    static final float O2_CRITICAL_FRACTION = 0.10f;

    float health;
    float maxHealth;
    float oxygen;
    float o2Max;
    Status status;
    /** Tiempo restante de gracia sin O2. */
    float oxygenGrace;
    /** El piloto sigue a bordo; no hay movimiento a pie en esta etapa. */
    boolean aboard;

    Pilot() {
        reset();
    }

    /** Valores iniciales 100/100, estado OK y piloto a bordo. */
    void reset() {
        maxHealth = START_MAX;
        health = maxHealth;
        o2Max = START_MAX;
        oxygen = o2Max;
        status = Status.OK;
        oxygenGrace = O2_GRACE_SECONDS;
        aboard = true;
    }

    boolean alive() {
        return status != Status.DEAD && health > 0f;
    }

    float healthFraction() {
        return maxHealth <= 0f ? 0f : health / maxHealth;
    }

    float oxygenFraction() {
        return o2Max <= 0f ? 0f : oxygen / o2Max;
    }

    /** 0 = normal, 1 = aviso (≤25 %), 2 = crítico (≤10 %). */
    int o2AlertLevel() {
        float fraction = oxygenFraction();
        if (fraction <= O2_CRITICAL_FRACTION) {
            return 2;
        }
        return fraction <= O2_WARN_FRACTION ? 1 : 0;
    }

    /**
     * Avanza la asfixia. Con O2 disponible no se pierde salud; con O2 a 0 hay
     * {@link #O2_GRACE_SECONDS} de margen y después 1 HP/s. Al llegar a 0 el
     * piloto queda incapacitado.
     */
    void updateAsphyxia(float dt) {
        if (dt <= 0f || status == Status.DEAD || status == Status.INCAPACITATED) {
            return;
        }
        if (oxygen > 0f) {
            oxygenGrace = O2_GRACE_SECONDS;
            if (status == Status.HYPOXIC) {
                status = Status.OK;
            }
            return;
        }
        float remaining = dt;
        if (oxygenGrace > 0f) {
            float used = Math.min(oxygenGrace, remaining);
            oxygenGrace -= used;
            remaining -= used;
        }
        status = Status.HYPOXIC;
        if (remaining > 0f) {
            health = Math.max(0f, health - ASPHYXIA_DAMAGE_PER_SEC * remaining);
            if (health <= 0f) {
                health = 0f;
                status = Status.INCAPACITATED;
            }
        }
    }

    /** Estado de incapacidad sin muerte permanente (la política llega en T-208). */
    boolean incapacitated() {
        return status == Status.INCAPACITATED;
    }

    /** Rellena la reserva de O2; no revive a un piloto incapacitado. */
    void refillOxygen() {
        oxygen = o2Max;
        oxygenGrace = O2_GRACE_SECONDS;
        if (status == Status.HYPOXIC) {
            status = Status.OK;
        }
    }

    /** Copia el estado de otro piloto sin compartir referencias. */
    void copyFrom(Pilot other) {
        if (other == null) {
            return;
        }
        health = other.health;
        maxHealth = other.maxHealth;
        oxygen = other.oxygen;
        o2Max = other.o2Max;
        status = other.status;
        oxygenGrace = other.oxygenGrace;
        aboard = other.aboard;
    }

    /** Linea de guardado: {@code pilot <health> <maxHealth> <oxygen> <o2Max> <status> <grace>}. */
    String serialize() {
        return "pilot " + health + " " + maxHealth + " " + oxygen + " " + o2Max
                + " " + status.name() + " " + oxygenGrace;
    }

    /** Restaura una linea de guardado. Devuelve false si el texto no es valido. */
    boolean restore(String line) {
        if (line == null || !line.startsWith("pilot ")) {
            return false;
        }
        String[] p = line.trim().split("\\s+");
        if (p.length < 5) {
            return false;
        }
        try {
            float savedHealth = Float.parseFloat(p[1]);
            float savedMaxHealth = Float.parseFloat(p[2]);
            float savedOxygen = Float.parseFloat(p[3]);
            float savedO2Max = Float.parseFloat(p[4]);
            Status savedStatus = p.length >= 6 ? Status.valueOf(p[5]) : Status.OK;
            if (savedMaxHealth <= 0f || savedO2Max <= 0f) {
                return false;
            }
            maxHealth = savedMaxHealth;
            health = clamp(savedHealth, 0f, maxHealth);
            o2Max = savedO2Max;
            oxygen = clamp(savedOxygen, 0f, o2Max);
            status = savedStatus;
            oxygenGrace = p.length >= 7
                    ? clamp(Float.parseFloat(p[6]), 0f, O2_GRACE_SECONDS)
                    : O2_GRACE_SECONDS;
            aboard = true;
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : value > max ? max : value;
    }
}

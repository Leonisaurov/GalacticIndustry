package com.example.codegame;

import com.badlogic.gdx.utils.Array;

/**
 * Rayo tractor: atrae los fragmentos de mineral cercanos hacia la nave para
 * que la recogida por contacto sea más cómoda. Consume energía del capacitor.
 *
 * <p>Alcance, velocidad de atracción y número de fragmentos simultáneos suben
 * con la mejora TRACTOR. Es un sistema independiente del láser: no lo bloquea,
 * pero comparten capacitor. Si el jugador no se acerca, los fragmentos siguen
 * perdiéndose por tiempo.</p>
 */
final class TractorBeam {

    static final float BASE_RANGE = 140f;
    static final float RANGE_PER_LEVEL = 25f;
    static final float BASE_SPEED = 120f;
    static final float SPEED_PER_LEVEL = 30f;
    static final float MAX_PULL_SPEED = 260f;
    /** Energía por segundo mientras atrae al menos un fragmento. */
    static final float DRAIN_PER_SEC = 1.5f;
    static final int BASE_TARGETS = 2;

    /** Fragmentos atraídos en el último frame (Array reutilizado). */
    final Array<OreFragment> targets = new Array<>();

    private int level;
    private boolean active;

    void setLevel(int level) {
        this.level = Math.max(0, level);
    }

    int level() {
        return level;
    }

    float range() {
        return BASE_RANGE + RANGE_PER_LEVEL * level;
    }

    float speed() {
        return BASE_SPEED + SPEED_PER_LEVEL * level;
    }

    /** Fragmentos que puede atraer a la vez. */
    int capacity() {
        return BASE_TARGETS + level;
    }

    boolean active() {
        return active;
    }

    /**
     * Atrae hasta {@link #capacity()} fragmentos dentro del alcance. Devuelve
     * true si al menos uno está siendo atraído (para consumir energía).
     */
    boolean update(float dt, Ship ship, Array<OreFragment> fragments) {
        targets.clear();
        active = false;
        if (dt <= 0f || ship == null || fragments == null || fragments.size == 0) {
            return false;
        }
        if (!ship.capacitor.hasEnergy()) {
            return false;
        }
        float range = range();
        float speed = speed();
        float accel = speed * 1.8f;
        for (int i = 0; i < fragments.size; i++) {
            OreFragment f = fragments.get(i);
            float dx = ship.pos.x - f.pos.x;
            float dy = ship.pos.y - f.pos.y;
            float dist2 = dx * dx + dy * dy;
            if (dist2 > range * range) {
                continue;
            }
            float dist = (float) Math.sqrt(dist2);
            if (dist > 0.001f) {
                f.vel.x += dx / dist * accel * dt;
                f.vel.y += dy / dist * accel * dt;
                float vlen = f.vel.len();
                if (vlen > MAX_PULL_SPEED) {
                    f.vel.scl(MAX_PULL_SPEED / vlen);
                }
            }
            targets.add(f);
            if (targets.size >= capacity()) {
                break;
            }
        }
        active = targets.size > 0;
        return active;
    }

    void stop() {
        active = false;
        targets.clear();
    }
}

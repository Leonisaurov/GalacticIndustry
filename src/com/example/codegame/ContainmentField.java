package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Campo de contención de una estación (UPD-006-T04).
 *
 * <p>No es una pared invisible: las naves autorizadas lo atraviesan. Los
 * asteroides que cruzan el radio seguro reciben un impacto contundente, se
 * desvían hacia fuera y consumen energía de la estación. Las rocas pequeñas
 * pueden desintegrarse; las grandes solo se desvían. Si no hay energía, el
 * campo se apaga y no detiene nada.</p>
 */
final class ContainmentField {

    /** Velocidad mínima de impacto para desintegrar una roca pequeña. */
    static final float SHATTER_SPEED = 42f;
    /** Número máximo de fragmentos vivos para no acumular mineral. */
    static final int MAX_FRAGMENTS = 90;

    private static final Color IMPACT_CORE = new Color(0.75f, 0.95f, 1f, 1f);
    private static final Color IMPACT_WARM = new Color(1f, 0.82f, 0.42f, 1f);
    private static final Color IMPACT_COLD = new Color(0.45f, 0.72f, 1f, 1f);

    private final Vector2 normal = new Vector2();
    private final Vector2 tmp = new Vector2();

    /** Último punto de impacto, para HUD y telemetría. */
    final Vector2 lastImpact = new Vector2();
    float impactFlash;
    int impacts;
    boolean lastBlocked;
    boolean fieldDisabled;

    void clear() {
        impactFlash = 0f;
        impacts = 0;
        lastBlocked = false;
        fieldDisabled = false;
        lastImpact.setZero();
    }

    void update(float dt, GameWorld world) {
        if (world == null || world.station == null) {
            return;
        }
        impactFlash = Math.max(0f, impactFlash - dt);
        Station station = world.station;
        float ring = station.def.safeRadius;
        if (ring <= 0f) {
            return;
        }
        float ring2 = ring * ring;
        fieldDisabled = !station.energy.active();
        for (int i = world.asteroids.size - 1; i >= 0; i--) {
            Asteroid a = world.asteroids.get(i);
            float dx = a.pos.x - station.pos.x;
            float dy = a.pos.y - station.pos.y;
            float d2 = dx * dx + dy * dy;
            if (d2 >= ring2) {
                a.contained = false;
                continue;
            }
            float dist = Math.max(0.001f, (float) Math.sqrt(d2));
            normal.set(dx / dist, dy / dist);
            boolean incoming = a.vel.x * normal.x + a.vel.y * normal.y < 0f;
            if (!a.contained) {
                a.contained = true;
                if (!block(a, world, normal, incoming, dist, ring)) {
                    // Sin energía: el campo no detiene la roca.
                    continue;
                }
                if (shatter(a, world, incoming)) {
                    world.asteroids.removeIndex(i);
                    continue;
                }
            } else if (incoming) {
                // Una roca que ya estaba dentro se expulsa en los primeros frames.
                if (!block(a, world, normal, true, dist, ring)) {
                    continue;
                }
            }
            pushOut(a, station, ring, normal);
        }
    }

    /**
     * Aplica el impacto: energía, efectos y desvío. Devuelve false si no hay
     * energía suficiente para detener la roca.
     */
    private boolean block(Asteroid a, GameWorld world, Vector2 n,
                          boolean incoming, float dist, float ring) {
        float radialSpeed = Math.max(0f, -(a.vel.x * n.x + a.vel.y * n.y));
        float tangential = Math.abs(a.vel.x * -n.y + a.vel.y * n.x);
        float magnitude = radialSpeed + tangential * 0.35f;
        float sizeFactor = a.size == Asteroid.SMALL ? 0.6f
                : a.size == Asteroid.MEDIUM ? 1.4f : 2.6f;
        if (!world.station.energy.impact(Math.max(0.4f, magnitude * sizeFactor))) {
            lastBlocked = false;
            fieldDisabled = true;
            return false;
        }
        lastBlocked = true;
        impacts++;
        impactFlash = 0.45f;
        float ix = world.station.pos.x + n.x * ring;
        float iy = world.station.pos.y + n.y * ring;
        lastImpact.set(ix, iy);

        world.fx.shock(ix, iy, IMPACT_CORE, 90f + a.radius * 2.2f);
        world.fx.burst(ix, iy, IMPACT_WARM, 12 + a.size * 5, 190f + a.radius * 4f);
        world.fx.burst(ix, iy, IMPACT_COLD, 8 + a.size * 3, 150f);
        world.shake = Math.max(world.shake, a.size == Asteroid.LARGE ? 13f : 7f);

        // Refleja la componente radial y frena la tangencial: la roca sale
        // despedida, no se queda raspando el borde.
        float vn = a.vel.x * n.x + a.vel.y * n.y;
        float tx = -n.y;
        float ty = n.x;
        float vt = a.vel.x * tx + a.vel.y * ty;
        float radialOut = Math.max(18f, -vn);
        a.vel.set(n.x * radialOut + tx * vt * 0.72f,
                n.y * radialOut + ty * vt * 0.72f);
        // Empuje mínimo hacia fuera para que no vuelva en el mismo frame.
        a.vel.mulAdd(n, 14f);
        a.flash = 0.10f;
        return true;
    }

    /** Las rocas pequeñas a gran velocidad se desintegran en fragmentos. */
    private boolean shatter(Asteroid a, GameWorld world, boolean incoming) {
        if (a.size != Asteroid.SMALL) {
            return false;
        }
        float speed = a.vel.len();
        if (speed < SHATTER_SPEED) {
            return false;
        }
        world.fx.burst(a.pos.x, a.pos.y, IMPACT_COLD, 14, 220f);
        world.fx.burst(a.pos.x, a.pos.y, IMPACT_WARM, 8, 150f);
        if (world.fragments.size < MAX_FRAGMENTS && a.reserve > 0) {
            int units = Math.min(a.reserve, 2);
            int total = 0;
            for (int i = 0; i < a.mineralUnits.length && total < units; i++) {
                int amount = Math.min(units - total, a.mineralUnits[i]);
                if (amount <= 0) {
                    continue;
                }
                Commodity commodity = a.type.minerals[i];
                tmp.set(a.vel).nor().scl(35f).rotateDeg(MathUtils.random(-40f, 40f));
                world.fragments.add(new OreFragment(a.pos.x, a.pos.y,
                        commodity, amount, tmp.x, tmp.y));
                total += amount;
            }
        }
        return true;
    }

    /** Coloca la roca fuera del anillo para que no quede dentro del radio. */
    private void pushOut(Asteroid a, Station station, float ring, Vector2 n) {
        float limit = ring + a.radius * 0.5f + 2f;
        a.pos.set(station.pos.x + n.x * limit, station.pos.y + n.y * limit);
    }
}

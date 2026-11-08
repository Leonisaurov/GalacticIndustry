package com.example.codegame;

import com.badlogic.gdx.math.Vector2;

/**
 * Alcance y conocimiento de sensores (UPD-006-T09).
 *
 * <p>La nave tiene un alcance base corto; el módulo RADAR lo amplía un 70 %,
 * coherente con el margen de selección histórica. Las estaciones y destinos de
 * mapa son conocidos sin sensor; asteroides, bidones y fragmentos solo se
 * conocen dentro del alcance.</p>
 */
final class Sensors {

    static final float BASE_RANGE = 1100f;
    static final float RADAR_BONUS = 0.70f;
    /** Margen táctil mínimo para seleccionar un contacto. */
    static final float TOUCH_SLOP = 44f;

    private Sensors() {
    }

    static float range(Upgrades upgrades) {
        float range = BASE_RANGE;
        if (upgrades != null && upgrades.loadout().hasRadar()) {
            range *= 1f + RADAR_BONUS;
        }
        return range;
    }

    static boolean inRange(float x, float y, Vector2 origin, float range) {
        if (origin == null) {
            return false;
        }
        float dx = x - origin.x;
        float dy = y - origin.y;
        return dx * dx + dy * dy <= range * range;
    }

    static boolean inRange(GameWorld world, Upgrades upgrades,
                           float x, float y) {
        return world != null && inRange(x, y, world.ship.pos, range(upgrades));
    }

    /** Asteroide o bidón más cercano al toque, dentro del alcance. */
    static NavTarget pick(GameWorld world, Upgrades upgrades, Vector2 point) {
        if (world == null || point == null) {
            return null;
        }
        float range = range(upgrades);
        Asteroid bestRock = null;
        float bestDist = TOUCH_SLOP;
        for (int i = 0; i < world.asteroids.size; i++) {
            Asteroid a = world.asteroids.get(i);
            if (!inRange(a.pos.x, a.pos.y, world.ship.pos, range)) {
                continue;
            }
            float d = a.pos.dst(point) - a.radius;
            if (d < bestDist) {
                bestDist = d;
                bestRock = a;
            }
        }
        FuelPickup bestFuel = null;
        float bestFuelDist = TOUCH_SLOP;
        for (int i = 0; i < world.fuel.size; i++) {
            FuelPickup f = world.fuel.get(i);
            if (!inRange(f.pos.x, f.pos.y, world.ship.pos, range)) {
                continue;
            }
            float d = f.pos.dst(point) - FuelPickup.RADIUS;
            if (d < bestFuelDist) {
                bestFuelDist = d;
                bestFuel = f;
            }
        }
        if (bestRock != null && (bestFuel == null || bestDist <= bestFuelDist)) {
            return NavTarget.ofAsteroid(bestRock);
        }
        return bestFuel == null ? null : NavTarget.ofFuel(bestFuel);
    }
}

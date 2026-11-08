package com.example.codegame;

import com.badlogic.gdx.math.Vector2;

/**
 * Objetivo de navegación conocido (UPD-006-T09). La brújula avanzada y el mapa
 * local comparten este registro; no se inventan posiciones ni se revelan
 * entidades fuera del alcance o del conocimiento del jugador.
 */
final class NavTarget {

    enum Type {
        MOTHERSHIP,
        STATION,
        ASTEROID,
        FUEL,
        DESTINATION,
        POINT
    }

    final Type type;
    final String id;
    final String label;
    final Vector2 pos = new Vector2();
    /** Referencia viva para validar que el objetivo sigue existiendo. */
    Object ref;

    NavTarget(Type type, String id, String label, float x, float y) {
        this.type = type == null ? Type.POINT : type;
        this.id = id == null ? "" : id;
        this.label = label == null ? "" : label;
        this.pos.set(x, y);
    }

    static NavTarget ofStation(Station station, boolean mothership) {
        if (station == null) {
            return null;
        }
        NavTarget target = new NavTarget(mothership ? Type.MOTHERSHIP : Type.STATION,
                station.def.name, station.def.name, station.pos.x, station.pos.y);
        target.ref = station;
        return target;
    }

    static NavTarget ofAsteroid(Asteroid asteroid) {
        if (asteroid == null) {
            return null;
        }
        NavTarget target = new NavTarget(Type.ASTEROID, "rock@" + asteroid.seed,
                "ASTEROIDE " + asteroid.type.name(), asteroid.pos.x, asteroid.pos.y);
        target.ref = asteroid;
        return target;
    }

    static NavTarget ofFuel(FuelPickup pickup) {
        if (pickup == null) {
            return null;
        }
        NavTarget target = new NavTarget(Type.FUEL, "fuel@" + System.identityHashCode(pickup),
                "BIDON", pickup.pos.x, pickup.pos.y);
        target.ref = pickup;
        return target;
    }

    void sync() {
        if (ref instanceof Station) {
            pos.set(((Station) ref).pos);
        } else if (ref instanceof Asteroid) {
            pos.set(((Asteroid) ref).pos);
        } else if (ref instanceof FuelPickup) {
            pos.set(((FuelPickup) ref).pos);
        }
    }

    /** Comprueba que la entidad referenciada sigue viva en el mundo. */
    boolean alive(GameWorld world) {
        if (world == null) {
            return false;
        }
        if (ref == null) {
            return true;
        }
        if (ref instanceof Station) {
            return ref == world.station || ref == world.playerStation;
        }
        if (ref instanceof Asteroid) {
            return world.asteroids.contains((Asteroid) ref, true);
        }
        if (ref instanceof FuelPickup) {
            return world.fuel.contains((FuelPickup) ref, true);
        }
        return true;
    }
}

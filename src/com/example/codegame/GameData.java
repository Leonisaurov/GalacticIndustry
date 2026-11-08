package com.example.codegame;

import java.util.Random;

/** Datos compartidos de minerales, asteroides y bodega. */
final class GameData {

    static final float BASE_CARGO_CAPACITY = CargoHold.DEFAULT_CAPACITY;
    static final Commodity[] COMMODITIES = Commodity.values();
    static final AsteroidType[] ASTEROID_TYPES = AsteroidType.values();

    /** Nave nodriza del sector inicial: gran estación con todos los servicios. */
    static final float MOTHERSHIP_X = 0f;
    static final float MOTHERSHIP_Y = 480f;
    static final StationDef MOTHERSHIP = new StationDef(
            "NODRIZA", 200f, 260f, 480f, true, true, true, true, true, true);

    private GameData() {
    }

    /** Bodega inicial de la nave, todavía sin mejoras. */
    static CargoHold newCargoHold() {
        return new CargoHold(BASE_CARGO_CAPACITY);
    }

    static Commodity commodity(String name) {
        return Commodity.byDisplayName(name);
    }

    static AsteroidType asteroidType(String name) {
        return AsteroidType.byName(name);
    }

    static AsteroidType rollAsteroidType(Random random) {
        return AsteroidType.roll(random);
    }
}

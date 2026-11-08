package com.example.codegame;

/** Datos compartidos de minerales y bodega. */
final class GameData {

    static final float BASE_CARGO_CAPACITY = CargoHold.DEFAULT_CAPACITY;
    static final Commodity[] COMMODITIES = Commodity.values();

    private GameData() {
    }

    /** Bodega inicial de la nave, todavía sin mejoras. */
    static CargoHold newCargoHold() {
        return new CargoHold(BASE_CARGO_CAPACITY);
    }

    static Commodity commodity(String name) {
        return Commodity.byDisplayName(name);
    }
}

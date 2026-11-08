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

    /** Objetivos de stock del mercado inicial. */
    static final int MARKET_HIELO = 200;
    static final int MARKET_SILICATO = 200;
    static final int MARKET_HIERRO = 150;
    static final int MARKET_COBRE = 100;
    static final int MARKET_TITANIO = 60;
    static final int MARKET_ORO = 40;
    /** El stock inicial arranca al 80 % del objetivo. */
    static final float MARKET_INITIAL = 0.8f;

    /** Agentes autónomos iniciales y máximo. */
    static final int INITIAL_AGENTS = 8;
    static final int MAX_AGENTS = 24;

    static Market newMarket() {
        java.util.EnumMap<Commodity, Integer> targets =
                new java.util.EnumMap<>(Commodity.class);
        targets.put(Commodity.HIELO, MARKET_HIELO);
        targets.put(Commodity.SILICATO, MARKET_SILICATO);
        targets.put(Commodity.HIERRO, MARKET_HIERRO);
        targets.put(Commodity.COBRE, MARKET_COBRE);
        targets.put(Commodity.TITANIO, MARKET_TITANIO);
        targets.put(Commodity.ORO, MARKET_ORO);
        Market market = new Market(targets, MARKET_INITIAL);
        // La nodriza consume hielo para soporte vital y hierro/cobre para
        // mantenimiento; produce algo de silicato. El resto queda estable.
        market.setConsumption(Commodity.HIELO, 2);
        market.setConsumption(Commodity.HIERRO, 1);
        market.setConsumption(Commodity.COBRE, 1);
        market.setProduction(Commodity.SILICATO, 1);
        return market;
    }

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

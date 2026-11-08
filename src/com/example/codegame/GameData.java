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

    /** IDs estables del universo inicial (no cambiar sin migración). */
    static final String START_GALAXY_ID = "milky-way";
    static final String START_SYSTEM_ID = "sol";
    static final String START_SECTOR_ID = "sol-prime";
    static final String ANDROMEDA_GALAXY_ID = "andromeda";
    static final String DRACO_GALAXY_ID = "nebula-draco";

    /** Semillas estables por galaxia, sistema y sector. */
    static final long SEED_MILKY_WAY = 0x4D494C4B59L;
    static final long SEED_SOL = 0x534F4CL;
    static final long SEED_SOL_PRIME = 0x5052494D45L;
    static final long SEED_SOL_BELT = 0x42454C54L;
    static final long SEED_SOL_ICE = 0x494345L;
    static final long SEED_ANDROMEDA = 0x414E44524FL;
    static final long SEED_ANDROMEDA_CORE = 0x414E44434FL;
    static final long SEED_ANDROMEDA_FORGE = 0x464F524745L;
    static final long SEED_ANDROMEDA_DRIFT = 0x4452494654L;
    static final long SEED_DRACO = 0x445241434FL;
    static final long SEED_DRACO_EYE = 0x445241434F45L;

    /** Estaciones secundarias de los sectores del universo inicial. */
    static final StationDef OUTPOST = new StationDef(
            "PUESTO", 130f, 170f, 340f, true, true, false, false, true, true);
    static final StationDef MINING_RELAY = new StationDef(
            "RELEVO MINERO", 120f, 160f, 320f, false, true, false, false, true, true);
    static final StationDef FORGE = new StationDef(
            "FORJA", 150f, 190f, 380f, true, true, false, true, true, true);

    /** Construye el universo inicial con IDs y semillas estables. */
    static UniverseState newUniverse() {
        UniverseState universe = new UniverseState();

        GalaxyDef milkyWay = new GalaxyDef(START_GALAXY_ID, "VIA LACTEA",
                "Galaxia de arranque; equilibrada y con la nodriza.",
                SEED_MILKY_WAY, 0x2A3A6A, 0xD8E6FF,
                biases(1.10f, 1.00f, 1.00f, 1.00f, 0.95f, 0.90f));
        SystemDef sol = new SystemDef(START_SYSTEM_ID, "SOL",
                START_GALAXY_ID, SEED_SOL);
        sol.addSector(new SectorDef(START_SECTOR_ID, "SECTOR INICIAL",
                START_SYSTEM_ID, SEED_SOL_PRIME,
                SectorDef.DEFAULT_WIDTH, SectorDef.DEFAULT_HEIGHT,
                SectorDef.Profile.INICIAL, MOTHERSHIP.safeRadius)
                .addStation(MOTHERSHIP));
        sol.addSector(new SectorDef("sol-belt", "CINTURON",
                START_SYSTEM_ID, SEED_SOL_BELT,
                SectorDef.DEFAULT_WIDTH, SectorDef.DEFAULT_HEIGHT,
                SectorDef.Profile.CINTURON, 340f)
                .addStation(MINING_RELAY));
        sol.addSector(new SectorDef("sol-ice", "CAMPO HELADO",
                START_SYSTEM_ID, SEED_SOL_ICE,
                10000f, 10000f, SectorDef.Profile.HELADO, 300f)
                .addStation(OUTPOST));
        milkyWay.addSystem(sol);
        universe.addGalaxy(milkyWay);

        GalaxyDef andromeda = new GalaxyDef(ANDROMEDA_GALAXY_ID, "ANDROMEDA",
                "Galaxia rica en metales y titanio.",
                SEED_ANDROMEDA, 0x4A2A5A, 0xFFE0C8,
                biases(0.85f, 1.00f, 1.25f, 1.15f, 1.30f, 1.10f));
        SystemDef andromedaCore = new SystemDef("andromeda-core", "NUCLEO",
                ANDROMEDA_GALAXY_ID, SEED_ANDROMEDA_CORE);
        andromedaCore.addSector(new SectorDef("andromeda-forge", "FORJA",
                "andromeda-core", SEED_ANDROMEDA_FORGE,
                14000f, 14000f, SectorDef.Profile.METALICO, 380f)
                .addStation(FORGE));
        andromedaCore.addSector(new SectorDef("andromeda-drift", "DERIVA",
                "andromeda-core", SEED_ANDROMEDA_DRIFT,
                SectorDef.DEFAULT_WIDTH, SectorDef.DEFAULT_HEIGHT,
                SectorDef.Profile.CINTURON, 340f)
                .addStation(OUTPOST));
        andromeda.addSystem(andromedaCore);
        universe.addGalaxy(andromeda);

        GalaxyDef draco = new GalaxyDef(DRACO_GALAXY_ID, "NEBULOSA DRACO",
                "Galaxia ficticia helada, rica en hielo y silicato.",
                SEED_DRACO, 0x1F4A4A, 0xC8FFF0,
                biases(1.50f, 1.20f, 0.90f, 0.85f, 0.80f, 0.70f));
        SystemDef dracoEye = new SystemDef("draco-eye", "OJO",
                DRACO_GALAXY_ID, SEED_DRACO_EYE);
        dracoEye.addSector(new SectorDef("draco-core", "NUCLEO HELADO",
                "draco-eye", SEED_DRACO_EYE,
                SectorDef.DEFAULT_WIDTH, SectorDef.DEFAULT_HEIGHT,
                SectorDef.Profile.HELADO, 320f)
                .addStation(OUTPOST));
        draco.addSystem(dracoEye);
        universe.addGalaxy(draco);

        return universe;
    }

    private static java.util.EnumMap<Commodity, Float> biases(
            float hielo, float silicato, float hierro, float cobre,
            float titanio, float oro) {
        java.util.EnumMap<Commodity, Float> map =
                new java.util.EnumMap<>(Commodity.class);
        map.put(Commodity.HIELO, hielo);
        map.put(Commodity.SILICATO, silicato);
        map.put(Commodity.HIERRO, hierro);
        map.put(Commodity.COBRE, cobre);
        map.put(Commodity.TITANIO, titanio);
        map.put(Commodity.ORO, oro);
        return map;
    }

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

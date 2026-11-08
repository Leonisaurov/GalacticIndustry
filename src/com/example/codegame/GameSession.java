package com.example.codegame;

/**
 * Sesion de juego: agrupa el perfil del jugador, el sector activo (con su nave)
 * y los servicios de guardado de la partida en curso.
 *
 * <p>Es el punto de composicion que usara {@link MyGame} para crear, cargar y
 * guardar la partida. El piloto independiente y el loadout modular se anaden en
 * T-102 y T-103; aqui solo se prepara la estructura sin mover la logica de
 * {@link Upgrades} ni cambiar la economia.</p>
 */
class GameSession {

    /**
     * Clave heredada de la partida en curso. T-104 la sustituye por
     * {@code SaveService} v2 con migracion desde este mismo formato.
     */
    static final String RUN_KEY = "run2";

    final PlayerProfile profile;
    /** Piloto del jugador; su salud y O2 no pertenecen a la nave. */
    final Pilot pilot;
    /** Filtro y fugas del soporte vital; se conectará al O2 en T-202/T-203. */
    final LifeSupport lifeSupport;
    /** Jerarquía de universo; el runtime finito llega en T-802. */
    final UniverseState universe;
    /** Sector activo según la definición del universo. */
    SectorDef currentSector;
    /** Streaming de sectores: carga cercanos y descarga lejanos. */
    final SectorStreamer streamer;
    /** Estación personal del jugador; vacía hasta construir módulos (T-902/T-903). */
    final PlayerStation playerStation = new PlayerStation();
    /** Última posición conocida del avatar dentro del interior. */
    String interiorRoom;
    float interiorX;
    float interiorY;
    boolean interiorActive;
    /** Runtime del sector actual; contiene la nave, asteroides y efectos. */
    GameWorld sector;

    /** Propiedad de nave (UPD-006-T27). Los guardados antiguos migran a true. */
    boolean hasShip = true;
    /** Fondo de arranque concedido una sola vez por cuenta. */
    boolean starterBudgetGranted;
    int starterBudget;
    /** Nave comprada en el mercado; null si es la nave clásica de pruebas. */
    ShipSpec ownedShip;
    /** Mercado determinista de naves de la nodriza. */
    ShipMarket shipMarket;

    private boolean runActive;

    GameSession() {
        this(new Upgrades());
    }

    GameSession(Upgrades upgrades) {
        this(new PlayerProfile(upgrades));
    }

    GameSession(PlayerProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile nulo");
        }
        this.profile = profile;
        this.pilot = new Pilot();
        this.lifeSupport = new LifeSupport();
        this.universe = GameData.newUniverse();
        this.currentSector = universe.defaultSector();
        this.streamer = new SectorStreamer(universe, playerStation);
        this.streamer.switchTo(currentSector == null ? null : currentSector.id, null);
        this.universe.discoverInitial(currentSector == null ? null : currentSector.id);
        this.sector = new GameWorld(profile.upgrades, currentSector,
                universe.galaxyOfSector(currentSector == null ? null
                        : currentSector.id), playerStation);
        this.sector.attachLifeSupport(lifeSupport);
    }

    Upgrades upgrades() {
        return profile.upgrades;
    }

    Ship ship() {
        return sector.ship;
    }

    /** Bodega de la nave activa. */
    CargoHold cargo() {
        return sector.ship.cargo();
    }

    /**
     * Guarda el estado resumido del sector actual en el universo. Se usa al
     * salir hacia otro sector; los recursos minados quedan persistidos.
     */
    void saveAvatar(Avatar avatar, boolean active) {
        if (avatar == null) {
            interiorActive = active;
            return;
        }
        interiorRoom = avatar.roomId;
        interiorX = avatar.x;
        interiorY = avatar.y;
        interiorActive = active;
    }

    void restoreAvatar(Avatar avatar) {
        if (avatar == null || interiorRoom == null) {
            return;
        }
        avatar.roomId = interiorRoom;
        avatar.x = interiorX;
        avatar.y = interiorY;
    }

    SectorRuntime.Snapshot captureCurrentSector() {
        SectorRuntime.Snapshot snapshot = SectorRuntime.capture(sector,
                currentSector == null ? "sol-prime" : currentSector.id);
        universe.putSnapshot(snapshot);
        return snapshot;
    }

    /**
     * Cambia el sector activo conservando la nave (combustible, carga, daños y
     * energía). El sector anterior queda como snapshot y el destino se carga
     * desde el streaming. Devuelve el nuevo runtime o null si el ID no existe.
     */
    GameWorld switchSector(String sectorId) {
        SectorDef target = universe.sector(sectorId);
        if (target == null || sector == null) {
            return null;
        }
        GameWorld old = sector;
        streamer.switchTo(sectorId, old);
        GameWorld next = streamer.load(sectorId, profile.upgrades);
        if (next == null) {
            return null;
        }
        next.ship.copyTravelStateFrom(old.ship);
        next.attachLifeSupport(lifeSupport);
        universe.discover(sectorId);
        universe.discoverSystem(target.systemId);
        currentSector = target;
        sector = next;
        return next;
    }

    boolean runActive() {
        return runActive;
    }

    /** Marca la sesión como cargada/activa tras un guardado v2 válido. */
    void markActive() {
        runActive = true;
    }

    /** Empieza una partida nueva en el sector actual. */
    void startRun() {
        pilot.reset();
        lifeSupport.reset();
        sector.start();
        runActive = true;
    }

    /**
     * Arranca el flujo de comienzo aprobado (UPD-006-T27): personaje a pie en
     * la nodriza, sin nave y con 1.200 CR la primera vez por cuenta.
     */
    void beginStarterFlow() {
        beginStarterFlow(true);
    }

    /** Variante que conserva el mundo tras la muerte del personaje anterior. */
    void beginStarterFlow(boolean resetWorld) {
        if (!starterBudgetGranted) {
            profile.upgrades.setCoins(1200);
            starterBudget = 1200;
            starterBudgetGranted = true;
        }
        hasShip = false;
        ownedShip = null;
        if (shipMarket == null) {
            shipMarket = new ShipMarket(marketSeed());
        }
        pilot.reset();
        lifeSupport.reset();
        if (resetWorld) {
            sector.start();
        } else {
            sector.ship.reset(profile.upgrades, 0f, 0f);
        }
        sector.ship.wrecked = true;
        sector.ship.docked = false;
        runActive = true;
    }

    long marketSeed() {
        String id = profile == null ? null : profile.id;
        long hash = 0x5A17C0DEL;
        if (id != null) {
            for (int i = 0; i < id.length(); i++) {
                hash = hash * 31L + id.charAt(i);
            }
        }
        return hash;
    }

    /** Coste total de una oferta con el combustible inicial solicitado. */
    int purchaseCost(ShipOffer offer, int fuelUnits) {
        if (offer == null || offer.spec == null) {
            return 0;
        }
        int units = Math.max(0, Math.min(fuelUnits, (int) offer.spec.fuelCapacity));
        return offer.spec.price + units;
    }

    /**
     * Compra la nave y la deja en la boca de atraque. Exige que el
     * combustible inicial llegue al mínimo operativo (60 UM) y que quede saldo
     * suficiente para al menos una reparación básica.
     */
    boolean purchaseShip(ShipOffer offer, int fuelUnits) {
        if (offer == null || offer.spec == null || hasShip) {
            return false;
        }
        if (fuelUnits < 60 || fuelUnits > (int) offer.spec.fuelCapacity) {
            return false;
        }
        int cost = purchaseCost(offer, fuelUnits);
        if (profile.upgrades.coins() < cost) {
            return false;
        }
        if (!profile.upgrades.spendCoins(cost)) {
            return false;
        }
        hasShip = true;
        ownedShip = offer.spec;
        Ship ship = sector.ship;
        offer.spec.applyTo(ship, profile.upgrades);
        ship.fuel = Math.min(fuelUnits, ship.maxFuel);
        if (sector.station != null) {
            sector.station.snapToDock(ship.pos);
            ship.heading = sector.station.dockHeading();
            ship.dir.set(com.badlogic.gdx.math.MathUtils.cosDeg(ship.heading),
                    com.badlogic.gdx.math.MathUtils.sinDeg(ship.heading));
        }
        ship.docked = false;
        ship.wrecked = false;
        return true;
    }

    /** Nave de servicio de emergencia (UPD-006-T31), sin coste inmediato. */
    boolean grantServiceShip() {
        if (hasShip) {
            return false;
        }
        ShipSpec spec = new ShipSpec("service-1", "NAVE DE SERVICIO",
                ShipSpec.Category.SCRAP, ShipClassDef.MINER, 70f, 0.45f,
                110f, 1.15f, 1.10f, 85f, new String[0], 0,
                "Cesion de la nodriza; se descuenta de los proximos beneficios.");
        hasShip = true;
        ownedShip = spec;
        spec.applyTo(sector.ship, profile.upgrades);
        sector.ship.fuel = Math.min(60f, sector.ship.maxFuel);
        if (sector.station != null) {
            sector.station.snapToDock(sector.ship.pos);
            sector.ship.heading = sector.station.dockHeading();
        }
        sector.ship.docked = false;
        sector.ship.wrecked = false;
        profile.upgrades.addDebt(300);
        return true;
    }

    /** Carga la partida guardada. Devuelve true si habia una partida valida. */
    boolean loadRun() {
        return restoreLegacy(profile.upgrades.getText(RUN_KEY));
    }

    /**
     * Restaura el formato heredado {@code run2} (texto del mundo + linea opcional
     * de piloto). Lo usa la migracion de SaveService y el guardado actual.
     */
    boolean restoreLegacy(String saved) {
        if (saved == null) {
            return false;
        }
        // El piloto y el soporte vital se parsean antes de tocar el mundo.
        Pilot parsedPilot = null;
        LifeSupport parsedLife = null;
        for (String line : saved.split("\n")) {
            if (line.startsWith("pilot ")) {
                Pilot candidate = new Pilot();
                if (candidate.restore(line)) {
                    parsedPilot = candidate;
                }
            } else if (line.startsWith("life ")) {
                LifeSupport candidate = new LifeSupport();
                if (candidate.restore(line)) {
                    parsedLife = candidate;
                }
            }
        }
        if (sector.restore(saved)) {
            if (parsedPilot != null) {
                pilot.copyFrom(parsedPilot);
            } else {
                // Guardados anteriores a T-102 no tienen linea de piloto.
                pilot.reset();
            }
            if (parsedLife != null) {
                lifeSupport.copyFrom(parsedLife);
            } else {
                // Guardados anteriores a T-201 no tienen linea de soporte.
                lifeSupport.reset();
            }
            runActive = true;
            return true;
        }
        return false;
    }

    /** Guarda la partida si hay una activa y no ha terminado. */
    boolean saveRun() {
        if (runActive && !sector.isOver()) {
            profile.upgrades.putText(RUN_KEY,
                    pilot.serialize() + "\n" + lifeSupport.serialize() + "\n" + sector.serialize());
            return true;
        }
        return false;
    }

    /** Borra la partida guardada y deja la sesion sin partida activa. */
    void clearRun() {
        runActive = false;
        profile.upgrades.putText(RUN_KEY, null);
    }

    /** Avanza el sector activo y el consumo de O2 del piloto. */
    void update(float dt, Controls c) {
        sector.update(dt, c);
        lifeSupport.update(pilot, sector.ship, dt);
    }
}

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
    /** Runtime del sector actual; contiene la nave, asteroides y efectos. */
    GameWorld sector;

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
        this.streamer = new SectorStreamer(universe);
        this.streamer.switchTo(currentSector == null ? null : currentSector.id, null);
        this.sector = new GameWorld(profile.upgrades, currentSector);
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

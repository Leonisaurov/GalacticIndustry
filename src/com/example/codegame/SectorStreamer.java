package com.example.codegame;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Streaming de sectores: mantiene cargado el detalle del sector activo y de sus
 * vecinos cercanos, y descarga (deja solo snapshot) los lejanos. No guarda todo
 * el universo en memoria: el coste depende del radio de carga, no del tamaño
 * total.
 */
final class SectorStreamer {

    /** Radio de carga en unidades del mapa de universo. */
    static final float LOAD_RADIUS = 10f;

    private final UniverseState universe;
    private final PlayerStation playerStation;
    private final Set<String> loaded = new LinkedHashSet<>();
    private String activeId;

    SectorStreamer(UniverseState universe) {
        this(universe, new PlayerStation());
    }

    SectorStreamer(UniverseState universe, PlayerStation playerStation) {
        this.universe = universe;
        this.playerStation = playerStation != null ? playerStation : new PlayerStation();
    }

    String activeId() {
        return activeId;
    }

    boolean isLoaded(String sectorId) {
        return sectorId != null && loaded.contains(sectorId);
    }

    List<String> loadedSectors() {
        return new ArrayList<>(loaded);
    }

    int loadedCount() {
        return loaded.size();
    }

    int unloadedCount() {
        return universe.sectorCount() - loaded.size();
    }

    /**
     * Cambia el sector activo. Si había otro, guarda su estado resumido antes
     * de descargarlo. Los sectores fuera del radio quedan descargados.
     *
     * @param oldWorld runtime del sector anterior; puede ser null si no había.
     */
    void switchTo(String sectorId, GameWorld oldWorld) {
        if (sectorId == null || universe.sector(sectorId) == null) {
            return;
        }
        if (activeId != null && !activeId.equals(sectorId) && oldWorld != null) {
            universe.putSnapshot(SectorRuntime.capture(oldWorld, activeId));
        }
        activeId = sectorId;
        recomputeLoaded();
    }

    /** Recalcula qué sectores están dentro del radio del activo. */
    void recomputeLoaded() {
        loaded.clear();
        SectorDef active = universe.sector(activeId);
        if (active == null) {
            return;
        }
        for (int i = 0; i < universe.galaxies().size(); i++) {
            GalaxyDef galaxy = universe.galaxies().get(i);
            for (int k = 0; k < galaxy.systems.size(); k++) {
                SystemDef system = galaxy.systems.get(k);
                for (int j = 0; j < system.sectors.size(); j++) {
                    SectorDef sector = system.sectors.get(j);
                    if (sector.distanceTo(active) <= LOAD_RADIUS) {
                        loaded.add(sector.id);
                    }
                }
            }
        }
    }

    /** Carga (o recarga) un sector desde su snapshot, si existe. */
    GameWorld load(String sectorId, Upgrades upgrades) {
        SectorDef def = universe.sector(sectorId);
        if (def == null) {
            return null;
        }
        GameWorld world = new GameWorld(upgrades, def,
                universe.galaxyOfSector(def.id), playerStation);
        world.start();
        SectorRuntime.Snapshot snapshot = universe.snapshot(sectorId);
        if (snapshot != null) {
            SectorRuntime.restore(world, snapshot);
        }
        loaded.add(sectorId);
        return world;
    }
}

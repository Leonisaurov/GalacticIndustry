package com.example.codegame;

import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de una galaxia: nodo con semilla propia y de uno a tres sectores.
 * Los IDs son estables y sirven para guardar y buscar sin ambigüedad.
 */
final class SystemDef {

    static final int MIN_SECTORS = 1;
    static final int MAX_SECTORS = 3;

    final String id;
    final String name;
    final String galaxyId;
    final long seed;
    final List<SectorDef> sectors = new ArrayList<>();

    SystemDef(String id, String name, String galaxyId, long seed) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id de sistema vacio");
        }
        this.id = id;
        this.name = name == null ? id : name;
        this.galaxyId = galaxyId;
        this.seed = seed;
    }

    SectorDef addSector(SectorDef sector) {
        if (sector != null && sectors.size() < MAX_SECTORS
                && sector(sector.id) == null) {
            sectors.add(sector);
        }
        return sector;
    }

    SectorDef sector(String sectorId) {
        if (sectorId == null) {
            return null;
        }
        for (int i = 0; i < sectors.size(); i++) {
            if (sectorId.equals(sectors.get(i).id)) {
                return sectors.get(i);
            }
        }
        return null;
    }

    int sectorCount() {
        return sectors.size();
    }
}

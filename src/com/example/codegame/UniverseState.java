package com.example.codegame;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenedor de galaxias, sistemas y sectores. La jerarquía es
 * universo → galaxia → sistema → sector. Los IDs son estables y únicos dentro
 * de cada nivel para poder guardar y buscar sin depender del orden.
 */
final class UniverseState {

    private final List<GalaxyDef> galaxies = new ArrayList<>();

    GalaxyDef addGalaxy(GalaxyDef galaxy) {
        if (galaxy != null && galaxy(galaxy.id) == null) {
            galaxies.add(galaxy);
        }
        return galaxy;
    }

    List<GalaxyDef> galaxies() {
        return new ArrayList<>(galaxies);
    }

    int galaxyCount() {
        return galaxies.size();
    }

    int systemCount() {
        int count = 0;
        for (int i = 0; i < galaxies.size(); i++) {
            count += galaxies.get(i).systemCount();
        }
        return count;
    }

    int sectorCount() {
        int count = 0;
        for (int i = 0; i < galaxies.size(); i++) {
            count += galaxies.get(i).sectorCount();
        }
        return count;
    }

    GalaxyDef galaxy(String galaxyId) {
        if (galaxyId == null) {
            return null;
        }
        for (int i = 0; i < galaxies.size(); i++) {
            if (galaxyId.equals(galaxies.get(i).id)) {
                return galaxies.get(i);
            }
        }
        return null;
    }

    SystemDef system(String systemId) {
        if (systemId == null) {
            return null;
        }
        for (int i = 0; i < galaxies.size(); i++) {
            SystemDef system = galaxies.get(i).system(systemId);
            if (system != null) {
                return system;
            }
        }
        return null;
    }

    SectorDef sector(String sectorId) {
        if (sectorId == null) {
            return null;
        }
        for (int i = 0; i < galaxies.size(); i++) {
            GalaxyDef galaxy = galaxies.get(i);
            for (int k = 0; k < galaxy.systems.size(); k++) {
                SectorDef sector = galaxy.systems.get(k).sector(sectorId);
                if (sector != null) {
                    return sector;
                }
            }
        }
        return null;
    }

    /** Galaxia de arranque: la Vía Láctea. */
    GalaxyDef defaultGalaxy() {
        GalaxyDef galaxy = galaxy(GameData.START_GALAXY_ID);
        return galaxy != null ? galaxy : galaxies.isEmpty() ? null : galaxies.get(0);
    }

    /** Sistema y sector de arranque. */
    SystemDef defaultSystem() {
        GalaxyDef galaxy = defaultGalaxy();
        return galaxy == null || galaxy.systems.isEmpty()
                ? null : galaxy.systems.get(0);
    }

    SectorDef defaultSector() {
        SystemDef system = defaultSystem();
        return system == null || system.sectors.isEmpty()
                ? null : system.sectors.get(0);
    }

    /** Lista de errores de validación; vacía si los datos son coherentes. */
    List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (galaxies.isEmpty()) {
            errors.add("el universo no tiene galaxias");
            return errors;
        }
        List<String> galaxyIds = new ArrayList<>();
        List<String> systemIds = new ArrayList<>();
        List<String> sectorIds = new ArrayList<>();
        for (int i = 0; i < galaxies.size(); i++) {
            GalaxyDef galaxy = galaxies.get(i);
            if (galaxyIds.contains(galaxy.id)) {
                errors.add("id de galaxia duplicado: " + galaxy.id);
            }
            galaxyIds.add(galaxy.id);
            if (galaxy.seed == 0L) {
                errors.add("galaxia sin semilla: " + galaxy.id);
            }
            if (galaxy.nebulaColor == 0 || galaxy.starColor == 0) {
                errors.add("galaxia sin paleta: " + galaxy.id);
            }
            for (Commodity commodity : Commodity.values()) {
                float bias = galaxy.bias(commodity);
                if (!Float.isFinite(bias) || bias <= 0f) {
                    errors.add("sesgo invalido en " + galaxy.id + ": " + commodity);
                }
            }
            if (galaxy.systems.size() < SystemDef.MIN_SECTORS) {
                errors.add("galaxia sin sistemas: " + galaxy.id);
            }
            for (int k = 0; k < galaxy.systems.size(); k++) {
                SystemDef system = galaxy.systems.get(k);
                if (systemIds.contains(system.id)) {
                    errors.add("id de sistema duplicado: " + system.id);
                }
                systemIds.add(system.id);
                if (system.galaxyId != null && !galaxy.id.equals(system.galaxyId)) {
                    errors.add("sistema con galaxia incoherente: " + system.id);
                }
                if (system.sectorCount() < SystemDef.MIN_SECTORS
                        || system.sectorCount() > SystemDef.MAX_SECTORS) {
                    errors.add("sistema con numero de sectores invalido: " + system.id);
                }
                for (int j = 0; j < system.sectors.size(); j++) {
                    SectorDef sector = system.sectors.get(j);
                    if (sectorIds.contains(sector.id)) {
                        errors.add("id de sector duplicado: " + sector.id);
                    }
                    sectorIds.add(sector.id);
                    if (sector.systemId != null && !system.id.equals(sector.systemId)) {
                        errors.add("sector con sistema incoherente: " + sector.id);
                    }
                    if (sector.width <= 0f || sector.height <= 0f) {
                        errors.add("sector sin limites: " + sector.id);
                    }
                    if (sector.safeRadius >= Math.min(sector.width, sector.height) * 0.5f) {
                        errors.add("zona segura mayor que el sector: " + sector.id);
                    }
                    if (sector.profile == null) {
                        errors.add("sector sin perfil: " + sector.id);
                    }
                    if (sector.stationCount() <= 0) {
                        errors.add("sector sin estaciones: " + sector.id);
                    }
                }
            }
        }
        return errors;
    }

    boolean valid() {
        return validate().isEmpty();
    }
}

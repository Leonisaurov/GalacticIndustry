package com.example.codegame;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Galaxia como pack de contenido: semilla estable, paleta visual y sesgos de
 * minerales. Contiene uno o más sistemas. Es un dato puro, sin render ni
 * simulación; añadir una galaxia debe ser añadir datos.
 */
final class GalaxyDef {

    final String id;
    final String name;
    final String description;
    /** Semilla estable para generar sistemas, sectores y recursos. */
    final long seed;
    /** Colores de nebulosa y estrellas en formato 0xRRGGBB. */
    final int nebulaColor;
    final int starColor;
    private final EnumMap<Commodity, Float> mineralBias =
            new EnumMap<>(Commodity.class);
    final List<SystemDef> systems = new ArrayList<>();

    GalaxyDef(String id, String name, String description, long seed,
              int nebulaColor, int starColor, Map<Commodity, Float> biases) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id de galaxia vacio");
        }
        this.id = id;
        this.name = name == null ? id : name;
        this.description = description == null ? "" : description;
        this.seed = seed;
        this.nebulaColor = nebulaColor;
        this.starColor = starColor;
        for (Commodity commodity : Commodity.values()) {
            float value = biases == null ? 1f
                    : biases.getOrDefault(commodity, 1f);
            mineralBias.put(commodity, value > 0f ? value : 1f);
        }
    }

    SystemDef addSystem(SystemDef system) {
        if (system != null && system(system.id) == null) {
            systems.add(system);
        }
        return system;
    }

    SystemDef system(String systemId) {
        if (systemId == null) {
            return null;
        }
        for (int i = 0; i < systems.size(); i++) {
            if (systemId.equals(systems.get(i).id)) {
                return systems.get(i);
            }
        }
        return null;
    }

    /** Multiplicador de aparición/valor del mineral en esta galaxia. */
    float bias(Commodity commodity) {
        Float value = mineralBias.get(commodity);
        return value == null ? 1f : value;
    }

    EnumMap<Commodity, Float> biases() {
        return new EnumMap<>(mineralBias);
    }

    int systemCount() {
        return systems.size();
    }

    int sectorCount() {
        int count = 0;
        for (int i = 0; i < systems.size(); i++) {
            count += systems.get(i).sectorCount();
        }
        return count;
    }
}

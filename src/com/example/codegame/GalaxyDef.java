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
    private final EnumMap<Commodity, Integer> marketTargets =
            new EnumMap<>(Commodity.class);
    /** Fracción inicial del objetivo de stock y spread del mercado. */
    final float marketInitial;
    final float marketSpread;
    final List<SystemDef> systems = new ArrayList<>();
    /** Centroide de los sistemas, recalculado al añadirlos (UPD-006-T15). */
    float mapX;
    float mapY;

    GalaxyDef(String id, String name, String description, long seed,
              int nebulaColor, int starColor, Map<Commodity, Float> biases) {
        this(id, name, description, seed, nebulaColor, starColor, biases,
                null, GameData.MARKET_INITIAL, PriceModel.DEFAULT_SPREAD);
    }

    GalaxyDef(String id, String name, String description, long seed,
              int nebulaColor, int starColor, Map<Commodity, Float> biases,
              Map<Commodity, Integer> targets, float marketInitial,
              float marketSpread) {
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
            int target = targets == null ? GameData.defaultMarketTarget(commodity)
                    : targets.getOrDefault(commodity,
                    GameData.defaultMarketTarget(commodity));
            marketTargets.put(commodity, Math.max(1, target));
        }
        this.marketInitial = Math.max(0f, marketInitial);
        this.marketSpread = Math.max(0.01f, Math.min(0.5f, marketSpread));
    }

    SystemDef addSystem(SystemDef system) {
        if (system != null && system(system.id) == null) {
            systems.add(system);
            updatePosition();
        }
        return system;
    }

    /** Centroide determinista de las posiciones de sus sistemas. */
    void updatePosition() {
        if (systems.isEmpty()) {
            mapX = 0f;
            mapY = 0f;
            return;
        }
        float sx = 0f;
        float sy = 0f;
        for (int i = 0; i < systems.size(); i++) {
            systems.get(i).updatePosition();
            sx += systems.get(i).mapX;
            sy += systems.get(i).mapY;
        }
        mapX = sx / systems.size();
        mapY = sy / systems.size();
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

    int marketTarget(Commodity commodity) {
        Integer value = marketTargets.get(commodity);
        return value == null ? GameData.defaultMarketTarget(commodity) : value;
    }

    EnumMap<Commodity, Integer> marketTargets() {
        return new EnumMap<>(marketTargets);
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

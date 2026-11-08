package com.example.codegame;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Definición de un módulo construible de la estación personal. Cada módulo
 * ocupa una ranura, tiene coste en créditos y materiales, puede exigir otro
 * módulo y aporta un servicio concreto. El catálogo es ampliable sin tocar el
 * núcleo de {@link PlayerStation}.
 */
enum ModuleDef {

    STORAGE("ALMACEN", "Guardar mineral", 120, 0, null,
            Service.STORAGE, materials(0, 0, 20, 0, 0, 0)),
    REPAIR("TALLER", "Reparar casco y sistemas", 180, 0, STORAGE,
            Service.REPAIR, materials(0, 0, 15, 10, 0, 0)),
    REFINERY("REFINERIA", "Refinar mineral", 260, 0, STORAGE,
            Service.REFINERY, materials(0, 30, 0, 0, 10, 0)),
    MARKET("MERCADO", "Comprar y vender", 220, 0, STORAGE,
            Service.MARKET, materials(0, 10, 0, 20, 0, 0));

    enum Service {
        STORAGE,
        REPAIR,
        REFINERY,
        MARKET
    }

    final String displayName;
    final String description;
    final int creditCost;
    /** Nivel mínimo de estación (reservado para ampliaciones). */
    final int tier;
    /** Módulo que debe existir antes de construir este. */
    final ModuleDef requires;
    final Service service;
    private final EnumMap<Commodity, Integer> materials =
            new EnumMap<>(Commodity.class);

    ModuleDef(String displayName, String description, int creditCost, int tier,
              ModuleDef requires, Service service, Map<Commodity, Integer> cost) {
        this.displayName = displayName;
        this.description = description;
        this.creditCost = Math.max(0, creditCost);
        this.tier = Math.max(0, tier);
        this.requires = requires;
        this.service = service;
        for (Commodity commodity : Commodity.values()) {
            int amount = cost == null ? 0 : cost.getOrDefault(commodity, 0);
            materials.put(commodity, Math.max(0, amount));
        }
    }

    int material(Commodity commodity) {
        Integer amount = materials.get(commodity);
        return amount == null ? 0 : amount;
    }

    Map<Commodity, Integer> materials() {
        return Collections.unmodifiableMap(materials);
    }

    boolean needsMaterials() {
        for (Commodity commodity : Commodity.values()) {
            if (material(commodity) > 0) {
                return true;
            }
        }
        return false;
    }

    static ModuleDef byId(String id) {
        if (id == null) {
            return null;
        }
        for (ModuleDef def : values()) {
            if (def.name().equalsIgnoreCase(id)
                    || def.displayName.equalsIgnoreCase(id)) {
                return def;
            }
        }
        return null;
    }

    private static EnumMap<Commodity, Integer> materials(
            int hielo, int silicato, int hierro, int cobre, int titanio, int oro) {
        EnumMap<Commodity, Integer> map = new EnumMap<>(Commodity.class);
        map.put(Commodity.HIELO, hielo);
        map.put(Commodity.SILICATO, silicato);
        map.put(Commodity.HIERRO, hierro);
        map.put(Commodity.COBRE, cobre);
        map.put(Commodity.TITANIO, titanio);
        map.put(Commodity.ORO, oro);
        return map;
    }
}

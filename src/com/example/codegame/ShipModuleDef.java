package com.example.codegame;

/**
 * Catálogo de módulos de nave: cada uno ocupa una ranura de {@link ShipLoadout}
 * y tiene un efecto real (no solo un número).
 *
 * <p>UPD-006 separa el piloto automático de minería ({@link #AUTOPILOT}) del
 * de estacionamiento ({@link #DOCKING_AUTOPILOT}) y añade la brújula avanzada
 * ({@link #NAVIGATION}). Cada módulo declara su tipo de montaje para la pantalla
 * visual de equipamiento.</p>
 */
enum ShipModuleDef {

    AUTOPILOT("autopilot", "AUTOPILOTO", "Elige blancos y vuela solo", 150,
            ShipSlotDef.Mount.NAV),
    MINING("mining", "MINERIA", "Laser mas potente", 180,
            ShipSlotDef.Mount.MINING),
    RADAR("radar", "RADAR", "Sensores y radar circular", 140,
            ShipSlotDef.Mount.SENSOR),
    PROPULSION("propulsion", "PROPULSION", "Mas empuje y velocidad", 200,
            ShipSlotDef.Mount.PROPULSION),
    TOOLS("tools", "HERRAMIENTAS", "Mayor alcance del tractor", 160,
            ShipSlotDef.Mount.UTILITY),
    NAVIGATION("navigation", "NAVEGACION", "Brujula avanzada y objetivos", 190,
            ShipSlotDef.Mount.NAV),
    DOCKING_AUTOPILOT("docking", "ATRAQUE AUTO", "Aproxima, alinea y acopla", 220,
            ShipSlotDef.Mount.NAV);

    final String id;
    final String displayName;
    final String description;
    final int creditCost;
    final ShipSlotDef.Mount mountType;

    ShipModuleDef(String id, String displayName, String description,
                  int creditCost, ShipSlotDef.Mount mountType) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.creditCost = Math.max(0, creditCost);
        this.mountType = mountType == null ? ShipSlotDef.Mount.UNIVERSAL : mountType;
    }

    static ShipModuleDef byId(String id) {
        if (id == null) {
            return null;
        }
        for (ShipModuleDef def : values()) {
            if (def.id.equalsIgnoreCase(id)
                    || def.name().equalsIgnoreCase(id)) {
                return def;
            }
        }
        return null;
    }

    /** Módulos compatibles con una ranura, en orden de catálogo. */
    static int compatibleCount(ShipSlotDef slot) {
        int count = 0;
        for (ShipModuleDef def : values()) {
            if (slot != null && slot.accepts(def)) {
                count++;
            }
        }
        return count;
    }
}

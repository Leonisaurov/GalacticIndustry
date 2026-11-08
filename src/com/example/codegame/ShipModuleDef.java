package com.example.codegame;

/**
 * Catálogo de módulos de nave: cada uno ocupa una ranura de {@link ShipLoadout}
 * y tiene un efecto real (no solo un número). El piloto automático deja de ser
 * una capacidad universal: requiere el módulo instalado.
 */
enum ShipModuleDef {

    AUTOPILOT("autopilot", "AUTOPILOTO", "Elige blancos y vuela solo", 150),
    MINING("mining", "MINERIA", "Laser mas potente", 180),
    RADAR("radar", "RADAR", "Selecciona blancos lejanos", 140),
    PROPULSION("propulsion", "PROPULSION", "Mas empuje y velocidad", 200),
    TOOLS("tools", "HERRAMIENTAS", "Mayor alcance del tractor", 160);

    final String id;
    final String displayName;
    final String description;
    final int creditCost;

    ShipModuleDef(String id, String displayName, String description,
                  int creditCost) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.creditCost = Math.max(0, creditCost);
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
}

package com.example.codegame;

/**
 * Clase de nave: función económica, estadísticas base y silueta. No son meras
 * variaciones de color: cambian capacidad, depósito, casco, empuje, velocidad y
 * proporciones del dibujo. La personalización se hace con módulos intercambiables
 * del {@link ShipLoadout}.
 */
enum ShipClassDef {

    MINER("minero", "MINERO", Role.MINING,
            1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f),
    EXPLORER("explorador", "EXPLORADOR", Role.EXPLORATION,
            0.70f, 1.50f, 0.85f, 1.25f, 1.20f, 0.82f, 1.12f),
    HAULER("transporte", "TRANSPORTE", Role.HAULING,
            1.80f, 0.85f, 1.30f, 0.75f, 0.82f, 1.32f, 0.86f);

    enum Role {
        MINING,
        EXPLORATION,
        HAULING
    }

    final String id;
    final String displayName;
    final Role role;
    /** Multiplicadores sobre las mejoras base. */
    final float cargoMultiplier;
    final float fuelMultiplier;
    final float hullMultiplier;
    final float thrustMultiplier;
    final float speedMultiplier;
    /** Proporciones del dibujo: ancho y largo. */
    final float shapeWidth;
    final float shapeLength;

    ShipClassDef(String id, String displayName, Role role, float cargo,
                 float fuel, float hull, float thrust, float speed,
                 float shapeWidth, float shapeLength) {
        this.id = id;
        this.displayName = displayName;
        this.role = role;
        this.cargoMultiplier = cargo;
        this.fuelMultiplier = fuel;
        this.hullMultiplier = hull;
        this.thrustMultiplier = thrust;
        this.speedMultiplier = speed;
        this.shapeWidth = shapeWidth;
        this.shapeLength = shapeLength;
    }

    static ShipClassDef byId(String id) {
        if (id != null) {
            for (ShipClassDef def : values()) {
                if (def.id.equalsIgnoreCase(id)
                        || def.name().equalsIgnoreCase(id)) {
                    return def;
                }
            }
        }
        return MINER;
    }

    String description() {
        switch (role) {
            case EXPLORATION:
                return "Rapida, mucho deposito y poca bodega";
            case HAULING:
                return "Gran bodega y casco, lenta";
            case MINING:
            default:
                return "Equilibrada para minar";
        }
    }
}

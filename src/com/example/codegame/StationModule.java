package com.example.codegame;

/**
 * Módulo instalado en una ranura de la estación personal. Guarda su definición
 * y nivel; los servicios se derivan de la definición. Las ampliaciones suben el
 * nivel y el coste, sin cambiar el tipo de servicio.
 */
final class StationModule {

    static final int MAX_LEVEL = 3;

    final ModuleDef def;
    private int level = 1;

    StationModule(ModuleDef def) {
        if (def == null) {
            throw new IllegalArgumentException("modulo nulo");
        }
        this.def = def;
    }

    int level() {
        return level;
    }

    boolean canUpgrade() {
        return level < MAX_LEVEL;
    }

    void upgrade() {
        if (canUpgrade()) {
            level++;
        }
    }

    /** Coste de la siguiente ampliación (0 si está al máximo). */
    int upgradeCredits() {
        return canUpgrade() ? def.creditCost * (level + 1) : 0;
    }

    /** Material extra de la siguiente ampliación, repartido del coste base. */
    int upgradeMaterial(Commodity commodity) {
        if (!canUpgrade() || commodity == null) {
            return 0;
        }
        int base = def.material(commodity);
        return base <= 0 ? 0 : Math.max(1, base / 2);
    }

    ModuleDef.Service service() {
        return def.service;
    }
}

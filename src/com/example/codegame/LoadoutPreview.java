package com.example.codegame;

/**
 * Previsualización de estadísticas de equipamiento (UPD-006-T25). Calcula el
 * valor actual y el resultante tras instalar o retirar un módulo sin mutar el
 * loadout real. Las fórmulas se centralizan aquí y no duplican la tienda.
 */
final class LoadoutPreview {

    final ShipModuleDef module;
    final boolean installing;
    final int slot;
    final String error;

    final float thrust;
    final float thrustAfter;
    final float maxSpeed;
    final float maxSpeedAfter;
    final float maxFuel;
    final float maxFuelAfter;
    final float hull;
    final float hullAfter;
    final float fuelUse;
    final float fuelUseAfter;
    final float cargo;
    final float cargoAfter;
    final int laserLevel;
    final int laserLevelAfter;
    final float tractorRange;
    final float tractorRangeAfter;
    final float sensorRange;
    final float sensorRangeAfter;
    final int cost;

    private LoadoutPreview(Upgrades up, ShipModuleDef module, boolean installing,
                           int slot, String error) {
        this.module = module;
        this.installing = installing;
        this.slot = slot;
        this.error = error == null ? "" : error;
        this.thrust = up.thrust();
        this.maxSpeed = up.maxSpeed();
        this.maxFuel = up.maxFuel();
        this.hull = up.hull();
        this.fuelUse = up.fuelUse();
        this.cargo = up.cargoCapacity();
        this.laserLevel = up.laserLevel();
        this.tractorRange = up.tractorRange();
        this.sensorRange = Sensors.range(up);
        this.cost = installing && module != null ? module.creditCost : 0;
        this.thrustAfter = thrust * factor(module, installing, ShipModuleDef.PROPULSION, 1.10f);
        this.maxSpeedAfter = maxSpeed * factor(module, installing, ShipModuleDef.PROPULSION, 1.08f);
        // Los módulos actuales no cambian depósito, casco, consumo ni bodega;
        // se mantienen los valores para dejar la ficha completa y comparable.
        this.maxFuelAfter = maxFuel;
        this.hullAfter = hull;
        this.fuelUseAfter = fuelUse;
        this.cargoAfter = cargo;
        int laserAfter = laserLevel
                + (module == ShipModuleDef.MINING ? (installing ? 1 : -1) : 0);
        this.laserLevelAfter = Math.max(0, laserAfter);
        this.tractorRangeAfter = tractorRange + (module == ShipModuleDef.TOOLS
                ? (installing ? Upgrades.TOOLS_TRACTOR_BONUS
                : -Upgrades.TOOLS_TRACTOR_BONUS) : 0f);
        this.sensorRangeAfter = sensorRange * (module == ShipModuleDef.RADAR
                ? (installing ? 1f + Sensors.RADAR_BONUS
                : 1f / (1f + Sensors.RADAR_BONUS)) : 1f);
    }

    static LoadoutPreview preview(Upgrades upgrades, ShipModuleDef module,
                                  boolean installing, int slot,
                                  ShipClassDef shipClass) {
        if (upgrades == null) {
            return null;
        }
        String error = "";
        if (module == null) {
            error = "ELIGE UN MODULO";
        } else if (installing) {
            ShipSlotDef def = ShipSlotDef.slotFor(shipClass, slot);
            if (def == null || !def.accepts(module)) {
                error = "RANURA NO COMPATIBLE";
            } else if (upgrades.hasShipModule(module)) {
                error = "YA INSTALADO";
            } else if (upgrades.loadout().module(slot) != null) {
                error = "RANURA OCUPADA";
            } else if (upgrades.coins() < module.creditCost) {
                error = "SIN CREDITOS";
            }
        }
        return new LoadoutPreview(upgrades, module, installing, slot, error);
    }

    private static float factor(ShipModuleDef module, boolean installing,
                                ShipModuleDef target, float value) {
        if (module != target) {
            return 1f;
        }
        return installing ? value : 1f / value;
    }

    boolean ok() {
        return error.isEmpty();
    }

    String effectLabel() {
        if (module == null) {
            return "";
        }
        switch (module) {
            case PROPULSION:
                return "EMPUJE Y VELOCIDAD +10%";
            case RADAR:
                return "ALCANCE DE SENSORES +70%";
            case MINING:
                return "LASER +1 NIVEL";
            case TOOLS:
                return "TRACTOR +" + Math.round(Upgrades.TOOLS_TRACTOR_BONUS) + " u";
            case NAVIGATION:
                return "BRUJULA AVANZADA";
            case DOCKING_AUTOPILOT:
                return "ATRAQUE AUTOMATICO";
            case AUTOPILOT:
            default:
                return "SELECCION AUTOMATICA DE BLANCOS";
        }
    }
}

package com.example.codegame;

/**
 * Ficha real de una nave del mercado (UPD-006-T36/T39): casco, componentes,
 * desgaste, depósito, eficiencia, masa, bodega, módulos y precio. Los valores
 * son visibles y coherentes; no hay estadísticas ocultas que contradigan la
 * ficha.
 */
final class ShipSpec {

    enum Category {
        SCRAP,
        USED,
        NEW
    }

    final String id;
    final String label;
    final Category category;
    final ShipClassDef shipClass;
    final float hull;
    /** Integridad de motor, depósito, cañón, timón, alas, soporte y láser. */
    final float[] parts = new float[Ship.PART_COUNT];
    final float fuelCapacity;
    /** Multiplicador de consumo: 1 = normal, >1 gasta más. */
    final float efficiency;
    /** Multiplicador de masa base: >1 acelera peor. */
    final float massFactor;
    final float cargoCapacity;
    /** Módulos incluidos en el precio (ids del catálogo). */
    final String[] modules;
    final int price;
    final String description;

    ShipSpec(String id, String label, Category category, ShipClassDef shipClass,
             float hull, float componentIntegrity, float fuelCapacity,
             float efficiency, float massFactor, float cargoCapacity,
             String[] modules, int price, String description) {
        this.id = id;
        this.label = label;
        this.category = category == null ? Category.USED : category;
        this.shipClass = shipClass == null ? ShipClassDef.MINER : shipClass;
        float maxHull = Math.max(20f, hull);
        this.hull = maxHull;
        this.parts[Ship.HULL] = maxHull;
        float integrity = Math.max(0f, Math.min(1f, componentIntegrity));
        for (int i = Ship.ENGINE; i < Ship.PART_COUNT; i++) {
            parts[i] = Math.max(0f, integrity * Ship.PART_MAX);
        }
        this.fuelCapacity = Math.max(40f, fuelCapacity);
        this.efficiency = Math.max(0.6f, efficiency);
        this.massFactor = Math.max(0.7f, massFactor);
        this.cargoCapacity = Math.max(40f, cargoCapacity);
        this.modules = modules == null ? new String[0] : modules;
        this.price = Math.max(0, price);
        this.description = description == null ? "" : description;
    }

    int moduleCount() {
        return modules.length;
    }

    boolean hasModule(ShipModuleDef def) {
        if (def == null) {
            return false;
        }
        for (int i = 0; i < modules.length; i++) {
            if (def.id.equals(modules[i])) {
                return true;
            }
        }
        return false;
    }

    /** Aplica la ficha a la nave real después de la compra. */
    void applyTo(Ship ship, Upgrades upgrades) {
        if (ship == null || upgrades == null) {
            return;
        }
        upgrades.setShipClass(shipClass);
        // Módulos incluidos: se instalan en una ranura compatible sin cobrar.
        for (int i = 0; i < modules.length; i++) {
            ShipModuleDef def = ShipModuleDef.byId(modules[i]);
            if (def == null || upgrades.hasShipModule(def)) {
                continue;
            }
            int slot = upgrades.loadout().freeCompatibleSlot(def, shipClass);
            if (slot >= 0) {
                upgrades.loadout().installModuleChecked(slot, def, shipClass);
            }
        }
        ship.reset(upgrades, ship.pos.x, ship.pos.y);
        ship.shipClass = shipClass;
        ship.maxHull = hull;
        for (int i = 0; i < Ship.PART_COUNT; i++) {
            ship.part[i] = parts[i];
        }
        ship.specFuelCapacity = fuelCapacity;
        ship.specFuelFactor = efficiency;
        ship.specMassFactor = massFactor;
        ship.maxFuel = fuelCapacity;
        ship.fuel = Math.min(ship.fuel, fuelCapacity);
        ship.cargo.setCapacity(cargoCapacity);
        ship.syncLaser(upgrades);
        ship.syncTractor(upgrades);
    }

    String categoryLabel() {
        switch (category) {
            case SCRAP:
                return "CHATARRA";
            case NEW:
                return "NUEVA";
            case USED:
            default:
                return "USADA";
        }
    }

    String conditionLabel() {
        float f = hull / Math.max(1f, shipClass.hullMultiplier * 225f);
        int pct = Math.round(Math.max(0f, Math.min(1f, f)) * 100f);
        return pct + "%";
    }
}

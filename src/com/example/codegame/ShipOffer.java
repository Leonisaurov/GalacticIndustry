package com.example.codegame;

/** Oferta del mercado de naves: ficha real y estado de inspección (UPD-006-T37). */
final class ShipOffer {

    static final int INSPECTION_COST = 15;

    final ShipSpec spec;
    boolean inspected;

    ShipOffer(ShipSpec spec) {
        this.spec = spec;
    }

    /** Realiza la inspección; cobra el coste y devuelve true si se completó. */
    boolean inspect(Upgrades upgrades) {
        if (inspected) {
            return true;
        }
        if (upgrades == null || upgrades.coins() < INSPECTION_COST) {
            return false;
        }
        upgrades.spendCoins(INSPECTION_COST);
        inspected = true;
        return true;
    }

    /** La información de la ficha es la misma antes y después de inspeccionar. */
    boolean dataIsConsistent() {
        return spec != null && spec.price >= 0f
                && spec.hull > 0f && spec.fuelCapacity > 0f
                && spec.cargoCapacity > 0f
                && spec.parts[Ship.HULL] == spec.hull;
    }
}

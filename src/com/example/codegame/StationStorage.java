package com.example.codegame;

/**
 * Almacén de la estación: recibe mineral de la nave y lo vende a precio base.
 * La capacidad se mide en masa (UM), igual que la bodega.
 */
final class StationStorage {

    final CargoHold hold;

    StationStorage(float capacity) {
        this.hold = new CargoHold(capacity);
    }

    float capacity() {
        return hold.capacity();
    }

    float usedMass() {
        return hold.usedMass();
    }

    float freeMass() {
        return hold.freeMass();
    }

    boolean isEmpty() {
        return hold.isEmpty();
    }

    int totalUnits() {
        return hold.totalUnits();
    }

    int amount(Commodity commodity) {
        return hold.amount(commodity);
    }

    float totalValue() {
        return hold.totalValue();
    }

    /**
     * Descarga la bodega de la nave en el almacén. Si no cabe todo, mueve lo
     * que quepa (por orden de mineral) y deja el resto en la nave.
     * Devuelve las unidades descargadas.
     */
    int depositAll(CargoHold cargo) {
        if (cargo == null || cargo.isEmpty()) {
            return 0;
        }
        int moved = 0;
        for (Cargo entry : cargo.entries()) {
            int fit = (int) Math.floor((hold.freeMass() + 0.0001f)
                    / entry.commodity.massPerUnit);
            int amount = Math.min(entry.amount, Math.max(0, fit));
            if (amount <= 0) {
                continue;
            }
            cargo.removeExact(entry.commodity, amount);
            hold.addExact(entry.commodity, amount);
            moved += amount;
        }
        return moved;
    }

    /** Vende todo el almacén a precio base y devuelve los créditos. */
    int sellAll() {
        int credits = 0;
        for (Cargo entry : hold.entries()) {
            credits += Math.round(entry.amount * entry.commodity.basePrice);
        }
        hold.clear();
        return credits;
    }
}

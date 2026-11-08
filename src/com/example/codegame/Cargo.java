package com.example.codegame;

/** Entrada inmutable de carga: un mineral y una cantidad. */
final class Cargo {

    final Commodity commodity;
    final int amount;

    Cargo(Commodity commodity, int amount) {
        if (commodity == null) {
            throw new IllegalArgumentException("commodity nulo");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("cantidad negativa");
        }
        this.commodity = commodity;
        this.amount = amount;
    }

    /** Masa total de la entrada, en UM. */
    float mass() {
        return amount * commodity.massPerUnit;
    }

    /** Valor base de la entrada, en créditos. */
    float value() {
        return amount * commodity.basePrice;
    }
}

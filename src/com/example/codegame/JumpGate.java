package com.example.codegame;

/**
 * Puerta de salto opcional: nunca es requisito para viajar. Si conecta origen y
 * destino, reduce el coste del warp; sin puerta el viaje sigue siendo posible.
 */
final class JumpGate {

    final String fromSectorId;
    final String toSectorId;
    final String name;
    /** Multiplicador de coste (0,5 = mitad de combustible/energía). */
    final float costFactor;

    JumpGate(String name, String fromSectorId, String toSectorId, float costFactor) {
        this.name = name == null ? "PUERTA" : name;
        this.fromSectorId = fromSectorId;
        this.toSectorId = toSectorId;
        this.costFactor = Math.max(0.1f, Math.min(1f, costFactor));
    }

    boolean connects(String from, String to) {
        return from != null && to != null
                && ((from.equals(fromSectorId) && to.equals(toSectorId))
                || (from.equals(toSectorId) && to.equals(fromSectorId)));
    }
}

package com.example.codegame;

/** Punto interactuable dentro de una habitación (servicio o salida). */
final class Interactable {

    enum Action {
        STORAGE,
        REPAIR,
        REFINERY,
        MARKET,
        EXIT,
        BUY_SHIP,
        EQUIP,
        FUEL,
        CONTRACTS,
        BOARD_SHIP
    }

    final String id;
    final String roomId;
    final String label;
    final float x;
    final float y;
    final float radius;
    final Action action;

    Interactable(String id, String roomId, String label, float x, float y,
                 float radius, Action action) {
        this.id = id;
        this.roomId = roomId;
        this.label = label;
        this.x = x;
        this.y = y;
        this.radius = Math.max(8f, radius);
        this.action = action;
    }

    boolean inRange(float px, float py) {
        float dx = px - x;
        float dy = py - y;
        return dx * dx + dy * dy <= radius * radius;
    }
}

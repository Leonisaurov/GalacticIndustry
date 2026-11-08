package com.example.codegame;

/** Puerta que conecta dos habitaciones por sus IDs. */
final class Door {

    final String id;
    final String fromRoomId;
    final String toRoomId;
    /** Posición del centro de la puerta en el interior. */
    final float x;
    final float y;

    Door(String id, String fromRoomId, String toRoomId, float x, float y) {
        this.id = id;
        this.fromRoomId = fromRoomId;
        this.toRoomId = toRoomId;
        this.x = x;
        this.y = y;
    }

    boolean connects(String a, String b) {
        return a != null && b != null
                && ((a.equals(fromRoomId) && b.equals(toRoomId))
                || (a.equals(toRoomId) && b.equals(fromRoomId)));
    }
}

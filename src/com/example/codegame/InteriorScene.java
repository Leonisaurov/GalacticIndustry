package com.example.codegame;

import java.util.ArrayList;
import java.util.List;

/**
 * Marco genérico de interiores: salas, puertas e interactuables. Se aplica a la
 * estación personal y a naves. El interior de la estación se deriva de sus
 * módulos construidos, de modo que cada módulo tiene su habitación.
 */
final class InteriorScene {

    enum Kind {
        PLAYER_STATION,
        SHIP
    }

    static final float HUB_W = 300f;
    static final float HUB_H = 220f;
    static final float ROOM_W = 220f;
    static final float ROOM_H = 170f;

    final String id;
    final Kind kind;
    final List<Room> rooms = new ArrayList<>();
    final List<Door> doors = new ArrayList<>();
    final List<Interactable> interactables = new ArrayList<>();

    InteriorScene(String id, Kind kind) {
        this.id = id == null ? "interior" : id;
        this.kind = kind == null ? Kind.PLAYER_STATION : kind;
    }

    Room addRoom(Room room) {
        if (room != null && room(room.id) == null) {
            rooms.add(room);
        }
        return room;
    }

    Door addDoor(Door door) {
        if (door != null && room(door.fromRoomId) != null
                && room(door.toRoomId) != null) {
            doors.add(door);
        }
        return door;
    }

    Interactable addInteractable(Interactable interactable) {
        if (interactable != null && room(interactable.roomId) != null) {
            interactables.add(interactable);
        }
        return interactable;
    }

    Room room(String roomId) {
        if (roomId == null) {
            return null;
        }
        for (int i = 0; i < rooms.size(); i++) {
            if (roomId.equals(rooms.get(i).id)) {
                return rooms.get(i);
            }
        }
        return null;
    }

    int roomCount() {
        return rooms.size();
    }

    int doorCount() {
        return doors.size();
    }

    int interactableCount() {
        return interactables.size();
    }

    Interactable nearest(float x, float y) {
        Interactable best = null;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < interactables.size(); i++) {
            Interactable item = interactables.get(i);
            if (!item.inRange(x, y)) {
                continue;
            }
            float dx = item.x - x;
            float dy = item.y - y;
            float dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = item;
            }
        }
        return best;
    }

    /** Valida IDs únicos, puertas e interactuables dentro de salas. */
    boolean valid() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (ids.contains(room.id)) {
                return false;
            }
            ids.add(room.id);
        }
        for (int i = 0; i < doors.size(); i++) {
            Door door = doors.get(i);
            if (room(door.fromRoomId) == null || room(door.toRoomId) == null) {
                return false;
            }
        }
        for (int i = 0; i < interactables.size(); i++) {
            Interactable item = interactables.get(i);
            Room room = room(item.roomId);
            if (room == null || !room.contains(item.x, item.y)) {
                return false;
            }
        }
        return true;
    }

    /** Interior de la estación personal derivado de sus módulos. */
    static InteriorScene fromPlayerStation(PlayerStation station) {
        InteriorScene scene = new InteriorScene(
                station == null ? PlayerStation.ID : station.id,
                Kind.PLAYER_STATION);
        Room hub = new Room("hub", "NUCLEO", 0f, 0f, HUB_W, HUB_H,
                -1, null);
        scene.addRoom(hub);
        scene.addInteractable(new Interactable("exit", "hub", "SALIR A LA NAVE",
                0f, -HUB_H * 0.5f + 24f, 34f, Interactable.Action.EXIT));
        if (station == null) {
            return scene;
        }
        for (int i = 0; i < station.slotCount(); i++) {
            StationModule module = station.moduleAt(i);
            if (module == null) {
                continue;
            }
            int side = i % 4;
            float x = side == 0 ? 0f : side == 1 ? 300f
                    : side == 2 ? 0f : -300f;
            float y = side == 0 ? 300f : side == 1 ? 0f
                    : side == 2 ? -300f : 0f;
            String roomId = "module-" + i;
            Room room = new Room(roomId, module.def.displayName,
                    x, y, ROOM_W, ROOM_H, i, module.def);
            scene.addRoom(room);
            float dx = side == 0 ? 0f : side == 1 ? 190f
                    : side == 2 ? 0f : -190f;
            float dy = side == 0 ? 190f : side == 1 ? 0f
                    : side == 2 ? -190f : 0f;
            scene.addDoor(new Door("door-" + i, "hub", roomId, dx, dy));
            scene.addInteractable(new Interactable("service-" + i, roomId,
                    module.def.displayName, x, y,
                    46f, actionOf(module.def)));
        }
        return scene;
    }

    /** Interior mínimo de una nave: cabina y salida. */
    static InteriorScene ship(String shipId) {
        InteriorScene scene = new InteriorScene(shipId, Kind.SHIP);
        Room cockpit = new Room("cockpit", "CABINA", 0f, 0f, 260f, 200f,
                -1, null);
        scene.addRoom(cockpit);
        scene.addInteractable(new Interactable("exit", "cockpit", "SALIR",
                0f, -90f, 34f, Interactable.Action.EXIT));
        return scene;
    }

    private static Interactable.Action actionOf(ModuleDef def) {
        switch (def.service) {
            case STORAGE:
                return Interactable.Action.STORAGE;
            case REPAIR:
                return Interactable.Action.REPAIR;
            case REFINERY:
                return Interactable.Action.REFINERY;
            case MARKET:
            default:
                return Interactable.Action.MARKET;
        }
    }
}

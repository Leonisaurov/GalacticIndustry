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
        SHIP,
        MOTHERSHIP
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
    final List<NpcActor> npcs = new ArrayList<>();

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

    /** Sala que contiene un punto, o null. */
    Room roomAt(float x, float y) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).contains(x, y)) {
                return rooms.get(i);
            }
        }
        return null;
    }

    NpcActor addNpc(NpcActor npc) {
        if (npc != null) {
            npcs.add(npc);
        }
        return npc;
    }

    int npcCount() {
        return npcs.size();
    }

    void updateNpcs(float dt) {
        for (int i = 0; i < npcs.size(); i++) {
            npcs.get(i).update(dt, this);
        }
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

    /**
     * Interior amplio de la nave nodriza (UPD-006-T22): hangar, concesionario,
     * taller, comercios, instalaciones técnicas, pasillos y zona habitable.
     * Se construye con los mismos {@link Room}, {@link Door} e
     * {@link Interactable} que la estación personal.
     */
    static InteriorScene mothership() {
        InteriorScene scene = new InteriorScene("mothership", Kind.MOTHERSHIP);

        Room hub = new Room("hub", "PASILLO CENTRAL", 0f, 0f, 480f, 420f,
                -1, null);
        Room hangar = new Room("hangar", "HANGAR", 0f, 460f, 520f, 300f,
                -1, null);
        Room dealer = new Room("dealer", "CONCESIONARIO", 480f, 180f,
                280f, 300f, -1, null);
        Room workshop = new Room("workshop", "TALLER", -480f, 180f,
                280f, 300f, -1, null);
        Room commerce = new Room("commerce", "COMERCIOS", 480f, -200f,
                280f, 280f, -1, null);
        Room technical = new Room("technical", "SISTEMAS", -480f, -200f,
                280f, 280f, -1, null);
        Room living = new Room("living", "ZONA HABITABLE", 0f, -460f,
                520f, 280f, -1, null);
        scene.addRoom(hub);
        scene.addRoom(hangar);
        scene.addRoom(dealer);
        scene.addRoom(workshop);
        scene.addRoom(commerce);
        scene.addRoom(technical);
        scene.addRoom(living);

        scene.addDoor(new Door("door-hangar", "hub", "hangar", 0f, 300f));
        scene.addDoor(new Door("door-dealer", "hub", "dealer", 300f, 180f));
        scene.addDoor(new Door("door-workshop", "hub", "workshop", -300f, 180f));
        scene.addDoor(new Door("door-commerce", "hub", "commerce", 300f, -200f));
        scene.addDoor(new Door("door-technical", "hub", "technical", -300f, -200f));
        scene.addDoor(new Door("door-living", "hub", "living", 0f, -300f));

        scene.addInteractable(new Interactable("hangar-exit", "hangar",
                "SALIR A LA NAVE", -160f, 430f, 40f, Interactable.Action.EXIT));
        scene.addInteractable(new Interactable("hangar-board", "hangar",
                "ABORDAR NAVE", 150f, 470f, 44f, Interactable.Action.BOARD_SHIP));
        scene.addInteractable(new Interactable("dealer-buy", "dealer",
                "COMPRAR NAVE", 480f, 190f, 50f, Interactable.Action.BUY_SHIP));
        scene.addInteractable(new Interactable("workshop-equip", "workshop",
                "EQUIPAMIENTO", -480f, 190f, 50f, Interactable.Action.EQUIP));
        scene.addInteractable(new Interactable("commerce-fuel", "commerce",
                "COMBUSTIBLE", 408f, -190f, 40f, Interactable.Action.FUEL));
        scene.addInteractable(new Interactable("commerce-market", "commerce",
                "MERCADO", 548f, -190f, 40f, Interactable.Action.MARKET));
        scene.addInteractable(new Interactable("technical-repair", "technical",
                "REPARAR", -480f, -190f, 44f, Interactable.Action.REPAIR));
        scene.addInteractable(new Interactable("hub-contracts", "hub",
                "CONTRATOS", 150f, -160f, 42f, Interactable.Action.CONTRACTS));
        scene.addInteractable(new Interactable("living-rest", "living",
                "ZONA DE DESCANSO", 0f, -460f, 46f, Interactable.Action.MARKET));

        // Población reducida: 8 NPC con rutinas cortas entre salas.
        addNpc(scene, "hub", -60f, 40f,
                new float[]{-60f, 40f, 0f, 220f, 0f, 380f, 0f, 220f});
        addNpc(scene, "dealer", 430f, 140f,
                new float[]{430f, 140f, 480f, 180f, 300f, 180f, 240f, 160f});
        addNpc(scene, "commerce", 520f, -230f,
                new float[]{520f, -230f, 480f, -200f, 300f, -200f, 240f, -150f});
        addNpc(scene, "workshop", -430f, 140f,
                new float[]{-430f, 140f, -480f, 180f, -300f, 180f, -240f, 120f});
        addNpc(scene, "living", 120f, -430f,
                new float[]{120f, -430f, 0f, -380f, 0f, -300f, 80f, -220f});
        addNpc(scene, "technical", -500f, -240f,
                new float[]{-500f, -240f, -480f, -200f, -300f, -200f, -240f, -160f});
        addNpc(scene, "hangar", -60f, 380f,
                new float[]{-60f, 380f, 0f, 300f, 0f, 220f, 80f, 180f});
        addNpc(scene, "hub", 90f, -60f,
                new float[]{90f, -60f, 150f, -160f, 0f, -300f, 0f, -380f});
        return scene;
    }

    private static void addNpc(InteriorScene scene, String roomId, float x,
                               float y, float[] waypoints) {
        scene.addNpc(new NpcActor(roomId, x, y, waypoints,
                npcColor(scene.npcCount()), "NPC"));
    }

    private static com.badlogic.gdx.graphics.Color npcColor(int index) {
        switch (index % 4) {
            case 0:
                return new com.badlogic.gdx.graphics.Color(0.82f, 0.55f, 0.32f, 1f);
            case 1:
                return new com.badlogic.gdx.graphics.Color(0.45f, 0.70f, 0.55f, 1f);
            case 2:
                return new com.badlogic.gdx.graphics.Color(0.60f, 0.50f, 0.78f, 1f);
            default:
                return new com.badlogic.gdx.graphics.Color(0.72f, 0.68f, 0.42f, 1f);
        }
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

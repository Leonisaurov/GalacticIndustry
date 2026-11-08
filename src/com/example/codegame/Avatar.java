package com.example.codegame;

/**
 * Personaje a pie dentro de un interior. Se mueve sin gravedad ni combate;
 * las paredes bloquean y solo se cambia de sala a través de una puerta.
 */
final class Avatar {

    static final float RADIUS = 12f;
    static final float SPEED = 170f;
    static final float DOOR_REACH = 72f;

    float x;
    float y;
    float facingX;
    float facingY = 1f;
    String roomId;

    Avatar(InteriorScene scene) {
        if (scene == null || scene.rooms.isEmpty()) {
            roomId = "hub";
            return;
        }
        Room room = scene.room("hub");
        if (room == null) {
            room = scene.rooms.get(0);
        }
        roomId = room.id;
        x = room.x;
        y = room.y;
    }

    void update(float dt, InteriorInput input, InteriorScene scene) {
        if (scene == null || input == null || dt <= 0f) {
            return;
        }
        float dx = input.moveX;
        float dy = input.moveY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) {
            return;
        }
        if (len > 1f) {
            dx /= len;
            dy /= len;
        }
        facingX = dx;
        facingY = dy;
        float step = SPEED * dt;
        if (!tryMove(dx * step, dy * step, scene)) {
            tryMove(dx * step, 0f, scene);
            tryMove(0f, dy * step, scene);
        }
    }

    private boolean tryMove(float dx, float dy, InteriorScene scene) {
        Room current = scene.room(roomId);
        if (current == null) {
            return false;
        }
        float nx = x + dx;
        float ny = y + dy;
        if (current.contains(nx, ny, RADIUS)) {
            x = nx;
            y = ny;
            return true;
        }
        for (int i = 0; i < scene.doors.size(); i++) {
            Door door = scene.doors.get(i);
            if (!door.fromRoomId.equals(roomId)
                    && !door.toRoomId.equals(roomId)) {
                continue;
            }
            Room target = scene.room(door.fromRoomId.equals(roomId)
                    ? door.toRoomId : door.fromRoomId);
            if (target == null) {
                continue;
            }
            float near = distance(door.x, door.y);
            if (near > DOOR_REACH
                    && distanceTo(door.x, door.y, nx, ny) > DOOR_REACH) {
                continue;
            }
            if (target.contains(nx, ny, RADIUS)) {
                roomId = target.id;
                x = nx;
                y = ny;
                return true;
            }
            float tx = clamp(door.x, target.x - target.width * 0.5f + RADIUS,
                    target.x + target.width * 0.5f - RADIUS);
            float ty = clamp(door.y, target.y - target.height * 0.5f + RADIUS,
                    target.y + target.height * 0.5f - RADIUS);
            if (target.contains(tx, ty, RADIUS)) {
                roomId = target.id;
                x = tx;
                y = ty;
                return true;
            }
        }
        float cx = clamp(nx, current.x - current.width * 0.5f + RADIUS,
                current.x + current.width * 0.5f - RADIUS);
        float cy = clamp(ny, current.y - current.height * 0.5f + RADIUS,
                current.y + current.height * 0.5f - RADIUS);
        if (Math.abs(cx - x) > 0.001f || Math.abs(cy - y) > 0.001f) {
            x = cx;
            y = cy;
            return true;
        }
        return false;
    }

    boolean atExit(InteriorScene scene) {
        Interactable item = nearest(scene);
        return item != null && item.action == Interactable.Action.EXIT;
    }

    Interactable nearest(InteriorScene scene) {
        return scene == null ? null : scene.nearest(x, y);
    }

    Room room(InteriorScene scene) {
        return scene == null ? null : scene.room(roomId);
    }

    private float distance(float px, float py) {
        return distanceTo(px, py, x, y);
    }

    private static float distanceTo(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : value > max ? max : value;
    }
}

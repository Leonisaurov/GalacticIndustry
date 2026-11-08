package com.example.codegame;

/**
 * Personaje a pie dentro de un interior. Se mueve sin gravedad ni combate;
 * las paredes bloquean y solo se cambia de sala a través de una puerta.
 *
 * <p>UPD-006-T20 añade estado visual: fase de caminata ligada a la distancia
 * recorrida, reposo y orientación continua. La colisión no cambia.</p>
 */
final class Avatar {

    static final float RADIUS = 12f;
    static final float SPEED = 170f;
    static final float DOOR_REACH = 120f;
    /** Longitud de zancada para el ciclo de caminata. */
    static final float STRIDE = 34f;

    float x;
    float y;
    float facingX;
    float facingY = 1f;
    String roomId;

    /** Dirección de mirada en grados (0 = derecha). */
    float facingAngle = 90f;
    /** Fase de caminata en radianes. */
    float walkPhase;
    /** Tiempo detenido, para la respiración del reposo. */
    float idleTime;
    /** Distancia recorrida desde el último cambio de sala. */
    float travelled;
    boolean moving;

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
            moving = false;
            idleTime += dt;
            walkPhase *= 0.90f;
            if (walkPhase < 0.01f) {
                walkPhase = 0f;
            }
            return;
        }
        if (len > 1f) {
            dx /= len;
            dy /= len;
        }
        facingX = dx;
        facingY = dy;
        facingAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
        float step = SPEED * dt;
        float beforeX = x;
        float beforeY = y;
        boolean moved = false;
        if (!tryMove(dx * step, dy * step, scene)) {
            moved = tryMove(dx * step, 0f, scene);
            moved = tryMove(0f, dy * step, scene) || moved;
        } else {
            moved = true;
        }
        float dist = (float) Math.sqrt((x - beforeX) * (x - beforeX)
                + (y - beforeY) * (y - beforeY));
        moving = moved && dist > 0.01f;
        if (moving) {
            travelled += dist;
            walkPhase += dist / STRIDE * (float) Math.PI * 2f;
            if (walkPhase > (float) Math.PI * 2f) {
                walkPhase -= (float) Math.PI * 2f;
            }
            idleTime = 0f;
        } else {
            idleTime += dt;
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

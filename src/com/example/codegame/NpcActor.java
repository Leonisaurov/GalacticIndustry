package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

/**
 * NPC básico de interior (UPD-006-T41): recorre una lista corta de puntos
 * (sala, puerta, pasillo, otra sala) a baja frecuencia, sin colisión compleja y
 * sin bloquear al jugador. No crea objetos por frame.
 */
final class NpcActor {

    static final float SPEED = 42f;
    static final float WAYPOINT_RADIUS = 14f;

    float x;
    float y;
    String roomId;
    float facingAngle = 90f;
    float walkPhase;
    boolean moving;
    Color color = new Color(0.75f, 0.55f, 0.35f, 1f);
    String name = "NPC";

    private final float[] waypoints;
    private int current;
    private float pause;

    NpcActor(String roomId, float startX, float startY, float[] waypoints,
             Color color, String name) {
        this.roomId = roomId;
        this.x = startX;
        this.y = startY;
        this.waypoints = waypoints == null ? new float[0] : waypoints;
        if (color != null) {
            this.color = color;
        }
        if (name != null) {
            this.name = name;
        }
    }

    /**
     * Avanza hacia el siguiente punto. {@code dt} se puede acumular a baja
     * frecuencia desde MyGame; no se hacen búsquedas ni asignaciones.
     */
    void update(float dt, InteriorScene scene) {
        if (waypoints.length < 2 || dt <= 0f) {
            return;
        }
        if (pause > 0f) {
            pause -= dt;
            moving = false;
            return;
        }
        float tx = waypoints[current * 2];
        float ty = waypoints[current * 2 + 1];
        float dx = tx - x;
        float dy = ty - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < WAYPOINT_RADIUS) {
            current = (current + 1) % (waypoints.length / 2);
            pause = 0.4f + MathUtils.random() * 1.6f;
            moving = false;
            return;
        }
        float step = SPEED * dt;
        if (step > dist) {
            step = dist;
        }
        x += dx / dist * step;
        y += dy / dist * step;
        facingAngle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        walkPhase += step / 26f * (float) Math.PI * 2f;
        if (walkPhase > (float) Math.PI * 2f) {
            walkPhase -= (float) Math.PI * 2f;
        }
        moving = true;
        if (scene != null) {
            Room room = scene.roomAt(x, y);
            if (room != null) {
                roomId = room.id;
            }
        }
    }
}

package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Dibuja un interior cenital con formas y texto. No crea objetos por frame; las
 * etiquetas usan los nombres ya existentes en las salas e interactuables.
 *
 * <p>UPD-006: sprite cenital procedural del personaje (T19), animación de
 * caminata y reposo (T20), puertas animadas y resaltado del interactuable más
 * cercano (T21), y NPC básicos (T41).</p>
 */
final class InteriorRenderer {

    private static final Color FLOOR = new Color(0.055f, 0.085f, 0.145f, 1f);
    private static final Color FLOOR_HUB = new Color(0.065f, 0.105f, 0.180f, 1f);
    private static final Color WALL = new Color(0.28f, 0.52f, 0.82f, 0.85f);
    private static final Color WALL_INNER = new Color(0.10f, 0.20f, 0.34f, 1f);
    private static final Color DOOR_C = new Color(0.35f, 0.95f, 0.85f, 0.9f);
    private static final Color AVATAR_C = new Color(0.45f, 0.95f, 1f, 1f);
    private static final Color TEXT_C = new Color(0.82f, 0.92f, 1f, 1f);
    private static final Color TEXT_DIM = new Color(0.55f, 0.70f, 0.85f, 1f);
    private static final Color SUIT_DARK = new Color(0.13f, 0.30f, 0.40f, 1f);
    private static final Color SUIT_MID = new Color(0.28f, 0.62f, 0.72f, 1f);
    private static final Color SUIT_LIGHT = new Color(0.55f, 0.90f, 1f, 1f);
    private static final Color AMBER = new Color(1f, 0.72f, 0.30f, 1f);
    private static final Color NPC_BODY = new Color(0.55f, 0.62f, 0.72f, 1f);
    private static final Color HIGHLIGHT = new Color(1f, 0.82f, 0.35f, 1f);

    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;

    InteriorRenderer(ShapeRenderer shapes, SpriteBatch batch, BitmapFont font) {
        this.shapes = shapes;
        this.batch = batch;
        this.font = font;
    }

    void draw(InteriorScene scene, Avatar avatar, float time) {
        if (scene == null) {
            return;
        }
        for (int i = 0; i < scene.rooms.size(); i++) {
            Room room = scene.rooms.get(i);
            boolean hub = "hub".equals(room.id);
            shapes.setColor(hub ? FLOOR_HUB : FLOOR);
            shapes.rect(room.x - room.width * 0.5f, room.y - room.height * 0.5f,
                    room.width, room.height);
            shapes.setColor(WALL);
            float left = room.x - room.width * 0.5f;
            float bottom = room.y - room.height * 0.5f;
            shapes.rect(left, bottom, room.width, 5f);
            shapes.rect(left, bottom + room.height - 5f, room.width, 5f);
            shapes.rect(left, bottom, 5f, room.height);
            shapes.rect(left + room.width - 5f, bottom, 5f, room.height);
            shapes.setColor(WALL_INNER);
            shapes.rect(left + 8f, bottom + 8f, room.width - 16f, 2f);
            // Marcas de suelo para dar escala.
            shapes.setColor(0.10f, 0.16f, 0.24f, 0.35f);
            for (float gx = left + 36f; gx < left + room.width - 20f; gx += 44f) {
                shapes.rect(gx, bottom + 12f, 1.2f, room.height - 24f);
            }
        }
        drawDoors(scene, avatar, time);
        Interactable near = avatar == null ? null
                : scene.nearest(avatar.x, avatar.y);
        for (int i = 0; i < scene.interactables.size(); i++) {
            Interactable item = scene.interactables.get(i);
            Color color = item.action == Interactable.Action.EXIT
                    ? DOOR_C : Visuals.GOLD;
            float pulse = 0.75f + 0.25f * (float) Math.sin(time * 3f + i);
            shapes.setColor(color.r, color.g, color.b, 0.16f * pulse);
            shapes.circle(item.x, item.y, item.radius, 22);
            shapes.setColor(color);
            shapes.circle(item.x, item.y, 9f, 16);
            shapes.setColor(0.03f, 0.05f, 0.08f, 0.85f);
            shapes.circle(item.x, item.y, 4f, 10);
            if (item == near) {
                shapes.setColor(HIGHLIGHT.r, HIGHLIGHT.g, HIGHLIGHT.b, 0.90f);
                shapes.circle(item.x, item.y, item.radius + 5f, 24);
                shapes.setColor(FLOOR);
                shapes.circle(item.x, item.y, item.radius + 1.5f, 22);
            }
        }
        drawNpcs(scene, time);
        if (avatar != null) {
            drawAvatar(avatar, time);
        }
    }

    /** Puertas que se abren al acercarse, sin bloquear el movimiento. */
    private void drawDoors(InteriorScene scene, Avatar avatar, float time) {
        for (int i = 0; i < scene.doors.size(); i++) {
            Door door = scene.doors.get(i);
            float open = 0f;
            if (avatar != null) {
                float d = avatar.x == door.x && avatar.y == door.y ? 0f
                        : (float) Math.sqrt((avatar.x - door.x) * (avatar.x - door.x)
                        + (avatar.y - door.y) * (avatar.y - door.y));
                open = MathUtils.clamp(1f - d / 140f, 0f, 1f);
            }
            float pulse = 0.65f + 0.35f * MathUtils.sin(time * 2.4f + i);
            shapes.setColor(DOOR_C.r, DOOR_C.g, DOOR_C.b, 0.20f + 0.25f * open);
            shapes.circle(door.x, door.y, 22f, 18);
            float spread = 5f + open * 9f;
            shapes.setColor(DOOR_C);
            shapes.rect(door.x - spread - 3f, door.y - 12f, 6f, 24f);
            shapes.rect(door.x + spread - 3f, door.y - 12f, 6f, 24f);
            shapes.setColor(1f, 1f, 1f, 0.25f + 0.35f * pulse);
            shapes.circle(door.x, door.y, 2.4f, 8);
        }
    }

    /** Sprite cenital procedural: cabeza, visera, torso, mochila, brazos y piernas. */
    private void drawAvatar(Avatar avatar, float time) {
        float c = MathUtils.cosDeg(avatar.facingAngle);
        float s = MathUtils.sinDeg(avatar.facingAngle);
        float breathe = avatar.moving ? 0f
                : MathUtils.sin(avatar.idleTime * 2.2f) * 0.8f;
        float swing = avatar.moving ? MathUtils.sin(avatar.walkPhase) : 0f;
        float lift = avatar.moving ? MathUtils.cos(avatar.walkPhase) * 2.2f : 0f;

        // Sombra elíptica.
        shapes.setColor(0f, 0f, 0f, 0.35f);
        shapes.ellipse(avatar.x - 12f, avatar.y - 10f, 24f, 14f, 20);

        // Piernas (dos cápsulas que alternan).
        float legLen = 9f;
        float px = -s * 4.2f;
        float py = c * 4.2f;
        shapes.setColor(SUIT_DARK);
        shapes.rectLine(avatar.x - c * 2f + px, avatar.y - s * 2f + py,
                avatar.x - c * (2f + legLen) + px + c * swing * 2f,
                avatar.y - s * (2f + legLen) + py + s * swing * 2f, 4.6f);
        shapes.rectLine(avatar.x - c * 2f - px, avatar.y - s * 2f - py,
                avatar.x - c * (2f + legLen) - px - c * swing * 2f,
                avatar.y - s * (2f + legLen) - py - s * swing * 2f, 4.6f);

        // Mochila.
        shapes.setColor(SUIT_DARK);
        shapes.circle(avatar.x - c * 6f, avatar.y - s * 6f, 6.4f + breathe * 0.3f, 14);
        shapes.setColor(AMBER.r, AMBER.g, AMBER.b, 0.85f);
        shapes.circle(avatar.x - c * 8f, avatar.y - s * 8f, 1.8f, 8);

        // Torso.
        shapes.setColor(SUIT_MID);
        shapes.circle(avatar.x, avatar.y, 7.2f + breathe * 0.4f, 16);
        shapes.setColor(SUIT_LIGHT);
        shapes.rectLine(avatar.x + c * 4f, avatar.y + s * 4f,
                avatar.x + c * 1f, avatar.y + s * 1f, 2.4f);

        // Brazos con balanceo opuesto a las piernas.
        float ax = -s * 7f;
        float ay = c * 7f;
        shapes.setColor(SUIT_DARK);
        shapes.rectLine(avatar.x + ax, avatar.y + ay,
                avatar.x + ax + c * (6f + swing * 2f) + lift,
                avatar.y + ay + s * (6f + swing * 2f) + lift, 3.4f);
        shapes.rectLine(avatar.x - ax, avatar.y - ay,
                avatar.x - ax + c * (6f - swing * 2f) - lift,
                avatar.y - ay + s * (6f - swing * 2f) - lift, 3.4f);

        // Cabeza y visera orientada.
        float hx = avatar.x + c * 4.5f;
        float hy = avatar.y + s * 4.5f;
        shapes.setColor(0.82f, 0.92f, 1f, 1f);
        shapes.circle(hx, hy, 5.2f, 16);
        shapes.setColor(0.10f, 0.55f, 0.75f, 1f);
        shapes.circle(hx + c * 2.2f, hy + s * 2.2f, 3.2f, 12);
        shapes.setColor(0.75f, 1f, 1f, 0.9f);
        shapes.circle(hx + c * 3.2f, hy + s * 3.2f, 1.2f, 8);
    }

    /** NPC reducidos: cuerpo distinto y paso suave. */
    private void drawNpcs(InteriorScene scene, float time) {
        for (int i = 0; i < scene.npcs.size(); i++) {
            NpcActor npc = scene.npcs.get(i);
            float c = MathUtils.cosDeg(npc.facingAngle);
            float s = MathUtils.sinDeg(npc.facingAngle);
            float bob = npc.moving ? MathUtils.sin(npc.walkPhase) * 1.4f : 0f;
            shapes.setColor(0f, 0f, 0f, 0.28f);
            shapes.ellipse(npc.x - 9f, npc.y - 7f, 18f, 10f, 16);
            shapes.setColor(NPC_BODY);
            shapes.circle(npc.x, npc.y, 6.2f, 14);
            shapes.setColor(npc.color);
            shapes.circle(npc.x, npc.y, 4.4f, 12);
            shapes.setColor(0.92f, 0.88f, 0.80f, 1f);
            shapes.circle(npc.x + c * 3.6f, npc.y + s * 3.6f, 3.4f + bob * 0.2f, 10);
            shapes.setColor(0.18f, 0.20f, 0.24f, 1f);
            shapes.circle(npc.x + c * 4.6f, npc.y + s * 4.6f, 1.2f, 8);
        }
    }

    /** Etiquetas de salas, interactuables y personajes; requiere batch normal. */
    void drawLabels(InteriorScene scene, Avatar avatar) {
        font.getData().setScale(0.55f);
        Interactable near = avatar == null ? null
                : scene.nearest(avatar.x, avatar.y);
        for (int i = 0; i < scene.rooms.size(); i++) {
            Room room = scene.rooms.get(i);
            font.setColor(TEXT_DIM);
            font.draw(batch, room.name, room.x - room.width * 0.5f + 14f,
                    room.y + room.height * 0.5f - 16f);
        }
        for (int i = 0; i < scene.interactables.size(); i++) {
            Interactable item = scene.interactables.get(i);
            font.setColor(item == near ? HIGHLIGHT
                    : item.action == Interactable.Action.EXIT ? DOOR_C : TEXT_C);
            font.draw(batch, item.label, item.x - item.label.length() * 2.6f,
                    item.y - item.radius - 8f);
        }
        if (avatar != null) {
            font.setColor(TEXT_C);
            font.draw(batch, "TU", avatar.x - 7f, avatar.y + Avatar.RADIUS + 18f);
        }
        font.getData().setScale(1f);
    }
}

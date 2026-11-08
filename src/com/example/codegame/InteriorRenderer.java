package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Dibuja un interior cenital con formas y texto. No crea objetos por frame; las
 * etiquetas usan los nombres ya existentes en las salas e interactuables.
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
        }
        for (int i = 0; i < scene.doors.size(); i++) {
            Door door = scene.doors.get(i);
            shapes.setColor(DOOR_C);
            shapes.circle(door.x, door.y, 18f, 16);
            shapes.setColor(FLOOR);
            shapes.circle(door.x, door.y, 10f, 14);
        }
        for (int i = 0; i < scene.interactables.size(); i++) {
            Interactable item = scene.interactables.get(i);
            Color color = item.action == Interactable.Action.EXIT
                    ? DOOR_C : Visuals.GOLD;
            float pulse = 0.75f + 0.25f * (float) Math.sin(time * 3f + i);
            shapes.setColor(color.r, color.g, color.b, 0.18f * pulse);
            shapes.circle(item.x, item.y, item.radius, 20);
            shapes.setColor(color);
            shapes.circle(item.x, item.y, 10f, 16);
        }
        if (avatar != null) {
            shapes.setColor(0f, 0f, 0f, 0.35f);
            shapes.circle(avatar.x + 3f, avatar.y - 4f, Avatar.RADIUS + 2f, 18);
            shapes.setColor(AVATAR_C);
            shapes.circle(avatar.x, avatar.y, Avatar.RADIUS, 18);
            shapes.setColor(1f, 1f, 1f, 0.85f);
            shapes.circle(avatar.x + avatar.facingX * 5f,
                    avatar.y + avatar.facingY * 5f, 3.5f, 8);
        }
    }

    /** Etiquetas de salas e interactuables; requiere batch en modo normal. */
    void drawLabels(InteriorScene scene, Avatar avatar) {
        font.getData().setScale(0.55f);
        for (int i = 0; i < scene.rooms.size(); i++) {
            Room room = scene.rooms.get(i);
            font.setColor(TEXT_DIM);
            font.draw(batch, room.name, room.x - room.width * 0.5f + 14f,
                    room.y + room.height * 0.5f - 16f);
        }
        for (int i = 0; i < scene.interactables.size(); i++) {
            Interactable item = scene.interactables.get(i);
            font.setColor(item.action == Interactable.Action.EXIT
                    ? DOOR_C : TEXT_C);
            font.draw(batch, item.label, item.x - item.label.length() * 2.6f,
                    item.y - item.radius - 8f);
        }
        if (avatar != null) {
            font.setColor(TEXT_C);
            font.draw(batch, "TU", avatar.x - 7f, avatar.y + Avatar.RADIUS + 18f);
        }
    }
}

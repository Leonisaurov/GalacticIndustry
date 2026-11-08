package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Estación nodriza instanciada en el sector inicial: posición, radio, zona
 * segura y almacén. Se dibuja a gran escala y gira lentamente.
 */
final class Station {

    static final float STORAGE_CAPACITY = 2000f;

    private static final Color HULL_DARK = new Color(0.09f, 0.11f, 0.18f, 1f);
    private static final Color HULL_MID = new Color(0.22f, 0.28f, 0.40f, 1f);
    private static final Color HULL_LIGHT = new Color(0.42f, 0.52f, 0.68f, 1f);
    private static final Color LIGHT = new Color(0.45f, 0.95f, 1f, 1f);
    private static final Color LIGHT_WARM = new Color(1f, 0.78f, 0.35f, 1f);

    final StationDef def;
    final Vector2 pos = new Vector2();
    final StationStorage storage;
    /** Mercado de la estación (stock y precios). */
    final Market market;
    private float rotation;

    Station(StationDef def, float x, float y) {
        this(def, x, y, null);
    }

    Station(StationDef def, float x, float y, Market market) {
        this.def = def;
        this.pos.set(x, y);
        this.storage = new StationStorage(STORAGE_CAPACITY);
        this.market = market != null ? market : GameData.newMarket();
    }

    void update(float dt) {
        rotation += dt * 4f;
        if (rotation >= 360f) {
            rotation -= 360f;
        }
    }

    float distanceTo(Vector2 point) {
        return point == null ? -1f : point.dst(pos);
    }

    boolean inSafeZone(float x, float y) {
        float dx = x - pos.x;
        float dy = y - pos.y;
        return dx * dx + dy * dy < def.safeRadius * def.safeRadius;
    }

    boolean inSafeZone(Vector2 point) {
        return point != null && inSafeZone(point.x, point.y);
    }

    boolean canDock(Vector2 point, float speed) {
        return distanceTo(point) <= def.dockRadius && speed < 30f;
    }

    void draw(ShapeRenderer shapes, float time) {
        float r = def.radius;
        float bob = MathUtils.sin(time * 0.6f) * 3f;

        // Sombra y anillo exterior.
        shapes.setColor(0f, 0f, 0f, 0.45f);
        shapes.circle(pos.x, pos.y - 8f + bob, r * 1.06f, 48);
        shapes.setColor(HULL_DARK);
        shapes.circle(pos.x, pos.y + bob, r, 48);
        shapes.setColor(HULL_MID);
        shapes.circle(pos.x, pos.y + bob, r * 0.92f, 48);
        shapes.setColor(HULL_DARK);
        shapes.circle(pos.x, pos.y + bob, r * 0.78f, 48);

        // Paneles del anillo.
        for (int i = 0; i < 16; i++) {
            float a = i * 22.5f + rotation;
            float px = pos.x + MathUtils.cosDeg(a) * r * 0.85f;
            float py = pos.y + bob + MathUtils.sinDeg(a) * r * 0.85f;
            shapes.setColor(i % 4 == 0 ? HULL_LIGHT : HULL_MID);
            shapes.circle(px, py, r * 0.055f, 8);
        }

        // Brazos y núcleo.
        for (int i = 0; i < 4; i++) {
            float a = i * 90f + rotation * 0.35f;
            float ex = pos.x + MathUtils.cosDeg(a) * r * 0.74f;
            float ey = pos.y + bob + MathUtils.sinDeg(a) * r * 0.74f;
            shapes.setColor(HULL_LIGHT);
            shapes.rectLine(pos.x, pos.y + bob, ex, ey, r * 0.045f);
        }
        shapes.setColor(HULL_MID);
        shapes.circle(pos.x, pos.y + bob, r * 0.30f, 32);
        shapes.setColor(HULL_DARK);
        shapes.circle(pos.x, pos.y + bob, r * 0.22f, 32);
        shapes.setColor(LIGHT);
        shapes.circle(pos.x, pos.y + bob, r * 0.10f, 24);
        shapes.setColor(1f, 1f, 1f, 0.9f);
        shapes.circle(pos.x, pos.y + bob, r * 0.05f, 18);

        // Bahía de acople en el lado inferior y luces de posición.
        float bayY = pos.y - r * 0.92f + bob;
        shapes.setColor(HULL_DARK);
        Visuals.roundRect(shapes, pos.x - r * 0.18f, bayY - r * 0.10f,
                r * 0.36f, r * 0.16f, r * 0.05f);
        shapes.setColor(LIGHT_WARM);
        shapes.rect(pos.x - r * 0.13f, bayY - r * 0.04f, r * 0.26f, r * 0.03f);
        float blink = 0.55f + 0.45f * MathUtils.sin(time * 3.2f);
        for (int i = 0; i < 6; i++) {
            float a = 30f + i * 60f;
            shapes.setColor(0.35f, 0.90f, 1f, 0.45f * blink);
            shapes.circle(pos.x + MathUtils.cosDeg(a) * r * 1.01f,
                    pos.y + bob + MathUtils.sinDeg(a) * r * 1.01f, r * 0.022f, 8);
        }
    }

    void drawGlow(SpriteBatch batch, Visuals art, float time) {
        float pulse = 0.7f + 0.3f * MathUtils.sin(time * 1.8f);
        art.glow(batch, pos.x, pos.y, def.radius * 2.2f, Visuals.ACCENT, 0.10f * pulse);
        art.glow(batch, pos.x, pos.y, def.radius * 0.8f, Color.WHITE, 0.12f * pulse);
        art.glow(batch, pos.x, pos.y - def.radius * 0.92f,
                def.radius * 0.5f, LIGHT_WARM, 0.22f * pulse);
    }

}

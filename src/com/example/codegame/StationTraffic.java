package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Tráfico cosmético alrededor de la estación: 3 naves pequeñas que entran y
 * salen. Solo se actualizan y dibujan si el jugador está cerca; no colisionan
 * ni tocan el mercado.
 */
final class StationTraffic {

    static final int SHIPS = 3;
    static final float ACTIVE_RADIUS = 1400f;
    private static final float[] ANGLES = {260f, 270f, 290f};
    private static final float[] RANGES = {300f, 380f, 300f};
    private static final float[] SPEEDS = {0.055f, 0.070f, 0.045f};
    private static final float SHIP_SIZE = 11f;

    private final float[] cycle = new float[SHIPS];
    private final float[] x = new float[SHIPS];
    private final float[] y = new float[SHIPS];
    private final float[] heading = new float[SHIPS];
    private float trailTimer;
    private boolean active;

    StationTraffic() {
        reset();
    }

    void reset() {
        for (int i = 0; i < SHIPS; i++) {
            cycle[i] = i * 0.31f;
        }
        trailTimer = 0f;
        active = false;
    }

    boolean active() {
        return active;
    }

    /** Naves visibles ahora (todas o ninguna). */
    int activeCount() {
        return active ? SHIPS : 0;
    }

    void update(float dt, Vector2 playerPos, Station station, Effects effects) {
        if (playerPos == null || station == null || dt <= 0f) {
            return;
        }
        active = playerPos.dst(station.pos) <= ACTIVE_RADIUS;
        if (!active) {
            return;
        }
        trailTimer += dt;
        boolean emit = trailTimer >= 0.06f;
        if (emit) {
            trailTimer = 0f;
        }
        for (int i = 0; i < SHIPS; i++) {
            cycle[i] += SPEEDS[i] * dt;
            while (cycle[i] >= 2f) {
                cycle[i] -= 2f;
            }
            float phase = cycle[i] <= 1f ? cycle[i] : 2f - cycle[i];
            boolean outbound = cycle[i] <= 1f;
            float angle = ANGLES[i];
            float radius = station.def.radius + 55f + phase * RANGES[i];
            x[i] = station.pos.x + MathUtils.cosDeg(angle) * radius;
            y[i] = station.pos.y + MathUtils.sinDeg(angle) * radius;
            heading[i] = outbound ? angle : angle + 180f;
            if (emit && effects != null) {
                float back = SHIP_SIZE * 1.05f;
                float ex = x[i] - MathUtils.cosDeg(heading[i]) * back;
                float ey = y[i] - MathUtils.sinDeg(heading[i]) * back;
                effects.trail(ex, ey, -MathUtils.cosDeg(heading[i]) * 20f,
                        -MathUtils.sinDeg(heading[i]) * 20f,
                        Visuals.WARN, 1.6f, 0.18f);
            }
        }
    }

    void draw(ShapeRenderer shapes, Station station, float time) {
        if (!active || station == null) {
            return;
        }
        for (int i = 0; i < SHIPS; i++) {
            Ship.drawBody(shapes, x[i], y[i], heading[i], SHIP_SIZE, true, 0.92f,
                    time + i);
        }
    }

    void drawGlow(SpriteBatch batch, Visuals art, Station station, float time) {
        if (!active || station == null) {
            return;
        }
        for (int i = 0; i < SHIPS; i++) {
            float back = SHIP_SIZE * 1.15f;
            art.glow(batch, x[i] - MathUtils.cosDeg(heading[i]) * back,
                    y[i] - MathUtils.sinDeg(heading[i]) * back,
                    SHIP_SIZE * 2.0f, Visuals.WARN, 0.22f);
        }
    }
}

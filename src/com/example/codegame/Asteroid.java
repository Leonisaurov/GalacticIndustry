package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Meteorito irregular con facetas, cráteres y brillo al recibir un impacto. */
class Asteroid {

    static final int SMALL = 0;
    static final int MEDIUM = 1;
    static final int LARGE = 2;

    private static final int SIDES = 9;
    /** Dirección de la luz, en grados (arriba-izquierda). */
    private static final float LIGHT_DEG = 135f;

    final Vector2 pos = new Vector2();
    final Vector2 vel = new Vector2();
    final int size;
    final float radius;
    final int reward;
    int hp;
    float flash;

    private float rot;
    private final float spin;
    private final float[] offsets = new float[SIDES];
    private final float[] poly = new float[SIDES * 2];
    private final int craterCount;
    private final float[] craterAng;
    private final float[] craterDist;
    private final float[] craterRad;

    Asteroid(float x, float y, int size) {
        pos.set(x, y);
        this.size = size;
        radius = size == SMALL ? 12f : size == MEDIUM ? 22f : 34f;
        hp = size == SMALL ? 1 : size == MEDIUM ? 2 : 4;
        reward = size == SMALL ? 2 : size == MEDIUM ? 5 : 10;
        vel.set(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f))
                .nor().scl(MathUtils.random(15f, 80f));
        spin = MathUtils.random(-60f, 60f);
        for (int i = 0; i < SIDES; i++) {
            offsets[i] = MathUtils.random(0.74f, 1f);
        }
        craterCount = 1 + (size == LARGE ? 3 : size == MEDIUM ? 1 : 0);
        craterAng = new float[craterCount];
        craterDist = new float[craterCount];
        craterRad = new float[craterCount];
        for (int i = 0; i < craterCount; i++) {
            craterAng[i] = MathUtils.random(360f);
            craterDist[i] = MathUtils.random(0.12f, 0.55f);
            craterRad[i] = MathUtils.random(0.13f, 0.26f);
        }
    }

    void update(float dt) {
        pos.mulAdd(vel, dt);
        rot += spin * dt;
        if (flash > 0f) {
            flash -= dt;
        }
    }

    boolean hit(Vector2 point, float otherRadius) {
        return pos.dst(point) < radius + otherRadius;
    }

    void draw(ShapeRenderer shapes) {
        for (int i = 0; i < SIDES; i++) {
            float r = radius * offsets[i];
            float a = rot + i * 360f / SIDES;
            poly[i * 2] = pos.x + MathUtils.cosDeg(a) * r;
            poly[i * 2 + 1] = pos.y + MathUtils.sinDeg(a) * r;
        }

        float flashT = MathUtils.clamp(flash / 0.08f, 0f, 1f);

        // Silueta exterior algo mayor, para separar la roca del fondo.
        shapes.setColor(0.20f, 0.17f, 0.16f, 0.95f);
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            float ax = pos.x + (poly[i * 2] - pos.x) * 1.06f;
            float ay = pos.y + (poly[i * 2 + 1] - pos.y) * 1.06f;
            float bx = pos.x + (poly[next * 2] - pos.x) * 1.06f;
            float by = pos.y + (poly[next * 2 + 1] - pos.y) * 1.06f;
            shapes.triangle(pos.x, pos.y, ax, ay, bx, by);
        }

        // Caras con sombreado por facetas.
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            float mid = rot + (i + 0.5f) * 360f / SIDES;
            float lit = 0.50f + 0.50f * Math.max(0f, MathUtils.cosDeg(mid - LIGHT_DEG));
            float r = 0.72f * lit;
            float g = 0.66f * lit;
            float b = 0.62f * lit;
            // Un tono azulado frío en las zonas en sombra.
            r += 0.04f * (1f - lit);
            g += 0.05f * (1f - lit);
            b += 0.08f * (1f - lit);
            if (flashT > 0f) {
                r += (1f - r) * flashT;
                g += (1f - g) * flashT;
                b += (1f - b) * flashT;
            }
            shapes.setColor(r, g, b, 1f);
            shapes.triangle(pos.x, pos.y,
                    poly[i * 2], poly[i * 2 + 1],
                    poly[next * 2], poly[next * 2 + 1]);
            if (lit > 0.82f) {
                shapes.setColor(0.88f, 0.83f, 0.74f, 0.40f + 0.3f * flashT);
                shapes.rectLine(poly[i * 2], poly[i * 2 + 1],
                        poly[next * 2], poly[next * 2 + 1], 1.4f);
            }
        }

        // Cráteres: hueco oscuro y borde iluminado.
        for (int i = 0; i < craterCount; i++) {
            float a = rot + craterAng[i];
            float cx = pos.x + MathUtils.cosDeg(a) * radius * craterDist[i];
            float cy = pos.y + MathUtils.sinDeg(a) * radius * craterDist[i];
            float cr = radius * craterRad[i];
            shapes.setColor(0.20f, 0.18f, 0.19f, 0.85f);
            shapes.circle(cx, cy, cr, 12);
            shapes.setColor(0.78f, 0.72f, 0.66f, 0.35f + 0.3f * flashT);
            shapes.circle(cx - cr * 0.22f, cy + cr * 0.20f, cr * 0.62f, 10);
            shapes.setColor(0.30f, 0.27f, 0.27f, 0.9f);
            shapes.circle(cx - cr * 0.18f, cy + cr * 0.16f, cr * 0.45f, 10);
        }
    }

    private static final Color FLASH_GLOW = new Color(1f, 0.95f, 0.75f, 1f);

    /** Brillos de impacto; el resto del tiempo la roca no emite luz. */
    void drawGlow(SpriteBatch batch, Visuals art) {
        if (flash > 0f) {
            float t = MathUtils.clamp(flash / 0.08f, 0f, 1f);
            art.glow(batch, pos.x, pos.y, radius * 3.2f, FLASH_GLOW, 0.45f * t);
        }
    }
}

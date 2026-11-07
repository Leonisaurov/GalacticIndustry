package com.example.codegame;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Meteorito irregular. Al destruirlo da monedas; los grandes se parten en dos. */
class Asteroid {

    static final int SMALL = 0;
    static final int MEDIUM = 1;
    static final int LARGE = 2;

    private static final int SIDES = 9;

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
            offsets[i] = MathUtils.random(0.75f, 1f);
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
        float base = flash > 0f ? 0.95f : 0.5f;
        shapes.setColor(base, base * 0.92f, base * 0.85f, 1f);
        // polygon() solo funciona en modo Line; en modo Filled se rellena con triángulos desde el centro.
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            shapes.triangle(pos.x, pos.y,
                    poly[i * 2], poly[i * 2 + 1],
                    poly[next * 2], poly[next * 2 + 1]);
        }
    }
}

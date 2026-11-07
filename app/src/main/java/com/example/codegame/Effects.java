package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/** Partículas de explosiones y chispas. */
class Effects {

    private static class Particle {
        float x, y, vx, vy, life, maxLife, size;
        Color color;
    }

    private final Array<Particle> list = new Array<>();

    void burst(float x, float y, Color color, int count, float speed) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            float a = MathUtils.random(360f);
            float s = MathUtils.random(speed * 0.3f, speed);
            p.vx = MathUtils.cosDeg(a) * s;
            p.vy = MathUtils.sinDeg(a) * s;
            p.maxLife = MathUtils.random(0.3f, 0.6f);
            p.life = p.maxLife;
            p.size = MathUtils.random(2f, 5f);
            p.color = color;
            list.add(p);
        }
    }

    void update(float dt) {
        for (int i = list.size - 1; i >= 0; i--) {
            Particle p = list.get(i);
            p.life -= dt;
            if (p.life <= 0f) {
                list.removeIndex(i);
                continue;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }
    }

    void draw(ShapeRenderer shapes) {
        for (int i = 0; i < list.size; i++) {
            Particle p = list.get(i);
            float t = p.life / p.maxLife;
            shapes.setColor(p.color.r, p.color.g, p.color.b, t);
            shapes.circle(p.x, p.y, p.size * t + 0.5f, 6);
        }
    }

    void clear() {
        list.clear();
    }
}

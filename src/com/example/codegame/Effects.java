package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/** Partículas de explosiones, estelas y ondas de choque. */
class Effects {

    private static final int MAX_PARTICLES = 520;

    private static class Particle {
        float x, y, vx, vy, life, maxLife, size;
        Color color;
    }

    private static class Shock {
        float x, y, maxRadius, life, maxLife;
        Color color;
    }

    private final Array<Particle> list = new Array<>();
    private final Array<Shock> shocks = new Array<>();

    void burst(float x, float y, Color color, int count, float speed) {
        for (int i = 0; i < count && list.size < MAX_PARTICLES; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            float a = MathUtils.random(360f);
            float s = MathUtils.random(speed * 0.25f, speed);
            p.vx = MathUtils.cosDeg(a) * s;
            p.vy = MathUtils.sinDeg(a) * s;
            p.maxLife = MathUtils.random(0.28f, 0.65f);
            p.life = p.maxLife;
            p.size = MathUtils.random(2f, 5.5f);
            p.color = color;
            list.add(p);
        }
    }

    /** Chispa suelta para estelas de motor y de misiles. */
    void trail(float x, float y, float vx, float vy, Color color, float size, float life) {
        if (list.size >= MAX_PARTICLES) {
            return;
        }
        Particle p = new Particle();
        p.x = x + MathUtils.random(-2f, 2f);
        p.y = y + MathUtils.random(-2f, 2f);
        p.vx = vx + MathUtils.random(-14f, 14f);
        p.vy = vy + MathUtils.random(-14f, 14f);
        p.maxLife = life;
        p.life = life;
        p.size = size;
        p.color = color;
        list.add(p);
    }

    /** Onda expansiva circular. */
    void shock(float x, float y, Color color, float maxRadius) {
        Shock s = new Shock();
        s.x = x;
        s.y = y;
        s.maxRadius = maxRadius;
        s.maxLife = 0.42f;
        s.life = s.maxLife;
        s.color = color;
        shocks.add(s);
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
            float drag = Math.max(0f, 1f - 2.4f * dt);
            p.vx *= drag;
            p.vy *= drag;
        }
        for (int i = shocks.size - 1; i >= 0; i--) {
            Shock s = shocks.get(i);
            s.life -= dt;
            if (s.life <= 0f) {
                shocks.removeIndex(i);
            }
        }
    }

    void draw(ShapeRenderer shapes) {
        for (int i = 0; i < list.size; i++) {
            Particle p = list.get(i);
            float t = p.life / p.maxLife;
            shapes.setColor(p.color.r, p.color.g, p.color.b, t * t);
            shapes.circle(p.x, p.y, p.size * (0.35f + 0.65f * t) + 0.4f, 6);
        }
        for (int i = 0; i < shocks.size; i++) {
            Shock s = shocks.get(i);
            float p = 1f - s.life / s.maxLife;
            float ease = 1f - (1f - p) * (1f - p);
            float radius = s.maxRadius * (0.12f + 0.88f * ease);
            float alpha = (1f - p) * (1f - p);
            for (int k = 0; k < 22; k++) {
                float a = k * (360f / 22f) + p * 30f;
                shapes.setColor(s.color.r, s.color.g, s.color.b, alpha * 0.85f);
                shapes.circle(s.x + MathUtils.cosDeg(a) * radius,
                        s.y + MathUtils.sinDeg(a) * radius, 1.8f + 2.2f * (1f - p), 5);
            }
        }
    }

    /** Versión luminosa de las partículas para la pasada aditiva. */
    void drawGlow(SpriteBatch batch, Visuals art) {
        for (int i = 0; i < list.size; i++) {
            Particle p = list.get(i);
            float t = p.life / p.maxLife;
            float size = p.size * (2.4f + 1.8f * t);
            art.glow(batch, p.x, p.y, size, p.color, t * 0.55f);
        }
        for (int i = 0; i < shocks.size; i++) {
            Shock s = shocks.get(i);
            float p = 1f - s.life / s.maxLife;
            float ease = 1f - (1f - p) * (1f - p);
            float radius = s.maxRadius * (0.12f + 0.88f * ease);
            float alpha = (1f - p) * (1f - p);
            for (int k = 0; k < 22; k++) {
                float a = k * (360f / 22f) + p * 30f;
                art.glow(batch, s.x + MathUtils.cosDeg(a) * radius,
                        s.y + MathUtils.sinDeg(a) * radius,
                        10f * (1f - p) + 4f, s.color, alpha * 0.5f);
            }
        }
    }

    void clear() {
        list.clear();
        shocks.clear();
    }

    /** Número de partículas activas; útil para telemetría y pruebas. */
    int particleCount() {
        return list.size;
    }
}

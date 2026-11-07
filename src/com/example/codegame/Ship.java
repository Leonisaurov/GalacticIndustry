package com.example.codegame;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Nave del jugador: gira hacia el joystick o con teclado, acelera con inercia y gasta combustible. */
class Ship {

    static final float RADIUS = 16f;
    static final float TURN_SPEED = 220f;
    static final float FUEL_PER_SEC_THRUST = 12f;
    static final float FUEL_PER_SHOT = 2f;

    final Vector2 pos = new Vector2();
    final Vector2 vel = new Vector2();
    final Vector2 dir = new Vector2(0f, 1f);
    float heading = 90f;
    float fuel;
    float maxFuel;
    int lives;
    int maxLives;
    float cooldown;
    float invulnerable;
    boolean thrusting;

    void reset(Upgrades up, float x, float y) {
        pos.set(x, y);
        vel.set(0f, 0f);
        heading = 90f;
        dir.set(0f, 1f);
        maxFuel = up.maxFuel();
        fuel = maxFuel;
        maxLives = up.lives();
        lives = maxLives;
        cooldown = 0f;
        invulnerable = 0f;
    }

    /** Gira, acelera según el nivel del acelerador y mueve la nave. */
    void update(float dt, Upgrades up, Controls c) {
        if (c.aiming) {
            float step = TURN_SPEED * dt;
            heading += MathUtils.clamp(wrapDeg(c.aimDeg - heading), -step, step);
        } else {
            heading += c.turn * TURN_SPEED * dt;
        }
        heading = wrapDeg(heading);
        dir.set(MathUtils.cosDeg(heading), MathUtils.sinDeg(heading));

        float level = MathUtils.clamp(c.thrust, 0f, 1f);
        thrusting = level > 0.01f && fuel > 0f;
        if (thrusting) {
            vel.mulAdd(dir, up.thrust() * level * dt);
            fuel = Math.max(0f, fuel - FUEL_PER_SEC_THRUST * up.fuelUse() * level * dt);
        }
        float max = up.maxSpeed();
        if (vel.len2() > max * max) {
            vel.setLength(max);
        }
        pos.mulAdd(vel, dt);

        cooldown = Math.max(0f, cooldown - dt);
        invulnerable = Math.max(0f, invulnerable - dt);
    }

    /** Ángulo normalizado al intervalo [-180, 180). */
    static float wrapDeg(float a) {
        return ((a % 360f) + 540f) % 360f - 180f;
    }

    boolean canFire() {
        return cooldown <= 0f && fuel >= FUEL_PER_SHOT;
    }

    Missile fire(Upgrades up) {
        cooldown = up.fireCooldown();
        fuel = Math.max(0f, fuel - FUEL_PER_SHOT * up.fuelUse());
        return new Missile(pos.x + dir.x * RADIUS, pos.y + dir.y * RADIUS, dir, vel, up.damage());
    }

    void refuel(float amount) {
        fuel = Math.min(maxFuel, fuel + amount);
    }

    void draw(ShapeRenderer shapes) {
        float flicker = invulnerable > 0f && ((int) (invulnerable * 12f) % 2 == 0) ? 0.4f : 1f;
        float c = MathUtils.cosDeg(heading);
        float s = MathUtils.sinDeg(heading);
        float nx = -s;
        float ny = c;

        if (thrusting) {
            float fx = pos.x - c * RADIUS * 0.8f;
            float fy = pos.y - s * RADIUS * 0.8f;
            shapes.setColor(1f, 0.6f, 0.2f, 0.9f * flicker);
            shapes.triangle(
                    pos.x + nx * 5f, pos.y + ny * 5f,
                    pos.x - nx * 5f, pos.y - ny * 5f,
                    fx, fy);
        }

        float tipX = pos.x + c * RADIUS;
        float tipY = pos.y + s * RADIUS;
        float lx = pos.x - c * RADIUS * 0.7f + nx * RADIUS * 0.8f;
        float ly = pos.y - s * RADIUS * 0.7f + ny * RADIUS * 0.8f;
        float rx = pos.x - c * RADIUS * 0.7f - nx * RADIUS * 0.8f;
        float ry = pos.y - s * RADIUS * 0.7f - ny * RADIUS * 0.8f;

        shapes.setColor(0.25f, 0.85f, 0.95f, flicker);
        shapes.triangle(tipX, tipY, lx, ly, rx, ry);
        shapes.setColor(1f, 1f, 1f, 0.7f * flicker);
        shapes.circle(pos.x + c * 3f, pos.y + s * 3f, 3f, 8);
    }
}

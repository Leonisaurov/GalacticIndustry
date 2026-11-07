package com.example.codegame;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

/** Misil disparado por la nave. Destruye meteoritos al impactar. */
class Missile {

    static final float SPEED = 560f;
    static final float LIFE = 1.1f;
    static final float RADIUS = 3f;

    final Vector2 pos = new Vector2();
    final Vector2 vel = new Vector2();
    final int damage;
    private float life = LIFE;

    Missile(float x, float y, Vector2 dir, Vector2 shipVel, int damage) {
        pos.set(x, y);
        vel.set(dir).scl(SPEED).add(shipVel);
        this.damage = damage;
    }

    /** Segundos de vida que le quedan al misil. */
    float remaining() {
        return life;
    }

    /** Avanza el misil. Devuelve false cuando ha caducado. */
    boolean update(float dt) {
        pos.mulAdd(vel, dt);
        life -= dt;
        return life > 0f;
    }

    void draw(ShapeRenderer shapes) {
        shapes.setColor(1f, 0.6f, 0.2f, 0.6f);
        shapes.rectLine(pos.x, pos.y, pos.x - vel.x * 0.05f, pos.y - vel.y * 0.05f, 3f);
        shapes.setColor(1f, 0.95f, 0.5f, 1f);
        shapes.circle(pos.x, pos.y, RADIUS + damage * 0.5f, 8);
    }
}

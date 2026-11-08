package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

/** Misil disparado por la nave. Destruye meteoritos al impactar. */
class Missile {

    static final float SPEED = 560f;
    static final float LIFE = 1.1f;
    static final float RADIUS = 3f;

    private static final Color GLOW = new Color(1f, 0.55f, 0.15f, 1f);

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
        float len = vel.len();
        float dx = len > 0.001f ? vel.x / len : 1f;
        float dy = len > 0.001f ? vel.y / len : 0f;
        float tail = 16f + damage * 2f;

        // Estela.
        shapes.setColor(1f, 0.42f, 0.10f, 0.30f);
        shapes.rectLine(pos.x, pos.y, pos.x - dx * tail, pos.y - dy * tail, 4.5f);
        shapes.setColor(1f, 0.78f, 0.35f, 0.55f);
        shapes.rectLine(pos.x, pos.y, pos.x - dx * tail * 0.6f, pos.y - dy * tail * 0.6f, 2.2f);

        // Cabeza.
        shapes.setColor(1f, 0.55f, 0.15f, 0.85f);
        shapes.circle(pos.x, pos.y, RADIUS + damage * 0.55f + 1.5f, 10);
        shapes.setColor(1f, 0.95f, 0.65f, 1f);
        shapes.circle(pos.x, pos.y, RADIUS + damage * 0.35f, 8);
        shapes.setColor(Color.WHITE);
        shapes.circle(pos.x + dx * 1.2f, pos.y + dy * 1.2f, 1.6f, 6);
    }

    void drawGlow(SpriteBatch batch, Visuals art) {
        float size = 16f + damage * 4f;
        art.glow(batch, pos.x, pos.y, size, GLOW, 0.55f);
        art.glow(batch, pos.x, pos.y, size * 0.45f, Color.WHITE, 0.75f);
    }
}

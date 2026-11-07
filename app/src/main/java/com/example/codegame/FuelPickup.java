package com.example.codegame;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Bidón de combustible flotando en el mundo. Recargar al tocarlo con la nave. */
class FuelPickup {

    static final float RADIUS = 12f;
    static final float LIFE = 30f;
    static final float AMOUNT = 40f;

    final Vector2 pos = new Vector2();
    private float life = LIFE;
    private float bob;

    FuelPickup(float x, float y) {
        this(x, y, LIFE);
    }

    /** Bidón con el tiempo de vida que le quedaba (al restaurar una partida). */
    FuelPickup(float x, float y, float life) {
        pos.set(x, y);
        this.life = life;
        bob = MathUtils.random(0f, MathUtils.PI2);
    }

    float life() {
        return life;
    }

    /** Devuelve false cuando el bidón desaparece por tiempo. */
    boolean update(float dt) {
        life -= dt;
        bob += dt * 3f;
        return life > 0f;
    }

    void draw(ShapeRenderer shapes) {
        float y = pos.y + MathUtils.sin(bob) * 3f;
        shapes.setColor(0.2f, 0.9f, 0.4f, 0.25f);
        shapes.circle(pos.x, y, RADIUS * 1.6f, 16);
        shapes.setColor(0.3f, 1f, 0.5f, 1f);
        shapes.rect(pos.x - 7f, y - 9f, 14f, 18f);
        shapes.setColor(0.9f, 1f, 0.9f, 1f);
        shapes.rect(pos.x - 3f, y - 4f, 6f, 8f);
    }
}

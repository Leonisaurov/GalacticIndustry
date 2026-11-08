package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Bidón de combustible flotando en el mundo. Recargar al tocarlo con la nave. */
class FuelPickup {

    static final float RADIUS = 12f;
    static final float LIFE = 30f;
    static final float AMOUNT = 40f;
    /** Margen de captura para que un roce cuente como recogida (especialmente con piloto). */
    static final float CAPTURE_PAD = 6f;

    private static final Color GLOW = new Color(0.30f, 1f, 0.55f, 1f);

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
        float y = pos.y + MathUtils.sin(bob) * 4f;
        float blink = life < 5f && ((int) (life * 8f) % 2 == 0) ? 0.45f : 1f;
        float pulse = 0.75f + 0.25f * MathUtils.sin(bob * 1.7f);

        // Anillo de partículas que late.
        for (int i = 0; i < 10; i++) {
            float a = i * 36f + bob * 8f;
            float rr = RADIUS * 1.85f + MathUtils.sin(bob * 2f + i) * 2f;
            shapes.setColor(0.25f, 0.95f, 0.5f, 0.18f * pulse * blink);
            shapes.circle(pos.x + MathUtils.cosDeg(a) * rr, y + MathUtils.sinDeg(a) * rr, 2.4f, 5);
        }

        // Sombra/contorno del bidón.
        shapes.setColor(0.03f, 0.10f, 0.06f, 0.9f * blink);
        Visuals.roundRect(shapes, pos.x - 10f, y - 14f, 20f, 28f, 6f);
        // Cuerpo.
        shapes.setColor(0.16f, 0.78f, 0.38f, 0.98f * blink);
        Visuals.roundRect(shapes, pos.x - 8.5f, y - 12.5f, 17f, 25f, 5f);
        // Brillo lateral.
        shapes.setColor(0.55f, 1f, 0.70f, 0.55f * blink);
        Visuals.roundRect(shapes, pos.x - 6.5f, y - 9.5f, 4f, 19f, 2f);
        // Franja oscura.
        shapes.setColor(0.06f, 0.35f, 0.18f, 0.9f * blink);
        shapes.rect(pos.x - 8.5f, y + 2.5f, 17f, 4f);
        // Tapón.
        shapes.setColor(0.65f, 1f, 0.75f, 0.95f * blink);
        shapes.rect(pos.x - 3.5f, y + 12f, 7f, 4f);
        // Cruz de combustible.
        shapes.setColor(1f, 1f, 1f, 0.95f * blink);
        shapes.rect(pos.x - 5f, y - 1.5f, 10f, 3f);
        shapes.rect(pos.x - 1.5f, y - 5f, 3f, 10f);
    }

    void drawGlow(SpriteBatch batch, Visuals art) {
        float y = pos.y + MathUtils.sin(bob) * 4f;
        float pulse = 0.72f + 0.28f * MathUtils.sin(bob * 1.7f);
        float blink = life < 5f && ((int) (life * 8f) % 2 == 0) ? 0.4f : 1f;
        art.glow(batch, pos.x, y, RADIUS * 5.2f, GLOW, 0.22f * pulse * blink);
        art.glow(batch, pos.x, y, RADIUS * 2.4f, Color.WHITE, 0.16f * pulse * blink);
    }
}

package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Fragmento de mineral flotando en el espacio. Lo generan los misiles al
 * romper un asteroide; no entra en la bodega hasta que la nave lo recoge (o,
 * más adelante, el rayo tractor lo atrae). Tiene una vida limitada, así que
 * parte del material puede perderse.
 */
class OreFragment {

    static final float RADIUS = 7f;
    static final float LIFE = 90f;
    /** Margen de captura para que un roce cuente como recogida. */
    static final float CAPTURE_PAD = 6f;

    private static final Color HIELO_C = new Color(0.68f, 0.90f, 1f, 1f);
    private static final Color SILICATO_C = new Color(0.78f, 0.80f, 0.82f, 1f);
    private static final Color HIERRO_C = new Color(0.82f, 0.55f, 0.42f, 1f);
    private static final Color COBRE_C = new Color(1f, 0.62f, 0.32f, 1f);
    private static final Color TITANIO_C = new Color(0.62f, 0.76f, 0.92f, 1f);
    private static final Color ORO_C = new Color(1f, 0.85f, 0.35f, 1f);

    final Commodity commodity;
    final int amount;
    final Vector2 pos = new Vector2();
    final Vector2 vel = new Vector2();
    private float life = LIFE;
    private float spin;
    private float rot;

    OreFragment(float x, float y, Commodity commodity, int amount,
                float vx, float vy) {
        this(x, y, commodity, amount, vx, vy, LIFE);
    }

    /** Fragmento restaurado desde un guardado, con la vida que le quedaba. */
    OreFragment(float x, float y, Commodity commodity, int amount,
                float vx, float vy, float life) {
        pos.set(x, y);
        this.commodity = commodity;
        this.amount = Math.max(1, amount);
        vel.set(vx, vy);
        this.life = Math.max(0.01f, life);
        spin = MathUtils.random(-140f, 140f);
        rot = MathUtils.random(360f);
    }

    float life() {
        return life;
    }

    /** Devuelve false cuando el fragmento se pierde por tiempo. */
    boolean update(float dt) {
        pos.mulAdd(vel, dt);
        vel.scl(0.995f);
        rot += spin * dt;
        life -= dt;
        return life > 0f;
    }

    /** Intenta meter el fragmento completo en la bodega. */
    boolean collect(CargoHold cargo) {
        return cargo != null && cargo.addExact(commodity, amount);
    }

    void draw(ShapeRenderer shapes) {
        float blink = life < 8f && ((int) (life * 8f) % 2 == 0) ? 0.45f : 1f;
        Color base = colorFor(commodity);
        float size = RADIUS * (0.85f + 0.25f * MathUtils.sin(rot * MathUtils.degreesToRadians * 2f));

        shapes.setColor(0.02f, 0.03f, 0.06f, 0.75f * blink);
        shapes.circle(pos.x, pos.y + 1.5f, size * 1.15f, 8);

        shapes.setColor(base.r * 0.55f, base.g * 0.55f, base.b * 0.55f, 0.95f * blink);
        shapes.triangle(pos.x, pos.y + size, pos.x - size * 0.9f, pos.y - size * 0.2f,
                pos.x + size * 0.2f, pos.y - size * 0.9f);
        shapes.setColor(base.r, base.g, base.b, 0.95f * blink);
        shapes.triangle(pos.x, pos.y + size * 0.95f, pos.x + size, pos.y,
                pos.x + size * 0.15f, pos.y - size * 0.35f);
        shapes.setColor(1f, 1f, 1f, 0.55f * blink);
        shapes.triangle(pos.x + size * 0.10f, pos.y + size * 0.45f,
                pos.x + size * 0.50f, pos.y + size * 0.05f,
                pos.x + size * 0.20f, pos.y - size * 0.10f);
    }

    void drawGlow(SpriteBatch batch, Visuals art) {
        Color base = colorFor(commodity);
        float blink = life < 8f && ((int) (life * 8f) % 2 == 0) ? 0.4f : 1f;
        art.glow(batch, pos.x, pos.y, RADIUS * 3.6f, base, 0.20f * blink);
        art.glow(batch, pos.x, pos.y, RADIUS * 1.5f, Color.WHITE, 0.10f * blink);
    }

    static Color colorFor(Commodity commodity) {
        if (commodity == null) {
            return SILICATO_C;
        }
        switch (commodity) {
            case HIELO: return HIELO_C;
            case HIERRO: return HIERRO_C;
            case COBRE: return COBRE_C;
            case TITANIO: return TITANIO_C;
            case ORO: return ORO_C;
            default: return SILICATO_C;
        }
    }
}

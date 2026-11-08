package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.Random;

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
    /** Recompensa legacy por destrucción; T-405 la sustituye por mineral. */
    final int reward;
    final AsteroidType type;
    int hp;
    int reserve;
    final int maxReserve;
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
        this(x, y, size, AsteroidType.C, -1, -1,
                seedFrom(x, y, size, AsteroidType.C));
    }

    Asteroid(float x, float y, int size, AsteroidType type, long seed) {
        this(x, y, size, type, -1, -1, seed);
    }

    Asteroid(float x, float y, int size, AsteroidType type,
             int reserve, int maxReserve, long seed) {
        pos.set(x, y);
        this.size = size;
        this.type = type == null ? AsteroidType.C : type;
        radius = size == SMALL ? 12f : size == MEDIUM ? 22f : 34f;
        hp = size == SMALL ? 1 : size == MEDIUM ? 2 : 4;
        reward = size == SMALL ? 2 : size == MEDIUM ? 5 : 10;
        Random random = new Random(seed);
        int rolledReserve = reserve >= 0 ? reserve : this.type.rollReserve(random);
        this.maxReserve = Math.max(rolledReserve, maxReserve);
        this.reserve = Math.min(rolledReserve, this.maxReserve);
        vel.set(random.nextFloat() * 2f - 1f, random.nextFloat() * 2f - 1f)
                .nor().scl(15f + random.nextFloat() * 65f);
        spin = -60f + random.nextFloat() * 120f;
        for (int i = 0; i < SIDES; i++) {
            offsets[i] = 0.74f + random.nextFloat() * 0.26f;
        }
        craterCount = 1 + (size == LARGE ? 3 : size == MEDIUM ? 1 : 0);
        craterAng = new float[craterCount];
        craterDist = new float[craterCount];
        craterRad = new float[craterCount];
        for (int i = 0; i < craterCount; i++) {
            craterAng[i] = random.nextFloat() * 360f;
            craterDist[i] = 0.12f + random.nextFloat() * 0.43f;
            craterRad[i] = 0.13f + random.nextFloat() * 0.13f;
        }
    }

    /** Semilla estable para que una roca sea reproducible desde su posición y tipo. */
    static long seedFrom(float x, float y, int size, AsteroidType type) {
        long seed = ((long) Float.floatToIntBits(x)) * 73856093L
                ^ ((long) Float.floatToIntBits(y)) * 19349663L
                ^ ((long) (size + 1)) * 83492791L
                ^ ((long) ((type == null ? 0 : type.ordinal()) + 1)) * 2654435761L;
        return seed == 0L ? 1L : seed;
    }

    float hardness() {
        return type.hardness;
    }

    float compositionFraction(Commodity commodity) {
        return type.fraction(commodity);
    }

    /** Extrae hasta {@code amount} UM de reserva y devuelve lo extraído. */
    int takeReserve(int amount) {
        int taken = Math.min(reserve, Math.max(0, amount));
        reserve -= taken;
        return taken;
    }

    boolean depleted() {
        return reserve <= 0;
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

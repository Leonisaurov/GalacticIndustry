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
    /** Unidades de cada mineral que quedan en la roca. */
    final int[] mineralUnits;
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
        mineralUnits = new int[this.type.minerals.length];
        rebuildComposition();
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
        int requested = Math.min(reserve, Math.max(0, amount));
        if (requested <= 0) {
            return 0;
        }
        Cargo[] batch = compositionBatch(requested);
        return commitExtraction(batch, requested);
    }

    boolean depleted() {
        return reserve <= 0;
    }

    /**
     * Reparte {@code units} UM tomando de la composición precalculada de la
     * roca. No modifica la reserva; el llamador confirma con
     * {@link #commitExtraction(Cargo[], int)}.
     */
    Cargo[] compositionBatch(int units) {
        int requested = Math.min(Math.max(0, units), reserve);
        Cargo[] batch = new Cargo[type.minerals.length];
        int left = requested;
        for (int i = 0; i < type.minerals.length; i++) {
            int amount = Math.min(left, mineralUnits[i]);
            batch[i] = new Cargo(type.minerals[i], amount);
            left -= amount;
        }
        return batch;
    }

    /** Confirma una extracción ya repartida. */
    int commitExtraction(Cargo[] batch, int units) {
        int committed = Math.min(Math.max(0, units), reserve);
        if (batch != null) {
            for (int i = 0; i < mineralUnits.length && i < batch.length; i++) {
                mineralUnits[i] = Math.max(0, mineralUnits[i] - batch[i].amount);
            }
        }
        reserve -= committed;
        return committed;
    }

    /**
     * Recalcula la composición pendiente a partir de la reserva extraída.
     * La extracción se consume en orden de mineral, así que el estado se puede
     * reconstruir desde {@code maxReserve - reserve} sin guardar el array.
     */
    private void rebuildComposition() {
        int[] full = new int[type.minerals.length];
        int assigned = 0;
        float[] remainders = new float[type.minerals.length];
        for (int i = 0; i < type.minerals.length; i++) {
            float exact = maxReserve * type.fractions[i];
            int base = (int) Math.floor(exact + 0.0001f);
            full[i] = base;
            remainders[i] = exact - base;
            assigned += base;
        }
        int left = maxReserve - assigned;
        boolean[] used = new boolean[type.minerals.length];
        while (left > 0) {
            int pick = -1;
            float best = -1f;
            for (int i = 0; i < remainders.length; i++) {
                if (!used[i] && remainders[i] > best) {
                    best = remainders[i];
                    pick = i;
                }
            }
            if (pick < 0) {
                used = new boolean[type.minerals.length];
                continue;
            }
            full[pick]++;
            used[pick] = true;
            left--;
        }
        int mined = Math.max(0, maxReserve - reserve);
        for (int i = 0; i < mineralUnits.length; i++) {
            int take = Math.min(mined, full[i]);
            full[i] -= take;
            mined -= take;
            mineralUnits[i] = full[i];
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

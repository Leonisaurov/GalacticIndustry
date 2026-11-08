package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Nave del jugador: gira hacia el joystick o con teclado, acelera con inercia y gasta combustible. */
class Ship {

    static final float RADIUS = 16f;
    static final float TURN_SPEED = 220f;
    static final float FUEL_PER_SEC_THRUST = 10f;
    static final float FUEL_PER_SHOT = 2f;

    /** Partes de la nave que se pueden danar y reparar en la tienda. */
    static final int HULL = 0;    static final int ENGINE = 1;
    static final int TANK = 2;
    static final int CANNON = 3;
    static final int STEERING = 4;
    static final int WING_LEFT = 5;
    static final int WING_RIGHT = 6;
    /** Soporte vital: afecta al filtro de O2; se añade al final para no romper índices. */
    static final int LIFE_SUPPORT = 7;
    /** Láser minero: afecta a la tasa de extracción. */
    static final int MINING_LASER = 8;
    static final int PART_COUNT = 9;
    /** Integridad maxima de los sistemas; el casco crece con la mejora Blindaje. */
    static final float PART_MAX = 100f;
    /** Fuga maxima del deposito danado, en combustible por segundo. */
    static final float TANK_LEAK = 2.2f;

    static final String[] PART_NAMES = {
            "CASCO", "MOTOR", "DEPOSITO", "CANON", "TIMON", "ALA IZQ", "ALA DER",
            "SOPORTE", "LASER"
    };

    /** Bodega de la nave; la capacidad se ajustará con la mejora de bodega. */
    final CargoHold cargo = GameData.newCargoHold();
    final EnergyCapacitor capacitor = new EnergyCapacitor();
    final MiningLaser laser = new MiningLaser();
    final TractorBeam tractor = new TractorBeam();
    final Vector2 pos = new Vector2();
    final Vector2 vel = new Vector2();
    final Vector2 dir = new Vector2(0f, 1f);
    float heading = 90f;
    float fuel;
    float maxFuel;
    float maxHull;
    /** Integridad de cada parte: 0 = destruida, {@link #partMax(int)} = intacta.
     *  El casco es integridad de la nave; la salud del piloto vive en {@link Pilot}. */
    final float[] part = new float[PART_COUNT];
    /** Ultima parte golpeada y aviso temporal para el HUD. */
    int lastDamaged = -1;
    float damageFlash;
    float cooldown;
    float invulnerable;
    boolean thrusting;
    /** La nave ha explotado; se deja de dibujar hasta la siguiente partida. */
    boolean wrecked;

    void reset(Upgrades up, float x, float y) {
        pos.set(x, y);
        vel.set(0f, 0f);
        heading = 90f;
        dir.set(0f, 1f);
        maxHull = up.hull();
        for (int i = 0; i < PART_COUNT; i++) {
            part[i] = i == HULL ? maxHull : PART_MAX;
        }
        maxFuel = up.maxFuel();
        fuel = maxFuel;
        cargo.setCapacity(up.cargoCapacity());
        cargo.clear();
        laser.setLevel(up.laserLevel());
        capacitor.setMaxEnergy(up.capacitorMax());
        capacitor.reset();
        laser.stop();
        tractor.setLevel(up.tractorLevel());
        tractor.stop();
        cooldown = 0f;
        invulnerable = 0f;
        lastDamaged = -1;
        damageFlash = 0f;
        wrecked = false;
    }

    CargoHold cargo() {
        return cargo;
    }

    /** Gira, acelera según el nivel del acelerador y mueve la nave. */
    void update(float dt, Upgrades up, Controls c) {
        float wing = wingFactor();
        float steer = steerFactor() * (0.55f + 0.45f * wing);
        float massTurn = MassModel.turnFactor(cargo);
        float massAccel = MassModel.accelerationFactor(cargo);
        float massSpeed = MassModel.speedFactor(cargo);
        float massFuel = MassModel.fuelFactor(cargo);
        if (c.aiming) {
            float step = TURN_SPEED * steer * massTurn * dt;
            heading += MathUtils.clamp(wrapDeg(c.aimDeg - heading), -step, step);
        } else {
            heading += c.turn * TURN_SPEED * steer * massTurn * dt;
        }
        // Con un ala perdida la nave vira sola hacia el lado danado.
        heading = wrapDeg(heading + wingBalance() * 9f * dt);
        dir.set(MathUtils.cosDeg(heading), MathUtils.sinDeg(heading));

        float level = MathUtils.clamp(c.thrust, 0f, 1f);
        float engine = engineFactor();
        thrusting = level > 0.01f && fuel > 0f;
        if (thrusting) {
            vel.mulAdd(dir, up.thrust() * engine * level * massAccel * dt);
            fuel = Math.max(0f, fuel - FUEL_PER_SEC_THRUST * up.fuelUse() * massFuel * level * dt);
        }
        float max = up.maxSpeed() * (0.35f + 0.65f * engine) * (0.78f + 0.22f * wing) * massSpeed;
        if (vel.len2() > max * max) {
            vel.setLength(max);
        }
        pos.mulAdd(vel, dt);

        // Deposito danado: menos capacidad y fuga lenta de combustible.
        maxFuel = up.maxFuel() * tankFactor();
        if (fuel > maxFuel) {
            fuel = maxFuel;
        }
        float tank = part[TANK] / PART_MAX;
        if (tank < 0.999f) {
            fuel = Math.max(0f, fuel - TANK_LEAK * (1f - tank) * dt);
        }

        cooldown = Math.max(0f, cooldown - dt);
        invulnerable = Math.max(0f, invulnerable - dt);
        damageFlash = Math.max(0f, damageFlash - dt);
    }

    /** Empuje y velocidad segun la integridad del motor (10 % con el motor destruido). */
    float engineFactor() {
        return 0.10f + 0.90f * part[ENGINE] / PART_MAX;
    }

    /** Velocidad de giro segun el timon (20 % con el timon destruido). */
    float steerFactor() {
        return 0.20f + 0.80f * part[STEERING] / PART_MAX;
    }

    /** Dan o y cadencia del canon; con el canon destruido no se puede disparar. */
    float cannonFactor() {
        return part[CANNON] / PART_MAX;
    }

    /** Capacidad del deposito (40 % con el deposito destruido). */
    float tankFactor() {
        return 0.40f + 0.60f * part[TANK] / PART_MAX;
    }

    /** Integridad del láser minero (0 = sin extracción). */
    float laserFactor() {
        return part[MINING_LASER] / PART_MAX;
    }

    /** Media de integridad de las dos alas; afecta a giro y velocidad. */
    float wingFactor() {
        return (part[WING_LEFT] + part[WING_RIGHT]) / (2f * PART_MAX);
    }

    /** Desequilibrio entre alas: negativo si falta la derecha, positivo si falta la izquierda. */
    float wingBalance() {
        return (part[WING_RIGHT] - part[WING_LEFT]) / PART_MAX;
    }

    float partMax(int index) {
        return index == HULL ? maxHull : PART_MAX;
    }

    boolean hullGone() {
        return part[HULL] <= 0f;
    }

    void damage(int index, float amount) {
        if (index < 0 || index >= PART_COUNT || amount <= 0f) {
            return;
        }
        part[index] = Math.max(0f, part[index] - amount);
        lastDamaged = index;
        damageFlash = 1.4f;
    }

    void repair(int index) {
        part[index] = partMax(index);
        damageFlash = 0f;
        lastDamaged = -1;
    }

    /** Recalcula la capacidad del deposito tras una reparacion o un cambio de mejora. */
    void syncFuel(Upgrades up) {
        maxFuel = up.maxFuel() * tankFactor();
        if (fuel > maxFuel) {
            fuel = maxFuel;
        }
    }

    /** Recalcula la capacidad de bodega tras comprar la mejora. */
    void syncCargo(Upgrades up) {
        cargo.setCapacity(up.cargoCapacity());
    }

    /** Aplica la mejora de láser conservando la carga relativa del capacitor. */
    void syncLaser(Upgrades up) {
        laser.setLevel(up.laserLevel());
        float oldMax = capacitor.maxEnergy;
        float fraction = oldMax > 0f ? capacitor.energy / oldMax : 1f;
        capacitor.setMaxEnergy(up.capacitorMax());
        capacitor.energy = capacitor.maxEnergy * MathUtils.clamp(fraction, 0f, 1f);
    }

    /** Aplica la mejora del rayo tractor. */
    void syncTractor(Upgrades up) {
        tractor.setLevel(up.tractorLevel());
    }

    /** Ángulo normalizado al intervalo [-180, 180). */
    static float wrapDeg(float a) {
        return ((a % 360f) + 540f) % 360f - 180f;
    }

    boolean canFire() {
        return cooldown <= 0f && fuel >= FUEL_PER_SHOT && part[CANNON] > 0f;
    }

    Missile fire(Upgrades up) {
        float cannon = cannonFactor();
        cooldown = up.fireCooldown() * (1f + 1.6f * (1f - cannon));
        fuel = Math.max(0f, fuel - FUEL_PER_SHOT * up.fuelUse());
        int damage = Math.max(1, Math.round(up.damage() * (0.35f + 0.65f * cannon)));
        return new Missile(pos.x + dir.x * RADIUS, pos.y + dir.y * RADIUS, dir, vel, damage);
    }

    void refuel(float amount) {
        fuel = Math.min(maxFuel, fuel + amount);
    }

    void draw(ShapeRenderer shapes, float time) {
        if (wrecked) {
            return;
        }
        float flicker = invulnerable > 0f && ((int) (invulnerable * 12f) % 2 == 0) ? 0.4f : 1f;
        // Un poco más grande que el radio de colisión: la nave se lee mejor en pantalla
        // y el margen extra perdona al jugador.
        drawBody(shapes, pos.x, pos.y, heading, RADIUS * 1.28f, thrusting, flicker, time,
                part[WING_LEFT], part[WING_RIGHT]);
    }

    /** Brillo del motor y escudo, en la pasada aditiva. */
    private static final Color FLAME = new Color(1f, 0.42f, 0.12f, 1f);

    void drawGlow(SpriteBatch batch, Visuals art, float time) {
        if (wrecked) {
            return;
        }
        float c = MathUtils.cosDeg(heading);
        float s = MathUtils.sinDeg(heading);
        float ex = pos.x - c * RADIUS * 1.32f;
        float ey = pos.y - s * RADIUS * 1.32f;
        if (thrusting) {
            float f = 0.72f + 0.28f * MathUtils.sin(time * 46f);
            art.glow(batch, ex, ey, RADIUS * 3.8f, FLAME, 0.55f * f);
            art.glow(batch, ex, ey, RADIUS * 1.8f, Color.WHITE, 0.70f * f);
        } else {
            art.glow(batch, ex, ey, RADIUS * 1.7f, Visuals.ACCENT, 0.18f);
        }
        if (invulnerable > 0f) {
            float pulse = 0.55f + 0.45f * MathUtils.sin(time * 22f);
            art.glow(batch, pos.x, pos.y, RADIUS * 7.0f, Visuals.ACCENT, 0.16f * pulse);
        }
    }

    /**
     * Dibuja una nave con la paleta nueva. Se usa en la partida y en la vista previa del menú.
     *
     * @param size longitud del morro (en unidades del mundo, o píxeles para el menú).
     */
    static void drawBody(ShapeRenderer s, float x, float y, float headingDeg, float size,
                         boolean thrusting, float alpha, float time) {
        drawBody(s, x, y, headingDeg, size, thrusting, alpha, time, PART_MAX, PART_MAX);
    }

    /**
     * Variante con la integridad de las alas. Si un ala llega a 0 no se dibuja y
     * queda un muñón; con daño parcial se encoge y se oscurece.
     */
    static void drawBody(ShapeRenderer s, float x, float y, float headingDeg, float size,
                         boolean thrusting, float alpha, float time,
                         float wingLeft, float wingRight) {
        float c = MathUtils.cosDeg(headingDeg);
        float sn = MathUtils.sinDeg(headingDeg);
        float leftScale = wingLeft <= 0f ? 0f : 0.55f + 0.45f * wingLeft / PART_MAX;
        float rightScale = wingRight <= 0f ? 0f : 0.55f + 0.45f * wingRight / PART_MAX;

        if (thrusting) {
            float f = 0.72f + 0.28f * MathUtils.sin(time * 46f);
            // Llama exterior naranja y núcleo azul-blanc o.
            tri(s, x, y, c, sn, size, -0.72f, 0.22f, -1.48f, 0f, -0.72f, -0.22f,
                    1f, 0.40f, 0.10f, 0.80f * f * alpha);
            tri(s, x, y, c, sn, size, -0.70f, 0.12f, -1.12f, 0f, -0.70f, -0.12f,
                    0.55f, 0.85f, 1f, 0.90f * f * alpha);
            tri(s, x, y, c, sn, size, -0.68f, 0.05f, -0.96f, 0f, -0.68f, -0.05f,
                    1f, 1f, 1f, 0.95f * f * alpha);
        }

        // Silueta exterior oscura. Los vértices de las alas se encogen con ellas.
        int n = OUTLINE.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float ax = OUTLINE[i * 2];
            float ay = OUTLINE[i * 2 + 1];
            float bx = OUTLINE[j * 2];
            float by = OUTLINE[j * 2 + 1];
            if ((i == 2 || i == 3) && leftScale < 1f) {
                ax = 0.35f + (ax - 0.35f) * leftScale;
                ay = 0.13f + (ay - 0.13f) * leftScale;
            }
            if ((j == 2 || j == 3) && leftScale < 1f) {
                bx = 0.35f + (bx - 0.35f) * leftScale;
                by = 0.13f + (by - 0.13f) * leftScale;
            }
            if ((i == 8 || i == 9) && rightScale < 1f) {
                ax = 0.35f + (ax - 0.35f) * rightScale;
                ay = -0.13f + (ay + 0.13f) * rightScale;
            }
            if ((j == 8 || j == 9) && rightScale < 1f) {
                bx = 0.35f + (bx - 0.35f) * rightScale;
                by = -0.13f + (by + 0.13f) * rightScale;
            }
            tri(s, x, y, c, sn, size * 1.085f, ax, ay, bx, by,
                    0f, 0f, 0.015f, 0.05f, 0.11f, 0.95f * alpha);
        }

        // Casco central.
        tri(s, x, y, c, sn, size, 0.98f, 0f, 0.30f, 0.17f, -0.62f, 0.13f,
                0.82f, 0.94f, 1f, 0.98f * alpha);
        tri(s, x, y, c, sn, size, 0.98f, 0f, -0.62f, 0.13f, -0.62f, -0.13f,
                0.68f, 0.84f, 0.98f, 0.98f * alpha);
        tri(s, x, y, c, sn, size, 0.98f, 0f, -0.62f, -0.13f, 0.30f, -0.17f,
                0.52f, 0.72f, 0.92f, 0.98f * alpha);
        // Espina luminosa.
        tri(s, x, y, c, sn, size, 0.94f, 0f, 0.22f, 0.055f, 0.22f, -0.055f,
                1f, 1f, 1f, 0.75f * alpha);

        // Alas superiores e inferiores.
        float[][] wings = {
                {0.35f, 0.13f, 0.16f, 0.62f, -0.29f, 0.55f, -0.31f, 0.16f},
                {0.35f, -0.13f, 0.16f, -0.62f, -0.29f, -0.55f, -0.31f, -0.16f}
        };
        float[] wingScale = {leftScale, rightScale};
        for (int wi = 0; wi < wings.length; wi++) {
            float[] w = wings[wi];
            float sc = wingScale[wi];
            float rx = w[0];
            float ry = w[1];
            if (sc <= 0f) {
                // Muñón del ala arrancada: sombra y borde caliente.
                float sg = w[1] > 0f ? 1f : -1f;
                tri(s, x, y, c, sn, size, 0.34f, sg * 0.12f, 0.16f, sg * 0.34f,
                        0.10f, sg * 0.05f, 0.18f, 0.15f, 0.18f, 0.95f * alpha);
                tri(s, x, y, c, sn, size, 0.30f, sg * 0.13f, 0.18f, sg * 0.30f,
                        0.14f, sg * 0.12f, 1f, 0.42f, 0.12f, 0.75f * alpha);
                continue;
            }
            float ax = rx;
            float ay = ry;
            float bx = rx + (w[2] - rx) * sc;
            float by = ry + (w[3] - ry) * sc;
            float cx = rx + (w[4] - rx) * sc;
            float cy = ry + (w[5] - ry) * sc;
            float dx = rx + (w[6] - rx) * sc;
            float dy = ry + (w[7] - ry) * sc;
            float ex = rx + (w[0] * 0.62f - rx) * sc;
            float ey = ry + (w[1] * 1.45f - ry) * sc;
            float tx = rx + (w[4] * 1.22f - rx) * sc;
            float ty = ry + (w[5] * 1.15f - ry) * sc;
            float wear = 0.45f + 0.55f * sc;
            tri(s, x, y, c, sn, size, ax, ay, bx, by, cx, cy,
                    0.19f * wear, 0.46f * wear, 0.82f * wear, 0.97f * alpha);
            tri(s, x, y, c, sn, size, ax, ay, cx, cy, dx, dy,
                    0.10f * wear, 0.30f * wear, 0.62f * wear, 0.97f * alpha);
            // Borde de ataque iluminado.
            tri(s, x, y, c, sn, size, ax, ay, bx, by, ex, ey,
                    0.48f * wear, 0.82f * wear, 1f * wear, 0.65f * alpha);
            // Punta cálida.
            tri(s, x, y, c, sn, size, bx, by, cx, cy, tx, ty,
                    1f, 0.58f, 0.20f, 0.90f * alpha);
        }

        // Estabilizador trasero.
        tri(s, x, y, c, sn, size, -0.30f, 0.15f, -0.74f, 0.10f, -0.74f, -0.10f,
                0.14f, 0.24f, 0.48f, 0.98f * alpha);
        tri(s, x, y, c, sn, size, -0.30f, -0.15f, -0.74f, -0.10f, -0.74f, 0.10f,
                0.10f, 0.18f, 0.40f, 0.98f * alpha);

        // Tobera del motor.
        tri(s, x, y, c, sn, size, -0.62f, 0.11f, -0.78f, 0.07f, -0.78f, -0.07f,
                0.05f, 0.09f, 0.16f, alpha);
        tri(s, x, y, c, sn, size, -0.62f, -0.11f, -0.78f, -0.07f, -0.78f, 0.07f,
                0.05f, 0.09f, 0.16f, alpha);

        // Cabina.
        float ccx = x + (0.30f * c - 0f * sn) * size;
        float ccy = y + (0.30f * sn + 0f * c) * size;
        s.setColor(0.10f, 0.75f, 0.95f, 0.98f * alpha);
        s.circle(ccx, ccy, 0.145f * size, 14);
        s.setColor(0.70f, 1f, 1f, 0.95f * alpha);
        s.circle(ccx + 0.035f * size * c, ccy + 0.035f * size * sn, 0.075f * size, 10);
    }

    private static final float[] OUTLINE = {
            1.00f, 0.00f,
            0.42f, 0.18f,
            0.16f, 0.64f,
            -0.30f, 0.58f,
            -0.34f, 0.16f,
            -0.78f, 0.10f,
            -0.78f, -0.10f,
            -0.34f, -0.16f,
            -0.30f, -0.58f,
            0.16f, -0.64f,
            0.42f, -0.18f
    };

    private static void tri(ShapeRenderer s, float x, float y, float c, float sn, float size,
                            float ax, float ay, float bx, float by, float cx, float cy,
                            float r, float g, float b, float a) {
        float x0 = x + (ax * c - ay * sn) * size;
        float y0 = y + (ax * sn + ay * c) * size;
        float x1 = x + (bx * c - by * sn) * size;
        float y1 = y + (bx * sn + by * c) * size;
        float x2 = x + (cx * c - cy * sn) * size;
        float y2 = y + (cx * sn + cy * c) * size;
        s.setColor(r, g, b, a);
        s.triangle(x0, y0, x1, y1, x2, y2);
    }
}

package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.HashSet;
import java.util.Random;

/**
 * Mundo infinito. El espacio se divide en zonas (chunks) de {@link #CHUNK} unidades; cada zona
 * genera sus meteoritos y bidones con una semilla derivada de sus coordenadas, así que siempre sale igual.
 */
class GameWorld {

    static final float CHUNK = 800f;
    static final int RING = 2;
    static final float SAFE_RADIUS = 320f;
    /** Debe superar la distancia máxima de una zona del anillo (~3400 en la esquina), o las zonas nuevas se vaciarían. */
    static final float DESPAWN_DIST = 3600f;
    static final int MAX_ASTEROIDS = 300;
    /** Probabilidad de que una zona tenga un bidón. */
    static final float FUEL_CHANCE = 0.75f;

    final Upgrades up;
    final Ship ship = new Ship();
    /** Referencia al soporte vital de la sesión; opcional en pruebas aisladas. */
    LifeSupport lifeSupport;
    /** Restos de la nave del personaje anterior, en el punto de muerte. */
    boolean legacyWreck;
    final Vector2 legacyWreckPos = new Vector2();
    float legacyWreckHeading;
    float legacyWreckHull;
    final float[] legacyWreckParts = new float[Ship.PART_COUNT];
    final Array<Asteroid> asteroids = new Array<>();
    final Array<Missile> missiles = new Array<>();
    final Array<FuelPickup> fuel = new Array<>();
    final Effects fx = new Effects();
    /** Piloto automático activado por un toque sobre un bidón o un meteorito. */
    final Autopilot pilot = new Autopilot();

    int coinsEarned;
    int score;
    int wave = 1;
    float elapsed;
    /** Magnitud del temblor de cámara, en unidades del mundo. */
    float shake;
    /** Modo de pruebas: la nave no recibe dano al chocar. */
    boolean immortal;
    /** Impactos absorbidos por el modo inmortal (telemetría de pruebas). */
    int ignoredHits;
    private float deathTimer;
    private int deathStage;

    private static final Color SPARK_WARM = new Color(1f, 0.72f, 0.30f, 1f);
    private static final Color SPARK_WHITE = new Color(1f, 1f, 0.90f, 1f);
    private static final Color SPARK_GREEN = new Color(0.40f, 1f, 0.60f, 1f);
    private static final Color SHOCK_GOLD = new Color(1f, 0.85f, 0.45f, 1f);
    private static final Color SHOCK_GREEN = new Color(0.40f, 1f, 0.60f, 1f);
    private static final Color EXPLOSION_ORANGE = new Color(1f, 0.50f, 0.20f, 1f);
    private static final Color TRAIL_ORANGE = new Color(1f, 0.55f, 0.18f, 1f);
    private static final Color WING_SMOKE = new Color(0.38f, 0.38f, 0.44f, 1f);
    private static final Color WING_DEBRIS = new Color(0.70f, 0.70f, 0.75f, 1f);
    private static final Color LEAK_GAS = new Color(0.72f, 0.88f, 1f, 1f);

    private final HashSet<Long> generated = new HashSet<>();
    private final Vector2 tmp = new Vector2();

    /** Zonas ya generadas; lo usa SaveCodec para restaurar el sector sin regenerar. */
    HashSet<Long> generatedChunks() {
        return generated;
    }

    GameWorld(Upgrades up) {
        this.up = up;
    }

    /** La sesión conecta su soporte vital para fugas y avisos visuales. */
    void attachLifeSupport(LifeSupport lifeSupport) {
        this.lifeSupport = lifeSupport;
    }

    void start() {
        pilot.cancel();
        ship.reset(up, 0f, 0f);
        asteroids.clear();
        missiles.clear();
        fuel.clear();
        fx.clear();
        generated.clear();
        coinsEarned = 0;
        score = 0;
        wave = 1;
        elapsed = 0f;
        shake = 0f;
        deathTimer = 0f;
        deathStage = 0;
        ignoredHits = 0;
        legacyWreck = false;
        legacyWreckPos.setZero();
        legacyWreckHeading = 0f;
        legacyWreckHull = 0f;
        updateChunks();
    }

    /** Registra la nave del personaje muerto como resto persistente. */
    void markLegacyWreck(Ship wreck) {
        if (wreck == null) {
            return;
        }
        legacyWreck = true;
        legacyWreckPos.set(wreck.pos);
        legacyWreckHeading = wreck.heading;
        legacyWreckHull = wreck.part[Ship.HULL];
        System.arraycopy(wreck.part, 0, legacyWreckParts, 0, Ship.PART_COUNT);
    }

    /** Copia el estado persistente del mundo y deja la nave del guardado como resto. */
    void copyWorldFrom(GameWorld other) {
        if (other == null) {
            return;
        }
        generated.clear();
        generated.addAll(other.generated);
        asteroids.clear();
        asteroids.addAll(other.asteroids);
        fuel.clear();
        fuel.addAll(other.fuel);
        legacyWreck = true;
        legacyWreckPos.set(other.ship.pos);
        legacyWreckHeading = other.ship.heading;
        legacyWreckHull = other.ship.part[Ship.HULL];
        System.arraycopy(other.ship.part, 0, legacyWreckParts, 0, Ship.PART_COUNT);
    }

    /**
     * Prepara la nave de un personaje nuevo sin reiniciar el mundo: conserva
     * asteroides, bidones, chunks y restos; solo resetea la nave y los contadores.
     */
    void startNewCharacter(float x, float y) {
        pilot.cancel();
        ship.reset(up, x, y);
        missiles.clear();
        fx.clear();
        coinsEarned = 0;
        score = 0;
        elapsed = 0f;
        wave = 1;
        shake = 0f;
        ignoredHits = 0;
        deathTimer = 0f;
        deathStage = 0;
        updateChunks();
    }

    boolean isOver() {
        return !immortal && ship.hullGone();
    }

    /** Inicia la secuencia de destrucción de la nave: explosión, ondas y temblor. */
    void beginDeath() {
        deathTimer = 0f;
        deathStage = 0;
        pilot.cancel();
        fx.clear();
        ship.wrecked = true;
        shake = Math.max(shake, 28f);
        fx.shock(ship.pos.x, ship.pos.y, SHOCK_GOLD, 250f);
        fx.shock(ship.pos.x, ship.pos.y, SPARK_WHITE, 150f);
        fx.burst(ship.pos.x, ship.pos.y, SPARK_WHITE, 34, 430f);
        fx.burst(ship.pos.x, ship.pos.y, EXPLOSION_ORANGE, 46, 330f);
        fx.burst(ship.pos.x, ship.pos.y, new Color(0.95f, 0.20f, 0.12f, 1f), 28, 260f);
    }

    /** Avanza solo los efectos de la explosión final; el resto del mundo queda congelado. */
    void updateDeath(float dt) {
        deathTimer += dt;
        fx.update(dt);
        shake = Math.max(0f, shake - dt * 26f);
        if (deathStage == 0 && deathTimer > 0.20f) {
            deathStage = 1;
            float ox = ship.pos.x + MathUtils.random(-16f, 16f);
            float oy = ship.pos.y + MathUtils.random(-16f, 16f);
            fx.shock(ox, oy, EXPLOSION_ORANGE, 170f);
            fx.burst(ox, oy, EXPLOSION_ORANGE, 26, 260f);
            fx.burst(ox, oy, SPARK_WARM, 16, 220f);
            shake = Math.max(shake, 16f);
        }
        if (deathStage == 1 && deathTimer > 0.55f) {
            deathStage = 2;
            float ox = ship.pos.x + MathUtils.random(-24f, 24f);
            float oy = ship.pos.y + MathUtils.random(-24f, 24f);
            fx.shock(ox, oy, SHOCK_GOLD, 120f);
            fx.burst(ox, oy, EXPLOSION_ORANGE, 18, 190f);
            fx.burst(ox, oy, SPARK_WHITE, 10, 160f);
            shake = Math.max(shake, 9f);
        }
        if (deathStage == 2 && deathTimer > 0.95f) {
            deathStage = 3;
            float ox = ship.pos.x + MathUtils.random(-30f, 30f);
            float oy = ship.pos.y + MathUtils.random(-30f, 30f);
            fx.shock(ox, oy, EXPLOSION_ORANGE, 95f);
            fx.burst(ox, oy, EXPLOSION_ORANGE, 14, 160f);
            fx.burst(ox, oy, SPARK_WARM, 10, 130f);
            shake = Math.max(shake, 6f);
        }
        if (deathStage == 3 && deathTimer > 1.35f) {
            deathStage = 4;
            fx.burst(ship.pos.x, ship.pos.y, new Color(0.75f, 0.35f, 0.18f, 1f), 10, 100f);
        }
    }

    /** Estado de la partida en texto (nave, partes, contadores, zonas, meteoritos y bidones). */
    String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("ship ").append(ship.pos.x).append(' ').append(ship.pos.y).append(' ')
                .append(ship.vel.x).append(' ').append(ship.vel.y).append(' ')
                .append(ship.heading).append(' ').append(ship.fuel).append(' ')
                .append(ship.part[Ship.HULL]).append(' ')
                .append(ship.invulnerable).append('\n');
        sb.append("parts");
        for (int i = 0; i < Ship.PART_COUNT; i++) {
            sb.append(' ').append(ship.part[i]);
        }
        sb.append('\n');
        sb.append("world ").append(coinsEarned).append(' ').append(score).append(' ')
                .append(elapsed).append('\n');
        sb.append("chunks");
        for (Long k : generated) {
            sb.append(' ').append(k);
        }
        sb.append('\n');
        for (int i = 0; i < asteroids.size; i++) {
            Asteroid a = asteroids.get(i);
            sb.append("a ").append(a.pos.x).append(' ').append(a.pos.y).append(' ')
                    .append(a.vel.x).append(' ').append(a.vel.y).append(' ')
                    .append(a.size).append(' ').append(a.hp).append('\n');
        }
        for (int i = 0; i < fuel.size; i++) {
            FuelPickup f = fuel.get(i);
            sb.append("f ").append(f.pos.x).append(' ').append(f.pos.y).append(' ')
                    .append(f.life()).append('\n');
        }
        return sb.toString();
    }

    /** Restaura una partida guardada con {@link #serialize()}. Devuelve false si el texto no es válido. */
    boolean restore(String text) {
        try {
            float x = 0f, y = 0f, vx = 0f, vy = 0f, heading = 90f, fuelLeft = 0f;
            float hull = -1f, invuln = 0f;
            int coins = 0, pts = 0;
            float time = 0f;
            float[] parts = new float[Ship.PART_COUNT];
            for (int i = 0; i < parts.length; i++) {
                parts[i] = i == Ship.HULL ? -1f : Ship.PART_MAX;
            }
            HashSet<Long> chunks = new HashSet<>();
            Array<Asteroid> rocks = new Array<>();
            Array<FuelPickup> cells = new Array<>();

            for (String line : text.split("\n")) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] p = line.split(" ");
                if (p[0].equals("ship")) {
                    x = Float.parseFloat(p[1]);
                    y = Float.parseFloat(p[2]);
                    vx = Float.parseFloat(p[3]);
                    vy = Float.parseFloat(p[4]);
                    heading = Float.parseFloat(p[5]);
                    fuelLeft = Float.parseFloat(p[6]);
                    hull = Float.parseFloat(p[7]);
                    invuln = Float.parseFloat(p[8]);
                } else if (p[0].equals("parts")) {
                    for (int i = 0; i < Ship.PART_COUNT && i + 1 < p.length; i++) {
                        parts[i] = Float.parseFloat(p[i + 1]);
                    }
                } else if (p[0].equals("world")) {
                    coins = Integer.parseInt(p[1]);
                    pts = Integer.parseInt(p[2]);
                    time = Float.parseFloat(p[3]);
                } else if (p[0].equals("chunks")) {
                    for (int i = 1; i < p.length; i++) {
                        chunks.add(Long.parseLong(p[i]));
                    }
                } else if (p[0].equals("a")) {
                    Asteroid a = new Asteroid(Float.parseFloat(p[1]), Float.parseFloat(p[2]),
                            Integer.parseInt(p[5]));
                    a.vel.set(Float.parseFloat(p[3]), Float.parseFloat(p[4]));
                    a.hp = Integer.parseInt(p[6]);
                    rocks.add(a);
                } else if (p[0].equals("f")) {
                    cells.add(new FuelPickup(Float.parseFloat(p[1]), Float.parseFloat(p[2]),
                            Float.parseFloat(p[3])));
                }
            }
            if (hull < 0f) {
                return false;
            }

            // Solo se modifica el mundo cuando todo el texto se ha leído bien.
            ship.reset(up, x, y);
            ship.vel.set(vx, vy);
            ship.heading = heading;
            ship.dir.set(MathUtils.cosDeg(heading), MathUtils.sinDeg(heading));
            ship.part[Ship.HULL] = Math.min(hull, ship.maxHull);
            for (int i = 1; i < Ship.PART_COUNT; i++) {
                ship.part[i] = MathUtils.clamp(parts[i], 0f, Ship.PART_MAX);
            }
            ship.syncFuel(up);
            ship.fuel = Math.min(fuelLeft, ship.maxFuel);
            ship.invulnerable = invuln;
            coinsEarned = coins;
            score = pts;
            elapsed = time;
            wave = 1 + (int) (elapsed / 45f);
            generated.clear();
            generated.addAll(chunks);
            asteroids.clear();
            asteroids.addAll(rocks);
            fuel.clear();
            fuel.addAll(cells);
            missiles.clear();
            fx.clear();
            pilot.cancel();
            shake = 0f;
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Avanza la simulación con los controles de un fotograma. */
    void update(float dt, Controls c) {
        elapsed += dt;
        wave = 1 + (int) (elapsed / 45f);

        if (pilot.active()) {
            pilot.steer(c, this, dt);
        }
        ship.update(dt, up, c);
        if (lifeSupport != null) {
            lifeSupport.updateLeak(ship);
        }
        emitLeak(dt);
        emitWingDamage(dt);
        if (c.fire && ship.canFire()) {
            missiles.add(ship.fire(up));
        }
        if (ship.thrusting) {
            float ex = ship.pos.x - ship.dir.x * Ship.RADIUS * 1.32f;
            float ey = ship.pos.y - ship.dir.y * Ship.RADIUS * 1.32f;
            fx.trail(ex, ey, -ship.dir.x * 40f + ship.vel.x * 0.15f,
                    -ship.dir.y * 40f + ship.vel.y * 0.15f, TRAIL_ORANGE, 3.4f, 0.36f);
        }
        shake = Math.max(0f, shake - dt * 42f);

        updateMissiles(dt);
        updateAsteroids(dt);
        updateFuel(dt);
        collideShipAsteroids();
        collideShipFuel();
        collideMissilesAsteroids();
        fx.update(dt);
        updateChunks();
    }

    /** Versión con teclas y botones: izquierda/derecha giran, acelerar a plena potencia. */
    void update(float dt, boolean left, boolean right, boolean thrust, boolean fire) {
        Controls c = new Controls();
        c.turn = left ? 1f : right ? -1f : 0f;
        c.thrust = thrust ? 1f : 0f;
        c.fire = fire;
        update(dt, c);
    }

    private void updateMissiles(float dt) {
        for (int i = missiles.size - 1; i >= 0; i--) {
            Missile m = missiles.get(i);
            if (!m.update(dt)) {
                missiles.removeIndex(i);
            } else {
                fx.trail(m.pos.x, m.pos.y, -m.vel.x * 0.07f, -m.vel.y * 0.07f,
                        TRAIL_ORANGE, 2.4f, 0.28f);
            }
        }
    }

    private void updateAsteroids(float dt) {
        for (int i = asteroids.size - 1; i >= 0; i--) {
            Asteroid a = asteroids.get(i);
            a.update(dt);
            if (a.pos.dst(ship.pos) > DESPAWN_DIST) {
                asteroids.removeIndex(i);
            }
        }
    }

    private void updateFuel(float dt) {
        for (int i = fuel.size - 1; i >= 0; i--) {
            if (!fuel.get(i).update(dt)) {
                fuel.removeIndex(i);
            }
        }
    }

    private void collideShipAsteroids() {
        if (immortal) {
            // Modo de pruebas: los impactos no hacen daño; se consume la roca para
            // que la nave no se quede atascada dentro de ella.
            for (int i = asteroids.size - 1; i >= 0; i--) {
                Asteroid a = asteroids.get(i);
                if (a.hit(ship.pos, Ship.RADIUS)) {
                    ignoredHits++;
                    fx.burst(a.pos.x, a.pos.y, SPARK_WARM, 8, 150f);
                    asteroids.removeIndex(i);
                    return;
                }
            }
            return;
        }
        if (ship.invulnerable > 0f) {
            return;
        }
        for (int i = asteroids.size - 1; i >= 0; i--) {
            Asteroid a = asteroids.get(i);
            if (a.hit(ship.pos, Ship.RADIUS)) {
                damageShip(a);
                ship.invulnerable = 1.5f;
                shake = Math.max(shake, 20f);
                fx.burst(ship.pos.x, ship.pos.y, EXPLOSION_ORANGE, 22, 340f);
                fx.burst(ship.pos.x, ship.pos.y, SPARK_WHITE, 8, 220f);
                fx.shock(ship.pos.x, ship.pos.y, SHOCK_GOLD, 130f);
                fx.burst(a.pos.x, a.pos.y, new Color(0.8f, 0.8f, 0.8f, 1f), 14, 220f);
                asteroids.removeIndex(i);
                return;
            }
        }
    }

    /**
     * Un impacto siempre muerde el casco y ademas dana un sistema al azar.
     * El casco a cero destruye la nave; los sistemas a cero aplican su penalizacion.
     */
    private void damageShip(Asteroid a) {
        float base = a.size == Asteroid.SMALL ? 14f : a.size == Asteroid.MEDIUM ? 24f : 36f;
        ship.damage(Ship.HULL, base * MathUtils.random(0.85f, 1.20f));
        if (lifeSupport != null) {
            lifeSupport.onHullDamage(ship, base);
        }
        int system = Ship.ENGINE + MathUtils.random(0, Ship.PART_COUNT - 2);
        boolean wasOk = ship.part[system] > 0f;
        ship.damage(system, base * MathUtils.random(0.70f, 1.30f));
        if (wasOk && ship.part[system] <= 0f) {
            // Aviso extra cuando un sistema queda fuera de servicio.
            if (system == Ship.WING_LEFT || system == Ship.WING_RIGHT) {
                fx.burst(ship.pos.x, ship.pos.y, WING_DEBRIS, 18, 260f);
                fx.burst(ship.pos.x, ship.pos.y, EXPLOSION_ORANGE, 10, 180f);
            }
            fx.burst(ship.pos.x, ship.pos.y, new Color(1f, 0.32f, 0.42f, 1f), 16, 240f);
            fx.shock(ship.pos.x, ship.pos.y, SHOCK_GOLD, 90f);
        }
    }

    /** Gas escapando del casco dañado; más denso cuando hay brecha crítica. */
    private void emitLeak(float dt) {
        if (lifeSupport == null || lifeSupport.leak <= 0.01f) {
            return;
        }
        float rate = (lifeSupport.breach ? 9f : 4f) + lifeSupport.leak * 0.8f;
        if (MathUtils.random() > Math.min(1f, dt * rate)) {
            return;
        }
        float angle = MathUtils.random(360f);
        float ox = MathUtils.cosDeg(angle) * Ship.RADIUS * 0.9f;
        float oy = MathUtils.sinDeg(angle) * Ship.RADIUS * 0.9f;
        fx.trail(ship.pos.x + ox, ship.pos.y + oy,
                -ship.dir.x * 24f + ox * 1.8f, -ship.dir.y * 24f + oy * 1.8f,
                LEAK_GAS, lifeSupport.breach ? 3.2f : 2.4f,
                lifeSupport.breach ? 0.55f : 0.38f);
    }

    /** Humo y chispas desde el muñón de cada ala perdida. */
    private void emitWingDamage(float dt) {        if (ship.part[Ship.WING_LEFT] > 0f && ship.part[Ship.WING_RIGHT] > 0f) {
            return;
        }
        float size = Ship.RADIUS * 1.28f;
        float c = MathUtils.cosDeg(ship.heading);
        float sn = MathUtils.sinDeg(ship.heading);
        if (ship.part[Ship.WING_LEFT] <= 0f) {
            emitWingSmoke(0.42f, c, sn, size, dt);
        }
        if (ship.part[Ship.WING_RIGHT] <= 0f) {
            emitWingSmoke(-0.42f, c, sn, size, dt);
        }
    }

    private void emitWingSmoke(float lateral, float c, float sn, float size, float dt) {
        if (MathUtils.random() > dt * 13f) {
            return;
        }
        float wx = ship.pos.x - lateral * sn * size;
        float wy = ship.pos.y + lateral * c * size;
        float vx = -ship.dir.x * 35f + ship.vel.x * 0.20f;
        float vy = -ship.dir.y * 35f + ship.vel.y * 0.20f;
        fx.trail(wx, wy, vx, vy, WING_SMOKE, 3.2f, 0.55f);
        if (MathUtils.random() < 0.35f) {
            fx.trail(wx, wy, vx * 1.2f, vy * 1.2f, SPARK_WARM, 2.2f, 0.30f);
        }
    }

    private void collideShipFuel() {
        for (int i = fuel.size - 1; i >= 0; i--) {
            FuelPickup f = fuel.get(i);
            if (f.pos.dst(ship.pos) < FuelPickup.RADIUS + Ship.RADIUS + FuelPickup.CAPTURE_PAD) {
                ship.refuel(FuelPickup.AMOUNT);
                fx.burst(f.pos.x, f.pos.y, SPARK_GREEN, 14, 170f);
                fx.burst(f.pos.x, f.pos.y, SPARK_WHITE, 6, 110f);
                fx.shock(f.pos.x, f.pos.y, SHOCK_GREEN, 70f);
                fuel.removeIndex(i);
            }
        }
    }

    private void collideMissilesAsteroids() {
        for (int m = missiles.size - 1; m >= 0; m--) {
            Missile miss = missiles.get(m);
            for (int i = asteroids.size - 1; i >= 0; i--) {
                Asteroid a = asteroids.get(i);
                if (!a.hit(miss.pos, Missile.RADIUS)) {
                    continue;
                }
                missiles.removeIndex(m);
                a.hp -= miss.damage;
                a.flash = 0.08f;
                fx.burst(miss.pos.x, miss.pos.y, SPARK_WARM, 5, 140f);
                if (a.hp <= 0) {
                    destroyAsteroid(i);
                }
                break;
            }
        }
    }

    private void destroyAsteroid(int index) {
        Asteroid a = asteroids.get(index);
        coinsEarned += a.reward;
        score += a.reward * 10;
        shake = Math.max(shake, a.size == Asteroid.LARGE ? 12f : a.size == Asteroid.MEDIUM ? 7f : 4f);
        fx.burst(a.pos.x, a.pos.y, SPARK_WARM, 20, 280f);
        fx.burst(a.pos.x, a.pos.y, EXPLOSION_ORANGE, 10, 190f);
        fx.burst(a.pos.x, a.pos.y, SPARK_WHITE, 6, 150f);
        fx.shock(a.pos.x, a.pos.y, SHOCK_GOLD, a.radius * 5.5f);
        asteroids.removeIndex(index);
        if (a.size == Asteroid.LARGE) {
            splitAsteroid(a, Asteroid.MEDIUM);
        } else if (a.size == Asteroid.MEDIUM) {
            splitAsteroid(a, Asteroid.SMALL);
        }
    }

    private void splitAsteroid(Asteroid parent, int size) {
        for (int i = 0; i < 2; i++) {
            Asteroid child = new Asteroid(parent.pos.x, parent.pos.y, size);
            child.vel.add(parent.vel).scl(1.2f);
            asteroids.add(child);
        }
    }

    // ---------- generación procedural ----------

    /** Genera una vez las zonas alrededor de la nave. Una zona visitada no se vuelve a generar. */
    private void updateChunks() {
        int cx = chunkOf(ship.pos.x);
        int cy = chunkOf(ship.pos.y);
        for (int dx = -RING; dx <= RING; dx++) {
            for (int dy = -RING; dy <= RING; dy++) {
                if (generated.add(key(cx + dx, cy + dy))) {
                    generateChunk(cx + dx, cy + dy);
                }
            }
        }
    }

    private void generateChunk(int cx, int cy) {
        Random rnd = new Random(cx * 73856093L ^ cy * 19349663L);
        int count = 2 + rnd.nextInt(3) + Math.min(wave / 2, 2);
        for (int i = 0; i < count && asteroids.size < MAX_ASTEROIDS; i++) {
            float x = (cx + rnd.nextFloat()) * CHUNK;
            float y = (cy + rnd.nextFloat()) * CHUNK;
            if (x * x + y * y < SAFE_RADIUS * SAFE_RADIUS) {
                continue;
            }
            int roll = rnd.nextInt(100);
            int size = roll < 50 ? Asteroid.SMALL : roll < 85 ? Asteroid.MEDIUM : Asteroid.LARGE;
            asteroids.add(new Asteroid(x, y, size));
        }
        if (rnd.nextFloat() < FUEL_CHANCE) {
            float x = (cx + rnd.nextFloat()) * CHUNK;
            float y = (cy + rnd.nextFloat()) * CHUNK;
            fuel.add(new FuelPickup(x, y));
        }
    }

    private static int chunkOf(float v) {
        return (int) Math.floor(v / CHUNK);
    }

    private static long key(int cx, int cy) {
        return ((long) cx << 32) | (cy & 0xffffffffL);
    }

    /** Dibuja el mundo visible con la cámara ya posicionada. */
    void draw(ShapeRenderer shapes) {
        for (int i = 0; i < fuel.size; i++) {
            fuel.get(i).draw(shapes);
        }
        for (int i = 0; i < asteroids.size; i++) {
            asteroids.get(i).draw(shapes);
        }
        for (int i = 0; i < missiles.size; i++) {
            missiles.get(i).draw(shapes);
        }
        fx.draw(shapes);
        drawLegacyWreck(shapes);
        pilot.draw(shapes, ship, elapsed);
        ship.draw(shapes, elapsed);
    }

    private void drawLegacyWreck(ShapeRenderer shapes) {
        if (!legacyWreck) {
            return;
        }
        shapes.setColor(0.55f, 0.18f, 0.22f, 0.28f);
        shapes.circle(legacyWreckPos.x, legacyWreckPos.y, Ship.RADIUS * 2.2f, 20);
        Ship.drawBody(shapes, legacyWreckPos.x, legacyWreckPos.y, legacyWreckHeading,
                Ship.RADIUS * 1.15f, false, 0.55f, 0f,
                legacyWreckParts[Ship.WING_LEFT], legacyWreckParts[Ship.WING_RIGHT]);
    }

    /** Brillos aditivos del mundo: motores, impactos, objetivos y partículas. */
    void drawGlow(SpriteBatch batch, Visuals art) {
        for (int i = 0; i < fuel.size; i++) {
            fuel.get(i).drawGlow(batch, art);
        }
        for (int i = 0; i < asteroids.size; i++) {
            asteroids.get(i).drawGlow(batch, art);
        }
        for (int i = 0; i < missiles.size; i++) {
            missiles.get(i).drawGlow(batch, art);
        }
        pilot.drawGlow(batch, art, ship, elapsed);
        ship.drawGlow(batch, art, elapsed);
        fx.drawGlow(batch, art);
    }
}

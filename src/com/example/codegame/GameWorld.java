package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
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
    private final HashSet<Long> generated = new HashSet<>();
    private final Vector2 tmp = new Vector2();

    GameWorld(Upgrades up) {
        this.up = up;
    }

    void start() {
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
        updateChunks();
    }

    boolean isOver() {
        return ship.lives <= 0;
    }

    /** Estado de la partida en texto (nave, contadores, zonas visitadas, meteoritos y bidones). */
    String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("ship ").append(ship.pos.x).append(' ').append(ship.pos.y).append(' ')
                .append(ship.vel.x).append(' ').append(ship.vel.y).append(' ')
                .append(ship.heading).append(' ').append(ship.fuel).append(' ')
                .append(ship.lives).append(' ').append(ship.maxLives).append(' ')
                .append(ship.invulnerable).append('\n');
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
            String[] lines = text.split("\n");
            String[] sh = lines[0].split(" ");
            String[] wd = lines[1].split(" ");
            String[] ch = lines[2].split(" ");
            float x = Float.parseFloat(sh[1]);
            float y = Float.parseFloat(sh[2]);
            float vx = Float.parseFloat(sh[3]);
            float vy = Float.parseFloat(sh[4]);
            float heading = Float.parseFloat(sh[5]);
            float fuelLeft = Float.parseFloat(sh[6]);
            int lives = Integer.parseInt(sh[7]);
            int maxLives = Integer.parseInt(sh[8]);
            float invuln = Float.parseFloat(sh[9]);
            int coins = Integer.parseInt(wd[1]);
            int pts = Integer.parseInt(wd[2]);
            float time = Float.parseFloat(wd[3]);
            HashSet<Long> chunks = new HashSet<>();
            for (int i = 1; i < ch.length; i++) {
                chunks.add(Long.parseLong(ch[i]));
            }
            Array<Asteroid> rocks = new Array<>();
            Array<FuelPickup> cells = new Array<>();
            for (int i = 3; i < lines.length; i++) {
                String[] p = lines[i].split(" ");
                if (p[0].equals("a")) {
                    Asteroid a = new Asteroid(Float.parseFloat(p[1]), Float.parseFloat(p[2]), Integer.parseInt(p[5]));
                    a.vel.set(Float.parseFloat(p[3]), Float.parseFloat(p[4]));
                    a.hp = Integer.parseInt(p[6]);
                    rocks.add(a);
                } else if (p[0].equals("f")) {
                    cells.add(new FuelPickup(Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])));
                }
            }

            // Solo se modifica el mundo cuando todo el texto se ha leído bien.
            ship.reset(up, x, y);
            ship.vel.set(vx, vy);
            ship.heading = heading;
            ship.dir.set(MathUtils.cosDeg(heading), MathUtils.sinDeg(heading));
            ship.fuel = fuelLeft;
            ship.lives = lives;
            ship.maxLives = maxLives;
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
        if (c.fire && ship.canFire()) {
            missiles.add(ship.fire(up));
        }

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
            if (!missiles.get(i).update(dt)) {
                missiles.removeIndex(i);
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
        if (ship.invulnerable > 0f) {
            return;
        }
        for (int i = asteroids.size - 1; i >= 0; i--) {
            Asteroid a = asteroids.get(i);
            if (a.hit(ship.pos, Ship.RADIUS)) {
                ship.lives--;
                ship.invulnerable = 1.5f;
                fx.burst(ship.pos.x, ship.pos.y, new Color(1f, 0.5f, 0.3f, 1f), 20, 320f);
                fx.burst(a.pos.x, a.pos.y, new Color(0.8f, 0.8f, 0.8f, 1f), 14, 220f);
                asteroids.removeIndex(i);
                return;
            }
        }
    }

    private void collideShipFuel() {
        for (int i = fuel.size - 1; i >= 0; i--) {
            FuelPickup f = fuel.get(i);
            if (f.pos.dst(ship.pos) < FuelPickup.RADIUS + Ship.RADIUS) {
                ship.refuel(FuelPickup.AMOUNT);
                fx.burst(f.pos.x, f.pos.y, new Color(0.4f, 1f, 0.6f, 1f), 10, 150f);
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
                fx.burst(miss.pos.x, miss.pos.y, new Color(1f, 0.8f, 0.4f, 1f), 4, 120f);
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
        fx.burst(a.pos.x, a.pos.y, new Color(1f, 0.85f, 0.3f, 1f), 18, 260f);
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
        int count = 1 + rnd.nextInt(3) + Math.min(wave / 2, 2);
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
        pilot.draw(shapes);
        ship.draw(shapes);
    }
}

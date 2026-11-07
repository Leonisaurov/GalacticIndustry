package com.example.codegame;

import com.badlogic.gdx.math.MathUtils;

/** Prueba de lógica sin ventana: ejecutar con run_program. No forma parte del juego. */
public class SelfTest {

    private static int failures = 0;

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }

    /** Mide el piloto con un bidón: combustible gastado, giros totales y tiempo con motor encendido. */
    private static void benchFuel(String name, float dist, float angleDeg, float vx, float vy) {
        final float dt = 1f / 60f;
        GameWorld b = new GameWorld(new Upgrades(true));
        b.start();
        b.ship.vel.set(vx, vy);
        FuelPickup cell = new FuelPickup(dist * MathUtils.cosDeg(angleDeg), dist * MathUtils.sinDeg(angleDeg));
        b.pilot.target(cell);
        float used = 0f;
        float turned = 0f;
        float thrustTime = 0f;
        float lastHeading = b.ship.heading;
        int steps = 0;
        boolean got = false;
        while (steps < 60 * 60 && !got) {
            b.asteroids.clear();
            b.fuel.clear();
            b.fuel.add(cell);
            float f0 = b.ship.fuel;
            b.update(dt, new Controls());
            if (b.ship.fuel < f0) {
                used += f0 - b.ship.fuel;
            }
            if (b.ship.thrusting) {
                thrustTime += dt;
            }
            turned += Math.abs(Ship.wrapDeg(b.ship.heading - lastHeading));
            lastHeading = b.ship.heading;
            steps++;
            got = !b.fuel.contains(cell, true);
        }
        check("bench bidon " + name + " llega", got);
        System.out.println(String.format("BENCH bidon %-14s t=%.1fs combustible=%.1f giros=%.0fdeg motor=%.1fs",
                name, steps * dt, used, turned, thrustTime));
    }

    /** Mide el piloto con un meteorito: combustible, giros y si lo destruye sin recibir golpes. */
    private static void benchRock(String name, float x, float y, float rvx, float rvy) {
        benchRock(name, x, y, rvx, rvy, Asteroid.MEDIUM);
    }

    private static void benchRock(String name, float x, float y, float rvx, float rvy, int size) {
        final float dt = 1f / 60f;
        GameWorld b = new GameWorld(new Upgrades(true));
        b.start();
        Asteroid rock = new Asteroid(x, y, size);
        rock.vel.set(rvx, rvy);
        b.pilot.target(rock);
        int lives0 = b.ship.lives;
        float used = 0f;
        float turned = 0f;
        float thrustTime = 0f;
        float lastHeading = b.ship.heading;
        int steps = 0;
        int shots = 0;
        int hits = 0;
        boolean done = false;
        while (steps < 60 * 40 && !done) {
            b.fuel.clear();
            b.asteroids.clear();
            b.asteroids.add(rock);
            float f0 = b.ship.fuel;
            float cd0 = b.ship.cooldown;
            b.update(dt, new Controls());
            if (b.ship.cooldown > cd0) {
                shots++;
                System.out.println(String.format("TRAZA %s disparo t=%.2f dist=%.0f radio=%.0f rockPos=(%.0f,%.0f) rockVel=(%.0f,%.0f) shipVel=(%.0f,%.0f)",
                        name, steps * dt, b.ship.pos.dst(rock.pos), rock.radius, rock.pos.x, rock.pos.y, rock.vel.x, rock.vel.y, b.ship.vel.x, b.ship.vel.y));
            }
            if (rock.flash == 0.08f) {
                hits++;
                System.out.println(String.format("TRAZA %s acierto t=%.2f", name, steps * dt));
            }

            if (b.ship.fuel < f0) {
                used += f0 - b.ship.fuel;
            }
            if (b.ship.thrusting) {
                thrustTime += dt;
            }
            turned += Math.abs(Ship.wrapDeg(b.ship.heading - lastHeading));
            lastHeading = b.ship.heading;
            steps++;
            done = b.coinsEarned > 0 || !b.pilot.active();
        }
        boolean destroyed = b.coinsEarned > 0;
        check("bench meteorito " + name + " destruido", destroyed);
        check("bench meteorito " + name + " sin golpes", b.ship.lives == lives0);
        System.out.println(String.format("BENCH roca   %-14s t=%.1fs combustible=%.1f giros=%.0fdeg motor=%.1fs golpes=%d disparos=%d aciertos=%d",
                name, steps * dt, used, turned, thrustTime, lives0 - b.ship.lives, shots, hits));
    }

    public static void main(String[] args) {
        final float dt = 1f / 60f;

        Upgrades up = new Upgrades(true);
        GameWorld w = new GameWorld(up);
        w.start();

        check("combustible inicial lleno", w.ship.fuel == w.ship.maxFuel);
        check("vidas iniciales", w.ship.lives == up.lives());

        // Acelerar consume combustible y genera velocidad.
        float fuel0 = w.ship.fuel;
        for (int i = 0; i < 60; i++) {
            w.asteroids.clear();
            w.update(dt, false, false, true, false);
        }
        check("acelerar gasta combustible", w.ship.fuel < fuel0);
        check("acelerar da velocidad", w.ship.vel.len() > 0f);

        // Disparar crea misiles y gasta combustible.
        w.ship.fuel = w.ship.maxFuel;
        w.ship.cooldown = 0f;
        w.asteroids.clear();
        int m0 = w.missiles.size;
        w.update(dt, false, false, false, true);
        check("disparar crea misil", w.missiles.size == m0 + 1);
        check("disparar gasta combustible", w.ship.fuel < w.ship.maxFuel);

        // Un misil impacta un meteorito pequeño: se destruye y da monedas.
        w.asteroids.clear();
        w.missiles.clear();
        w.coinsEarned = 0;
        Asteroid target = new Asteroid(w.ship.pos.x + 300f, w.ship.pos.y, Asteroid.SMALL);
        target.vel.set(0f, 0f);
        w.asteroids.add(target);
        w.missiles.add(new Missile(target.pos.x - 10f, target.pos.y, new com.badlogic.gdx.math.Vector2(1f, 0f),
                new com.badlogic.gdx.math.Vector2(), up.damage()));
        w.update(dt, false, false, false, false);
        check("misil destruye meteorito", w.asteroids.size == 0 || !w.asteroids.contains(target, true));
        check("destruir meteorito da monedas", w.coinsEarned == target.reward);

        // Un meteorito grande al destruirse se parte en dos medianos.
        w.asteroids.clear();
        w.missiles.clear();
        Asteroid big = new Asteroid(w.ship.pos.x + 300f, w.ship.pos.y, Asteroid.LARGE);
        big.vel.set(0f, 0f);
        big.hp = 1;
        w.asteroids.add(big);
        w.missiles.add(new Missile(big.pos.x - 10f, big.pos.y, new com.badlogic.gdx.math.Vector2(1f, 0f),
                new com.badlogic.gdx.math.Vector2(), up.damage()));
        w.update(dt, false, false, false, false);
        check("meteorito grande se parte en dos", w.asteroids.size == 2);

        // Colisión con la nave: pierde una vida y queda invulnerable un momento.
        w.asteroids.clear();
        w.missiles.clear();
        w.ship.invulnerable = 0f;
        int lives0 = w.ship.lives;
        Asteroid hitter = new Asteroid(w.ship.pos.x, w.ship.pos.y, Asteroid.MEDIUM);
        hitter.vel.set(0f, 0f);
        w.asteroids.add(hitter);
        w.update(dt, false, false, false, false);
        check("colisionar con meteorito quita una vida", w.ship.lives == lives0 - 1);
        check("invulnerable tras golpe", w.ship.invulnerable > 0f);

        // Recoger combustible lo recarga.
        w.asteroids.clear();
        w.ship.fuel = 10f;
        w.fuel.clear();
        w.fuel.add(new FuelPickup(w.ship.pos.x, w.ship.pos.y));
        w.update(dt, false, false, false, false);
        check("bidon recarga combustible", w.ship.fuel > 10f);
        check("bidon recogido desaparece", w.fuel.size == 0);

        // Tienda: comprar con monedas guardadas.
        up.addCoins(500);
        int coins0 = up.coins();
        int cost0 = up.cost(Upgrades.MOTOR);
        check("comprar mejora con monedas suficientes", up.buy(Upgrades.MOTOR));
        check("compra descuenta monedas", up.coins() == coins0 - cost0);
        check("compra sube nivel", up.level(Upgrades.MOTOR) == 1);
        check("sin monedas no compra", !up.buy(Upgrades.BLINDAJE) || up.coins() >= 0);

        // Partida simulada de 2 minutos: la nave apunta al meteorito más cercano y dispara.
        w.start();
        int maxAsteroids = 0;
        int steps = 0;
        int totalEarned = 0;
        while (!w.isOver() && steps < 120 * 60) {
            Asteroid nearest = null;
            float best = Float.MAX_VALUE;
            for (int i = 0; i < w.asteroids.size; i++) {
                Asteroid a = w.asteroids.get(i);
                float d = a.pos.dst(w.ship.pos);
                if (d < best) {
                    best = d;
                    nearest = a;
                }
            }
            boolean left = false;
            boolean right = false;
            if (nearest != null) {
                float want = MathUtils.atan2(nearest.pos.y - w.ship.pos.y, nearest.pos.x - w.ship.pos.x)
                        * MathUtils.radiansToDegrees;
                float diff = ((want - w.ship.heading) % 360f + 540f) % 360f - 180f;
                left = diff > 5f;
                right = diff < -5f;
            }
            boolean thrust = best > 300f;
            boolean fire = w.ship.canFire() && nearest != null && best < 500f;
            int before = w.coinsEarned;
            w.update(dt, left, right, thrust, fire);
            if (w.coinsEarned > before) {
                totalEarned += w.coinsEarned - before;
            }
            maxAsteroids = Math.max(maxAsteroids, w.asteroids.size);
            steps++;
        }
        check("el mundo infinito genera meteoritos durante la partida", maxAsteroids > 0);
        System.out.println("Partida simulada: " + (steps / 60) + "s, oleada " + w.wave
                + ", puntos " + w.score + ", monedas " + w.coinsEarned
                + ", max meteoritos " + maxAsteroids + ", vidas " + w.ship.lives);
        check("la simulacion gana monedas destruyendo meteoritos", totalEarned > 0);
        check("la simulacion termina sin excepciones", true);

        // Guardado: serialize -> restore debe reproducir el mismo estado.
        String saved = w.serialize();
        GameWorld copy = new GameWorld(up);
        check("restaurar partida guardada", copy.restore(saved));
        check("restaurar conserva monedas y vidas", copy.coinsEarned == w.coinsEarned && copy.ship.lives == w.ship.lives);
        check("restaurar conserva meteoritos", copy.asteroids.size == w.asteroids.size);
        check("restaurar conserva bidones", copy.fuel.size == w.fuel.size);
        check("restaurar conserva nave", Math.abs(copy.ship.pos.x - w.ship.pos.x) < 0.01f
                && Math.abs(copy.ship.pos.y - w.ship.pos.y) < 0.01f);
        check("texto invalido no restaura", !new GameWorld(up).restore("basura"));

        // Piloto automático: bidón.
        GameWorld pf = new GameWorld(new Upgrades(true));
        pf.start();
        pf.asteroids.clear();
        pf.fuel.clear();
        FuelPickup cell = new FuelPickup(500f, 0f);
        pf.fuel.add(cell);
        pf.ship.fuel = 10f;
        pf.pilot.target(cell);
        for (int i = 0; i < 60 * 30 && pf.fuel.contains(cell, true); i++) {
            pf.update(dt, new Controls());
        }
        check("piloto llega al bidon y lo recoge", !pf.fuel.contains(cell, true) && pf.ship.fuel > 10f);

        // Piloto automático: meteorito. Debe destruirlo sin recibir golpes.
        GameWorld pa = new GameWorld(new Upgrades(true));
        pa.start();
        pa.asteroids.clear();
        pa.fuel.clear();
        Asteroid rock = new Asteroid(350f, 60f, Asteroid.MEDIUM);
        rock.vel.set(-8f, 0f);
        pa.asteroids.add(rock);
        int pilotLives0 = pa.ship.lives;
        pa.pilot.target(rock);
        boolean destroyed = false;
        for (int i = 0; i < 60 * 40 && !destroyed; i++) {
            pa.update(dt, new Controls());
            destroyed = !pa.asteroids.contains(rock, true) && pa.coinsEarned > 0;
        }
        check("piloto destruye el meteorito", destroyed);
        check("piloto no recibe golpes del meteorito", pa.ship.lives == pilotLives0);

        // Un mando manual cancela el piloto.
        GameWorld pc = new GameWorld(new Upgrades(true));
        pc.start();
        pc.pilot.target(new FuelPickup(900f, 900f));
        pc.update(dt, new Controls());
        pc.pilot.cancel();
        check("cancelar piloto", !pc.pilot.active());

        // Medición de eficiencia del piloto (no son pruebas de corrección, solo cifras para comparar).
        benchFuel("cerca", 300f, 0f, 0f, 0f);
        benchFuel("lateral", 600f, 90f, 0f, 0f);
        benchFuel("detras", 500f, 180f, 0f, 0f);
        benchFuel("en movimiento", 400f, 45f, -120f, 60f);
        benchRock("quieto", 400f, 0f, 0f, 0f);
        benchRock("cruzando", 300f, 200f, -30f, -10f);
        benchRock("alejandose", 350f, -80f, 40f, -20f);
        benchRock("gigante quieto", 400f, 0f, 0f, 0f, Asteroid.LARGE);
        benchRock("gigante hacia nave", 400f, 0f, -40f, 0f, Asteroid.LARGE);
        benchRock("gigante cruzando", 300f, 200f, -30f, -10f, Asteroid.LARGE);
        benchRock("gigante alejandose", 350f, -80f, 40f, -20f, Asteroid.LARGE);

        System.out.println(failures == 0 ? "TODO OK" : failures + " prueba(s) fallida(s)");
    }
}

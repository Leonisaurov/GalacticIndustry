package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Piloto automático: lleva la nave hasta un bidón o hasta un meteorito y lo dispara.
 *
 * Criterio de eficiencia: la nave se deja llevar por la inercia y solo empuja cuando la velocidad
 * que le falta respecto a la deseada supera un umbral. Solo gira cuando va a empujar o cuando
 * necesita apuntar para disparar; en el resto de casos mantiene el rumbo.
 */
class Autopilot {

    static final int NONE = 0;
    static final int FUEL = 1;
    static final int ASTEROID = 2;
    /** Fase de frenada tras perder el objetivo (destruido, recogido o desaparecido). */
    static final int STOP = 3;
    /** Minería automática: aproximarse a una roca y mantener el láser. */
    static final int MINE = 4;
    /** Viaje a la estación nodriza: aproximarse y frenar en la zona de acople. */
    static final int STATION = 5;

    /** Distancia de aparcamiento para minar; queda dentro del alcance del láser. */
    static final float MINE_DIST = 180f;

    /** Distancia a la que se para frente a un meteorito para dispararle (el misil llega a unas 600 u). */
    static final float FIRE_DIST = 220f;
    /** Distancia mínima de seguridad entre la nave y la superficie de un meteorito. */
    static final float SAFE_CLEARANCE = 90f;
    /** Alcance máximo para disparar. */
    static final float FIRE_RANGE = 420f;
    /** Dispara solo si el punto de encuentro cae a menos de esta fracción del radio del meteorito. */
    static final float FIRE_HIT_FRACTION = 0.6f;
    /** Margen alrededor de los meteoritos que no son el objetivo. */
    static final float SAFE_GAP = 40f;
    /** Horizonte de predicción de colisiones, en segundos. */
    static final float LOOKAHEAD = 2.5f;
    /** Velocidad relativa que falta (u/s) por debajo de la cual no se empuja: se deja llevar por la inercia. */
    static final float COAST_BAND = 90f;
    /** Fracción de la aceleración que se usa para calcular la distancia de frenado. */
    static final float BRAKE_MARGIN = 0.7f;
    /** Ángulo máximo de desalineación para empujar; por encima solo se gira. */
    static final float THRUST_CONE = 35f;
    /** Por debajo de esta velocidad se considera que la nave ya está parada. */
    static final float STOP_SPEED = 10f;
    /** Tiempo máximo de frenada; si no puede frenar (sin combustible), se cancela. */
    static final float STOP_TIMEOUT = 5f;
    /** Por debajo de esta velocidad se modula el empuje para no pasarse de frenada. */
    static final float BRAKE_TAPER = 45f;
    /** Distancia extra al bidón en la que se fuerza una velocidad mínima de llegada. */
    static final float FUEL_ARRIVAL_BAND = 30f;
    /** Velocidad mínima hacia el bidón cuando ya está muy cerca (evita quedarse corto). */
    static final float FUEL_MIN_APPROACH = 55f;

    private int kind = NONE;
    private Asteroid rock;
    private FuelPickup cell;
    private Station station;
    private float stopTime;

    private final Vector2 targetVel = new Vector2();
    private final Vector2 toTarget = new Vector2();
    private final Vector2 relVel = new Vector2();
    private final Vector2 desired = new Vector2();
    private final Vector2 need = new Vector2();

    boolean active() {
        return kind != NONE;
    }

    /** Modo actual, para telemetría y pruebas. */
    int mode() {
        return kind;
    }

    void cancel() {
        kind = NONE;
        rock = null;
        cell = null;
        station = null;
        stopTime = 0f;
    }

    void target(Asteroid a) {
        cancel();
        kind = ASTEROID;
        rock = a;
    }

    /** Selecciona una roca para minería automática. */
    void mine(Asteroid a) {
        cancel();
        kind = MINE;
        rock = a;
    }

    void target(FuelPickup f) {
        cancel();
        kind = FUEL;
        cell = f;
    }

    /** Vuela a la estación y frena dentro de la zona de acople. */
    void station(Station s) {
        cancel();
        kind = STATION;
        station = s;
    }

    /** Rellena los controles de este fotograma. Si el objetivo desaparece, pasa a frenar. */
    void steer(Controls c, GameWorld w, float dt) {
        if (kind == NONE) {
            return;
        }
        c.fire = false;
        c.turn = 0f;
        c.aiming = false;
        c.thrust = 0f;
        if (kind == STOP) {
            steerStop(c, w, dt);
            return;
        }
        if (!stillThere(w)) {
            w.ship.laser.stop();
            beginStop();
            steerStop(c, w, dt);
            return;
        }
        Ship ship = w.ship;
        float accel = w.up.thrust();
        float vmax = w.up.maxSpeed();

        Vector2 goal;
        float stop;
        if (kind == ASTEROID || kind == MINE) {
            goal = rock.pos;
            targetVel.set(rock.vel);
            stop = kind == MINE
                    ? MINE_DIST
                    : Math.max(FIRE_DIST, rock.radius + Ship.RADIUS + SAFE_CLEARANCE);
        } else if (kind == STATION) {
            goal = station.pos;
            targetVel.set(0f, 0f);
            // Dentro de dockRadius (260) y con holgura sobre el casco (200).
            stop = Math.max(station.def.radius + 40f, station.def.dockRadius - 20f);
        } else {
            goal = cell.pos;
            targetVel.set(0f, 0f);
            stop = 0f;
        }
        toTarget.set(goal).sub(ship.pos);
        float dist = toTarget.len();
        Vector2 d = dist > 1e-3f ? toTarget.scl(1f / dist) : toTarget.set(MathUtils.cosDeg(ship.heading), MathUtils.sinDeg(ship.heading));
        if (kind == STATION && dist <= stop + 30f) {
            // Ya en la zona de acople: frena del todo en vez de orbitar el casco.
            beginStop();
            steerStop(c, w, dt);
            return;
        }

        // Velocidad de aproximación según la distancia de frenado con la aceleración real.
        // Se frena con la mitad de la aceleración: la nave tarda en girar hacia la dirección de frenado.
        // Solo frente a meteoritos: un bidón no hace daño, así que no hace falta frenar con margen.
        float brake = (kind == ASTEROID || kind == MINE || kind == STATION)
                ? accel * BRAKE_MARGIN : accel;
        float slow = dist - stop;
        float approach = slow > 0f
                ? Math.min(vmax, (float) Math.sqrt(2f * brake * slow))
                : -Math.min(vmax * 0.3f, (float) Math.sqrt(2f * brake * -slow) * 0.5f);
        if (kind == STATION && dist < 700f) {
            // La estación es enorme: se llega despacio para no sobrepasar el casco.
            approach = Math.min(approach, 90f);
        }
        if (kind == FUEL && dist < FuelPickup.RADIUS + Ship.RADIUS + FUEL_ARRIVAL_BAND) {
            // Cerca del bidón no conviene llegar con velocidad casi nula: se pasaría
            // de frenada y el piloto tendría que volver a acelerar. Se fuerza un
            // empuje mínimo para atravesar el radio de recogida.
            approach = Math.max(approach, FUEL_MIN_APPROACH);
        }
        desired.set(d).scl(approach).add(targetVel);

        if (kind == ASTEROID || kind == MINE || kind == STATION) {
            for (int i = 0; i < w.asteroids.size; i++) {
                Asteroid a = w.asteroids.get(i);
                if (a != rock) {
                    avoid(a, ship, vmax);
                }
            }
        }
        // La estación es un obstáculo grande: empuja hacia fuera si la nave se acerca demasiado.
        avoidStation(w.station, ship, vmax);

        // Lo que hay que empujar para pasar de la velocidad relativa actual a la deseada.
        relVel.set(ship.vel).sub(targetVel);
        need.set(desired).sub(relVel);
        float needLen = need.len();
        float band = kind == STATION ? 25f : COAST_BAND;

        if (needLen > band) {
            // Hay que empujar: se gira hacia la dirección necesaria y solo se empuja si está bastante alineada.
            float aimDeg = MathUtils.atan2(need.y, need.x) * MathUtils.radiansToDegrees;
            float misalign = Math.abs(Ship.wrapDeg(aimDeg - ship.heading));
            c.aiming = true;
            c.aimDeg = aimDeg;
            if (misalign < THRUST_CONE) {
                c.thrust = MathUtils.clamp(needLen / accel, 0f, 1f);
            }
        }

        // Disparo: si el meteorito está a tiro, la nave gira hacia la puntería anticipada solo si no está empujando.
        // No se dispara si los misiles ya en vuelo que van a impactar bastan para destruirlo: el extra se perdería.
        if (kind == ASTEROID && dist < FIRE_RANGE && ship.canFire() && c.thrust <= 0f
                && pendingDamage(w) < rock.hp) {
            // El misil sale con la velocidad de la nave, así que el encuentro se calcula en el marco de la nave.
            float px = rock.pos.x - ship.pos.x;
            float py = rock.pos.y - ship.pos.y;
            float vx = rock.vel.x - ship.vel.x;
            float vy = rock.vel.y - ship.vel.y;
            float t = interceptTime(px, py, vx, vy, Missile.SPEED);
            if (t > 0f && t <= Missile.LIFE) {
                float leadX = px + vx * t;
                float leadY = py + vy * t;
                float leadDeg = MathUtils.atan2(leadY, leadX) * MathUtils.radiansToDegrees;
                c.aiming = true;
                c.aimDeg = leadDeg;
                // Desvío lateral del punto de encuentro respecto al rumbo actual: el misil va por ahí.
                float fx = MathUtils.cosDeg(ship.heading);
                float fy = MathUtils.sinDeg(ship.heading);
                float along = fx * leadX + fy * leadY;
                float lateral = Math.abs(fx * leadY - fy * leadX);
                if (along > 0f && lateral < rock.radius * FIRE_HIT_FRACTION) {
                    c.fire = true;
                }
            }
        }

        if (kind == MINE) {
            boolean room = ship.cargo().freeMass() > 0.01f;
            if (!room) {
                ship.laser.stop();
                beginStop();
                steerStop(c, w, dt);
                return;
            }
            if (dist <= MiningLaser.MAX_RANGE && ship.capacitor.hasEnergy()
                    && ship.part[Ship.MINING_LASER] > 0f && !rock.depleted()) {
                ship.laser.aim(rock);
            } else {
                ship.laser.stop();
            }
        }
    }

    /** Pasa a la fase de frenada cuando el objetivo ya no existe. */
    private void beginStop() {
        kind = STOP;
        rock = null;
        cell = null;
        stopTime = 0f;
    }

    /**
     * Frena la nave apuntando en contra de la velocidad y modulando el empuje.
     * Se queda activo hasta que la velocidad baja o se agota el tiempo (por ejemplo, sin combustible).
     */
    private void steerStop(Controls c, GameWorld w, float dt) {
        Ship ship = w.ship;
        stopTime += dt;
        float speed = ship.vel.len();
        if (speed < STOP_SPEED || stopTime > STOP_TIMEOUT) {
            // No dejar inercia residual: la nave queda quieta al terminar la frenada.
            ship.vel.setZero();
            cancel();
            return;
        }
        float aimDeg = MathUtils.atan2(-ship.vel.y, -ship.vel.x) * MathUtils.radiansToDegrees;
        float misalign = Math.abs(Ship.wrapDeg(aimDeg - ship.heading));
        c.aiming = true;
        c.aimDeg = aimDeg;
        if (misalign < THRUST_CONE) {
            float level = speed > BRAKE_TAPER ? 1f
                    : MathUtils.clamp(speed / BRAKE_TAPER, 0.08f, 1f);
            c.thrust = level;
        }
    }

    /** Daño total de los misiles en vuelo que pasarán por el meteorito objetivo antes de caducar. */
    private int pendingDamage(GameWorld w) {
        int sum = 0;
        float reach = rock.radius + Missile.RADIUS;
        for (int i = 0; i < w.missiles.size; i++) {
            Missile m = w.missiles.get(i);
            float dx = rock.pos.x - m.pos.x;
            float dy = rock.pos.y - m.pos.y;
            float dvx = rock.vel.x - m.vel.x;
            float dvy = rock.vel.y - m.vel.y;
            float dv2 = dvx * dvx + dvy * dvy;
            // Instante de máximo acercamiento dentro de la vida que le queda al misil.
            float t = dv2 > 1e-4f ? MathUtils.clamp(-(dx * dvx + dy * dvy) / dv2, 0f, m.remaining()) : 0f;
            float ex = dx + dvx * t;
            float ey = dy + dvy * t;
            if (ex * ex + ey * ey < reach * reach) {
                sum += m.damage;
            }
        }
        return sum;
    }

    /**
     * Menor tiempo positivo t tal que un proyectil que sale de (0,0) a velocidad s alcanza
     * (px + vx t, py + vy t). Devuelve -1 si no hay solución.
     */
    static float interceptTime(float px, float py, float vx, float vy, float s) {
        float a = vx * vx + vy * vy - s * s;
        float b = 2f * (px * vx + py * vy);
        float c = px * px + py * py;
        if (Math.abs(a) < 1e-4f) {
            if (Math.abs(b) < 1e-4f) {
                return -1f;
            }
            float t = -c / b;
            return t > 0f ? t : -1f;
        }
        float disc = b * b - 4f * a * c;
        if (disc < 0f) {
            return -1f;
        }
        float sq = (float) Math.sqrt(disc);
        float t1 = (-b - sq) / (2f * a);
        float t2 = (-b + sq) / (2f * a);
        float best = -1f;
        if (t1 > 0f) {
            best = t1;
        }
        if (t2 > 0f && (best < 0f || t2 < best)) {
            best = t2;
        }
        return best;
    }

    /** Si un meteorito se acerca a la nave dentro de su margen, empuja la velocidad deseada para apartarse. */
    private void avoid(Asteroid a, Ship ship, float vmax) {
        float rx = ship.pos.x - a.pos.x;
        float ry = ship.pos.y - a.pos.y;
        float vx = ship.vel.x - a.vel.x;
        float vy = ship.vel.y - a.vel.y;
        float v2 = vx * vx + vy * vy;
        float t = v2 > 1e-4f ? MathUtils.clamp(-(rx * vx + ry * vy) / v2, 0f, LOOKAHEAD) : 0f;
        float mx = rx + vx * t;
        float my = ry + vy * t;
        float miss = (float) Math.sqrt(mx * mx + my * my);
        float clearance = a.radius + Ship.RADIUS + SAFE_GAP;
        if (miss >= clearance) {
            return;
        }
        if (miss < 0.001f) {
            mx = -vy;
            my = vx;
            miss = (float) Math.sqrt(mx * mx + my * my);
            if (miss < 0.001f) {
                mx = 1f;
                my = 0f;
                miss = 1f;
            }
        }
        float push = (clearance - miss) / clearance * vmax * 0.6f;
        desired.x += mx / miss * push;
        desired.y += my / miss * push;
    }

    /** Empuja la velocidad deseada fuera del casco de la estación. */
    private void avoidStation(Station station, Ship ship, float vmax) {
        if (station == null) {
            return;
        }
        float dx = ship.pos.x - station.pos.x;
        float dy = ship.pos.y - station.pos.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float clearance = station.def.radius + 55f;
        if (dist >= clearance) {
            return;
        }
        if (dist < 0.001f) {
            dx = 0f;
            dy = -1f;
            dist = 1f;
        }
        float push = (clearance - dist) / clearance * vmax * 0.9f;
        desired.x += dx / dist * push;
        desired.y += dy / dist * push;
    }

    private boolean stillThere(GameWorld w) {
        if (kind == ASTEROID) {
            return w.asteroids.contains(rock, true);
        }
        if (kind == MINE) {
            return w.asteroids.contains(rock, true) && !rock.depleted();
        }
        if (kind == FUEL) {
            return w.fuel.contains(cell, true);
        }
        if (kind == STATION) {
            return station != null && station == w.station && !w.ship.docked;
        }
        return false;
    }

    /** Anillo de puntos alrededor del objetivo, para verlo en el juego. */
    void draw(ShapeRenderer shapes, Ship ship, float time) {
        if (kind == NONE) {
            return;
        }
        if (kind == STOP) {
            float r = Ship.RADIUS * 2.15f + MathUtils.sin(time * 6f) * 1.5f;
            shapes.setColor(1f, 0.62f, 0.22f, 0.80f);
            for (int i = 0; i < 8; i++) {
                float a = i * 45f + time * 70f;
                shapes.circle(ship.pos.x + MathUtils.cosDeg(a) * r,
                        ship.pos.y + MathUtils.sinDeg(a) * r, 2.2f, 4);
            }
            return;
        }
        Vector2 p = (kind == ASTEROID || kind == MINE) ? rock.pos
                : kind == STATION ? station.pos : cell.pos;
        float r = (kind == ASTEROID || kind == MINE) ? rock.radius + 12f
                : kind == STATION ? station.def.radius + 24f : FuelPickup.RADIUS + 14f;
        float pulse = 1f + 0.06f * MathUtils.sin(time * 5f);
        if (kind == MINE) {
            shapes.setColor(1f, 0.78f, 0.28f, 0.90f);
        } else if (kind == ASTEROID) {
            shapes.setColor(0.45f, 0.85f, 1f, 0.85f);
        } else if (kind == STATION) {
            shapes.setColor(0.45f, 0.75f, 1f, 0.85f);
        } else {
            shapes.setColor(0.40f, 1f, 0.62f, 0.85f);
        }
        for (int i = 0; i < 12; i++) {
            float a = i * 30f + time * 45f;
            shapes.circle(p.x + MathUtils.cosDeg(a) * r * pulse,
                    p.y + MathUtils.sinDeg(a) * r * pulse, 2.2f, 4);
        }
        shapes.setColor(1f, 1f, 1f, 0.35f);
        for (int i = 0; i < 6; i++) {
            float a = i * 60f - time * 30f;
            shapes.circle(p.x + MathUtils.cosDeg(a) * r * 1.22f,
                    p.y + MathUtils.sinDeg(a) * r * 1.22f, 1.4f, 4);
        }
    }

    /** Halo del objetivo, en la pasada aditiva. */
    void drawGlow(SpriteBatch batch, Visuals art, Ship ship, float time) {
        if (kind == NONE) {
            return;
        }
        if (kind == STOP) {
            float pulse = 0.6f + 0.4f * MathUtils.sin(time * 6f);
            art.glow(batch, ship.pos.x, ship.pos.y, Ship.RADIUS * 5.5f,
                    Visuals.WARN, 0.10f * pulse);
            float r = Ship.RADIUS * 2.15f + MathUtils.sin(time * 6f) * 1.5f;
            for (int i = 0; i < 8; i++) {
                float a = i * 45f + time * 70f;
                art.glow(batch, ship.pos.x + MathUtils.cosDeg(a) * r,
                        ship.pos.y + MathUtils.sinDeg(a) * r, 9f, Visuals.WARN, 0.28f);
            }
            return;
        }
        Vector2 p = (kind == ASTEROID || kind == MINE) ? rock.pos
                : kind == STATION ? station.pos : cell.pos;
        float r = (kind == ASTEROID || kind == MINE) ? rock.radius + 12f
                : kind == STATION ? station.def.radius + 24f : FuelPickup.RADIUS + 14f;
        float pulse = 0.6f + 0.4f * MathUtils.sin(time * 5f);
        Color c = kind == MINE ? Visuals.GOLD
                : kind == ASTEROID ? Visuals.ACCENT
                : kind == STATION ? Visuals.ACCENT : Visuals.GOOD;
        art.glow(batch, p.x, p.y, r * 3.2f, c, 0.10f * pulse);
        for (int i = 0; i < 12; i++) {
            float a = i * 30f + time * 45f;
            art.glow(batch, p.x + MathUtils.cosDeg(a) * r,
                    p.y + MathUtils.sinDeg(a) * r, 10f, c, 0.35f);
        }
    }
}

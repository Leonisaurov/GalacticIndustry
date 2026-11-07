package com.example.codegame;

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

    private int kind = NONE;
    private Asteroid rock;
    private FuelPickup cell;

    private final Vector2 targetVel = new Vector2();
    private final Vector2 toTarget = new Vector2();
    private final Vector2 relVel = new Vector2();
    private final Vector2 desired = new Vector2();
    private final Vector2 need = new Vector2();

    boolean active() {
        return kind != NONE;
    }

    void cancel() {
        kind = NONE;
        rock = null;
        cell = null;
    }

    void target(Asteroid a) {
        cancel();
        kind = ASTEROID;
        rock = a;
    }

    void target(FuelPickup f) {
        cancel();
        kind = FUEL;
        cell = f;
    }

    /** Rellena los controles de este fotograma. Si el objetivo ya no existe, el piloto se cancela. */
    void steer(Controls c, GameWorld w, float dt) {
        c.fire = false;
        c.turn = 0f;
        c.aiming = false;
        c.thrust = 0f;
        if (!stillThere(w)) {
            cancel();
            return;
        }
        Ship ship = w.ship;
        float accel = w.up.thrust();
        float vmax = w.up.maxSpeed();

        Vector2 goal;
        float stop;
        if (kind == ASTEROID) {
            goal = rock.pos;
            targetVel.set(rock.vel);
            stop = Math.max(FIRE_DIST, rock.radius + Ship.RADIUS + SAFE_CLEARANCE);
        } else {
            goal = cell.pos;
            targetVel.set(0f, 0f);
            stop = 0f;
        }
        toTarget.set(goal).sub(ship.pos);
        float dist = toTarget.len();
        Vector2 d = dist > 1e-3f ? toTarget.scl(1f / dist) : toTarget.set(MathUtils.cosDeg(ship.heading), MathUtils.sinDeg(ship.heading));

        // Velocidad de aproximación según la distancia de frenado con la aceleración real.
        // Se frena con la mitad de la aceleración: la nave tarda en girar hacia la dirección de frenado.
        // Solo frente a meteoritos: un bidón no hace daño, así que no hace falta frenar con margen.
        float brake = kind == ASTEROID ? accel * BRAKE_MARGIN : accel;
        float slow = dist - stop;
        float approach = slow > 0f
                ? Math.min(vmax, (float) Math.sqrt(2f * brake * slow))
                : -Math.min(vmax * 0.3f, (float) Math.sqrt(2f * brake * -slow) * 0.5f);
        desired.set(d).scl(approach).add(targetVel);

        if (kind == ASTEROID) {
            for (int i = 0; i < w.asteroids.size; i++) {
                Asteroid a = w.asteroids.get(i);
                if (a != rock) {
                    avoid(a, ship, vmax);
                }
            }
        }

        // Lo que hay que empujar para pasar de la velocidad relativa actual a la deseada.
        relVel.set(ship.vel).sub(targetVel);
        need.set(desired).sub(relVel);
        float needLen = need.len();

        if (needLen > COAST_BAND) {
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

    private boolean stillThere(GameWorld w) {
        if (kind == ASTEROID) {
            return w.asteroids.contains(rock, true);
        }
        if (kind == FUEL) {
            return w.fuel.contains(cell, true);
        }
        return false;
    }

    /** Anillo de puntos alrededor del objetivo, para verlo en el juego. */
    void draw(ShapeRenderer shapes) {
        if (!active()) {
            return;
        }
        Vector2 p = kind == ASTEROID ? rock.pos : cell.pos;
        float r = kind == ASTEROID ? rock.radius + 10f : FuelPickup.RADIUS + 12f;
        shapes.setColor(0.6f, 0.9f, 1f, 0.8f);
        for (int i = 0; i < 16; i++) {
            float a = i * 22.5f;
            shapes.circle(p.x + MathUtils.cosDeg(a) * r, p.y + MathUtils.sinDeg(a) * r, 2.5f, 4);
        }
    }
}

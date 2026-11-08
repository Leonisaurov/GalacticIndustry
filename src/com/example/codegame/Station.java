package com.example.codegame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Estación nodriza instanciada en el sector inicial: posición, radio, zona
 * segura y almacén. Se dibuja a gran escala y gira lentamente.
 */
final class Station {

    static final float STORAGE_CAPACITY = 2000f;

    private static final Color HULL_DARK = new Color(0.09f, 0.11f, 0.18f, 1f);
    private static final Color HULL_MID = new Color(0.22f, 0.28f, 0.40f, 1f);
    private static final Color HULL_LIGHT = new Color(0.42f, 0.52f, 0.68f, 1f);
    private static final Color LIGHT = new Color(0.45f, 0.95f, 1f, 1f);
    private static final Color LIGHT_WARM = new Color(1f, 0.78f, 0.35f, 1f);

    final StationDef def;
    final Vector2 pos = new Vector2();
    /** Boca de atraque, fija respecto al centro (UPD-006-T02). */
    final Vector2 dockPoint = new Vector2();
    /** Punto de espera del corredor de aproximación. */
    final Vector2 approachPoint = new Vector2();
    /** Eje unitario del centro a la boca. */
    final Vector2 dockAxis = new Vector2(0f, -1f);
    /** Energía del campo de contención (UPD-006-T04). */
    final StationEnergy energy;
    final StationStorage storage;
    /** Mercado de la estación (stock y precios). */
    final Market market;
    /** Contratos ofrecidos por la estación. */
    final MissionService missions = new MissionService();
    private float rotation;

    Station(StationDef def, float x, float y) {
        this(def, x, y, null);
    }

    Station(StationDef def, float x, float y, Market market) {
        this.def = def;
        this.pos.set(x, y);
        this.energy = StationEnergy.of(def);
        this.storage = new StationStorage(STORAGE_CAPACITY);
        this.market = market != null ? market : GameData.newMarket();
        this.missions.ensureOffers(this.market, def == null ? "NODRIZA" : def.name);
        updateDockGeometry();
    }

    /** Recalcula boca, espera y eje a partir de la definición. */
    void updateDockGeometry() {
        float angle = def == null ? StationDef.DEFAULT_DOCK_ANGLE : def.dockAngleDeg;
        dockAxis.set(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle));
        float offset = def == null ? 0f : def.dockOffset;
        float approach = def == null ? offset + 220f : def.approachDistance;
        dockPoint.set(pos.x + dockAxis.x * offset, pos.y + dockAxis.y * offset);
        approachPoint.set(pos.x + dockAxis.x * approach,
                pos.y + dockAxis.y * approach);
    }

    /** Distancia al punto de atraque. */
    float distanceToDock(Vector2 point) {
        return point == null ? -1f : point.dst(dockPoint);
    }

    /**
     * Corredor de entrada: cápsula entre el punto de espera y la boca. Dentro
     * del corredor no se aplica la colisión del casco.
     */
    boolean inDockCorridor(float x, float y) {
        float half = def == null ? StationDef.DEFAULT_APPROACH_HALF_WIDTH
                : def.approachHalfWidth;
        return distanceToSegment(x, y, approachPoint.x, approachPoint.y,
                dockPoint.x, dockPoint.y) <= half;
    }

    boolean inDockCorridor(Vector2 point) {
        return point != null && inDockCorridor(point.x, point.y);
    }

    /** Desviación lateral con signo respecto al eje de entrada (u). */
    float lateralDeviation(float x, float y) {
        float dx = x - pos.x;
        float dy = y - pos.y;
        return dx * -dockAxis.y + dy * dockAxis.x;
    }

    /** Ángulo que debe mirar la nave para entrar: hacia el centro. */
    float dockHeading() {
        return wrapDeg(def == null ? StationDef.DEFAULT_DOCK_ANGLE + 180f
                : def.dockAngleDeg + 180f);
    }

    /** Error de rumbo respecto al eje de entrada, en grados [0, 180]. */
    float headingError(float headingDeg) {
        float error = Math.abs(wrapDeg(headingDeg - dockHeading()));
        return Math.min(180f, error);
    }

    /**
     * Acople completo (UPD-006-T02): boca, velocidad, orientación y corredor.
     * Se mantiene la variante de dos argumentos para la compatibilidad de la
     * documentación y las pruebas antiguas (solo proximidad y velocidad).
     */
    boolean canDock(Vector2 point, float speed) {
        if (point == null) {
            return false;
        }
        return distanceTo(point) <= def.dockRadius && speed < def.dockMaxSpeed;
    }

    boolean canDock(Vector2 point, float speed, float headingDeg) {
        if (point == null) {
            return false;
        }
        return distanceToDock(point) <= def.dockPointRadius
                && speed < def.dockMaxSpeed
                && headingError(headingDeg) <= def.dockMaxAngle
                && inDockCorridor(point);
    }

    /** Coloca la nave en la boca al completar el acople. */
    void snapToDock(Vector2 shipPos) {
        if (shipPos != null) {
            shipPos.set(dockPoint);
        }
    }

    private static float distanceToSegment(float px, float py,
                                           float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float len2 = dx * dx + dy * dy;
        if (len2 <= 0.0001f) {
            return (float) Math.sqrt((px - ax) * (px - ax) + (py - ay) * (py - ay));
        }
        float t = ((px - ax) * dx + (py - ay) * dy) / len2;
        t = MathUtils.clamp(t, 0f, 1f);
        float cx = ax + dx * t;
        float cy = ay + dy * t;
        return (float) Math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy));
    }

    private static float wrapDeg(float a) {
        return ((a % 360f) + 540f) % 360f - 180f;
    }

    void update(float dt) {
        missions.update(dt);
        rotation += dt * 4f;
        if (rotation >= 360f) {
            rotation -= 360f;
        }
    }

    float distanceTo(Vector2 point) {
        return point == null ? -1f : point.dst(pos);
    }

    boolean inSafeZone(float x, float y) {
        float dx = x - pos.x;
        float dy = y - pos.y;
        return dx * dx + dy * dy < def.safeRadius * def.safeRadius;
    }

    boolean inSafeZone(Vector2 point) {
        return point != null && inSafeZone(point.x, point.y);
    }

    void draw(ShapeRenderer shapes, float time) {
        float r = def.radius;
        float bob = MathUtils.sin(time * 0.6f) * 3f;

        drawContainmentRing(shapes, time);

        // Sombra y anillo exterior.
        shapes.setColor(0f, 0f, 0f, 0.45f);
        shapes.circle(pos.x, pos.y - 8f + bob, r * 1.06f, 48);
        shapes.setColor(HULL_DARK);
        shapes.circle(pos.x, pos.y + bob, r, 48);
        shapes.setColor(HULL_MID);
        shapes.circle(pos.x, pos.y + bob, r * 0.92f, 48);
        shapes.setColor(HULL_DARK);
        shapes.circle(pos.x, pos.y + bob, r * 0.78f, 48);

        // Paneles del anillo.
        for (int i = 0; i < 16; i++) {
            float a = i * 22.5f + rotation;
            float px = pos.x + MathUtils.cosDeg(a) * r * 0.85f;
            float py = pos.y + bob + MathUtils.sinDeg(a) * r * 0.85f;
            shapes.setColor(i % 4 == 0 ? HULL_LIGHT : HULL_MID);
            shapes.circle(px, py, r * 0.055f, 8);
        }

        // Brazos y núcleo.
        for (int i = 0; i < 4; i++) {
            float a = i * 90f + rotation * 0.35f;
            float ex = pos.x + MathUtils.cosDeg(a) * r * 0.74f;
            float ey = pos.y + bob + MathUtils.sinDeg(a) * r * 0.74f;
            shapes.setColor(HULL_LIGHT);
            shapes.rectLine(pos.x, pos.y + bob, ex, ey, r * 0.045f);
        }
        shapes.setColor(HULL_MID);
        shapes.circle(pos.x, pos.y + bob, r * 0.30f, 32);
        shapes.setColor(HULL_DARK);
        shapes.circle(pos.x, pos.y + bob, r * 0.22f, 32);
        shapes.setColor(LIGHT);
        shapes.circle(pos.x, pos.y + bob, r * 0.10f, 24);
        shapes.setColor(1f, 1f, 1f, 0.9f);
        shapes.circle(pos.x, pos.y + bob, r * 0.05f, 18);

        // Bahía de acople en la boca real y luces de posición.
        float bx = dockPoint.x;
        float by = dockPoint.y + bob;
        shapes.setColor(HULL_DARK);
        Visuals.roundRect(shapes, bx - r * 0.18f, by - r * 0.10f,
                r * 0.36f, r * 0.16f, r * 0.05f);
        shapes.setColor(LIGHT_WARM);
        shapes.rect(bx - r * 0.13f, by - r * 0.04f, r * 0.26f, r * 0.03f);
        float blink = 0.55f + 0.45f * MathUtils.sin(time * 3.2f);
        for (int i = 0; i < 6; i++) {
            float a = 30f + i * 60f;
            shapes.setColor(0.35f, 0.90f, 1f, 0.45f * blink);
            shapes.circle(pos.x + MathUtils.cosDeg(a) * r * 1.01f,
                    pos.y + bob + MathUtils.sinDeg(a) * r * 1.01f, r * 0.022f, 8);
        }

        drawDockGuides(shapes, time, blink);
    }

    /** Anillo de contención con pulsos y marcas cada 45 grados (UPD-006-T04). */
    private void drawContainmentRing(ShapeRenderer shapes, float time) {
        float ring = def.safeRadius;
        if (ring <= 0f) {
            return;
        }
        boolean active = energy.active();
        float pulse = 0.55f + 0.45f * MathUtils.sin(time * 1.1f);
        Color c = active ? LIGHT : new Color(1f, 0.35f, 0.32f, 1f);
        shapes.setColor(c.r, c.g, c.b, active ? 0.10f + 0.06f * pulse : 0.10f);
        shapes.circle(pos.x, pos.y, ring, 72);
        shapes.setColor(c.r, c.g, c.b, active ? 0.22f : 0.20f);
        for (int i = 0; i < 8; i++) {
            float a = i * 45f + rotation * 0.10f;
            float ix = pos.x + MathUtils.cosDeg(a) * ring;
            float iy = pos.y + MathUtils.sinDeg(a) * ring;
            float ox = pos.x + MathUtils.cosDeg(a) * (ring + 9f);
            float oy = pos.y + MathUtils.sinDeg(a) * (ring + 9f);
            shapes.rectLine(ix, iy, ox, oy, 2.2f);
        }
    }

    /** Luces de guía del corredor y de la boca de atraque (UPD-006-T02). */
    private void drawDockGuides(ShapeRenderer shapes, float time, float blink) {
        float half = def.approachHalfWidth;
        for (int side = -1; side <= 1; side += 2) {
            float ox = -dockAxis.y * half * side;
            float oy = dockAxis.x * half * side;
            for (int i = 0; i <= 4; i++) {
                float t = i / 4f;
                float x = approachPoint.x + (dockPoint.x - approachPoint.x) * t + ox;
                float y = approachPoint.y + (dockPoint.y - approachPoint.y) * t + oy;
                float phase = (i * 0.22f - time * 1.6f) % 1f;
                if (phase < 0f) {
                    phase += 1f;
                }
                shapes.setColor(LIGHT_WARM.r, LIGHT_WARM.g, LIGHT_WARM.b,
                        0.20f + 0.55f * (1f - phase) * blink);
                shapes.circle(x, y, 3.4f, 8);
            }
        }
        // Corredor tenue.
        shapes.setColor(LIGHT_WARM.r, LIGHT_WARM.g, LIGHT_WARM.b, 0.05f);
        shapes.rectLine(approachPoint.x, approachPoint.y, dockPoint.x, dockPoint.y,
                half * 2f);
        // Boca: aro y eje de alineación.
        shapes.setColor(LIGHT.r, LIGHT.g, LIGHT.b, 0.45f + 0.35f * blink);
        shapes.circle(dockPoint.x, dockPoint.y, def.dockPointRadius, 22);
        shapes.setColor(HULL_DARK);
        shapes.circle(dockPoint.x, dockPoint.y, def.dockPointRadius * 0.72f, 20);
        shapes.setColor(LIGHT_WARM);
        shapes.circle(dockPoint.x, dockPoint.y, 5f, 12);
        shapes.setColor(LIGHT.r, LIGHT.g, LIGHT.b, 0.35f);
        shapes.rectLine(dockPoint.x, dockPoint.y,
                dockPoint.x + dockAxis.x * 46f, dockPoint.y + dockAxis.y * 46f, 2f);
    }

    void drawGlow(SpriteBatch batch, Visuals art, float time) {
        float pulse = 0.7f + 0.3f * MathUtils.sin(time * 1.8f);
        art.glow(batch, pos.x, pos.y, def.radius * 2.2f, Visuals.ACCENT, 0.10f * pulse);
        art.glow(batch, pos.x, pos.y, def.radius * 0.8f, Color.WHITE, 0.12f * pulse);
        art.glow(batch, pos.x, pos.y - def.radius * 0.92f,
                def.radius * 0.5f, LIGHT_WARM, 0.22f * pulse);
    }

}

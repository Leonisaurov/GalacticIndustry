package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.Random;

/**
 * Paleta de color y texturas suaves generadas al arrancar (brillo, estrella, nube y viñeta).
 * No hay ficheros de imagen: todo se dibuja en un Pixmap y queda como textura reutilizable.
 */
class Visuals {

    // ---------- paleta ----------
    static final Color SKY_TOP = new Color(0.035f, 0.045f, 0.13f, 1f);
    static final Color SKY_BOTTOM = new Color(0.12f, 0.055f, 0.235f, 1f);

    static final Color ACCENT = new Color(0.36f, 0.86f, 1f, 1f);
    static final Color ACCENT_DIM = new Color(0.25f, 0.55f, 0.8f, 1f);
    static final Color O2 = new Color(0.36f, 0.86f, 1f, 1f);
    static final Color HEALTH = new Color(0.45f, 1f, 0.60f, 1f);
    static final Color GOLD = new Color(1f, 0.80f, 0.30f, 1f);
    static final Color GOOD = new Color(0.35f, 1f, 0.60f, 1f);
    static final Color WARN = new Color(1f, 0.72f, 0.22f, 1f);
    static final Color DANGER = new Color(1f, 0.32f, 0.42f, 1f);
    static final Color TEXT = new Color(0.92f, 0.96f, 1f, 1f);
    static final Color TEXT_DIM = new Color(0.55f, 0.63f, 0.78f, 1f);
    static final Color TEXT_OFF = new Color(0.52f, 0.56f, 0.66f, 1f);

    static final Color PANEL = new Color(0.035f, 0.055f, 0.125f, 0.88f);
    static final Color PANEL_EDGE = new Color(0.30f, 0.62f, 0.95f, 0.45f);
    static final Color CARD = new Color(0.07f, 0.11f, 0.21f, 0.86f);
    static final Color CARD_EDGE = new Color(0.26f, 0.48f, 0.75f, 0.38f);

    static final Color BTN_PRIMARY = new Color(0.10f, 0.52f, 0.72f, 0.95f);
    static final Color BTN_SECONDARY = new Color(0.13f, 0.22f, 0.42f, 0.92f);
    static final Color BTN_WARM = new Color(0.72f, 0.38f, 0.10f, 0.95f);
    static final Color BTN_OFF = new Color(0.16f, 0.17f, 0.22f, 0.82f);

    static final Color NEBULA_PURPLE = new Color(0.55f, 0.22f, 0.85f, 1f);
    static final Color NEBULA_BLUE = new Color(0.18f, 0.42f, 0.85f, 1f);
    static final Color NEBULA_TEAL = new Color(0.12f, 0.62f, 0.68f, 1f);

    /** Haz del láser según el tipo de roca: C hielo, S silicato, M metálico. */
    static final Color LASER_C = new Color(0.45f, 0.95f, 1f, 1f);
    static final Color LASER_S = new Color(0.86f, 0.92f, 1f, 1f);
    static final Color LASER_M = new Color(1f, 0.76f, 0.36f, 1f);

    static Color laserColor(AsteroidType type) {
        if (type == AsteroidType.M) {
            return LASER_M;
        }
        if (type == AsteroidType.S) {
            return LASER_S;
        }
        return LASER_C;
    }

    // ---------- texturas ----------
    private Texture glowTex;
    private Texture starTex;
    private Texture cloudTex;
    private Texture vignetteTex;

    TextureRegion glow;
    TextureRegion star;
    TextureRegion cloud;
    TextureRegion vignette;

    void create() {
        glowTex = radial(128, 2.3f, true);
        starTex = radial(64, 4.5f, true);
        cloudTex = cloudTexture(160, 0x5EED1234L);
        vignetteTex = vignetteTexture(256);
        glow = new TextureRegion(glowTex);
        star = new TextureRegion(starTex);
        cloud = new TextureRegion(cloudTex);
        vignette = new TextureRegion(vignetteTex);
    }

    void dispose() {
        if (glowTex != null) glowTex.dispose();
        if (starTex != null) starTex.dispose();
        if (cloudTex != null) cloudTex.dispose();
        if (vignetteTex != null) vignetteTex.dispose();
    }

    // ---------- mezcla ----------

    /** Mezcla aditiva: los brillos suman luz en vez de tapar el fondo. */
    static void additive(SpriteBatch batch) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        // SpriteBatch solo aplica su mezcla al hacer flush. ShapeRenderer usa el estado
        // de OpenGL directamente, así que hay que dejarlo ya preparado para la siguiente pasada.
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
    }

    static void normal(SpriteBatch batch) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ---------- dibujo de sprites ----------

    void glow(SpriteBatch batch, float x, float y, float size, Color c, float alpha) {
        batch.setColor(c.r, c.g, c.b, alpha);
        batch.draw(glow, x - size * 0.5f, y - size * 0.5f, size, size);
    }

    void star(SpriteBatch batch, float x, float y, float size, Color c, float alpha) {
        batch.setColor(c.r, c.g, c.b, alpha);
        batch.draw(star, x - size * 0.5f, y - size * 0.5f, size, size);
    }

    void cloud(SpriteBatch batch, float x, float y, float w, float h, Color c, float alpha) {
        batch.setColor(c.r, c.g, c.b, alpha);
        batch.draw(cloud, x - w * 0.5f, y - h * 0.5f, w, h);
    }

    void vignette(SpriteBatch batch, float w, float h, float alpha) {
        batch.setColor(0f, 0f, 0f, alpha);
        batch.draw(vignette, 0f, 0f, w, h);
    }

    /** Viñeta tintada (para el destello rojo al recibir un golpe). */
    void vignetteTint(SpriteBatch batch, float w, float h, float r, float g, float b, float alpha) {
        batch.setColor(r, g, b, alpha);
        batch.draw(vignette, 0f, 0f, w, h);
    }

    // ---------- formas ----------

    /** Rectángulo con esquinas redondeadas sin solapar rellenos (la transparencia queda uniforme). */
    static void roundRect(ShapeRenderer s, float x, float y, float w, float h, float r) {
        if (w <= 0f || h <= 0f) {
            return;
        }
        r = Math.min(r, Math.min(w, h) * 0.5f);
        if (r < 0.5f) {
            s.rect(x, y, w, h);
            return;
        }
        s.rect(x + r, y, w - 2f * r, h);
        s.rect(x, y + r, r, h - 2f * r);
        s.rect(x + w - r, y + r, r, h - 2f * r);
        // Cuartos de círculo (no círculos completos): así no se pisan con los rectángulos
        // y una transparencia no se vuelve más opaca en las esquinas.
        s.arc(x + r, y + r, r, 180f, 90f, 10);
        s.arc(x + w - r, y + r, r, 270f, 90f, 10);
        s.arc(x + w - r, y + h - r, r, 0f, 90f, 10);
        s.arc(x + r, y + h - r, r, 90f, 90f, 10);
    }

    // ---------- generación de texturas ----------

    /** Degradado radial blanco con alfa; power controla lo rápido que cae el borde. */
    private static Texture radial(int size, float power, boolean softCore) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float half = (size - 1) * 0.5f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - half) / half;
                float dy = (y - half) / half;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = MathUtils.clamp(1f - d, 0f, 1f);
                a = (float) Math.pow(a, power);
                if (softCore) {
                    a += 0.35f * (float) Math.pow(MathUtils.clamp(1f - d, 0f, 1f), power * 5f);
                }
                a = MathUtils.clamp(a, 0f, 1f);
                p.setColor(1f, 1f, 1f, a);
                p.drawPixel(x, y);
            }
        }
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    /** Nube suave hecha con varias manchas gaussianas; los bordes se desvanecen. */
    private static Texture cloudTexture(int size, long seed) {
        Random rnd = new Random(seed);
        float[] acc = new float[size * size];
        int blobs = 20;
        float half = (size - 1) * 0.5f;
        for (int b = 0; b < blobs; b++) {
            float cx = rnd.nextFloat() * size;
            float cy = rnd.nextFloat() * size;
            float r = size * (0.10f + rnd.nextFloat() * 0.32f);
            float strength = 0.25f + rnd.nextFloat() * 0.80f;
            int x0 = Math.max(0, (int) (cx - r));
            int x1 = Math.min(size - 1, (int) (cx + r));
            int y0 = Math.max(0, (int) (cy - r));
            int y1 = Math.min(size - 1, (int) (cy + r));
            for (int y = y0; y <= y1; y++) {
                float dy = (y - cy) / r;
                for (int x = x0; x <= x1; x++) {
                    float dx = (x - cx) / r;
                    float d2 = dx * dx + dy * dy;
                    if (d2 < 1f) {
                        float f = 1f - d2;
                        acc[y * size + x] += strength * f * f;
                    }
                }
            }
        }
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - half) / half;
                float dy = (y - half) / half;
                float edge = MathUtils.clamp(1.05f - (float) Math.sqrt(dx * dx + dy * dy), 0f, 1f);
                float a = MathUtils.clamp(acc[y * size + x] * 0.8f, 0f, 1f) * edge * edge;
                p.setColor(1f, 1f, 1f, a);
                p.drawPixel(x, y);
            }
        }
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private static Texture vignetteTexture(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float half = (size - 1) * 0.5f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - half) / half;
                float dy = (y - half) / half;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = MathUtils.clamp((d - 0.68f) / 0.62f, 0f, 1f);
                a = a * a * (3f - 2f * a) * 0.9f;
                p.setColor(1f, 1f, 1f, a);
                p.drawPixel(x, y);
            }
        }
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }
}

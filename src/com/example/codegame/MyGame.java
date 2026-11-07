package com.example.codegame;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/** Punto de entrada del juego: gestiona menú, partida, pausa, tienda y fin de partida. */
public class MyGame implements ApplicationListener {

    static final int MENU = 0;
    static final int PLAYING = 1;
    static final int PAUSED = 2;
    static final int SHOP = 3;
    static final int OVER = 4;

    private static final float MIN_W = 480f;
    private static final float MIN_H = 800f;
    private static final float STAR_CELL = 240f;
    private static final int STARS_PER_CELL = 3;

    private static final Color SKY_TOP = new Color(0.04f, 0.05f, 0.14f, 1f);
    private static final Color SKY_BOTTOM = new Color(0.12f, 0.06f, 0.24f, 1f);
    private static final Color HUD_PANEL = new Color(0f, 0f, 0f, 0.35f);
    private static final Color DIM = new Color(0f, 0f, 0f, 0.6f);
    /** Igual que CTRL_BASE: todos los botones comparten estilo. */
    private static final Color BTN_COLOR = new Color(0.2f, 0.32f, 0.6f, 0.45f);
    private static final Color BTN_OFF = new Color(0.25f, 0.25f, 0.3f, 0.6f);
    /** Mismo estilo para joystick, barra de aceleración y botón de pausa. */
    private static final Color CTRL_BASE = new Color(0.2f, 0.32f, 0.6f, 0.45f);
    private static final Color CTRL_KNOB = new Color(0.45f, 0.7f, 1f, 0.85f);

    private OrthographicCamera worldCam;
    private ExtendViewport worldView;
    private OrthographicCamera hudCam;
    private ScreenViewport hudView;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;
    private Upgrades upgrades;
    private GameWorld world;
    private final Vector2 touch = new Vector2();

    private int state = MENU;
    private int shopReturn = MENU;
    private float time;
    private float u = 1f;

    private Button btnPlay, btnShop, btnResume, btnPauseShop, btnMenu, btnRetry, btnOverMenu, btnBack;
    /** Barra de aceleración: su rectángulo es btnThrust.bounds. */
    private Button btnThrust, btnPause;
    private final Array<Button> buyButtons = new Array<>();
    /** Recarga de gasolina en la tienda de pausa (solo durante la partida). */
    private Button btnRefill;

    // Joystick (izquierda) y barra de aceleración (derecha), en coordenadas de pantalla.
    private float joyCx, joyCy, joyR;
    private final Rectangle joyZone = new Rectangle();
    private final Rectangle sliderZone = new Rectangle();
    private int joyPointer = -1;
    private int sliderPointer = -1;
    private float knobX, knobY;
    private boolean joyAiming;
    private float joyAimDeg;
    private float thrustLevel;
    private float throttle;
    private final boolean[] wasDown = new boolean[5];
    /** Clave de preferencias donde se guarda la partida en curso. */
    private static final String RUN_KEY = "run";
    /** Hay una partida empezada (guardada o en curso) que se puede reanudar. */
    private boolean runActive;

    @Override
    public void create() {
        worldCam = new OrthographicCamera();
        worldView = new ExtendViewport(MIN_W, MIN_H, worldCam);
        hudCam = new OrthographicCamera();
        hudView = new ScreenViewport(hudCam);
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        // La fuente por defecto de gdx vive en el classpath del jar, y apkc no
        // empaqueta recursos de jars: se copia a assets/fonts y se carga de ahí.
        font = new BitmapFont(Gdx.files.internal("fonts/lsans-15.fnt"));
        layout = new GlyphLayout();
        upgrades = new Upgrades();
        world = new GameWorld(upgrades);
        loadRun();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /** Si hay una partida guardada, la carga en pausa para que el jugador decida cuándo seguir. */
    private void loadRun() {
        String saved = upgrades.getText(RUN_KEY);
        if (saved != null && world.restore(saved)) {
            runActive = true;
            state = PAUSED;
        }
    }

    /** Guarda la partida en curso en las preferencias (la pausa y el cierre de la app la llaman). */
    private void saveRun() {
        if (runActive && !world.isOver()) {
            upgrades.putText(RUN_KEY, world.serialize());
        }
    }

    /** Borra la partida guardada al terminar la partida. */
    private void clearRun() {
        runActive = false;
        upgrades.putText(RUN_KEY, null);
    }

    @Override
    public void resize(int width, int height) {
        worldView.update(width, height, true);
        hudView.update(width, height, true);
        u = Math.max(1f, Gdx.graphics.getDensity());
        layoutUi();
    }

    /** Calcula posiciones de controles y botones en coordenadas de pantalla. */
    private void layoutUi() {
        float w = hudView.getWorldWidth();
        float h = hudView.getWorldHeight();
        float pad = 14f * u;
        float bw = Math.min(w * 0.7f, 260f * u);
        float bh = 52f * u;
        float gap = 16f * u;
        float cx = (w - bw) / 2f;
        float cy = h * 0.5f;

        btnPlay = stacked("Jugar", cx, cy, bw, bh, gap, 0, 2);
        btnShop = stacked("Tienda", cx, cy, bw, bh, gap, 1, 2);
        btnResume = stacked("Continuar", cx, cy, bw, bh, gap, 0, 3);
        btnPauseShop = stacked("Tienda", cx, cy, bw, bh, gap, 1, 3);
        btnMenu = stacked("Menu", cx, cy, bw, bh, gap, 2, 3);
        btnRetry = stacked("Reintentar", cx, cy, bw, bh, gap, 0, 2);
        btnOverMenu = stacked("Menu", cx, cy, bw, bh, gap, 1, 2);
        btnBack = new Button("Volver", cx, pad, bw, bh);

        joyR = 74f * u;
        joyCx = pad + joyR;
        joyCy = pad + joyR;
        joyZone.set(joyCx - joyR * 1.5f, joyCy - joyR * 1.5f, joyR * 3f, joyR * 3f);

        float barW = 56f * u;
        float barH = 260f * u;
        btnThrust = new Button("ACEL", w - pad - barW, pad, barW, barH);
        sliderZone.set(btnThrust.bounds.x - 16f * u, pad, barW + 32f * u, barH);

        float ps = 52f * u;
        btnPause = new Button("II", w - pad - ps, h - pad - ps, ps, ps);

        buyButtons.clear();
        float bbw = 150f * u;
        float bbh = 46f * u;
        for (int i = 0; i < Upgrades.COUNT; i++) {
            buyButtons.add(new Button(buyLabel(i), w - pad - bbw, shopRowTop(i, h) - 56f * u, bbw, bbh));
        }
        btnRefill = new Button("Gasolina " + upgrades.refillCost(), w - pad - bbw,
                shopRowTop(Upgrades.COUNT, h) - 56f * u, bbw, bbh);
    }

    /** La recarga solo se ofrece en partida, con el depósito no lleno y saldo suficiente. */
    private boolean canRefill() {
        return world.ship.fuel < world.ship.maxFuel
                && upgrades.coins() + world.coinsEarned >= upgrades.refillCost();
    }

    private String buyLabel(int i) {
        return upgrades.level(i) >= Upgrades.MAX_LEVEL ? "MAX" : "Comprar " + upgrades.cost(i);
    }

    private Button stacked(String label, float cx, float cy, float bw, float bh, float gap, int index, int count) {
        float total = count * bh + (count - 1) * gap;
        float top = cy + total / 2f;
        float y = top - (index + 1) * bh - index * gap;
        return new Button(label, cx, y, bw, bh);
    }

    private float shopRowTop(int index, float h) {
        return h * 0.7f - index * 86f * u;
    }

    /** Pulsación nueva sobre el botón (en cualquier dedo). */
    private boolean tapped(Button b) {
        if (!Gdx.input.justTouched()) {
            return false;
        }
        return held(b);
    }

    /** Botón mantenido pulsado (cualquier dedo). */
    private boolean held(Button b) {
        for (int i = 0; i < 5; i++) {
            if (Gdx.input.isTouched(i) && hit(b, i)) {
                return true;
            }
        }
        return false;
    }

    private boolean hit(Button b, int pointer) {
        return pointerIn(b.bounds, pointer);
    }

    private boolean pointerIn(Rectangle r, int pointer) {
        touch.set(Gdx.input.getX(pointer), Gdx.graphics.getHeight() - Gdx.input.getY(pointer));
        return r.contains(touch.x, touch.y);
    }

    private boolean pointerInCircle(float cx, float cy, float radius, int pointer) {
        touch.set(Gdx.input.getX(pointer), Gdx.graphics.getHeight() - Gdx.input.getY(pointer));
        return touch.dst(cx, cy) <= radius;
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
        time += dt;
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        switch (state) {
            case PLAYING: updatePlaying(dt); break;
            case PAUSED: handlePause(); break;
            case SHOP: handleShop(); break;
            case OVER: handleOver(); break;
            default: handleMenu(); break;
        }

        if (state == PLAYING || state == PAUSED || state == OVER) {
            drawWorld();
        }
        drawHud();
        updateTouchState();
    }

    /** Guarda qué dedos estaban pulsados en el fotograma anterior (para detectar toques nuevos). */
    private void updateTouchState() {
        for (int i = 0; i < wasDown.length; i++) {
            wasDown[i] = Gdx.input.isTouched(i);
        }
    }

    private void handleMenu() {
        if (tapped(btnPlay)) {
            startRun();
        } else if (tapped(btnShop)) {
            openShop(MENU);
        }
    }

    /** Reanuda la partida guardada o empieza una nueva si no hay ninguna. */
    private void startRun() {
        resetControls();
        if (!runActive) {
            world.start();
            runActive = true;
        }
        state = PLAYING;
    }

    private void resetControls() {
        joyPointer = -1;
        sliderPointer = -1;
        knobX = 0f;
        knobY = 0f;
        joyAiming = false;
        thrustLevel = 0f;
        throttle = 0f;
    }

    private void updatePlaying(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || tapped(btnPause)) {
            resetControls();
            state = PAUSED;
            saveRun();
            return;
        }
        updateJoystick();
        updateSlider();

        // Cualquier mando manual cancela el piloto automático.
        boolean keyTurn = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean keyThrust = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean keyFire = Gdx.input.isKeyPressed(Input.Keys.SPACE);
        boolean sliderHeld = sliderPointer >= 0 && thrustLevel > 0.01f;
        if (joyAiming || keyTurn || keyThrust || keyFire || sliderHeld) {
            world.pilot.cancel();
        }
        // La cámara debe estar ya en la nave para convertir los toques a coordenadas del mundo.
        worldCam.position.set(world.ship.pos.x, world.ship.pos.y, 0f);
        worldCam.update();
        boolean freeTap = handleTaps();

        Controls c = new Controls();
        c.turn = (Gdx.input.isKeyPressed(Input.Keys.LEFT) ? 1f : 0f)
                - (Gdx.input.isKeyPressed(Input.Keys.RIGHT) ? 1f : 0f);
        c.aiming = joyAiming;
        c.aimDeg = joyAimDeg;
        throttle = Math.max(thrustLevel, keyThrust ? 1f : 0f);
        c.thrust = throttle;
        c.fire = keyFire || freeTap;
        world.update(dt, c);
        if (world.pilot.active()) {
            // El piloto ha tomado los controles: la barra muestra su empuje.
            throttle = c.thrust;
        }
        if (world.isOver()) {
            bankCoins();
            resetControls();
            clearRun();
            state = OVER;
        }
    }

    /** Asigna el primer dedo nuevo que toque el joystick y calcula el rumbo. */
    private void updateJoystick() {
        if (joyPointer >= 0 && !Gdx.input.isTouched(joyPointer)) {
            joyPointer = -1;
        }
        if (joyPointer < 0) {
            for (int i = 0; i < wasDown.length; i++) {
                if (Gdx.input.isTouched(i) && !wasDown[i] && i != sliderPointer
                        && pointerInCircle(joyCx, joyCy, joyR * 1.5f, i)) {
                    joyPointer = i;
                    break;
                }
            }
        }
        joyAiming = false;
        knobX = 0f;
        knobY = 0f;
        if (joyPointer >= 0) {
            touch.set(Gdx.input.getX(joyPointer), Gdx.graphics.getHeight() - Gdx.input.getY(joyPointer));
            float dx = touch.x - joyCx;
            float dy = touch.y - joyCy;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > joyR) {
                dx *= joyR / len;
                dy *= joyR / len;
                len = joyR;
            }
            knobX = dx;
            knobY = dy;
            joyAiming = len > joyR * 0.2f;
            joyAimDeg = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        }
    }

    /** La barra de aceleración sigue al dedo y conserva su nivel al soltarlo. */
    private void updateSlider() {
        if (sliderPointer >= 0 && !Gdx.input.isTouched(sliderPointer)) {
            sliderPointer = -1;
        }
        if (sliderPointer < 0) {
            for (int i = 0; i < wasDown.length; i++) {
                if (Gdx.input.isTouched(i) && !wasDown[i] && i != joyPointer && pointerIn(sliderZone, i)) {
                    sliderPointer = i;
                    break;
                }
            }
        }
        if (sliderPointer >= 0) {
            touch.set(Gdx.input.getX(sliderPointer), Gdx.graphics.getHeight() - Gdx.input.getY(sliderPointer));
            thrustLevel = MathUtils.clamp((touch.y - btnThrust.bounds.y) / btnThrust.bounds.height, 0f, 1f);
        }
    }

    /**
     * Toques nuevos fuera de los controles. Sobre un bidón o meteorito activan el piloto automático;
     * en el espacio libre cancelan el piloto y disparan. Devuelve true si hubo disparo manual.
     */
    private boolean handleTaps() {
        boolean free = false;
        for (int i = 0; i < wasDown.length; i++) {
            if (!Gdx.input.isTouched(i) || wasDown[i] || i == joyPointer || i == sliderPointer || onControl(i)) {
                continue;
            }
            if (!pickTarget(worldPoint(i))) {
                free = true;
            }
        }
        if (free) {
            world.pilot.cancel();
        }
        return free;
    }

    private final Vector2 worldTouch = new Vector2();

    /** Convierte la posición de un dedo en coordenadas del mundo. */
    private Vector2 worldPoint(int pointer) {
        worldTouch.set(Gdx.input.getX(pointer), Gdx.input.getY(pointer));
        worldView.unproject(worldTouch);
        return worldTouch;
    }

    /** Elige el bidón o meteorito más cercano al toque (con margen) y lo asigna al piloto. */
    private boolean pickTarget(Vector2 p) {
        final float slop = 30f;
        Asteroid rock = null;
        float rockD = slop;
        for (int i = 0; i < world.asteroids.size; i++) {
            Asteroid a = world.asteroids.get(i);
            float d = a.pos.dst(p) - a.radius;
            if (d < rockD) {
                rockD = d;
                rock = a;
            }
        }
        FuelPickup cell = null;
        float cellD = slop;
        for (int i = 0; i < world.fuel.size; i++) {
            FuelPickup f = world.fuel.get(i);
            float d = f.pos.dst(p) - FuelPickup.RADIUS;
            if (d < cellD) {
                cellD = d;
                cell = f;
            }
        }
        if (rock == null && cell == null) {
            return false;
        }
        if (rock != null && (cell == null || rockD <= cellD)) {
            world.pilot.target(rock);
        } else {
            world.pilot.target(cell);
        }
        return true;
    }

    private boolean onControl(int pointer) {
        return pointerInCircle(joyCx, joyCy, joyR * 1.5f, pointer)
                || pointerIn(sliderZone, pointer)
                || hit(btnPause, pointer);
    }

    private void handlePause() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || tapped(btnResume)) {
            state = PLAYING;
        } else if (tapped(btnPauseShop)) {
            openShop(PAUSED);
        } else if (tapped(btnMenu)) {
            state = MENU;
            saveRun();
        }
    }

    private void handleShop() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || tapped(btnBack)) {
            state = shopReturn;
            return;
        }
        if (shopReturn == PAUSED && tapped(btnRefill) && canRefill()) {
            bankCoins();
            if (upgrades.spendCoins(upgrades.refillCost())) {
                world.ship.refuel(world.ship.maxFuel);
                upgrades.recordRefill();
                saveRun();
            }
            layoutUi();
            return;
        }
        for (int i = 0; i < buyButtons.size; i++) {
            if (tapped(buyButtons.get(i))) {
                upgrades.buy(i);
                layoutUi();
                break;
            }
        }
    }

    private void handleOver() {
        if (tapped(btnRetry)) {
            startRun();
        } else if (tapped(btnOverMenu)) {
            state = MENU;
        }
    }

    private void openShop(int from) {
        shopReturn = from;
        state = SHOP;
        layoutUi();
    }

    /** Suma al saldo guardado las monedas de la partida y las pone a cero. */
    private void bankCoins() {
        upgrades.addCoins(world.coinsEarned);
        world.coinsEarned = 0;
    }

    @Override
    public void pause() {
        if (state == PLAYING) {
            resetControls();
            state = PAUSED;
        }
        saveRun();
    }

    @Override
    public void resume() {
    }

    // ---------- dibujo: mundo ----------

    private void drawWorld() {
        worldCam.position.set(world.ship.pos.x, world.ship.pos.y, 0f);
        worldCam.update();
        worldView.apply();
        shapes.setProjectionMatrix(worldCam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawSpace();
        world.draw(shapes);
        shapes.end();
    }

    /** Fondo infinito: estrellas generadas por celdas a partir de sus coordenadas. */
    private void drawSpace() {
        float hw = worldView.getWorldWidth() / 2f + STAR_CELL;
        float hh = worldView.getWorldHeight() / 2f + STAR_CELL;
        float cx = worldCam.position.x;
        float cy = worldCam.position.y;
        shapes.rect(cx - hw, cy - hh, hw * 2f, hh * 2f, SKY_BOTTOM, SKY_BOTTOM, SKY_TOP, SKY_TOP);

        int x0 = (int) Math.floor((cx - hw) / STAR_CELL);
        int x1 = (int) Math.floor((cx + hw) / STAR_CELL);
        int y0 = (int) Math.floor((cy - hh) / STAR_CELL);
        int y1 = (int) Math.floor((cy + hh) / STAR_CELL);
        for (int gx = x0; gx <= x1; gx++) {
            for (int gy = y0; gy <= y1; gy++) {
                for (int k = 0; k < STARS_PER_CELL; k++) {
                    float sx = (gx + hash(gx, gy, k * 3)) * STAR_CELL;
                    float sy = (gy + hash(gx, gy, k * 3 + 1)) * STAR_CELL;
                    float size = 1f + hash(gx, gy, k * 3 + 2) * 1.8f;
                    float tw = 0.5f + 0.5f * MathUtils.sin(time * 2f + hash(gx, gy, k) * 10f);
                    shapes.setColor(1f, 1f, 1f, 0.25f + 0.5f * tw);
                    shapes.circle(sx, sy, size, 6);
                }
            }
        }
    }

    /** Número pseudoaleatorio en [0, 1) determinista para una celda y un índice. */
    private static float hash(int x, int y, int k) {
        int h = x * 374761393 + y * 668265263 + k * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h = h ^ (h >>> 16);
        return (h & 0xffffff) / (float) 0x1000000;
    }

    // ---------- dibujo: HUD y menús (coordenadas de pantalla) ----------

    private void drawHud() {
        hudView.apply();
        float w = hudView.getWorldWidth();
        float h = hudView.getWorldHeight();
        shapes.setProjectionMatrix(hudCam.combined);
        batch.setProjectionMatrix(hudCam.combined);

        if (state == MENU) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.rect(0f, 0f, w, h, SKY_BOTTOM, SKY_BOTTOM, SKY_TOP, SKY_TOP);
            shapes.end();
            drawTitleScreen(w, h);
        } else if (state == SHOP) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.rect(0f, 0f, w, h, SKY_BOTTOM, SKY_BOTTOM, SKY_TOP, SKY_TOP);
            shapes.end();
            drawShop(w, h);
        } else {
            drawPlayHud(w, h);
            if (state == PAUSED) {
                drawPause(w, h);
            } else if (state == OVER) {
                drawOver(w, h);
            }
        }
    }

    private void drawTitleScreen(float w, float h) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawButton(btnPlay, BTN_COLOR);
        drawButton(btnShop, BTN_COLOR);
        shapes.end();
        batch.begin();
        drawTextCentered("ASTEROIDES", w / 2f, h * 0.8f, 2.2f * u);
        drawTextCentered("Monedas: " + upgrades.coins(), w / 2f, h * 0.72f, 1.2f * u);
        drawButtonText(btnPlay);
        drawButtonText(btnShop);
        batch.end();
    }

    private void drawShop(float w, float h) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(HUD_PANEL);
        shapes.rect(10f, 10f, w - 20f, h - 20f);
        drawButton(btnBack, BTN_COLOR);
        for (int i = 0; i < buyButtons.size; i++) {
            Button b = buyButtons.get(i);
            boolean can = upgrades.canBuy(i);
            drawButton(b, can ? BTN_COLOR : BTN_OFF);
        }
        if (shopReturn == PAUSED) {
            drawButton(btnRefill, canRefill() ? BTN_COLOR : BTN_OFF);
        }
        shapes.end();
        batch.begin();
        drawTextCentered("TIENDA", w / 2f, h - 40f * u, 1.8f * u);
        drawTextLeft("Monedas: " + upgrades.coins(), 24f * u, h - 40f * u, 1.2f * u);
        for (int i = 0; i < buyButtons.size; i++) {
            float rowY = shopRowTop(i, h);
            drawTextLeft(Upgrades.NAMES[i] + " Nv " + upgrades.level(i) + "/" + Upgrades.MAX_LEVEL,
                    24f * u, rowY + 10f * u, 1.1f * u);
            drawTextLeft(Upgrades.DESCS[i], 24f * u, rowY - 14f * u, 0.9f * u);
            drawButtonText(buyButtons.get(i));
        }
        if (shopReturn == PAUSED) {
            drawButtonText(btnRefill);
        }
        drawButtonText(btnBack);
        batch.end();
    }

    private void drawPlayHud(float w, float h) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (state == PLAYING) {
            drawControls();
        }
        float panelH = 64f * u;
        shapes.setColor(HUD_PANEL);
        shapes.rect(0f, h - panelH, w, panelH);
        drawFuelBar(w, h, panelH);
        drawLifeDots(w, h, panelH);
        drawButton(btnPause, CTRL_BASE);
        shapes.end();

        batch.begin();
        drawTextLeft("Monedas: " + (upgrades.coins() + world.coinsEarned), 14f * u, h - 16f * u, 1.1f * u);
        drawTextLeft("Puntos: " + world.score, 14f * u, h - 40f * u, 0.9f * u);
        drawTextCentered("Oleada " + world.wave, w / 2f, h - 16f * u, 1f * u);
        drawButtonText(btnPause);
        if (state == PLAYING) {
            drawTextCentered("GIRO", joyCx, joyCy + joyR + 14f * u, 1f * u);
            drawTextCentered("ACEL", btnThrust.bounds.x + btnThrust.bounds.width / 2f,
                    btnThrust.bounds.y + btnThrust.bounds.height + 14f * u, 1f * u);
        }
        batch.end();
    }

    private void drawControls() {
        // Joystick: base y nudo.
        shapes.setColor(CTRL_BASE);
        shapes.circle(joyCx, joyCy, joyR, 40);
        shapes.setColor(CTRL_KNOB);
        shapes.circle(joyCx + knobX, joyCy + knobY, joyR * 0.42f, 24);

        // Barra de aceleración: relleno = nivel de acelerador; marca blanca = velocidad actual.
        float x = btnThrust.bounds.x;
        float y = btnThrust.bounds.y;
        float bw = btnThrust.bounds.width;
        float bh = btnThrust.bounds.height;
        float max = world.up.maxSpeed();
        float speedFrac = max > 0f ? MathUtils.clamp(world.ship.vel.len() / max, 0f, 1f) : 0f;
        shapes.setColor(CTRL_BASE);
        shapes.rect(x, y, bw, bh);
        shapes.setColor(CTRL_KNOB);
        shapes.rect(x, y, bw, bh * throttle);
        shapes.setColor(1f, 1f, 1f, 0.9f);
        shapes.rect(x - 4f * u, y + bh * speedFrac - 2f * u, bw + 8f * u, 4f * u);
    }

    private void drawFuelBar(float w, float h, float panelH) {
        float frac = world.ship.maxFuel > 0f ? world.ship.fuel / world.ship.maxFuel : 0f;
        float x = 14f * u;
        float bw = w * 0.3f;
        float y = h - panelH + 14f * u;
        float bh = 12f * u;
        shapes.setColor(0.15f, 0.15f, 0.2f, 1f);
        shapes.rect(x, y, bw, bh);
        shapes.setColor(frac > 0.3f ? new Color(0.3f, 0.9f, 0.5f, 1f) : new Color(1f, 0.4f, 0.3f, 1f));
        shapes.rect(x, y, bw * frac, bh);
    }

    private void drawLifeDots(float w, float h, float panelH) {
        int max = Math.max(1, world.ship.maxLives);
        for (int i = 0; i < max; i++) {
            float cx = w - 20f * u - i * 22f * u;
            float cy = h - panelH / 2f;
            shapes.setColor(i < world.ship.lives ? new Color(1f, 0.4f, 0.5f, 1f) : new Color(0.3f, 0.3f, 0.35f, 1f));
            shapes.circle(cx, cy, 7f * u, 16);
        }
    }

    private void drawPause(float w, float h) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(DIM);
        shapes.rect(0f, 0f, w, h);
        drawButton(btnResume, BTN_COLOR);
        drawButton(btnPauseShop, BTN_COLOR);
        drawButton(btnMenu, BTN_COLOR);
        shapes.end();
        batch.begin();
        drawTextCentered("PAUSA", w / 2f, h * 0.5f + 150f * u, 2f * u);
        drawButtonText(btnResume);
        drawButtonText(btnPauseShop);
        drawButtonText(btnMenu);
        batch.end();
    }

    private void drawOver(float w, float h) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(DIM);
        shapes.rect(0f, 0f, w, h);
        drawButton(btnRetry, BTN_COLOR);
        drawButton(btnOverMenu, BTN_COLOR);
        shapes.end();
        batch.begin();
        drawTextCentered("FIN DE PARTIDA", w / 2f, h * 0.5f + 150f * u, 2f * u);
        drawTextCentered("Monedas ganadas: " + world.coinsEarned, w / 2f, h * 0.5f + 90f * u, 1.2f * u);
        drawTextCentered("Puntos: " + world.score, w / 2f, h * 0.5f + 60f * u, 1.1f * u);
        drawButtonText(btnRetry);
        drawButtonText(btnOverMenu);
        batch.end();
    }

    private void drawButton(Button b, Color c) {
        b.drawShape(shapes, c);
    }

    private void drawButtonText(Button b) {
        b.drawText(batch, font, layout);
    }

    private void drawTextCentered(String text, float cx, float y, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        font.setColor(Color.WHITE);
        font.draw(batch, layout, cx - layout.width / 2f, y);
        font.getData().setScale(1f);
    }

    private void drawTextLeft(String text, float x, float y, float scale) {
        font.getData().setScale(scale);
        font.setColor(Color.WHITE);
        font.draw(batch, text, x, y);
        font.getData().setScale(1f);
    }

    // ---------- ciclo de vida ----------

    @Override
    public void dispose() {
        saveRun();
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}

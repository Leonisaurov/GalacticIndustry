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

import java.util.Locale;

/** Punto de entrada del juego: gestiona menú, partida, pausa, tienda y fin de partida. */
public class MyGame implements ApplicationListener {

    static final int MENU = 0;
    static final int PLAYING = 1;
    static final int PAUSED = 2;
    static final int SHOP = 3;
    static final int OVER = 4;
    static final int DYING = 5;

    private static final float MIN_W = 480f;
    private static final float MIN_H = 800f;
    private static final float STAR_CELL = 280f;
    private static final int STARS_PER_CELL = 3;

    private final Color tint = new Color();
    private final Color tint2 = new Color();

    private OrthographicCamera worldCam;
    private ExtendViewport worldView;
    private OrthographicCamera hudCam;
    private ScreenViewport hudView;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;
    private Visuals art;
    /** Composicion de perfil, sector y guardado. */
    private GameSession session;
    /** Alias del progreso de la sesion; no crea un segundo estado. */
    private Upgrades upgrades;
    /** Alias del sector activo de la sesion. */
    private GameWorld world;
    /** Guardado v2 con backup y migracion del formato anterior. */
    private SaveService saves;
    /** Snapshot del último personaje muerto: mundo y nave legado. */
    private SaveService legacySaves;
    private int characterSerial;
    private final Vector2 touch = new Vector2();

    private int state = MENU;
    private int shopReturn = MENU;
    private float time;
    private float u = 1f;
    private float btnTextScale = 3f;
    /** Tiempo de la secuencia de explosión antes del diálogo de fin de partida. */
    private float deathTimer;
    /** Monedas ganadas en la última partida (se muestran en el diálogo final). */
    private int overCoins;
    /** Modo de pruebas: nave inmortal + telemetría JSON en logcat. */
    private final boolean testMode;
    /** Solo para pruebas automatizadas: arranca y destruye la nave nada más empezar. */
    private final boolean debugDeath;
    /** Ralentiza la explosión final para poder capturar varios fotogramas de diseño. */
    private final boolean slowDeath;
    /** Arranca una partida con un objetivo cercano y el piloto activo (prueba de frenada). */
    private final boolean autopilotTest;
    /** Arranca con alas y sistemas dañados para probar la reparación y el dibujo. */
    private final boolean wingTest;
    /** Arranca con O2 bajo, salud dañada y fuga para probar el HUD de supervivencia. */
    private final boolean survivalTest;
    /** Arranca con el piloto incapacitado para probar la muerte por asfixia. */
    private final boolean pilotDeathTest;
    /** Cualquier modo de prueba activa la telemetría estructurada. */
    private final boolean debugMode;
    private float telemetryTimer;
    private int lastState = -1;
    private static final String TAG = "AsteroidsTelemetry";

    private static final Color PLANET_GLOW = new Color(0.28f, 0.55f, 1f, 1f);
    private static final Color ENGINE_GLOW = new Color(1f, 0.45f, 0.12f, 1f);
    private static final Color HINT_TEXT = new Color(0.40f, 0.48f, 0.62f, 1f);
    private static final Color PANEL_BOTTOM_C = new Color(0.020f, 0.035f, 0.085f, 0.88f);
    private static final Color PANEL_TOP_C = new Color(0.070f, 0.110f, 0.230f, 0.93f);
    private static final Color SHOP_PANEL_C = new Color(0.020f, 0.035f, 0.080f, 0.86f);
    private static final Color REFILL_FILL_C = new Color(0.09f, 0.12f, 0.10f, 0.88f);
    private static final Color REFILL_EDGE_C = new Color(0.30f, 0.62f, 0.42f, 0.40f);
    private static final Color REPAIR_FILL_C = new Color(0.12f, 0.10f, 0.06f, 0.88f);
    private static final Color REPAIR_EDGE_C = new Color(0.88f, 0.62f, 0.24f, 0.42f);
    private static final Color HULL_TRACK_C = new Color(0.04f, 0.07f, 0.12f, 0.95f);
    private static final Color OVER_EDGE_C = new Color(0.80f, 0.30f, 0.35f, 0.55f);
    private static final Color FUEL_TRACK_C = new Color(0.03f, 0.05f, 0.10f, 0.95f);
    private static final Color FUEL_EDGE_C = new Color(0.22f, 0.38f, 0.58f, 0.55f);
    private static final Color CONTROL_TRACK_C = new Color(0.04f, 0.07f, 0.14f, 0.62f);
    private static final Color CONTROL_EDGE_C = new Color(0.28f, 0.55f, 0.85f, 0.30f);
    private static final Color COIN_CHIP_C = new Color(0.08f, 0.09f, 0.16f, 0.92f);

    private Button btnPlay, btnShop, btnResume, btnPauseShop, btnMenu, btnRetry, btnOverMenu, btnBack;
    /** Barra de aceleración: su rectángulo es btnThrust.bounds. */
    private Button btnThrust, btnPause;
    /** Recuperar la nave del personaje anterior cuando se está cerca. */
    private Button btnRecover;
    private final Array<Button> buyButtons = new Array<>();
    /** Botones de reparacion de cada parte (solo con una partida activa). */
    private final Array<Button> repairButtons = new Array<>();
    /** Recarga de gasolina en la tienda (solo durante la partida). */
    private Button btnRefill;
    /** Recarga de O2 en la tienda temporal (solo durante la partida). */
    private Button btnO2;
    /** Pestañas de la tienda: 0 mejoras, 1 reparaciones. */
    private Button btnTabUpgrades, btnTabRepairs;
    private int shopTab = 0;

    // Geometría de la tienda (se recalcula en layoutUi).
    private float shopListTop;
    private float shopRowH;
    private float shopRowGap;
    private int shopRows;

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
    /** Hay una partida empezada (guardada o en curso) que se puede reanudar. */
    private boolean runActive;

    public MyGame() {
        this(false, false, false, false, false);
    }

    public MyGame(boolean testMode, boolean debugDeath, boolean slowDeath) {
        this(testMode, debugDeath, slowDeath, false, false);
    }

    public MyGame(boolean testMode, boolean debugDeath, boolean slowDeath, boolean autopilotTest) {
        this(testMode, debugDeath, slowDeath, autopilotTest, false);
    }

    /**
     * @param testMode      nave inmortal y telemetría JSON para automatización.
     * @param debugDeath    arranca en partida y destruye la nave para probar la explosión.
     * @param slowDeath     ralentiza la explosión para capturar fotogramas de diseño.
     * @param autopilotTest arranca con un meteorito cercano y el piloto automático activo.
     * @param wingTest      arranca con un ala y varios sistemas dañados.
     */
    public MyGame(boolean testMode, boolean debugDeath, boolean slowDeath,
                  boolean autopilotTest, boolean wingTest) {
        this(testMode, debugDeath, slowDeath, autopilotTest, wingTest, false, false);
    }

    public MyGame(boolean testMode, boolean debugDeath, boolean slowDeath,
                  boolean autopilotTest, boolean wingTest, boolean survivalTest) {
        this(testMode, debugDeath, slowDeath, autopilotTest, wingTest, survivalTest, false);
    }

    /**
     * @param survivalTest   arranca con O2 bajo, salud dañada y fuga visible.
     * @param pilotDeathTest arranca con el piloto incapacitado para probar la muerte.
     */
    public MyGame(boolean testMode, boolean debugDeath, boolean slowDeath,
                  boolean autopilotTest, boolean wingTest, boolean survivalTest,
                  boolean pilotDeathTest) {
        this.testMode = testMode;
        this.debugDeath = debugDeath;
        this.slowDeath = slowDeath;
        this.autopilotTest = autopilotTest;
        this.wingTest = wingTest;
        this.survivalTest = survivalTest;
        this.pilotDeathTest = pilotDeathTest;
        this.debugMode = testMode || debugDeath || slowDeath || autopilotTest
                || wingTest || survivalTest || pilotDeathTest;
    }

    @Override
    public void create() {
        worldCam = new OrthographicCamera();
        worldView = new ExtendViewport(MIN_W, MIN_H, worldCam);
        hudCam = new OrthographicCamera();
        hudView = new ScreenViewport(hudCam);
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        art = new Visuals();
        art.create();
        // La fuente por defecto de gdx vive en el classpath del jar, y apkc no
        // empaqueta recursos de jars: se copia a assets/fonts y se carga de ahí.
        font = new BitmapFont(Gdx.files.internal("fonts/lsans-15.fnt"));
        // La fuente trae un espacio muy estrecho; se ensancha un poco para que las
        // frases cortas del HUD no se lean pegadas.
        font.getData().spaceXadvance = 7f;
        layout = new GlyphLayout();
        session = new GameSession();
        upgrades = session.upgrades();
        world = session.sector;
        world.immortal = testMode;
        saves = new SaveService(
                new SaveService.FileStorage(Gdx.files.local("save").file()), upgrades);
        legacySaves = new SaveService(
                new SaveService.FileStorage(Gdx.files.local("save").file()), upgrades,
                SaveService.LEGACY_SLOT);
        loadRun();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (debugDeath) {
            session.startRun();
            runActive = true;
            state = PLAYING;
            world.coinsEarned = 7;
            triggerDeath();
        } else if (autopilotTest) {
            session.startRun();
            runActive = true;
            state = PLAYING;
            world.asteroids.clear();
            world.fuel.clear();
            world.ship.heading = 0f;
            world.ship.vel.set(160f, 0f);
            Asteroid target = new Asteroid(420f, 0f, Asteroid.SMALL);
            target.vel.set(0f, 0f);
            world.asteroids.add(target);
            world.pilot.target(target);
            // Acelerador manual residual: el piloto debe limpiarlo al terminar.
            thrustLevel = 0.5f;
            throttle = 0.5f;
        } else if (wingTest) {
            session.startRun();
            runActive = true;
            state = PLAYING;
            world.asteroids.clear();
            world.fuel.clear();
            world.ship.heading = 0f;
            world.ship.part[Ship.WING_RIGHT] = 0f;
            world.ship.part[Ship.WING_LEFT] = 85f;
            world.ship.part[Ship.ENGINE] = 55f;
            world.ship.part[Ship.TANK] = 45f;
            world.ship.part[Ship.CANNON] = 70f;
            world.ship.lastDamaged = Ship.WING_RIGHT;
            world.ship.damageFlash = 2f;
            world.coinsEarned = 200;
        } else if (survivalTest) {
            session.startRun();
            runActive = true;
            state = PLAYING;
            world.asteroids.clear();
            world.fuel.clear();
            world.ship.part[Ship.HULL] = world.ship.maxHull * 0.5f;
            session.pilot.oxygen = 24f;
            session.pilot.health = 70f;
            session.lifeSupport.breach = true;
            session.lifeSupport.updateLeak(world.ship);
        } else if (pilotDeathTest) {
            session.startRun();
            runActive = true;
            state = PLAYING;
            world.asteroids.clear();
            world.fuel.clear();
            session.pilot.oxygen = 0f;
            session.pilot.health = 0f;
            session.pilot.oxygenGrace = 0f;
            session.pilot.status = Pilot.Status.INCAPACITATED;
        }
    }

    /** El casco llegó a 0: la nave explota y termina la partida. */
    private void triggerDeath() {
        finishRun(true, "hull");
    }

    /** El piloto llegó a 0 HP o murió asfixiado: termina la partida sin explosión. */
    private void triggerPilotDeath() {
        finishRun(false, "pilot");
    }

    /**
     * Cierra la partida y guarda el mundo/nave legado. El personaje no se
     * recupera; el siguiente personaje entrará en el mismo mundo.
     */
    private void finishRun(boolean shipExploded, String cause) {
        overCoins = world.coinsEarned;
        bankCoins();
        resetControls();
        if (shipExploded) {
            world.ship.part[Ship.HULL] = 0f;
        }
        session.pilot.health = 0f;
        session.pilot.status = Pilot.Status.DEAD;
        world.markLegacyWreck(world.ship);
        if (legacySaves != null) {
            legacySaves.save(session);
        }
        clearRun();
        if (shipExploded) {
            world.beginDeath();
        } else {
            world.pilot.cancel();
            world.fx.clear();
            world.ship.wrecked = true;
            world.shake = 0f;
        }
        deathTimer = 0f;
        state = DYING;
        logEvent(String.format(Locale.US,
                "{\"event\":\"death\",\"cause\":\"%s\",\"coins\":%d,\"score\":%d,"
                        + "\"wave\":%d,\"legacyX\":%.1f,\"legacyY\":%.1f}",
                cause, overCoins, world.score, world.wave,
                world.legacyWreckPos.x, world.legacyWreckPos.y));
    }

    /** Si hay una partida guardada, la carga en pausa para que el jugador decida cuándo seguir. */
    private void loadRun() {
        SaveService.LoadStatus status = saves.load(session);
        if (status == SaveService.LoadStatus.V2
                || status == SaveService.LoadStatus.BACKUP
                || status == SaveService.LoadStatus.MIGRATED) {
            runActive = true;
            state = PAUSED;
            if (status != SaveService.LoadStatus.V2) {
                logEvent("{\"event\":\"save_recovered\",\"source\":\""
                        + status.name().toLowerCase(Locale.US) + "\"}");
            }
        } else if (status == SaveService.LoadStatus.INVALID) {
            logEvent("{\"event\":\"save_invalid\"}");
        }
    }

    /** Guarda la partida en curso (la pausa, la tienda y el cierre de la app la llaman). */
    private void saveRun() {
        if (runActive && !world.isOver()) {
            saves.save(session);
        }
    }

    /** Borra la partida guardada al terminar la partida. */
    private void clearRun() {
        if (saves != null) {
            saves.clear();
        }
        session.clearRun();
        runActive = false;
    }

    @Override
    public void resize(int width, int height) {
        worldView.update(width, height, true);
        hudView.update(width, height, true);
        u = Math.max(1f, Gdx.graphics.getDensity());
        btnTextScale = 1.08f * u;
        layoutUi();
    }

    /** Calcula posiciones de controles y botones en coordenadas de pantalla. */
    private void layoutUi() {
        float w = hudView.getWorldWidth();
        float h = hudView.getWorldHeight();
        float pad = 16f * u;
        float bw = Math.min(w * 0.72f, 290f * u);
        float bh = 56f * u;
        float gap = 18f * u;
        float cx = (w - bw) * 0.5f;

        float menuCy = h * 0.33f;
        btnPlay = stacked(runActive ? "CONTINUAR" : "JUGAR", cx, menuCy, bw, bh, gap, 0, 2);
        btnShop = stacked("TIENDA", cx, menuCy, bw, bh, gap, 1, 2);

        float pauseCy = h * 0.44f;
        btnResume = stacked("CONTINUAR", cx, pauseCy, bw, bh, gap, 0, 3);
        btnPauseShop = stacked("TIENDA", cx, pauseCy, bw, bh, gap, 1, 3);
        btnMenu = stacked("MENU", cx, pauseCy, bw, bh, gap, 2, 3);

        float overCy = h * 0.41f;
        btnRetry = stacked("NUEVO PERSONAJE", cx, overCy, bw, bh, gap, 0, 2);
        btnOverMenu = stacked("MENU", cx, overCy, bw, bh, gap, 1, 2);
        btnBack = new Button("VOLVER", cx, pad, bw, bh);

        joyR = 70f * u;
        joyCx = pad + joyR * 1.06f;
        joyCy = pad + joyR * 1.06f;
        joyZone.set(joyCx - joyR * 1.45f, joyCy - joyR * 1.45f, joyR * 2.9f, joyR * 2.9f);

        float barW = 60f * u;
        float barH = Math.min(300f * u, h * 0.44f);
        btnThrust = new Button("", w - pad - barW, pad + 6f * u, barW, barH);
        sliderZone.set(btnThrust.bounds.x - 18f * u, btnThrust.bounds.y - 12f * u,
                barW + 36f * u, barH + 24f * u);

        float ps = 54f * u;
        float panelH = topPanelH();
        btnPause = new Button("", w - pad - ps, h - panelH * 0.5f - ps * 0.5f + 6f * u, ps, ps);
        btnRecover = new Button("RECUPERAR NAVE", cx + bw * 0.05f, h * 0.30f,
                bw * 0.9f, 46f * u);

        // Pestañas de la tienda: solo hay reparaciones si existe una partida.
        if (!runActive) {
            shopTab = 0;
        }
        float tabH = 38f * u;
        float tabY = h - 96f * u;
        float tabX = pad * 1.5f;
        float tabGap = 8f * u;
        float tabW = (w - 3f * pad - tabGap) * 0.5f;
        btnTabUpgrades = new Button("MEJORAS", tabX, tabY, tabW, tabH);
        btnTabRepairs = new Button("REPARAR", tabX + tabW + tabGap, tabY, tabW, tabH);

        // Filas de la tienda: mejoras o reparaciones + recarga.
        shopRows = shopRowCount();
        shopListTop = h - (runActive ? 112f : 74f) * u;
        float bottomLimit = pad + bh + 26f * u;
        float avail = shopListTop - bottomLimit;
        shopRowGap = 10f * u;
        shopRowH = Math.min(112f * u, (avail - (shopRows - 1) * shopRowGap) / shopRows);
        if (shopRowH < 56f * u) {
            shopRowH = Math.max(40f * u, (avail - (shopRows - 1) * shopRowGap) / shopRows);
        }
        float cardX = pad + 6f * u;
        float cardW = w - 2f * pad - 12f * u;
        float bbw = Math.min(158f * u, cardW * 0.40f);
        float bbh = Math.min(52f * u, Math.max(26f * u, shopRowH - 18f * u));

        buyButtons.clear();
        repairButtons.clear();
        btnRefill = null;
        btnO2 = null;
        if (shopTab == 0) {
            for (int i = 0; i < Upgrades.COUNT; i++) {
                buyButtons.add(shopButton(buyLabel(i), i, cardX, cardW, bbw, bbh));
            }
        } else {
            for (int i = 0; i < Ship.PART_COUNT; i++) {
                repairButtons.add(shopButton(repairLabel(i), i, cardX, cardW, bbw, bbh));
            }
            btnRefill = shopButton("RELLENAR " + upgrades.refillCost(),
                    Ship.PART_COUNT, cardX, cardW, bbw, bbh);
            btnO2 = shopButton(o2RefillLabel(),
                    Ship.PART_COUNT + 1, cardX, cardW, bbw, bbh);
        }
    }

    private Button shopButton(String label, int row, float cardX, float cardW, float bbw, float bbh) {
        float top = shopRowTop(row);
        return new Button(label,
                cardX + cardW - 10f * u - bbw,
                top - shopRowH + (shopRowH - bbh) * 0.5f, bbw, bbh);
    }

    private int shopRowCount() {
        return runActive && shopTab == 1 ? Ship.PART_COUNT + 2 : Upgrades.COUNT;
    }

    private float topPanelH() {
        return 112f * u;
    }

    /** La recarga solo se ofrece con una partida activa, deposito no lleno y saldo suficiente. */
    private boolean canRefill() {
        return runActive && world.ship.fuel < world.ship.maxFuel
                && upgrades.coins() + world.coinsEarned >= upgrades.refillCost();
    }

    /** Coste de rellenar O2 según lo que falte. */
    private int o2RefillCost() {
        float missing = Math.max(0f, session.pilot.o2Max - session.pilot.oxygen);
        return upgrades.o2RefillCost(missing);
    }

    private String o2RefillLabel() {
        int cost = o2RefillCost();
        return cost <= 0 ? "O2 OK" : "O2 " + cost;
    }

    private boolean canRefillO2() {
        return runActive && session.pilot.oxygen < session.pilot.o2Max - 0.01f
                && upgrades.coins() + world.coinsEarned >= o2RefillCost();
    }

    /** Hay una nave legado y el jugador está dentro de su radio de recuperación. */
    private boolean canRecoverLegacy() {
        if (state != PLAYING || !runActive || !world.legacyWreck) {
            return false;
        }
        float distance = world.legacyDistance(world.ship.pos);
        return distance >= 0f && distance <= GameWorld.LEGACY_RECOVERY_RADIUS;
    }

    /** Coste de reparar una parte: proporcional a la integridad que le falta. */
    private int repairCost(int index) {
        float missing = world.ship.partMax(index) - world.ship.part[index];
        if (missing < 1f) {
            return 0;
        }
        return Math.max(5, Math.round(missing * 0.70f));
    }

    private String repairLabel(int index) {
        int cost = repairCost(index);
        return cost == 0 ? "OK" : "REPARAR " + cost;
    }

    private boolean canRepair(int index) {
        return runActive && repairCost(index) > 0
                && upgrades.coins() + world.coinsEarned >= repairCost(index);
    }

    /** Repara por completo una parte y conserva el combustible que gana el deposito al ampliarse. */
    private void repairPart(int index) {
        float oldMax = world.ship.maxFuel;
        world.ship.repair(index);
        if (index == Ship.TANK) {
            world.ship.syncFuel(upgrades);
            float gained = world.ship.maxFuel - oldMax;
            if (gained > 0f) {
                world.ship.refuel(gained);
            }
        }
    }

    /** Aplica al casco/deposito de la partida en curso las mejoras compradas en la tienda. */
    private void applyUpgradeToRun(int index) {
        if (!runActive) {
            return;
        }
        if (index == Upgrades.BLINDAJE) {
            float oldMax = world.ship.maxHull;
            world.ship.maxHull = upgrades.hull();
            float gained = world.ship.maxHull - oldMax;
            if (gained > 0f) {
                world.ship.part[Ship.HULL] = Math.min(world.ship.maxHull,
                        world.ship.part[Ship.HULL] + gained);
            }
        } else if (index == Upgrades.DEPOSITO) {
            float oldMax = world.ship.maxFuel;
            world.ship.syncFuel(upgrades);
            float gained = world.ship.maxFuel - oldMax;
            if (gained > 0f) {
                world.ship.refuel(gained);
            }
        }
        saveRun();
    }

    private String buyLabel(int i) {
        return upgrades.level(i) >= Upgrades.MAX_LEVEL ? "MAX" : "COMPRAR " + upgrades.cost(i);
    }

    private Button stacked(String label, float cx, float cy, float bw, float bh, float gap, int index, int count) {
        float total = count * bh + (count - 1) * gap;
        float top = cy + total / 2f;
        float y = top - (index + 1) * bh - index * gap;
        return new Button(label, cx, y, bw, bh);
    }

    private float shopRowTop(int index) {
        return shopListTop - index * (shopRowH + shopRowGap);
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
            case DYING: updateDying(dt); break;
            default: handleMenu(); break;
        }

        if (state == PLAYING || state == PAUSED || state == OVER || state == DYING) {
            drawWorld();
        }
        drawHud();
        updateTouchState();
        telemetry(dt);
    }

    /** Estado en texto para la telemetría. */
    private static String stateName(int s) {
        switch (s) {
            case PLAYING: return "PLAYING";
            case PAUSED: return "PAUSED";
            case SHOP: return "SHOP";
            case OVER: return "OVER";
            case DYING: return "DYING";
            default: return "MENU";
        }
    }

    /** Nombre del modo del piloto para telemetría. */
    private static String pilotName(int mode) {
        switch (mode) {
            case Autopilot.FUEL: return "FUEL";
            case Autopilot.ASTEROID: return "ASTEROID";
            case Autopilot.STOP: return "STOP";
            default: return "NONE";
        }
    }

    /** Emite una línea JSON con el estado del juego; solo en modo de pruebas. */
    private void telemetry(float dt) {
        if (!debugMode) {
            return;
        }
        if (state != lastState) {
            lastState = state;
            Gdx.app.log(TAG, "{\"event\":\"state\",\"state\":\"" + stateName(state) + "\"}");
        }
        telemetryTimer += dt;
        if (telemetryTimer < 0.4f) {
            return;
        }
        telemetryTimer = 0f;
        StringBuilder partJson = new StringBuilder("[");
        for (int i = 0; i < Ship.PART_COUNT; i++) {
            if (i > 0) {
                partJson.append(',');
            }
            partJson.append(String.format(Locale.US, "%.1f", world.ship.part[i]));
        }
        partJson.append(']');
        Gdx.app.log(TAG, String.format(Locale.US,
                "{\"event\":\"status\",\"state\":\"%s\",\"fps\":%d,\"coins\":%d,\"banked\":%d,"
                        + "\"score\":%d,\"wave\":%d,\"hull\":%.1f,\"maxHull\":%.1f,"
                        + "\"parts\":%s,"
                        + "\"fuel\":%.1f,\"maxFuel\":%.1f,\"throttle\":%.2f,\"speed\":%.1f,"
                        + "\"asteroids\":%d,\"pickups\":%d,\"missiles\":%d,"
                        + "\"ignoredHits\":%d,\"x\":%.1f,\"y\":%.1f,\"w\":%d,\"h\":%d,"
                        + "\"pilot\":\"%s\",\"immortal\":%s,\"shopReturn\":\"%s\",\"overCoins\":%d,"
                        + "\"pilotHealth\":%.1f,\"pilotMaxHealth\":%.1f,"
                        + "\"pilotOxygen\":%.1f,\"pilotO2Max\":%.1f,\"pilotStatus\":\"%s\","
                        + "\"pilotGrace\":%.1f,\"pilotLeak\":%.1f,\"pilotBreach\":%s,"
                        + "\"o2Alert\":%d,\"pilotO2Seconds\":%.0f,"
                        + "\"legacyWreck\":%s,\"legacyX\":%.1f,\"legacyY\":%.1f,"
                        + "\"legacyNear\":%s,\"legacyDistance\":%.1f}",
                stateName(state), Gdx.graphics.getFramesPerSecond(),
                upgrades.coins() + world.coinsEarned, upgrades.coins(),
                world.score, world.wave, world.ship.part[Ship.HULL], world.ship.maxHull,
                partJson,
                world.ship.fuel, world.ship.maxFuel, throttle, world.ship.vel.len(),
                world.asteroids.size, world.fuel.size, world.missiles.size,
                world.ignoredHits, world.ship.pos.x, world.ship.pos.y,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                pilotName(world.pilot.mode()), world.immortal, stateName(shopReturn), overCoins,
                session.pilot.health, session.pilot.maxHealth,
                session.pilot.oxygen, session.pilot.o2Max, session.pilot.status.name(),
                session.pilot.oxygenGrace, session.lifeSupport.leak,
                session.lifeSupport.breach, session.pilot.o2AlertLevel(),
                Math.min(999999f, session.lifeSupport.estimatedSeconds(
                        session.pilot, world.ship)),
                world.legacyWreck, world.legacyWreckPos.x, world.legacyWreckPos.y,
                world.legacyWreck && world.legacyDistance(world.ship.pos)
                        <= GameWorld.LEGACY_RECOVERY_RADIUS,
                world.legacyDistance(world.ship.pos)));
    }

    private void logEvent(String json) {
        if (debugMode) {
            Gdx.app.log(TAG, json);
        }
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
        boolean resume = runActive;
        resetControls();
        world.pilot.cancel();
        deathTimer = 0f;
        overCoins = 0;
        worldCam.zoom = 1f;
        if (!runActive) {
            if (legacySaves != null && legacySaves.hasSave()) {
                startNewCharacter();
                return;
            }
            session.startRun();
            runActive = true;
        }
        state = PLAYING;
        logEvent("{\"event\":\"run_start\",\"resume\":" + resume + "}");
    }

    /**
     * Crea un personaje nuevo. Si hay un mundo legado, lo carga y deja la nave
     * anterior como resto; si no, empieza un universo nuevo.
     */
    private void startNewCharacter() {
        resetControls();
        world.pilot.cancel();
        deathTimer = 0f;
        overCoins = 0;
        worldCam.zoom = 1f;
        boolean legacyLoaded = loadLegacyWorld();
        session.profile.startNewCharacter(nextCharacterId(), PlayerProfile.DEFAULT_NAME);
        session.pilot.reset();
        session.lifeSupport.reset();
        if (!legacyLoaded) {
            world.start();
        }
        world.startNewCharacter(0f, 0f);
        runActive = true;
        state = PLAYING;
        if (saves.save(session) && legacyLoaded && legacySaves != null) {
            legacySaves.clear();
        }
        logEvent("{\"event\":\"new_character\",\"legacy\":" + legacyLoaded + "}");
    }

    /** Carga el último mundo de un personaje muerto en una sesión temporal. */
    private boolean loadLegacyWorld() {
        if (legacySaves == null || !legacySaves.hasSave()) {
            return false;
        }
        GameSession temp = new GameSession(new PlayerProfile(new Upgrades(true)));
        SaveService.LoadStatus status = legacySaves.load(temp);
        if (status == SaveService.LoadStatus.V2
                || status == SaveService.LoadStatus.BACKUP) {
            world.copyWorldFrom(temp.sector);
            return true;
        }
        return false;
    }

    private String nextCharacterId() {
        characterSerial++;
        return "pilot-" + System.currentTimeMillis() + "-" + characterSerial;
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
        if (canRecoverLegacy() && tapped(btnRecover)) {
            int salvage = world.recoverLegacyWreck(world.ship.pos);
            if (salvage >= 0) {
                world.coinsEarned += salvage;
                saveRun();
                logEvent("{\"event\":\"legacy_recovered\",\"salvage\":"
                        + salvage + "}");
            }
            return;
        }
        updateJoystick();
        updateSlider();
        boolean pilotWasActive = world.pilot.active();

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
        session.update(dt, c);
        if (world.pilot.active()) {
            // El piloto ha tomado los controles: la barra muestra su empuje.
            throttle = c.thrust;
        } else if (pilotWasActive && sliderPointer < 0 && !keyThrust) {
            // El piloto ha terminado (objetivo destruido/recogido): el acelerador
            // vuelve a 0 para que no siga gastando combustible en la dirección de frenada.
            thrustLevel = 0f;
            throttle = 0f;
        }
        if (world.isOver()) {
            triggerDeath();
        } else if (!session.pilot.alive()) {
            triggerPilotDeath();
        }
    }

    /** Secuencia de explosión final antes del diálogo. */
    private void updateDying(float dt) {
        float step = dt * (slowDeath ? 0.35f : 1f);
        deathTimer += step;
        world.updateDeath(step);
        if (deathTimer >= 2.45f) {
            state = OVER;
        }
    }

    /** Asigna el primer dedo nuevo que toque el joystick y calcula el rumbo. */
    private void updateJoystick() {        if (joyPointer >= 0 && !Gdx.input.isTouched(joyPointer)) {
            joyPointer = -1;
        }
        if (joyPointer < 0) {
            for (int i = 0; i < wasDown.length; i++) {
                if (Gdx.input.isTouched(i) && !wasDown[i] && i != sliderPointer
                        && pointerInCircle(joyCx, joyCy, joyR * 1.45f, i)) {
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
        // El piloto toma el mando: el acelerador manual no debe reactivarse al terminar.
        if (sliderPointer < 0) {
            thrustLevel = 0f;
            throttle = 0f;
        }
        return true;
    }

    private boolean onControl(int pointer) {
        return pointerInCircle(joyCx, joyCy, joyR * 1.45f, pointer)
                || pointerIn(sliderZone, pointer)
                || hit(btnPause, pointer)
                || (canRecoverLegacy() && hit(btnRecover, pointer));
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
        if (runActive && tapped(btnTabUpgrades)) {
            if (shopTab != 0) {
                shopTab = 0;
                layoutUi();
            }
            return;
        }
        if (runActive && tapped(btnTabRepairs)) {
            if (shopTab != 1) {
                shopTab = 1;
                layoutUi();
            }
            return;
        }
        if (shopTab == 1) {
            if (btnRefill != null && tapped(btnRefill) && canRefill()) {
                if (upgrades.spendCoins(upgrades.refillCost())) {
                    world.ship.refuel(world.ship.maxFuel);
                    upgrades.recordRefill();
                    saveRun();
                    logEvent("{\"event\":\"refill\",\"coins\":" + upgrades.coins() + "}");
                }
                layoutUi();
                return;
            }
            if (btnO2 != null && tapped(btnO2) && canRefillO2()) {
                int cost = o2RefillCost();
                if (upgrades.spendCoins(cost)) {
                    session.pilot.refillOxygen();
                    saveRun();
                    logEvent(String.format(Locale.US,
                            "{\"event\":\"o2_refill\",\"cost\":%d,\"coins\":%d}",
                            cost, upgrades.coins()));
                }
                layoutUi();
                return;
            }
            for (int i = 0; i < repairButtons.size; i++) {
                if (!tapped(repairButtons.get(i))) {
                    continue;
                }
                int cost = repairCost(i);
                if (cost > 0) {
                    if (upgrades.spendCoins(cost)) {
                        repairPart(i);
                        saveRun();
                        logEvent(String.format(Locale.US,
                                "{\"event\":\"repair\",\"part\":%d,\"cost\":%d,\"coins\":%d}",
                                i, cost, upgrades.coins()));
                    }
                }
                layoutUi();
                return;
            }
            return;
        }
        for (int i = 0; i < buyButtons.size; i++) {
            if (tapped(buyButtons.get(i))) {
                if (upgrades.buy(i)) {
                    applyUpgradeToRun(i);
                    logEvent(String.format(Locale.US,
                            "{\"event\":\"buy\",\"index\":%d,\"level\":%d,\"coins\":%d}",
                            i, upgrades.level(i), upgrades.coins()));
                }
                layoutUi();
                break;
            }
        }
    }

    private void handleOver() {
        if (tapped(btnRetry)) {
            startNewCharacter();
        } else if (tapped(btnOverMenu)) {
            state = MENU;
        }
    }

    private void openShop(int from) {
        shopReturn = from;
        state = SHOP;
        shopTab = 0;
        // Las monedas de la partida se ingresan al entrar para poder gastarlas
        // en mejoras, recargas y reparaciones sin dejar dos saldos distintos.
        bankCoins();
        saveRun();
        layoutUi();
        logEvent("{\"event\":\"shop\",\"from\":\"" + stateName(from) + "\"}");
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

    /** Deja el estado de mezcla normal y arranca ShapeRenderer. */
    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void drawWorld() {
        float sx = 0f;
        float sy = 0f;
        if (world.shake > 0.01f) {
            sx = MathUtils.sin(time * 73f) * world.shake;
            sy = MathUtils.cos(time * 91f) * world.shake * 0.7f;
        }
        float camX = world.ship.pos.x + sx;
        float camY = world.ship.pos.y + sy;
        worldCam.zoom = (state == DYING || state == OVER)
                ? 1f + 0.16f * MathUtils.clamp(deathTimer / 1.5f, 0f, 1f)
                : 1f;
        worldCam.position.set(camX, camY, 0f);
        worldCam.update();
        worldView.apply();

        float ww = worldView.getWorldWidth() * worldCam.zoom;
        float wh = worldView.getWorldHeight() * worldCam.zoom;
        shapes.setProjectionMatrix(worldCam.combined);
        batch.setProjectionMatrix(worldCam.combined);

        beginShapes();
        drawSpaceGradient(camX, camY, ww, wh);
        shapes.end();

        batch.begin();
        Visuals.additive(batch);
        drawNebula(camX, camY, ww, wh, 1f, 1.15f);
        drawStars(camX, camY, ww, wh, 1f, 1f);
        batch.end();
        Visuals.normal(batch);

        beginShapes();
        world.draw(shapes);
        shapes.end();

        batch.begin();
        Visuals.additive(batch);
        world.drawGlow(batch, art);
        batch.end();
        Visuals.normal(batch);
    }

    private void drawSpaceGradient(float camX, float camY, float ww, float wh) {
        float hw = ww * 0.5f + STAR_CELL;
        float hh = wh * 0.5f + STAR_CELL;
        shapes.rect(camX - hw, camY - hh, hw * 2f, hh * 2f,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
    }

    /** Estrellas en tres capas de parallax. unit = unidades del mundo por unidad de dibujo. */
    private void drawStars(float camX, float camY, float ww, float wh, float unit, float intensity) {
        float hw = ww * 0.5f;
        float hh = wh * 0.5f;
        for (int layer = 0; layer < 3; layer++) {
            float par = 0.22f + 0.28f * layer;
            float ox = camX * par;
            float oy = camY * par;
            float cell = STAR_CELL * unit;
            int x0 = (int) Math.floor((ox - hw) / cell) - 1;
            int x1 = (int) Math.floor((ox + hw) / cell) + 1;
            int y0 = (int) Math.floor((oy - hh) / cell) - 1;
            int y1 = (int) Math.floor((oy + hh) / cell) + 1;
            for (int gx = x0; gx <= x1; gx++) {
                for (int gy = y0; gy <= y1; gy++) {
                    for (int k = 0; k < STARS_PER_CELL; k++) {
                        float h1 = hash(gx, gy, k * 7 + layer * 131);
                        float h2 = hash(gx, gy, k * 11 + layer * 197 + 5);
                        float h3 = hash(gx, gy, k * 17 + layer * 251 + 9);
                        float px = (gx + hash(gx, gy, k * 3 + layer * 41)) * cell - ox;
                        float py = (gy + hash(gx, gy, k * 5 + layer * 67 + 2)) * cell - oy;
                        float tw = 0.55f + 0.45f * MathUtils.sin(time * 1.7f + h1 * 31f);
                        float size = (1.8f + h2 * 3.2f + layer * 0.9f) * unit;
                        float alpha = (0.30f + layer * 0.20f + 0.25f * tw) * intensity;
                        Color c;
                        if (h3 < 0.08f) {
                            c = Visuals.WARN;
                        } else if (h3 < 0.30f) {
                            c = Visuals.ACCENT;
                        } else {
                            c = Color.WHITE;
                        }
                        art.star(batch, camX + px, camY + py, size, c, alpha);
                        if (layer == 2 && h3 > 0.965f) {
                            float big = size * 3.4f;
                            art.star(batch, camX + px, camY + py, big, c, alpha * 0.45f);
                            art.glow(batch, camX + px, camY + py, big * 1.7f, c, alpha * 0.18f);
                        }
                    }
                }
            }
        }
    }

    /** Nubes de nebulosa muy tenues para dar color y profundidad al fondo. */
    private void drawNebula(float camX, float camY, float ww, float wh, float unit, float intensity) {
        float hw = ww * 0.5f;
        float hh = wh * 0.5f;
        float cell = 1450f * unit;
        for (int layer = 0; layer < 3; layer++) {
            float par = 0.10f + 0.09f * layer;
            float ox = camX * par;
            float oy = camY * par;
            int x0 = (int) Math.floor((ox - hw) / cell) - 1;
            int x1 = (int) Math.floor((ox + hw) / cell) + 1;
            int y0 = (int) Math.floor((oy - hh) / cell) - 1;
            int y1 = (int) Math.floor((oy + hh) / cell) + 1;
            for (int gx = x0; gx <= x1; gx++) {
                for (int gy = y0; gy <= y1; gy++) {
                    float h = hash(gx, gy, 701 + layer * 17);
                    if (h < 0.28f) {
                        continue;
                    }
                    float px = (gx + hash(gx, gy, 733 + layer * 17)) * cell - ox;
                    float py = (gy + hash(gx, gy, 767 + layer * 17)) * cell - oy;
                    float size = (760f + hash(gx, gy, 799 + layer * 17) * 980f) * unit;
                    Color c = h < 0.63f ? Visuals.NEBULA_PURPLE
                            : h < 0.82f ? Visuals.NEBULA_BLUE : Visuals.NEBULA_TEAL;
                    float alpha = (0.080f + 0.100f * hash(gx, gy, 823 + layer * 17)) * intensity;
                    art.cloud(batch, camX + px, camY + py, size, size * 0.82f, c, alpha);
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
            drawMenu(w, h);
        } else if (state == SHOP) {
            drawShop(w, h);
        } else if (state == DYING) {
            drawDeathFlourish(w, h);
        } else {
            drawPlayHud(w, h);
            if (state == PAUSED) {
                drawPause(w, h);
            } else if (state == OVER) {
                drawOver(w, h);
            }
        }

        batch.begin();
        Visuals.normal(batch);
        art.vignette(batch, w, h, 0.55f);
        if ((state == PLAYING || state == PAUSED) && world.ship.invulnerable > 0f) {
            float pulse = 0.35f + 0.65f * Math.abs(MathUtils.sin(time * 16f));
            art.vignetteTint(batch, w, h, 1f, 0.16f, 0.22f, 0.34f * pulse);
        }
        batch.end();
    }

    private void drawMenu(float w, float h) {
        float camX = time * 16f * u;
        float camY = time * 7f * u;
        float planetX = w * 0.93f;
        float planetY = h * 0.92f;
        float planetR = w * 0.16f;

        float bob = MathUtils.sin(time * 1.1f) * 5f * u;
        float shipX = w * 0.5f;
        float shipY = h * 0.635f + bob;
        float shipHeading = 22f + MathUtils.sin(time * 0.55f) * 5f;
        float shipSize = 52f * u;
        float engX = shipX - MathUtils.cosDeg(shipHeading) * shipSize * 1.05f;
        float engY = shipY - MathUtils.sinDeg(shipHeading) * shipSize * 1.05f;
        btnPlay.label = runActive ? "CONTINUAR" : "JUGAR";

        beginShapes();
        shapes.rect(0f, 0f, w, h,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
        shapes.end();

        batch.begin();
        Visuals.additive(batch);
        drawNebula(camX, camY, w, h, u, 1.15f);
        drawStars(camX, camY, w, h, u, 1.05f);
        art.glow(batch, planetX, planetY, planetR * 2.8f,
                PLANET_GLOW, 0.22f);
        art.glow(batch, w * 0.5f, h * 0.835f, w * 1.0f, Visuals.ACCENT, 0.07f);
        art.glow(batch, engX, engY, shipSize * 2.6f, ENGINE_GLOW, 0.48f);
        art.glow(batch, engX, engY, shipSize * 1.1f, Color.WHITE, 0.50f);
        batch.end();
        Visuals.normal(batch);

        beginShapes();
        drawPlanet(planetX, planetY, planetR);
        Ship.drawBody(shapes, shipX, shipY, shipHeading, shipSize, true, 1f, time);

        drawButton(btnPlay, Visuals.BTN_PRIMARY, true);
        drawButton(btnShop, Visuals.BTN_SECONDARY, true);
        coinChipShape(w * 0.5f, h * 0.725f);
        shapes.end();

        batch.begin();
        drawTitle("ASTEROIDES", w * 0.5f, h * 0.845f, 2.35f * u);
        textCentered("PILOTA. MINA. SOBREVIVE.", w * 0.5f, h * 0.795f, 0.95f * u, Visuals.ACCENT);
        coinChipText(w * 0.5f, h * 0.725f, upgrades.coins());
        drawButtonText(btnPlay, true);
        drawButtonText(btnShop, true);
        if (runActive) {
            textCentered("PARTIDA GUARDADA", w * 0.5f, h * 0.56f, 0.78f * u, Visuals.GOLD);
        }
        textCentered("JOYSTICK GIRA - BARRA ACELERA", w * 0.5f, h * 0.215f, 0.68f * u, Visuals.TEXT_DIM);
        textCentered("TOCA EL VACIO PARA DISPARAR - TOCA UNA ROCA PARA EL PILOTO",
                w * 0.5f, 22f * u, 0.62f * u, HINT_TEXT);
        batch.end();
    }

    private static final Color PLANET_LEFT = new Color();
    private static final Color PLANET_RIGHT = new Color();

    /** Planeta con bandas y sombreado lateral, dibujado con franjas horizontales. */
    private void drawPlanet(float cx, float cy, float r) {
        int bands = 44;
        for (int i = 0; i < bands; i++) {
            float t0 = i / (float) bands;
            float t1 = (i + 1) / (float) bands;
            float y0 = -r + 2f * r * t0;
            float y1 = -r + 2f * r * t1;
            float ym = (y0 + y1) * 0.5f;
            float half = (float) Math.sqrt(Math.max(0f, r * r - ym * ym));
            if (half <= 0f) {
                continue;
            }
            float band = 0.5f + 0.30f * MathUtils.sin(ym * 0.115f + 2.1f)
                    + 0.20f * MathUtils.sin(ym * 0.035f + 0.6f);
            float br = 0.20f + 0.13f * band;
            float bg = 0.28f + 0.16f * band;
            float bb = 0.52f + 0.22f * band;
            // Luz desde arriba-izquierda: borde izquierdo claro, derecho en sombra.
            PLANET_LEFT.set(Math.min(1f, br * 1.55f), Math.min(1f, bg * 1.55f),
                    Math.min(1f, bb * 1.45f), 1f);
            PLANET_RIGHT.set(br * 0.28f, bg * 0.28f, bb * 0.28f, 1f);
            shapes.rect(cx - half, cy + y0, half * 2f, (y1 - y0) + 1f,
                    PLANET_LEFT, PLANET_RIGHT, PLANET_RIGHT, PLANET_LEFT);
        }
    }

    private void drawShop(float w, float h) {
        float camX = time * 10f * u;
        float camY = time * 4f * u;
        beginShapes();
        shapes.rect(0f, 0f, w, h,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
        shapes.end();

        batch.begin();
        Visuals.additive(batch);
        drawNebula(camX, camY, w, h, u, 0.8f);
        drawStars(camX, camY, w, h, u, 0.8f);
        batch.end();
        Visuals.normal(batch);

        float pad = 16f * u;
        beginShapes();
        panel(pad * 0.5f, pad * 0.5f, w - pad, h - pad, 20f * u,
                SHOP_PANEL_C, Visuals.PANEL_EDGE);
        shapes.setColor(0.28f, 0.58f, 0.90f, 0.35f);
        shapes.rect(pad * 1.5f, h - 64f * u, w - 3f * pad, 2f * u);

        if (runActive) {
            drawButton(btnTabUpgrades,
                    shopTab == 0 ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY, true);
            drawButton(btnTabRepairs,
                    shopTab == 1 ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY, true);
        }
        if (shopTab == 0) {
            for (int i = 0; i < Upgrades.COUNT; i++) {
                drawShopCard(i);
            }
        } else {
            for (int i = 0; i < Ship.PART_COUNT; i++) {
                drawRepairCard(i);
            }
            drawRefillCard();
            drawO2Card();
        }
        float chipCx = w - pad * 1.5f - 75f * u;
        float chipCy = h - 34f * u;
        coinChipShape(chipCx, chipCy);
        drawButton(btnBack, Visuals.BTN_SECONDARY, true);
        shapes.end();

        batch.begin();
        textLeft("TIENDA", pad * 1.5f, h - 22f * u, 1.55f * u, Color.WHITE);
        coinChipText(chipCx, chipCy, upgrades.coins());
        if (runActive) {
            drawButtonText(btnTabUpgrades, true);
            drawButtonText(btnTabRepairs, true);
        }
        if (shopTab == 0) {
            for (int i = 0; i < Upgrades.COUNT; i++) {
                drawShopCardText(i);
            }
        } else {
            for (int i = 0; i < Ship.PART_COUNT; i++) {
                drawRepairCardText(i);
            }
            drawRefillCardText();
            drawO2CardText();
        }
        drawButtonText(btnBack, true);
        batch.end();
    }

    private float cardX() {
        return 22f * u;
    }

    private float cardW(float w) {
        return w - 44f * u;
    }

    private float rowTitleY(float top) {
        return top - Math.max(10f * u, shopRowH * 0.30f);
    }

    private float rowDescY(float top) {
        return top - Math.max(26f * u, shopRowH * 0.62f);
    }

    private void drawShopCard(int i) {
        float x = cardX();
        float w = cardW(hudView.getWorldWidth());
        float top = shopRowTop(i);
        float y = top - shopRowH;
        panel(x, y, w, shopRowH, 16f * u, Visuals.CARD, Visuals.CARD_EDGE);
        drawLevelPips(x + 18f * u, y + 2f * u, upgrades.level(i));
        drawButton(buyButtons.get(i), upgrades.canBuy(i) ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY,
                upgrades.canBuy(i));
    }

    private void drawShopCardText(int i) {
        float x = cardX();
        float top = shopRowTop(i);
        float maxW = buyButtons.get(i).bounds.x - (x + 18f * u) - 10f * u;
        textLeftFit(Upgrades.NAMES[i], x + 18f * u, rowTitleY(top), 1.02f * u, maxW, Visuals.TEXT);
        textLeftFit(Upgrades.DESCS[i], x + 18f * u, rowDescY(top), 0.72f * u, maxW, Visuals.TEXT_DIM);
        drawButtonText(buyButtons.get(i), upgrades.canBuy(i));
    }

    private void drawRepairCard(int i) {
        float x = cardX();
        float w = cardW(hudView.getWorldWidth());
        float top = shopRowTop(i);
        float y = top - shopRowH;
        panel(x, y, w, shopRowH, 16f * u, REPAIR_FILL_C, REPAIR_EDGE_C);
        float barX = x + 18f * u;
        float barY = y + 2f * u;
        float barW = repairButtons.get(i).bounds.x - barX - 12f * u;
        drawIntegrityBar(barX, barY, barW, Math.max(3f * u, shopRowH * 0.07f),
                world.ship.part[i] / world.ship.partMax(i));
        drawButton(repairButtons.get(i), canRepair(i) ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY,
                canRepair(i));
    }

    private void drawRepairCardText(int i) {
        float x = cardX();
        float top = shopRowTop(i);
        float maxW = repairButtons.get(i).bounds.x - (x + 18f * u) - 10f * u;
        float frac = world.ship.part[i] / world.ship.partMax(i);
        int pct = Math.round(frac * 100f);
        textLeftFit(Ship.PART_NAMES[i] + "  " + pct + "%", x + 18f * u,
                rowTitleY(top), 1.02f * u, maxW, Visuals.TEXT);
        textLeftFit(partEffect(i), x + 18f * u, rowDescY(top), 0.68f * u, maxW,
                frac < 0.35f ? Visuals.DANGER : Visuals.TEXT_DIM);
        drawButtonText(repairButtons.get(i), canRepair(i));
    }

    private void drawRefillCard() {
        float x = cardX();
        float w = cardW(hudView.getWorldWidth());
        float top = shopRowTop(Ship.PART_COUNT);
        float y = top - shopRowH;
        panel(x, y, w, shopRowH, 16f * u, REFILL_FILL_C, REFILL_EDGE_C);
        drawButton(btnRefill, canRefill() ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY, canRefill());
    }

    private void drawRefillCardText() {
        float x = cardX();
        float top = shopRowTop(Ship.PART_COUNT);
        float maxW = btnRefill.bounds.x - (x + 18f * u) - 10f * u;
        textLeftFit("RECARGA", x + 18f * u, rowTitleY(top), 1.02f * u, maxW, Visuals.TEXT);
        textLeftFit("Rellena el deposito al instante", x + 18f * u,
                rowDescY(top), 0.72f * u, maxW, Visuals.TEXT_DIM);
        drawButtonText(btnRefill, canRefill());
    }

    private void drawO2Card() {
        float x = cardX();
        float w = cardW(hudView.getWorldWidth());
        float top = shopRowTop(Ship.PART_COUNT + 1);
        float y = top - shopRowH;
        panel(x, y, w, shopRowH, 16f * u, REFILL_FILL_C, REFILL_EDGE_C);
        drawButton(btnO2, canRefillO2() ? Visuals.BTN_PRIMARY : Visuals.BTN_SECONDARY,
                canRefillO2());
    }

    private void drawO2CardText() {
        float x = cardX();
        float top = shopRowTop(Ship.PART_COUNT + 1);
        float maxW = btnO2.bounds.x - (x + 18f * u) - 10f * u;
        textLeftFit("OXIGENO", x + 18f * u, rowTitleY(top), 1.02f * u, maxW, Visuals.TEXT);
        textLeftFit("Rellena la reserva de O2", x + 18f * u,
                rowDescY(top), 0.72f * u, maxW, Visuals.TEXT_DIM);
        drawButtonText(btnO2, canRefillO2());
    }

    /** Texto corto del efecto de cada parte cuando esta danada. */
    private static String partEffect(int index) {
        switch (index) {
            case Ship.HULL: return "A 0 se destruye la nave";
            case Ship.ENGINE: return "Menos empuje y velocidad";
            case Ship.TANK: return "Menos capacidad y fugas";
            case Ship.CANNON: return "Menos dano y cadencia";
            case Ship.STEERING: return "Gira mas despacio";
            case Ship.LIFE_SUPPORT: return "Filtro de O2 al 20%";
            default: return "Se pierde el ala y el giro";
        }
    }

    private void drawLevelPips(float x, float y, int level) {
        float pipW = 12f * u;
        float pipH = 5f * u;
        float gap = 4f * u;
        for (int k = 0; k < Upgrades.MAX_LEVEL; k++) {
            if (k < level) {
                shapes.setColor(Visuals.ACCENT);
            } else {
                shapes.setColor(0.20f, 0.24f, 0.34f, 0.9f);
            }
            Visuals.roundRect(shapes, x + k * (pipW + gap), y, pipW, pipH, pipH * 0.5f);
        }
    }

    /** Barra de integridad de una parte (verde, ambar o rojo). */
    private void drawIntegrityBar(float x, float y, float w, float h, float frac) {
        shapes.setColor(HULL_TRACK_C);
        Visuals.roundRect(shapes, x, y, w, h, h * 0.5f);
        if (frac > 0.001f) {
            shapes.setColor(partColor(frac));
            Visuals.roundRect(shapes, x, y, Math.max(h, w * MathUtils.clamp(frac, 0f, 1f)), h, h * 0.5f);
        }
    }

    private static Color partColor(float frac) {
        if (frac > 0.55f) {
            return Visuals.GOOD;
        }
        return frac > 0.28f ? Visuals.WARN : Visuals.DANGER;
    }

    private void drawPlayHud(float w, float h) {
        float pad = 16f * u;
        float panelH = topPanelH();

        beginShapes();
        shapes.rect(0f, h - panelH, w, panelH,
                PANEL_BOTTOM_C,
                PANEL_BOTTOM_C,
                PANEL_TOP_C,
                PANEL_TOP_C);
        shapes.setColor(0.30f, 0.65f, 1f, 0.45f);
        shapes.rect(0f, h - panelH, w, 2f * u);
        shapes.setColor(0f, 0f, 0f, 0.35f);
        shapes.rect(0f, h - panelH - 4f * u, w, 4f * u);

        drawFuelBar(w, h, panelH);
        drawHullChip(w, h);
        drawSurvivalBars(w, h);
        drawPartAlerts(w, h, panelH);
        if (canRecoverLegacy()) {
            drawButton(btnRecover, Visuals.BTN_PRIMARY, true);
        }
        drawIconButton(btnPause);
        drawCoinIcon(pad + 18f * u, h - 54f * u, 13f * u);
        if (state == PLAYING) {
            drawControls();
        }
        shapes.end();

        drawHudGlow(w, h);

        batch.begin();
        textLeft(String.valueOf(upgrades.coins() + world.coinsEarned),
                pad + 38f * u, h - 44f * u, 1.15f * u, Visuals.GOLD);
        textCentered("OLEADA " + world.wave, w * 0.38f, h - 44f * u, 0.82f * u, Visuals.TEXT);
        float hullFrac = world.ship.part[Ship.HULL] / world.ship.maxHull;
        float hx = hullChipX(w);
        float hy = hullChipY(h);
        textLeft("CASCO", hx + 34f * u, hy + 31f * u, 0.55f * u, Visuals.TEXT_DIM);
        textLeft(Math.round(hullFrac * 100f) + "%", hx + 34f * u, hy + 17f * u,
                0.78f * u, partColor(hullFrac));
        textLeftMiddle("PUNTOS " + world.score, pad + 44f * u + fuelBarW(w) + 14f * u,
                h - 86f * u, 0.78f * u, Visuals.TEXT_DIM);
        float frac = world.ship.maxFuel > 0f ? world.ship.fuel / world.ship.maxFuel : 0f;
        textCenteredMiddle(Math.round(frac * 100f) + "%",
                pad + 44f * u + fuelBarW(w) * 0.5f, h - 86f * u, 0.78f * u, Color.WHITE);
        drawSurvivalText(w, h);
        if (world.ship.damageFlash > 0f && world.ship.lastDamaged > 0) {
            int hit = world.ship.lastDamaged;
            float hitFrac = world.ship.part[hit] / world.ship.partMax(hit);
            int pct = Math.round(hitFrac * 100f);
            textLeft("GOLPE EN " + Ship.PART_NAMES[hit] + "  " + pct + "%",
                    pad + 8f * u, alertY(h, panelH, 0) + 15f * u, 0.78f * u, partColor(hitFrac));
        }
        int o2Alert = session.pilot.o2AlertLevel();
        int o2Pct = Math.round(session.pilot.oxygenFraction() * 100f);
        if (o2Alert == 2) {
            textLeft("O2 CRITICO " + o2Pct + "%",
                    pad + 8f * u, h - 154f * u, 0.78f * u, Visuals.DANGER);
        } else if (o2Alert == 1) {
            textLeft("O2 BAJO " + o2Pct + "%",
                    pad + 8f * u, h - 154f * u, 0.78f * u, Visuals.WARN);
        }
        if (session.lifeSupport.leak > 0.01f || session.lifeSupport.breach) {
            textLeft(session.lifeSupport.breach ? "BRECHA O2" : "FUGA O2",
                    pad + 8f * u, h - 176f * u, 0.78f * u,
                    session.lifeSupport.breach ? Visuals.DANGER : Visuals.WARN);
        }
        int slot = 0;
        for (int i = 1; i < Ship.PART_COUNT; i++) {
            float pf = world.ship.part[i] / world.ship.partMax(i);
            if (pf >= 0.72f) {
                continue;
            }
            float ax = alertX(w);
            float ay = alertY(h, panelH, slot);
            int pct = Math.round(pf * 100f);
            textLeft(Ship.PART_NAMES[i] + " " + pct + "%",
                    ax + 14f * u, ay + 15f * u, 0.68f * u, partColor(pf));
            slot++;
        }
        if (canRecoverLegacy()) {
            drawButtonText(btnRecover, true);
            textCentered("NAVE LEGADO A "
                            + Math.round(world.legacyDistance(world.ship.pos)) + " u",
                    btnRecover.bounds.x + btnRecover.bounds.width * 0.5f,
                    btnRecover.bounds.y + btnRecover.bounds.height + 22f * u,
                    0.72f * u, Visuals.WARN);
        }
        if (state == PLAYING) {
            textCenteredMiddle("GIRO", joyCx, joyCy + joyR + 28f * u, 0.68f * u, Visuals.TEXT_DIM);
            textCenteredMiddle("ACEL", btnThrust.bounds.x + btnThrust.bounds.width * 0.5f,
                    btnThrust.bounds.y + btnThrust.bounds.height + 26f * u,
                    0.68f * u, Visuals.TEXT_DIM);
        }
        batch.end();
    }

    private float fuelBarW(float w) {
        float pad = 16f * u;
        return w - 2f * pad - 44f * u - 80f * u;
    }

    private void drawFuelBar(float w, float h, float panelH) {
        float pad = 16f * u;
        float barX = pad + 44f * u;
        float barY = h - 96f * u;
        float barH = 20f * u;
        float barW = fuelBarW(w);
        float frac = world.ship.maxFuel > 0f ? MathUtils.clamp(world.ship.fuel / world.ship.maxFuel, 0f, 1f) : 0f;

        drawFuelIcon(pad + 18f * u, barY + barH * 0.5f, 12f * u);
        panel(barX, barY, barW, barH, barH * 0.5f,
                FUEL_TRACK_C,
                FUEL_EDGE_C);
        if (frac > 0.001f) {
            Color fill;
            if (frac > 0.5f) {
                fill = tint.set(0.16f, 0.82f, 0.48f, 1f);
            } else if (frac > 0.25f) {
                fill = tint.set(1f, 0.70f, 0.22f, 1f);
            } else {
                float pulse = 0.75f + 0.25f * MathUtils.sin(time * 9f);
                fill = tint.set(1f, 0.30f, 0.34f, 1f).mul(pulse, pulse, pulse, 1f);
            }
            float inset = 4f * u;
            float fw = Math.max(barH - inset * 2f, (barW - inset * 2f) * frac);
            shapes.setColor(fill);
            Visuals.roundRect(shapes, barX + inset, barY + inset,
                    fw, barH - inset * 2f, (barH - inset * 2f) * 0.5f);
            shapes.setColor(1f, 1f, 1f, 0.20f);
            Visuals.roundRect(shapes, barX + inset + 2f * u, barY + barH * 0.55f,
                    Math.max(0f, fw - 4f * u), barH * 0.28f, barH * 0.14f);
        }
    }

    private float hullChipW() {
        return 96f * u;
    }

    private float hullChipH() {
        return 40f * u;
    }

    private float hullChipX(float w) {
        return btnPause.bounds.x - 12f * u - hullChipW();
    }

    private float hullChipY(float h) {
        return h - 74f * u;
    }

    private void drawHullChip(float w, float h) {
        float x = hullChipX(w);
        float y = hullChipY(h);
        float cw = hullChipW();
        float ch = hullChipH();
        float frac = world.ship.part[Ship.HULL] / world.ship.maxHull;
        panel(x, y, cw, ch, ch * 0.34f, COIN_CHIP_C, Visuals.PANEL_EDGE);
        drawShield(x + 18f * u, y + ch * 0.60f, 9f * u, partColor(frac));
        drawIntegrityBar(x + 34f * u, y + 6f * u, cw - 44f * u,
                Math.max(4f * u, ch * 0.15f), frac);
    }

    private float survivalBarH() {
        return 18f * u;
    }

    private float survivalGap() {
        return 10f * u;
    }

    private float survivalBarW(float w) {
        return (w - 32f * u - survivalGap()) * 0.5f;
    }

    private float survivalX1() {
        return 16f * u;
    }

    private float survivalX2(float w) {
        return survivalX1() + survivalBarW(w) + survivalGap();
    }

    private float survivalY(float h) {
        // Justo debajo del panel superior (112 u de alto).
        return h - 130f * u;
    }

    private void drawSurvivalBars(float w, float h) {
        float y = survivalY(h);
        float bh = survivalBarH();
        float bw = survivalBarW(w);
        float x1 = survivalX1();
        float x2 = survivalX2(w);
        float o2 = MathUtils.clamp(session.pilot.oxygenFraction(), 0f, 1f);
        float hp = MathUtils.clamp(session.pilot.healthFraction(), 0f, 1f);
        drawSurvivalBar(x1, y, bw, bh, o2, o2Color(o2), true);
        drawSurvivalBar(x2, y, bw, bh, hp, healthColor(hp), false);
    }

    private void drawSurvivalBar(float x, float y, float w, float h, float frac,
                                 Color fill, boolean oxygen) {
        boolean leaking = oxygen && (session.lifeSupport.leak > 0.01f
                || session.lifeSupport.breach);
        panel(x, y, w, h, h * 0.5f, COIN_CHIP_C,
                leaking ? Visuals.DANGER : Visuals.PANEL_EDGE);
        if (frac > 0.001f) {
            float inset = 3f * u;
            float fw = Math.max(h - inset * 2f, (w - inset * 2f) * frac);
            shapes.setColor(fill);
            Visuals.roundRect(shapes, x + inset, y + inset,
                    fw, h - inset * 2f, (h - inset * 2f) * 0.5f);
            shapes.setColor(1f, 1f, 1f, 0.18f);
            Visuals.roundRect(shapes, x + inset + 2f * u, y + h * 0.56f,
                    Math.max(0f, fw - 4f * u), h * 0.24f, h * 0.12f);
        }
        float iconX = x + h * 0.62f;
        float iconY = y + h * 0.5f;
        if (oxygen) {
            drawO2Icon(iconX, iconY, h * 0.24f, fill);
        } else {
            drawHealthIcon(iconX, iconY, h * 0.24f, fill);
        }
    }

    private Color o2Color(float frac) {
        if (session.pilot.o2AlertLevel() == 2) {
            return Visuals.DANGER;
        }
        if (session.pilot.o2AlertLevel() == 1) {
            return Visuals.WARN;
        }
        return Visuals.O2;
    }

    private Color healthColor(float frac) {
        return frac > 0.55f ? Visuals.HEALTH : frac > 0.28f ? Visuals.WARN : Visuals.DANGER;
    }

    private void drawO2Icon(float cx, float cy, float r, Color c) {
        shapes.setColor(c);
        shapes.circle(cx, cy, r, 10);
        shapes.setColor(0.02f, 0.06f, 0.11f, 1f);
        shapes.circle(cx, cy, r * 0.46f, 8);
    }

    private void drawHealthIcon(float cx, float cy, float r, Color c) {
        shapes.setColor(c);
        shapes.rect(cx - r * 0.22f, cy - r, r * 0.44f, r * 2f);
        shapes.rect(cx - r, cy - r * 0.22f, r * 2f, r * 0.44f);
    }

    private void drawSurvivalText(float w, float h) {
        float y = survivalY(h) + survivalBarH() * 0.5f;
        float x1 = survivalX1();
        float x2 = survivalX2(w);
        int o2Pct = Math.round(session.pilot.oxygenFraction() * 100f);
        int hpPct = Math.round(session.pilot.healthFraction() * 100f);
        textLeftMiddle("O2 " + o2Pct + "%", x1 + 26f * u, y, 0.66f * u, Color.WHITE);
        textLeftMiddle("HP " + hpPct + "%", x2 + 26f * u, y, 0.66f * u, Color.WHITE);
        if (session.lifeSupport.breach) {
            textLeftMiddle("BRECHA", x1 + survivalBarW(w) - 72f * u, y,
                    0.58f * u, Visuals.DANGER);
        } else if (session.lifeSupport.leak > 0.01f) {
            textLeftMiddle("FUGA", x1 + survivalBarW(w) - 56f * u, y,
                    0.58f * u, Visuals.WARN);
        }
    }

    private void drawShield(float cx, float cy, float r, Color c) {        shapes.setColor(0.03f, 0.06f, 0.12f, 0.95f);
        shieldShape(cx, cy, r * 1.18f);
        shapes.setColor(c);
        shieldShape(cx, cy, r);
        shapes.setColor(1f, 1f, 1f, 0.35f);
        shapes.circle(cx - r * 0.30f, cy + r * 0.30f, r * 0.20f, 8);
    }

    private void shieldShape(float cx, float cy, float r) {
        shapes.rect(cx - r * 0.78f, cy - r * 0.05f, r * 1.56f, r * 0.85f);
        shapes.triangle(cx - r * 0.78f, cy + r * 0.20f,
                cx + r * 0.78f, cy + r * 0.20f,
                cx, cy - r * 1.10f);
    }

    private float alertW() {
        return 168f * u;
    }

    private float alertH() {
        return 22f * u;
    }

    private float alertX(float w) {
        return w - 16f * u - alertW();
    }

    private float alertY(float h, float panelH, int slot) {
        return h - panelH - 32f * u - slot * 26f * u;
    }

    /** Avisos de los sistemas danados (el casco tiene su propio indicador). */
    private void drawPartAlerts(float w, float h, float panelH) {
        int slot = 0;
        for (int i = 1; i < Ship.PART_COUNT; i++) {
            float frac = world.ship.part[i] / world.ship.partMax(i);
            if (frac >= 0.72f) {
                continue;
            }
            float x = alertX(w);
            float y = alertY(h, panelH, slot);
            panel(x, y, alertW(), alertH(), alertH() * 0.45f, COIN_CHIP_C, Visuals.PANEL_EDGE);
            shapes.setColor(partColor(frac));
            Visuals.roundRect(shapes, x + 4f * u, y + 4f * u, 4f * u, alertH() - 8f * u, 2f * u);
            slot++;
        }
    }

    private void drawIconButton(Button b) {
        float x = b.bounds.x;
        float y = b.bounds.y;
        float w = b.bounds.width;
        float h = b.bounds.height;
        float r = h * 0.30f;
        boolean pressed = held(b);
        panel(x, y, w, h, r,
                pressed ? tint2.set(Visuals.BTN_PRIMARY).lerp(Color.WHITE, 0.12f) : Visuals.BTN_SECONDARY,
                Visuals.PANEL_EDGE);
        float bw = w * 0.10f;
        float bh = h * 0.42f;
        shapes.setColor(0.85f, 0.95f, 1f, 0.95f);
        Visuals.roundRect(shapes, x + w * 0.5f - bw * 1.6f, y + h * 0.5f - bh * 0.5f, bw, bh, bw * 0.5f);
        Visuals.roundRect(shapes, x + w * 0.5f + bw * 0.6f, y + h * 0.5f - bh * 0.5f, bw, bh, bw * 0.5f);
    }

    private void drawControls() {
        // Joystick: base, anillo, dirección y nudo.
        shapes.setColor(0.10f, 0.18f, 0.35f, 0.28f);
        shapes.circle(joyCx, joyCy, joyR, 40);
        shapes.setColor(0.30f, 0.65f, 1f, 0.30f);
        shapes.circle(joyCx, joyCy, joyR, 40);
        shapes.setColor(0.06f, 0.10f, 0.22f, 0.55f);
        shapes.circle(joyCx, joyCy, joyR - 3f * u, 40);
        shapes.setColor(0.30f, 0.65f, 1f, 0.18f);
        shapes.circle(joyCx, joyCy, joyR * 0.72f, 32);
        // Cuatro marcas cardinales.
        for (int i = 0; i < 4; i++) {
            float a = i * 90f;
            shapes.setColor(0.55f, 0.85f, 1f, 0.45f);
            shapes.circle(joyCx + MathUtils.cosDeg(a) * joyR * 0.84f,
                    joyCy + MathUtils.sinDeg(a) * joyR * 0.84f, 2.2f * u, 5);
        }
        if (joyAiming) {
            shapes.setColor(0.36f, 0.86f, 1f, 0.30f);
            shapes.rectLine(joyCx, joyCy, joyCx + knobX, joyCy + knobY, 4f * u);
        }
        shapes.setColor(0.10f, 0.30f, 0.55f, 0.95f);
        shapes.circle(joyCx + knobX, joyCy + knobY, joyR * 0.46f, 28);
        shapes.setColor(0.36f, 0.80f, 1f, 0.95f);
        shapes.circle(joyCx + knobX, joyCy + knobY, joyR * 0.38f, 24);
        shapes.setColor(0.80f, 0.96f, 1f, 0.65f);
        shapes.circle(joyCx + knobX - joyR * 0.10f, joyCy + knobY + joyR * 0.10f, joyR * 0.12f, 10);

        // Barra de aceleración.
        float x = btnThrust.bounds.x;
        float y = btnThrust.bounds.y;
        float bw = btnThrust.bounds.width;
        float bh = btnThrust.bounds.height;
        float max = world.up.maxSpeed();
        float speedFrac = max > 0f ? MathUtils.clamp(world.ship.vel.len() / max, 0f, 1f) : 0f;
        panel(x, y, bw, bh, bw * 0.46f,
                CONTROL_TRACK_C,
                CONTROL_EDGE_C);
        if (throttle > 0.01f) {
            shapes.setColor(0.14f, 0.52f, 0.82f, 0.30f);
            Visuals.roundRect(shapes, x + 4f * u, y + 4f * u,
                    bw - 8f * u, (bh - 8f * u) * throttle, bw * 0.40f);
            shapes.setColor(0.35f, 0.86f, 1f, 0.38f);
            shapes.rect(x + 7f * u, y + 7f * u, bw - 14f * u, (bh - 14f * u) * throttle);
        }
        // Marcas de nivel.
        for (int i = 1; i < 5; i++) {
            float ly = y + bh * i / 5f;
            shapes.setColor(1f, 1f, 1f, i == 5 ? 0.10f : 0.16f);
            shapes.rect(x + bw * 0.18f, ly, bw * 0.64f, 1.5f * u);
        }
        // Marca blanca de velocidad actual.
        shapes.setColor(1f, 1f, 1f, 0.90f);
        shapes.rect(x - 6f * u, y + bh * speedFrac - 2f * u, bw + 12f * u, 4f * u);
        shapes.setColor(1f, 0.75f, 0.35f, 0.90f);
        shapes.circle(x + bw * 0.5f, y + bh * speedFrac, 4f * u, 10);
        // Nudo del acelerador.
        float knobY = y + bh * throttle;
        shapes.setColor(0.10f, 0.30f, 0.55f, 0.95f);
        shapes.circle(x + bw * 0.5f, knobY, bw * 0.30f, 20);
        shapes.setColor(0.36f, 0.80f, 1f, 0.95f);
        shapes.circle(x + bw * 0.5f, knobY, bw * 0.22f, 16);
    }

    private void drawHudGlow(float w, float h) {
        batch.begin();
        Visuals.additive(batch);
        if (state == PLAYING) {
            art.glow(batch, joyCx + knobX, joyCy + knobY, joyR * 1.15f, Visuals.ACCENT, 0.10f);
            float x = btnThrust.bounds.x + btnThrust.bounds.width * 0.5f;
            float knobY = btnThrust.bounds.y + btnThrust.bounds.height * throttle;
            art.glow(batch, x, knobY, btnThrust.bounds.width * 0.85f, Visuals.ACCENT, 0.12f);
        }
        float barX = 16f * u + 44f * u;
        float barY = h - 96f * u;
        float frac = world.ship.maxFuel > 0f ? world.ship.fuel / world.ship.maxFuel : 0f;
        if (frac < 0.25f) {
            float pulse = 0.5f + 0.5f * MathUtils.sin(time * 9f);
            art.glow(batch, barX + fuelBarW(w) * 0.5f, barY + 10f * u, 140f * u,
                    Visuals.DANGER, 0.10f * pulse);
        }
        float hullFrac = world.ship.part[Ship.HULL] / world.ship.maxHull;
        if (hullFrac < 0.35f) {
            float pulse = 0.5f + 0.5f * MathUtils.sin(time * 9f);
            art.glow(batch, hullChipX(w) + hullChipW() * 0.5f, hullChipY(h) + hullChipH() * 0.5f,
                    hullChipW() * 1.3f, Visuals.DANGER, 0.12f * pulse);
        }
        batch.end();
        Visuals.normal(batch);
    }

    /** Destello blanco, viñeta roja y fundido mientras la nave explota. */
    private void drawDeathFlourish(float w, float h) {
        float flash = MathUtils.clamp(1f - deathTimer / 0.30f, 0f, 1f);
        float dim = MathUtils.clamp((deathTimer - 1.20f) / 1.20f, 0f, 1f) * 0.60f;
        beginShapes();
        if (flash > 0f) {
            shapes.setColor(1f, 0.96f, 0.86f, flash * 0.62f);
            shapes.rect(0f, 0f, w, h);
        }
        if (dim > 0f) {
            shapes.setColor(0f, 0f, 0f, dim);
            shapes.rect(0f, 0f, w, h);
        }
        shapes.end();

        batch.begin();
        Visuals.normal(batch);
        if (deathTimer < 0.75f) {
            float p = 1f - deathTimer / 0.75f;
            art.vignetteTint(batch, w, h, 1f, 0.20f, 0.16f, 0.30f * p * p);
        }
        batch.end();
    }

    private void drawPause(float w, float h) {
        float pad = 16f * u;
        float pw = Math.min(w - 4f * pad, 520f * u);
        float ph = 390f * u;
        float px = (w - pw) * 0.5f;
        float py = (h - ph) * 0.5f;

        beginShapes();
        shapes.setColor(0f, 0f, 0f, 0.64f);
        shapes.rect(0f, 0f, w, h);
        panel(px, py, pw, ph, 26f * u, Visuals.PANEL, Visuals.PANEL_EDGE);
        shapes.setColor(0.30f, 0.65f, 1f, 0.55f);
        shapes.rect(px + pw * 0.25f, py + ph - 4f * u, pw * 0.5f, 3f * u);
        drawButton(btnResume, Visuals.BTN_PRIMARY, true);
        drawButton(btnPauseShop, Visuals.BTN_SECONDARY, true);
        drawButton(btnMenu, Visuals.BTN_SECONDARY, true);
        shapes.end();

        batch.begin();
        textCenteredMiddle("PAUSA", w * 0.5f, h * 0.5f + 172f * u, 1.9f * u, Color.WHITE);
        textCenteredMiddle("OLEADA " + world.wave + "   -   PUNTOS " + world.score,
                w * 0.5f, h * 0.5f + 105f * u, 0.92f * u, Visuals.ACCENT);
        drawButtonText(btnResume, true);
        drawButtonText(btnPauseShop, true);
        drawButtonText(btnMenu, true);
        batch.end();
    }

    private void drawOver(float w, float h) {
        float pad = 16f * u;
        float pw = Math.min(w - 4f * pad, 520f * u);
        float ph = 390f * u;
        float px = (w - pw) * 0.5f;
        float py = (h - ph) * 0.5f;

        beginShapes();
        shapes.setColor(0f, 0f, 0f, 0.66f);
        shapes.rect(0f, 0f, w, h);
        panel(px, py, pw, ph, 26f * u, Visuals.PANEL, OVER_EDGE_C);
        shapes.setColor(1f, 0.35f, 0.42f, 0.60f);
        shapes.rect(px + pw * 0.25f, py + ph - 4f * u, pw * 0.5f, 3f * u);
        drawButton(btnRetry, Visuals.BTN_PRIMARY, true);
        drawButton(btnOverMenu, Visuals.BTN_SECONDARY, true);
        shapes.end();

        batch.begin();
        textCenteredMiddle("PERSONAJE PERDIDO", w * 0.5f, h * 0.5f + 172f * u, 1.75f * u, Color.WHITE);
        textCenteredMiddle("MONEDAS GANADAS  +" + overCoins,
                w * 0.5f, h * 0.5f + 112f * u, 0.95f * u, Visuals.GOLD);
        textCenteredMiddle("PUNTOS " + world.score + "   -   OLEADA " + world.wave,
                w * 0.5f, h * 0.5f + 78f * u, 0.90f * u, Visuals.TEXT_DIM);
        textCenteredMiddle("La nave queda en el espacio", w * 0.5f,
                h * 0.5f + 48f * u, 0.72f * u, Visuals.TEXT_DIM);
        drawButtonText(btnRetry, true);
        drawButtonText(btnOverMenu, true);
        batch.end();
    }

    // ---------- piezas de interfaz ----------

    private void panel(float x, float y, float w, float h, float r, Color fill, Color border) {
        shapes.setColor(0f, 0f, 0f, 0.35f);
        Visuals.roundRect(shapes, x + 2f * u, y - 4f * u, w, h, r);
        shapes.setColor(border);
        Visuals.roundRect(shapes, x - 1.5f * u, y - 1.5f * u, w + 3f * u, h + 3f * u, r + 1.5f * u);
        shapes.setColor(fill);
        Visuals.roundRect(shapes, x, y, w, h, r);
    }

    private void drawButton(Button b, Color fill, boolean enabled) {
        float x = b.bounds.x;
        float y = b.bounds.y;
        float w = b.bounds.width;
        float h = b.bounds.height;
        float r = Math.min(h * 0.30f, 20f * u);
        boolean pressed = enabled && held(b);

        shapes.setColor(0f, 0f, 0f, 0.38f);
        Visuals.roundRect(shapes, x + 2f * u, y - 5f * u, w, h, r);

        Color body = fill;
        if (!enabled) {
            body = Visuals.BTN_OFF;
        } else if (pressed) {
            body = tint2.set(fill).lerp(Color.WHITE, 0.16f);
        }
        float br = Math.min(1f, body.r * 1.5f + 0.18f);
        float bg = Math.min(1f, body.g * 1.5f + 0.18f);
        float bb = Math.min(1f, body.b * 1.5f + 0.18f);
        shapes.setColor(br, bg, bb, enabled ? 0.75f : 0.45f);
        Visuals.roundRect(shapes, x - 2f * u, y - 2f * u, w + 4f * u, h + 4f * u, r + 2f * u);

        shapes.setColor(body);
        Visuals.roundRect(shapes, x, y, w, h, r);
        // Brillo superior y base en sombra.
        shapes.setColor(1f, 1f, 1f, enabled ? 0.12f : 0.06f);
        Visuals.roundRect(shapes, x + 3f * u, y + h * 0.56f, w - 6f * u, h * 0.38f, r * 0.75f);
        shapes.setColor(0f, 0f, 0f, 0.18f);
        Visuals.roundRect(shapes, x + 3f * u, y + 2f * u, w - 6f * u, h * 0.20f, r * 0.55f);
    }

    private void drawButtonText(Button b, boolean enabled) {
        b.drawText(batch, font, layout, btnTextScale, enabled ? Visuals.TEXT : Visuals.TEXT_OFF);
    }

    private void coinChipShape(float cx, float cy) {
        float cw = 150f * u;
        float ch = 42f * u;
        panel(cx - cw * 0.5f, cy - ch * 0.5f, cw, ch, ch * 0.5f,
                COIN_CHIP_C, Visuals.PANEL_EDGE);
        drawCoinIcon(cx - cw * 0.5f + 25f * u, cy, 12f * u);
    }

    private void coinChipText(float cx, float cy, int value) {
        textCenteredMiddle(String.valueOf(value), cx + 12f * u, cy, 1.05f * u, Visuals.GOLD);
    }

    private void drawCoinIcon(float cx, float cy, float r) {
        shapes.setColor(0.55f, 0.40f, 0.08f, 0.95f);
        shapes.circle(cx, cy, r, 20);
        shapes.setColor(Visuals.GOLD);
        shapes.circle(cx, cy, r * 0.86f, 20);
        shapes.setColor(1f, 0.92f, 0.60f, 0.95f);
        shapes.circle(cx - r * 0.20f, cy + r * 0.22f, r * 0.42f, 12);
        shapes.setColor(0.80f, 0.55f, 0.10f, 0.95f);
        shapes.triangle(cx, cy + r * 0.48f,
                cx + r * 0.34f, cy,
                cx, cy - r * 0.48f);
        shapes.triangle(cx, cy + r * 0.48f,
                cx - r * 0.34f, cy,
                cx, cy - r * 0.48f);
    }

    private void drawFuelIcon(float cx, float cy, float r) {
        shapes.setColor(0.10f, 0.55f, 0.30f, 0.95f);
        Visuals.roundRect(shapes, cx - r * 0.55f, cy - r * 0.75f, r * 1.1f, r * 1.5f, r * 0.32f);
        shapes.setColor(0.45f, 1f, 0.65f, 0.95f);
        Visuals.roundRect(shapes, cx - r * 0.55f, cy - r * 0.75f, r * 0.30f, r * 1.5f, r * 0.15f);
        shapes.setColor(0.60f, 1f, 0.75f, 0.95f);
        shapes.rect(cx - r * 0.20f, cy + r * 0.72f, r * 0.40f, r * 0.22f);
        shapes.setColor(Color.WHITE);
        shapes.rect(cx - r * 0.12f, cy - r * 0.28f, r * 0.24f, r * 0.56f);
        shapes.rect(cx - r * 0.28f, cy - r * 0.12f, r * 0.56f, r * 0.24f);
    }

    // ---------- texto ----------

    private void drawTitle(String text, float cx, float topY, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float x = cx - layout.width * 0.5f;
        float o = Math.max(2f, scale * 0.75f);
        font.setColor(0.36f, 0.86f, 1f, 0.16f);
        font.draw(batch, text, x + o, topY);
        font.draw(batch, text, x - o, topY);
        font.draw(batch, text, x, topY + o);
        font.draw(batch, text, x, topY - o);
        font.setColor(0f, 0f, 0f, 0.55f);
        font.draw(batch, text, x + o * 1.5f, topY - o * 1.5f);
        font.setColor(Color.WHITE);
        font.draw(batch, text, x, topY);
        font.getData().setScale(1f);
    }

    private void textCentered(String text, float cx, float topY, float scale, Color c) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float x = cx - layout.width * 0.5f;
        float sh = Math.max(1.5f, scale * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, topY - sh);
        font.setColor(c);
        font.draw(batch, text, x, topY);
        font.getData().setScale(1f);
    }

    private void textCenteredMiddle(String text, float cx, float cy, float scale, Color c) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float x = cx - layout.width * 0.5f;
        float y = cy + layout.height * 0.5f;
        float sh = Math.max(1.5f, scale * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, y - sh);
        font.setColor(c);
        font.draw(batch, text, x, y);
        font.getData().setScale(1f);
    }

    private void textLeft(String text, float x, float topY, float scale, Color c) {
        font.getData().setScale(scale);
        float sh = Math.max(1.5f, scale * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, topY - sh);
        font.setColor(c);
        font.draw(batch, text, x, topY);
        font.getData().setScale(1f);
    }

    /** Como {@link #textLeft} pero reduce la escala si el texto no cabe en maxWidth. */
    private void textLeftFit(String text, float x, float topY, float scale, float maxWidth, Color c) {
        float s = scale;
        font.getData().setScale(s);
        layout.setText(font, text);
        if (layout.width > maxWidth && layout.width > 0f) {
            s *= maxWidth / layout.width;
            font.getData().setScale(s);
        }
        float sh = Math.max(1.5f, s * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, topY - sh);
        font.setColor(c);
        font.draw(batch, text, x, topY);
        font.getData().setScale(1f);
    }

    private void textLeftMiddle(String text, float x, float cy, float scale, Color c) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float y = cy + layout.height * 0.5f;
        float sh = Math.max(1.5f, scale * 0.6f);
        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, text, x + sh, y - sh);
        font.setColor(c);
        font.draw(batch, text, x, y);
        font.getData().setScale(1f);
    }

    // ---------- ciclo de vida ----------

    @Override
    public void dispose() {
        if (saves != null) {
            saveRun();
        }
        shapes.dispose();
        batch.dispose();
        font.dispose();
        art.dispose();
    }
}

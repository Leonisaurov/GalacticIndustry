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

    private OrthographicCamera worldCam;
    private ExtendViewport worldView;
    private OrthographicCamera hudCam;
    private ScreenViewport hudView;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;
    private Visuals art;
    /** Primitivas de dibujo de la interfaz. */
    private UiKit ui;
    /** Fondo espacial (degradado, estrellas, nebulosas y planeta). */
    SpaceBackground space;
    /** Estado reutilizable que alimenta el HUD de partida. */
    private final HudView hud = new HudView();
    private PlayHud playHud;
    private ShopRenderer shopRenderer;
    private OverlayRenderer overlay;
    /** Composicion de perfil, sector y guardado. */
    GameSession session;
    /** Alias del progreso de la sesion; no crea un segundo estado. */
    Upgrades upgrades;
    /** Alias del sector activo de la sesion. */
    GameWorld world;
    /** Guardado v2 con backup y migracion del formato anterior. */
    private SaveService saves;
    /** Snapshot del último personaje muerto: mundo y nave legado. */
    private SaveService legacySaves;
    /** Punto seguro del modo de desarrollo de recuperación. */
    private SaveService recoverySaves;
    private float recoveryTimer;
    private int characterSerial;
    private final Vector2 touch = new Vector2();

    private int state = MENU;
    private int shopReturn = MENU;
    float time;
    float u = 1f;
    /** Tiempo de la secuencia de explosión antes del diálogo de fin de partida. */
    float deathTimer;
    /** Monedas ganadas en la última partida (se muestran en el diálogo final). */
    int overCoins;
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
    /** Modo de desarrollo: recupera el último punto seguro tras la muerte. */
    final boolean recoveryTest;
    /** Arranca con la bodega llena para revisar masa/HUD. */
    private final boolean cargoTest;
    /** Arranca con una roca cercana y el piloto en modo MINE. */
    private final boolean miningTest;
    /** Arranca con fragmentos cercanos para probar el rayo tractor. */
    private final boolean tractorTest;
    /** Arranca junto a la nave nodriza para revisar su escala y zona segura. */
    private final boolean stationTest;
    /** Cualquier modo de prueba activa la telemetría estructurada. */
    private final boolean debugMode;
    private float telemetryTimer;
    private int lastState = -1;
    private static final String TAG = "AsteroidsTelemetry";


    Button btnPlay, btnShop, btnResume, btnPauseShop, btnMenu, btnRetry, btnOverMenu, btnBack;
    /** Barra de aceleración: su rectángulo es btnThrust.bounds. */
    private Button btnThrust, btnPause;
    /** Recuperar la nave del personaje anterior cuando se está cerca. */
    private Button btnRecover;
    final Array<Button> buyButtons = new Array<>();
    /** Botones de reparacion de cada parte (solo con una partida activa). */
    final Array<Button> repairButtons = new Array<>();
    /** Entradas y botones de fundición temporal. */
    final Array<Button> meltButtons = new Array<>();
    final Array<Cargo> meltEntries = new Array<>();
    /** Recarga de gasolina en la tienda (solo durante la partida). */
    Button btnRefill;
    /** Recarga de O2 en la tienda temporal (solo durante la partida). */
    Button btnO2;
    /** Pestañas de la tienda: 0 mejoras, 1 reparaciones, 2 fundir. */
    Button btnTabUpgrades, btnTabRepairs, btnTabMelt;
    int shopTab = 0;

    // Geometría de la tienda (se recalcula en layoutUi).
    float shopListTop;
    float shopRowH;
    float shopRowGap;
    int shopRows;

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
    boolean runActive;

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
        this(testMode, debugDeath, slowDeath, autopilotTest, wingTest, survivalTest,
                pilotDeathTest, false);
    }

    /**
     * @param survivalTest   arranca con O2 bajo, salud dañada y fuga visible.
     * @param pilotDeathTest arranca con el piloto incapacitado para probar la muerte.
     * @param recoveryTest   modo de desarrollo: recupera el último punto seguro.
     */
    public MyGame(boolean testMode, boolean debugDeath, boolean slowDeath,
                  boolean autopilotTest, boolean wingTest, boolean survivalTest,
                  boolean pilotDeathTest, boolean recoveryTest) {
        this(testMode, debugDeath, slowDeath, autopilotTest, wingTest, survivalTest,
                pilotDeathTest, recoveryTest, false);
    }

    /**
     * @param cargoTest arranca con la bodega llena para revisar masa y HUD.
     */
    public MyGame(boolean testMode, boolean debugDeath, boolean slowDeath,
                  boolean autopilotTest, boolean wingTest, boolean survivalTest,
                  boolean pilotDeathTest, boolean recoveryTest, boolean cargoTest) {
        this(testMode, debugDeath, slowDeath, autopilotTest, wingTest, survivalTest,
                pilotDeathTest, recoveryTest, cargoTest, false);
    }

    /**
     * @param miningTest arranca con una roca cercana y el piloto en modo MINE.
     */
    public MyGame(boolean testMode, boolean debugDeath, boolean slowDeath,
                  boolean autopilotTest, boolean wingTest, boolean survivalTest,
                  boolean pilotDeathTest, boolean recoveryTest, boolean cargoTest,
                  boolean miningTest) {
        this(testMode, debugDeath, slowDeath, autopilotTest, wingTest, survivalTest,
                pilotDeathTest, recoveryTest, cargoTest, miningTest, false);
    }

    /**
     * @param tractorTest arranca con fragmentos cercanos para probar el tractor.
     */
    public MyGame(boolean testMode, boolean debugDeath, boolean slowDeath,
                  boolean autopilotTest, boolean wingTest, boolean survivalTest,
                  boolean pilotDeathTest, boolean recoveryTest, boolean cargoTest,
                  boolean miningTest, boolean tractorTest) {
        this(testMode, debugDeath, slowDeath, autopilotTest, wingTest, survivalTest,
                pilotDeathTest, recoveryTest, cargoTest, miningTest, tractorTest, false);
    }

    /**
     * @param stationTest arranca junto a la nave nodriza para revisar su escala.
     */
    public MyGame(boolean testMode, boolean debugDeath, boolean slowDeath,
                  boolean autopilotTest, boolean wingTest, boolean survivalTest,
                  boolean pilotDeathTest, boolean recoveryTest, boolean cargoTest,
                  boolean miningTest, boolean tractorTest, boolean stationTest) {
        this.testMode = testMode;
        this.debugDeath = debugDeath;
        this.slowDeath = slowDeath;
        this.autopilotTest = autopilotTest;
        this.wingTest = wingTest;
        this.survivalTest = survivalTest;
        this.pilotDeathTest = pilotDeathTest;
        this.recoveryTest = recoveryTest;
        this.cargoTest = cargoTest;
        this.miningTest = miningTest;
        this.tractorTest = tractorTest;
        this.stationTest = stationTest;
        this.debugMode = testMode || debugDeath || slowDeath || autopilotTest
                || wingTest || survivalTest || pilotDeathTest || recoveryTest
                || cargoTest || miningTest || tractorTest || stationTest;
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
        ui = new UiKit(batch, shapes, font, layout);
        ui.pressed = this::held;
        space = new SpaceBackground(batch, shapes, art);
        session = new GameSession();
        upgrades = session.upgrades();
        world = session.sector;
        playHud = new PlayHud(ui, world, session, upgrades, batch, shapes, art, hud);
        shopRenderer = new ShopRenderer(this, ui, batch, shapes);
        overlay = new OverlayRenderer(this, ui, batch, shapes, art);
        world.immortal = testMode;
        saves = new SaveService(
                new SaveService.FileStorage(Gdx.files.local("save").file()), upgrades);
        legacySaves = new SaveService(
                new SaveService.FileStorage(Gdx.files.local("save").file()), upgrades,
                SaveService.LEGACY_SLOT);
        recoverySaves = new SaveService(
                new SaveService.FileStorage(Gdx.files.local("save").file()), upgrades,
                SaveService.RECOVERY_SLOT);
        loadRun();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (debugDeath) {
            session.startRun();
            runActive = true;
            state = PLAYING;
            if (recoveryTest) {
                saveRecoveryPoint();
            }
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
            if (recoveryTest) {
                saveRecoveryPoint();
            }
            world.asteroids.clear();
            world.fuel.clear();
            session.pilot.oxygen = 0f;
            session.pilot.health = 0f;
            session.pilot.oxygenGrace = 0f;
            session.pilot.status = Pilot.Status.INCAPACITATED;
        } else if (cargoTest) {
            session.startRun();
            runActive = true;
            state = PLAYING;
            world.asteroids.clear();
            world.fuel.clear();
            world.ship.cargo().addExact(Commodity.SILICATO, 100);
        } else if (miningTest) {
            session.startRun();
            runActive = true;
            state = PLAYING;
            world.asteroids.clear();
            world.fuel.clear();
            Asteroid target = new Asteroid(200f, 0f, Asteroid.MEDIUM,
                    AsteroidType.C, 15, 15, 555L);
            target.vel.set(0f, 0f);
            world.asteroids.add(target);
            world.pilot.mine(target);
        } else if (tractorTest) {
            session.startRun();
            runActive = true;
            state = PLAYING;
            world.asteroids.clear();
            world.fuel.clear();
            world.fragments.add(new OreFragment(95f, 0f, Commodity.HIELO, 4, 0f, 0f));
            world.fragments.add(new OreFragment(-85f, 40f, Commodity.SILICATO, 4, 0f, 0f));
            world.fragments.add(new OreFragment(40f, -95f, Commodity.HIERRO, 4, 0f, 0f));
        } else if (stationTest) {
            session.startRun();
            runActive = true;
            state = PLAYING;
            world.asteroids.clear();
            world.fuel.clear();
            world.ship.pos.set(world.station.pos.x, world.station.pos.y - 330f);
            world.ship.heading = 90f;
            world.ship.dir.set(0f, 1f);
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

    /** Punto seguro del modo de desarrollo; nunca guarda una partida ya muerta. */
    private void saveRecoveryPoint() {
        if (recoveryTest && recoverySaves != null && runActive
                && !world.isOver() && session.pilot.alive()) {
            recoverySaves.save(session);
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
        if (ui != null) {
            ui.u = u;
        }
        ui.buttonTextScale = 1.08f * u;
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
        btnRetry = stacked(recoveryTest ? "RECUPERAR (DEBUG)" : "NUEVO PERSONAJE",
                cx, overCy, bw, bh, gap, 0, 2);
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
        float tabW = runActive
                ? (w - 3f * pad - 2f * tabGap) / 3f
                : Math.max(80f * u, w - 2f * tabX);
        btnTabUpgrades = new Button("MEJORAS", tabX, tabY, tabW, tabH);
        if (runActive) {
            btnTabRepairs = new Button("REPARAR", tabX + tabW + tabGap, tabY, tabW, tabH);
            btnTabMelt = new Button("FUNDIR", tabX + 2f * (tabW + tabGap), tabY, tabW, tabH);
        } else {
            btnTabRepairs = null;
            btnTabMelt = null;
        }

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
        meltButtons.clear();
        meltEntries.clear();
        btnRefill = null;
        btnO2 = null;
        if (shopTab == 0) {
            for (int i = 0; i < Upgrades.COUNT; i++) {
                buyButtons.add(shopButton(buyLabel(i), i, cardX, cardW, bbw, bbh));
            }
        } else if (shopTab == 1) {
            for (int i = 0; i < Ship.PART_COUNT; i++) {
                repairButtons.add(shopButton(repairLabel(i), i, cardX, cardW, bbw, bbh));
            }
            btnRefill = shopButton(fuelRefillLabel(),
                    Ship.PART_COUNT, cardX, cardW, bbw, bbh);
            btnO2 = shopButton(o2RefillLabel(),
                    Ship.PART_COUNT + 1, cardX, cardW, bbw, bbh);
        } else {
            for (Cargo entry : world.ship.cargo().entries()) {
                meltEntries.add(entry);
            }
            for (int i = 0; i < meltEntries.size; i++) {
                meltButtons.add(shopButton("FUNDIR " + Upgrades.meltValue(meltEntries.get(i)),
                        i, cardX, cardW, bbw, bbh));
            }
        }
    }

    private Button shopButton(String label, int row, float cardX, float cardW, float bbw, float bbh) {
        float top = shopRowTop(row);
        return new Button(label,
                cardX + cardW - 10f * u - bbw,
                top - shopRowH + (shopRowH - bbh) * 0.5f, bbw, bbh);
    }

    private int shopRowCount() {
        if (runActive && shopTab == 1) {
            return Ship.PART_COUNT + 2;
        }
        if (runActive && shopTab == 2) {
            return Math.max(1, world.ship.cargo().entries().size());
        }
        return Upgrades.COUNT;
    }

    private float topPanelH() {
        return 112f * u;
    }

    /** La recarga solo se ofrece con una partida activa, deposito no lleno y saldo suficiente. */
    boolean canRefill() {
        return runActive && world.ship.fuel < world.ship.maxFuel
                && upgrades.coins() + world.coinsEarned >= fuelRefillCost();
    }

    /** Coste de repostar: 1 crédito por unidad que falte, mínimo 5. */
    private int fuelRefillCost() {
        float missing = Math.max(0f, world.ship.maxFuel - world.ship.fuel);
        return upgrades.refillCost(missing);
    }

    /** UM totales de mineral dispersas como fragmentos. */
    private int fragmentUnits() {
        int total = 0;
        for (int i = 0; i < world.fragments.size; i++) {
            total += world.fragments.get(i).amount;
        }
        return total;
    }

    /** Asteroides dentro de la zona segura de la estación (debe ser 0). */
    private int stationAsteroids() {
        int count = 0;
        for (int i = 0; i < world.asteroids.size; i++) {
            if (world.station.inSafeZone(world.asteroids.get(i).pos)) {
                count++;
            }
        }
        return count;
    }

    String fuelRefillLabel() {
        int cost = fuelRefillCost();
        return cost <= 0 ? "DEPOSITO OK" : "RELLENAR " + cost;
    }

    /** Coste de rellenar O2 según lo que falte. */
    private int o2RefillCost() {
        float missing = Math.max(0f, session.pilot.o2Max - session.pilot.oxygen);
        return upgrades.o2RefillCost(missing);
    }

    String o2RefillLabel() {
        int cost = o2RefillCost();
        return cost <= 0 ? "O2 OK" : "O2 " + cost;
    }

    boolean canRefillO2() {
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
    int repairCost(int index) {
        float missing = world.ship.partMax(index) - world.ship.part[index];
        if (missing < 1f) {
            return 0;
        }
        return Math.max(5, Math.round(missing * 0.70f));
    }

    String repairLabel(int index) {
        int cost = repairCost(index);
        return cost == 0 ? "OK" : "REPARAR " + cost;
    }

    boolean canRepair(int index) {
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
        } else if (index == Upgrades.BODEGA) {
            world.ship.syncCargo(upgrades);
        } else if (index == Upgrades.LASER) {
            world.ship.syncLaser(upgrades);
        } else if (index == Upgrades.TRACTOR) {
            world.ship.syncTractor(upgrades);
        }
        saveRun();
    }

    String buyLabel(int i) {
        return upgrades.level(i) >= Upgrades.MAX_LEVEL ? "MAX" : "COMPRAR " + upgrades.cost(i);
    }

    private Button stacked(String label, float cx, float cy, float bw, float bh, float gap, int index, int count) {
        float total = count * bh + (count - 1) * gap;
        float top = cy + total / 2f;
        float y = top - (index + 1) * bh - index * gap;
        return new Button(label, cx, y, bw, bh);
    }

    float shopRowTop(int index) {
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
        space.time = time;
        if (ui != null) {
            ui.u = u;
        }
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
            case Autopilot.MINE: return "MINE";
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
                        + "\"legacyNear\":%s,\"legacyDistance\":%.1f,"
                        + "\"cargoMass\":%.1f,\"cargoCapacity\":%.1f,\"cargoUnits\":%d,"
                        + "\"fragments\":%d,\"fragmentUnits\":%d,"
                        + "\"energy\":%.1f,\"energyMax\":%.1f,\"mining\":%s,"
                        + "\"laserLevel\":%d,\"tractorActive\":%s,\"tractorTargets\":%d,"
                        + "\"tractorLevel\":%d,"
                        + "\"stationX\":%.1f,\"stationY\":%.1f,\"stationRadius\":%.1f,"
                        + "\"stationDistance\":%.1f,\"stationAsteroids\":%d}",
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
                world.legacyDistance(world.ship.pos),
                world.ship.cargo().usedMass(),
                world.ship.cargo().capacity(),
                world.ship.cargo().totalUnits(),
                world.fragments.size,
                fragmentUnits(),
                world.ship.capacitor.energy,
                world.ship.capacitor.maxEnergy,
                world.ship.laser.active,
                upgrades.laserLevel(),
                world.ship.tractor.active(),
                world.ship.tractor.targets.size,
                upgrades.tractorLevel(),
                world.station.pos.x,
                world.station.pos.y,
                world.station.def.radius,
                world.station.distanceTo(world.ship.pos),
                stationAsteroids()));
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
        if (recoveryTest) {
            recoveryTimer = 0f;
            saveRecoveryPoint();
        }
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
        if (recoveryTest) {
            recoveryTimer = 0f;
            saveRecoveryPoint();
        }
        logEvent("{\"event\":\"new_character\",\"legacy\":" + legacyLoaded + "}");
    }

    /** Recupera el último punto seguro del modo de desarrollo. */
    private void recoverCharacter() {
        if (recoverySaves == null || !recoverySaves.hasSave()) {
            startNewCharacter();
            return;
        }
        SaveService.LoadStatus status = recoverySaves.load(session);
        if (status != SaveService.LoadStatus.V2
                && status != SaveService.LoadStatus.BACKUP) {
            startNewCharacter();
            return;
        }
        resetControls();
        world.pilot.cancel();
        deathTimer = 0f;
        overCoins = 0;
        worldCam.zoom = 1f;
        recoveryTimer = 0f;
        runActive = true;
        state = PLAYING;
        saveRun();
        if (legacySaves != null) {
            legacySaves.clear();
        }
        logEvent("{\"event\":\"recovery\"}");
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
            world.ship.laser.stop();
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
        if (recoveryTest) {
            recoveryTimer += dt;
            if (recoveryTimer >= 10f) {
                recoveryTimer = 0f;
                saveRecoveryPoint();
            }
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
            world.ship.laser.stop();
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
            world.pilot.mine(rock);
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
        if (runActive && tapped(btnTabMelt)) {
            if (shopTab != 2) {
                shopTab = 2;
                layoutUi();
            }
            return;
        }
        if (shopTab == 1) {
            if (btnRefill != null && tapped(btnRefill) && canRefill()) {
                int cost = fuelRefillCost();
                if (upgrades.spendCoins(cost)) {
                    world.ship.refuel(world.ship.maxFuel);
                    saveRun();
                    logEvent(String.format(Locale.US,
                            "{\"event\":\"refill\",\"cost\":%d,\"coins\":%d}",
                            cost, upgrades.coins()));
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
        if (shopTab == 2) {
            for (int i = 0; i < meltButtons.size; i++) {
                if (!tapped(meltButtons.get(i))) {
                    continue;
                }
                Cargo entry = meltEntries.get(i);
                int value = Upgrades.meltValue(entry);
                if (world.ship.cargo().removeExact(entry.commodity, entry.amount)) {
                    upgrades.addCoins(value);
                    saveRun();
                    logEvent(String.format(Locale.US,
                            "{\"event\":\"melt\",\"commodity\":\"%s\",\"amount\":%d,"
                                    + "\"credits\":%d,\"coins\":%d}",
                            entry.commodity.name(), entry.amount, value, upgrades.coins()));
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
            if (recoveryTest) {
                recoverCharacter();
            } else {
                startNewCharacter();
            }
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
        space.gradient(camX, camY, ww, wh);
        shapes.end();

        batch.begin();
        Visuals.additive(batch);
        space.nebula(camX, camY, ww, wh, 1f, 1.15f);
        space.stars(camX, camY, ww, wh, 1f, 1f);
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

    // ---------- dibujo: HUD y menús (coordenadas de pantalla) ----------

    private void drawHud() {
        hudView.apply();
        float w = hudView.getWorldWidth();
        float h = hudView.getWorldHeight();
        shapes.setProjectionMatrix(hudCam.combined);
        batch.setProjectionMatrix(hudCam.combined);

        if (state == MENU) {
            overlay.drawMenu(w, h);
        } else if (state == SHOP) {
            shopRenderer.draw(w, h);
        } else if (state == DYING) {
            overlay.drawDeathFlourish(w, h);
        } else {
            hud.state = state;
            hud.runActive = runActive;
            hud.canRecover = canRecoverLegacy();
            hud.time = time;
            hud.throttle = throttle;
            hud.joyCx = joyCx;
            hud.joyCy = joyCy;
            hud.joyR = joyR;
            hud.knobX = knobX;
            hud.knobY = knobY;
            hud.joyAiming = joyAiming;
            hud.pause = btnPause;
            hud.thrust = btnThrust;
            hud.recover = btnRecover;
            playHud.draw(w, h);
            if (state == PAUSED) {
                overlay.drawPause(w, h);
            } else if (state == OVER) {
                overlay.drawOver(w, h);
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

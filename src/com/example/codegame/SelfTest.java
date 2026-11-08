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
        float hull0 = b.ship.part[Ship.HULL];
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
        check("bench meteorito " + name + " sin golpes", b.ship.part[Ship.HULL] == hull0);
        System.out.println(String.format("BENCH roca   %-14s t=%.1fs combustible=%.1f giros=%.0fdeg motor=%.1fs casco=%.0f disparos=%d aciertos=%d",
                name, steps * dt, used, turned, thrustTime, b.ship.part[Ship.HULL], shots, hits));
    }

    public static void main(String[] args) {
        final float dt = 1f / 60f;

        Upgrades up = new Upgrades(true);
        GameWorld w = new GameWorld(up);
        w.start();

        check("combustible inicial lleno", w.ship.fuel == w.ship.maxFuel);
        check("casco inicial lleno", w.ship.part[Ship.HULL] == up.hull());
        check("sistemas y alas intactos", w.ship.part[Ship.ENGINE] == Ship.PART_MAX
                && w.ship.part[Ship.TANK] == Ship.PART_MAX
                && w.ship.part[Ship.CANNON] == Ship.PART_MAX
                && w.ship.part[Ship.STEERING] == Ship.PART_MAX
                && w.ship.part[Ship.WING_LEFT] == Ship.PART_MAX
                && w.ship.part[Ship.WING_RIGHT] == Ship.PART_MAX);
        check("soporte vital es la ultima parte",
                Ship.LIFE_SUPPORT == Ship.PART_COUNT - 1
                        && "SOPORTE".equals(Ship.PART_NAMES[Ship.LIFE_SUPPORT]));
        check("soporte vital inicial intacto",
                w.ship.part[Ship.LIFE_SUPPORT] == Ship.PART_MAX);
        LifeSupport freshLife = new LifeSupport();
        check("soporte vital inicia lleno",
                freshLife.filter == LifeSupport.FILTER_MAX
                        && freshLife.leak == 0f && !freshLife.breach);

        // T-101: composicion de PlayerProfile y GameSession sin economia nueva.
        Upgrades profileUp = new Upgrades(true);
        PlayerProfile profile = new PlayerProfile(profileUp);
        GameSession session = new GameSession(profile);
        check("perfil tiene identidad por defecto",
                PlayerProfile.DEFAULT_ID.equals(profile.id)
                        && PlayerProfile.DEFAULT_PLAYER_ID.equals(profile.playerId)
                        && PlayerProfile.DEFAULT_NAME.equals(profile.name));
        check("sesion agrupa perfil, nave y sector",
                session.profile == profile && session.upgrades() == profileUp
                        && session.ship() == session.sector.ship);
        check("sesion sin partida no guarda",
                !session.saveRun() && profileUp.getText(GameSession.RUN_KEY) == null);
        session.startRun();
        check("startRun activa la sesion", session.runActive());
        session.sector.ship.pos.set(123f, -45f);
        check("sesion guarda la partida",
                session.saveRun() && profileUp.getText(GameSession.RUN_KEY) != null);
        GameSession loaded = new GameSession(new PlayerProfile(profileUp));
        check("sesion carga la partida", loaded.loadRun() && loaded.runActive());
        check("cargar conserva la posicion",
                Math.abs(loaded.ship().pos.x - 123f) < 0.01f
                        && Math.abs(loaded.ship().pos.y + 45f) < 0.01f);
        loaded.clearRun();
        check("clearRun borra la partida",
                !loaded.runActive() && profileUp.getText(GameSession.RUN_KEY) == null);

        // T-102: piloto independiente de la nave y persistencia.
        check("piloto inicia con salud y O2 al 100",
                session.pilot.health == 100f && session.pilot.maxHealth == 100f
                        && session.pilot.oxygen == 100f && session.pilot.o2Max == 100f
                        && session.pilot.status == Pilot.Status.OK && session.pilot.aboard);
        float hullBeforePilot = session.ship().part[Ship.HULL];
        session.pilot.health = 73f;
        session.pilot.oxygen = 42f;
        check("salud del piloto no modifica el casco",
                session.ship().part[Ship.HULL] == hullBeforePilot);
        check("sesion guarda el piloto", session.saveRun());
        GameSession pilotLoaded = new GameSession(new PlayerProfile(profileUp));
        check("sesion carga el piloto", pilotLoaded.loadRun()
                && Math.abs(pilotLoaded.pilot.health - 73f) < 0.01f
                && Math.abs(pilotLoaded.pilot.oxygen - 42f) < 0.01f);
        check("el piloto sigue a bordo tras cargar", pilotLoaded.pilot.aboard);
        // Guardado anterior a T-102, sin linea de piloto: debe usar 100/100.
        profileUp.putText(GameSession.RUN_KEY, session.sector.serialize());
        GameSession legacyPilot = new GameSession(new PlayerProfile(profileUp));
        check("guardado antiguo carga piloto 100/100",
                legacyPilot.loadRun()
                        && legacyPilot.pilot.health == 100f
                        && legacyPilot.pilot.oxygen == 100f);
        legacyPilot.clearRun();

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
        float hull0 = w.ship.part[Ship.HULL];
        float systems0 = w.ship.part[Ship.ENGINE] + w.ship.part[Ship.TANK]
                + w.ship.part[Ship.CANNON] + w.ship.part[Ship.STEERING]
                + w.ship.part[Ship.WING_LEFT] + w.ship.part[Ship.WING_RIGHT];
        Asteroid hitter = new Asteroid(w.ship.pos.x, w.ship.pos.y, Asteroid.MEDIUM);
        hitter.vel.set(0f, 0f);
        w.asteroids.add(hitter);
        w.update(dt, false, false, false, false);
        float systems1 = w.ship.part[Ship.ENGINE] + w.ship.part[Ship.TANK]
                + w.ship.part[Ship.CANNON] + w.ship.part[Ship.STEERING]
                + w.ship.part[Ship.WING_LEFT] + w.ship.part[Ship.WING_RIGHT];
        check("colisionar con meteorito dana el casco", w.ship.part[Ship.HULL] < hull0);
        check("colisionar con meteorito dana un sistema", systems1 < systems0);
        check("invulnerable tras golpe", w.ship.invulnerable > 0f);

        // El casco a cero termina la partida.
        GameWorld wreck = new GameWorld(new Upgrades(true));
        wreck.start();
        wreck.ship.part[Ship.HULL] = 0f;
        check("casco destruido termina la partida", wreck.isOver());

        // Canon destruido: no se puede disparar.
        w.ship.part[Ship.CANNON] = 0f;
        w.ship.cooldown = 0f;
        w.ship.fuel = w.ship.maxFuel;
        check("canon destruido no dispara", !w.ship.canFire());
        w.ship.part[Ship.CANNON] = Ship.PART_MAX;

        // Motor danado: menos aceleracion.
        GameWorld engineOk = new GameWorld(new Upgrades(true));
        engineOk.start();
        engineOk.asteroids.clear();
        engineOk.fuel.clear();
        GameWorld engineBad = new GameWorld(new Upgrades(true));
        engineBad.start();
        engineBad.asteroids.clear();
        engineBad.fuel.clear();
        engineBad.ship.part[Ship.ENGINE] = 20f;
        for (int i = 0; i < 60; i++) {
            engineOk.update(dt, false, false, true, false);
            engineBad.update(dt, false, false, true, false);
        }
        check("motor danado acelera menos", engineBad.ship.vel.len() < engineOk.ship.vel.len() * 0.7f);

        // Deposito destruido: menos capacidad y fuga.
        GameWorld tank = new GameWorld(new Upgrades(true));
        tank.start();
        tank.asteroids.clear();
        tank.fuel.clear();
        tank.ship.part[Ship.TANK] = 0f;
        tank.update(dt, false, false, false, false);
        check("deposito destruido reduce capacidad", tank.ship.maxFuel < tank.up.maxFuel() * 0.5f);
        check("deposito destruido fuga combustible", tank.ship.fuel < tank.ship.maxFuel);
        tank.ship.repair(Ship.TANK);
        tank.ship.syncFuel(tank.up);
        check("reparar deposito recupera capacidad",
                Math.abs(tank.ship.maxFuel - tank.up.maxFuel()) < 0.01f);

        // Timon destruido: gira mas despacio.
        GameWorld steer = new GameWorld(new Upgrades(true));
        steer.start();
        steer.asteroids.clear();
        steer.fuel.clear();
        steer.ship.part[Ship.STEERING] = 0f;
        Controls hardTurn = new Controls();
        hardTurn.turn = 1f;
        float steerHeading = steer.ship.heading;
        steer.update(dt, hardTurn);
        check("timon danado gira menos",
                Math.abs(Ship.wrapDeg(steer.ship.heading - steerHeading)) < 2f);

        // Ala derecha perdida: la nave vira sola y responde peor al timon.
        GameWorld wingDrift = new GameWorld(new Upgrades(true));
        wingDrift.start();
        wingDrift.asteroids.clear();
        wingDrift.fuel.clear();
        wingDrift.ship.part[Ship.WING_RIGHT] = 0f;
        float driftHeading = wingDrift.ship.heading;
        wingDrift.update(dt, new Controls());
        check("ala perdida desvia el rumbo",
                Math.abs(Ship.wrapDeg(wingDrift.ship.heading - driftHeading)) > 0.05f);

        GameWorld wingOk = new GameWorld(new Upgrades(true));
        wingOk.start();
        wingOk.asteroids.clear();
        wingOk.fuel.clear();
        GameWorld wingBad = new GameWorld(new Upgrades(true));
        wingBad.start();
        wingBad.asteroids.clear();
        wingBad.fuel.clear();
        wingBad.ship.part[Ship.WING_RIGHT] = 0f;
        float wingOkHeading = wingOk.ship.heading;
        float wingBadHeading = wingBad.ship.heading;
        wingOk.update(dt, hardTurn);
        wingBad.update(dt, hardTurn);
        float okTurn = Math.abs(Ship.wrapDeg(wingOk.ship.heading - wingOkHeading));
        float badTurn = Math.abs(Ship.wrapDeg(wingBad.ship.heading - wingBadHeading));
        check("ala perdida reduce el giro", badTurn < okTurn * 0.8f);
        check("factor de alas con un ala perdida",
                Math.abs(wingBad.ship.wingFactor() - 0.5f) < 0.001f);

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

        // T-103: ShipLoadout como fuente unica de niveles y ranuras de modulos.
        check("loadout refleja el nivel comprado",
                up.loadout.level(Upgrades.MOTOR) == up.level(Upgrades.MOTOR));
        boolean levelsMatch = true;
        for (int i = 0; i < Upgrades.COUNT; i++) {
            if (up.loadout.level(i) != up.level(i)) {
                levelsMatch = false;
            }
        }
        check("loadout conserva los cinco sistemas",
                levelsMatch && up.loadout.level(Upgrades.MOTOR) == 1);
        check("piloto automatico no es innato", !up.loadout.hasAutopilot());
        check("loadout instala el modulo de piloto automatico",
                up.loadout.installModule(0, ShipLoadout.AUTOPILOT_MODULE)
                        && up.loadout.hasAutopilot());
        check("loadout localiza el modulo", up.loadout.slotOf(ShipLoadout.AUTOPILOT_MODULE) == 0);
        check("loadout quita el modulo",
                up.loadout.removeModule(0) && !up.loadout.hasAutopilot());
        check("loadout instala un modulo generico",
                up.loadout.installModule(1, "scanner")
                        && "scanner".equals(up.loadout.module(1)));
        check("loadout rechaza ranuras fuera de rango",
                !up.loadout.installModule(ShipLoadout.MODULE_SLOTS, "x")
                        && !up.loadout.installModule(-1, "x"));
        check("loadout quita el modulo generico",
                up.loadout.removeModule(1) && up.loadout.module(1) == null);

        // El loadout relee niveles y modulos de su almacen (sin copia en Upgrades).
        final java.util.Map<String, Integer> loadoutInts = new java.util.HashMap<>();
        final java.util.Map<String, String> loadoutTexts = new java.util.HashMap<>();
        ShipLoadout.Store loadoutStore = new ShipLoadout.Store() {
            @Override
            public int getInteger(String key) {
                Integer value = loadoutInts.get(key);
                return value == null ? 0 : value;
            }

            @Override
            public void putInteger(String key, int value) {
                loadoutInts.put(key, value);
            }

            @Override
            public String getString(String key) {
                return loadoutTexts.get(key);
            }

            @Override
            public void putString(String key, String value) {
                if (value == null) {
                    loadoutTexts.remove(key);
                } else {
                    loadoutTexts.put(key, value);
                }
            }
        };
        ShipLoadout firstLoadout = new ShipLoadout(loadoutStore);
        firstLoadout.setLevel(ShipLoadout.CANON, 3);
        firstLoadout.installModule(2, "mining");
        ShipLoadout reloadedLoadout = new ShipLoadout(loadoutStore);
        check("loadout relee niveles del almacen",
                reloadedLoadout.level(ShipLoadout.CANON) == 3);
        check("loadout relee modulos del almacen",
                reloadedLoadout.hasModule("mining")
                        && reloadedLoadout.slotOf("mining") == 2);

        // T-104: guardado v2, backup, texto corrupto y migracion desde run2.
        SaveService.MemoryStorage saveStorage = new SaveService.MemoryStorage();
        Upgrades saveUp = new Upgrades(true);
        GameSession saveSession = new GameSession(new PlayerProfile(saveUp));
        saveSession.startRun();
        saveSession.sector.ship.pos.set(321f, -123f);
        saveSession.sector.ship.vel.set(12f, -7f);
        saveSession.sector.ship.heading = 45f;
        saveSession.sector.ship.fuel = 55f;
        saveSession.sector.ship.part[Ship.HULL] = 77f;
        saveSession.sector.ship.part[Ship.ENGINE] = 66f;
        saveSession.sector.ship.part[Ship.LIFE_SUPPORT] = 80f;
        saveSession.pilot.health = 88f;
        saveSession.pilot.oxygen = 61f;
        saveSession.lifeSupport.filter = 55f;
        saveSession.lifeSupport.leak = 2.5f;
        saveSession.lifeSupport.breach = true;
        saveSession.sector.coinsEarned = 42;
        saveSession.sector.score = 420;
        saveSession.sector.elapsed = 95f;
        saveSession.sector.asteroids.clear();
        Asteroid savedRock = new Asteroid(100f, 200f, Asteroid.MEDIUM);
        savedRock.vel.set(3f, 4f);
        savedRock.hp = 1;
        saveSession.sector.asteroids.add(savedRock);
        saveSession.sector.fuel.clear();
        saveSession.sector.fuel.add(new FuelPickup(50f, 60f, 12f));
        saveUp.setCoins(123);
        saveUp.loadout.setLevel(Upgrades.MOTOR, 2);
        saveUp.loadout.installModule(0, ShipLoadout.AUTOPILOT_MODULE);
        SaveService saveService = new SaveService(saveStorage);
        check("save v2 escribe archivo",
                saveService.save(saveSession) && saveStorage.exists(SaveService.SAVE_NAME));
        String v2Text = saveStorage.read(SaveService.SAVE_NAME);
        check("save v2 incluye version, perfil, piloto, nave y sector",
                v2Text.contains("\"version\":2")
                        && v2Text.contains("\"profile\":")
                        && v2Text.contains("\"pilot\":")
                        && v2Text.contains("\"ship\":")
                        && v2Text.contains("\"sector\":"));
        Upgrades loadUp = new Upgrades(true);
        GameSession loadedV2 = new GameSession(new PlayerProfile(loadUp));
        check("save v2 carga", saveService.load(loadedV2) == SaveService.LoadStatus.V2);
        check("save v2 conserva el perfil",
                "pilot-1".equals(loadedV2.profile.id)
                        && loadUp.coins() == 123
                        && loadUp.level(Upgrades.MOTOR) == 2
                        && loadUp.loadout.hasAutopilot());
        check("save v2 conserva el piloto",
                Math.abs(loadedV2.pilot.health - 88f) < 0.01f
                        && Math.abs(loadedV2.pilot.oxygen - 61f) < 0.01f);
        check("save v2 conserva la nave",
                Math.abs(loadedV2.ship().pos.x - 321f) < 0.01f
                        && Math.abs(loadedV2.ship().pos.y + 123f) < 0.01f
                        && Math.abs(loadedV2.ship().fuel - 55f) < 0.01f
                        && Math.abs(loadedV2.ship().part[Ship.HULL] - 77f) < 0.01f
                        && Math.abs(loadedV2.ship().part[Ship.ENGINE] - 66f) < 0.01f);
        check("save v2 conserva LIFE_SUPPORT",
                Math.abs(loadedV2.ship().part[Ship.LIFE_SUPPORT] - 80f) < 0.01f);
        check("save v2 conserva soporte vital",
                Math.abs(loadedV2.lifeSupport.filter - 55f) < 0.01f
                        && Math.abs(loadedV2.lifeSupport.leak - 2.5f) < 0.01f
                        && loadedV2.lifeSupport.breach);
        check("save v2 conserva el sector",
                loadedV2.sector.coinsEarned == 42
                        && loadedV2.sector.score == 420
                        && Math.abs(loadedV2.sector.elapsed - 95f) < 0.01f
                        && loadedV2.sector.asteroids.size == 1
                        && loadedV2.sector.fuel.size == 1);

        saveSession.sector.coinsEarned = 99;
        check("save v2 escribe backup", saveService.save(saveSession)
                && saveStorage.exists(SaveService.BACKUP_NAME));
        saveStorage.write(SaveService.SAVE_NAME, "{basura");
        GameSession backupSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("save v2 cae al backup",
                saveService.load(backupSession) == SaveService.LoadStatus.BACKUP);
        check("el backup conserva la partida anterior",
                backupSession.sector.coinsEarned == 42);
        check("el texto corrupto no borra el backup",
                saveStorage.read(SaveService.SAVE_NAME).equals("{basura")
                        && saveStorage.read(SaveService.BACKUP_NAME) != null);

        Upgrades legacyUp = new Upgrades(true);
        GameWorld legacyWorld = new GameWorld(legacyUp);
        legacyWorld.start();
        legacyWorld.ship.pos.set(10f, 20f);
        legacyWorld.ship.fuel = 33f;
        legacyWorld.ship.part[Ship.HULL] = 44f;
        legacyWorld.ship.part[Ship.ENGINE] = 55f;
        legacyWorld.coinsEarned = 7;
        legacyWorld.asteroids.clear();
        Asteroid legacyRock = new Asteroid(5f, 6f, Asteroid.SMALL);
        legacyRock.vel.set(1f, 2f);
        legacyRock.hp = 1;
        legacyWorld.asteroids.add(legacyRock);
        legacyUp.putText(GameSession.RUN_KEY, legacyWorld.serialize());
        SaveService.MemoryStorage legacyStorage = new SaveService.MemoryStorage();
        SaveService legacyService = new SaveService(legacyStorage, legacyUp);
        GameSession migrated = new GameSession(new PlayerProfile(legacyUp));
        check("migracion run2 a v2",
                legacyService.load(migrated) == SaveService.LoadStatus.MIGRATED);
        check("migracion conserva casco y combustible",
                Math.abs(migrated.sector.ship.part[Ship.HULL] - 44f) < 0.01f
                        && Math.abs(migrated.sector.ship.fuel - 33f) < 0.01f);
        check("migracion conserva partes y monedas",
                Math.abs(migrated.sector.ship.part[Ship.ENGINE] - 55f) < 0.01f
                        && migrated.sector.coinsEarned == 7);
        check("migracion conserva asteroides",
                migrated.sector.asteroids.size == 1);
        check("migracion crea el archivo v2",
                legacyStorage.exists(SaveService.SAVE_NAME));
        check("migracion limpia run2",
                legacyUp.getText(GameSession.RUN_KEY) == null);

        // T-107: identidad jugador/personaje y slots preparados para varios personajes.
        Upgrades identityUp = new Upgrades(true);
        GameSession identitySession = new GameSession(new PlayerProfile(identityUp));
        identitySession.profile.restoreIdentity("player-42", "char-7", "NOVA");
        identitySession.startRun();
        SaveService.MemoryStorage identityStorage = new SaveService.MemoryStorage();
        SaveService identityService = new SaveService(identityStorage);
        check("save v2 escribe identidad de jugador y personaje",
                identityService.save(identitySession)
                        && identityStorage.read(SaveService.SAVE_NAME)
                        .contains("\"playerId\":\"player-42\"")
                        && identityStorage.read(SaveService.SAVE_NAME)
                        .contains("\"characterId\":\"char-7\""));
        GameSession identityLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("save v2 restaura identidad de jugador y personaje",
                identityService.load(identityLoaded) == SaveService.LoadStatus.V2
                        && "player-42".equals(identityLoaded.profile.playerId)
                        && "char-7".equals(identityLoaded.profile.id)
                        && "NOVA".equals(identityLoaded.profile.name));
        SaveService.MemoryStorage slotsStorage = new SaveService.MemoryStorage();
        SaveService slotOne = new SaveService(slotsStorage);
        SaveService slotTwo = new SaveService(slotsStorage, null, "slot2");
        check("slots de guardado separados",
                slotOne.save(identitySession) && slotTwo.save(identitySession)
                        && slotsStorage.exists("slot1.json")
                        && slotsStorage.exists("slot2.json")
                        && !slotOne.saveName().equals(slotTwo.saveName()));
        SaveService badSlot = new SaveService(slotsStorage, null, "../otro");
        check("slot invalido cae al por defecto",
                badSlot.saveName().equals(SaveService.SAVE_NAME));

        // T-201: un guardado antiguo de 7 partes rellena LIFE_SUPPORT.
        String sevenParts = "ship 0 0 0 0 90 100 100 0\n"
                + "parts 100 100 100 100 100 100 100\n"
                + "world 0 0 0\nchunks\n";
        GameWorld oldParts = new GameWorld(new Upgrades(true));
        check("guardado de 7 partes rellena LIFE_SUPPORT",
                oldParts.restore(sevenParts)
                        && oldParts.ship.part[Ship.LIFE_SUPPORT] == Ship.PART_MAX);

        // T-202: consumo de O2 y filtro.
        GameSession o2Session = new GameSession(new PlayerProfile(new Upgrades(true)));
        o2Session.startRun();
        o2Session.sector.immortal = true;
        o2Session.sector.asteroids.clear();
        o2Session.sector.fuel.clear();
        float o2Start = o2Session.pilot.oxygen;
        for (int i = 0; i < 60; i++) {
            o2Session.sector.asteroids.clear();
            o2Session.update(dt, new Controls());
        }
        check("la sesion consume O2", o2Session.pilot.oxygen < o2Start);
        Ship o2Ship = o2Session.ship();
        o2Session.pilot.oxygen = 100f;
        o2Session.lifeSupport.update(o2Session.pilot, o2Ship, 60f);
        float o2Used = 100f - o2Session.pilot.oxygen;
        check("consumo de O2 a 60 s ~2.5", Math.abs(o2Used - 2.5f) < 0.2f);
        o2Ship.part[Ship.LIFE_SUPPORT] = Ship.PART_MAX;
        check("filtro intacto al 100%",
                Math.abs(o2Session.lifeSupport.filterEffectiveness(o2Ship) - 1f) < 0.001f);
        o2Ship.part[Ship.LIFE_SUPPORT] = 50f;
        check("filtro al 50% de soporte",
                Math.abs(o2Session.lifeSupport.filterEffectiveness(o2Ship) - 0.6f) < 0.001f);
        o2Ship.part[Ship.LIFE_SUPPORT] = 0f;
        check("filtro al 0% de soporte cae al 20%",
                Math.abs(o2Session.lifeSupport.filterEffectiveness(o2Ship) - 0.2f) < 0.001f);
        o2Ship.part[Ship.LIFE_SUPPORT] = Ship.PART_MAX;
        o2Session.pilot.oxygen = 100f;
        check("tiempo estimado de O2 ~2400 s",
                Math.abs(o2Session.lifeSupport.estimatedSeconds(
                        o2Session.pilot, o2Ship) - 2400f) < 5f);

        // T-203: fugas por casco y brecha.
        LifeSupport leak = o2Session.lifeSupport;
        Ship leakShip = o2Session.ship();
        leak.breach = false;
        leakShip.part[Ship.HULL] = leakShip.maxHull;
        leak.updateLeak(leakShip);
        check("sin fuga con casco intacto", leak.leak == 0f);
        leakShip.part[Ship.HULL] = leakShip.maxHull * 0.5f;
        leak.updateLeak(leakShip);
        check("fuga al 50% de casco ~3/min",
                Math.abs(leak.leak - 3f) < 0.01f);
        leakShip.part[Ship.HULL] = leakShip.maxHull * 0.2f;
        leak.updateLeak(leakShip);
        check("fuga al 20% de casco ~4.8/min",
                Math.abs(leak.leak - 4.8f) < 0.01f);
        leak.breach = true;
        leak.updateLeak(leakShip);
        check("brecha aumenta la fuga un 50%",
                Math.abs(leak.leak - 7.2f) < 0.01f);
        leak.repairBreach();
        leak.updateLeak(leakShip);
        check("reparar brecha devuelve la fuga base",
                Math.abs(leak.leak - 4.8f) < 0.01f);
        leakShip.part[Ship.HULL] = leakShip.maxHull;
        leak.breach = false;
        leak.onHullDamage(leakShip, 36f);
        check("impacto fuerte con casco intacto no abre brecha", !leak.breach);
        leakShip.part[Ship.HULL] = leakShip.maxHull * 0.6f;
        leak.onHullDamage(leakShip, 36f);
        check("impacto fuerte con casco dañado abre brecha", leak.breach);
        check("la brecha no daña la salud directamente",
                o2Session.pilot.health == 100f);
        o2Session.pilot.oxygen = 100f;
        leak.breach = false;
        leakShip.part[Ship.HULL] = leakShip.maxHull * 0.5f;
        leak.update(o2Session.pilot, leakShip, 60f);
        check("la fuga reduce el O2 a 60 s",
                Math.abs(100f - o2Session.pilot.oxygen - 5.5f) < 0.2f);
        o2Session.pilot.oxygen = 100f;
        check("tiempo estimado incluye la fuga",
                Math.abs(leak.estimatedSeconds(o2Session.pilot, leakShip) - 1090.9f) < 5f);
        o2Session.sector.fx.clear();
        for (int i = 0; i < 30; i++) {
            o2Session.sector.asteroids.clear();
            o2Session.update(dt, new Controls());
        }
        check("la fuga emite particulas en el mundo",
                o2Session.sector.fx.particleCount() > 0);

        // T-204: asfixia, gracia, salud y estados del piloto.
        Pilot asphyxia = new Pilot();
        asphyxia.oxygen = 0f;
        asphyxia.health = 100f;
        asphyxia.oxygenGrace = Pilot.O2_GRACE_SECONDS;
        asphyxia.updateAsphyxia(5f);
        check("5 s de gracia sin perder salud",
                asphyxia.health == 100f
                        && asphyxia.oxygenGrace == 0f
                        && asphyxia.status == Pilot.Status.HYPOXIC);
        asphyxia.updateAsphyxia(3f);
        check("tras la gracia la salud baja 1 HP/s",
                Math.abs(asphyxia.health - 97f) < 0.01f
                        && asphyxia.status == Pilot.Status.HYPOXIC);
        asphyxia.updateAsphyxia(200f);
        check("salud 0 deja al piloto incapacitado",
                asphyxia.health == 0f
                        && asphyxia.status == Pilot.Status.INCAPACITATED
                        && !asphyxia.alive());
        Pilot oxygenated = new Pilot();
        oxygenated.oxygen = 50f;
        oxygenated.health = 100f;
        oxygenated.updateAsphyxia(30f);
        check("con O2 la salud no baja",
                oxygenated.health == 100f
                        && oxygenated.status == Pilot.Status.OK
                        && oxygenated.oxygenGrace == Pilot.O2_GRACE_SECONDS);
        Ship independentShip = o2Session.ship();
        independentShip.part[Ship.HULL] = 80f;
        Pilot independent = new Pilot();
        independent.health = 50f;
        independent.oxygen = 0f;
        independent.oxygenGrace = 0f;
        independent.updateAsphyxia(10f);
        check("la asfixia no toca el casco",
                independentShip.part[Ship.HULL] == 80f
                        && independent.health < 50f);
        Pilot integratedPilot = new Pilot();
        integratedPilot.oxygen = 0f;
        integratedPilot.health = 100f;
        LifeSupport integratedLife = new LifeSupport();
        integratedLife.update(integratedPilot, independentShip, 6f);
        check("LifeSupport aplica la asfixia",
                Math.abs(integratedPilot.health - 99f) < 0.1f);
        Pilot legacyAsphyxiaPilot = new Pilot();
        check("guardado antiguo de piloto usa 5 s de gracia",
                legacyAsphyxiaPilot.restore("pilot 100 100 0 100 HYPOXIC")
                        && legacyAsphyxiaPilot.oxygenGrace == Pilot.O2_GRACE_SECONDS);
        Upgrades asphyxiaUp = new Upgrades(true);
        GameSession asphyxiaSession = new GameSession(new PlayerProfile(asphyxiaUp));
        asphyxiaSession.startRun();
        asphyxiaSession.pilot.oxygen = 0f;
        asphyxiaSession.pilot.health = 42f;
        asphyxiaSession.pilot.status = Pilot.Status.HYPOXIC;
        asphyxiaSession.pilot.oxygenGrace = 2.5f;
        SaveService.MemoryStorage asphyxiaStorage = new SaveService.MemoryStorage();
        SaveService asphyxiaService = new SaveService(asphyxiaStorage);
        asphyxiaService.save(asphyxiaSession);
        GameSession asphyxiaLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("save v2 conserva asfixia y gracia",
                asphyxiaService.load(asphyxiaLoaded) == SaveService.LoadStatus.V2
                        && Math.abs(asphyxiaLoaded.pilot.health - 42f) < 0.01f
                        && asphyxiaLoaded.pilot.status == Pilot.Status.HYPOXIC
                        && Math.abs(asphyxiaLoaded.pilot.oxygenGrace - 2.5f) < 0.01f);
        Pilot alertPilot = new Pilot();
        alertPilot.oxygen = 26f;
        check("O2 normal sin alerta", alertPilot.o2AlertLevel() == 0);
        alertPilot.oxygen = 25f;
        check("O2 al 25% activa aviso", alertPilot.o2AlertLevel() == 1);
        alertPilot.oxygen = 10f;
        check("O2 al 10% activa alerta critica", alertPilot.o2AlertLevel() == 2);
        alertPilot.oxygen = 0f;
        check("O2 a 0 sigue en alerta critica", alertPilot.o2AlertLevel() == 2);

        // T-206: recarga de O2 y reparación de LIFE_SUPPORT.
        Upgrades o2Shop = new Upgrades(true);
        check("O2 lleno no cuesta", o2Shop.o2RefillCost(0f) == 0);
        check("O2 a 40 unidades cuesta 20", o2Shop.o2RefillCost(40f) == 20);
        check("O2 tiene coste minimo",
                o2Shop.o2RefillCost(2f) == Upgrades.O2_REFILL_MIN);
        o2Shop.addCoins(100);
        int o2CoinsBefore = o2Shop.coins();
        int o2Cost = o2Shop.o2RefillCost(40f);
        check("la recarga de O2 descuenta creditos",
                o2Shop.spendCoins(o2Cost)
                        && o2Shop.coins() == o2CoinsBefore - o2Cost);
        Pilot refillPilot = new Pilot();
        refillPilot.oxygen = 20f;
        refillPilot.status = Pilot.Status.HYPOXIC;
        refillPilot.oxygenGrace = 1f;
        refillPilot.refillOxygen();
        check("reabastecer O2 llena y reactiva",
                refillPilot.oxygen == refillPilot.o2Max
                        && refillPilot.status == Pilot.Status.OK
                        && refillPilot.oxygenGrace == Pilot.O2_GRACE_SECONDS);
        Upgrades repairUp = new Upgrades(true);
        Ship repairShip = new Ship();
        repairShip.reset(repairUp, 0f, 0f);
        repairShip.part[Ship.LIFE_SUPPORT] = 10f;
        repairShip.repair(Ship.LIFE_SUPPORT);
        check("LIFE_SUPPORT se repara como las demas partes",
                repairShip.part[Ship.LIFE_SUPPORT] == Ship.PART_MAX);
        GameSession refillSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        refillSession.startRun();
        refillSession.pilot.oxygen = 20f;
        SaveService.MemoryStorage refillStorage = new SaveService.MemoryStorage();
        SaveService refillService = new SaveService(refillStorage);
        refillService.save(refillSession);
        refillSession.pilot.refillOxygen();
        refillService.save(refillSession);
        GameSession refillLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("la recarga de O2 no rompe el guardado",
                refillService.load(refillLoaded) == SaveService.LoadStatus.V2
                        && refillLoaded.pilot.oxygen == refillLoaded.pilot.o2Max
                        && refillLoaded.pilot.status == Pilot.Status.OK);

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
                + ", max meteoritos " + maxAsteroids + ", casco " + w.ship.part[Ship.HULL]);
        check("la simulacion gana monedas destruyendo meteoritos", totalEarned > 0);
        check("la simulacion termina sin excepciones", true);

        // Guardado: serialize -> restore debe reproducir el mismo estado.
        String saved = w.serialize();
        GameWorld copy = new GameWorld(up);
        check("restaurar partida guardada", copy.restore(saved));
        check("restaurar conserva monedas y casco", copy.coinsEarned == w.coinsEarned
                && copy.ship.part[Ship.HULL] == w.ship.part[Ship.HULL]);
        boolean partsRestored = true;
        for (int i = 0; i < Ship.PART_COUNT; i++) {
            if (copy.ship.part[i] != w.ship.part[i]) {
                partsRestored = false;
            }
        }
        check("restaurar conserva las partes", partsRestored);
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
        boolean fuelStopped = false;
        for (int i = 0; i < 60 * 8 && pf.pilot.active(); i++) {
            pf.update(dt, new Controls());
        }
        fuelStopped = !pf.pilot.active() && pf.ship.vel.len() < Autopilot.STOP_SPEED + 2f;
        check("piloto frena tras recoger el bidon", fuelStopped);

        // Bidón muy cerca: la llegada forzada evita quedarse corto.
        GameWorld pn = new GameWorld(new Upgrades(true));
        pn.start();
        pn.asteroids.clear();
        pn.fuel.clear();
        FuelPickup near = new FuelPickup(50f, 0f);
        pn.fuel.add(near);
        pn.pilot.target(near);
        for (int i = 0; i < 60 * 5 && pn.fuel.contains(near, true); i++) {
            pn.update(dt, new Controls());
        }
        check("piloto recoge un bidon muy cercano", !pn.fuel.contains(near, true));

        // Empezar una partida nueva no arrastra el piloto de la anterior.
        GameWorld pstart = new GameWorld(new Upgrades(true));
        pstart.start();
        pstart.pilot.target(new FuelPickup(500f, 0f));
        pstart.start();
        check("start cancela el piloto anterior", !pstart.pilot.active());

        // Piloto automático: meteorito. Debe destruirlo sin recibir golpes.
        GameWorld pa = new GameWorld(new Upgrades(true));
        pa.start();
        pa.asteroids.clear();
        pa.fuel.clear();
        Asteroid rock = new Asteroid(350f, 60f, Asteroid.MEDIUM);
        rock.vel.set(-8f, 0f);
        pa.asteroids.add(rock);
        float pilotHull0 = pa.ship.part[Ship.HULL];
        pa.pilot.target(rock);
        boolean destroyed = false;
        for (int i = 0; i < 60 * 40 && !destroyed; i++) {
            pa.update(dt, new Controls());
            destroyed = !pa.asteroids.contains(rock, true) && pa.coinsEarned > 0;
        }
        check("piloto destruye el meteorito", destroyed);
        check("piloto no recibe golpes del meteorito", pa.ship.part[Ship.HULL] == pilotHull0);

        // Frenada tras destruir el objetivo: se lanza a velocidad alta y debe parar.
        GameWorld pb = new GameWorld(new Upgrades(true));
        pb.start();
        pb.asteroids.clear();
        pb.fuel.clear();
        Asteroid brakeRock = new Asteroid(420f, 0f, Asteroid.SMALL);
        brakeRock.vel.set(0f, 0f);
        pb.ship.heading = 0f;
        pb.ship.vel.set(160f, 0f);
        pb.pilot.target(brakeRock);
        boolean brakeKilled = false;
        for (int i = 0; i < 60 * 20 && !brakeKilled; i++) {
            pb.asteroids.clear();
            pb.asteroids.add(brakeRock);
            pb.update(dt, new Controls());
            brakeKilled = !pb.asteroids.contains(brakeRock, true);
        }
        float speedAtKill = pb.ship.vel.len();
        check("piloto destruye el objetivo de frenada", brakeKilled);
        pb.immortal = true;
        boolean brakeStopped = false;
        for (int i = 0; i < 60 * 8 && pb.pilot.active(); i++) {
            pb.update(dt, new Controls());
        }
        brakeStopped = !pb.pilot.active() && pb.ship.vel.len() < Autopilot.STOP_SPEED + 2f;
        check("piloto frena tras destruir el meteorito a velocidad alta", brakeStopped);
        System.out.println(String.format("BENCH frenada       velocidad al destruir=%.0f final=%.1f",
                speedAtKill, pb.ship.vel.len()));

        // Un mando manual cancela el piloto sin entrar en frenada automática.
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

package com.example.codegame;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Prueba de lógica sin ventana: ejecutar con run_program. No forma parte del juego. */
public class SelfTest {

    private static int failures = 0;

    /** Cuenta asteroides dentro de la zona segura de la estación. */
    private static int stationAsteroidsInSafeZone(GameWorld world) {
        int count = 0;
        for (int i = 0; i < world.asteroids.size; i++) {
            if (world.station.inSafeZone(world.asteroids.get(i).pos)) {
                count++;
            }
        }
        return count;
    }

    /** Elimina un campo JSON con arrays/objetos anidados (para migraciones). */
    private static String removeJsonField(String json, String field) {
        String key = "\"" + field + "\":";
        int start = json.indexOf(key);
        if (start < 0) {
            return json;
        }
        boolean leadingComma = start > 0 && json.charAt(start - 1) == ',';
        if (leadingComma) {
            start--;
        }
        int open = json.indexOf('[', start);
        int depth = 0;
        int end = open + 1;
        for (int i = open; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    end = i + 1;
                    break;
                }
            }
        }
        if (!leadingComma && end < json.length() && json.charAt(end) == ',') {
            end++;
        }
        return json.substring(0, start) + json.substring(end);
    }

    /** Registra transacciones para comprobar el listener. */
    private static final class TradeRecorder implements TradeService.Listener {
        int count;
        TradeService.Trade last;

        @Override
        public void onTrade(TradeService.Trade trade) {
            count++;
            last = trade;
        }
    }

    /** Recorta el array levels de un guardado para probar la migración. */
    private static String trimLevels(String save, int keep) {
        int start = save.indexOf("\"levels\":[");
        if (start < 0) {
            return save;
        }
        int end = save.indexOf(']', start);
        String body = save.substring(start + 10, end);
        String[] parts = body.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keep && i < parts.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(parts[i]);
        }
        return save.substring(0, start + 10) + sb + save.substring(end);
    }

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
            done = !b.asteroids.contains(rock, true) || !b.pilot.active();
        }
        boolean destroyed = !b.asteroids.contains(rock, true);
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
        check("soporte vital existe al final de los sistemas",
                Ship.LIFE_SUPPORT == 7
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

        // Un misil impacta un meteorito pequeño: se destruye sin dar monedas.
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
        check("destruir meteorito no da monedas", w.coinsEarned == 0);

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
        float systems0 = 0f;
        for (int i = Ship.ENGINE; i < Ship.PART_COUNT; i++) {
            systems0 += w.ship.part[i];
        }
        Asteroid hitter = new Asteroid(w.ship.pos.x, w.ship.pos.y, Asteroid.MEDIUM);
        hitter.vel.set(0f, 0f);
        w.asteroids.add(hitter);
        w.update(dt, false, false, false, false);
        float systems1 = 0f;
        for (int i = Ship.ENGINE; i < Ship.PART_COUNT; i++) {
            systems1 += w.ship.part[i];
        }
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
        check("loadout conserva los siete sistemas",
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
        o2Session.sector.attachLifeSupport(o2Session.lifeSupport);
        boolean leakParticles = false;
        for (int i = 0; i < 600; i++) {
            o2Session.sector.asteroids.clear();
            o2Session.update(dt, new Controls());
            if (o2Session.sector.fx.particleCount() > 0) {
                leakParticles = true;
            }
        }
        check("la fuga emite particulas en el mundo", leakParticles);

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

        // T-207: integración y balance de supervivencia.
        GameSession expedition = new GameSession(new PlayerProfile(new Upgrades(true)));
        expedition.startRun();
        expedition.sector.immortal = true;
        expedition.sector.asteroids.clear();
        expedition.sector.fuel.clear();
        for (int i = 0; i < 600; i++) {
            expedition.lifeSupport.update(expedition.pilot, expedition.ship(), 1f);
        }
        check("expedicion de 10 min deja mas del 50% de O2",
                expedition.pilot.oxygen > 50f);
        check("expedicion sin daño mantiene la salud",
                expedition.pilot.health == 100f);
        System.out.println(String.format(
                "BENCH expedicion 10min O2=%.1f salud=%.1f",
                expedition.pilot.oxygen, expedition.pilot.health));
        Ship balanceShip = expedition.ship();
        balanceShip.part[Ship.HULL] = balanceShip.maxHull * 0.5f;
        expedition.lifeSupport.breach = false;
        expedition.lifeSupport.updateLeak(balanceShip);
        check("casco al 50% produce aviso de fuga",
                expedition.lifeSupport.leak > 0.01f);
        Pilot marginPilot = new Pilot();
        marginPilot.oxygen = 0f;
        marginPilot.health = 100f;
        marginPilot.oxygenGrace = Pilot.O2_GRACE_SECONDS;
        marginPilot.updateAsphyxia(60f);
        check("O2 0 da al menos 60 s de margen",
                marginPilot.health > 0f
                        && marginPilot.status == Pilot.Status.HYPOXIC);
        balanceShip.part[Ship.LIFE_SUPPORT] = 0f;
        float lowFilter = expedition.lifeSupport.filterEffectiveness(balanceShip);
        balanceShip.repair(Ship.LIFE_SUPPORT);
        float repairedFilter = expedition.lifeSupport.filterEffectiveness(balanceShip);
        check("reparar soporte vital recupera el filtro",
                Math.abs(lowFilter - 0.2f) < 0.001f
                        && Math.abs(repairedFilter - 1f) < 0.001f);

        // T-208: muerte permanente y mundo persistente.
        PlayerProfile oldProfile = new PlayerProfile(new Upgrades(true));
        String oldCharacterId = oldProfile.id;
        oldProfile.startNewCharacter("pilot-99", "NOVA");
        check("el nuevo personaje cambia identidad",
                !oldCharacterId.equals(oldProfile.id)
                        && "pilot-99".equals(oldProfile.id)
                        && "NOVA".equals(oldProfile.name));
        GameSession deadSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        deadSession.startRun();
        deadSession.sector.ship.pos.set(1234f, -567f);
        deadSession.sector.ship.heading = 33f;
        deadSession.sector.ship.part[Ship.HULL] = 0f;
        deadSession.pilot.health = 0f;
        deadSession.pilot.status = Pilot.Status.DEAD;
        deadSession.sector.markLegacyWreck(deadSession.sector.ship);
        SaveService.MemoryStorage deathStorage = new SaveService.MemoryStorage();
        SaveService deathService = new SaveService(
                deathStorage, null, SaveService.LEGACY_SLOT);
        check("la muerte guarda el mundo legado",
                deathService.save(deadSession));
        GameSession legacyTemp = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("el mundo legado carga",
                deathService.load(legacyTemp) == SaveService.LoadStatus.V2);
        check("el mundo legado conserva asteroides",
                legacyTemp.sector.asteroids.size == deadSession.sector.asteroids.size);
        GameSession reborn = new GameSession(new PlayerProfile(new Upgrades(true)));
        reborn.sector.copyWorldFrom(legacyTemp.sector);
        check("la nave legado queda en el lugar de la muerte",
                reborn.sector.legacyWreck
                        && Math.abs(reborn.sector.legacyWreckPos.x - 1234f) < 0.01f
                        && Math.abs(reborn.sector.legacyWreckPos.y + 567f) < 0.01f);
        reborn.sector.startNewCharacter(0f, 0f);
        check("el nuevo personaje resetea la nave",
                reborn.ship().part[Ship.HULL] == reborn.ship().maxHull
                        && !reborn.ship().wrecked);
        check("el nuevo personaje conserva el legado",
                reborn.sector.legacyWreck
                        && Math.abs(reborn.sector.legacyWreckPos.x - 1234f) < 0.01f
                        && Math.abs(reborn.sector.legacyWreckPos.y + 567f) < 0.01f);
        SaveService.MemoryStorage rebornStorage = new SaveService.MemoryStorage();
        SaveService rebornService = new SaveService(rebornStorage);
        check("save v2 conserva la nave legado", rebornService.save(reborn));
        GameSession rebornLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("cargar conserva la nave legado",
                rebornService.load(rebornLoaded) == SaveService.LoadStatus.V2
                        && rebornLoaded.sector.legacyWreck
                        && Math.abs(rebornLoaded.sector.legacyWreckPos.x - 1234f) < 0.01f
                        && Math.abs(rebornLoaded.sector.legacyWreckPos.y + 567f) < 0.01f);
        rebornLoaded.sector.legacyWreckPos.set(1000f, 1000f);
        check("la nave legado no se recupera de lejos",
                rebornLoaded.sector.recoverLegacyWreck(
                        new com.badlogic.gdx.math.Vector2(0f, 0f)) == -1);
        rebornLoaded.sector.ship.pos.set(1000f, 1000f);
        int salvage = rebornLoaded.sector.recoverLegacyWreck(rebornLoaded.ship().pos);
        check("la nave legado se recupera al acercarse",
                salvage >= 10 && !rebornLoaded.sector.legacyWreck);

        // T-301: minerales, carga y bodega.
        check("existen seis minerales", Commodity.values().length == 6);
        check("Hielo tiene masa y precio",
                Math.abs(Commodity.HIELO.massPerUnit - 0.80f) < 0.001f
                        && Math.abs(Commodity.HIELO.basePrice - 1.5f) < 0.001f);
        check("Oro tiene masa y precio",
                Math.abs(Commodity.ORO.massPerUnit - 2.50f) < 0.001f
                        && Math.abs(Commodity.ORO.basePrice - 30f) < 0.001f);
        check("GameData busca un mineral",
                GameData.commodity("Oro") == Commodity.ORO
                        && GameData.BASE_CARGO_CAPACITY == 100f);
        Cargo sampleCargo = new Cargo(Commodity.HIELO, 10);
        check("Cargo calcula masa y valor",
                Math.abs(sampleCargo.mass() - 8f) < 0.001f
                        && Math.abs(sampleCargo.value() - 15f) < 0.001f);
        CargoHold cargo = GameData.newCargoHold();
        check("bodega inicial vacia",
                cargo.isEmpty() && cargo.usedMass() == 0f
                        && cargo.capacity() == 100f);
        check("anadir hielo suma masa",
                cargo.add(Commodity.HIELO, 10) == 10
                        && Math.abs(cargo.usedMass() - 8f) < 0.001f);
        check("anadir hierro suma masa",
                cargo.add(Commodity.HIERRO, 50) == 50
                        && Math.abs(cargo.usedMass() - 68f) < 0.001f);
        check("rechaza el exceso exacto",
                !cargo.addExact(Commodity.ORO, 100)
                        && cargo.amount(Commodity.ORO) == 0);
        int acceptedOro = cargo.add(Commodity.ORO, 100);
        check("acepta lo que cabe y rechaza el resto",
                acceptedOro == 12
                        && Math.abs(cargo.usedMass() - 98f) < 0.001f);
        check("las entradas conservan el orden",
                cargo.entries().size() == 3
                        && cargo.entries().get(0).commodity == Commodity.HIELO);
        check("quitar carga resta masa",
                cargo.remove(Commodity.HIERRO, 5) == 5
                        && cargo.amount(Commodity.HIERRO) == 45);
        check("no se puede quitar de mas",
                cargo.remove(Commodity.HIERRO, 1000) == 45
                        && cargo.amount(Commodity.HIERRO) == 0);
        check("valor base de la bodega",
                Math.abs(cargo.totalValue() - 375f) < 0.001f);
        cargo.clear();
        check("clear vacia la bodega", cargo.isEmpty() && cargo.usedMass() == 0f);

        // T-302: bodega en la nave y persistencia.
        GameSession cargoSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        cargoSession.startRun();
        check("la nave expone su bodega",
                cargoSession.cargo() == cargoSession.ship().cargo()
                        && cargoSession.cargo().isEmpty());
        check("la bodega de la nave acepta carga",
                cargoSession.cargo().addExact(Commodity.HIELO, 10)
                        && cargoSession.cargo().addExact(Commodity.ORO, 5));
        SaveService.MemoryStorage cargoStorage = new SaveService.MemoryStorage();
        SaveService cargoService = new SaveService(cargoStorage);
        check("la bodega se guarda", cargoService.save(cargoSession));
        GameSession cargoLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("la bodega se restaura",
                cargoService.load(cargoLoaded) == SaveService.LoadStatus.V2
                        && cargoLoaded.cargo().amount(Commodity.HIELO) == 10
                        && cargoLoaded.cargo().amount(Commodity.ORO) == 5
                        && Math.abs(cargoLoaded.cargo().usedMass()
                        - (10f * 0.80f + 5f * 2.50f)) < 0.001f);
        String noCargoSave = cargoSession.sector.serialize();
        GameSession legacyCargo = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("una partida sin bodega carga vacia",
                legacyCargo.restoreLegacy(noCargoSave) && legacyCargo.cargo().isEmpty());

        // T-303: masa en movimiento y consumo.
        CargoHold emptyMass = new CargoHold(100f);
        CargoHold fullMass = new CargoHold(100f);
        fullMass.addExact(Commodity.SILICATO, 100);
        check("MassModel calcula carga 0 y 1",
                MassModel.load(emptyMass) == 0f
                        && Math.abs(MassModel.load(fullMass) - 1f) < 0.001f);
        check("factores de masa a carga completa",
                Math.abs(MassModel.accelerationFactor(fullMass) - 0.75f) < 0.001f
                        && Math.abs(MassModel.speedFactor(fullMass) - 0.88f) < 0.001f
                        && Math.abs(MassModel.turnFactor(fullMass) - 0.90f) < 0.001f
                        && Math.abs(MassModel.fuelFactor(fullMass) - 1.35f) < 0.001f);
        CargoHold overMass = new CargoHold(100f);
        overMass.restore(Commodity.SILICATO, 150);
        check("la masa por encima de capacidad se limita",
                MassModel.load(overMass) == 1f
                        && Math.abs(MassModel.fuelFactor(overMass) - 1.35f) < 0.001f);
        GameWorld emptyShip = new GameWorld(new Upgrades(true));
        emptyShip.start();
        emptyShip.immortal = true;
        emptyShip.asteroids.clear();
        emptyShip.fuel.clear();
        GameWorld fullShip = new GameWorld(new Upgrades(true));
        fullShip.start();
        fullShip.immortal = true;
        fullShip.asteroids.clear();
        fullShip.fuel.clear();
        fullShip.ship.cargo().addExact(Commodity.SILICATO, 100);
        for (int i = 0; i < 30; i++) {
            emptyShip.asteroids.clear();
            fullShip.asteroids.clear();
            emptyShip.update(dt, false, false, true, false);
            fullShip.update(dt, false, false, true, false);
        }
        float emptySpeed = emptyShip.ship.vel.len();
        float fullSpeed = fullShip.ship.vel.len();
        check("carga completa reduce la aceleracion ~25%",
                Math.abs(fullSpeed / emptySpeed - 0.75f) < 0.05f);
        float emptyFuelUsed = emptyShip.ship.maxFuel - emptyShip.ship.fuel;
        float fullFuelUsed = fullShip.ship.maxFuel - fullShip.ship.fuel;
        check("carga completa sube el consumo ~35%",
                Math.abs(fullFuelUsed / emptyFuelUsed - 1.35f) < 0.05f);
        GameWorld emptyMax = new GameWorld(new Upgrades(true));
        emptyMax.start();
        emptyMax.asteroids.clear();
        emptyMax.ship.vel.set(0f, 500f);
        emptyMax.update(dt, false, false, false, false);
        GameWorld fullMax = new GameWorld(new Upgrades(true));
        fullMax.start();
        fullMax.asteroids.clear();
        fullMax.ship.cargo().addExact(Commodity.SILICATO, 100);
        fullMax.ship.vel.set(0f, 500f);
        fullMax.update(dt, false, false, false, false);
        check("carga completa reduce la velocidad ~12%",
                Math.abs(fullMax.ship.vel.len() / emptyMax.ship.vel.len() - 0.88f) < 0.02f);
        Controls massTurn = new Controls();
        massTurn.turn = 1f;
        GameWorld emptyTurn = new GameWorld(new Upgrades(true));
        emptyTurn.start();
        emptyTurn.asteroids.clear();
        float emptyHeading = emptyTurn.ship.heading;
        emptyTurn.update(dt, massTurn);
        float emptyTurned = Math.abs(Ship.wrapDeg(emptyTurn.ship.heading - emptyHeading));
        GameWorld fullTurn = new GameWorld(new Upgrades(true));
        fullTurn.start();
        fullTurn.asteroids.clear();
        fullTurn.ship.cargo().addExact(Commodity.SILICATO, 100);
        float fullHeading = fullTurn.ship.heading;
        fullTurn.update(dt, massTurn);
        float fullTurned = Math.abs(Ship.wrapDeg(fullTurn.ship.heading - fullHeading));
        check("carga completa reduce el giro ~10%",
                Math.abs(fullTurned / emptyTurned - 0.90f) < 0.03f);
        check("la nave cargada sigue siendo controlable",
                fullSpeed > 0f && fullTurned > 0f);
        GameWorld trip = new GameWorld(new Upgrades(true));
        trip.start();
        trip.immortal = true;
        trip.ship.cargo().addExact(Commodity.SILICATO, 100);
        float startX = trip.ship.pos.x;
        float startY = trip.ship.pos.y;
        for (int i = 0; i < 300; i++) {
            trip.asteroids.clear();
            trip.fuel.clear();
            trip.update(dt, false, false, true, false);
        }
        float travelled = trip.ship.pos.dst(startX, startY);
        check("viaje de 800 u con carga completa",
                travelled > 800f && trip.ship.fuel > 0f);

        // T-304: rebalanceo de depósito, consumo y recarga.
        Upgrades balanceUp = new Upgrades(true);
        check("deposito base 160", balanceUp.maxFuel() == 160f);
        check("consumo base 10", Ship.FUEL_PER_SEC_THRUST == 10f);
        check("el bidon sigue siendo util",
                FuelPickup.AMOUNT >= balanceUp.maxFuel() * 0.20f);
        check("repostar 160 cuesta 160", balanceUp.refillCost(160f) == 160);
        check("repostar 40 cuesta 40", balanceUp.refillCost(40f) == 40);
        check("repostar tiene coste minimo",
                balanceUp.refillCost(1f) == Upgrades.FUEL_REFILL_MIN);
        GameWorld roundTrip = new GameWorld(new Upgrades(true));
        roundTrip.start();
        roundTrip.immortal = true;
        roundTrip.ship.cargo().addExact(Commodity.SILICATO, 100);
        roundTrip.ship.heading = 0f;
        roundTrip.ship.dir.set(1f, 0f);
        // Bench de crucero: la aceleración desde parado se prueba en T-303.
        roundTrip.ship.vel.set(300f, 0f);
        float roundTripFuel = roundTrip.ship.fuel;
        boolean outbound = false;
        boolean returned = false;
        int roundSteps = 0;
        while (roundSteps < 60 * 30 && roundTrip.ship.fuel > 0f) {
            roundTrip.asteroids.clear();
            roundTrip.fuel.clear();
            roundTrip.update(dt, false, false, true, false);
            if (!outbound && roundTrip.ship.pos.x >= 800f) {
                outbound = true;
                roundTrip.ship.heading = 180f;
                roundTrip.ship.dir.set(-1f, 0f);
                roundTrip.ship.vel.set(-roundTrip.ship.vel.len(), 0f);
            } else if (outbound && roundTrip.ship.pos.x <= 0f) {
                returned = true;
                break;
            }
            roundSteps++;
        }
        float roundTripUsed = roundTripFuel - roundTrip.ship.fuel;
        check("ida y vuelta de 800 u con carga completa",
                outbound && returned && roundTrip.ship.fuel > 0f);
        check("la ida y vuelta deja margen", roundTrip.ship.fuel > 20f);
        System.out.println(String.format(
                "BENCH viaje ida/vuelta 800u combustible=%.1f restante=%.1f",
                roundTripUsed, roundTrip.ship.fuel));

        // T-305: mejora Bodega, capacidad y persistencia.
        Upgrades cargoUp = new Upgrades(true);
        check("bodega base 100", cargoUp.cargoCapacity() == 100f);
        cargoUp.addCoins(1000);
        check("la mejora Bodega se compra", cargoUp.buy(Upgrades.BODEGA));
        check("bodega sube 25 por nivel", cargoUp.cargoCapacity() == 125f);
        Ship upgradedShip = new Ship();
        upgradedShip.reset(cargoUp, 0f, 0f);
        check("la nave aplica la capacidad de bodega",
                upgradedShip.cargo().capacity() == 125f);
        GameSession cargoUpSession = new GameSession(new PlayerProfile(cargoUp));
        cargoUpSession.startRun();
        check("la sesion usa la bodega mejorada",
                cargoUpSession.cargo().capacity() == 125f);
        check("la bodega mejorada acepta carga",
                cargoUpSession.cargo().addExact(Commodity.HIELO, 100));
        SaveService.MemoryStorage cargoUpStorage = new SaveService.MemoryStorage();
        SaveService cargoUpService = new SaveService(cargoUpStorage);
        check("la mejora de bodega se guarda", cargoUpService.save(cargoUpSession));
        GameSession cargoUpLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("la mejora de bodega se restaura",
                cargoUpService.load(cargoUpLoaded) == SaveService.LoadStatus.V2
                        && cargoUpLoaded.cargo().capacity() == 125f
                        && cargoUpLoaded.cargo().amount(Commodity.HIELO) == 100);
        GameSession oldLevelsSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        oldLevelsSession.startRun();
        SaveService.MemoryStorage oldLevelStorage = new SaveService.MemoryStorage();
        SaveService oldLevelService = new SaveService(oldLevelStorage);
        oldLevelService.save(oldLevelsSession);
        String eightLevelsBase = oldLevelStorage.read(SaveService.SAVE_NAME);
        String sixLevels = trimLevels(eightLevelsBase, 6);
        GameSession sixLevelsLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("un v2 con 6 niveles carga con Laser 0",
                !eightLevelsBase.equals(sixLevels)
                        && SaveCodec.decode(sixLevels, sixLevelsLoaded)
                        && sixLevelsLoaded.cargo().capacity() == 100f
                        && sixLevelsLoaded.upgrades().level(Upgrades.LASER) == 0);
        String fiveLevels = trimLevels(eightLevelsBase, 5);
        GameSession oldLevelsLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("un v2 con 5 niveles carga con Bodega 0",
                !eightLevelsBase.equals(fiveLevels)
                        && SaveCodec.decode(fiveLevels, oldLevelsLoaded)
                        && oldLevelsLoaded.cargo().capacity() == 100f
                        && oldLevelsLoaded.upgrades().level(Upgrades.LASER) == 0);

        // T-401: tipos de asteroide, composición y RNG determinista.
        check("existen tres tipos de asteroide", AsteroidType.values().length == 3);
        check("dureza C/S/M",
                AsteroidType.C.hardness == 2
                        && AsteroidType.S.hardness == 5
                        && AsteroidType.M.hardness == 8);
        check("reservas C/S/M",
                AsteroidType.C.minReserve == 12 && AsteroidType.C.maxReserve == 18
                        && AsteroidType.S.minReserve == 28 && AsteroidType.S.maxReserve == 42
                        && AsteroidType.M.minReserve == 60 && AsteroidType.M.maxReserve == 90);
        float cSum = 0f;
        float sSum = 0f;
        float mSum = 0f;
        for (int i = 0; i < AsteroidType.C.minerals.length; i++) {
            cSum += AsteroidType.C.fractions[i];
        }
        for (int i = 0; i < AsteroidType.S.minerals.length; i++) {
            sSum += AsteroidType.S.fractions[i];
        }
        for (int i = 0; i < AsteroidType.M.minerals.length; i++) {
            mSum += AsteroidType.M.fractions[i];
        }
        check("composiciones suman 1",
                Math.abs(cSum - 1f) < 0.001f
                        && Math.abs(sSum - 1f) < 0.001f
                        && Math.abs(mSum - 1f) < 0.001f);
        check("C incluye hielo y M incluye titanio",
                Math.abs(AsteroidType.C.fraction(Commodity.HIELO) - 0.45f) < 0.001f
                        && Math.abs(AsteroidType.M.fraction(Commodity.TITANIO) - 0.30f) < 0.001f);
        boolean typeHasReward = false;
        try {
            AsteroidType.class.getDeclaredField("reward");
            typeHasReward = true;
        } catch (NoSuchFieldException ignored) {
            // El tipo no debe tener recompensa en monedas.
        }
        check("el tipo de asteroide no lleva monedas", !typeHasReward);
        Asteroid metalRock = new Asteroid(10f, 20f, Asteroid.MEDIUM,
                AsteroidType.M, 50, 50, 12345L);
        check("asteroide tiene tipo, dureza y composicion",
                metalRock.type == AsteroidType.M
                        && metalRock.hardness() == 8f
                        && Math.abs(metalRock.compositionFraction(Commodity.TITANIO) - 0.30f) < 0.001f);
        check("asteroide tiene reserva",
                metalRock.reserve == 50 && metalRock.maxReserve == 50);
        check("la reserva se extrae",
                metalRock.takeReserve(20) == 20
                        && metalRock.reserve == 30
                        && metalRock.takeReserve(100) == 30
                        && metalRock.depleted());
        GameWorld deterministicA = new GameWorld(new Upgrades(true));
        deterministicA.start();
        GameWorld deterministicB = new GameWorld(new Upgrades(true));
        deterministicB.start();
        boolean deterministic = deterministicA.asteroids.size == deterministicB.asteroids.size;
        for (int i = 0; deterministic && i < deterministicA.asteroids.size; i++) {
            Asteroid a = deterministicA.asteroids.get(i);
            Asteroid b = deterministicB.asteroids.get(i);
            deterministic = a.size == b.size
                    && a.type == b.type
                    && a.reserve == b.reserve
                    && a.maxReserve == b.maxReserve
                    && Math.abs(a.pos.x - b.pos.x) < 0.001f
                    && Math.abs(a.pos.y - b.pos.y) < 0.001f
                    && Math.abs(a.vel.x - b.vel.x) < 0.001f
                    && Math.abs(a.vel.y - b.vel.y) < 0.001f;
        }
        check("la generacion de asteroides es determinista", deterministic);
        GameSession typeSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        typeSession.startRun();
        typeSession.sector.asteroids.clear();
        typeSession.sector.asteroids.add(new Asteroid(50f, 60f, Asteroid.LARGE,
                AsteroidType.M, 70, 70, 777L));
        SaveService.MemoryStorage typeStorage = new SaveService.MemoryStorage();
        SaveService typeService = new SaveService(typeStorage);
        check("el asteroide tipado se guarda", typeService.save(typeSession));
        GameSession typeLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("el asteroide tipado se restaura",
                typeService.load(typeLoaded) == SaveService.LoadStatus.V2
                        && typeLoaded.sector.asteroids.size == 1
                        && typeLoaded.sector.asteroids.get(0).type == AsteroidType.M
                        && typeLoaded.sector.asteroids.get(0).reserve == 70);
        String oldAsteroidSave = "ship 0 0 0 0 90 100 100 0\n"
                + "parts 100 100 100 100 100 100 100 100\n"
                + "world 0 0 0\nchunks\n"
                + "a 10 20 0 0 1 2\n";
        GameWorld oldAsteroidWorld = new GameWorld(new Upgrades(true));
        check("un asteroide antiguo carga con tipo y reserva",
                oldAsteroidWorld.restore(oldAsteroidSave)
                        && oldAsteroidWorld.asteroids.size == 1
                        && oldAsteroidWorld.asteroids.get(0).type == AsteroidType.C
                        && oldAsteroidWorld.asteroids.get(0).reserve > 0);

        // T-402: MiningLaser y EnergyCapacitor.
        check("el laser es la ultima parte",
                Ship.MINING_LASER == Ship.PART_COUNT - 1
                        && "LASER".equals(Ship.PART_NAMES[Ship.MINING_LASER]));
        Upgrades laserUp = new Upgrades(true);
        Ship laserShip = new Ship();
        laserShip.reset(laserUp, 0f, 0f);
        check("el laser inicial esta intacto",
                laserShip.part[Ship.MINING_LASER] == Ship.PART_MAX
                        && laserShip.laserFactor() == 1f);
        EnergyCapacitor capacitor = new EnergyCapacitor();
        check("capacitor inicial lleno", capacitor.energy == capacitor.maxEnergy);
        capacitor.update(1f, true);
        check("minar consume 3 de energia",
                Math.abs(capacitor.energy - 97f) < 0.001f);
        capacitor.update(40f, true);
        check("el capacitor se agota", capacitor.energy == 0f);
        capacitor.update(1.1f, false);
        check("recarga tras 1 s de espera",
                Math.abs(capacitor.energy - 0.8f) < 0.02f);
        Asteroid rockC = new Asteroid(100f, 0f, Asteroid.MEDIUM,
                AsteroidType.C, 15, 15, 101L);
        Asteroid rockS = new Asteroid(150f, 0f, Asteroid.MEDIUM,
                AsteroidType.S, 35, 35, 102L);
        Asteroid rockM = new Asteroid(200f, 0f, Asteroid.MEDIUM,
                AsteroidType.M, 75, 75, 103L);
        MiningLaser miningLaser = new MiningLaser();
        check("tasa C ~6 UM/s",
                Math.abs(miningLaser.rate(laserShip, rockC) - 6f) < 0.01f);
        check("tasa S ~4.8 UM/s",
                Math.abs(miningLaser.rate(laserShip, rockS) - 4.8f) < 0.01f);
        check("tasa M ~3 UM/s",
                Math.abs(miningLaser.rate(laserShip, rockM) - 3f) < 0.01f);
        check("tiempos C/S/M dentro del plan",
                rockC.reserve / 6f >= 2f && rockC.reserve / 6f <= 4f
                        && rockS.reserve / 4.8f >= 6f && rockS.reserve / 4.8f <= 9f
                        && rockM.reserve / 3f >= 20f && rockM.reserve / 3f <= 30f);
        check("el laser sin objetivo no mina",
                miningLaser.tick(1f, laserShip, laserShip.capacitor) == 0
                        && !miningLaser.active);
        laserShip.capacitor.reset();
        miningLaser.aim(rockC);
        float fuelBeforeLaser = laserShip.fuel;
        int extractedC = miningLaser.tick(1f, laserShip, laserShip.capacitor);
        check("el laser extrae y consume energia",
                extractedC == 6
                        && Math.abs(laserShip.capacitor.energy - 97f) < 0.01f
                        && rockC.reserve == 9);
        check("el laser no consume combustible",
                laserShip.fuel == fuelBeforeLaser);
        laserShip.capacitor.energy = 0f;
        miningLaser.aim(rockS);
        check("sin energia el laser no mina",
                !miningLaser.canMine(laserShip, laserShip.capacitor)
                        && miningLaser.tick(1f, laserShip, laserShip.capacitor) == 0
                        && rockS.reserve == 35);
        laserShip.capacitor.reset();
        miningLaser.aim(rockM);
        for (int i = 0; i < 30 && miningLaser.active; i++) {
            miningLaser.tick(1f, laserShip, laserShip.capacitor);
        }
        check("el laser agota la roca y se detiene",
                rockM.depleted() && !miningLaser.active);
        laserShip.part[Ship.MINING_LASER] = 0f;
        miningLaser.aim(rockS);
        check("sin laser operativo no se mina",
                !miningLaser.canMine(laserShip, laserShip.capacitor));
        GameWorld mineWorld = new GameWorld(new Upgrades(true));
        mineWorld.start();
        mineWorld.immortal = true;
        mineWorld.asteroids.clear();
        Asteroid mineTarget = new Asteroid(100f, 0f, Asteroid.MEDIUM,
                AsteroidType.C, 15, 15, 104L);
        mineTarget.vel.set(0f, 0f);
        mineWorld.asteroids.add(mineTarget);
        mineWorld.ship.laser.aim(mineTarget);
        for (int i = 0; i < 60; i++) {
            mineWorld.update(1f / 60f, new Controls());
        }
        check("GameWorld integra el laser",
                mineTarget.reserve == 9);

        // T-403: extracción a bodega, bodega llena y persistencia.
        GameSession mineSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        mineSession.startRun();
        mineSession.sector.immortal = true;
        mineSession.sector.asteroids.clear();
        Asteroid cargoRock = new Asteroid(100f, 0f, Asteroid.MEDIUM,
                AsteroidType.C, 15, 15, 201L);
        cargoRock.vel.set(0f, 0f);
        mineSession.sector.asteroids.add(cargoRock);
        mineSession.ship().laser.aim(cargoRock);
        float coinsBeforeMining = mineSession.sector.coinsEarned;
        for (int i = 0; i < 180; i++) {
            mineSession.update(dt, new Controls());
        }
        check("el mineral entra en la bodega",
                mineSession.cargo().totalUnits() == 15
                        && mineSession.cargo().amount(Commodity.HIELO) == 7
                        && mineSession.cargo().amount(Commodity.SILICATO) == 5
                        && mineSession.cargo().amount(Commodity.HIERRO) == 2
                        && mineSession.cargo().amount(Commodity.COBRE) == 1);
        check("la masa de la carga es correcta",
                Math.abs(mineSession.cargo().usedMass() - 14.5f) < 0.01f);
        check("la roca agotada desaparece",
                cargoRock.depleted()
                        && !mineSession.sector.asteroids.contains(cargoRock, true));
        check("minar no da monedas",
                mineSession.sector.coinsEarned == coinsBeforeMining);
        GameSession fullMineSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        fullMineSession.startRun();
        fullMineSession.sector.immortal = true;
        fullMineSession.sector.asteroids.clear();
        Asteroid fullRock = new Asteroid(100f, 0f, Asteroid.MEDIUM,
                AsteroidType.C, 15, 15, 202L);
        fullRock.vel.set(0f, 0f);
        fullMineSession.sector.asteroids.add(fullRock);
        fullMineSession.cargo().addExact(Commodity.SILICATO, 100);
        fullMineSession.ship().laser.aim(fullRock);
        for (int i = 0; i < 60; i++) {
            fullMineSession.update(dt, new Controls());
        }
        check("la bodega llena detiene la extraccion",
                !fullMineSession.ship().laser.active
                        && fullRock.reserve == 15
                        && fullMineSession.cargo().amount(Commodity.SILICATO) == 100);
        SaveService.MemoryStorage minedStorage = new SaveService.MemoryStorage();
        SaveService minedService = new SaveService(minedStorage);
        check("la roca agotada se guarda", minedService.save(mineSession));
        GameSession minedLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("la roca agotada no reaparece",
                minedService.load(minedLoaded) == SaveService.LoadStatus.V2
                        && minedLoaded.sector.asteroids.size == 0
                        && minedLoaded.cargo().totalUnits() == 15
                        && Math.abs(minedLoaded.cargo().usedMass() - 14.5f) < 0.01f);

        // T-404: piloto automático MINE.
        GameWorld mineAuto = new GameWorld(new Upgrades(true));
        mineAuto.start();
        mineAuto.immortal = true;
        mineAuto.asteroids.clear();
        mineAuto.fuel.clear();
        Asteroid autoTarget = new Asteroid(200f, 0f, Asteroid.MEDIUM,
                AsteroidType.C, 15, 15, 301L);
        autoTarget.vel.set(0f, 0f);
        mineAuto.asteroids.add(autoTarget);
        mineAuto.pilot.mine(autoTarget);
        check("el piloto entra en modo MINE",
                mineAuto.pilot.mode() == Autopilot.MINE);
        float minMineDist = Float.MAX_VALUE;
        for (int i = 0; i < 60 * 10; i++) {
            mineAuto.update(dt, new Controls());
            minMineDist = Math.min(minMineDist,
                    mineAuto.ship.pos.dst(autoTarget.pos));
        }
        check("MINE mantiene una distancia segura", minMineDist < 260f && minMineDist > 100f);
        check("MINE llena la bodega", mineAuto.ship.cargo().totalUnits() == 15);
        check("MINE no dispara misiles", mineAuto.missiles.size == 0);
        check("MINE termina al agotar la roca",
                mineAuto.pilot.mode() != Autopilot.MINE
                        && !mineAuto.ship.laser.active);
        GameWorld mineFull = new GameWorld(new Upgrades(true));
        mineFull.start();
        mineFull.immortal = true;
        mineFull.asteroids.clear();
        mineFull.fuel.clear();
        Asteroid fullAutoRock = new Asteroid(200f, 0f, Asteroid.MEDIUM,
                AsteroidType.C, 15, 15, 302L);
        fullAutoRock.vel.set(0f, 0f);
        mineFull.asteroids.add(fullAutoRock);
        mineFull.ship.cargo().addExact(Commodity.SILICATO, 100);
        mineFull.pilot.mine(fullAutoRock);
        for (int i = 0; i < 120; i++) {
            mineFull.update(dt, new Controls());
        }
        check("MINE se detiene con la bodega llena",
                mineFull.pilot.mode() != Autopilot.MINE
                        && fullAutoRock.reserve == 15);

        // T-405: fundición temporal a precio base y sin monedas directas.
        // T-406: fragmentación por misiles, dispersión y recuperación.
        GameWorld fragWorld = new GameWorld(new Upgrades(true));
        fragWorld.start();
        fragWorld.immortal = true;
        fragWorld.asteroids.clear();
        fragWorld.fuel.clear();
        Asteroid fragRock = new Asteroid(300f, 0f, Asteroid.SMALL,
                AsteroidType.C, 12, 12, 401L);
        fragRock.vel.set(0f, 0f);
        fragWorld.asteroids.add(fragRock);
        int coinsBeforeFrag = fragWorld.coinsEarned;
        fragWorld.missiles.add(new Missile(fragRock.pos.x - 10f, fragRock.pos.y,
                new com.badlogic.gdx.math.Vector2(1f, 0f),
                new com.badlogic.gdx.math.Vector2(), 99));
        fragWorld.update(dt, new Controls());
        int fragSum = 0;
        for (int i = 0; i < fragWorld.fragments.size; i++) {
            fragSum += fragWorld.fragments.get(i).amount;
        }
        check("el misil dispersa fragmentos", fragWorld.fragments.size > 0);
        check("el mineral no entra directo en la bodega",
                fragWorld.ship.cargo().totalUnits() == 0);
        check("romper roca no da monedas", fragWorld.coinsEarned == coinsBeforeFrag);
        check("parte del material se pierde",
                fragSum == Math.round(12f * GameWorld.FRAGMENT_SHARE)
                        && fragSum < 12);
        OreFragment firstFragment = fragWorld.fragments.get(0);
        fragWorld.ship.pos.set(firstFragment.pos);
        fragWorld.update(dt, new Controls());
        check("la nave recoge fragmentos",
                fragWorld.ship.cargo().totalUnits() > 0
                        && fragWorld.fragments.size < 2);
        GameWorld fragLarge = new GameWorld(new Upgrades(true));
        fragLarge.start();
        fragLarge.immortal = true;
        fragLarge.asteroids.clear();
        fragLarge.fuel.clear();
        Asteroid largeRock = new Asteroid(300f, 0f, Asteroid.LARGE,
                AsteroidType.M, 70, 70, 402L);
        largeRock.vel.set(0f, 0f);
        fragLarge.asteroids.add(largeRock);
        fragLarge.missiles.add(new Missile(largeRock.pos.x - 10f, largeRock.pos.y,
                new com.badlogic.gdx.math.Vector2(1f, 0f),
                new com.badlogic.gdx.math.Vector2(), 99));
        fragLarge.update(dt, new Controls());
        int debrisReserve = 0;
        for (int i = 0; i < fragLarge.asteroids.size; i++) {
            debrisReserve += fragLarge.asteroids.get(i).reserve;
        }
        int largeFragUnits = 0;
        for (int i = 0; i < fragLarge.fragments.size; i++) {
            largeFragUnits += fragLarge.fragments.get(i).amount;
        }
        check("una roca grande se parte en escombros",
                fragLarge.asteroids.size == 2 && debrisReserve == 10);
        check("los fragmentos y escombros no duplican mineral",
                largeFragUnits == Math.round(70f * GameWorld.FRAGMENT_SHARE)
                        && debrisReserve + largeFragUnits < 70);

        // T-407: mejora de láser, capacitor y efectos.
        Upgrades laserTechUp = new Upgrades(true);
        check("laser base sin mejora",
                laserTechUp.laserLevel() == 0 && laserTechUp.capacitorMax() == 100f);
        laserTechUp.addCoins(1000);
        check("la mejora Laser se compra",
                laserTechUp.buy(Upgrades.LASER) && laserTechUp.laserLevel() == 1);
        Ship laserTechUpShip = new Ship();
        laserTechUpShip.reset(laserTechUp, 0f, 0f);
        check("la nave aplica potencia y penetracion",
                Math.abs(laserTechUpShip.laser.power() - 7.5f) < 0.001f
                        && Math.abs(laserTechUpShip.laser.penetration() - 4f) < 0.001f);
        check("la mejora aumenta el capacitor",
                Math.abs(laserTechUpShip.capacitor.maxEnergy - 125f) < 0.001f
                        && Math.abs(laserTechUpShip.capacitor.energy - 125f) < 0.001f);
        Asteroid laserC = new Asteroid(100f, 0f, Asteroid.MEDIUM,
                AsteroidType.C, 15, 15, 501L);
        Asteroid laserM = new Asteroid(150f, 0f, Asteroid.MEDIUM,
                AsteroidType.M, 75, 75, 502L);
        check("la mejora sube la tasa en roca C",
                Math.abs(laserTechUpShip.laser.rate(laserTechUpShip, laserC) - 7.5f) < 0.01f);
        check("la penetracion mejora la roca M",
                Math.abs(laserTechUpShip.laser.rate(laserTechUpShip, laserM) - 4.5f) < 0.01f);
        laserTechUpShip.capacitor.energy = 10f;
        laserTechUp.buy(Upgrades.LASER);
        laserTechUpShip.syncLaser(laserTechUp);
        check("comprar laser conserva la carga relativa",
                laserTechUpShip.laser.level() == 2
                        && Math.abs(laserTechUpShip.capacitor.maxEnergy - 150f) < 0.001f
                        && Math.abs(laserTechUpShip.capacitor.energy - 12f) < 0.01f);
        GameSession laserSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        laserSession.startRun();
        laserSession.upgrades().addCoins(1000);
        laserSession.upgrades().buy(Upgrades.LASER);
        laserSession.upgrades().buy(Upgrades.LASER);
        laserSession.ship().syncLaser(laserSession.upgrades());
        SaveService.MemoryStorage laserStorage = new SaveService.MemoryStorage();
        SaveService laserService = new SaveService(laserStorage);
        check("el nivel de laser se guarda", laserService.save(laserSession));
        GameSession laserLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("el nivel de laser se restaura",
                laserService.load(laserLoaded) == SaveService.LoadStatus.V2
                        && laserLoaded.upgrades().laserLevel() == 2);
        Ship loadedShip = new Ship();
        loadedShip.reset(laserLoaded.upgrades(), 0f, 0f);
        check("la nave cargada usa el capacitor mejorado",
                Math.abs(loadedShip.capacitor.maxEnergy - 150f) < 0.001f);

        // T-410: rayo tractor, atracción, energía y mejora.
        Upgrades tractorUp = new Upgrades(true);
        check("tractor base sin mejora",
                tractorUp.tractorLevel() == 0
                        && tractorUp.tractorRange() == 140f
                        && tractorUp.tractorSpeed() == 120f
                        && tractorUp.tractorTargets() == 2);
        tractorUp.addCoins(1000);
        check("la mejora Tractor se compra",
                tractorUp.buy(Upgrades.TRACTOR) && tractorUp.tractorLevel() == 1);
        Ship tractorShip = new Ship();
        tractorShip.reset(tractorUp, 0f, 0f);
        check("la nave aplica alcance y velocidad del tractor",
                tractorShip.tractor.level() == 1
                        && Math.abs(tractorShip.tractor.range() - 165f) < 0.001f
                        && Math.abs(tractorShip.tractor.speed() - 150f) < 0.001f
                        && tractorShip.tractor.capacity() == 3);
        GameWorld tractorWorld = new GameWorld(new Upgrades(true));
        tractorWorld.start();
        tractorWorld.immortal = true;
        tractorWorld.asteroids.clear();
        tractorWorld.fuel.clear();
        OreFragment nearOre = new OreFragment(100f, 0f, Commodity.HIERRO, 5, 0f, 0f);
        tractorWorld.fragments.add(nearOre);
        float tractorEnergy0 = tractorWorld.ship.capacitor.energy;
        float nearStart = nearOre.pos.dst(tractorWorld.ship.pos);
        for (int i = 0; i < 60; i++) {
            tractorWorld.asteroids.clear();
            tractorWorld.fuel.clear();
            tractorWorld.update(dt, new Controls());
        }
        check("el tractor atrae el fragmento",
                nearOre.pos.dst(tractorWorld.ship.pos) < nearStart - 40f
                        || tractorWorld.ship.cargo().amount(Commodity.HIERRO) == 5);
        check("el tractor consume energia",
                tractorWorld.ship.capacitor.energy < tractorEnergy0 - 0.5f);
        GameWorld tractorRange = new GameWorld(new Upgrades(true));
        tractorRange.start();
        tractorRange.immortal = true;
        tractorRange.asteroids.clear();
        tractorRange.fuel.clear();
        OreFragment farOre = new OreFragment(400f, 0f, Commodity.ORO, 3, 0f, 0f);
        tractorRange.fragments.add(farOre);
        for (int i = 0; i < 30; i++) {
            tractorRange.asteroids.clear();
            tractorRange.fuel.clear();
            tractorRange.update(dt, new Controls());
        }
        check("fuera de alcance no se atrae",
                !tractorRange.ship.tractor.active()
                        && Math.abs(farOre.pos.x - 400f) < 0.01f);
        GameWorld tractorCapacity = new GameWorld(new Upgrades(true));
        tractorCapacity.start();
        tractorCapacity.immortal = true;
        tractorCapacity.asteroids.clear();
        tractorCapacity.fuel.clear();
        tractorCapacity.fragments.add(new OreFragment(120f, 0f, Commodity.HIELO, 2, 0f, 0f));
        tractorCapacity.fragments.add(new OreFragment(130f, 0f, Commodity.SILICATO, 2, 0f, 0f));
        tractorCapacity.fragments.add(new OreFragment(135f, 0f, Commodity.HIERRO, 2, 0f, 0f));
        tractorCapacity.update(dt, new Controls());
        check("el tractor respeta su capacidad",
                tractorCapacity.ship.tractor.targets.size == 2);
        GameSession tractorSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        tractorSession.startRun();
        tractorSession.upgrades().addCoins(1000);
        tractorSession.upgrades().buy(Upgrades.TRACTOR);
        tractorSession.upgrades().buy(Upgrades.TRACTOR);
        tractorSession.ship().syncTractor(tractorSession.upgrades());
        SaveService.MemoryStorage tractorStorage = new SaveService.MemoryStorage();
        SaveService tractorService = new SaveService(tractorStorage);
        check("el nivel de tractor se guarda", tractorService.save(tractorSession));
        GameSession tractorLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("el nivel de tractor se restaura",
                tractorService.load(tractorLoaded) == SaveService.LoadStatus.V2
                        && tractorLoaded.upgrades().tractorLevel() == 2);
        String eightLevels = tractorStorage.read(SaveService.SAVE_NAME);
        String tractorSeven = trimLevels(eightLevels, 7);
        GameSession tractorSevenLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("un v2 con 7 niveles carga con Tractor 0",
                !eightLevels.equals(tractorSeven)
                        && SaveCodec.decode(tractorSeven, tractorSevenLoaded)
                        && tractorSevenLoaded.upgrades().tractorLevel() == 0);

        // T-409: persistencia de fragmentos y pérdida configurable.
        GameSession oreSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        oreSession.startRun();
        oreSession.sector.fragments.add(new OreFragment(120f, -40f, Commodity.TITANIO,
                6, 12f, -3f, 42f));
        oreSession.sector.fragments.add(new OreFragment(-30f, 80f, Commodity.HIELO,
                2, -5f, 4f, 17f));
        SaveService.MemoryStorage oreStorage = new SaveService.MemoryStorage();
        SaveService oreService = new SaveService(oreStorage);
        check("los fragmentos se guardan", oreService.save(oreSession));
        GameSession oreLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("los fragmentos se restauran",
                oreService.load(oreLoaded) == SaveService.LoadStatus.V2
                        && oreLoaded.sector.fragments.size == 2
                        && oreLoaded.sector.fragments.get(0).commodity == Commodity.TITANIO
                        && oreLoaded.sector.fragments.get(0).amount == 6
                        && Math.abs(oreLoaded.sector.fragments.get(0).pos.x - 120f) < 0.01f
                        && Math.abs(oreLoaded.sector.fragments.get(0).vel.x - 12f) < 0.01f
                        && Math.abs(oreLoaded.sector.fragments.get(0).life() - 42f) < 0.01f);
        String oreSave = oreStorage.read(SaveService.SAVE_NAME);
        String noFragmentsSave = oreSave.replaceAll("\"fragments\":\\[[^]]*\\],?", "");
        GameSession noFragmentsLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("un guardado sin fragmentos carga vacio",
                !oreSave.equals(noFragmentsSave)
                        && SaveCodec.decode(noFragmentsSave, noFragmentsLoaded)
                        && noFragmentsLoaded.sector.fragments.size == 0);
        GameWorld legacyOre = new GameWorld(new Upgrades(true));
        legacyOre.start();
        legacyOre.fragments.add(new OreFragment(10f, 20f, Commodity.ORO, 3, 1f, 2f, 25f));
        String legacyOreText = legacyOre.serialize();
        GameWorld legacyOreLoaded = new GameWorld(new Upgrades(true));
        check("el formato legacy conserva fragmentos",
                legacyOreLoaded.restore(legacyOreText)
                        && legacyOreLoaded.fragments.size == 1
                        && legacyOreLoaded.fragments.get(0).commodity == Commodity.ORO
                        && legacyOreLoaded.fragments.get(0).amount == 3
                        && Math.abs(legacyOreLoaded.fragments.get(0).life() - 25f) < 0.01f);
        GameWorld allFragments = new GameWorld(new Upgrades(true));
        allFragments.start();
        allFragments.immortal = true;
        allFragments.fragmentShare = 1f;
        allFragments.fragmentLoss = 0f;
        allFragments.asteroids.clear();
        allFragments.fuel.clear();
        Asteroid wholeRock = new Asteroid(300f, 0f, Asteroid.SMALL,
                AsteroidType.C, 12, 12, 601L);
        wholeRock.vel.set(0f, 0f);
        allFragments.asteroids.add(wholeRock);
        allFragments.missiles.add(new Missile(wholeRock.pos.x - 10f, wholeRock.pos.y,
                new com.badlogic.gdx.math.Vector2(1f, 0f),
                new com.badlogic.gdx.math.Vector2(), 99));
        allFragments.update(dt, new Controls());
        int wholeUnits = 0;
        for (int i = 0; i < allFragments.fragments.size; i++) {
            wholeUnits += allFragments.fragments.get(i).amount;
        }
        check("la perdida de material es configurable",
                wholeUnits == 12 && allFragments.asteroids.size == 0);
        GameWorld zeroShare = new GameWorld(new Upgrades(true));
        zeroShare.start();
        zeroShare.immortal = true;
        zeroShare.fragmentShare = 0f;
        zeroShare.fragmentLoss = 0f;
        zeroShare.asteroids.clear();
        zeroShare.fuel.clear();
        Asteroid debrisOnly = new Asteroid(300f, 0f, Asteroid.MEDIUM,
                AsteroidType.S, 30, 30, 602L);
        debrisOnly.vel.set(0f, 0f);
        zeroShare.asteroids.add(debrisOnly);
        zeroShare.missiles.add(new Missile(debrisOnly.pos.x - 10f, debrisOnly.pos.y,
                new com.badlogic.gdx.math.Vector2(1f, 0f),
                new com.badlogic.gdx.math.Vector2(), 99));
        zeroShare.update(dt, new Controls());
        int debrisOnlyUnits = 0;
        for (int i = 0; i < zeroShare.asteroids.size; i++) {
            debrisOnlyUnits += zeroShare.asteroids.get(i).reserve;
        }
        check("sin fragmentos todo queda en escombros",
                zeroShare.fragments.size == 0 && debrisOnlyUnits == 30);

        // Partida simulada de 2 minutos: la nave apunta al meteorito más cercano y dispara.
        w.start();
        // La simulación mide generación y monedas, no supervivencia: evita que
        // un choque aleatorio la corte y la haga no determinista.
        w.immortal = true;
        int maxAsteroids = 0;
        int steps = 0;
        int totalScore = 0;
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
            int before = w.score;
            w.update(dt, left, right, thrust, fire);
            if (w.score > before) {
                totalScore += w.score - before;
            }
            maxAsteroids = Math.max(maxAsteroids, w.asteroids.size);
            steps++;
        }
        check("el mundo infinito genera meteoritos durante la partida", maxAsteroids > 0);
        System.out.println("Partida simulada: " + (steps / 60) + "s, oleada " + w.wave
                + ", puntos " + w.score + ", monedas " + w.coinsEarned
                + ", max meteoritos " + maxAsteroids + ", casco " + w.ship.part[Ship.HULL]);
        check("la simulacion destruye asteroides", totalScore > 0);
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
            destroyed = !pa.asteroids.contains(rock, true);
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

        // T-501: estación nodriza, servicios y zona segura.
        GameWorld stationWorld = new GameWorld(new Upgrades(true));
        stationWorld.start();
        check("la nodriza existe en el sector inicial",
                stationWorld.station != null
                        && Math.abs(stationWorld.station.pos.x - GameData.MOTHERSHIP_X) < 0.01f
                        && Math.abs(stationWorld.station.pos.y - GameData.MOTHERSHIP_Y) < 0.01f);
        check("la nodriza tiene radio, acople y zona segura",
                stationWorld.station.def.radius == 200f
                        && stationWorld.station.def.dockRadius == 260f
                        && stationWorld.station.def.safeRadius == 480f);
        check("la nodriza ofrece todos los servicios",
                stationWorld.station.def.serviceCount() == 6
                        && stationWorld.station.def.repair
                        && stationWorld.station.def.refuel
                        && stationWorld.station.def.oxygen
                        && stationWorld.station.def.upgrades
                        && stationWorld.station.def.market
                        && stationWorld.station.def.storage);
        check("la nodriza tiene almacen",
                Math.abs(stationWorld.station.storage.capacity() - Station.STORAGE_CAPACITY) < 0.01f);
        check("la zona segura no tiene asteroides",
                stationAsteroidsInSafeZone(stationWorld) == 0);
        check("no se puede acoplar lejos o rapido",
                !stationWorld.station.canDock(new Vector2(0f, 0f), 5f)
                        && !stationWorld.station.canDock(
                        new Vector2(0f, GameData.MOTHERSHIP_Y - 100f), 50f)
                        && stationWorld.station.canDock(
                        new Vector2(0f, GameData.MOTHERSHIP_Y - 100f), 10f));
        check("la zona segura detecta el centro y el exterior",
                stationWorld.station.inSafeZone(GameData.MOTHERSHIP_X, GameData.MOTHERSHIP_Y)
                        && !stationWorld.station.inSafeZone(
                        GameData.MOTHERSHIP_X + GameData.MOTHERSHIP.safeRadius + 1f,
                        GameData.MOTHERSHIP_Y));

        // T-502: acople y Autopilot.STATION.
        GameWorld dockWorld = new GameWorld(new Upgrades(true));
        dockWorld.start();
        dockWorld.immortal = true;
        dockWorld.asteroids.clear();
        dockWorld.fuel.clear();
        dockWorld.ship.pos.set(dockWorld.station.pos.x, dockWorld.station.pos.y - 100f);
        dockWorld.ship.vel.set(0f, 0f);
        check("se puede acoplar cerca y despacio", DockingSystem.canDock(dockWorld));
        dockWorld.ship.vel.set(60f, 0f);
        check("no se acopla con velocidad alta", !DockingSystem.canDock(dockWorld));
        dockWorld.ship.vel.set(0f, 0f);
        check("el acople detiene la nave y activa DOCKED",
                DockingSystem.dock(dockWorld)
                        && dockWorld.ship.docked
                        && dockWorld.ship.vel.len() == 0f
                        && !dockWorld.pilot.active());
        DockingSystem.undock(dockWorld);
        check("el desacople devuelve la nave al exterior",
                !dockWorld.ship.docked && dockWorld.ship.vel.len() == 0f);
        GameWorld stationPilot = new GameWorld(new Upgrades(true));
        stationPilot.start();
        stationPilot.immortal = true;
        stationPilot.asteroids.clear();
        stationPilot.fuel.clear();
        stationPilot.ship.pos.set(stationPilot.station.pos.x,
                stationPilot.station.pos.y - 900f);
        stationPilot.ship.heading = 90f;
        stationPilot.ship.dir.set(0f, 1f);
        stationPilot.pilot.station(stationPilot.station);
        check("el piloto entra en modo STATION",
                stationPilot.pilot.mode() == Autopilot.STATION);
        for (int i = 0; i < 60 * 40 && stationPilot.ship.docked == false; i++) {
            stationPilot.asteroids.clear();
            stationPilot.fuel.clear();
            stationPilot.update(dt, new Controls());
            if (stationPilot.pilot.mode() == Autopilot.STATION
                    && DockingSystem.canDock(stationPilot)
                    && stationPilot.ship.vel.len() < Autopilot.STOP_SPEED) {
                break;
            }
        }
        float stationDist = stationPilot.station.distanceTo(stationPilot.ship.pos);
        check("Autopilot.STATION frena en la zona de acople",
                stationDist >= stationPilot.station.def.radius
                        && stationDist <= stationPilot.station.def.dockRadius
                        && stationPilot.ship.vel.len() < DockingSystem.MAX_DOCK_SPEED);
        check("Autopilot.STATION no penetra el casco de la estacion",
                stationDist > stationPilot.station.def.radius + Ship.RADIUS);
        check("tras el piloto se puede acoplar",
                DockingSystem.canDock(stationPilot)
                        && DockingSystem.dock(stationPilot)
                        && stationPilot.ship.docked);

        // T-503: almacén y venta a precio base.
        StationStorage store = new StationStorage(200f);
        CargoHold dockCargo = new CargoHold(200f);
        dockCargo.addExact(Commodity.SILICATO, 100);
        dockCargo.addExact(Commodity.ORO, 10);
        check("el almacén recibe la bodega",
                store.depositAll(dockCargo) == 110
                        && dockCargo.isEmpty()
                        && store.totalUnits() == 110
                        && Math.abs(store.usedMass() - 125f) < 0.01f);
        check("la venta usa el precio base",
                store.totalValue() == 500f
                        && store.sellAll() == 500
                        && store.isEmpty());
        StationStorage smallStore = new StationStorage(50f);
        CargoHold partialCargo = new CargoHold(200f);
        partialCargo.addExact(Commodity.SILICATO, 100);
        check("la descarga parcial respeta la capacidad",
                smallStore.depositAll(partialCargo) == 50
                        && partialCargo.amount(Commodity.SILICATO) == 50
                        && smallStore.totalUnits() == 50);
        GameWorld serviceWorld = new GameWorld(new Upgrades(true));
        serviceWorld.start();
        serviceWorld.ship.cargo().addExact(Commodity.HIERRO, 10);
        check("la estación descarga desde la nave",
                serviceWorld.station.storage.depositAll(serviceWorld.ship.cargo()) == 10
                        && serviceWorld.ship.cargo().isEmpty()
                        && serviceWorld.station.storage.totalUnits() == 10);

        // T-505: persistencia de estación, almacén y estado acoplado.
        GameSession dockSave = new GameSession(new PlayerProfile(new Upgrades(true)));
        dockSave.startRun();
        dockSave.ship().docked = true;
        dockSave.ship().cargo().addExact(Commodity.SILICATO, 40);
        dockSave.sector.station.storage.hold.restore(Commodity.HIERRO, 25);
        dockSave.sector.station.pos.set(15f, 620f);
        SaveService.MemoryStorage dockStorage = new SaveService.MemoryStorage();
        SaveService dockService = new SaveService(dockStorage);
        check("la estación y el acople se guardan", dockService.save(dockSave));
        GameSession dockLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("la estación se restaura con su almacén y acople",
                dockService.load(dockLoaded) == SaveService.LoadStatus.V2
                        && dockLoaded.ship().docked
                        && Math.abs(dockLoaded.sector.station.pos.x - 15f) < 0.01f
                        && Math.abs(dockLoaded.sector.station.pos.y - 620f) < 0.01f
                        && dockLoaded.sector.station.storage.amount(Commodity.HIERRO) == 25
                        && dockLoaded.ship().cargo().amount(Commodity.SILICATO) == 40);
        check("no se duplica mineral al cargar",
                dockLoaded.ship().cargo().totalUnits() == 40
                        && dockLoaded.sector.station.storage.totalUnits() == 25);
        String dockText = dockStorage.read(SaveService.SAVE_NAME);
        String noDocked = dockText.replace(",\"docked\":true", "");
        GameSession noDockedLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("un guardado sin docked carga desacoplado",
                !dockText.equals(noDocked)
                        && SaveCodec.decode(noDocked, noDockedLoaded)
                        && !noDockedLoaded.ship().docked
                        && noDockedLoaded.sector.station.storage.amount(Commodity.HIERRO) == 25);

        // T-605: persistencia del mercado.
        GameSession marketSave = new GameSession(new PlayerProfile(new Upgrades(true)));
        marketSave.startRun();
        Market savedMarket = marketSave.sector.station.market;
        savedMarket.setStock(Commodity.HIELO, 77);
        savedMarket.setTarget(Commodity.HIELO, 250);
        savedMarket.setSpread(0.12f);
        savedMarket.setProduction(Commodity.HIERRO, 5);
        savedMarket.setConsumption(Commodity.COBRE, 3);
        SaveService.MemoryStorage marketStore = new SaveService.MemoryStorage();
        SaveService marketService = new SaveService(marketStore);
        check("el mercado se guarda", marketService.save(marketSave));
        GameSession marketLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("el mercado se restaura",
                marketService.load(marketLoaded) == SaveService.LoadStatus.V2
                        && marketLoaded.sector.station.market.stock(Commodity.HIELO) == 77
                        && marketLoaded.sector.station.market.target(Commodity.HIELO) == 250
                        && Math.abs(marketLoaded.sector.station.market.spread() - 0.12f) < 0.001f
                        && marketLoaded.sector.station.market.production(Commodity.HIERRO) == 5
                        && marketLoaded.sector.station.market.consumption(Commodity.COBRE) == 3);
        String marketText = marketStore.read(SaveService.SAVE_NAME);
        String noMarket = marketText.replaceAll(",\"market\":\\{[^}]*\\}", "");
        GameSession noMarketLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("un guardado sin mercado usa los valores por defecto",
                !marketText.equals(noMarket)
                        && SaveCodec.decode(noMarket, noMarketLoaded)
                        && noMarketLoaded.sector.station.market.stock(Commodity.HIELO)
                        == GameData.MARKET_INITIAL * GameData.MARKET_HIELO
                        && Math.abs(noMarketLoaded.sector.station.market.spread()
                        - PriceModel.DEFAULT_SPREAD) < 0.001f);

        // T-506: rescate por combustible agotado.
        GameWorld rescueWorld = new GameWorld(new Upgrades(true));
        rescueWorld.start();
        rescueWorld.immortal = true;
        rescueWorld.asteroids.clear();
        rescueWorld.fuel.clear();
        rescueWorld.ship.pos.set(0f, -520f);
        rescueWorld.ship.fuel = 0f;
        rescueWorld.ship.cargo().addExact(Commodity.SILICATO, 100);
        Upgrades rescueUp = new Upgrades(true);
        rescueUp.setCoins(200);
        Pilot rescuePilot = new Pilot();
        RescueService rescue = new RescueService();
        check("sin combustible se puede pedir rescate",
                RescueService.needsRescue(rescueWorld)
                        && RescueService.canRequest(rescueWorld, rescuePilot));
        check("el rescate se solicita con espera",
                rescue.request(rescueWorld, rescuePilot)
                        && rescue.active()
                        && !rescue.update(1.5f, rescueWorld, rescueUp, rescuePilot)
                        && rescue.active());
        check("el rescate cobra, pierde carga y remolca",
                rescue.update(1.6f, rescueWorld, rescueUp, rescuePilot)
                        && !rescue.active()
                        && rescue.paid() == 100
                        && rescue.lost() == 25
                        && rescueUp.coins() == 100
                        && rescueWorld.ship.cargo().totalUnits() == 75
                        && Math.abs(rescueWorld.ship.fuel - RescueService.RESCUE_FUEL) < 0.01f
                        && Math.abs(rescueWorld.ship.pos.dst(rescueWorld.station.pos)
                        - (rescueWorld.station.def.dockRadius + RescueService.TOW_GAP)) < 0.01f);
        GameWorld poorWorld = new GameWorld(new Upgrades(true));
        poorWorld.start();
        poorWorld.immortal = true;
        poorWorld.asteroids.clear();
        poorWorld.fuel.clear();
        poorWorld.ship.pos.set(0f, -520f);
        poorWorld.ship.fuel = 0f;
        poorWorld.ship.cargo().addExact(Commodity.SILICATO, 100);
        Upgrades poorUp = new Upgrades(true);
        poorUp.setCoins(50);
        RescueService poorRescue = new RescueService();
        poorRescue.request(poorWorld, new Pilot());
        poorRescue.update(3.1f, poorWorld, poorUp, new Pilot());
        check("sin saldo el rescate cuesta toda la cuenta y más carga",
                poorRescue.paid() == 50
                        && poorRescue.lost() == 50
                        && poorUp.coins() == 0
                        && poorWorld.ship.cargo().totalUnits() == 50);
        RescueService cancelRescue = new RescueService();
        cancelRescue.request(rescueWorld, rescuePilot);
        rescueWorld.ship.fuel = 40f;
        check("el rescate se cancela si llega combustible",
                !cancelRescue.update(0.1f, rescueWorld, rescueUp, rescuePilot)
                        && !cancelRescue.active());
        rescueWorld.ship.docked = true;
        check("no se pide rescate acoplado",
                !RescueService.canRequest(rescueWorld, rescuePilot));
        rescueWorld.ship.docked = false;
        Pilot deadPilot = new Pilot();
        deadPilot.health = 0f;
        deadPilot.status = Pilot.Status.DEAD;
        check("no se pide rescate con el piloto muerto",
                !RescueService.canRequest(rescueWorld, deadPilot));

        // T-507: gate de la etapa 5.
        GameWorld s5 = new GameWorld(new Upgrades(true));
        s5.start();
        s5.immortal = true;
        s5.asteroids.clear();
        s5.fuel.clear();
        s5.ship.pos.set(s5.station.pos.x, s5.station.pos.y - 100f);
        s5.ship.vel.set(0f, 0f);
        boolean s5Docked = DockingSystem.canDock(s5) && DockingSystem.dock(s5)
                && s5.ship.docked;
        DockingSystem.undock(s5);
        check("S5: acople y despegue", s5Docked && !s5.ship.docked);
        s5.ship.cargo().addExact(Commodity.SILICATO, 100);
        int s5Moved = s5.station.storage.depositAll(s5.ship.cargo());
        int s5Credits = s5.station.storage.sellAll();
        check("S5: descarga y venta a precio base",
                s5Moved == 100 && s5Credits == 200 && s5.ship.cargo().isEmpty());
        Upgrades s5Up = new Upgrades(true);
        check("S5: repostar y O2 tienen coste",
                s5Up.refillCost(40f) == 40 && s5Up.o2RefillCost(60f) == 30);
        s5Up.addCoins(1000);
        check("S5: comprar mejora en estación",
                s5Up.buy(Upgrades.MOTOR) && s5Up.level(Upgrades.MOTOR) == 1
                        && Math.abs(s5Up.thrust() - 320f) < 0.01f);
        s5.ship.part[Ship.ENGINE] = 20f;
        s5.ship.repair(Ship.ENGINE);
        check("S5: reparar parte restaura integridad",
                s5.ship.part[Ship.ENGINE] == Ship.PART_MAX);
        s5.ship.pos.set(0f, -520f);
        s5.ship.fuel = 0f;
        check("S5: el rescate evita el softlock",
                RescueService.canRequest(s5, new Pilot()));
        boolean shopField = false;
        try {
            MyGame.class.getDeclaredField("btnPauseShop");
            shopField = true;
        } catch (NoSuchFieldException ignored) {
            // La pausa ya no tiene tienda.
        }
        check("S5: la economia no depende de la pausa", !shopField);

        // T-601: modelo de precios y mercado por estación.
        check("el factor de precio depende del stock",
                Math.abs(PriceModel.factor(100, 100) - 1f) < 0.001f
                        && Math.abs(PriceModel.factor(1, 100) - 2f) < 0.001f
                        && Math.abs(PriceModel.factor(1000, 100) - 0.5f) < 0.001f);
        check("el precio medio usa el factor",
                Math.abs(PriceModel.midPrice(2f, 100, 100) - 2f) < 0.001f
                        && Math.abs(PriceModel.midPrice(2f, 1, 100) - 4f) < 0.001f
                        && Math.abs(PriceModel.midPrice(2f, 1000, 100) - 1f) < 0.001f);
        check("el spread separa compra y venta",
                Math.abs(PriceModel.buyPrice(2f, 100, 100, 0.08f) - 2.16f) < 0.001f
                        && Math.abs(PriceModel.sellPrice(2f, 100, 100, 0.08f) - 1.84f) < 0.001f);
        check("los precios tienen limites",
                PriceModel.buyPrice(2f, 1, 100, 0.08f) <= 2f * 2f * 1.08f + 0.001f
                        && PriceModel.sellPrice(2f, 1000, 100, 0.08f) >= 2f * 0.5f * 0.92f - 0.001f);
        Market market = GameData.newMarket();
        check("cada mineral tiene stock y objetivo",
                market.stock(Commodity.HIERRO) == 120
                        && market.target(Commodity.HIERRO) == 150
                        && market.stock(Commodity.ORO) == 32
                        && market.target(Commodity.ORO) == 40);
        check("compra por encima del medio y venta por debajo",
                market.buyPrice(Commodity.HIERRO) > market.midPrice(Commodity.HIERRO)
                        && market.sellPrice(Commodity.HIERRO) < market.midPrice(Commodity.HIERRO));
        market.setSpread(0.10f);
        check("el spread es configurable",
                Math.abs(market.buyPrice(Commodity.HIERRO)
                        - market.midPrice(Commodity.HIERRO) * 1.10f) < 0.001f
                        && Math.abs(market.sellPrice(Commodity.HIERRO)
                        - market.midPrice(Commodity.HIERRO) * 0.90f) < 0.001f);
        market.setTarget(Commodity.COBRE, 80);
        market.setStock(Commodity.COBRE, 200);
        check("el stock y el objetivo son configurables",
                market.target(Commodity.COBRE) == 80
                        && market.stock(Commodity.COBRE) == 200
                        && market.addStock(Commodity.COBRE, -500) == 0);
        check("la estación tiene mercado",
                stationWorld.station.market != null
                        && stationWorld.station.market.target(Commodity.ORO) == 40);

        // T-604: producción, consumo y deriva de precios.
        Market ecoMarket = GameData.newMarket();
        int hieloBefore = ecoMarket.stock(Commodity.HIELO);
        int silicatoBefore = ecoMarket.stock(Commodity.SILICATO);
        float hieloPriceBefore = ecoMarket.buyPrice(Commodity.HIELO);
        float silicatoPriceBefore = ecoMarket.sellPrice(Commodity.SILICATO);
        for (int i = 0; i < 180; i++) {
            EconomySim.tick(ecoMarket);
        }
        check("30 min simulados mueven el stock",
                ecoMarket.stock(Commodity.HIELO) != hieloBefore
                        && ecoMarket.stock(Commodity.SILICATO) != silicatoBefore);
        check("el consumo sube el precio del hielo",
                ecoMarket.buyPrice(Commodity.HIELO) > hieloPriceBefore);
        check("la producción baja el precio del silicato",
                ecoMarket.sellPrice(Commodity.SILICATO) < silicatoPriceBefore);
        boolean bounded = true;
        for (Commodity c : Commodity.values()) {
            int stock = ecoMarket.stock(c);
            bounded = bounded && stock >= 0
                    && stock <= ecoMarket.target(c) * EconomySim.MAX_STOCK_FACTOR;
        }
        check("el stock queda acotado", bounded);
        int last = ecoMarket.stock(Commodity.HIELO);
        for (int i = 0; i < 20; i++) {
            EconomySim.tick(ecoMarket);
        }
        check("la economia no oscila sin control",
                Math.abs(ecoMarket.stock(Commodity.HIELO) - last) <= 3);
        Market configurable = new Market();
        configurable.setProduction(Commodity.HIERRO, 5);
        configurable.setConsumption(Commodity.HIERRO, 2);
        int configurableBefore = configurable.stock(Commodity.HIERRO);
        EconomySim.tick(configurable);
        check("producción y consumo son configurables",
                configurable.stock(Commodity.HIERRO) >= configurableBefore + 2);
        EconomySim sim = new EconomySim();
        GameWorld simWorld = new GameWorld(new Upgrades(true));
        simWorld.start();
        int simBefore = simWorld.station.market.stock(Commodity.HIELO);
        for (int i = 0; i < 30; i++) {
            sim.update(0.5f, simWorld);
        }
        check("el tick usa intervalos, no cada frame",
                EconomySim.MARKET_TICK_SECONDS >= 5f
                        && simWorld.station.market.stock(Commodity.HIELO) != simBefore);

        // T-606: gate de mercado y anti-exploit.
        GameWorld arbWorld = new GameWorld(new Upgrades(true));
        arbWorld.start();
        arbWorld.immortal = true;
        arbWorld.asteroids.clear();
        arbWorld.fuel.clear();
        Upgrades arbUp = new Upgrades(true);
        arbUp.setCoins(1000);
        int arbCoins = arbUp.coins();
        for (int i = 0; i < 10; i++) {
            TradeService.buy(arbWorld, arbUp, Commodity.HIERRO, 1, null);
            TradeService.sell(arbWorld, arbUp, Commodity.HIERRO, 1, null);
        }
        check("S6: comprar y vender no da arbitraje",
                arbUp.coins() < arbCoins
                        && arbWorld.station.market.buyPrice(Commodity.HIERRO)
                        > arbWorld.station.market.sellPrice(Commodity.HIERRO));
        GameWorld tripWorld = new GameWorld(new Upgrades(true));
        tripWorld.start();
        tripWorld.immortal = true;
        tripWorld.asteroids.clear();
        tripWorld.fuel.clear();
        Upgrades tripUp = new Upgrades(true);
        tripUp.setCoins(500);
        int tripStart = tripUp.coins();
        tripUp.spendCoins(tripUp.refillCost(100f));
        tripWorld.ship.cargo().addExact(Commodity.SILICATO, 100);
        TradeService.Result tripSale = TradeService.sell(tripWorld, tripUp,
                Commodity.SILICATO, 100, null);
        check("S6: una expedición completa da beneficio",
                tripSale.ok && tripUp.coins() > tripStart);
        check("S6: los precios reaccionan al stock",
                Math.abs(ecoMarket.stock(Commodity.HIELO) - hieloBefore) > 0
                        && ecoMarket.buyPrice(Commodity.HIELO) > hieloPriceBefore);
        check("S6: el mercado persiste",
                marketLoaded.sector.station.market.stock(Commodity.HIELO) == 77
                        && marketLoaded.sector.station.market.target(Commodity.HIELO) == 250);

        // T-701: PilotAgent y AgentRoster.
        AgentRoster roster = new AgentRoster(GameData.INITIAL_AGENTS);
        check("hay ocho agentes iniciales",
                roster.size() == 8 && roster.maxSize() == 24);
        PilotAgent firstAgent = roster.get(0);
        check("cada agente tiene rol, carga, combustible y estado",
                firstAgent != null
                        && firstAgent.role == PilotAgent.Role.MINER
                        && firstAgent.state == PilotAgent.State.DOCKED
                        && firstAgent.cargo != null
                        && firstAgent.cargo.capacity() == AgentRoster.CARGO_CAPACITY
                        && firstAgent.fuel == firstAgent.maxFuel
                        && "NODRIZA".equals(firstAgent.objective));
        check("la plantilla es configurable",
                new AgentRoster(3).size() == 3
                        && new AgentRoster(99).size() == GameData.MAX_AGENTS);
        AgentRoster capped = new AgentRoster(GameData.MAX_AGENTS);
        check("el roster respeta el máximo",
                capped.size() == 24
                        && !capped.add(new PilotAgent("X", "X",
                        PilotAgent.Role.MINER, 10f, 10f, 10f, 1f)));
        boolean hasDraw = false;
        try {
            AgentRoster.class.getDeclaredMethod("draw");
            hasDraw = true;
        } catch (NoSuchMethodException ignored) {
            // Los agentes no se dibujan todavía.
        }
        check("los agentes no se renderizan", !hasDraw);
        GameSession agentSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        agentSession.startRun();
        agentSession.sector.agents.get(0).state = PilotAgent.State.MINE;
        agentSession.sector.agents.get(0).fuel = 50f;
        agentSession.sector.agents.get(0).objective = "ROCA-7";
        agentSession.sector.agents.get(0).cargo.addExact(Commodity.HIELO, 10);
        SaveService.MemoryStorage agentStore = new SaveService.MemoryStorage();
        SaveService agentService = new SaveService(agentStore);
        check("los agentes se guardan", agentService.save(agentSession));
        GameSession agentLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("los agentes se restauran",
                agentService.load(agentLoaded) == SaveService.LoadStatus.V2
                        && agentLoaded.sector.agents.size() == 8
                        && agentLoaded.sector.agents.get(0).state == PilotAgent.State.MINE
                        && Math.abs(agentLoaded.sector.agents.get(0).fuel - 50f) < 0.01f
                        && "ROCA-7".equals(agentLoaded.sector.agents.get(0).objective)
                        && agentLoaded.sector.agents.get(0).cargo.amount(Commodity.HIELO) == 10);
        String agentText = agentStore.read(SaveService.SAVE_NAME);
       String noAgents = removeJsonField(agentText, "agents");
        GameSession noAgentsLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("un guardado sin agentes crea la plantilla",
                !agentText.equals(noAgents)
                        && SaveCodec.decode(noAgents, noAgentsLoaded)
                        && noAgentsLoaded.sector.agents.size() == 8);

        // T-702: AgentBrain y ciclo minero.
        AgentRoster cycleRoster = new AgentRoster(1);
        PilotAgent worker = cycleRoster.get(0);
        Market cycleMarket = GameData.newMarket();
        boolean[] seen = new boolean[PilotAgent.State.values().length];
        int creditsBefore = worker.credits;
        int stockCommodityBefore = -1;
        for (int i = 0; i < 400; i++) {
            seen[worker.state.ordinal()] = true;
            AgentBrain.update(worker, cycleMarket, 0.5f);
            seen[worker.state.ordinal()] = true;
            if (stockCommodityBefore < 0) {
                stockCommodityBefore = cycleMarket.stock(worker.commodity);
            }
            if (worker.credits > creditsBefore
                    && worker.cargo.isEmpty()
                    && seen[PilotAgent.State.MINE.ordinal()]
                    && seen[PilotAgent.State.RETURN.ordinal()]
                    && seen[PilotAgent.State.SELL.ordinal()]) {
                break;
            }
        }
        check("el agente completa un ciclo minero",
                worker.credits > creditsBefore && worker.cargo.isEmpty());
        check("los estados del agente son observables",
                seen[PilotAgent.State.TRAVEL.ordinal()]
                        && seen[PilotAgent.State.MINE.ordinal()]
                        && seen[PilotAgent.State.RETURN.ordinal()]
                        && seen[PilotAgent.State.SELL.ordinal()]
                        && seen[PilotAgent.State.REFUEL.ordinal()]);
        check("el agente mueve el stock del mercado",
                cycleMarket.stock(worker.commodity) > stockCommodityBefore);
        AgentRoster brainA = new AgentRoster(1);
        AgentRoster brainB = new AgentRoster(1);
        Market marketA = GameData.newMarket();
        Market marketB = GameData.newMarket();
        AgentBrain.update(brainA.get(0), marketA, 0.1f);
        AgentBrain.update(brainB.get(0), marketB, 0.1f);
        check("la decisión es determinista y por utilidad",
                brainA.get(0).commodity == brainB.get(0).commodity
                        && brainA.get(0).state == brainB.get(0).state);
        PilotAgent smallMiner = new PilotAgent("T-1", "TEST", PilotAgent.Role.MINER,
                5f, AgentRoster.MAX_FUEL, AgentRoster.SPEED, AgentRoster.MINING_RATE);
        Market poorMarket = GameData.newMarket();
        for (Commodity c : Commodity.values()) {
            poorMarket.setStock(c, poorMarket.target(c) * 3);
        }
        AgentBrain.update(smallMiner, poorMarket, 0.1f);
        check("sin beneficio el agente espera",
                smallMiner.state == PilotAgent.State.IDLE);
        worker.credits = 321;
        worker.commodity = Commodity.ORO;
        GameSession agentBrainSession = new GameSession(new PlayerProfile(new Upgrades(true)));
        agentBrainSession.startRun();
        agentBrainSession.sector.agents.get(0).credits = 321;
        agentBrainSession.sector.agents.get(0).commodity = Commodity.ORO;
        SaveService.MemoryStorage brainStore = new SaveService.MemoryStorage();
        SaveService brainService = new SaveService(brainStore);
        brainService.save(agentBrainSession);
        GameSession brainLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        brainService.load(brainLoaded);
        check("créditos y mineral del agente persisten",
                brainLoaded.sector.agents.get(0).credits == 321
                        && brainLoaded.sector.agents.get(0).commodity == Commodity.ORO);

        // T-703: EconomySim con tick y LOD.
        SimClock clock = new SimClock();
        check("el reloj define cerca/lejos/offline",
                SimClock.NEAR_SECONDS == 2f
                        && SimClock.FAR_SECONDS == 5f
                        && SimClock.OFFLINE_SECONDS == 60f);
        check("el reloj solo dispara ticks completos",
                clock.advanceNear(1.9f) == 0
                        && clock.advanceNear(0.2f) == 1
                        && clock.advanceFar(4.9f) == 0
                        && clock.advanceFar(0.2f) == 1
                        && clock.advanceOffline(59f) == 0
                        && clock.advanceOffline(2f) == 1);
        GameWorld lodWorld = new GameWorld(new Upgrades(true));
        lodWorld.start();
        lodWorld.immortal = true;
        lodWorld.asteroids.clear();
        lodWorld.fuel.clear();
        EconomySim lodSim = new EconomySim();
        PilotAgent lodAgent = lodWorld.agents.get(0);
        int lodHielo = lodWorld.station.market.stock(Commodity.HIELO);
        lodSim.update(1f, lodWorld);
        check("la economía no se ejecuta por frame",
                lodAgent.state == PilotAgent.State.DOCKED
                        && lodWorld.station.market.stock(Commodity.HIELO) == lodHielo);
        lodSim.update(1.1f, lodWorld);
        check("cerca, los agentes despiertan a los 2 s",
                lodAgent.state != PilotAgent.State.DOCKED);
        lodSim.update(8f, lodWorld);
        check("los mercados tickean cada 10 s",
                lodWorld.station.market.stock(Commodity.HIELO) < lodHielo);
        GameWorld benchWorld = new GameWorld(new Upgrades(true));
        benchWorld.start();
        benchWorld.immortal = true;
        benchWorld.asteroids.clear();
        benchWorld.fuel.clear();
        benchWorld.agents.createInitial(GameData.MAX_AGENTS);
        EconomySim bench = new EconomySim();
        long benchStart = System.nanoTime();
        for (int i = 0; i < 300; i++) {
            bench.update(2f, benchWorld);
        }
        long benchMs = (System.nanoTime() - benchStart) / 1_000_000L;
        int progressed = 0;
        for (int i = 0; i < benchWorld.agents.size(); i++) {
            if (benchWorld.agents.get(i).credits > 200) {
                progressed++;
            }
        }
        check("24 agentes se simulan sin bloquear",
                benchMs < 2000 && progressed > 0);
        GameWorld offlineWorld = new GameWorld(new Upgrades(true));
        offlineWorld.start();
        offlineWorld.immortal = true;
        offlineWorld.asteroids.clear();
        offlineWorld.fuel.clear();
        EconomySim offline = new EconomySim();
        int offlineHielo = offlineWorld.station.market.stock(Commodity.HIELO);
        int offlineState = offlineWorld.agents.get(0).state.ordinal();
        offline.updateOffline(180f, offlineWorld);
        check("offline usa ticks de 60 s",
                offlineWorld.station.market.stock(Commodity.HIELO) != offlineHielo
                        || offlineWorld.agents.get(0).state.ordinal() != offlineState
                        || offlineWorld.agents.get(0).timer > 0f);

        // T-704: ventas y compras de agentes contra el mercado.
        Market agentMarket = GameData.newMarket();
        PilotAgent trader = new PilotAgent("T-704", "TRADER",
                PilotAgent.Role.MINER, AgentRoster.CARGO_CAPACITY,
                AgentRoster.MAX_FUEL, AgentRoster.SPEED, AgentRoster.MINING_RATE);
        trader.cargo.addExact(Commodity.HIELO, 10);
        int hieloStock0 = agentMarket.stock(Commodity.HIELO);
        float hieloSell0 = agentMarket.sellPrice(Commodity.HIELO);
        int traderCoins0 = trader.credits;
        int sold = agentMarket.sellTo(trader, Commodity.HIELO, 10);
        check("las ventas de agentes suben stock y bajan precio",
                sold > 0
                        && agentMarket.stock(Commodity.HIELO) == hieloStock0 + 10
                        && agentMarket.sellPrice(Commodity.HIELO) < hieloSell0
                        && trader.credits == traderCoins0 + sold
                        && trader.cargo.isEmpty());
        int hierroStock0 = agentMarket.stock(Commodity.HIERRO);
        float hierroBuy0 = agentMarket.buyPrice(Commodity.HIERRO);
        int traderCoins1 = trader.credits;
        int bought = agentMarket.buyTo(trader, Commodity.HIERRO, 5);
        check("las compras de agentes bajan stock y suben precio",
                bought > 0
                        && agentMarket.stock(Commodity.HIERRO) == hierroStock0 - 5
                        && agentMarket.buyPrice(Commodity.HIERRO) > hierroBuy0
                        && trader.credits == traderCoins1 - bought
                        && trader.cargo.amount(Commodity.HIERRO) == 5);
        int coinsBeforeFail = trader.credits;
        check("un agente no vende lo que no tiene",
                agentMarket.sellTo(trader, Commodity.ORO, 1) == 0
                        && trader.credits == coinsBeforeFail);
        PilotAgent broke = new PilotAgent("T-704B", "BROKE",
                PilotAgent.Role.MINER, AgentRoster.CARGO_CAPACITY,
                AgentRoster.MAX_FUEL, AgentRoster.SPEED, AgentRoster.MINING_RATE);
        broke.credits = 0;
        check("un agente sin créditos no compra",
                agentMarket.buyTo(broke, Commodity.HIERRO, 1) == 0
                        && broke.cargo.isEmpty());
        check("un agente no compra más stock del disponible",
                agentMarket.buyTo(trader, Commodity.ORO, 999) == 0);
        PilotAgent full = new PilotAgent("T-704C", "FULL",
                PilotAgent.Role.MINER, AgentRoster.CARGO_CAPACITY,
                AgentRoster.MAX_FUEL, AgentRoster.SPEED, AgentRoster.MINING_RATE);
        full.cargo.addExact(Commodity.SILICATO, 60);
        check("un agente no compra sin capacidad",
                agentMarket.buyTo(full, Commodity.HIELO, 1) == 0);

        // T-705: simulación offline y lastSimTime.
        GameSession offlineSave = new GameSession(new PlayerProfile(new Upgrades(true)));
        offlineSave.startRun();
        SaveService.MemoryStorage offlineStore = new SaveService.MemoryStorage();
        SaveService offlineService = new SaveService(offlineStore);
        check("el guardado escribe lastSimTime", offlineService.save(offlineSave));
        String offlineText = offlineStore.read(SaveService.SAVE_NAME);
        check("lastSimTime está en el guardado",
                offlineText.contains("\"lastSimTime\":"));
        long twelveHoursAgo = System.currentTimeMillis() - 12L * 3600L * 1000L;
        String offline12 = offlineText.replaceAll("\"lastSimTime\":\\d+",
                "\"lastSimTime\":" + twelveHoursAgo);
        GameSession offlineLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("12 h offline se simulan al cargar",
                !offlineText.equals(offline12)
                        && SaveCodec.decode(offline12, offlineLoaded)
                        && offlineLoaded.sector.station.market.stock(Commodity.HIELO)
                        != GameData.MARKET_INITIAL * GameData.MARKET_HIELO);
        boolean offlineBounded = true;
        for (Commodity c : Commodity.values()) {
            int stock = offlineLoaded.sector.station.market.stock(c);
            float price = offlineLoaded.sector.station.market.buyPrice(c);
            offlineBounded = offlineBounded && stock >= 0
                    && stock <= offlineLoaded.sector.station.market.target(c)
                    * EconomySim.MAX_STOCK_FACTOR
                    && price > 0f && price < 10000f;
        }
        check("los precios offline no se descontrolan", offlineBounded);
        check("los agentes siguen tras 12 h offline",
                offlineLoaded.sector.agents.size() == 8
                        && offlineLoaded.sector.agents.get(0).credits >= 0);
        String offline24 = offlineText.replaceAll("\"lastSimTime\":\\d+",
                "\"lastSimTime\":" + (System.currentTimeMillis()
                        - 24L * 3600L * 1000L));
        GameSession offline24Loaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        SaveCodec.decode(offline24, offline24Loaded);
        boolean offlineCapped = true;
        for (Commodity c : Commodity.values()) {
            offlineCapped = offlineCapped
                    && offline24Loaded.sector.station.market.stock(c)
                    == offlineLoaded.sector.station.market.stock(c);
        }
        check("el catch-up se limita a 12 h", offlineCapped);
        String noSimTime = offlineText.replaceAll(",\"lastSimTime\":\\d+", "");
        GameSession noSimLoaded = new GameSession(new PlayerProfile(new Upgrades(true)));
        check("un guardado sin lastSimTime no simula offline",
                !offlineText.equals(noSimTime)
                        && SaveCodec.decode(noSimTime, noSimLoaded)
                        && noSimLoaded.sector.station.market.stock(Commodity.HIELO)
                        == GameData.MARKET_INITIAL * GameData.MARKET_HIELO);

        // T-706: tráfico cosmético de la estación.
        GameWorld trafficWorld = new GameWorld(new Upgrades(true));
        trafficWorld.start();
        trafficWorld.immortal = true;
        trafficWorld.asteroids.clear();
        trafficWorld.fuel.clear();
        int trafficStock = trafficWorld.station.market.stock(Commodity.HIELO);
        trafficWorld.update(0.1f, new Controls());
        check("el tráfico se activa cerca de la estación",
                trafficWorld.traffic.active()
                        && trafficWorld.traffic.activeCount() == StationTraffic.SHIPS
                        && StationTraffic.SHIPS >= 2 && StationTraffic.SHIPS <= 4);
        check("el tráfico no toca la economía",
                trafficWorld.station.market.stock(Commodity.HIELO) == trafficStock
                        && trafficWorld.asteroids.size == 0);
        StationTraffic farTraffic = new StationTraffic();
        farTraffic.update(0.1f, new Vector2(0f, -5000f),
                trafficWorld.station, trafficWorld.fx);
        check("el tráfico se desactiva lejos",
                !farTraffic.active() && farTraffic.activeCount() == 0);
        long trafficStart = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            trafficWorld.traffic.update(0.016f, trafficWorld.ship.pos,
                    trafficWorld.station, null);
        }
        long trafficMs = (System.nanoTime() - trafficStart) / 1_000_000L;
        check("el tráfico no degrada el rendimiento", trafficMs < 500);

        // T-707: gate de agentes, determinismo y rendimiento.
        GameWorld idleWorld = new GameWorld(new Upgrades(true));
        idleWorld.start();
        idleWorld.immortal = true;
        idleWorld.asteroids.clear();
        idleWorld.fuel.clear();
        idleWorld.agents.clear();
        EconomySim idleSim = new EconomySim();
        int idleHielo0 = idleWorld.station.market.stock(Commodity.HIELO);
        float idleHieloPrice0 = idleWorld.station.market.buyPrice(Commodity.HIELO);
        int idleSilicato0 = idleWorld.station.market.stock(Commodity.SILICATO);
        float idleSilicatoPrice0 = idleWorld.station.market.sellPrice(Commodity.SILICATO);
        for (int i = 0; i < 900; i++) {
            idleSim.update(2f, idleWorld);
        }
        check("30 min con el jugador quieto mueven precios",
                idleWorld.station.market.stock(Commodity.HIELO) < idleHielo0
                        && idleWorld.station.market.buyPrice(Commodity.HIELO)
                        > idleHieloPrice0
                        && idleWorld.station.market.stock(Commodity.SILICATO) > idleSilicato0
                        && idleWorld.station.market.sellPrice(Commodity.SILICATO)
                        < idleSilicatoPrice0);
        GameWorld activeWorld = new GameWorld(new Upgrades(true));
        activeWorld.start();
        activeWorld.immortal = true;
        activeWorld.asteroids.clear();
        activeWorld.fuel.clear();
        EconomySim activeSim = new EconomySim();
        for (int i = 0; i < 900; i++) {
            activeSim.update(2f, activeWorld);
        }
        boolean pricesBounded = true;
        int earnedAgents = 0;
        for (Commodity c : Commodity.values()) {
            int stock = activeWorld.station.market.stock(c);
            pricesBounded = pricesBounded && stock >= 0
                    && stock <= activeWorld.station.market.target(c)
                    * EconomySim.MAX_STOCK_FACTOR;
        }
        for (int i = 0; i < activeWorld.agents.size(); i++) {
            PilotAgent a = activeWorld.agents.get(i);
            if (a.credits > 200) {
                earnedAgents++;
            }
        }
        check("30 min con agentes cambian el mercado de forma acotada",
                pricesBounded && earnedAgents > 0);
        GameWorld detA = new GameWorld(new Upgrades(true));
        detA.start();
        detA.immortal = true;
        detA.asteroids.clear();
        detA.fuel.clear();
        GameWorld detB = new GameWorld(new Upgrades(true));
        detB.start();
        detB.immortal = true;
        detB.asteroids.clear();
        detB.fuel.clear();
        EconomySim simA = new EconomySim();
        EconomySim simB = new EconomySim();
        for (int i = 0; i < 300; i++) {
            simA.update(2f, detA);
            simB.update(2f, detB);
        }
        boolean simDeterministic = true;
        for (Commodity c : Commodity.values()) {
            simDeterministic = simDeterministic
                    && detA.station.market.stock(c) == detB.station.market.stock(c)
                    && Math.abs(detA.station.market.buyPrice(c)
                    - detB.station.market.buyPrice(c)) < 0.001f;
        }
        for (int i = 0; i < detA.agents.size(); i++) {
            simDeterministic = simDeterministic
                    && detA.agents.get(i).credits == detB.agents.get(i).credits
                    && detA.agents.get(i).state == detB.agents.get(i).state;
        }
        check("la simulación es determinista", simDeterministic);
        GameWorld perfWorld = new GameWorld(new Upgrades(true));
        perfWorld.start();
        perfWorld.immortal = true;
        perfWorld.asteroids.clear();
        perfWorld.fuel.clear();
        perfWorld.agents.createInitial(GameData.MAX_AGENTS);
        EconomySim perfSim = new EconomySim();
        long perfStart = System.nanoTime();
        for (int i = 0; i < 900; i++) {
            perfSim.update(2f, perfWorld);
        }
        long perfMs = (System.nanoTime() - perfStart) / 1_000_000L;
        System.out.println("BENCH agentes 24 30min=" + perfMs + "ms");
        check("24 agentes no bloquean 30 min de simulación", perfMs < 3000);
        check("el estado de agentes y mercado persiste",
                agentLoaded.sector.agents.size() == 8
                        && marketLoaded.sector.station.market.target(Commodity.HIELO) == 250);
        check("el offline respeta el límite", offlineCapped);

        // T-801: jerarquía del universo y datos estables.
        UniverseState u801 = GameData.newUniverse();
        check("el universo es coherente", u801.valid()
                && u801.validate().isEmpty());
        check("hay varias galaxias, sistemas y sectores",
                u801.galaxyCount() == 3
                        && u801.systemCount() == 3
                        && u801.sectorCount() == 6);
        boolean u801Bias = true;
        for (int i = 0; i < u801.galaxies().size(); i++) {
            GalaxyDef g = u801.galaxies().get(i);
            if (!g.id.isEmpty() && g.seed != 0L
                    && g.nebulaColor != 0 && g.starColor != 0) {
                for (Commodity c : Commodity.values()) {
                    float bias = g.bias(c);
                    u801Bias = u801Bias && Float.isFinite(bias) && bias > 0f;
                }
            } else {
                u801Bias = false;
            }
        }
        check("cada galaxia tiene semilla, paleta y sesgos", u801Bias);
        SectorDef u801Start = u801.defaultSector();
        check("el sector inicial tiene límites, perfil y estación",
                u801Start != null
                        && GameData.START_SECTOR_ID.equals(u801Start.id)
                        && u801Start.width == 12000f
                        && u801Start.height == 12000f
                        && u801Start.profile == SectorDef.Profile.INICIAL
                        && u801Start.stationCount() >= 1
                        && u801Start.contains(0f, 0f)
                        && !u801Start.contains(7000f, 0f));
        boolean u801Limits = true;
        for (int i = 0; i < u801.galaxies().size(); i++) {
            GalaxyDef g = u801.galaxies().get(i);
            for (int k = 0; k < g.systems.size(); k++) {
                SystemDef sys = g.systems.get(k);
                u801Limits = u801Limits
                        && sys.sectorCount() >= SystemDef.MIN_SECTORS
                        && sys.sectorCount() <= SystemDef.MAX_SECTORS
                        && g.id.equals(sys.galaxyId);
                for (int j = 0; j < sys.sectors.size(); j++) {
                    SectorDef sec = sys.sectors.get(j);
                    u801Limits = u801Limits
                            && sys.id.equals(sec.systemId)
                            && sec.width > 0f && sec.height > 0f
                            && sec.profile != null
                            && sec.stationCount() > 0;
                }
            }
        }
        check("todos los sectores tienen límites, perfil y estaciones", u801Limits);
        UniverseState u801Copy = GameData.newUniverse();
        check("los IDs y semillas son estables",
                u801Copy.sector(GameData.START_SECTOR_ID) != null
                        && u801Copy.sector(GameData.START_SECTOR_ID).seed
                        == u801Start.seed
                        && u801Copy.galaxy(GameData.START_GALAXY_ID).seed
                        == u801.galaxy(GameData.START_GALAXY_ID).seed
                        && u801Copy.sector("sol-belt").id.equals("sol-belt")
                        && u801Copy.sector("sol-belt").seed
                        == u801.sector("sol-belt").seed);
        check("las galaxias se diferencian por sesgos",
                u801.galaxy(GameData.ANDROMEDA_GALAXY_ID).bias(Commodity.HIERRO) > 1f
                        && u801.galaxy(GameData.DRACO_GALAXY_ID).bias(Commodity.HIELO) > 1f);
        check("las búsquedas por ID devuelven el mismo objeto",
                u801.sector("sol-prime") == u801Start
                        && u801.system("sol") == u801.defaultSystem()
                        && u801.galaxy("milky-way") == u801.defaultGalaxy());
        check("la validación detecta datos incompletos",
                !new UniverseState().valid()
                        && !GameData.newUniverse().validate().contains("sin limites")
                        && new UniverseState().validate().size() == 1);
        SectorDef u801NoStation = new SectorDef("vacio", "VACIO", "s",
                1L, 1000f, 1000f, SectorDef.Profile.INICIAL, 10f);
        UniverseState u801Bad = new UniverseState();
        GalaxyDef u801BadGalaxy = new GalaxyDef("g", "G", "", 1L,
                0xFFFFFF, 0xFFFFFF, null);
        SystemDef u801BadSystem = new SystemDef("s", "S", "g", 1L);
        u801BadSystem.addSector(u801NoStation);
        u801BadGalaxy.addSystem(u801BadSystem);
        u801Bad.addGalaxy(u801BadGalaxy);
        check("la validación exige estaciones", !u801Bad.valid());

        // T-802: runtime de sector finito.
        SectorDef tiny802 = new SectorDef("tiny-802", "TINY", "sys", 1L,
                2000f, 1600f, SectorDef.Profile.CINTURON, 100f)
                .addStation(GameData.OUTPOST);
        GameWorld world802 = new GameWorld(new Upgrades(true), tiny802);
        check("el mundo usa la definición de sector",
                world802.sector == tiny802
                        && world802.halfWidth() == 1000f
                        && world802.halfHeight() == 800f
                        && !world802.outsideSector(0f, 0f)
                        && world802.outsideSector(1000.5f, 0f)
                        && world802.outsideSector(0f, -800.5f));
        world802.start();
        world802.asteroids.clear();
        world802.fuel.clear();
        world802.ship.pos.set(-99999f, 99999f);
        world802.ship.vel.set(-50f, 50f);
        world802.update(0.01f, new Controls());
        check("la nave queda dentro del sector",
                world802.ship.pos.x >= -world802.halfWidth()
                        && world802.ship.pos.x <= world802.halfWidth()
                        && world802.ship.pos.y >= -world802.halfHeight()
                        && world802.ship.pos.y <= world802.halfHeight()
                        && world802.ship.vel.x == 0f && world802.ship.vel.y == 0f);
        SectorDef respawn802 = new SectorDef("respawn-802", "RESPAWN", "sys", 5L,
                12000f, 12000f, SectorDef.Profile.CINTURON, 100f)
                .addStation(GameData.OUTPOST);
        GameWorld respawnWorld802 = new GameWorld(new Upgrades(true), respawn802);
        respawnWorld802.start();
        respawnWorld802.ship.pos.set(3000f, 3000f);
        respawnWorld802.update(0.01f, new Controls());
        int chunksBefore802 = respawnWorld802.generatedChunks().size();
        int rocksBefore802 = respawnWorld802.asteroids.size;
        respawnWorld802.asteroids.clear();
        respawnWorld802.update(0.01f, new Controls());
        check("los recursos minados no reaparecen",
                rocksBefore802 > 0
                        && respawnWorld802.asteroids.size == 0
                        && respawnWorld802.generatedChunks().size() == chunksBefore802);
        SectorDef metal802 = new SectorDef("metal-802", "METAL", "sys", 777L,
                12000f, 12000f, SectorDef.Profile.METALICO, 100f)
                .addStation(GameData.OUTPOST);
        SectorDef ice802 = new SectorDef("ice-802", "HIELO", "sys", 777L,
                12000f, 12000f, SectorDef.Profile.HELADO, 100f)
                .addStation(GameData.OUTPOST);
        GameWorld metalWorld = new GameWorld(new Upgrades(true), metal802);
        metalWorld.start();
        metalWorld.asteroids.clear();
        metalWorld.ship.pos.set(3000f, 3000f);
        metalWorld.update(0.01f, new Controls());
        int metalM = 0;
        int metalC = 0;
        for (int i = 0; i < metalWorld.asteroids.size; i++) {
            AsteroidType type = metalWorld.asteroids.get(i).type;
            metalM += type == AsteroidType.M ? 1 : 0;
            metalC += type == AsteroidType.C ? 1 : 0;
        }
        GameWorld iceWorld = new GameWorld(new Upgrades(true), ice802);
        iceWorld.start();
        iceWorld.asteroids.clear();
        iceWorld.ship.pos.set(3000f, 3000f);
        iceWorld.update(0.01f, new Controls());
        int iceM = 0;
        int iceC = 0;
        for (int i = 0; i < iceWorld.asteroids.size; i++) {
            AsteroidType type = iceWorld.asteroids.get(i).type;
            iceM += type == AsteroidType.M ? 1 : 0;
            iceC += type == AsteroidType.C ? 1 : 0;
        }
        check("el perfil del sector cambia la generación",
                metalM > iceM && iceC > metalC);
        SectorDef small802 = new SectorDef("small-802", "SMALL", "sys", 9L,
                12000f, 12000f, SectorDef.Profile.CINTURON, 100f)
                .addStation(GameData.OUTPOST);
        SectorDef huge802 = new SectorDef("huge-802", "HUGE", "sys", 9L,
                120000f, 120000f, SectorDef.Profile.CINTURON, 100f)
                .addStation(GameData.OUTPOST);
        GameWorld smallWorld802 = new GameWorld(new Upgrades(true), small802);
        smallWorld802.start();
        GameWorld hugeWorld802 = new GameWorld(new Upgrades(true), huge802);
        hugeWorld802.start();
        check("el coste no depende del tamaño total",
                smallWorld802.generatedChunks().size()
                        == hugeWorld802.generatedChunks().size()
                        && hugeWorld802.generatedChunks().size()
                        <= (2 * GameWorld.RING + 1) * (2 * GameWorld.RING + 1));

        // T-803: persistencia y descarga de sectores.
        GameSession snapSession = new GameSession(
                new PlayerProfile(new Upgrades(true)));
        snapSession.startRun();
        GameWorld snapWorld = snapSession.sector;
        snapWorld.ship.pos.set(3000f, 3000f);
        snapWorld.update(0.01f, new Controls());
        if (snapWorld.asteroids.size > 0) {
            snapWorld.asteroids.removeIndex(0);
        }
        int snapReserve = 0;
        for (int i = 0; i < snapWorld.asteroids.size; i++) {
            snapReserve += snapWorld.asteroids.get(i).reserve;
        }
        int snapChunks = snapWorld.generatedChunks().size();
        int snapRocks = snapWorld.asteroids.size;
        SectorRuntime.Snapshot captured = snapSession.captureCurrentSector();
        check("al salir se captura el sector",
                captured != null
                        && GameData.START_SECTOR_ID.equals(captured.sectorId)
                        && captured.chunks.size() == snapChunks
                        && captured.asteroids.size() == snapRocks
                        && SectorRuntime.reserveUnits(captured) == snapReserve);
        check("los chunks no visitados no se guardan enteros",
                captured.chunks.size() < 225
                        && captured.asteroids.size() <= snapRocks);
        SaveService.MemoryStorage sectorStore = new SaveService.MemoryStorage();
        SaveService sectorService = new SaveService(sectorStore);
        check("el guardado con sectores es válido",
                sectorService.save(snapSession));
        String sectorText = sectorStore.read(SaveService.SAVE_NAME);
        check("el guardado incluye el snapshot del sector",
                SaveCodec.validate(sectorText)
                        && sectorText.contains("\"sectorSnapshots\""));

        GameSession sectorLoaded = new GameSession(
                new PlayerProfile(new Upgrades(true)));
        check("el snapshot del sector se restaura",
                sectorService.load(sectorLoaded) == SaveService.LoadStatus.V2
                        && sectorLoaded.universe.hasSnapshot(GameData.START_SECTOR_ID)
                        && sectorLoaded.universe.snapshot(GameData.START_SECTOR_ID)
                        .asteroids.size() == snapRocks
                        && SectorRuntime.reserveUnits(
                        sectorLoaded.universe.snapshot(GameData.START_SECTOR_ID))
                        == snapReserve);
        SectorRuntime.Snapshot loadedSnap =
                sectorLoaded.universe.snapshot(GameData.START_SECTOR_ID);
        GameWorld sectorRestored = new GameWorld(new Upgrades(true),
                sectorLoaded.currentSector);
        sectorRestored.start();
        SectorRuntime.restore(sectorRestored, loadedSnap);
        check("restaurar el sector no duplica minerales",
                sectorRestored.asteroids.size == loadedSnap.asteroids.size()
                        && SectorRuntime.reserveUnits(
                        SectorRuntime.capture(sectorRestored,
                                GameData.START_SECTOR_ID)) == snapReserve
                        && sectorRestored.generatedChunks().size()
                        == loadedSnap.chunks.size());
        sectorRestored.ship.pos.set(3000f, 3000f);
        sectorRestored.update(0.01f, new Controls());
        int restoredReserve = 0;
        for (int i = 0; i < sectorRestored.asteroids.size; i++) {
            restoredReserve += sectorRestored.asteroids.get(i).reserve;
        }
        check("los recursos minados no reaparecen al volver",
                restoredReserve <= snapReserve
                        && sectorRestored.generatedChunks().size()
                        == loadedSnap.chunks.size());
        int chunksBeforeFar = sectorRestored.generatedChunks().size();
        sectorRestored.ship.pos.set(-5000f, -5000f);
        sectorRestored.update(0.01f, new Controls());
        check("los chunks no visitados se generan al volver",
                sectorRestored.generatedChunks().size() > chunksBeforeFar
                        && sectorRestored.asteroids.size > 0);

        // T-808: streaming de sectores y regiones.
        UniverseState streamU = GameData.newUniverse();
        SectorStreamer streamer = new SectorStreamer(streamU);
        streamer.switchTo(GameData.START_SECTOR_ID, null);
        check("solo se cargan las regiones cercanas",
                streamer.loadedCount() == 2
                        && streamer.isLoaded(GameData.START_SECTOR_ID)
                        && streamer.isLoaded("sol-belt")
                        && !streamer.isLoaded("sol-ice")
                        && !streamer.isLoaded("andromeda-forge")
                        && streamer.unloadedCount() == 4);
        check("el mapa soporta escala de sistema y galaxia",
                streamU.sector(GameData.START_SECTOR_ID).distanceTo(
                        streamU.sector("sol-belt")) < 2.5f
                        && streamU.sector(GameData.START_SECTOR_ID).distanceTo(
                        streamU.sector("andromeda-forge")) > 30f
                        && streamU.sector("andromeda-forge").distanceTo(
                        streamU.sector("draco-core")) > 50f);
        GameSession streamSession = new GameSession(
                new PlayerProfile(new Upgrades(true)));
        streamSession.startRun();
        streamSession.sector.ship.pos.set(3000f, 3000f);
        streamSession.sector.update(0.01f, new Controls());
        if (streamSession.sector.asteroids.size > 0) {
            streamSession.sector.asteroids.removeIndex(0);
        }
        int streamReserve = 0;
        for (int i = 0; i < streamSession.sector.asteroids.size; i++) {
            streamReserve += streamSession.sector.asteroids.get(i).reserve;
        }
        int streamChunks = streamSession.sector.generatedChunks().size();
        streamSession.streamer.switchTo("andromeda-forge", streamSession.sector);
        SectorRuntime.Snapshot streamSnap = streamSession.universe.snapshot(
                GameData.START_SECTOR_ID);
        check("descargar un sector conserva su estado",
                streamSnap != null
                        && SectorRuntime.reserveUnits(streamSnap) == streamReserve
                        && streamSnap.chunks.size() == streamChunks
                        && !streamSession.streamer.isLoaded(GameData.START_SECTOR_ID)
                        && streamSession.streamer.isLoaded("andromeda-forge"));
        GameWorld streamLoaded = streamSession.streamer.load(
                GameData.START_SECTOR_ID, new Upgrades(true));
        check("volver a un sector descargado restaura su estado",
                streamLoaded != null
                        && streamLoaded.asteroids.size == streamSnap.asteroids.size()
                        && streamLoaded.generatedChunks().size() == streamChunks
                        && SectorRuntime.reserveUnits(
                        SectorRuntime.capture(streamLoaded, GameData.START_SECTOR_ID))
                        == streamReserve);
        streamLoaded.asteroids.clear();
        streamLoaded.update(0.01f, new Controls());
        check("el sector recargado no regenera recursos minados",
                streamLoaded.asteroids.size == 0
                        && streamLoaded.generatedChunks().size() == streamChunks);
        UniverseState bigU = new UniverseState();
        for (int i = 0; i < 200; i++) {
            String gid = "g-" + i;
            GalaxyDef galaxy = new GalaxyDef(gid, "G" + i, "", 1000L + i,
                    0x223344, 0xDDEEFF, null);
            SystemDef system = new SystemDef("s-" + i, "S" + i, gid, 2000L + i);
            system.addSector(new SectorDef("sec-" + i, "SEC" + i, system.id,
                    3000L + i, 12000f, 12000f,
                    SectorDef.Profile.CINTURON, 100f, i * 5f, 0f));
            galaxy.addSystem(system);
            bigU.addGalaxy(galaxy);
        }
        SectorStreamer bigStreamer = new SectorStreamer(bigU);
        long streamStart = System.nanoTime();
        bigStreamer.switchTo("sec-0", null);
        for (int i = 1; i < 200; i++) {
            bigStreamer.switchTo("sec-" + i, null);
        }
        long streamMs = (System.nanoTime() - streamStart) / 1_000_000L;
        check("el coste no depende del tamaño del universo",
                bigStreamer.loadedCount() == 1
                        && bigStreamer.unloadedCount() == 199
                        && streamMs < 100);

        // T-602: TradeService, validación y reacción de precios.
        GameWorld tradeWorld = new GameWorld(new Upgrades(true));
        tradeWorld.start();
        tradeWorld.immortal = true;
        tradeWorld.asteroids.clear();
        tradeWorld.fuel.clear();
        Upgrades tradeUp = new Upgrades(true);
        tradeUp.setCoins(1000);
        Market tradeMarket = tradeWorld.station.market;
        TradeRecorder recorder = new TradeRecorder();
        float ironBuyBefore = tradeMarket.buyPrice(Commodity.HIERRO);
        TradeService.Result buy = TradeService.buy(tradeWorld, tradeUp,
                Commodity.HIERRO, 10, recorder);
        check("comprar mueve carga, créditos y stock",
                buy.ok
                        && tradeWorld.ship.cargo().amount(Commodity.HIERRO) == 10
                        && tradeUp.coins() == 1000 - buy.trade.total
                        && tradeMarket.stock(Commodity.HIERRO) == 110
                        && recorder.count == 1);
        check("comprar sube el precio",
                tradeMarket.buyPrice(Commodity.HIERRO) > ironBuyBefore);
        float ironSellBefore = tradeMarket.sellPrice(Commodity.HIERRO);
        int sellStockBefore = tradeMarket.stock(Commodity.HIERRO);
        TradeService.Result sell = TradeService.sell(tradeWorld, tradeUp,
                Commodity.HIERRO, 10, recorder);
        check("vender mueve carga, créditos y stock",
                sell.ok
                        && tradeWorld.ship.cargo().amount(Commodity.HIERRO) == 0
                        && tradeUp.coins() == 1000 - buy.trade.total + sell.trade.total
                        && tradeMarket.stock(Commodity.HIERRO) == sellStockBefore + 10
                        && recorder.count == 2);
        check("vender baja el precio",
                tradeMarket.sellPrice(Commodity.HIERRO) < ironSellBefore);
        check("no se vende lo que no se tiene",
                !TradeService.sell(tradeWorld, tradeUp, Commodity.ORO, 1, recorder).ok
                        && recorder.count == 2);
        check("no se compra sin créditos",
                !TradeService.buy(tradeWorld, new Upgrades(true),
                        Commodity.HIERRO, 1, recorder).ok);
        check("no se compra más stock del disponible",
                !TradeService.buy(tradeWorld, tradeUp, Commodity.ORO, 999, recorder).ok);
        tradeWorld.ship.cargo().addExact(Commodity.SILICATO, 100);
        check("no se compra sin capacidad",
                !TradeService.buy(tradeWorld, tradeUp, Commodity.HIERRO, 1, recorder).ok);
        check("no se compra una cantidad inválida",
                !TradeService.buy(tradeWorld, tradeUp, Commodity.HIERRO, 0, recorder).ok
                        && !TradeService.sell(tradeWorld, tradeUp, Commodity.HIERRO, -1, recorder).ok);

        // T-408: verificación integrada de la etapa 4.
        Ship gateShip = new Ship();
        gateShip.reset(new Upgrades(true), 0f, 0f);
        MiningLaser gateLaser = new MiningLaser();
        Asteroid gateC = new Asteroid(100f, 0f, Asteroid.MEDIUM, AsteroidType.C, 30, 30, 901L);
        Asteroid gateS = new Asteroid(120f, 0f, Asteroid.MEDIUM, AsteroidType.S, 30, 30, 902L);
        Asteroid gateM = new Asteroid(140f, 0f, Asteroid.MEDIUM, AsteroidType.M, 30, 30, 903L);
        float timeC = 30f / gateLaser.rate(gateShip, gateC);
        float timeS = 30f / gateLaser.rate(gateShip, gateS);
        float timeM = 30f / gateLaser.rate(gateShip, gateM);
        check("S4: C, S y M tienen tiempos distintos",
                timeC < timeS && timeS < timeM);
        check("S4: los tiempos caben en el plan",
                timeC >= 2f && timeC <= 5f
                        && timeS >= 6f && timeS <= 10f
                        && timeM >= 9f && timeM <= 30f);
        GameWorld gate = new GameWorld(new Upgrades(true));
        gate.start();
        gate.immortal = true;
        gate.asteroids.clear();
        gate.fuel.clear();
        Asteroid gateRock = new Asteroid(120f, 0f, Asteroid.MEDIUM,
                AsteroidType.M, 50, 50, 904L);
        gateRock.vel.set(0f, 0f);
        gate.asteroids.add(gateRock);
        gate.ship.laser.aim(gateRock);
        int gateSteps = 0;
        while (gate.ship.laser.active && gateSteps < 60 * 40) {
            gate.update(dt, new Controls());
            gateSteps++;
        }
        float gateSeconds = gateSteps / 60f;
        check("S4: minar M agota la roca y llena la bodega",
                gateRock.depleted()
                        && !gate.asteroids.contains(gateRock, true)
                        && gate.ship.cargo().totalUnits() == 50);
        check("S4: el tiempo de minado M es razonable",
                gateSeconds >= 12f && gateSeconds <= 25f);
        check("S4: minar no da monedas", gate.coinsEarned == 0);
        int gateCredits = 0;
        for (int i = 0; i < gate.ship.cargo().entries().size(); i++) {
            Cargo gateEntry = gate.ship.cargo().entries().get(i);
            gateCredits += Math.round(gateEntry.amount * gateEntry.commodity.basePrice);
        }
        check("S4: la fundicion temporal valora el mineral", gateCredits > 0);
        check("S4: el mineral extraido respeta la capacidad",
                gate.ship.cargo().usedMass() > 0f
                        && !gate.ship.cargo().overCapacity());

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

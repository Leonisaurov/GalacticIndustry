package com.example.codegame;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Codec del guardado v2. Convierte una {@link GameSession} completa a JSON y
 * viceversa: version, perfil, piloto, nave y sector.
 *
 * <p>La decodificacion se hace primero a un objeto {@link Data} y solo se
 * aplica a la sesion cuando todo el texto es valido. Asi un guardado corrupto
 * no deja media partida cargada.</p>
 */
final class SaveCodec {

    static final int VERSION = 2;

    private SaveCodec() {
    }

    static String encode(GameSession session) {
        try {
            StringWriter out = new StringWriter(1024);
            JsonWriter w = new JsonWriter(out);
            w.object();
            w.set("version", VERSION);

            PlayerProfile profile = session.profile;
            w.name("profile").object();
            w.set("playerId", profile.playerId);
            w.set("characterId", profile.id);
            // Se conserva "id" para leer guardados v2 creados antes de T-107.
            w.set("id", profile.id);
            w.set("name", profile.name);
            w.set("coins", profile.upgrades.coins());
            w.set("refills", profile.upgrades.refillCount());
            w.name("levels").array();
            for (int i = 0; i < ShipLoadout.COUNT; i++) {
                w.value(profile.upgrades.loadout.level(i));
            }
            w.pop();
            w.name("modules").array();
            for (int i = 0; i < ShipLoadout.MODULE_SLOTS; i++) {
                String module = profile.upgrades.loadout.module(i);
                if (module == null) {
                    w.value((String) null);
                } else {
                    w.value(module);
                }
            }
            w.pop();
            w.pop();

            Pilot pilot = session.pilot;
            w.name("pilot").object();
            w.set("health", pilot.health);
            w.set("maxHealth", pilot.maxHealth);
            w.set("oxygen", pilot.oxygen);
            w.set("o2Max", pilot.o2Max);
            w.set("status", pilot.status.name());
            w.set("aboard", pilot.aboard);
            w.set("grace", pilot.oxygenGrace);
            w.pop();

            LifeSupport life = session.lifeSupport;
            w.name("lifeSupport").object();
            w.set("filter", life.filter);
            w.set("leak", life.leak);
            w.set("breach", life.breach);
            w.pop();

            GameWorld world = session.sector;
            Ship ship = world.ship;
            w.name("ship").object();
            w.set("x", ship.pos.x);
            w.set("y", ship.pos.y);
            w.set("vx", ship.vel.x);
            w.set("vy", ship.vel.y);
            w.set("heading", ship.heading);
            w.set("fuel", ship.fuel);
            w.set("hull", ship.part[Ship.HULL]);
            w.set("invulnerable", ship.invulnerable);
            w.name("parts").array();
            for (int i = 0; i < Ship.PART_COUNT; i++) {
                w.value(ship.part[i]);
            }
            w.pop();
            w.name("cargo").array();
            for (Cargo cargo : ship.cargo().entries()) {
                w.object();
                w.set("commodity", cargo.commodity.name());
                w.set("amount", cargo.amount);
                w.pop();
            }
            w.pop();
            w.pop();

            w.name("sector").object();
            w.set("coinsEarned", world.coinsEarned);
            w.set("score", world.score);
            w.set("elapsed", world.elapsed);
            w.set("legacyWreck", world.legacyWreck);
            w.set("legacyX", world.legacyWreckPos.x);
            w.set("legacyY", world.legacyWreckPos.y);
            w.set("legacyHeading", world.legacyWreckHeading);
            w.set("legacyHull", world.legacyWreckHull);
            w.name("legacyParts").array();
            for (int i = 0; i < Ship.PART_COUNT; i++) {
                w.value(world.legacyWreckParts[i]);
            }
            w.pop();
            w.name("chunks").array();
            for (Long key : world.generatedChunks()) {
                w.value(key);
            }
            w.pop();
            w.name("asteroids").array();
            for (int i = 0; i < world.asteroids.size; i++) {
                Asteroid a = world.asteroids.get(i);
                w.object();
                w.set("x", a.pos.x);
                w.set("y", a.pos.y);
                w.set("vx", a.vel.x);
                w.set("vy", a.vel.y);
                w.set("size", a.size);
                w.set("hp", a.hp);
                w.set("type", a.type.name());
                w.set("reserve", a.reserve);
                w.set("maxReserve", a.maxReserve);
                w.pop();
            }
            w.pop();
            w.name("pickups").array();
            for (int i = 0; i < world.fuel.size; i++) {
                FuelPickup f = world.fuel.get(i);
                w.object();
                w.set("x", f.pos.x);
                w.set("y", f.pos.y);
                w.set("life", f.life());
                w.pop();
            }
            w.pop();
            w.pop();

            w.pop();
            w.close();
            return out.toString();
        } catch (IOException e) {
            throw new IllegalStateException("no se pudo generar el guardado", e);
        }
    }

    /** Valida la estructura sin modificar ninguna sesion. */
    static boolean validate(String text) {
        return parse(text) != null;
    }

    /** Decodifica y aplica el guardado; devuelve false si el texto no es valido. */
    static boolean decode(String text, GameSession session) {
        Data data = parse(text);
        return data != null && apply(data, session);
    }

    private static Data parse(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            JsonValue root = new JsonReader().parse(text);
            if (root == null || root.getInt("version", 0) != VERSION) {
                return null;
            }
            JsonValue profile = root.get("profile");
            JsonValue pilot = root.get("pilot");
            JsonValue ship = root.get("ship");
            JsonValue sector = root.get("sector");
            JsonValue lifeSupport = root.get("lifeSupport");
            if (profile == null || pilot == null || ship == null || sector == null) {
                return null;
            }
            Data d = new Data();
            d.playerId = profile.getString("playerId", PlayerProfile.DEFAULT_PLAYER_ID);
            d.profileId = profile.getString("characterId",
                    profile.getString("id", PlayerProfile.DEFAULT_ID));
            d.profileName = profile.getString("name", PlayerProfile.DEFAULT_NAME);
            d.coins = profile.getInt("coins", 0);
            d.refills = profile.getInt("refills", 0);
            JsonValue levels = profile.get("levels");
            // Los guardados anteriores a T-305 tienen 5 niveles; BODEGA se
            // rellena a 0.
            if (levels == null || !levels.isArray()
                    || levels.size < 5 || levels.size > ShipLoadout.COUNT) {
                return null;
            }
            d.levels = new int[ShipLoadout.COUNT];
            for (int i = 0; i < ShipLoadout.COUNT; i++) {
                d.levels[i] = i < levels.size ? levels.get(i).asInt() : 0;
            }
            d.modules = new String[ShipLoadout.MODULE_SLOTS];
            JsonValue modules = profile.get("modules");
            if (modules != null && modules.isArray()) {
                int count = Math.min(modules.size, ShipLoadout.MODULE_SLOTS);
                for (int i = 0; i < count; i++) {
                    JsonValue value = modules.get(i);
                    if (value != null && !value.isNull()) {
                        d.modules[i] = value.asString();
                    }
                }
            }

            d.health = pilot.getFloat("health", Pilot.START_MAX);
            d.maxHealth = pilot.getFloat("maxHealth", Pilot.START_MAX);
            d.oxygen = pilot.getFloat("oxygen", Pilot.START_MAX);
            d.o2Max = pilot.getFloat("o2Max", Pilot.START_MAX);
            d.status = pilot.getString("status", Pilot.Status.OK.name());
            d.aboard = pilot.getBoolean("aboard", true);
            d.grace = pilot.getFloat("grace", Pilot.O2_GRACE_SECONDS);
            if (lifeSupport != null) {
                d.filter = lifeSupport.getFloat("filter", LifeSupport.FILTER_MAX);
                d.leak = lifeSupport.getFloat("leak", 0f);
                d.breach = lifeSupport.getBoolean("breach", false);
            }

            d.x = ship.getFloat("x", 0f);
            d.y = ship.getFloat("y", 0f);
            d.vx = ship.getFloat("vx", 0f);
            d.vy = ship.getFloat("vy", 0f);
            d.heading = ship.getFloat("heading", 90f);
            d.fuel = ship.getFloat("fuel", 0f);
            d.hull = ship.getFloat("hull", 0f);
            d.invulnerable = ship.getFloat("invulnerable", 0f);
            JsonValue parts = ship.get("parts");
            // Los guardados anteriores a T-201 tienen 7 partes; LIFE_SUPPORT se
            // rellena con integridad completa.
            if (parts == null || !parts.isArray() || parts.size < 7) {
                return null;
            }
            d.parts = new float[Ship.PART_COUNT];
            for (int i = 0; i < Ship.PART_COUNT; i++) {
                d.parts[i] = i < parts.size ? parts.get(i).asFloat() : Ship.PART_MAX;
            }
            JsonValue cargo = ship.get("cargo");
            if (cargo != null && cargo.isArray()) {
                for (int i = 0; i < cargo.size; i++) {
                    JsonValue entry = cargo.get(i);
                    if (entry == null) {
                        continue;
                    }
                    Commodity commodity = Commodity.byDisplayName(
                            entry.getString("commodity", null));
                    int amount = entry.getInt("amount", 0);
                    if (commodity != null && amount > 0) {
                        d.cargo.add(new CargoData(commodity, amount));
                    }
                }
            }

            d.coinsEarned = sector.getInt("coinsEarned", 0);
            d.score = sector.getInt("score", 0);
            d.elapsed = sector.getFloat("elapsed", 0f);
            d.legacyWreck = sector.getBoolean("legacyWreck", false);
            d.legacyX = sector.getFloat("legacyX", 0f);
            d.legacyY = sector.getFloat("legacyY", 0f);
            d.legacyHeading = sector.getFloat("legacyHeading", 0f);
            d.legacyHull = sector.getFloat("legacyHull", 0f);
            JsonValue legacyParts = sector.get("legacyParts");
            if (legacyParts != null && legacyParts.isArray()) {
                int count = Math.min(legacyParts.size, Ship.PART_COUNT);
                for (int i = 0; i < count; i++) {
                    d.legacyParts[i] = legacyParts.get(i).asFloat();
                }
            }
            JsonValue chunks = sector.get("chunks");
            if (chunks == null || !chunks.isArray()) {
                return null;
            }
            d.chunks = new long[chunks.size];
            for (int i = 0; i < chunks.size; i++) {
                d.chunks[i] = chunks.get(i).asLong();
            }
            JsonValue rocks = sector.get("asteroids");
            if (rocks == null || !rocks.isArray()) {
                return null;
            }
            for (int i = 0; i < rocks.size; i++) {
                JsonValue rock = rocks.get(i);
                if (rock == null) {
                    return null;
                }
                AsteroidData a = new AsteroidData();
                a.x = rock.getFloat("x", 0f);
                a.y = rock.getFloat("y", 0f);
                a.vx = rock.getFloat("vx", 0f);
                a.vy = rock.getFloat("vy", 0f);
                a.size = rock.getInt("size", Asteroid.SMALL);
                a.hp = rock.getInt("hp", 1);
                a.type = AsteroidType.byName(
                        rock.getString("type", AsteroidType.C.name()));
                a.reserve = rock.getInt("reserve", -1);
                a.maxReserve = rock.getInt("maxReserve", a.reserve);
                d.asteroids.add(a);
            }
            JsonValue pickups = sector.get("pickups");
            if (pickups == null || !pickups.isArray()) {
                return null;
            }
            for (int i = 0; i < pickups.size; i++) {
                JsonValue pickup = pickups.get(i);
                if (pickup == null) {
                    return null;
                }
                PickupData f = new PickupData();
                f.x = pickup.getFloat("x", 0f);
                f.y = pickup.getFloat("y", 0f);
                f.life = pickup.getFloat("life", FuelPickup.LIFE);
                d.pickups.add(f);
            }
            return d;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean apply(Data d, GameSession session) {
        // Valida el piloto antes de tocar la sesion.
        Pilot pilot = new Pilot();
        String pilotLine = "pilot " + d.health + " " + d.maxHealth + " "
                + d.oxygen + " " + d.o2Max + " " + d.status + " " + d.grace;
        if (!pilot.restore(pilotLine)) {
            return false;
        }
        pilot.aboard = d.aboard;

        LifeSupport life = new LifeSupport();
        life.filter = clamp(d.filter, 0f, LifeSupport.FILTER_MAX);
        life.leak = Math.max(0f, d.leak);
        life.breach = d.breach;

        Upgrades up = session.upgrades();
        up.setCoins(d.coins);
        up.setRefillCount(d.refills);
        for (int i = 0; i < ShipLoadout.COUNT; i++) {
            up.loadout.setLevel(i, d.levels[i]);
        }
        for (int i = 0; i < ShipLoadout.MODULE_SLOTS; i++) {
            if (d.modules[i] == null) {
                up.loadout.removeModule(i);
            } else {
                up.loadout.installModule(i, d.modules[i]);
            }
        }
        session.profile.restoreIdentity(d.playerId, d.profileId, d.profileName);

        GameWorld world = session.sector;
        world.start();
        Ship ship = world.ship;
        ship.pos.set(d.x, d.y);
        ship.vel.set(d.vx, d.vy);
        ship.heading = d.heading;
        ship.dir.set(MathUtils.cosDeg(d.heading), MathUtils.sinDeg(d.heading));
        ship.maxHull = up.hull();
        ship.part[Ship.HULL] = clamp(d.hull, 0f, ship.maxHull);
        for (int i = 1; i < Ship.PART_COUNT; i++) {
            ship.part[i] = clamp(d.parts[i], 0f, Ship.PART_MAX);
        }
        ship.syncFuel(up);
        ship.fuel = clamp(d.fuel, 0f, ship.maxFuel);
        ship.invulnerable = Math.max(0f, d.invulnerable);
        ship.cargo().clear();
        for (int i = 0; i < d.cargo.size(); i++) {
            CargoData cargo = d.cargo.get(i);
            ship.cargo().restore(cargo.commodity, cargo.amount);
        }
        session.pilot.copyFrom(pilot);
        session.lifeSupport.copyFrom(life);

        world.coinsEarned = d.coinsEarned;
        world.score = d.score;
        world.elapsed = Math.max(0f, d.elapsed);
        world.wave = 1 + (int) (world.elapsed / 45f);
        world.legacyWreck = d.legacyWreck;
        world.legacyWreckPos.set(d.legacyX, d.legacyY);
        world.legacyWreckHeading = d.legacyHeading;
        world.legacyWreckHull = d.legacyHull;
        System.arraycopy(d.legacyParts, 0, world.legacyWreckParts, 0, Ship.PART_COUNT);
        world.generatedChunks().clear();
        for (long chunk : d.chunks) {
            world.generatedChunks().add(chunk);
        }
        world.asteroids.clear();
        for (int i = 0; i < d.asteroids.size(); i++) {
            AsteroidData a = d.asteroids.get(i);
            int size = clampSize(a.size);
            AsteroidType type = a.type == null ? AsteroidType.C : a.type;
            Asteroid rock = new Asteroid(a.x, a.y, size, type, a.reserve,
                    a.maxReserve, Asteroid.seedFrom(a.x, a.y, size, type));
            rock.vel.set(a.vx, a.vy);
            rock.hp = Math.max(1, a.hp);
            world.asteroids.add(rock);
        }
        world.fuel.clear();
        for (int i = 0; i < d.pickups.size(); i++) {
            PickupData f = d.pickups.get(i);
            world.fuel.add(new FuelPickup(f.x, f.y, Math.max(0.01f, f.life)));
        }
        world.missiles.clear();
        world.fx.clear();
        world.pilot.cancel();
        world.shake = 0f;
        session.markActive();
        return true;
    }

    private static int clampSize(int size) {
        return size < Asteroid.SMALL ? Asteroid.SMALL
                : size > Asteroid.LARGE ? Asteroid.LARGE : size;
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : value > max ? max : value;
    }

    private static final class Data {
        String playerId;
        String profileId;
        String profileName;
        int coins;
        int refills;
        int[] levels;
        String[] modules;
        float health;
        float maxHealth;
        float oxygen;
        float o2Max;
        String status;
        boolean aboard;
        float grace = Pilot.O2_GRACE_SECONDS;
        float filter = LifeSupport.FILTER_MAX;
        float leak;
        boolean breach;
        float x;
        float y;
        float vx;
        float vy;
        float heading;
        float fuel;
        float hull;
        float invulnerable;
        float[] parts;
        int coinsEarned;
        int score;
        float elapsed;
        boolean legacyWreck;
        float legacyX;
        float legacyY;
        float legacyHeading;
        float legacyHull;
        final float[] legacyParts = new float[Ship.PART_COUNT];
        long[] chunks;
        final List<AsteroidData> asteroids = new ArrayList<>();
        final List<CargoData> cargo = new ArrayList<>();
        final List<PickupData> pickups = new ArrayList<>();
    }

    private static final class CargoData {
        final Commodity commodity;
        final int amount;

        CargoData(Commodity commodity, int amount) {
            this.commodity = commodity;
            this.amount = amount;
        }
    }

    private static final class AsteroidData {
        float x;
        float y;
        float vx;
        float vy;
        int size;
        int hp;
        AsteroidType type;
        int reserve;
        int maxReserve;
    }

    private static final class PickupData {
        float x;
        float y;
        float life;
    }
}

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
            w.set("docked", ship.docked);
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
            w.set("sectorId", session.currentSector == null
                    ? GameData.START_SECTOR_ID : session.currentSector.id);
            w.set("coinsEarned", world.coinsEarned);
            w.set("score", world.score);
            w.set("elapsed", world.elapsed);
            w.set("lastSimTime", System.currentTimeMillis());
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
            w.name("fragments").array();
            for (int i = 0; i < world.fragments.size; i++) {
                OreFragment o = world.fragments.get(i);
                w.object();
                w.set("x", o.pos.x);
                w.set("y", o.pos.y);
                w.set("vx", o.vel.x);
                w.set("vy", o.vel.y);
                w.set("commodity", o.commodity.name());
                w.set("amount", o.amount);
                w.set("life", o.life());
                w.pop();
            }
            w.pop();
            w.name("station").object();
            w.set("x", world.station.pos.x);
            w.set("y", world.station.pos.y);
            w.set("name", world.station.def.name);
            w.name("storage").array();
            for (Cargo cargo : world.station.storage.hold.entries()) {
                w.object();
                w.set("commodity", cargo.commodity.name());
                w.set("amount", cargo.amount);
                w.pop();
            }
            w.pop();
            w.name("market").object();
            w.set("spread", world.station.market.spread());
            w.name("stock").array();
            for (Commodity commodity : Commodity.values()) {
                w.value(world.station.market.stock(commodity));
            }
            w.pop();
            w.name("target").array();
            for (Commodity commodity : Commodity.values()) {
                w.value(world.station.market.target(commodity));
            }
            w.pop();
            w.name("production").array();
            for (Commodity commodity : Commodity.values()) {
                w.value(world.station.market.production(commodity));
            }
            w.pop();
            w.name("consumption").array();
            for (Commodity commodity : Commodity.values()) {
                w.value(world.station.market.consumption(commodity));
            }
            w.pop();
            w.pop();
            w.pop();
            w.name("agents").array();
            for (int i = 0; i < world.agents.size(); i++) {
                PilotAgent agent = world.agents.get(i);
                w.object();
                w.set("id", agent.id);
                w.set("name", agent.name);
                w.set("role", agent.role.name());
                w.set("state", agent.state.name());
                w.set("fuel", agent.fuel);
                w.set("maxFuel", agent.maxFuel);
                w.set("credits", agent.credits);
                w.set("commodity", agent.commodity.name());
                w.set("timer", agent.timer);
                w.set("objective", agent.objective);
                w.name("cargo").array();
                for (Cargo cargo : agent.cargo.entries()) {
                    w.object();
                    w.set("commodity", cargo.commodity.name());
                    w.set("amount", cargo.amount);
                    w.pop();
                }
                w.pop();
                w.pop();
            }
            w.pop();
            w.name("playerStation").object();
            w.set("built", session.playerStation.built());
            w.set("x", session.playerStation.pos.x);
            w.set("y", session.playerStation.pos.y);
            w.set("sectorId", session.playerStation.sectorId());
            w.name("modules").array();
            for (int i = 0; i < session.playerStation.slotCount(); i++) {
                StationModule module = session.playerStation.moduleAt(i);
                if (module == null) {
                    continue;
                }
                w.object();
                w.set("slot", i);
                w.set("def", module.def.name());
                w.set("level", module.level());
                w.pop();
            }
            w.pop();
            w.pop();
            w.name("sectorSnapshots").array();
            for (SectorRuntime.Snapshot snapshot : session.universe.snapshots()) {
                writeSnapshot(w, snapshot);
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
            d.docked = ship.getBoolean("docked", false);
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

            d.sectorId = sector.getString("sectorId", GameData.START_SECTOR_ID);
            d.coinsEarned = sector.getInt("coinsEarned", 0);
            d.score = sector.getInt("score", 0);
            d.elapsed = sector.getFloat("elapsed", 0f);
            d.lastSimTime = sector.getLong("lastSimTime", 0L);
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
            JsonValue fragments = sector.get("fragments");
            if (fragments != null && fragments.isArray()) {
                for (int i = 0; i < fragments.size; i++) {
                    JsonValue fragment = fragments.get(i);
                    if (fragment == null) {
                        continue;
                    }
                    Commodity commodity = Commodity.byDisplayName(
                            fragment.getString("commodity", null));
                    int amount = fragment.getInt("amount", 0);
                    if (commodity == null || amount <= 0) {
                        continue;
                    }
                    FragmentData o = new FragmentData();
                    o.x = fragment.getFloat("x", 0f);
                    o.y = fragment.getFloat("y", 0f);
                    o.vx = fragment.getFloat("vx", 0f);
                    o.vy = fragment.getFloat("vy", 0f);
                    o.commodity = commodity;
                    o.amount = amount;
                    o.life = fragment.getFloat("life", OreFragment.LIFE);
                    d.fragments.add(o);
                }
            }
            JsonValue station = sector.get("station");
            if (station != null) {
                d.stationX = station.getFloat("x", GameData.MOTHERSHIP_X);
                d.stationY = station.getFloat("y", GameData.MOTHERSHIP_Y);
                d.stationName = station.getString("name", GameData.MOTHERSHIP.name);
                JsonValue storage = station.get("storage");
                if (storage != null && storage.isArray()) {
                    for (int i = 0; i < storage.size; i++) {
                        JsonValue entry = storage.get(i);
                        if (entry == null) {
                            continue;
                        }
                        Commodity commodity = Commodity.byDisplayName(
                                entry.getString("commodity", null));
                        int amount = entry.getInt("amount", 0);
                        if (commodity != null && amount > 0) {
                            d.stationStorage.add(new CargoData(commodity, amount));
                        }
                    }
                }
                JsonValue agents = sector.get("agents");
                if (agents != null && agents.isArray()) {
                    d.hasAgents = true;
                    for (int i = 0; i < agents.size; i++) {
                        JsonValue a = agents.get(i);
                        if (a == null) {
                            continue;
                        }
                        AgentData data = new AgentData();
                        data.id = a.getString("id", "M-" + i);
                        data.name = a.getString("name", data.id);
                        data.role = PilotAgent.Role.valueOf(
                                a.getString("role", PilotAgent.Role.MINER.name()));
                        data.state = PilotAgent.State.valueOf(
                                a.getString("state", PilotAgent.State.DOCKED.name()));
                        data.fuel = a.getFloat("fuel", AgentRoster.MAX_FUEL);
                        data.maxFuel = a.getFloat("maxFuel", AgentRoster.MAX_FUEL);
                        data.credits = a.getInt("credits", 200);
                        data.commodity = Commodity.byDisplayName(
                                a.getString("commodity", Commodity.SILICATO.name()));
                        data.timer = a.getFloat("timer", 0f);
                        data.objective = a.getString("objective", "NODRIZA");
                        JsonValue agentCargo = a.get("cargo");
                        if (agentCargo != null && agentCargo.isArray()) {
                            for (int k = 0; k < agentCargo.size; k++) {
                                JsonValue entry = agentCargo.get(k);
                                if (entry == null) {
                                    continue;
                                }
                                Commodity commodity = Commodity.byDisplayName(
                                        entry.getString("commodity", null));
                                int amount = entry.getInt("amount", 0);
                                if (commodity != null && amount > 0) {
                                    data.cargo.add(new CargoData(commodity, amount));
                                }
                            }
                        }
                        d.agents.add(data);
                    }
                }
                JsonValue market = station.get("market");
                if (market != null) {
                    d.hasMarket = true;
                    d.marketSpread = market.getFloat("spread",
                            PriceModel.DEFAULT_SPREAD);
                    d.marketStock = intArray(market.get("stock"));
                    d.marketTarget = intArray(market.get("target"));
                    d.marketProduction = intArray(market.get("production"));
                    d.marketConsumption = intArray(market.get("consumption"));
                }
            }
            JsonValue playerStation = sector.get("playerStation");
            if (playerStation != null) {
                d.hasPlayerStation = true;
                d.playerStationBuilt = playerStation.getBoolean("built", false);
                d.playerStationX = playerStation.getFloat("x", 0f);
                d.playerStationY = playerStation.getFloat("y", 0f);
                d.playerStationSector = playerStation.getString("sectorId",
                        GameData.START_SECTOR_ID);
                JsonValue stationModules = playerStation.get("modules");
                if (stationModules != null && stationModules.isArray()) {
                    for (int i = 0; i < stationModules.size; i++) {
                        JsonValue value = stationModules.get(i);
                        if (value == null) {
                            continue;
                        }
                        ModuleDef def = ModuleDef.byId(value.getString("def", null));
                        if (def == null) {
                            continue;
                        }
                        StationModuleData data = new StationModuleData();
                        data.slot = value.getInt("slot", -1);
                        data.def = def;
                        data.level = value.getInt("level", 1);
                        d.stationModules.add(data);
                    }
                }
            }
            JsonValue snapList = sector.get("sectorSnapshots");
            if (snapList != null && snapList.isArray()) {
                for (int i = 0; i < snapList.size; i++) {
                    JsonValue value = snapList.get(i);
                    if (value == null) {
                        continue;
                    }
                    String snapshotId = value.getString("id", null);
                    if (snapshotId == null || snapshotId.isEmpty()) {
                        continue;
                    }
                    SectorRuntime.Snapshot snapshot =
                            new SectorRuntime.Snapshot(snapshotId);
                    JsonValue snapChunks = value.get("chunks");
                    if (snapChunks != null && snapChunks.isArray()) {
                        for (int k = 0; k < snapChunks.size; k++) {
                            snapshot.chunks.add(snapChunks.get(k).asLong());
                        }
                    }
                    JsonValue snapRocks = value.get("asteroids");
                    if (snapRocks != null && snapRocks.isArray()) {
                        for (int k = 0; k < snapRocks.size; k++) {
                            JsonValue rock = snapRocks.get(k);
                            if (rock == null) {
                                continue;
                            }
                            SectorRuntime.Rock data = new SectorRuntime.Rock();
                            data.x = rock.getFloat("x", 0f);
                            data.y = rock.getFloat("y", 0f);
                            data.vx = rock.getFloat("vx", 0f);
                            data.vy = rock.getFloat("vy", 0f);
                            data.size = rock.getInt("size", Asteroid.SMALL);
                            data.hp = rock.getInt("hp", 1);
                            data.type = AsteroidType.byName(
                                    rock.getString("type", AsteroidType.C.name()));
                            data.reserve = rock.getInt("reserve", -1);
                            data.maxReserve = rock.getInt("maxReserve", data.reserve);
                            data.seed = rock.getLong("seed", 1L);
                            snapshot.asteroids.add(data);
                        }
                    }
                    JsonValue snapPickups = value.get("pickups");
                    if (snapPickups != null && snapPickups.isArray()) {
                        for (int k = 0; k < snapPickups.size; k++) {
                            JsonValue pickup = snapPickups.get(k);
                            if (pickup == null) {
                                continue;
                            }
                            SectorRuntime.Fuel data = new SectorRuntime.Fuel();
                            data.x = pickup.getFloat("x", 0f);
                            data.y = pickup.getFloat("y", 0f);
                            data.life = pickup.getFloat("life", FuelPickup.LIFE);
                            snapshot.fuel.add(data);
                        }
                    }
                    d.snapshots.add(snapshot);
                }
            }
            return d;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void writeSnapshot(com.badlogic.gdx.utils.JsonWriter w,
                                      SectorRuntime.Snapshot snapshot)
            throws IOException {
        w.object();
        w.set("id", snapshot.sectorId);
        w.name("chunks").array();
        for (int i = 0; i < snapshot.chunks.size(); i++) {
            w.value(snapshot.chunks.get(i));
        }
        w.pop();
        w.name("asteroids").array();
        for (int i = 0; i < snapshot.asteroids.size(); i++) {
            SectorRuntime.Rock rock = snapshot.asteroids.get(i);
            w.object();
            w.set("x", rock.x);
            w.set("y", rock.y);
            w.set("vx", rock.vx);
            w.set("vy", rock.vy);
            w.set("size", rock.size);
            w.set("hp", rock.hp);
            w.set("type", rock.type.name());
            w.set("reserve", rock.reserve);
            w.set("maxReserve", rock.maxReserve);
            w.set("seed", rock.seed);
            w.pop();
        }
        w.pop();
        w.name("pickups").array();
        for (int i = 0; i < snapshot.fuel.size(); i++) {
            SectorRuntime.Fuel fuel = snapshot.fuel.get(i);
            w.object();
            w.set("x", fuel.x);
            w.set("y", fuel.y);
            w.set("life", fuel.life);
            w.pop();
        }
        w.pop();
        w.pop();
    }

    private static int[] intArray(JsonValue array) {
        if (array == null || !array.isArray()) {
            return null;
        }
        int[] values = new int[array.size];
        for (int i = 0; i < array.size; i++) {
            values[i] = array.get(i).asInt();
        }
        return values;
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

        if (d.sectorId != null && !d.sectorId.isEmpty()
                && (session.currentSector == null
                || !d.sectorId.equals(session.currentSector.id))) {
            session.switchSector(d.sectorId);
        }
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
        world.fragments.clear();
        for (int i = 0; i < d.fragments.size(); i++) {
            FragmentData o = d.fragments.get(i);
            world.fragments.add(new OreFragment(o.x, o.y, o.commodity, o.amount,
                    o.vx, o.vy, Math.max(0.01f, o.life)));
        }
        world.station.pos.set(d.stationX, d.stationY);
        world.station.storage.hold.clear();
        for (int i = 0; i < d.stationStorage.size(); i++) {
            CargoData entry = d.stationStorage.get(i);
            world.station.storage.hold.restore(entry.commodity, entry.amount);
        }
        if (d.hasAgents) {
            world.agents.clear();
            for (int i = 0; i < d.agents.size(); i++) {
                AgentData data = d.agents.get(i);
                PilotAgent agent = new PilotAgent(data.id, data.name, data.role,
                        AgentRoster.CARGO_CAPACITY, data.maxFuel,
                        AgentRoster.SPEED, AgentRoster.MINING_RATE);
                agent.state = data.state;
                agent.fuel = Math.max(0f, Math.min(data.fuel, data.maxFuel));
                agent.credits = Math.max(0, data.credits);
                if (data.commodity != null) {
                    agent.commodity = data.commodity;
                }
                agent.timer = Math.max(0f, data.timer);
                agent.objective = data.objective;
                agent.cargo.clear();
                for (int k = 0; k < data.cargo.size(); k++) {
                    CargoData entry = data.cargo.get(k);
                    agent.cargo.restore(entry.commodity, entry.amount);
                }
                world.agents.add(agent);
            }
        }
        if (d.hasMarket) {
            world.station.market.setSpread(d.marketSpread);
            for (int i = 0; i < Commodity.values().length; i++) {
                Commodity commodity = Commodity.values()[i];
                if (d.marketStock != null && i < d.marketStock.length) {
                    world.station.market.setStock(commodity, d.marketStock[i]);
                }
                if (d.marketTarget != null && i < d.marketTarget.length) {
                    world.station.market.setTarget(commodity, d.marketTarget[i]);
                }
                if (d.marketProduction != null && i < d.marketProduction.length) {
                    world.station.market.setProduction(commodity, d.marketProduction[i]);
                }
                if (d.marketConsumption != null && i < d.marketConsumption.length) {
                    world.station.market.setConsumption(commodity, d.marketConsumption[i]);
                }
            }
        }
        world.ship.docked = d.docked;
        if (d.lastSimTime > 0L) {
            float seconds = Math.max(0f,
                    (System.currentTimeMillis() - d.lastSimTime) / 1000f);
            if (seconds >= SimClock.OFFLINE_SECONDS) {
                world.economy.updateOffline(seconds, world);
            }
        }
        world.missiles.clear();
        world.fx.clear();
        world.pilot.cancel();
        world.shake = 0f;
        if (d.hasPlayerStation) {
            session.playerStation.restore(d.playerStationBuilt,
                    d.playerStationX, d.playerStationY, d.playerStationSector);
            for (int i = 0; i < d.stationModules.size(); i++) {
                StationModuleData module = d.stationModules.get(i);
                session.playerStation.restoreModule(module.slot, module.def,
                        module.level);
            }
        }
        session.universe.clearSnapshots();
        for (int i = 0; i < d.snapshots.size(); i++) {
            session.universe.putSnapshot(d.snapshots.get(i));
        }
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
        String sectorId = GameData.START_SECTOR_ID;
        int coinsEarned;
        int score;
        float elapsed;
        long lastSimTime;
        boolean docked;
        float stationX = GameData.MOTHERSHIP_X;
        float stationY = GameData.MOTHERSHIP_Y;
        String stationName = GameData.MOTHERSHIP.name;
        final List<CargoData> stationStorage = new ArrayList<>();
        boolean hasAgents;
        final List<AgentData> agents = new ArrayList<>();
        final List<SectorRuntime.Snapshot> snapshots = new ArrayList<>();
        boolean hasPlayerStation;
        boolean playerStationBuilt;
        float playerStationX;
        float playerStationY;
        String playerStationSector = GameData.START_SECTOR_ID;
        final List<StationModuleData> stationModules = new ArrayList<>();
        boolean hasMarket;
        float marketSpread = PriceModel.DEFAULT_SPREAD;
        int[] marketStock;
        int[] marketTarget;
        int[] marketProduction;
        int[] marketConsumption;
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
        final List<FragmentData> fragments = new ArrayList<>();
    }

    private static final class FragmentData {
        float x;
        float y;
        float vx;
        float vy;
        Commodity commodity;
        int amount;
        float life = OreFragment.LIFE;
    }

    private static final class AgentData {
        String id;
        String name;
        PilotAgent.Role role = PilotAgent.Role.MINER;
        PilotAgent.State state = PilotAgent.State.DOCKED;
        float fuel = AgentRoster.MAX_FUEL;
        float maxFuel = AgentRoster.MAX_FUEL;
        int credits = 200;
        Commodity commodity = Commodity.SILICATO;
        float timer;
        String objective = "NODRIZA";
        final List<CargoData> cargo = new ArrayList<>();
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

    private static final class StationModuleData {
        int slot;
        ModuleDef def;
        int level = 1;
    }

    private static final class PickupData {
        float x;
        float y;
        float life;
    }
}

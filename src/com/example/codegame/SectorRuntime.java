package com.example.codegame;

import java.util.ArrayList;
import java.util.List;

/**
 * Captura y restaura el estado resumido de un sector. No se guardan los chunks
 * no visitados: solo la lista de chunks ya generados y el estado de los
 * asteroides/bidones que siguen activos. Al volver, los chunks no visitados se
 * generan de nuevo desde la semilla, así que los recursos minados no reaparecen
 * y el guardado no crece con el tamaño total del sector.
 */
final class SectorRuntime {

    private SectorRuntime() {
    }

    /** Estado mínimo de un sector visitado. */
    static final class Snapshot {
        final String sectorId;
        final List<Long> chunks = new ArrayList<>();
        final List<Rock> asteroids = new ArrayList<>();
        final List<Fuel> fuel = new ArrayList<>();

        Snapshot(String sectorId) {
            this.sectorId = sectorId;
        }
    }

    static final class Rock {
        float x;
        float y;
        float vx;
        float vy;
        int size;
        int hp;
        AsteroidType type = AsteroidType.C;
        int reserve;
        int maxReserve;
        long seed;
    }

    static final class Fuel {
        float x;
        float y;
        float life;
    }

    static Snapshot capture(GameWorld world, String sectorId) {
        Snapshot snapshot = new Snapshot(sectorId);
        if (world == null) {
            return snapshot;
        }
        snapshot.chunks.addAll(world.generatedChunks());
        for (int i = 0; i < world.asteroids.size; i++) {
            Asteroid asteroid = world.asteroids.get(i);
            Rock rock = new Rock();
            rock.x = asteroid.pos.x;
            rock.y = asteroid.pos.y;
            rock.vx = asteroid.vel.x;
            rock.vy = asteroid.vel.y;
            rock.size = asteroid.size;
            rock.hp = asteroid.hp;
            rock.type = asteroid.type;
            rock.reserve = asteroid.reserve;
            rock.maxReserve = asteroid.maxReserve;
            rock.seed = asteroid.seed;
            snapshot.asteroids.add(rock);
        }
        for (int i = 0; i < world.fuel.size; i++) {
            FuelPickup pickup = world.fuel.get(i);
            Fuel fuel = new Fuel();
            fuel.x = pickup.pos.x;
            fuel.y = pickup.pos.y;
            fuel.life = pickup.life();
            snapshot.fuel.add(fuel);
        }
        return snapshot;
    }

    /** Restaura un snapshot en un mundo del mismo sector. */
    static void restore(GameWorld world, Snapshot snapshot) {
        if (world == null || snapshot == null) {
            return;
        }
        world.asteroids.clear();
        world.fuel.clear();
        world.fragments.clear();
        for (int i = 0; i < snapshot.asteroids.size(); i++) {
            Rock rock = snapshot.asteroids.get(i);
            Asteroid asteroid = new Asteroid(rock.x, rock.y, rock.size, rock.type,
                    rock.reserve, rock.maxReserve, rock.seed);
            asteroid.pos.set(rock.x, rock.y);
            asteroid.vel.set(rock.vx, rock.vy);
            asteroid.hp = rock.hp;
            world.asteroids.add(asteroid);
        }
        for (int i = 0; i < snapshot.fuel.size(); i++) {
            Fuel fuel = snapshot.fuel.get(i);
            world.fuel.add(new FuelPickup(fuel.x, fuel.y, Math.max(0.01f, fuel.life)));
        }
        world.generatedChunks().clear();
        world.generatedChunks().addAll(snapshot.chunks);
    }

    /** Suma de reserva guardada; se usa para comprobar que no se duplica. */
    static int reserveUnits(Snapshot snapshot) {
        int total = 0;
        if (snapshot != null) {
            for (int i = 0; i < snapshot.asteroids.size(); i++) {
                total += snapshot.asteroids.get(i).reserve;
            }
        }
        return total;
    }
}

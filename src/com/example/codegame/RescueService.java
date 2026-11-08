package com.example.codegame;

/**
 * Rescate por falta de combustible: evita el softlock sin resucitar al piloto.
 * Tiene un tiempo de espera, cobra créditos y provoca pérdida de carga; si no
 * hay saldo suficiente, la pérdida es mayor. Deja la nave junto a la estación
 * con combustible de emergencia para poder acoplar.
 */
final class RescueService {

    static final float FUEL_THRESHOLD = 0.5f;
    static final float DELAY = 3f;
    static final int CREDIT_COST = 100;
    static final float CARGO_LOSS = 0.25f;
    static final float LATE_CARGO_LOSS = 0.50f;
    static final float RESCUE_FUEL = 20f;
    /** Distancia extra fuera del radio de acople al dejar la nave. */
    static final float TOW_GAP = 40f;

    private boolean active;
    private float timer;
    private int paid;
    private int lost;
    private boolean fullPayment;

    boolean active() {
        return active;
    }

    float remaining() {
        return Math.max(0f, timer);
    }

    int paid() {
        return paid;
    }

    int lost() {
        return lost;
    }

    boolean fullPayment() {
        return fullPayment;
    }

    static boolean needsRescue(GameWorld world) {
        return world != null && world.ship != null
                && !world.ship.docked
                && world.ship.fuel <= FUEL_THRESHOLD;
    }

    /** Se puede pedir si falta combustible y no se está ya en la zona de acople. */
    static boolean canRequest(GameWorld world, Pilot pilot) {
        return needsRescue(world)
                && (pilot == null || pilot.alive())
                && !DockingSystem.canDock(world);
    }

    boolean request(GameWorld world, Pilot pilot) {
        if (active || !canRequest(world, pilot)) {
            return false;
        }
        active = true;
        timer = DELAY;
        paid = 0;
        lost = 0;
        fullPayment = false;
        return true;
    }

    void cancel() {
        active = false;
        timer = 0f;
    }

    /**
     * Avanza la espera. Devuelve true cuando el rescate se ha completado en
     * este fotograma. Si el piloto muere, la nave se acopla o llega combustible,
     * se cancela.
     */
    boolean update(float dt, GameWorld world, Upgrades upgrades, Pilot pilot) {
        if (!active) {
            return false;
        }
        if (world == null || world.ship.docked || (pilot != null && !pilot.alive())) {
            cancel();
            return false;
        }
        if (world.ship.fuel > FUEL_THRESHOLD) {
            cancel();
            return false;
        }
        timer -= dt;
        if (timer > 0f) {
            return false;
        }
        apply(world, upgrades);
        active = false;
        timer = 0f;
        return true;
    }

    private void apply(GameWorld world, Upgrades upgrades) {
        int available = upgrades == null ? 0 : upgrades.coins();
        fullPayment = available >= CREDIT_COST;
        paid = fullPayment ? CREDIT_COST : Math.max(0, available);
        if (paid > 0 && upgrades != null) {
            upgrades.spendCoins(paid);
        }
        lost = loseCargo(world.ship.cargo(), fullPayment ? CARGO_LOSS : LATE_CARGO_LOSS);
        world.ship.pos.set(world.station.pos.x,
                world.station.pos.y - (world.station.def.dockRadius + TOW_GAP));
        world.ship.vel.setZero();
        world.ship.heading = 90f;
        world.ship.dir.set(0f, 1f);
        world.ship.fuel = Math.min(world.ship.maxFuel, RESCUE_FUEL);
        world.ship.thrusting = false;
        world.ship.laser.stop();
        world.ship.tractor.stop();
        world.pilot.cancel();
    }

    /** Descarta hasta {@code fraction} de las unidades de la bodega. */
    static int loseCargo(CargoHold cargo, float fraction) {
        if (cargo == null || cargo.isEmpty() || fraction <= 0f) {
            return 0;
        }
        int total = cargo.totalUnits();
        int toRemove = Math.min(total, (int) Math.ceil(total * fraction));
        int removed = 0;
        for (Cargo entry : cargo.entries()) {
            int take = Math.min(toRemove - removed, entry.amount);
            if (take <= 0) {
                break;
            }
            cargo.removeExact(entry.commodity, take);
            removed += take;
            if (removed >= toRemove) {
                break;
            }
        }
        return removed;
    }
}

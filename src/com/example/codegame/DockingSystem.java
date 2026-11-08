package com.example.codegame;

/**
 * Acople de la nave a una estación: exige proximidad y velocidad baja, detiene
 * la nave y deja constancia del estado DOCKED. El desacople la devuelve al
 * exterior con velocidad cero.
 */
final class DockingSystem {

    /** Velocidad máxima para iniciar el acople, en u/s. */
    static final float MAX_DOCK_SPEED = 30f;

    private DockingSystem() {
    }

    static boolean canDock(GameWorld world) {
        return world != null && world.station != null && world.ship != null
                && !world.ship.docked
                && world.station.canDock(world.ship.pos, world.ship.vel.len());
    }

    static boolean dock(GameWorld world) {
        if (!canDock(world)) {
            return false;
        }
        world.ship.docked = true;
        world.ship.vel.setZero();
        world.ship.thrusting = false;
        world.ship.laser.stop();
        world.ship.tractor.stop();
        world.pilot.cancel();
        return true;
    }

    static void undock(GameWorld world) {
        if (world == null || world.ship == null) {
            return;
        }
        world.ship.docked = false;
        world.ship.vel.setZero();
        world.ship.thrusting = false;
    }
}

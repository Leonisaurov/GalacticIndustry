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

    static boolean canDockPlayer(GameWorld world) {
        return world != null && world.playerStation != null && world.ship != null
                && !world.ship.docked
                && world.playerStation.inSector(world.sector.id)
                && world.playerStation.canDock(world.ship.pos, world.ship.vel.len());
    }

    static boolean canDock(GameWorld world) {
        return world != null && world.ship != null && !world.ship.docked
                && ((world.station != null
                && world.station.canDock(world.ship.pos, world.ship.vel.len()))
                || canDockPlayer(world));
    }

    static boolean dock(GameWorld world) {
        if (!canDock(world)) {
            return false;
        }
        boolean player = canDockPlayer(world);
        world.dockedAtPlayer = player;
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
        world.dockedAtPlayer = false;
        world.ship.vel.setZero();
        world.ship.thrusting = false;
    }
}

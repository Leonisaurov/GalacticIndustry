package com.example.codegame;

/**
 * Acople de la nave a una estación: exige proximidad a la boca, velocidad baja,
 * orientación alineada y estar dentro del corredor de entrada (UPD-006-T02).
 * La plataforma personal conserva su comportamiento histórico de radio.
 * El desacople devuelve la nave al exterior con velocidad cero.
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
                && world.station.canDock(world.ship.pos, world.ship.vel.len(),
                world.ship.heading))
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
        if (!player && world.station != null) {
            world.station.snapToDock(world.ship.pos);
        }
        return true;
    }

    static void undock(GameWorld world) {
        if (world == null || world.ship == null) {
            return;
        }
        boolean player = world.dockedAtPlayer;
        world.ship.docked = false;
        world.dockedAtPlayer = false;
        world.ship.vel.setZero();
        world.ship.thrusting = false;
        if (!player && world.station != null
                && !world.station.inDockCorridor(world.ship.pos)) {
            // Al despegar se sale por la boca, nunca desde dentro del casco.
            world.ship.pos.set(world.station.dockPoint);
        }
    }
}

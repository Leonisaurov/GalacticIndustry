package com.example.codegame;

/**
 * Construcción y colocación del núcleo de la estación personal. La plataforma
 * cuesta créditos y materiales, no puede solaparse con la nodriza ni salir del
 * sector, y nace vacía: sin almacén, reparación, refinería ni mercado.
 */
final class ConstructionService {

    static final int CORE_CREDITS = 300;
    static final int CORE_HIERRO = 25;
    static final int CORE_COBRE = 10;
    static final int CORE_SILICATO = 20;
    static final float STATION_CLEARANCE = 200f;
    static final float BORDER_MARGIN = 180f;

    static final class Result {
        final boolean ok;
        final String error;

        private Result(boolean ok, String error) {
            this.ok = ok;
            this.error = error;
        }
    }

    private ConstructionService() {
    }

    static Result canBuild(GameSession session, float x, float y) {
        if (session == null || session.playerStation == null) {
            return new Result(false, "SIN ESTACION");
        }
        if (session.playerStation.built()) {
            return new Result(false, "YA EXISTE UNA BASE");
        }
        GameWorld world = session.sector;
        if (world == null || session.currentSector == null) {
            return new Result(false, "SIN SECTOR");
        }
        if (world.outsideSector(x, y)
                || x < -world.halfWidth() + BORDER_MARGIN
                || x > world.halfWidth() - BORDER_MARGIN
                || y < -world.halfHeight() + BORDER_MARGIN
                || y > world.halfHeight() - BORDER_MARGIN) {
            return new Result(false, "FUERA DEL SECTOR");
        }
        if (world.station != null
                && world.station.distanceTo(new com.badlogic.gdx.math.Vector2(x, y))
                < world.station.def.dockRadius + STATION_CLEARANCE) {
            return new Result(false, "DEMASIADO CERCA DE LA NODRIZA");
        }
        if (session.upgrades().coins() < CORE_CREDITS) {
            return new Result(false, "FALTAN CREDITOS");
        }
        CargoHold cargo = session.ship().cargo();
        if (cargo.amount(Commodity.HIERRO) < CORE_HIERRO
                || cargo.amount(Commodity.COBRE) < CORE_COBRE
                || cargo.amount(Commodity.SILICATO) < CORE_SILICATO) {
            return new Result(false, "FALTAN MATERIALES");
        }
        return new Result(true, "");
    }

    static Result buildCore(GameSession session, float x, float y) {
        Result check = canBuild(session, x, y);
        if (!check.ok) {
            return check;
        }
        session.upgrades().spendCoins(CORE_CREDITS);
        session.ship().cargo().removeExact(Commodity.HIERRO, CORE_HIERRO);
        session.ship().cargo().removeExact(Commodity.COBRE, CORE_COBRE);
        session.ship().cargo().removeExact(Commodity.SILICATO, CORE_SILICATO);
        session.playerStation.place(x, y, session.currentSector.id);
        return new Result(true, "");
    }

    static boolean built(GameSession session) {
        return session != null && session.playerStation != null
                && session.playerStation.built();
    }
}

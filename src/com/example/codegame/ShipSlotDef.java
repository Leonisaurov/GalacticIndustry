package com.example.codegame;

/**
 * Ranura visual y de compatibilidad de un módulo (UPD-006-T23).
 *
 * <p>Las cuatro ranuras históricas se mapean 1:1 a {@code slot-0..3}; la
 * posición es un dato de presentación sobre la silueta de la nave. Las clases
 * de nave pueden reordenar los tipos, pero nunca cambian el número ni el orden
 * persistente de las ranuras.</p>
 */
final class ShipSlotDef {

    /** Tipo de montaje de un módulo y de una ranura. */
    enum Mount {
        NAV,
        SENSOR,
        MINING,
        PROPULSION,
        UTILITY,
        UNIVERSAL
    }

    final int index;
    final String id;
    final Mount mount;
    /** Posición relativa sobre la silueta (unidades de tamaño de nave). */
    final float localX;
    final float localY;
    final String label;

    ShipSlotDef(int index, Mount mount, float localX, float localY, String label) {
        this.index = index;
        this.id = "slot-" + index;
        this.mount = mount == null ? Mount.UNIVERSAL : mount;
        this.localX = localX;
        this.localY = localY;
        this.label = label;
    }

    /** Compatibilidad: la ranura universal acepta todo y el tipo debe coincidir. */
    boolean accepts(Mount moduleMount) {
        if (moduleMount == null) {
            return false;
        }
        return mount == Mount.UNIVERSAL || moduleMount == Mount.UNIVERSAL
                || mount == moduleMount
                // El sensor y la navegación comparten espacio físico en la cabina.
                || (mount == Mount.SENSOR && moduleMount == Mount.NAV)
                || (mount == Mount.NAV && moduleMount == Mount.SENSOR);
    }

    boolean accepts(ShipModuleDef module) {
        return module != null && accepts(module.mountType);
    }

    /** Disposición por clase: cuatro posiciones persistentes. */
    static ShipSlotDef[] forClass(ShipClassDef shipClass) {
        if (shipClass == ShipClassDef.EXPLORER) {
            return EXPLORER;
        }
        if (shipClass == ShipClassDef.HAULER) {
            return HAULER;
        }
        return MINER;
    }

    static ShipSlotDef slotFor(ShipClassDef shipClass, int index) {
        ShipSlotDef[] slots = forClass(shipClass);
        return index >= 0 && index < slots.length ? slots[index] : null;
    }

    private static final ShipSlotDef[] MINER = {
            new ShipSlotDef(0, Mount.NAV, 0.18f, 0.02f, "NAV"),
            new ShipSlotDef(1, Mount.MINING, 0.30f, 0.30f, "MIN"),
            new ShipSlotDef(2, Mount.PROPULSION, -0.34f, 0.00f, "PROP"),
            new ShipSlotDef(3, Mount.UNIVERSAL, 0.10f, -0.30f, "UTIL")
    };

    private static final ShipSlotDef[] EXPLORER = {
            new ShipSlotDef(0, Mount.NAV, 0.20f, 0.00f, "NAV"),
            new ShipSlotDef(1, Mount.SENSOR, 0.34f, 0.16f, "SENSOR"),
            new ShipSlotDef(2, Mount.PROPULSION, -0.34f, 0.00f, "PROP"),
            new ShipSlotDef(3, Mount.UTILITY, 0.08f, -0.28f, "UTIL")
    };

    private static final ShipSlotDef[] HAULER = {
            new ShipSlotDef(0, Mount.NAV, 0.14f, 0.00f, "NAV"),
            new ShipSlotDef(1, Mount.UTILITY, 0.30f, 0.30f, "UTIL"),
            new ShipSlotDef(2, Mount.PROPULSION, -0.34f, 0.00f, "PROP"),
            new ShipSlotDef(3, Mount.UTILITY, 0.10f, -0.30f, "CARGA")
    };
}

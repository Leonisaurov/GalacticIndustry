package com.example.codegame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mercado de naves (UPD-006-T36): chatarra, usadas y nuevas, con datos reales y
 * generación determinista por semilla. Las ofertas se generan al abrir el
 * mercado, no por frame.
 */
final class ShipMarket {

    static final int SCRAP_COUNT = 2;
    static final int USED_COUNT = 2;
    static final int NEW_COUNT = 2;

    private final long seed;
    private final List<ShipOffer> offers = new ArrayList<>();
    private int refresh;

    ShipMarket(long seed) {
        this.seed = seed;
        generate();
    }

    ShipMarket() {
        this(0x5A17C0DEL);
    }

    List<ShipOffer> offers() {
        return offers;
    }

    int size() {
        return offers.size();
    }

    ShipOffer offer(int index) {
        return index >= 0 && index < offers.size() ? offers.get(index) : null;
    }

    ShipOffer cheapest() {
        ShipOffer best = null;
        for (int i = 0; i < offers.size(); i++) {
            if (best == null || offers.get(i).spec.price < best.spec.price) {
                best = offers.get(i);
            }
        }
        return best;
    }

    /** Regenera el mercado con la misma semilla y una variación estable. */
    void regenerate() {
        refresh++;
        generate();
    }

    private void generate() {
        offers.clear();
        Random rnd = new Random(seed + refresh * 7919L);
        addScrap(rnd, 0);
        addScrap(rnd, 1);
        addUsed(rnd, 0);
        addUsed(rnd, 1);
        addNew(rnd, 0);
        addNew(rnd, 1);
    }

    private void addScrap(Random rnd, int index) {
        ShipClassDef[] classes = {ShipClassDef.MINER, ShipClassDef.HAULER};
        ShipClassDef shipClass = classes[index % classes.length];
        float hull = (90f + 25f * rnd.nextFloat()) * shipClass.hullMultiplier;
        float integrity = 0.32f + 0.28f * rnd.nextFloat();
        float fuel = (105f + 35f * rnd.nextFloat()) * shipClass.fuelMultiplier;
        float efficiency = 1.10f + 0.25f * rnd.nextFloat();
        float mass = 1.02f + 0.18f * rnd.nextFloat();
        float cargo = (80f + 25f * rnd.nextFloat()) * shipClass.cargoMultiplier;
        int price = 240 + rnd.nextInt(150);
        String[] modules = index == 0 ? new String[]{ShipModuleDef.TOOLS.id}
                : new String[0];
        offers.add(new ShipOffer(new ShipSpec("scrap-" + index,
                "CHATARRA " + shipClass.displayName, ShipSpec.Category.SCRAP,
                shipClass, hull, integrity, fuel, efficiency, mass, cargo,
                modules, price,
                "Casco gastado; requiere reparaciones antes de una expedition larga.")));
    }

    private void addUsed(Random rnd, int index) {
        ShipClassDef[] classes = {ShipClassDef.MINER, ShipClassDef.EXPLORER};
        ShipClassDef shipClass = classes[index % classes.length];
        float hull = (120f + 35f * rnd.nextFloat()) * shipClass.hullMultiplier;
        float integrity = 0.62f + 0.28f * rnd.nextFloat();
        float fuel = (140f + 45f * rnd.nextFloat()) * shipClass.fuelMultiplier;
        float efficiency = 0.95f + 0.22f * rnd.nextFloat();
        float mass = 0.95f + 0.18f * rnd.nextFloat();
        float cargo = (100f + 30f * rnd.nextFloat()) * shipClass.cargoMultiplier;
        int price = 480 + rnd.nextInt(230);
        String[] modules = index == 0
                ? new String[]{ShipModuleDef.AUTOPILOT.id}
                : new String[]{ShipModuleDef.MINING.id, ShipModuleDef.TOOLS.id};
        offers.add(new ShipOffer(new ShipSpec("used-" + index,
                "USADA " + shipClass.displayName, ShipSpec.Category.USED,
                shipClass, hull, integrity, fuel, efficiency, mass, cargo,
                modules, price,
                "Revision reciente; estado y desgaste visibles en la ficha.")));
    }

    private void addNew(Random rnd, int index) {
        ShipClassDef[] classes = {ShipClassDef.MINER, ShipClassDef.EXPLORER};
        ShipClassDef shipClass = classes[index % classes.length];
        float hull = 200f * shipClass.hullMultiplier;
        float fuel = (190f + 30f * rnd.nextFloat()) * shipClass.fuelMultiplier;
        float efficiency = 0.86f + 0.10f * rnd.nextFloat();
        float mass = 0.92f + 0.10f * rnd.nextFloat();
        float cargo = 125f * shipClass.cargoMultiplier;
        int price = 860 + rnd.nextInt(220);
        String[] modules = index == 0
                ? new String[]{ShipModuleDef.AUTOPILOT.id, ShipModuleDef.MINING.id}
                : new String[]{ShipModuleDef.AUTOPILOT.id, ShipModuleDef.TOOLS.id,
                ShipModuleDef.DOCKING_AUTOPILOT.id};
        offers.add(new ShipOffer(new ShipSpec("new-" + index,
                "PAQUETE " + shipClass.displayName, ShipSpec.Category.NEW,
                shipClass, hull, 1f, fuel, efficiency, mass, cargo,
                modules, price,
                "Configuracion de comerciante lista para volar; precio con montaje.")));
    }
}

package com.example.codegame;

import java.util.Random;

/**
 * Tipo de asteroide C/S/M con dureza, reserva y composición mineral.
 *
 * <p>La composición es una tabla de fracciones por unidad de reserva; el
 * asteroide concreto guarda su reserva total y la extracción se resolverá en
 * T-402/T-403. No hay recompensa en monedas en el tipo.</p>
 */
enum AsteroidType {

    C("C carbonaceo/helado", 2, 12, 18, 55,
            new Commodity[]{Commodity.HIELO, Commodity.SILICATO, Commodity.HIERRO,
                    Commodity.COBRE},
            new float[]{0.45f, 0.35f, 0.15f, 0.05f}),
    S("S silicaceo", 5, 28, 42, 33,
            new Commodity[]{Commodity.SILICATO, Commodity.HIERRO, Commodity.COBRE,
                    Commodity.TITANIO, Commodity.ORO},
            new float[]{0.40f, 0.30f, 0.20f, 0.08f, 0.02f}),
    M("M metalico", 8, 60, 90, 12,
            new Commodity[]{Commodity.HIERRO, Commodity.COBRE, Commodity.TITANIO,
                    Commodity.ORO},
            new float[]{0.40f, 0.25f, 0.30f, 0.05f});

    final String displayName;
    final int hardness;
    final int minReserve;
    final int maxReserve;
    final int frequency;
    final Commodity[] minerals;
    final float[] fractions;

    AsteroidType(String displayName, int hardness, int minReserve, int maxReserve,
                int frequency, Commodity[] minerals, float[] fractions) {
        if (minerals.length != fractions.length) {
            throw new IllegalArgumentException("composicion desalineada");
        }
        this.displayName = displayName;
        this.hardness = hardness;
        this.minReserve = minReserve;
        this.maxReserve = maxReserve;
        this.frequency = frequency;
        this.minerals = minerals;
        this.fractions = fractions;
    }

    float fraction(Commodity commodity) {
        for (int i = 0; i < minerals.length; i++) {
            if (minerals[i] == commodity) {
                return fractions[i];
            }
        }
        return 0f;
    }

    int rollReserve(Random random) {
        return minReserve + random.nextInt(maxReserve - minReserve + 1);
    }

    static AsteroidType roll(Random random) {
        int roll = random.nextInt(100);
        if (roll < C.frequency) {
            return C;
        }
        if (roll < C.frequency + S.frequency) {
            return S;
        }
        return M;
    }

    static AsteroidType byName(String name) {
        if (name == null) {
            return C;
        }
        for (AsteroidType type : values()) {
            if (type.name().equalsIgnoreCase(name)
                    || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return C;
    }
}

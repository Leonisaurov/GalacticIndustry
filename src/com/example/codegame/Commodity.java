package com.example.codegame;

/**
 * Minerales y materiales comerciables.
 *
 * <p>La masa se mide en unidades de masa (UM) y el precio en créditos por
 * unidad. Los valores son los iniciales del plan maestro y se ajustarán por
 * balance en etapas posteriores.</p>
 */
enum Commodity {

    HIELO("Hielo", 0.80f, 1.5f, "O2 y combustible"),
    SILICATO("Silicato", 1.00f, 2.0f, "Construccion"),
    HIERRO("Hierro", 1.20f, 3.0f, "Reparacion"),
    COBRE("Cobre", 1.50f, 5.0f, "Electronica"),
    TITANIO("Titanio", 1.80f, 12.0f, "Mejoras"),
    ORO("Oro", 2.50f, 30.0f, "Comercio de alto valor");

    final String displayName;
    final float massPerUnit;
    final float basePrice;
    final String use;

    Commodity(String displayName, float massPerUnit, float basePrice, String use) {
        this.displayName = displayName;
        this.massPerUnit = massPerUnit;
        this.basePrice = basePrice;
        this.use = use;
    }

    static Commodity byDisplayName(String name) {
        if (name == null) {
            return null;
        }
        for (Commodity c : values()) {
            if (c.displayName.equalsIgnoreCase(name) || c.name().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

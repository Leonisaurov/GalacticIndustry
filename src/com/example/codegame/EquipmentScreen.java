package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * Pantalla visual de equipamiento (UPD-006-T24/T25): nave central, ranuras
 * tocables, compatibilidad, ficha del componente y previsualización antes de
 * confirmar. Usa el loadout real, no una copia.
 */
final class EquipmentScreen {

    private static final Color PANEL_C = new Color(0.030f, 0.042f, 0.058f, 0.95f);
    private static final Color CARD_C = new Color(0.060f, 0.080f, 0.100f, 0.94f);
    private static final Color CARD_SEL_C = new Color(0.12f, 0.115f, 0.085f, 0.96f);
    private static final Color EDGE_C = new Color(0.24f, 0.33f, 0.40f, 0.50f);

    private final MyGame game;
    private final UiKit ui;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;

    private final Array<Button> rows = new Array<>();
    private Button btnAction, btnBack;
    private int selectedSlot;
    private int selectedModule = -1;
    private LoadoutPreview preview;
    private String message = "";
    private float px, py, pw, ph;
    private float shipCx, shipCy;

    EquipmentScreen(MyGame game, UiKit ui, SpriteBatch batch, ShapeRenderer shapes) {
        this.game = game;
        this.ui = ui;
        this.batch = batch;
        this.shapes = shapes;
        ShipModuleDef[] modules = ShipModuleDef.values();
        for (int i = 0; i < modules.length; i++) {
            rows.add(new Button("", 0f, 0f, 100f, 40f));
        }
    }

    void enter() {
        selectedSlot = 0;
        selectedModule = -1;
        message = "";
        layout();
    }

    void layout() {
        layout(game.hudView.getWorldWidth(), game.hudView.getWorldHeight());
    }

    void layout(float w, float h) {
        float u = game.u;
        float pad = 16f * u;
        px = pad * 0.5f;
        py = pad * 0.5f;
        pw = w - pad;
        ph = h - pad;
        shipCx = w * 0.5f;
        shipCy = h * 0.62f;
        float rowH = Math.min(46f * u, (h * 0.28f) / Math.max(1, rows.size));
        float gap = 5f * u;
        float top = h * 0.38f;
        for (int i = 0; i < rows.size; i++) {
            rows.get(i).bounds.set(px + 22f * u, top - (i + 1) * rowH - i * gap,
                    pw - 44f * u, rowH);
        }
        float bw = Math.min(w * 0.34f, 150f * u);
        btnAction = new Button("", px + 22f * u, py + 16f * u, bw, 50f * u);
        btnBack = new Button("VOLVER", px + 22f * u + bw + 8f * u,
                py + 16f * u, bw, 50f * u);
    }

    boolean handle() {
        if (btnAction == null) {
            layout();
        }
        if (game.tapped(btnBack)) {
            game.closeEquipment();
            return true;
        }
        // Ranuras: puntos sobre la silueta.
        for (int i = 0; i < 4; i++) {
            ShipSlotDef slot = ShipSlotDef.slotFor(game.upgrades.shipClass(), i);
            if (slot == null) {
                continue;
            }
            float sx = shipCx + slot.localX * 300f * game.u;
            float sy = shipCy + slot.localY * 300f * game.u;
            if (tapCircle(sx, sy, 28f * game.u) && selectedSlot != i) {
                selectedSlot = i;
                selectedModule = -1;
                message = "";
                return true;
            }
        }
        for (int i = 0; i < rows.size; i++) {
            if (game.tapped(rows.get(i))) {
                selectedModule = i;
                message = "";
                return true;
            }
        }
        if (game.tapped(btnAction)) {
            act();
            return true;
        }
        return false;
    }

    private void act() {
        ShipModuleDef module = selectedModule();
        if (module == null) {
            message = "ELIGE UN MODULO";
            return;
        }
        int installed = game.shipModuleSlot(module);
        if (installed >= 0) {
            if (game.removeShipModule(installed)) {
                message = "RETIRADO SIN REEMBOLSO";
                selectedSlot = installed;
            } else {
                message = "NO SE PUDO RETIRAR";
            }
            return;
        }
        if (!game.slotAccepts(selectedSlot, module)) {
            message = "RANURA NO COMPATIBLE";
            return;
        }
        if (game.upgrades.coins() < module.creditCost) {
            message = "SIN CREDITOS";
            return;
        }
        if (game.installShipModule(selectedSlot, module)) {
            message = "INSTALADO EN " + (selectedSlot + 1);
        } else {
            message = "NO SE PUDO INSTALAR";
        }
    }

    private ShipModuleDef selectedModule() {
        ShipModuleDef[] modules = ShipModuleDef.values();
        return selectedModule >= 0 && selectedModule < modules.length
                ? modules[selectedModule] : null;
    }

    String previewModule() {
        return preview == null || preview.module == null
                ? "" : preview.module.displayName;
    }

    boolean previewOk() {
        return preview != null && preview.ok();
    }

    float previewThrust() {
        return preview == null ? 0f : preview.thrust;
    }

    float previewThrustAfter() {
        return preview == null ? 0f : preview.thrustAfter;
    }

    private boolean tapCircle(float cx, float cy, float r) {
        if (!Gdx.input.justTouched()) {
            return false;
        }
        for (int i = 0; i < 5; i++) {
            if (!Gdx.input.isTouched(i)) {
                continue;
            }
            float x = Gdx.input.getX(i);
            float y = Gdx.graphics.getHeight() - Gdx.input.getY(i);
            if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r) {
                return true;
            }
        }
        return false;
    }

    void draw(float w, float h) {
        if (btnAction == null) {
            layout(w, h);
        }
        float u = game.u;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0f, 0f, w, h,
                Visuals.SKY_BOTTOM, Visuals.SKY_BOTTOM, Visuals.SKY_TOP, Visuals.SKY_TOP);
        shapes.end();
        beginShapes();
        ui.grid(w, h, 80f * u, 0.014f);
        ui.frame(px, py, pw, ph, 14f * u, PANEL_C, Visuals.PANEL_EDGE, Visuals.ACCENT);
        ui.divider(px + 22f * u, h - 96f * u, pw - 44f * u,
                Visuals.PANEL_EDGE, Visuals.ACCENT);
        // Silueta de la nave y halo de ranuras.
        Ship.drawBody(shapes, shipCx, shipCy, 90f, 92f * u, false, 1f,
                game.time, Ship.PART_MAX, Ship.PART_MAX,
                game.upgrades.shipClass());
        drawSlots();
        drawRows();
        drawPreviewPanel(w, h);
        boolean can = selectedModule() != null;
        ui.drawButton(btnAction, can ? Visuals.BTN_PRIMARY : Visuals.BTN_OFF, can);
        ui.drawButton(btnBack, Visuals.BTN_SECONDARY, true);
        shapes.end();

        batch.begin();
        ui.textLeft("EQUIPAMIENTO", px + 24f * u, h - 30f * u, 0.94f * u,
                Visuals.TEXT);
        ui.textRight(game.upgrades.coins() + " CR", px + pw - 24f * u,
                h - 30f * u, 0.82f * u, Visuals.GOLD);
        ui.textLeftFit("CLASE " + game.upgrades.shipClass().displayName,
                px + 24f * u, h - 58f * u, 0.62f * u, pw * 0.6f, Visuals.TEXT_DIM);
        ui.textRight("RANURA " + (selectedSlot + 1) + " / 4",
                px + pw - 24f * u, h - 58f * u, 0.62f * u, Visuals.ACCENT);
        drawRowTexts();
        drawPreview(w, h);
        ui.drawButtonText(btnAction, can);
        ui.drawButtonText(btnBack, true);
        if (!message.isEmpty()) {
            ui.textCenteredFit(message, w * 0.5f, py + 82f * u, 0.62f * u,
                    pw - 40f * u, Visuals.WARN);
        }
        batch.end();
    }

    private void drawSlots() {
        float u = game.u;
        for (int i = 0; i < 4; i++) {
            ShipSlotDef slot = ShipSlotDef.slotFor(game.upgrades.shipClass(), i);
            if (slot == null) {
                continue;
            }
            float sx = shipCx + slot.localX * 300f * u;
            float sy = shipCy + slot.localY * 300f * u;
            boolean selected = i == selectedSlot;
            ShipModuleDef installed = game.upgrades.loadout().moduleDef(i);
            Color c = installed != null ? Visuals.GOOD : Visuals.TEXT_OFF;
            if (selected) {
                shapes.setColor(Visuals.ACCENT.r, Visuals.ACCENT.g,
                        Visuals.ACCENT.b, 0.22f);
                shapes.circle(sx, sy, 30f * u, 18);
            }
            shapes.setColor(c.r, c.g, c.b, 0.35f);
            shapes.circle(sx, sy, 19f * u, 16);
            shapes.setColor(c);
            shapes.circle(sx, sy, 13f * u, 14);
            shapes.setColor(0.03f, 0.05f, 0.08f, 0.9f);
            shapes.circle(sx, sy, 7f * u, 12);
        }
    }

    private void drawRows() {
        float u = game.u;
        ShipClassDef shipClass = game.upgrades.shipClass();
        ShipSlotDef slot = ShipSlotDef.slotFor(shipClass, selectedSlot);
        for (int i = 0; i < rows.size; i++) {
            Button row = rows.get(i);
            ShipModuleDef module = ShipModuleDef.values()[i];
            boolean compatible = slot != null && slot.accepts(module);
            boolean installed = game.shipModuleSlot(module) >= 0;
            boolean chosen = i == selectedModule;
            Color fill = chosen ? CARD_SEL_C : CARD_C;
            ui.panel(row.bounds.x - 3f * u, row.bounds.y - 3f * u,
                    row.bounds.width + 6f * u, row.bounds.height + 6f * u,
                    6f * u, fill, chosen ? Visuals.ACCENT
                            : installed ? Visuals.GOOD
                            : compatible ? EDGE_C : Visuals.BTN_OFF);
            ui.led(row.bounds.x + 14f * u,
                    row.bounds.y + row.bounds.height * 0.5f, 3f * u,
                    installed ? Visuals.GOOD : compatible ? Visuals.ACCENT
                            : Visuals.LED_OFF);
        }
    }

    private void drawRowTexts() {
        float u = game.u;
        ShipClassDef shipClass = game.upgrades.shipClass();
        ShipSlotDef slot = ShipSlotDef.slotFor(shipClass, selectedSlot);
        for (int i = 0; i < rows.size; i++) {
            Button row = rows.get(i);
            ShipModuleDef module = ShipModuleDef.values()[i];
            boolean compatible = slot != null && slot.accepts(module);
            boolean installed = game.shipModuleSlot(module) >= 0;
            float mid = row.bounds.y + row.bounds.height * 0.5f;
            ui.textLeftFit(module.displayName, row.bounds.x + 28f * u,
                    mid + 12f * u, 0.72f * u, row.bounds.width * 0.55f,
                    compatible || installed ? Visuals.TEXT : Visuals.TEXT_OFF);
            ui.textLeftFit(module.description, row.bounds.x + 28f * u,
                    mid - 7f * u, 0.52f * u, row.bounds.width * 0.60f,
                    Visuals.TEXT_DIM);
            String status = installed ? "INSTALADO"
                    : compatible ? module.creditCost + " CR" : "NO COMPATIBLE";
            ui.textRight(status, row.bounds.x + row.bounds.width - 10f * u,
                    mid + 4f * u, 0.58f * u,
                    installed ? Visuals.GOOD
                            : compatible ? Visuals.GOLD : Visuals.TEXT_OFF);
        }
    }

    private void drawPreviewPanel(float w, float h) {
        float u = game.u;
        ShipModuleDef module = selectedModule();
        float y = h - 236f * u;
        ui.panel(px + 20f * u, y - 96f * u, pw - 40f * u, 118f * u, 6f * u,
                CARD_C, module == null ? EDGE_C : Visuals.ACCENT);
    }

    private void drawPreview(float w, float h) {
        float u = game.u;
        ShipModuleDef module = selectedModule();
        float y = h - 236f * u;
        if (module == null) {
            preview = null;
            ui.textLeftFit("FICHA DEL COMPONENTE", px + 32f * u,
                    y + 6f * u, 0.60f * u, pw - 64f * u, Visuals.TEXT_OFF);
            ui.textLeftFit("TOCA UNA RANURA Y UN MODULO",
                    px + 32f * u, y - 20f * u, 0.54f * u, pw - 64f * u,
                    Visuals.TEXT_OFF);
            return;
        }
        int installedSlot = game.shipModuleSlot(module);
        boolean installing = installedSlot < 0;
        preview = LoadoutPreview.preview(game.upgrades, module,
                installing, installedSlot >= 0 ? installedSlot : selectedSlot,
                game.upgrades.shipClass());
        ui.textLeftFit(module.displayName, px + 32f * u, y + 4f * u,
                0.72f * u, pw * 0.55f, Visuals.TEXT);
        ui.textRight(installing ? "COSTE " + module.creditCost + " CR"
                        : "INSTALADO R" + (installedSlot + 1),
                px + pw - 32f * u, y + 4f * u, 0.62f * u,
                installing ? Visuals.GOLD : Visuals.GOOD);
        ui.textLeftFit(preview.effectLabel(), px + 32f * u, y - 22f * u,
                0.54f * u, pw - 64f * u, Visuals.ACCENT);
        String stats = "EMP " + Math.round(preview.thrust) + ">"
                + Math.round(preview.thrustAfter)
                + "  VEL " + Math.round(preview.maxSpeed) + ">"
                + Math.round(preview.maxSpeedAfter)
                + "  LASER " + preview.laserLevel + ">"
                + preview.laserLevelAfter
                + "  TRACT " + Math.round(preview.tractorRange) + ">"
                + Math.round(preview.tractorRangeAfter)
                + "  SENS " + Math.round(preview.sensorRange) + ">"
                + Math.round(preview.sensorRangeAfter);
        ui.textLeftFit(stats, px + 32f * u, y - 46f * u, 0.48f * u,
                pw - 64f * u, Visuals.TEXT_DIM);
        if (!preview.ok()) {
            ui.textLeftFit(preview.error, px + 32f * u, y - 72f * u,
                    0.54f * u, pw - 64f * u, Visuals.WARN);
        }
        btnAction.label = installing ? "INSTALAR" : "RETIRAR";
    }

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }
}

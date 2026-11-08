package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

/**
 * Servicio de audio local: carga efectos de assets/audio y ofrece reproducción,
 * bucle de motor y silencio. No usa red ni recursos de jars.
 */
final class AudioService {

    enum Sfx {
        ENGINE("engine"),
        LASER("laser"),
        MISSILE("missile"),
        EXPLOSION("explosion"),
        IMPACT("impact"),
        PICKUP("pickup"),
        ALARM("alarm"),
        DOCK("dock"),
        TRADE("trade"),
        MODULE("module"),
        WARP("warp");

        final String file;

        Sfx(String file) {
            this.file = file;
        }
    }

    private final Sound[] sounds = new Sound[Sfx.values().length];
    private final Upgrades upgrades;
    private long engineId = -1L;
    private boolean muted;

    AudioService(Upgrades upgrades) {
        this.upgrades = upgrades;
        this.muted = upgrades != null && upgrades.audioMuted();
        for (Sfx sfx : Sfx.values()) {
            try {
                sounds[sfx.ordinal()] = Gdx.audio.newSound(
                        Gdx.files.internal("audio/" + sfx.file + ".wav"));
            } catch (RuntimeException ignored) {
                sounds[sfx.ordinal()] = null;
            }
        }
    }

    boolean muted() {
        return muted;
    }

    void setMuted(boolean value) {
        muted = value;
        if (muted) {
            stopEngine();
        }
        if (upgrades != null) {
            upgrades.setAudioMuted(value);
        }
    }

    void toggleMuted() {
        setMuted(!muted);
    }

    void play(Sfx sfx) {
        if (muted || sfx == null || sfx == Sfx.ENGINE) {
            return;
        }
        Sound sound = sounds[sfx.ordinal()];
        if (sound != null) {
            sound.play(0.65f);
        }
    }

    /** Bucle de motor mientras la nave acelera. */
    void setEngine(boolean on) {
        Sound sound = sounds[Sfx.ENGINE.ordinal()];
        if (sound == null) {
            return;
        }
        if (on && !muted && engineId < 0L) {
            engineId = sound.loop(0.35f);
        } else if ((!on || muted) && engineId >= 0L) {
            stopEngine();
        }
    }

    private void stopEngine() {
        Sound sound = sounds[Sfx.ENGINE.ordinal()];
        if (sound != null && engineId >= 0L) {
            sound.stop(engineId);
        }
        engineId = -1L;
    }

    void dispose() {
        stopEngine();
        for (Sound sound : sounds) {
            if (sound != null) {
                sound.dispose();
            }
        }
    }
}

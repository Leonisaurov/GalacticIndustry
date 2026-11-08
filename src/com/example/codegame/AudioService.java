package com.example.codegame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.audio.Music;
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
    private Music exploreMusic;
    private Music tensionMusic;
    private boolean musicMuted;
    private boolean musicStarted;
    private float tensionWeight;
    private boolean tension;

    AudioService(Upgrades upgrades) {
        this.upgrades = upgrades;
        this.muted = upgrades != null && upgrades.audioMuted();
        this.musicMuted = upgrades != null && upgrades.musicMuted();
        try {
            exploreMusic = Gdx.audio.newMusic(
                    localFile("audio/music/explore.mp3"));
            tensionMusic = Gdx.audio.newMusic(
                    localFile("audio/music/tension.mp3"));
            exploreMusic.setLooping(true);
            tensionMusic.setLooping(true);
        } catch (RuntimeException e) {
            Gdx.app.error("AudioService", "music load failed", e);
            exploreMusic = null;
            tensionMusic = null;
        }
        for (Sfx sfx : Sfx.values()) {
            try {
                sounds[sfx.ordinal()] = Gdx.audio.newSound(
                        localFile("audio/" + sfx.file + ".wav"));
            } catch (RuntimeException ignored) {
                sounds[sfx.ordinal()] = null;
            }
        }
    }

    /**
     * Los assets del APK van comprimidos y SoundPool/MediaPlayer necesitan un
     * descriptor o ruta real. Se copian una vez a almacenamiento local.
     */
    private static FileHandle localFile(String path) {
        FileHandle source = Gdx.files.internal(path);
        FileHandle target = Gdx.files.local("audio-cache/" + path);
        if (!target.exists() || target.length() != source.length()) {
            target.writeBytes(source.readBytes(), false);
        }
        return target;
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

    boolean musicMuted() {
        return musicMuted;
    }

    void setMusicMuted(boolean value) {
        musicMuted = value;
        if (upgrades != null) {
            upgrades.setMusicMuted(value);
        }
        if (musicMuted) {
            stopMusic();
        }
    }

    void toggleMusicMuted() {
        setMusicMuted(!musicMuted);
    }

    String musicMode() {
        if (musicMuted || exploreMusic == null) {
            return "OFF";
        }
        return tension ? "TENSION" : "EXPLORE";
    }

    /** Mezcla suave entre exploración y tensión. */
    void updateMusic(float dt, boolean dangerous) {
        tension = dangerous;
        if (musicMuted || exploreMusic == null || tensionMusic == null) {
            stopMusic();
            return;
        }
        if (!musicStarted) {
            exploreMusic.setVolume(0f);
            tensionMusic.setVolume(0f);
            exploreMusic.play();
            tensionMusic.play();
            musicStarted = true;
        }
        float target = dangerous ? 1f : 0f;
        float step = Math.max(0f, dt) * 0.45f;
        if (tensionWeight < target) {
            tensionWeight = Math.min(target, tensionWeight + step);
        } else if (tensionWeight > target) {
            tensionWeight = Math.max(target, tensionWeight - step);
        }
        exploreMusic.setVolume(0.30f * (1f - tensionWeight));
        tensionMusic.setVolume(0.32f * tensionWeight);
    }

    private void stopMusic() {
        if (exploreMusic != null) {
            exploreMusic.stop();
        }
        if (tensionMusic != null) {
            tensionMusic.stop();
        }
        musicStarted = false;
        tensionWeight = 0f;
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
        stopMusic();
        if (exploreMusic != null) {
            exploreMusic.dispose();
        }
        if (tensionMusic != null) {
            tensionMusic.dispose();
        }
        for (Sound sound : sounds) {
            if (sound != null) {
                sound.dispose();
            }
        }
    }
}

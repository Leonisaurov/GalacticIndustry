package com.example.codegame;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Guardado v2 en un archivo local con escritura atomica, copia de seguridad y
 * migracion desde el formato heredado {@code run2}.
 *
 * <p>El almacenamiento se abstrae con {@link Storage} para poder probar la
 * logica en la JVM sin Android. En el juego se usa {@link FileStorage} sobre el
 * directorio privado de la app.</p>
 */
class SaveService {

    static final String DEFAULT_SLOT = "slot1";
    /** Slot separado con el último mundo/nave de un personaje muerto. */
    static final String LEGACY_SLOT = "legacy";
    /** Slot de recuperación usado solo por el modo de desarrollo. */
    static final String RECOVERY_SLOT = "recovery";
    static final String SAVE_NAME = DEFAULT_SLOT + ".json";
    static final String BACKUP_NAME = SAVE_NAME + ".bak";

    enum LoadStatus {
        V2,
        BACKUP,
        MIGRATED,
        NOT_FOUND,
        INVALID
    }

    interface Storage {
        boolean exists(String name);

        String read(String name) throws IOException;

        void write(String name, String text) throws IOException;

        void delete(String name) throws IOException;
    }

    private final Storage storage;
    private final Upgrades legacy;
    private final String slot;

    SaveService(Storage storage) {
        this(storage, null, DEFAULT_SLOT);
    }

    /**
     * @param legacy fuente del guardado {@code run2}. Puede ser null en pruebas
     *               que solo usan v2.
     */
    SaveService(Storage storage, Upgrades legacy) {
        this(storage, legacy, DEFAULT_SLOT);
    }

    /**
     * @param slot nombre del slot lógico (por ejemplo, un personaje). Los
     *             archivos se derivan como {@code <slot>.json} y {@code <slot>.json.bak}.
     */
    SaveService(Storage storage, Upgrades legacy, String slot) {
        if (storage == null) {
            throw new IllegalArgumentException("storage nulo");
        }
        this.storage = storage;
        this.legacy = legacy;
        this.slot = sanitizeSlot(slot);
    }

    String saveName() {
        return slot + ".json";
    }

    String backupName() {
        return saveName() + ".bak";
    }

    boolean hasSave() {
        return storage.exists(saveName()) || storage.exists(backupName());
    }

    /**
     * Carga la mejor partida disponible: v2, backup o migracion de {@code run2}.
     * No modifica el guardado si todo es invalido.
     */
    LoadStatus load(GameSession session) {
        LoadStatus primary = tryLoad(session, saveName(), LoadStatus.V2);
        if (primary == LoadStatus.V2) {
            return LoadStatus.V2;
        }
        LoadStatus backup = tryLoad(session, backupName(), LoadStatus.BACKUP);
        if (backup == LoadStatus.BACKUP) {
            return LoadStatus.BACKUP;
        }
        if (legacy != null) {
            String old = legacy.getText(GameSession.RUN_KEY);
            if (old != null && session.restoreLegacy(old)) {
                if (save(session)) {
                    legacy.putText(GameSession.RUN_KEY, null);
                }
                return LoadStatus.MIGRATED;
            }
        }
        if (primary == LoadStatus.INVALID || backup == LoadStatus.INVALID) {
            return LoadStatus.INVALID;
        }
        return LoadStatus.NOT_FOUND;
    }

    /** Guarda la sesion; conserva el guardado anterior como backup. */
    boolean save(GameSession session) {
        try {
            String text = SaveCodec.encode(session);
            if (storage.exists(saveName())) {
                String previous = storage.read(saveName());
                // Nunca sustituye un backup bueno por un archivo corrupto.
                if (previous != null && SaveCodec.validate(previous)) {
                    storage.write(backupName(), previous);
                }
            }
            storage.write(saveName(), text);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    void clear() {
        try {
            storage.delete(saveName());
            storage.delete(backupName());
        } catch (IOException ignored) {
            // Si no se puede borrar, el siguiente guardado lo reemplazara.
        }
        if (legacy != null) {
            legacy.putText(GameSession.RUN_KEY, null);
        }
    }

    private LoadStatus tryLoad(GameSession session, String name, LoadStatus success) {
        if (!storage.exists(name)) {
            return LoadStatus.NOT_FOUND;
        }
        try {
            String text = storage.read(name);
            if (text != null && SaveCodec.decode(text, session)) {
                return success;
            }
        } catch (IOException | RuntimeException e) {
            // Se tratara como corrupto y se probara el backup.
        }
        return LoadStatus.INVALID;
    }

    private static String sanitizeSlot(String slot) {
        if (slot == null || slot.isEmpty()
                || slot.indexOf('/') >= 0 || slot.indexOf('\\') >= 0
                || slot.contains("..")) {
            return DEFAULT_SLOT;
        }
        return slot;
    }

    /** Almacenamiento en memoria para SelfTest. */
    static final class MemoryStorage implements Storage {
        private final Map<String, String> files = new HashMap<>();

        @Override
        public boolean exists(String name) {
            return files.containsKey(name);
        }

        @Override
        public String read(String name) {
            return files.get(name);
        }

        @Override
        public void write(String name, String text) {
            files.put(name, text);
        }

        @Override
        public void delete(String name) {
            files.remove(name);
        }
    }

    /** Almacenamiento en disco con archivo temporal y rename atomico. */
    static final class FileStorage implements Storage {
        private final File dir;

        FileStorage(File dir) {
            this.dir = dir;
        }

        @Override
        public boolean exists(String name) {
            return file(name).isFile();
        }

        @Override
        public String read(String name) throws IOException {
            File file = file(name);
            if (!file.isFile()) {
                return null;
            }
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        }

        @Override
        public void write(String name, String text) throws IOException {
            if (dir != null && !dir.exists() && !dir.mkdirs() && !dir.isDirectory()) {
                throw new IOException("no se pudo crear " + dir);
            }
            File target = file(name);
            File parent = target.getParentFile();
            File temp = new File(parent, target.getName() + ".tmp");
            Files.write(temp.toPath(), text.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            try {
                Files.move(temp.toPath(), target.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException | UnsupportedOperationException e) {
                Files.move(temp.toPath(), target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }

        @Override
        public void delete(String name) throws IOException {
            Files.deleteIfExists(file(name).toPath());
        }

        private File file(String name) {
            return dir == null ? new File(name) : new File(dir, name);
        }
    }
}

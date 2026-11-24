package com.permadeath;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Estado persistente de la serie, guardado en la carpeta del mundo. Usa reloj REAL. */
public final class SeasonState {
    public static final long HOUR_MS = 3_600_000L;
    public static final long DAY_MS = 24 * HOUR_MS;

    /** Momento real (ms) en que se abrio el mundo = inicio del dia 1. */
    public long startMs;
    /** Momento real (ms) en que termina la tormenta acumulada. */
    public long stormEndMs;
    public int lastPhaseAnnounced = -1;
    public boolean seasonEndAnnounced = false;
    /** Ultimo reset del Death Train anunciado (dia/25: 1 = dia 25, 2 = dia 50...). */
    public int lastResetAnnounced = 0;
    /** Jugador -> momento real en que debe ser baneado. */
    public final Map<String, Long> pendingBans = new LinkedHashMap<>();
    /** Mensajes de muerte personalizados (clave: nombre en minusculas). */
    public final Map<String, String> deathMessages = new HashMap<>();
    /** Jugadores a los que ya se les mostro la ventana de bienvenida (minusculas). */
    public final Set<String> prompted = new HashSet<>();

    private final Path file;

    private SeasonState(Path file) {
        this.file = file;
    }

    public static SeasonState load(Path worldDir) {
        SeasonState s = new SeasonState(worldDir.resolve("permadeath_state.properties"));
        if (Files.exists(s.file)) {
            try (Reader r = Files.newBufferedReader(s.file)) {
                Properties p = new Properties();
                p.load(r);
                s.startMs = Long.parseLong(p.getProperty("startMs"));
                s.stormEndMs = Long.parseLong(p.getProperty("stormEndMs", "0"));
                s.lastPhaseAnnounced = Integer.parseInt(p.getProperty("lastPhaseAnnounced", "-1"));
                s.seasonEndAnnounced = Boolean.parseBoolean(p.getProperty("seasonEndAnnounced", "false"));
                s.lastResetAnnounced = Integer.parseInt(p.getProperty("lastResetAnnounced", "0"));
                for (String key : p.stringPropertyNames()) {
                    if (key.startsWith("pending.")) {
                        s.pendingBans.put(key.substring(8), Long.parseLong(p.getProperty(key)));
                    } else if (key.startsWith("msg.")) {
                        s.deathMessages.put(key.substring(4), p.getProperty(key));
                    } else if (key.startsWith("prompted.")) {
                        s.prompted.add(key.substring(9));
                    }
                }
                return s;
            } catch (IOException | RuntimeException e) {
                PermadeathMod.LOGGER.error("Estado corrupto, se reinicia el contador", e);
            }
        }
        s.startMs = System.currentTimeMillis();
        s.save();
        PermadeathMod.LOGGER.info("Permadeath: contador iniciado ahora (dia 1).");
        return s;
    }

    public synchronized void save() {
        Properties p = new Properties();
        p.setProperty("startMs", Long.toString(startMs));
        p.setProperty("stormEndMs", Long.toString(stormEndMs));
        p.setProperty("lastPhaseAnnounced", Integer.toString(lastPhaseAnnounced));
        p.setProperty("seasonEndAnnounced", Boolean.toString(seasonEndAnnounced));
        p.setProperty("lastResetAnnounced", Integer.toString(lastResetAnnounced));
        pendingBans.forEach((name, t) -> p.setProperty("pending." + name, Long.toString(t)));
        deathMessages.forEach((name, m) -> p.setProperty("msg." + name, m));
        prompted.forEach(name -> p.setProperty("prompted." + name, "true"));
        try (Writer w = Files.newBufferedWriter(file)) {
            p.store(w, "Permadeath state");
        } catch (IOException e) {
            PermadeathMod.LOGGER.error("No se pudo guardar el estado", e);
        }
    }

    /** Dia de la serie: el dia 1 empieza al abrir el mundo, cada 24 h reales sube uno. */
    public int currentDay(long nowMs) {
        return (int) ((nowMs - startMs) / DAY_MS) + 1;
    }
}

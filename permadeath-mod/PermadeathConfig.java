package com.permadeath;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Configuracion en config/permadeath.properties */
public final class PermadeathConfig {
    public int banDelaySeconds = 5;
    public int seasonDays = 110;
    public int phaseLengthDays = 10;
    /** Nombres (en minusculas) que no son baneados al morir. */
    public Set<String> exempt = new HashSet<>();
    /** Avisos de organizacion por fase (fase -> lista de mensajes). */
    public Map<Integer, List<String>> announcements = new HashMap<>();

    private static final String DEFAULT = """
            # Configuracion de Permadeath
            # Segundos entre el anuncio PERMADEATH y el baneo
            banDelaySeconds=5
            # Duracion total de la serie (dias reales)
            seasonDays=110
            # Cada cuantos dias reales cambia la fase
            phaseLengthDays=10
            # Jugadores exentos de baneo, separados por coma (ej: Admin1,Admin2)
            exempt=

            # Avisos por fase, se anuncian en el chat al empezar la fase. Varios avisos se separan con |
            # Fase 0 = inicio de la serie (dias 1-9). Fase 1 = dia 10, fase 2 = dia 20, ... fase 11 = dia 110.
            # Aqui tambien van las reglas de ORGANIZACION que dependen de los jugadores.
            phase.0.announce=Minecraft vanilla. Cada muerte activa una tormenta perpetua.
            phase.1.announce=¡Todas las arañas ahora tienen efectos de poción! (entre 1 y 3 al azar)|Doble de mobs|Mínimo 4 jugadores para pasar la noche
            #phase.2.announce=Desde hoy no se permiten zonas comunitarias.
            """;

    public static PermadeathConfig load() {
        PermadeathConfig cfg = new PermadeathConfig();
        Path file = FabricLoader.getInstance().getConfigDir().resolve("permadeath.properties");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, DEFAULT);
            }
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(file)) {
                p.load(r);
            }
            cfg.banDelaySeconds = Integer.parseInt(p.getProperty("banDelaySeconds", "5").trim());
            cfg.seasonDays = Integer.parseInt(p.getProperty("seasonDays", "110").trim());
            cfg.phaseLengthDays = Math.max(1, Integer.parseInt(p.getProperty("phaseLengthDays", "10").trim()));
            for (String n : p.getProperty("exempt", "").split(",")) {
                if (!n.isBlank()) cfg.exempt.add(n.trim().toLowerCase());
            }
            for (String key : p.stringPropertyNames()) {
                if (key.startsWith("phase.") && key.endsWith(".announce")) {
                    int phase = Integer.parseInt(key.substring(6, key.length() - 9));
                    List<String> msgs = new ArrayList<>();
                    for (String m : p.getProperty(key).split("\\|")) {
                        if (!m.isBlank()) msgs.add(m.trim());
                    }
                    cfg.announcements.put(phase, msgs);
                }
            }
        } catch (IOException | RuntimeException e) {
            PermadeathMod.LOGGER.error("Error leyendo permadeath.properties, usando valores por defecto", e);
        }
        return cfg;
    }
}

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
    /** Cada cuantos dias se reinicia el Death Train (horas de tormenta "como si fuese el dia 1"). */
    public int stormResetDays = 25;
    /** Dia 30: % de endermans del End que se convierten en Ender Ghast / Ender Creeper al aparecer. */
    public int enderGhastPercent = 8;
    public int enderCreeperPercent = 12;
    /** Nombres (en minusculas) que no son baneados al morir. */
    public Set<String> exempt = new HashSet<>();
    /** Avisos de organizacion por fase (fase -> lista de mensajes). */
    public Map<Integer, List<String>> announcements = new HashMap<>();
    /** Avisos para dias especiales no multiplos de 10 (ej. dia 25). */
    public Map<Integer, List<String>> dayAnnouncements = new HashMap<>();
    /** Drops de armadura de netherite (dia 25): clave -> {item, probabilidad %}. */
    public Map<String, String[]> netheriteDrops = new HashMap<>();

    private static final String DEFAULT = """
            # Configuracion de Permadeath
            # Segundos entre el anuncio PERMADEATH y el baneo
            banDelaySeconds=5
            # Duracion total de la serie (dias reales)
            seasonDays=110
            # Cada cuantos dias reales cambia la fase
            phaseLengthDays=10
            # Cada cuantos dias se reinicia el Death Train (horas de tormenta por muerte = dia % este valor)
            stormResetDays=25
            # Dia 30: porcentaje de endermans del End que aparecen como Ender Ghast / Ender Creeper
            enderGhastPercent=8
            enderCreeperPercent=12
            # Jugadores exentos de baneo, separados por coma (ej: Admin1,Admin2)
            exempt=

            # Avisos por fase, se anuncian en el chat al empezar la fase. Varios avisos se separan con |
            # Fase 0 = inicio de la serie (dias 1-9). Fase 1 = dia 10, fase 2 = dia 20, ... fase 11 = dia 110.
            # Aqui tambien van las reglas de ORGANIZACION que dependen de los jugadores.
            phase.0.announce=Minecraft vanilla. Cada muerte activa una tormenta perpetua.
            #phase.3.announce=Aqui iran los avisos del dia 30.

            # Drops de netherite del dia 25: item,probabilidad en %
            day25.drop.gigaslime=netherite_chestplate,100
            day25.drop.gigamagmacube=netherite_leggings,100
            day25.drop.ghast=netherite_boots,100
            day25.drop.cavespider=netherite_helmet,10
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
            cfg.stormResetDays = Math.max(1, Integer.parseInt(p.getProperty("stormResetDays", "25").trim()));
            cfg.enderGhastPercent = Integer.parseInt(p.getProperty("enderGhastPercent", "8").trim());
            cfg.enderCreeperPercent = Integer.parseInt(p.getProperty("enderCreeperPercent", "12").trim());
            for (String n : p.getProperty("exempt", "").split(",")) {
                if (!n.isBlank()) cfg.exempt.add(n.trim().toLowerCase());
            }
            for (String key : p.stringPropertyNames()) {
                if (key.startsWith("phase.") && key.endsWith(".announce")) {
                    int phase = Integer.parseInt(key.substring(6, key.length() - 9));
                    cfg.announcements.put(phase, split(p.getProperty(key)));
                } else if (key.startsWith("day.") && key.endsWith(".announce")) {
                    int day = Integer.parseInt(key.substring(4, key.length() - 9));
                    cfg.dayAnnouncements.put(day, split(p.getProperty(key)));
                } else if (key.startsWith("day25.drop.")) {
                    String[] v = p.getProperty(key).split(",");
                    if (v.length == 2) cfg.netheriteDrops.put(key.substring(11), new String[]{v[0].trim(), v[1].trim()});
                }
            }
        } catch (IOException | RuntimeException e) {
            PermadeathMod.LOGGER.error("Error leyendo permadeath.properties, usando valores por defecto", e);
        }

        // ---- Valores incorporados (se usan si el archivo de config no define esa entrada) ----
        List<String> p0 = new ArrayList<>(cfg.announcements.getOrDefault(0, List.of()));
        String lockNote = "Ancient debris y plantillas de mejora de netherita eliminados del mundo: no se puede conseguir netherita de forma manual";
        if (!p0.contains(lockNote)) p0.add(lockNote);
        cfg.announcements.put(0, p0);
        cfg.announcements.putIfAbsent(1, List.of(
                "¡Todas las arañas ahora tienen efectos de poción! (entre 1 y 3 al azar)",
                "Doble mobcap de mobs hostiles: pueden aparecer el doble de mobs a la vez",
                "Mínimo 4 jugadores para pasar la noche"));
        cfg.announcements.putIfAbsent(2, List.of(
                "Ciclo de día y noche constante: no puedes saltar la noche",
                "Todos los mobs pacíficos (incluidos ajolotes, sniffers, camellos, allays, golems y mascotas...) ahora son agresivos y los pigmans están cabreados de antemano",
                "Todas las arañas ahora tienen de 3 a 5 efectos",
                "Todas las arañas aparecen con un esqueleto encima (1 de 5 clases)",
                "1 de cada 100 Ravagers suelta un Tótem de Inmortalidad",
                "Todos los Phantoms son ahora de tamaño 9 y doble de vida",
                "Sin drops: Iron Golems, Pigmans, Ghasts, Guardianes, Magma Cubes, Endermans, Brujas, Wither Skeletons, Evokers, Phantoms, Slimes, Drowneds y Blazes",
                "Ya no puedes ayudar o ser ayudado por jugadores externos a tu equipo a cazar/sobrevivir (raids, cacerías, wither bosses...)"));
        cfg.announcements.putIfAbsent(3, List.of(
                "Ya no se puede obtener la armadura de Netherite",
                "Ya no se pueden usar Zonas Comunitarias a excepción del SPAWN",
                "Los Tótems tienen un 99% de probabilidades de activarse y un 1% de fallar (al activarse, el chat muestra la probabilidad sobre 100)",
                "Los Mobs Especiales ahora tienen nombre especial",
                "Los Calamares son ahora Guardianes con Speed II",
                "Los Murciélagos son ahora Blazes de Resistencia II",
                "Los Creepers son ahora Eléctricos",
                "Los Pillagers son ahora invisibles y tienen ballestas de Quick Charge X",
                "Los Esqueletos son ahora Esqueletos con CLASE y llevan en la mano izquierda flechas de Daño II",
                "Los Pigmans tienen ahora armadura de diamante",
                "Los Iron Golems tienen ahora Velocidad IV",
                "Los Endermans tienen ahora Fuerza II",
                "Los Silverfish tienen 5 efectos de la misma lista que las Arañas",
                "Los Shulkers son Shulkers Explosivos y dropean con 20% de probabilidad una Shulker Shell",
                "Aparecen Ender Ghasts y Ender Creepers en el End"));
        cfg.dayAnnouncements.putIfAbsent(25, List.of(
                "RESET DEL DEATH TRAIN: cada 25 días el Death Train se vuelve más peligroso y las horas de tormenta por muerte se reinician como si fuese el día 1",
                "Durante el Death Train todos los mobs tienen Fuerza I, Velocidad I y Resistencia I",
                "Todas las arañas tienen ahora 5 efectos",
                "Todos los Ravagers tienen Fuerza II y Velocidad I, y los tótems ahora los sueltan con un 20%",
                "GIGASLIMES: los slimes solo spawnean de tamaño 15 con doble de vida",
                "GIGA MAGMACUBES: los magmacubes solo spawnean de tamaño 16",
                "GHAST DEMONÍACOS: los ghasts spawnean con 40 a 60 de vida y sus bolas de fuego tienen potencia 3, 4 o 5",
                "¡ARMADURA DE NETHERITE DESBLOQUEADA! Peto: Gigaslimes. Pantalones: Giga Magmacubes. Botas: Ghast Demoníacos. Casco: Arañas de Cueva",
                "Cada pieza de armadura de Netherite da 1 corazón extra",
                "La armadura de Netherite será completamente necesaria para sobrevivir más días"));
        cfg.netheriteDrops.putIfAbsent("gigaslime", new String[]{"netherite_chestplate", "100"});
        cfg.netheriteDrops.putIfAbsent("gigamagmacube", new String[]{"netherite_leggings", "100"});
        cfg.netheriteDrops.putIfAbsent("ghast", new String[]{"netherite_boots", "100"});
        cfg.netheriteDrops.putIfAbsent("cavespider", new String[]{"netherite_helmet", "10"});
        return cfg;
    }

    private static List<String> split(String value) {
        List<String> msgs = new ArrayList<>();
        for (String m : value.split("\\|")) {
            if (!m.isBlank()) msgs.add(m.trim());
        }
        return msgs;
    }
}

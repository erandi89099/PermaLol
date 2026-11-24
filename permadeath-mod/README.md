# Permadeath (Fabric, Minecraft 26.2)

Mod de servidor (los jugadores pueden entrar con cliente vanilla) para la serie Permadeath.

## Calendario (110 dias reales)
El dia 1 empieza al abrir el mundo por primera vez; cada 24 h reales sube un dia.
- Dias 1-9: Minecraft vanilla + tormenta por muertes.
- Dia 10, 20, 30 ... 110: aumento de dificultad (fase 1 ... fase 11).

## Siempre activo
- Al morir: titulo **¡Permadeath!** + "<jugador> ha muerto" en pantalla y sonido para todos, mensaje en chat
  "Este es el comienzo del sufrimiento eterno de X. ¡HA SIDO PERMABANEADO!", mensaje de muerte personalizado
  del jugador, y baneo a los 5 s.
- **Tormenta perpetua** (solo Overworld): cada muerte suma tantas horas REALES como el numero del dia. Acumulable.
- Primera vez que entra un jugador: ventana para escribir su mensaje de muerte (tambien: `/permadeath mensaje <texto>`).

## Dia 10 (fase 1)
- Todas las aranas (y aranas de cueva) reciben entre 1 y 3 efectos infinitos al azar: Velocidad III, Fuerza IV,
  Salto V, Brillo, Regeneracion IV, Invisibilidad, Caida lenta, Resistencia III.
- Doble mobcap de mobs hostiles (el limite de mobs hostiles se duplica; no se duplican mobs individuales).
- Minimo 4 jugadores durmiendo para pasar la noche.

## Dia 20 (fase 2)
- Ciclo dia/noche constante: ya no se puede saltar la noche.
- Todos los mobs no hostiles (animales, acuaticos, ajolotes, sniffers, camellos, allays, aldeanos, golem de nieve...)
  son agresivos (incluidos golems y mascotas domesticadas) y los pigmans estan enfadados de antemano.
- Aranas: de 3 a 5 efectos, y aparecen con un esqueleto encima (5 clases).
- 1 de cada 100 Ravagers suelta un Totem de Inmortalidad.
- Phantoms de tamano 9 y doble de vida.
- Sin drops: Iron Golems, Pigmans, Ghasts, Guardianes, Magma Cubes, Endermans, Brujas, Wither Skeletons, Evokers,
  Phantoms, Slimes, Drowneds y Blazes.
- Regla de organizacion (solo aviso): no se puede ayudar ni ser ayudado por jugadores externos al equipo.

## Dia 25 (sorpresa) y reset del Death Train
- Cada 25 dias (25, 50, 75, 100) las horas de tormenta por muerte se reinician como si fuese el dia 1
  (dia 24 -> 24 h, dia 25 -> 1 h, dia 26 -> 2 h ... dia 49 -> 25 h, dia 50 -> 1 h).
- Durante el Death Train (tormenta activa) todos los mobs tienen Fuerza I, Velocidad I y Resistencia I.
- Aranas con 5 efectos. Ravagers con Fuerza II y Velocidad I; tótem con 20%.
- Gigaslimes (tamano 15, doble vida), giga magmacubes (tamano 16), ghast demoniacos (40-60 vida, explosion 3-5).
- Armadura de netherite: gigaslime = peto, giga magmacube = pantalones, ghast demoniaco = botas, arana de cueva = casco
  (probabilidades en `day25.drop.*` del config). Cada pieza equipada da +1 corazon.

## Desde el inicio del mundo
- Sin netherita manual: el ancient debris se convierte en netherrack al cargar los chunks del Nether y los items
  ancient debris / netherite scrap / lingotes / bloques / plantillas de mejora se quitan de todas las tablas de loot.

## Dia 30 (fase 3)
- Ya no se puede obtener armadura de netherite (los drops del dia 25 solo valen del dia 25 al 29).
- Totems: 99% de activarse / 1% de fallar; al activarse el chat muestra la probabilidad sobre 100 (requiere el mixin).
- Mobs especiales con nombre. Esqueletos con clase (Guerrero, Infernal, Asesino, Tactico, Pesadilla), 40 de vida y
  flechas de Dano II en la mano izquierda. Tactico y Pesadilla son esqueletos Wither (sustituyen al normal).
- Calamares -> Guardianes (Speed II). Murcielagos -> Blazes (Resistencia II). Creepers electricos. Pillagers invisibles con
  ballestas Quick Charge X. Pigmans con armadura de diamante. Iron Golems con Velocidad IV. Endermans con Fuerza II.
  Silverfish con 5 efectos de arana.
- Shulkers Explosivos (sus proyectiles explotan; al morir sueltan una dinamita encendida) y Shulker Shell con 20%.
- Ender Ghast y Ender Creeper aparecen en el End en lugar de algunos endermans (`enderGhastPercent`, `enderCreeperPercent`).

## Comandos
- `/permadeath estado`, `/permadeath mensaje <texto>` (todos)
- `/permadeath revivir <jugador>`, `/permadeath setdia <n>`, `/permadeath tormenta sumar <horas>|limpiar` (admin)

## Config
`config/permadeath.properties`: segundos del ban, jugadores exentos, avisos de organizacion por fase.

## Anadir mecanicas
Implementa `com.permadeath.mechanics.Mechanic` y registrala en `MechanicRegistry.registerAll()`.

## Compilar en GitHub
1. Sube todo el contenido de esta carpeta a un repo (incluida la carpeta `.github`).
2. Pestana **Actions** -> workflow **build** -> espera a que termine.
3. Descarga el artefacto **permadeath-mod** y usa el `.jar` SIN `-sources`.
Servidor: Fabric Loader 0.19.3+, Fabric API 26.2 y Java 25.

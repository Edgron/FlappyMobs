# 🪽 FlappyMobs

Sistema avanzado de vuelos con criaturas para **Minecraft 1.21+ / Paper 1.21.10+**.

## ✨ Características
- Rutas con waypoints personalizables
- 7 criaturas voladoras configurables
- PHANTOM, GHAST, BLAZE, BEE, PARROT, VEX, ALLAY
- Las criaturas pueden cambiar de tamaño y velocidad
- Paracaídas configurable al desmontar
- Anti Enderpearl configurable
- Economía Vault integrada
- Carteles `[FlappyMobs]` para vuelos automatizados configurables
- Mensajes configurables
- Comandos intuitivos de crear/editar/eliminar vuelos
- Debug mode completo

## 📦 Requisitos
- **Paper 1.21.10+** (o superior)
- Java 21
- Vault (opcional, para economía)

## 🆕 Novedades Paper 1.21+
- ✅ Mejor rendimiento y estabilidad
- ✅ API mejorada para attributes

## ⚙️ Secciones de Configuración
Sección	Descripción
general	Ajustes globales del plugin: archivo de idioma, duración por defecto del paracaídas, modo debug/logs.
parachute	Configuración visual y de vida del paracaídas (pollo): salud máxima y escala (tamaño visual).
signs	Personalización de carteles para vuelos: clave, y colores para cada línea usando códigos y hex.
sounds	Sonidos personalizados para eventos: inicio de vuelo, despliegue de paracaídas y descenso.
creatures	Propiedades individuales de cada criatura voladora (activar, salud, velocidad, escala, silencio).
messages	Control granular sobre mensajes enviados: puedes activar o desactivar por clave cada mensaje.
Ejemplo de contenido:

text
# Sección general
general:
  language: "es"           # Archivo de idioma usado
  parachute_time: 5        # Segundos por defecto de paracaídas
  debug: false             # Activa logs extra

# Paracaídas
parachute:
  chicken_health: 10.0     # Salud del pollo-paracaídas
  chicken_scale: 1.5       # Tamaño visual

# Carteles
signs:
  key: "[FlappyMobs]"      # Identificador en el cartel
  line0_color: "&6"        # Color primer línea
  line1_color: "&e"        # Color vuelo
  ...

# Sonidos
sounds:
  start:                   # Sonido al iniciar vuelo
    enabled: true
    sound: "ENTITY_BREEZE_WIND_BURST"
    ...

# Criaturas
creatures:
  PHANTOM:
    enabled: true
    health: 20.0
    speed: 2.0
    scale: 0.8
    silent: true
  ...
  
## 🪧 Carteles
Pon `[FlappyMobs]` en línea 1, nombre del vuelo en línea 2.

## 🎮 Comandos

### Jugadores
| Comando               | Descripción                     |
|-----------------------|--------------------------------|
| `/fp flight <nombre>` | Inicia un vuelo                |
| `/fp dismount`        | Desmonta de la criatura        |
| `/fp list`            | Lista vuelos disponibles       |
| `/fp info <nombre>`   | Ver detalles del vuelo         |

### Administradores
| Comando                         | Descripción                         |
|--------------------------------|-----------------------------------|
| `/fp create <nombre> <mob> [costo]`   | Crea un nuevo vuelo               |
| `/fp setwp`                    | Añade waypoint                    |
| `/fp save`                    | Guarda el vuelo                   |
| `/fp delete <nombre>`          | Elimina un vuelo                  |
| `/fp edit <nombre> <propiedad> <valor>`| Edita propiedades             |
| `/fp removemobs`               | Limpia criaturas sin jinete       |
| `/fp reload`                  | Recarga configuración             |
| `/fp send <nombre> <jugador>` | Envía a un jugador a vuelo específico |

## 🔐 Permisos

| Permiso             | Descripción                                | Comandos relacionados                 |
|---------------------|-------------------------------------------|-------------------------------------|
| `flappymobs.use`     | Permite usar el comando `/fp` básico      | `flight`, `list`, `info`, `dismount`|
| `flappymobs.flight`  | Permite iniciar vuelos con `/fp flight`  | `flight`                            |
| `flappymobs.send`    | Permite enviar a otros jugadores a vuelos| `send`                             |
| `flappymobs.stop`    | Permite desmontar vuelo con `/fp dismount`| `dismount`                        |
| `flappymobs.create`  | Permite crear vuelos                      | `create`, `setwp`, `save`           |
| `flappymobs.edit`    | Permite editar vuelos                     | `edit`                             |
| `flappymobs.delete`  | Permite eliminar vuelos                   | `delete`                           |
| `flappymobs.removemobs` | Permite limpiar criaturas sin jinete    | `removemobs`                       |
| `flappymobs.reload`  | Permite recargar configuración            | `reload`                           |


## 📝 Notas
- Requiere **Paper 1.21.10+** para escala completa y estabilidad.
- Vault es opcional para manejar economía.
- Debug mode brinda información detallada para desarrollo y testing.

## 🔗 Enlaces
- [PaperMC](https://papermc.io/)
- [Vault Plugin](https://www.spigotmc.org/resources/vault.34315/)

---

**Versión:** 1.0.0  
**API:** Paper 1.21.3  
**Java:** 21

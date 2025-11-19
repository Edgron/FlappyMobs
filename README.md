# 🪽 FlappyMobs

Sistema avanzado de vuelos con criaturas para **Minecraft 1.21+ / Paper 1.21.10+**.

## ✨ Características
- Rutas con waypoints personalizables
- 8 criaturas voladoras con **scale completamente funcional**
- Todas las criaturas pueden cambiar de tamaño y velocidad
- Paracaídas configurable al desmontar
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

## ⚙️ Configuración
Edita `config.yml` y `flights.yml`. Activa debug para ver logs detallados:

```yaml
general:
  debug: true
```


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

### Carteles
Pon `[FlappyMobs]` en línea 1, nombre del vuelo en línea 2.

## 🦅 Criaturas con Scale
Todas funcionan con Paper 1.21+:
- ENDER_DRAGON ✓
- PHANTOM ✓
- GHAST ✓
- BLAZE ✓
- BEE ✓
- PARROT ✓
- VEX ✓
- ALLAY ✓

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

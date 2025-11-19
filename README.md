# 🐉 FlappyMobs

Sistema avanzado de vuelos con criaturas para **Minecraft 1.21+ / Paper 1.21.10+**.

## ✨ Características
- Rutas con waypoints personalizables
- 8 criaturas voladoras con **scale completamente funcional**
- Paracaídas configurable al desmontar
- Economía Vault integrada
- Carteles `[FlappyMobs]` para vuelos automatizados
- Comandos intuitivos de crear/editar/eliminar vuelos
- Debug mode completo

## 📦 Requisitos
- **Paper 1.21.10+** (o superior)
- Java 21
- Vault (opcional, para economía)

## 🆕 Novedades Paper 1.21+
- ✅ **GENERIC_SCALE**: Todas las criaturas pueden cambiar de tamaño
- ✅ Mejor rendimiento y estabilidad
- ✅ API mejorada para attributes

## 🔧 Instalación
1. Descarga Paper 1.21.10 de https://papermc.io/downloads/paper
2. Compila el plugin: `mvn clean package` (requiere Java 21)
3. Sube `FlappyMobs-*.jar` a `plugins/`
4. Reinicia el servidor

## ⚙️ Configuración
Edita `config.yml` y `flights.yml`. Activa debug para ver logs detallados:

```yaml
general:
  debug: true
```

## 🎮 Comandos

### Jugadores
| Comando | Descripción |
|---------|------------|
| `/fp flight <nombre>` | Inicia un vuelo |
| `/fp dismount` | Desmonta de la criatura |
| `/fp list` | Lista vuelos disponibles |
| `/fp info <nombre>` | Ver detalles del vuelo |

### Administradores
| Comando | Descripción |
|---------|------------|
| `/fp create <nombre> <mob> [costo]` | Crea un nuevo vuelo |
| `/fp setwp` | Añade waypoint |
| `/fp save` | Guarda el vuelo |
| `/fp delete <nombre>` | Elimina un vuelo |
| `/fp edit <nombre> <propiedad> <valor>` | Edita propiedades |
| `/fp removemobs` | Limpia criaturas sin jinete |
| `/fp reload` | Recarga configuración |
/fp send <nombre> <jugador>	Envía a un jugador a vuelo especificado

🔐 Permisos
Permiso	Descripción	Comandos relacionados
flappymobs.use	Permite usar el comando /fp básico	/fp flight, /fp list, /fp info, /fp dismount
flappymobs.flight	Permite iniciar vuelos con /fp flight	/fp flight
flappymobs.send	Permite enviar a otros jugadores a vuelos	/fp send
flappymobs.stop	Permite desmontar vuelo con /fp dismount	/fp dismount
flappymobs.create	Permite crear nuevos vuelos	/fp create, /fp setwp, /fp save
flappymobs.edit	Permite editar vuelos	/fp edit
flappymobs.delete	Permite eliminar vuelos	/fp delete
flappymobs.removemobs	Permite limpiar criaturas sin jinete	/fp removemobs
flappymobs.reload	Permite recargar configuración y mensajes	/fp reload

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

## 📝 Notas
- Requiere **Paper 1.21.10+** para scale completo
- Vault opcional para economía
- Debug mode muestra tracking detallado de movimiento

## 🔗 Enlaces
- Paper: https://papermc.io/
- Vault: https://www.spigotmc.org/resources/vault.34315/

---

**Versión**: 1.0.0 | **API**: Paper 1.21.3 | **Java**: 21

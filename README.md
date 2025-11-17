# 🐉 FlappyMobs

Sistema avanzado de vuelos con criaturas para Minecraft 1.20.1+/Paper 1.20.1.

## ✨ Características
- Rutas con waypoints personalizables
- 8 criaturas voladoras seleccionables
- Paracaídas configurable al desmontar o destruir el mob
- Economía Vault integrada
- Carteles `[FlappyMobs]` para vuelos automatizados
- Comandos intuitivos de crear/editar/eliminar vuelos

## 📦 Instalación
1. Compila usando `mvn clean package` (Java 17+)
2. Sube `FlappyMobs-*.jar` a `plugins/` en tu Paper 1.20.1
3. Reinicia el servidor

## ⚙️ Configuración
Edita `config.yml` y `flights.yml` para definir criaturas, rutas, economía y permisos.

## 🎮 Comandos

#### Comandos jugadores
| Comando                 | Descripción                                |
|-------------------------|--------------------------------------------|
| `/fp flight <nombre>`   | Inicia un vuelo en la ruta especificada    |
| `/fp dismount`          | Desmonta de la criatura                    |
| `/fp list`              | Muestra todos los vuelos disponibles       |
| `/fp info <nombre>`     | Ver detalles de un vuelo                   |

#### Comandos admins
| Comando | Descripción |
|---------|------------|
| `/fp create <nombre> <mob> [costo]` | Inicia la creación de un vuelo nuevo |
| `/fp setwp`      | Añade waypoint donde estés en el modo creación |
| `/fp remlastwp`  | Quita el último waypoint añadido              |
| `/fp save`       | Guarda el vuelo creado y sale del modo edición|
| `/fp cancel`     | Cancela completamente la creación             |
| `/fp delete <nombre>` | Elimina un vuelo guardado                  |
| `/fp edit <nombre> <propiedad> <valor>` | Edita una propiedad del vuelo     |
| `/fp removemobs` | Elimina todas las criaturas sin jugador       |
| `/fp reload`     | Recarga la configuración                     |

#### Carteles interactivoss
- Pon `[FlappyMobs]` en la primera línea, y el nombre del vuelo en la segunda.
- Haz click derecho para viajar o ver el destino/costo en el cartel.

## 🦅 Lista de criaturas
- ENDER_DRAGON
- PHANTOM
- GHAST
- BLAZE
- BEE
- PARROT
- VEX
- ALLAY

## 📝 Notas
- Los permisos de comandos y carteles están definidos en `plugin.yml`
- Paracaídas configurable por vuelo y global en config.yml
- Lectura detallada de cada comando en el readme original

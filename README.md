# STW V Planner 🚀
**La herramienta definitiva para gestionar tus pavos de Fortnite: Salvar el Mundo.**

---

## 📱 Guía para el Usuario

STW V Planner te permite llevar un control detallado de tus ingresos y gastos de pavos, con sincronización en la nube, respaldo automático y funciones colaborativas.

### ✨ Características Destacadas
- ⏱️ **Respaldo Automático Configurable**: Elige la frecuencia con la que se respaldan tus datos en la nube:
  - *Al abrir la app* (limitado a máximo 1 vez por día).
  - *1 Día* (diario).
  - *1 Semana* (predeterminado).
  - *1 Mes*.
  - *Personalizado* (especifica el número exacto de días).
- 🔘 **Control de Respaldo**: Interruptor sencillo para activar o desactivar el respaldo automático según tus preferencias, junto a un botón de configuración flotante ⚙️.
- 📂 **Menú Nube Reestructurado**: División clara entre **RESPALDO (Manual / Automático)** y **CÓDIGOS DE TRANSFERENCIA**.
- 🛡️ **Validación de Integridad**: Descargas de respaldo verificadas con hash SHA-256 y tamaño en bytes para garantizar que los datos estén intactos.
- ⚡ **Experiencia de Uso Ágil**: Inicio de app directo e instantáneo.
- 🔄 **Interfaz Refinada**: Animaciones fluidas e indicadores de carga precisos.

### 📊 Funciones Principales
- 📊 **Cuentas Ilimitadas**: Gestiona tu cuenta principal y las de tus dependientes o amigos por separado.
- ⚡ **Registro Veloz**: Botones rápidos para misiones diarias (+100 o +150) y alertas (+50).
- 📅 **Calendario Completo**: Historial visual mes a mes para un control total de tus ingresos y gastos.
- ☁️ **Nube Inteligente con Firebase**: Respaldo automático configurable y actualización de vistas compartidas al instante.

### 📥 Instalación
1. Descarga el archivo APK más reciente ubicado en la raíz de este proyecto o desde la sección de Releases en GitHub.
2. Abre el archivo en tu dispositivo Android y permite la instalación de "Fuentes desconocidas".
3. **Actualizaciones**: Recibirás una notificación automática dentro de la app cuando exista una nueva versión disponible en GitHub Releases.

---

## 🕹️ Guía de Uso Rápida

### 1. Modos de Acceso
- **Cuenta de Google**: Desbloquea todas las funciones en la nube: respaldo automático, subir/bajar datos manuales, códigos de transferencia y compartir vistas de solo lectura.
- **Modo Invitado / Local**: Operación 100% offline en tu dispositivo. Las opciones que requieren la nube de Firebase permanecen bloqueadas de forma segura hasta iniciar sesión.

### 2. Gestión de la Nube (Solo Cuenta de Google)
- **Respaldo Manual**:
  - *Subir a la nube*: Guarda tu base de datos completa en Firestore y actualiza tu vista compartida activa.
  - *Bajar de la nube*: Restaura tu información verificada con hash SHA-256.
- **Respaldo Automático**:
  - Activa el interruptor en el menú de la nube y presiona el engranaje ⚙️ para elegir el intervalo que mejor se adapte a ti.
- **Códigos de Transferencia**:
  - Genera códigos de 10 dígitos para migrar tu base de datos entre dispositivos de forma segura.

---

## 🛠️ Guía para Desarrolladores

### 🏗️ Arquitectura
- **Clean Architecture + MVVM**.
- **Workers**: `WorkManager` mediante `ConfigureAutomaticBackupUseCase` para respaldos automáticos periódicos o al inicio de sesión.
- **Data Layer**:
  - **Room Database**: Persistencia local.
  - **Firebase Firestore**: Respaldo en la nube con fragmentación por bloques (chunks) y verificación SHA-256.
  - **EmailJS API**: Envío de solicitudes de autorización de infraestructura (App Check).
  - **GitHub API**: Verificación automática de actualizaciones In-App.

---
Creado con ❤️ para la comunidad de Fortnite STW.

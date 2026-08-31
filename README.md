# STW V Planner 🚀
**La herramienta definitiva para gestionar tus pavos de Fortnite: Salvar el Mundo.**

---

## 📱 Guía para el Usuario (v2.1)

Esta versión introduce mejoras críticas en la estabilidad de los datos y el control de la nube.

### ✨ ¿Qué puedes hacer con STW V Planner?
- 📊 **Cuentas Ilimitadas**: Gestiona tu cuenta principal y las de tus amigos por separado.
- ⚡ **Registro Veloz**: Botones rápidos para misiones diarias (+100 o +150) y alertas (+50).
- 📝 **Modo Manual**: Elige la fecha, monto y descripción exacta para cada movimiento.
- 📅 **Calendario Completo**: Historial visual mes a mes para que no olvides ninguna misión.
- 🔔 **Recordatorio 6 PM**: Notificaciones automáticas para recordarte tus diarias.
- ☁️ **Respaldo Manual (Sobrescritura)**: Tú decides cuándo guardar. El botón "Respaldar" sube tu estado actual a la nube, reemplazando lo anterior y asegurando que lo que borres se quede borrado.
- 🔗 **Fuente Única de Verdad**: Los regalos son un solo registro compartido. Si lo editas en un lado, se actualiza en el otro al instante.

### 📥 Instalación
1. Descarga el archivo `STW_V_Planner_v2.1.apk` de la raíz de este proyecto.
2. Abre el archivo en tu Android y permite la instalación de "Fuentes desconocidas" si se solicita.
3. ¡Inicia sesión con Google y empieza a registrar!

---

## 🕹️ Guía de Uso Rápida

### 1. Selección de Cuenta
Al abrir la app, selecciona tu cuenta o crea una nueva. Puedes tener múltiples cuentas principales (ej. si gestionas cuentas de familiares).

### 2. Panel de Control y Regalos
- **Botón GASTO/REGALO**: Al registrar un gasto, puedes seleccionar a un "Dependiente". Esto restará los pavos de tu cuenta y los mostrará automáticamente como un ingreso en la cuenta del destinatario.
- **Deduplicación**: El sistema v2.1 detecta automáticamente registros antiguos y duplicados, mostrando solo la información relevante y corregida.

### 3. Gestión de la Nube (Menú Cloud)
- **Respaldar todo**: Sube tu base de datos local actual a Firestore. Úsalo cuando hayas terminado de hacer cambios o borrados importantes.
- **Restaurar todo**: Descarga tu último respaldo guardado. Úsalo si cambias de dispositivo o reinstalas la app.
- **Transferencia**: Genera códigos para que amigos puedan importar tu configuración de cuentas.

### 4. Historial Detallado
Presiona sobre cualquier fila del historial para **Editar** o **Borrar**. Gracias al nuevo modelo local, lo que borres no volverá a aparecer al reiniciar la app.

---

## 🛠️ Guía para Desarrolladores

### 🏗️ Arquitectura
- **Clean Architecture + MVVM**.
- **Jetpack Compose / Material 3**.
- **Offline-First**: La app funciona de forma local y solo usa red para Auth y Respaldos manuales.
- **Single Source of Truth**: Lógica avanzada en Room para vincular transacciones entre cuentas sin duplicar datos.

### 🚀 Configuración
1. Clonar el repositorio.
2. Configurar Firebase (Auth y Firestore).
3. Compilar con Android Studio Koala o superior.

---
Creado con ❤️ para la comunidad de Fortnite STW.

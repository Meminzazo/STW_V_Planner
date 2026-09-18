# STW V Planner 🚀
**La herramienta definitiva para gestionar tus pavos de Fortnite: Salvar el Mundo.**

---

## 📱 Guía para el Usuario (v4.0.0 - Versión Social & Nube Conectada)

Esta versión marca un hito en la conectividad del proyecto, permitiendo compartir cuentas, actualizar la app sin salir de ella y solicitar acceso a la infraestructura de forma oficial.

### ✨ Novedades de la v4.0.0
- 🔄 **Actualizador In-App**: Recibe notificaciones de nuevas versiones y descárgalas directamente desde el HUD de cuentas.
- 👥 **Vista Compartida de Solo Lectura**: Genera un código de 10 caracteres para que otros vean tus estadísticas (balance, gráficos de ingresos/gastos y transacciones recientes) sin riesgo de modificación.
- 📌 **Cuentas Vinculadas**: Guarda los códigos de tus amigos para acceder a sus vistas compartidas con un solo toque desde tu lista principal.
- 🌐 **Solicitud de Acceso**: ¿Sin acceso a la nube? Envía tu token de dispositivo directamente al administrador desde la app para recibir autorización rápida.
- ⚙️ **Mantenimiento Automático**: La app ahora limpia archivos temporales y versiones antiguas para ahorrar espacio.

### ✨ Funciones Principales
- 📊 **Cuentas Ilimitadas**: Gestiona tu cuenta principal y las de tus amigos por separado.
- ⚡ **Registro Veloz**: Botones rápidos para misiones diarias (+100 o +150) y alertas (+50).
- 📅 **Calendario Completo**: Historial visual mes a mes para un control total.
- ☁️ **Respaldo Manual**: Tú decides cuándo guardar. El botón "Subir a la nube" sincroniza tu estado y actualiza tu vista compartida automáticamente.

### 📥 Instalación
1. Descarga el archivo `STW_V_Planner_v4.0.0.apk` de la raíz de este proyecto.
2. Abre el archivo y permite la instalación de "Fuentes desconocidas".
3. A partir de esta versión, podrás actualizar automáticamente desde dentro de la app.

---

## 🕹️ Guía de Uso Rápida

### 1. Acceso y Social
- **Google**: Activa el respaldo en la nube, sincronización y generación de vistas compartidas.
- **Modo Invitado**: Uso local. Puedes ver cuentas compartidas de otros mediante código, pero no puedes subir las tuyas hasta vincular Google.
- **Compartir**: Entra al detalle de una cuenta, toca el icono de compartir y envía el código de 10 caracteres.

### 2. Gestión de la Nube (Solo Google)
- **Subir a la nube**: Respalda tu base de datos completa y actualiza tu vista compartida activa.
- **Bajar de la nube**: Recupera tus datos en cualquier momento.
- **Transferencia**: Genera códigos de 10 dígitos para mover toda tu base de datos a otro dispositivo (Caducidad: 1h).

---

## 🛠️ Guía para Desarrolladores

### 🏗️ Arquitectura
- **Clean Architecture + MVVM**.
- **Data**: Room (Local), Firestore (Nube), GitHub API (Actualizaciones).
- **Notificaciones**: Integración con EmailJS para solicitudes de infraestructura.

---
Creado con ❤️ para la comunidad de Fortnite STW.

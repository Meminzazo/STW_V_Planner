# STW V Planner 🚀
**La herramienta definitiva para gestionar tus pavos de Fortnite: Salvar el Mundo.**

---

## 📱 Guía para el Usuario (v4.1.0 - Automatización & HUD Espejo)

Esta versión perfecciona la experiencia social con sincronización automática de fondo y un visor de solo lectura que es un reflejo exacto de la cuenta real.

### ✨ Novedades de la v4.1.0
- 🤖 **Respaldo Automático**: Olvídate de subir tus datos manualmente. La app ahora realiza un backup y actualiza tu vista compartida cada semana en segundo plano.
- 🪞 **HUD Espejo Total**: El visor de solo lectura ahora permite ver desgloses por categoría y detalles de regalos, igual que la cuenta del dueño.
- ✅ **Verificación Instantánea**: ¿Pediste acceso a la nube? Usa el nuevo botón de verificación para entrar en cuanto el administrador te apruebe.
- 🛠️ **UX Refinada**: Mejoras en el teclado y formato de códigos para una experiencia más fluida.

### ✨ Funciones Principales
- 📊 **Cuentas Ilimitadas**: Gestiona tu cuenta principal y las de tus amigos por separado.
- ⚡ **Registro Veloz**: Botones rápidos para misiones diarias (+100 o +150) y alertas (+50).
- 📅 **Calendario Completo**: Historial visual mes a mes para un control total.
- ☁️ **Nube Inteligente**: Respaldo automático semanal y actualización de snapshots compartidos sin mover un dedo.

### 📥 Instalación
1. Descarga el archivo `STW_V_Planner_v4.1.0.apk` de la raíz de este proyecto.
2. Abre el archivo y permite la instalación de "Fuentes desconocidas".
3. **Actualizaciones**: Recibirás un aviso dentro de la app cada vez que haya una versión nueva disponible.

---

## 🕹️ Guía de Uso Rápida

### 1. Acceso y Social
- **Google**: Activa el respaldo en la nube, sincronización y generación de vistas compartidas.
- **Modo Invitado**: Uso local. Puedes ver cuentas compartidas de otros mediante código.
- **Vincular Cuentas**: Introduce el código de un amigo una vez y quedará guardado en tu lista principal para acceso rápido.

### 2. Gestión de la Nube (Solo Google)
- **Subir a la nube**: Respalda tu base de datos completa y actualiza tu vista compartida activa.
- **Bajar de la nube**: Recupera tus datos en cualquier momento.
- **Transferencia**: Genera códigos de 10 dígitos para mover toda tu base de datos a otro dispositivo (Caducidad: 1h).

---

## 🛠️ Guía para Desarrolladores

### 🏗️ Arquitectura
- **Clean Architecture + MVVM**.
- **Workers**: WorkManager para respaldos semanales y recordatorios.
- **Data**: Room (Local), Firestore (Nube), GitHub API (Actualizaciones).
- **Notificaciones**: Integración con EmailJS para solicitudes de infraestructura.

---
Creado con ❤️ para la comunidad de Fortnite STW.

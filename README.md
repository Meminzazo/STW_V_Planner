# STW V Planner 🚀
**La herramienta definitiva para gestionar tus pavos de Fortnite: Salvar el Mundo.**

---

## 📱 Guía para el Usuario (APK)

Si solo quieres usar la aplicación para llevar el control de tus pavos, lee esta sección.

### ✨ ¿Qué puedes hacer con STW V Planner?
- 📊 **Cuentas Ilimitadas**: Gestiona tu cuenta principal y las de tus amigos por separado.
- ⚡ **Registro Veloz**: Botones rápidos para misiones diarias (+100 o +150) y alertas (+50).
- 📝 **Modo Manual**: ¿Tienes datos en Excel? Pásalos aquí eligiendo la fecha y descripción exacta.
- 📅 **Calendario Completo**: Visualiza tu historial mes a mes. Los días vacíos te recordarán qué misiones te faltó registrar.
- 🔔 **Recordatorio 6 PM**: No pierdas ni una diaria. La app te avisará automáticamente a las 6:00 PM.
- ☁️ **Respaldo en la Nube**: Inicia sesión con Google para guardar tus datos. Si cambias de teléfono, solo presiona "Restaurar" y todo volverá a su lugar.
- 🔑 **Seguridad Total**: Tus datos son privados y están protegidos por tu cuenta de Google.

### 📥 Instalación
1. Descarga el archivo `.apk` de la sección de **Releases** (si está disponible).
2. Abre el archivo en tu Android y permite la instalación de "Fuentes desconocidas" si se solicita.
3. ¡Inicia sesión y empieza a ahorrar!

---

## 🛠️ Guía para Desarrolladores

Si eres programador y quieres ver cómo está hecha la app o colaborar, esta sección es para ti.

### 🏗️ Arquitectura y Tecnologías
Este proyecto sigue los principios de **Clean Architecture** y **MVVM**:
- **UI**: Jetpack Compose con Material 3 (Tema personalizado STW).
- **Inyección de Dependencias**: Hilt (Dagger).
- **Base de Datos**: Room (SQLite) con estrategia *Offline-First*.
- **Backend**: Firebase (Authentication & Cloud Firestore).
- **Asincronía**: Kotlin Coroutines & Flow.
- **Background Tasks**: WorkManager (Sincronización y Notificaciones).

### 🚀 Configuración del Entorno
1. **Clonar**: `git clone https://github.com/tu-usuario/STW-V-Planner.git`
2. **Firebase**:
   - Crea un proyecto en [Firebase Console](https://console.firebase.google.com/).
   - Añade una app Android con el paquete `com.meminzazo.stwvplanner`.
   - Genera tu SHA-1 de debug (`./gradlew signingReport`) y regístralo en Firebase.
   - Descarga el `google-services.json` y ponlo en `/app`.
3. **Firestore**:
   - Habilita Firestore en modo producción.
   - Usa las reglas de seguridad incluidas en el archivo `firestore.rules` del repositorio.

### 📜 Comentarios del Código
Todo el código fuente está documentado en español para facilitar su comprensión, especialmente las partes de sincronización y fragmentación de datos (*chunking*).

---
Creado con ❤️ para la comunidad de Fortnite STW.

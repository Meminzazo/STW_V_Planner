# STW V Planner 🚀
**La herramienta definitiva para gestionar tus pavos de Fortnite: Salvar el Mundo.**

---

## 📱 Guía para el Usuario (v3.1.4 - Estabilidad & HUD Refinado)

Esta versión consolida la seguridad de la v3.0 con correcciones críticas de acceso y un HUD (interfaz) más limpio y profesional.

### ✨ Novedades de la v3.1.4
- 🛠️ **Fix Acceso Total**: Corregido el error de Google Sign-in y App Check que impedía el inicio de sesión en algunos dispositivos.
- ⚡ **Importación Optimizada**: Se eliminó el bloqueo de seguridad incorrecto al usar códigos de transferencia, permitiendo recuperaciones instantáneas.
- 🎨 **Rediseño HUD**:
    - **Acciones Rápidas**: Nuevo espaciado para evitar toques accidentales.
    - **Jerarquía Visual**: Encabezados más claros y tipografía optimizada para lectura rápida.
    - **Desglose Inteligente**: La columna "Cuenta" ahora se oculta en Ingresos para dar más espacio a las descripciones.
- 🥈 **Detalle de Regalos**: Visualización de totales mensuales en tono plateado y sin prefijos redundantes para una estética más limpia.
- 👤 **Modo Invitado Mejorado**: Ahora es más sencillo vincular tu cuenta de Google desde el HUD local sin perder tus registros previos.

### ✨ Funciones Principales
- 📊 **Cuentas Ilimitadas**: Gestiona tu cuenta principal y las de tus amigos por separado.
- ⚡ **Registro Veloz**: Botones rápidos para misiones diarias (+100 o +150) y alertas (+50).
- 📅 **Calendario Completo**: Historial visual mes a mes para un control total.
- ☁️ **Respaldo Manual**: Tú decides cuándo guardar. El botón "Respaldar" sube tu estado actual a la nube mediante sobrescritura segura.

### 📥 Instalación
1. Descarga el archivo `STW_V_Planner_v3.1.4.apk` de la raíz de este proyecto.
2. **IMPORTANTE**: Si tienes versiones anteriores a la v3.0, desinstálalas primero. 
3. Abre el archivo y permite la instalación de "Fuentes desconocidas".

---

## 🕹️ Guía de Uso Rápida

### 1. Acceso
- **Google**: Usa tu cuenta para activar el respaldo en la nube y sincronización.
- **Invitado**: Uso puramente local. Ideal si quieres privacidad extrema o no tienes internet. **¡Puedes vincular Google en cualquier momento para activar la nube!**

### 2. Gestión de la Nube (Solo Google)
- **Respaldar**: Sube tu base de datos a la nube. Reemplaza lo anterior.
- **Restaurar**: Baja tus datos en un nuevo dispositivo.
- **Transferencia**: Genera códigos de 10 dígitos. Los códigos caducan en **1 hora** por seguridad.

---

## 🛠️ Guía para Desarrolladores

### 🏗️ Arquitectura
- **MVVM + Clean Architecture**.
- **Seguridad**: Firebase App Check (Play Integrity) + Throttling de red.
- **Base de Datos**: Room con transacciones atómicas y `withTransaction` para integridad de datos.

---
Creado con ❤️ para la comunidad de Fortnite STW.

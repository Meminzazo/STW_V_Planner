# STW V Planner 🚀

Aplicación Android para llevar el control de **Pavos de Fortnite: Salvar el Mundo (STW)**.

Permite gestionar varias cuentas, registrar ganancias, consultar el historial, realizar copias de seguridad y compartir una cuenta mediante una vista de solo lectura.

## ✨ Características

* 👤 Gestión de múltiples cuentas.
* ⚡ Registro rápido de misiones y alertas.
* 📅 Historial y calendario de ganancias.
* ☁️ Copias de seguridad y restauración.
* 🤖 Respaldo automático semanal.
* 🪞 **Shared View / HUD Mirror** de solo lectura.
* 🔄 Transferencia de datos entre dispositivos mediante código temporal.
* 🔐 Inicio de sesión con Google y funciones de nube.
* 🔄 Comprobación de nuevas versiones desde GitHub.

## 📥 Instalación

Descarga la versión más reciente desde **GitHub Releases** e instala el APK.

En Android puede ser necesario permitir la instalación desde fuentes desconocidas para la aplicación desde la que abras el APK.

---

# 🕹️ Guía rápida

## 1. Crear una cuenta

Al iniciar la aplicación puedes utilizarla en **modo local** o iniciar sesión con Google.

Para utilizar las funciones de nube, inicia sesión con tu cuenta de Google.

Una vez dentro, crea o selecciona la cuenta de STW que quieras administrar.

---

## 2. Registrar ganancias

Desde la pantalla principal selecciona la cuenta correspondiente.

Utiliza los botones de registro rápido para añadir las ganancias obtenidas:

* **+100** — Misión diaria.
* **+150** — Misión diaria.
* **+50** — Alerta.

Los movimientos registrados aparecen en el historial y actualizan el balance de la cuenta.

---

## 3. Consultar el historial

Abre el calendario para consultar los registros por fecha.

Puedes revisar:

* Ganancias diarias.
* Historial de movimientos.
* Balance de la cuenta.
* Estadísticas y gráficas.
* Desglose de las ganancias por categoría.

---

## 4. Copia de seguridad

Si tienes una cuenta de Google configurada:

**Subir a la nube**
→ Guarda una copia de tus datos y actualiza la vista compartida activa.

**Bajar de la nube**
→ Recupera los datos almacenados en la nube.

Además, la aplicación puede realizar un **respaldo automático semanal** en segundo plano.

---

## 5. Compartir una cuenta

Desde las opciones de la cuenta puedes generar una **Shared View**.

La aplicación genera un código que puedes compartir con otra persona.

La persona que introduzca el código podrá consultar la información disponible, incluyendo el balance, historial y estadísticas, pero **no podrá modificar la cuenta**.

> Trata el código como privado. Cualquier persona que lo tenga podrá acceder a la información asociada a esa vista.

---

## 6. Vincular una cuenta compartida

Si otra persona te proporciona un código de Shared View:

1. Introduce el código en la opción correspondiente.
2. La cuenta aparecerá entre tus cuentas vinculadas.
3. Podrás consultarla sin modificar sus datos.

Esto permite tener varias cuentas compartidas disponibles desde la misma aplicación.

---

## 7. Transferir tus datos a otro dispositivo

Para mover toda tu base de datos:

**En el dispositivo anterior**

1. Abre la opción de **Transferencia**.
2. Genera un código.
3. Guarda el código.

**En el nuevo dispositivo**

1. Abre la opción de transferencia.
2. Introduce el código.
3. Confirma la importación.

Los códigos de transferencia son temporales y tienen una duración de **1 hora**.

> No compartas un código de transferencia con otras personas.

---

## 🛠️ Tecnologías

* Kotlin
* Jetpack Compose
* Clean Architecture + MVVM
* Room
* Hilt
* Firebase Authentication
* Cloud Firestore
* Firebase App Check
* WorkManager
* GitHub API
* EmailJS

## 📌 Información

**Versión actual:** 4.1.0

Proyecto independiente para la comunidad de Fortnite: Salvar el Mundo.

No está afiliado, patrocinado ni respaldado por Epic Games.

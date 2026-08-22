package com.meminzazo.stwvplanner.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meminzazo.stwvplanner.R

/**
 * Trabajador encargado de mostrar la notificación de recordatorio diario.
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_reminder_channel"

        // Crear canal de notificación para Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios Diarios",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones para recordar las misiones diarias de Salvar el Mundo"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Construir la notificación
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("¿Ya hiciste tu misión diaria?")
            .setContentText("No olvides registrar tus pavos ganados hoy en STW V Planner.")
            .setSmallIcon(R.drawable.ic_vbucks) // Usando el nuevo icono
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}

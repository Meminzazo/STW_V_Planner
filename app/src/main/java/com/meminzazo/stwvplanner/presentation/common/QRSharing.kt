package com.meminzazo.stwvplanner.presentation.common

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.SharingPayload
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object QRSharing {
    private val gson = Gson()

    // DTO optimizado para reducir tamaño en el QR
    private data class CompactTx(val a: Int, val t: Int, val s: Int, val d: String, val dt: Long)
    private data class CompactPayload(val n: String, val m: Boolean, val txs: List<CompactTx>)

    fun generateQRCode(payload: SharingPayload): Bitmap? {
        return try {
            // 1. Convertir a formato compacto (Bajamos a 15 transacciones para máxima compatibilidad)
            val compact = CompactPayload(
                n = payload.account.name,
                m = payload.account.isMain,
                txs = payload.transactions.take(15).map { 
                    CompactTx(it.amount, it.type.ordinal, it.source.ordinal, it.description.take(20), it.date) 
                }
            )
            val json = gson.toJson(compact)

            // 2. Comprimir con GZIP
            val bos = ByteArrayOutputStream()
            GZIPOutputStream(bos).use { it.write(json.toByteArray()) }
            val compressedBase64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)

            // 3. Generar QR
            val matrix = MultiFormatWriter().encode(compressedBase64, BarcodeFormat.QR_CODE, 512, 512)
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) {
                for (y in 0 until 512) {
                    bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseQRCode(base64: String): SharingPayload? {
        return try {
            // 1. Descomprimir GZIP
            val compressed = Base64.decode(base64, Base64.NO_WRAP)
            val bis = ByteArrayInputStream(compressed)
            val json = GZIPInputStream(bis).bufferedReader().readText()

            // 2. Parsear formato compacto
            val compact = gson.fromJson(json, CompactPayload::class.java)
            
            // 3. Reconstruir objeto real
            SharingPayload(
                account = Account(name = compact.n, isMain = compact.m),
                transactions = compact.txs.map { 
                    Transaction(
                        accountId = 0,
                        amount = it.a,
                        type = TransactionType.entries[it.t],
                        source = VBucksSource.entries[it.s],
                        description = it.d,
                        date = it.dt
                    )
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

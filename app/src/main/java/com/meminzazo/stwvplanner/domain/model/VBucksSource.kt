package com.meminzazo.stwvplanner.domain.model

enum class VBucksSource {
    DAILY,      // Misión Diaria (80, 90, 100, 130, 150)
    ALERT,      // Alerta de Misión (50)
    SSD,        // Defensa de Escudo
    BATTLE_PASS,// Pase de Batalla
    SHOP,       // Tienda (Gasto)
    GIFT,       // Regalo (Gasto)
    ADJUSTMENT, // Ajuste de Epic / Otros
    PACK        // Paquete de inicio / Historia
}

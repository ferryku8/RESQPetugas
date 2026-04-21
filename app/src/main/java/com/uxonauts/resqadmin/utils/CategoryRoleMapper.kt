package com.uxonauts.resqadmin.utils

object CategoryRoleMapper {

    fun getTargetRoles(category: String): List<Int> {
        return when (category.lowercase()) {
            "kecelakaan" -> listOf(2, 3) // Polisi + Medis
            "kebakaran" -> listOf(2, 3, 4) // Polisi + Medis + Damkar
            "darurat medis" -> listOf(3) // Medis saja
            "tindak kriminal" -> listOf(2) // Polisi saja
            "bencana alam" -> listOf(2, 3, 4) // Semua kecuali admin
            "orang hilang" -> listOf(2) // Polisi saja
            else -> listOf(2) // Default polisi
        }
    }

    fun categoryDescription(category: String): String {
        val roles = getTargetRoles(category)
        val names = roles.map { roleId ->
            when (roleId) {
                2 -> "Polisi"
                3 -> "Medis/Ambulans"
                4 -> "Damkar"
                else -> ""
            }
        }.filter { it.isNotEmpty() }
        return "Alert dikirim ke: ${names.joinToString(", ")}"
    }
}
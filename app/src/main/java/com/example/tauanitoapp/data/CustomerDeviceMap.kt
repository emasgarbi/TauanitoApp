package com.example.tauanitoapp.data

/**
 * Mappa statica cliente → insieme di ID device (estratti dall'URL /device/{id}/dati).
 * Per device con nome speciale (non numerico) si fa substring-match sul nome.
 */
object CustomerDeviceMap {

    private val map: Map<String, Set<String>> = mapOf(
        "ADA RICERCA"                          to setOf("81", "146"),
        "Aeroporto Marconi"                    to setOf("40", "45", "48", "59", "60", "63", "64", "65", "66", "67", "68", "69", "135"),
        "Agrofertil Tecnico"                   to setOf("136", "137", "138", "139"),
        "ALISEA"                               to setOf("134", "140", "141", "142", "144"),
        "Aria Suolo Acqua ASA Srl"             to setOf("61"),
        "Arienti Condominio"                   to setOf("51"),
        "Aura System"                          to setOf("82"),
        "Bacchi"                               to setOf("BACCHI - UFFICI", "BACCHI - Area Ristoro", "Bacchi - Sala Riunioni", "EKORU"),
        "Bellini Tiziana S.r.l. a socio unico" to setOf("TN165", "TN166", "TN167", "TN168", "TN169"),
        "BIOGEOTEK"                            to setOf("7"),
        "Bsg tech"                             to setOf("24", "35"),
        "Comune di Bologna"                    to setOf("97", "98", "105", "106", "107", "108", "110", "111", "112", "114"),
        "CONFINDUSTRIA"                        to setOf("57"),
        "CTE MOLISE"                           to setOf("93"),
        "Designtech"                           to setOf("152", "153"),
        "DT4 Benefit Srlb"                     to setOf("TN118", "DT4 Benefit"),
        "Evoluzione servizi"                   to setOf("30", "34"),
        "FILAIR"                               to setOf("TN158"),
        "Gaetano Ingenito"                     to setOf("46", "52", "53", "72"),
        "Gaetano Settimo"                      to setOf("91"),
        "Gianfranco Silvestri"                 to setOf("Taua 117"),
        "IQC"                                  to setOf("99"),
        "Logimatic"                            to setOf("83", "84", "85", "86", "87"),
        "Montanari Galletti"                   to setOf("74"),
        "NUOVASORMU"                           to setOf("39"),
        "OBS"                                  to setOf("95", "104"),
        "OLTRE SRL"                            to setOf("126", "127", "128", "130", "131", "132"),
        "SAVIO"                                to setOf("56"),
        "Signify"                              to setOf("25"),
        "Strobilo"                             to setOf("58"),
        "Taua"                                 to setOf(
            "1", "12", "21", "47", "49", "54", "70", "75", "76", "78", "79", "80",
            "92", "96", "113", "133", "143", "151",
            "TN154", "TN155", "TN156", "TN157", "TN159", "TN160",
            "TN161", "TN162", "TN163", "TN164", "TN170", "TN172", "TN173"
        )
    )

    /**
     * Restituisce true se il device appartiene al cliente selezionato.
     *
     * Strategia di match:
     * - ID puramente numerico → match esatto sull'ID estratto dall'URL
     * - ID con prefisso TN   → match normalizzando gli spazi (es. "TN 163" = "TN163")
     * - ID testuale           → substring nel nome del device (es. "BACCHI")
     */
    fun matchesCustomer(customer: String, deviceId: String, deviceName: String): Boolean {
        val ids = map[customer] ?: return false
        val normId   = deviceId.replace(" ", "").lowercase()
        val normName = deviceName.lowercase()
        return ids.any { id ->
            when {
                id.all { it.isDigit() } ->
                    normId == id

                id.startsWith("TN", ignoreCase = true) ->
                    normId == id.replace(" ", "").lowercase()

                else ->
                    normName.contains(id.lowercase())
            }
        }
    }
}

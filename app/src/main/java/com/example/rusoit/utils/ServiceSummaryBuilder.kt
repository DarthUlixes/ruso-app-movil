package com.example.rusoit.utils

import com.example.rusoit.data.model.Folio

object ServiceSummaryBuilder {
    fun build(folio: Folio): String {
        // 1. Si ya existe un resumen narrativo manual en la base de datos, lo respetamos
        if (!folio.summary.isNullOrBlank()) return folio.summary

        // 2. Extracción de datos básicos (WebApp Logic Mirror)
        val nameService = "Servicio Operativo"
        val folioNum = folio.folio ?: "N/A"
        val dateOpen = folio.date_to_open?.take(10) ?: "FECHA S/N"
        val timeOpen = folio.time_to_open ?: "00:00"
        val street = folio.stret ?: "Calle no especificada"
        val crossing = if (!folio.crossing.isNullOrBlank()) ", al cruce con ${folio.crossing}" else ""
        val reporter = folio.reporter ?: "Anónimo"
        val phoneReporter = if (!folio.phone_reporter.isNullOrBlank()) 
            "proporcionó el contacto ${folio.phone_reporter}" 
            else "no proporcionó contacto"
        
        // 3. Gestión de Tripulación (Portado de tripulacionTexto en summaryService.js)
        val crew = folio.personal_on_a_service ?: emptyList()
        val chofer = crew.find { it.position_on_service?.lowercase()?.contains("chofer") == true || it.position_on_service?.lowercase()?.contains("conductor") == true }
            ?.let { "${it.users?.first_name ?: ""} ${it.users?.last_name ?: ""}" } ?: "[Personal No Asignado]"
        
        val assistants = crew.filter { 
            val pos = it.position_on_service?.lowercase() ?: ""
            !pos.contains("chofer") && !pos.contains("conductor") 
        }.map { "${it.users?.first_name ?: ""} ${it.users?.last_name ?: ""}" }
        
        val tripulacionTexto = if (assistants.isNotEmpty()) {
            " en compañía de " + when {
                assistants.size == 1 -> assistants[0]
                else -> assistants.dropLast(1).joinToString(", ") + " y " + assistants.last()
            } + "."
        } else "."

        val conclusion = folio.general_and_conclusion ?: "Incidente atendido según protocolos de emergencia."

        // 4. Bloque Narrativo Base
        val base = """
            ${nameService.uppercase()}: $folioNum

            El día $dateOpen, a las $timeOpen horas, se recibió reporte de ${nameService.lowercase()} en la ubicación $street$crossing. El reporte fue realizado por $reporter, quien $phoneReporter.

            En seguimiento a la atención del incidente, se dirigió al lugar la unidad operativa con número económico U-${folio.vehicle_id ?: "S/N"}, operada por $chofer$tripulacionTexto A su arribo, el personal llevó a cabo una evaluación inicial de la situación y las maniobras de control correspondientes.

            Durante la atención del servicio, se dejaron constancia de las siguientes acciones: $conclusion Las labores se desarrollaron conforme a los procedimientos operativos establecidos.
        """.trimIndent()

        // 5. Bloque de Cierre (Solo si el servicio está concluido)
        return if (!folio.date_to_close.isNullOrBlank()) {
            val closure = """

            Una vez confirmada que la situación se encontraba bajo control y se descartaron riesgos para la población y el personal, el servicio se dio por concluido el día ${folio.date_to_close.take(10)} a las ${folio.time_to_close ?: ""}, quedando el evento con estatus ${folio.status?.uppercase() ?: "CERRADO"} y tipo de cierre: ${folio.close_type ?: "Finalizado"}.

            La unidad registró un kilometraje inicial de ${folio.kilometers ?: 0} y finalizó con ${folio.close_kilometers ?: "N/A"}, recorriendo un total de ${folio.kilometers_traveled ?: "N/A"} kilómetros durante el servicio.
            """.trimIndent()
            base + closure
        } else {
            base + "\n\nActualmente el servicio continúa en atención, manteniéndose con estatus ${folio.status?.uppercase() ?: "EN CURSO"}."
        }
    }
}

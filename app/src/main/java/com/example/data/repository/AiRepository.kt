package com.example.data.repository

import kotlinx.coroutines.delay

data class AiDescriptionResult(
    val text: String,
    val isPlaceholder: Boolean
)

interface AiRepository {
    suspend fun generateComponentDescription(
        nome: String,
        categoria: String,
        codigoSku: String
    ): AiDescriptionResult
}

/**
 * Implementação de demonstração usada enquanto o Firebase AI Logic não está habilitado.
 */
class PlaceholderAiRepository : AiRepository {
    override suspend fun generateComponentDescription(
        nome: String,
        categoria: String,
        codigoSku: String
    ): AiDescriptionResult {
        delay(350)
        val nomeLimpo = nome.ifBlank { "Componente Eletrônico" }
        val skuLine = if (codigoSku.isNotBlank()) "Código/SKU: $codigoSku\n" else ""

        val sample = buildString {
            appendLine("[Exemplo ilustrativo — requer Firebase AI Logic para IA Gemini real]")
            appendLine()
            appendLine("Componente: $nomeLimpo")
            appendLine("Categoria: $categoria")
            if (skuLine.isNotEmpty()) append(skuLine)
            appendLine()
            appendLine("1. Função Técnica:")
            appendLine("• Componente aplicado em circuitos de manutenção eletrônica e sistemas de segurança ($categoria), responsável por proteção, chaveamento, filtragem ou alimentação conforme especificação do fabricante.")
            appendLine()
            appendLine("2. Aplicações Comuns na Oficina:")
            appendLine("• Placas inversoras de portão eletrônico, centrais de alarme, fontes chaveadas, nobreaks, DVRs/NVRs e câmeras CFTV.")
            appendLine()
            appendLine("3. Inspeção & Bancada:")
            appendLine("• Conferir encapsulamento, pinagem, tensão/corrente máxima e polaridade antes da soldagem.")
            appendLine("• Realizar teste de continuidade/queda de tensão com multímetro e verificar componentes adjacentes em curto.")
        }.trim()

        return AiDescriptionResult(
            text = sample,
            isPlaceholder = true
        )
    }
}

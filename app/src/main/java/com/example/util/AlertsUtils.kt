package com.example.util

import com.example.data.model.OrdemServicoDto
import com.example.data.model.PecaEstoqueDto
import com.example.data.model.PedidoPecaDto
import com.example.data.model.PedidoPecaStatus
import com.example.data.model.StatusFinanceiro
import com.example.data.model.StatusOS
import com.example.data.model.TipoTransacao
import com.example.data.model.TransacaoFinanceiraDto

data class TransacaoAlertItem(
    val transacao: TransacaoFinanceiraDto,
    val diasEmAtraso: Long
)

data class OsAlertItem(
    val os: OrdemServicoDto,
    val diasEmAberto: Long
)

data class WorkshopAlerts(
    val contasReceberMais30Dias: List<TransacaoAlertItem> = emptyList(),
    val contasFixasEmAtraso: List<TransacaoAlertItem> = emptyList(),
    val ordensMais30Dias: List<OsAlertItem> = emptyList(),
    val pedidosPecasPendentes: List<PedidoPecaDto> = emptyList(),
    val pecasEstoqueBaixoCount: Int = 0
) {
    val totalContasReceber30Dias: Double
        get() = contasReceberMais30Dias.sumOf { it.transacao.valor }

    val totalContasFixasEmAtraso: Double
        get() = contasFixasEmAtraso.sumOf { it.transacao.valor }

    val activeAlertsCount: Int
        get() = contasReceberMais30Dias.size +
            contasFixasEmAtraso.size +
            ordensMais30Dias.size +
            pedidosPecasPendentes.size +
            (if (pecasEstoqueBaixoCount > 0) 1 else 0)

    val financeiroAlertsCount: Int
        get() = contasReceberMais30Dias.size + contasFixasEmAtraso.size
}

object AlertCalculator {

    fun diasSemConclusao(os: OrdemServicoDto, referenceDateIso: String = Formatters.todayIsoDate()): Long {
        if (os.statusEnum == StatusOS.ENTREGUE) return 0L
        val baseDate = os.dataEntrada?.takeIf { it.isNotBlank() } ?: os.createdAt
        return Formatters.daysBetweenIsoDates(baseDate, referenceDateIso).coerceAtLeast(0L)
    }

    fun diasEmAtraso(tx: TransacaoFinanceiraDto, referenceDateIso: String = Formatters.todayIsoDate()): Long {
        if (tx.statusEnum == StatusFinanceiro.PAGO) return 0L
        val baseDate = tx.dataVencimento.takeIf { it.isNotBlank() } ?: tx.createdAt
        return Formatters.daysBetweenIsoDates(baseDate, referenceDateIso).coerceAtLeast(0L)
    }

    fun compute(
        ordens: List<OrdemServicoDto>,
        transacoes: List<TransacaoFinanceiraDto>,
        pedidosPecas: List<PedidoPecaDto>,
        pecas: List<PecaEstoqueDto>,
        canAccessFinanceiro: Boolean,
        referenceDateIso: String = Formatters.todayIsoDate()
    ): WorkshopAlerts {
        val os30d = ordens
            .filter { it.statusEnum != StatusOS.ENTREGUE }
            .mapNotNull { os ->
                val dias = diasSemConclusao(os, referenceDateIso)
                if (dias > 30L) OsAlertItem(os = os, diasEmAberto = dias) else null
            }
            .sortedByDescending { it.diasEmAberto }

        val receber30d = if (canAccessFinanceiro) {
            transacoes
                .filter { it.tipoEnum == TipoTransacao.RECEBER && it.statusEnum == StatusFinanceiro.PENDENTE }
                .mapNotNull { tx ->
                    val dias = diasEmAtraso(tx, referenceDateIso)
                    if (dias > 30L) TransacaoAlertItem(transacao = tx, diasEmAtraso = dias) else null
                }
                .sortedByDescending { it.diasEmAtraso }
        } else emptyList()

        val fixasAtraso = if (canAccessFinanceiro) {
            transacoes
                .filter {
                    it.tipoEnum == TipoTransacao.PAGAR &&
                        it.statusEnum == StatusFinanceiro.PENDENTE &&
                        it.isContaFixaEfetiva
                }
                .mapNotNull { tx ->
                    val dias = diasEmAtraso(tx, referenceDateIso)
                    if (dias >= 1L) TransacaoAlertItem(transacao = tx, diasEmAtraso = dias) else null
                }
                .sortedByDescending { it.diasEmAtraso }
        } else emptyList()

        val pedidosPendentes = pedidosPecas.filter { it.statusEnum != PedidoPecaStatus.RECEBIDA }
        val lowStockCount = pecas.count { it.isEstoqueBaixo }

        return WorkshopAlerts(
            contasReceberMais30Dias = receber30d,
            contasFixasEmAtraso = fixasAtraso,
            ordensMais30Dias = os30d,
            pedidosPecasPendentes = pedidosPendentes,
            pecasEstoqueBaixoCount = lowStockCount
        )
    }
}

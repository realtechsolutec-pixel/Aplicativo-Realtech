package com.example.util

import com.example.data.model.FuncionarioDto
import com.example.data.model.OrdemServicoDto
import com.example.data.model.StatusOS
import com.example.data.model.UserSession

data class OsCommissionBreakdown(
    val os: OrdemServicoDto,
    val tecnicoNome: String,
    val percentApplied: Double,
    val ganhoTecnico: Double,
    val valorGeradoLoja: Double,
    val liquidoLoja: Double,
    val isFinalizada: Boolean
)

data class TechnicianPerformanceSummary(
    val key: String,
    val funcionario: FuncionarioDto?,
    val nome: String,
    val email: String,
    val cargo: String,
    val ativo: Boolean,
    val comissaoPadraoPercent: Double,
    val osGeradasCount: Int,
    val osFinalizadasCount: Int,
    val osEmAndamentoCount: Int,
    val totalGeradoLojaFinalizado: Double,
    val totalGeradoLojaEmAndamento: Double,
    val maoObraFinalizada: Double,
    val pecasFinalizadas: Double,
    val comissaoConfirmada: Double,
    val comissaoPrevista: Double,
    val liquidoLojaConfirmado: Double,
    val ordensDoTecnico: List<OsCommissionBreakdown>
)

object CommissionCalculator {

    fun findMatchingFuncionario(
        os: OrdemServicoDto,
        funcionarios: List<FuncionarioDto>
    ): FuncionarioDto? {
        if (os.tecnicoId != null) {
            funcionarios.find { it.id == os.tecnicoId }?.let { return it }
        }
        if (os.tecnicoEmail.isNotBlank()) {
            funcionarios.find { it.email.equals(os.tecnicoEmail.trim(), ignoreCase = true) }?.let { return it }
        }
        if (os.tecnicoNome.isNotBlank()) {
            funcionarios.find { it.nome.equals(os.tecnicoNome.trim(), ignoreCase = true) }?.let { return it }
        }
        return null
    }

    fun findFuncionarioForSession(
        session: UserSession?,
        funcionarios: List<FuncionarioDto>
    ): FuncionarioDto? {
        if (session == null) return null
        return funcionarios.find {
            (it.funcionarioUserId != null && it.funcionarioUserId == session.userId) ||
                it.email.equals(session.email.trim(), ignoreCase = true) ||
                it.nome.equals(session.fullName.trim(), ignoreCase = true)
        }
    }

    fun resolveEffectivePercent(
        os: OrdemServicoDto,
        funcionarios: List<FuncionarioDto>
    ): Double {
        if (os.comissaoPercentual > 0.0) {
            return os.comissaoPercentual.coerceIn(0.0, 100.0)
        }
        val matched = findMatchingFuncionario(os, funcionarios)
        return (matched?.comissaoPercentual ?: 0.0).coerceIn(0.0, 100.0)
    }

    fun calculateGanho(
        valorMaoObra: Double,
        percentual: Double
    ): Double {
        val cleanMaoObra = valorMaoObra.coerceAtLeast(0.0)
        val cleanPct = percentual.coerceIn(0.0, 100.0)
        return cleanMaoObra * (cleanPct / 100.0)
    }

    fun buildBreakdown(
        os: OrdemServicoDto,
        funcionarios: List<FuncionarioDto>
    ): OsCommissionBreakdown {
        val matched = findMatchingFuncionario(os, funcionarios)
        val pct = resolveEffectivePercent(os, funcionarios)
        val ganho = calculateGanho(os.valorMaoObra, pct)
        val totalLoja = os.valorTotal.coerceAtLeast(0.0)
        val liquido = (totalLoja - ganho).coerceAtLeast(0.0)
        val nome = os.tecnicoNome.ifBlank {
            matched?.nome ?: os.criadoPorNome.ifBlank { "Não atribuído" }
        }
        return OsCommissionBreakdown(
            os = os,
            tecnicoNome = nome,
            percentApplied = pct,
            ganhoTecnico = ganho,
            valorGeradoLoja = totalLoja,
            liquidoLoja = liquido,
            isFinalizada = os.statusEnum == StatusOS.ENTREGUE
        )
    }

    fun matchesEmployee(
        os: OrdemServicoDto,
        funcionario: FuncionarioDto?,
        userName: String,
        userEmail: String
    ): Boolean {
        if (funcionario != null) {
            if (!funcionario.id.isNullOrBlank() && os.tecnicoId == funcionario.id) return true
            if (os.tecnicoEmail.isNotBlank() && os.tecnicoEmail.equals(funcionario.email, ignoreCase = true)) return true
            if (os.tecnicoNome.isNotBlank() && os.tecnicoNome.equals(funcionario.nome, ignoreCase = true)) return true
        }
        if (userEmail.isNotBlank() && os.tecnicoEmail.equals(userEmail.trim(), ignoreCase = true)) return true
        if (userName.isNotBlank() && os.tecnicoNome.equals(userName.trim(), ignoreCase = true)) return true
        if (os.tecnicoNome.isBlank() && os.tecnicoId.isNullOrBlank() && userName.isNotBlank() &&
            os.criadoPorNome.equals(userName.trim(), ignoreCase = true)
        ) {
            return true
        }
        return false
    }

    fun filterOrdensForEmployee(
        ordens: List<OrdemServicoDto>,
        funcionario: FuncionarioDto?,
        userName: String,
        userEmail: String
    ): List<OrdemServicoDto> {
        return ordens.filter { matchesEmployee(it, funcionario, userName, userEmail) }
    }

    fun buildTeamPerformance(
        ordens: List<OrdemServicoDto>,
        funcionarios: List<FuncionarioDto>
    ): List<TechnicianPerformanceSummary> {
        val summaries = mutableListOf<TechnicianPerformanceSummary>()
        val assignedOsIds = mutableSetOf<String>()

        funcionarios.forEach { func ->
            val funcOrders = ordens.filter { os ->
                val matchesTech = (!func.id.isNullOrBlank() && os.tecnicoId == func.id) ||
                    (os.tecnicoEmail.isNotBlank() && os.tecnicoEmail.equals(func.email, ignoreCase = true)) ||
                    (os.tecnicoNome.isNotBlank() && os.tecnicoNome.equals(func.nome, ignoreCase = true))
                if (matchesTech && !os.id.isNullOrBlank()) {
                    assignedOsIds.add(os.id)
                }
                matchesTech
            }

            val geradasCount = ordens.count { os ->
                os.criadoPorNome.equals(func.nome, ignoreCase = true) ||
                    (!func.id.isNullOrBlank() && os.tecnicoId == func.id) ||
                    (os.tecnicoEmail.isNotBlank() && os.tecnicoEmail.equals(func.email, ignoreCase = true)) ||
                    (os.tecnicoNome.isNotBlank() && os.tecnicoNome.equals(func.nome, ignoreCase = true))
            }

            val breakdowns = funcOrders.map { buildBreakdown(it, funcionarios) }
            val finalizadas = breakdowns.filter { it.isFinalizada }
            val emAndamento = breakdowns.filterNot { it.isFinalizada }

            val totalLojaFin = finalizadas.sumOf { it.valorGeradoLoja }
            val comissaoConf = finalizadas.sumOf { it.ganhoTecnico }

            summaries.add(
                TechnicianPerformanceSummary(
                    key = func.id ?: func.email,
                    funcionario = func,
                    nome = func.nome,
                    email = func.email,
                    cargo = func.cargo,
                    ativo = func.ativo,
                    comissaoPadraoPercent = func.comissaoPercentual,
                    osGeradasCount = geradasCount,
                    osFinalizadasCount = finalizadas.size,
                    osEmAndamentoCount = emAndamento.size,
                    totalGeradoLojaFinalizado = totalLojaFin,
                    totalGeradoLojaEmAndamento = emAndamento.sumOf { it.valorGeradoLoja },
                    maoObraFinalizada = finalizadas.sumOf { it.os.valorMaoObra },
                    pecasFinalizadas = finalizadas.sumOf { it.os.valorPecas },
                    comissaoConfirmada = comissaoConf,
                    comissaoPrevista = emAndamento.sumOf { it.ganhoTecnico },
                    liquidoLojaConfirmado = (totalLojaFin - comissaoConf).coerceAtLeast(0.0),
                    ordensDoTecnico = breakdowns.sortedByDescending { it.os.numeroOs }
                )
            )
        }

        // Agrupa OS atribuídas a nomes avulsos (ex: Admin ou técnico digitado manualmente fora da tabela funcionarios)
        val extraOrdens = ordens.filter { os ->
            val alreadyCounted = !os.id.isNullOrBlank() && os.id in assignedOsIds
            !alreadyCounted && os.tecnicoNome.isNotBlank()
        }
        extraOrdens.groupBy { it.tecnicoNome.trim() }.forEach { (techName, techOrders) ->
            val breakdowns = techOrders.map { buildBreakdown(it, funcionarios) }
            val finalizadas = breakdowns.filter { it.isFinalizada }
            val emAndamento = breakdowns.filterNot { it.isFinalizada }
            val totalLojaFin = finalizadas.sumOf { it.valorGeradoLoja }
            val comissaoConf = finalizadas.sumOf { it.ganhoTecnico }

            summaries.add(
                TechnicianPerformanceSummary(
                    key = "custom_$techName",
                    funcionario = null,
                    nome = techName,
                    email = techOrders.firstOrNull { it.tecnicoEmail.isNotBlank() }?.tecnicoEmail ?: "",
                    cargo = "Responsável Técnico",
                    ativo = true,
                    comissaoPadraoPercent = breakdowns.firstOrNull()?.percentApplied ?: 0.0,
                    osGeradasCount = techOrders.size,
                    osFinalizadasCount = finalizadas.size,
                    osEmAndamentoCount = emAndamento.size,
                    totalGeradoLojaFinalizado = totalLojaFin,
                    totalGeradoLojaEmAndamento = emAndamento.sumOf { it.valorGeradoLoja },
                    maoObraFinalizada = finalizadas.sumOf { it.os.valorMaoObra },
                    pecasFinalizadas = finalizadas.sumOf { it.os.valorPecas },
                    comissaoConfirmada = comissaoConf,
                    comissaoPrevista = emAndamento.sumOf { it.ganhoTecnico },
                    liquidoLojaConfirmado = (totalLojaFin - comissaoConf).coerceAtLeast(0.0),
                    ordensDoTecnico = breakdowns.sortedByDescending { it.os.numeroOs }
                )
            )
        }

        return summaries.sortedWith(
            compareByDescending<TechnicianPerformanceSummary> { it.totalGeradoLojaFinalizado }
                .thenByDescending { it.osFinalizadasCount }
                .thenBy { it.nome.lowercase() }
        )
    }
}

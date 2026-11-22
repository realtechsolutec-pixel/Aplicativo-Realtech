package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FuncionarioDto
import com.example.data.model.OrdemServicoDto
import com.example.data.model.UserSession
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiCard
import com.example.ui.components.StatusOsBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ScopeLojaColor
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.util.CommissionCalculator
import com.example.util.Formatters
import com.example.util.OsCommissionBreakdown
import com.example.util.TechnicianPerformanceSummary
import java.util.Locale

private enum class CommissionStatusFilter(val label: String) {
    TODAS("Todas"),
    FINALIZADAS("Finalizadas"),
    EM_ANDAMENTO("Em Andamento")
}

@Composable
fun ComissoesScreen(
    session: UserSession,
    ordens: List<OrdemServicoDto>,
    funcionarios: List<FuncionarioDto>,
    isSubmitting: Boolean,
    onUpdateFuncionarioPerfil: (FuncionarioDto, String, Double, () -> Unit) -> Unit
) {
    if (session.isAdmin) {
        AdminTeamPerformanceView(
            ordens = ordens,
            funcionarios = funcionarios,
            isSubmitting = isSubmitting,
            onUpdateFuncionarioPerfil = onUpdateFuncionarioPerfil
        )
    } else {
        EmployeeMyEarningsView(
            session = session,
            ordens = ordens,
            funcionarios = funcionarios
        )
    }
}

@Composable
private fun EmployeeMyEarningsView(
    session: UserSession,
    ordens: List<OrdemServicoDto>,
    funcionarios: List<FuncionarioDto>
) {
    var statusFilter by remember { mutableStateOf(CommissionStatusFilter.TODAS) }

    val myFuncionario = remember(session, funcionarios) {
        CommissionCalculator.findFuncionarioForSession(session, funcionarios)
    }
    val myOrders = remember(ordens, myFuncionario, session) {
        CommissionCalculator.filterOrdensForEmployee(
            ordens = ordens,
            funcionario = myFuncionario,
            userName = session.fullName,
            userEmail = session.email
        )
    }
    val allBreakdowns = remember(myOrders, funcionarios) {
        myOrders.map { CommissionCalculator.buildBreakdown(it, funcionarios) }
            .sortedByDescending { it.os.numeroOs }
    }

    val finalizadas = remember(allBreakdowns) { allBreakdowns.filter { it.isFinalizada } }
    val emAndamento = remember(allBreakdowns) { allBreakdowns.filterNot { it.isFinalizada } }

    val ganhoConfirmado = finalizadas.sumOf { it.ganhoTecnico }
    val ganhoPrevisto = emAndamento.sumOf { it.ganhoTecnico }
    val maoObraFinalizada = finalizadas.sumOf { it.os.valorMaoObra }

    val configuredPct = myFuncionario?.comissaoPercentual
        ?: allBreakdowns.firstOrNull()?.percentApplied
        ?: 0.0
    val roleLabel = myFuncionario?.cargo?.ifBlank { "Técnico Operacional" } ?: "Operacional"

    val filteredBreakdowns = remember(allBreakdowns, statusFilter) {
        when (statusFilter) {
            CommissionStatusFilter.TODAS -> allBreakdowns
            CommissionStatusFilter.FINALIZADAS -> allBreakdowns.filter { it.isFinalizada }
            CommissionStatusFilter.EM_ANDAMENTO -> allBreakdowns.filterNot { it.isFinalizada }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("employee_earnings_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Meus Ganhos por Serviço",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Valores calculados sobre a porcentagem de mão de obra definida para sua função",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Cartão de Perfil e Porcentagem do Funcionário
        item {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("employee_commission_header_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = roleLabel.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = myFuncionario?.nome ?: session.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "OS atribuídas: ${allBreakdowns.size} • Finalizadas: ${finalizadas.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Surface(
                        color = StatusSuccess,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${formatPercent(configuredPct)}%",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "Mão de obra",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // KPIs do Funcionário
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "GANHO CONFIRMADO",
                    value = Formatters.formatCurrency(ganhoConfirmado),
                    subtitle = "${finalizadas.size} OS finalizadas (${Formatters.formatCurrency(maoObraFinalizada)} MO)",
                    icon = Icons.Default.Payments,
                    accentColor = StatusSuccess,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_my_confirmed_earnings")
                )
                KpiCard(
                    title = "PREVISTO (ABERTAS)",
                    value = Formatters.formatCurrency(ganhoPrevisto),
                    subtitle = "${emAndamento.size} OS em andamento",
                    icon = Icons.Default.Schedule,
                    accentColor = StatusInfo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_my_projected_earnings")
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CommissionStatusFilter.entries.forEach { filter ->
                    val count = when (filter) {
                        CommissionStatusFilter.TODAS -> allBreakdowns.size
                        CommissionStatusFilter.FINALIZADAS -> finalizadas.size
                        CommissionStatusFilter.EM_ANDAMENTO -> emAndamento.size
                    }
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { statusFilter = filter },
                        label = { Text("${filter.label} ($count)") }
                    )
                }
            }
        }

        if (filteredBreakdowns.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Payments,
                    title = "Nenhum serviço registrado para você",
                    description = "Ao abrir ou ser atribuído como técnico/atendente em uma OS, seu ganho por serviço aparecerá aqui automaticamente."
                )
            }
        } else {
            items(filteredBreakdowns, key = { it.os.id ?: it.os.numeroOs }) { item ->
                EmployeeServiceCommissionCard(breakdown = item)
            }
        }
    }
}

@Composable
private fun EmployeeServiceCommissionCard(breakdown: OsCommissionBreakdown) {
    val os = breakdown.os
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "OS #${os.numeroOs}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    StatusOsBadge(status = os.statusEnum)
                }

                Surface(
                    color = if (breakdown.isFinalizada) StatusSuccessContainer else StatusInfoContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (breakdown.isFinalizada) {
                            "+ ${Formatters.formatCurrency(breakdown.ganhoTecnico)}"
                        } else {
                            "Prev. ${Formatters.formatCurrency(breakdown.ganhoTecnico)}"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (breakdown.isFinalizada) StatusSuccess else StatusInfo,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${os.equipamento} ${os.marcaModelo}".trim(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Cliente: ${os.clienteNome} • Entrada: ${Formatters.formatDateBr(os.dataEntrada)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mão de Obra do Serviço",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Formatters.formatCurrency(os.valorMaoObra),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Sua %",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatPercent(breakdown.percentApplied)}%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (breakdown.isFinalizada) "Ganho Confirmado" else "Ganho Previsto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Formatters.formatCurrency(breakdown.ganhoTecnico),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (breakdown.isFinalizada) StatusSuccess else StatusInfo
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminTeamPerformanceView(
    ordens: List<OrdemServicoDto>,
    funcionarios: List<FuncionarioDto>,
    isSubmitting: Boolean,
    onUpdateFuncionarioPerfil: (FuncionarioDto, String, Double, () -> Unit) -> Unit
) {
    var selectedTechKey by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf(CommissionStatusFilter.TODAS) }
    var editingFunc by remember { mutableStateOf<FuncionarioDto?>(null) }

    val teamSummaries = remember(ordens, funcionarios) {
        CommissionCalculator.buildTeamPerformance(ordens, funcionarios)
    }

    val allBreakdowns = remember(ordens, funcionarios) {
        ordens.map { CommissionCalculator.buildBreakdown(it, funcionarios) }
            .sortedByDescending { it.os.numeroOs }
    }

    val totalGeradoFinalizado = teamSummaries.sumOf { it.totalGeradoLojaFinalizado }
    val totalGeradoAndamento = teamSummaries.sumOf { it.totalGeradoLojaEmAndamento }
    val totalComissaoConfirmada = teamSummaries.sumOf { it.comissaoConfirmada }
    val totalComissaoPrevista = teamSummaries.sumOf { it.comissaoPrevista }
    val totalLiquidoLoja = (totalGeradoFinalizado - totalComissaoConfirmada).coerceAtLeast(0.0)
    val totalOsGeradas = ordens.size
    val totalOsFinalizadas = allBreakdowns.count { it.isFinalizada }

    val selectedSummary = teamSummaries.find { it.key == selectedTechKey }
    val baseBreakdowns = selectedSummary?.ordensDoTecnico ?: allBreakdowns
    val filteredBreakdowns = remember(baseBreakdowns, statusFilter) {
        when (statusFilter) {
            CommissionStatusFilter.TODAS -> baseBreakdowns
            CommissionStatusFilter.FINALIZADAS -> baseBreakdowns.filter { it.isFinalizada }
            CommissionStatusFilter.EM_ANDAMENTO -> baseBreakdowns.filterNot { it.isFinalizada }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_commissions_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Rendimento da Equipe & Comissões",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Acompanhe o valor gerado para a loja, OS geradas/finalizadas e ajuste a % de mão de obra por funcionário",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // KPIs da Loja
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "GERADO P/ LOJA",
                    value = Formatters.formatCurrency(totalGeradoFinalizado),
                    subtitle = "Em andamento: ${Formatters.formatCurrency(totalGeradoAndamento)}",
                    icon = Icons.Default.Store,
                    accentColor = ScopeLojaColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_admin_total_generated")
                )
                KpiCard(
                    title = "COMISSÕES EQUIPE",
                    value = Formatters.formatCurrency(totalComissaoConfirmada),
                    subtitle = "Previsto abertas: ${Formatters.formatCurrency(totalComissaoPrevista)}",
                    icon = Icons.Default.Payments,
                    accentColor = StatusWarning,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_admin_total_commissions")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "LÍQUIDO P/ LOJA",
                    value = Formatters.formatCurrency(totalLiquidoLoja),
                    subtitle = "Receita finalizada - comissões",
                    icon = Icons.Default.AttachMoney,
                    accentColor = StatusSuccess,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_admin_net_store")
                )
                KpiCard(
                    title = "OS GERADAS / FIN.",
                    value = "$totalOsGeradas / $totalOsFinalizadas",
                    subtitle = "${teamSummaries.size} ${if (teamSummaries.size == 1) "responsável" else "responsáveis"}",
                    icon = Icons.Default.Build,
                    accentColor = StatusInfo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_admin_os_counts")
                )
            }
        }

        // Seção Rendimento por Funcionário / Técnico
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Desempenho por Funcionário",
                    style = MaterialTheme.typography.titleMedium
                )
                if (selectedTechKey != null) {
                    TextButton(onClick = { selectedTechKey = null }) {
                        Text("Mostrar todos")
                    }
                }
            }
        }

        if (teamSummaries.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Groups,
                    title = "Nenhum técnico ou OS atribuída",
                    description = "Cadastre funcionários em Config. da Loja / Equipe e atribua o técnico responsável nas Ordens de Serviço."
                )
            }
        } else {
            items(teamSummaries, key = { it.key }) { summary ->
                TechnicianPerformanceCard(
                    summary = summary,
                    isSelected = selectedTechKey == summary.key,
                    onSelectFilter = {
                        selectedTechKey = if (selectedTechKey == summary.key) null else summary.key
                    },
                    onEditCommission = {
                        summary.funcionario?.let { editingFunc = it }
                    }
                )
            }
        }

        // Lista Detalhada de Serviços por Técnico
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (selectedSummary != null) {
                    "Serviços de ${selectedSummary.nome}"
                } else {
                    "Detalhamento por Ordem de Serviço"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (teamSummaries.isNotEmpty()) {
                    FilterChip(
                        selected = selectedTechKey == null,
                        onClick = { selectedTechKey = null },
                        label = { Text("Todos") }
                    )
                    teamSummaries.forEach { summary ->
                        FilterChip(
                            selected = selectedTechKey == summary.key,
                            onClick = {
                                selectedTechKey = if (selectedTechKey == summary.key) null else summary.key
                            },
                            label = { Text("${summary.nome} (${formatPercent(summary.comissaoPadraoPercent)}%)") }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CommissionStatusFilter.entries.forEach { filter ->
                    val count = when (filter) {
                        CommissionStatusFilter.TODAS -> baseBreakdowns.size
                        CommissionStatusFilter.FINALIZADAS -> baseBreakdowns.count { it.isFinalizada }
                        CommissionStatusFilter.EM_ANDAMENTO -> baseBreakdowns.count { !it.isFinalizada }
                    }
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { statusFilter = filter },
                        label = { Text("${filter.label} ($count)") }
                    )
                }
            }
        }

        if (filteredBreakdowns.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(vertical = 8.dp)) {
                    EmptyStateCard(
                        icon = Icons.Default.CheckCircle,
                        title = "Nenhuma OS neste filtro",
                        description = "Altere o filtro acima para visualizar outras ordens de serviço."
                    )
                }
            }
        } else {
            items(filteredBreakdowns, key = { "detail_${it.os.id ?: it.os.numeroOs}" }) { breakdown ->
                AdminOsCommissionCard(breakdown = breakdown)
            }
        }
    }

    editingFunc?.let { target ->
        EditFuncionarioComissaoDialog(
            funcionario = target,
            isSubmitting = isSubmitting,
            onDismiss = { editingFunc = null },
            onSave = { cargo, pct ->
                onUpdateFuncionarioPerfil(target, cargo, pct) {
                    editingFunc = null
                }
            }
        )
    }
}

@Composable
private fun TechnicianPerformanceCard(
    summary: TechnicianPerformanceSummary,
    isSelected: Boolean,
    onSelectFilter: () -> Unit,
    onEditCommission: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tech_summary_card_${summary.key}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = summary.nome,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = summary.cargo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (!summary.ativo) {
                            Surface(
                                color = StatusDangerContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "INATIVO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusDanger,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (summary.email.isNotBlank()) {
                        Text(
                            text = summary.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = StatusInfoContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${formatPercent(summary.comissaoPadraoPercent)}% MO",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = StatusInfo,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    label = "OS Geradas",
                    value = summary.osGeradasCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "OS Finalizadas",
                    value = summary.osFinalizadasCount.toString(),
                    modifier = Modifier.weight(1f),
                    valueColor = StatusSuccess
                )
                MetricTile(
                    label = "Em Aberto",
                    value = summary.osEmAndamentoCount.toString(),
                    modifier = Modifier.weight(1f),
                    valueColor = StatusInfo
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    label = "Gerado p/ Loja",
                    value = Formatters.formatCurrency(summary.totalGeradoLojaFinalizado),
                    subtitle = "MO ${Formatters.formatCurrency(summary.maoObraFinalizada)}",
                    modifier = Modifier.weight(1f),
                    valueColor = ScopeLojaColor
                )
                MetricTile(
                    label = "Ganho Funcionário",
                    value = Formatters.formatCurrency(summary.comissaoConfirmada),
                    subtitle = if (summary.comissaoPrevista > 0.0) {
                        "Prev: ${Formatters.formatCurrency(summary.comissaoPrevista)}"
                    } else "Confirmado",
                    modifier = Modifier.weight(1f),
                    valueColor = StatusWarning
                )
                MetricTile(
                    label = "Líquido Loja",
                    value = Formatters.formatCurrency(summary.liquidoLojaConfirmado),
                    subtitle = "Após comissão",
                    modifier = Modifier.weight(1f),
                    valueColor = StatusSuccess
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSelectFilter,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_tech_orders_${summary.key}")
                ) {
                    Text(
                        text = if (isSelected) "Ocultar Filtro" else "Ver OS (${summary.ordensDoTecnico.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (summary.funcionario != null) {
                    OutlinedButton(
                        onClick = onEditCommission,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_tech_percent_${summary.key}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Alterar % / Função",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AdminOsCommissionCard(breakdown: OsCommissionBreakdown) {
    val os = breakdown.os
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "OS #${os.numeroOs}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    StatusOsBadge(status = os.statusEnum)
                }

                Text(
                    text = Formatters.formatCurrency(breakdown.valorGeradoLoja),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ScopeLojaColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${os.equipamento} ${os.marcaModelo}".trim(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append("Responsável: ${breakdown.tecnicoNome}")
                    if (os.criadoPorNome.isNotBlank() && !os.criadoPorNome.equals(breakdown.tecnicoNome, ignoreCase = true)) {
                        append(" • Aberta por: ${os.criadoPorNome}")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Cliente: ${os.clienteNome} • Entrada: ${Formatters.formatDateBr(os.dataEntrada)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Mão de Obra", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatCurrency(os.valorMaoObra),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.SemiBold)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Comissão (${formatPercent(breakdown.percentApplied)}%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatCurrency(breakdown.ganhoTecnico),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold),
                        color = StatusWarning
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Líquido Loja", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatCurrency(breakdown.liquidoLoja),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold),
                        color = StatusSuccess
                    )
                }
            }
        }
    }
}

private val defaultEmployeeRoles = listOf(
    "Técnico",
    "Atendente",
    "Técnico Operacional",
    "Instalador"
)

private val quickCommissionPercents = listOf(30, 40, 50, 60, 70, 80)

@Composable
fun EditFuncionarioComissaoDialog(
    funcionario: FuncionarioDto,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (cargo: String, comissaoPercentual: Double) -> Unit
) {
    var cargo by remember(funcionario.id) { mutableStateOf(funcionario.cargo) }
    var percentText by remember(funcionario.id) {
        mutableStateOf(formatPercent(funcionario.comissaoPercentual))
    }
    val parsedPct = Formatters.parseDoubleInput(percentText).coerceIn(0.0, 100.0)
    val exampleGanho = CommissionCalculator.calculateGanho(100.0, parsedPct)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Função e % de ${funcionario.nome}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Defina a função e a porcentagem de ganho sobre o valor da mão de obra para este funcionário.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    defaultEmployeeRoles.forEach { presetRole ->
                        FilterChip(
                            selected = cargo.equals(presetRole, ignoreCase = true),
                            onClick = { cargo = presetRole },
                            label = { Text(presetRole) }
                        )
                    }
                }

                OutlinedTextField(
                    value = cargo,
                    onValueChange = { cargo = it },
                    label = { Text("Função / Cargo *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_func_cargo_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickCommissionPercents.forEach { pct ->
                        FilterChip(
                            selected = parsedPct.toInt() == pct,
                            onClick = { percentText = pct.toString() },
                            label = { Text("$pct%") }
                        )
                    }
                }

                OutlinedTextField(
                    value = percentText,
                    onValueChange = { percentText = it },
                    label = { Text("Comissão sobre Mão de Obra (%) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_func_percent_input")
                )

                Surface(
                    color = StatusSuccessContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Simulação: Serviço de R$ 100,00 de mão de obra → ${funcionario.nome} recebe ${Formatters.formatCurrency(exampleGanho)} (${formatPercent(parsedPct)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusSuccess,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(cargo.trim().ifBlank { "Técnico" }, parsedPct) },
                enabled = !isSubmitting && cargo.isNotBlank(),
                modifier = Modifier.testTag("confirm_edit_func_comissao_button")
            ) {
                Text(if (isSubmitting) "Salvando..." else "Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

internal fun formatPercent(value: Double): String {
    val rounded = value.coerceIn(0.0, 100.0)
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", rounded)
    }
}

package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.StatusOS
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FinanceScopeChartCard
import com.example.ui.components.KpiCard
import com.example.ui.components.OsStatusDistributionChart
import com.example.ui.components.PriorityOsBadge
import com.example.ui.components.StatusOsBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ScopeLojaColor
import com.example.ui.theme.ScopePessoalColor
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoContainer
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.viewmodel.WorkshopUiState
import com.example.util.CommissionCalculator
import com.example.util.Formatters

@Composable
fun DashboardScreen(
    uiState: WorkshopUiState,
    onNavigateToOs: () -> Unit,
    onNavigateToVendas: () -> Unit = {},
    onNavigateToEstoque: () -> Unit,
    onNavigateToFinanceiro: () -> Unit,
    onNavigateToClientes: () -> Unit,
    onNavigateToComissoes: () -> Unit = {},
    onNavigateToPedidosPecas: () -> Unit = {},
    onNavigateToOsOverdue30d: () -> Unit = {},
    onNavigateToFinanceReceber30d: () -> Unit = {},
    onNavigateToFinanceContasFixas: () -> Unit = {}
) {
    val openOrders = uiState.ordens.filter { it.statusEnum != StatusOS.ENTREGUE }
    val lowStock = uiState.pecasEstoqueBaixo
    val alerts = uiState.alerts
    val isAdmin = uiState.isAdmin

    val teamSummaries = remember(uiState.ordens, uiState.funcionarios, isAdmin) {
        if (isAdmin) CommissionCalculator.buildTeamPerformance(uiState.ordens, uiState.funcionarios) else emptyList()
    }
    val myFuncionario = remember(uiState.funcionarios, uiState.currentUserEmail, uiState.currentUserName, isAdmin) {
        if (!isAdmin) {
            uiState.funcionarios.find {
                (uiState.currentUserEmail.isNotBlank() && it.email.equals(uiState.currentUserEmail.trim(), ignoreCase = true)) ||
                    (uiState.currentUserName.isNotBlank() && it.nome.equals(uiState.currentUserName.trim(), ignoreCase = true))
            }
        } else null
    }
    val myBreakdowns = remember(uiState.ordens, uiState.funcionarios, myFuncionario, uiState.currentUserName, uiState.currentUserEmail, isAdmin) {
        if (!isAdmin) {
            CommissionCalculator.filterOrdensForEmployee(
                ordens = uiState.ordens,
                funcionario = myFuncionario,
                userName = uiState.currentUserName,
                userEmail = uiState.currentUserEmail
            ).map { CommissionCalculator.buildBreakdown(it, uiState.funcionarios) }
        } else emptyList()
    }
    val myConfirmedEarnings = myBreakdowns.filter { it.isFinalizada }.sumOf { it.ganhoTecnico }
    val myProjectedEarnings = myBreakdowns.filterNot { it.isFinalizada }.sumOf { it.ganhoTecnico }
    val myRatePercent = myFuncionario?.comissaoPercentual ?: myBreakdowns.firstOrNull()?.percentApplied ?: 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAdmin) "Visão Geral da Oficina" else "Painel Operacional",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = if (isAdmin) {
                            "Resumo operacional de OS, vendas, faturamento e estoque"
                        } else {
                            "Resumo operacional de Ordens de Serviço, vendas e estoque"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onNavigateToVendas,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("dashboard_nova_venda_button")
                ) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDV")
                }
            }
        }

        if (!isAdmin) {
            item {
                Surface(
                    color = StatusInfoContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_operational_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = StatusInfo,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Perfil Funcionário ativo: acesso liberado ao operacional (OS, Clientes e Estoque). Dados financeiros ocultos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF082F49)
                        )
                    }
                }
            }
        }

        // Alertas Críticos do Sistema
        if (alerts.ordensMais30Dias.isNotEmpty() ||
            (isAdmin && alerts.contasReceberMais30Dias.isNotEmpty()) ||
            (isAdmin && alerts.contasFixasEmAtraso.isNotEmpty()) ||
            alerts.pedidosPecasPendentes.isNotEmpty() ||
            lowStock.isNotEmpty()
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = StatusDanger,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Alertas do Sistema (${alerts.activeAlertsCount})",
                            style = MaterialTheme.typography.titleSmall,
                            color = StatusDanger
                        )
                    }

                    if (isAdmin && alerts.contasReceberMais30Dias.isNotEmpty()) {
                        DashboardAlertCard(
                            icon = Icons.Default.NotificationsActive,
                            title = "${alerts.contasReceberMais30Dias.size} conta(s) +30 dias sem receber",
                            subtitle = "Total em atraso: ${Formatters.formatCurrency(alerts.totalContasReceber30Dias)} • Toque p/ cobrar",
                            containerColor = StatusDangerContainer,
                            accentColor = StatusDanger,
                            testTag = "dashboard_alert_receber_30d",
                            onClick = onNavigateToFinanceReceber30d
                        )
                    }

                    if (isAdmin && alerts.contasFixasEmAtraso.isNotEmpty()) {
                        DashboardAlertCard(
                            icon = Icons.Default.ReceiptLong,
                            title = "${alerts.contasFixasEmAtraso.size} conta(s) fixa(s) em atraso",
                            subtitle = "Vencido: ${Formatters.formatCurrency(alerts.totalContasFixasEmAtraso)} • Abrir aba Contas Fixas",
                            containerColor = StatusDangerContainer,
                            accentColor = StatusDanger,
                            testTag = "dashboard_alert_contas_fixas",
                            onClick = onNavigateToFinanceContasFixas
                        )
                    }

                    if (alerts.ordensMais30Dias.isNotEmpty()) {
                        DashboardAlertCard(
                            icon = Icons.Default.Schedule,
                            title = "${alerts.ordensMais30Dias.size} OS com +30 dias sem conclusão",
                            subtitle = alerts.ordensMais30Dias.take(2).joinToString(", ") {
                                "#${it.os.numeroOs} (${it.diasEmAberto}d)"
                            },
                            containerColor = StatusDangerContainer,
                            accentColor = StatusDanger,
                            testTag = "dashboard_alert_os_30d",
                            onClick = onNavigateToOsOverdue30d
                        )
                    }

                    if (alerts.pedidosPecasPendentes.isNotEmpty()) {
                        DashboardAlertCard(
                            icon = Icons.Default.AddShoppingCart,
                            title = "${alerts.pedidosPecasPendentes.size} peça(s) aguardando pedido/chegada",
                            subtitle = alerts.pedidosPecasPendentes.take(2).joinToString(", ") { it.nomePeca },
                            containerColor = StatusWarningContainer,
                            accentColor = StatusWarning,
                            testTag = "dashboard_alert_pedidos_pecas",
                            onClick = onNavigateToPedidosPecas
                        )
                    }

                    if (lowStock.isNotEmpty()) {
                        DashboardAlertCard(
                            icon = Icons.Default.Warning,
                            title = "${lowStock.size} ${if (lowStock.size == 1) "peça com estoque baixo" else "peças com estoque baixo"}",
                            subtitle = lowStock.take(3).joinToString(", ") { "${it.nome} (${it.quantidade} un)" },
                            containerColor = StatusDangerContainer,
                            accentColor = StatusDanger,
                            testTag = "dashboard_low_stock_alert",
                            onClick = onNavigateToEstoque
                        )
                    }
                }
            }
        }

        // KPIs Linha 1: OS Abertas e Prontas (Todos)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "OS ABERTAS",
                    value = uiState.osAbertasCount.toString(),
                    subtitle = "${uiState.ordens.size} registradas no total",
                    icon = Icons.Default.Build,
                    accentColor = StatusInfo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_os_abertas"),
                    onClick = onNavigateToOs
                )
                KpiCard(
                    title = "PRONTAS / RETIRADA",
                    value = uiState.osProntasCount.toString(),
                    subtitle = "Aguardando cliente",
                    icon = Icons.Default.CheckCircle,
                    accentColor = StatusReady,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_os_prontas"),
                    onClick = onNavigateToOs
                )
            }
        }

        if (isAdmin) {
            // KPIs Linha 2: Faturamento Loja e Saldo Loja (Somente Admin)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "FATURAMENTO LOJA",
                        value = Formatters.formatCurrency(uiState.faturamentoLojaRecebido),
                        subtitle = "A receber: ${Formatters.formatCurrency(uiState.aReceberPendenteLoja)}",
                        icon = Icons.Default.AttachMoney,
                        accentColor = StatusSuccess,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_faturamento_loja"),
                        onClick = onNavigateToFinanceiro
                    )
                    KpiCard(
                        title = "SALDO OFICINA",
                        value = Formatters.formatCurrency(uiState.saldoLoja),
                        subtitle = "A pagar loja: ${Formatters.formatCurrency(uiState.aPagarPendenteLoja)}",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = ScopeLojaColor,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_saldo_loja"),
                        onClick = onNavigateToFinanceiro
                    )
                }
            }

            // KPIs Linha 3: Vendas Balcão (Hoje) e Clientes/Peças (Somente Admin)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "VENDAS BALCÃO (HOJE)",
                        value = Formatters.formatCurrency(uiState.totalVendasHoje),
                        subtitle = "Mês: ${Formatters.formatCurrency(uiState.totalVendasMes)} • ${uiState.vendas.size} vendas",
                        icon = Icons.Default.PointOfSale,
                        accentColor = StatusSuccess,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_vendas_hoje"),
                        onClick = onNavigateToVendas
                    )
                    KpiCard(
                        title = "CLIENTES & PEÇAS",
                        value = "${uiState.clientes.size} / ${uiState.pecas.size}",
                        subtitle = "${lowStock.size} itens em alerta",
                        icon = Icons.Default.Inventory2,
                        accentColor = StatusWarning,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToClientes
                    )
                }
            }

            // KPIs Linha 4: Gastos Pessoais (Somente Admin)
            item {
                KpiCard(
                    title = "GASTOS PESSOAIS",
                    value = Formatters.formatCurrency(uiState.gastosPessoaisPagos),
                    subtitle = "Pendente: ${Formatters.formatCurrency(uiState.gastosPessoaisPendentes)}",
                    icon = Icons.Default.Person,
                    accentColor = ScopePessoalColor,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToFinanceiro
                )
            }
        } else {
            // KPIs Operacionais para Funcionário (Clientes, Peças e Meus Ganhos, sem dados financeiros da loja)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "MEUS GANHOS (${formatPercent(myRatePercent)}%)",
                        value = Formatters.formatCurrency(myConfirmedEarnings),
                        subtitle = "Previsto abertas: ${Formatters.formatCurrency(myProjectedEarnings)}",
                        icon = Icons.Default.Payments,
                        accentColor = StatusSuccess,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_employee_earnings"),
                        onClick = onNavigateToComissoes
                    )
                    KpiCard(
                        title = "PEÇAS NO ESTOQUE",
                        value = uiState.pecas.size.toString(),
                        subtitle = "${lowStock.size} em nível baixo • ${uiState.clientes.size} clientes",
                        icon = Icons.Default.Inventory2,
                        accentColor = StatusWarning,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToEstoque
                    )
                }
            }
        }

        // Gráfico de OS por Status
        item {
            OsStatusDistributionChart(ordens = uiState.ordens)
        }

        // Gráfico comparativo Loja vs Pessoal + Rendimento da Equipe (Somente Admin)
        if (isAdmin) {
            item {
                FinanceScopeChartCard(
                    receitasLoja = uiState.faturamentoLojaRecebido,
                    despesasLoja = uiState.despesasLojaPagas,
                    gastosPessoais = uiState.gastosPessoaisPagos
                )
            }

            if (teamSummaries.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToComissoes() }
                            .testTag("dashboard_team_performance_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rendimento da Equipe",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TextButton(onClick = onNavigateToComissoes) {
                                    Text("Ver & Alterar %")
                                }
                            }
                            teamSummaries.take(4).forEachIndexed { index, summary ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${summary.nome} (${summary.cargo} • ${formatPercent(summary.comissaoPadraoPercent)}%)",
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Geradas: ${summary.osGeradasCount} • Finalizadas: ${summary.osFinalizadasCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Loja: ${Formatters.formatCurrency(summary.totalGeradoLojaFinalizado)}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = ScopeLojaColor
                                        )
                                        Text(
                                            text = "Ganho: ${Formatters.formatCurrency(summary.comissaoConfirmada)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                            color = StatusSuccess
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // OS Abertas Recentes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OS em Andamento",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onNavigateToOs) {
                    Text("Abrir Kanban / Lista")
                }
            }
        }

        if (openOrders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Build,
                    title = "Nenhuma OS aberta no momento",
                    description = "Acesse Ordens de Serviço no menu lateral para abrir uma nova OS de manutenção ou segurança."
                )
            }
        } else {
            items(openOrders.take(5), key = { it.id ?: it.numeroOs }) { os ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToOs() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        text = "#${os.numeroOs}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                StatusOsBadge(status = os.statusEnum)
                                PriorityOsBadge(priority = os.prioridadeEnum)
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
                                    append("Cliente: ${os.clienteNome}")
                                    if (os.tecnicoNome.isNotBlank()) {
                                        append(" • Resp.: ${os.tecnicoNome}")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isAdmin) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Formatters.formatCurrency(os.valorTotal),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardAlertCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = accentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onClick) {
                Text("Ver", color = accentColor)
            }
        }
    }
}

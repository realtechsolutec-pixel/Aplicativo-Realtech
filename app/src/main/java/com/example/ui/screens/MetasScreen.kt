package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MetaDiariaDto
import com.example.ui.components.KpiCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.viewmodel.WorkshopUiState
import com.example.util.Formatters
import kotlin.math.ceil

@Composable
fun MetasScreen(
    uiState: WorkshopUiState,
    onSaveMeta: (MetaDiariaDto) -> Unit
) {
    val saved = uiState.metaDiaria
    var metaMensalText by remember(saved.id, saved.metaMensal) {
        mutableStateOf(saved.metaMensal.toString())
    }
    var custosFixosText by remember(saved.id, saved.custosFixosMensais) {
        mutableStateOf(saved.custosFixosMensais.toString())
    }
    var diasUteisMesText by remember(saved.id, saved.diasUteisMes) {
        mutableStateOf(saved.diasUteisMes.toString())
    }
    var diasRestantesText by remember(saved.id, saved.diasUteisRestantes) {
        mutableStateOf(saved.diasUteisRestantes.toString())
    }
    var ticketMedioText by remember(saved.id, saved.ticketMedioEstimado) {
        mutableStateOf(saved.ticketMedioEstimado.toString())
    }

    LaunchedEffect(saved) {
        metaMensalText = saved.metaMensal.toString()
        custosFixosText = saved.custosFixosMensais.toString()
        diasUteisMesText = saved.diasUteisMes.toString()
        diasRestantesText = saved.diasUteisRestantes.toString()
        ticketMedioText = saved.ticketMedioEstimado.toString()
    }

    val metaMensal = Formatters.parseDoubleInput(metaMensalText).coerceAtLeast(0.0)
    val custosFixos = Formatters.parseDoubleInput(custosFixosText).coerceAtLeast(0.0)
    val diasMes = (diasUteisMesText.toIntOrNull() ?: 22).coerceAtLeast(1)
    val diasRestantes = (diasRestantesText.toIntOrNull() ?: 15).coerceIn(1, diasMes)
    val ticketMedio = Formatters.parseDoubleInput(ticketMedioText).coerceAtLeast(1.0)

    val realizadoLoja = uiState.faturamentoLojaRecebido
    val aReceberLoja = uiState.aReceberPendenteLoja
    val faltaParaMeta = (metaMensal - realizadoLoja).coerceAtLeast(0.0)
    val metaDiariaIdeal = metaMensal / diasMes
    val metaDiariaAjustada = faltaParaMeta / diasRestantes
    val pontoEquilibrioDiario = custosFixos / diasMes
    val osPorDiaNecessarias = ceil(metaDiariaAjustada / ticketMedio).toInt().coerceAtLeast(0)
    val osPorDiaIdeal = ceil(metaDiariaIdeal / ticketMedio).toInt().coerceAtLeast(0)
    val progresso = if (metaMensal > 0) (realizadoLoja / metaMensal).toFloat().coerceIn(0f, 1f) else 0f
    val porcentagem = (progresso * 100f).toInt()
    val custosCobertos = realizadoLoja >= custosFixos

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("metas_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Calculadora de Metas Diárias",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Planeje o faturamento diário, ponto de equilíbrio e volume de OS da oficina",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Cartão de Progresso Mensal vs Faturamento Real
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Progresso da Meta Mensal",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Realizado (Loja Pago): ${Formatters.formatCurrency(realizadoLoja)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = if (porcentagem >= 100) StatusSuccessContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "$porcentagem%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (porcentagem >= 100) StatusSuccess else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    val fillColor = if (porcentagem >= 100) StatusSuccess else MaterialTheme.colorScheme.primary
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        drawRoundRect(
                            color = trackColor,
                            cornerRadius = CornerRadius(16f, 16f),
                            size = size
                        )
                        if (progresso > 0f) {
                            drawRoundRect(
                                color = fillColor,
                                cornerRadius = CornerRadius(16f, 16f),
                                size = Size(width = size.width * progresso, height = size.height)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (faltaParaMeta <= 0.0) "Meta mensal atingida!" else "Faltam ${Formatters.formatCurrency(faltaParaMeta)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (faltaParaMeta <= 0.0) StatusSuccess else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "A receber: ${Formatters.formatCurrency(aReceberLoja)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = if (custosCobertos) StatusSuccessContainer else StatusWarningContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (custosCobertos) Icons.Default.CheckCircle else Icons.Default.AutoGraph,
                                contentDescription = null,
                                tint = if (custosCobertos) StatusSuccess else StatusWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (custosCobertos) {
                                    "Custos fixos (${Formatters.formatCurrency(custosFixos)}) já estão cobertos!"
                                } else {
                                    "Ponto de equilíbrio: faltam ${Formatters.formatCurrency((custosFixos - realizadoLoja).coerceAtLeast(0.0))} para pagar os custos fixos."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (custosCobertos) StatusSuccess else StatusWarning
                            )
                        }
                    }
                }
            }
        }

        // KPIs calculados em tempo real
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "META DIÁRIA IDEAL",
                    value = Formatters.formatCurrency(metaDiariaIdeal),
                    subtitle = "$osPorDiaIdeal OS/dia • $diasMes dias úteis",
                    icon = Icons.Default.TrendingUp,
                    accentColor = StatusInfo,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "META AJUSTADA HOJE",
                    value = Formatters.formatCurrency(metaDiariaAjustada),
                    subtitle = "$osPorDiaNecessarias OS/dia nos $diasRestantes dias rest.",
                    icon = Icons.Default.Calculate,
                    accentColor = if (metaDiariaAjustada > metaDiariaIdeal * 1.15) StatusWarning else StatusSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "EQUILÍBRIO DIÁRIO",
                    value = Formatters.formatCurrency(pontoEquilibrioDiario),
                    subtitle = "Mínimo/dia p/ custos fixos",
                    icon = Icons.Default.AutoGraph,
                    accentColor = StatusWarning,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "TICKET MÉDIO REAL",
                    value = Formatters.formatCurrency(uiState.ticketMedioRealOs),
                    subtitle = "${uiState.ordens.count { it.valorTotal > 0 }} OS com valor",
                    icon = Icons.Default.Build,
                    accentColor = StatusReady,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Parâmetros editáveis da calculadora
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Parâmetros do Cálculo",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = metaMensalText,
                            onValueChange = { metaMensalText = it },
                            label = { Text("Meta Mensal (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("meta_mensal_input")
                        )
                        OutlinedTextField(
                            value = custosFixosText,
                            onValueChange = { custosFixosText = it },
                            label = { Text("Custos Fixos (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("meta_custos_input")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = diasUteisMesText,
                            onValueChange = { diasUteisMesText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Dias Úteis/Mês") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("meta_dias_mes_input")
                        )
                        OutlinedTextField(
                            value = diasRestantesText,
                            onValueChange = { diasRestantesText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Dias Restantes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("meta_dias_restantes_input")
                        )
                    }

                    OutlinedTextField(
                        value = ticketMedioText,
                        onValueChange = { ticketMedioText = it },
                        label = { Text("Ticket Médio Estimado por OS (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("meta_ticket_input")
                    )

                    if (uiState.ticketMedioRealOs > 0.0) {
                        OutlinedButton(
                            onClick = {
                                ticketMedioText = String.format(
                                    java.util.Locale.US,
                                    "%.2f",
                                    uiState.ticketMedioRealOs
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("usar_ticket_real_button")
                        ) {
                            Text("Usar Ticket Médio Real das OS (${Formatters.formatCurrency(uiState.ticketMedioRealOs)})")
                        }
                    }

                    Button(
                        onClick = {
                            onSaveMeta(
                                saved.copy(
                                    metaMensal = metaMensal,
                                    custosFixosMensais = custosFixos,
                                    diasUteisMes = diasMes,
                                    diasUteisRestantes = diasRestantes,
                                    ticketMedioEstimado = ticketMedio
                                )
                            )
                        },
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_metas_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.isSubmitting) "Salvando metas..." else "Salvar Metas")
                    }
                }
            }
        }
    }
}

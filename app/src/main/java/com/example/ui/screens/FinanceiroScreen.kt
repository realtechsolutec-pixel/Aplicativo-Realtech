package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.EscopoFinanceiro
import com.example.data.model.StatusFinanceiro
import com.example.data.model.TipoTransacao
import com.example.data.model.TransacaoFinanceiraDto
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ScopeBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ScopeLojaColor
import com.example.ui.theme.ScopePessoalColor
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.util.AlertCalculator
import com.example.util.Formatters

private val categoriasLoja = listOf(
    "Ordem de Serviço",
    "Venda de Equipamento",
    "Compra de Peças",
    "Aluguel / Energia / Internet",
    "Ferramentas & Bancada"
)

private val categoriasPessoais = listOf(
    "Retirada / Pró-labore",
    "Alimentação",
    "Moradia / Contas",
    "Transporte / Combustível",
    "Outros Pessoais"
)

private val formasPagamento = listOf("PIX", "Cartão Crédito", "Cartão Débito", "Dinheiro", "Boleto")

@Composable
fun FinanceiroScreen(
    transacoes: List<TransacaoFinanceiraDto>,
    isSubmitting: Boolean,
    initialContasFixasTab: Boolean = false,
    initialFilterOverdue30d: Boolean = false,
    onConsumeNavRequests: () -> Unit = {},
    onSaveTransacao: (TransacaoFinanceiraDto, () -> Unit) -> Unit,
    onToggleStatus: (TransacaoFinanceiraDto) -> Unit,
    onDeleteTransacao: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(if (initialContasFixasTab) 1 else 0) }
    var escopoFilter by remember { mutableStateOf<EscopoFinanceiro?>(null) }
    var tipoFilter by remember { mutableStateOf<TipoTransacao?>(null) }
    var statusFilter by remember { mutableStateOf<StatusFinanceiro?>(null) }
    var onlyReceber30d by remember { mutableStateOf(initialFilterOverdue30d) }
    var editingTx by remember { mutableStateOf<TransacaoFinanceiraDto?>(null) }
    var defaultNewAsContaFixa by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var deletingTx by remember { mutableStateOf<TransacaoFinanceiraDto?>(null) }

    LaunchedEffect(initialContasFixasTab, initialFilterOverdue30d) {
        if (initialContasFixasTab) {
            selectedTab = 1
            onlyReceber30d = false
            onConsumeNavRequests()
        } else if (initialFilterOverdue30d) {
            selectedTab = 0
            onlyReceber30d = true
            onConsumeNavRequests()
        }
    }

    val saldoLoja = remember(transacoes) {
        val rec = transacoes.filter { it.escopoEnum == EscopoFinanceiro.LOJA && it.tipoEnum == TipoTransacao.RECEBER && it.statusEnum == StatusFinanceiro.PAGO }.sumOf { it.valor }
        val pag = transacoes.filter { it.escopoEnum == EscopoFinanceiro.LOJA && it.tipoEnum == TipoTransacao.PAGAR && it.statusEnum == StatusFinanceiro.PAGO }.sumOf { it.valor }
        rec - pag
    }

    val gastosPessoais = remember(transacoes) {
        transacoes.filter { it.escopoEnum == EscopoFinanceiro.PESSOAL && it.tipoEnum == TipoTransacao.PAGAR }.sumOf { it.valor }
    }

    val receber30dList = remember(transacoes) {
        transacoes.filter {
            it.tipoEnum == TipoTransacao.RECEBER &&
                it.statusEnum == StatusFinanceiro.PENDENTE &&
                AlertCalculator.diasEmAtraso(it) > 30L
        }
    }

    val contasFixasList = remember(transacoes) {
        transacoes.filter { it.isContaFixaEfetiva }
            .sortedWith(
                compareByDescending<TransacaoFinanceiraDto> {
                    it.statusEnum == StatusFinanceiro.PENDENTE && AlertCalculator.diasEmAtraso(it) >= 1L
                }.thenBy { it.statusEnum == StatusFinanceiro.PAGO }
                    .thenBy { it.dataVencimento }
            )
    }

    val contasFixasEmAtraso = remember(contasFixasList) {
        contasFixasList.filter {
            it.tipoEnum == TipoTransacao.PAGAR &&
                it.statusEnum == StatusFinanceiro.PENDENTE &&
                AlertCalculator.diasEmAtraso(it) >= 1L
        }
    }

    val filtered = remember(transacoes, escopoFilter, tipoFilter, statusFilter, onlyReceber30d) {
        transacoes.filter { tx ->
            val match30d = !onlyReceber30d || (
                tx.tipoEnum == TipoTransacao.RECEBER &&
                    tx.statusEnum == StatusFinanceiro.PENDENTE &&
                    AlertCalculator.diasEmAtraso(tx) > 30L
                )
            match30d &&
                (escopoFilter == null || tx.escopoEnum == escopoFilter) &&
                (tipoFilter == null || tx.tipoEnum == tipoFilter) &&
                (statusFilter == null || tx.statusEnum == statusFilter)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTx = null
                    defaultNewAsContaFixa = (selectedTab == 1)
                    showForm = true
                },
                modifier = Modifier.testTag("add_financeiro_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Lançamento")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Movimentações")
                            if (receber30dList.isNotEmpty()) {
                                Badge(containerColor = StatusDanger, contentColor = Color.White) {
                                    Text(receber30dList.size.toString())
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_financeiro_movimentacoes")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Contas Fixas")
                            if (contasFixasEmAtraso.isNotEmpty()) {
                                Badge(containerColor = StatusDanger, contentColor = Color.White) {
                                    Text(contasFixasEmAtraso.size.toString())
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_financeiro_contas_fixas")
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    // Alerta de contas com mais de 30 dias sem receber
                    if (receber30dList.isNotEmpty()) {
                        Card(
                            onClick = { onlyReceber30d = !onlyReceber30d },
                            colors = CardDefaults.cardColors(containerColor = StatusDangerContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fin_alert_receber_30d")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Alerta de contas +30 dias sem receber",
                                    tint = StatusDanger
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Alerta: ${receber30dList.size} conta(s) com +30 dias sem receber",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = StatusDanger
                                    )
                                    Text(
                                        text = "Total em aberto: ${Formatters.formatCurrency(receber30dList.sumOf { it.valor })} • " +
                                            if (onlyReceber30d) "Filtrando apenas +30d (toque p/ ver tudo)" else "Toque para filtrar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusDanger.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Resumo separado Loja vs Pessoal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = ScopeLojaColor.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "SALDO LOJA (PAGO)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ScopeLojaColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Formatters.formatCurrency(saldoLoja),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = ScopePessoalColor.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "GASTOS PESSOAIS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ScopePessoalColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Formatters.formatCurrency(gastosPessoais),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filtro de Escopo (Todos | Loja | Pessoal)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = escopoFilter == null,
                            onClick = { escopoFilter = null },
                            shape = SegmentedButtonDefaults.itemShape(0, 3),
                            modifier = Modifier.testTag("scope_all_button")
                        ) { Text("Todos") }
                        SegmentedButton(
                            selected = escopoFilter == EscopoFinanceiro.LOJA,
                            onClick = { escopoFilter = EscopoFinanceiro.LOJA },
                            shape = SegmentedButtonDefaults.itemShape(1, 3),
                            modifier = Modifier.testTag("scope_loja_button")
                        ) { Text("Loja") }
                        SegmentedButton(
                            selected = escopoFilter == EscopoFinanceiro.PESSOAL,
                            onClick = { escopoFilter = EscopoFinanceiro.PESSOAL },
                            shape = SegmentedButtonDefaults.itemShape(2, 3),
                            modifier = Modifier.testTag("scope_pessoal_button")
                        ) { Text("Pessoal") }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (receber30dList.isNotEmpty()) {
                            FilterChip(
                                selected = onlyReceber30d,
                                onClick = { onlyReceber30d = !onlyReceber30d },
                                label = { Text("+30d sem receber (${receber30dList.size})") }
                            )
                        }
                        TipoTransacao.entries.forEach { t ->
                            FilterChip(
                                selected = tipoFilter == t,
                                onClick = { tipoFilter = if (tipoFilter == t) null else t },
                                label = { Text(t.label) }
                            )
                        }
                        StatusFinanceiro.entries.forEach { st ->
                            FilterChip(
                                selected = statusFilter == st,
                                onClick = { statusFilter = if (statusFilter == st) null else st },
                                label = { Text(st.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filtered.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Sem lançamentos financeiros",
                            description = "Controle contas a pagar e receber separando os gastos da oficina (Loja) dos gastos Pessoais."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id ?: it.descricao + it.dataVencimento }) { tx ->
                                TransacaoCard(
                                    tx = tx,
                                    onToggleStatus = { onToggleStatus(tx) },
                                    onEdit = {
                                        editingTx = tx
                                        defaultNewAsContaFixa = tx.isContaFixaEfetiva
                                        showForm = true
                                    },
                                    onDelete = { deletingTx = tx }
                                )
                            }
                        }
                    }
                } else {
                    // ABA DE CONTAS FIXAS (Com alerta explícito de contas em atraso)
                    if (contasFixasEmAtraso.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusDangerContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fin_alert_contas_fixas_atraso")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alerta de contas fixas em atraso",
                                    tint = StatusDanger
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Atenção: ${contasFixasEmAtraso.size} conta(s) fixa(s) em atraso!",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = StatusDanger
                                    )
                                    Text(
                                        text = "Total vencido: ${Formatters.formatCurrency(contasFixasEmAtraso.sumOf { it.valor })} — " +
                                            contasFixasEmAtraso.take(2).joinToString(", ") {
                                                "${it.descricao} (${AlertCalculator.diasEmAtraso(it)}d)"
                                            },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusDanger.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    val totalFixasPendentes = contasFixasList
                        .filter { it.statusEnum == StatusFinanceiro.PENDENTE }
                        .sumOf { it.valor }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "CONTAS FIXAS PENDENTES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusWarning,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Formatters.formatCurrency(totalFixasPendentes),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (contasFixasEmAtraso.isNotEmpty()) StatusDangerContainer else StatusSuccessContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "EM ATRASO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (contasFixasEmAtraso.isNotEmpty()) StatusDanger else StatusSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${contasFixasEmAtraso.size} conta(s)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (contasFixasEmAtraso.isNotEmpty()) StatusDanger else StatusSuccess
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (contasFixasList.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Default.ReceiptLong,
                            title = "Nenhuma conta fixa cadastrada",
                            description = "Toque em + para lançar aluguel, energia, internet, água ou sistema e receber alertas de atraso."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(contasFixasList, key = { "fixa_${it.id ?: it.descricao + it.dataVencimento}" }) { tx ->
                                TransacaoCard(
                                    tx = tx,
                                    onToggleStatus = { onToggleStatus(tx) },
                                    onEdit = {
                                        editingTx = tx
                                        defaultNewAsContaFixa = true
                                        showForm = true
                                    },
                                    onDelete = { deletingTx = tx }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        TransacaoFormSheet(
            initial = editingTx,
            defaultIsContaFixa = defaultNewAsContaFixa,
            isSubmitting = isSubmitting,
            onDismiss = { showForm = false },
            onSave = { dto ->
                onSaveTransacao(dto) { showForm = false }
            }
        )
    }

    deletingTx?.let { target ->
        ConfirmDeleteDialog(
            title = "Excluir lançamento",
            message = "Deseja excluir '${target.descricao}'?",
            onConfirm = {
                target.id?.let(onDeleteTransacao)
                deletingTx = null
            },
            onDismiss = { deletingTx = null }
        )
    }
}

@Composable
private fun TransacaoCard(
    tx: TransacaoFinanceiraDto,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isReceber = tx.tipoEnum == TipoTransacao.RECEBER
    val isPago = tx.statusEnum == StatusFinanceiro.PAGO
    val diasAtraso = AlertCalculator.diasEmAtraso(tx)
    val isReceber30d = isReceber && !isPago && diasAtraso > 30L
    val isFixaAtrasada = !isReceber && !isPago && tx.isContaFixaEfetiva && diasAtraso >= 1L

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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ScopeBadge(escopo = tx.escopoEnum)
                    Surface(
                        color = if (isPago) StatusSuccessContainer else StatusWarningContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPago) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isPago) StatusSuccess else StatusWarning,
                                modifier = Modifier.height(14.dp)
                            )
                            Text(
                                text = tx.statusEnum.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPago) StatusSuccess else StatusWarning
                            )
                        }
                    }
                    if (isReceber30d) {
                        Surface(
                            color = StatusDangerContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "+${diasAtraso}d SEM RECEBER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusDanger,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    } else if (isFixaAtrasada) {
                        Surface(
                            color = StatusDangerContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ATRASADA ${diasAtraso}d",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusDanger,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isReceber) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isReceber) StatusSuccess else StatusDanger,
                        modifier = Modifier.height(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (if (isReceber) "+ " else "- ") + Formatters.formatCurrency(tx.valor),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isReceber) StatusSuccess else StatusDanger
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tx.descricao,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append("${tx.categoria} • ${tx.formaPagamento} • Venc: ${Formatters.formatDateBr(tx.dataVencimento)}")
                    if (tx.isContaFixaEfetiva) append(" • Conta Fixa")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleStatus) {
                    Text(if (isPago) "Marcar como Pendente" else "Dar Baixa (Pago)")
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar lançamento")
                    }
                    if (!tx.id.isNullOrBlank()) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Excluir lançamento",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransacaoFormSheet(
    initial: TransacaoFinanceiraDto?,
    defaultIsContaFixa: Boolean = false,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (TransacaoFinanceiraDto) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var escopo by remember { mutableStateOf(initial?.escopoEnum ?: EscopoFinanceiro.LOJA) }
    var tipo by remember {
        mutableStateOf(
            initial?.tipoEnum ?: if (defaultIsContaFixa) TipoTransacao.PAGAR else TipoTransacao.RECEBER
        )
    }
    var status by remember { mutableStateOf(initial?.statusEnum ?: StatusFinanceiro.PENDENTE) }
    var isContaFixa by remember { mutableStateOf(initial?.isContaFixa ?: defaultIsContaFixa) }
    var descricao by remember { mutableStateOf(initial?.descricao ?: "") }
    var categoria by remember {
        mutableStateOf(
            initial?.categoria ?: if (defaultIsContaFixa) "Aluguel / Energia / Internet" else categoriasLoja.first()
        )
    }
    var valorText by remember { mutableStateOf(if ((initial?.valor ?: 0.0) > 0) initial!!.valor.toString() else "") }
    var formaPagamento by remember { mutableStateOf(initial?.formaPagamento ?: "PIX") }
    var dataVencimento by remember { mutableStateOf(initial?.dataVencimento ?: Formatters.todayIsoDate()) }
    var observacoes by remember { mutableStateOf(initial?.observacoes ?: "") }

    val suggestedCategories = if (escopo == EscopoFinanceiro.LOJA) categoriasLoja else categoriasPessoais

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (initial == null) "Novo Lançamento Financeiro" else "Editar Lançamento",
                style = MaterialTheme.typography.headlineSmall
            )

            Text("Escopo (Separação Loja x Pessoal)", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                EscopoFinanceiro.entries.forEachIndexed { idx, item ->
                    SegmentedButton(
                        selected = escopo == item,
                        onClick = {
                            escopo = item
                            if (item == EscopoFinanceiro.PESSOAL && tipo == TipoTransacao.RECEBER) {
                                categoria = categoriasPessoais.first()
                            } else if (item == EscopoFinanceiro.LOJA) {
                                categoria = categoriasLoja.first()
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(idx, EscopoFinanceiro.entries.size),
                        modifier = Modifier.testTag("form_escopo_${item.code.lowercase()}")
                    ) { Text(item.label) }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TipoTransacao.entries.forEachIndexed { idx, item ->
                    SegmentedButton(
                        selected = tipo == item,
                        onClick = { tipo = item },
                        shape = SegmentedButtonDefaults.itemShape(idx, TipoTransacao.entries.size)
                    ) { Text(item.label) }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                StatusFinanceiro.entries.forEachIndexed { idx, item ->
                    SegmentedButton(
                        selected = status == item,
                        onClick = { status = item },
                        shape = SegmentedButtonDefaults.itemShape(idx, StatusFinanceiro.entries.size)
                    ) { Text(item.label) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Conta Fixa Mensal", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "Ex: Aluguel, energia, internet, sistema (gera alerta quando atrasar)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isContaFixa,
                    onCheckedChange = {
                        isContaFixa = it
                        if (it) tipo = TipoTransacao.PAGAR
                    },
                    modifier = Modifier.testTag("fin_conta_fixa_switch")
                )
            }

            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it },
                label = { Text("Descrição *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fin_descricao_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestedCategories.forEach { cat ->
                    FilterChip(
                        selected = categoria == cat,
                        onClick = {
                            categoria = cat
                            if (cat.contains("Aluguel", ignoreCase = true) || cat.contains("Moradia", ignoreCase = true)) {
                                isContaFixa = true
                            }
                        },
                        label = { Text(cat) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = valorText,
                    onValueChange = { valorText = it },
                    label = { Text("Valor (R$) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("fin_valor_input")
                )
                OutlinedTextField(
                    value = dataVencimento,
                    onValueChange = { dataVencimento = it },
                    label = { Text("Vencimento (AAAA-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                formasPagamento.forEach { fp ->
                    FilterChip(
                        selected = formaPagamento == fp,
                        onClick = { formaPagamento = fp },
                        label = { Text(fp) }
                    )
                }
            }

            OutlinedTextField(
                value = observacoes,
                onValueChange = { observacoes = it },
                label = { Text("Observações") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val parsedValor = Formatters.parseDoubleInput(valorText)
            Button(
                onClick = {
                    if (descricao.isNotBlank() && parsedValor > 0.0) {
                        onSave(
                            (initial ?: TransacaoFinanceiraDto(
                                descricao = descricao.trim(),
                                dataVencimento = dataVencimento.trim().ifBlank { Formatters.todayIsoDate() }
                            )).copy(
                                descricao = descricao.trim(),
                                categoria = categoria,
                                tipo = tipo.code,
                                escopo = escopo.code,
                                status = status.code,
                                isContaFixa = isContaFixa,
                                valor = parsedValor,
                                formaPagamento = formaPagamento,
                                dataVencimento = dataVencimento.trim().ifBlank { Formatters.todayIsoDate() },
                                observacoes = observacoes.trim()
                            )
                        )
                    }
                },
                enabled = !isSubmitting && descricao.isNotBlank() && parsedValor > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_financeiro_button")
            ) {
                Text(if (isSubmitting) "Salvando..." else "Salvar Lançamento")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.OrdemServicoDto
import com.example.data.model.PecaEstoqueDto
import com.example.data.model.PedidoPecaDto
import com.example.data.model.PedidoPecaMotivo
import com.example.data.model.PedidoPecaStatus
import com.example.data.model.PrioridadeOS
import com.example.data.model.StatusOS
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiCard
import com.example.ui.components.PriorityOsBadge
import com.example.ui.components.SearchBarField
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.util.Formatters

@Composable
fun PedidosPecasScreen(
    pedidos: List<PedidoPecaDto>,
    pecasEstoque: List<PecaEstoqueDto>,
    ordens: List<OrdemServicoDto>,
    currentUserName: String,
    isSubmitting: Boolean,
    onSavePedido: (PedidoPecaDto, () -> Unit) -> Unit,
    onUpdateStatus: (PedidoPecaDto, PedidoPecaStatus) -> Unit,
    onDeletePedido: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<PedidoPecaStatus?>(null) }
    var motivoFilter by remember { mutableStateOf<PedidoPecaMotivo?>(null) }
    var editingPedido by remember { mutableStateOf<PedidoPecaDto?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var deletingPedido by remember { mutableStateOf<PedidoPecaDto?>(null) }

    val pendentesCount = remember(pedidos) { pedidos.count { it.statusEnum == PedidoPecaStatus.PENDENTE } }
    val paraOsCount = remember(pedidos) {
        pedidos.count { it.statusEnum != PedidoPecaStatus.RECEBIDA && it.motivoEnum == PedidoPecaMotivo.PARA_OS }
    }
    val faltantesCount = remember(pedidos) {
        pedidos.count { it.statusEnum != PedidoPecaStatus.RECEBIDA && it.motivoEnum == PedidoPecaMotivo.FALTANTE }
    }
    val lowStockPecas = remember(pecasEstoque) { pecasEstoque.filter { it.isEstoqueBaixo } }

    val filtered = remember(pedidos, search, statusFilter, motivoFilter) {
        pedidos.filter { p ->
            val matchStatus = statusFilter == null || p.statusEnum == statusFilter
            val matchMotivo = motivoFilter == null || p.motivoEnum == motivoFilter
            val q = search.trim().lowercase()
            val matchQuery = q.isBlank() ||
                p.nomePeca.lowercase().contains(q) ||
                p.codigoSku.lowercase().contains(q) ||
                (p.numeroOs?.toString()?.contains(q) == true) ||
                p.solicitadoPor.lowercase().contains(q) ||
                p.fornecedorSugerido.lowercase().contains(q)
            matchStatus && matchMotivo && matchQuery
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPedido = null
                    showForm = true
                },
                modifier = Modifier.testTag("add_pedido_peca_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Pedido de Peça")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("pedidos_pecas_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Pedido de Peças & Faltantes",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Lista de peças faltantes no estoque e componentes necessários para concluir Ordens de Serviço",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "AGUARDANDO PEDIDO",
                        value = pendentesCount.toString(),
                        subtitle = "$faltantesCount faltantes em estoque",
                        icon = Icons.Default.AddShoppingCart,
                        accentColor = StatusDanger,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "P/ CONCLUIR OS",
                        value = paraOsCount.toString(),
                        subtitle = "${pedidos.count { it.statusEnum == PedidoPecaStatus.PEDIDO_FEITO }} já compradas",
                        icon = Icons.Default.Build,
                        accentColor = StatusWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (lowStockPecas.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StatusDangerContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${lowStockPecas.size} peça(s) do estoque abaixo do mínimo",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StatusDanger
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                lowStockPecas.take(6).forEach { peca ->
                                    OutlinedButton(
                                        onClick = {
                                            editingPedido = PedidoPecaDto(
                                                pecaId = peca.id,
                                                nomePeca = peca.nome,
                                                codigoSku = peca.codigoSku,
                                                quantidade = (peca.estoqueMinimo - peca.quantidade).coerceAtLeast(1),
                                                motivo = PedidoPecaMotivo.FALTANTE.code,
                                                fornecedorSugerido = peca.fornecedor,
                                                solicitadoPor = currentUserName,
                                                prioridade = if (peca.quantidade == 0) PrioridadeOS.URGENTE.code else PrioridadeOS.ALTA.code
                                            )
                                            showForm = true
                                        }
                                    ) {
                                        Text("+ Pedir ${peca.nome} (${peca.quantidade} un)")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SearchBarField(
                    query = search,
                    onQueryChange = { search = it },
                    placeholder = "Buscar peça, SKU, OS # ou solicitante...",
                    testTag = "pedidos_pecas_search_input"
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == null && motivoFilter == null,
                        onClick = {
                            statusFilter = null
                            motivoFilter = null
                        },
                        label = { Text("Todas (${pedidos.size})") }
                    )
                    PedidoPecaStatus.entries.forEach { st ->
                        val count = pedidos.count { it.statusEnum == st }
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = if (statusFilter == st) null else st },
                            label = { Text("${st.label} ($count)") }
                        )
                    }
                    PedidoPecaMotivo.entries.forEach { mot ->
                        FilterChip(
                            selected = motivoFilter == mot,
                            onClick = { motivoFilter = if (motivoFilter == mot) null else mot },
                            label = { Text(mot.label) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.AddShoppingCart,
                        title = "Nenhuma peça na lista de pedidos",
                        description = "Toque em + para adicionar peças faltantes ou peças necessárias para concluir um serviço (OS)."
                    )
                }
            } else {
                items(filtered, key = { it.id ?: (it.nomePeca + it.numeroOs + it.createdAt) }) { item ->
                    PedidoPecaCard(
                        pedido = item,
                        isSubmitting = isSubmitting,
                        onUpdateStatus = { next -> onUpdateStatus(item, next) },
                        onEdit = {
                            editingPedido = item
                            showForm = true
                        },
                        onDelete = { deletingPedido = item }
                    )
                }
            }
        }
    }

    if (showForm) {
        PedidoPecaFormSheet(
            initial = editingPedido,
            pecasEstoque = pecasEstoque,
            ordens = ordens,
            currentUserName = currentUserName,
            isSubmitting = isSubmitting,
            onDismiss = { showForm = false },
            onSave = { dto ->
                onSavePedido(dto) { showForm = false }
            }
        )
    }

    deletingPedido?.let { target ->
        ConfirmDeleteDialog(
            title = "Remover pedido de peça",
            message = "Remover '${target.nomePeca}' da lista de pedidos?",
            onConfirm = {
                target.id?.let(onDeletePedido)
                deletingPedido = null
            },
            onDismiss = { deletingPedido = null }
        )
    }
}

@Composable
private fun PedidoPecaCard(
    pedido: PedidoPecaDto,
    isSubmitting: Boolean,
    onUpdateStatus: (PedidoPecaStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val st = pedido.statusEnum
    val mot = pedido.motivoEnum

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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = st.containerColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = st.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = st.accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = if (mot == PedidoPecaMotivo.PARA_OS) StatusInfoContainer else StatusDangerContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (mot == PedidoPecaMotivo.PARA_OS && pedido.numeroOs != null) {
                                "OS #${pedido.numeroOs}"
                            } else {
                                mot.label
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (mot == PedidoPecaMotivo.PARA_OS) StatusInfo else StatusDanger,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    PriorityOsBadge(priority = pedido.prioridadeEnum)
                }

                Text(
                    text = "${pedido.quantidade} un",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = pedido.nomePeca,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    if (pedido.codigoSku.isNotBlank()) append("SKU: ${pedido.codigoSku} • ")
                    if (pedido.solicitadoPor.isNotBlank()) append("Solicitado por: ${pedido.solicitadoPor}")
                    if (pedido.fornecedorSugerido.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append("Forn.: ${pedido.fornecedorSugerido}")
                    }
                    if (!pedido.createdAt.isNullOrBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(Formatters.formatDateBr(pedido.createdAt))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (pedido.observacoes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Obs.: ${pedido.observacoes}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (st == PedidoPecaStatus.PENDENTE) {
                        TextButton(
                            onClick = { onUpdateStatus(PedidoPecaStatus.PEDIDO_FEITO) },
                            enabled = !isSubmitting
                        ) {
                            Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pedido Feito")
                        }
                    }
                    if (st != PedidoPecaStatus.RECEBIDA) {
                        TextButton(
                            onClick = { onUpdateStatus(PedidoPecaStatus.RECEBIDA) },
                            enabled = !isSubmitting
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recebida", color = StatusSuccess)
                        }
                    } else {
                        TextButton(
                            onClick = { onUpdateStatus(PedidoPecaStatus.PENDENTE) },
                            enabled = !isSubmitting
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reabrir")
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar pedido de peça")
                    }
                    if (!pedido.id.isNullOrBlank()) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Excluir pedido de peça",
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
fun PedidoPecaFormSheet(
    initial: PedidoPecaDto?,
    pecasEstoque: List<PecaEstoqueDto>,
    ordens: List<OrdemServicoDto>,
    currentUserName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (PedidoPecaDto) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPecaId by remember { mutableStateOf(initial?.pecaId) }
    var nomePeca by remember { mutableStateOf(initial?.nomePeca ?: "") }
    var codigoSku by remember { mutableStateOf(initial?.codigoSku ?: "") }
    var quantidadeText by remember { mutableStateOf((initial?.quantidade ?: 1).toString()) }
    var motivo by remember { mutableStateOf(initial?.motivoEnum ?: PedidoPecaMotivo.PARA_OS) }
    var status by remember { mutableStateOf(initial?.statusEnum ?: PedidoPecaStatus.PENDENTE) }
    var prioridade by remember { mutableStateOf(initial?.prioridadeEnum ?: PrioridadeOS.NORMAL) }
    var selectedOsId by remember { mutableStateOf(initial?.osId) }
    var selectedNumeroOs by remember { mutableStateOf(initial?.numeroOs) }
    var fornecedorSugerido by remember { mutableStateOf(initial?.fornecedorSugerido ?: "") }
    var observacoes by remember { mutableStateOf(initial?.observacoes ?: "") }

    var pecaDropdownExpanded by remember { mutableStateOf(false) }
    var osDropdownExpanded by remember { mutableStateOf(false) }

    val openOrdens = remember(ordens) {
        ordens.filter { it.statusEnum != StatusOS.ENTREGUE }
    }

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
                text = if (initial?.id != null) "Editar Pedido de Peça" else "Novo Pedido de Peça",
                style = MaterialTheme.typography.headlineSmall
            )

            Text("Motivo da Solicitação", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PedidoPecaMotivo.entries.forEachIndexed { idx, item ->
                    SegmentedButton(
                        selected = motivo == item,
                        onClick = { motivo = item },
                        shape = SegmentedButtonDefaults.itemShape(idx, PedidoPecaMotivo.entries.size)
                    ) { Text(item.label) }
                }
            }

            if (pecasEstoque.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = pecaDropdownExpanded,
                    onExpandedChange = { pecaDropdownExpanded = !pecaDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = nomePeca,
                        onValueChange = {
                            nomePeca = it
                            selectedPecaId = null
                        },
                        label = { Text("Peça / Componente (selecione ou digite) *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pecaDropdownExpanded) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .testTag("pedido_peca_nome_input")
                    )
                    ExposedDropdownMenu(
                        expanded = pecaDropdownExpanded,
                        onDismissRequest = { pecaDropdownExpanded = false }
                    ) {
                        pecasEstoque.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.nome} (${p.quantidade} un)") },
                                onClick = {
                                    selectedPecaId = p.id
                                    nomePeca = p.nome
                                    codigoSku = p.codigoSku
                                    fornecedorSugerido = p.fornecedor
                                    pecaDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = nomePeca,
                    onValueChange = { nomePeca = it },
                    label = { Text("Nome da Peça / Componente *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pedido_peca_nome_input")
                )
            }

            if (openOrdens.isNotEmpty()) {
                val osLabel = if (selectedNumeroOs != null) {
                    val osObj = openOrdens.find { it.numeroOs == selectedNumeroOs }
                    if (osObj != null) "OS #${osObj.numeroOs} - ${osObj.equipamento} (${osObj.clienteNome})"
                    else "OS #$selectedNumeroOs"
                } else {
                    "Nenhuma OS vinculada (opcional)"
                }
                ExposedDropdownMenuBox(
                    expanded = osDropdownExpanded,
                    onExpandedChange = { osDropdownExpanded = !osDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = osLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("OS que precisa desta peça") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = osDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = osDropdownExpanded,
                        onDismissRequest = { osDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Nenhuma OS (Reposição de Estoque)") },
                            onClick = {
                                selectedOsId = null
                                selectedNumeroOs = null
                                motivo = PedidoPecaMotivo.FALTANTE
                                osDropdownExpanded = false
                            }
                        )
                        openOrdens.forEach { os ->
                            DropdownMenuItem(
                                text = { Text("OS #${os.numeroOs} • ${os.equipamento} (${os.clienteNome})") },
                                onClick = {
                                    selectedOsId = os.id
                                    selectedNumeroOs = os.numeroOs
                                    motivo = PedidoPecaMotivo.PARA_OS
                                    osDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = codigoSku,
                    onValueChange = { codigoSku = it },
                    label = { Text("Código / SKU") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = quantidadeText,
                    onValueChange = { quantidadeText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Quantidade *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.7f)
                        .testTag("pedido_peca_qtd_input")
                )
            }

            OutlinedTextField(
                value = fornecedorSugerido,
                onValueChange = { fornecedorSugerido = it },
                label = { Text("Fornecedor sugerido (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Prioridade", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrioridadeOS.entries.forEach { p ->
                    FilterChip(
                        selected = prioridade == p,
                        onClick = { prioridade = p },
                        label = { Text(p.label) }
                    )
                }
            }

            if (initial?.id != null) {
                Text("Status do Pedido", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PedidoPecaStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = observacoes,
                onValueChange = { observacoes = it },
                label = { Text("Observações / Especificação técnica") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            val parsedQtd = (quantidadeText.toIntOrNull() ?: 1).coerceAtLeast(1)
            Button(
                onClick = {
                    if (nomePeca.isNotBlank()) {
                        onSave(
                            (initial ?: PedidoPecaDto(nomePeca = nomePeca.trim())).copy(
                                pecaId = selectedPecaId,
                                osId = selectedOsId,
                                numeroOs = selectedNumeroOs,
                                nomePeca = nomePeca.trim(),
                                codigoSku = codigoSku.trim(),
                                quantidade = parsedQtd,
                                motivo = motivo.code,
                                status = status.code,
                                prioridade = prioridade.code,
                                solicitadoPor = initial?.solicitadoPor?.ifBlank { currentUserName } ?: currentUserName,
                                fornecedorSugerido = fornecedorSugerido.trim(),
                                observacoes = observacoes.trim()
                            )
                        )
                    }
                },
                enabled = !isSubmitting && nomePeca.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_pedido_peca_button")
            ) {
                Text(if (isSubmitting) "Salvando pedido..." else "Salvar na Lista de Pedidos")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

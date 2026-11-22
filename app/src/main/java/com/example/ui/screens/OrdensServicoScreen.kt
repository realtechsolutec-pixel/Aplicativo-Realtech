package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ClienteDto
import com.example.data.model.ConfiguracaoOficinaDto
import com.example.data.model.FuncionarioDto
import com.example.data.model.OrdemServicoDto
import com.example.data.model.PecaEstoqueDto
import com.example.data.model.PedidoPecaDto
import com.example.data.model.PedidoPecaMotivo
import com.example.data.model.StatusOS
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.PriorityOsBadge
import com.example.ui.components.SearchBarField
import com.example.ui.components.StatusOsBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.OsViewMode
import com.example.util.AlertCalculator
import com.example.util.Formatters
import com.example.util.ImageCompressionPreset
import com.example.util.ReceiptFormat
import com.example.util.ReceiptGenerator
import com.example.util.ReceiptKind
import com.example.util.WhatsAppHelper

@Composable
fun OrdensServicoScreen(
    ordens: List<OrdemServicoDto>,
    clientes: List<ClienteDto>,
    funcionarios: List<FuncionarioDto> = emptyList(),
    pecasEstoque: List<PecaEstoqueDto> = emptyList(),
    currentUserName: String = "",
    currentUserEmail: String = "",
    isAdmin: Boolean = true,
    initialFilterOverdue30d: Boolean = false,
    onConsumeOsNavRequest: () -> Unit = {},
    storeConfig: ConfiguracaoOficinaDto = ConfiguracaoOficinaDto(),
    canAccessFinanceiro: Boolean = true,
    compressionPreset: ImageCompressionPreset = ImageCompressionPreset.EQUILIBRADO,
    onCompressionPresetChange: (ImageCompressionPreset) -> Unit = {},
    viewMode: OsViewMode,
    isSubmitting: Boolean,
    onViewModeChange: (OsViewMode) -> Unit,
    onSaveOs: (OrdemServicoDto, List<Uri>, (OrdemServicoDto) -> Unit) -> Unit,
    onMoveStatus: (OrdemServicoDto, StatusOS) -> Unit,
    onDeleteOs: (String) -> Unit,
    onLancarFinanceiro: (OrdemServicoDto) -> Unit,
    onSavePedidoPeca: (PedidoPecaDto, () -> Unit) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<StatusOS?>(null) }
    var onlyOverdue30d by remember { mutableStateOf(initialFilterOverdue30d) }
    var editingOs by remember { mutableStateOf<OrdemServicoDto?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var requestPartInitial by remember { mutableStateOf<PedidoPecaDto?>(null) }
    var receiptOsAndKind by remember { mutableStateOf<Pair<OrdemServicoDto, ReceiptKind>?>(null) }
    var deletingOs by remember { mutableStateOf<OrdemServicoDto?>(null) }
    var previewPhoto by remember { mutableStateOf<Pair<Any, String>?>(null) }

    LaunchedEffect(initialFilterOverdue30d) {
        if (initialFilterOverdue30d) {
            onlyOverdue30d = true
            onConsumeOsNavRequest()
        }
    }

    val overdue30dOrdens = remember(ordens) {
        ordens.filter { it.statusEnum != StatusOS.ENTREGUE && AlertCalculator.diasSemConclusao(it) > 30L }
    }

    val filtered = remember(ordens, search, statusFilter, onlyOverdue30d) {
        ordens.filter { os ->
            val matchOverdue = !onlyOverdue30d || (
                os.statusEnum != StatusOS.ENTREGUE &&
                    AlertCalculator.diasSemConclusao(os) > 30L
                )
            val matchStatus = statusFilter == null || os.statusEnum == statusFilter
            val q = search.trim().lowercase()
            val matchQuery = q.isBlank() ||
                os.numeroOs.toString().contains(q) ||
                os.clienteNome.lowercase().contains(q) ||
                os.tecnicoNome.lowercase().contains(q) ||
                os.equipamento.lowercase().contains(q) ||
                os.marcaModelo.lowercase().contains(q) ||
                os.numeroSerie.lowercase().contains(q)
            matchOverdue && matchStatus && matchQuery
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingOs = null
                    showForm = true
                },
                modifier = Modifier.testTag("add_os_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Ordem de Serviço")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = viewMode == OsViewMode.LISTA,
                        onClick = { onViewModeChange(OsViewMode.LISTA) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        icon = { Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("os_view_list_button")
                    ) {
                        Text("Lista")
                    }
                    SegmentedButton(
                        selected = viewMode == OsViewMode.KANBAN,
                        onClick = { onViewModeChange(OsViewMode.KANBAN) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        icon = { Icon(Icons.Default.ViewKanban, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("os_view_kanban_button")
                    ) {
                        Text("Kanban")
                    }
                }

                SearchBarField(
                    query = search,
                    onQueryChange = { search = it },
                    placeholder = "Buscar OS #, cliente, equipamento ou série...",
                    testTag = "os_search_input"
                )

                if (overdue30dOrdens.isNotEmpty()) {
                    Card(
                        onClick = { onlyOverdue30d = !onlyOverdue30d },
                        colors = CardDefaults.cardColors(containerColor = StatusDangerContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("os_overdue_30d_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alerta de OS acima de 30 dias",
                                tint = StatusDanger,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Alerta: ${overdue30dOrdens.size} OS com +30 dias sem conclusão",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StatusDanger
                                )
                                Text(
                                    text = if (onlyOverdue30d) "Mostrando apenas OS atrasadas (+30d). Toque p/ ver todas." else "Toque para filtrar as OS acima de 30 dias em aberto.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusDanger.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            if (viewMode == OsViewMode.LISTA) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (overdue30dOrdens.isNotEmpty()) {
                        FilterChip(
                            selected = onlyOverdue30d,
                            onClick = { onlyOverdue30d = !onlyOverdue30d },
                            label = { Text("+30d Abertas (${overdue30dOrdens.size})") }
                        )
                    }
                    FilterChip(
                        selected = statusFilter == null && !onlyOverdue30d,
                        onClick = {
                            statusFilter = null
                            onlyOverdue30d = false
                        },
                        label = { Text("Todas (${ordens.size})") }
                    )
                    StatusOS.entries.forEach { st ->
                        val count = ordens.count { it.statusEnum == st }
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = if (statusFilter == st) null else st },
                            label = { Text("${st.shortLabel} ($count)") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Default.Build,
                            title = "Nenhuma Ordem de Serviço",
                            description = "Toque em + para registrar um equipamento com fotos, imprimir comprovante ou enviar via WhatsApp."
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id ?: it.numeroOs }) { os ->
                            OsListCard(
                                os = os,
                                canAccessFinanceiro = canAccessFinanceiro,
                                onEdit = {
                                    editingOs = os
                                    showForm = true
                                },
                                onOpenReceipt = { kind -> receiptOsAndKind = os to kind },
                                onSendOsWhatsApp = {
                                    WhatsAppHelper.sendTextViaWhatsApp(
                                        context,
                                        os.clienteTelefone,
                                        WhatsAppHelper.buildOsMessage(os, storeConfig)
                                    )
                                },
                                onSendWarrantyWhatsApp = {
                                    WhatsAppHelper.sendTextViaWhatsApp(
                                        context,
                                        os.clienteTelefone,
                                        WhatsAppHelper.buildWarrantyMessage(os, storeConfig)
                                    )
                                },
                                onSendWarrantyPdfWhatsApp = {
                                    val file = ReceiptGenerator.generatePdfFile(
                                        context,
                                        os,
                                        ReceiptKind.GARANTIA,
                                        ReceiptFormat.PDF_A4,
                                        storeConfig
                                    )
                                    WhatsAppHelper.sendPdfViaWhatsApp(
                                        context = context,
                                        pdfFile = file,
                                        caption = "Nota de Garantia da OS #${os.numeroOs} (${os.equipamento})",
                                        title = "Nota de Garantia - OS #${os.numeroOs}"
                                    )
                                },
                                onMoveStatus = { next -> onMoveStatus(os, next) },
                                onLancarFinanceiro = { onLancarFinanceiro(os) },
                                onRequestPart = {
                                    requestPartInitial = PedidoPecaDto(
                                        osId = os.id,
                                        numeroOs = os.numeroOs,
                                        nomePeca = "",
                                        motivo = PedidoPecaMotivo.PARA_OS.code,
                                        solicitadoPor = currentUserName,
                                        prioridade = os.prioridade,
                                        observacoes = "Peça necessária para concluir OS #${os.numeroOs} (${os.equipamento})"
                                    )
                                },
                                onPreviewPhoto = { url -> previewPhoto = url to "OS #${os.numeroOs} - ${os.equipamento}" },
                                onDelete = { deletingOs = os }
                            )
                        }
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 84.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(StatusOS.entries, key = { it.code }) { columnStatus ->
                        val colOrders = filtered.filter { it.statusEnum == columnStatus }
                        KanbanColumn(
                            status = columnStatus,
                            orders = colOrders,
                            onEdit = { os ->
                                editingOs = os
                                showForm = true
                            },
                            onReceipt = { os -> receiptOsAndKind = os to ReceiptKind.ENTRADA },
                            onWhatsAppOs = { os ->
                                WhatsAppHelper.sendTextViaWhatsApp(
                                    context,
                                    os.clienteTelefone,
                                    WhatsAppHelper.buildOsMessage(os, storeConfig)
                                )
                            },
                            onMoveStatus = onMoveStatus
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        OrdemServicoFormSheet(
            initial = editingOs,
            clientes = clientes,
            funcionarios = funcionarios,
            currentUserName = currentUserName,
            currentUserEmail = currentUserEmail,
            isAdmin = isAdmin,
            defaultGarantiaDias = storeConfig.garantiaPadraoDias,
            compressionPreset = compressionPreset,
            onCompressionPresetChange = onCompressionPresetChange,
            isSubmitting = isSubmitting,
            onDismiss = { showForm = false },
            onPreviewPhoto = { model -> previewPhoto = model to "Foto do Equipamento" },
            onSave = { dto, localUris ->
                onSaveOs(dto, localUris) { showForm = false }
            }
        )
    }

    requestPartInitial?.let { initialPedido ->
        PedidoPecaFormSheet(
            initial = initialPedido,
            pecasEstoque = pecasEstoque,
            ordens = ordens,
            currentUserName = currentUserName,
            isSubmitting = isSubmitting,
            onDismiss = { requestPartInitial = null },
            onSave = { dto ->
                onSavePedidoPeca(dto) { requestPartInitial = null }
            }
        )
    }

    receiptOsAndKind?.let { (os, initialKind) ->
        ReceiptPreviewSheet(
            os = os,
            storeConfig = storeConfig,
            initialKind = initialKind,
            onDismiss = { receiptOsAndKind = null }
        )
    }

    previewPhoto?.let { (model, title) ->
        FullScreenPhotoDialog(
            imageModel = model,
            title = title,
            onDismiss = { previewPhoto = null }
        )
    }

    deletingOs?.let { target ->
        ConfirmDeleteDialog(
            title = "Excluir OS #${target.numeroOs}",
            message = "Deseja excluir a Ordem de Serviço de ${target.clienteNome} (${target.equipamento})?",
            onConfirm = {
                target.id?.let(onDeleteOs)
                deletingOs = null
            },
            onDismiss = { deletingOs = null }
        )
    }
}

@Composable
private fun OsListCard(
    os: OrdemServicoDto,
    canAccessFinanceiro: Boolean,
    onEdit: () -> Unit,
    onOpenReceipt: (ReceiptKind) -> Unit,
    onSendOsWhatsApp: () -> Unit,
    onSendWarrantyWhatsApp: () -> Unit,
    onSendWarrantyPdfWhatsApp: () -> Unit,
    onMoveStatus: (StatusOS) -> Unit,
    onLancarFinanceiro: () -> Unit,
    onRequestPart: () -> Unit,
    onPreviewPhoto: (String) -> Unit,
    onDelete: () -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var whatsappMenuExpanded by remember { mutableStateOf(false) }
    val diasSemConclusao = remember(os.id, os.status, os.dataEntrada, os.createdAt) {
        AlertCalculator.diasSemConclusao(os)
    }
    val isOverdue30d = os.statusEnum != StatusOS.ENTREGUE && diasSemConclusao > 30L

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
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
                    PriorityOsBadge(priority = os.prioridadeEnum)
                    if (isOverdue30d) {
                        Surface(
                            color = StatusDangerContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "+${diasSemConclusao}d",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = StatusDanger,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Text(
                    text = Formatters.formatCurrency(os.valorTotal),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${os.equipamento} ${os.marcaModelo}".trim(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${os.categoriaEquipamento} • Cliente: ${os.clienteNome}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (os.tecnicoNome.isNotBlank()) {
                Text(
                    text = "Técnico / Resp.: ${os.tecnicoNome}" +
                        if (os.criadoPorNome.isNotBlank() && !os.criadoPorNome.equals(os.tecnicoNome, ignoreCase = true)) {
                            " • Aberta por: ${os.criadoPorNome}"
                        } else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Defeito: ${os.defeitoRelatado}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Miniaturas das fotos do equipamento
            if (os.fotosUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    os.fotosUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Foto do equipamento OS #${os.numeroOs}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onPreviewPhoto(url) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Entrada: ${Formatters.formatDateBr(os.dataEntrada)} • Garantia: ${os.garantiaDias} dias",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        TextButton(
                            onClick = { statusMenuExpanded = true },
                            modifier = Modifier.testTag("os_status_menu_button")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Status")
                        }
                        DropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false }
                        ) {
                            StatusOS.entries.forEach { target ->
                                DropdownMenuItem(
                                    text = { Text(target.label) },
                                    onClick = {
                                        statusMenuExpanded = false
                                        onMoveStatus(target)
                                    }
                                )
                            }
                        }
                    }

                    // Menu WhatsApp (Enviar OS / Enviar Nota de Garantia Texto ou PDF)
                    Box {
                        TextButton(
                            onClick = { whatsappMenuExpanded = true },
                            modifier = Modifier.testTag("os_whatsapp_menu_button")
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", color = Color(0xFF16A34A))
                        }
                        DropdownMenu(
                            expanded = whatsappMenuExpanded,
                            onDismissRequest = { whatsappMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Enviar Resumo da OS") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    whatsappMenuExpanded = false
                                    onSendOsWhatsApp()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Enviar Nota de Garantia (Texto)") },
                                leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusSuccess) },
                                onClick = {
                                    whatsappMenuExpanded = false
                                    onSendWarrantyWhatsApp()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Enviar Nota de Garantia (PDF)") },
                                leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusSuccess) },
                                onClick = {
                                    whatsappMenuExpanded = false
                                    onSendWarrantyPdfWhatsApp()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Abrir Comprovante / Garantia") },
                                leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                onClick = {
                                    whatsappMenuExpanded = false
                                    onOpenReceipt(ReceiptKind.GARANTIA)
                                }
                            )
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = onRequestPart,
                        modifier = Modifier.testTag("os_request_part_button")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = "Pedir peça para esta OS")
                    }
                    IconButton(
                        onClick = { onOpenReceipt(ReceiptKind.ENTRADA) },
                        modifier = Modifier.testTag("os_receipt_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Imprimir / PDF / Garantia")
                    }
                    if (canAccessFinanceiro) {
                        IconButton(
                            onClick = onLancarFinanceiro,
                            modifier = Modifier.testTag("os_lancar_financeiro_button")
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Lançar no Financeiro")
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar OS")
                    }
                    if (!os.id.isNullOrBlank()) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Excluir OS",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanColumn(
    status: StatusOS,
    orders: List<OrdemServicoDto>,
    onEdit: (OrdemServicoDto) -> Unit,
    onReceipt: (OrdemServicoDto) -> Unit,
    onWhatsAppOs: (OrdemServicoDto) -> Unit,
    onMoveStatus: (OrdemServicoDto, StatusOS) -> Unit
) {
    val columnTotal = orders.sumOf { it.valorTotal }

    Surface(
        modifier = Modifier
            .width(292.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusOsBadge(status = status)
                Text(
                    text = "${orders.size} • ${Formatters.formatCurrency(columnTotal)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sem OS nesta etapa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(orders, key = { it.id ?: it.numeroOs }) { os ->
                        var menuOpen by remember { mutableStateOf(false) }
                        Card(
                            onClick = { onEdit(os) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${os.numeroOs}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (os.fotosUrls.isNotEmpty()) {
                                            Icon(
                                                Icons.Default.PhotoCamera,
                                                contentDescription = "${os.fotosUrls.size} fotos",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        PriorityOsBadge(priority = os.prioridadeEnum)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${os.equipamento} ${os.marcaModelo}".trim(),
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = os.clienteNome,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (os.tecnicoNome.isNotBlank()) {
                                    Text(
                                        text = "Resp.: ${os.tecnicoNome}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = Formatters.formatCurrency(os.valorTotal),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row {
                                        IconButton(
                                            onClick = { onReceipt(os) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Print,
                                                contentDescription = "Comprovante",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onWhatsAppOs(os) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = "WhatsApp",
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Row {
                                        Box {
                                            TextButton(onClick = { menuOpen = true }) {
                                                Text("Mover", style = MaterialTheme.typography.labelMedium)
                                            }
                                            DropdownMenu(
                                                expanded = menuOpen,
                                                onDismissRequest = { menuOpen = false }
                                            ) {
                                                StatusOS.entries.forEach { target ->
                                                    DropdownMenuItem(
                                                        text = { Text(target.label) },
                                                        onClick = {
                                                            menuOpen = false
                                                            onMoveStatus(os, target)
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        val next = os.statusEnum.nextStatus()
                                        if (next != null) {
                                            IconButton(
                                                onClick = { onMoveStatus(os, next) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "Avançar para ${next.shortLabel}",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

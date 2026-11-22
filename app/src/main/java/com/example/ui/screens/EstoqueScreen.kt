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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.OrdemServicoDto
import com.example.data.model.PecaEstoqueDto
import com.example.data.model.PedidoPecaDto
import com.example.data.model.PedidoPecaMotivo
import com.example.data.model.PrioridadeOS
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.SearchBarField
import com.example.ui.components.SinglePhotoPickerSection
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.util.Formatters
import com.example.util.ImageCompressionPreset

private val partCategories = listOf(
    "Componentes Eletrônicos",
    "CFTV & Cabos",
    "Alarmes & Sensores",
    "Fontes & Baterias",
    "Automatizadores"
)

@Composable
fun EstoqueScreen(
    pecas: List<PecaEstoqueDto>,
    ordens: List<OrdemServicoDto> = emptyList(),
    currentUserName: String = "",
    isSubmitting: Boolean,
    isAdmin: Boolean = true,
    compressionPreset: ImageCompressionPreset = ImageCompressionPreset.EQUILIBRADO,
    onCompressionPresetChange: (ImageCompressionPreset) -> Unit = {},
    aiGenerating: Boolean,
    aiPlaceholderNotice: Boolean,
    onSavePeca: (PecaEstoqueDto, Uri?, () -> Unit) -> Unit,
    onAdjustQuantity: (PecaEstoqueDto, Int) -> Unit,
    onDeletePeca: (String) -> Unit,
    onGenerateAiDescription: (nome: String, categoria: String, sku: String, onResult: (String) -> Unit) -> Unit,
    onSavePedidoPeca: (PedidoPecaDto, () -> Unit) -> Unit = { _, _ -> }
) {
    var search by remember { mutableStateOf("") }
    var onlyLowStock by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var editingPeca by remember { mutableStateOf<PecaEstoqueDto?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var requestPedidoInitial by remember { mutableStateOf<PedidoPecaDto?>(null) }
    var deletingPeca by remember { mutableStateOf<PecaEstoqueDto?>(null) }
    var previewPhoto by remember { mutableStateOf<Pair<Any, String>?>(null) }
    var viewingSpecPeca by remember { mutableStateOf<PecaEstoqueDto?>(null) }

    val lowStockCount = remember(pecas) { pecas.count { it.isEstoqueBaixo } }

    val filtered = remember(pecas, search, onlyLowStock, selectedCategory) {
        pecas.filter { p ->
            val matchLow = !onlyLowStock || p.isEstoqueBaixo
            val matchCat = selectedCategory == null || p.categoria == selectedCategory
            val q = search.trim().lowercase()
            val matchQuery = q.isBlank() ||
                p.nome.lowercase().contains(q) ||
                p.codigoSku.lowercase().contains(q) ||
                p.fornecedor.lowercase().contains(q)
            matchLow && matchCat && matchQuery
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPeca = null
                    showForm = true
                },
                modifier = Modifier.testTag("add_peca_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Peça")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            if (lowStockCount > 0) {
                Card(
                    onClick = { onlyLowStock = !onlyLowStock },
                    colors = CardDefaults.cardColors(containerColor = StatusDangerContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("estoque_low_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta de estoque baixo",
                            tint = StatusDanger
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alerta de Estoque Baixo ($lowStockCount)",
                                style = MaterialTheme.typography.titleSmall,
                                color = StatusDanger
                            )
                            Text(
                                text = if (onlyLowStock) "Mostrando apenas itens críticos. Toque para ver todos." else "Toque para filtrar os itens abaixo do mínimo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusDanger.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            SearchBarField(
                query = search,
                onQueryChange = { search = it },
                placeholder = "Buscar peça, SKU ou fornecedor...",
                testTag = "estoque_search_input"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = onlyLowStock,
                    onClick = { onlyLowStock = !onlyLowStock },
                    label = { Text("Estoque Baixo ($lowStockCount)") },
                    leadingIcon = {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("filter_low_stock_chip")
                )
                FilterChip(
                    selected = selectedCategory == null && !onlyLowStock,
                    onClick = {
                        selectedCategory = null
                        onlyLowStock = false
                    },
                    label = { Text("Todas (${pecas.size})") }
                )
                partCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.Inventory2,
                    title = "Nenhuma peça cadastrada",
                    description = "Cadastre capacitores, CIs, fontes, câmeras, sensores e baterias com foto e estoque mínimo."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id ?: it.codigoSku + it.nome }) { peca ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Foto do componente/produto no estoque
                                    if (peca.fotoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = peca.fotoUrl,
                                            contentDescription = "Foto de ${peca.nome}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { previewPhoto = peca.fotoUrl to peca.nome }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Memory,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
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
                                                if (peca.codigoSku.isNotBlank()) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = peca.codigoSku,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    color = if (peca.isEstoqueBaixo) StatusDangerContainer else StatusSuccessContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (peca.isEstoqueBaixo) "BAIXO" else "OK",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (peca.isEstoqueBaixo) StatusDanger else StatusSuccess,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = Formatters.formatCurrency(peca.precoVenda),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = peca.nome,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append(peca.categoria)
                                                if (peca.localizacao.isNotBlank()) append(" • Gaveta: ${peca.localizacao}")
                                                if (peca.fornecedor.isNotBlank()) append(" • Forn: ${peca.fornecedor}")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (peca.descricaoDetalhada.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = peca.descricaoDetalhada,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FilledTonalIconButton(
                                            onClick = { onAdjustQuantity(peca, -1) },
                                            enabled = peca.quantidade > 0 && !isSubmitting,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Diminuir estoque")
                                        }
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${peca.quantidade} un",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (peca.isEstoqueBaixo) StatusDanger else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Mín: ${peca.estoqueMinimo}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        FilledTonalIconButton(
                                            onClick = { onAdjustQuantity(peca, +1) },
                                            enabled = !isSubmitting,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Aumentar estoque")
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                requestPedidoInitial = PedidoPecaDto(
                                                    pecaId = peca.id,
                                                    nomePeca = peca.nome,
                                                    codigoSku = peca.codigoSku,
                                                    quantidade = (peca.estoqueMinimo - peca.quantidade).coerceAtLeast(1),
                                                    motivo = PedidoPecaMotivo.FALTANTE.code,
                                                    prioridade = if (peca.quantidade == 0) PrioridadeOS.URGENTE.code else if (peca.isEstoqueBaixo) PrioridadeOS.ALTA.code else PrioridadeOS.NORMAL.code,
                                                    solicitadoPor = currentUserName,
                                                    fornecedorSugerido = peca.fornecedor
                                                )
                                            },
                                            modifier = Modifier.testTag("peca_request_order_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddShoppingCart,
                                                contentDescription = "Adicionar à lista de pedido de peças",
                                                tint = if (peca.isEstoqueBaixo) StatusDanger else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewingSpecPeca = peca },
                                            modifier = Modifier.testTag("peca_ficha_ia_button")
                                        ) {
                                            Icon(
                                                imageVector = if (peca.descricaoDetalhada.isBlank()) Icons.Default.AutoAwesome else Icons.Default.Description,
                                                contentDescription = "Ficha técnica IA",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = {
                                            editingPeca = peca
                                            showForm = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar peça")
                                        }
                                        if (!peca.id.isNullOrBlank()) {
                                            IconButton(onClick = { deletingPeca = peca }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Excluir peça",
                                                    tint = MaterialTheme.colorScheme.error
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

    if (showForm) {
        PecaFormSheet(
            initial = editingPeca,
            isSubmitting = isSubmitting,
            isAdmin = isAdmin,
            compressionPreset = compressionPreset,
            onCompressionPresetChange = onCompressionPresetChange,
            aiGenerating = aiGenerating,
            aiPlaceholderNotice = aiPlaceholderNotice,
            onDismiss = { showForm = false },
            onPreviewPhoto = { model -> previewPhoto = model to "Foto do Componente" },
            onGenerateAiDescription = onGenerateAiDescription,
            onSave = { dto, localUri ->
                onSavePeca(dto, localUri) { showForm = false }
            }
        )
    }

    requestPedidoInitial?.let { initialPedido ->
        PedidoPecaFormSheet(
            initial = initialPedido,
            pecasEstoque = pecas,
            ordens = ordens,
            currentUserName = currentUserName,
            isSubmitting = isSubmitting,
            onDismiss = { requestPedidoInitial = null },
            onSave = { dto ->
                onSavePedidoPeca(dto) { requestPedidoInitial = null }
            }
        )
    }

    viewingSpecPeca?.let { target ->
        PecaSpecDialog(
            peca = target,
            aiGenerating = aiGenerating,
            isSubmitting = isSubmitting,
            onGenerateAndSave = {
                onGenerateAiDescription(target.nome, target.categoria, target.codigoSku) { generated ->
                    val updated = target.copy(descricaoDetalhada = generated)
                    viewingSpecPeca = updated
                    onSavePeca(updated, null) {}
                }
            },
            onEdit = {
                viewingSpecPeca = null
                editingPeca = target
                showForm = true
            },
            onDismiss = { viewingSpecPeca = null }
        )
    }

    previewPhoto?.let { (model, title) ->
        FullScreenPhotoDialog(
            imageModel = model,
            title = title,
            onDismiss = { previewPhoto = null }
        )
    }

    deletingPeca?.let { target ->
        ConfirmDeleteDialog(
            title = "Excluir peça",
            message = "Remover ${target.nome} do catálogo de estoque?",
            onConfirm = {
                target.id?.let(onDeletePeca)
                deletingPeca = null
            },
            onDismiss = { deletingPeca = null }
        )
    }
}

@Composable
private fun PecaSpecDialog(
    peca: PecaEstoqueDto,
    aiGenerating: Boolean,
    isSubmitting: Boolean,
    onGenerateAndSave: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(peca.nome, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${peca.categoria}${if (peca.codigoSku.isNotBlank()) " • SKU: ${peca.codigoSku}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (peca.descricaoDetalhada.isBlank()) {
                    Text(
                        text = "Este componente ainda não possui ficha técnica detalhada.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = peca.descricaoDetalhada,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Surface(
                    color = StatusInfoContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = StatusInfo,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Modo demonstrativo: gera texto técnico de exemplo. Para IA Gemini real, habilite o Firebase AI Logic.",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusInfo
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onGenerateAndSave,
                    enabled = !aiGenerating && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_generate_ai_button")
                ) {
                    if (aiGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gerando descrição...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (peca.descricaoDetalhada.isBlank()) "Gerar Descrição IA e Salvar" else "Atualizar Descrição IA")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) {
                Text("Editar Peça")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PecaFormSheet(
    initial: PecaEstoqueDto?,
    isSubmitting: Boolean,
    isAdmin: Boolean,
    compressionPreset: ImageCompressionPreset,
    onCompressionPresetChange: (ImageCompressionPreset) -> Unit,
    aiGenerating: Boolean,
    aiPlaceholderNotice: Boolean,
    onDismiss: () -> Unit,
    onPreviewPhoto: (Any) -> Unit,
    onGenerateAiDescription: (nome: String, categoria: String, sku: String, onResult: (String) -> Unit) -> Unit,
    onSave: (PecaEstoqueDto, Uri?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sku by remember { mutableStateOf(initial?.codigoSku ?: "") }
    var nome by remember { mutableStateOf(initial?.nome ?: "") }
    var categoria by remember { mutableStateOf(initial?.categoria ?: partCategories.first()) }
    var fornecedor by remember { mutableStateOf(initial?.fornecedor ?: "") }
    var localizacao by remember { mutableStateOf(initial?.localizacao ?: "") }
    var existingFotoUrl by remember { mutableStateOf(initial?.fotoUrl ?: "") }
    var localFotoUri by remember { mutableStateOf<Uri?>(null) }
    var quantidadeText by remember { mutableStateOf((initial?.quantidade ?: 1).toString()) }
    var minimoText by remember { mutableStateOf((initial?.estoqueMinimo ?: 3).toString()) }
    var custoText by remember { mutableStateOf(if ((initial?.precoCusto ?: 0.0) > 0) initial!!.precoCusto.toString() else "") }
    var vendaText by remember { mutableStateOf(if ((initial?.precoVenda ?: 0.0) > 0) initial!!.precoVenda.toString() else "") }
    var descricaoDetalhada by remember { mutableStateOf(initial?.descricaoDetalhada ?: "") }

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
                text = if (initial == null) "Nova Peça no Estoque" else "Editar Peça",
                style = MaterialTheme.typography.headlineSmall
            )

            SinglePhotoPickerSection(
                title = "Foto do Componente / Produto",
                existingUrl = existingFotoUrl,
                localUri = localFotoUri,
                compressionPreset = compressionPreset,
                onCompressionPresetChange = onCompressionPresetChange,
                onClearPhoto = {
                    existingFotoUrl = ""
                    localFotoUri = null
                },
                onSelectLocalUri = { uri -> localFotoUri = uri },
                onPreviewPhoto = onPreviewPhoto
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("Código / SKU") },
                    singleLine = true,
                    modifier = Modifier.weight(0.4f)
                )
                OutlinedTextField(
                    value = localizacao,
                    onValueChange = { localizacao = it },
                    label = { Text("Gaveta / Prateleira") },
                    singleLine = true,
                    modifier = Modifier.weight(0.6f)
                )
            }

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome da Peça / Componente *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("peca_nome_input")
            )

            Text("Categoria", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                partCategories.forEach { cat ->
                    FilterChip(
                        selected = categoria == cat,
                        onClick = { categoria = cat },
                        label = { Text(cat) }
                    )
                }
            }

            OutlinedTextField(
                value = fornecedor,
                onValueChange = { fornecedor = it },
                label = { Text("Fornecedor") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantidadeText,
                    onValueChange = { quantidadeText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Qtd. Atual *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("peca_quantidade_input")
                )
                OutlinedTextField(
                    value = minimoText,
                    onValueChange = { minimoText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Estoque Mínimo *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("peca_minimo_input")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAdmin) {
                    OutlinedTextField(
                        value = custoText,
                        onValueChange = { custoText = it },
                        label = { Text("Preço Custo (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = vendaText,
                    onValueChange = { vendaText = it },
                    label = { Text("Preço Venda (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Descrição Técnica (IA)", style = MaterialTheme.typography.labelLarge)
                FilledTonalButton(
                    onClick = {
                        onGenerateAiDescription(nome.trim(), categoria, sku.trim()) { generated ->
                            descricaoDetalhada = generated
                        }
                    },
                    enabled = !aiGenerating && nome.isNotBlank(),
                    modifier = Modifier.testTag("generate_ai_description_button")
                ) {
                    if (aiGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gerando...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gerar com IA")
                    }
                }
            }

            if (aiPlaceholderNotice || descricaoDetalhada.contains("Exemplo ilustrativo")) {
                Surface(
                    color = StatusInfoContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aviso: resposta de exemplo ilustrativa. A geração real por Gemini AI requer habilitar o Firebase AI Logic.",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusInfo,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            OutlinedTextField(
                value = descricaoDetalhada,
                onValueChange = { descricaoDetalhada = it },
                label = { Text("Especificações técnicas, pinagem e aplicação") },
                minLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("peca_descricao_detalhada_input")
            )

            Button(
                onClick = {
                    if (nome.isNotBlank()) {
                        val dto = (initial ?: PecaEstoqueDto(nome = nome.trim())).copy(
                            codigoSku = sku.trim(),
                            nome = nome.trim(),
                            categoria = categoria,
                            fornecedor = fornecedor.trim(),
                            localizacao = localizacao.trim(),
                            fotoUrl = existingFotoUrl,
                            descricaoDetalhada = descricaoDetalhada.trim(),
                            quantidade = quantidadeText.toIntOrNull() ?: 0,
                            estoqueMinimo = minimoText.toIntOrNull() ?: 3,
                            precoCusto = Formatters.parseDoubleInput(custoText),
                            precoVenda = Formatters.parseDoubleInput(vendaText)
                        )
                        onSave(dto, localFotoUri)
                    }
                },
                enabled = !isSubmitting && nome.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_peca_button")
            ) {
                Text(if (isSubmitting) "Salvando e enviando foto..." else "Salvar Peça")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

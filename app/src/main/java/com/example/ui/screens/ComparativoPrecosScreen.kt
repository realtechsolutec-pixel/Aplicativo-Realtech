package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CotacaoPrecoDto
import com.example.data.model.PecaEstoqueDto
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SearchBarField
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.util.Formatters
import java.net.URLEncoder

@Composable
fun ComparativoPrecosScreen(
    pecas: List<PecaEstoqueDto>,
    cotacoes: List<CotacaoPrecoDto>,
    isSubmitting: Boolean,
    onSaveCotacao: (CotacaoPrecoDto, () -> Unit) -> Unit,
    onDeleteCotacao: (String) -> Unit
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var editingCotacao by remember { mutableStateOf<CotacaoPrecoDto?>(null) }
    var prefillProduto by remember { mutableStateOf("" to "") }
    var deletingCotacao by remember { mutableStateOf<CotacaoPrecoDto?>(null) }

    val q = search.trim().lowercase()

    val matchingPecas = remember(pecas, q) {
        if (q.isBlank()) pecas.take(6)
        else pecas.filter {
            it.nome.lowercase().contains(q) ||
                it.codigoSku.lowercase().contains(q) ||
                it.fornecedor.lowercase().contains(q)
        }
    }

    val groupedQuotes = remember(cotacoes, q) {
        val filtered = if (q.isBlank()) {
            cotacoes
        } else {
            cotacoes.filter {
                it.produtoNome.lowercase().contains(q) ||
                    it.codigoSku.lowercase().contains(q) ||
                    it.fornecedor.lowercase().contains(q)
            }
        }
        filtered
            .groupBy { it.produtoNome.trim().lowercase() }
            .values
            .map { group -> group.sortedBy { it.custoTotal } }
            .sortedBy { it.first().produtoNome.lowercase() }
    }

    fun openExternalSearch(baseUrl: String) {
        val term = search.trim().ifBlank { "componente eletronico cftv" }
        val encoded = URLEncoder.encode(term, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$baseUrl$encoded"))
        context.startActivity(intent)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCotacao = null
                    prefillProduto = search.trim() to ""
                    showForm = true
                },
                modifier = Modifier.testTag("add_cotacao_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Cotação")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Busca & Comparativo de Preços",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Compare cotações de fornecedores, seu estoque interno e pesquise preços online",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SearchBarField(
                    query = search,
                    onQueryChange = { search = it },
                    placeholder = "Buscar componente, peça, SKU ou fornecedor...",
                    testTag = "precos_search_input"
                )
            }

            // Pesquisa externa rápida no navegador
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Pesquisar Preços na Internet",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { openExternalSearch("https://lista.mercadolivre.com.br/") },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("search_mercadolivre_button")
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mercado Livre")
                            }
                            OutlinedButton(
                                onClick = { openExternalSearch("https://www.google.com/search?tbm=shop&q=") },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("search_google_shopping_button")
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Google Shopping")
                            }
                            OutlinedButton(
                                onClick = { openExternalSearch("https://www.google.com/search?q=datasheet+") },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("search_datasheet_button")
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Datasheet")
                            }
                        }
                    }
                }
            }

            // Seção 1: Comparativo de Cotações por Produto
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comparativo entre Fornecedores (${cotacoes.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = {
                        editingCotacao = null
                        prefillProduto = search.trim() to ""
                        showForm = true
                    }) {
                        Text("+ Cotação")
                    }
                }
            }

            if (groupedQuotes.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.CompareArrows,
                        title = "Nenhuma cotação cadastrada",
                        description = "Adicione preços de diferentes fornecedores (ex: Distribuidora A vs B) para destacar o menor custo com frete e margem."
                    )
                }
            } else {
                items(groupedQuotes, key = { it.first().produtoNome.lowercase() }) { group ->
                    val leader = group.first()
                    val worst = group.last()
                    val economia = (worst.custoTotal - leader.custoTotal).coerceAtLeast(0.0)
                    val matchingStock = pecas.find {
                        it.nome.equals(leader.produtoNome, ignoreCase = true) ||
                            (leader.codigoSku.isNotBlank() && it.codigoSku.equals(leader.codigoSku, ignoreCase = true))
                    }

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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = leader.produtoNome,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (leader.codigoSku.isNotBlank()) {
                                        Text(
                                            text = "SKU: ${leader.codigoSku}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (group.size > 1 && economia > 0.0) {
                                    Surface(
                                        color = StatusSuccessContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Economia ${Formatters.formatCurrency(economia)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusSuccess,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (matchingStock != null && matchingStock.precoVenda > 0.0) {
                                val lucro = matchingStock.precoVenda - leader.custoTotal
                                val margem = (lucro / matchingStock.precoVenda) * 100.0
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = StatusInfoContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Venda no estoque: ${Formatters.formatCurrency(matchingStock.precoVenda)} • Margem com melhor cotação: ${Formatters.formatCurrency(lucro)} (${margem.toInt()}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusInfo,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            group.forEachIndexed { idx, quote ->
                                val isBest = idx == 0
                                Surface(
                                    color = if (isBest) StatusSuccessContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = quote.fornecedor,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isBest) {
                                                    Surface(
                                                        color = StatusSuccess,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "MELHOR PREÇO",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = androidx.compose.ui.graphics.Color.White,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "Unit: ${Formatters.formatCurrency(quote.precoUnitario)} + Frete: ${Formatters.formatCurrency(quote.frete)} • ${quote.prazoEntregaDias}d",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = Formatters.formatCurrency(quote.custoTotal),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isBest) StatusSuccess else MaterialTheme.colorScheme.onSurface
                                        )
                                        IconButton(onClick = {
                                            editingCotacao = quote
                                            showForm = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar cotação", modifier = Modifier.size(18.dp))
                                        }
                                        if (!quote.id.isNullOrBlank()) {
                                            IconButton(onClick = { deletingCotacao = quote }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Excluir cotação",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    editingCotacao = null
                                    prefillProduto = leader.produtoNome to leader.codigoSku
                                    showForm = true
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adicionar outro fornecedor para este item")
                            }
                        }
                    }
                }
            }

            // Seção 2: Itens no Estoque Interno
            if (matchingPecas.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Referência no Seu Estoque (${matchingPecas.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(matchingPecas, key = { it.id ?: it.nome }) { peca ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(peca.nome, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "Custo: ${Formatters.formatCurrency(peca.precoCusto)} • Venda: ${Formatters.formatCurrency(peca.precoVenda)} • Qtd: ${peca.quantidade}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (peca.fornecedor.isNotBlank()) {
                                    Text(
                                        text = "Fornecedor atual: ${peca.fornecedor}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    editingCotacao = null
                                    prefillProduto = peca.nome to peca.codigoSku
                                    showForm = true
                                }
                            ) {
                                Text("Cotar")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        CotacaoFormSheet(
            initial = editingCotacao,
            defaultProduto = prefillProduto.first,
            defaultSku = prefillProduto.second,
            isSubmitting = isSubmitting,
            onDismiss = { showForm = false },
            onSave = { dto ->
                onSaveCotacao(dto) { showForm = false }
            }
        )
    }

    deletingCotacao?.let { target ->
        ConfirmDeleteDialog(
            title = "Excluir cotação",
            message = "Remover a cotação de ${target.fornecedor} para ${target.produtoNome}?",
            onConfirm = {
                target.id?.let(onDeleteCotacao)
                deletingCotacao = null
            },
            onDismiss = { deletingCotacao = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CotacaoFormSheet(
    initial: CotacaoPrecoDto?,
    defaultProduto: String,
    defaultSku: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (CotacaoPrecoDto) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var produtoNome by remember { mutableStateOf(initial?.produtoNome ?: defaultProduto) }
    var codigoSku by remember { mutableStateOf(initial?.codigoSku ?: defaultSku) }
    var fornecedor by remember { mutableStateOf(initial?.fornecedor ?: "") }
    var precoText by remember { mutableStateOf(if ((initial?.precoUnitario ?: 0.0) > 0) initial!!.precoUnitario.toString() else "") }
    var freteText by remember { mutableStateOf(if ((initial?.frete ?: 0.0) > 0) initial!!.frete.toString() else "") }
    var prazoText by remember { mutableStateOf((initial?.prazoEntregaDias ?: 2).toString()) }
    var observacoes by remember { mutableStateOf(initial?.observacoes ?: "") }

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
                text = if (initial == null) "Nova Cotação de Preço" else "Editar Cotação",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = produtoNome,
                onValueChange = { produtoNome = it },
                label = { Text("Produto / Componente *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cotacao_produto_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = codigoSku,
                    onValueChange = { codigoSku = it },
                    label = { Text("SKU / Código") },
                    singleLine = true,
                    modifier = Modifier.weight(0.4f)
                )
                OutlinedTextField(
                    value = fornecedor,
                    onValueChange = { fornecedor = it },
                    label = { Text("Fornecedor / Loja *") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.6f)
                        .testTag("cotacao_fornecedor_input")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = precoText,
                    onValueChange = { precoText = it },
                    label = { Text("Preço Unit. (R$) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cotacao_preco_input")
                )
                OutlinedTextField(
                    value = freteText,
                    onValueChange = { freteText = it },
                    label = { Text("Frete (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = prazoText,
                onValueChange = { prazoText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Prazo de Entrega (dias)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = observacoes,
                onValueChange = { observacoes = it },
                label = { Text("Observações / Link ou Condição") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (produtoNome.isNotBlank() && fornecedor.isNotBlank()) {
                        val dto = (initial ?: CotacaoPrecoDto(
                            produtoNome = produtoNome.trim(),
                            fornecedor = fornecedor.trim()
                        )).copy(
                            produtoNome = produtoNome.trim(),
                            codigoSku = codigoSku.trim(),
                            fornecedor = fornecedor.trim(),
                            precoUnitario = Formatters.parseDoubleInput(precoText),
                            frete = Formatters.parseDoubleInput(freteText),
                            prazoEntregaDias = prazoText.toIntOrNull() ?: 2,
                            observacoes = observacoes.trim()
                        )
                        onSave(dto)
                    }
                },
                enabled = !isSubmitting && produtoNome.isNotBlank() && fornecedor.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_cotacao_button")
            ) {
                Text(if (isSubmitting) "Salvando..." else "Salvar Cotação")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

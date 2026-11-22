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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ClienteDto
import com.example.data.model.ConfiguracaoOficinaDto
import com.example.data.model.FuncionarioDto
import com.example.data.model.PecaEstoqueDto
import com.example.data.model.RegistrarVendaItemRequest
import com.example.data.model.RegistrarVendaRpcRequest
import com.example.data.model.SALE_PAYMENT_METHODS
import com.example.data.model.StatusVenda
import com.example.data.model.VendaDto
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SearchBarField
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.util.Formatters
import com.example.util.WhatsAppHelper

private enum class VendasTab(val label: String) {
    PDV("Nova Venda (PDV)"),
    HISTORICO("Histórico & Notas")
}

private data class CartSaleItem(
    val key: String,
    val pecaId: String?,
    val codigoSku: String,
    val nome: String,
    val precoUnitario: Double,
    val quantidade: Int,
    val maxEstoque: Int?
) {
    val subtotal: Double get() = precoUnitario * quantidade
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendasScreen(
    vendas: List<VendaDto>,
    pecas: List<PecaEstoqueDto>,
    clientes: List<ClienteDto>,
    funcionarios: List<FuncionarioDto>,
    storeConfig: ConfiguracaoOficinaDto,
    currentUserName: String,
    isAdmin: Boolean,
    isSubmitting: Boolean,
    onRegistrarVenda: (RegistrarVendaRpcRequest, onCompleted: (VendaDto) -> Unit) -> Unit,
    onCancelarVenda: (String, onDone: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(VendasTab.PDV) }

    // Estado do PDV
    var stockQuery by remember { mutableStateOf("") }
    val cart = remember { mutableStateListOf<CartSaleItem>() }
    var selectedCliente by remember { mutableStateOf<ClienteDto?>(null) }
    var clienteMenuExpanded by remember { mutableStateOf(false) }
    var clienteAvulsoNome by remember { mutableStateOf("Consumidor Final") }
    var clienteAvulsoTel by remember { mutableStateOf("") }

    var selectedVendedorNome by remember(currentUserName) { mutableStateOf(currentUserName) }
    var selectedVendedorId by remember { mutableStateOf<String?>(null) }
    var vendedorMenuExpanded by remember { mutableStateOf(false) }

    var formaPagamento by remember { mutableStateOf(SALE_PAYMENT_METHODS.first()) }
    var descontoText by remember { mutableStateOf("") }
    var pagamentoPendente by remember { mutableStateOf(false) }
    var observacoes by remember { mutableStateOf("") }

    var showCustomItemDialog by remember { mutableStateOf(false) }
    var receiptVenda by remember { mutableStateOf<VendaDto?>(null) }

    // Estado do Histórico
    var historyQuery by remember { mutableStateOf("") }
    var historyFilter by remember { mutableStateOf<StatusVenda?>(null) }
    var cancelingVenda by remember { mutableStateOf<VendaDto?>(null) }

    val cartSubtotal = cart.sumOf { it.subtotal }
    val descontoValor = Formatters.parseDoubleInput(descontoText).coerceIn(0.0, cartSubtotal)
    val cartTotal = (cartSubtotal - descontoValor).coerceAtLeast(0.0)

    val filteredPecas = remember(pecas, stockQuery) {
        val q = stockQuery.trim().lowercase()
        pecas.filter { p ->
            q.isEmpty() ||
                p.nome.lowercase().contains(q) ||
                p.codigoSku.lowercase().contains(q) ||
                p.categoria.lowercase().contains(q)
        }
    }

    val filteredVendas = remember(vendas, historyQuery, historyFilter) {
        val q = historyQuery.trim().lowercase()
        vendas.filter { v ->
            val matchFilter = historyFilter == null || v.statusEnum == historyFilter
            val matchQ = q.isEmpty() ||
                v.numeroVenda.toString().contains(q) ||
                v.clienteNome.lowercase().contains(q) ||
                v.vendedorNome.lowercase().contains(q)
            matchFilter && matchQ
        }
    }

    fun addStockItemToCart(peca: PecaEstoqueDto) {
        val pecaId = peca.id ?: return
        if (peca.quantidade <= 0) return
        val idx = cart.indexOfFirst { it.pecaId == pecaId }
        if (idx >= 0) {
            val existing = cart[idx]
            if (existing.quantidade < peca.quantidade) {
                cart[idx] = existing.copy(quantidade = existing.quantidade + 1)
            }
        } else {
            cart.add(
                CartSaleItem(
                    key = pecaId,
                    pecaId = pecaId,
                    codigoSku = peca.codigoSku,
                    nome = peca.nome,
                    precoUnitario = peca.precoVenda,
                    quantidade = 1,
                    maxEstoque = peca.quantidade
                )
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                VendasTab.entries.forEachIndexed { idx, tab ->
                    val label = if (tab == VendasTab.PDV && cart.isNotEmpty()) {
                        "${tab.label} (${cart.sumOf { it.quantidade }})"
                    } else if (tab == VendasTab.HISTORICO) {
                        "${tab.label} (${vendas.size})"
                    } else {
                        tab.label
                    }
                    SegmentedButton(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(idx, VendasTab.entries.size),
                        modifier = Modifier.testTag("vendas_tab_${tab.name.lowercase()}")
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        if (activeTab == VendasTab.PDV) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("vendas_pdv_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Busca de produtos no estoque + botão item avulso
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchBarField(
                            query = stockQuery,
                            onQueryChange = { stockQuery = it },
                            placeholder = "Buscar peça no estoque por nome ou SKU...",
                            testTag = "pdv_stock_search_input",
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { showCustomItemDialog = true },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("pdv_add_custom_item_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Item avulso")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Avulso")
                        }
                    }
                }

                // Catálogo rápido do estoque (baixa automática)
                item {
                    Text(
                        text = "Produtos do Estoque (baixa automática)",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                if (filteredPecas.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.ShoppingBag,
                            title = "Nenhuma peça encontrada",
                            description = "Cadastre itens no Estoque ou use o botão 'Avulso' acima."
                        )
                    }
                } else {
                    items(filteredPecas.take(12), key = { it.id ?: it.nome }) { peca ->
                        val inCartQty = cart.firstOrNull { it.pecaId == peca.id }?.quantidade ?: 0
                        val hasStock = peca.quantidade > inCartQty
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = peca.nome,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            if (peca.codigoSku.isNotBlank()) append("${peca.codigoSku} • ")
                                            append("Disp.: ${peca.quantidade} un")
                                            if (inCartQty > 0) append(" ($inCartQty no carrinho)")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (peca.quantidade <= 0) StatusDanger else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = Formatters.formatCurrency(peca.precoVenda),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Button(
                                    onClick = { addStockItemToCart(peca) },
                                    enabled = hasStock && peca.id != null,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("pdv_add_peca_${peca.id ?: peca.nome}")
                                ) {
                                    Icon(Icons.Default.AddShoppingCart, contentDescription = "Adicionar", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Carrinho de Venda
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Carrinho da Venda (${cart.sumOf { it.quantidade }} itens)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (cart.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Adicione produtos do estoque acima ou toque em 'Avulso' para montar a venda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(cart, key = { it.key }) { item ->
                        val idx = cart.indexOfFirst { it.key == item.key }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.nome, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${Formatters.formatCurrency(item.precoUnitario)} un • Sub: ${Formatters.formatCurrency(item.subtotal)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (idx >= 0) {
                                            if (item.quantidade > 1) {
                                                cart[idx] = item.copy(quantidade = item.quantidade - 1)
                                            } else {
                                                cart.removeAt(idx)
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("cart_dec_${item.key}")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Diminuir")
                                }
                                Text(
                                    text = item.quantidade.toString(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily)
                                )
                                IconButton(
                                    onClick = {
                                        if (idx >= 0 && (item.maxEstoque == null || item.quantidade < item.maxEstoque)) {
                                            cart[idx] = item.copy(quantidade = item.quantidade + 1)
                                        }
                                    },
                                    enabled = item.maxEstoque == null || item.quantidade < item.maxEstoque,
                                    modifier = Modifier.testTag("cart_inc_${item.key}")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Aumentar")
                                }
                                IconButton(
                                    onClick = { if (idx >= 0) cart.removeAt(idx) },
                                    modifier = Modifier.testTag("cart_del_${item.key}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = StatusDanger)
                                }
                            }
                        }
                    }
                }

                // Cliente, Vendedor, Pagamento e Checkout
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Dados da Venda & Pagamento", style = MaterialTheme.typography.titleMedium)

                            ExposedDropdownMenuBox(
                                expanded = clienteMenuExpanded,
                                onExpandedChange = { clienteMenuExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedCliente?.nome ?: "Consumidor Final (Avulso)",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Cliente") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clienteMenuExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("pdv_cliente_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = clienteMenuExpanded,
                                    onDismissRequest = { clienteMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Consumidor Final (Avulso)") },
                                        onClick = {
                                            selectedCliente = null
                                            clienteMenuExpanded = false
                                        }
                                    )
                                    clientes.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text("${c.nome} (${c.telefone})") },
                                            onClick = {
                                                selectedCliente = c
                                                clienteMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (selectedCliente == null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = clienteAvulsoNome,
                                        onValueChange = { clienteAvulsoNome = it },
                                        label = { Text("Nome no Cupom") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = clienteAvulsoTel,
                                        onValueChange = { clienteAvulsoTel = it },
                                        label = { Text("WhatsApp") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = vendedorMenuExpanded,
                                onExpandedChange = { vendedorMenuExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedVendedorNome.ifBlank { currentUserName },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Vendedor / Atendente") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vendedorMenuExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("pdv_vendedor_dropdown")
                                )
                                ExposedDropdownMenu(
                                    expanded = vendedorMenuExpanded,
                                    onDismissRequest = { vendedorMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("$currentUserName (Logado)") },
                                        onClick = {
                                            selectedVendedorId = null
                                            selectedVendedorNome = currentUserName
                                            vendedorMenuExpanded = false
                                        }
                                    )
                                    funcionarios.filter { it.ativo }.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text("${f.nome} (${f.cargo})") },
                                            onClick = {
                                                selectedVendedorId = f.id
                                                selectedVendedorNome = f.nome
                                                vendedorMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Text("Forma de Pagamento", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SALE_PAYMENT_METHODS.forEach { method ->
                                    FilterChip(
                                        selected = formaPagamento == method,
                                        onClick = { formaPagamento = method },
                                        label = { Text(method) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = descontoText,
                                    onValueChange = { descontoText = it },
                                    label = { Text("Desconto (R$)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pdv_desconto_input")
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Switch(
                                        checked = pagamentoPendente,
                                        onCheckedChange = { pagamentoPendente = it },
                                        modifier = Modifier.testTag("pdv_pendente_switch")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("A Receber", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            OutlinedTextField(
                                value = observacoes,
                                onValueChange = { observacoes = it },
                                label = { Text("Observações da Venda (opcional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "TOTAL DA VENDA",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = "Subtotal ${Formatters.formatCurrency(cartSubtotal)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                    Text(
                                        text = Formatters.formatCurrency(cartTotal),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val cliente = selectedCliente
                                    val req = RegistrarVendaRpcRequest(
                                        pClienteId = cliente?.id,
                                        pClienteNome = cliente?.nome ?: clienteAvulsoNome.ifBlank { "Consumidor Final" },
                                        pClienteTelefone = cliente?.telefone ?: clienteAvulsoTel,
                                        pVendedorId = selectedVendedorId,
                                        pVendedorNome = selectedVendedorNome.ifBlank { currentUserName },
                                        pFormaPagamento = formaPagamento,
                                        pDesconto = descontoValor,
                                        pPagamentoPendente = pagamentoPendente,
                                        pObservacoes = observacoes,
                                        pItens = cart.map { item ->
                                            RegistrarVendaItemRequest(
                                                pecaId = item.pecaId,
                                                codigoSku = item.codigoSku,
                                                nome = item.nome,
                                                quantidade = item.quantidade,
                                                precoUnitario = item.precoUnitario
                                            )
                                        }
                                    )
                                    onRegistrarVenda(req) { saved ->
                                        cart.clear()
                                        descontoText = ""
                                        observacoes = ""
                                        pagamentoPendente = false
                                        receiptVenda = saved
                                    }
                                },
                                enabled = !isSubmitting && cart.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("pdv_finalizar_venda_button")
                            ) {
                                Icon(Icons.Default.PointOfSale, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isSubmitting) "Processando venda..." else "Finalizar Venda & Emitir Nota")
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        } else {
            // Histórico de Vendas & Notas
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchBarField(
                        query = historyQuery,
                        onQueryChange = { historyQuery = it },
                        placeholder = "Buscar venda por nº, cliente ou vendedor...",
                        testTag = "vendas_history_search_input"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = historyFilter == null,
                            onClick = { historyFilter = null },
                            label = { Text("Todas (${vendas.size})") }
                        )
                        StatusVenda.entries.forEach { st ->
                            FilterChip(
                                selected = historyFilter == st,
                                onClick = { historyFilter = if (historyFilter == st) null else st },
                                label = { Text("${st.label} (${vendas.count { it.statusEnum == st }})") }
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("vendas_history_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredVendas.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.ReceiptLong,
                                title = "Nenhuma venda registrada",
                                description = "As vendas feitas no PDV aparecem aqui com Nota A4, Cupom Térmico 80mm e histórico de baixa no estoque."
                            )
                        }
                    } else {
                        items(filteredVendas, key = { it.id ?: it.numeroVenda }) { venda ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
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
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "#${venda.numeroVenda}",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(
                                                color = venda.statusEnum.containerColor,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = venda.statusEnum.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = venda.statusEnum.accentColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = Formatters.formatCurrency(venda.valorTotal),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (venda.statusEnum == StatusVenda.CANCELADA) StatusDanger else StatusSuccess
                                        )
                                    }

                                    Text(
                                        text = venda.clienteNome,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${venda.formaPagamento} • ${Formatters.formatDateTimeBr(venda.dataVenda ?: venda.createdAt)} • ${venda.itens.size} item(ns)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (venda.vendedorNome.isNotBlank()) {
                                        Text(
                                            text = "Vendedor: ${venda.vendedorNome}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusInfo
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { receiptVenda = venda },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("venda_nota_button_${venda.numeroVenda}")
                                        ) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Nota / Cupom")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val msg = WhatsAppHelper.buildSaleMessage(venda, storeConfig)
                                                WhatsAppHelper.sendTextViaWhatsApp(context, venda.clienteTelefone, msg)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("WhatsApp")
                                        }
                                        if (isAdmin && venda.statusEnum != StatusVenda.CANCELADA && !venda.id.isNullOrBlank()) {
                                            IconButton(
                                                onClick = { cancelingVenda = venda },
                                                modifier = Modifier.testTag("cancel_venda_button_${venda.numeroVenda}")
                                            ) {
                                                Icon(Icons.Default.Cancel, contentDescription = "Cancelar Venda", tint = StatusDanger)
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

    if (showCustomItemDialog) {
        AddCustomSaleItemDialog(
            onDismiss = { showCustomItemDialog = false },
            onConfirm = { nome, qty, preco ->
                cart.add(
                    CartSaleItem(
                        key = "avulso_${System.currentTimeMillis()}",
                        pecaId = null,
                        codigoSku = "AVULSO",
                        nome = nome,
                        precoUnitario = preco,
                        quantidade = qty,
                        maxEstoque = null
                    )
                )
                showCustomItemDialog = false
            }
        )
    }

    receiptVenda?.let { venda ->
        SaleReceiptPreviewSheet(
            venda = venda,
            storeConfig = storeConfig,
            onDismiss = { receiptVenda = null }
        )
    }

    cancelingVenda?.let { venda ->
        AlertDialog(
            onDismissRequest = { cancelingVenda = null },
            title = { Text("Cancelar Venda #${venda.numeroVenda}?") },
            text = {
                Text("As peças vinculadas ao estoque serão devolvidas automaticamente e o lançamento financeiro será removido.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = venda.id ?: return@TextButton
                        cancelingVenda = null
                        onCancelarVenda(id) {}
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusDanger),
                    modifier = Modifier.testTag("confirm_cancel_venda_button")
                ) {
                    Text("Confirmar Cancelamento")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelingVenda = null }) {
                    Text("Voltar")
                }
            }
        )
    }
}

@Composable
private fun AddCustomSaleItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (nome: String, qty: Int, preco: Double) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("1") }
    var precoText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Item Avulso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Descrição do Produto / Serviço *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                        label = { Text("Qtd") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = precoText,
                        onValueChange = { precoText = it },
                        label = { Text("Valor Unit. (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = (qtyText.toIntOrNull() ?: 1).coerceAtLeast(1)
                    val preco = Formatters.parseDoubleInput(precoText).coerceAtLeast(0.0)
                    if (nome.isNotBlank()) {
                        onConfirm(nome.trim(), qty, preco)
                    }
                },
                enabled = nome.isNotBlank()
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

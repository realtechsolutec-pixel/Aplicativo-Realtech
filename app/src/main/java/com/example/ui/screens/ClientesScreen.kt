package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ClienteDto
import com.example.data.model.OrdemServicoDto
import com.example.data.model.TipoCliente
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SearchBarField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(
    clientes: List<ClienteDto>,
    ordens: List<OrdemServicoDto>,
    isSubmitting: Boolean,
    onSaveCliente: (ClienteDto, () -> Unit) -> Unit,
    onDeleteCliente: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var tipoFilter by remember { mutableStateOf<TipoCliente?>(null) }
    var editingCliente by remember { mutableStateOf<ClienteDto?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var deletingCliente by remember { mutableStateOf<ClienteDto?>(null) }

    val filtered = remember(clientes, search, tipoFilter) {
        clientes.filter { c ->
            val matchTipo = tipoFilter == null || c.tipo == tipoFilter?.code
            val q = search.trim().lowercase()
            val matchQuery = q.isBlank() ||
                c.nome.lowercase().contains(q) ||
                c.telefone.lowercase().contains(q) ||
                c.documento.lowercase().contains(q)
            matchTipo && matchQuery
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCliente = null
                    showForm = true
                },
                modifier = Modifier.testTag("add_cliente_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Cliente")
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
            SearchBarField(
                query = search,
                onQueryChange = { search = it },
                placeholder = "Buscar por nome, telefone ou CPF/CNPJ...",
                testTag = "clientes_search_input"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tipoFilter == null,
                    onClick = { tipoFilter = null },
                    label = { Text("Todos (${clientes.size})") }
                )
                TipoCliente.entries.forEach { tipo ->
                    FilterChip(
                        selected = tipoFilter == tipo,
                        onClick = { tipoFilter = if (tipoFilter == tipo) null else tipo },
                        label = { Text(tipo.label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.Person,
                    title = "Nenhum cliente encontrado",
                    description = "Toque no botão + para cadastrar clientes Pessoa Física ou Empresas de segurança/eletrônica."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id ?: it.nome + it.telefone }) { cliente ->
                        val osCount = ordens.count {
                            (cliente.id != null && it.clienteId == cliente.id) ||
                                it.clienteNome.equals(cliente.nome, ignoreCase = true)
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (cliente.tipo == TipoCliente.PJ.code) {
                                                Icons.Default.Business
                                            } else {
                                                Icons.Default.Person
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cliente.nome,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${TipoCliente.fromCode(cliente.tipo).label}" +
                                                if (cliente.documento.isNotBlank()) " • ${cliente.documento}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = "$osCount OS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cliente.telefone,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (cliente.endereco.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cliente.endereco,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = {
                                            editingCliente = cliente
                                            showForm = true
                                        },
                                        modifier = Modifier.testTag("edit_cliente_button")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar cliente")
                                    }
                                    if (!cliente.id.isNullOrBlank()) {
                                        IconButton(
                                            onClick = { deletingCliente = cliente },
                                            modifier = Modifier.testTag("delete_cliente_button")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Excluir cliente",
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

    if (showForm) {
        ClienteFormSheet(
            initial = editingCliente,
            isSubmitting = isSubmitting,
            onDismiss = { showForm = false },
            onSave = { dto ->
                onSaveCliente(dto) { showForm = false }
            }
        )
    }

    deletingCliente?.let { target ->
        ConfirmDeleteDialog(
            title = "Excluir cliente",
            message = "Deseja remover ${target.nome} do cadastro? As OS vinculadas manterão o nome registrado.",
            onConfirm = {
                target.id?.let(onDeleteCliente)
                deletingCliente = null
            },
            onDismiss = { deletingCliente = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClienteFormSheet(
    initial: ClienteDto?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (ClienteDto) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nome by remember { mutableStateOf(initial?.nome ?: "") }
    var tipo by remember { mutableStateOf(TipoCliente.fromCode(initial?.tipo)) }
    var documento by remember { mutableStateOf(initial?.documento ?: "") }
    var telefone by remember { mutableStateOf(initial?.telefone ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var endereco by remember { mutableStateOf(initial?.endereco ?: "") }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (initial == null) "Novo Cliente" else "Editar Cliente",
                style = MaterialTheme.typography.headlineSmall
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TipoCliente.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = tipo == item,
                        onClick = { tipo = item },
                        shape = SegmentedButtonDefaults.itemShape(index, TipoCliente.entries.size)
                    ) {
                        Text(item.label)
                    }
                }
            }

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome completo / Razão Social *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cliente_nome_input")
            )

            OutlinedTextField(
                value = telefone,
                onValueChange = { telefone = it },
                label = { Text("Telefone / WhatsApp *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cliente_telefone_input")
            )

            OutlinedTextField(
                value = documento,
                onValueChange = { documento = it },
                label = { Text(if (tipo == TipoCliente.PJ) "CNPJ" else "CPF") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = endereco,
                onValueChange = { endereco = it },
                label = { Text("Endereço completo") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = observacoes,
                onValueChange = { observacoes = it },
                label = { Text("Observações (portão, CFTV instalado, etc.)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (nome.isNotBlank() && telefone.isNotBlank()) {
                        onSave(
                            (initial ?: ClienteDto(nome = nome.trim(), telefone = telefone.trim())).copy(
                                nome = nome.trim(),
                                tipo = tipo.code,
                                documento = documento.trim(),
                                telefone = telefone.trim(),
                                email = email.trim(),
                                endereco = endereco.trim(),
                                observacoes = observacoes.trim()
                            )
                        )
                    }
                },
                enabled = !isSubmitting && nome.isNotBlank() && telefone.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_cliente_button")
            ) {
                Text(if (isSubmitting) "Salvando..." else "Salvar Cliente")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

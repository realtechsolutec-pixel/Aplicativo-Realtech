package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ConfiguracaoOficinaDto
import com.example.data.model.FuncionarioDto
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.util.CommissionCalculator
import com.example.util.Formatters
import com.example.util.ImageCompressionPreset

@Composable
fun ConfiguracaoOficinaScreen(
    config: ConfiguracaoOficinaDto,
    workshopOwnerId: String,
    compressionPreset: ImageCompressionPreset,
    funcionarios: List<FuncionarioDto>,
    isSubmitting: Boolean,
    onSaveConfig: (ConfiguracaoOficinaDto) -> Unit,
    onCompressionPresetChange: (ImageCompressionPreset) -> Unit,
    onCreateFuncionario: (nome: String, email: String, password: String, cargo: String, comissaoPercentual: Double, onDone: () -> Unit) -> Unit,
    onUpdateFuncionarioPerfil: (FuncionarioDto, String, Double, () -> Unit) -> Unit,
    onToggleFuncionarioAtivo: (FuncionarioDto) -> Unit,
    onDeleteFuncionario: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var nomeLoja by remember(config.id) { mutableStateOf(config.nomeLoja) }
    var cnpj by remember(config.id) { mutableStateOf(config.cnpj) }
    var telefone by remember(config.id) { mutableStateOf(config.telefone) }
    var email by remember(config.id) { mutableStateOf(config.email) }
    var endereco by remember(config.id) { mutableStateOf(config.endereco) }
    var garantiaDiasText by remember(config.id) { mutableStateOf(config.garantiaPadraoDias.toString()) }
    var rodapeOs by remember(config.id) { mutableStateOf(config.rodapeOs) }

    var showAddEmployeeSheet by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<FuncionarioDto?>(null) }
    var deletingEmployee by remember { mutableStateOf<FuncionarioDto?>(null) }

    LaunchedEffect(config) {
        nomeLoja = config.nomeLoja
        cnpj = config.cnpj
        telefone = config.telefone
        email = config.email
        endereco = config.endereco
        garantiaDiasText = config.garantiaPadraoDias.toString()
        rodapeOs = config.rodapeOs
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("config_oficina_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Dados da Loja na Ordem de Serviço",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Essas informações aparecem nos PDFs A4, cupons térmicos 80mm/58mm e mensagens do WhatsApp",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Pré-visualização do cabeçalho da OS
        item {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PRÉVIA DO CABEÇALHO DA OS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "OS #1001",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = nomeLoja.ifBlank { "RTerp Eletrônica & Segurança" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (cnpj.isNotBlank() || telefone.isNotBlank()) {
                        Text(
                            text = buildList {
                                if (cnpj.isNotBlank()) add("CNPJ: $cnpj")
                                if (telefone.isNotBlank()) add("Tel: $telefone")
                            }.joinToString("  •  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                    if (endereco.isNotBlank()) {
                        Text(
                            text = endereco,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    if (email.isNotBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Formulário da Loja
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nomeLoja,
                        onValueChange = { nomeLoja = it },
                        label = { Text("Nome Fantasia / Razão Social *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("config_nome_loja_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = cnpj,
                            onValueChange = { cnpj = it },
                            label = { Text("CNPJ / CPF") },
                            placeholder = { Text("00.000.000/0001-00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("config_cnpj_input")
                        )
                        OutlinedTextField(
                            value = telefone,
                            onValueChange = { telefone = it },
                            label = { Text("Telefone / WhatsApp") },
                            placeholder = { Text("(11) 99999-9999") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("config_telefone_input")
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail de Contato") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("config_email_input")
                    )

                    OutlinedTextField(
                        value = endereco,
                        onValueChange = { endereco = it },
                        label = { Text("Endereço Completo (Rua, Nº, Bairro, Cidade)") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("config_endereco_input")
                    )

                    OutlinedTextField(
                        value = garantiaDiasText,
                        onValueChange = { garantiaDiasText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Garantia Padrão das OS (dias)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("config_garantia_input")
                    )

                    OutlinedTextField(
                        value = rodapeOs,
                        onValueChange = { rodapeOs = it },
                        label = { Text("Termos / Observação Padrão no Rodapé da OS") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("config_rodape_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (nomeLoja.isNotBlank()) {
                                onSaveConfig(
                                    config.copy(
                                        nomeLoja = nomeLoja.trim(),
                                        cnpj = cnpj.trim(),
                                        telefone = telefone.trim(),
                                        email = email.trim(),
                                        endereco = endereco.trim(),
                                        garantiaPadraoDias = garantiaDiasText.toIntOrNull() ?: 90,
                                        rodapeOs = rodapeOs.trim()
                                    )
                                )
                            }
                        },
                        enabled = !isSubmitting && nomeLoja.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_config_oficina_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isSubmitting) "Salvando configurações..." else "Salvar Dados da Loja")
                    }
                }
            }
        }

        // Compactação de Imagens no Supabase Storage
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Compress,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Compactação de Fotos (Supabase)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Converte fotos de OS e estoque para WebP otimizado antes do upload",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ImageCompressionPreset.entries.forEach { preset ->
                            val selected = compressionPreset == preset
                            FilterChip(
                                selected = selected,
                                onClick = { onCompressionPresetChange(preset) },
                                label = { Text("${preset.label} — ${preset.description}") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("config_compression_${preset.code.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Arquitetura Multi-Loja (SaaS) — Capacidade Comercial
        item {
            Surface(
                color = StatusInfoContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("config_saas_capacity_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Arquitetura Multi-Loja (SaaS Comercial)",
                        style = MaterialTheme.typography.titleSmall,
                        color = StatusInfo,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Preparado para 200+ contas ADM simultâneas × até 4 funcionários cada (~1.000 acessos concorrentes), com isolamento RLS otimizado, índices por loja e retry automático.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF082F49)
                    )
                }
            }
        }

        // Equipe / Login de Funcionários (Operacional — Máximo 4 acessos)
        item {
            val canAddMore = funcionarios.size < 4
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Equipe & Login de Funcionários",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Acesso somente ao operacional (OS, Clientes, Estoque e Peças). Financeiro bloqueado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (canAddMore) StatusInfoContainer else StatusDangerContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("funcionarios_limit_badge")
                        ) {
                            Text(
                                text = "${funcionarios.size}/4 acessos",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (canAddMore) StatusInfo else StatusDanger,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        color = if (canAddMore) StatusInfoContainer.copy(alpha = 0.65f) else StatusDangerContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (canAddMore) {
                                "Cada oficina suporta até 4 acessos de funcionários (${4 - funcionarios.size} vaga(s) disponível(is))."
                            } else {
                                "Limite máximo de 4 acessos de funcionários atingido (4/4). Remova ou substitua um acesso para cadastrar outro."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (canAddMore) StatusInfo else StatusDanger,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }

                    if (workshopOwnerId.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Código da Oficina (p/ vincular funcionário)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = workshopOwnerId,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(workshopOwnerId))
                                        Toast.makeText(context, "Código da Oficina copiado!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("copy_workshop_code_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar código", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copiar")
                                }
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = { showAddEmployeeSheet = true },
                        enabled = canAddMore && !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_funcionario_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (canAddMore) "Cadastrar Login de Funcionário" else "Limite de 4 acessos atingido (4/4)")
                    }
                }
            }
        }

        if (funcionarios.isNotEmpty()) {
            items(funcionarios, key = { it.id ?: it.email }) { func ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = func.nome,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = if (func.ativo) StatusSuccessContainer else StatusDangerContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (func.ativo) "ATIVO" else "BLOQUEADO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (func.ativo) StatusSuccess else StatusDanger,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${func.cargo} • ${formatPercent(func.comissaoPercentual)}% mão de obra • ${func.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = StatusInfoContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Comissão: ${formatPercent(func.comissaoPercentual)}% s/ mão de obra",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = StatusInfo,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (!func.id.isNullOrBlank()) {
                            IconButton(
                                onClick = { editingEmployee = func },
                                modifier = Modifier.testTag("edit_funcionario_${func.id}")
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Alterar função e comissão"
                                )
                            }
                        }
                        Switch(
                            checked = func.ativo,
                            onCheckedChange = { onToggleFuncionarioAtivo(func) },
                            enabled = !isSubmitting
                        )
                        if (!func.id.isNullOrBlank()) {
                            IconButton(onClick = { deletingEmployee = func }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remover funcionário",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEmployeeSheet) {
        AddFuncionarioSheet(
            isSubmitting = isSubmitting,
            onDismiss = { showAddEmployeeSheet = false },
            onSave = { nome, emailFunc, senha, cargo, comissao ->
                onCreateFuncionario(nome, emailFunc, senha, cargo, comissao) {
                    showAddEmployeeSheet = false
                }
            }
        )
    }

    editingEmployee?.let { target ->
        EditFuncionarioComissaoDialog(
            funcionario = target,
            isSubmitting = isSubmitting,
            onDismiss = { editingEmployee = null },
            onSave = { novoCargo, novaComissao ->
                onUpdateFuncionarioPerfil(target, novoCargo, novaComissao) {
                    editingEmployee = null
                }
            }
        )
    }

    deletingEmployee?.let { target ->
        ConfirmDeleteDialog(
            title = "Remover funcionário",
            message = "Remover ${target.nome} (${target.email}) da equipe?",
            onConfirm = {
                target.id?.let(onDeleteFuncionario)
                deletingEmployee = null
            },
            onDismiss = { deletingEmployee = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFuncionarioSheet(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (nome: String, email: String, password: String, cargo: String, comissaoPercentual: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nome by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("Técnico") }
    var comissaoText by remember { mutableStateOf("60") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val parsedComissao = Formatters.parseDoubleInput(comissaoText).coerceIn(0.0, 100.0)
    val previewGanho = CommissionCalculator.calculateGanho(100.0, parsedComissao)
    val quickRoles = listOf("Técnico", "Atendente", "Técnico Operacional", "Instalador")
    val quickPercents = listOf(40, 50, 60, 70, 80)

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
                text = "Novo Funcionário (Operacional)",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Defina a função (Técnico, Atendente, etc.) e a porcentagem de ganho sobre a mão de obra. O financeiro da loja permanece oculto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome do Funcionário *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("func_nome_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickRoles.forEach { preset ->
                    FilterChip(
                        selected = cargo.equals(preset, ignoreCase = true),
                        onClick = {
                            cargo = preset
                            if (preset == "Atendente" && comissaoText == "60") {
                                comissaoText = "80"
                            } else if (preset == "Técnico" && comissaoText == "80") {
                                comissaoText = "60"
                            }
                        },
                        label = { Text(preset) }
                    )
                }
            }

            OutlinedTextField(
                value = cargo,
                onValueChange = { cargo = it },
                label = { Text("Cargo / Função *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("func_cargo_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickPercents.forEach { pct ->
                    FilterChip(
                        selected = parsedComissao.toInt() == pct,
                        onClick = { comissaoText = pct.toString() },
                        label = { Text("$pct%") }
                    )
                }
            }

            OutlinedTextField(
                value = comissaoText,
                onValueChange = { comissaoText = it },
                label = { Text("Comissão sobre Mão de Obra (%) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("func_comissao_input")
            )

            Surface(
                color = StatusSuccessContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Exemplo: Em serviço de R$ 100,00 de mão de obra, o funcionário recebe ${Formatters.formatCurrency(previewGanho)} (${formatPercent(parsedComissao)}%).",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusSuccess,
                    modifier = Modifier.padding(10.dp)
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail de Login *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("func_email_input")
            )

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha inicial (mín. 6 caracteres) *") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Mostrar ou ocultar senha"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("func_password_input")
            )

            Button(
                onClick = {
                    if (nome.isNotBlank() && email.isNotBlank() && senha.length >= 6) {
                        onSave(nome, email, senha, cargo, parsedComissao)
                    }
                },
                enabled = !isSubmitting && nome.isNotBlank() && email.isNotBlank() && senha.length >= 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_funcionario_button")
            ) {
                Text(if (isSubmitting) "Criando acesso..." else "Criar Acesso Operacional")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

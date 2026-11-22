package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ClienteDto
import com.example.data.model.FuncionarioDto
import com.example.data.model.OrdemServicoDto
import com.example.data.model.PrioridadeOS
import com.example.data.model.StatusOS
import com.example.ui.components.MultiPhotoPickerSection
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.util.CommissionCalculator
import com.example.util.Formatters
import com.example.util.ImageCompressionPreset

private val equipmentCategories = listOf(
    "Eletrônica Geral",
    "CFTV / Câmeras & DVR",
    "Alarme / Cerca Elétrica",
    "Automatizador / Portão",
    "Nobreak / Fonte",
    "Controle de Acesso"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdemServicoFormSheet(
    initial: OrdemServicoDto?,
    clientes: List<ClienteDto>,
    funcionarios: List<FuncionarioDto> = emptyList(),
    currentUserName: String = "",
    currentUserEmail: String = "",
    isAdmin: Boolean = true,
    defaultGarantiaDias: Int = 90,
    compressionPreset: ImageCompressionPreset = ImageCompressionPreset.EQUILIBRADO,
    onCompressionPresetChange: (ImageCompressionPreset) -> Unit = {},
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onPreviewPhoto: (Any) -> Unit,
    onSave: (OrdemServicoDto, List<Uri>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activeFuncionarios = remember(funcionarios) {
        funcionarios.filter { it.ativo }
    }
    val loggedInFuncionario = remember(activeFuncionarios, currentUserEmail, currentUserName) {
        activeFuncionarios.find {
            (currentUserEmail.isNotBlank() && it.email.equals(currentUserEmail.trim(), ignoreCase = true)) ||
                (currentUserName.isNotBlank() && it.nome.equals(currentUserName.trim(), ignoreCase = true))
        }
    }

    var selectedClienteId by remember { mutableStateOf(initial?.clienteId) }
    var clienteNome by remember { mutableStateOf(initial?.clienteNome ?: "") }
    var clienteTelefone by remember { mutableStateOf(initial?.clienteTelefone ?: "") }
    var clientDropdownExpanded by remember { mutableStateOf(false) }

    var selectedTecnicoId by remember {
        mutableStateOf(
            if (initial != null) initial.tecnicoId else loggedInFuncionario?.id
        )
    }
    var tecnicoNome by remember {
        mutableStateOf(
            when {
                initial != null && initial.tecnicoNome.isNotBlank() -> initial.tecnicoNome
                loggedInFuncionario != null -> loggedInFuncionario.nome
                !isAdmin && currentUserName.isNotBlank() -> currentUserName
                else -> currentUserName
            }
        )
    }
    var tecnicoEmail by remember {
        mutableStateOf(
            when {
                initial != null && initial.tecnicoEmail.isNotBlank() -> initial.tecnicoEmail
                loggedInFuncionario != null -> loggedInFuncionario.email
                !isAdmin && currentUserEmail.isNotBlank() -> currentUserEmail
                else -> currentUserEmail
            }
        )
    }
    var comissaoPercentual by remember {
        mutableStateOf(
            when {
                initial != null && initial.comissaoPercentual > 0.0 -> initial.comissaoPercentual
                initial != null -> {
                    CommissionCalculator.resolveEffectivePercent(initial, funcionarios)
                }
                loggedInFuncionario != null -> loggedInFuncionario.comissaoPercentual
                else -> 0.0
            }
        )
    }
    var techDropdownExpanded by remember { mutableStateOf(false) }

    var categoriaEquip by remember { mutableStateOf(initial?.categoriaEquipamento ?: equipmentCategories.first()) }
    var equipamento by remember { mutableStateOf(initial?.equipamento ?: "") }
    var marcaModelo by remember { mutableStateOf(initial?.marcaModelo ?: "") }
    var numeroSerie by remember { mutableStateOf(initial?.numeroSerie ?: "") }
    var acessorios by remember { mutableStateOf(initial?.acessoriosEntregues ?: "") }
    var defeito by remember { mutableStateOf(initial?.defeitoRelatado ?: "") }
    var diagnostico by remember { mutableStateOf(initial?.diagnosticoTecnico ?: "") }
    var pecasUsadas by remember { mutableStateOf(initial?.pecasUtilizadas ?: "") }
    var existingPhotos by remember { mutableStateOf(initial?.fotosUrls ?: emptyList()) }
    var localPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var status by remember { mutableStateOf(initial?.statusEnum ?: StatusOS.ABERTA) }
    var prioridade by remember { mutableStateOf(initial?.prioridadeEnum ?: PrioridadeOS.NORMAL) }
    var maoObraText by remember { mutableStateOf(if ((initial?.valorMaoObra ?: 0.0) > 0) initial!!.valorMaoObra.toString() else "") }
    var pecasText by remember { mutableStateOf(if ((initial?.valorPecas ?: 0.0) > 0) initial!!.valorPecas.toString() else "") }
    var descontoText by remember { mutableStateOf(if ((initial?.desconto ?: 0.0) > 0) initial!!.desconto.toString() else "") }
    var garantiaText by remember { mutableStateOf((initial?.garantiaDias ?: defaultGarantiaDias).toString()) }

    val maoObraVal = Formatters.parseDoubleInput(maoObraText)
    val pecasVal = Formatters.parseDoubleInput(pecasText)
    val descontoVal = Formatters.parseDoubleInput(descontoText)
    val totalVal = (maoObraVal + pecasVal - descontoVal).coerceAtLeast(0.0)
    val estimatedCommission = CommissionCalculator.calculateGanho(maoObraVal, comissaoPercentual)

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
                text = if (initial == null) "Nova Ordem de Serviço" else "Editar OS #${initial.numeroOs}",
                style = MaterialTheme.typography.headlineSmall
            )

            if (clientes.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = clientDropdownExpanded,
                    onExpandedChange = { clientDropdownExpanded = !clientDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = clienteNome,
                        onValueChange = {
                            clienteNome = it
                            selectedClienteId = null
                        },
                        label = { Text("Cliente (selecione ou digite) *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .testTag("os_cliente_input")
                    )
                    ExposedDropdownMenu(
                        expanded = clientDropdownExpanded,
                        onDismissRequest = { clientDropdownExpanded = false }
                    ) {
                        clientes.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.nome} (${c.telefone})") },
                                onClick = {
                                    selectedClienteId = c.id
                                    clienteNome = c.nome
                                    clienteTelefone = c.telefone
                                    clientDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = clienteNome,
                    onValueChange = { clienteNome = it },
                    label = { Text("Nome do Cliente *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("os_cliente_input")
                )
            }

            OutlinedTextField(
                value = clienteTelefone,
                onValueChange = { clienteTelefone = it },
                label = { Text("Telefone / WhatsApp do Cliente") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Técnico / Atendente Responsável
            if (activeFuncionarios.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = techDropdownExpanded,
                    onExpandedChange = { techDropdownExpanded = !techDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = tecnicoNome,
                        onValueChange = { typed ->
                            tecnicoNome = typed
                            selectedTecnicoId = null
                            tecnicoEmail = ""
                            val exact = activeFuncionarios.find { it.nome.equals(typed.trim(), ignoreCase = true) }
                            if (exact != null) {
                                selectedTecnicoId = exact.id
                                tecnicoEmail = exact.email
                                comissaoPercentual = exact.comissaoPercentual
                            }
                        },
                        label = { Text("Técnico / Funcionário Responsável") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = techDropdownExpanded) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .testTag("os_tecnico_input")
                    )
                    ExposedDropdownMenu(
                        expanded = techDropdownExpanded,
                        onDismissRequest = { techDropdownExpanded = false }
                    ) {
                        if (currentUserName.isNotBlank() && isAdmin) {
                            DropdownMenuItem(
                                text = { Text("$currentUserName (Admin / Loja • 0%)") },
                                onClick = {
                                    selectedTecnicoId = null
                                    tecnicoNome = currentUserName
                                    tecnicoEmail = currentUserEmail
                                    comissaoPercentual = 0.0
                                    techDropdownExpanded = false
                                }
                            )
                        }
                        activeFuncionarios.forEach { f ->
                            DropdownMenuItem(
                                text = {
                                    Text("${f.nome} (${f.cargo} • ${formatPercent(f.comissaoPercentual)}% MO)")
                                },
                                onClick = {
                                    selectedTecnicoId = f.id
                                    tecnicoNome = f.nome
                                    tecnicoEmail = f.email
                                    comissaoPercentual = f.comissaoPercentual
                                    techDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = tecnicoNome,
                    onValueChange = { tecnicoNome = it },
                    label = { Text("Técnico / Funcionário Responsável") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("os_tecnico_input")
                )
            }

            Text("Categoria do Equipamento", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                equipmentCategories.forEach { cat ->
                    FilterChip(
                        selected = categoriaEquip == cat,
                        onClick = { categoriaEquip = cat },
                        label = { Text(cat) }
                    )
                }
            }

            OutlinedTextField(
                value = equipamento,
                onValueChange = { equipamento = it },
                label = { Text("Equipamento (ex: DVR 8CH, Central Alarme) *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("os_equipamento_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = marcaModelo,
                    onValueChange = { marcaModelo = it },
                    label = { Text("Marca / Modelo") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = numeroSerie,
                    onValueChange = { numeroSerie = it },
                    label = { Text("Nº de Série") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = acessorios,
                onValueChange = { acessorios = it },
                label = { Text("Acessórios deixados (fonte, controle, cabos)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            MultiPhotoPickerSection(
                title = "Fotos do Equipamento",
                existingUrls = existingPhotos,
                localUris = localPhotoUris,
                compressionPreset = compressionPreset,
                onCompressionPresetChange = onCompressionPresetChange,
                onRemoveExistingUrl = { url -> existingPhotos = existingPhotos.filterNot { it == url } },
                onAddLocalUris = { added -> localPhotoUris = (localPhotoUris + added).distinct() },
                onRemoveLocalUri = { uri -> localPhotoUris = localPhotoUris.filterNot { it == uri } },
                onPreviewPhoto = onPreviewPhoto
            )

            OutlinedTextField(
                value = defeito,
                onValueChange = { defeito = it },
                label = { Text("Defeito relatado pelo cliente *") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("os_defeito_input")
            )

            OutlinedTextField(
                value = diagnostico,
                onValueChange = { diagnostico = it },
                label = { Text("Diagnóstico técnico / Serviço executado") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = pecasUsadas,
                onValueChange = { pecasUsadas = it },
                label = { Text("Peças substituídas") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Status da OS", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusOS.entries.forEach { st ->
                    FilterChip(
                        selected = status == st,
                        onClick = { status = st },
                        label = { Text(st.shortLabel) }
                    )
                }
            }

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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = maoObraText,
                    onValueChange = { maoObraText = it },
                    label = { Text("Mão de Obra (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = pecasText,
                    onValueChange = { pecasText = it },
                    label = { Text("Peças (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = descontoText,
                    onValueChange = { descontoText = it },
                    label = { Text("Desconto (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = garantiaText,
                    onValueChange = { garantiaText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Garantia (dias)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Estimado da OS", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = Formatters.formatCurrency(totalVal),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (comissaoPercentual > 0.0 && tecnicoNome.isNotBlank()) {
                Surface(
                    color = StatusSuccessContainer,
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
                                text = "Ganho de $tecnicoNome (${formatPercent(comissaoPercentual)}% Mão de Obra)",
                                style = MaterialTheme.typography.labelMedium,
                                color = StatusSuccess
                            )
                            Text(
                                text = "Base: ${Formatters.formatCurrency(maoObraVal)} de mão de obra",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusSuccess.copy(alpha = 0.85f)
                            )
                        }
                        Text(
                            text = Formatters.formatCurrency(estimatedCommission),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = StatusSuccess
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (clienteNome.isNotBlank() && equipamento.isNotBlank() && defeito.isNotBlank()) {
                        val dto = (initial ?: OrdemServicoDto(
                            clienteNome = clienteNome.trim(),
                            equipamento = equipamento.trim(),
                            defeitoRelatado = defeito.trim(),
                            criadoPorNome = currentUserName.trim()
                        )).copy(
                            clienteId = selectedClienteId,
                            clienteNome = clienteNome.trim(),
                            clienteTelefone = clienteTelefone.trim(),
                            tecnicoId = selectedTecnicoId,
                            tecnicoNome = tecnicoNome.trim(),
                            tecnicoEmail = tecnicoEmail.trim(),
                            criadoPorNome = initial?.criadoPorNome?.ifBlank { currentUserName.trim() } ?: currentUserName.trim(),
                            comissaoPercentual = comissaoPercentual.coerceIn(0.0, 100.0),
                            categoriaEquipamento = categoriaEquip,
                            equipamento = equipamento.trim(),
                            marcaModelo = marcaModelo.trim(),
                            numeroSerie = numeroSerie.trim(),
                            acessoriosEntregues = acessorios.trim(),
                            defeitoRelatado = defeito.trim(),
                            diagnosticoTecnico = diagnostico.trim(),
                            pecasUtilizadas = pecasUsadas.trim(),
                            fotosUrls = existingPhotos,
                            status = status.code,
                            prioridade = prioridade.code,
                            valorMaoObra = maoObraVal,
                            valorPecas = pecasVal,
                            desconto = descontoVal,
                            valorTotal = totalVal,
                            garantiaDias = garantiaText.toIntOrNull() ?: defaultGarantiaDias
                        )
                        onSave(dto, localPhotoUris)
                    }
                },
                enabled = !isSubmitting && clienteNome.isNotBlank() && equipamento.isNotBlank() && defeito.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_os_button")
            ) {
                Text(if (isSubmitting) "Salvando e enviando fotos..." else "Salvar Ordem de Serviço")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

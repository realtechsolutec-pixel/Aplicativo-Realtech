package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConfiguracaoOficinaDto
import com.example.data.model.OrdemServicoDto
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.StatusSuccess
import com.example.util.Formatters
import com.example.util.ReceiptFormat
import com.example.util.ReceiptGenerator
import com.example.util.ReceiptKind
import com.example.util.WhatsAppHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPreviewSheet(
    os: OrdemServicoDto,
    storeConfig: ConfiguracaoOficinaDto = ConfiguracaoOficinaDto(),
    initialKind: ReceiptKind = ReceiptKind.ENTRADA,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kind by remember(os.id, initialKind) { mutableStateOf(initialKind) }
    var format by remember { mutableStateOf(ReceiptFormat.PDF_A4) }

    val thermalPreviewText = remember(os, kind, storeConfig) {
        ReceiptGenerator.buildThermalText(os, kind, storeConfig)
    }

    val whatsappGreen = Color(0xFF16A34A)
    val baseDelivery = os.dataSaida ?: Formatters.nowIsoTimestamp()
    val validUntil = Formatters.addDaysToDateBr(baseDelivery, os.garantiaDias)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Comprovantes & Garantia • OS #${os.numeroOs}",
                style = MaterialTheme.typography.headlineSmall
            )

            // Tipo: Entrada vs Saída vs Nota de Garantia
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ReceiptKind.entries.forEachIndexed { idx, item ->
                    SegmentedButton(
                        selected = kind == item,
                        onClick = { kind = item },
                        shape = SegmentedButtonDefaults.itemShape(idx, ReceiptKind.entries.size),
                        modifier = Modifier.testTag("receipt_kind_${item.name.lowercase()}")
                    ) {
                        val shortLabel = when (item) {
                            ReceiptKind.ENTRADA -> "Entrada"
                            ReceiptKind.SAIDA -> "Saída"
                            ReceiptKind.GARANTIA -> "Garantia"
                        }
                        Text(shortLabel)
                    }
                }
            }

            // Formato: PDF A4 vs Térmica 80mm/58mm
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ReceiptFormat.entries.forEachIndexed { idx, item ->
                    SegmentedButton(
                        selected = format == item,
                        onClick = { format = item },
                        shape = SegmentedButtonDefaults.itemShape(idx, ReceiptFormat.entries.size),
                        modifier = Modifier.testTag("receipt_format_${item.name.lowercase()}")
                    ) {
                        Text(item.label)
                    }
                }
            }

            // Pré-visualização
            if (format == ReceiptFormat.TERMICA_80MM) {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    Text(
                        text = thermalPreviewText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(14.dp)
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (kind == ReceiptKind.GARANTIA) Color(0xFF064E3B) else Color(0xFF0F172A),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = storeConfig.nomeLoja.ifBlank { "RTerp Eletrônica & Segurança" },
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (storeConfig.cnpj.isNotBlank() || storeConfig.telefone.isNotBlank()) {
                                    Text(
                                        text = buildList {
                                            if (storeConfig.cnpj.isNotBlank()) add("CNPJ: ${storeConfig.cnpj}")
                                            if (storeConfig.telefone.isNotBlank()) add(storeConfig.telefone)
                                        }.joinToString(" • "),
                                        color = Color(0xFFCBD5E1),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                if (storeConfig.endereco.isNotBlank()) {
                                    Text(
                                        text = storeConfig.endereco,
                                        color = Color(0xFF94A3B8),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text(
                                    text = kind.label,
                                    color = if (kind == ReceiptKind.GARANTIA) Color(0xFF34D399) else Color(0xFF38BDF8),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = "OS #${os.numeroOs}",
                                color = if (kind == ReceiptKind.GARANTIA) Color(0xFF34D399) else Color(0xFF38BDF8),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Text("Cliente: ${os.clienteNome}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        if (os.tecnicoNome.isNotBlank()) {
                            Text("Técnico Responsável: ${os.tecnicoNome}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            "Equipamento: ${os.categoriaEquipamento} - ${os.equipamento} ${os.marcaModelo}".trim(),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (os.numeroSerie.isNotBlank()) {
                            Text("Nº Série: ${os.numeroSerie}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (kind == ReceiptKind.GARANTIA) {
                            Text(
                                "Serviço coberto: ${os.diagnosticoTecnico.ifBlank { os.defeitoRelatado }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Garantia válida até $validUntil (${os.garantiaDias} dias)",
                                style = MaterialTheme.typography.labelMedium,
                                color = StatusSuccess
                            )
                        } else {
                            Text("Defeito: ${os.defeitoRelatado}", style = MaterialTheme.typography.bodySmall)
                            if (kind == ReceiptKind.SAIDA && os.diagnosticoTecnico.isNotBlank()) {
                                Text("Diagnóstico: ${os.diagnosticoTecnico}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total: ${Formatters.formatCurrency(os.valorTotal)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily)
                            )
                            Text(
                                text = "Garantia: ${os.garantiaDias} dias",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Ações de Impressão e PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val file = ReceiptGenerator.generatePdfFile(context, os, kind, format, storeConfig)
                        ReceiptGenerator.printPdfFile(
                            context = context,
                            file = file,
                            jobName = "RTerp_OS_${os.numeroOs}_${kind.name}",
                            isThermal = format == ReceiptFormat.TERMICA_80MM
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("print_receipt_button")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Imprimir")
                }

                OutlinedButton(
                    onClick = {
                        val file = ReceiptGenerator.generatePdfFile(context, os, kind, format, storeConfig)
                        ReceiptGenerator.sharePdfFile(
                            context = context,
                            file = file,
                            title = "${kind.label} - OS #${os.numeroOs}"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_pdf_receipt_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF")
                }
            }

            // Ações WhatsApp (Texto Direto + Anexo PDF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val msg = if (kind == ReceiptKind.GARANTIA) {
                            WhatsAppHelper.buildWarrantyMessage(os, storeConfig)
                        } else {
                            WhatsAppHelper.buildOsMessage(os, storeConfig)
                        }
                        WhatsAppHelper.sendTextViaWhatsApp(context, os.clienteTelefone, msg)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = whatsappGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("whatsapp_text_button")
                ) {
                    Icon(
                        if (kind == ReceiptKind.GARANTIA) Icons.Default.VerifiedUser else Icons.Default.Share,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (kind == ReceiptKind.GARANTIA) "Garantia WhatsApp" else "OS WhatsApp")
                }

                OutlinedButton(
                    onClick = {
                        val file = ReceiptGenerator.generatePdfFile(context, os, kind, format, storeConfig)
                        val caption = if (kind == ReceiptKind.GARANTIA) {
                            "Segue a Nota de Garantia referente à OS #${os.numeroOs} (${os.equipamento})."
                        } else {
                            "Segue o comprovante da OS #${os.numeroOs} (${os.equipamento})."
                        }
                        WhatsAppHelper.sendPdfViaWhatsApp(
                            context = context,
                            pdfFile = file,
                            caption = caption,
                            title = "${kind.label} - OS #${os.numeroOs}"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("whatsapp_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF WhatsApp")
                }
            }

            if (format == ReceiptFormat.TERMICA_80MM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ReceiptGenerator.copyToClipboard(
                                context,
                                "Cupom OS #${os.numeroOs}",
                                thermalPreviewText
                            )
                            Toast.makeText(context, "Cupom térmico copiado!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar Cupom")
                    }
                    OutlinedButton(
                        onClick = {
                            ReceiptGenerator.shareThermalText(
                                context = context,
                                text = thermalPreviewText,
                                title = "Cupom Térmico OS #${os.numeroOs}"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartilhar Cupom")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

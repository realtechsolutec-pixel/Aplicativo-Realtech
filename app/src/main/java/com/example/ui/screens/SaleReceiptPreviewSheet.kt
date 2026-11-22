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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.example.data.model.StatusVenda
import com.example.data.model.VendaDto
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.Formatters
import com.example.util.ReceiptFormat
import com.example.util.ReceiptGenerator
import com.example.util.WhatsAppHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleReceiptPreviewSheet(
    venda: VendaDto,
    storeConfig: ConfiguracaoOficinaDto = ConfiguracaoOficinaDto(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var format by remember { mutableStateOf(ReceiptFormat.PDF_A4) }

    val thermalPreviewText = remember(venda, storeConfig) {
        ReceiptGenerator.buildSaleThermalText(venda, storeConfig)
    }

    val whatsappGreen = Color(0xFF16A34A)
    val isCanceled = venda.statusEnum == StatusVenda.CANCELADA

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
                text = "Nota / Cupom de Venda #${venda.numeroVenda}",
                style = MaterialTheme.typography.headlineSmall
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ReceiptFormat.entries.forEachIndexed { idx, item ->
                    SegmentedButton(
                        selected = format == item,
                        onClick = { format = item },
                        shape = SegmentedButtonDefaults.itemShape(idx, ReceiptFormat.entries.size),
                        modifier = Modifier.testTag("sale_receipt_format_${item.name.lowercase()}")
                    ) {
                        Text(item.label)
                    }
                }
            }

            if (format == ReceiptFormat.TERMICA_80MM) {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
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
                                    if (isCanceled) Color(0xFF7F1D1D) else Color(0xFF0F172A),
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
                                Text(
                                    text = "NOTA DE VENDA (${venda.statusEnum.label.uppercase()})",
                                    color = if (isCanceled) Color(0xFFFCA5A5) else Color(0xFF38BDF8),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = "#${venda.numeroVenda}",
                                color = if (isCanceled) Color(0xFFFCA5A5) else Color(0xFF38BDF8),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Text(
                            text = "Cliente: ${venda.clienteNome}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (venda.vendedorNome.isNotBlank()) {
                            Text("Vendedor: ${venda.vendedorNome}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = "Pagamento: ${venda.formaPagamento} • ${Formatters.formatDateTimeBr(venda.dataVenda ?: venda.createdAt)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        venda.itens.forEachIndexed { idx, item ->
                            val itemSub = if (item.subtotal > 0.0) item.subtotal else item.quantidade * item.precoUnitario
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${idx + 1}. ${item.nome} (${item.quantidade}x)",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = Formatters.formatCurrency(itemSub),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily)
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        if (venda.desconto > 0.0) {
                            Text(
                                text = "Desconto: -${Formatters.formatCurrency(venda.desconto)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "Total da Venda: ${Formatters.formatCurrency(venda.valorTotal)}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val file = ReceiptGenerator.generateSalePdfFile(context, venda, format, storeConfig)
                        ReceiptGenerator.printPdfFile(
                            context = context,
                            file = file,
                            jobName = "RTerp_Venda_${venda.numeroVenda}",
                            isThermal = format == ReceiptFormat.TERMICA_80MM
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("print_sale_receipt_button")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Imprimir")
                }

                OutlinedButton(
                    onClick = {
                        val file = ReceiptGenerator.generateSalePdfFile(context, venda, format, storeConfig)
                        ReceiptGenerator.sharePdfFile(
                            context = context,
                            file = file,
                            title = "Nota de Venda #${venda.numeroVenda}"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_sale_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val msg = WhatsAppHelper.buildSaleMessage(venda, storeConfig)
                        WhatsAppHelper.sendTextViaWhatsApp(context, venda.clienteTelefone, msg)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = whatsappGreen),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("whatsapp_sale_text_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp")
                }

                OutlinedButton(
                    onClick = {
                        val file = ReceiptGenerator.generateSalePdfFile(context, venda, format, storeConfig)
                        WhatsAppHelper.sendPdfViaWhatsApp(
                            context = context,
                            pdfFile = file,
                            caption = "Segue a Nota de Venda #${venda.numeroVenda}.",
                            title = "Nota de Venda #${venda.numeroVenda}"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("whatsapp_sale_pdf_button")
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
                                "Cupom Venda #${venda.numeroVenda}",
                                thermalPreviewText
                            )
                            Toast.makeText(context, "Cupom copiado!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("copy_sale_coupon_button")
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
                                title = "Cupom Venda #${venda.numeroVenda}"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_sale_coupon_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Texto Cupom")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

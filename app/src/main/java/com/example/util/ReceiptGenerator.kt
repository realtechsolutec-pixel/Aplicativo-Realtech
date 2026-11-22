package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.ConfiguracaoOficinaDto
import com.example.data.model.OrdemServicoDto
import com.example.data.model.StatusVenda
import com.example.data.model.VendaDto
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class ReceiptKind(val label: String) {
    ENTRADA("Comprovante de Entrada"),
    SAIDA("Comprovante de Saída"),
    GARANTIA("Nota de Garantia")
}

enum class ReceiptFormat(val label: String) {
    PDF_A4("PDF A4"),
    TERMICA_80MM("Térmica 80mm / 58mm")
}

object ReceiptGenerator {

    fun buildThermalText(
        os: OrdemServicoDto,
        kind: ReceiptKind,
        storeConfig: ConfiguracaoOficinaDto = ConfiguracaoOficinaDto(),
        columns: Int = 38
    ): String {
        val sep = "=".repeat(columns)
        val dash = "-".repeat(columns)
        val sb = StringBuilder()

        fun center(text: String): String {
            val clean = text.take(columns)
            val pad = ((columns - clean.length) / 2).coerceAtLeast(0)
            return " ".repeat(pad) + clean
        }

        fun kv(label: String, value: String): String = "$label: $value"

        val baseDelivery = os.dataSaida ?: Formatters.nowIsoTimestamp()
        val validUntil = Formatters.addDaysToDateBr(baseDelivery, os.garantiaDias)
        val storeTitle = storeConfig.nomeLoja.ifBlank { "TECSEC ELETRÔNICA & SEGURANÇA" }.uppercase()

        sb.appendLine(sep)
        sb.appendLine(center(storeTitle))
        if (storeConfig.cnpj.isNotBlank()) sb.appendLine(center("CNPJ: ${storeConfig.cnpj}"))
        if (storeConfig.telefone.isNotBlank()) sb.appendLine(center("Tel: ${storeConfig.telefone}"))
        if (storeConfig.endereco.isNotBlank()) sb.appendLine(storeConfig.endereco)
        sb.appendLine(sep)
        sb.appendLine(center(kind.label.uppercase()))
        sb.appendLine(center("OS Nº #${os.numeroOs}"))
        sb.appendLine(dash)
        sb.appendLine(kv("Entrada", Formatters.formatDateTimeBr(os.dataEntrada)))
        if (kind == ReceiptKind.SAIDA || kind == ReceiptKind.GARANTIA) {
            sb.appendLine(kv("Entrega", Formatters.formatDateTimeBr(baseDelivery)))
        }
        sb.appendLine(kv("Status", os.statusEnum.label))
        if (os.tecnicoNome.isNotBlank()) {
            sb.appendLine(kv("Técnico Resp.", os.tecnicoNome))
        }
        sb.appendLine(dash)
        sb.appendLine("CLIENTE:")
        sb.appendLine(os.clienteNome)
        if (os.clienteTelefone.isNotBlank()) {
            sb.appendLine(kv("Tel/Whats", os.clienteTelefone))
        }
        sb.appendLine(dash)
        sb.appendLine("EQUIPAMENTO:")
        sb.appendLine("${os.categoriaEquipamento} - ${os.equipamento}")
        if (os.marcaModelo.isNotBlank()) sb.appendLine(kv("Modelo", os.marcaModelo))
        if (os.numeroSerie.isNotBlank()) sb.appendLine(kv("Nº Série", os.numeroSerie))
        if (kind == ReceiptKind.ENTRADA && os.acessoriosEntregues.isNotBlank()) {
            sb.appendLine(kv("Acessórios", os.acessoriosEntregues))
        }
        sb.appendLine(dash)
        if (kind == ReceiptKind.GARANTIA) {
            sb.appendLine("SERVIÇO COBERTO PELA GARANTIA:")
            sb.appendLine(os.diagnosticoTecnico.ifBlank { os.defeitoRelatado })
            if (os.pecasUtilizadas.isNotBlank()) {
                sb.appendLine(kv("Peças", os.pecasUtilizadas))
            }
            sb.appendLine(dash)
            sb.appendLine(kv("Valor Pago", Formatters.formatCurrency(os.valorTotal)))
            sb.appendLine(kv("Prazo", "${os.garantiaDias} dias"))
            sb.appendLine(kv("Válida até", validUntil))
            sb.appendLine(dash)
            sb.appendLine("TERMO DE GARANTIA:")
            sb.appendLine("Cobre apenas os serviços e peças")
            sb.appendLine("acima descritos. Não cobre mau uso,")
            sb.appendLine("umidade, descarga elétrica ou lacre")
            sb.appendLine("rompido.")
        } else {
            sb.appendLine("DEFEITO RELATADO:")
            sb.appendLine(os.defeitoRelatado)

            if (kind == ReceiptKind.SAIDA) {
                if (os.diagnosticoTecnico.isNotBlank()) {
                    sb.appendLine(dash)
                    sb.appendLine("DIAGNÓSTICO / SERVIÇO:")
                    sb.appendLine(os.diagnosticoTecnico)
                }
                if (os.pecasUtilizadas.isNotBlank()) {
                    sb.appendLine(kv("Peças", os.pecasUtilizadas))
                }
                sb.appendLine(dash)
                sb.appendLine(kv("Mão de Obra", Formatters.formatCurrency(os.valorMaoObra)))
                sb.appendLine(kv("Peças", Formatters.formatCurrency(os.valorPecas)))
                if (os.desconto > 0) {
                    sb.appendLine(kv("Desconto", "-${Formatters.formatCurrency(os.desconto)}"))
                }
                sb.appendLine(kv("TOTAL", Formatters.formatCurrency(os.valorTotal)))
                sb.appendLine(kv("Garantia", "${os.garantiaDias} dias (até $validUntil)"))
            } else {
                if (os.valorTotal > 0) {
                    sb.appendLine(dash)
                    sb.appendLine(kv("Orçamento Est.", Formatters.formatCurrency(os.valorTotal)))
                }
                sb.appendLine(dash)
                sb.appendLine(storeConfig.rodapeOs.ifBlank {
                    "OBS: Equipamentos não retirados em 90 dias estarão sujeitos a taxa de estadia/descarte."
                })
            }
        }

        sb.appendLine(dash)
        sb.appendLine()
        sb.appendLine("Ass: _________________________________")
        sb.appendLine(center(os.clienteNome.take(columns)))
        sb.appendLine(sep)
        return sb.toString()
    }

    fun generatePdfFile(
        context: Context,
        os: OrdemServicoDto,
        kind: ReceiptKind,
        format: ReceiptFormat,
        storeConfig: ConfiguracaoOficinaDto = ConfiguracaoOficinaDto()
    ): File {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val suffix = when (kind) {
            ReceiptKind.ENTRADA -> "entrada"
            ReceiptKind.SAIDA -> "saida"
            ReceiptKind.GARANTIA -> "nota_garantia"
        }
        val mode = if (format == ReceiptFormat.PDF_A4) "a4" else "termica"
        val file = File(dir, "OS_${os.numeroOs}_${suffix}_${mode}.pdf")

        val document = PdfDocument()
        if (format == ReceiptFormat.PDF_A4) {
            drawA4Page(document, os, kind, storeConfig)
        } else {
            drawThermalPage(document, os, kind, storeConfig)
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    private fun drawA4Page(
        document: PdfDocument,
        os: OrdemServicoDto,
        kind: ReceiptKind,
        storeConfig: ConfiguracaoOficinaDto
    ) {
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val margin = 40f
        val contentWidth = (pageWidth - margin * 2).toInt()
        var y = 40f

        val headerBg = Paint().apply {
            color = if (kind == ReceiptKind.GARANTIA) Color.parseColor("#064E3B") else Color.parseColor("#0F172A")
        }
        val accentPaint = Paint().apply {
            color = if (kind == ReceiptKind.GARANTIA) Color.parseColor("#059669") else Color.parseColor("#0284C7")
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        val baseDelivery = os.dataSaida ?: Formatters.nowIsoTimestamp()
        val validUntil = Formatters.addDaysToDateBr(baseDelivery, os.garantiaDias)
        val workshopName = storeConfig.nomeLoja.ifBlank { "RTerp Eletrônica & Segurança" }

        // Cabeçalho com dados da loja configurada
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + 88f, 8f, 8f, headerBg)
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 16.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 9.5f
            isAntiAlias = true
        }
        val badgePaint = Paint().apply {
            color = if (kind == ReceiptKind.GARANTIA) Color.parseColor("#34D399") else Color.parseColor("#38BDF8")
            textSize = 15f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val badgeSubPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val infoLine1 = buildList {
            if (storeConfig.cnpj.isNotBlank()) add("CNPJ: ${storeConfig.cnpj}")
            if (storeConfig.telefone.isNotBlank()) add("Tel: ${storeConfig.telefone}")
            if (storeConfig.email.isNotBlank()) add(storeConfig.email)
        }.joinToString("  •  ").ifBlank { "Manutenção Eletrônica • CFTV • Alarmes • Automatizadores" }

        val infoLine2 = storeConfig.endereco.ifBlank { "Assistência Técnica Especializada" }

        canvas.drawText(workshopName, margin + 16f, y + 26f, titlePaint)
        canvas.drawText(infoLine1.take(66), margin + 16f, y + 46f, subPaint)
        canvas.drawText(infoLine2.take(66), margin + 16f, y + 64f, subPaint)

        canvas.drawText("OS #${os.numeroOs}", pageWidth - margin - 16f, y + 32f, badgePaint)
        canvas.drawText(kind.label.uppercase(), pageWidth - margin - 16f, y + 52f, badgeSubPaint)

        y += 108f

        val sectionPaint = Paint().apply {
            color = if (kind == ReceiptKind.GARANTIA) Color.parseColor("#059669") else Color.parseColor("#0052CC")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = TextPaint().apply {
            color = Color.parseColor("#334155")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val boldPaint = TextPaint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        fun drawWrapped(text: String, paint: TextPaint, width: Int = contentWidth): Float {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1.15f)
                .build()
            canvas.save()
            canvas.translate(margin, y)
            layout.draw(canvas)
            canvas.restore()
            return layout.height.toFloat()
        }

        // Dados Gerais
        canvas.drawText("DADOS DO ATENDIMENTO & CLIENTE", margin, y, sectionPaint)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, accentPaint)
        y += 14f

        y += drawWrapped("Cliente: ${os.clienteNome}   |   Telefone: ${os.clienteTelefone.ifBlank { "—" }}", boldPaint) + 4f
        if (os.tecnicoNome.isNotBlank()) {
            y += drawWrapped("Técnico Responsável: ${os.tecnicoNome}", boldPaint) + 4f
        }
        val dateSummary = when (kind) {
            ReceiptKind.ENTRADA -> "Data de Entrada: ${Formatters.formatDateTimeBr(os.dataEntrada)}   |   Status Atual: ${os.statusEnum.label}"
            ReceiptKind.SAIDA -> "Entrada: ${Formatters.formatDateTimeBr(os.dataEntrada)}   |   Saída: ${Formatters.formatDateTimeBr(baseDelivery)}"
            ReceiptKind.GARANTIA -> "Entrega: ${Formatters.formatDateBr(baseDelivery)}   |   Garantia Válida Até: $validUntil (${os.garantiaDias} dias)"
        }
        y += drawWrapped(dateSummary, labelPaint) + 14f

        // Equipamento
        canvas.drawText("EQUIPAMENTO", margin, y, sectionPaint)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 14f

        y += drawWrapped("Categoria: ${os.categoriaEquipamento}   |   Equipamento: ${os.equipamento}", boldPaint) + 4f
        y += drawWrapped(
            "Marca/Modelo: ${os.marcaModelo.ifBlank { "—" }}   |   Nº de Série: ${os.numeroSerie.ifBlank { "—" }}",
            labelPaint
        ) + 4f
        if (os.acessoriosEntregues.isNotBlank()) {
            y += drawWrapped("Acessórios Entregues: ${os.acessoriosEntregues}", labelPaint) + 4f
        }
        y += 10f

        // Relato / Serviço coberto
        canvas.drawText(
            if (kind == ReceiptKind.GARANTIA) "SERVIÇO & COMPONENTES COBERTOS PELA GARANTIA" else "RELATO TÉCNICO",
            margin,
            y,
            sectionPaint
        )
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 14f

        if (kind != ReceiptKind.GARANTIA) {
            y += drawWrapped("Defeito Relatado: ${os.defeitoRelatado}", labelPaint) + 8f
        }
        if (kind == ReceiptKind.SAIDA || kind == ReceiptKind.GARANTIA || os.diagnosticoTecnico.isNotBlank()) {
            y += drawWrapped(
                "Diagnóstico / Serviço Executado: ${os.diagnosticoTecnico.ifBlank { os.defeitoRelatado }}",
                labelPaint
            ) + 6f
        }
        if (os.pecasUtilizadas.isNotBlank()) {
            y += drawWrapped("Peças / Componentes Substituídos: ${os.pecasUtilizadas}", labelPaint) + 6f
        }
        y += 12f

        // Valores e Garantia
        canvas.drawText("RESUMO FINANCEIRO & GARANTIA", margin, y, sectionPaint)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 14f

        y += drawWrapped(
            "Mão de Obra: ${Formatters.formatCurrency(os.valorMaoObra)}   |   Peças: ${Formatters.formatCurrency(os.valorPecas)}   |   Desconto: ${Formatters.formatCurrency(os.desconto)}",
            labelPaint
        ) + 6f
        val totalBoxPaint = Paint().apply {
            color = if (kind == ReceiptKind.GARANTIA) Color.parseColor("#D1FAE5") else Color.parseColor("#E0F2FE")
        }
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + 40f, 6f, 6f, totalBoxPaint)
        val totalTextPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 13.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(
            "TOTAL: ${Formatters.formatCurrency(os.valorTotal)}",
            margin + 14f,
            y + 25f,
            totalTextPaint
        )
        canvas.drawText(
            "GARANTIA: ${os.garantiaDias} DIAS (ATÉ $validUntil)",
            pageWidth - margin - 260f,
            y + 25f,
            totalTextPaint.apply { textSize = 10.5f }
        )
        y += 56f

        // Termos
        val terms = when (kind) {
            ReceiptKind.ENTRADA ->
                storeConfig.rodapeOs.ifBlank {
                    "Termos de Entrada: O orçamento será comunicado após análise técnica. Equipamentos não retirados em até 90 dias após a notificação estarão sujeitos à cobrança de estadia ou descarte."
                }
            ReceiptKind.SAIDA, ReceiptKind.GARANTIA ->
                "Certificado de Garantia: A garantia de ${os.garantiaDias} dias (válida até $validUntil) cobre exclusivamente os serviços e peças discriminados nesta Ordem de Serviço, não cobrindo mau uso, quedas, descargas elétricas, umidade ou violação do lacre técnico."
        }
        y += drawWrapped(terms, labelPaint.apply { textSize = 9.5f }) + 44f

        // Assinaturas
        val sigY = maxOf(y + 24f, 740f)
        canvas.drawLine(margin, sigY, margin + 220f, sigY, linePaint)
        canvas.drawLine(pageWidth - margin - 220f, sigY, pageWidth - margin, sigY, linePaint)
        val sigPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 9.5f
            isAntiAlias = true
        }
        canvas.drawText("Assinatura do Cliente (${os.clienteNome.take(26)})", margin, sigY + 14f, sigPaint)
        val techSigLabel = if (os.tecnicoNome.isNotBlank()) {
            "Técnico Resp.: ${os.tecnicoNome.take(26)}"
        } else {
            "Responsável Técnico - ${workshopName.take(24)}"
        }
        canvas.drawText(techSigLabel, pageWidth - margin - 220f, sigY + 14f, sigPaint)

        document.finishPage(page)
    }

    private fun drawThermalPage(
        document: PdfDocument,
        os: OrdemServicoDto,
        kind: ReceiptKind,
        storeConfig: ConfiguracaoOficinaDto
    ) {
        val receiptText = buildThermalText(os, kind, storeConfig, columns = 38)
        val pageWidth = 226
        val margin = 10f
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 8.2f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        val layout = StaticLayout.Builder.obtain(
            receiptText,
            0,
            receiptText.length,
            textPaint,
            (pageWidth - margin * 2).toInt()
        ).setLineSpacing(1.5f, 1.05f).build()

        val pageHeight = (layout.height + 28).coerceAtLeast(320)
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawColor(Color.WHITE)
        page.canvas.save()
        page.canvas.translate(margin, 14f)
        layout.draw(page.canvas)
        page.canvas.restore()
        document.finishPage(page)
    }

    fun printPdfFile(context: Context, file: File, jobName: String, isThermal: Boolean) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }

        val attributes = if (isThermal) {
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize("THERMAL_80MM", "Bobina 80mm", 3150, 8000))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        } else {
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()
        }

        printManager.print(jobName, adapter, attributes)
    }

    fun sharePdfFile(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar Comprovante PDF"))
    }

    fun shareThermalText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Enviar Cupom para Impressão / WhatsApp"))
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    // =========================================================================
    // NOTA DE VENDA / CUPOM DE VENDA PDV (A4 + Térmica 80mm / 58mm)
    // =========================================================================

    fun buildSaleThermalText(
        venda: VendaDto,
        storeConfig: ConfiguracaoOficinaDto = ConfiguracaoOficinaDto(),
        columns: Int = 38
    ): String {
        val sep = "=".repeat(columns)
        val dash = "-".repeat(columns)
        val sb = StringBuilder()

        fun center(text: String): String {
            val clean = text.take(columns)
            val pad = ((columns - clean.length) / 2).coerceAtLeast(0)
            return " ".repeat(pad) + clean
        }

        val storeTitle = storeConfig.nomeLoja.ifBlank { "RTERP ELETRÔNICA & SEGURANÇA" }.uppercase()
        sb.appendLine(sep)
        sb.appendLine(center(storeTitle))
        if (storeConfig.cnpj.isNotBlank()) sb.appendLine(center("CNPJ: ${storeConfig.cnpj}"))
        if (storeConfig.telefone.isNotBlank()) sb.appendLine(center("Tel: ${storeConfig.telefone}"))
        if (storeConfig.endereco.isNotBlank()) sb.appendLine(storeConfig.endereco)
        sb.appendLine(sep)
        sb.appendLine(center("CUPOM / NOTA DE VENDA"))
        sb.appendLine(center("VENDA Nº #${venda.numeroVenda}"))
        sb.appendLine(dash)
        sb.appendLine("Data: ${Formatters.formatDateTimeBr(venda.dataVenda ?: venda.createdAt)}")
        sb.appendLine("Cliente: ${venda.clienteNome}")
        if (venda.clienteTelefone.isNotBlank()) {
            sb.appendLine("Tel/Whats: ${venda.clienteTelefone}")
        }
        if (venda.vendedorNome.isNotBlank()) {
            sb.appendLine("Vendedor: ${venda.vendedorNome}")
        }
        sb.appendLine("Pagamento: ${venda.formaPagamento}")
        sb.appendLine("Status: ${venda.statusEnum.label}")
        sb.appendLine(dash)
        sb.appendLine("ITENS DA VENDA:")
        venda.itens.forEachIndexed { idx, item ->
            val skuNote = if (item.codigoSku.isNotBlank()) " [${item.codigoSku}]" else ""
            sb.appendLine("${idx + 1}. ${item.nome}$skuNote")
            val lineTotal = if (item.subtotal > 0.0) item.subtotal else item.quantidade * item.precoUnitario
            sb.appendLine(
                "   ${item.quantidade}x ${Formatters.formatCurrency(item.precoUnitario)} = ${Formatters.formatCurrency(lineTotal)}"
            )
        }
        sb.appendLine(dash)
        sb.appendLine("Subtotal: ${Formatters.formatCurrency(venda.subtotal)}")
        if (venda.desconto > 0.0) {
            sb.appendLine("Desconto: -${Formatters.formatCurrency(venda.desconto)}")
        }
        sb.appendLine("TOTAL: ${Formatters.formatCurrency(venda.valorTotal)}")
        if (venda.observacoes.isNotBlank()) {
            sb.appendLine(dash)
            sb.appendLine("Obs: ${venda.observacoes}")
        }
        sb.appendLine(dash)
        sb.appendLine(center("Obrigado pela preferência!"))
        sb.appendLine(center("Documento sem valor fiscal"))
        sb.appendLine(sep)
        return sb.toString()
    }

    fun generateSalePdfFile(
        context: Context,
        venda: VendaDto,
        format: ReceiptFormat,
        storeConfig: ConfiguracaoOficinaDto = ConfiguracaoOficinaDto()
    ): File {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val mode = if (format == ReceiptFormat.PDF_A4) "a4" else "termica"
        val file = File(dir, "Venda_${venda.numeroVenda}_${mode}.pdf")

        val document = PdfDocument()
        if (format == ReceiptFormat.PDF_A4) {
            drawSaleA4Page(document, venda, storeConfig)
        } else {
            drawSaleThermalPage(document, venda, storeConfig)
        }

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    private fun drawSaleA4Page(
        document: PdfDocument,
        venda: VendaDto,
        storeConfig: ConfiguracaoOficinaDto
    ) {
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val margin = 40f
        val contentWidth = (pageWidth - margin * 2).toInt()
        var y = 40f

        val isCanceled = venda.statusEnum == StatusVenda.CANCELADA
        val headerBg = Paint().apply {
            color = if (isCanceled) Color.parseColor("#7F1D1D") else Color.parseColor("#0F172A")
        }
        val accentPaint = Paint().apply {
            color = if (isCanceled) Color.parseColor("#DC2626") else Color.parseColor("#0284C7")
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }
        val workshopName = storeConfig.nomeLoja.ifBlank { "RTerp Eletrônica & Segurança" }

        canvas.drawRoundRect(margin, y, pageWidth - margin, y + 88f, 8f, 8f, headerBg)
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 16.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 9.5f
            isAntiAlias = true
        }
        val badgePaint = Paint().apply {
            color = if (isCanceled) Color.parseColor("#FCA5A5") else Color.parseColor("#38BDF8")
            textSize = 15f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val badgeSubPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val infoLine1 = buildList {
            if (storeConfig.cnpj.isNotBlank()) add("CNPJ: ${storeConfig.cnpj}")
            if (storeConfig.telefone.isNotBlank()) add("Tel: ${storeConfig.telefone}")
            if (storeConfig.email.isNotBlank()) add(storeConfig.email)
        }.joinToString("  •  ").ifBlank { "Venda de Peças, Componentes & Segurança" }

        val infoLine2 = storeConfig.endereco.ifBlank { "Comprovante de Venda Balcão / PDV" }

        canvas.drawText(workshopName, margin + 16f, y + 26f, titlePaint)
        canvas.drawText(infoLine1.take(66), margin + 16f, y + 46f, subPaint)
        canvas.drawText(infoLine2.take(66), margin + 16f, y + 64f, subPaint)

        canvas.drawText("VENDA #${venda.numeroVenda}", pageWidth - margin - 16f, y + 32f, badgePaint)
        canvas.drawText("NOTA DE VENDA (${venda.statusEnum.label.uppercase()})", pageWidth - margin - 16f, y + 52f, badgeSubPaint)

        y += 108f

        val sectionPaint = Paint().apply {
            color = Color.parseColor("#0052CC")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = TextPaint().apply {
            color = Color.parseColor("#334155")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val boldPaint = TextPaint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        fun drawWrapped(text: String, paint: TextPaint, width: Int = contentWidth): Float {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1.15f)
                .build()
            canvas.save()
            canvas.translate(margin, y)
            layout.draw(canvas)
            canvas.restore()
            return layout.height.toFloat()
        }

        canvas.drawText("DADOS DA VENDA & CLIENTE", margin, y, sectionPaint)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, accentPaint)
        y += 14f

        y += drawWrapped("Cliente: ${venda.clienteNome}   |   Telefone: ${venda.clienteTelefone.ifBlank { "—" }}", boldPaint) + 4f
        if (venda.vendedorNome.isNotBlank()) {
            y += drawWrapped("Vendedor Responsável: ${venda.vendedorNome}", boldPaint) + 4f
        }
        y += drawWrapped(
            "Data: ${Formatters.formatDateTimeBr(venda.dataVenda ?: venda.createdAt)}   |   Pagamento: ${venda.formaPagamento}   |   Status: ${venda.statusEnum.label}",
            labelPaint
        ) + 14f

        canvas.drawText("ITENS VENDIDOS", margin, y, sectionPaint)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 14f

        venda.itens.forEachIndexed { idx, item ->
            val sku = if (item.codigoSku.isNotBlank()) " [${item.codigoSku}]" else ""
            val itemTotal = if (item.subtotal > 0.0) item.subtotal else item.quantidade * item.precoUnitario
            val line = "${idx + 1}. ${item.nome}$sku — ${item.quantidade} un × ${Formatters.formatCurrency(item.precoUnitario)} = ${Formatters.formatCurrency(itemTotal)}"
            y += drawWrapped(line, labelPaint) + 4f
            if (y > 670f) return@forEachIndexed
        }

        y += 10f
        canvas.drawText("RESUMO DE PAGAMENTO", margin, y, sectionPaint)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 14f

        y += drawWrapped(
            "Subtotal: ${Formatters.formatCurrency(venda.subtotal)}   |   Desconto: ${Formatters.formatCurrency(venda.desconto)}   |   Forma: ${venda.formaPagamento}",
            labelPaint
        ) + 8f

        val totalBoxPaint = Paint().apply {
            color = if (isCanceled) Color.parseColor("#FEE2E2") else Color.parseColor("#E0F2FE")
        }
        canvas.drawRoundRect(margin, y, pageWidth - margin, y + 40f, 6f, 6f, totalBoxPaint)
        val totalTextPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 13.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(
            "TOTAL DA VENDA: ${Formatters.formatCurrency(venda.valorTotal)}",
            margin + 14f,
            y + 25f,
            totalTextPaint
        )
        y += 52f

        if (venda.observacoes.isNotBlank()) {
            y += drawWrapped("Observações: ${venda.observacoes}", labelPaint) + 10f
        }

        val noteFooter = "Documento emitido pelo sistema RTerp PDV. Comprovante de venda não fiscal. Trocas ou garantias mediante apresentação desta nota."
        y += drawWrapped(noteFooter, labelPaint.apply { textSize = 9.5f }) + 32f

        val sigY = maxOf(y + 24f, 740f)
        canvas.drawLine(margin, sigY, margin + 220f, sigY, linePaint)
        canvas.drawLine(pageWidth - margin - 220f, sigY, pageWidth - margin, sigY, linePaint)
        val sigPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 9.5f
            isAntiAlias = true
        }
        canvas.drawText("Cliente: ${venda.clienteNome.take(28)}", margin, sigY + 14f, sigPaint)
        canvas.drawText(
            if (venda.vendedorNome.isNotBlank()) "Vendedor: ${venda.vendedorNome.take(26)}" else workshopName.take(28),
            pageWidth - margin - 220f,
            sigY + 14f,
            sigPaint
        )

        document.finishPage(page)
    }

    private fun drawSaleThermalPage(
        document: PdfDocument,
        venda: VendaDto,
        storeConfig: ConfiguracaoOficinaDto
    ) {
        val receiptText = buildSaleThermalText(venda, storeConfig, columns = 38)
        val pageWidth = 226
        val margin = 10f
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 8.2f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        val layout = StaticLayout.Builder.obtain(
            receiptText,
            0,
            receiptText.length,
            textPaint,
            (pageWidth - margin * 2).toInt()
        ).setLineSpacing(1.5f, 1.05f).build()

        val pageHeight = (layout.height + 28).coerceAtLeast(320)
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawColor(Color.WHITE)
        page.canvas.save()
        page.canvas.translate(margin, 14f)
        layout.draw(page.canvas)
        page.canvas.restore()
        document.finishPage(page)
    }
}

package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.ConfiguracaoOficinaDto
import com.example.data.model.OrdemServicoDto
import com.example.data.model.VendaDto
import java.io.File
import java.net.URLEncoder

object WhatsAppHelper {

    fun buildOsMessage(
        os: OrdemServicoDto,
        storeConfig: ConfiguracaoOficinaDto = ConfiguracaoOficinaDto()
    ): String {
        val workshopName = storeConfig.nomeLoja.ifBlank { "RTerp Eletrônica & Segurança" }
        val sb = StringBuilder()
        sb.appendLine("*$workshopName*")
        if (storeConfig.cnpj.isNotBlank()) sb.appendLine("CNPJ: ${storeConfig.cnpj}")
        if (storeConfig.telefone.isNotBlank()) sb.appendLine("📞 ${storeConfig.telefone}")
        if (storeConfig.endereco.isNotBlank()) sb.appendLine("📍 ${storeConfig.endereco}")
        sb.appendLine("📋 *ORDEM DE SERVIÇO Nº #${os.numeroOs}*")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("👤 *Cliente:* ${os.clienteNome}")
        if (os.tecnicoNome.isNotBlank()) sb.appendLine("🛠️ *Técnico Responsável:* ${os.tecnicoNome}")
        sb.appendLine("🔧 *Equipamento:* ${os.categoriaEquipamento} - ${os.equipamento}")
        if (os.marcaModelo.isNotBlank()) sb.appendLine("🏷️ *Modelo:* ${os.marcaModelo}")
        if (os.numeroSerie.isNotBlank()) sb.appendLine("🔢 *Nº Série:* ${os.numeroSerie}")
        if (os.acessoriosEntregues.isNotBlank()) sb.appendLine("🎒 *Acessórios:* ${os.acessoriosEntregues}")
        sb.appendLine("📌 *Status:* ${os.statusEnum.label}")
        sb.appendLine("📅 *Entrada:* ${Formatters.formatDateTimeBr(os.dataEntrada)}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("⚠️ *Defeito Relatado:* ${os.defeitoRelatado}")
        if (os.diagnosticoTecnico.isNotBlank()) {
            sb.appendLine("🛠️ *Diagnóstico / Serviço:* ${os.diagnosticoTecnico}")
        }
        if (os.pecasUtilizadas.isNotBlank()) {
            sb.appendLine("🔩 *Peças:* ${os.pecasUtilizadas}")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        if (os.valorMaoObra > 0) sb.appendLine("Mão de Obra: ${Formatters.formatCurrency(os.valorMaoObra)}")
        if (os.valorPecas > 0) sb.appendLine("Peças: ${Formatters.formatCurrency(os.valorPecas)}")
        if (os.desconto > 0) sb.appendLine("Desconto: -${Formatters.formatCurrency(os.desconto)}")
        sb.appendLine("💰 *VALOR TOTAL:* ${Formatters.formatCurrency(os.valorTotal)}")
        sb.appendLine("🛡️ *Garantia:* ${os.garantiaDias} dias")
        if (os.fotosUrls.isNotEmpty()) {
            sb.appendLine("📷 *Fotos anexadas:* ${os.fotosUrls.size}")
        }
        return sb.toString().trim()
    }

    fun buildWarrantyMessage(
        os: OrdemServicoDto,
        storeConfig: ConfiguracaoOficinaDto = ConfiguracaoOficinaDto()
    ): String {
        val workshopName = storeConfig.nomeLoja.ifBlank { "RTerp Eletrônica & Segurança" }
        val baseDate = os.dataSaida ?: Formatters.nowIsoTimestamp()
        val validUntil = Formatters.addDaysToDateBr(baseDate, os.garantiaDias)
        val sb = StringBuilder()
        sb.appendLine("*$workshopName*")
        if (storeConfig.cnpj.isNotBlank()) sb.appendLine("CNPJ: ${storeConfig.cnpj}")
        if (storeConfig.telefone.isNotBlank()) sb.appendLine("📞 ${storeConfig.telefone}")
        if (storeConfig.endereco.isNotBlank()) sb.appendLine("📍 ${storeConfig.endereco}")
        sb.appendLine("🛡️ *NOTA / CERTIFICADO DE GARANTIA*")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📄 *OS Referência:* #${os.numeroOs}")
        sb.appendLine("👤 *Cliente:* ${os.clienteNome}")
        if (os.tecnicoNome.isNotBlank()) sb.appendLine("🛠️ *Técnico Responsável:* ${os.tecnicoNome}")
        sb.appendLine("🔧 *Equipamento:* ${os.equipamento} ${os.marcaModelo}".trim())
        if (os.numeroSerie.isNotBlank()) sb.appendLine("🔢 *Nº Série:* ${os.numeroSerie}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🛠️ *Serviço Executado:*")
        sb.appendLine(os.diagnosticoTecnico.ifBlank { os.defeitoRelatado })
        if (os.pecasUtilizadas.isNotBlank()) {
            sb.appendLine("🔩 *Peças Substituídas:* ${os.pecasUtilizadas}")
        }
        sb.appendLine("💰 *Valor Pago:* ${Formatters.formatCurrency(os.valorTotal)}")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("📅 *Data de Entrega:* ${Formatters.formatDateBr(baseDate)}")
        sb.appendLine("✅ *Prazo de Garantia:* ${os.garantiaDias} dias")
        sb.appendLine("📆 *Válida até:* *$validUntil*")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("_Esta garantia cobre exclusivamente os serviços e componentes substituídos descritos nesta OS. Não cobre danos por mau uso, quedas, umidade, descarga elétrica ou violação do lacre._")
        return sb.toString().trim()
    }

    fun buildSaleMessage(
        venda: VendaDto,
        storeConfig: ConfiguracaoOficinaDto = ConfiguracaoOficinaDto()
    ): String {
        val workshopName = storeConfig.nomeLoja.ifBlank { "RTerp Eletrônica & Segurança" }
        val sb = StringBuilder()
        sb.appendLine("*$workshopName*")
        if (storeConfig.cnpj.isNotBlank()) sb.appendLine("CNPJ: ${storeConfig.cnpj}")
        if (storeConfig.telefone.isNotBlank()) sb.appendLine("📞 ${storeConfig.telefone}")
        sb.appendLine("🧾 *NOTA / CUPOM DE VENDA Nº #${venda.numeroVenda}*")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("👤 *Cliente:* ${venda.clienteNome}")
        if (venda.vendedorNome.isNotBlank()) sb.appendLine("🧑‍💼 *Vendedor:* ${venda.vendedorNome}")
        sb.appendLine("📅 *Data:* ${Formatters.formatDateTimeBr(venda.dataVenda ?: venda.createdAt)}")
        sb.appendLine("💳 *Pagamento:* ${venda.formaPagamento} (${venda.statusEnum.label})")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("🛒 *Itens:*")
        venda.itens.forEachIndexed { idx, item ->
            val sub = if (item.subtotal > 0.0) item.subtotal else item.quantidade * item.precoUnitario
            sb.appendLine("${idx + 1}. ${item.nome} — ${item.quantidade}x ${Formatters.formatCurrency(item.precoUnitario)} = ${Formatters.formatCurrency(sub)}")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        if (venda.desconto > 0.0) {
            sb.appendLine("Subtotal: ${Formatters.formatCurrency(venda.subtotal)}")
            sb.appendLine("Desconto: -${Formatters.formatCurrency(venda.desconto)}")
        }
        sb.appendLine("💰 *TOTAL DA VENDA:* ${Formatters.formatCurrency(venda.valorTotal)}")
        if (venda.observacoes.isNotBlank()) {
            sb.appendLine("📝 *Obs:* ${venda.observacoes}")
        }
        return sb.toString().trim()
    }

    fun sendTextViaWhatsApp(context: Context, phoneRaw: String, message: String) {
        val cleanPhone = normalizeBrazilPhone(phoneRaw)
        if (cleanPhone != null) {
            val encoded = URLEncoder.encode(message, "UTF-8")
            val waUri = Uri.parse("https://wa.me/$cleanPhone?text=$encoded")
            val viewIntent = Intent(Intent.ACTION_VIEW, waUri)
            val whatsappPkg = findInstalledWhatsAppPackage(context)
            if (whatsappPkg != null) {
                viewIntent.setPackage(whatsappPkg)
            }
            try {
                context.startActivity(viewIntent)
                return
            } catch (_: Exception) {
            }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val whatsappPkg = findInstalledWhatsAppPackage(context)
        if (whatsappPkg != null) {
            sendIntent.setPackage(whatsappPkg)
        }
        try {
            context.startActivity(
                if (whatsappPkg != null) sendIntent
                else Intent.createChooser(sendIntent, "Enviar via WhatsApp")
            )
        } catch (_: Exception) {
            Toast.makeText(context, "Nenhum aplicativo compatível encontrado.", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendPdfViaWhatsApp(
        context: Context,
        pdfFile: File,
        caption: String,
        title: String
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val whatsappPkg = findInstalledWhatsAppPackage(context)
        if (whatsappPkg != null) {
            sendIntent.setPackage(whatsappPkg)
            try {
                context.startActivity(sendIntent)
                return
            } catch (_: Exception) {
                sendIntent.setPackage(null)
            }
        }
        context.startActivity(Intent.createChooser(sendIntent, "Enviar PDF via WhatsApp"))
    }

    private fun findInstalledWhatsAppPackage(context: Context): String? {
        val pm = context.packageManager
        val candidates = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in candidates) {
            try {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                return pkg
            } catch (_: Throwable) {
                try {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                    return pkg
                } catch (_: Exception) {
                }
            }
        }
        return null
    }

    private fun normalizeBrazilPhone(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        if (digits.length < 10) return null
        return if (digits.startsWith("55") && digits.length >= 12) digits else "55$digits"
    }
}

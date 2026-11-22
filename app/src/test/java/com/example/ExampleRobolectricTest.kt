package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FuncionarioDto
import com.example.data.model.OrdemServicoDto
import com.example.data.model.PecaEstoqueDto
import com.example.data.model.PedidoPecaDto
import com.example.data.model.PedidoPecaMotivo
import com.example.data.model.PedidoPecaStatus
import com.example.data.model.StatusFinanceiro
import com.example.data.model.StatusOS
import com.example.data.model.TipoTransacao
import com.example.data.model.TransacaoFinanceiraDto
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.ui.viewmodel.WorkshopUiState
import com.example.util.AlertCalculator
import com.example.util.CommissionCalculator
import com.example.util.ImageCompressionPreset
import com.example.util.ImageCompressor
import com.example.util.ReceiptGenerator
import com.example.util.ReceiptKind
import com.example.util.WhatsAppHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RTerp", appName)
  }

  @Test
  fun `employee session blocks financial access while admin allows it`() {
    val admin = UserSession(
      accessToken = "tok",
      refreshToken = "ref",
      userId = "owner-1",
      email = "admin@rterp.com",
      fullName = "Admin",
      expiresAtEpochSec = 9999999999L,
      role = UserRole.ADMIN,
      workshopOwnerId = "owner-1"
    )
    val funcionario = UserSession(
      accessToken = "tok2",
      refreshToken = "ref2",
      userId = "func-1",
      email = "tec@rterp.com",
      fullName = "Tecnico",
      expiresAtEpochSec = 9999999999L,
      role = UserRole.FUNCIONARIO,
      workshopOwnerId = "owner-1"
    )

    assertTrue(admin.isAdmin)
    assertTrue(admin.canAccessFinanceiro)
    assertFalse(funcionario.isAdmin)
    assertTrue(funcionario.isFuncionario)
    assertFalse(funcionario.canAccessFinanceiro)
    assertEquals("owner-1", funcionario.workshopOwnerId)
  }

  @Test
  fun `commission calculator computes technician 60 percent and attendant 80 percent on labor`() {
    val fulano = FuncionarioDto(
      id = "f-1",
      ownerId = "owner-1",
      nome = "Fulano",
      email = "fulano@rterp.com",
      cargo = "Técnico",
      comissaoPercentual = 60.0
    )
    val beltrano = FuncionarioDto(
      id = "f-2",
      ownerId = "owner-1",
      nome = "Beltrano",
      email = "beltrano@rterp.com",
      cargo = "Atendente",
      comissaoPercentual = 80.0
    )
    val funcionarios = listOf(fulano, beltrano)

    val osFulanoFinalizada = OrdemServicoDto(
      id = "os-1",
      numeroOs = 1001,
      clienteNome = "Cliente A",
      tecnicoId = "f-1",
      tecnicoNome = "Fulano",
      tecnicoEmail = "fulano@rterp.com",
      criadoPorNome = "Fulano",
      comissaoPercentual = 60.0,
      equipamento = "DVR 8CH",
      defeitoRelatado = "Sem vídeo",
      status = StatusOS.ENTREGUE.code,
      valorMaoObra = 100.0,
      valorPecas = 50.0,
      valorTotal = 150.0
    )
    val osFulanoAberta = OrdemServicoDto(
      id = "os-2",
      numeroOs = 1002,
      clienteNome = "Cliente B",
      tecnicoId = "f-1",
      tecnicoNome = "Fulano",
      tecnicoEmail = "fulano@rterp.com",
      criadoPorNome = "Fulano",
      comissaoPercentual = 0.0, // fallback to FuncionarioDto.comissaoPercentual (60%)
      equipamento = "Nobreak",
      defeitoRelatado = "Bateria",
      status = StatusOS.EM_ANALISE.code,
      valorMaoObra = 100.0,
      valorPecas = 0.0,
      valorTotal = 100.0
    )
    val osBeltranoFinalizada = OrdemServicoDto(
      id = "os-3",
      numeroOs = 1003,
      clienteNome = "Cliente C",
      tecnicoId = "f-2",
      tecnicoNome = "Beltrano",
      tecnicoEmail = "beltrano@rterp.com",
      criadoPorNome = "Beltrano",
      comissaoPercentual = 80.0,
      equipamento = "Central Alarme",
      defeitoRelatado = "Configuração",
      status = StatusOS.ENTREGUE.code,
      valorMaoObra = 100.0,
      valorPecas = 0.0,
      valorTotal = 100.0
    )

    val ordens = listOf(osFulanoFinalizada, osFulanoAberta, osBeltranoFinalizada)
    val teamSummaries = CommissionCalculator.buildTeamPerformance(ordens, funcionarios)

    val fulanoSummary = teamSummaries.first { it.nome == "Fulano" }
    assertEquals(60.0, fulanoSummary.comissaoPadraoPercent, 0.01)
    assertEquals(2, fulanoSummary.osGeradasCount)
    assertEquals(1, fulanoSummary.osFinalizadasCount)
    assertEquals(150.0, fulanoSummary.totalGeradoLojaFinalizado, 0.01)
    assertEquals(60.0, fulanoSummary.comissaoConfirmada, 0.01)
    assertEquals(60.0, fulanoSummary.comissaoPrevista, 0.01)
    assertEquals(90.0, fulanoSummary.liquidoLojaConfirmado, 0.01)

    val beltranoSummary = teamSummaries.first { it.nome == "Beltrano" }
    assertEquals(80.0, beltranoSummary.comissaoPadraoPercent, 0.01)
    assertEquals(1, beltranoSummary.osGeradasCount)
    assertEquals(1, beltranoSummary.osFinalizadasCount)
    assertEquals(100.0, beltranoSummary.totalGeradoLojaFinalizado, 0.01)
    assertEquals(80.0, beltranoSummary.comissaoConfirmada, 0.01)
    assertEquals(20.0, beltranoSummary.liquidoLojaConfirmado, 0.01)

    // Employee view isolation: Fulano only sees Fulano's orders
    val fulanoOnly = CommissionCalculator.filterOrdensForEmployee(
      ordens = ordens,
      funcionario = fulano,
      userName = "Fulano",
      userEmail = "fulano@rterp.com"
    )
    assertEquals(2, fulanoOnly.size)
    assertTrue(fulanoOnly.none { it.tecnicoNome == "Beltrano" })
  }

  @Test
  fun `alert calculator detects overdue receivables 30d, fixed bills, open OS 30d and enforces role isolation`() {
    val refDate = "2026-05-15"

    val txReceber35d = TransacaoFinanceiraDto(
      id = "tx-1",
      descricao = "OS #1001 Cliente X",
      tipo = TipoTransacao.RECEBER.code,
      status = StatusFinanceiro.PENDENTE.code,
      valor = 350.0,
      dataVencimento = "2026-04-10" // 35 days overdue
    )
    val txReceber20d = TransacaoFinanceiraDto(
      id = "tx-2",
      descricao = "OS #1002 Cliente Y",
      tipo = TipoTransacao.RECEBER.code,
      status = StatusFinanceiro.PENDENTE.code,
      valor = 200.0,
      dataVencimento = "2026-04-25" // 20 days overdue (must NOT alert >30d)
    )
    val txReceberPaid40d = TransacaoFinanceiraDto(
      id = "tx-3",
      descricao = "OS #999 Paga",
      tipo = TipoTransacao.RECEBER.code,
      status = StatusFinanceiro.PAGO.code,
      valor = 180.0,
      dataVencimento = "2026-04-05" // paid -> must NOT alert
    )
    val txFixaAtrasada = TransacaoFinanceiraDto(
      id = "tx-4",
      descricao = "Aluguel Loja",
      categoria = "Aluguel / Energia / Internet",
      tipo = TipoTransacao.PAGAR.code,
      status = StatusFinanceiro.PENDENTE.code,
      isContaFixa = true,
      valor = 1200.0,
      dataVencimento = "2026-05-13" // 2 days overdue
    )
    val txFixaPaga = TransacaoFinanceiraDto(
      id = "tx-5",
      descricao = "Internet Fibra",
      categoria = "Aluguel / Energia / Internet",
      tipo = TipoTransacao.PAGAR.code,
      status = StatusFinanceiro.PAGO.code,
      isContaFixa = true,
      valor = 120.0,
      dataVencimento = "2026-05-01" // paid -> must NOT alert
    )

    val osAberta32d = OrdemServicoDto(
      id = "os-10",
      numeroOs = 1010,
      clienteNome = "Carlos",
      equipamento = "DVR 16CH",
      defeitoRelatado = "Sem rede",
      status = StatusOS.EM_ANALISE.code,
      dataEntrada = "2026-04-13T10:00:00Z" // 32 days open
    )
    val osEntregue40d = OrdemServicoDto(
      id = "os-11",
      numeroOs = 1011,
      clienteNome = "Ana",
      equipamento = "Nobreak",
      defeitoRelatado = "Troca bateria",
      status = StatusOS.ENTREGUE.code,
      dataEntrada = "2026-04-05T10:00:00Z" // delivered -> must NOT alert
    )

    val pedidoPendente = PedidoPecaDto(
      id = "ped-1",
      nomePeca = "Capacitor 470uF",
      motivo = PedidoPecaMotivo.FALTANTE.code,
      status = PedidoPecaStatus.PENDENTE.code
    )
    val pecaBaixa = PecaEstoqueDto(
      id = "p-1",
      nome = "Bateria 12V 7Ah",
      quantidade = 1,
      estoqueMinimo = 3
    )

    val adminAlerts = AlertCalculator.compute(
      ordens = listOf(osAberta32d, osEntregue40d),
      transacoes = listOf(txReceber35d, txReceber20d, txReceberPaid40d, txFixaAtrasada, txFixaPaga),
      pedidosPecas = listOf(pedidoPendente),
      pecas = listOf(pecaBaixa),
      canAccessFinanceiro = true,
      referenceDateIso = refDate
    )

    assertEquals(1, adminAlerts.contasReceberMais30Dias.size)
    assertEquals("tx-1", adminAlerts.contasReceberMais30Dias.first().transacao.id)
    assertEquals(35L, adminAlerts.contasReceberMais30Dias.first().diasEmAtraso)

    assertEquals(1, adminAlerts.contasFixasEmAtraso.size)
    assertEquals("tx-4", adminAlerts.contasFixasEmAtraso.first().transacao.id)
    assertEquals(2L, adminAlerts.contasFixasEmAtraso.first().diasEmAtraso)

    assertEquals(1, adminAlerts.ordensMais30Dias.size)
    assertEquals(1010, adminAlerts.ordensMais30Dias.first().os.numeroOs)
    assertEquals(32L, adminAlerts.ordensMais30Dias.first().diasEmAberto)

    assertEquals(1, adminAlerts.pedidosPecasPendentes.size)
    assertEquals(1, adminAlerts.pecasEstoqueBaixoCount)
    assertEquals(5, adminAlerts.activeAlertsCount)

    // Employee role must NOT see financial alerts, only operational alerts
    val employeeAlerts = AlertCalculator.compute(
      ordens = listOf(osAberta32d, osEntregue40d),
      transacoes = listOf(txReceber35d, txFixaAtrasada),
      pedidosPecas = listOf(pedidoPendente),
      pecas = listOf(pecaBaixa),
      canAccessFinanceiro = false,
      referenceDateIso = refDate
    )

    assertTrue(employeeAlerts.contasReceberMais30Dias.isEmpty())
    assertTrue(employeeAlerts.contasFixasEmAtraso.isEmpty())
    assertEquals(0, employeeAlerts.financeiroAlertsCount)
    assertEquals(1, employeeAlerts.ordensMais30Dias.size)
    assertEquals(1, employeeAlerts.pedidosPecasPendentes.size)
  }

  @Test
  fun `workshop ui state enforces max 4 employee accounts`() {
    val threeEmployees = (1..3).map { idx ->
      FuncionarioDto(id = "f-$idx", ownerId = "o-1", nome = "Func $idx", email = "f$idx@rterp.com")
    }
    val fourEmployees = threeEmployees + FuncionarioDto(
      id = "f-4",
      ownerId = "o-1",
      nome = "Func 4",
      email = "f4@rterp.com"
    )

    assertTrue(WorkshopUiState(funcionarios = threeEmployees).canAddMoreFuncionarios)
    assertFalse(WorkshopUiState(funcionarios = fourEmployees).canAddMoreFuncionarios)
  }

  @Test
  fun `os receipt and whatsapp message include responsible technician name`() {
    val os = OrdemServicoDto(
      id = "os-99",
      numeroOs = 1099,
      clienteNome = "João Silva",
      tecnicoNome = "Carlos Técnico",
      equipamento = "Central Alarme",
      defeitoRelatado = "Sem disparo de sirene",
      valorTotal = 250.0
    )

    val thermal = ReceiptGenerator.buildThermalText(os, ReceiptKind.ENTRADA)
    val waMessage = WhatsAppHelper.buildOsMessage(os)

    assertTrue(thermal.contains("Carlos Técnico"))
    assertTrue(waMessage.contains("Carlos Técnico"))
  }

  @Test
  fun `image compressor reduces large bitmap to compact webp`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempFile = File(context.cacheDir, "test_large.png")
    val bitmap = Bitmap.createBitmap(2000, 1500, Bitmap.Config.ARGB_8888)
    FileOutputStream(tempFile).use { out ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    bitmap.recycle()

    val result = ImageCompressor.compressUri(
      context = context,
      uri = Uri.fromFile(tempFile),
      preset = ImageCompressionPreset.ECONOMIA_MAXIMA
    )

    assertNotNull(result)
    assertEquals("image/webp", result!!.mimeType)
    assertEquals("webp", result.extension)
    assertTrue(result.width <= ImageCompressionPreset.ECONOMIA_MAXIMA.maxDimensionPx)
    assertTrue(result.height <= ImageCompressionPreset.ECONOMIA_MAXIMA.maxDimensionPx)
    assertTrue(result.compressedBytes > 0)
    tempFile.delete()
  }
}

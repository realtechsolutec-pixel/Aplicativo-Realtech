package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.StatusAnalysis
import com.example.ui.theme.StatusAnalysisContainer
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoContainer
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusReadyContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.squareup.moshi.Json

// ============================================================================
// Auth Models (Supabase GoTrue)
// ============================================================================

data class SupabaseAuthRequest(
    val email: String,
    val password: String
)

data class SupabaseSignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

data class SupabaseRefreshRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    val user: SupabaseUser? = null
)

data class SupabaseUser(
    val id: String,
    val email: String? = null,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null
)

data class SupabaseErrorResponse(
    val message: String? = null,
    val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null,
    val msg: String? = null,
    val details: String? = null,
    val hint: String? = null
)

enum class UserRole(val code: String, val label: String) {
    ADMIN("ADMIN", "Proprietário / Admin"),
    FUNCIONARIO("FUNCIONARIO", "Funcionário (Operacional)");

    companion object {
        fun fromCode(code: String?): UserRole =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: ADMIN
    }
}

data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val fullName: String,
    val expiresAtEpochSec: Long,
    val role: UserRole = UserRole.ADMIN,
    val workshopOwnerId: String = userId
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN
    val isFuncionario: Boolean get() = role == UserRole.FUNCIONARIO
    val canAccessFinanceiro: Boolean get() = role == UserRole.ADMIN
}

// ============================================================================
// Domain Enums
// ============================================================================

enum class TipoCliente(val code: String, val label: String) {
    PF("PF", "Pessoa Física"),
    PJ("PJ", "Empresa (PJ)");

    companion object {
        fun fromCode(code: String?): TipoCliente = entries.find { it.code == code } ?: PF
    }
}

enum class StatusOS(
    val code: String,
    val label: String,
    val shortLabel: String,
    val accentColor: Color,
    val containerColor: Color
) {
    ABERTA("ABERTA", "Aberta / Triagem", "Aberta", StatusInfo, StatusInfoContainer),
    EM_ANALISE("EM_ANALISE", "Em Análise", "Análise", StatusAnalysis, StatusAnalysisContainer),
    AGUARDANDO_PECA("AGUARDANDO_PECA", "Aguardando Peça", "Ag. Peça", StatusWarning, StatusWarningContainer),
    APROVADA("APROVADA", "Em Reparo", "Em Reparo", Color(0xFF0052CC), Color(0xFFD9E6FF)),
    PRONTA("PRONTA", "Pronta p/ Retirada", "Pronta", StatusReady, StatusReadyContainer),
    ENTREGUE("ENTREGUE", "Entregue / Concluída", "Entregue", StatusSuccess, StatusSuccessContainer);

    fun nextStatus(): StatusOS? {
        val idx = entries.indexOf(this)
        return if (idx in 0 until entries.lastIndex) entries[idx + 1] else null
    }

    companion object {
        fun fromCode(code: String?): StatusOS = entries.find { it.code == code } ?: ABERTA
    }
}

enum class PrioridadeOS(
    val code: String,
    val label: String,
    val color: Color,
    val containerColor: Color
) {
    BAIXA("BAIXA", "Baixa", Color(0xFF64748B), Color(0xFFF1F5F9)),
    NORMAL("NORMAL", "Normal", StatusInfo, StatusInfoContainer),
    ALTA("ALTA", "Alta", StatusWarning, StatusWarningContainer),
    URGENTE("URGENTE", "Urgente", StatusDanger, StatusDangerContainer);

    companion object {
        fun fromCode(code: String?): PrioridadeOS = entries.find { it.code == code } ?: NORMAL
    }
}

enum class TipoTransacao(val code: String, val label: String) {
    RECEBER("RECEBER", "A Receber"),
    PAGAR("PAGAR", "A Pagar");

    companion object {
        fun fromCode(code: String?): TipoTransacao = entries.find { it.code == code } ?: RECEBER
    }
}

enum class EscopoFinanceiro(val code: String, val label: String) {
    LOJA("LOJA", "Loja (Oficina)"),
    PESSOAL("PESSOAL", "Pessoal");

    companion object {
        fun fromCode(code: String?): EscopoFinanceiro = entries.find { it.code == code } ?: LOJA
    }
}

enum class StatusFinanceiro(val code: String, val label: String) {
    PENDENTE("PENDENTE", "Pendente"),
    PAGO("PAGO", "Pago");

    companion object {
        fun fromCode(code: String?): StatusFinanceiro = entries.find { it.code == code } ?: PENDENTE
    }
}

// ============================================================================
// Supabase Table DTOs
// ============================================================================

data class ClienteDto(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    val nome: String,
    val tipo: String = "PF",
    val documento: String = "",
    val telefone: String,
    val email: String = "",
    val endereco: String = "",
    val observacoes: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

data class PecaEstoqueDto(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "codigo_sku") val codigoSku: String = "",
    val nome: String,
    val categoria: String = "Componentes Eletrônicos",
    val fornecedor: String = "",
    val localizacao: String = "",
    val quantidade: Int = 0,
    @Json(name = "estoque_minimo") val estoqueMinimo: Int = 3,
    @Json(name = "preco_custo") val precoCusto: Double = 0.0,
    @Json(name = "preco_venda") val precoVenda: Double = 0.0,
    @Json(name = "foto_url") val fotoUrl: String = "",
    @Json(name = "descricao_detalhada") val descricaoDetalhada: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    val isEstoqueBaixo: Boolean get() = quantidade <= estoqueMinimo
}

data class OrdemServicoDto(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "numero_os") val numeroOs: Int = 1001,
    @Json(name = "cliente_id") val clienteId: String? = null,
    @Json(name = "cliente_nome") val clienteNome: String,
    @Json(name = "cliente_telefone") val clienteTelefone: String = "",
    @Json(name = "tecnico_id") val tecnicoId: String? = null,
    @Json(name = "tecnico_nome") val tecnicoNome: String = "",
    @Json(name = "tecnico_email") val tecnicoEmail: String = "",
    @Json(name = "criado_por_nome") val criadoPorNome: String = "",
    @Json(name = "comissao_percentual") val comissaoPercentual: Double = 0.0,
    @Json(name = "categoria_equipamento") val categoriaEquipamento: String = "Eletrônica Geral",
    val equipamento: String,
    @Json(name = "marca_modelo") val marcaModelo: String = "",
    @Json(name = "numero_serie") val numeroSerie: String = "",
    @Json(name = "acessorios_entregues") val acessoriosEntregues: String = "",
    @Json(name = "defeito_relatado") val defeitoRelatado: String,
    @Json(name = "diagnostico_tecnico") val diagnosticoTecnico: String = "",
    @Json(name = "pecas_utilizadas") val pecasUtilizadas: String = "",
    @Json(name = "fotos_urls") val fotosUrls: List<String> = emptyList(),
    val status: String = StatusOS.ABERTA.code,
    val prioridade: String = PrioridadeOS.NORMAL.code,
    @Json(name = "valor_mao_obra") val valorMaoObra: Double = 0.0,
    @Json(name = "valor_pecas") val valorPecas: Double = 0.0,
    val desconto: Double = 0.0,
    @Json(name = "valor_total") val valorTotal: Double = 0.0,
    @Json(name = "garantia_dias") val garantiaDias: Int = 90,
    @Json(name = "data_entrada") val dataEntrada: String? = null,
    @Json(name = "data_saida") val dataSaida: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    val statusEnum: StatusOS get() = StatusOS.fromCode(status)
    val prioridadeEnum: PrioridadeOS get() = PrioridadeOS.fromCode(prioridade)
    val effectiveComissaoValor: Double
        get() = (valorMaoObra * (comissaoPercentual.coerceIn(0.0, 100.0) / 100.0)).coerceAtLeast(0.0)
}

data class TransacaoFinanceiraDto(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "os_id") val osId: String? = null,
    @Json(name = "venda_id") val vendaId: String? = null,
    val descricao: String,
    val categoria: String = "Serviços / Reparo",
    val tipo: String = TipoTransacao.RECEBER.code,
    val escopo: String = EscopoFinanceiro.LOJA.code,
    val status: String = StatusFinanceiro.PENDENTE.code,
    val valor: Double = 0.0,
    @Json(name = "forma_pagamento") val formaPagamento: String = "PIX",
    @Json(name = "data_vencimento") val dataVencimento: String,
    @Json(name = "data_pagamento") val dataPagamento: String? = null,
    @Json(name = "is_conta_fixa") val isContaFixa: Boolean = false,
    val observacoes: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    val tipoEnum: TipoTransacao get() = TipoTransacao.fromCode(tipo)
    val escopoEnum: EscopoFinanceiro get() = EscopoFinanceiro.fromCode(escopo)
    val statusEnum: StatusFinanceiro get() = StatusFinanceiro.fromCode(status)
    val isContaFixaEfetiva: Boolean
        get() = isContaFixa ||
            categoria.contains("Aluguel", ignoreCase = true) ||
            categoria.contains("Fixa", ignoreCase = true) ||
            categoria.contains("Moradia / Contas", ignoreCase = true)
}

data class ConfiguracaoOficinaDto(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "nome_loja") val nomeLoja: String = "RTerp Eletrônica & Segurança",
    val cnpj: String = "",
    val telefone: String = "",
    val email: String = "",
    val endereco: String = "",
    @Json(name = "rodape_os") val rodapeOs: String = "Equipamentos não retirados em até 90 dias após aviso estarão sujeitos à taxa de estadia/descarte.",
    @Json(name = "garantia_padrao_dias") val garantiaPadraoDias: Int = 90,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

data class FuncionarioDto(
    val id: String? = null,
    @Json(name = "owner_id") val ownerId: String = "",
    @Json(name = "funcionario_user_id") val funcionarioUserId: String? = null,
    val nome: String,
    val email: String,
    val cargo: String = "Técnico Operacional",
    @Json(name = "comissao_percentual") val comissaoPercentual: Double = 50.0,
    val ativo: Boolean = true,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

data class MetaDiariaDto(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "meta_mensal") val metaMensal: Double = 15000.0,
    @Json(name = "custos_fixos_mensais") val custosFixosMensais: Double = 3500.0,
    @Json(name = "dias_uteis_mes") val diasUteisMes: Int = 22,
    @Json(name = "dias_uteis_restantes") val diasUteisRestantes: Int = 15,
    @Json(name = "ticket_medio_estimado") val ticketMedioEstimado: Double = 250.0,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

data class CotacaoPrecoDto(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "produto_nome") val produtoNome: String,
    @Json(name = "codigo_sku") val codigoSku: String = "",
    val fornecedor: String,
    @Json(name = "preco_unitario") val precoUnitario: Double = 0.0,
    val frete: Double = 0.0,
    @Json(name = "prazo_entrega_dias") val prazoEntregaDias: Int = 2,
    val observacoes: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    val custoTotal: Double get() = precoUnitario + frete
}

enum class PedidoPecaMotivo(val code: String, val label: String) {
    FALTANTE("FALTANTE", "Falta no Estoque"),
    PARA_OS("PARA_OS", "P/ Concluir Serviço (OS)");

    companion object {
        fun fromCode(code: String?): PedidoPecaMotivo = entries.find { it.code == code } ?: PARA_OS
    }
}

enum class PedidoPecaStatus(
    val code: String,
    val label: String,
    val accentColor: Color,
    val containerColor: Color
) {
    PENDENTE("PENDENTE", "Pendente", StatusDanger, StatusDangerContainer),
    PEDIDO_FEITO("PEDIDO_FEITO", "Pedido Feito", StatusWarning, StatusWarningContainer),
    RECEBIDA("RECEBIDA", "Recebida", StatusSuccess, StatusSuccessContainer);

    companion object {
        fun fromCode(code: String?): PedidoPecaStatus = entries.find { it.code == code } ?: PENDENTE
    }
}

data class PedidoPecaDto(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "peca_id") val pecaId: String? = null,
    @Json(name = "os_id") val osId: String? = null,
    @Json(name = "numero_os") val numeroOs: Int? = null,
    @Json(name = "nome_peca") val nomePeca: String,
    @Json(name = "codigo_sku") val codigoSku: String = "",
    val quantidade: Int = 1,
    val motivo: String = PedidoPecaMotivo.PARA_OS.code,
    val status: String = PedidoPecaStatus.PENDENTE.code,
    val prioridade: String = PrioridadeOS.NORMAL.code,
    @Json(name = "solicitado_por") val solicitadoPor: String = "",
    @Json(name = "fornecedor_sugerido") val fornecedorSugerido: String = "",
    val observacoes: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    val motivoEnum: PedidoPecaMotivo get() = PedidoPecaMotivo.fromCode(motivo)
    val statusEnum: PedidoPecaStatus get() = PedidoPecaStatus.fromCode(status)
    val prioridadeEnum: PrioridadeOS get() = PrioridadeOS.fromCode(prioridade)
}

// ============================================================================
// VENDAS / PDV INTEGRADO (Estoque + Financeiro)
// ============================================================================

enum class StatusVenda(
    val code: String,
    val label: String,
    val accentColor: Color,
    val containerColor: Color
) {
    CONCLUIDA("CONCLUIDA", "Concluída", StatusSuccess, StatusSuccessContainer),
    PENDENTE("PENDENTE", "A Receber", StatusWarning, StatusWarningContainer),
    CANCELADA("CANCELADA", "Cancelada", StatusDanger, StatusDangerContainer);

    companion object {
        fun fromCode(code: String?): StatusVenda =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: CONCLUIDA
    }
}

val SALE_PAYMENT_METHODS = listOf(
    "PIX",
    "Dinheiro",
    "Cartão de Débito",
    "Cartão de Crédito",
    "Transferência / Boleto",
    "Crediário / Fiado"
)

data class VendaItemDto(
    @Json(name = "peca_id") val pecaId: String? = null,
    @Json(name = "codigo_sku") val codigoSku: String = "",
    val nome: String = "Produto",
    val quantidade: Int = 1,
    @Json(name = "preco_unitario") val precoUnitario: Double = 0.0,
    val subtotal: Double = 0.0
)

data class VendaDto(
    val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "numero_venda") val numeroVenda: Int = 1,
    @Json(name = "cliente_id") val clienteId: String? = null,
    @Json(name = "cliente_nome") val clienteNome: String = "Consumidor Final",
    @Json(name = "cliente_telefone") val clienteTelefone: String = "",
    @Json(name = "vendedor_id") val vendedorId: String? = null,
    @Json(name = "vendedor_nome") val vendedorNome: String = "",
    @Json(name = "forma_pagamento") val formaPagamento: String = "PIX",
    val status: String = StatusVenda.CONCLUIDA.code,
    val subtotal: Double = 0.0,
    val desconto: Double = 0.0,
    @Json(name = "valor_total") val valorTotal: Double = 0.0,
    val observacoes: String = "",
    val itens: List<VendaItemDto> = emptyList(),
    @Json(name = "data_venda") val dataVenda: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    val statusEnum: StatusVenda get() = StatusVenda.fromCode(status)
}

data class RegistrarVendaItemRequest(
    @Json(name = "peca_id") val pecaId: String? = null,
    @Json(name = "codigo_sku") val codigoSku: String = "",
    val nome: String,
    val quantidade: Int,
    @Json(name = "preco_unitario") val precoUnitario: Double
)

data class RegistrarVendaRpcRequest(
    @Json(name = "p_cliente_id") val pClienteId: String? = null,
    @Json(name = "p_cliente_nome") val pClienteNome: String = "Consumidor Final",
    @Json(name = "p_cliente_telefone") val pClienteTelefone: String = "",
    @Json(name = "p_vendedor_id") val pVendedorId: String? = null,
    @Json(name = "p_vendedor_nome") val pVendedorNome: String = "",
    @Json(name = "p_forma_pagamento") val pFormaPagamento: String = "PIX",
    @Json(name = "p_desconto") val pDesconto: Double = 0.0,
    @Json(name = "p_pagamento_pendente") val pPagamentoPendente: Boolean = false,
    @Json(name = "p_observacoes") val pObservacoes: String = "",
    @Json(name = "p_itens") val pItens: List<RegistrarVendaItemRequest>
)

data class CancelarVendaRpcRequest(
    @Json(name = "p_venda_id") val pVendaId: String
)

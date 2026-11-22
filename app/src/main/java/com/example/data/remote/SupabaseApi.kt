package com.example.data.remote

import com.example.data.model.CancelarVendaRpcRequest
import com.example.data.model.ClienteDto
import com.example.data.model.ConfiguracaoOficinaDto
import com.example.data.model.CotacaoPrecoDto
import com.example.data.model.FuncionarioDto
import com.example.data.model.MetaDiariaDto
import com.example.data.model.OrdemServicoDto
import com.example.data.model.PecaEstoqueDto
import com.example.data.model.RegistrarVendaRpcRequest
import com.example.data.model.SupabaseAuthRequest
import com.example.data.model.SupabaseAuthResponse
import com.example.data.model.SupabaseRefreshRequest
import com.example.data.model.SupabaseSignUpRequest
import com.example.data.model.TransacaoFinanceiraDto
import com.example.data.model.VendaDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SupabaseApi {

    // =========================================================================
    // AUTH (GoTrue)
    // =========================================================================

    @POST("auth/v1/token?grant_type=password")
    suspend fun signInWithPassword(
        @Body body: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body body: SupabaseSignUpRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Body body: SupabaseRefreshRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/logout")
    suspend fun signOut(
        @Header("Authorization") bearerToken: String
    ): Response<Unit>

    // =========================================================================
    // CLIENTES
    // =========================================================================

    @GET("rest/v1/clientes")
    suspend fun getClientes(
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "nome.asc"
    ): Response<List<ClienteDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/clientes")
    suspend fun createCliente(
        @Header("Authorization") bearerToken: String,
        @Body cliente: ClienteDto
    ): Response<List<ClienteDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/clientes")
    suspend fun updateCliente(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String,
        @Body cliente: ClienteDto
    ): Response<List<ClienteDto>>

    @DELETE("rest/v1/clientes")
    suspend fun deleteCliente(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    // =========================================================================
    // ESTOQUE DE PEÇAS
    // =========================================================================

    @GET("rest/v1/pecas_estoque")
    suspend fun getPecas(
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "nome.asc"
    ): Response<List<PecaEstoqueDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/pecas_estoque")
    suspend fun createPeca(
        @Header("Authorization") bearerToken: String,
        @Body peca: PecaEstoqueDto
    ): Response<List<PecaEstoqueDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/pecas_estoque")
    suspend fun updatePeca(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String,
        @Body peca: PecaEstoqueDto
    ): Response<List<PecaEstoqueDto>>

    @DELETE("rest/v1/pecas_estoque")
    suspend fun deletePeca(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    // =========================================================================
    // ORDENS DE SERVIÇO
    // =========================================================================

    @GET("rest/v1/ordens_servico")
    suspend fun getOrdensServico(
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "numero_os.desc"
    ): Response<List<OrdemServicoDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/ordens_servico")
    suspend fun createOrdemServico(
        @Header("Authorization") bearerToken: String,
        @Body os: OrdemServicoDto
    ): Response<List<OrdemServicoDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/ordens_servico")
    suspend fun updateOrdemServico(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String,
        @Body os: OrdemServicoDto
    ): Response<List<OrdemServicoDto>>

    @DELETE("rest/v1/ordens_servico")
    suspend fun deleteOrdemServico(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    // =========================================================================
    // FINANCEIRO
    // =========================================================================

    @GET("rest/v1/transacoes_financeiras")
    suspend fun getTransacoes(
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "data_vencimento.desc,created_at.desc"
    ): Response<List<TransacaoFinanceiraDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/transacoes_financeiras")
    suspend fun createTransacao(
        @Header("Authorization") bearerToken: String,
        @Body transacao: TransacaoFinanceiraDto
    ): Response<List<TransacaoFinanceiraDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/transacoes_financeiras")
    suspend fun updateTransacao(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String,
        @Body transacao: TransacaoFinanceiraDto
    ): Response<List<TransacaoFinanceiraDto>>

    @DELETE("rest/v1/transacoes_financeiras")
    suspend fun deleteTransacao(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    // =========================================================================
    // STORAGE (Fotos de Equipamentos e Componentes)
    // =========================================================================

    @POST("storage/v1/object/tecsec-midias/{objectPath}")
    suspend fun uploadMedia(
        @Header("Authorization") bearerToken: String,
        @Header("x-upsert") upsert: String = "true",
        @Path(value = "objectPath", encoded = true) objectPath: String,
        @Body body: RequestBody
    ): Response<Unit>

    // =========================================================================
    // CONFIGURAÇÕES DA OFICINA
    // =========================================================================

    @GET("rest/v1/configuracoes_oficina")
    suspend fun getConfiguracaoOficina(
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*"
    ): Response<List<ConfiguracaoOficinaDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/configuracoes_oficina")
    suspend fun createConfiguracaoOficina(
        @Header("Authorization") bearerToken: String,
        @Body config: ConfiguracaoOficinaDto
    ): Response<List<ConfiguracaoOficinaDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/configuracoes_oficina")
    suspend fun updateConfiguracaoOficina(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String,
        @Body config: ConfiguracaoOficinaDto
    ): Response<List<ConfiguracaoOficinaDto>>

    // =========================================================================
    // METAS DIÁRIAS
    // =========================================================================

    @GET("rest/v1/metas_oficina")
    suspend fun getMetaDiaria(
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*"
    ): Response<List<MetaDiariaDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/metas_oficina")
    suspend fun createMetaDiaria(
        @Header("Authorization") bearerToken: String,
        @Body meta: MetaDiariaDto
    ): Response<List<MetaDiariaDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/metas_oficina")
    suspend fun updateMetaDiaria(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String,
        @Body meta: MetaDiariaDto
    ): Response<List<MetaDiariaDto>>

    // =========================================================================
    // COTAÇÕES E COMPARATIVO DE PREÇOS
    // =========================================================================

    @GET("rest/v1/cotacoes_precos")
    suspend fun getCotacoes(
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "produto_nome.asc,preco_unitario.asc"
    ): Response<List<CotacaoPrecoDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/cotacoes_precos")
    suspend fun createCotacao(
        @Header("Authorization") bearerToken: String,
        @Body cotacao: CotacaoPrecoDto
    ): Response<List<CotacaoPrecoDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/cotacoes_precos")
    suspend fun updateCotacao(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String,
        @Body cotacao: CotacaoPrecoDto
    ): Response<List<CotacaoPrecoDto>>

    @DELETE("rest/v1/cotacoes_precos")
    suspend fun deleteCotacao(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    // =========================================================================
    // PEDIDOS DE PEÇAS FALTANTES / PARA CONCLUIR OS
    // =========================================================================

    @GET("rest/v1/pedidos_pecas")
    suspend fun getPedidosPecas(
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<com.example.data.model.PedidoPecaDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/pedidos_pecas")
    suspend fun createPedidoPeca(
        @Header("Authorization") bearerToken: String,
        @Body pedido: com.example.data.model.PedidoPecaDto
    ): Response<List<com.example.data.model.PedidoPecaDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/pedidos_pecas")
    suspend fun updatePedidoPeca(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String,
        @Body pedido: com.example.data.model.PedidoPecaDto
    ): Response<List<com.example.data.model.PedidoPecaDto>>

    @DELETE("rest/v1/pedidos_pecas")
    suspend fun deletePedidoPeca(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    // =========================================================================
    // FUNCIONÁRIOS / EQUIPE OPERACIONAL
    // =========================================================================

    @GET("rest/v1/funcionarios")
    suspend fun getFuncionarios(
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "nome.asc"
    ): Response<List<FuncionarioDto>>

    @GET("rest/v1/funcionarios")
    suspend fun getFuncionarioByEmail(
        @Header("Authorization") bearerToken: String,
        @Query("email") emailFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<FuncionarioDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/funcionarios")
    suspend fun createFuncionario(
        @Header("Authorization") bearerToken: String,
        @Body funcionario: FuncionarioDto
    ): Response<List<FuncionarioDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/funcionarios")
    suspend fun updateFuncionario(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String,
        @Body funcionario: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<FuncionarioDto>>

    @DELETE("rest/v1/funcionarios")
    suspend fun deleteFuncionario(
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    // =========================================================================
    // VENDAS / PDV INTEGRADO (RPC Atômico com Baixa de Estoque + Financeiro)
    // =========================================================================

    @GET("rest/v1/vendas")
    suspend fun getVendas(
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "numero_venda.desc,data_venda.desc",
        @Query("limit") limit: String = "300"
    ): Response<List<VendaDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/rpc/registrar_venda_completa")
    suspend fun registrarVendaCompleta(
        @Header("Authorization") bearerToken: String,
        @Body body: RegistrarVendaRpcRequest
    ): Response<List<VendaDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/rpc/cancelar_venda_completa")
    suspend fun cancelarVendaCompleta(
        @Header("Authorization") bearerToken: String,
        @Body body: CancelarVendaRpcRequest
    ): Response<List<VendaDto>>
}

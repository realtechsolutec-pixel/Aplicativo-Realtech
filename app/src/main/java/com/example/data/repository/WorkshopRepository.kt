package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.SessionManager
import com.example.data.model.CancelarVendaRpcRequest
import com.example.data.model.ClienteDto
import com.example.data.model.ConfiguracaoOficinaDto
import com.example.data.model.CotacaoPrecoDto
import com.example.data.model.EscopoFinanceiro
import com.example.data.model.FuncionarioDto
import com.example.data.model.MetaDiariaDto
import com.example.data.model.OrdemServicoDto
import com.example.data.model.PecaEstoqueDto
import com.example.data.model.PedidoPecaDto
import com.example.data.model.RegistrarVendaRpcRequest
import com.example.data.model.StatusFinanceiro
import com.example.data.model.StatusOS
import com.example.data.model.SupabaseAuthRequest
import com.example.data.model.SupabaseErrorResponse
import com.example.data.model.SupabaseRefreshRequest
import com.example.data.model.SupabaseSignUpRequest
import com.example.data.model.TipoTransacao
import com.example.data.model.TransacaoFinanceiraDto
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.model.VendaDto
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConfig
import com.example.util.Formatters
import com.example.util.ImageCompressionPreset
import com.example.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.util.UUID

sealed class RepoResult<out T> {
    data class Success<T>(val data: T) : RepoResult<T>()
    data class Error(val message: String, val unauthorized: Boolean = false) : RepoResult<Nothing>()
}

data class UploadedMediaResult(
    val publicUrl: String,
    val originalBytes: Long,
    val compressedBytes: Int
)

class WorkshopRepository(private val sessionManager: SessionManager) {

    private val api = SupabaseClient.api
    private val errorAdapter = SupabaseClient.moshi.adapter(SupabaseErrorResponse::class.java)

    val sessionFlow: Flow<UserSession?> = sessionManager.sessionFlow
    val compressionPresetFlow: Flow<ImageCompressionPreset> = sessionManager.compressionPresetFlow

    suspend fun saveCompressionPreset(preset: ImageCompressionPreset) {
        sessionManager.saveCompressionPreset(preset)
    }

    private fun checkConfigured(): String? {
        return if (!SupabaseConfig.isConfigured) {
            "Configure SUPABASE_URL e SUPABASE_ANON_KEY no painel Secrets do AI Studio."
        } else {
            null
        }
    }

    suspend fun signIn(
        email: String,
        password: String,
        requestedRole: UserRole = UserRole.ADMIN
    ): RepoResult<UserSession> {
        checkConfigured()?.let { return RepoResult.Error(it) }
        return try {
            val cleanEmail = email.trim()
            val res = api.signInWithPassword(SupabaseAuthRequest(cleanEmail, password))
            if (res.isSuccessful) {
                val body = res.body()
                val token = body?.accessToken
                val refresh = body?.refreshToken
                val user = body?.user
                if (token != null && refresh != null && user != null) {
                    val bearer = "Bearer $token"
                    val userEmail = (user.email ?: cleanEmail).lowercase()
                    val metaName = (user.userMetadata?.get("full_name") as? String)
                        ?: cleanEmail.substringBefore("@")
                    val metaRole = (user.userMetadata?.get("role") as? String)
                    val metaOwnerId = (user.userMetadata?.get("owner_id") as? String)?.takeIf { it.isNotBlank() }

                    // Verifica na tabela funcionarios se este usuário pertence a uma oficina como funcionário
                    val funcRecord = try {
                        val funcRes = api.getFuncionarioByEmail(bearer, "eq.$userEmail")
                        if (funcRes.isSuccessful) funcRes.body()?.firstOrNull() else null
                    } catch (_: Exception) {
                        null
                    }

                    if (funcRecord != null && !funcRecord.ativo) {
                        return RepoResult.Error("Seu acesso de funcionário foi desativado pelo proprietário da oficina.")
                    }

                    // Se existe registro de funcionário ainda sem funcionario_user_id vinculado, vincula agora
                    if (funcRecord != null && funcRecord.id != null && funcRecord.funcionarioUserId.isNullOrBlank()) {
                        try {
                            api.updateFuncionario(
                                bearerToken = bearer,
                                idFilter = "eq.${funcRecord.id}",
                                funcionario = mapOf("funcionario_user_id" to user.id)
                            )
                        } catch (_: Exception) {
                        }
                    }

                    val resolvedRole: UserRole
                    val resolvedOwnerId: String
                    if (funcRecord != null) {
                        resolvedRole = UserRole.FUNCIONARIO
                        resolvedOwnerId = funcRecord.ownerId.ifBlank { metaOwnerId ?: user.id }
                    } else if (metaRole.equals(UserRole.FUNCIONARIO.code, ignoreCase = true)) {
                        resolvedRole = UserRole.FUNCIONARIO
                        resolvedOwnerId = metaOwnerId ?: user.id
                    } else if (requestedRole == UserRole.FUNCIONARIO) {
                        // Permite que o dono acesse no modo operacional/funcionário quando solicitado no login
                        resolvedRole = UserRole.FUNCIONARIO
                        resolvedOwnerId = user.id
                    } else {
                        resolvedRole = UserRole.ADMIN
                        resolvedOwnerId = user.id
                    }

                    val expiresAt = (System.currentTimeMillis() / 1000L) + (body.expiresIn ?: 3600L)
                    val session = UserSession(
                        accessToken = token,
                        refreshToken = refresh,
                        userId = user.id,
                        email = user.email ?: cleanEmail,
                        fullName = funcRecord?.nome?.takeIf { it.isNotBlank() } ?: metaName,
                        expiresAtEpochSec = expiresAt,
                        role = resolvedRole,
                        workshopOwnerId = resolvedOwnerId
                    )
                    sessionManager.saveSession(session)
                    RepoResult.Success(session)
                } else {
                    RepoResult.Error("Resposta de autenticação incompleta do Supabase.")
                }
            } else {
                RepoResult.Error(parseError(res, "Falha ao fazer login."))
            }
        } catch (e: Exception) {
            RepoResult.Error("Erro de conexão com o Supabase: ${e.localizedMessage ?: "verifique a internet"}")
        }
    }

    suspend fun signUp(fullName: String, email: String, password: String): RepoResult<String> {
        checkConfigured()?.let { return RepoResult.Error(it) }
        return try {
            val res = api.signUp(
                SupabaseSignUpRequest(
                    email = email.trim(),
                    password = password,
                    data = mapOf(
                        "full_name" to fullName.trim(),
                        "role" to UserRole.ADMIN.code
                    )
                )
            )
            if (res.isSuccessful) {
                val body = res.body()
                val token = body?.accessToken
                val refresh = body?.refreshToken
                val user = body?.user
                if (token != null && refresh != null && user != null) {
                    val expiresAt = (System.currentTimeMillis() / 1000L) + (body.expiresIn ?: 3600L)
                    sessionManager.saveSession(
                        UserSession(
                            accessToken = token,
                            refreshToken = refresh,
                            userId = user.id,
                            email = user.email ?: email.trim(),
                            fullName = fullName.trim().ifBlank { email.substringBefore("@") },
                            expiresAtEpochSec = expiresAt,
                            role = UserRole.ADMIN,
                            workshopOwnerId = user.id
                        )
                    )
                    RepoResult.Success("AUTHENTICATED")
                } else {
                    RepoResult.Success("CONFIRM_EMAIL")
                }
            } else {
                RepoResult.Error(parseError(res, "Não foi possível criar a conta."))
            }
        } catch (e: Exception) {
            RepoResult.Error("Erro de conexão ao cadastrar: ${e.localizedMessage ?: "tente novamente"}")
        }
    }

    suspend fun signUpAsEmployee(
        fullName: String,
        email: String,
        password: String,
        workshopCode: String
    ): RepoResult<String> {
        checkConfigured()?.let { return RepoResult.Error(it) }
        val ownerId = workshopCode.trim()
        return try {
            val cleanEmail = email.trim().lowercase()
            val res = api.signUp(
                SupabaseSignUpRequest(
                    email = cleanEmail,
                    password = password,
                    data = mapOf(
                        "full_name" to fullName.trim(),
                        "role" to UserRole.FUNCIONARIO.code,
                        "owner_id" to ownerId
                    )
                )
            )
            if (res.isSuccessful) {
                val body = res.body()
                val token = body?.accessToken
                val refresh = body?.refreshToken
                val user = body?.user
                if (token != null && refresh != null && user != null) {
                    val bearer = "Bearer $token"
                    try {
                        val existing = api.getFuncionarioByEmail(bearer, "eq.$cleanEmail")
                        val existingRow = if (existing.isSuccessful) existing.body()?.firstOrNull() else null
                        if (existingRow != null && existingRow.id != null) {
                            api.updateFuncionario(
                                bearerToken = bearer,
                                idFilter = "eq.${existingRow.id}",
                                funcionario = mapOf("funcionario_user_id" to user.id)
                            )
                        } else {
                            api.createFuncionario(
                                bearerToken = bearer,
                                funcionario = FuncionarioDto(
                                    ownerId = ownerId,
                                    funcionarioUserId = user.id,
                                    nome = fullName.trim(),
                                    email = cleanEmail,
                                    cargo = "Técnico Operacional",
                                    ativo = true
                                )
                            )
                        }
                    } catch (_: Exception) {
                    }
                    val expiresAt = (System.currentTimeMillis() / 1000L) + (body.expiresIn ?: 3600L)
                    sessionManager.saveSession(
                        UserSession(
                            accessToken = token,
                            refreshToken = refresh,
                            userId = user.id,
                            email = user.email ?: cleanEmail,
                            fullName = fullName.trim().ifBlank { cleanEmail.substringBefore("@") },
                            expiresAtEpochSec = expiresAt,
                            role = UserRole.FUNCIONARIO,
                            workshopOwnerId = ownerId
                        )
                    )
                    RepoResult.Success("AUTHENTICATED")
                } else {
                    RepoResult.Success("CONFIRM_EMAIL")
                }
            } else {
                RepoResult.Error(parseError(res, "Não foi possível vincular a conta de funcionário."))
            }
        } catch (e: Exception) {
            RepoResult.Error("Erro de conexão ao cadastrar funcionário: ${e.localizedMessage ?: "tente novamente"}")
        }
    }

    suspend fun signOut() {
        val current = sessionManager.getCurrentSession()
        if (current != null && SupabaseConfig.isConfigured) {
            try {
                api.signOut("Bearer ${current.accessToken}")
            } catch (_: Exception) {
            }
        }
        sessionManager.clearSession()
    }

    private suspend fun getValidSession(): UserSession? {
        val current = sessionManager.getCurrentSession() ?: return null
        val nowSec = System.currentTimeMillis() / 1000L
        if (nowSec < current.expiresAtEpochSec - 60) {
            return current
        }
        return try {
            val res = api.refreshToken(SupabaseRefreshRequest(current.refreshToken))
            if (res.isSuccessful) {
                val body = res.body()
                if (body?.accessToken != null && body.refreshToken != null && body.user != null) {
                    val refreshed = current.copy(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        expiresAtEpochSec = nowSec + (body.expiresIn ?: 3600L)
                    )
                    sessionManager.saveSession(refreshed)
                    refreshed
                } else current
            } else {
                if (res.code() == 400 || res.code() == 401) {
                    sessionManager.clearSession()
                    null
                } else current
            }
        } catch (_: Exception) {
            current
        }
    }

    private suspend fun <T> withAuth(
        block: suspend (session: UserSession, bearer: String) -> Response<T>
    ): RepoResult<T> {
        checkConfigured()?.let { return RepoResult.Error(it) }
        val session = getValidSession()
            ?: return RepoResult.Error("Sessão expirada. Faça login novamente.", unauthorized = true)
        return try {
            val response = block(session, "Bearer ${session.accessToken}")
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    RepoResult.Success(body)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    RepoResult.Success(Unit as T)
                }
            } else if (response.code() == 401) {
                sessionManager.clearSession()
                RepoResult.Error("Sessão expirada. Faça login novamente.", unauthorized = true)
            } else {
                RepoResult.Error(parseError(response, "Erro ao comunicar com o banco Supabase."))
            }
        } catch (e: Exception) {
            RepoResult.Error("Falha de rede: ${e.localizedMessage ?: "verifique sua conexão"}")
        }
    }

    // =========================================================================
    // STORAGE (Upload de Imagens Compactadas em WebP para Supabase Storage)
    // =========================================================================

    suspend fun uploadImageToSupabase(
        context: Context,
        uri: Uri,
        folder: String,
        preset: ImageCompressionPreset? = null
    ): RepoResult<UploadedMediaResult> {
        checkConfigured()?.let { return RepoResult.Error(it) }
        val session = getValidSession()
            ?: return RepoResult.Error("Sessão expirada. Faça login novamente.", unauthorized = true)

        val activePreset = preset ?: sessionManager.getCompressionPreset()
        val compressed = withContext(Dispatchers.IO) {
            ImageCompressor.compressUri(context, uri, activePreset)
        } ?: return RepoResult.Error("Não foi possível ler e compactar a imagem selecionada.")

        val objectPath = "${session.workshopOwnerId}/$folder/${UUID.randomUUID()}.${compressed.extension}"
        val body = compressed.bytes.toRequestBody(compressed.mimeType.toMediaType())

        return try {
            val res = api.uploadMedia(
                bearerToken = "Bearer ${session.accessToken}",
                objectPath = objectPath,
                body = body
            )
            if (res.isSuccessful) {
                val publicUrl = "${SupabaseConfig.url}/storage/v1/object/public/tecsec-midias/$objectPath"
                RepoResult.Success(
                    UploadedMediaResult(
                        publicUrl = publicUrl,
                        originalBytes = compressed.originalBytes,
                        compressedBytes = compressed.compressedBytes
                    )
                )
            } else {
                RepoResult.Error(
                    parseError(
                        res,
                        "Falha ao enviar foto. Verifique se o bucket 'tecsec-midias' foi criado via supabase_schema.sql."
                    )
                )
            }
        } catch (e: Exception) {
            RepoResult.Error("Erro no upload da foto: ${e.localizedMessage ?: "verifique a internet"}")
        }
    }

    // =========================================================================
    // CLIENTES
    // =========================================================================

    suspend fun fetchClientes(): RepoResult<List<ClienteDto>> =
        withAuth { _, bearer -> api.getClientes(bearer) }

    suspend fun saveCliente(cliente: ClienteDto): RepoResult<ClienteDto> {
        return withAuth { session, bearer ->
            val payload = cliente.copy(userId = session.workshopOwnerId)
            if (cliente.id.isNullOrBlank()) {
                api.createCliente(bearer, payload.copy(id = null, createdAt = null, updatedAt = null))
            } else {
                api.updateCliente(bearer, "eq.${cliente.id}", payload.copy(id = null, createdAt = null, updatedAt = null))
            }
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("Cliente salvo sem retorno do servidor.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    suspend fun deleteCliente(id: String): RepoResult<Unit> =
        withAuth { _, bearer -> api.deleteCliente(bearer, "eq.$id") }

    // =========================================================================
    // ESTOQUE DE PEÇAS
    // =========================================================================

    suspend fun fetchPecas(): RepoResult<List<PecaEstoqueDto>> =
        withAuth { _, bearer -> api.getPecas(bearer) }

    suspend fun savePeca(peca: PecaEstoqueDto): RepoResult<PecaEstoqueDto> {
        return withAuth { session, bearer ->
            val payload = peca.copy(userId = session.workshopOwnerId)
            if (peca.id.isNullOrBlank()) {
                api.createPeca(bearer, payload.copy(id = null, createdAt = null, updatedAt = null))
            } else {
                api.updatePeca(bearer, "eq.${peca.id}", payload.copy(id = null, createdAt = null, updatedAt = null))
            }
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("Peça salva sem retorno do servidor.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    suspend fun deletePeca(id: String): RepoResult<Unit> =
        withAuth { _, bearer -> api.deletePeca(bearer, "eq.$id") }

    // =========================================================================
    // ORDENS DE SERVIÇO
    // =========================================================================

    suspend fun fetchOrdensServico(): RepoResult<List<OrdemServicoDto>> =
        withAuth { _, bearer -> api.getOrdensServico(bearer) }

    suspend fun saveOrdemServico(os: OrdemServicoDto): RepoResult<OrdemServicoDto> {
        return withAuth { session, bearer ->
            val computedTotal = (os.valorMaoObra + os.valorPecas - os.desconto).coerceAtLeast(0.0)
            val exitDate = if (os.status == StatusOS.ENTREGUE.code && os.dataSaida.isNullOrBlank()) {
                Formatters.nowIsoTimestamp()
            } else {
                os.dataSaida
            }
            val payload = os.copy(
                userId = session.workshopOwnerId,
                valorTotal = computedTotal,
                dataEntrada = os.dataEntrada ?: Formatters.nowIsoTimestamp(),
                dataSaida = exitDate
            )
            if (os.id.isNullOrBlank()) {
                api.createOrdemServico(bearer, payload.copy(id = null, createdAt = null, updatedAt = null))
            } else {
                api.updateOrdemServico(bearer, "eq.${os.id}", payload.copy(id = null, createdAt = null, updatedAt = null))
            }
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("OS salva sem retorno do servidor.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    suspend fun deleteOrdemServico(id: String): RepoResult<Unit> =
        withAuth { _, bearer -> api.deleteOrdemServico(bearer, "eq.$id") }

    // =========================================================================
    // FINANCEIRO (Somente Admin)
    // =========================================================================

    suspend fun fetchTransacoes(): RepoResult<List<TransacaoFinanceiraDto>> =
        withAuth { session, bearer ->
            if (!session.canAccessFinanceiro) {
                throw SecurityException("Acesso financeiro restrito ao proprietário.")
            }
            api.getTransacoes(bearer)
        }

    suspend fun saveTransacao(transacao: TransacaoFinanceiraDto): RepoResult<TransacaoFinanceiraDto> {
        return withAuth { session, bearer ->
            if (!session.canAccessFinanceiro) {
                throw SecurityException("Acesso financeiro restrito ao proprietário.")
            }
            val paidDate = if (transacao.status == StatusFinanceiro.PAGO.code && transacao.dataPagamento.isNullOrBlank()) {
                Formatters.todayIsoDate()
            } else if (transacao.status == StatusFinanceiro.PENDENTE.code) {
                null
            } else {
                transacao.dataPagamento
            }
            val payload = transacao.copy(
                userId = session.workshopOwnerId,
                dataPagamento = paidDate
            )
            if (transacao.id.isNullOrBlank()) {
                api.createTransacao(bearer, payload.copy(id = null, createdAt = null, updatedAt = null))
            } else {
                api.updateTransacao(bearer, "eq.${transacao.id}", payload.copy(id = null, createdAt = null, updatedAt = null))
            }
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("Lançamento salvo sem retorno do servidor.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    suspend fun lancarOsNoFinanceiro(os: OrdemServicoDto): RepoResult<TransacaoFinanceiraDto> {
        val tx = TransacaoFinanceiraDto(
            osId = os.id,
            descricao = "OS #${os.numeroOs} - ${os.equipamento} (${os.clienteNome})",
            categoria = "Ordem de Serviço",
            tipo = TipoTransacao.RECEBER.code,
            escopo = EscopoFinanceiro.LOJA.code,
            status = if (os.status == StatusOS.ENTREGUE.code) StatusFinanceiro.PAGO.code else StatusFinanceiro.PENDENTE.code,
            valor = os.valorTotal,
            formaPagamento = "PIX",
            dataVencimento = Formatters.todayIsoDate(),
            dataPagamento = if (os.status == StatusOS.ENTREGUE.code) Formatters.todayIsoDate() else null,
            observacoes = "Lançado automaticamente a partir da OS #${os.numeroOs}"
        )
        return saveTransacao(tx)
    }

    suspend fun deleteTransacao(id: String): RepoResult<Unit> =
        withAuth { session, bearer ->
            if (!session.canAccessFinanceiro) {
                throw SecurityException("Acesso financeiro restrito ao proprietário.")
            }
            api.deleteTransacao(bearer, "eq.$id")
        }

    // =========================================================================
    // CONFIGURAÇÕES DA OFICINA
    // =========================================================================

    suspend fun fetchConfiguracaoOficina(): RepoResult<ConfiguracaoOficinaDto?> {
        return when (val res = withAuth { _, bearer -> api.getConfiguracaoOficina(bearer) }) {
            is RepoResult.Success -> RepoResult.Success(res.data.firstOrNull())
            is RepoResult.Error -> res
        }
    }

    suspend fun saveConfiguracaoOficina(config: ConfiguracaoOficinaDto): RepoResult<ConfiguracaoOficinaDto> {
        return withAuth { session, bearer ->
            val payload = config.copy(userId = session.workshopOwnerId, id = null, createdAt = null, updatedAt = null)
            if (config.id.isNullOrBlank()) {
                api.createConfiguracaoOficina(bearer, payload)
            } else {
                api.updateConfiguracaoOficina(bearer, "eq.${config.id}", payload)
            }
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("Configuração salva sem retorno.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    // =========================================================================
    // FUNCIONÁRIOS / EQUIPE OPERACIONAL (Somente Admin)
    // =========================================================================

    suspend fun fetchFuncionarios(): RepoResult<List<FuncionarioDto>> =
        withAuth { _, bearer -> api.getFuncionarios(bearer) }

    suspend fun createFuncionarioAccount(
        nome: String,
        email: String,
        password: String,
        cargo: String,
        comissaoPercentual: Double = 50.0
    ): RepoResult<FuncionarioDto> {
        checkConfigured()?.let { return RepoResult.Error(it) }
        val session = getValidSession()
            ?: return RepoResult.Error("Sessão expirada. Faça login novamente.", unauthorized = true)
        if (!session.isAdmin) {
            return RepoResult.Error("Apenas o proprietário pode gerenciar funcionários.")
        }
        val currentList = try {
            val res = api.getFuncionarios("Bearer ${session.accessToken}")
            if (res.isSuccessful) res.body().orEmpty() else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        if (currentList.size >= 4) {
            return RepoResult.Error("Limite máximo de 4 acessos de funcionários por oficina atingido.")
        }

        val cleanEmail = email.trim().lowercase()
        val cleanNome = nome.trim()
        val cleanCargo = cargo.trim().ifBlank { "Técnico Operacional" }
        val cleanComissao = comissaoPercentual.coerceIn(0.0, 100.0)

        var createdUserId: String? = null
        if (password.length >= 6) {
            try {
                val signUpRes = api.signUp(
                    SupabaseSignUpRequest(
                        email = cleanEmail,
                        password = password,
                        data = mapOf(
                            "full_name" to cleanNome,
                            "role" to UserRole.FUNCIONARIO.code,
                            "owner_id" to session.workshopOwnerId
                        )
                    )
                )
                if (signUpRes.isSuccessful) {
                    createdUserId = signUpRes.body()?.user?.id
                }
            } catch (_: Exception) {
            }
        }

        return withAuth { _, bearer ->
            api.createFuncionario(
                bearer,
                FuncionarioDto(
                    ownerId = session.workshopOwnerId,
                    funcionarioUserId = createdUserId,
                    nome = cleanNome,
                    email = cleanEmail,
                    cargo = cleanCargo,
                    comissaoPercentual = cleanComissao,
                    ativo = true
                )
            )
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("Funcionário salvo sem retorno do servidor.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    suspend fun updateFuncionarioPerfil(
        func: FuncionarioDto,
        cargo: String,
        comissaoPercentual: Double
    ): RepoResult<FuncionarioDto> {
        val id = func.id ?: return RepoResult.Error("ID de funcionário inválido.")
        val cleanCargo = cargo.trim().ifBlank { func.cargo.ifBlank { "Técnico Operacional" } }
        val cleanComissao = comissaoPercentual.coerceIn(0.0, 100.0)
        return withAuth { session, bearer ->
            if (!session.isAdmin) {
                throw SecurityException("Apenas o proprietário pode alterar função e comissão.")
            }
            api.updateFuncionario(
                bearerToken = bearer,
                idFilter = "eq.$id",
                funcionario = mapOf(
                    "cargo" to cleanCargo,
                    "comissao_percentual" to cleanComissao
                )
            )
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("Não foi possível atualizar função e comissão.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    suspend fun toggleFuncionarioAtivo(func: FuncionarioDto): RepoResult<FuncionarioDto> {
        val id = func.id ?: return RepoResult.Error("ID de funcionário inválido.")
        return withAuth { _, bearer ->
            api.updateFuncionario(
                bearerToken = bearer,
                idFilter = "eq.$id",
                funcionario = mapOf("ativo" to !func.ativo)
            )
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("Não foi possível atualizar status do funcionário.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    suspend fun deleteFuncionario(id: String): RepoResult<Unit> =
        withAuth { _, bearer -> api.deleteFuncionario(bearer, "eq.$id") }

    // =========================================================================
    // METAS DIÁRIAS (Somente Admin)
    // =========================================================================

    suspend fun fetchMetaDiaria(): RepoResult<MetaDiariaDto?> {
        return when (val res = withAuth { _, bearer -> api.getMetaDiaria(bearer) }) {
            is RepoResult.Success -> RepoResult.Success(res.data.firstOrNull())
            is RepoResult.Error -> res
        }
    }

    suspend fun saveMetaDiaria(meta: MetaDiariaDto): RepoResult<MetaDiariaDto> {
        return withAuth { session, bearer ->
            val payload = meta.copy(userId = session.workshopOwnerId, id = null, createdAt = null, updatedAt = null)
            if (meta.id.isNullOrBlank()) {
                api.createMetaDiaria(bearer, payload)
            } else {
                api.updateMetaDiaria(bearer, "eq.${meta.id}", payload)
            }
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("Meta salva sem retorno.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    // =========================================================================
    // COTAÇÕES E COMPARATIVO DE PREÇOS
    // =========================================================================

    suspend fun fetchCotacoes(): RepoResult<List<CotacaoPrecoDto>> =
        withAuth { _, bearer -> api.getCotacoes(bearer) }

    suspend fun saveCotacao(cotacao: CotacaoPrecoDto): RepoResult<CotacaoPrecoDto> {
        return withAuth { session, bearer ->
            val payload = cotacao.copy(userId = session.workshopOwnerId, id = null, createdAt = null, updatedAt = null)
            if (cotacao.id.isNullOrBlank()) {
                api.createCotacao(bearer, payload)
            } else {
                api.updateCotacao(bearer, "eq.${cotacao.id}", payload)
            }
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("Cotação salva sem retorno.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    suspend fun deleteCotacao(id: String): RepoResult<Unit> =
        withAuth { _, bearer -> api.deleteCotacao(bearer, "eq.$id") }

    // =========================================================================
    // PEDIDOS DE PEÇAS FALTANTES / PARA CONCLUIR OS
    // =========================================================================

    suspend fun fetchPedidosPecas(): RepoResult<List<PedidoPecaDto>> =
        withAuth { _, bearer -> api.getPedidosPecas(bearer) }

    suspend fun savePedidoPeca(pedido: PedidoPecaDto): RepoResult<PedidoPecaDto> {
        return withAuth { session, bearer ->
            val payload = pedido.copy(
                userId = session.workshopOwnerId,
                solicitadoPor = pedido.solicitadoPor.ifBlank { session.fullName }
            )
            if (pedido.id.isNullOrBlank()) {
                api.createPedidoPeca(bearer, payload.copy(id = null, createdAt = null, updatedAt = null))
            } else {
                api.updatePedidoPeca(bearer, "eq.${pedido.id}", payload.copy(id = null, createdAt = null, updatedAt = null))
            }
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("Pedido de peça salvo sem retorno.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    suspend fun deletePedidoPeca(id: String): RepoResult<Unit> =
        withAuth { _, bearer -> api.deletePedidoPeca(bearer, "eq.$id") }

    // =========================================================================
    // VENDAS / PDV INTEGRADO (Estoque + Financeiro)
    // =========================================================================

    suspend fun fetchVendas(): RepoResult<List<VendaDto>> =
        withAuth { _, bearer -> api.getVendas(bearer) }

    suspend fun registrarVenda(request: RegistrarVendaRpcRequest): RepoResult<VendaDto> {
        return withAuth { session, bearer ->
            val sanitized = request.copy(
                pClienteNome = request.pClienteNome.trim().ifBlank { "Consumidor Final" },
                pClienteTelefone = request.pClienteTelefone.trim(),
                pVendedorNome = request.pVendedorNome.trim().ifBlank { session.fullName },
                pFormaPagamento = request.pFormaPagamento.trim().ifBlank { "PIX" },
                pDesconto = request.pDesconto.coerceAtLeast(0.0),
                pObservacoes = request.pObservacoes.trim()
            )
            api.registrarVendaCompleta(bearer, sanitized)
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("Venda registrada sem retorno do servidor.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    suspend fun cancelarVenda(vendaId: String): RepoResult<VendaDto> {
        return withAuth { _, bearer ->
            api.cancelarVendaCompleta(bearer, CancelarVendaRpcRequest(pVendaId = vendaId))
        }.let { result ->
            when (result) {
                is RepoResult.Success -> {
                    val saved = result.data.firstOrNull()
                    if (saved != null) RepoResult.Success(saved)
                    else RepoResult.Error("Cancelamento concluído sem retorno do servidor.")
                }
                is RepoResult.Error -> result
            }
        }
    }

    private fun parseError(response: Response<*>, fallback: String): String {
        val raw = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        if (!raw.isNullOrBlank()) {
            try {
                val parsed = errorAdapter.fromJson(raw)
                val msg = parsed?.errorDescription ?: parsed?.message ?: parsed?.msg ?: parsed?.error
                if (!msg.isNullOrBlank()) {
                    return translateSupabaseMessage(msg)
                }
            } catch (_: Exception) {
            }
        }
        return "$fallback (HTTP ${response.code()})"
    }

    private fun translateSupabaseMessage(msg: String): String {
        return when {
            msg.contains("Limite máximo de 4", ignoreCase = true) ->
                "Limite máximo de 4 acessos de funcionários por oficina atingido."
            msg.contains("registrar_venda_completa", ignoreCase = true) ||
                msg.contains("cancelar_venda_completa", ignoreCase = true) ->
                "Módulo de Vendas não encontrado no Supabase. Execute o arquivo supabase_schema.sql atualizado no SQL Editor."
            msg.contains("Invalid login credentials", ignoreCase = true) ->
                "E-mail ou senha inválidos."
            msg.contains("Email not confirmed", ignoreCase = true) ->
                "Confirme seu e-mail antes de fazer login."
            msg.contains("User already registered", ignoreCase = true) ->
                "Este e-mail já está cadastrado."
            msg.contains("Password should be at least", ignoreCase = true) ->
                "A senha deve ter pelo menos 6 caracteres."
            msg.contains("relation", ignoreCase = true) && msg.contains("does not exist", ignoreCase = true) ->
                "Tabelas não encontradas no Supabase. Execute o arquivo supabase_schema.sql no SQL Editor."
            msg.contains("Bucket not found", ignoreCase = true) ->
                "Bucket 'tecsec-midias' não encontrado no Supabase Storage. Execute o supabase_schema.sql atualizado."
            else -> msg
        }
    }
}

package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.model.ClienteDto
import com.example.data.model.ConfiguracaoOficinaDto
import com.example.data.model.CotacaoPrecoDto
import com.example.data.model.EscopoFinanceiro
import com.example.data.model.FuncionarioDto
import com.example.data.model.MetaDiariaDto
import com.example.data.model.OrdemServicoDto
import com.example.data.model.PecaEstoqueDto
import com.example.data.model.PedidoPecaDto
import com.example.data.model.PedidoPecaStatus
import com.example.data.model.RegistrarVendaRpcRequest
import com.example.data.model.StatusFinanceiro
import com.example.data.model.StatusOS
import com.example.data.model.StatusVenda
import com.example.data.model.TipoTransacao
import com.example.data.model.TransacaoFinanceiraDto
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.model.VendaDto
import com.example.data.repository.AiRepository
import com.example.data.repository.PlaceholderAiRepository
import com.example.data.repository.RepoResult
import com.example.data.repository.WorkshopRepository
import com.example.util.AlertCalculator
import com.example.util.Formatters
import com.example.util.ImageCompressionPreset
import com.example.util.ImageCompressor
import com.example.util.WorkshopAlerts
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Checking : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val session: UserSession) : AuthState
}

enum class OsViewMode(val label: String) {
    LISTA("Lista"),
    KANBAN("Kanban")
}

data class WorkshopUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val authLoading: Boolean = false,
    val authError: String? = null,
    val authInfo: String? = null,
    val snackbarMessage: String? = null,
    val userRole: UserRole = UserRole.ADMIN,
    val workshopOwnerId: String = "",
    val currentUserName: String = "",
    val currentUserEmail: String = "",
    val compressionPreset: ImageCompressionPreset = ImageCompressionPreset.EQUILIBRADO,
    val clientes: List<ClienteDto> = emptyList(),
    val ordens: List<OrdemServicoDto> = emptyList(),
    val pecas: List<PecaEstoqueDto> = emptyList(),
    val transacoes: List<TransacaoFinanceiraDto> = emptyList(),
    val configuracaoOficina: ConfiguracaoOficinaDto = ConfiguracaoOficinaDto(),
    val metaDiaria: MetaDiariaDto = MetaDiariaDto(),
    val cotacoes: List<CotacaoPrecoDto> = emptyList(),
    val funcionarios: List<FuncionarioDto> = emptyList(),
    val pedidosPecas: List<PedidoPecaDto> = emptyList(),
    val vendas: List<VendaDto> = emptyList(),
    val aiGenerating: Boolean = false,
    val aiPlaceholderNotice: Boolean = false,
    val osViewMode: OsViewMode = OsViewMode.LISTA,
    val financeContasFixasTabRequested: Boolean = false,
    val financeOverdue30dFilterRequested: Boolean = false,
    val osOverdue30dFilterRequested: Boolean = false
) {
    val isAdmin: Boolean get() = userRole == UserRole.ADMIN
    val canAccessFinanceiro: Boolean get() = userRole == UserRole.ADMIN
    val canAddMoreFuncionarios: Boolean get() = funcionarios.size < 4

    val alerts: WorkshopAlerts
        get() = AlertCalculator.compute(
            ordens = ordens,
            transacoes = transacoes,
            pedidosPecas = pedidosPecas,
            pecas = pecas,
            canAccessFinanceiro = canAccessFinanceiro
        )

    val osAbertasCount: Int
        get() = ordens.count { it.statusEnum != StatusOS.ENTREGUE }

    val osProntasCount: Int
        get() = ordens.count { it.statusEnum == StatusOS.PRONTA }

    val pecasEstoqueBaixo: List<PecaEstoqueDto>
        get() = pecas.filter { it.isEstoqueBaixo }

    val faturamentoLojaRecebido: Double
        get() = transacoes
            .filter { it.escopoEnum == EscopoFinanceiro.LOJA && it.tipoEnum == TipoTransacao.RECEBER && it.statusEnum == StatusFinanceiro.PAGO }
            .sumOf { it.valor }

    val despesasLojaPagas: Double
        get() = transacoes
            .filter { it.escopoEnum == EscopoFinanceiro.LOJA && it.tipoEnum == TipoTransacao.PAGAR && it.statusEnum == StatusFinanceiro.PAGO }
            .sumOf { it.valor }

    val saldoLoja: Double
        get() = faturamentoLojaRecebido - despesasLojaPagas

    val aReceberPendenteLoja: Double
        get() = transacoes
            .filter { it.escopoEnum == EscopoFinanceiro.LOJA && it.tipoEnum == TipoTransacao.RECEBER && it.statusEnum == StatusFinanceiro.PENDENTE }
            .sumOf { it.valor }

    val aPagarPendenteLoja: Double
        get() = transacoes
            .filter { it.escopoEnum == EscopoFinanceiro.LOJA && it.tipoEnum == TipoTransacao.PAGAR && it.statusEnum == StatusFinanceiro.PENDENTE }
            .sumOf { it.valor }

    val gastosPessoaisPagos: Double
        get() = transacoes
            .filter { it.escopoEnum == EscopoFinanceiro.PESSOAL && it.tipoEnum == TipoTransacao.PAGAR && it.statusEnum == StatusFinanceiro.PAGO }
            .sumOf { it.valor }

    val gastosPessoaisPendentes: Double
        get() = transacoes
            .filter { it.escopoEnum == EscopoFinanceiro.PESSOAL && it.tipoEnum == TipoTransacao.PAGAR && it.statusEnum == StatusFinanceiro.PENDENTE }
            .sumOf { it.valor }

    val ticketMedioRealOs: Double
        get() {
            val valued = ordens.filter { it.valorTotal > 0.0 }
            return if (valued.isEmpty()) 0.0 else valued.sumOf { it.valorTotal } / valued.size
        }

    val nextNumeroOs: Int
        get() = (ordens.maxOfOrNull { it.numeroOs } ?: 1000) + 1

    val totalVendasHoje: Double
        get() {
            val today = Formatters.todayIsoDate()
            return vendas
                .filter { it.statusEnum != StatusVenda.CANCELADA && (it.dataVenda ?: it.createdAt ?: "").startsWith(today) }
                .sumOf { it.valorTotal }
        }

    val totalVendasMes: Double
        get() {
            val monthPrefix = Formatters.todayIsoDate().take(7)
            return vendas
                .filter { it.statusEnum != StatusVenda.CANCELADA && (it.dataVenda ?: it.createdAt ?: "").startsWith(monthPrefix) }
                .sumOf { it.valorTotal }
        }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application.applicationContext)
    private val repository = WorkshopRepository(sessionManager)
    private val aiRepository: AiRepository = PlaceholderAiRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _uiState = MutableStateFlow(WorkshopUiState())
    val uiState: StateFlow<WorkshopUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.compressionPresetFlow.collect { preset ->
                _uiState.update { it.copy(compressionPreset = preset) }
            }
        }
        viewModelScope.launch {
            repository.sessionFlow.collect { session ->
                if (session == null) {
                    _authState.value = AuthState.Unauthenticated
                    val currentPreset = _uiState.value.compressionPreset
                    _uiState.value = WorkshopUiState(compressionPreset = currentPreset)
                } else {
                    val prev = (_authState.value as? AuthState.Authenticated)?.session
                    val changedAccount = prev == null || prev.userId != session.userId || prev.role != session.role
                    _authState.value = AuthState.Authenticated(session)
                    _uiState.update {
                        it.copy(
                            userRole = session.role,
                            workshopOwnerId = session.workshopOwnerId,
                            currentUserName = session.fullName,
                            currentUserEmail = session.email,
                            transacoes = if (session.canAccessFinanceiro) it.transacoes else emptyList()
                        )
                    }
                    if (changedAccount) {
                        refreshAll()
                    }
                }
            }
        }
    }

    fun clearAuthFeedback() {
        _uiState.update { it.copy(authError = null, authInfo = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun setOsViewMode(mode: OsViewMode) {
        _uiState.update { it.copy(osViewMode = mode) }
    }

    fun requestFinanceContasFixasTab() {
        _uiState.update { it.copy(financeContasFixasTabRequested = true, financeOverdue30dFilterRequested = false) }
    }

    fun requestFinanceOverdue30dFilter() {
        _uiState.update { it.copy(financeOverdue30dFilterRequested = true, financeContasFixasTabRequested = false) }
    }

    fun clearFinanceNavRequests() {
        _uiState.update { it.copy(financeContasFixasTabRequested = false, financeOverdue30dFilterRequested = false) }
    }

    fun requestOsOverdue30dFilter() {
        _uiState.update { it.copy(osOverdue30dFilterRequested = true, osViewMode = OsViewMode.LISTA) }
    }

    fun clearOsNavRequests() {
        _uiState.update { it.copy(osOverdue30dFilterRequested = false) }
    }

    fun setCompressionPreset(preset: ImageCompressionPreset) {
        _uiState.update { it.copy(compressionPreset = preset) }
        viewModelScope.launch {
            repository.saveCompressionPreset(preset)
        }
    }

    // =========================================================================
    // AUTH
    // =========================================================================

    fun signIn(email: String, password: String, requestedRole: UserRole = UserRole.ADMIN) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authError = "Informe e-mail e senha para entrar.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, authError = null, authInfo = null) }
            when (val res = repository.signIn(email, password, requestedRole)) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(authLoading = false, authError = null) }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(authLoading = false, authError = res.message) }
                }
            }
        }
    }

    fun signUp(fullName: String, email: String, password: String) {
        if (fullName.isBlank() || email.isBlank() || password.length < 6) {
            _uiState.update {
                it.copy(authError = "Preencha nome, e-mail válido e senha com pelo menos 6 caracteres.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, authError = null, authInfo = null) }
            when (val res = repository.signUp(fullName, email, password)) {
                is RepoResult.Success -> {
                    val infoMsg = if (res.data == "CONFIRM_EMAIL") {
                        "Cadastro criado! Confirme o link enviado ao seu e-mail para entrar."
                    } else null
                    _uiState.update { it.copy(authLoading = false, authInfo = infoMsg) }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(authLoading = false, authError = res.message) }
                }
            }
        }
    }

    fun signUpAsEmployee(fullName: String, email: String, password: String, workshopCode: String) {
        if (fullName.isBlank() || email.isBlank() || password.length < 6 || workshopCode.isBlank()) {
            _uiState.update {
                it.copy(authError = "Informe seu nome, o Código da Oficina, e-mail válido e senha (mín. 6 caracteres).")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, authError = null, authInfo = null) }
            when (val res = repository.signUpAsEmployee(fullName, email, password, workshopCode)) {
                is RepoResult.Success -> {
                    val infoMsg = if (res.data == "CONFIRM_EMAIL") {
                        "Acesso de funcionário criado! Confirme o link enviado ao seu e-mail para entrar."
                    } else null
                    _uiState.update { it.copy(authLoading = false, authInfo = infoMsg) }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(authLoading = false, authError = res.message) }
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }

    // =========================================================================
    // SYNC ALL
    // =========================================================================

    fun refreshAll() {
        val currentSession = (_authState.value as? AuthState.Authenticated)?.session
        val canFinance = currentSession?.canAccessFinanceiro == true

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val clientesDef = async { repository.fetchClientes() }
            val ordensDef = async { repository.fetchOrdensServico() }
            val pecasDef = async { repository.fetchPecas() }
            val configDef = async { repository.fetchConfiguracaoOficina() }
            val cotacoesDef = async { repository.fetchCotacoes() }
            val pedidosPecasDef = async { repository.fetchPedidosPecas() }
            val vendasDef = async { repository.fetchVendas() }

            val transacoesDef = if (canFinance) async { repository.fetchTransacoes() } else null
            val metaDef = if (canFinance) async { repository.fetchMetaDiaria() } else null
            val funcDef = async { repository.fetchFuncionarios() }

            val clientesRes = clientesDef.await()
            val ordensRes = ordensDef.await()
            val pecasRes = pecasDef.await()
            val configRes = configDef.await()
            val cotacoesRes = cotacoesDef.await()
            val pedidosPecasRes = pedidosPecasDef.await()
            val vendasRes = vendasDef.await()
            val transacoesRes = transacoesDef?.await()
            val metaRes = metaDef?.await()
            val funcRes = funcDef.await()

            val coreResults = buildList {
                add(clientesRes)
                add(ordensRes)
                add(pecasRes)
                add(configRes)
                add(cotacoesRes)
                add(pedidosPecasRes)
                add(vendasRes)
                add(funcRes)
                if (transacoesRes != null) add(transacoesRes)
                if (metaRes != null) add(metaRes)
            }
            val firstError = coreResults
                .filterIsInstance<RepoResult.Error>()
                .firstOrNull()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    clientes = (clientesRes as? RepoResult.Success)?.data ?: state.clientes,
                    ordens = (ordensRes as? RepoResult.Success)?.data ?: state.ordens,
                    pecas = (pecasRes as? RepoResult.Success)?.data ?: state.pecas,
                    transacoes = if (canFinance) {
                        (transacoesRes as? RepoResult.Success)?.data ?: state.transacoes
                    } else {
                        emptyList()
                    },
                    configuracaoOficina = (configRes as? RepoResult.Success)?.data ?: state.configuracaoOficina,
                    metaDiaria = if (canFinance) {
                        (metaRes as? RepoResult.Success)?.data ?: state.metaDiaria
                    } else {
                        MetaDiariaDto()
                    },
                    cotacoes = (cotacoesRes as? RepoResult.Success)?.data ?: state.cotacoes,
                    pedidosPecas = (pedidosPecasRes as? RepoResult.Success)?.data ?: state.pedidosPecas,
                    vendas = (vendasRes as? RepoResult.Success)?.data ?: state.vendas,
                    funcionarios = (funcRes as? RepoResult.Success)?.data ?: state.funcionarios,
                    snackbarMessage = firstError?.message ?: state.snackbarMessage
                )
            }
        }
    }

    // =========================================================================
    // CLIENTES
    // =========================================================================

    fun saveCliente(cliente: ClienteDto, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.saveCliente(cliente)) {
                is RepoResult.Success -> {
                    val updatedList = if (cliente.id.isNullOrBlank()) {
                        (_uiState.value.clientes + res.data).sortedBy { it.nome.lowercase() }
                    } else {
                        _uiState.value.clientes.map { if (it.id == res.data.id) res.data else it }
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            clientes = updatedList,
                            snackbarMessage = "Cliente salvo no Supabase."
                        )
                    }
                    onSaved()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    fun deleteCliente(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.deleteCliente(id)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            clientes = it.clientes.filterNot { c -> c.id == id },
                            snackbarMessage = "Cliente removido."
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    // =========================================================================
    // ORDENS DE SERVIÇO
    // =========================================================================

    fun saveOrdemServico(
        os: OrdemServicoDto,
        localPhotoUris: List<Uri> = emptyList(),
        onSaved: (OrdemServicoDto) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val uploadedUrls = mutableListOf<String>()
            var totalOriginalBytes = 0L
            var totalCompressedBytes = 0L
            val context = getApplication<Application>().applicationContext
            val preset = _uiState.value.compressionPreset

            for (uri in localPhotoUris) {
                when (val uploadRes = repository.uploadImageToSupabase(context, uri, "os", preset)) {
                    is RepoResult.Success -> {
                        uploadedUrls.add(uploadRes.data.publicUrl)
                        totalOriginalBytes += uploadRes.data.originalBytes
                        totalCompressedBytes += uploadRes.data.compressedBytes
                    }
                    is RepoResult.Error -> {
                        _uiState.update { it.copy(isSubmitting = false, snackbarMessage = uploadRes.message) }
                        return@launch
                    }
                }
            }

            val currentSession = (_authState.value as? AuthState.Authenticated)?.session
            val matchingFunc = com.example.util.CommissionCalculator.findFuncionarioForSession(
                session = currentSession,
                funcionarios = _uiState.value.funcionarios
            )
            val attributedOs = if (os.id.isNullOrBlank()) {
                val creator = os.criadoPorNome.ifBlank { currentSession?.fullName ?: "" }
                if (os.tecnicoNome.isBlank() && currentSession != null) {
                    os.copy(
                        tecnicoId = matchingFunc?.id,
                        tecnicoNome = matchingFunc?.nome ?: currentSession.fullName,
                        tecnicoEmail = matchingFunc?.email ?: currentSession.email,
                        criadoPorNome = creator,
                        comissaoPercentual = if (os.comissaoPercentual > 0.0) os.comissaoPercentual else (matchingFunc?.comissaoPercentual ?: 0.0)
                    )
                } else {
                    os.copy(criadoPorNome = creator)
                }
            } else {
                os
            }

            val withNumberAndPhotos = (if (attributedOs.id.isNullOrBlank()) {
                attributedOs.copy(numeroOs = _uiState.value.nextNumeroOs)
            } else attributedOs).copy(fotosUrls = attributedOs.fotosUrls + uploadedUrls)

            when (val res = repository.saveOrdemServico(withNumberAndPhotos)) {
                is RepoResult.Success -> {
                    val updatedList = if (os.id.isNullOrBlank()) {
                        listOf(res.data) + _uiState.value.ordens
                    } else {
                        _uiState.value.ordens.map { if (it.id == res.data.id) res.data else it }
                    }
                    val compressionNote = if (uploadedUrls.isNotEmpty() && totalOriginalBytes > 0L) {
                        val savedPct = (((totalOriginalBytes - totalCompressedBytes).toDouble() / totalOriginalBytes) * 100)
                            .toInt().coerceIn(0, 99)
                        " • Fotos WebP: ${ImageCompressor.formatBytes(totalOriginalBytes)} → ${ImageCompressor.formatBytes(totalCompressedBytes)} (-$savedPct%)"
                    } else ""
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            ordens = updatedList.sortedByDescending { o -> o.numeroOs },
                            snackbarMessage = "OS #${res.data.numeroOs} salva$compressionNote"
                        )
                    }
                    onSaved(res.data)
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    fun updateOsStatus(os: OrdemServicoDto, newStatus: StatusOS) {
        val updated = os.copy(
            status = newStatus.code,
            dataSaida = if (newStatus == StatusOS.ENTREGUE && os.dataSaida.isNullOrBlank()) {
                Formatters.nowIsoTimestamp()
            } else os.dataSaida
        )
        saveOrdemServico(updated)
    }

    fun deleteOrdemServico(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.deleteOrdemServico(id)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            ordens = it.ordens.filterNot { o -> o.id == id },
                            snackbarMessage = "Ordem de Serviço excluída."
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    fun lancarOsNoFinanceiro(os: OrdemServicoDto) {
        if (!_uiState.value.canAccessFinanceiro) {
            _uiState.update { it.copy(snackbarMessage = "Acesso ao módulo Financeiro restrito ao proprietário.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.lancarOsNoFinanceiro(os)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            transacoes = listOf(res.data) + it.transacoes,
                            snackbarMessage = "OS #${os.numeroOs} lançada no Financeiro (Loja)."
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    // =========================================================================
    // ESTOQUE DE PEÇAS
    // =========================================================================

    fun savePeca(
        peca: PecaEstoqueDto,
        localPhotoUri: Uri? = null,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            var finalFotoUrl = peca.fotoUrl
            var compressionNote = ""
            if (localPhotoUri != null) {
                val context = getApplication<Application>().applicationContext
                val preset = _uiState.value.compressionPreset
                when (val uploadRes = repository.uploadImageToSupabase(context, localPhotoUri, "pecas", preset)) {
                    is RepoResult.Success -> {
                        finalFotoUrl = uploadRes.data.publicUrl
                        val orig = uploadRes.data.originalBytes
                        val comp = uploadRes.data.compressedBytes.toLong()
                        if (orig > 0L) {
                            val pct = (((orig - comp).toDouble() / orig) * 100).toInt().coerceIn(0, 99)
                            compressionNote = " • WebP: ${ImageCompressor.formatBytes(orig)} → ${ImageCompressor.formatBytes(comp)} (-$pct%)"
                        }
                    }
                    is RepoResult.Error -> {
                        _uiState.update { it.copy(isSubmitting = false, snackbarMessage = uploadRes.message) }
                        return@launch
                    }
                }
            }

            when (val res = repository.savePeca(peca.copy(fotoUrl = finalFotoUrl))) {
                is RepoResult.Success -> {
                    val updatedList = if (peca.id.isNullOrBlank()) {
                        (_uiState.value.pecas + res.data).sortedBy { it.nome.lowercase() }
                    } else {
                        _uiState.value.pecas.map { if (it.id == res.data.id) res.data else it }
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            pecas = updatedList,
                            snackbarMessage = "Peça salva no estoque$compressionNote"
                        )
                    }
                    onSaved()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    fun adjustPecaQuantity(peca: PecaEstoqueDto, delta: Int) {
        val nextQty = (peca.quantidade + delta).coerceAtLeast(0)
        if (nextQty == peca.quantidade) return
        savePeca(peca.copy(quantidade = nextQty))
    }

    fun deletePeca(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.deletePeca(id)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            pecas = it.pecas.filterNot { p -> p.id == id },
                            snackbarMessage = "Peça removida do estoque."
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    // =========================================================================
    // FINANCEIRO (Somente Admin)
    // =========================================================================

    fun saveTransacao(transacao: TransacaoFinanceiraDto, onSaved: () -> Unit = {}) {
        if (!_uiState.value.canAccessFinanceiro) {
            _uiState.update { it.copy(snackbarMessage = "Acesso ao módulo Financeiro restrito ao proprietário.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.saveTransacao(transacao)) {
                is RepoResult.Success -> {
                    val updatedList = if (transacao.id.isNullOrBlank()) {
                        listOf(res.data) + _uiState.value.transacoes
                    } else {
                        _uiState.value.transacoes.map { if (it.id == res.data.id) res.data else it }
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            transacoes = updatedList,
                            snackbarMessage = "Lançamento financeiro salvo."
                        )
                    }
                    onSaved()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    fun toggleTransacaoStatus(tx: TransacaoFinanceiraDto) {
        val next = if (tx.statusEnum == StatusFinanceiro.PAGO) {
            StatusFinanceiro.PENDENTE
        } else {
            StatusFinanceiro.PAGO
        }
        saveTransacao(
            tx.copy(
                status = next.code,
                dataPagamento = if (next == StatusFinanceiro.PAGO) Formatters.todayIsoDate() else null
            )
        )
    }

    fun deleteTransacao(id: String) {
        if (!_uiState.value.canAccessFinanceiro) {
            _uiState.update { it.copy(snackbarMessage = "Acesso ao módulo Financeiro restrito ao proprietário.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.deleteTransacao(id)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            transacoes = it.transacoes.filterNot { t -> t.id == id },
                            snackbarMessage = "Lançamento excluído."
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    // =========================================================================
    // IA PARA DESCRIÇÃO DE COMPONENTES (PLACEHOLDER / FIREBASE DECLINED)
    // =========================================================================

    fun generateComponentDescription(
        nome: String,
        categoria: String,
        codigoSku: String,
        onGenerated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(aiGenerating = true) }
            val res = aiRepository.generateComponentDescription(
                nome = nome,
                categoria = categoria,
                codigoSku = codigoSku
            )
            _uiState.update {
                it.copy(
                    aiGenerating = false,
                    aiPlaceholderNotice = res.isPlaceholder
                )
            }
            onGenerated(res.text)
        }
    }

    // =========================================================================
    // CONFIGURAÇÕES DA OFICINA & EQUIPE DE FUNCIONÁRIOS (Somente Admin)
    // =========================================================================

    fun saveConfiguracaoOficina(config: ConfiguracaoOficinaDto, onSaved: () -> Unit = {}) {
        if (!_uiState.value.isAdmin) {
            _uiState.update { it.copy(snackbarMessage = "Apenas o proprietário pode alterar configurações da loja.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.saveConfiguracaoOficina(config)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            configuracaoOficina = res.data,
                            snackbarMessage = "Dados da loja e OS salvos."
                        )
                    }
                    onSaved()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    fun createFuncionario(
        nome: String,
        email: String,
        password: String,
        cargo: String,
        comissaoPercentual: Double = 50.0,
        onSaved: () -> Unit = {}
    ) {
        if (!_uiState.value.isAdmin) return
        if (_uiState.value.funcionarios.size >= 4) {
            _uiState.update {
                it.copy(snackbarMessage = "Limite máximo de 4 acessos de funcionários por oficina atingido.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.createFuncionarioAccount(nome, email, password, cargo, comissaoPercentual)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            funcionarios = (it.funcionarios + res.data).sortedBy { f -> f.nome.lowercase() },
                            snackbarMessage = "Funcionário ${res.data.nome} cadastrado (${res.data.comissaoPercentual.toInt()}% mão de obra)."
                        )
                    }
                    onSaved()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    fun updateFuncionarioPerfil(
        func: FuncionarioDto,
        cargo: String,
        comissaoPercentual: Double,
        onSaved: () -> Unit = {}
    ) {
        if (!_uiState.value.isAdmin) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.updateFuncionarioPerfil(func, cargo, comissaoPercentual)) {
                is RepoResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            funcionarios = state.funcionarios.map { if (it.id == res.data.id) res.data else it },
                            snackbarMessage = "${res.data.nome} atualizado: ${res.data.cargo} • ${res.data.comissaoPercentual.toInt()}% mão de obra."
                        )
                    }
                    onSaved()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    fun toggleFuncionarioAtivo(func: FuncionarioDto) {
        if (!_uiState.value.isAdmin) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.toggleFuncionarioAtivo(func)) {
                is RepoResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            funcionarios = state.funcionarios.map { if (it.id == res.data.id) res.data else it },
                            snackbarMessage = if (res.data.ativo) "Funcionário reativado." else "Acesso do funcionário desativado."
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    fun deleteFuncionario(id: String) {
        if (!_uiState.value.isAdmin) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.deleteFuncionario(id)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            funcionarios = it.funcionarios.filterNot { f -> f.id == id },
                            snackbarMessage = "Funcionário removido da equipe."
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    // =========================================================================
    // METAS DIÁRIAS (Somente Admin)
    // =========================================================================

    fun saveMetaDiaria(meta: MetaDiariaDto, onSaved: () -> Unit = {}) {
        if (!_uiState.value.canAccessFinanceiro) {
            _uiState.update { it.copy(snackbarMessage = "Acesso restrito ao proprietário.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.saveMetaDiaria(meta)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            metaDiaria = res.data,
                            snackbarMessage = "Meta diária atualizada no Supabase."
                        )
                    }
                    onSaved()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    // =========================================================================
    // COTAÇÕES E COMPARATIVO DE PREÇOS
    // =========================================================================

    fun saveCotacao(cotacao: CotacaoPrecoDto, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.saveCotacao(cotacao)) {
                is RepoResult.Success -> {
                    val updatedList = if (cotacao.id.isNullOrBlank()) {
                        listOf(res.data) + _uiState.value.cotacoes
                    } else {
                        _uiState.value.cotacoes.map { if (it.id == res.data.id) res.data else it }
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            cotacoes = updatedList,
                            snackbarMessage = "Cotação salva para comparativo."
                        )
                    }
                    onSaved()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    fun deleteCotacao(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.deleteCotacao(id)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            cotacoes = it.cotacoes.filterNot { c -> c.id == id },
                            snackbarMessage = "Cotação removida."
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    // =========================================================================
    // PEDIDOS DE PEÇAS FALTANTES / PARA CONCLUIR OS
    // =========================================================================

    fun savePedidoPeca(pedido: PedidoPecaDto, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val currentSession = (_authState.value as? AuthState.Authenticated)?.session
            val enriched = pedido.copy(
                solicitadoPor = pedido.solicitadoPor.ifBlank { currentSession?.fullName ?: "" }
            )
            when (val res = repository.savePedidoPeca(enriched)) {
                is RepoResult.Success -> {
                    val updatedList = if (pedido.id.isNullOrBlank()) {
                        listOf(res.data) + _uiState.value.pedidosPecas
                    } else {
                        _uiState.value.pedidosPecas.map { if (it.id == res.data.id) res.data else it }
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            pedidosPecas = updatedList,
                            snackbarMessage = "Pedido de peça (${res.data.nomePeca}) salvo na lista."
                        )
                    }
                    onSaved()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    fun updatePedidoPecaStatus(pedido: PedidoPecaDto, newStatus: PedidoPecaStatus) {
        savePedidoPeca(pedido.copy(status = newStatus.code))
    }

    fun deletePedidoPeca(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.deletePedidoPeca(id)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            pedidosPecas = it.pedidosPecas.filterNot { p -> p.id == id },
                            snackbarMessage = "Pedido de peça removido."
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    // =========================================================================
    // VENDAS / PDV INTEGRADO (Baixa Automática de Estoque + Financeiro)
    // =========================================================================

    fun registrarVenda(
        request: RegistrarVendaRpcRequest,
        onCompleted: (VendaDto) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.registrarVenda(request)) {
                is RepoResult.Success -> {
                    val saved = res.data
                    val canFinance = _uiState.value.canAccessFinanceiro
                    val pecasDef = async { repository.fetchPecas() }
                    val transacoesDef = if (canFinance) async { repository.fetchTransacoes() } else null
                    val pecasRes = pecasDef.await()
                    val transacoesRes = transacoesDef?.await()

                    _uiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            vendas = listOf(saved) + state.vendas.filterNot { it.id == saved.id },
                            pecas = (pecasRes as? RepoResult.Success)?.data ?: state.pecas,
                            transacoes = if (canFinance) {
                                (transacoesRes as? RepoResult.Success)?.data ?: state.transacoes
                            } else {
                                emptyList()
                            },
                            snackbarMessage = "Venda #${saved.numeroVenda} finalizada! Estoque e financeiro atualizados."
                        )
                    }
                    onCompleted(saved)
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }

    fun cancelarVenda(vendaId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val res = repository.cancelarVenda(vendaId)) {
                is RepoResult.Success -> {
                    val canceled = res.data
                    val canFinance = _uiState.value.canAccessFinanceiro
                    val pecasDef = async { repository.fetchPecas() }
                    val transacoesDef = if (canFinance) async { repository.fetchTransacoes() } else null
                    val pecasRes = pecasDef.await()
                    val transacoesRes = transacoesDef?.await()

                    _uiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            vendas = state.vendas.map { if (it.id == canceled.id) canceled else it },
                            pecas = (pecasRes as? RepoResult.Success)?.data ?: state.pecas,
                            transacoes = if (canFinance) {
                                (transacoesRes as? RepoResult.Success)?.data ?: state.transacoes
                            } else {
                                emptyList()
                            },
                            snackbarMessage = "Venda #${canceled.numeroVenda} cancelada e peças estornadas ao estoque."
                        )
                    }
                    onDone()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, snackbarMessage = res.message) }
                }
            }
        }
    }
}

package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.ui.screens.ClientesScreen
import com.example.ui.screens.ComissoesScreen
import com.example.ui.screens.ComparativoPrecosScreen
import com.example.ui.screens.ConfiguracaoOficinaScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EstoqueScreen
import com.example.ui.screens.FinanceiroScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MetasScreen
import com.example.ui.screens.OrdensServicoScreen
import com.example.ui.screens.PedidosPecasScreen
import com.example.ui.screens.VendasScreen
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.WorkshopUiState
import com.example.util.Formatters
import com.example.util.WorkshopAlerts
import kotlinx.coroutines.launch

object Routes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_CLIENTES = "clientes"
    const val ROUTE_ORDENS_SERVICO = "ordens_servico"
    const val ROUTE_VENDAS = "vendas"
    const val ROUTE_COMISSOES = "comissoes"
    const val ROUTE_PEDIDOS_PECAS = "pedidos_pecas"
    const val ROUTE_ESTOQUE = "estoque"
    const val ROUTE_PRECOS = "precos"
    const val ROUTE_FINANCEIRO = "financeiro"
    const val ROUTE_METAS = "metas"
    const val ROUTE_CONFIG_OFICINA = "config_oficina"
}

private data class DrawerNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String,
    val adminOnly: Boolean = false
)

private fun drawerItemsForRole(isAdmin: Boolean): List<DrawerNavItem> = listOf(
    DrawerNavItem(Routes.ROUTE_DASHBOARD, "Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    DrawerNavItem(Routes.ROUTE_VENDAS, "Vendas (PDV)", Icons.Default.PointOfSale, "nav_vendas"),
    DrawerNavItem(Routes.ROUTE_CLIENTES, "Clientes", Icons.Default.People, "nav_clientes"),
    DrawerNavItem(Routes.ROUTE_ORDENS_SERVICO, "Ordens de Serviço", Icons.Default.Build, "nav_os"),
    DrawerNavItem(
        route = Routes.ROUTE_COMISSOES,
        title = if (isAdmin) "Rendimento & Comissões" else "Meus Ganhos",
        icon = Icons.Default.Payments,
        testTag = "nav_comissoes"
    ),
    DrawerNavItem(Routes.ROUTE_PEDIDOS_PECAS, "Pedido de Peças", Icons.Default.AddShoppingCart, "nav_pedidos_pecas"),
    DrawerNavItem(Routes.ROUTE_ESTOQUE, "Estoque de Peças", Icons.Default.Inventory2, "nav_estoque"),
    DrawerNavItem(Routes.ROUTE_PRECOS, "Busca & Preços", Icons.Default.CompareArrows, "nav_precos"),
    DrawerNavItem(Routes.ROUTE_FINANCEIRO, "Financeiro", Icons.Default.AccountBalanceWallet, "nav_financeiro", adminOnly = true),
    DrawerNavItem(Routes.ROUTE_METAS, "Metas Diárias", Icons.Default.TrendingUp, "nav_metas", adminOnly = true),
    DrawerNavItem(Routes.ROUTE_CONFIG_OFICINA, "Config. da Loja / Equipe", Icons.Default.Store, "nav_config_oficina", adminOnly = true)
)

@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val auth = authState) {
        AuthState.Checking -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AuthState.Unauthenticated -> {
            LoginScreen(
                isLoading = uiState.authLoading,
                errorMessage = uiState.authError,
                infoMessage = uiState.authInfo,
                onSignIn = viewModel::signIn,
                onSignUp = viewModel::signUp,
                onSignUpEmployee = viewModel::signUpAsEmployee,
                onClearMessages = viewModel::clearAuthFeedback
            )
        }
        is AuthState.Authenticated -> {
            AuthenticatedShell(
                session = auth.session,
                uiState = uiState,
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell(
    session: UserSession,
    uiState: WorkshopUiState,
    viewModel: MainViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.ROUTE_DASHBOARD
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAlertsSheet by remember { mutableStateOf(false) }

    val allRoleItems = remember(session.isAdmin) { drawerItemsForRole(session.isAdmin) }
    val visibleDrawerItems = remember(allRoleItems, session.isAdmin) {
        if (session.isAdmin) allRoleItems else allRoleItems.filterNot { it.adminOnly }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    fun navigateTo(route: String) {
        val targetIsAdminOnly = allRoleItems.find { it.route == route }?.adminOnly == true
        if (targetIsAdminOnly && !session.isAdmin) {
            return
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        scope.launch { drawerState.close() }
    }

    val currentTitle = visibleDrawerItems.find { it.route == currentRoute }?.title ?: "RTerp"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 840.dp

        val drawerContent: @Composable () -> Unit = {
            DrawerMenuBody(
                session = session,
                uiState = uiState,
                drawerItems = visibleDrawerItems,
                currentRoute = currentRoute,
                onNavigate = ::navigateTo,
                onSignOut = {
                    scope.launch { drawerState.close() }
                    viewModel.signOut()
                }
            )
        }

        val mainScaffold: @Composable () -> Unit = {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    text = currentTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                if (!isExpandedScreen) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("open_drawer_button")
                                    ) {
                                        Icon(Icons.Default.Menu, contentDescription = "Abrir menu lateral")
                                    }
                                }
                            },
                            actions = {
                                val alertsCount = uiState.alerts.activeAlertsCount
                                IconButton(
                                    onClick = { showAlertsSheet = true },
                                    modifier = Modifier.testTag("alerts_center_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (alertsCount > 0) {
                                                Badge(
                                                    containerColor = StatusDanger,
                                                    contentColor = Color.White
                                                ) {
                                                    Text(alertsCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (alertsCount > 0) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                            contentDescription = "Central de Alertas",
                                            tint = if (alertsCount > 0) StatusDanger else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.refreshAll() },
                                    enabled = !uiState.isLoading,
                                    modifier = Modifier.testTag("sync_data_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Sincronizar com Supabase")
                                }
                            }
                        )
                        if (uiState.isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AppNavHost(
                        navController = navController,
                        session = session,
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigate = ::navigateTo
                    )
                }
            }
        }

        if (isExpandedScreen) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(280.dp)) {
                        drawerContent()
                    }
                },
                content = mainScaffold
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                        drawerContent()
                    }
                },
                content = mainScaffold
            )
        }
    }

    if (showAlertsSheet) {
        AlertsCenterSheet(
            alerts = uiState.alerts,
            canAccessFinanceiro = session.canAccessFinanceiro,
            onDismiss = { showAlertsSheet = false },
            onNavigateToOsOverdue30d = {
                showAlertsSheet = false
                viewModel.requestOsOverdue30dFilter()
                navigateTo(Routes.ROUTE_ORDENS_SERVICO)
            },
            onNavigateToFinanceReceber30d = {
                showAlertsSheet = false
                if (session.canAccessFinanceiro) {
                    viewModel.requestFinanceOverdue30dFilter()
                    navigateTo(Routes.ROUTE_FINANCEIRO)
                }
            },
            onNavigateToFinanceContasFixas = {
                showAlertsSheet = false
                if (session.canAccessFinanceiro) {
                    viewModel.requestFinanceContasFixasTab()
                    navigateTo(Routes.ROUTE_FINANCEIRO)
                }
            },
            onNavigateToPedidosPecas = {
                showAlertsSheet = false
                navigateTo(Routes.ROUTE_PEDIDOS_PECAS)
            },
            onNavigateToEstoque = {
                showAlertsSheet = false
                navigateTo(Routes.ROUTE_ESTOQUE)
            }
        )
    }
}

@Composable
private fun DrawerMenuBody(
    session: UserSession,
    uiState: WorkshopUiState,
    drawerItems: List<DrawerNavItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Cabeçalho RTerp
        Surface(
            color = Color(0xFF040E18),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_rterp_logo),
                        contentDescription = "Logo RTerp",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = uiState.configuracaoOficina.nomeLoja.ifBlank { "RTerp" },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(StatusSuccess)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RTerp • Supabase",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = if (session.role == UserRole.ADMIN) Color(0xFF0369A1) else Color(0xFF0F766E),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (session.role == UserRole.ADMIN) "PROPRIETÁRIO / ADMIN" else "FUNCIONÁRIO • OPERACIONAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("drawer_role_badge")
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = session.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = session.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            drawerItems.forEach { item ->
                val selected = currentRoute == item.route
                val badgeCount = when (item.route) {
                    Routes.ROUTE_ORDENS_SERVICO -> uiState.osAbertasCount
                    Routes.ROUTE_PEDIDOS_PECAS -> uiState.alerts.pedidosPecasPendentes.size
                    Routes.ROUTE_ESTOQUE -> uiState.pecasEstoqueBaixo.size
                    Routes.ROUTE_FINANCEIRO -> uiState.alerts.financeiroAlertsCount
                    else -> 0
                }
                val badgeIsAlert = when (item.route) {
                    Routes.ROUTE_ESTOQUE -> badgeCount > 0
                    Routes.ROUTE_PEDIDOS_PECAS -> badgeCount > 0
                    Routes.ROUTE_FINANCEIRO -> badgeCount > 0
                    Routes.ROUTE_ORDENS_SERVICO -> uiState.alerts.ordensMais30Dias.isNotEmpty()
                    else -> false
                }

                NavigationDrawerItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(item.title) },
                    badge = {
                        if (badgeCount > 0) {
                            Badge(
                                containerColor = if (badgeIsAlert) StatusDanger else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (badgeIsAlert) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Text(badgeCount.toString())
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag(item.testTag)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        NavigationDrawerItem(
            selected = false,
            onClick = onSignOut,
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = StatusDanger
                )
            },
            label = { Text("Sair da Conta", color = StatusDanger) },
            modifier = Modifier
                .padding(NavigationDrawerItemDefaults.ItemPadding)
                .testTag("logout_button")
        )
        Text(
            text = "Desenvolvido por Realtech Soluções",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 4.dp)
                .testTag("drawer_developer_credit")
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun RestrictedAccessView(onBackToDashboard: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Acesso restrito",
                tint = StatusDanger,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Acesso Restrito ao Proprietário",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Contas de funcionário possuem acesso apenas aos módulos operacionais (OS, Clientes e Estoque).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onBackToDashboard) {
                Text("Voltar ao Dashboard")
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    session: UserSession,
    uiState: WorkshopUiState,
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Routes.ROUTE_DASHBOARD
    ) {
        composable(Routes.ROUTE_DASHBOARD) {
            DashboardScreen(
                uiState = uiState,
                onNavigateToOs = { onNavigate(Routes.ROUTE_ORDENS_SERVICO) },
                onNavigateToVendas = { onNavigate(Routes.ROUTE_VENDAS) },
                onNavigateToEstoque = { onNavigate(Routes.ROUTE_ESTOQUE) },
                onNavigateToFinanceiro = {
                    if (session.canAccessFinanceiro) onNavigate(Routes.ROUTE_FINANCEIRO)
                },
                onNavigateToClientes = { onNavigate(Routes.ROUTE_CLIENTES) },
                onNavigateToComissoes = { onNavigate(Routes.ROUTE_COMISSOES) },
                onNavigateToPedidosPecas = { onNavigate(Routes.ROUTE_PEDIDOS_PECAS) },
                onNavigateToOsOverdue30d = {
                    viewModel.requestOsOverdue30dFilter()
                    onNavigate(Routes.ROUTE_ORDENS_SERVICO)
                },
                onNavigateToFinanceReceber30d = {
                    if (session.canAccessFinanceiro) {
                        viewModel.requestFinanceOverdue30dFilter()
                        onNavigate(Routes.ROUTE_FINANCEIRO)
                    }
                },
                onNavigateToFinanceContasFixas = {
                    if (session.canAccessFinanceiro) {
                        viewModel.requestFinanceContasFixasTab()
                        onNavigate(Routes.ROUTE_FINANCEIRO)
                    }
                }
            )
        }
        composable(Routes.ROUTE_VENDAS) {
            VendasScreen(
                vendas = uiState.vendas,
                pecas = uiState.pecas,
                clientes = uiState.clientes,
                funcionarios = uiState.funcionarios,
                storeConfig = uiState.configuracaoOficina,
                currentUserName = session.fullName,
                isAdmin = session.isAdmin,
                isSubmitting = uiState.isSubmitting,
                onRegistrarVenda = viewModel::registrarVenda,
                onCancelarVenda = viewModel::cancelarVenda
            )
        }
        composable(Routes.ROUTE_CLIENTES) {
            ClientesScreen(
                clientes = uiState.clientes,
                ordens = uiState.ordens,
                isSubmitting = uiState.isSubmitting,
                onSaveCliente = viewModel::saveCliente,
                onDeleteCliente = viewModel::deleteCliente
            )
        }
        composable(Routes.ROUTE_ORDENS_SERVICO) {
            OrdensServicoScreen(
                ordens = uiState.ordens,
                clientes = uiState.clientes,
                funcionarios = uiState.funcionarios,
                pecasEstoque = uiState.pecas,
                currentUserName = session.fullName,
                currentUserEmail = session.email,
                isAdmin = session.isAdmin,
                initialFilterOverdue30d = uiState.osOverdue30dFilterRequested,
                onConsumeOsNavRequest = viewModel::clearOsNavRequests,
                storeConfig = uiState.configuracaoOficina,
                canAccessFinanceiro = session.canAccessFinanceiro,
                compressionPreset = uiState.compressionPreset,
                onCompressionPresetChange = viewModel::setCompressionPreset,
                viewMode = uiState.osViewMode,
                isSubmitting = uiState.isSubmitting,
                onViewModeChange = viewModel::setOsViewMode,
                onSaveOs = { os, uris, onSaved ->
                    viewModel.saveOrdemServico(os, uris, onSaved)
                },
                onMoveStatus = viewModel::updateOsStatus,
                onDeleteOs = viewModel::deleteOrdemServico,
                onLancarFinanceiro = viewModel::lancarOsNoFinanceiro,
                onSavePedidoPeca = viewModel::savePedidoPeca
            )
        }
        composable(Routes.ROUTE_COMISSOES) {
            ComissoesScreen(
                session = session,
                ordens = uiState.ordens,
                funcionarios = uiState.funcionarios,
                isSubmitting = uiState.isSubmitting,
                onUpdateFuncionarioPerfil = viewModel::updateFuncionarioPerfil
            )
        }
        composable(Routes.ROUTE_PEDIDOS_PECAS) {
            PedidosPecasScreen(
                pedidos = uiState.pedidosPecas,
                pecasEstoque = uiState.pecas,
                ordens = uiState.ordens,
                currentUserName = session.fullName,
                isSubmitting = uiState.isSubmitting,
                onSavePedido = viewModel::savePedidoPeca,
                onUpdateStatus = viewModel::updatePedidoPecaStatus,
                onDeletePedido = viewModel::deletePedidoPeca
            )
        }
        composable(Routes.ROUTE_ESTOQUE) {
            EstoqueScreen(
                pecas = uiState.pecas,
                ordens = uiState.ordens,
                currentUserName = session.fullName,
                isSubmitting = uiState.isSubmitting,
                isAdmin = session.isAdmin,
                compressionPreset = uiState.compressionPreset,
                onCompressionPresetChange = viewModel::setCompressionPreset,
                aiGenerating = uiState.aiGenerating,
                aiPlaceholderNotice = uiState.aiPlaceholderNotice,
                onSavePeca = { peca, uri, onSaved ->
                    viewModel.savePeca(peca, uri, onSaved)
                },
                onAdjustQuantity = viewModel::adjustPecaQuantity,
                onDeletePeca = viewModel::deletePeca,
                onGenerateAiDescription = viewModel::generateComponentDescription,
                onSavePedidoPeca = viewModel::savePedidoPeca
            )
        }
        composable(Routes.ROUTE_PRECOS) {
            ComparativoPrecosScreen(
                pecas = uiState.pecas,
                cotacoes = uiState.cotacoes,
                isSubmitting = uiState.isSubmitting,
                onSaveCotacao = viewModel::saveCotacao,
                onDeleteCotacao = viewModel::deleteCotacao
            )
        }
        composable(Routes.ROUTE_FINANCEIRO) {
            if (session.canAccessFinanceiro) {
                FinanceiroScreen(
                    transacoes = uiState.transacoes,
                    isSubmitting = uiState.isSubmitting,
                    initialContasFixasTab = uiState.financeContasFixasTabRequested,
                    initialFilterOverdue30d = uiState.financeOverdue30dFilterRequested,
                    onConsumeNavRequests = viewModel::clearFinanceNavRequests,
                    onSaveTransacao = viewModel::saveTransacao,
                    onToggleStatus = viewModel::toggleTransacaoStatus,
                    onDeleteTransacao = viewModel::deleteTransacao
                )
            } else {
                RestrictedAccessView(onBackToDashboard = { onNavigate(Routes.ROUTE_DASHBOARD) })
            }
        }
        composable(Routes.ROUTE_METAS) {
            if (session.canAccessFinanceiro) {
                MetasScreen(
                    uiState = uiState,
                    onSaveMeta = { meta -> viewModel.saveMetaDiaria(meta) }
                )
            } else {
                RestrictedAccessView(onBackToDashboard = { onNavigate(Routes.ROUTE_DASHBOARD) })
            }
        }
        composable(Routes.ROUTE_CONFIG_OFICINA) {
            if (session.isAdmin) {
                ConfiguracaoOficinaScreen(
                    config = uiState.configuracaoOficina,
                    workshopOwnerId = uiState.workshopOwnerId,
                    compressionPreset = uiState.compressionPreset,
                    funcionarios = uiState.funcionarios,
                    isSubmitting = uiState.isSubmitting,
                    onSaveConfig = { cfg -> viewModel.saveConfiguracaoOficina(cfg) },
                    onCompressionPresetChange = viewModel::setCompressionPreset,
                    onCreateFuncionario = viewModel::createFuncionario,
                    onUpdateFuncionarioPerfil = viewModel::updateFuncionarioPerfil,
                    onToggleFuncionarioAtivo = viewModel::toggleFuncionarioAtivo,
                    onDeleteFuncionario = viewModel::deleteFuncionario
                )
            } else {
                RestrictedAccessView(onBackToDashboard = { onNavigate(Routes.ROUTE_DASHBOARD) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertsCenterSheet(
    alerts: WorkshopAlerts,
    canAccessFinanceiro: Boolean,
    onDismiss: () -> Unit,
    onNavigateToOsOverdue30d: () -> Unit,
    onNavigateToFinanceReceber30d: () -> Unit,
    onNavigateToFinanceContasFixas: () -> Unit,
    onNavigateToPedidosPecas: () -> Unit,
    onNavigateToEstoque: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Central de Alertas (${alerts.activeAlertsCount})",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Pendências críticas da oficina e prazos vencidos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (alerts.activeAlertsCount == 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusSuccess
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tudo em dia! Nenhum alerta pendente no momento.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                if (canAccessFinanceiro && alerts.contasReceberMais30Dias.isNotEmpty()) {
                    AlertSheetItemCard(
                        icon = Icons.Default.NotificationsActive,
                        title = "${alerts.contasReceberMais30Dias.size} conta(s) com +30 dias sem receber",
                        detail = "Total em aberto: ${Formatters.formatCurrency(alerts.totalContasReceber30Dias)} • " +
                            alerts.contasReceberMais30Dias.take(2).joinToString(", ") {
                                "${it.transacao.descricao} (${it.diasEmAtraso}d)"
                            },
                        actionLabel = "Ver Recebimentos +30d",
                        containerColor = StatusDangerContainer,
                        accentColor = StatusDanger,
                        onAction = onNavigateToFinanceReceber30d
                    )
                }

                if (canAccessFinanceiro && alerts.contasFixasEmAtraso.isNotEmpty()) {
                    AlertSheetItemCard(
                        icon = Icons.Default.ReceiptLong,
                        title = "${alerts.contasFixasEmAtraso.size} conta(s) fixa(s) em atraso",
                        detail = "Total vencido: ${Formatters.formatCurrency(alerts.totalContasFixasEmAtraso)} • " +
                            alerts.contasFixasEmAtraso.take(2).joinToString(", ") {
                                "${it.transacao.descricao} (${it.diasEmAtraso}d)"
                            },
                        actionLabel = "Ver Contas Fixas",
                        containerColor = StatusDangerContainer,
                        accentColor = StatusDanger,
                        onAction = onNavigateToFinanceContasFixas
                    )
                }

                if (alerts.ordensMais30Dias.isNotEmpty()) {
                    AlertSheetItemCard(
                        icon = Icons.Default.Schedule,
                        title = "${alerts.ordensMais30Dias.size} OS com +30 dias sem conclusão",
                        detail = alerts.ordensMais30Dias.take(3).joinToString(", ") {
                            "#${it.os.numeroOs} ${it.os.equipamento} (${it.diasEmAberto}d)"
                        },
                        actionLabel = "Ver OS +30d",
                        containerColor = StatusDangerContainer,
                        accentColor = StatusDanger,
                        onAction = onNavigateToOsOverdue30d
                    )
                }

                if (alerts.pedidosPecasPendentes.isNotEmpty()) {
                    AlertSheetItemCard(
                        icon = Icons.Default.AddShoppingCart,
                        title = "${alerts.pedidosPecasPendentes.size} pedido(s) de peças em aberto",
                        detail = alerts.pedidosPecasPendentes.take(3).joinToString(", ") {
                            "${it.nomePeca} (${it.quantidade} un)"
                        },
                        actionLabel = "Ver Pedido de Peças",
                        containerColor = StatusWarningContainer,
                        accentColor = StatusWarning,
                        onAction = onNavigateToPedidosPecas
                    )
                }

                if (alerts.pecasEstoqueBaixoCount > 0) {
                    AlertSheetItemCard(
                        icon = Icons.Default.Warning,
                        title = "${alerts.pecasEstoqueBaixoCount} peça(s) com estoque baixo",
                        detail = "Itens abaixo do estoque mínimo cadastrado.",
                        actionLabel = "Ver Estoque",
                        containerColor = StatusDangerContainer,
                        accentColor = StatusDanger,
                        onAction = onNavigateToEstoque
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AlertSheetItemCard(
    icon: ImageVector,
    title: String,
    detail: String,
    actionLabel: String,
    containerColor: Color,
    accentColor: Color,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, color = accentColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

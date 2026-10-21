package com.shohankhan.hisaabos.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shohankhan.hisaabos.data.db.AccountEntity
import com.shohankhan.hisaabos.data.prefs.AppSettings
import com.shohankhan.hisaabos.di.LocalAppContainer
import com.shohankhan.hisaabos.di.appViewModel
import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.CategoryKind
import com.shohankhan.hisaabos.domain.model.DebtDirection
import com.shohankhan.hisaabos.domain.model.TxType
import com.shohankhan.hisaabos.security.BiometricGate
import com.shohankhan.hisaabos.ui.accounts.AccountDetailScreen
import com.shohankhan.hisaabos.ui.accounts.AccountFormScreen
import com.shohankhan.hisaabos.ui.accounts.AccountsScreen
import com.shohankhan.hisaabos.ui.accounts.AccountsViewModel
import com.shohankhan.hisaabos.ui.backup.BackupScreen
import com.shohankhan.hisaabos.ui.books.BooksScreen
import com.shohankhan.hisaabos.ui.books.BooksViewModel
import com.shohankhan.hisaabos.ui.budgets.BudgetsScreen
import com.shohankhan.hisaabos.ui.categories.CategoriesScreen
import com.shohankhan.hisaabos.ui.categories.CategoriesViewModel
import com.shohankhan.hisaabos.ui.components.HideAmountsProvider
import com.shohankhan.hisaabos.ui.components.LoadingScreen
import com.shohankhan.hisaabos.ui.emi.EmiDetailScreen
import com.shohankhan.hisaabos.ui.emi.EmiScreen
import com.shohankhan.hisaabos.ui.home.HomeScreen
import com.shohankhan.hisaabos.ui.home.HomeViewModel
import com.shohankhan.hisaabos.ui.loans.LoanDetailScreen
import com.shohankhan.hisaabos.ui.loans.LoansScreen
import com.shohankhan.hisaabos.ui.more.MoreScreen
import com.shohankhan.hisaabos.ui.people.PeopleScreen
import com.shohankhan.hisaabos.ui.people.PersonDetailScreen
import com.shohankhan.hisaabos.ui.purchases.PurchaseEntryScreen
import com.shohankhan.hisaabos.ui.reminders.RemindersScreen
import com.shohankhan.hisaabos.ui.reports.ReportsScreen
import com.shohankhan.hisaabos.ui.reports.ReportsViewModel
import com.shohankhan.hisaabos.ui.savings.SavingsScreen
import com.shohankhan.hisaabos.ui.search.SearchScreen
import com.shohankhan.hisaabos.ui.settings.AboutScreen
import com.shohankhan.hisaabos.ui.settings.AppearanceScreen
import com.shohankhan.hisaabos.ui.settings.SecurityScreen
import com.shohankhan.hisaabos.ui.settings.VerifyBooksScreen
import com.shohankhan.hisaabos.ui.shops.ShopDetailScreen
import com.shohankhan.hisaabos.ui.shops.ShopsScreen
import com.shohankhan.hisaabos.ui.text.Bn
import com.shohankhan.hisaabos.ui.theme.Emerald
import com.shohankhan.hisaabos.ui.theme.EmeraldSoft
import com.shohankhan.hisaabos.ui.theme.TextSecondary
import com.shohankhan.hisaabos.ui.transactions.AddTransactionScreen
import com.shohankhan.hisaabos.ui.transactions.AddTransactionViewModel
import com.shohankhan.hisaabos.ui.transactions.TransactionDetailScreen
import com.shohankhan.hisaabos.ui.transactions.TransactionDetailViewModel
import com.shohankhan.hisaabos.ui.transactions.TransactionListScreen
import com.shohankhan.hisaabos.ui.transactions.TransactionListViewModel
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, Bn.navHome, Icons.Outlined.Home),
    Tab(Routes.TRANSACTIONS, Bn.navTransactions, Icons.Outlined.ReceiptLong),
    Tab(Routes.BOOKS, Bn.navBooks, Icons.Outlined.MenuBook),
    Tab(Routes.REPORTS, Bn.navReports, Icons.Outlined.Insights),
    Tab(Routes.MORE, Bn.navMore, Icons.Outlined.MoreHoriz),
)

private fun Long?.toId(): Long? = this?.takeIf { it > 0 }

@Composable
fun MainScaffold(appViewModel: AppViewModel, settings: AppSettings) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    HideAmountsProvider(hidden = settings.hideAmounts, banglaDigits = settings.showBanglaDigits) {
        Scaffold(
            containerColor = com.shohankhan.hisaabos.ui.theme.AppBackground,
            bottomBar = {
                val showBar = currentRoute in tabs.map { it.route }
                if (showBar) {
                    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Emerald,
                                    selectedTextColor = Emerald,
                                    indicatorColor = EmeraldSoft,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                ),
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute == Routes.HOME || currentRoute == Routes.TRANSACTIONS) {
                    FloatingActionButton(
                        onClick = { navController.navigate(Routes.addTx(TxType.EXPENSE)) },
                        containerColor = Emerald,
                        contentColor = Color.White,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = Bn.addTransaction)
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                NavHost(navController = navController, startDestination = Routes.HOME) {
                    composable(Routes.HOME) {
                        val vm = appViewModel { HomeViewModel(it.repository) }
                        val state by vm.state.collectAsStateWithLifecycle()
                        HomeScreen(
                            state = state,
                            onToggleAmounts = { appViewModel.setHideAmounts(!settings.hideAmounts) },
                            onAddIncome = { navController.navigate(Routes.addTx(TxType.INCOME)) },
                            onAddExpense = { navController.navigate(Routes.addTx(TxType.EXPENSE)) },
                            onAddPurchase = { navController.navigate(Routes.purchase()) },
                            onCreditPurchase = { navController.navigate(Routes.addTx(TxType.CREDIT_PURCHASE)) },
                            onBorrow = { navController.navigate(Routes.addTx(TxType.DEBT_BORROWED)) },
                            onLend = { navController.navigate(Routes.addTx(TxType.LENDING)) },
                            onPayment = { navController.navigate(Routes.addTx(TxType.SHOP_PAYMENT)) },
                            onSavings = { navController.navigate(Routes.addTx(TxType.SAVINGS_DEPOSIT)) },
                            onTransfer = { navController.navigate(Routes.addTx(TxType.TRANSFER)) },
                            onAdjust = { navController.navigate(Routes.addTx(TxType.ADJUSTMENT)) },
                            onTransactionClick = { navController.navigate(Routes.txDetail(it)) },
                            onSeeAllTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                            onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                            onNewAccount = { navController.navigate(Routes.accountForm()) },
                            onSearch = { navController.navigate(Routes.SEARCH) },
                            onObligationClick = { obligation ->
                                val id = obligation.linkId
                                when (obligation.linkKind) {
                                    com.shohankhan.hisaabos.domain.model.LinkKind.LOAN -> if (id != null) navController.navigate(Routes.loanDetail(id))
                                    com.shohankhan.hisaabos.domain.model.LinkKind.EMI -> if (id != null) navController.navigate(Routes.emiDetail(id))
                                    com.shohankhan.hisaabos.domain.model.LinkKind.DEBT -> if (id != null) navController.navigate(Routes.PEOPLE)
                                    else -> navController.navigate(Routes.REMINDERS)
                                }
                            },
                            onOpenLoans = { navController.navigate(Routes.LOANS) },
                            onOpenSavings = { navController.navigate(Routes.SAVINGS) },
                        )
                    }

                    composable(Routes.TRANSACTIONS) {
                        val vm = appViewModel { TransactionListViewModel(it.repository) }
                        val state by vm.state.collectAsStateWithLifecycle()
                        TransactionListScreen(
                            state = state,
                            onSearch = vm::setSearch,
                            onApplyFilters = vm::applyFilters,
                            onClearFilters = vm::clearFilters,
                            onSort = vm::setSort,
                            onLoadMore = vm::loadMore,
                            onTransactionClick = { navController.navigate(Routes.txDetail(it)) },
                            onAdd = { navController.navigate(Routes.addTx(TxType.EXPENSE)) },
                        )
                    }

                    composable(Routes.BOOKS) {
                        val vm = appViewModel { BooksViewModel(it.repository) }
                        val snapshot by vm.snapshot.collectAsStateWithLifecycle()
                        BooksScreen(
                            snapshot = snapshot,
                            onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                            onOpenShops = { navController.navigate(Routes.SHOPS) },
                            onOpenPeople = { navController.navigate(Routes.PEOPLE) },
                            onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                            onOpenLoans = { navController.navigate(Routes.LOANS) },
                            onOpenEmi = { navController.navigate(Routes.EMI) },
                            onOpenSavings = { navController.navigate(Routes.SAVINGS) },
                            onOpenBudgets = { navController.navigate(Routes.BUDGETS) },
                        )
                    }

                    composable(Routes.REPORTS) {
                        val vm = appViewModel { ReportsViewModel(it.repository) }
                        val state by vm.state.collectAsStateWithLifecycle()
                        ReportsScreen(
                            state = state,
                            onPeriodChange = vm::setPeriod,
                            onCustomRange = vm::setCustomRange,
                            onExportCsv = { vm.buildCsv() },
                        )
                    }

                    composable(Routes.MORE) {
                        MoreScreen(
                            onAccounts = { navController.navigate(Routes.ACCOUNTS) },
                            onCategories = { navController.navigate(Routes.CATEGORIES) },
                            onPeople = { navController.navigate(Routes.PEOPLE) },
                            onShops = { navController.navigate(Routes.SHOPS) },
                            onLoans = { navController.navigate(Routes.LOANS) },
                            onEmi = { navController.navigate(Routes.EMI) },
                            onSavings = { navController.navigate(Routes.SAVINGS) },
                            onBudgets = { navController.navigate(Routes.BUDGETS) },
                            onRecurring = { navController.navigate(Routes.REMINDERS) },
                            onReminders = { navController.navigate(Routes.REMINDERS) },
                            onBackup = { navController.navigate(Routes.BACKUP) },
                            onSecurity = { navController.navigate(Routes.SECURITY) },
                            onAppearance = { navController.navigate(Routes.APPEARANCE) },
                            onAbout = { navController.navigate(Routes.ABOUT) },
                            onVerify = { navController.navigate(Routes.VERIFY) },
                        )
                    }

                    composable(
                        route = Routes.ADD_TX,
                        arguments = listOf(
                            navArgument("type") { type = NavType.StringType; defaultValue = TxType.EXPENSE.name },
                            navArgument("shopId") { type = NavType.LongType; defaultValue = -1L },
                            navArgument("personId") { type = NavType.LongType; defaultValue = -1L },
                            navArgument("accountId") { type = NavType.LongType; defaultValue = -1L },
                        ),
                    ) { entry ->
                        val type = runCatching {
                            TxType.valueOf(entry.arguments?.getString("type") ?: TxType.EXPENSE.name)
                        }.getOrDefault(TxType.EXPENSE)
                        val shopId = entry.arguments?.getLong("shopId").toId()
                        val personId = entry.arguments?.getLong("personId").toId()
                        val accountId = entry.arguments?.getLong("accountId").toId()
                        val vm = appViewModel(key = "add-${entry.id}") { container ->
                            AddTransactionViewModel(container.repository, type, shopId, personId, accountId, null)
                        }
                        val state = vm.state
                        AddTransactionScreen(
                            state = state,
                            onUpdate = vm::update,
                            onTypeChange = vm::setType,
                            onSave = { vm.save { navController.popBackStack() } },
                            onSaveDespiteDuplicate = { vm.saveDespiteDuplicate { navController.popBackStack() } },
                            onDismissDuplicate = vm::dismissDuplicateWarning,
                            onCreateAccount = vm::createAccount,
                            onCreateCategory = vm::createCategory,
                            onCreateShop = vm::createShop,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = Routes.EDIT_TX,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: return@composable
                        val vm = appViewModel(key = "edit-$id") { container ->
                            AddTransactionViewModel(container.repository, TxType.EXPENSE, null, null, null, id)
                        }
                        val state = vm.state
                        AddTransactionScreen(
                            state = state,
                            onUpdate = vm::update,
                            onTypeChange = vm::setType,
                            onSave = { vm.save { navController.popBackStack() } },
                            onSaveDespiteDuplicate = { vm.saveDespiteDuplicate { navController.popBackStack() } },
                            onDismissDuplicate = vm::dismissDuplicateWarning,
                            onCreateAccount = vm::createAccount,
                            onCreateCategory = vm::createCategory,
                            onCreateShop = vm::createShop,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(
                        route = Routes.TX_DETAIL,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: return@composable
                        val vm = appViewModel(key = "tx-$id") { container -> TransactionDetailViewModel(container.repository, id) }
                        val state by vm.state.collectAsStateWithLifecycle()
                        TransactionDetailScreen(
                            state = state,
                            onBack = { navController.popBackStack() },
                            onEdit = { navController.navigate(Routes.editTx(it)) },
                            onDelete = { done -> vm.delete(done) },
                            onRestore = { vm.restore() },
                            onOpenShop = { navController.navigate(Routes.shopDetail(it)) },
                            onOpenPerson = { navController.navigate(Routes.personDetail(it)) },
                        )
                    }

                    composable(Routes.ACCOUNTS) {
                        val vm = appViewModel { AccountsViewModel(it.repository) }
                        val state by vm.state.collectAsStateWithLifecycle()
                        AccountsScreen(
                            state = state,
                            onBack = { navController.popBackStack() },
                            onAdd = { navController.navigate(Routes.accountForm()) },
                            onAccountClick = { navController.navigate(Routes.accountDetail(it)) },
                            onToggleArchived = vm::toggleArchived,
                        )
                    }

                    composable(
                        route = Routes.ACCOUNT_DETAIL,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: return@composable
                        AccountDetailScreen(
                            accountId = id,
                            onBack = { navController.popBackStack() },
                            onEdit = { navController.navigate(Routes.accountForm(it)) },
                            onOpenTransaction = { navController.navigate(Routes.txDetail(it)) },
                        )
                    }

                    composable(
                        route = Routes.ACCOUNT_FORM,
                        arguments = listOf(navArgument("accountId") { type = NavType.LongType; defaultValue = -1L }),
                    ) { entry ->
                        val id = entry.arguments?.getLong("accountId").toId()
                        val container = LocalAppContainer.current
                        var existing by remember { mutableStateOf<AccountEntity?>(null) }
                        var loaded by remember { mutableStateOf(id == null) }
                        val vm = appViewModel(key = "accounts-list") { AccountsViewModel(it.repository) }
                        val state by vm.state.collectAsStateWithLifecycle()
                        androidx.compose.runtime.LaunchedEffect(id) {
                            if (id != null) {
                                existing = container.repository.account(id)
                                loaded = true
                            }
                        }
                        if (!loaded) {
                            LoadingScreen()
                        } else {
                            AccountFormScreen(
                                existing = existing,
                                error = state.error,
                                onBack = { navController.popBackStack() },
                                onSave = { name, type, institution, opening, notes, liability ->
                                    vm.saveAccount(existing, name, type, institution, opening, notes, liability) {
                                        navController.popBackStack()
                                    }
                                },
                                onArchiveToggle = { archived ->
                                    existing?.let { vm.setArchived(it.id, archived) }
                                },
                            )
                        }
                    }

                    composable(Routes.CATEGORIES) {
                        val vm = appViewModel { CategoriesViewModel(it.repository) }
                        val state by vm.state.collectAsStateWithLifecycle()
                        CategoriesScreen(
                            state = state,
                            onBack = { navController.popBackStack() },
                            onKindChange = vm::setKind,
                            onSave = { existing, name, iconKey, color, kind ->
                                vm.save(existing, name, iconKey, color, kind)
                            },
                            onArchive = vm::archive,
                            onDelete = vm::deleteOrReport,
                            onReassignAndDelete = vm::reassignAndDelete,
                            onClearError = vm::clearError,
                        )
                    }

                    composable(Routes.SHOPS) {
                        ShopsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenShop = { navController.navigate(Routes.shopDetail(it)) },
                            onAddPurchase = { navController.navigate(Routes.purchase(shopId = it)) },
                        )
                    }

                    composable(
                        route = Routes.SHOP_DETAIL,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: return@composable
                        ShopDetailScreen(
                            shopId = id,
                            onBack = { navController.popBackStack() },
                            onAddPurchase = { navController.navigate(Routes.purchase(shopId = it)) },
                            onOpenTransaction = { navController.navigate(Routes.txDetail(it)) },
                        )
                    }

                    composable(
                        route = Routes.PURCHASE,
                        arguments = listOf(
                            navArgument("shopId") { type = NavType.LongType; defaultValue = -1L },
                            navArgument("personId") { type = NavType.LongType; defaultValue = -1L },
                        ),
                    ) { entry ->
                        PurchaseEntryScreen(
                            shopId = entry.arguments?.getLong("shopId").toId(),
                            personId = entry.arguments?.getLong("personId").toId(),
                            onBack = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.PEOPLE) {
                        PeopleScreen(
                            onBack = { navController.popBackStack() },
                            onOpenPerson = { navController.navigate(Routes.personDetail(it)) },
                        )
                    }

                    composable(
                        route = Routes.PERSON_DETAIL,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: return@composable
                        PersonDetailScreen(
                            personId = id,
                            onBack = { navController.popBackStack() },
                            onOpenTransaction = { navController.navigate(Routes.txDetail(it)) },
                        )
                    }

                    composable(Routes.LOANS) {
                        LoansScreen(
                            onBack = { navController.popBackStack() },
                            onOpenLoan = { navController.navigate(Routes.loanDetail(it)) },
                        )
                    }

                    composable(
                        route = Routes.LOAN_DETAIL,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: return@composable
                        LoanDetailScreen(loanId = id, onBack = { navController.popBackStack() })
                    }

                    composable(Routes.EMI) {
                        EmiScreen(
                            onBack = { navController.popBackStack() },
                            onOpenEmi = { navController.navigate(Routes.emiDetail(it)) },
                        )
                    }

                    composable(
                        route = Routes.EMI_DETAIL,
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        val id = entry.arguments?.getLong("id") ?: return@composable
                        EmiDetailScreen(emiId = id, onBack = { navController.popBackStack() })
                    }

                    composable(Routes.SAVINGS) { SavingsScreen(onBack = { navController.popBackStack() }) }
                    composable(Routes.BUDGETS) { BudgetsScreen(onBack = { navController.popBackStack() }) }
                    composable(Routes.REMINDERS) { RemindersScreen(onBack = { navController.popBackStack() }) }

                    composable(Routes.SEARCH) {
                        SearchScreen(
                            onBack = { navController.popBackStack() },
                            onOpenTransaction = { navController.navigate(Routes.txDetail(it)) },
                            onOpenPerson = { navController.navigate(Routes.personDetail(it)) },
                            onOpenShop = { navController.navigate(Routes.shopDetail(it)) },
                            onOpenCategory = { navController.navigate(Routes.CATEGORIES) },
                        )
                    }

                    composable(Routes.BACKUP) {
                        BackupScreen(
                            onBack = { navController.popBackStack() },
                            onWiped = {
                                appViewModel.resetSecurityAfterWipe {
                                    navController.popBackStack(Routes.HOME, inclusive = false)
                                }
                            },
                        )
                    }

                    composable(Routes.SECURITY) {
                        var error by remember { mutableStateOf<String?>(null) }
                        SecurityScreen(
                            settings = settings,
                            biometricAvailable = BiometricGate.isAvailable(context),
                            error = error,
                            onBack = { navController.popBackStack() },
                            onChangePin = { current, newPin, done ->
                                error = null
                                appViewModel.changePin(current, newPin) {
                                    done()
                                    scope.launch { }
                                }
                            },
                            onBiometric = { appViewModel.setBiometric(it) },
                            onAutoLock = { appViewModel.setAutoLockSeconds(it) },
                            onScreenProtection = { appViewModel.setScreenProtection(it) },
                            onLockNow = { appViewModel.lockNow() },
                        )
                    }

                    composable(Routes.APPEARANCE) {
                        AppearanceScreen(
                            settings = settings,
                            onBack = { navController.popBackStack() },
                            onBanglaDigits = { appViewModel.setShowBanglaDigits(it) },
                            onHideAmounts = { appViewModel.setHideAmounts(it) },
                        )
                    }

                    composable(Routes.ABOUT) { AboutScreen(settings = settings, onBack = { navController.popBackStack() }) }

                    composable(Routes.VERIFY) {
                        var issues by remember { mutableStateOf<List<String>>(emptyList()) }
                        var ran by remember { mutableStateOf(false) }
                        VerifyBooksScreen(
                            issues = if (ran) issues else emptyList(),
                            onBack = { navController.popBackStack() },
                            onRun = {
                                scope.launch {
                                    issues = container.repository.verifyIntegrity().map { "লেনদেন #${it.transactionId}: ${it.message}" }
                                    ran = true
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

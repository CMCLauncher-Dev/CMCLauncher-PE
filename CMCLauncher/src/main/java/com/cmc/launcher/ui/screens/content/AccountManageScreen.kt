package com.cmc.launcher.ui.screens.content

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.res.painterResource
import com.cmc.launcher.R
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmc.launcher.context.COPY_LABEL_ACCOUNT_UUID
import com.cmc.launcher.game.account.Account
import com.cmc.launcher.game.account.AccountsManager
import com.cmc.launcher.game.account.RegionGate
import com.cmc.launcher.game.account.auth_server.data.AuthServer
import com.cmc.launcher.game.account.isAuthServerAccount
import com.cmc.launcher.game.account.isMicrosoftAccount
import com.cmc.launcher.game.account.isMicrosoftLogging
import com.cmc.launcher.game.account.wardrobe.toArmModel
import com.cmc.launcher.game.account.yggdrasil.PlayerProfile
import com.cmc.launcher.ui.AndroidStringText
import com.cmc.launcher.ui.androidText
import com.cmc.launcher.ui.base.BaseScreen
import com.cmc.launcher.ui.components.AnimatedDirection
import com.cmc.launcher.ui.components.BackgroundCard
import com.cmc.launcher.ui.components.MarqueeText
import com.cmc.launcher.ui.components.PetModelView
import com.cmc.launcher.ui.components.ScalingActionButton
import com.cmc.launcher.ui.components.ScalingLabel
import com.cmc.launcher.ui.components.SimpleAlertDialog
import com.cmc.launcher.ui.components.SimpleEditDialog
import com.cmc.launcher.ui.components.SimpleListDialog
import com.cmc.launcher.ui.components.SimpleListItem
import com.cmc.launcher.ui.components.animated
import com.cmc.launcher.ui.components.rememberAnimatedContext
import com.cmc.launcher.ui.screens.NormalNavKey
import com.cmc.launcher.ui.screens.content.elements.AccountItem
import com.cmc.launcher.ui.screens.content.elements.AccountOperation
import com.cmc.launcher.ui.screens.content.elements.AccountSkinOperation
import com.cmc.launcher.ui.screens.content.elements.ChangeSkinDialog
import com.cmc.launcher.ui.screens.content.elements.LocalLoginDialog
import com.cmc.launcher.ui.screens.content.elements.LocalLoginOperation
import com.cmc.launcher.ui.screens.content.elements.LoginMenuDialog
import com.cmc.launcher.ui.screens.content.elements.LoginMenuOperation
import com.cmc.launcher.ui.screens.content.elements.MicrosoftLoginMethodDialog
import com.cmc.launcher.ui.screens.content.elements.MicrosoftLoginOperation
import com.cmc.launcher.ui.screens.content.elements.MicrosoftLoginTipDialog
import com.cmc.launcher.ui.screens.content.elements.MicrosoftReloginDialog
import com.cmc.launcher.ui.screens.content.elements.OtherAccountReloginDialog
import com.cmc.launcher.ui.screens.content.elements.OtherLoginOperation
import com.cmc.launcher.ui.screens.content.elements.OtherServerLoginDialog
import com.cmc.launcher.ui.screens.content.elements.ServerOperation
import com.cmc.launcher.utils.copyText
import com.cmc.launcher.utils.string.getMessageOrToString
import com.cmc.launcher.viewmodel.AccountManageEffect
import com.cmc.launcher.viewmodel.AccountManageIntent
import com.cmc.launcher.viewmodel.AccountManageViewModel
import com.cmc.launcher.viewmodel.ErrorViewModel
import com.cmc.launcher.viewmodel.EventViewModel
import com.cmc.launcher.viewmodel.LocalBackgroundViewModel
import com.cmc.launcher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.cmc.launcher.ui.theme.Space
import com.cmc.launcher.ui.kit.CmcDivider
import com.cmc.launcher.ui.kit.CmcListAction
import com.cmc.launcher.ui.kit.CmcSection
import com.cmc.launcher.ui.theme.FieldShape

private data class AccountActions(
    val onIntent: (AccountManageIntent) -> Unit,
    val openLink: (url: String) -> Unit,
    val backToMainScreen: () -> Unit,
    val navigateToWeb: (url: String) -> Unit,
    val checkIfInWebScreen: () -> Boolean,
    val formatError: (Throwable) -> AndroidStringText,
    val submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
)

enum class FirstLoginMenu {

    NONE,

    MICROSOFT,

    NORMAL
}

@Composable
fun AccountManageScreen(
    key: NormalNavKey.AccountManager,
    backStackViewModel: ScreenBackStackViewModel,
    backToMainScreen: () -> Unit,
    openLink: (url: String) -> Unit,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    eventViewModel: EventViewModel
) {
    val viewModel: AccountManageViewModel = hiltViewModel { factory: AccountManageViewModel.Factory ->
        factory.create(eventViewModel)
    }

    val loginUiState by viewModel.loginUiState.collectAsStateWithLifecycle()
    val profileUiState by viewModel.profileUiState.collectAsStateWithLifecycle()
    val operationUiState by viewModel.operationUiState.collectAsStateWithLifecycle()

    val actions = remember(
        viewModel,
        backToMainScreen,
        openLink,
        backStackViewModel,
        submitError
    ) {
        AccountActions(
            onIntent = viewModel::onIntent,
            openLink = openLink,
            backToMainScreen = backToMainScreen,
            navigateToWeb = { url -> backStackViewModel.mainScreen.backStack.navigateToWeb(url) },
            checkIfInWebScreen = { backStackViewModel.mainScreen.currentKey is NormalNavKey.WebScreen },
            formatError = { th -> viewModel.formatAccountError(th) },
            submitError = submitError,
        )
    }

    LaunchedEffect(Unit) {
        when (key.loginMenu) {
            FirstLoginMenu.NONE -> {}
            FirstLoginMenu.MICROSOFT -> {
                actions.onIntent(AccountManageIntent.UpdateMicrosoftLoginOp(MicrosoftLoginOperation.Tip))
            }
            FirstLoginMenu.NORMAL -> {
                actions.onIntent(AccountManageIntent.UpdateLoginMenuOp(LoginMenuOperation.Login))
            }
        }

        viewModel.effect.collect { effect ->
            when (effect) {
                is AccountManageEffect.ShowError -> {
                    submitError(ErrorViewModel.ThrowableMessage(effect.title, effect.message))
                }
            }
        }
    }

    BaseScreen(
        screenKey = key,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) {
        AccountManageContent(
            loginUiState = loginUiState,
            profileUiState = profileUiState,
            operationUiState = operationUiState,
            actions = actions
        )
    }
}

@Composable
private fun AccountManageContent(
    loginUiState: AccountManageViewModel.LoginUiState,
    profileUiState: AccountManageViewModel.ProfileUiState,
    operationUiState: AccountManageViewModel.OperationUiState,
    actions: AccountActions,
) {
    val context = LocalContext.current
    var showRegionGate by rememberSaveable { mutableStateOf(false) }
    if (showRegionGate) {
        AlertDialog(
            onDismissRequest = { showRegionGate = false },
            title = { Text(stringResource(R.string.pe_account_gate_title)) },
            text = { Text(stringResource(R.string.pe_account_gate_message)) },
            confirmButton = {
                TextButton(onClick = { showRegionGate = false }) {
                    Text(stringResource(R.string.generic_confirm))
                }
            }
        )
    }

    AccountOperation(operationUiState.accountOp, actions)
    AccountSkinOperation(
        accountSkinOperation = operationUiState.accountSkinOp,
        skinDialogState = operationUiState.accountSkinDialogState,
        accountCapes = profileUiState.accountCapeOpMap,
        actions = actions
    )
    AccountsLayout(
        accounts = profileUiState.accounts,
        currentAccount = profileUiState.currentAccount,
        actions = actions,
        onMicrosoft = {
            if (!isMicrosoftLogging()) {
                actions.onIntent(AccountManageIntent.UpdateMicrosoftLoginOp(MicrosoftLoginOperation.Tip))
            }
        },
        onOffline = {
            if (RegionGate.canUseOffline(context)) {
                actions.onIntent(AccountManageIntent.UpdateLocalLoginOp(LocalLoginOperation.Edit))
            } else {
                showRegionGate = true
            }
        },
        onAuthServer = {
            actions.onIntent(AccountManageIntent.UpdateLoginMenuOp(LoginMenuOperation.Login))
        }
    )

    LoginMenuOperation(loginUiState.menuOp, actions, profileUiState.authServers)
    MicrosoftLoginOperation(loginUiState.microsoftOp, actions)
    LocalLoginOperation(loginUiState.localOp, actions)
    OtherLoginOperation(loginUiState.otherOp, actions)
    ServerTypeOperation(operationUiState.serverOp, actions)
}

@Composable
private fun LoginMenuOperation(
    operation: LoginMenuOperation,
    actions: AccountActions,
    authServers: List<AuthServer>
) {
    when (operation) {
        LoginMenuOperation.None -> {}
        LoginMenuOperation.Login -> {
            LoginMenuDialog(
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateLoginMenuOp(LoginMenuOperation.None)
                    )
                },
                authServers = authServers,
                onAuthServerLogin = { server ->
                    actions.onIntent(
                        AccountManageIntent.UpdateOtherLoginOp(
                            OtherLoginOperation.OnLogin(server)
                        )
                    )
                },
                onAddAuthServer = {
                    actions.onIntent(AccountManageIntent.UpdateServerOp(ServerOperation.AddNew))
                },
                onDeleteAuthServer = { server ->
                    actions.onIntent(
                        AccountManageIntent.UpdateServerOp(
                            ServerOperation.Delete(server)
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun MicrosoftLoginOperation(
    operation: MicrosoftLoginOperation,
    actions: AccountActions
) {
    when (operation) {
        is MicrosoftLoginOperation.None -> {}
        is MicrosoftLoginOperation.Tip -> {
            MicrosoftLoginTipDialog(
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateMicrosoftLoginOp(
                            MicrosoftLoginOperation.None
                        )
                    )
                },
                onConfirm = {
                    actions.onIntent(
                        AccountManageIntent.UpdateMicrosoftLoginOp(
                            MicrosoftLoginOperation.SelectBrowser
                        )
                    )
                },
                openLink = actions.openLink
            )
        }
        is MicrosoftLoginOperation.SelectBrowser -> {
            MicrosoftLoginMethodDialog(
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateMicrosoftLoginOp(
                            MicrosoftLoginOperation.None
                        )
                    )
                },
                onSystemBrowser = {
                    actions.onIntent(
                        AccountManageIntent.PerformMicrosoftLogin(
                            useSystemBrowser = true,
                            toWeb = actions.navigateToWeb,
                            toSystemBrowser = actions.openLink,
                            backToMain = actions.backToMainScreen,
                            checkIfInWebScreen = actions.checkIfInWebScreen
                        )
                    )
                },
                onInternalBrowser = {
                    actions.onIntent(
                        AccountManageIntent.PerformMicrosoftLogin(
                            useSystemBrowser = false,
                            toWeb = actions.navigateToWeb,
                            toSystemBrowser = actions.openLink,
                            backToMain = actions.backToMainScreen,
                            checkIfInWebScreen = actions.checkIfInWebScreen
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun LocalLoginOperation(
    operation: LocalLoginOperation,
    actions: AccountActions
) {
    when (operation) {
        is LocalLoginOperation.None -> {}
        is LocalLoginOperation.Edit -> {
            LocalLoginDialog(
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateLocalLoginOp(
                            LocalLoginOperation.None
                        )
                    )
                },
                onConfirm = { isInvalid, name, uuid ->
                    val nextOp = if (isInvalid) LocalLoginOperation.Alert(
                        name,
                        uuid
                    ) else LocalLoginOperation.Create(name, uuid)
                    actions.onIntent(AccountManageIntent.UpdateLocalLoginOp(nextOp))
                },
                openLink = actions.openLink
            )
        }

        is LocalLoginOperation.Create -> {
            LaunchedEffect(operation) {
                actions.onIntent(
                    AccountManageIntent.CreateLocalAccount(
                        operation.userName,
                        operation.userUUID
                    )
                )
            }
        }

        is LocalLoginOperation.Alert -> {
            SimpleAlertDialog(
                title = stringResource(R.string.account_supporting_username_invalid_title),
                text = {
                    Text(text = stringResource(R.string.account_supporting_username_invalid_local_message_hint1))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.account_supporting_username_invalid_local_message_hint2),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stringResource(R.string.account_supporting_username_invalid_local_message_hint3))
                    Text(text = stringResource(R.string.account_supporting_username_invalid_local_message_hint4))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.account_supporting_username_invalid_local_message_hint5),
                        fontWeight = FontWeight.Bold
                    )
                },
                confirmText = stringResource(R.string.account_supporting_username_invalid_still_use),
                onConfirm = {
                    actions.onIntent(
                        AccountManageIntent.UpdateLocalLoginOp(
                            LocalLoginOperation.Create(operation.userName, operation.userUUID)
                        )
                    )
                },
                onCancel = {
                    actions.onIntent(
                        AccountManageIntent.UpdateLocalLoginOp(
                            LocalLoginOperation.None
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun OtherLoginOperation(
    operation: OtherLoginOperation,
    actions: AccountActions
) {
    when (operation) {
        is OtherLoginOperation.None -> {}
        is OtherLoginOperation.OnLogin -> {
            OtherServerLoginDialog(
                server = operation.server,
                onRegisterClick = { url ->
                    actions.openLink(url)
                    actions.onIntent(AccountManageIntent.UpdateOtherLoginOp(OtherLoginOperation.None))
                },
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateOtherLoginOp(
                            OtherLoginOperation.None
                        )
                    )
                },
                onConfirm = { email, password ->
                    actions.onIntent(AccountManageIntent.UpdateOtherLoginOp(OtherLoginOperation.None))
                    actions.onIntent(
                        AccountManageIntent.LoginWithOtherServer(
                            operation.server,
                            email,
                            password
                        )
                    )
                }
            )
        }

        is OtherLoginOperation.OnFailed -> {
            LaunchedEffect(operation) {
                actions.submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.account_logging_in_failed),
                        message = actions.formatError(operation.th)
                    )
                )
                actions.onIntent(AccountManageIntent.UpdateOtherLoginOp(OtherLoginOperation.None))
            }
        }

        is OtherLoginOperation.SelectRole -> {
            SimpleListDialog(
                title = stringResource(R.string.account_other_login_select_role),
                items = operation.profiles,
                onItemSelected = { operation.selected(it) },
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateOtherLoginOp(
                            OtherLoginOperation.None
                        )
                    )
                },
                itemLayout = { item, isCurrent, onClick ->
                    SimpleListItem(
                        selected = isCurrent,
                        itemName = item.name,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onClick
                    )
                }
            )
        }
    }
}

@Composable
private fun ServerTypeOperation(
    operation: ServerOperation,
    actions: AccountActions
) {
    when (operation) {
        is ServerOperation.AddNew -> {
            var serverUrl by rememberSaveable { mutableStateOf("") }
            SimpleEditDialog(
                title = stringResource(R.string.account_add_new_server),
                value = serverUrl,
                onValueChange = { serverUrl = it.trim() },
                label = { Text(text = stringResource(R.string.account_label_server_url)) },
                singleLine = true,
                onDismissRequest = {
                    actions.onIntent(
                        AccountManageIntent.UpdateServerOp(
                            ServerOperation.None
                        )
                    )
                },
                onConfirm = {
                    if (serverUrl.isNotEmpty()) {
                        actions.onIntent(AccountManageIntent.AddServer(serverUrl))
                    }
                }
            )
        }

        is ServerOperation.Delete -> {
            SimpleAlertDialog(
                title = stringResource(R.string.account_other_login_delete_server_title),
                text = stringResource(
                    R.string.account_other_login_delete_server_message,
                    operation.server.serverName
                ),
                onDismiss = { actions.onIntent(AccountManageIntent.UpdateServerOp(ServerOperation.None)) },
                onConfirm = { actions.onIntent(AccountManageIntent.DeleteServer(operation.server)) }
            )
        }

        is ServerOperation.OnThrowable -> {
            LaunchedEffect(operation) {
                actions.submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.account_other_login_adding_failure),
                        message = androidText(operation.throwable.getMessageOrToString())
                    )
                )
                actions.onIntent(AccountManageIntent.UpdateServerOp(ServerOperation.None))
            }
        }

        is ServerOperation.None -> {}
    }
}

@Composable
private fun AccountsLayout(
    accounts: List<Account>,
    currentAccount: Account?,
    actions: AccountActions,
    onMicrosoft: () -> Unit,
    onOffline: () -> Unit,
    onAuthServer: () -> Unit
) {
    var choosingProvider by rememberSaveable { mutableStateOf(false) }
    val active = accounts.firstOrNull { it.uniqueUUID == currentAccount?.uniqueUUID }
    val others = accounts.filterNot { it.uniqueUUID == active?.uniqueUUID }
    val choose: (() -> Unit) -> Unit = { open -> choosingProvider = false; open() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Space.screenH, end = Space.screenH, bottom = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        item(key = "add") {
            Row(
                Modifier.fillMaxWidth().padding(top = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(if (active == null) R.string.pe_account_saved else R.string.pe_account_active),
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { choosingProvider = !choosingProvider }) {
                    Icon(painterResource(if (choosingProvider) R.drawable.ic_close else R.drawable.ic_add),
                        null, Modifier.size(18.dp))
                    Spacer(Modifier.width(Space.xs))
                    Text(stringResource(if (choosingProvider) R.string.generic_cancel else R.string.account_add_new_account))
                }
            }
        }
        if (choosingProvider) {
            item(key = "providers") {
                Surface(shape = FieldShape, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column {
                        CmcListAction(stringResource(R.string.account_type_microsoft), R.drawable.ic_login, { choose(onMicrosoft) })
                        CmcDivider()
                        CmcListAction(stringResource(R.string.account_type_local), R.drawable.ic_person_outlined, { choose(onOffline) })
                        CmcDivider()
                        CmcListAction(stringResource(R.string.pe_account_auth_server), R.drawable.ic_dns_outlined, { choose(onAuthServer) })
                    }
                }
            }
        }
        if (active != null) {
            item(key = "profile:${active.uniqueUUID}") { ManagedAccountRow(active, currentAccount, actions) }
            if (others.isNotEmpty()) item(key = "otherHeading") {
                CmcSection(title = stringResource(R.string.pe_account_other))
            }
        }
        items(others, key = { "profile:${it.uniqueUUID}" }) { account ->
            ManagedAccountRow(account, currentAccount, actions)
        }
        if (accounts.isEmpty()) item(key = "empty") {
            Text(stringResource(R.string.account_no_account), Modifier.padding(vertical = Space.lg),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ManagedAccountRow(account: Account, currentAccount: Account?, actions: AccountActions) {
    val context = LocalContext.current
    AccountItem(
        modifier = Modifier.fillMaxWidth(),
        account = account,
        currentAccount = currentAccount,
        onSelected = { AccountsManager.setCurrentAccount(it) },
        openChangeSkinDialog = {
            if (!account.isAuthServerAccount()) {
                actions.onIntent(AccountManageIntent.UpdateAccountSkinOp(AccountSkinOperation.ChangeSkin(account)))
            }
        },
        onRefreshClick = { actions.onIntent(AccountManageIntent.RefreshAccount(account)) },
        onCopyUUID = { copyText(COPY_LABEL_ACCOUNT_UUID, account.profileId, context, true) },
        onDeleteClick = { actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.Delete(account))) }
    )
}

@Composable
private fun AccountSkinOperation(
    accountSkinOperation: AccountSkinOperation,
    skinDialogState: AccountManageViewModel.AccountSkinDialogState,
    accountCapes: Map<String, List<PlayerProfile.Cape>>,
    actions: AccountActions
) {
    when (accountSkinOperation) {
        is AccountSkinOperation.None -> {}
        is AccountSkinOperation.ChangeSkin -> {
            val account = accountSkinOperation.account
            ChangeSkinDialog(
                account = account,
                availableCapes = accountCapes[account.uniqueUUID],
                skinState = skinDialogState.pendingSkinData,
                onSkinStateChange = { skinState ->
                    actions.onIntent(
                        AccountManageIntent.UpdatePendingSkinData(
                            skinState
                        )
                    )
                },
                capeState = skinDialogState.pendingCapeData,
                onCapeStateChange = { capeState ->
                    actions.onIntent(
                        AccountManageIntent.UpdatePendingCapeData(
                            capeState
                        )
                    )
                },
                isImportingSkin = skinDialogState.importingSkin,
                onSkinPicked = { uri ->
                    actions.onIntent(
                        AccountManageIntent.OnSkinPicked(uri)
                    )
                },
                onDismissRequest = {
                    actions.onIntent(AccountManageIntent.ResetAccountSkinDialogState)
                    actions.onIntent(AccountManageIntent.UpdateAccountSkinOp(AccountSkinOperation.None))
                },
                onResetSkin = {
                    actions.onIntent(AccountManageIntent.ResetSkin(account))
                },
                onFetchCapes = {
                    actions.onIntent(AccountManageIntent.FetchMicrosoftCapes(account))
                },
                onApplySkin = { file, model ->
                    actions.onIntent(AccountManageIntent.ApplySkin(account, file, model))
                },
                onApplyCape = { cape ->
                    actions.onIntent(AccountManageIntent.ApplyMicrosoftCape(account, cape))
                }
            )
        }
    }
}

@Composable
private fun AccountOperation(
    operation: AccountOperation,
    actions: AccountActions
) {
    when (operation) {
        is AccountOperation.Delete -> {
            SimpleAlertDialog(
                title = stringResource(R.string.account_delete_title),
                text = stringResource(R.string.account_delete_message, operation.account.username),
                onConfirm = { actions.onIntent(AccountManageIntent.DeleteAccount(operation.account)) },
                onDismiss = { actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.None)) }
            )
        }

        is AccountOperation.OnFailed -> {
            LaunchedEffect(operation) {
                actions.submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.account_logging_in_failed),
                        message = actions.formatError(operation.th)
                    )
                )
                actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.None))
            }
        }

        is AccountOperation.OnRelogin -> {
            if (operation.account.isMicrosoftAccount()) {
                MicrosoftReloginDialog(
                    onDismissRequest = {
                        actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.None))
                    },
                    onConfirm = {
                        actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.None))
                        actions.onIntent(
                            AccountManageIntent.UpdateMicrosoftLoginOp(
                                MicrosoftLoginOperation.SelectBrowser
                            )
                        )
                    }
                )
            } else {
                OtherAccountReloginDialog(
                    account = operation.account,
                    logging = operation.logging,
                    error = operation.error,
                    onDismissRequest = {
                        actions.onIntent(AccountManageIntent.UpdateAccountOp(AccountOperation.None))
                    },
                    onConfirm = { password ->
                        actions.onIntent(
                            AccountManageIntent.UpdateAccountOp(
                                AccountOperation.OnRelogin(operation.account, logging = true)
                            )
                        )
                        actions.onIntent(
                            AccountManageIntent.ReloginOtherAccount(
                                account = operation.account,
                                password = password
                            )
                        )
                    }
                )
            }
        }

        is AccountOperation.None -> {}
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 480)
@Composable
private fun AccountManageContentPreview() {
    CompositionLocalProvider(LocalBackgroundViewModel provides null) {
        MaterialExpressiveTheme {
            Surface {
                AccountManageContent(
                    loginUiState = AccountManageViewModel.LoginUiState(),
                    profileUiState = AccountManageViewModel.ProfileUiState(),
                    operationUiState = AccountManageViewModel.OperationUiState(),
                    actions = AccountActions(
                        onIntent = {},
                        openLink = {},
                        backToMainScreen = {},
                        navigateToWeb = {},
                        checkIfInWebScreen = { false },
                        formatError = { AndroidStringText.Text("") },
                        submitError = {},
                    )
                )
            }
        }
    }
}

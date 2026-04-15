package com.daveharris.mumlauncher

import android.app.TimePickerDialog
import android.app.admin.DevicePolicyManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.daveharris.mumlauncher.data.Contact
import com.daveharris.mumlauncher.data.ContactRepository
import com.daveharris.mumlauncher.data.LauncherMode
import com.daveharris.mumlauncher.data.LauncherSettings
import com.daveharris.mumlauncher.data.SettingsStore
import com.daveharris.mumlauncher.ui.theme.MumLauncherTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Calendar

private const val ACTION_OPEN_FOCUS_NOW = "com.daveharris.mumlauncher.action.OPEN_FOCUS_NOW"

private enum class Screen {
    HOME,
    CALLS,
    MESSAGES,
    ADMIN,
}

private enum class EffectiveMode {
    SIMPLE,
    REGULAR,
}

data class AppUiState(
    val contacts: List<Contact> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
    val lastError: String? = null,
)

class MainViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val contactRepository = ContactRepository(application)
    private val settingsStore = SettingsStore(application)
    private val transientError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AppUiState> = combine(
        contactRepository.observeContacts(),
        settingsStore.settings,
        transientError,
    ) { contacts, settings, error ->
        AppUiState(contacts = contacts, settings = settings, lastError = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    init {
        viewModelScope.launch {
            contactRepository.ensureSeedData()
        }
    }

    fun clearError() {
        transientError.value = null
    }

    fun completeSetup(pin: String) {
        if (pin.isNotBlank() && pin.length < 4) {
            transientError.value = "Choose a PIN with at least 4 digits."
            return
        }
        viewModelScope.launch {
            settingsStore.configureAdminPin(pin.takeIf { it.isNotBlank() }?.let(::hashPin))
            settingsStore.setSetupComplete(true)
        }
    }

    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        if (!uiState.value.settings.adminPinEnabled) {
            onResult(true)
            return
        }
        onResult(hashPin(pin) == uiState.value.settings.pinHash)
    }

    fun setAdminPin(pin: String?) {
        if (!pin.isNullOrBlank() && pin.length < 4) {
            transientError.value = "Choose a PIN with at least 4 digits."
            return
        }
        viewModelScope.launch {
            settingsStore.configureAdminPin(pin?.takeIf { it.isNotBlank() }?.let(::hashPin))
        }
    }

    fun setAllowUserEditing(allowed: Boolean) {
        viewModelScope.launch { settingsStore.setAllowUserContactEditing(allowed) }
    }

    fun setKioskEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setKioskEnabled(enabled) }
    }

    fun setLauncherMode(mode: LauncherMode) {
        viewModelScope.launch { settingsStore.setLauncherMode(mode) }
    }

    fun setSchedule(days: Set<Int>, startMinutes: Int, endMinutes: Int) {
        viewModelScope.launch { settingsStore.setSchedule(days, startMinutes, endMinutes) }
    }

    fun setShowFocusUntilText(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setShowFocusUntilText(enabled) }
    }

    fun setWarnIfScheduleNotificationsOff(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setWarnIfScheduleNotificationsOff(enabled) }
    }

    fun setShowLauncherAppIcon(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setShowLauncherAppIcon(enabled) }
    }

    fun setFocusSession(active: Boolean, anchor: String?) {
        viewModelScope.launch { settingsStore.setFocusSession(active, anchor) }
    }

    fun addContact(name: String, phoneNumber: String) {
        if (name.isBlank() || phoneNumber.isBlank()) {
            transientError.value = "Both name and phone number are required."
            return
        }
        viewModelScope.launch { contactRepository.add(name, phoneNumber) }
    }

    fun updateContact(contact: Contact, name: String, phoneNumber: String) {
        if (name.isBlank() || phoneNumber.isBlank()) {
            transientError.value = "Both name and phone number are required."
            return
        }
        viewModelScope.launch {
            contactRepository.update(contact.copy(displayName = name, phoneNumber = phoneNumber))
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch { contactRepository.delete(contact) }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

class MainActivity : ComponentActivity() {
    private val rehideSystemUi = Runnable { hideSystemUi() }
    private val promptAction = MutableStateFlow<PromptAction?>(null)
    private val resumeTick = MutableStateFlow(0L)
    private val focusShortcutLaunchTick = MutableStateFlow(0L)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handlePromptIntent(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val systemBarsVisible = insets.isVisible(WindowInsetsCompat.Type.systemBars())
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            window.decorView.removeCallbacks(rehideSystemUi)
            if (systemBarsVisible && !imeVisible && hasWindowFocus()) {
                window.decorView.postDelayed(rehideSystemUi, 1800L)
            }
            insets
        }
        setContent {
            MumLauncherTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application),
                )
                LauncherApp(
                    activity = this,
                    viewModel = viewModel,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.value = System.currentTimeMillis()
        window.decorView.removeCallbacks(rehideSystemUi)
        hideSystemUi()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePromptIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.removeCallbacks(rehideSystemUi)
            hideSystemUi()
        }
    }

    fun hideSystemUi() {
        val controller = window.insetsController ?: return
        controller.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsets.Type.systemBars())
    }

    fun promptActionFlow(): StateFlow<PromptAction?> = promptAction

    fun resumeTickFlow(): StateFlow<Long> = resumeTick

    fun focusShortcutLaunchFlow(): StateFlow<Long> = focusShortcutLaunchTick

    fun consumePromptAction() {
        promptAction.value = null
        intent?.removeExtra(EXTRA_PROMPT_ACTION)
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handlePromptIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_FOCUS_NOW) {
            focusShortcutLaunchTick.value = System.currentTimeMillis()
        }
        promptAction.value = PromptAction.from(intent?.getStringExtra(EXTRA_PROMPT_ACTION))
    }
}

@Composable
private fun LauncherApp(
    activity: MainActivity,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val promptAction by activity.promptActionFlow().collectAsState()
    val resumeTick by activity.resumeTickFlow().collectAsState()
    val focusShortcutLaunchTick by activity.focusShortcutLaunchFlow().collectAsState()
    var currentTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var showPinPrompt by rememberSaveable { mutableStateOf(false) }
    var showEditingDialog by remember { mutableStateOf<Contact?>(null) }
    var isCreatingContact by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var autoPrompt by rememberSaveable { mutableStateOf<DuePrompt?>(null) }
    val effectiveMode = remember(
        uiState.settings.launcherMode,
        uiState.settings.scheduleDays,
        uiState.settings.scheduleStartMinutes,
        uiState.settings.scheduleEndMinutes,
        currentTimeMs,
        uiState.settings.lastSystemEventAtMs,
    ) {
        if (shouldUseFocusLauncherNow(uiState.settings, currentTimeMs)) {
            EffectiveMode.SIMPLE
        } else {
            EffectiveMode.REGULAR
        }
    }

    LaunchedEffect(uiState.settings.kioskEnabled, uiState.settings.setupComplete) {
        activity.hideSystemUi()
        val kioskState = KioskController.syncKioskPolicy(
            context,
            uiState.settings.kioskEnabled && uiState.settings.setupComplete,
        )
        if (uiState.settings.kioskEnabled && uiState.settings.setupComplete && kioskState.isLockTaskPermitted) {
            runCatching { activity.startLockTask() }
        } else {
            runCatching { activity.stopLockTask() }
        }
    }

    LaunchedEffect(
        uiState.settings.launcherMode,
        uiState.settings.scheduleDays,
        uiState.settings.scheduleStartMinutes,
        uiState.settings.scheduleEndMinutes,
        uiState.settings.setupComplete,
        uiState.settings.lastSystemEventAtMs,
        uiState.settings.focusSessionActive,
        uiState.settings.focusSessionAnchor,
        resumeTick,
    ) {
        SchedulePromptController.sync(context, uiState.settings)
    }

    LaunchedEffect(uiState.settings.showLauncherAppIcon) {
        setLauncherEntryIconEnabled(context, uiState.settings.showLauncherAppIcon)
    }

    LaunchedEffect(uiState.settings.setupComplete, uiState.settings.lastSystemEventAtMs) {
        currentTimeMs = System.currentTimeMillis()
        while (uiState.settings.setupComplete) {
            delay(60_000)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(
        uiState.settings.launcherMode,
        uiState.settings.scheduleDays,
        uiState.settings.scheduleStartMinutes,
        uiState.settings.scheduleEndMinutes,
        currentTimeMs,
        resumeTick,
        focusShortcutLaunchTick,
        uiState.settings.focusSessionActive,
        uiState.settings.focusSessionAnchor,
        promptAction,
    ) {
        val duePrompt = currentDuePrompt(uiState.settings, currentTimeMs)
        if (duePrompt == null) {
            autoPrompt = null
        } else if (
            promptAction == null &&
            (resumeTick != 0L || focusShortcutLaunchTick != 0L) &&
            autoPrompt != duePrompt
        ) {
            autoPrompt = duePrompt
        }
    }

    LaunchedEffect(
        uiState.settings.focusSessionActive,
        uiState.settings.focusSessionAnchor,
        uiState.settings.launcherMode,
        uiState.settings.scheduleDays,
        uiState.settings.scheduleStartMinutes,
        uiState.settings.scheduleEndMinutes,
        currentTimeMs,
    ) {
        if (!uiState.settings.focusSessionActive) return@LaunchedEffect
        val activeWindow = currentScheduledWindow(uiState.settings, currentTimeMs)
        val recentWindow = mostRecentEndedWindow(uiState.settings, currentTimeMs)
        val anchor = uiState.settings.focusSessionAnchor
        val anchorStillRelevant =
            (activeWindow != null && activeWindow.anchor == anchor) ||
                (recentWindow != null && recentWindow.anchor == anchor)
        if (!anchorStillRelevant) {
            viewModel.setFocusSession(false, null)
        }
    }

    LaunchedEffect(uiState.lastError) {
        uiState.lastError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    if (!uiState.settings.setupComplete) {
        BackHandler {}
        SetupScreen(
            onOpenHomeSettings = { openHomeSettings(context) },
            onEnableDeviceAdmin = { requestDeviceAdmin(context) },
            onFinishSetup = { pin -> viewModel.completeSetup(pin) },
        )
        return
    }

    LaunchedEffect(
        focusShortcutLaunchTick,
        uiState.settings.setupComplete,
    ) {
        if (focusShortcutLaunchTick == 0L || !uiState.settings.setupComplete) return@LaunchedEffect
        screen = Screen.HOME
    }

    BackHandler {
        screen = when (screen) {
            Screen.HOME -> Screen.HOME
            Screen.CALLS, Screen.MESSAGES, Screen.ADMIN -> Screen.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .navigationBarsPadding(),
        ) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    effectiveMode = effectiveMode,
                    settings = uiState.settings,
                    onOpenCalls = { screen = Screen.CALLS },
                    onOpenMessages = { screen = Screen.MESSAGES },
                    onOpenAdmin = {
                        if (uiState.settings.adminPinEnabled) {
                            showPinPrompt = true
                        } else {
                            screen = Screen.ADMIN
                        }
                    },
                )

                Screen.CALLS -> ContactListScreen(
                    title = "Calls",
                    contacts = uiState.contacts.filter { it.callable },
                    canEdit = uiState.settings.allowUserContactEditing,
                    actionLabel = "Call",
                    onBack = { screen = Screen.HOME },
                    onPrimaryAction = { launchDialer(context, it.phoneNumber) },
                    onAdd = { isCreatingContact = true },
                    onEdit = { showEditingDialog = it },
                    onDelete = viewModel::deleteContact,
                )

                Screen.MESSAGES -> ContactListScreen(
                    title = "Messages",
                    contacts = uiState.contacts.filter { it.messageable },
                    canEdit = uiState.settings.allowUserContactEditing,
                    actionLabel = "Text",
                    onBack = { screen = Screen.HOME },
                    onPrimaryAction = { launchSms(context, it.phoneNumber) },
                    onAdd = { isCreatingContact = true },
                    onEdit = { showEditingDialog = it },
                    onDelete = viewModel::deleteContact,
                )

                Screen.ADMIN -> AdminScreen(
                    settings = uiState.settings,
                    effectiveMode = effectiveMode,
                    contacts = uiState.contacts,
                    diagnostics = buildDiagnostics(context),
                    onBack = { screen = Screen.HOME },
                    onToggleEditing = viewModel::setAllowUserEditing,
                    onToggleKiosk = viewModel::setKioskEnabled,
                    onSetLauncherMode = viewModel::setLauncherMode,
                    onSetSchedule = viewModel::setSchedule,
                    onToggleShowFocusUntilText = viewModel::setShowFocusUntilText,
                    onToggleScheduleNotificationWarning = viewModel::setWarnIfScheduleNotificationsOff,
                    onToggleLauncherAppIcon = viewModel::setShowLauncherAppIcon,
                    onToggleRequireAdminPin = { enabled ->
                        if (enabled) {
                            showSetPinDialog = true
                        } else {
                            viewModel.setAdminPin(null)
                        }
                    },
                    onAddHomeScreenShortcut = { requestPinnedFocusShortcut(context) },
                    onOpenNotificationSettings = { SchedulePromptController.openNotificationSettings(context) },
                    onOpenExactAlarmSettings = { SchedulePromptController.openExactAlarmSettings(context) },
                    canPostNotifications = SchedulePromptController.canPostNotifications(context),
                    isFocusSessionActive = uiState.settings.focusSessionActive,
                    isInsideScheduledWindow = currentScheduledWindow(uiState.settings, currentTimeMs) != null,
                    onAdd = { isCreatingContact = true },
                    onEdit = { showEditingDialog = it },
                    onDelete = viewModel::deleteContact,
                    onOpenHomeSettings = {
                        viewModel.setFocusSession(false, null)
                        openHomeSettings(context)
                    },
                    onOpenAccessibilitySettings = { openAccessibilitySettings(context) },
                    onEnableDeviceAdmin = { requestDeviceAdmin(context) },
                )
            }
        }
    }

    if (showPinPrompt) {
        PinPromptDialog(
            onDismiss = { showPinPrompt = false },
            onSubmit = { pin ->
                viewModel.verifyPin(pin) { valid ->
                    if (valid) {
                        showPinPrompt = false
                        screen = Screen.ADMIN
                    } else {
                        Toast.makeText(context, "Incorrect PIN.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    if (showSetPinDialog) {
        SetAdminPinDialog(
            onDismiss = { showSetPinDialog = false },
            onSave = { pin ->
                viewModel.setAdminPin(pin)
                if (pin.length >= 4) {
                    showSetPinDialog = false
                }
            },
        )
    }

    val dialogAction = promptAction ?: autoPrompt?.action
    dialogAction?.let { action ->
        PromptActionDialog(
            action = action,
            onDismiss = {
                if (promptAction != null) {
                    activity.consumePromptAction()
                } else {
                    autoPrompt = null
                }
            },
            onConfirm = {
                when (action) {
                    PromptAction.ENTER_FOCUS -> {
                        val anchor = currentScheduledWindow(uiState.settings, System.currentTimeMillis())?.anchor
                        viewModel.setFocusSession(true, anchor)
                        if (!isAppDefaultLauncher(context)) {
                            requestFocusLauncher(activity)
                        }
                    }
                    PromptAction.EXIT_FOCUS -> {
                        viewModel.setFocusSession(false, null)
                        if (isAppDefaultLauncher(context)) {
                            openHomeSettings(context)
                        }
                    }
                }
                if (promptAction != null) {
                    activity.consumePromptAction()
                } else {
                    autoPrompt = null
                }
            },
        )
    }

    if (isCreatingContact) {
        ContactEditorDialog(
            title = "Add contact",
            initialName = "",
            initialPhone = "",
            onDismiss = { isCreatingContact = false },
            onSave = { name, phone ->
                viewModel.addContact(name, phone)
                isCreatingContact = false
            },
            onDelete = null,
        )
    }

    showEditingDialog?.let { contact ->
        ContactEditorDialog(
            title = "Edit contact",
            initialName = contact.displayName,
            initialPhone = contact.phoneNumber,
            onDismiss = { showEditingDialog = null },
            onSave = { name, phone ->
                viewModel.updateContact(contact, name, phone)
                showEditingDialog = null
            },
            onDelete = {
                viewModel.deleteContact(contact)
                showEditingDialog = null
            },
        )
    }
}

private data class Diagnostics(
    val batteryPercent: Int,
    val networkSummary: String,
    val notificationsReady: Boolean,
    val exactTimingAvailable: Boolean,
    val isDeviceAdminEnabled: Boolean,
    val isLockTaskPermitted: Boolean,
    val isDeviceOwner: Boolean,
    val dialerPackage: String?,
    val smsPackage: String?,
    val allowlistedPackages: List<String>,
)

private fun buildDiagnostics(context: Context): Diagnostics {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = runCatching {
        val network = connectivityManager.activeNetwork
        connectivityManager.getNetworkCapabilities(network)
    }.getOrNull()
    val networkSummary = when {
        capabilities == null -> "Offline"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi connected"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data connected"
        else -> "Connected"
    }
    val kioskState = KioskController.getState(context)
    return Diagnostics(
        batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
        networkSummary = networkSummary,
        notificationsReady = SchedulePromptController.canPostNotifications(context),
        exactTimingAvailable = SchedulePromptController.canScheduleExactAlarms(context),
        isDeviceAdminEnabled = kioskState.isDeviceAdminEnabled,
        isLockTaskPermitted = kioskState.isLockTaskPermitted,
        isDeviceOwner = kioskState.isDeviceOwner,
        dialerPackage = kioskState.dialerPackage,
        smsPackage = kioskState.smsPackage,
        allowlistedPackages = kioskState.allowlistedPackages,
    )
}

private fun requestDeviceAdmin(context: Context) {
    val adminComponent = ComponentName(context, MumDeviceAdminReceiver::class.java)
    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
        putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Enable device admin to prepare kiosk mode and support access.",
        )
    }
    context.startActivity(intent)
}

private fun openHomeSettings(context: Context) {
    val intent = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "Home app settings are not available on this phone.", Toast.LENGTH_LONG).show()
        }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun setLauncherEntryIconEnabled(context: Context, enabled: Boolean) {
    val component = ComponentName(context, "com.daveharris.mumlauncher.LauncherEntryAlias")
    val state = if (enabled) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
    context.packageManager.setComponentEnabledSetting(
        component,
        state,
        PackageManager.DONT_KILL_APP,
    )
}

private fun requestPinnedFocusShortcut(context: Context) {
    val shortcutManager = context.getSystemService(ShortcutManager::class.java)
    if (shortcutManager == null || !shortcutManager.isRequestPinShortcutSupported) {
        Toast.makeText(context, "This launcher does not support pinned shortcuts.", Toast.LENGTH_LONG).show()
        return
    }
    val shortcutIntent = Intent(context, MainActivity::class.java).apply {
        action = ACTION_OPEN_FOCUS_NOW
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    val shortcut = ShortcutInfo.Builder(context, "focus_now_shortcut")
        .setShortLabel("Focus now")
        .setLongLabel("Open Mum Launcher now")
        .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(shortcutIntent)
        .build()
    val accepted = shortcutManager.requestPinShortcut(shortcut, null)
    if (!accepted) {
        Toast.makeText(context, "This launcher does not support pinned shortcuts.", Toast.LENGTH_LONG).show()
    }
}

private fun launchDialer(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    launchExternalIntent(context, intent, "No phone app is available.")
}

private fun launchSms(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phoneNumber)}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    launchExternalIntent(context, intent, "No messaging app is available.")
}

private fun launchExternalIntent(context: Context, intent: Intent, errorMessage: String) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun requestFocusLauncher(activity: MainActivity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = activity.getSystemService(RoleManager::class.java)
        if (roleManager != null &&
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        ) {
            runCatching {
                activity.startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
            }.onFailure {
                openHomeSettings(activity)
            }
            return
        }
    }
    openHomeSettings(activity)
}

private fun formatTimeLabel(context: Context, minutes: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}

private fun describeDays(days: Set<Int>): String {
    val labels = listOf(
        Calendar.SUNDAY to "Sun",
        Calendar.MONDAY to "Mon",
        Calendar.TUESDAY to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY to "Thu",
        Calendar.FRIDAY to "Fri",
        Calendar.SATURDAY to "Sat",
    )
    return labels.filter { days.contains(it.first) }.joinToString(" ") { it.second }
}

private fun modeLabel(mode: LauncherMode): String = when (mode) {
    LauncherMode.SIMPLE -> "Simple"
    LauncherMode.SCHEDULED -> "Scheduled"
}

private fun effectiveModeLabel(mode: EffectiveMode): String = when (mode) {
    EffectiveMode.SIMPLE -> "Simple"
    EffectiveMode.REGULAR -> "Regular launcher"
}

@Composable
private fun SetupScreen(
    onOpenHomeSettings: () -> Unit,
    onEnableDeviceAdmin: () -> Unit,
    onFinishSetup: (String) -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Set up Mum Launcher",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Use this screen once, before handing over the phone. Android treats this as a Home app, not a normal app, so it must be chosen in Home settings. Admin PIN is optional.",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 18.sp,
        )
        SetupStepCard(
            title = "1. Set this as the home app",
            body = "Choose Mum Launcher as the default home app so the phone always returns here.",
            actionLabel = "Open Home Settings",
            onAction = onOpenHomeSettings,
        )
        SetupStepCard(
            title = "2. Enable device admin",
            body = "Best effort for kiosk support. Full device-owner lockdown still needs ADB provisioning on a clean device.",
            actionLabel = "Enable Device Admin",
            onAction = onEnableDeviceAdmin,
        )
        SetupStepCard(
            title = "3. Note prototype permissions",
            body = "This build uses Android intents for calls and SMS, so it avoids extra runtime call or SMS permissions for now.",
            actionLabel = null,
            onAction = null,
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Admin PIN (optional)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            Button(
                onClick = { onFinishSetup(pin) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text("Finish setup", fontSize = 22.sp)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SetupStepCard(
    title: String,
    body: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.14f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text(body, color = Color.White.copy(alpha = 0.9f))
            if (actionLabel != null && onAction != null) {
                FilledTonalButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(
    effectiveMode: EffectiveMode,
    settings: LauncherSettings,
    onOpenCalls: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenAdmin: () -> Unit,
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableStateOf(0L) }
    var cogTapCount by remember { mutableIntStateOf(0) }
    var lastCogTapMs by remember { mutableStateOf(0L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Mum's Phone",
                modifier = Modifier
                    .align(Alignment.Center)
                    .combinedClickable(
                        onClick = {
                            val now = System.currentTimeMillis()
                            tapCount = if (now - lastTapMs < 900) tapCount + 1 else 1
                            lastTapMs = now
                            if (tapCount >= 3) {
                                tapCount = 0
                                onOpenAdmin()
                            }
                        },
                        onLongClick = onOpenAdmin,
                    ),
                textAlign = TextAlign.Center,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            IconButton(
                onClick = {
                    val now = System.currentTimeMillis()
                    cogTapCount = if (now - lastCogTapMs < 900) cogTapCount + 1 else 1
                    lastCogTapMs = now
                    if (cogTapCount >= 3) {
                        cogTapCount = 0
                        onOpenAdmin()
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Admin",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.85f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            HomeActionButton(
                label = "Calls",
                icon = Icons.Outlined.Call,
                color = MaterialTheme.colorScheme.primary,
                onClick = onOpenCalls,
            )
            HomeActionButton(
                label = "Messages",
                icon = Icons.Outlined.Mail,
                color = MaterialTheme.colorScheme.secondary,
                onClick = onOpenMessages,
            )
        }

        Spacer(modifier = Modifier.weight(0.55f))
        if (
            settings.showFocusUntilText &&
            settings.launcherMode == LauncherMode.SCHEDULED &&
            effectiveMode == EffectiveMode.SIMPLE
        ) {
            Text(
                text = "Focus mode until ${formatTimeLabel(LocalContext.current, settings.scheduleEndMinutes)}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                fontSize = 16.sp,
                lineHeight = 20.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Spacer(modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun HomeActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 144.dp),
        shape = RoundedCornerShape(36.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    label,
                    modifier = Modifier.width(154.dp),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactListScreen(
    title: String,
    contacts: List<Contact>,
    canEdit: Boolean,
    actionLabel: String,
    onBack: () -> Unit,
    onPrimaryAction: (Contact) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Contact) -> Unit,
    onDelete: (Contact) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            if (canEdit) {
                FilledTonalButton(onClick = onAdd) { Text("Add") }
            } else {
                Spacer(modifier = Modifier.size(70.dp))
            }
        }

        if (contacts.isEmpty()) {
            Card {
                Text(
                    text = "No contacts yet.",
                    modifier = Modifier.padding(24.dp),
                    fontSize = 22.sp,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(contacts, key = { it.id }) { contact ->
                    ContactCard(
                        contact = contact,
                        actionLabel = actionLabel,
                        canEdit = canEdit,
                        onPrimaryAction = { onPrimaryAction(contact) },
                        onEdit = { onEdit(contact) },
                        onDelete = { onDelete(contact) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactCard(
    contact: Contact,
    actionLabel: String,
    canEdit: Boolean,
    onPrimaryAction: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                contact.displayName,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 30.sp,
            )
            Text(
                contact.phoneNumber,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(actionLabel, fontSize = 20.sp)
                }
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminScreen(
    settings: LauncherSettings,
    effectiveMode: EffectiveMode,
    contacts: List<Contact>,
    diagnostics: Diagnostics,
    onBack: () -> Unit,
    onToggleEditing: (Boolean) -> Unit,
    onToggleKiosk: (Boolean) -> Unit,
    onSetLauncherMode: (LauncherMode) -> Unit,
    onSetSchedule: (Set<Int>, Int, Int) -> Unit,
    onToggleShowFocusUntilText: (Boolean) -> Unit,
    onToggleScheduleNotificationWarning: (Boolean) -> Unit,
    onToggleLauncherAppIcon: (Boolean) -> Unit,
    onToggleRequireAdminPin: (Boolean) -> Unit,
    onAddHomeScreenShortcut: () -> Unit,
    isFocusSessionActive: Boolean,
    isInsideScheduledWindow: Boolean,
    canPostNotifications: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Contact) -> Unit,
    onDelete: (Contact) -> Unit,
    onOpenHomeSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onEnableDeviceAdmin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(
                text = "Admin",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Icon(Icons.Outlined.Settings, contentDescription = null)
        }

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Launcher mode", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Current active mode: ${effectiveModeLabel(effectiveMode)}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    "Simple keeps this launcher on screen. Scheduled sends prompts at the start and end of focus time. Returning to your regular launcher stays manual from Home settings.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    lineHeight = 20.sp,
                )
                Text(
                    "Focus session active: ${if (isFocusSessionActive) "Yes" else "No"}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                Text(
                    "Inside scheduled focus window now: ${if (isInsideScheduledWindow) "Yes" else "No"}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                Text(
                    "Notifications ready: ${if (diagnostics.notificationsReady) "Yes" else "No"}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                Text(
                    "Exact timing available: ${if (diagnostics.exactTimingAvailable) "Yes" else "No"}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                ModeSelector(
                    selectedMode = settings.launcherMode,
                    onSelectMode = onSetLauncherMode,
                )
                ToggleCard(
                    title = "Require PIN for admin",
                    checked = settings.adminPinEnabled,
                    onCheckedChange = onToggleRequireAdminPin,
                )
                ToggleCard(
                    title = "Show app icon in the app drawer",
                    checked = settings.showLauncherAppIcon,
                    onCheckedChange = onToggleLauncherAppIcon,
                )
                FilledTonalButton(onClick = onAddHomeScreenShortcut) {
                    Text("Add focus shortcut to home screen")
                }
                FilledTonalButton(onClick = onOpenHomeSettings) {
                    Text("Revert to normal launcher")
                }
                if (settings.launcherMode == LauncherMode.SCHEDULED) {
                    ToggleCard(
                        title = "Warn if schedule notifications are off",
                        checked = settings.warnIfScheduleNotificationsOff,
                        onCheckedChange = onToggleScheduleNotificationWarning,
                    )
                    if (!canPostNotifications && settings.warnIfScheduleNotificationsOff) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    "Scheduled focus needs notifications turned on",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    "Android notifications are turned off or not prominent enough for Mum Launcher, so scheduled prompts may be easy to miss.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                FilledTonalButton(onClick = onOpenNotificationSettings) {
                                    Text("Open notification settings")
                                }
                            }
                        }
                    }
                    if (!diagnostics.exactTimingAvailable) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    "Exact timing is not available",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    "Scheduled prompts still work, but prompt times may drift significantly without exact alarms.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                FilledTonalButton(onClick = onOpenExactAlarmSettings) {
                                    Text("Open alarm timing settings")
                                }
                            }
                        }
                    }
                    ToggleCard(
                        title = "Show \"Focus mode until...\" on the home screen",
                        checked = settings.showFocusUntilText,
                        onCheckedChange = onToggleShowFocusUntilText,
                    )
                    ScheduleEditor(
                        selectedDays = settings.scheduleDays,
                        startMinutes = settings.scheduleStartMinutes,
                        endMinutes = settings.scheduleEndMinutes,
                        onSaveSchedule = onSetSchedule,
                    )
                }
            }
        }

        ToggleCard(
            title = "Allow contact editing while using this launcher",
            checked = settings.allowUserContactEditing,
            onCheckedChange = onToggleEditing,
        )
        ToggleCard(
            title = "Kiosk mode enabled",
            checked = settings.kioskEnabled,
            onCheckedChange = onToggleKiosk,
        )

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Diagnostics", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("Battery: ${diagnostics.batteryPercent}%")
                Text("Network: ${diagnostics.networkSummary}")
                Text("Notifications ready: ${if (diagnostics.notificationsReady) "Yes" else "No"}")
                Text("Exact timing available: ${if (diagnostics.exactTimingAvailable) "Yes" else "No"}")
                Text("Device admin: ${if (diagnostics.isDeviceAdminEnabled) "Enabled" else "Not enabled"}")
                Text("Device owner: ${if (diagnostics.isDeviceOwner) "Enabled" else "Not enabled"}")
                Text("Lock task allowed: ${if (diagnostics.isLockTaskPermitted) "Yes" else "No"}")
                Text("Dialer app: ${diagnostics.dialerPackage ?: "Not found"}")
                Text("Messages app: ${diagnostics.smsPackage ?: "Not found"}")
                Text("Kiosk allowlist: ${diagnostics.allowlistedPackages.joinToString()}")
                FilledTonalButton(onClick = onEnableDeviceAdmin) { Text("Open Device Admin") }
                FilledTonalButton(onClick = onOpenAccessibilitySettings) { Text("Open Accessibility Settings") }
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Recovery notes", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("Keep ADB enabled during setup so you can reprovision the device if kiosk mode misbehaves.")
                Text("If the PIN is lost during prototyping, reinstalling the app or clearing app data will reset the local settings.")
                Text("Full device-owner provisioning still needs ADB on a clean device.")
                Text("Device owner command: adb shell dpm set-device-owner com.daveharris.mumlauncher/.MumDeviceAdminReceiver")
            }
        }

        Text("Contacts", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        FilledTonalButton(onClick = onAdd) { Text("Add contact") }
        contacts.forEach { contact ->
            ContactCard(
                contact = contact,
                actionLabel = "Edit",
                canEdit = true,
                onPrimaryAction = { onEdit(contact) },
                onEdit = { onEdit(contact) },
                onDelete = { onDelete(contact) },
            )
        }
    }
}

@Composable
private fun ModeSelector(
    selectedMode: LauncherMode,
    onSelectMode: (LauncherMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LauncherMode.entries.forEach { mode ->
            val label = modeLabel(mode)
            val selected = selectedMode == mode
            if (selected) {
                FilledTonalButton(
                    onClick = { onSelectMode(mode) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(label, textAlign = TextAlign.Center)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelectMode(mode) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(label, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun ScheduleEditor(
    selectedDays: Set<Int>,
    startMinutes: Int,
    endMinutes: Int,
    onSaveSchedule: (Set<Int>, Int, Int) -> Unit,
) {
    val context = LocalContext.current
    var draftDays by remember(selectedDays) { mutableStateOf(selectedDays) }
    var draftStart by remember(startMinutes) { mutableIntStateOf(startMinutes) }
    var draftEnd by remember(endMinutes) { mutableIntStateOf(endMinutes) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Days: ${describeDays(draftDays)}",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Calendar.MONDAY to "M",
                Calendar.TUESDAY to "Tu",
                Calendar.WEDNESDAY to "W",
                Calendar.THURSDAY to "Th",
                Calendar.FRIDAY to "F",
                Calendar.SATURDAY to "Sa",
                Calendar.SUNDAY to "Su",
            ).forEach { (day, label) ->
                val selected = draftDays.contains(day)
                val onClick = {
                    draftDays = if (selected) draftDays - day else draftDays + day
                }
                if (selected) {
                    FilledTonalButton(
                        onClick = onClick,
                        modifier = Modifier.widthIn(min = 0.dp),
                    ) { Text(label) }
                } else {
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.widthIn(min = 0.dp),
                    ) { Text(label) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    showTimePicker(context, draftStart) { draftStart = it }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Start: ${formatTimeLabel(context, draftStart)}")
            }
            OutlinedButton(
                onClick = {
                    showTimePicker(context, draftEnd) { draftEnd = it }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("End: ${formatTimeLabel(context, draftEnd)}")
            }
        }
        FilledTonalButton(
            onClick = { onSaveSchedule(draftDays, draftStart, draftEnd) },
            enabled = draftDays.isNotEmpty(),
        ) {
            Text("Save schedule")
        }
    }
}

private fun showTimePicker(
    context: Context,
    initialMinutes: Int,
    onPicked: (Int) -> Unit,
) {
    TimePickerDialog(
        context,
        { _: TimePicker, hour: Int, minute: Int -> onPicked(hour * 60 + minute) },
        initialMinutes / 60,
        initialMinutes % 60,
        DateFormat.is24HourFormat(context),
    ).show()
}

@Composable
private fun PromptActionDialog(
    action: PromptAction,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = when (action) {
        PromptAction.ENTER_FOCUS -> "Focus time starting"
        PromptAction.EXIT_FOCUS -> "Focus time ended"
    }
    val body = when (action) {
        PromptAction.ENTER_FOCUS ->
            "Switch to Mum Launcher now. Android may ask you to confirm the Home app change."
        PromptAction.EXIT_FOCUS ->
            "Focus time has ended. Leave focus mode now. If Mum Launcher is your current Home app, Android will open Home settings so you can switch back."
    }
    val confirm = when (action) {
        PromptAction.ENTER_FOCUS -> "Switch now"
        PromptAction.EXIT_FOCUS -> "Leave focus now"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        },
    )
}

@Composable
private fun ToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 20.sp,
                lineHeight = 24.sp,
            )
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun PinPromptDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin PIN") },
        text = {
            Column(
                modifier = Modifier.imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(pin) }) { Text("Open") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SetAdminPinDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set admin PIN") },
        text = {
            Column(
                modifier = Modifier.imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Enter a PIN with at least 4 digits. Leave admin PIN off if triple-tap security is enough.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(pin) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ContactEditorDialog(
    title: String,
    initialName: String,
    initialPhone: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var phone by rememberSaveable { mutableStateOf(initialPhone) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, phone) }) { Text("Save") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

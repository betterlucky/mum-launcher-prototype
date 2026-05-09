package com.mumslauncher.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.app.TimePickerDialog
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PhoneForwarded
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.mumslauncher.app.data.Contact
import com.mumslauncher.app.data.ContactRepository
import com.mumslauncher.app.data.LauncherSettings
import com.mumslauncher.app.data.Preset
import com.mumslauncher.app.data.PresetRepository
import com.mumslauncher.app.data.SettingsStore
import com.mumslauncher.app.ui.theme.MumLauncherTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private enum class Screen {
    HOME,
    CALLS,
    ROTARY_DIALER,
    MESSAGES,
    ADMIN,
    RELAXED,
    PRESET_MANAGER,
    PRESET_EDITOR,
    IMPORT_CONTACTS,
}

data class InstalledApp(
    val packageName: String,
    val label: String,
)

data class AppUiState(
    val contacts: List<Contact> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
    val presets: List<Preset> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val lastError: String? = null,
)

class MainViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val contactRepository = ContactRepository(application)
    private val settingsStore = SettingsStore(application)
    private val presetRepository = PresetRepository(application)
    private val transientError = MutableStateFlow<String?>(null)
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())

    val uiState: StateFlow<AppUiState> = combine(
        contactRepository.observeContacts(),
        settingsStore.settings,
        presetRepository.observePresets(),
        _installedApps,
        transientError,
    ) { contacts, settings, presets, installedApps, error ->
        AppUiState(
            contacts = contacts,
            settings = settings,
            presets = presets,
            installedApps = installedApps,
            lastError = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    private val packageChangeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            loadInstalledApps()
        }
    }

    init {
        viewModelScope.launch {
            contactRepository.ensureSeedData()
        }
        loadInstalledApps()
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        getApplication<android.app.Application>().registerReceiver(packageChangeReceiver, filter)
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<android.app.Application>().unregisterReceiver(packageChangeReceiver)
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<android.app.Application>().packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            val apps = pm.queryIntentActivities(intent, 0)
                .filter { it.activityInfo.packageName != "com.mumslauncher.app" }
                .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
                .sortedBy { it.label.lowercase() }
                .distinctBy { it.packageName }
            _installedApps.value = apps
        }
    }

    fun clearError() {
        transientError.value = null
    }

    fun completeSetup(pin: String) {
        if (pin.isNotEmpty() && pin.length < 4) {
            transientError.value = "PIN must be at least 4 digits, or leave blank for no PIN."
            return
        }
        viewModelScope.launch {
            if (pin.isNotEmpty()) {
                val hash = withContext(Dispatchers.Default) { hashPin(pin) }
                settingsStore.setPinHash(hash)
            }
            settingsStore.setSetupComplete(true)
        }
    }

    private var failedPinAttempts = 0
    private var pinLockedUntil = 0L

    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        if (uiState.value.settings.pinHash == null) {
            onResult(true)
            return
        }
        val now = System.currentTimeMillis()
        if (now < pinLockedUntil) {
            val remainingSecs = ((pinLockedUntil - now) / 1000).coerceAtLeast(1)
            transientError.value = "Too many attempts. Try again in ${remainingSecs}s."
            onResult(false)
            return
        }
        viewModelScope.launch {
            val valid = withContext(Dispatchers.Default) {
                verifyPinHash(pin, uiState.value.settings.pinHash)
            }
            if (valid) {
                failedPinAttempts = 0
                pinLockedUntil = 0L
                onResult(true)
            } else {
                failedPinAttempts++
                val lockMs = when {
                    failedPinAttempts >= 9 -> 5 * 60 * 1000L
                    failedPinAttempts >= 6 -> 30 * 1000L
                    failedPinAttempts >= 3 -> 5 * 1000L
                    else -> 0L
                }
                if (lockMs > 0) {
                    pinLockedUntil = now + lockMs
                    transientError.value = "Incorrect PIN. Locked for ${lockMs / 1000}s."
                } else {
                    transientError.value = "Incorrect PIN."
                }
                onResult(false)
            }
        }
    }

    fun setPhoneTitle(title: String) {
        viewModelScope.launch { settingsStore.setPhoneTitle(title) }
    }

    fun setDarkMode(pref: com.mumslauncher.app.data.DarkModePreference) {
        viewModelScope.launch { settingsStore.setDarkMode(pref) }
    }

    fun setAllowUserEditing(allowed: Boolean) {
        viewModelScope.launch { settingsStore.setAllowUserContactEditing(allowed) }
    }

    fun setShowRelaxedButton(show: Boolean) {
        viewModelScope.launch { settingsStore.setShowRelaxedButton(show) }
    }

    fun setShowHelpButton(show: Boolean) {
        viewModelScope.launch { settingsStore.setShowHelpButton(show) }
    }

    fun setHelpContactId(id: Long?) {
        viewModelScope.launch { settingsStore.setHelpContactId(id) }
    }

    fun setAllowUserAddRelaxedApps(allow: Boolean) {
        viewModelScope.launch { settingsStore.setAllowUserAddRelaxedApps(allow) }
    }

    fun setSimpleApps(apps: List<String>) {
        viewModelScope.launch { settingsStore.setSimpleApps(apps) }
    }

    fun setRelaxedScrollHorizontal(horizontal: Boolean) {
        viewModelScope.launch { settingsStore.setRelaxedScrollHorizontal(horizontal) }
    }

    fun setUseRotaryDialer(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setUseRotaryDialer(enabled) }
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

    fun importContacts(entries: List<Pair<String, String>>) {
        viewModelScope.launch { contactRepository.addAll(entries) }
    }

    fun restoreContacts(entries: List<Pair<String, String>>) {
        viewModelScope.launch { contactRepository.replaceAll(entries) }
    }

    fun restoreContactsFull(contacts: List<com.mumslauncher.app.data.Contact>) {
        viewModelScope.launch { contactRepository.restoreAll(contacts) }
    }

    fun setNativeLauncher(pkg: String, label: String) {
        viewModelScope.launch { settingsStore.setNativeLauncher(pkg, label) }
    }

    private val _diagnostics = MutableStateFlow<Diagnostics?>(null)
    internal val diagnostics: StateFlow<Diagnostics?> = _diagnostics

    fun refreshDiagnostics() {
        _diagnostics.value = buildDiagnosticsFromApp(getApplication())
    }

    fun createPreset(name: String, apps: List<String>, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = presetRepository.add(name, apps)
            onCreated(id)
        }
    }

    fun updatePreset(preset: Preset) {
        viewModelScope.launch { presetRepository.update(preset) }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch { presetRepository.delete(id) }
    }

    fun setSchedulingEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setSchedulingEnabled(enabled)
            SchedulePromptController.sync(context, settingsStore.settings.first())
        }
    }

    fun setScheduleDays(context: Context, days: Set<Int>) {
        viewModelScope.launch {
            settingsStore.setScheduleDays(days)
            SchedulePromptController.sync(context, settingsStore.settings.first())
        }
    }

    fun setScheduleStartMinutes(context: Context, minutes: Int) {
        viewModelScope.launch {
            settingsStore.setScheduleStartMinutes(minutes)
            val current = settingsStore.settings.first()
            if (current.scheduleEndMinutes <= minutes) {
                settingsStore.setScheduleEndMinutes(minutes + 30)
            }
            SchedulePromptController.sync(context, settingsStore.settings.first())
        }
    }

    fun setScheduleEndMinutes(context: Context, minutes: Int) {
        viewModelScope.launch {
            val current = settingsStore.settings.first()
            if (minutes <= current.scheduleStartMinutes) {
                transientError.value = "End time must be after start time."
                return@launch
            }
            settingsStore.setScheduleEndMinutes(minutes)
            SchedulePromptController.sync(context, settingsStore.settings.first())
        }
    }

    fun setScheduledMode(mode: com.mumslauncher.app.data.LauncherMode) {
        viewModelScope.launch { settingsStore.setScheduledMode(mode) }
    }

    fun setScheduleAudioAlert(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setScheduleAudioAlert(enabled) }
    }

    fun setAllowUserSkipSession(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAllowUserSkipSession(enabled) }
    }

    fun setAllowUserDelaySession(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAllowUserDelaySession(enabled) }
    }

    fun setAllowUserExtendSession(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAllowUserExtendSession(enabled) }
    }

    fun acknowledgePrompt(context: Context, action: PromptAction, anchor: String) {
        viewModelScope.launch {
            when (action) {
                PromptAction.ENTER_FOCUS -> settingsStore.setFocusSession(true, anchor)
                PromptAction.EXIT_FOCUS -> settingsStore.setFocusSession(false, null)
            }
            SchedulePromptController.sync(context, settingsStore.settings.first())
        }
    }

    fun skipCurrentWindow(context: Context) {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            val window = currentScheduledWindow(settings, System.currentTimeMillis())
            val untilMs = window?.endMs ?: (System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
            settingsStore.setScheduleSkippedUntil(untilMs)
            settingsStore.setFocusSession(false, null)
            SchedulePromptController.sync(context, settingsStore.settings.first())
        }
    }

    fun delaySessionStart(context: Context) {
        viewModelScope.launch {
            settingsStore.setScheduleSkippedUntil(System.currentTimeMillis() + 15 * 60 * 1000L)
            SchedulePromptController.sync(context, settingsStore.settings.first())
        }
    }

    fun extendCurrentSession(context: Context) {
        viewModelScope.launch {
            settingsStore.setScheduleSkippedUntil(System.currentTimeMillis() + 30 * 60 * 1000L)
            SchedulePromptController.sync(context, settingsStore.settings.first())
        }
    }

    private fun hashPin(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        return "$saltHex:${pbkdf2Hash(pin, salt)}"
    }

    private fun verifyPinHash(pin: String, stored: String?): Boolean {
        if (stored == null) return false
        return if (':' in stored) {
            val colonIdx = stored.indexOf(':')
            val saltHex = stored.substring(0, colonIdx)
            val expectedHash = stored.substring(colonIdx + 1)
            val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            pbkdf2Hash(pin, salt) == expectedHash
        } else {
            // Legacy SHA-256 fallback for existing installs
            MessageDigest.getInstance("SHA-256")
                .digest(pin.toByteArray())
                .joinToString("") { "%02x".format(it) } == stored
        }
    }

    private fun pbkdf2Hash(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 100_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
            .joinToString("") { "%02x".format(it) }
    }
}

class MainActivity : ComponentActivity() {
    private val rehideSystemUi = Runnable { applySystemUi() }

    val pendingPromptAction = kotlinx.coroutines.flow.MutableStateFlow<PromptAction?>(null)
    var relaxedModeActive: Boolean = false
    var lightThemeActive: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingPromptAction.value = PromptAction.from(intent?.getStringExtra(EXTRA_PROMPT_ACTION))
        enableEdgeToEdge()
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
            val viewModel: MainViewModel = viewModel(
                factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application),
            )
            LauncherApp(
                activity = this,
                viewModel = viewModel,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPromptAction.value = PromptAction.from(intent.getStringExtra(EXTRA_PROMPT_ACTION))
    }

    override fun onResume() {
        super.onResume()
        window.decorView.removeCallbacks(rehideSystemUi)
        applySystemUi()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.removeCallbacks(rehideSystemUi)
            applySystemUi()
        }
    }

    fun applySystemUi() {
        val controller = window.insetsController ?: return
        if (relaxedModeActive) {
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
            controller.show(WindowInsets.Type.statusBars())
            controller.hide(WindowInsets.Type.navigationBars())
            if (lightThemeActive) {
                controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                )
            } else {
                controller.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                )
            }
        } else {
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsets.Type.systemBars())
        }
    }

    fun hideSystemUi() {
        relaxedModeActive = false
        applySystemUi()
    }
}

@Composable
private fun LauncherApp(
    activity: MainActivity,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()

    val darkTheme = when (uiState.settings.darkMode) {
        com.mumslauncher.app.data.DarkModePreference.LIGHT -> false
        com.mumslauncher.app.data.DarkModePreference.DARK -> true
        com.mumslauncher.app.data.DarkModePreference.SYSTEM -> isSystemDark
    }

    MumLauncherTheme(darkTheme = darkTheme) {
        LauncherContent(activity = activity, viewModel = viewModel, uiState = uiState, darkTheme = darkTheme)
    }
}

@Composable
private fun LauncherContent(
    activity: MainActivity,
    viewModel: MainViewModel,
    uiState: AppUiState,
    darkTheme: Boolean,
) {
    val context = LocalContext.current
    val diagnostics by viewModel.diagnostics.collectAsState()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var showPinPrompt by rememberSaveable { mutableStateOf(false) }
    var isDefault by remember { mutableStateOf(isDefaultLauncher(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefault = isDefaultLauncher(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val otherLaunchers = remember { getOtherLaunchers(context) }
    var showEditingDialog by remember { mutableStateOf<Contact?>(null) }
    var isCreatingContact by remember { mutableStateOf(false) }
    var activePresetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingPresetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showFirstRunPreset by remember { mutableStateOf(false) }
    var showPresetPicker by remember { mutableStateOf(false) }
    var showNewPresetDialog by remember { mutableStateOf(false) }
    var dialerReturnScreen by remember { mutableStateOf(Screen.CALLS) }

    LaunchedEffect(Unit) { activity.hideSystemUi() }

    LaunchedEffect(isDefault, uiState.settings.setupComplete) {
        if (!isDefault && uiState.settings.setupComplete && screen == Screen.HOME) {
            if (uiState.settings.pinHash == null) screen = Screen.ADMIN
            else showPinPrompt = true
        }
    }

    var callPhoneGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CALL_PHONE,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val callPhoneLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> callPhoneGranted = granted }

    var readContactsGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_CONTACTS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val readContactsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        readContactsGranted = granted
        if (granted) screen = Screen.IMPORT_CONTACTS
    }

    val scope = rememberCoroutineScope()
    val importFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val json = context.contentResolver.openInputStream(uri)
                        ?.use { it.readBytes().decodeToString() } ?: return@runCatching
                    val entries = parseContactsJson(json)
                    if (entries.isNotEmpty()) viewModel.restoreContactsFull(entries)
                }
            }
        }
    }

    val isRelaxed = screen == Screen.RELAXED
    SideEffect {
        activity.relaxedModeActive = isRelaxed
        activity.lightThemeActive = !darkTheme
        activity.applySystemUi()
    }

    LaunchedEffect(uiState.lastError) {
        uiState.lastError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val duePrompt = remember(uiState.settings) {
        currentDuePrompt(uiState.settings, System.currentTimeMillis())
    }

    val promptAction by activity.pendingPromptAction.collectAsState()
    LaunchedEffect(promptAction) {
        val action = promptAction ?: return@LaunchedEffect
        activity.pendingPromptAction.value = null
        val settings = uiState.settings
        val nowMs = System.currentTimeMillis()
        val window = currentScheduledWindow(settings, nowMs)
            ?: mostRecentEndedWindow(settings, nowMs)
        val anchor = window?.anchor ?: return@LaunchedEffect
        viewModel.acknowledgePrompt(context, action, anchor)
        when (action) {
            PromptAction.ENTER_FOCUS -> when (settings.scheduledMode) {
                com.mumslauncher.app.data.LauncherMode.RELAXED -> {
                    if (uiState.presets.isNotEmpty()) {
                        activePresetId = activePresetId ?: uiState.presets.first().id
                        screen = Screen.RELAXED
                    }
                }
                com.mumslauncher.app.data.LauncherMode.SIMPLE -> screen = Screen.HOME
            }
            PromptAction.EXIT_FOCUS -> screen = Screen.HOME
        }
    }

    if (!uiState.settings.setupComplete) {
        BackHandler {}
        SetupScreen(
            otherLaunchers = otherLaunchers,
            onSetNativeLauncher = viewModel::setNativeLauncher,
            onOpenHomeSettings = { openHomeSettings(context) },
            onPinShortcut = { requestPinShortcut(context) },
            onFinishSetup = { pin -> viewModel.completeSetup(pin) },
        )
        return
    }

    BackHandler {
        screen = when (screen) {
            Screen.HOME -> Screen.HOME
            Screen.CALLS, Screen.MESSAGES, Screen.ADMIN, Screen.RELAXED -> Screen.HOME
            Screen.ROTARY_DIALER -> dialerReturnScreen
            Screen.IMPORT_CONTACTS -> Screen.ADMIN
            Screen.PRESET_MANAGER -> Screen.ADMIN
            Screen.PRESET_EDITOR -> Screen.PRESET_MANAGER
        }
    }

    fun enterRelaxed() {
        when {
            uiState.presets.isEmpty() -> showFirstRunPreset = true
            uiState.presets.size == 1 -> {
                activePresetId = uiState.presets.first().id
                screen = Screen.RELAXED
            }
            else -> showPresetPicker = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    phoneTitle = uiState.settings.phoneTitle,
                    carerContact = if (uiState.settings.showHelpButton)
                        uiState.settings.helpContactId
                            ?.let { id -> uiState.contacts.find { it.id == id } }
                            ?: uiState.contacts.firstOrNull { it.callable }
                    else null,
                    onDirectCall = { contact ->
                        if (callPhoneGranted) {
                            directCall(context, contact.phoneNumber)
                        } else {
                            callPhoneLauncher.launch(android.Manifest.permission.CALL_PHONE)
                        }
                    },
                    onOpenCalls = { screen = Screen.CALLS },
                    onOpenMessages = { screen = Screen.MESSAGES },
                    onOpenAdmin = {
                        if (uiState.settings.pinHash == null) screen = Screen.ADMIN
                        else showPinPrompt = true
                    },
                    showRelaxedButton = uiState.settings.showRelaxedButton,
                    onOpenRelaxed = { enterRelaxed() },
                    simpleApps = uiState.settings.simpleApps
                        .mapNotNull { pkg -> uiState.installedApps.find { it.packageName == pkg } },
                    onLaunchApp = { pkg -> launchApp(context, pkg) },
                    duePrompt = duePrompt,
                    allowSkip = uiState.settings.allowUserSkipSession,
                    allowDelay = uiState.settings.allowUserDelaySession,
                    onSkipSession = { viewModel.skipCurrentWindow(context) },
                    onDelaySession = { viewModel.delaySessionStart(context) },
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
                    onDial = if (uiState.settings.useRotaryDialer) {
                        { dialerReturnScreen = Screen.CALLS; screen = Screen.ROTARY_DIALER }
                    } else null,
                )

                Screen.ROTARY_DIALER -> RotaryDialerScreen(
                    onBack = { screen = dialerReturnScreen },
                    onCall = { number ->
                        if (callPhoneGranted) directCall(context, number)
                        else callPhoneLauncher.launch(android.Manifest.permission.CALL_PHONE)
                    },
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

                Screen.ADMIN -> {
                    LaunchedEffect(Unit) { viewModel.refreshDiagnostics() }
                    AdminScreen(
                        settings = uiState.settings,
                        contacts = uiState.contacts,
                        diagnostics = diagnostics,
                        onBack = { screen = Screen.HOME },
                        onSwitchToSimple = { screen = Screen.HOME },
                        onSwitchToRelaxed = { enterRelaxed() },
                        onSetPhoneTitle = viewModel::setPhoneTitle,
                        onSetDarkMode = viewModel::setDarkMode,
                        onToggleEditing = viewModel::setAllowUserEditing,
                        onToggleRelaxedButton = viewModel::setShowRelaxedButton,
                        onToggleHelpButton = viewModel::setShowHelpButton,
                        onSetHelpContactId = viewModel::setHelpContactId,
                        onToggleRelaxedScroll = { viewModel.setRelaxedScrollHorizontal(!uiState.settings.relaxedScrollHorizontal) },
                        onToggleAllowUserAddRelaxedApps = viewModel::setAllowUserAddRelaxedApps,
                        onToggleRotaryDialer = viewModel::setUseRotaryDialer,
                        installedApps = uiState.installedApps,
                        onSetSimpleApps = viewModel::setSimpleApps,
                        onAdd = { isCreatingContact = true },
                        onEdit = { showEditingDialog = it },
                        onDelete = viewModel::deleteContact,
                        onImportFromDevice = {
                            if (readContactsGranted) screen = Screen.IMPORT_CONTACTS
                            else readContactsLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        },
                        onExportContacts = { exportContacts(context, uiState.contacts) },
                        onImportFromFile = { importFileLauncher.launch("*/*") },
                        isDefaultLauncher = isDefault,
                        onSetNativeLauncher = viewModel::setNativeLauncher,
                        onUsePhoneNormally = uiState.settings.nativeLauncherPackage?.let { pkg ->
                            { openNativeLauncher(context, pkg) }
                        },
                        onOpenPresets = { screen = Screen.PRESET_MANAGER },
                        onOpenHomeSettings = { openHomeSettings(context) },
                        onOpenAccessibilitySettings = { openAccessibilitySettings(context) },
                        onSetSchedulingEnabled = { viewModel.setSchedulingEnabled(context, it) },
                        onSetScheduleDays = { viewModel.setScheduleDays(context, it) },
                        onSetScheduleStart = { viewModel.setScheduleStartMinutes(context, it) },
                        onSetScheduleEnd = { viewModel.setScheduleEndMinutes(context, it) },
                        onSetScheduledMode = { viewModel.setScheduledMode(it) },
                        onSetAudioAlert = { viewModel.setScheduleAudioAlert(it) },
                        onSetAllowSkip = { viewModel.setAllowUserSkipSession(it) },
                        onSetAllowDelay = { viewModel.setAllowUserDelaySession(it) },
                        onSetAllowExtend = { viewModel.setAllowUserExtendSession(it) },
                        onOpenNotificationSettings = { SchedulePromptController.openNotificationSettings(context) },
                        onOpenAlarmSettings = { SchedulePromptController.openExactAlarmSettings(context) },
                    )
                }

                Screen.RELAXED -> {
                    val activePreset = uiState.presets.find { it.id == activePresetId }
                    if (activePreset != null) {
                        RelaxedHomeScreen(
                            preset = activePreset,
                            installedApps = uiState.installedApps,
                            scrollHorizontal = uiState.settings.relaxedScrollHorizontal,
                            showSwitchButton = uiState.presets.size > 1,
                            onBack = { screen = Screen.HOME },
                            onSwitchPreset = { showPresetPicker = true },
                            onOpenAdmin = {
                                if (uiState.settings.pinHash == null) screen = Screen.ADMIN
                                else showPinPrompt = true
                            },
                            duePrompt = duePrompt,
                            allowSkip = uiState.settings.allowUserSkipSession,
                            allowDelay = uiState.settings.allowUserDelaySession,
                            allowExtend = uiState.settings.allowUserExtendSession,
                            onSkipSession = { viewModel.skipCurrentWindow(context) },
                            onDelaySession = { viewModel.delaySessionStart(context) },
                            onExtendSession = { viewModel.extendCurrentSession(context) },
                            allowAddApps = uiState.settings.allowUserAddRelaxedApps,
                            onUpdatePreset = viewModel::updatePreset,
                            onOpenRotaryDialer = if (uiState.settings.useRotaryDialer) {
                                { dialerReturnScreen = Screen.RELAXED; screen = Screen.ROTARY_DIALER }
                            } else null,
                        )
                    } else {
                        screen = Screen.HOME
                    }
                }

                Screen.PRESET_MANAGER -> PresetManagerScreen(
                    presets = uiState.presets,
                    onBack = { screen = Screen.ADMIN },
                    onEdit = { preset ->
                        editingPresetId = preset.id
                        screen = Screen.PRESET_EDITOR
                    },
                    onDelete = { viewModel.deletePreset(it.id) },
                    onNewPreset = { showNewPresetDialog = true },
                )

                Screen.PRESET_EDITOR -> {
                    val editingPreset = uiState.presets.find { it.id == editingPresetId }
                    if (editingPreset != null) {
                        PresetEditorScreen(
                            preset = editingPreset,
                            installedApps = uiState.installedApps,
                            onBack = { screen = Screen.PRESET_MANAGER },
                            onUpdatePreset = viewModel::updatePreset,
                        )
                    } else {
                        screen = Screen.PRESET_MANAGER
                    }
                }

                Screen.IMPORT_CONTACTS -> ImportContactsScreen(
                    existingNumbers = uiState.contacts.map { normalizeNumber(it.phoneNumber) }.toSet(),
                    onBack = { screen = Screen.ADMIN },
                    onImport = { entries ->
                        viewModel.importContacts(entries)
                        screen = Screen.ADMIN
                    },
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
                    }
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

    if (showFirstRunPreset) {
        FirstRunPresetDialog(
            onConfirm = { name, startWithAll ->
                showFirstRunPreset = false
                val apps = if (startWithAll) uiState.installedApps.map { it.packageName } else emptyList()
                viewModel.createPreset(name, apps) { newId ->
                    activePresetId = newId
                    screen = Screen.RELAXED
                }
            },
            onDismiss = { showFirstRunPreset = false },
        )
    }

    if (showPresetPicker) {
        PresetPickerDialog(
            presets = uiState.presets,
            selectedId = activePresetId,
            onSelect = { preset ->
                activePresetId = preset.id
                showPresetPicker = false
                screen = Screen.RELAXED
            },
            onDismiss = { showPresetPicker = false },
        )
    }

    if (showNewPresetDialog) {
        NewPresetNameDialog(
            onConfirm = { name, startWithAll ->
                showNewPresetDialog = false
                val apps = if (startWithAll) uiState.installedApps.map { it.packageName } else emptyList()
                viewModel.createPreset(name, apps) { newId ->
                    editingPresetId = newId
                    screen = Screen.PRESET_EDITOR
                }
            },
            onDismiss = { showNewPresetDialog = false },
        )
    }
}

internal data class Diagnostics(
    val batteryPercent: Int,
    val networkSummary: String,
    val dialerPackage: String?,
    val smsPackage: String?,
)

private fun buildDiagnosticsFromApp(context: Context): Diagnostics {
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
    val pm = context.packageManager
    val dialerPackage = Intent(Intent.ACTION_DIAL)
        .let { pm.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) }
        ?.activityInfo?.packageName
    val smsPackage = Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:"))
        .let { pm.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) }
        ?.activityInfo?.packageName
    return Diagnostics(
        batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
        networkSummary = networkSummary,
        dialerPackage = dialerPackage,
        smsPackage = smsPackage,
    )
}

private fun isDefaultLauncher(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return info?.activityInfo?.packageName == context.packageName
}

private fun getOtherLaunchers(context: Context): List<Pair<String, String>> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return context.packageManager.queryIntentActivities(intent, 0)
        .mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || pkg == "android") null
            else Pair(pkg, info.loadLabel(context.packageManager).toString())
        }
        .distinctBy { it.first }
}

private fun openHomeSettings(context: Context) {
    val intent = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, "Home app settings are not available on this phone.", Toast.LENGTH_LONG).show()
        }
}

private fun requestPinShortcut(context: Context) {
    val sm = context.getSystemService(ShortcutManager::class.java)
    if (!sm.isRequestPinShortcutSupported) return
    val shortcut = ShortcutInfo.Builder(context, "main_shortcut")
        .setShortLabel(context.getString(R.string.app_name))
        .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(Intent(context, MainActivity::class.java).apply { action = Intent.ACTION_MAIN })
        .build()
    sm.requestPinShortcut(shortcut, null)
}

private fun openNativeLauncher(context: Context, packageName: String) {
    val homeIntent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_HOME)
        .setPackage(packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val fallback = context.packageManager.getLaunchIntentForPackage(packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val intent = if (context.packageManager.resolveActivity(homeIntent, 0) != null) homeIntent else fallback
    if (intent != null) {
        runCatching { context.startActivity(intent) }
            .onFailure {
                Toast.makeText(context, "Could not open the previous launcher.", Toast.LENGTH_SHORT).show()
            }
    } else {
        Toast.makeText(context, "Previous launcher is no longer installed.", Toast.LENGTH_SHORT).show()
    }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager
        .getLaunchIntentForPackage(packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (intent != null) runCatching { context.startActivity(intent) }
}

private fun launchDialer(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    launchExternalIntent(context, intent, "No phone app is available.")
}

private fun directCall(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(phoneNumber)}"))
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

private fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) return bitmap
    val bmp = Bitmap.createBitmap(
        intrinsicWidth.coerceAtLeast(1),
        intrinsicHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bmp
}

// ── Setup ─────────────────────────────────────────────────────────────────────

@Composable
private fun SetupScreen(
    otherLaunchers: List<Pair<String, String>>,
    onSetNativeLauncher: (String, String) -> Unit,
    onOpenHomeSettings: () -> Unit,
    onPinShortcut: () -> Unit,
    onFinishSetup: (String) -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var showLauncherPicker by rememberSaveable { mutableStateOf(false) }

    if (showLauncherPicker) {
        AlertDialog(
            onDismissRequest = { showLauncherPicker = false },
            title = { Text("Choose the normal launcher") },
            text = {
                Column {
                    otherLaunchers.forEach { (pkg, label) ->
                        TextButton(
                            onClick = {
                                onSetNativeLauncher(pkg, label)
                                showLauncherPicker = false
                                onOpenHomeSettings()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(label, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLauncherPicker = false }) { Text("Cancel") }
            },
        )
    }

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
            text = "Set up Dial It Back",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Use this screen once, before handing over the phone. The prototype uses the system Phone and Messages apps, but keeps contacts local to the launcher.",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 18.sp,
        )
        SetupStepCard(
            title = "1. Set this as the home app",
            body = "Choose Dial It Back as the default home app so the phone always returns here.",
            actionLabel = "Open Home Settings",
            onAction = {
                when (otherLaunchers.size) {
                    0 -> onOpenHomeSettings()
                    1 -> { onSetNativeLauncher(otherLaunchers[0].first, otherLaunchers[0].second); onOpenHomeSettings() }
                    else -> showLauncherPicker = true
                }
            },
        )
        SetupStepCard(
            title = "2. Pin to home screen",
            body = "Adds a shortcut to the home screen so carers and family can get back here easily.",
            actionLabel = "Pin shortcut",
            onAction = onPinShortcut,
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

// ── Home ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(
    phoneTitle: String,
    carerContact: Contact?,
    onDirectCall: (Contact) -> Unit,
    onOpenCalls: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenAdmin: () -> Unit,
    showRelaxedButton: Boolean,
    onOpenRelaxed: () -> Unit,
    simpleApps: List<InstalledApp> = emptyList(),
    onLaunchApp: (String) -> Unit = {},
    duePrompt: DuePrompt? = null,
    allowSkip: Boolean = false,
    allowDelay: Boolean = false,
    onSkipSession: () -> Unit = {},
    onDelaySession: () -> Unit = {},
) {

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableStateOf(0L) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header — same 64dp height as Relaxed screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
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
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = phoneTitle,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Scrollable body — centers when content fits, scrolls when apps overflow
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
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
            if (carerContact != null) {
                HomeActionButton(
                    label = "HELP",
                    icon = Icons.Outlined.PhoneForwarded,
                    color = MaterialTheme.colorScheme.error,
                    onClick = { onDirectCall(carerContact) },
                )
            }
            if (showRelaxedButton) {
                FilledTonalButton(
                    onClick = onOpenRelaxed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Relaxed Mode", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (duePrompt?.action == PromptAction.ENTER_FOCUS) {
                if (allowSkip) {
                    OutlinedButton(
                        onClick = onSkipSession,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) { Text("Skip session", fontSize = 18.sp) }
                }
                if (allowDelay) {
                    OutlinedButton(
                        onClick = onDelaySession,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) { Text("Not yet — 15 more minutes", fontSize = 18.sp) }
                }
            }
            if (simpleApps.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                simpleApps.forEach { app ->
                    SimpleAppButton(app = app, onClick = { onLaunchApp(app.packageName) })
                }
            }
        }
    }
}

@Composable
private fun SimpleAppButton(app: InstalledApp, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        AppIconImage(
            packageName = app.packageName,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
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

// ── Rotary Dialer ─────────────────────────────────────────────────────────────

@Composable
private fun RotaryDialerScreen(
    onBack: () -> Unit,
    onCall: (String) -> Unit,
) {
    var digits by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxHeight(),
            ) { Text("Back", fontSize = 18.sp) }
            Text(
                text = "Dial",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.size(70.dp))
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = digits.ifEmpty { " " },
                    modifier = Modifier.weight(1f),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    maxLines = 1,
                )
                if (digits.isNotEmpty()) {
                    TextButton(onClick = { digits = digits.dropLast(1) }) {
                        Text("⌫", fontSize = 24.sp)
                    }
                }
            }
        }

        RotaryDial(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onDigitDialled = { digit -> if (digits.length < 15) digits += digit },
        )

        Button(
            onClick = { if (digits.isNotEmpty()) onCall(digits) },
            enabled = digits.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text("Call", fontSize = 22.sp)
        }
    }
}

// ── Contacts ──────────────────────────────────────────────────────────────────

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
    onDial: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxHeight(),
            ) { Text("Back", fontSize = 18.sp) }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDial != null) {
                    FilledTonalButton(onClick = onDial) { Text("Dial") }
                }
                if (canEdit) {
                    FilledTonalButton(onClick = onAdd) { Text("Add") }
                } else if (onDial == null) {
                    Spacer(modifier = Modifier.size(70.dp))
                }
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
    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete contact") },
            text = { Text("Delete ${contact.displayName}?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
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
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

// ── Import contacts from device ───────────────────────────────────────────────

private data class DevicePhoneEntry(val displayName: String, val number: String)

private fun normalizeNumber(raw: String) =
    raw.filter { it.isDigit() || it == '+' }.trimStart('0').ifEmpty { raw }

@Composable
private fun ImportContactsScreen(
    existingNumbers: Set<String>,
    onBack: () -> Unit,
    onImport: (List<Pair<String, String>>) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var allEntries by remember { mutableStateOf<List<DevicePhoneEntry>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<DevicePhoneEntry>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val entries = withContext(kotlinx.coroutines.Dispatchers.IO) {
            val result = mutableListOf<DevicePhoneEntry>()
            val cursor = context.contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                null, null,
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC",
            )
            cursor?.use {
                val nameCol = it.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = it.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    result += DevicePhoneEntry(
                        displayName = it.getString(nameCol) ?: continue,
                        number = it.getString(numCol) ?: continue,
                    )
                }
            }
            result
        }
        allEntries = entries
        loading = false
    }

    val filtered = remember(allEntries, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) allEntries
        else allEntries.filter { it.displayName.lowercase().contains(q) || it.number.contains(q) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxHeight()) {
                Text("Back", fontSize = 18.sp)
            }
            Text(
                "Phone contacts",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            TextButton(
                onClick = { onImport(selected.map { it.displayName to it.number }) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxHeight(),
            ) { Text("Add ${if (selected.isEmpty()) "" else "(${selected.size})"}", fontSize = 16.sp) }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Search") },
            singleLine = true,
        )

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (query.isEmpty()) "No contacts on device." else "No matches.")
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(filtered, key = { "${it.displayName}|${it.number}" }) { entry ->
                    val alreadyImported = normalizeNumber(entry.number) in existingNumbers
                    val isSelected = entry in selected
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !alreadyImported) {
                                selected = if (isSelected) selected - entry else selected + entry
                            },
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = when {
                                alreadyImported -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = isSelected || alreadyImported,
                                onCheckedChange = null,
                                enabled = !alreadyImported,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.displayName,
                                    fontSize = 16.sp,
                                    color = if (alreadyImported)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    entry.number,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = if (alreadyImported) 0.3f else 0.6f,
                                    ),
                                )
                            }
                            if (alreadyImported) {
                                Text(
                                    "Added",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Contact backup helpers ─────────────────────────────────────────────────────

private fun exportContacts(context: Context, contacts: List<Contact>) {
    if (contacts.isEmpty()) {
        Toast.makeText(context, "No contacts to export.", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val array = org.json.JSONArray()
        contacts.forEach { c ->
            array.put(org.json.JSONObject().apply {
                put("name", c.displayName)
                put("number", c.phoneNumber)
                put("callable", c.callable)
                put("messageable", c.messageable)
            })
        }
        val exportDir = java.io.File(context.filesDir, "exports").also { it.mkdirs() }
        val file = java.io.File(exportDir, "dial_it_back_contacts.json")
        file.writeText(array.toString(2))
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Dial It Back contacts backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share contacts backup")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        Toast.makeText(context, "Export failed: ${it.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun parseContactsJson(json: String): List<Contact> = runCatching {
    val array = org.json.JSONArray(json)
    List(array.length()) { i ->
        val obj = array.getJSONObject(i)
        Contact(
            displayName = obj.optString("name"),
            phoneNumber = obj.optString("number"),
            callable = obj.optBoolean("callable", true),
            messageable = obj.optBoolean("messageable", true),
        )
    }.filter { it.displayName.isNotBlank() && it.phoneNumber.isNotBlank() }
}.getOrElse { emptyList() }

// ── Admin ─────────────────────────────────────────────────────────────────────

@Composable
private fun AdminScreen(
    settings: LauncherSettings,
    contacts: List<Contact>,
    diagnostics: Diagnostics?,
    onBack: () -> Unit,
    onSwitchToSimple: () -> Unit,
    onSwitchToRelaxed: () -> Unit,
    onSetPhoneTitle: (String) -> Unit,
    onSetDarkMode: (com.mumslauncher.app.data.DarkModePreference) -> Unit,
    onToggleEditing: (Boolean) -> Unit,
    onToggleRelaxedButton: (Boolean) -> Unit,
    onToggleHelpButton: (Boolean) -> Unit,
    onSetHelpContactId: (Long?) -> Unit,
    onToggleRelaxedScroll: () -> Unit,
    onToggleAllowUserAddRelaxedApps: (Boolean) -> Unit,
    onToggleRotaryDialer: (Boolean) -> Unit,
    installedApps: List<InstalledApp>,
    onSetSimpleApps: (List<String>) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Contact) -> Unit,
    onDelete: (Contact) -> Unit,
    onImportFromDevice: () -> Unit,
    onExportContacts: () -> Unit,
    onImportFromFile: () -> Unit,
    isDefaultLauncher: Boolean,
    onSetNativeLauncher: (String, String) -> Unit,
    onUsePhoneNormally: (() -> Unit)?,
    onOpenPresets: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSetSchedulingEnabled: (Boolean) -> Unit,
    onSetScheduleDays: (Set<Int>) -> Unit,
    onSetScheduleStart: (Int) -> Unit,
    onSetScheduleEnd: (Int) -> Unit,
    onSetScheduledMode: (com.mumslauncher.app.data.LauncherMode) -> Unit,
    onSetAudioAlert: (Boolean) -> Unit,
    onSetAllowSkip: (Boolean) -> Unit,
    onSetAllowDelay: (Boolean) -> Unit,
    onSetAllowExtend: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAlarmSettings: () -> Unit,
) {
    val context = LocalContext.current
    val otherLaunchers = remember { getOtherLaunchers(context) }
    var showAdminLauncherPicker by rememberSaveable { mutableStateOf(false) }

    if (showAdminLauncherPicker) {
        AlertDialog(
            onDismissRequest = { showAdminLauncherPicker = false },
            title = { Text("Full access launcher") },
            text = {
                Column {
                    otherLaunchers.forEach { (pkg, label) ->
                        TextButton(
                            onClick = {
                                onSetNativeLauncher(pkg, label)
                                showAdminLauncherPicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(label, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAdminLauncherPicker = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxHeight(),
            ) { Text("Back", fontSize = 18.sp) }
            Text(
                text = "Admin",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(72.dp))
        }

        if (!isDefaultLauncher) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Not set as home screen.",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    FilledTonalButton(onClick = onOpenHomeSettings) { Text("Restore") }
                }
            }
        }

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Switch mode", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = onSwitchToSimple,
                        modifier = Modifier.weight(1f),
                    ) { Text("Simple") }
                    FilledTonalButton(
                        onClick = onSwitchToRelaxed,
                        modifier = Modifier.weight(1f),
                    ) { Text("Relaxed") }
                    FilledTonalButton(
                        onClick = { onUsePhoneNormally?.invoke() },
                        enabled = onUsePhoneNormally != null,
                        modifier = Modifier.weight(1f),
                    ) { Text("Full access") }
                }
                Text(
                    "Pressing Home always returns to Dial It Back.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Text("Dark mode", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        com.mumslauncher.app.data.DarkModePreference.LIGHT to "Light",
                        com.mumslauncher.app.data.DarkModePreference.DARK to "Dark",
                        com.mumslauncher.app.data.DarkModePreference.SYSTEM to "Device",
                    ).forEach { (pref, label) ->
                        FilterChip(
                            selected = settings.darkMode == pref,
                            onClick = { onSetDarkMode(pref) },
                            label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        CollapsibleSection("Simple mode") {
            PhoneTitleCard(
                title = settings.phoneTitle,
                onSave = onSetPhoneTitle,
            )
            ToggleCard(
                title = "Allow contact editing",
                checked = settings.allowUserContactEditing,
                onCheckedChange = onToggleEditing,
            )
            ToggleCard(
                title = "Show Relaxed mode button",
                checked = settings.showRelaxedButton,
                onCheckedChange = onToggleRelaxedButton,
            )
            ToggleCard(
                title = "Show HELP button",
                checked = settings.showHelpButton,
                onCheckedChange = onToggleHelpButton,
            )
            if (settings.showHelpButton) {
                HelpContactPickerCard(
                    contacts = contacts.filter { it.callable },
                    selectedId = settings.helpContactId,
                    onSelect = onSetHelpContactId,
                )
            }
            ToggleCard(
                title = "Use rotary dialler for phone number entry",
                checked = settings.useRotaryDialer,
                onCheckedChange = onToggleRotaryDialer,
            )
            SimpleAppsCard(
                simpleApps = settings.simpleApps,
                installedApps = installedApps,
                onSetSimpleApps = onSetSimpleApps,
            )
        }

        CollapsibleSection("Relaxed mode") {
            ToggleCard(
                title = "Scroll grid horizontally (page by page)",
                checked = settings.relaxedScrollHorizontal,
                onCheckedChange = { onToggleRelaxedScroll() },
            )
            ToggleCard(
                title = "Allow adding apps from home screen",
                checked = settings.allowUserAddRelaxedApps,
                onCheckedChange = onToggleAllowUserAddRelaxedApps,
            )
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Create and edit app collections (presets) for Relaxed mode.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    FilledTonalButton(onClick = onOpenPresets, modifier = Modifier.fillMaxWidth()) {
                        Text("Manage presets")
                    }
                }
            }
        }

        CollapsibleSection("Full access") {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "When Full access is active, pressing Home goes to the selected launcher. Open the app icon to return here.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Launcher: ${settings.nativeLauncherLabel ?: "not set"}",
                            modifier = Modifier.weight(1f),
                        )
                        if (otherLaunchers.isNotEmpty()) {
                            TextButton(onClick = { showAdminLauncherPicker = true }) {
                                Text(if (settings.nativeLauncherLabel != null) "Change" else "Set")
                            }
                        }
                    }
                }
            }
        }

        CollapsibleSection(
            "Schedule",
            trailing = {
                Switch(
                    checked = settings.schedulingEnabled,
                    onCheckedChange = onSetSchedulingEnabled,
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
        ) {
            if (settings.schedulingEnabled) ScheduleCard(
                settings = settings,
                onSetSchedulingEnabled = onSetSchedulingEnabled,
                onSetScheduleDays = onSetScheduleDays,
                onSetScheduleStart = onSetScheduleStart,
                onSetScheduleEnd = onSetScheduleEnd,
                onSetScheduledMode = onSetScheduledMode,
                onSetAudioAlert = onSetAudioAlert,
                onSetAllowSkip = onSetAllowSkip,
                onSetAllowDelay = onSetAllowDelay,
                onSetAllowExtend = onSetAllowExtend,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onOpenAlarmSettings = onOpenAlarmSettings,
            )
        }

        CollapsibleSection("Contacts") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onAdd, modifier = Modifier.weight(1f)) { Text("Add") }
                FilledTonalButton(onClick = onImportFromDevice, modifier = Modifier.weight(1f)) { Text("From phone") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExportContacts, modifier = Modifier.weight(1f)) { Text("Export backup") }
                OutlinedButton(onClick = onImportFromFile, modifier = Modifier.weight(1f)) { Text("Restore backup") }
            }
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

        CollapsibleSection("System") {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = onOpenHomeSettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Home Settings")
                    }
                    FilledTonalButton(onClick = onOpenAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Accessibility Settings")
                    }
                }
            }
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Diagnostics", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    if (diagnostics == null) {
                        Text("Loading…")
                    } else {
                        Text("Battery: ${diagnostics.batteryPercent}%")
                        Text("Network: ${diagnostics.networkSummary}")
                        Text("Dialer app: ${diagnostics.dialerPackage ?: "Not found"}")
                        Text("Messages app: ${diagnostics.smsPackage ?: "Not found"}")
                    }
                }
            }
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Recovery", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("If the PIN is lost, clear app data in Android Settings to reset everything.")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleCard(
    settings: LauncherSettings,
    onSetSchedulingEnabled: (Boolean) -> Unit,
    onSetScheduleDays: (Set<Int>) -> Unit,
    onSetScheduleStart: (Int) -> Unit,
    onSetScheduleEnd: (Int) -> Unit,
    onSetScheduledMode: (com.mumslauncher.app.data.LauncherMode) -> Unit,
    onSetAudioAlert: (Boolean) -> Unit,
    onSetAllowSkip: (Boolean) -> Unit,
    onSetAllowDelay: (Boolean) -> Unit,
    onSetAllowExtend: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAlarmSettings: () -> Unit,
) {
    val context = LocalContext.current
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        TimePickerDialog(
            context,
            { _, h, m -> onSetScheduleStart(h * 60 + m); showStartPicker = false },
            settings.scheduleStartMinutes / 60,
            settings.scheduleStartMinutes % 60,
            true,
        ).apply { setOnDismissListener { showStartPicker = false } }.show()
    }
    if (showEndPicker) {
        TimePickerDialog(
            context,
            { _, h, m -> onSetScheduleEnd(h * 60 + m); showEndPicker = false },
            settings.scheduleEndMinutes / 60,
            settings.scheduleEndMinutes % 60,
            true,
        ).apply { setOnDismissListener { showEndPicker = false } }.show()
    }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (settings.schedulingEnabled) {
                Text("Days", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                val dayLabels = listOf(
                    Calendar.MONDAY to "Mon",
                    Calendar.TUESDAY to "Tue",
                    Calendar.WEDNESDAY to "Wed",
                    Calendar.THURSDAY to "Thu",
                    Calendar.FRIDAY to "Fri",
                    Calendar.SATURDAY to "Sat",
                    Calendar.SUNDAY to "Sun",
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dayLabels.forEach { (day, label) ->
                        FilterChip(
                            selected = settings.scheduleDays.contains(day),
                            onClick = {
                                val newDays = if (settings.scheduleDays.contains(day)) {
                                    settings.scheduleDays - day
                                } else {
                                    settings.scheduleDays + day
                                }
                                onSetScheduleDays(newDays)
                            },
                            label = { Text(label) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        OutlinedButton(
                            onClick = { showStartPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "%02d:%02d".format(
                                    settings.scheduleStartMinutes / 60,
                                    settings.scheduleStartMinutes % 60,
                                ),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("End", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        OutlinedButton(
                            onClick = { showEndPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "%02d:%02d".format(
                                    settings.scheduleEndMinutes / 60,
                                    settings.scheduleEndMinutes % 60,
                                ),
                            )
                        }
                    }
                }

                Text("Switch to", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.mumslauncher.app.data.LauncherMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.scheduledMode == mode,
                            onClick = { onSetScheduledMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        com.mumslauncher.app.data.LauncherMode.SIMPLE -> "Simple"
                                        com.mumslauncher.app.data.LauncherMode.RELAXED -> "Relaxed"
                                    },
                                )
                            },
                        )
                    }
                }

                ToggleCard(
                    title = "Audio alert at transition",
                    checked = settings.scheduleAudioAlert,
                    onCheckedChange = onSetAudioAlert,
                )
                ToggleCard(
                    title = "Allow person to skip a scheduled session",
                    checked = settings.allowUserSkipSession,
                    onCheckedChange = onSetAllowSkip,
                )
                ToggleCard(
                    title = "Allow person to delay a session start (15 min)",
                    checked = settings.allowUserDelaySession,
                    onCheckedChange = onSetAllowDelay,
                )
                ToggleCard(
                    title = "Allow person to extend a session (30 min)",
                    checked = settings.allowUserExtendSession,
                    onCheckedChange = onSetAllowExtend,
                )

                if (!SchedulePromptController.canPostNotifications(context)) {
                    FilledTonalButton(
                        onClick = onOpenNotificationSettings,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Enable notifications for session prompts")
                    }
                }
                if (!SchedulePromptController.canScheduleExactAlarms(context)) {
                    var showAlarmInfo by remember { mutableStateOf(false) }
                    if (showAlarmInfo) {
                        AlertDialog(
                            onDismissRequest = { showAlarmInfo = false },
                            title = { Text("Clock access") },
                            text = {
                                Text(
                                    "This permission lets the app access the phone's precise clock. Without it, session prompts may appear a few minutes late. It doesn't affect anything outside this app.",
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { showAlarmInfo = false; onOpenAlarmSettings() }) {
                                    Text("Grant access")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAlarmInfo = false }) { Text("Not now") }
                            },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick = onOpenAlarmSettings,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Grant clock access (recommended)")
                        }
                        IconButton(onClick = { showAlarmInfo = true }) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = "What is this?",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    defaultExpanded: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(defaultExpanded) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { expanded = !expanded }
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            trailing?.invoke(this)
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
            )
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
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
private fun PhoneTitleCard(title: String, onSave: (String) -> Unit) {
    var draft by remember(title) { mutableStateOf(title) }
    val isDirty = draft.trim() != title
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Phone name", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Shown at the top of the Simple home screen.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. Mum's Phone") },
            )
            if (isDirty) {
                Button(
                    onClick = { onSave(draft.trim()) },
                    modifier = Modifier.align(Alignment.End),
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun HelpContactPickerCard(
    contacts: List<Contact>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("HELP button calls", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (contacts.isEmpty()) {
                Text(
                    "No callable contacts yet. Add contacts below.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                contacts.forEach { contact ->
                    val selected = contact.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(contact.id) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = selected, onClick = { onSelect(contact.id) })
                        Column {
                            Text(contact.displayName, fontSize = 16.sp)
                            Text(
                                contact.phoneNumber,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
                if (contacts.size > 1 && selectedId == null) {
                    Text(
                        "None selected — will use the first callable contact.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

// ── Relaxed home ──────────────────────────────────────────────────────────────

@Composable
private fun RelaxedHomeScreen(
    preset: Preset,
    installedApps: List<InstalledApp>,
    scrollHorizontal: Boolean,
    showSwitchButton: Boolean,
    onBack: () -> Unit,
    onSwitchPreset: () -> Unit,
    onOpenAdmin: () -> Unit = {},
    onUpdatePreset: ((Preset) -> Unit)? = null,
    duePrompt: DuePrompt? = null,
    allowSkip: Boolean = false,
    allowDelay: Boolean = false,
    allowExtend: Boolean = false,
    onSkipSession: () -> Unit = {},
    onDelaySession: () -> Unit = {},
    onExtendSession: () -> Unit = {},
    allowAddApps: Boolean = false,
    onOpenRotaryDialer: (() -> Unit)? = null,
) {
    var showExtendDialog by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }

    fun swapItems(a: Int, b: Int) {
        val list = preset.apps.toMutableList()
        val tmp = list[a]; list[a] = list[b]; list[b] = tmp
        onUpdatePreset?.invoke(preset.copy(apps = list))
        selectedIndex = null
    }

    fun removeItem(idx: Int) {
        val list = preset.apps.toMutableList()
        list.removeAt(idx)
        onUpdatePreset?.invoke(preset.copy(apps = list))
        selectedIndex = null
    }

    fun insertGap(atIdx: Int) {
        val list = preset.apps.toMutableList()
        list.add(atIdx, "")
        onUpdatePreset?.invoke(preset.copy(apps = list))
        selectedIndex = null
    }

    fun onItemTap(idx: Int) {
        val sel = selectedIndex
        when {
            sel == idx -> selectedIndex = null
            sel != null -> swapItems(sel, idx)
            else -> selectedIndex = idx
        }
    }

    if (showExtendDialog) {
        AlertDialog(
            onDismissRequest = { showExtendDialog = false },
            title = { Text("Extend session?") },
            text = { Text("Keep this mode for another 30 minutes before switching.") },
            confirmButton = {
                TextButton(onClick = { onExtendSession(); showExtendDialog = false }) {
                    Text("Extend 30 min")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExtendDialog = false }) { Text("Cancel") }
            },
        )
    }

    val context = LocalContext.current

    // null = empty slot, non-null = app (uninstalled packages are dropped)
    val gridItems: List<IndexedValue<InstalledApp?>> = preset.apps.mapIndexedNotNull { i, pkg ->
        when {
            pkg.isEmpty() -> IndexedValue(i, null)
            else -> installedApps.find { it.packageName == pkg }?.let { IndexedValue(i, it) }
        }
    }

    fun launchApp(packageName: String) {
        val intent = context.packageManager
            .getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent != null) runCatching { context.startActivity(intent) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxHeight(),
            ) { Text("Simple", fontSize = 18.sp) }
            Text(
                text = preset.name,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenAdmin),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            if (editMode) {
                TextButton(onClick = { editMode = false; selectedIndex = null }) {
                    Text("Done", fontSize = 16.sp)
                }
            } else if (allowExtend && duePrompt?.action == PromptAction.EXIT_FOCUS) {
                TextButton(onClick = { showExtendDialog = true }) {
                    Text("Extend", fontSize = 16.sp)
                }
            } else if (showSwitchButton) {
                IconButton(onClick = onSwitchPreset) {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = "Switch preset")
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        if (editMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!allowAddApps) {
                    Text(
                        "To add apps, use Admin → Manage Presets",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                OutlinedButton(onClick = { insertGap(selectedIndex ?: preset.apps.size) }) {
                    Text("Gap")
                }
                if (allowAddApps) {
                    OutlinedButton(onClick = { showAppPicker = true }) {
                        Text("Add app")
                    }
                }
            }
        }

        if (duePrompt?.action == PromptAction.ENTER_FOCUS && (allowSkip || allowDelay)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (allowSkip) {
                    OutlinedButton(
                        onClick = onSkipSession,
                        modifier = Modifier.weight(1f),
                    ) { Text("Skip session") }
                }
                if (allowDelay) {
                    OutlinedButton(
                        onClick = onDelaySession,
                        modifier = Modifier.weight(1f),
                    ) { Text("15 more min") }
                }
            }
        }

        if (gridItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Text("No apps in this preset yet.", fontSize = 18.sp, textAlign = TextAlign.Center)
                    Text(
                        "Add apps via Admin → Manage Presets.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        } else if (scrollHorizontal) {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val colCount = (maxWidth / 96.dp).toInt().coerceAtLeast(2)
                val rowCount = (maxHeight / 120.dp).toInt().coerceAtLeast(2)
                val appsPerPage = colCount * rowCount
                val pages = gridItems.chunked(appsPerPage)
                val pagerState = rememberPagerState(pageCount = { pages.size })

                Column(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                    ) { pageIndex ->
                        val pageItems = pages.getOrElse(pageIndex) { emptyList() }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            pageItems.chunked(colCount).forEach { rowItems ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    rowItems.forEach { (idx, app) ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (app != null) {
                                                AppGridItem(
                                                    app = app,
                                                    onClick = { if (editMode) onItemTap(idx) else launchApp(app.packageName) },
                                                    editMode = editMode,
                                                    selected = selectedIndex == idx,
                                                    onLongPress = { editMode = true; selectedIndex = idx },
                                                    onRemove = { removeItem(idx) },
                                                )
                                            } else {
                                                EmptyGridSlot(
                                                    editMode = editMode,
                                                    selected = selectedIndex == idx,
                                                    onClick = { if (editMode) onItemTap(idx) },
                                                    onRemove = { removeItem(idx) },
                                                )
                                            }
                                        }
                                    }
                                    repeat(colCount - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                                }
                            }
                        }
                    }

                    if (pages.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            pages.indices.forEach { i ->
                                Box(
                                    modifier = Modifier
                                        .size(if (i == pagerState.currentPage) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (i == pagerState.currentPage)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        ),
                                )
                                if (i < pages.lastIndex) Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                gridItems(gridItems, key = { it.index }) { (idx, app) ->
                    if (app != null) {
                        AppGridItem(
                            app = app,
                            onClick = { if (editMode) onItemTap(idx) else launchApp(app.packageName) },
                            editMode = editMode,
                            selected = selectedIndex == idx,
                            onLongPress = { editMode = true; selectedIndex = idx },
                            onRemove = { removeItem(idx) },
                        )
                    } else {
                        EmptyGridSlot(
                            editMode = editMode,
                            selected = selectedIndex == idx,
                            onClick = { if (editMode) onItemTap(idx) },
                            onRemove = { removeItem(idx) },
                        )
                    }
                }
            }
        }
    } // end Column

    if (onOpenRotaryDialer != null && !editMode) {
        FloatingActionButton(
            onClick = onOpenRotaryDialer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Outlined.Call, contentDescription = "Rotary dialler")
        }
    }

    // App picker overlay (shown in edit mode when allowAddApps is true)
    if (showAppPicker) {
        val insertAt = selectedIndex?.let { it + 1 } ?: preset.apps.size
        RelaxedAppPickerOverlay(
            installedApps = installedApps,
            excludedPackages = preset.apps.filter { it.isNotEmpty() }.toSet(),
            onSelect = { pkg ->
                val list = preset.apps.toMutableList()
                list.add(insertAt.coerceAtMost(list.size), pkg)
                onUpdatePreset?.invoke(preset.copy(apps = list))
                showAppPicker = false
                selectedIndex = null
            },
            onDismiss = { showAppPicker = false },
        )
    }
    } // end Box
}

// ── Simple mode app picker ─────────────────────────────────────────────────────

@Composable
private fun SimpleAppsCard(
    simpleApps: List<String>,
    installedApps: List<InstalledApp>,
    onSetSimpleApps: (List<String>) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Simple screen apps", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Add extra apps (games, notes, etc.) below the main buttons on the Simple screen.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            if (simpleApps.isNotEmpty()) {
                simpleApps.forEach { pkg ->
                    val app = installedApps.find { it.packageName == pkg }
                    val label = app?.label ?: pkg
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (app != null) {
                                AppIconImage(
                                    packageName = pkg,
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
                                )
                            }
                            Text(label, fontSize = 16.sp)
                        }
                        IconButton(onClick = { onSetSimpleApps(simpleApps - pkg) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove $label")
                        }
                    }
                }
            }
            FilledTonalButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (simpleApps.isEmpty()) "Add apps" else "Edit apps")
            }
        }
    }

    if (showPicker) {
        SimpleAppPickerOverlay(
            installedApps = installedApps,
            selectedPackages = simpleApps.toSet(),
            onDone = { selected ->
                // Preserve existing order, append newly added ones alphabetically
                val existing = simpleApps.filter { it in selected }
                val added = selected
                    .filter { it !in simpleApps }
                    .sortedBy { pkg -> installedApps.find { it.packageName == pkg }?.label?.lowercase() ?: pkg }
                onSetSimpleApps(existing + added)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun SimpleAppPickerOverlay(
    installedApps: List<InstalledApp>,
    selectedPackages: Set<String>,
    onDone: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var current by remember { mutableStateOf(selectedPackages) }

    val filtered = remember(installedApps, query) {
        val q = query.trim().lowercase()
        installedApps
            .let { if (q.isEmpty()) it else it.filter { a -> a.label.lowercase().contains(q) } }
            .sortedBy { it.label.lowercase() }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxHeight()) {
                    Text("Cancel", fontSize = 18.sp)
                }
                Text(
                    "Simple screen apps",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = { onDone(current) }, modifier = Modifier.fillMaxHeight()) {
                    Text("Done", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search apps") },
                singleLine = true,
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val checked = app.packageName in current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                current = if (checked) current - app.packageName
                                          else current + app.packageName
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                current = if (it) current + app.packageName
                                           else current - app.packageName
                            },
                        )
                        AppIconImage(
                            packageName = app.packageName,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)),
                        )
                        Text(app.label, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RelaxedAppPickerOverlay(
    installedApps: List<InstalledApp>,
    excludedPackages: Set<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val available = remember(installedApps, excludedPackages, query) {
        val q = query.trim().lowercase()
        installedApps
            .filter { it.packageName !in excludedPackages }
            .let { if (q.isEmpty()) it else it.filter { a -> a.label.lowercase().contains(q) } }
            .sortedBy { it.label.lowercase() }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxHeight()) {
                    Text("Cancel", fontSize = 18.sp)
                }
                Text(
                    "Add app",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(80.dp))
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search apps") },
                singleLine = true,
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(available, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(app.packageName) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AppIconImage(
                            packageName = app.packageName,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                        )
                        Text(app.label, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
    app: InstalledApp,
    onClick: () -> Unit,
    editMode: Boolean = false,
    selected: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    Box {
        Column(
            modifier = Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongPress)
                .padding(8.dp)
                .then(
                    if (selected) Modifier.background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp),
                    ) else Modifier,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppIconImage(
                packageName = app.packageName,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Text(
                text = app.label,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp,
            )
        }
        if (editMode && onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = MaterialTheme.colorScheme.onError, fontSize = 14.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun EmptyGridSlot(
    editMode: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    Box {
        Column(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(8.dp)
                .then(
                    if (selected) Modifier.background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp),
                    ) else Modifier,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .then(
                        if (editMode) Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                        else Modifier,
                    ),
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
        if (editMode && onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = MaterialTheme.colorScheme.onError, fontSize = 14.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun AppIconImage(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap()
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap!!, contentDescription = null, modifier = modifier)
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

// ── Preset manager ────────────────────────────────────────────────────────────

@Composable
private fun PresetManagerScreen(
    presets: List<Preset>,
    onBack: () -> Unit,
    onEdit: (Preset) -> Unit,
    onDelete: (Preset) -> Unit,
    onNewPreset: () -> Unit,
) {
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxHeight(),
            ) { Text("Back", fontSize = 18.sp) }
            Text(
                text = "Presets",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(72.dp))
        }

        FilledTonalButton(onClick = onNewPreset, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New preset")
        }

        if (presets.isEmpty()) {
            Card {
                Text(
                    text = "No presets yet. Create one to set up a Relaxed mode app collection.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp,
                )
            }
        }

        presets.forEach { preset ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preset.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        val appCount = preset.apps.count { it.isNotEmpty() }
                        val spaceCount = preset.apps.count { it.isEmpty() }
                        Text(
                            buildString {
                                append("$appCount app${if (appCount == 1) "" else "s"}")
                                if (spaceCount > 0) append(", $spaceCount space${if (spaceCount == 1) "" else "s"}")
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    IconButton(onClick = { onEdit(preset) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { confirmDeleteId = preset.id }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }

    confirmDeleteId?.let { id ->
        val preset = presets.find { it.id == id }
        if (preset != null) {
            AlertDialog(
                onDismissRequest = { confirmDeleteId = null },
                title = { Text("Delete \"${preset.name}\"?") },
                text = { Text("This removes the preset and its app list.") },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete(preset)
                        confirmDeleteId = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") }
                },
            )
        }
    }
}

// ── Preset editor ─────────────────────────────────────────────────────────────

@Composable
private fun PresetEditorScreen(
    preset: Preset,
    installedApps: List<InstalledApp>,
    onBack: () -> Unit,
    onUpdatePreset: (Preset) -> Unit,
) {
    var localName by rememberSaveable(preset.id) { mutableStateOf(preset.name) }

    LaunchedEffect(localName) {
        if (localName.isBlank()) return@LaunchedEffect
        delay(500L)
        if (localName != preset.name) onUpdatePreset(preset.copy(name = localName))
    }

    val appsInPreset = preset.apps.filter { it.isNotEmpty() }.toSet()
    val appsToAdd = installedApps.filter { it.packageName !in appsInPreset }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxHeight(),
            ) { Text("Back", fontSize = 18.sp) }
            Text(
                text = "Edit Preset",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(72.dp))
        }

        OutlinedTextField(
            value = localName,
            onValueChange = { localName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text("Preset name") },
            singleLine = true,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "In preset",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    TextButton(onClick = { onUpdatePreset(preset.copy(apps = preset.apps + "")) }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add space", fontSize = 14.sp)
                    }
                }
            }

            if (preset.apps.isEmpty()) {
                item {
                    Text(
                        "No apps yet — add from the list below.",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }

            itemsIndexed(preset.apps, key = { i, _ -> i }) { index, pkg ->
                val app = if (pkg.isNotEmpty()) installedApps.find { it.packageName == pkg } else null
                PresetItemRow(
                    pkg = pkg,
                    app = app,
                    canMoveUp = index > 0,
                    canMoveDown = index < preset.apps.lastIndex,
                    onMoveUp = {
                        val list = preset.apps.toMutableList()
                        list.add(index - 1, list.removeAt(index))
                        onUpdatePreset(preset.copy(apps = list))
                    },
                    onMoveDown = {
                        val list = preset.apps.toMutableList()
                        list.add(index + 1, list.removeAt(index))
                        onUpdatePreset(preset.copy(apps = list))
                    },
                    onRemove = {
                        onUpdatePreset(preset.copy(apps = preset.apps.toMutableList().also { it.removeAt(index) }))
                    },
                )
            }

            if (appsToAdd.isNotEmpty()) {
                item {
                    Text(
                        "Add apps",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                items(appsToAdd, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconImage(
                            packageName = app.packageName,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = app.label, modifier = Modifier.weight(1f), fontSize = 16.sp)
                        IconButton(onClick = { onUpdatePreset(preset.copy(apps = preset.apps + app.packageName)) }) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetItemRow(
    pkg: String,
    app: InstalledApp?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            pkg.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("·  ·  ·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Space",
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            app != null -> {
                AppIconImage(
                    packageName = app.packageName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = app.label, modifier = Modifier.weight(1f), fontSize = 16.sp)
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Not installed",
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Column {
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(20.dp))
            }
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Outlined.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

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

@Composable
private fun FirstRunPresetDialog(
    onConfirm: (name: String, startWithAll: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(1) }
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set up Relaxed mode") },
        text = {
            if (step == 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Give your first preset a name.")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding(),
                        label = { Text("Preset name") },
                        singleLine = true,
                        placeholder = { Text("e.g. My Apps") },
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("How would you like to start \"${name.trim()}\"?")
                    FilledTonalButton(
                        onClick = { onConfirm(name.trim(), true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add all installed apps") }
                    OutlinedButton(
                        onClick = { onConfirm(name.trim(), false) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Start blank") }
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                TextButton(
                    onClick = { step = 2 },
                    enabled = name.isNotBlank(),
                ) { Text("Next") }
            }
        },
        dismissButton = {
            if (step == 2) {
                TextButton(onClick = { step = 1 }) { Text("Back") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun PresetPickerDialog(
    presets: List<Preset>,
    selectedId: Long?,
    onSelect: (Preset) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose preset") },
        text = {
            LazyColumn {
                items(presets, key = { it.id }) { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(preset) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (preset.id == selectedId) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(preset.name, fontSize = 18.sp)
                            val appCount = preset.apps.count { it.isNotEmpty() }
                            Text(
                                "$appCount app${if (appCount == 1) "" else "s"}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun NewPresetNameDialog(
    onConfirm: (name: String, startWithAll: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var startWithAll by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    label = { Text("Name") },
                    singleLine = true,
                )
                Text(
                    text = "Start from",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = !startWithAll,
                        onClick = { startWithAll = false },
                        label = { Text("Blank") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = startWithAll,
                        onClick = { startWithAll = true },
                        label = { Text("All apps") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), startWithAll) },
                enabled = name.isNotBlank(),
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

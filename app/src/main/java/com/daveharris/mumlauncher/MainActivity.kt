package com.daveharris.mumlauncher

import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.daveharris.mumlauncher.data.LauncherSettings
import com.daveharris.mumlauncher.data.SettingsStore
import com.daveharris.mumlauncher.ui.theme.MumLauncherTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest

private enum class Screen {
    HOME,
    CALLS,
    MESSAGES,
    ADMIN,
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
        if (pin.length < 4) {
            transientError.value = "Choose a PIN with at least 4 digits."
            return
        }
        viewModelScope.launch {
            settingsStore.setPinHash(hashPin(pin))
            settingsStore.setSetupComplete(true)
        }
    }

    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        onResult(hashPin(pin) == uiState.value.settings.pinHash)
    }

    fun setAllowUserEditing(allowed: Boolean) {
        viewModelScope.launch { settingsStore.setAllowUserContactEditing(allowed) }
    }

    fun setKioskEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setKioskEnabled(enabled) }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        window.decorView.removeCallbacks(rehideSystemUi)
        hideSystemUi()
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
}

@Composable
private fun LauncherApp(
    activity: MainActivity,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var showPinPrompt by rememberSaveable { mutableStateOf(false) }
    var showEditingDialog by remember { mutableStateOf<Contact?>(null) }
    var isCreatingContact by remember { mutableStateOf(false) }

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
                    onOpenCalls = { screen = Screen.CALLS },
                    onOpenMessages = { screen = Screen.MESSAGES },
                    onOpenAdmin = { showPinPrompt = true },
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
                    contacts = uiState.contacts,
                    diagnostics = buildDiagnostics(context),
                    onBack = { screen = Screen.HOME },
                    onToggleEditing = viewModel::setAllowUserEditing,
                    onToggleKiosk = viewModel::setKioskEnabled,
                    onAdd = { isCreatingContact = true },
                    onEdit = { showEditingDialog = it },
                    onDelete = viewModel::deleteContact,
                    onOpenHomeSettings = { openHomeSettings(context) },
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
            text = "Use this screen once, before handing over the phone. The prototype uses the system Phone and Messages apps, but keeps contacts local to the launcher.",
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
                label = { Text("Admin PIN") },
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

        Spacer(modifier = Modifier.weight(1.15f))
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
    contacts: List<Contact>,
    diagnostics: Diagnostics,
    onBack: () -> Unit,
    onToggleEditing: (Boolean) -> Unit,
    onToggleKiosk: (Boolean) -> Unit,
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

        ToggleCard(
            title = "Allow contact editing in normal mode",
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
                Text("Device admin: ${if (diagnostics.isDeviceAdminEnabled) "Enabled" else "Not enabled"}")
                Text("Device owner: ${if (diagnostics.isDeviceOwner) "Enabled" else "Not enabled"}")
                Text("Lock task allowed: ${if (diagnostics.isLockTaskPermitted) "Yes" else "No"}")
                Text("Dialer app: ${diagnostics.dialerPackage ?: "Not found"}")
                Text("Messages app: ${diagnostics.smsPackage ?: "Not found"}")
                Text("Kiosk allowlist: ${diagnostics.allowlistedPackages.joinToString()}")
                FilledTonalButton(onClick = onOpenHomeSettings) { Text("Open Home Settings") }
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

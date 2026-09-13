package br.f21campo.app

import android.os.Bundle
import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import org.json.JSONObject
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField as MaterialOutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import br.f21campo.files.RawFileStore
import androidx.room.Room
import br.f21campo.data.F21Database
import br.f21campo.data.ProjectStationRepository
import br.f21campo.data.DatabaseExchangeJsonCodec
import br.f21campo.data.Migrations
import br.f21campo.domain.EntityId
import br.f21campo.domain.Station
import br.f21campo.domain.Occupation
import br.f21campo.domain.OccupationState
import br.f21campo.domain.OccupationStateMachine
import br.f21campo.domain.Receiver
import br.f21campo.domain.Antenna
import br.f21campo.domain.ManualEquipment
import br.f21campo.domain.HeightObservation
import br.f21campo.domain.HeightPhase
import br.f21campo.domain.HeightSet
import br.f21campo.domain.HeightType
import br.f21campo.domain.DomainResult
import br.f21campo.domain.ReferencePoint
import br.f21campo.domain.ReferencePointType
import br.f21campo.domain.OccupationEvent
import br.f21campo.domain.OccupationEventCategory
import br.f21campo.domain.EventSeverity
import br.f21campo.domain.AuditEvent
import br.f21campo.domain.StoredFile
import br.f21campo.domain.HeightMeasurement
import br.f21campo.domain.TrackingTimer
import br.f21campo.domain.OccupationReadiness
import br.f21campo.domain.ReadinessInput
import br.f21campo.domain.ReceiverCatalogItem
import br.f21campo.domain.AntennaCatalogItem
import br.f21campo.receiver.api.ReceiverTransportType
import br.f21campo.receiver.api.ReceiverConnectionProfile
import br.f21campo.receiver.api.TcpReceiverTransport
import br.f21campo.receiver.manual.ManualReceiverConnection
import java.time.Instant
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.withContext

private fun heightToMeters(value: Double, unit: String?): Double? = when (unit) {
    "mm" -> value / 1000.0
    "cm" -> value / 100.0
    "m" -> value
    else -> null
}

private fun heightUnitFromObservation(observation: String?): String? =
    observation?.substringAfter("unit=", missingDelimiterValue = "")?.takeIf { it in setOf("mm", "cm", "m") }

private fun heightFromMeters(valueMeters: Double, unit: String?): Double = when (unit) {
    "mm" -> valueMeters * 1000.0
    "cm" -> valueMeters * 100.0
    else -> valueMeters
}

private fun heightUnitHint(readings: List<String>, selectedUnit: String?): String? {
    val first = readings.firstOrNull { it.isNotBlank() }?.trim() ?: return null
    if (selectedUnit != null) return null
    val digitsOnly = first.all(Char::isDigit)
    return when {
        digitsOnly && first.length >= 4 -> "Leitura com 4+ dígitos: selecione mm se o valor foi anotado em milímetros."
        digitsOnly && first.length == 3 -> "Leitura com 3 dígitos: confirme a unidade ou informe onde está a vírgula."
        else -> "Selecione a unidade da altura antes de registrar."
    }
}

private fun bluetoothRuntimePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

private fun hasBluetoothRuntimePermissions(context: Context): Boolean =
    bluetoothRuntimePermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

private fun isLocationEnabledForBluetoothDiscovery(context: Context): Boolean {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) return true
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return true
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        locationManager.isLocationEnabled
    } else {
        @Suppress("DEPRECATION")
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

/**
 * Keeps the focused field above the IME. This is deliberately applied to every
 * text field because field forms can be longer than the visible viewport.
 */
@Composable
private fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val requester = remember { BringIntoViewRequester() }
    val focusScope = androidx.compose.runtime.rememberCoroutineScope()
    MaterialOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier
            .bringIntoViewRequester(requester)
            .onFocusEvent { focusState ->
                if (focusState.isFocused) {
                    focusScope.launch {
                        delay(180)
                        requester.bringIntoView()
                    }
                }
            },
    )
}

private data class HeightStatistics(val meanMeters: Double, val rangeMeters: Double)

private fun heightStatistics(values: List<HeightMeasurement>): HeightStatistics? {
    val measurements = values.map(HeightMeasurement::valueMeters).filter { it.isFinite() }
    if (measurements.isEmpty()) return null
    return HeightStatistics(
        meanMeters = measurements.average(),
        rangeMeters = measurements.max() - measurements.min(),
    )
}

@Composable
private fun StepHeader(step: Int, title: String, detail: String, fieldBlueDark: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("NOVO RASTREIO", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text("ETAPA $step/7 · $title", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = Color(0xFFD5EAF5))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                (1..7).forEach { index ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (index <= step) Color.White else Color(0x665A86A0),
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            index.toString(),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            color = if (index <= step) fieldBlueDark else Color(0xFFD5EAF5),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldTopBar(
    title: String,
    subtitle: String,
    showBack: Boolean,
    onBack: () -> Unit,
    fieldBlueDark: Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = fieldBlueDark),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showBack) {
                OutlinedButton(onClick = onBack) { Text("‹", color = Color.White, style = MaterialTheme.typography.titleLarge) }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = Color(0xFFD5EAF5), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                BuildConfig.BUILD_MODE,
                color = Color(0xFFD5EAF5),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun EventCaptureControls(
    category: OccupationEventCategory,
    onCategoryChange: (OccupationEventCategory) -> Unit,
    severity: EventSeverity,
    onSeverityChange: (EventSeverity) -> Unit,
    fieldBlueDark: Color,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("CLASSIFICAÇÃO DO EVENTO", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OccupationEventCategory.entries.forEach { item ->
                    OutlinedButton(onClick = { onCategoryChange(item) }, enabled = category != item) { Text(item.name) }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EventSeverity.entries.forEach { item ->
                    OutlinedButton(onClick = { onSeverityChange(item) }, enabled = severity != item) { Text(item.name) }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(applicationContext, F21Database::class.java, "f21.db")
            .addMigrations(*Migrations.ALL)
            .build()
        setContent {
            StationScreen(
                ProjectStationRepository(
                    database.projectDao(),
                    database.stationDao(),
                    database.referencePointDao(),
                    database.occupationDao(),
                    database.occupationArtifactDao(),
                    database.occupationEventDao(),
                    database.heightMeasurementDao(),
                    database.receiverConnectionProfileDao(),
                    database,
                    database.receiverCatalogDao(),
                    database.antennaCatalogDao(),
                    database.auditEventDao(),
                    database.occupationSnapshotDao(),
                ),
            )
        }
    }
}

@Composable
private fun StationScreen(repository: ProjectStationRepository) {
    val context = LocalContext.current
    val rawStore = remember { RawFileStore(File(context.filesDir, "raw")) }
    var name by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    var municipality by remember { mutableStateOf("") }
    var savedId by remember { mutableStateOf<EntityId?>(null) }
    var status by remember { mutableStateOf("Banco de Estações") }
    var occupation by remember { mutableStateOf(Occupation(EntityId.new(), EntityId.new(), EntityId.new())) }
    var occupationPersisted by remember { mutableStateOf(false) }
    var receiverModel by remember { mutableStateOf("") }
    var antennaModel by remember { mutableStateOf("") }
    var receiverManufacturer by remember { mutableStateOf("") }
    var receiverFirmware by remember { mutableStateOf("") }
    var antennaManufacturer by remember { mutableStateOf("") }
    var receiverSerial by remember { mutableStateOf("") }
    var antennaSerial by remember { mutableStateOf("") }
    var before by remember { mutableStateOf(listOf("")) }
    var after by remember { mutableStateOf(listOf("")) }
    var beforeUndo by remember { mutableStateOf(emptyList<List<String>>()) }
    var afterUndo by remember { mutableStateOf(emptyList<List<String>>()) }
    var beforeUnit by remember { mutableStateOf<String?>(null) }
    var afterUnit by remember { mutableStateOf<String?>(null) }
    var beforeType by remember { mutableStateOf(HeightType.VERTICAL) }
    var afterType by remember { mutableStateOf(HeightType.VERTICAL) }
    var event by remember { mutableStateOf("") }
    var fieldEvents by remember { mutableStateOf(emptyList<OccupationEvent>()) }
    var auditEvents by remember { mutableStateOf(emptyList<AuditEvent>()) }
    var referenceCode by remember { mutableStateOf("") }
    var referenceType by remember { mutableStateOf(ReferencePointType.RN) }
    var eventCategory by remember { mutableStateOf(OccupationEventCategory.NOTE) }
    var eventSeverity by remember { mutableStateOf(EventSeverity.INFO) }
    var showHome by remember { mutableStateOf(true) }
    var route by remember { mutableStateOf("HOME") }
    var newStep by remember { mutableStateOf(1) }
    var projects by remember { mutableStateOf(emptyList<br.f21campo.domain.Project>()) }
    var stations by remember { mutableStateOf(emptyList<Station>()) }
    var projectSearch by remember { mutableStateOf("") }
    var stationSearch by remember { mutableStateOf("") }
    var projectEditorId by remember { mutableStateOf<EntityId?>(null) }
    var projectEditorCreatedAt by remember { mutableStateOf<Instant?>(null) }
    var stationEditorCreatedAt by remember { mutableStateOf<Instant?>(null) }
    var referencePoints by remember { mutableStateOf(emptyList<ReferencePoint>()) }
    var stationHistory by remember { mutableStateOf(emptyList<Occupation>()) }
    var reviewedOccupation by remember { mutableStateOf<Occupation?>(null) }
    var reviewedProjectName by remember { mutableStateOf("") }
    var reviewedStationName by remember { mutableStateOf("") }
    var reviewedReference by remember { mutableStateOf<ReferencePoint?>(null) }
    var reviewedHeights by remember { mutableStateOf(emptyList<HeightMeasurement>()) }
    var reviewedEvents by remember { mutableStateOf(emptyList<OccupationEvent>()) }
    var reviewedRawSummary by remember { mutableStateOf<String?>(null) }
    var pendingOccupationSummary by remember { mutableStateOf<String?>(null) }
    var rawImported by remember { mutableStateOf(false) }
    var rawSummary by remember { mutableStateOf<String?>(null) }
    var rawArtifacts by remember { mutableStateOf(emptyList<StoredFile>()) }
    var afterRegistered by remember { mutableStateOf(false) }
    var durationMinutes by remember { mutableStateOf("") }
    var trackingTimeAlerted by remember { mutableStateOf(false) }
    var connectionTransport by remember { mutableStateOf(ReceiverTransportType.WIFI_TCP) }
    var connectionHost by remember { mutableStateOf("") }
    var connectionPort by remember { mutableStateOf("") }
    var bluetoothName by remember { mutableStateOf("") }
    var bluetoothMac by remember { mutableStateOf("") }
    var selectedBluetoothMac by remember { mutableStateOf("") }
    var pairedBluetoothDevices by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    val discoveredBluetoothDevices = remember { mutableStateListOf<Pair<String, String>>() }
    var bluetoothDiscoveryStatus by remember { mutableStateOf("Ainda não consultado") }
    var bluetoothScanning by remember { mutableStateOf(false) }
    var bluetoothServiceUuids by remember { mutableStateOf("") }
    var bluetoothServiceUuidInput by remember { mutableStateOf("") }
    var bluetoothTransportStatus by remember { mutableStateOf("Canal Bluetooth não aberto") }
    var bluetoothChannelOpen by remember { mutableStateOf(false) }
    val bluetoothSocketHolder = remember { arrayOfNulls<BluetoothSocket>(1) }
    var connectionNotes by remember { mutableStateOf("") }
    var connectionStatus by remember { mutableStateOf("Nenhum perfil de conexão testado") }
    var connectionProfiles by remember { mutableStateOf(emptyList<ReceiverConnectionProfile>()) }
    var favoriteReceiverProfiles by remember { mutableStateOf(emptyList<ReceiverConnectionProfile>()) }
    var receiverCatalog by remember { mutableStateOf(emptyList<ReceiverCatalogItem>()) }
    var antennaCatalog by remember { mutableStateOf(emptyList<AntennaCatalogItem>()) }
    var selectedReceiverCatalogId by remember { mutableStateOf<EntityId?>(null) }
    var selectedAntennaCatalogId by remember { mutableStateOf<EntityId?>(null) }
    val manualConnection = remember { ManualReceiverConnection() }
    val tcpTransport = remember { TcpReceiverTransport() }
    var nowEpochMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val recordAudit: (String) -> Unit = { action ->
        scope.launch {
            repository.saveAudit(AuditEvent(EntityId.new(), Instant.now(), action, "operator", occupation.id))
            auditEvents = repository.findAuditEvents(occupation.id)
        }
    }
    val updateBluetoothDiscoveryStatus by rememberUpdatedState<(String) -> Unit> { message -> bluetoothDiscoveryStatus = message }
    val bluetoothDiscoveryReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        try {
                            @Suppress("DEPRECATION")
                            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                            val entry = device?.let { it.name.orEmpty() to it.address.orEmpty() }
                            if (entry != null && entry.second.isNotBlank()) {
                                val previousIndex = discoveredBluetoothDevices.indexOfFirst { it.second == entry.second }
                                if (previousIndex >= 0) {
                                    discoveredBluetoothDevices[previousIndex] = entry
                                } else {
                                    discoveredBluetoothDevices += entry
                                }
                                discoveredBluetoothDevices.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.first.ifBlank { it.second } })
                            }
                        } catch (_: SecurityException) {
                            bluetoothScanning = false
                            updateBluetoothDiscoveryStatus("O Android bloqueou a leitura do dispositivo. Autorize Dispositivos próximos.")
                        }
                    }
                    android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        bluetoothScanning = false
                        updateBluetoothDiscoveryStatus("Busca concluída. Para conectar, pareie o receptor nas configurações Android e depois selecione-o no F-21.")
                    }
                }
            }
        }
    }
    DisposableEffect(tcpTransport) {
        onDispose {
            tcpTransport.close()
            try { bluetoothSocketHolder[0]?.close() } catch (_: IOException) { }
            bluetoothSocketHolder[0] = null
        }
    }
    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        // ACTION_FOUND/DISCOVERY_FINISHED are emitted by the Android Bluetooth
        // service, so the context receiver must accept system-originated events.
        ContextCompat.registerReceiver(context, bluetoothDiscoveryReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose { context.unregisterReceiver(bluetoothDiscoveryReceiver) }
    }
    BackHandler(enabled = route != "HOME") {
        if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep > 1) {
            newStep -= 1
        } else {
            route = "HOME"
            showHome = true
        }
    }
    val rawPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val imported = File.createTempFile("import-", ".part", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input -> imported.outputStream().use { output -> input.copyTo(output) } }
            val stored = rawStore.import(imported)
            imported.delete()
            scope.launch {
                repository.saveRawArtifact(occupation.id, stored.path.absolutePath, stored.sizeBytes, stored.sha256)
                recordAudit("RAW_IMPORTED_SHA256")
                rawImported = true
                rawArtifacts = repository.findRawArtifacts(occupation.id)
                rawSummary = "${stored.sha256.take(12)} · ${stored.sizeBytes} bytes"
                status = "Bruto RAW_RECEIVER importado: ${stored.sha256.take(12)}…"
            }
        }
    }
    val databaseExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val envelope = repository.createDatabaseExchangeEnvelope()
                    val json = DatabaseExchangeJsonCodec.encode(envelope)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        ?: error("Não foi possível abrir o arquivo de destino")
                }.onSuccess { status = "Banco exportado como dados estruturados" }
                    .onFailure { status = "Falha ao exportar banco: ${it.message ?: "erro desconhecido"}" }
            }
        }
    }
    val databaseImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Não foi possível ler o arquivo selecionado")
                    val json = JSONObject(text)
                    check(json.optString("format") == "f21-database-exchange") { "Formato de troca não reconhecido" }
                    check(json.optInt("formatVersion", -1) == 1) { "Versão de troca não suportada" }
                    val envelope = DatabaseExchangeJsonCodec.decodeCore(text)
                    val summary = repository.importCoreExchange(envelope).getOrThrow()
                    "Banco importado por upsert: ${summary.projects} projeto(s), ${summary.stations} estação(ões), ${summary.referencePoints} referência(s), ${summary.occupations} ocupação(ões), ${summary.heights} altura(s), conflitos atualizados: ${summary.conflicts.size}"
                }.onSuccess { status = it }
                    .onFailure { status = "Importação rejeitada: ${it.message ?: "arquivo inválido"}" }
            }
        }
    }
    val bluetoothPermissions = bluetoothRuntimePermissions()
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val granted = permissions.values.all { it }
        bluetoothDiscoveryStatus = if (granted) {
            "Permissões concedidas. Toque novamente para procurar dispositivos próximos."
        } else {
            val denied = permissions.filterValues { !it }.keys.joinToString()
            "Permissão necessária não concedida ($denied). Autorize Dispositivos próximos e tente novamente."
        }
    }
    val inspectBluetoothServices: (String) -> Unit = { mac ->
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            bluetoothServiceUuids = "Permissão BLUETOOTH_CONNECT necessária para consultar os serviços."
        } else {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            val device = adapter?.getRemoteDevice(mac)
            @Suppress("DEPRECATION")
            val uuids = device?.uuids?.map { it.uuid.toString() }.orEmpty()
            bluetoothServiceUuids = if (uuids.isEmpty()) {
                "Nenhum UUID anunciado no cache do Android. Isso não prova que o receptor não ofereça serviço; será necessário observar o pareamento/conexão."
            } else uuids.joinToString("\n")
        }
    }
    suspend fun restorePendingOccupation(pending: Occupation) {
        val heights = repository.findHeights(pending.id)
        val historical = repository.findOccupationSnapshots(pending.id)
        val recoveredProject = repository.findProject(pending.projectId)
        val recoveredStation = repository.findStation(pending.stationId)
        val recoveredReference = pending.referencePointId?.let { repository.findReferencePoint(it) }
        occupation = pending.copy(snapshots = historical ?: pending.snapshots)
        occupationPersisted = true
        projectName = recoveredProject?.name.orEmpty()
        savedId = historical?.station?.stationId ?: recoveredStation?.id
        stationEditorCreatedAt = recoveredStation?.createdAt
        name = historical?.station?.name ?: recoveredStation?.name.orEmpty()
        locality = historical?.station?.locality ?: recoveredStation?.locality.orEmpty()
        municipality = historical?.station?.municipality ?: recoveredStation?.municipality.orEmpty()
        val recoveredEquipment = historical?.let { snapshots ->
            if (snapshots.receiver != null && snapshots.antenna != null) {
                br.f21campo.domain.EquipmentSnapshot(
                    br.f21campo.domain.Receiver(
                        snapshots.receiver.receiverId ?: EntityId.new(),
                        snapshots.receiver.manufacturer,
                        snapshots.receiver.model,
                        snapshots.receiver.serialNumber,
                        snapshots.receiver.source,
                        snapshots.receiver.firmware,
                    ),
                    br.f21campo.domain.Antenna(
                        snapshots.antenna.antennaId ?: EntityId.new(),
                        snapshots.antenna.manufacturer,
                        snapshots.antenna.model,
                        snapshots.antenna.serialNumber,
                        snapshots.antenna.source,
                    ),
                )
            } else null
        } ?: pending.equipment
        receiverModel = recoveredEquipment?.receiver?.model.orEmpty()
        receiverManufacturer = recoveredEquipment?.receiver?.manufacturer.orEmpty()
        receiverSerial = recoveredEquipment?.receiver?.serialNumber.orEmpty()
        receiverFirmware = recoveredEquipment?.receiver?.firmware.orEmpty()
        antennaModel = recoveredEquipment?.antenna?.model.orEmpty()
        antennaManufacturer = recoveredEquipment?.antenna?.manufacturer.orEmpty()
        antennaSerial = recoveredEquipment?.antenna?.serialNumber.orEmpty()
        durationMinutes = pending.plannedDurationSeconds?.div(60L)?.toString().orEmpty()
        val historicalReference = historical?.referencePoint
        if (historicalReference != null) {
            referenceType = historicalReference.type ?: recoveredReference?.type ?: ReferencePointType.RN
            referenceCode = historicalReference.code.orEmpty()
        } else if (recoveredReference != null) {
            referenceType = recoveredReference.type
            referenceCode = recoveredReference.code
        } else {
            referenceCode = ""
        }
        rawImported = repository.hasRawArtifact(pending.id)
        rawSummary = repository.rawArtifactSummary(pending.id)
        rawArtifacts = repository.findRawArtifacts(pending.id)
        fieldEvents = repository.findEvents(pending.id)
        auditEvents = repository.findAuditEvents(pending.id)
        val beforeHeights = heights.filter { it.phase == HeightPhase.BEFORE }
        val afterHeights = heights.filter { it.phase == HeightPhase.AFTER }
        beforeUnit = beforeHeights.firstNotNullOfOrNull { heightUnitFromObservation(it.observation) }
        afterUnit = afterHeights.firstNotNullOfOrNull { heightUnitFromObservation(it.observation) }
        beforeType = beforeHeights.firstOrNull()?.type ?: HeightType.VERTICAL
        afterType = afterHeights.firstOrNull()?.type ?: HeightType.VERTICAL
        before = beforeHeights.map { "%.4f".format(heightFromMeters(it.valueMeters, beforeUnit)) }.ifEmpty { listOf("") }
        after = afterHeights.map { "%.4f".format(heightFromMeters(it.valueMeters, afterUnit)) }.ifEmpty { listOf("") }
        afterRegistered = afterHeights.isNotEmpty()
        val firstMissingStep = when {
            projectName.isBlank() -> 1
            name.isBlank() || locality.isBlank() -> 2
            historicalReference == null && recoveredReference == null -> 3
            recoveredEquipment == null -> 4
            !pending.hasBeforeHeight -> 5
            else -> 6
        }
        status = "Rastreio recuperado: ${pending.state}"
        route = when (pending.state) {
            OccupationState.ACTIVE -> "ACTIVE"
            OccupationState.STOPPED, OccupationState.COLLECTED, OccupationState.VALIDATED -> "FINALIZATION"
            else -> "NEW"
        }
        newStep = firstMissingStep
        showHome = false
    }
    val startNewTracking = {
        name = ""
        projectName = ""
        locality = ""
        municipality = ""
        savedId = null
        occupation = Occupation(EntityId.new(), EntityId.new(), EntityId.new())
        occupationPersisted = false
        projectName = ""
        projectEditorId = null
        projectEditorCreatedAt = null
        stationEditorCreatedAt = null
        receiverModel = ""
        antennaModel = ""
        receiverManufacturer = ""
        receiverFirmware = ""
        antennaManufacturer = ""
        receiverSerial = ""
        antennaSerial = ""
        before = listOf("")
        after = listOf("")
        beforeUndo = emptyList()
        afterUndo = emptyList()
        beforeUnit = null
        afterUnit = null
        beforeType = HeightType.VERTICAL
        afterType = HeightType.VERTICAL
        afterRegistered = false
        event = ""
        eventCategory = OccupationEventCategory.NOTE
        eventSeverity = EventSeverity.INFO
        referenceCode = ""
        referenceType = ReferencePointType.RN
        rawImported = false
        rawSummary = null
        rawArtifacts = emptyList()
        durationMinutes = ""
        selectedReceiverCatalogId = null
        selectedAntennaCatalogId = null
        trackingTimeAlerted = false
        route = "NEW"
        newStep = 1
        showHome = false
        status = "Etapa 1/7 — Projeto"
    }
    LaunchedEffect(occupation, occupationPersisted) {
        if (occupationPersisted) repository.save(occupation)
    }
    LaunchedEffect(occupation.id) {
        fieldEvents = repository.findEvents(occupation.id)
        auditEvents = repository.findAuditEvents(occupation.id)
        rawArtifacts = repository.findRawArtifacts(occupation.id)
    }
    LaunchedEffect(occupation.state, occupation.confirmedStart) {
        while (occupation.state == OccupationState.ACTIVE) {
            nowEpochMillis = System.currentTimeMillis()
            delay(1000)
        }
    }
    LaunchedEffect(occupation.state, occupation.confirmedStart, occupation.plannedDurationSeconds, nowEpochMillis) {
        val startedAt = occupation.confirmedStart
        val target = occupation.plannedDurationSeconds
        if (occupation.state == OccupationState.ACTIVE && startedAt != null && target != null && !trackingTimeAlerted) {
            val trackingTime = TrackingTimer.measure(startedAt, Instant.ofEpochMilli(nowEpochMillis), target)
            if (trackingTime.targetReached) {
                trackingTimeAlerted = true
                Toast.makeText(context, "Tempo planejado do rastreio cumprido", Toast.LENGTH_LONG).show()
            }
        }
    }
    // Step-specific persisted choices must refresh when the wizard advances,
    // not only when the top-level route changes.
    LaunchedEffect(route, newStep) {
        if (route == "PROJECTS") projects = repository.findAllProjects()
        if (route == "STATIONS") stations = repository.findAllStations()
        if (route == "HOME") {
            val pending = repository.findIncompleteOccupations().firstOrNull()
            pendingOccupationSummary = pending?.let {
                val station = repository.findStation(it.stationId)
                val reference = it.referencePointId?.let { id -> repository.findReferencePoint(id) }
                listOfNotNull(
                    station?.name,
                    reference?.let { point -> "${point.type.name} ${point.code}" },
                    it.state.name,
                ).joinToString(" · ")
            }
        }
    }
    LaunchedEffect(route, newStep) {
        if (route == "NEW" && newStep == 1) projects = repository.findAllProjects()
        if (route == "NEW" && newStep == 2) stations = repository.findAllStations()
        if (route == "NEW" && newStep == 3) {
            referencePoints = savedId?.let { repository.findReferencePointsByStation(it) }.orEmpty()
        }
        if (route == "NEW" && newStep == 4) {
            favoriteReceiverProfiles = repository.findFavoriteReceiverProfiles()
            receiverCatalog = repository.findActiveReceiverCatalog()
            antennaCatalog = repository.findActiveAntennaCatalog()
        }
        if (route == "EQUIPMENT") {
            receiverCatalog = repository.findActiveReceiverCatalog()
            antennaCatalog = repository.findActiveAntennaCatalog()
        }
        if (route == "CONNECTION") {
            connectionProfiles = repository.findConnectionProfiles()
            favoriteReceiverProfiles = repository.findFavoriteReceiverProfiles()
        }
    }
    val fieldBlue = Color(0xFF0B4F71)
    val fieldBlueDark = Color(0xFF073653)
    val fieldBackground = Color(0xFFF1F6FA)
    MaterialTheme(colorScheme = lightColorScheme(primary = fieldBlue, secondary = Color(0xFF176E96), background = fieldBackground, surface = Color.White)) {
        Surface(modifier = Modifier.fillMaxSize(), color = fieldBackground) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 48.dp)
                    .imePadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (route == "HOME") {
                    FieldTopBar(
                        title = "F-21 Campo",
                        subtitle = "${BuildConfig.BUILD_MODE} · operação offline",
                        showBack = false,
                        onBack = {},
                        fieldBlueDark = fieldBlueDark,
                    )
                }
                if (route != "HOME") {
                    val routeTitle = when (route) {
                        "NEW" -> "Novo rastreio"
                        "PROJECTS" -> "Projetos"
                        "STATIONS" -> "Banco de estações"
                        "CONNECTION" -> "Conexão de bancada"
                        "EQUIPMENT" -> "Catálogo de equipamentos"
                        "SETTINGS" -> "Configurações"
                        "ABOUT" -> "Sobre"
                        "SUMMARY" -> "Resumo do rastreio"
                        "ACTIVE" -> "Rastreio ativo"
                        "FINALIZATION" -> "Finalização"
                        else -> "F-21 Campo"
                    }
                    FieldTopBar(
                        title = routeTitle,
                        subtitle = "F-21 Campo · ${BuildConfig.VERSION_NAME}",
                        showBack = true,
                        onBack = { route = "HOME"; showHome = true },
                        fieldBlueDark = fieldBlueDark,
                    )
                }
                if (route == "HOME") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("F-21 Campo", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                            Text("INÍCIO · ${BuildConfig.BUILD_MODE}", color = Color(0xFFD5EAF5), style = MaterialTheme.typography.labelLarge)
                            Text("Aquisição e rastreio de referências", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text("Operação offline · pronta para campo", color = Color(0xFFD5EAF5))
                            Text("Versão ${BuildConfig.VERSION_NAME}", color = Color(0xFFD5EAF5), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (pendingOccupationSummary != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F1F7)), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("RASTREIO NÃO FINALIZADO", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                                Text(pendingOccupationSummary ?: "")
                                Button(onClick = {
                                    scope.launch {
                                        val pending = repository.findIncompleteOccupations().firstOrNull()
                                        if (pending != null) {
                                            restorePendingOccupation(pending)
                                        }
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text("CONTINUAR") }
                            }
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("INÍCIO", style = MaterialTheme.typography.titleLarge, color = fieldBlueDark)
                            Text("Acesso rápido", color = Color(0xFF52636D))
                            Button(onClick = { startNewTracking() }, modifier = Modifier.fillMaxWidth()) { Text("NOVO RASTREIO") }
                            Button(onClick = {
                                scope.launch {
                                    val pending = repository.findIncompleteOccupations().firstOrNull()
                                    if (pending != null) {
                                        restorePendingOccupation(pending)
                                    }
                                    else status = "Nenhum rastreio incompleto encontrado"
                                }
                            }, enabled = pendingOccupationSummary != null, modifier = Modifier.fillMaxWidth()) { Text("CONTINUAR RASTREIO") }
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("DADOS DE CAMPO", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { route = "PROJECTS"; showHome = false }, modifier = Modifier.weight(1f)) { Text("PROJETOS") }
                                OutlinedButton(onClick = { route = "STATIONS"; showHome = false }, modifier = Modifier.weight(1f)) { Text("ESTAÇÕES") }
                            }
                            OutlinedButton(onClick = { databaseExportLauncher.launch("f21-database-${System.currentTimeMillis()}.json") }, modifier = Modifier.fillMaxWidth()) { Text("EXPORTAR BANCO PARA COMPARTILHAR") }
                            OutlinedButton(onClick = { databaseImportLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) { Text("IMPORTAR BANCO JSON") }
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("APLICATIVO", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            OutlinedButton(onClick = { route = "CONNECTION"; showHome = false }, modifier = Modifier.fillMaxWidth()) { Text("CONEXÃO DE BANCADA") }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { route = "SETTINGS"; showHome = false }, modifier = Modifier.weight(1f)) { Text("CONFIGURAÇÕES") }
                                OutlinedButton(onClick = { route = "ABOUT"; showHome = false }, modifier = Modifier.weight(1f)) { Text("SOBRE") }
                            }
                        }
                    }
                } else if (route == "PROJECTS") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("PROJETOS", color = Color.White, style = MaterialTheme.typography.headlineSmall); Text("Comissões e trabalhos salvos neste aparelho", color = Color(0xFFD5EAF5)) } }
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                    OutlinedTextField(name, { name = it }, label = { Text("Nome do projeto/comissão") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(projectSearch, { projectSearch = it }, label = { Text("Pesquisar projetos") }, modifier = Modifier.fillMaxWidth())
                    Button(enabled = name.isNotBlank(), onClick = {
                        scope.launch {
                            val project = br.f21campo.domain.Project(
                                id = projectEditorId ?: EntityId.new(),
                                name = name.trim(),
                                createdAt = projectEditorCreatedAt ?: Instant.now(),
                            )
                            repository.save(project)
                            projects = repository.findAllProjects()
                            projectEditorId = project.id
                            projectEditorCreatedAt = project.createdAt
                            status = "Projeto salvo localmente — ID preservado"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (projectEditorId == null) "SALVAR PROJETO" else "ATUALIZAR PROJETO") }
                    if (projectEditorId != null) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                repository.archiveProject(projectEditorId!!)
                                projects = repository.findAllProjects()
                                name = ""
                                projectEditorId = null
                                projectEditorCreatedAt = null
                                status = "Projeto arquivado localmente; nenhum dado de ocupação foi apagado"
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("ARQUIVAR PROJETO") }
                    }
                    Text("Projetos salvos neste aparelho", style = MaterialTheme.typography.titleMedium)
                    val visibleProjects = projects.filter { item -> projectSearch.isBlank() || item.name.contains(projectSearch.trim(), ignoreCase = true) }
                    if (visibleProjects.isEmpty()) Text(if (projects.isEmpty()) "Nenhum projeto ativo salvo ainda" else "Nenhum projeto corresponde à pesquisa")
                    visibleProjects.forEach { project ->
                        OutlinedButton(onClick = {
                            name = project.name
                            projectEditorId = project.id
                            projectEditorCreatedAt = project.createdAt
                            status = "Projeto aberto: ${project.name} · ID preservado"
                        }, modifier = Modifier.fillMaxWidth()) { Text("${project.name} · ID ${project.id.value.take(8)}") }
                    }
                    Text(status)
                } else if (route == "STATIONS") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("BANCO DE ESTAÇÕES", color = Color.White, style = MaterialTheme.typography.headlineSmall); Text("Estações e localidades salvas neste aparelho", color = Color(0xFFD5EAF5)) } }
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                    OutlinedTextField(name, { name = it }, label = { Text("Nome da estação") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(locality, { locality = it }, label = { Text("Localidade") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(municipality, { municipality = it }, label = { Text("Município (opcional; não inferido)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(stationSearch, { stationSearch = it }, label = { Text("Pesquisar estações") }, modifier = Modifier.fillMaxWidth())
                    Button(enabled = name.isNotBlank() && locality.isNotBlank(), onClick = {
                        scope.launch {
                            val existing = savedId?.let { repository.findStation(it) }
                            val station = Station(
                                id = savedId ?: EntityId.new(),
                                name = name.trim(),
                                locality = locality.trim(),
                                municipality = municipality.trim().ifBlank { existing?.municipality },
                                createdAt = stationEditorCreatedAt ?: existing?.createdAt ?: Instant.now(),
                            )
                            repository.save(station)
                            savedId = station.id
                            stationEditorCreatedAt = station.createdAt
                            stations = repository.findAllStations()
                            status = "Estação salva localmente — ID preservado"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (savedId == null) "SALVAR ESTAÇÃO" else "ATUALIZAR ESTAÇÃO") }
                    if (savedId != null) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                repository.archiveStation(savedId!!)
                                stations = repository.findAllStations()
                                savedId = null
                                stationEditorCreatedAt = null
                                stationHistory = emptyList()
                                status = "Estação arquivada localmente; histórico permanece preservado"
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("ARQUIVAR ESTAÇÃO") }
                    }
                    Text("Estações salvas neste aparelho", style = MaterialTheme.typography.titleMedium)
                    val visibleStations = stations.filter { item -> stationSearch.isBlank() || listOfNotNull(item.name, item.locality, item.municipality).any { value -> value.contains(stationSearch.trim(), ignoreCase = true) } }
                    if (visibleStations.isEmpty()) Text(if (stations.isEmpty()) "Nenhuma estação ativa salva ainda" else "Nenhuma estação corresponde à pesquisa")
                    visibleStations.forEach { station ->
                        OutlinedButton(onClick = {
                            savedId = station.id
                            stationEditorCreatedAt = station.createdAt
                            name = station.name
                            locality = station.locality.orEmpty()
                            municipality = station.municipality.orEmpty()
                            scope.launch { stationHistory = repository.findOccupationsByStation(station.id) }
                            status = "Estação aberta: ${station.name} · ID preservado"
                        }, modifier = Modifier.fillMaxWidth()) { Text("${station.name} · ${station.locality ?: "sem localidade"} · ID ${station.id.value.take(8)}") }
                    }
                    if (savedId != null) {
                        Text("HISTÓRICO DE RASTREIOS", style = MaterialTheme.typography.titleMedium)
                        if (stationHistory.isEmpty()) Text("Nenhum rastreio registrado para esta estação")
                        stationHistory.forEach { item ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Estado: ${item.state}", style = MaterialTheme.typography.titleSmall)
                                    Text("Início: ${item.confirmedStart ?: item.plannedStart ?: "não iniciado"}")
                                    Text("Fim: ${item.confirmedStop ?: "em aberto"}")
                                    Button(onClick = {
                                        scope.launch {
                                            val current = repository.findOccupation(item.id) ?: return@launch
                                            reviewedOccupation = current
                                            reviewedProjectName = repository.findProject(current.projectId)?.name.orEmpty()
                                            reviewedStationName = repository.findStation(current.stationId)?.name.orEmpty()
                                            reviewedReference = current.referencePointId?.let { repository.findReferencePoint(it) }
                                            reviewedHeights = repository.findHeights(current.id)
                                            reviewedEvents = repository.findEvents(current.id)
                                            reviewedRawSummary = repository.rawArtifactSummary(current.id)
                                            route = "SUMMARY"
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) { Text("VER RASTREIO") }
                                }
                            }
                        }
                    }
                    Text(status)
                } else if (route == "SUMMARY") {
                    val reviewed = reviewedOccupation
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("RESUMO DO RASTREIO", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            Text("Dados recuperados do armazenamento local", color = Color(0xFFD5EAF5))
                        }
                    }
                    Button(onClick = { route = "STATIONS" }) { Text("← BANCO DE ESTAÇÕES") }
                    if (reviewed == null) {
                        Text("Nenhum rastreio selecionado")
                    } else {
                        Text("Projeto/LH: ${reviewedProjectName.ifBlank { "não localizado" }}")
                        Text("Estação: ${reviewedStationName.ifBlank { "não localizada" }}")
                        Text("Referência: ${reviewedReference?.let { "${it.type} ${it.code}" } ?: "não registrada"}")
                        Text("Status: ${reviewed.state}")
                        Text("Início: ${reviewed.confirmedStart ?: "não iniciado"}")
                        Text("Fim: ${reviewed.confirmedStop ?: "em aberto"}")
                        Text("Receptor: ${reviewed.equipment?.receiver?.model ?: "não informado"}")
                        Text("Antena: ${reviewed.equipment?.antenna?.model ?: "não informada"}")
                        Text("ALTURAS", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                        if (reviewedHeights.isEmpty()) Text("Nenhuma altura registrada")
                        val beforeStatistics = heightStatistics(reviewedHeights.filter { it.phase == HeightPhase.BEFORE })
                        val afterStatistics = heightStatistics(reviewedHeights.filter { it.phase == HeightPhase.AFTER })
                        reviewedHeights.groupBy { it.phase }.forEach { (phase, values) ->
                            val statistics = heightStatistics(values)
                            Text("$phase: ${values.joinToString { "%.4f m".format(it.valueMeters) }}")
                            statistics?.let { Text("Média: %.4f m · amplitude: %.4f m".format(it.meanMeters, it.rangeMeters)) }
                        }
                        if (beforeStatistics != null && afterStatistics != null) {
                            Text("Delta entre médias: %.4f m".format(afterStatistics.meanMeters - beforeStatistics.meanMeters))
                        }
                        Text("EVENTOS", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                        if (reviewedEvents.isEmpty()) Text("Nenhum evento registrado")
                        reviewedEvents.forEach { savedEvent -> Text("${savedEvent.at}: ${savedEvent.description}") }
                        Text("RAW_RECEIVER: ${reviewedRawSummary ?: "não associado"}")
                    }
                } else if (route == "SETTINGS") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) { Text("CONFIGURAÇÕES", color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp)) }
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                    Text("Modo: ${BuildConfig.BUILD_MODE}")
                    Text("O aplicativo funciona offline e registra a origem manual dos equipamentos.")
                    Button(onClick = { route = "CONNECTION" }, modifier = Modifier.fillMaxWidth()) { Text("CONEXÃO DE BANCADA") }
                    Button(onClick = { route = "EQUIPMENT" }, modifier = Modifier.fillMaxWidth()) { Text("CATÁLOGO DE EQUIPAMENTOS") }
                } else if (route == "EQUIPMENT") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("CATÁLOGO DE EQUIPAMENTOS", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            Text("Receptor e antena são catálogos manuais independentes. Editar um item não altera snapshots de ocupações antigas.", color = Color(0xFFD5EAF5))
                        }
                    }
                    Button(onClick = { route = "HOME"; showHome = true }, modifier = Modifier.fillMaxWidth()) { Text("← INÍCIO") }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("RECEPTOR GNSS", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            OutlinedTextField(receiverManufacturer, { receiverManufacturer = it }, label = { Text("Fabricante") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(receiverModel, { receiverModel = it }, label = { Text("Modelo") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(receiverSerial, { receiverSerial = it }, label = { Text("Número de série") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(receiverFirmware, { receiverFirmware = it }, label = { Text("Firmware (se informado)") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                scope.launch {
                                    val item = ReceiverCatalogItem(
                                        id = selectedReceiverCatalogId ?: EntityId.new(),
                                        manufacturer = receiverManufacturer.trim().ifBlank { null },
                                        model = receiverModel.trim().ifBlank { null },
                                        serialNumber = receiverSerial.trim().ifBlank { null },
                                        firmware = receiverFirmware.trim().ifBlank { null },
                                        createdAt = receiverCatalog.firstOrNull { it.id == selectedReceiverCatalogId }?.createdAt ?: Instant.now(),
                                    )
                                    repository.saveReceiverCatalog(item)
                                    receiverCatalog = repository.findActiveReceiverCatalog()
                                    selectedReceiverCatalogId = item.id
                                    status = "Receptor salvo no catálogo local"
                                }
                            }, enabled = listOf(receiverManufacturer, receiverModel, receiverSerial, receiverFirmware).any { it.isNotBlank() }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (selectedReceiverCatalogId == null) "SALVAR RECEPTOR" else "ATUALIZAR RECEPTOR")
                            }
                            if (receiverCatalog.isEmpty()) Text("Nenhum receptor cadastrado")
                            receiverCatalog.forEach { item ->
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3F8)), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(listOfNotNull(item.manufacturer, item.model).ifEmpty { listOf("Receptor sem identificação") }.joinToString(" · "), color = fieldBlueDark)
                                        Text(listOfNotNull(item.serialNumber?.let { "S/N $it" }, item.firmware?.let { "FW $it" }).joinToString(" · ").ifBlank { "Sem serial/firmware informado" }, style = MaterialTheme.typography.bodySmall)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = {
                                                selectedReceiverCatalogId = item.id
                                                receiverManufacturer = item.manufacturer.orEmpty()
                                                receiverModel = item.model.orEmpty()
                                                receiverSerial = item.serialNumber.orEmpty()
                                                receiverFirmware = item.firmware.orEmpty()
                                                status = "Receptor selecionado para edição"
                                            }, modifier = Modifier.weight(1f)) { Text("EDITAR") }
                                            OutlinedButton(onClick = {
                                                scope.launch {
                                                    repository.archiveReceiverCatalog(item.id)
                                                    receiverCatalog = repository.findActiveReceiverCatalog()
                                                    if (selectedReceiverCatalogId == item.id) selectedReceiverCatalogId = null
                                                    status = "Receptor arquivado; ocupações antigas preservadas"
                                                }
                                            }, modifier = Modifier.weight(1f)) { Text("ARQUIVAR") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ANTENA", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            OutlinedTextField(antennaManufacturer, { antennaManufacturer = it }, label = { Text("Fabricante") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(antennaModel, { antennaModel = it }, label = { Text("Modelo") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(antennaSerial, { antennaSerial = it }, label = { Text("Número de série") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                scope.launch {
                                    val item = AntennaCatalogItem(
                                        id = selectedAntennaCatalogId ?: EntityId.new(),
                                        manufacturer = antennaManufacturer.trim().ifBlank { null },
                                        model = antennaModel.trim().ifBlank { null },
                                        serialNumber = antennaSerial.trim().ifBlank { null },
                                        createdAt = antennaCatalog.firstOrNull { it.id == selectedAntennaCatalogId }?.createdAt ?: Instant.now(),
                                    )
                                    repository.saveAntennaCatalog(item)
                                    antennaCatalog = repository.findActiveAntennaCatalog()
                                    selectedAntennaCatalogId = item.id
                                    status = "Antena salva no catálogo local"
                                }
                            }, enabled = listOf(antennaManufacturer, antennaModel, antennaSerial).any { it.isNotBlank() }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (selectedAntennaCatalogId == null) "SALVAR ANTENA" else "ATUALIZAR ANTENA")
                            }
                            if (antennaCatalog.isEmpty()) Text("Nenhuma antena cadastrada")
                            antennaCatalog.forEach { item ->
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3F8)), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(listOfNotNull(item.manufacturer, item.model).ifEmpty { listOf("Antena sem identificação") }.joinToString(" · "), color = fieldBlueDark)
                                        Text(item.serialNumber?.let { "S/N $it" } ?: "Sem número de série informado", style = MaterialTheme.typography.bodySmall)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = {
                                                selectedAntennaCatalogId = item.id
                                                antennaManufacturer = item.manufacturer.orEmpty()
                                                antennaModel = item.model.orEmpty()
                                                antennaSerial = item.serialNumber.orEmpty()
                                                status = "Antena selecionada para edição"
                                            }, modifier = Modifier.weight(1f)) { Text("EDITAR") }
                                            OutlinedButton(onClick = {
                                                scope.launch {
                                                    repository.archiveAntennaCatalog(item.id)
                                                    antennaCatalog = repository.findActiveAntennaCatalog()
                                                    if (selectedAntennaCatalogId == item.id) selectedAntennaCatalogId = null
                                                    status = "Antena arquivada; ocupações antigas preservadas"
                                                }
                                            }, modifier = Modifier.weight(1f)) { Text("ARQUIVAR") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text(status)
                } else if (route == "CONNECTION") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("CONEXÃO DE BANCADA", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            Text("Perfil neutro para testes reais de receptor", color = Color(0xFFD5EAF5))
                        }
                    }
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("RECEPTORES GNSS FAVORITOS", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            Text("Salve o equipamento completo que se comunica com o celular. A antena física usada na ocupação continua registrada separadamente.")
                            if (favoriteReceiverProfiles.isEmpty()) {
                                Text("Nenhum favorito salvo neste aparelho.")
                            } else {
                                favoriteReceiverProfiles.forEach { profile ->
                                    OutlinedButton(onClick = {
                                        receiverManufacturer = profile.receiverManufacturer.orEmpty()
                                        receiverModel = profile.receiverModel.orEmpty()
                                        receiverSerial = profile.receiverSerial.orEmpty()
                                        connectionTransport = profile.transportType
                                        connectionHost = profile.hostOrAddress.orEmpty()
                                        connectionPort = profile.port?.toString().orEmpty()
                                        bluetoothName = profile.bluetoothName.orEmpty()
                                        bluetoothMac = profile.bluetoothMac.orEmpty()
                                        bluetoothServiceUuidInput = profile.bluetoothServiceUuid.orEmpty()
                                        connectionNotes = profile.notes.orEmpty()
                                        connectionStatus = "Favorito carregado. Nenhum comando foi enviado ao receptor."
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text(listOfNotNull(profile.receiverManufacturer, profile.receiverModel, profile.receiverSerial?.let { "S/N $it" }).ifEmpty { listOf("Receptor sem identificação") }.joinToString(" · "))
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(receiverManufacturer, { receiverManufacturer = it }, label = { Text("Fabricante do receptor GNSS") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(receiverModel, { receiverModel = it }, label = { Text("Modelo do receptor GNSS") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(receiverSerial, { receiverSerial = it }, label = { Text("Nº de série do receptor GNSS") }, modifier = Modifier.fillMaxWidth())
                    Text("Transporte", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { connectionTransport = ReceiverTransportType.WIFI_TCP }, enabled = connectionTransport != ReceiverTransportType.WIFI_TCP) { Text("WI-FI/TCP") }
                        Button(onClick = { connectionTransport = ReceiverTransportType.BLUETOOTH }, enabled = connectionTransport != ReceiverTransportType.BLUETOOTH) { Text("BT") }
                        Button(onClick = { connectionTransport = ReceiverTransportType.SERIAL }, enabled = connectionTransport != ReceiverTransportType.SERIAL) { Text("SERIAL") }
                    }
                    if (connectionTransport == ReceiverTransportType.WIFI_TCP || connectionTransport == ReceiverTransportType.SERIAL) {
                        OutlinedTextField(connectionHost, { connectionHost = it }, label = { Text("IP/endereço do receptor") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(connectionPort, { connectionPort = it.filter(Char::isDigit) }, label = { Text("Porta, se conhecida") }, modifier = Modifier.fillMaxWidth())
                    }
                    if (connectionTransport == ReceiverTransportType.BLUETOOTH) {
                        OutlinedTextField(bluetoothName, { bluetoothName = it }, label = { Text("Nome Bluetooth, se conhecido") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(bluetoothMac, { bluetoothMac = it }, label = { Text("MAC Bluetooth, se conhecido") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            if (!hasBluetoothRuntimePermissions(context)) {
                                bluetoothPermissionLauncher.launch(bluetoothPermissions)
                            } else {
                                try {
                                    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
                                    when {
                                        adapter == null -> bluetoothDiscoveryStatus = "Este aparelho não possui adaptador Bluetooth disponível."
                                        !adapter.isEnabled -> bluetoothDiscoveryStatus = "Ative o Bluetooth do celular e tente novamente."
                                        else -> {
                                            pairedBluetoothDevices = adapter.bondedDevices
                                                .map { it.name.orEmpty() to it.address.orEmpty() }
                                                .sortedBy { it.first }
                                            bluetoothDiscoveryStatus = if (pairedBluetoothDevices.isEmpty()) {
                                                "Nenhum dispositivo pareado. Faça o pareamento nas configurações Android."
                                            } else "Selecione um dispositivo pareado para registrar o perfil de bancada."
                                        }
                                    }
                                } catch (_: SecurityException) {
                                    bluetoothDiscoveryStatus = "O Android bloqueou a consulta. Autorize Dispositivos próximos e tente novamente."
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("LISTAR DISPOSITIVOS PAREADOS") }
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("ABRIR CONFIGURAÇÕES BLUETOOTH") }
                        OutlinedButton(onClick = {
                            if (!hasBluetoothRuntimePermissions(context)) {
                                bluetoothPermissionLauncher.launch(bluetoothPermissions)
                            } else if (!isLocationEnabledForBluetoothDiscovery(context)) {
                                bluetoothDiscoveryStatus = "Ative a Localização do Android para permitir a busca Bluetooth neste aparelho."
                            } else {
                                try {
                                    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
                                    when {
                                        adapter == null -> bluetoothDiscoveryStatus = "Este aparelho não possui adaptador Bluetooth disponível."
                                        !adapter.isEnabled -> bluetoothDiscoveryStatus = "Ative o Bluetooth do celular e tente novamente."
                                        else -> {
                                            if (adapter.isDiscovering) {
                                                adapter.cancelDiscovery()
                                                bluetoothScanning = false
                                                bluetoothDiscoveryStatus = "Busca Bluetooth interrompida pelo operador."
                                            } else {
                                                discoveredBluetoothDevices.clear()
                                                bluetoothScanning = adapter.startDiscovery()
                                                bluetoothDiscoveryStatus = if (bluetoothScanning) {
                                                    "Procurando dispositivos Bluetooth próximos… mantenha o receptor ligado."
                                                } else {
                                                    "O Android recusou a busca. Confirme Dispositivos próximos, Localização (Android até 11) e tente novamente."
                                                }
                                            }
                                        }
                                    }
                                } catch (_: SecurityException) {
                                    bluetoothScanning = false
                                    bluetoothDiscoveryStatus = "Permissão Bluetooth insuficiente. Autorize Dispositivos próximos nas configurações do app."
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (bluetoothScanning) "PARAR BUSCA" else "PROCURAR DISPOSITIVOS PRÓXIMOS")
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("BLUETOOTH DE BANCADA", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                                Text(bluetoothDiscoveryStatus)
                                if (pairedBluetoothDevices.isNotEmpty()) {
                                    Text("PAREADOS (${pairedBluetoothDevices.size})", style = MaterialTheme.typography.labelMedium, color = fieldBlueDark)
                                }
                                pairedBluetoothDevices.forEach { (deviceName, mac) ->
                                    OutlinedButton(onClick = {
                                        bluetoothName = deviceName
                                        bluetoothMac = mac
                                        selectedBluetoothMac = mac
                                        inspectBluetoothServices(mac)
                                        connectionStatus = "Dispositivo selecionado; transporte e protocolo ainda não homologados."
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text("${if (selectedBluetoothMac == mac) "✓ " else ""}${deviceName.ifBlank { "Sem nome" }} · $mac")
                                    }
                                }
                                if (discoveredBluetoothDevices.isNotEmpty()) {
                                    Text("PRÓXIMOS ENCONTRADOS (${discoveredBluetoothDevices.size})", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                                    OutlinedButton(
                                        onClick = {
                                            discoveredBluetoothDevices.clear()
                                            bluetoothDiscoveryStatus = "Resultados próximos limpos. Inicie uma nova busca quando necessário."
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text("LIMPAR RESULTADOS") }
                                    discoveredBluetoothDevices.forEach { (deviceName, mac) ->
                                        OutlinedButton(onClick = {
                                            bluetoothName = deviceName
                                            bluetoothMac = mac
                                            selectedBluetoothMac = mac
                                            inspectBluetoothServices(mac)
                                            connectionStatus = "Dispositivo selecionado. Pareie-o no Android antes de tentar a conexão de bancada."
                                        }, modifier = Modifier.fillMaxWidth()) {
                                            Text("${if (selectedBluetoothMac == mac) "✓ " else ""}${deviceName.ifBlank { "Sem nome" }} · $mac")
                                        }
                                    }
                                } else if (bluetoothDiscoveryStatus.contains("concluída", ignoreCase = true)) {
                                    Text("Nenhum dispositivo próximo foi encontrado. Mantenha o receptor ligado e tente novamente.", style = MaterialTheme.typography.bodySmall)
                                }
                                if (bluetoothServiceUuids.isNotBlank()) {
                                    Text("SERVIÇOS BLUETOOTH OBSERVADOS", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                                    Text(bluetoothServiceUuids, style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedTextField(
                                    bluetoothServiceUuidInput,
                                    { bluetoothServiceUuidInput = it },
                                    label = { Text("UUID RFCOMM observado (opcional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text("O UUID deve vir do receptor/Android. Nenhum UUID é presumido pelo F-21.", style = MaterialTheme.typography.bodySmall)
                                Button(
                                    enabled = bluetoothMac.isNotBlank() && bluetoothServiceUuidInput.isNotBlank(),
                                    onClick = {
                                        scope.launch {
                                            val normalizedUuid = bluetoothServiceUuidInput.trim()
                                            val parsedUuid = runCatching { UUID.fromString(normalizedUuid) }.getOrNull()
                                            if (parsedUuid == null) {
                                                bluetoothTransportStatus = "UUID inválido. Use o UUID observado no Android, sem inventar um valor."
                                                return@launch
                                            }
                                            bluetoothTransportStatus = "Abrindo canal RFCOMM..."
                                            val result = withContext(Dispatchers.IO) {
                                                try {
                                                    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
                                                    val device = adapter?.getRemoteDevice(bluetoothMac)
                                                    val socket = device?.createRfcommSocketToServiceRecord(parsedUuid)
                                                        ?: error("Adaptador Bluetooth indisponível")
                                                    adapter.cancelDiscovery()
                                                    socket.connect()
                                                    bluetoothSocketHolder[0]?.close()
                                                    bluetoothSocketHolder[0] = socket
                                                    bluetoothChannelOpen = true
                                                    "Canal RFCOMM aberto; nenhum byte foi transmitido."
                                                } catch (error: Exception) {
                                                    try { bluetoothSocketHolder[0]?.close() } catch (_: IOException) { }
                                                    bluetoothSocketHolder[0] = null
                                                    bluetoothChannelOpen = false
                                                    "Falha ao abrir canal RFCOMM: ${error.message ?: error::class.simpleName}"
                                                }
                                            }
                                            bluetoothTransportStatus = result
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("TESTAR CANAL RFCOMM") }
                                OutlinedButton(
                                    enabled = bluetoothChannelOpen,
                                    onClick = {
                                        try { bluetoothSocketHolder[0]?.close() } catch (_: IOException) { }
                                        bluetoothSocketHolder[0] = null
                                        bluetoothChannelOpen = false
                                        bluetoothTransportStatus = "Canal RFCOMM fechado; nenhum comando foi enviado."
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("FECHAR CANAL") }
                                Text(bluetoothTransportStatus)
                            }
                        }
                    }
                    OutlinedTextField(connectionNotes, { connectionNotes = it }, label = { Text("Observações de bancada") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = {
                        if (receiverManufacturer.isBlank() && receiverModel.isBlank()) {
                            connectionStatus = "Informe ao menos fabricante ou modelo para salvar o receptor favorito."
                        } else {
                            scope.launch {
                                repository.saveConnectionProfile(
                                    ReceiverConnectionProfile(
                                        receiverManufacturer = receiverManufacturer.trim().ifBlank { null },
                                        receiverModel = receiverModel.trim().ifBlank { null },
                                        receiverSerial = receiverSerial.trim().ifBlank { null },
                                        isFavorite = true,
                                        transportType = connectionTransport,
                                        hostOrAddress = connectionHost.trim().ifBlank { null },
                                        port = connectionPort.toIntOrNull(),
                                        bluetoothName = bluetoothName.trim().ifBlank { null },
                                        bluetoothMac = bluetoothMac.trim().ifBlank { null },
                                        bluetoothServiceUuid = bluetoothServiceUuidInput.trim().ifBlank { null },
                                        notes = connectionNotes.trim().ifBlank { null },
                                    ),
                                )
                                favoriteReceiverProfiles = repository.findFavoriteReceiverProfiles()
                                connectionProfiles = repository.findConnectionProfiles()
                                connectionStatus = "Receptor salvo como favorito neste aparelho."
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("SALVAR COMO FAVORITO") }
                    Button(onClick = {
                        val endpoint = when (connectionTransport) {
                            ReceiverTransportType.WIFI_TCP, ReceiverTransportType.SERIAL -> listOf(connectionHost.trim(), connectionPort.trim()).filter { it.isNotBlank() }.joinToString(":")
                            ReceiverTransportType.BLUETOOTH -> listOf(bluetoothName.trim(), bluetoothMac.trim()).filter { it.isNotBlank() }.joinToString(" · ")
                            ReceiverTransportType.UNKNOWN -> ""
                        }
                        scope.launch {
                            if (endpoint.isNotBlank()) {
                                repository.saveConnectionProfile(
                                    ReceiverConnectionProfile(
                                        receiverManufacturer = receiverManufacturer.trim().ifBlank { null },
                                        receiverModel = receiverModel.trim().ifBlank { null },
                                        receiverSerial = receiverSerial.trim().ifBlank { null },
                                        transportType = connectionTransport,
                                        hostOrAddress = connectionHost.trim().ifBlank { null },
                                        port = connectionPort.toIntOrNull(),
                                        bluetoothName = bluetoothName.trim().ifBlank { null },
                                        bluetoothMac = bluetoothMac.trim().ifBlank { null },
                                        bluetoothServiceUuid = bluetoothServiceUuidInput.trim().ifBlank { null },
                                        notes = connectionNotes.trim().ifBlank { null },
                                    ),
                                )
                                connectionProfiles = repository.findConnectionProfiles()
                                favoriteReceiverProfiles = repository.findFavoriteReceiverProfiles()
                            }
                            val result = if (connectionTransport == ReceiverTransportType.WIFI_TCP) {
                                withContext(Dispatchers.IO) { tcpTransport.connect(connectionHost.trim(), connectionPort.toIntOrNull() ?: 0) }
                            } else {
                                manualConnection.connect(endpoint)
                            }
                            connectionStatus = "${result.state}: ${result.message ?: "sem mensagem"}"
                            status = if (result.state == br.f21campo.receiver.api.ReceiverConnectionState.CONNECTED) {
                                "TCP alcançável — nenhum comando Spectra foi enviado"
                            } else "Perfil de conexão registrado para bancada"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("TESTAR CONEXÃO / REGISTRAR PERFIL") }
                    Button(onClick = {
                        scope.launch {
                            val result = if (connectionTransport == ReceiverTransportType.WIFI_TCP) {
                                withContext(Dispatchers.IO) { tcpTransport.close() }
                            } else manualConnection.disconnect()
                            connectionStatus = "${result.state}: ${result.message ?: "sem mensagem"}"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("DESCONECTAR") }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Estado", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            Text(connectionStatus)
                            Text("No Wi-Fi/TCP, o app testa somente se a porta aceita conexão. No Bluetooth, ele lista dispositivos já pareados. Não envia RID, START, STOP nem outro comando Spectra. RFCOMM/BLE, UUID, framing e respostas dependem de validação física.")
                        }
                    }
                    if (connectionProfiles.isNotEmpty()) {
                        Text("PERFIS SALVOS NESTE APARELHO", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                        connectionProfiles.take(3).forEach { profile ->
                            Button(onClick = {
                                receiverManufacturer = profile.receiverManufacturer.orEmpty()
                                receiverModel = profile.receiverModel.orEmpty()
                                receiverSerial = profile.receiverSerial.orEmpty()
                                connectionTransport = profile.transportType
                                connectionHost = profile.hostOrAddress.orEmpty()
                                connectionPort = profile.port?.toString().orEmpty()
                                bluetoothName = profile.bluetoothName.orEmpty()
                                bluetoothMac = profile.bluetoothMac.orEmpty()
                                bluetoothServiceUuidInput = profile.bluetoothServiceUuid.orEmpty()
                                connectionNotes = profile.notes.orEmpty()
                                connectionStatus = "Perfil carregado; teste a conexão quando o receptor estiver acessível"
                            }, modifier = Modifier.fillMaxWidth()) {
                                val label = profile.receiverModel ?: profile.hostOrAddress ?: profile.bluetoothName ?: "perfil sem endereço"
                                Text("${profile.transportType}: $label${profile.port?.let { ":$it" }.orEmpty()}")
                            }
                        }
                    }
                } else if (route == "ABOUT") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("F-21 Campo", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            Text("Aquisição, conferência e rastreabilidade de ocupações de campo.", color = Color(0xFFD5EAF5))
                            Text("Versão ${BuildConfig.VERSION_NAME} · funcionamento totalmente offline", color = Color(0xFFD5EAF5), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Sobre este app", style = MaterialTheme.typography.titleLarge, color = fieldBlueDark)
                            Text("Fluxo manual de rastreio, persistência, proveniência e recuperação de dados para trabalho de campo.")
                            Text("Modo ${BuildConfig.BUILD_MODE}", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Dúvidas e sugestões", style = MaterialTheme.typography.titleLarge, color = fieldBlueDark)
                            Text("Contato: gabrielpsmsn@gmail.com")
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Privacidade", style = MaterialTheme.typography.titleLarge, color = fieldBlueDark)
                            Text("Não há login, servidor ou sincronização em nuvem. O rascunho, os projetos, o Banco de Estações e as evidências permanecem no armazenamento local do aparelho.")
                            HorizontalDivider()
                            Text("Política de privacidade", color = fieldBlueDark, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 1) {
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("INÍCIO") }
                    StepHeader(1, "PROJETO", "Identifique a comissão ou trabalho de campo.", fieldBlueDark)
                    OutlinedTextField(projectName, { projectName = it }, label = { Text("Nome do projeto/LH") })
                    Button(onClick = {
                        if (projectName.isBlank()) {
                            status = "Informe o nome do projeto/LH"
                        } else {
                            scope.launch {
                                val normalizedName = projectName.trim()
                                val project = repository.findActiveProjectByName(normalizedName)
                                    ?: br.f21campo.domain.Project(EntityId.new(), normalizedName, Instant.now())
                                repository.save(project)
                                occupation = occupation.copy(projectId = project.id)
                                status = if (project.id.value.isNotBlank()) "Projeto salvo/selecionado — ID preservado" else "Projeto salvo localmente"
                                newStep = 2
                            }
                        }
                    }) { Text("SALVAR E AVANÇAR") }
                    if (projects.isNotEmpty()) {
                        Text("OU SELECIONE UM PROJETO SALVO", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                        projects.takeLast(4).reversed().forEach { project ->
                            Button(onClick = {
                                projectName = project.name
                                occupation = occupation.copy(projectId = project.id)
                                status = "Projeto selecionado: ${project.name}"
                                newStep = 2
                            }, modifier = Modifier.fillMaxWidth()) { Text(project.name) }
                        }
                    }
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 2) {
                    Button(onClick = { newStep = 1 }) { Text("VOLTAR") }
                    StepHeader(2, "ESTAÇÃO", "Informe a estação desta ocasião de campo.", fieldBlueDark)
                    OutlinedTextField(name, { name = it }, label = { Text("Nome da estação") })
                    OutlinedTextField(locality, { locality = it }, label = { Text("Localidade") })
                    Button(onClick = {
                        if (name.isBlank() || locality.isBlank()) {
                            status = "Informe o nome da estação e a localidade"
                        } else {
                            scope.launch {
                                val normalizedName = name.trim()
                                val normalizedLocality = locality.trim()
                                val existing = savedId?.let { repository.findStation(it) }
                                    ?: repository.findActiveStationByIdentity(normalizedName, normalizedLocality)
                                val station = existing?.copy(
                                    name = normalizedName,
                                    locality = normalizedLocality,
                                    municipality = existing.municipality,
                                ) ?: Station(EntityId.new(), normalizedName, normalizedLocality, null, Instant.now())
                                savedId = station.id
                                repository.save(station)
                                occupation = occupation.copy(stationId = station.id)
                                occupationPersisted = true
                                status = "Estação salva/selecionada — ID preservado"
                                newStep = 3
                            }
                        }
                    }) { Text("SALVAR E AVANÇAR") }
                    if (stations.isNotEmpty()) {
                        Text("OU SELECIONE UMA ESTAÇÃO SALVA", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                        stations.filter { !it.locality.isNullOrBlank() }.takeLast(4).reversed().forEach { station ->
                            Button(onClick = {
                                savedId = station.id
                                name = station.name
                                locality = station.locality.orEmpty()
                                occupation = occupation.copy(stationId = station.id)
                                occupationPersisted = true
                                status = "Estação selecionada: ${station.name}"
                                newStep = 3
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text("${station.name} · ${station.locality ?: "sem localidade"}")
                            }
                        }
                    }
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 3) {
                    Button(onClick = { newStep = 2 }) { Text("VOLTAR") }
                    StepHeader(3, "REFERÊNCIA", "Escolha RN, MT ou PA e registre o código.", fieldBlueDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ReferencePointType.entries.forEach { type -> Button(onClick = { referenceType = type }, enabled = referenceType != type) { Text(type.name) } } }
                    OutlinedTextField(referenceCode, { referenceCode = it }, label = { Text("Código da referência") })
                    Button(onClick = {
                        val stationId = savedId
                        if (stationId == null || referenceCode.isBlank()) {
                            status = "Informe estação e código da referência"
                        } else {
                            scope.launch {
                                val normalizedCode = referenceCode.trim()
                                val point = repository.findReferencePointByStationTypeAndCode(stationId, referenceType, normalizedCode)
                                    ?: ReferencePoint(EntityId.new(), stationId, referenceType, normalizedCode)
                                repository.save(point)
                                occupation = occupation.copy(stationId = stationId, referencePointId = point.id)
                                status = "Referência ${point.type}: ${point.code} salva/selecionada"
                                newStep = 4
                            }
                        }
                    }) { Text("SALVAR E AVANÇAR") }
                    if (referencePoints.isNotEmpty()) {
                        Text("OU SELECIONE UMA REFERÊNCIA DESTA ESTAÇÃO", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                        referencePoints.forEach { point ->
                            Button(onClick = {
                                referenceType = point.type
                                referenceCode = point.code
                                occupation = occupation.copy(stationId = point.stationId, referencePointId = point.id)
                                occupationPersisted = true
                                status = "Referência selecionada: ${point.type} ${point.code}"
                                newStep = 4
                            }, modifier = Modifier.fillMaxWidth()) { Text("${point.type} · ${point.code}") }
                        }
                    }
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 4) {
                    Button(onClick = { newStep = 3 }) { Text("VOLTAR") }
                    StepHeader(4, "EQUIPAMENTO", "Informe receptor e antena usados nesta ocupação.", fieldBlueDark)
                    if (favoriteReceiverProfiles.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("RECEPTOR GNSS FAVORITO", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                                Text("Escolha um perfil para preencher o receptor desta ocupação. A origem permanece informada pelo operador.")
                                favoriteReceiverProfiles.forEach { profile ->
                                    OutlinedButton(onClick = {
                                        receiverManufacturer = profile.receiverManufacturer.orEmpty()
                                        receiverModel = profile.receiverModel.orEmpty()
                                        receiverSerial = profile.receiverSerial.orEmpty()
                                        status = "Receptor favorito selecionado: ${profile.receiverModel ?: "sem modelo"}"
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text(listOfNotNull(profile.receiverManufacturer, profile.receiverModel, profile.receiverSerial?.let { "S/N $it" }).ifEmpty { listOf("Receptor sem identificação") }.joinToString(" · "))
                                    }
                                }
                            }
                        }
                    }
                    if (receiverCatalog.isNotEmpty() || antennaCatalog.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("CATÁLOGO MANUAL", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                                Text("Selecione um equipamento já cadastrado. Esses dados são informados pelo operador e serão copiados para o snapshot desta ocupação.")
                                receiverCatalog.forEach { item ->
                                    OutlinedButton(onClick = {
                                        selectedReceiverCatalogId = item.id
                                        receiverManufacturer = item.manufacturer.orEmpty()
                                        receiverModel = item.model.orEmpty()
                                        receiverSerial = item.serialNumber.orEmpty()
                                        receiverFirmware = item.firmware.orEmpty()
                                        status = "Receptor do catálogo selecionado"
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text(listOfNotNull(item.manufacturer, item.model, item.serialNumber?.let { "S/N $it" }).ifEmpty { listOf("Receptor sem identificação") }.joinToString(" · "))
                                    }
                                }
                                antennaCatalog.forEach { item ->
                                    OutlinedButton(onClick = {
                                        selectedAntennaCatalogId = item.id
                                        antennaManufacturer = item.manufacturer.orEmpty()
                                        antennaModel = item.model.orEmpty()
                                        antennaSerial = item.serialNumber.orEmpty()
                                        status = "Antena do catálogo selecionada"
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text(listOfNotNull(item.manufacturer, item.model, item.serialNumber?.let { "S/N $it" }).ifEmpty { listOf("Antena sem identificação") }.joinToString(" · "))
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(receiverModel, { receiverModel = it }, label = { Text("Modelo do receptor") })
                    OutlinedTextField(receiverManufacturer, { receiverManufacturer = it }, label = { Text("Fabricante do receptor") })
                    OutlinedTextField(receiverSerial, { receiverSerial = it }, label = { Text("Nº de série do receptor") })
                    OutlinedTextField(receiverFirmware, { receiverFirmware = it }, label = { Text("Firmware do receptor (se informado)") })
                    OutlinedTextField(antennaModel, { antennaModel = it }, label = { Text("Modelo da antena") })
                    OutlinedTextField(antennaManufacturer, { antennaManufacturer = it }, label = { Text("Fabricante da antena") })
                    OutlinedTextField(antennaSerial, { antennaSerial = it }, label = { Text("Nº de série da antena") })
                    Button(onClick = { val receiver = Receiver(EntityId.new(), receiverManufacturer.ifBlank { null }, receiverModel.ifBlank { "manual" }, receiverSerial.ifBlank { null }, firmware = receiverFirmware.ifBlank { null }); val antenna = Antenna(EntityId.new(), antennaManufacturer.ifBlank { null }, antennaModel.ifBlank { "manual" }, antennaSerial.ifBlank { null }); val result = ManualEquipment.attachSnapshot(occupation, receiver, antenna); if (result is DomainResult.Success) { occupation = result.value; recordAudit("EQUIPMENT_ASSOCIATED"); status = "Equipamento associado"; newStep = 5 } }) { Text("ASSOCIAR E AVANÇAR") }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                val item = ReceiverCatalogItem(
                                    id = selectedReceiverCatalogId ?: EntityId.new(),
                                    manufacturer = receiverManufacturer.trim().ifBlank { null },
                                    model = receiverModel.trim().ifBlank { null },
                                    serialNumber = receiverSerial.trim().ifBlank { null },
                                    firmware = receiverFirmware.trim().ifBlank { null },
                                    createdAt = receiverCatalog.firstOrNull { it.id == selectedReceiverCatalogId }?.createdAt ?: Instant.now(),
                                )
                                repository.saveReceiverCatalog(item)
                                receiverCatalog = repository.findActiveReceiverCatalog()
                                selectedReceiverCatalogId = item.id
                                status = "Receptor salvo no catálogo manual"
                            }
                        }, modifier = Modifier.weight(1f)) { Text("SALVAR RECEPTOR") }
                        OutlinedButton(onClick = {
                            scope.launch {
                                val item = AntennaCatalogItem(
                                    id = selectedAntennaCatalogId ?: EntityId.new(),
                                    manufacturer = antennaManufacturer.trim().ifBlank { null },
                                    model = antennaModel.trim().ifBlank { null },
                                    serialNumber = antennaSerial.trim().ifBlank { null },
                                    createdAt = antennaCatalog.firstOrNull { it.id == selectedAntennaCatalogId }?.createdAt ?: Instant.now(),
                                )
                                repository.saveAntennaCatalog(item)
                                antennaCatalog = repository.findActiveAntennaCatalog()
                                selectedAntennaCatalogId = item.id
                                status = "Antena salva no catálogo manual"
                            }
                        }, modifier = Modifier.weight(1f)) { Text("SALVAR ANTENA") }
                    }
                    Button(onClick = { route = "CONNECTION" }, modifier = Modifier.fillMaxWidth()) { Text("CONFIGURAR CONEXÃO DE BANCADA") }
                    Text("Origem: informado pelo operador")
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 5) {
                    Button(onClick = { newStep = 4 }) { Text("VOLTAR") }
                    StepHeader(5, "ALTURAS BEFORE", "Registre pelo menos uma leitura antes de iniciar.", fieldBlueDark)
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ALTURA DA ANTENA · ANTES", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            Text("Registre 1 ou mais leituras. A unidade fica gravada junto de cada evidência.", style = MaterialTheme.typography.bodySmall)
                            Text("UNIDADE DA ALTURA", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("mm", "cm", "m").forEach { unit -> Button(onClick = { beforeUnit = unit }, enabled = beforeUnit != unit) { Text(unit) } } }
                            Text("Selecionada: ${beforeUnit ?: "nenhuma — selecione uma unidade"}")
                            heightUnitHint(before, beforeUnit)?.let { Text(it, color = fieldBlueDark) }
                            Text("TIPO DA MEDIÇÃO", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { HeightType.entries.forEach { type -> Button(onClick = { beforeType = type }, enabled = beforeType != type) { Text(type.name) } } }
                            before.forEachIndexed { index, value -> OutlinedTextField(value, { v -> before = before.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }, modifier = Modifier.fillMaxWidth()) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { beforeUndo = beforeUndo + listOf(before); before = before + "" }) { Text("+ BEFORE") }; Button(enabled = before.size > 1, onClick = { beforeUndo = beforeUndo + listOf(before); before = before.dropLast(1) }) { Text("−") }; Button(enabled = beforeUndo.isNotEmpty(), onClick = { before = beforeUndo.last(); beforeUndo = beforeUndo.dropLast(1) }) { Text("DESFAZER") } }
                        }
                    }
                    Button(onClick = {
                        val values = before.mapNotNull(String::toDoubleOrNull).filter { it.isFinite() }
                        val firstValid = values.firstOrNull()?.let { heightToMeters(it, beforeUnit) }
                        if (beforeUnit == null) {
                            status = heightUnitHint(before, beforeUnit) ?: "Confirme a unidade da altura BEFORE"
                        } else if (firstValid == null) {
                            status = "Informe uma altura BEFORE válida"
                        } else {
                            val currentOccupation = occupation.copy(hasBeforeHeight = true, beforeHeightMeters = firstValid)
                            occupation = currentOccupation
                            scope.launch {
                                repository.replaceHeights(
                                    currentOccupation.id,
                                    HeightPhase.BEFORE,
                                    values.map { value -> HeightMeasurement(HeightPhase.BEFORE, heightToMeters(value, beforeUnit) ?: value, beforeType, Instant.now(), "unit=${beforeUnit}") },
                                )
                                recordAudit("HEIGHT_BEFORE_RECORDED")
                                status = "${values.size} altura(s) BEFORE registrada(s) em ${beforeUnit} — pronto para READY"
                                newStep = 6
                            }
                        }
                    }) { Text("REGISTRAR BEFORE E AVANÇAR") }
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 6) {
                    Button(onClick = { newStep = 5 }) { Text("VOLTAR") }
                    StepHeader(6, "PREPARO", "Confira os requisitos antes de iniciar.", fieldBlueDark)
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Checklist para iniciar", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            Text("Projeto/LH: ${if (projectName.isNotBlank()) "OK" else "pendente"}")
                            Text("Estação/localidade: ${if (name.isNotBlank() && locality.isNotBlank()) "OK" else "pendente"}")
                            Text("Referência RN/MT/PA: ${if (occupation.referencePointId != null) "OK" else "pendente"}")
                            Text("Equipamento manual: ${if (occupation.equipment != null) "OK" else "pendente"}")
                            Text("Altura BEFORE: ${if (occupation.hasBeforeHeight) "OK" else "pendente"}")
                            Text("Tempo planejado: ${occupation.plannedDurationSeconds?.let { "${it / 60} min" } ?: "indefinido"}")
                        }
                    }
                    OutlinedTextField(durationMinutes, { value -> durationMinutes = value.filter(Char::isDigit); occupation = occupation.copy(plannedDurationSeconds = value.toLongOrNull()?.takeIf { it > 0 }?.times(60)) }, label = { Text("Tempo planejado em minutos (opcional)") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        val missing = OccupationReadiness.missing(ReadinessInput(
                            projectName = projectName,
                            stationName = name,
                            locality = locality,
                            hasReference = occupation.referencePointId != null,
                            hasEquipment = occupation.equipment != null,
                            hasBeforeHeight = occupation.hasBeforeHeight,
                        ))
                        if (missing.isNotEmpty()) {
                            newStep = when {
                                projectName.isBlank() -> 1
                                name.isBlank() || locality.isBlank() -> 2
                                occupation.referencePointId == null -> 3
                                occupation.equipment == null -> 4
                                else -> 5
                            }
                            status = "READY bloqueado: complete ${missing.joinToString(", ")}"
                        } else {
                            val result = OccupationStateMachine.ready(occupation)
                            if (result is DomainResult.Success) {
                                occupation = result.value
                                recordAudit("OCCUPATION_READY")
                                status = "READY confirmado — toque em INICIAR"
                            } else {
                                status = "READY bloqueado pela máquina de estados"
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("READY") }
                    Button(enabled = occupation.state == OccupationState.READY, onClick = {
                        val result = OccupationStateMachine.start(occupation, Instant.now())
                        if (result is DomainResult.Success) { trackingTimeAlerted = false; occupation = result.value; recordAudit("OCCUPATION_STARTED"); route = "ACTIVE"; status = "Rastreio iniciado" }
                    }, modifier = Modifier.fillMaxWidth()) { Text("INICIAR") }
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.READY) {
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                    Text("RASTREIO PRONTO", style = MaterialTheme.typography.headlineSmall)
                    Text("ETAPA 6/7 · READY", style = MaterialTheme.typography.titleMedium)
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tudo pronto para iniciar", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            Text("Referência: ${referenceType.name} ${referenceCode.ifBlank { "selecionada" }}")
                            Text("Receptor: ${occupation.equipment?.receiver?.model ?: receiverModel.ifBlank { "manual" }}")
                            Text("Antena: ${occupation.equipment?.antenna?.model ?: antennaModel.ifBlank { "manual" }}")
                            Text("Tempo planejado: ${occupation.plannedDurationSeconds?.let { "${it / 60} min" } ?: "indefinido"}")
                        }
                    }
                    Button(onClick = {
                        val result = OccupationStateMachine.start(occupation, Instant.now())
                        if (result is DomainResult.Success) { trackingTimeAlerted = false; occupation = result.value; recordAudit("OCCUPATION_STARTED"); route = "ACTIVE"; status = "Rastreio iniciado" }
                    }, modifier = Modifier.fillMaxWidth()) { Text("INICIAR RASTREIO") }
                    Text(status)
                } else if (route == "ACTIVE" && occupation.state == OccupationState.ACTIVE) {
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("RASTREIO ATIVO", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            Text("ETAPA 6/7 · CAMPO", color = Color(0xFFD5EAF5), style = MaterialTheme.typography.titleMedium)
                            Text("Acompanhe a ocupação e registre ocorrências.", color = Color(0xFFD5EAF5))
                        }
                    }
                    val trackingTime = occupation.confirmedStart?.let { TrackingTimer.measure(it, Instant.ofEpochMilli(nowEpochMillis), occupation.plannedDurationSeconds) }
                    val activeElapsed = trackingTime?.elapsedSeconds
                    val target = trackingTime?.plannedSeconds
                    val reached = trackingTime?.targetReached == true
                    Card(colors = CardDefaults.cardColors(containerColor = if (reached) Color(0xFFFFF4D6) else Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Status: ACTIVE", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            Text("Referência: ${referenceType.name} ${referenceCode.ifBlank { "selecionada" }}")
                            Text("Cronômetro: ${activeElapsed?.let { "${it / 60}m ${it % 60}s" } ?: "aguardando"}")
                            Text("Meta: ${target?.let { "${it / 60} min" } ?: "indefinida"}")
                            if (reached) Text("ALERTA: tempo planejado cumprido")
                            Text("Receptor: ${occupation.equipment?.receiver?.model ?: receiverModel.ifBlank { "manual" }}")
                            Text("Antena: ${occupation.equipment?.antenna?.model ?: antennaModel.ifBlank { "manual" }}")
                        }
                    }
                    EventCaptureControls(eventCategory, { eventCategory = it }, eventSeverity, { eventSeverity = it }, fieldBlueDark)
                    OutlinedTextField(event, { event = it }, label = { Text("Evento de campo") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        if (event.isNotBlank()) {
                            scope.launch {
                                repository.save(OccupationEvent(EntityId.new(), occupation.id, Instant.now(), eventCategory, eventSeverity, event.trim()))
                                recordAudit("OCCUPATION_EVENT_${eventCategory.name}")
                                fieldEvents = repository.findEvents(occupation.id)
                                event = ""
                                status = "Evento registrado"
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("REGISTRAR EVENTO") }
                    if (fieldEvents.isNotEmpty() || auditEvents.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("LINHA DO TEMPO · ${fieldEvents.size} evento(s) · ${auditEvents.size} auditoria(s)", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                                fieldEvents.takeLast(3).forEach { savedEvent ->
                                    Text("${savedEvent.at}: ${savedEvent.category}/${savedEvent.severity} · ${savedEvent.description}")
                                }
                                auditEvents.takeLast(3).forEach { savedAudit -> Text("${savedAudit.at}: ${savedAudit.action} · ${savedAudit.actor}", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                    Button(onClick = {
                        val result = OccupationStateMachine.stop(occupation, Instant.now())
                        if (result is DomainResult.Success) { occupation = result.value; recordAudit("OCCUPATION_STOPPED"); route = "FINALIZATION"; status = "Rastreio parado — finalização" }
                    }, modifier = Modifier.fillMaxWidth()) { Text("PARAR RASTREIO") }
                    Text(status)
                } else if (route == "FINALIZATION" && occupation.state in setOf(OccupationState.STOPPED, OccupationState.COLLECTED, OccupationState.VALIDATED)) {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("FINALIZAÇÃO", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            Text("Referência: ${referenceCode.ifBlank { "ocupação recuperada" }}", color = Color(0xFFD5EAF5))
                            Text("Status: ${occupation.state}", color = Color(0xFFD5EAF5))
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("PRÓXIMOS ITENS", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                            Text("1. Registre as alturas AFTER.")
                            Text("2. Importe o arquivo RAW diretamente nesta tela.")
                            Text("3. Finalize a coleta após as duas evidências.")
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ARQUIVO BRUTO · RAW", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            Text("Escolha aqui um arquivo RAW que esteja acessível no celular. O F-21 copia o original para o armazenamento controlado, calcula o SHA-256 e associa a evidência a esta ocupação.", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { rawPicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                                Text("IMPORTAR RAW DO CELULAR")
                            }
                            Text(
                                if (rawImported) "RAW_RECEIVER importado: ${rawSummary ?: "SHA-256 calculado"}"
                                else "Nenhum RAW associado ainda",
                                color = if (rawImported) fieldBlueDark else Color(0xFF52636D),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text("Importação direta da antena ainda não está disponível; ela depende de transporte e protocolo comprovados.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ALTURA DA ANTENA · DEPOIS", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            Text("Registre as leituras AFTER antes de finalizar a coleta.", style = MaterialTheme.typography.bodySmall)
                            Text("Unidade AFTER: ${afterUnit ?: "não selecionada"}")
                            heightUnitHint(after, afterUnit)?.let { Text(it, color = fieldBlueDark) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("mm", "cm", "m").forEach { unit ->
                                    Button(onClick = { afterUnit = unit }, enabled = afterUnit != unit) { Text(unit) }
                                }
                            }
                            Text("TIPO DA MEDIÇÃO", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { HeightType.entries.forEach { type -> Button(onClick = { afterType = type }, enabled = afterType != type) { Text(type.name) } } }
                            after.forEachIndexed { index, value -> OutlinedTextField(value, { v -> after = after.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }, modifier = Modifier.fillMaxWidth()) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { afterUndo = afterUndo + listOf(after); after = after + "" }) { Text("+ AFTER") }
                                Button(enabled = after.size > 1, onClick = { afterUndo = afterUndo + listOf(after); after = after.dropLast(1) }) { Text("−") }
                                Button(enabled = afterUndo.isNotEmpty(), onClick = { after = afterUndo.last(); afterUndo = afterUndo.dropLast(1) }) { Text("DESFAZER") }
                            }
                        }
                    }
                    Button(onClick = {
                        val values = after.mapNotNull(String::toDoubleOrNull).filter { it.isFinite() }
                        if (afterUnit == null) status = heightUnitHint(after, afterUnit) ?: "Confirme a unidade da altura AFTER"
                        else if (values.isEmpty()) status = "Informe ao menos uma altura AFTER válida"
                        else scope.launch {
                            repository.replaceHeights(
                                occupation.id,
                                HeightPhase.AFTER,
                                values.map { value -> HeightMeasurement(HeightPhase.AFTER, heightToMeters(value, afterUnit) ?: value, afterType, Instant.now(), "unit=${afterUnit}") },
                            )
                            recordAudit("HEIGHT_AFTER_RECORDED")
                            afterRegistered = true
                            status = "${values.size} altura(s) AFTER registrada(s) em ${afterUnit}"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("REGISTRAR AFTER") }
                    if (rawArtifacts.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("INVENTÁRIO RAW", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                                rawArtifacts.forEach { artifact ->
                                    Text(artifact.path.substringAfterLast(File.separator), color = fieldBlueDark)
                                    Text("SHA-256: ${artifact.sha256}", style = MaterialTheme.typography.bodySmall)
                                    Text("Tamanho: ${artifact.sizeBytes} bytes · original imutável", style = MaterialTheme.typography.bodySmall)
                                    OutlinedButton(onClick = {
                                        val valid = rawStore.verify(File(artifact.path), artifact.sizeBytes, artifact.sha256)
                                        status = if (valid) "SHA-256 confirmado para ${artifact.path.substringAfterLast(File.separator)}" else "Falha na verificação do SHA-256 — arquivo não deve ser usado"
                                        recordAudit(if (valid) "RAW_HASH_VERIFIED" else "RAW_HASH_INVALID")
                                    }, modifier = Modifier.fillMaxWidth()) { Text("VERIFICAR SHA-256") }
                                }
                            }
                        }
                    }
                    Text(if (afterRegistered) "Altura AFTER registrada" else "Altura AFTER pendente")
                    Button(enabled = occupation.state == OccupationState.STOPPED && rawImported && afterRegistered, onClick = {
                        val result = OccupationStateMachine.collectWithEvidence(occupation, hasRawEvidence = rawImported, hasAfterHeight = afterRegistered)
                        if (result is DomainResult.Success) { occupation = result.value; recordAudit("OCCUPATION_COLLECTED") }
                    }) { Text("FINALIZAR COLETA") }
                    Button(enabled = occupation.state == OccupationState.COLLECTED, onClick = { val result = OccupationStateMachine.validate(occupation); if (result is DomainResult.Success) { occupation = result.value; recordAudit("OCCUPATION_VALIDATED") } }) { Text("VALIDAR RASTREIO") }
                    Text("Resumo: início ${occupation.confirmedStart ?: "—"} · fim ${occupation.confirmedStop ?: "—"}")
                    if (fieldEvents.isNotEmpty()) Text("Eventos registrados: ${fieldEvents.size}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                        Button(onClick = { startNewTracking() }) { Text("NOVO RASTREIO") }
                    }
                } else {
                Button(onClick = { route = "HOME"; showHome = true }) { Text("INÍCIO") }
                Text(when (occupation.state) { OccupationState.ACTIVE -> "RASTREIO ATIVO"; OccupationState.STOPPED, OccupationState.COLLECTED, OccupationState.VALIDATED -> "FINALIZAÇÃO DO RASTREIO"; else -> "NOVO RASTREIO" }, style = MaterialTheme.typography.headlineSmall)
                Text(if (occupation.state == OccupationState.ACTIVE) "Etapa 6/7 — Rastreio" else if (occupation.state == OccupationState.STOPPED) "Etapa 7/7 — Finalização" else "Etapa 1/7 — Preparação")
                if (occupation.state == OccupationState.ACTIVE) {
                    Text("REFERÊNCIA: ${referenceCode.ifBlank { "selecionada" }}")
                    Text("STATUS: RASTREIO ATIVO")
                    val activeElapsed = occupation.confirmedStart?.let { ((nowEpochMillis - it.toEpochMilli()).coerceAtLeast(0) / 1000) }
                    Text("CRONÔMETRO: ${activeElapsed?.let { "${it / 60}m ${it % 60}s" } ?: "aguardando"}")
                    Text("Receptor: ${occupation.equipment?.receiver?.model ?: receiverModel.ifBlank { "manual" }}")
                    Text("Antena: ${occupation.equipment?.antenna?.model ?: antennaModel.ifBlank { "manual" }}")
                    EventCaptureControls(eventCategory, { eventCategory = it }, eventSeverity, { eventSeverity = it }, fieldBlueDark)
                    OutlinedTextField(event, { event = it }, label = { Text("Evento de campo") })
                    Button(onClick = { if (event.isNotBlank()) scope.launch { repository.save(OccupationEvent(EntityId.new(), occupation.id, Instant.now(), eventCategory, eventSeverity, event.trim())); recordAudit("OCCUPATION_EVENT_${eventCategory.name}"); event = ""; status = "Evento registrado" } }) { Text("REGISTRAR EVENTO") }
                    Button(onClick = { val result = OccupationStateMachine.stop(occupation, Instant.now()); if (result is DomainResult.Success) { occupation = result.value; recordAudit("OCCUPATION_STOPPED"); route = "FINALIZATION"; status = "Rastreio parado — finalização" } }) { Text("PARAR RASTREIO") }
                } else {
                Text("F-21 Campo", style = MaterialTheme.typography.headlineMedium)
                Text("Versão ${BuildConfig.VERSION_NAME}")
                Text("Modo: ${BuildConfig.BUILD_MODE}")
                Text(status)
                OutlinedTextField(name, { name = it }, label = { Text("Nome da estação") })
                OutlinedTextField(locality, { locality = it }, label = { Text("Localidade (opcional)") })
                Button(onClick = {
                    val id = savedId ?: EntityId.new().also { savedId = it }
                    scope.launch {
                        repository.save(Station(id, name, locality.ifBlank { null }, municipality = null, createdAt = Instant.now()))
                        status = "Estação salva — ID preservado"
                    }
                }) { Text("Salvar estação") }
                Text("Referência rastreada")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReferencePointType.entries.forEach { type ->
                        Button(onClick = { referenceType = type }, enabled = referenceType != type) { Text(type.name) }
                    }
                }
                OutlinedTextField(referenceCode, { referenceCode = it }, label = { Text("Código da referência") })
                Button(onClick = {
                    val stationId = savedId
                    if (stationId == null || referenceCode.isBlank()) {
                        status = "Salve a estação e informe o código da referência"
                    } else {
                        val point = ReferencePoint(EntityId.new(), stationId, referenceType, referenceCode.trim())
                        scope.launch {
                            repository.save(point)
                            occupation = occupation.copy(stationId = stationId, referencePointId = point.id)
                            status = "Referência ${point.type}: ${point.code} registrada"
                        }
                    }
                }) { Text("Registrar referência") }
                HorizontalDivider()
                Text("Ocupação: ${occupation.state}")
                OutlinedTextField(durationMinutes, { value -> durationMinutes = value.filter(Char::isDigit); occupation = occupation.copy(plannedDurationSeconds = value.toLongOrNull()?.takeIf { it > 0 }?.times(60)) }, label = { Text("Tempo planejado (minutos, opcional)") })
                val elapsedSeconds = occupation.confirmedStart?.let { start -> ((if (occupation.confirmedStop != null) occupation.confirmedStop!!.toEpochMilli() else nowEpochMillis) - start.toEpochMilli()).coerceAtLeast(0) / 1000 }
                if (elapsedSeconds != null) {
                    val target = occupation.plannedDurationSeconds
                    val reached = target != null && elapsedSeconds >= target
                    Text(if (target == null) "Tempo decorrido: ${elapsedSeconds / 60}m ${elapsedSeconds % 60}s — INDEFINIDO" else "Tempo: ${elapsedSeconds / 60}m ${elapsedSeconds % 60}s / ${target / 60}m — ${if (reached) "TEMPO ATINGIDO" else "EM ANDAMENTO"}")
                    if (reached) Text("⚠ ALERTA: tempo selecionado já foi cumprido")
                }
                Button(onClick = {
                    occupation = Occupation(EntityId.new(), savedId ?: EntityId.new(), EntityId.new())
                    occupationPersisted = false
                    receiverModel = ""
                    antennaModel = ""
                    receiverManufacturer = ""
                    antennaManufacturer = ""
                    receiverSerial = ""
                    antennaSerial = ""
                    before = listOf("")
                    after = listOf("")
                    beforeUndo = emptyList()
                    afterUndo = emptyList()
                    event = ""
                    rawImported = false
                    rawSummary = null
                    afterRegistered = false
                    status = "Nova ocupação criada"
                }) { Text("Nova ocupação") }
                OutlinedTextField(receiverModel, { receiverModel = it }, label = { Text("Modelo do receptor") })
                OutlinedTextField(receiverManufacturer, { receiverManufacturer = it }, label = { Text("Fabricante do receptor") })
                OutlinedTextField(receiverSerial, { receiverSerial = it }, label = { Text("Nº de série do receptor") })
                OutlinedTextField(antennaModel, { antennaModel = it }, label = { Text("Modelo da antena") })
                OutlinedTextField(antennaManufacturer, { antennaManufacturer = it }, label = { Text("Fabricante da antena") })
                OutlinedTextField(antennaSerial, { antennaSerial = it }, label = { Text("Nº de série da antena") })
                Text("Origem do equipamento: informado pelo operador")
                Button(onClick = {
                    val receiver = Receiver(EntityId.new(), manufacturer = receiverManufacturer.ifBlank { null }, model = receiverModel.ifBlank { "manual" }, serialNumber = receiverSerial.ifBlank { null }, firmware = receiverFirmware.ifBlank { null })
                    val antenna = Antenna(EntityId.new(), manufacturer = antennaManufacturer.ifBlank { null }, model = antennaModel.ifBlank { "manual" }, serialNumber = antennaSerial.ifBlank { null })
                    val result = ManualEquipment.attachSnapshot(occupation, receiver, antenna)
                    if (result is DomainResult.Success) { occupation = result.value; recordAudit("EQUIPMENT_ASSOCIATED"); status = "Equipamento associado" }
                }) { Text("Associar receptor e antena") }
                Button(enabled = occupation.state == OccupationState.DRAFT, onClick = {
                    val result = OccupationStateMachine.ready(occupation)
                    if (result is DomainResult.Success) { occupation = result.value; recordAudit("OCCUPATION_READY"); status = "READY confirmado — INICIAR está disponível" }
                    else {
                        if (occupation.referencePointId == null) newStep = 3
                        else if (!occupation.hasBeforeHeight) newStep = 5
                        status = when (val error = (result as DomainResult.Failure).error) {
                            is br.f21campo.domain.DomainError.InvalidValue -> "READY bloqueado: ${error.reason}. Use a etapa indicada para corrigir."
                            else -> "READY bloqueado: verifique os dados da ocupação"
                        }
                    }
                }) { Text("READY") }
                if (occupation.state == OccupationState.DRAFT) {
                    Text("Para READY: referência ${if (occupation.referencePointId != null) "OK" else "pendente"} · altura BEFORE ${if (occupation.hasBeforeHeight) "OK" else "pendente"}")
                    if (occupation.referencePointId == null) Button(onClick = { newStep = 3 }) { Text("IR PARA REFERÊNCIA") }
                    if (!occupation.hasBeforeHeight) Button(onClick = { newStep = 5 }) { Text("IR PARA ALTURA BEFORE") }
                }
                Button(enabled = occupation.state == OccupationState.READY, onClick = { val result = OccupationStateMachine.start(occupation, Instant.now()); if (result is DomainResult.Success) { trackingTimeAlerted = false; occupation = result.value; recordAudit("OCCUPATION_STARTED"); route = "ACTIVE"; status = "Rastreio iniciado" } }) { Text("INICIAR") }
                Button(enabled = occupation.state == OccupationState.ACTIVE, onClick = { val result = OccupationStateMachine.stop(occupation, Instant.now()); if (result is DomainResult.Success) { occupation = result.value; recordAudit("OCCUPATION_STOPPED"); route = "FINALIZATION"; status = "Rastreio parado — etapa de finalização" } }) { Text("PARAR") }
                Button(enabled = occupation.state == OccupationState.STOPPED && rawImported && afterRegistered, onClick = {
                    val result = OccupationStateMachine.collectWithEvidence(occupation, hasRawEvidence = rawImported, hasAfterHeight = afterRegistered)
                    if (result is DomainResult.Success) { occupation = result.value; recordAudit("OCCUPATION_COLLECTED"); status = "Coleta finalizada — pronta para resumo" }
                }) { Text("FINALIZAR COLETA") }
                Button(enabled = occupation.state == OccupationState.COLLECTED, onClick = {
                    val result = OccupationStateMachine.validate(occupation)
                    if (result is DomainResult.Success) { occupation = result.value; recordAudit("OCCUPATION_VALIDATED"); status = "Rastreio validado" }
                }) { Text("VALIDAR RASTREIO") }
                if (occupation.state in setOf(OccupationState.STOPPED, OccupationState.COLLECTED, OccupationState.VALIDATED)) {
                    Text("Resumo do rastreio")
                    Text("Início: ${occupation.confirmedStart ?: "não registrado"}")
                    Text("Fim: ${occupation.confirmedStop ?: "não registrado"}")
                    val totalSeconds = if (occupation.confirmedStart != null && occupation.confirmedStop != null) (occupation.confirmedStop!!.epochSecond - occupation.confirmedStart!!.epochSecond).coerceAtLeast(0) else null
                    Text("Duração real: ${totalSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "em aberto"}")
                    Text("Tempo planejado: ${occupation.plannedDurationSeconds?.let { "${it / 60} min" } ?: "indefinido"}")
                    val beforeValues = before.mapNotNull(String::toDoubleOrNull).filter(Double::isFinite)
                    val afterValues = after.mapNotNull(String::toDoubleOrNull).filter(Double::isFinite)
                    if (beforeValues.isNotEmpty()) Text("BEFORE: ${beforeValues.size} leitura(s), média ${"%.4f".format(beforeValues.average())} m, amplitude ${"%.4f".format(beforeValues.max() - beforeValues.min())} m")
                    if (afterValues.isNotEmpty()) Text("AFTER: ${afterValues.size} leitura(s), média ${"%.4f".format(afterValues.average())} m, amplitude ${"%.4f".format(afterValues.max() - afterValues.min())} m")
                    if (beforeValues.isNotEmpty() && afterValues.isNotEmpty()) Text("Delta das médias: ${"%.4f".format(afterValues.average() - beforeValues.average())} m")
                }
                Text("Alturas BEFORE")
                Text("Unidade BEFORE: ${beforeUnit ?: "não selecionada"}")
                heightUnitHint(before, beforeUnit)?.let { Text(it, color = fieldBlueDark) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("mm", "cm", "m").forEach { unit -> Button(onClick = { beforeUnit = unit }, enabled = beforeUnit != unit) { Text(unit) } } }
                Text("Tipo BEFORE: ${beforeType.name}", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { HeightType.entries.forEach { type -> Button(onClick = { beforeType = type }, enabled = beforeType != type) { Text(type.name) } } }
                before.forEachIndexed { index, value -> OutlinedTextField(value, { v -> before = before.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { beforeUndo = beforeUndo + listOf(before); before = before + "" }) { Text("+ BEFORE") }
                    Button(enabled = before.size > 1, onClick = { beforeUndo = beforeUndo + listOf(before); before = before.dropLast(1) }) { Text("−") }
                    Button(enabled = beforeUndo.isNotEmpty(), onClick = { before = beforeUndo.last(); beforeUndo = beforeUndo.dropLast(1) }) { Text("DESFAZER") }
                }
                Button(onClick = {
                    val validBefore = before.mapNotNull { it.toDoubleOrNull() }.firstOrNull { it.isFinite() }?.let { heightToMeters(it, beforeUnit) }
                    if (beforeUnit == null) {
                        status = heightUnitHint(before, beforeUnit) ?: "Confirme a unidade da altura BEFORE"
                    } else if (validBefore != null) {
                        occupation = occupation.copy(hasBeforeHeight = true, beforeHeightMeters = validBefore)
                        scope.launch {
                            repository.replaceHeights(occupation.id, HeightPhase.BEFORE, before.mapNotNull { it.toDoubleOrNull() }.filter { it.isFinite() }.map { value -> HeightMeasurement(HeightPhase.BEFORE, heightToMeters(value, beforeUnit) ?: value, beforeType, Instant.now(), "unit=${beforeUnit}") })
                            recordAudit("HEIGHT_BEFORE_RECORDED")
                            status = "${before.count { it.toDoubleOrNull() != null }} altura(s) BEFORE registrada(s)"
                        }
                    } else {
                        status = "Informe ao menos uma altura BEFORE válida"
                    }
                }) { Text("Registrar altura BEFORE") }
                Text("Alturas AFTER")
                Text("Unidade AFTER: ${afterUnit ?: "não selecionada"}")
                heightUnitHint(after, afterUnit)?.let { Text(it, color = fieldBlueDark) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("mm", "cm", "m").forEach { unit -> Button(onClick = { afterUnit = unit }, enabled = afterUnit != unit) { Text(unit) } } }
                Text("Tipo AFTER: ${afterType.name}", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { HeightType.entries.forEach { type -> Button(onClick = { afterType = type }, enabled = afterType != type) { Text(type.name) } } }
                after.forEachIndexed { index, value -> OutlinedTextField(value, { v -> after = after.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { afterUndo = afterUndo + listOf(after); after = after + "" }) { Text("+ AFTER") }
                    Button(enabled = after.size > 1, onClick = { afterUndo = afterUndo + listOf(after); after = after.dropLast(1) }) { Text("−") }
                    Button(enabled = afterUndo.isNotEmpty(), onClick = { after = afterUndo.last(); afterUndo = afterUndo.dropLast(1) }) { Text("DESFAZER") }
                }
                Button(onClick = {
                    val values = after.mapNotNull { it.toDoubleOrNull() }.filter { it.isFinite() }
                    if (afterUnit == null) status = heightUnitHint(after, afterUnit) ?: "Confirme a unidade da altura AFTER" else if (values.isEmpty()) status = "Informe ao menos uma altura AFTER válida" else scope.launch {
                        repository.replaceHeights(occupation.id, HeightPhase.AFTER, values.map { value -> HeightMeasurement(HeightPhase.AFTER, heightToMeters(value, afterUnit) ?: value, afterType, Instant.now(), "unit=${afterUnit}") })
                        recordAudit("HEIGHT_AFTER_RECORDED")
                        status = "${values.size} altura(s) AFTER registrada(s)"
                    }
                }) { Text("Registrar alturas AFTER") }
                EventCaptureControls(eventCategory, { eventCategory = it }, eventSeverity, { eventSeverity = it }, fieldBlueDark)
                OutlinedTextField(event, { event = it }, label = { Text("Evento de campo") })
                Button(onClick = {
                    if (event.isNotBlank()) {
                        scope.launch {
                            repository.save(OccupationEvent(EntityId.new(), occupation.id, Instant.now(), eventCategory, eventSeverity, event.trim()))
                            recordAudit("OCCUPATION_EVENT_${eventCategory.name}")
                            status = "Evento registrado: $event"
                            event = ""
                        }
                    }
                }) { Text("Registrar evento") }
                Button(onClick = { rawPicker.launch("*/*") }) { Text("Importar arquivo bruto") }
                }
                }
            }
        }
    }
}

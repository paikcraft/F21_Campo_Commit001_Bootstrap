package br.f21campo.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import br.f21campo.files.RawFileStore
import androidx.room.Room
import br.f21campo.data.F21Database
import br.f21campo.data.ProjectStationRepository
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
import br.f21campo.domain.HeightMeasurement
import br.f21campo.domain.TrackingTimer
import br.f21campo.receiver.api.ReceiverTransportType
import br.f21campo.receiver.manual.ManualReceiverConnection
import java.time.Instant
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.dp

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

private fun readinessMissingItems(
    projectName: String,
    stationName: String,
    locality: String,
    hasReference: Boolean,
    hasEquipment: Boolean,
    hasBeforeHeight: Boolean,
): List<String> = buildList {
    if (projectName.isBlank()) add("projeto/LH")
    if (stationName.isBlank()) add("nome da estação")
    if (locality.isBlank()) add("localidade")
    if (!hasReference) add("referência RN/MT/PA")
    if (!hasEquipment) add("equipamento")
    if (!hasBeforeHeight) add("altura BEFORE")
}

@Composable
private fun StepHeader(step: Int, title: String, detail: String, fieldBlueDark: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("NOVO RASTREIO", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text("ETAPA $step/7 · $title", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = Color(0xFFD5EAF5))
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(applicationContext, F21Database::class.java, "f21.db")
            .addMigrations(Migrations.V1_TO_V2, Migrations.V2_TO_V3, Migrations.V3_TO_V4, Migrations.V4_TO_V5, Migrations.V5_TO_V6, Migrations.V6_TO_V7, Migrations.V7_TO_V8, Migrations.V8_TO_V9, Migrations.V9_TO_V10, Migrations.V10_TO_V11, Migrations.V11_TO_V12)
            .build()
        setContent { StationScreen(ProjectStationRepository(database.projectDao(), database.stationDao(), database.referencePointDao(), database.occupationDao(), database.occupationArtifactDao(), database.occupationEventDao(), database.heightMeasurementDao())) }
    }
}

@Composable
private fun StationScreen(repository: ProjectStationRepository) {
    val context = LocalContext.current
    val rawStore = remember { RawFileStore(File(context.filesDir, "raw")) }
    var name by remember { mutableStateOf("") }
    var projectName by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    var savedId by remember { mutableStateOf<EntityId?>(null) }
    var status by remember { mutableStateOf("Banco de Estações") }
    var occupation by remember { mutableStateOf(Occupation(EntityId.new(), EntityId.new(), EntityId.new())) }
    var receiverModel by remember { mutableStateOf("") }
    var antennaModel by remember { mutableStateOf("") }
    var receiverManufacturer by remember { mutableStateOf("") }
    var antennaManufacturer by remember { mutableStateOf("") }
    var receiverSerial by remember { mutableStateOf("") }
    var antennaSerial by remember { mutableStateOf("") }
    var before by remember { mutableStateOf(listOf("")) }
    var after by remember { mutableStateOf(listOf("")) }
    var beforeUndo by remember { mutableStateOf(emptyList<List<String>>()) }
    var afterUndo by remember { mutableStateOf(emptyList<List<String>>()) }
    var beforeUnit by remember { mutableStateOf<String?>(null) }
    var afterUnit by remember { mutableStateOf<String?>(null) }
    var event by remember { mutableStateOf("") }
    var fieldEvents by remember { mutableStateOf(emptyList<OccupationEvent>()) }
    var referenceCode by remember { mutableStateOf("") }
    var referenceType by remember { mutableStateOf(ReferencePointType.RN) }
    var showHome by remember { mutableStateOf(true) }
    var route by remember { mutableStateOf("HOME") }
    var newStep by remember { mutableStateOf(1) }
    var projects by remember { mutableStateOf(emptyList<br.f21campo.domain.Project>()) }
    var stations by remember { mutableStateOf(emptyList<Station>()) }
    var stationHistory by remember { mutableStateOf(emptyList<Occupation>()) }
    var pendingOccupationSummary by remember { mutableStateOf<String?>(null) }
    var rawImported by remember { mutableStateOf(false) }
    var rawSummary by remember { mutableStateOf<String?>(null) }
    var afterRegistered by remember { mutableStateOf(false) }
    var durationMinutes by remember { mutableStateOf("") }
    var trackingTimeAlerted by remember { mutableStateOf(false) }
    var connectionTransport by remember { mutableStateOf(ReceiverTransportType.WIFI_TCP) }
    var connectionHost by remember { mutableStateOf("") }
    var connectionPort by remember { mutableStateOf("") }
    var bluetoothName by remember { mutableStateOf("") }
    var bluetoothMac by remember { mutableStateOf("") }
    var connectionNotes by remember { mutableStateOf("") }
    var connectionStatus by remember { mutableStateOf("Nenhum perfil de conexão testado") }
    val manualConnection = remember { ManualReceiverConnection() }
    var nowEpochMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val rawPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val imported = File.createTempFile("import-", ".part", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input -> imported.outputStream().use { output -> input.copyTo(output) } }
            val stored = rawStore.import(imported)
            imported.delete()
            scope.launch {
                repository.saveRawArtifact(occupation.id, stored.path.absolutePath, stored.sizeBytes, stored.sha256)
                rawImported = true
                rawSummary = "${stored.sha256.take(12)} · ${stored.sizeBytes} bytes"
                status = "Bruto RAW_RECEIVER importado: ${stored.sha256.take(12)}…"
            }
        }
    }
    val startNewTracking = {
        name = ""
        projectName = ""
        locality = ""
        savedId = null
        occupation = Occupation(EntityId.new(), EntityId.new(), EntityId.new())
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
        beforeUnit = null
        afterUnit = null
        afterRegistered = false
        event = ""
        referenceCode = ""
        referenceType = ReferencePointType.RN
        rawImported = false
        rawSummary = null
        durationMinutes = ""
        trackingTimeAlerted = false
        route = "NEW"
        newStep = 1
        showHome = false
        status = "Etapa 1/7 — Projeto"
    }
    LaunchedEffect(occupation) { repository.save(occupation) }
    LaunchedEffect(occupation.id) { fieldEvents = repository.findEvents(occupation.id) }
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
    LaunchedEffect(route) {
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
    val fieldBlue = Color(0xFF0B4F71)
    val fieldBlueDark = Color(0xFF073653)
    val fieldBackground = Color(0xFFF1F6FA)
    MaterialTheme(colorScheme = lightColorScheme(primary = fieldBlue, secondary = Color(0xFF176E96), background = fieldBackground, surface = Color.White)) {
        Surface(modifier = Modifier.fillMaxSize(), color = fieldBackground) {
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 48.dp).verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (route == "HOME") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("F-21 Campo", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                            Text("Aquisição e rastreio de referências", color = Color.White)
                            Text("Versão ${BuildConfig.VERSION_NAME} · funcionamento offline", color = Color(0xFFD5EAF5))
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
                                            val heights = repository.findHeights(pending.id)
                                            val recoveredProject = repository.findProject(pending.projectId)
                                            val recoveredStation = repository.findStation(pending.stationId)
                                            val recoveredReference = pending.referencePointId?.let { repository.findReferencePoint(it) }
                                            occupation = pending
                                            projectName = recoveredProject?.name.orEmpty()
                                            savedId = recoveredStation?.id
                                            name = recoveredStation?.name.orEmpty()
                                            locality = recoveredStation?.locality.orEmpty()
                                            if (recoveredReference != null) {
                                                referenceType = recoveredReference.type
                                                referenceCode = recoveredReference.code
                                            }
                                            rawImported = repository.hasRawArtifact(pending.id)
                                            rawSummary = repository.rawArtifactSummary(pending.id)
                                            val beforeHeights = heights.filter { it.phase == HeightPhase.BEFORE }
                                            val afterHeights = heights.filter { it.phase == HeightPhase.AFTER }
                                            beforeUnit = beforeHeights.firstNotNullOfOrNull { heightUnitFromObservation(it.observation) } ?: beforeUnit
                                            afterUnit = afterHeights.firstNotNullOfOrNull { heightUnitFromObservation(it.observation) } ?: afterUnit
                                            before = beforeHeights.map { "%.4f".format(heightFromMeters(it.valueMeters, beforeUnit)) }.ifEmpty { before }
                                            after = afterHeights.map { "%.4f".format(heightFromMeters(it.valueMeters, afterUnit)) }.ifEmpty { after }
                                            afterRegistered = heights.any { it.phase == HeightPhase.AFTER }
                                            status = "Rastreio recuperado: ${pending.state}"
                                            route = "NEW"
                                            newStep = 6
                                            showHome = false
                                        }
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text("CONTINUAR") }
                            }
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("INÍCIO", style = MaterialTheme.typography.titleLarge, color = fieldBlueDark)
                            Text("Escolha uma operação", color = Color(0xFF52636D))
                            Button(onClick = { startNewTracking() }, modifier = Modifier.fillMaxWidth()) { Text("NOVO RASTREIO") }
                            Button(onClick = {
                                scope.launch {
                                    val pending = repository.findIncompleteOccupations().firstOrNull()
                                    if (pending != null) {
                                        val heights = repository.findHeights(pending.id)
                                        val recoveredProject = repository.findProject(pending.projectId)
                                        val recoveredStation = repository.findStation(pending.stationId)
                                        val recoveredReference = pending.referencePointId?.let { repository.findReferencePoint(it) }
                                        occupation = pending
                                        projectName = recoveredProject?.name.orEmpty()
                                        savedId = recoveredStation?.id
                                        name = recoveredStation?.name.orEmpty()
                                        locality = recoveredStation?.locality.orEmpty()
                                        if (recoveredReference != null) {
                                            referenceType = recoveredReference.type
                                            referenceCode = recoveredReference.code
                                        }
                                        rawImported = repository.hasRawArtifact(pending.id)
                                        rawSummary = repository.rawArtifactSummary(pending.id)
                                        val beforeHeights = heights.filter { it.phase == HeightPhase.BEFORE }
                                        val afterHeights = heights.filter { it.phase == HeightPhase.AFTER }
                                        beforeUnit = beforeHeights.firstNotNullOfOrNull { heightUnitFromObservation(it.observation) } ?: beforeUnit
                                        afterUnit = afterHeights.firstNotNullOfOrNull { heightUnitFromObservation(it.observation) } ?: afterUnit
                                        before = beforeHeights.map { "%.4f".format(heightFromMeters(it.valueMeters, beforeUnit)) }.ifEmpty { before }
                                        after = afterHeights.map { "%.4f".format(heightFromMeters(it.valueMeters, afterUnit)) }.ifEmpty { after }
                                        afterRegistered = heights.any { it.phase == HeightPhase.AFTER }
                                        status = "Rastreio recuperado: ${pending.state}"
                                        route = "NEW"
                                        newStep = 6
                                        showHome = false
                                    }
                                    else status = "Nenhum rastreio incompleto encontrado"
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("CONTINUAR RASTREIO") }
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("DADOS DE CAMPO", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            Button(onClick = { route = "PROJECTS"; showHome = false }, modifier = Modifier.fillMaxWidth()) { Text("PROJETOS") }
                            Button(onClick = { route = "STATIONS"; showHome = false }, modifier = Modifier.fillMaxWidth()) { Text("BANCO DE ESTAÇÕES") }
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("APLICATIVO", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            Button(onClick = { route = "CONNECTION"; showHome = false }, modifier = Modifier.fillMaxWidth()) { Text("CONEXÃO DE BANCADA") }
                            Button(onClick = { route = "SETTINGS"; showHome = false }, modifier = Modifier.fillMaxWidth()) { Text("CONFIGURAÇÕES") }
                            Button(onClick = { route = "ABOUT"; showHome = false }, modifier = Modifier.fillMaxWidth()) { Text("SOBRE") }
                        }
                    }
                } else if (route == "PROJECTS") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("PROJETOS", color = Color.White, style = MaterialTheme.typography.headlineSmall); Text("Comissões e trabalhos salvos neste aparelho", color = Color(0xFFD5EAF5)) } }
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                    OutlinedTextField(name, { name = it }, label = { Text("Nome do projeto/comissão") })
                    Button(enabled = name.isNotBlank(), onClick = { scope.launch { repository.save(br.f21campo.domain.Project(EntityId.new(), name.trim(), Instant.now())); projects = repository.findAllProjects(); status = "Projeto salvo localmente" } }) { Text("SALVAR PROJETO") }
                    Text("Projetos salvos neste aparelho", style = MaterialTheme.typography.titleMedium)
                    if (projects.isEmpty()) Text("Nenhum projeto salvo ainda")
                    projects.forEach { project ->
                        Button(onClick = { name = project.name; status = "Projeto selecionado: ${project.name}" }) { Text(project.name) }
                    }
                    Text(status)
                } else if (route == "STATIONS") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("BANCO DE ESTAÇÕES", color = Color.White, style = MaterialTheme.typography.headlineSmall); Text("Estações e localidades salvas neste aparelho", color = Color(0xFFD5EAF5)) } }
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                    OutlinedTextField(name, { name = it }, label = { Text("Nome da estação") })
                    OutlinedTextField(locality, { locality = it }, label = { Text("Localidade") })
                    Button(enabled = name.isNotBlank() && locality.isNotBlank(), onClick = { val id = savedId ?: EntityId.new().also { savedId = it }; scope.launch { repository.save(Station(id, name.trim(), locality.trim(), null, Instant.now())); stations = repository.findAllStations(); status = "Estação salva localmente — ID preservado" } }) { Text("SALVAR ESTAÇÃO") }
                    Text("Estações salvas neste aparelho", style = MaterialTheme.typography.titleMedium)
                    if (stations.isEmpty()) Text("Nenhuma estação salva ainda")
                    stations.forEach { station ->
                        Button(onClick = { savedId = station.id; name = station.name; locality = station.locality.orEmpty(); scope.launch { stationHistory = repository.findOccupationsByStation(station.id) }; status = "Estação selecionada: ${station.name}" }) { Text("${station.name} · ${station.locality ?: "sem localidade"}") }
                    }
                    if (savedId != null) {
                        Text("HISTÓRICO DE RASTREIOS", style = MaterialTheme.typography.titleMedium)
                        if (stationHistory.isEmpty()) Text("Nenhum rastreio registrado para esta estação")
                        stationHistory.forEach { item ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Estado: ${item.state}", style = MaterialTheme.typography.titleSmall)
                                    Text("Início: ${item.confirmedStart ?: item.plannedStart ?: "não iniciado"}")
                                    Text("Fim: ${item.confirmedStop ?: "em aberto"}")
                                }
                            }
                        }
                    }
                    Text(status)
                } else if (route == "SETTINGS") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) { Text("CONFIGURAÇÕES", color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp)) }
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                    Text("Modo: ${BuildConfig.BUILD_MODE}")
                    Text("O aplicativo funciona offline e registra a origem manual dos equipamentos.")
                    Button(onClick = { route = "CONNECTION" }, modifier = Modifier.fillMaxWidth()) { Text("CONEXÃO DE BANCADA") }
                } else if (route == "CONNECTION") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("CONEXÃO DE BANCADA", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            Text("Perfil neutro para testes reais de receptor", color = Color(0xFFD5EAF5))
                        }
                    }
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
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
                    }
                    OutlinedTextField(connectionNotes, { connectionNotes = it }, label = { Text("Observações de bancada") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        val endpoint = when (connectionTransport) {
                            ReceiverTransportType.WIFI_TCP, ReceiverTransportType.SERIAL -> listOf(connectionHost.trim(), connectionPort.trim()).filter { it.isNotBlank() }.joinToString(":")
                            ReceiverTransportType.BLUETOOTH -> listOf(bluetoothName.trim(), bluetoothMac.trim()).filter { it.isNotBlank() }.joinToString(" · ")
                            ReceiverTransportType.UNKNOWN -> ""
                        }
                        val result = manualConnection.connect(endpoint)
                        connectionStatus = "${result.state}: ${result.message ?: "sem mensagem"}"
                        status = "Perfil de conexão registrado para bancada"
                    }, modifier = Modifier.fillMaxWidth()) { Text("REGISTRAR PERFIL / TESTAR LIMITE") }
                    Button(onClick = {
                        val result = manualConnection.disconnect()
                        connectionStatus = "${result.state}: ${result.message ?: "sem mensagem"}"
                    }, modifier = Modifier.fillMaxWidth()) { Text("DESCONECTAR") }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Estado", style = MaterialTheme.typography.titleMedium, color = fieldBlueDark)
                            Text(connectionStatus)
                            Text("Sem comandos Spectra automáticos nesta versão. Porta, framing, handshake e respostas dependem de validação física.")
                        }
                    }
                } else if (route == "ABOUT") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) { Text("SOBRE", color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp)) }
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                    Text("F-21 Campo")
                    Text("Versão ${BuildConfig.VERSION_NAME}")
                    Text("Fluxo manual de rastreio, persistência e proveniência.")
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 1) {
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("INÍCIO") }
                    StepHeader(1, "PROJETO", "Identifique a comissão ou trabalho de campo.", fieldBlueDark)
                    OutlinedTextField(projectName, { projectName = it }, label = { Text("Nome do projeto/LH") })
                    Button(onClick = { if (projectName.isBlank()) status = "Informe o nome do projeto/LH" else { scope.launch { val project = br.f21campo.domain.Project(EntityId.new(), projectName.trim(), Instant.now()); repository.save(project); occupation = occupation.copy(projectId = project.id); status = "Projeto salvo localmente"; newStep = 2 } } }) { Text("SALVAR E AVANÇAR") }
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 2) {
                    Button(onClick = { newStep = 1 }) { Text("VOLTAR") }
                    StepHeader(2, "ESTAÇÃO", "Informe a estação desta ocasião de campo.", fieldBlueDark)
                    OutlinedTextField(name, { name = it }, label = { Text("Nome da estação") })
                    OutlinedTextField(locality, { locality = it }, label = { Text("Localidade") })
                    Button(onClick = { if (name.isBlank() || locality.isBlank()) status = "Informe o nome da estação e a localidade" else { val id = savedId ?: EntityId.new().also { savedId = it }; scope.launch { repository.save(Station(id, name.trim(), locality.trim(), null, Instant.now())); status = "Estação salva — ID preservado"; newStep = 3 } } }) { Text("SALVAR E AVANÇAR") }
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 3) {
                    Button(onClick = { newStep = 2 }) { Text("VOLTAR") }
                    StepHeader(3, "REFERÊNCIA", "Escolha RN, MT ou PA e registre o código.", fieldBlueDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ReferencePointType.entries.forEach { type -> Button(onClick = { referenceType = type }, enabled = referenceType != type) { Text(type.name) } } }
                    OutlinedTextField(referenceCode, { referenceCode = it }, label = { Text("Código da referência") })
                    Button(onClick = { val stationId = savedId; if (stationId == null || referenceCode.isBlank()) status = "Informe estação e código da referência" else { val point = ReferencePoint(EntityId.new(), stationId, referenceType, referenceCode.trim()); scope.launch { repository.save(point); occupation = occupation.copy(stationId = stationId, referencePointId = point.id); status = "Referência ${point.type}: ${point.code} registrada"; newStep = 4 } } }) { Text("SALVAR E AVANÇAR") }
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 4) {
                    Button(onClick = { newStep = 3 }) { Text("VOLTAR") }
                    StepHeader(4, "EQUIPAMENTO", "Informe receptor e antena usados nesta ocupação.", fieldBlueDark)
                    OutlinedTextField(receiverModel, { receiverModel = it }, label = { Text("Modelo do receptor") })
                    OutlinedTextField(receiverManufacturer, { receiverManufacturer = it }, label = { Text("Fabricante do receptor") })
                    OutlinedTextField(receiverSerial, { receiverSerial = it }, label = { Text("Nº de série do receptor") })
                    OutlinedTextField(antennaModel, { antennaModel = it }, label = { Text("Modelo da antena") })
                    OutlinedTextField(antennaManufacturer, { antennaManufacturer = it }, label = { Text("Fabricante da antena") })
                    OutlinedTextField(antennaSerial, { antennaSerial = it }, label = { Text("Nº de série da antena") })
                    Button(onClick = { val receiver = Receiver(EntityId.new(), receiverManufacturer.ifBlank { null }, receiverModel.ifBlank { "manual" }, receiverSerial.ifBlank { null }); val antenna = Antenna(EntityId.new(), antennaManufacturer.ifBlank { null }, antennaModel.ifBlank { "manual" }, antennaSerial.ifBlank { null }); val result = ManualEquipment.attachSnapshot(occupation, receiver, antenna); if (result is DomainResult.Success) { occupation = result.value; status = "Equipamento associado"; newStep = 5 } }) { Text("ASSOCIAR E AVANÇAR") }
                    Button(onClick = { route = "CONNECTION" }, modifier = Modifier.fillMaxWidth()) { Text("CONFIGURAR CONEXÃO DE BANCADA") }
                    Text("Origem: informado pelo operador")
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 5) {
                    Button(onClick = { newStep = 4 }) { Text("VOLTAR") }
                    StepHeader(5, "ALTURAS BEFORE", "Registre pelo menos uma leitura antes de iniciar.", fieldBlueDark)
                    Text("UNIDADE DA ALTURA", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("mm", "cm", "m").forEach { unit -> Button(onClick = { beforeUnit = unit }, enabled = beforeUnit != unit) { Text(unit) } } }
                    Text("Selecionada: ${beforeUnit ?: "nenhuma — selecione uma unidade"}")
                    heightUnitHint(before, beforeUnit)?.let { Text(it, color = fieldBlueDark) }
                    before.forEachIndexed { index, value -> OutlinedTextField(value, { v -> before = before.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { beforeUndo = beforeUndo + listOf(before); before = before + "" }) { Text("+ BEFORE") }; Button(enabled = before.size > 1, onClick = { beforeUndo = beforeUndo + listOf(before); before = before.dropLast(1) }) { Text("−") }; Button(enabled = beforeUndo.isNotEmpty(), onClick = { before = beforeUndo.last(); beforeUndo = beforeUndo.dropLast(1) }) { Text("DESFAZER") } }
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
                                values.forEach { value ->
                                    repository.saveHeight(currentOccupation.id, HeightMeasurement(HeightPhase.BEFORE, heightToMeters(value, beforeUnit) ?: value, HeightType.VERTICAL, Instant.now(), "unit=${beforeUnit}"))
                                }
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
                        val missing = readinessMissingItems(
                            projectName = projectName,
                            stationName = name,
                            locality = locality,
                            hasReference = occupation.referencePointId != null,
                            hasEquipment = occupation.equipment != null,
                            hasBeforeHeight = occupation.hasBeforeHeight,
                        )
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
                                status = "READY confirmado — toque em INICIAR"
                            } else {
                                status = "READY bloqueado pela máquina de estados"
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("READY") }
                    Button(enabled = occupation.state == OccupationState.READY, onClick = {
                        val result = OccupationStateMachine.start(occupation, Instant.now())
                        if (result is DomainResult.Success) { trackingTimeAlerted = false; occupation = result.value; status = "Rastreio iniciado" }
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
                        if (result is DomainResult.Success) { trackingTimeAlerted = false; occupation = result.value; status = "Rastreio iniciado" }
                    }, modifier = Modifier.fillMaxWidth()) { Text("INICIAR RASTREIO") }
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.ACTIVE) {
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                    Text("RASTREIO ATIVO", style = MaterialTheme.typography.headlineSmall)
                    Text("ETAPA 6/7 · CAMPO", style = MaterialTheme.typography.titleMedium)
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
                    OutlinedTextField(event, { event = it }, label = { Text("Evento de campo") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        if (event.isNotBlank()) {
                            scope.launch {
                                repository.save(OccupationEvent(EntityId.new(), occupation.id, Instant.now(), OccupationEventCategory.NOTE, EventSeverity.INFO, event.trim()))
                                fieldEvents = repository.findEvents(occupation.id)
                                event = ""
                                status = "Evento registrado"
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("REGISTRAR EVENTO") }
                    if (fieldEvents.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("EVENTOS REGISTRADOS (${fieldEvents.size})", style = MaterialTheme.typography.labelLarge, color = fieldBlueDark)
                                fieldEvents.takeLast(3).forEach { savedEvent ->
                                    Text("${savedEvent.at}: ${savedEvent.description}")
                                }
                            }
                        }
                    }
                    Button(onClick = {
                        val result = OccupationStateMachine.stop(occupation, Instant.now())
                        if (result is DomainResult.Success) { occupation = result.value; status = "Rastreio parado — finalização" }
                    }, modifier = Modifier.fillMaxWidth()) { Text("PARAR RASTREIO") }
                    Text(status)
                } else if (occupation.state in setOf(OccupationState.STOPPED, OccupationState.COLLECTED, OccupationState.VALIDATED)) {
                    Text("FINALIZAÇÃO", style = MaterialTheme.typography.headlineSmall)
                    Text("Referência: ${referenceCode.ifBlank { "ocupação recuperada" }}")
                    Text("Status: ${occupation.state}")
                    Text("Registre as alturas AFTER e anexe o arquivo original copiado do receptor.")
                    Text("Alturas AFTER")
                    Text("Unidade AFTER: ${afterUnit ?: "não selecionada"}")
                    heightUnitHint(after, afterUnit)?.let { Text(it, color = fieldBlueDark) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("mm", "cm", "m").forEach { unit ->
                            Button(onClick = { afterUnit = unit }, enabled = afterUnit != unit) { Text(unit) }
                        }
                    }
                    after.forEachIndexed { index, value -> OutlinedTextField(value, { v -> after = after.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { afterUndo = afterUndo + listOf(after); after = after + "" }) { Text("+ AFTER") }
                        Button(enabled = after.size > 1, onClick = { afterUndo = afterUndo + listOf(after); after = after.dropLast(1) }) { Text("−") }
                        Button(enabled = afterUndo.isNotEmpty(), onClick = { after = afterUndo.last(); afterUndo = afterUndo.dropLast(1) }) { Text("DESFAZER") }
                    }
                    Button(onClick = {
                        val values = after.mapNotNull(String::toDoubleOrNull).filter { it.isFinite() }
                        if (afterUnit == null) status = heightUnitHint(after, afterUnit) ?: "Confirme a unidade da altura AFTER"
                        else if (values.isEmpty()) status = "Informe ao menos uma altura AFTER válida"
                        else scope.launch {
                            values.forEach { value ->
                                repository.saveHeight(occupation.id, HeightMeasurement(HeightPhase.AFTER, heightToMeters(value, afterUnit) ?: value, HeightType.VERTICAL, Instant.now(), "unit=${afterUnit}"))
                            }
                            afterRegistered = true
                            status = "${values.size} altura(s) AFTER registrada(s) em ${afterUnit}"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("REGISTRAR AFTER") }
                    Button(onClick = { rawPicker.launch("*/*") }) { Text("ANEXAR RAW DO CELULAR") }
                    Text(if (rawImported) "RAW_RECEIVER: ${rawSummary ?: "associado com SHA-256"}" else "RAW pendente: selecione o arquivo já salvo no celular")
                    Text(if (afterRegistered) "Altura AFTER registrada" else "Altura AFTER pendente")
                    Button(enabled = occupation.state == OccupationState.STOPPED && rawImported && afterRegistered, onClick = {
                        val result = OccupationStateMachine.collectWithEvidence(occupation, hasRawEvidence = rawImported, hasAfterHeight = afterRegistered)
                        if (result is DomainResult.Success) occupation = result.value
                    }) { Text("FINALIZAR COLETA") }
                    Button(enabled = occupation.state == OccupationState.COLLECTED, onClick = { val result = OccupationStateMachine.validate(occupation); if (result is DomainResult.Success) occupation = result.value }) { Text("VALIDAR RASTREIO") }
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
                    OutlinedTextField(event, { event = it }, label = { Text("Evento de campo") })
                    Button(onClick = { if (event.isNotBlank()) scope.launch { repository.save(OccupationEvent(EntityId.new(), occupation.id, Instant.now(), OccupationEventCategory.NOTE, EventSeverity.INFO, event.trim())); event = ""; status = "Evento registrado" } }) { Text("REGISTRAR EVENTO") }
                    Button(onClick = { val result = OccupationStateMachine.stop(occupation, Instant.now()); if (result is DomainResult.Success) { occupation = result.value; status = "Rastreio parado — finalização" } }) { Text("PARAR RASTREIO") }
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
                    val receiver = Receiver(EntityId.new(), manufacturer = receiverManufacturer.ifBlank { null }, model = receiverModel.ifBlank { "manual" }, serialNumber = receiverSerial.ifBlank { null })
                    val antenna = Antenna(EntityId.new(), manufacturer = antennaManufacturer.ifBlank { null }, model = antennaModel.ifBlank { "manual" }, serialNumber = antennaSerial.ifBlank { null })
                    val result = ManualEquipment.attachSnapshot(occupation, receiver, antenna)
                    if (result is DomainResult.Success) { occupation = result.value; status = "Equipamento associado" }
                }) { Text("Associar receptor e antena") }
                Button(enabled = occupation.state == OccupationState.DRAFT, onClick = {
                    val result = OccupationStateMachine.ready(occupation)
                    if (result is DomainResult.Success) { occupation = result.value; status = "READY confirmado — INICIAR está disponível" }
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
                Button(enabled = occupation.state == OccupationState.READY, onClick = { val result = OccupationStateMachine.start(occupation, Instant.now()); if (result is DomainResult.Success) { trackingTimeAlerted = false; occupation = result.value; status = "Rastreio iniciado" } }) { Text("INICIAR") }
                Button(enabled = occupation.state == OccupationState.ACTIVE, onClick = { val result = OccupationStateMachine.stop(occupation, Instant.now()); if (result is DomainResult.Success) { occupation = result.value; status = "Rastreio parado — etapa de finalização" } }) { Text("PARAR") }
                Button(enabled = occupation.state == OccupationState.STOPPED && rawImported && afterRegistered, onClick = {
                    val result = OccupationStateMachine.collectWithEvidence(occupation, hasRawEvidence = rawImported, hasAfterHeight = afterRegistered)
                    if (result is DomainResult.Success) { occupation = result.value; status = "Coleta finalizada — pronta para resumo" }
                }) { Text("FINALIZAR COLETA") }
                Button(enabled = occupation.state == OccupationState.COLLECTED, onClick = {
                    val result = OccupationStateMachine.validate(occupation)
                    if (result is DomainResult.Success) { occupation = result.value; status = "Rastreio validado" }
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
                            before.mapNotNull { it.toDoubleOrNull() }.filter { it.isFinite() }.forEach { value -> repository.saveHeight(occupation.id, HeightMeasurement(HeightPhase.BEFORE, heightToMeters(value, beforeUnit) ?: value, HeightType.VERTICAL, Instant.now(), "unit=${beforeUnit}")) }
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
                after.forEachIndexed { index, value -> OutlinedTextField(value, { v -> after = after.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { afterUndo = afterUndo + listOf(after); after = after + "" }) { Text("+ AFTER") }
                    Button(enabled = after.size > 1, onClick = { afterUndo = afterUndo + listOf(after); after = after.dropLast(1) }) { Text("−") }
                    Button(enabled = afterUndo.isNotEmpty(), onClick = { after = afterUndo.last(); afterUndo = afterUndo.dropLast(1) }) { Text("DESFAZER") }
                }
                Button(onClick = {
                    val values = after.mapNotNull { it.toDoubleOrNull() }.filter { it.isFinite() }
                    if (afterUnit == null) status = heightUnitHint(after, afterUnit) ?: "Confirme a unidade da altura AFTER" else if (values.isEmpty()) status = "Informe ao menos uma altura AFTER válida" else scope.launch {
                        values.forEach { value -> repository.saveHeight(occupation.id, HeightMeasurement(HeightPhase.AFTER, heightToMeters(value, afterUnit) ?: value, HeightType.VERTICAL, Instant.now(), "unit=${afterUnit}")) }
                        status = "${values.size} altura(s) AFTER registrada(s)"
                    }
                }) { Text("Registrar alturas AFTER") }
                OutlinedTextField(event, { event = it }, label = { Text("Evento de campo") })
                Button(onClick = {
                    if (event.isNotBlank()) {
                        scope.launch {
                            repository.save(OccupationEvent(EntityId.new(), occupation.id, Instant.now(), OccupationEventCategory.NOTE, EventSeverity.INFO, event.trim()))
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

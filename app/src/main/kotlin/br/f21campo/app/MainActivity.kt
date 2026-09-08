package br.f21campo.app

import android.os.Bundle
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
    var beforeUnit by remember { mutableStateOf<String?>(null) }
    var afterUnit by remember { mutableStateOf<String?>(null) }
    var event by remember { mutableStateOf("") }
    var referenceCode by remember { mutableStateOf("") }
    var referenceType by remember { mutableStateOf(ReferencePointType.RN) }
    var showHome by remember { mutableStateOf(true) }
    var route by remember { mutableStateOf("HOME") }
    var newStep by remember { mutableStateOf(1) }
    var projects by remember { mutableStateOf(emptyList<br.f21campo.domain.Project>()) }
    var stations by remember { mutableStateOf(emptyList<Station>()) }
    var stationHistory by remember { mutableStateOf(emptyList<Occupation>()) }
    var rawImported by remember { mutableStateOf(false) }
    var durationMinutes by remember { mutableStateOf("") }
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
                status = "Bruto RAW_RECEIVER importado: ${stored.sha256.take(12)}…"
            }
        }
    }
    LaunchedEffect(occupation) { repository.save(occupation) }
    LaunchedEffect(before) {
        val firstValidBefore = before.mapNotNull { it.toDoubleOrNull() }.firstOrNull { it.isFinite() }?.let { heightToMeters(it, beforeUnit) }
        if (firstValidBefore != null && occupation.beforeHeightMeters != firstValidBefore) {
            occupation = occupation.copy(hasBeforeHeight = true, beforeHeightMeters = firstValidBefore)
        }
    }
    LaunchedEffect(occupation.state, occupation.confirmedStart) {
        while (occupation.state == OccupationState.ACTIVE) {
            nowEpochMillis = System.currentTimeMillis()
            delay(1000)
        }
    }
    LaunchedEffect(route) {
        if (route == "PROJECTS") projects = repository.findAllProjects()
        if (route == "STATIONS") stations = repository.findAllStations()
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
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("INÍCIO", style = MaterialTheme.typography.titleLarge, color = fieldBlueDark)
                            Text("Escolha uma operação", color = Color(0xFF52636D))
                            Button(onClick = { route = "NEW"; newStep = 1; showHome = false; status = "Etapa 1/7 — Projeto" }, modifier = Modifier.fillMaxWidth()) { Text("NOVO RASTREIO") }
                            Button(onClick = {
                                scope.launch {
                                    val pending = repository.findIncompleteOccupations().firstOrNull()
                                    if (pending != null) { occupation = pending; rawImported = repository.hasRawArtifact(pending.id); status = "Rastreio recuperado: ${pending.state}"; route = "NEW"; newStep = 6; showHome = false }
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
                } else if (route == "ABOUT") {
                    Card(colors = CardDefaults.cardColors(containerColor = fieldBlueDark), modifier = Modifier.fillMaxWidth()) { Text("SOBRE", color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp)) }
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("← INÍCIO") }
                    Text("F-21 Campo")
                    Text("Versão ${BuildConfig.VERSION_NAME}")
                    Text("Fluxo manual de rastreio, persistência e proveniência.")
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 1) {
                    Button(onClick = { route = "HOME"; showHome = true }) { Text("INÍCIO") }
                    Text("NOVO RASTREIO", style = MaterialTheme.typography.headlineSmall)
                    Text("ETAPA 1/7 · PROJETO", style = MaterialTheme.typography.titleMedium)
                    Text("Identifique a comissão ou trabalho de campo.")
                    OutlinedTextField(projectName, { projectName = it }, label = { Text("Nome do projeto/LH") })
                    Button(onClick = { if (projectName.isBlank()) status = "Informe o nome do projeto/LH" else { scope.launch { val project = br.f21campo.domain.Project(EntityId.new(), projectName.trim(), Instant.now()); repository.save(project); occupation = occupation.copy(projectId = project.id); status = "Projeto salvo localmente"; newStep = 2 } } }) { Text("SALVAR E AVANÇAR") }
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 2) {
                    Button(onClick = { newStep = 1 }) { Text("VOLTAR") }
                    Text("NOVO RASTREIO", style = MaterialTheme.typography.headlineSmall)
                    Text("ETAPA 2/7 · ESTAÇÃO", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(name, { name = it }, label = { Text("Nome da estação") })
                    OutlinedTextField(locality, { locality = it }, label = { Text("Localidade") })
                    Button(onClick = { if (name.isBlank()) status = "Informe o nome da estação" else { val id = savedId ?: EntityId.new().also { savedId = it }; scope.launch { repository.save(Station(id, name.trim(), locality.ifBlank { null }, null, Instant.now())); status = "Estação salva — ID preservado"; newStep = 3 } } }) { Text("SALVAR E AVANÇAR") }
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 3) {
                    Button(onClick = { newStep = 2 }) { Text("VOLTAR") }
                    Text("NOVO RASTREIO", style = MaterialTheme.typography.headlineSmall)
                    Text("ETAPA 3/7 · REFERÊNCIA", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ReferencePointType.entries.forEach { type -> Button(onClick = { referenceType = type }, enabled = referenceType != type) { Text(type.name) } } }
                    OutlinedTextField(referenceCode, { referenceCode = it }, label = { Text("Código da referência") })
                    Button(onClick = { val stationId = savedId; if (stationId == null || referenceCode.isBlank()) status = "Informe estação e código da referência" else { val point = ReferencePoint(EntityId.new(), stationId, referenceType, referenceCode.trim()); scope.launch { repository.save(point); occupation = occupation.copy(stationId = stationId, referencePointId = point.id); status = "Referência ${point.type}: ${point.code} registrada"; newStep = 4 } } }) { Text("SALVAR E AVANÇAR") }
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 4) {
                    Button(onClick = { newStep = 3 }) { Text("VOLTAR") }
                    Text("NOVO RASTREIO", style = MaterialTheme.typography.headlineSmall)
                    Text("ETAPA 4/7 · EQUIPAMENTO", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(receiverModel, { receiverModel = it }, label = { Text("Modelo do receptor") })
                    OutlinedTextField(receiverManufacturer, { receiverManufacturer = it }, label = { Text("Fabricante do receptor") })
                    OutlinedTextField(receiverSerial, { receiverSerial = it }, label = { Text("Nº de série do receptor") })
                    OutlinedTextField(antennaModel, { antennaModel = it }, label = { Text("Modelo da antena") })
                    OutlinedTextField(antennaManufacturer, { antennaManufacturer = it }, label = { Text("Fabricante da antena") })
                    OutlinedTextField(antennaSerial, { antennaSerial = it }, label = { Text("Nº de série da antena") })
                    Button(onClick = { val receiver = Receiver(EntityId.new(), receiverManufacturer.ifBlank { null }, receiverModel.ifBlank { "manual" }, receiverSerial.ifBlank { null }); val antenna = Antenna(EntityId.new(), antennaManufacturer.ifBlank { null }, antennaModel.ifBlank { "manual" }, antennaSerial.ifBlank { null }); val result = ManualEquipment.attachSnapshot(occupation, receiver, antenna); if (result is DomainResult.Success) { occupation = result.value; status = "Equipamento associado"; newStep = 5 } }) { Text("ASSOCIAR E AVANÇAR") }
                    Text("Origem: informado pelo operador")
                    Text(status)
                } else if (route == "NEW" && occupation.state == OccupationState.DRAFT && newStep == 5) {
                    Button(onClick = { newStep = 4 }) { Text("VOLTAR") }
                    Text("NOVO RASTREIO", style = MaterialTheme.typography.headlineSmall)
                    Text("ETAPA 5/7 · ALTURAS BEFORE", style = MaterialTheme.typography.titleMedium)
                    Text("Registre pelo menos uma leitura antes de iniciar.")
                    Text("UNIDADE DA ALTURA", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("mm", "cm", "m").forEach { unit -> Button(onClick = { beforeUnit = unit }, enabled = beforeUnit != unit) { Text(unit) } } }
                    Text("Selecionada: ${beforeUnit ?: "nenhuma — selecione uma unidade"}")
                    before.forEachIndexed { index, value -> OutlinedTextField(value, { v -> before = before.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { before = before + "" }) { Text("+ BEFORE") }; Button(enabled = before.size > 1, onClick = { before = before.dropLast(1) }) { Text("−") } }
                    Button(onClick = { val valid = before.mapNotNull(String::toDoubleOrNull).firstOrNull { it.isFinite() }?.let { heightToMeters(it, beforeUnit) }; if (beforeUnit == null) status = "Confirme a unidade da altura BEFORE" else if (valid == null) status = "Informe uma altura BEFORE válida" else { occupation = occupation.copy(hasBeforeHeight = true, beforeHeightMeters = valid); status = "BEFORE registrado em ${beforeUnit} — pronto para READY"; newStep = 6 } }) { Text("REGISTRAR BEFORE E AVANÇAR") }
                    Text(status)
                } else if (occupation.state in setOf(OccupationState.STOPPED, OccupationState.COLLECTED, OccupationState.VALIDATED)) {
                    Text("FINALIZAÇÃO", style = MaterialTheme.typography.headlineSmall)
                    Text("Referência: ${referenceCode.ifBlank { "ocupação recuperada" }}")
                    Text("Status: ${occupation.state}")
                    Text("Registre as alturas AFTER e importe o RAW antes de finalizar.")
                    Text("Alturas AFTER")
                    after.forEachIndexed { index, value -> OutlinedTextField(value, { v -> after = after.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { after = after + "" }) { Text("+ AFTER") }
                        Button(enabled = after.size > 1, onClick = { after = after.dropLast(1) }) { Text("−") }
                    }
                    Button(onClick = { rawPicker.launch("*/*") }) { Text("IMPORTAR RAW") }
                    Text(if (rawImported) "RAW importado e associado" else "RAW pendente")
                    Button(enabled = occupation.state == OccupationState.STOPPED && rawImported, onClick = { val result = OccupationStateMachine.collect(occupation, true); if (result is DomainResult.Success) occupation = result.value }) { Text("FINALIZAR COLETA") }
                    Button(enabled = occupation.state == OccupationState.COLLECTED, onClick = { val result = OccupationStateMachine.validate(occupation); if (result is DomainResult.Success) occupation = result.value }) { Text("VALIDAR RASTREIO") }
                    Text("Resumo: início ${occupation.confirmedStart ?: "—"} · fim ${occupation.confirmedStop ?: "—"}")
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
                    event = ""
                    rawImported = false
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
                    else status = when (val error = (result as DomainResult.Failure).error) {
                        is br.f21campo.domain.DomainError.InvalidValue -> "READY bloqueado: ${error.reason}"
                        else -> "READY bloqueado: verifique os dados da ocupação"
                    }
                }) { Text("READY") }
                if (occupation.state == OccupationState.DRAFT) {
                    Text("Para READY: referência ${if (occupation.referencePointId != null) "OK" else "pendente"} · altura BEFORE ${if (occupation.hasBeforeHeight) "OK" else "pendente"}")
                }
                Button(enabled = occupation.state == OccupationState.READY, onClick = { val result = OccupationStateMachine.start(occupation, Instant.now()); if (result is DomainResult.Success) { occupation = result.value; status = "Rastreio iniciado" } }) { Text("INICIAR") }
                Button(enabled = occupation.state == OccupationState.ACTIVE, onClick = { val result = OccupationStateMachine.stop(occupation, Instant.now()); if (result is DomainResult.Success) { occupation = result.value; status = "Rastreio parado — etapa de finalização" } }) { Text("PARAR") }
                Button(enabled = occupation.state == OccupationState.STOPPED && rawImported, onClick = {
                    val result = OccupationStateMachine.collect(occupation, hasRawEvidence = rawImported)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("mm", "cm", "m").forEach { unit -> Button(onClick = { beforeUnit = unit }, enabled = beforeUnit != unit) { Text(unit) } } }
                before.forEachIndexed { index, value -> OutlinedTextField(value, { v -> before = before.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { before = before + "" }) { Text("+ BEFORE") }
                    Button(enabled = before.size > 1, onClick = { before = before.dropLast(1) }) { Text("−") }
                }
                Button(onClick = {
                    val validBefore = before.mapNotNull { it.toDoubleOrNull() }.firstOrNull { it.isFinite() }?.let { heightToMeters(it, beforeUnit) }
                    if (beforeUnit == null) {
                        status = "Confirme a unidade da altura BEFORE"
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("mm", "cm", "m").forEach { unit -> Button(onClick = { afterUnit = unit }, enabled = afterUnit != unit) { Text(unit) } } }
                after.forEachIndexed { index, value -> OutlinedTextField(value, { v -> after = after.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { after = after + "" }) { Text("+ AFTER") }
                    Button(enabled = after.size > 1, onClick = { after = after.dropLast(1) }) { Text("−") }
                }
                Button(onClick = {
                    val values = after.mapNotNull { it.toDoubleOrNull() }.filter { it.isFinite() }
                    if (afterUnit == null) status = "Confirme a unidade da altura AFTER" else if (values.isEmpty()) status = "Informe ao menos uma altura AFTER válida" else scope.launch {
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

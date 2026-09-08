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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
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
import java.time.Instant
import java.io.File
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(applicationContext, F21Database::class.java, "f21.db")
            .addMigrations(Migrations.V1_TO_V2, Migrations.V2_TO_V3, Migrations.V3_TO_V4, Migrations.V4_TO_V5, Migrations.V5_TO_V6, Migrations.V6_TO_V7)
            .build()
        setContent { StationScreen(ProjectStationRepository(database.projectDao(), database.stationDao(), database.referencePointDao(), database.occupationDao(), database.occupationArtifactDao(), database.occupationEventDao())) }
    }
}

@Composable
private fun StationScreen(repository: ProjectStationRepository) {
    val context = LocalContext.current
    val rawStore = remember { RawFileStore(File(context.filesDir, "raw")) }
    var name by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    var savedId by remember { mutableStateOf<EntityId?>(null) }
    var status by remember { mutableStateOf("Banco de Estações") }
    var occupation by remember { mutableStateOf(Occupation(EntityId.new(), EntityId.new(), EntityId.new())) }
    var receiverModel by remember { mutableStateOf("") }
    var antennaModel by remember { mutableStateOf("") }
    var before by remember { mutableStateOf(listOf("", "", "")) }
    var after by remember { mutableStateOf(listOf("", "", "")) }
    var event by remember { mutableStateOf("") }
    var referenceCode by remember { mutableStateOf("") }
    var referenceType by remember { mutableStateOf(ReferencePointType.RN) }
    var showHome by remember { mutableStateOf(true) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val rawPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val imported = File.createTempFile("import-", ".part", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input -> imported.outputStream().use { output -> input.copyTo(output) } }
            val stored = rawStore.import(imported)
            imported.delete()
            scope.launch {
                repository.saveRawArtifact(occupation.id, stored.path.absolutePath, stored.sizeBytes, stored.sha256)
                status = "Bruto RAW_RECEIVER importado: ${stored.sha256.take(12)}…"
            }
        }
    }
    LaunchedEffect(occupation) { repository.save(occupation) }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showHome) {
                    Spacer(Modifier.height(24.dp))
                    Text("F-21 Campo", style = MaterialTheme.typography.headlineMedium)
                    Text("Versão ${BuildConfig.VERSION_NAME}")
                    Text("Modo: ${BuildConfig.BUILD_MODE}")
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showHome = false }) { Text("Novo rastreio") }
                    Button(onClick = {
                        scope.launch {
                            val pending = repository.findIncompleteOccupations().firstOrNull()
                            if (pending != null) {
                                occupation = pending
                                status = "Rastreio recuperado: ${pending.state}"
                                showHome = false
                            } else {
                                status = "Nenhum rastreio incompleto encontrado"
                            }
                        }
                    }) { Text("Continuar rastreio") }
                    Button(onClick = { showHome = false; status = "Projetos — seleção em preparação" }) { Text("Projetos") }
                    Button(onClick = { showHome = false; status = "Banco de Estações" }) { Text("Banco de Estações") }
                    Button(onClick = { status = "Configurações — em preparação" }) { Text("Configurações") }
                    Button(onClick = { status = "F-21 Campo ${BuildConfig.VERSION_NAME}" }) { Text("Sobre") }
                } else {
                Button(onClick = { showHome = true }) { Text("Início") }
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
                Button(onClick = {
                    occupation = Occupation(EntityId.new(), savedId ?: EntityId.new(), EntityId.new())
                    receiverModel = ""
                    antennaModel = ""
                    before = listOf("", "", "")
                    after = listOf("", "", "")
                    event = ""
                    status = "Nova ocupação criada"
                }) { Text("Nova ocupação") }
                OutlinedTextField(receiverModel, { receiverModel = it }, label = { Text("Modelo do receptor") })
                OutlinedTextField(antennaModel, { antennaModel = it }, label = { Text("Modelo da antena") })
                Button(onClick = {
                    val receiver = Receiver(EntityId.new(), model = receiverModel.ifBlank { "manual" })
                    val antenna = Antenna(EntityId.new(), model = antennaModel.ifBlank { "manual" })
                    val result = ManualEquipment.attachSnapshot(occupation, receiver, antenna)
                    if (result is DomainResult.Success) { occupation = result.value; status = "Equipamento associado" }
                }) { Text("Associar receptor e antena") }
                Button(enabled = occupation.state == OccupationState.DRAFT, onClick = {
                    val result = OccupationStateMachine.ready(occupation)
                    if (result is DomainResult.Success) occupation = result.value
                    else status = "Registre a referência e ao menos uma altura BEFORE"
                }) { Text("READY") }
                Button(enabled = occupation.state == OccupationState.READY, onClick = { val result = OccupationStateMachine.start(occupation, Instant.now()); if (result is DomainResult.Success) occupation = result.value }) { Text("INICIAR") }
                Button(enabled = occupation.state == OccupationState.ACTIVE, onClick = { val result = OccupationStateMachine.stop(occupation, Instant.now()); if (result is DomainResult.Success) occupation = result.value }) { Text("PARAR") }
                Text("Alturas BEFORE")
                before.forEachIndexed { index, value -> OutlinedTextField(value, { v -> before = before.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }) }
                Button(onClick = {
                    val validBefore = before.mapNotNull { it.toDoubleOrNull() }.firstOrNull { it.isFinite() }
                    if (validBefore != null) {
                        occupation = occupation.copy(hasBeforeHeight = true, beforeHeightMeters = validBefore)
                        status = "Altura BEFORE registrada"
                    } else {
                        status = "Informe ao menos uma altura BEFORE válida"
                    }
                }) { Text("Registrar altura BEFORE") }
                Text("Alturas AFTER")
                after.forEachIndexed { index, value -> OutlinedTextField(value, { v -> after = after.toMutableList().also { it[index] = v } }, label = { Text("Leitura ${index + 1}") }) }
                Button(onClick = {
                    runCatching {
                        val b = before.map(String::toDouble)
                        val a = after.map(String::toDouble)
                        HeightSet(HeightObservation(HeightPhase.BEFORE, b[0], b[1], b[2], HeightType.VERTICAL), HeightObservation(HeightPhase.AFTER, a[0], a[1], a[2], HeightType.VERTICAL))
                    }.onSuccess { status = "Seis alturas registradas" }
                }) { Text("Registrar alturas informadas") }
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

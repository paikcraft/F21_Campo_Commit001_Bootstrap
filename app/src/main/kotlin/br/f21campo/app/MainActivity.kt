package br.f21campo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.room.Room
import br.f21campo.data.F21Database
import br.f21campo.data.ProjectStationRepository
import br.f21campo.domain.EntityId
import br.f21campo.domain.Station
import java.time.Instant
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(applicationContext, F21Database::class.java, "f21.db").build()
        setContent { StationScreen(ProjectStationRepository(database.projectDao(), database.stationDao())) }
    }
}

@Composable
private fun StationScreen(repository: ProjectStationRepository) {
    var name by remember { mutableStateOf("") }
    var locality by remember { mutableStateOf("") }
    var savedId by remember { mutableStateOf<EntityId?>(null) }
    var status by remember { mutableStateOf("Banco de Estações") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
            }
        }
    }
}

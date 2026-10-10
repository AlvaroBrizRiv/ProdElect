package com.prorganics.prodelect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prorganics.prodelect.data.repository.FirebaseRepository
import com.prorganics.prodelect.ui.theme.ProdElectTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // Ya no es necesario instanciar los repositorios manualmente.
    // Compose se encargará de inyectar el ViewModel usando hiltViewModel()
    // en los @Composables correspondientes.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProdElectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                        Text(text = "Dagger-Hilt Configurado.\nRevisa el Logcat o Firestore Console.")
                    }

                    // Por ahora dejamos comentada esta prueba de escritura 
                    // ya que migramos FirebaseRepository a ProductViewModel mediante Inyección de Dependencias
                    /* 
                    LaunchedEffect(Unit) {
                        launch(Dispatchers.IO) {
                            // firebaseRepository.agregarProductoDePrueba()
                        }
                    } 
                    */
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ProdElectTheme {
        Greeting("Android")
    }
}
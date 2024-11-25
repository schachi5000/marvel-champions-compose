import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import net.schacher.mcc.shared.datasource.database.DatabaseDao
import net.schacher.mcc.shared.datasource.database.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext

@Composable
fun MainView(
    onQuitApp: (() -> Unit) = {},
    onLoginClicked: ((LoginBridge) -> Unit),
) {
    val context = LocalContext.current
    App(
        databaseDao = DatabaseDao(DatabaseDriverFactory(LocalContext.current)),
        onKoinStart = { androidContext(context) },
        onQuitApp = onQuitApp,
        onLoginClicked = onLoginClicked,
    )
}
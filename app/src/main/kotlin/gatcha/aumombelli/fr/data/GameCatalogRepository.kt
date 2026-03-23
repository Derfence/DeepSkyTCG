package gatcha.aumombelli.fr.data

import android.content.Context
import gatcha.aumombelli.fr.model.CardDefinition
import gatcha.aumombelli.fr.model.ExtensionDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class GameCatalogRepository(
    private val context: Context,
) : CatalogGateway {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun loadExtensions(): List<ExtensionDefinition> = withContext(Dispatchers.IO) {
        context.assets.open("catalog/extensions.json").bufferedReader().use { reader ->
            json.decodeFromString<List<ExtensionDefinition>>(reader.readText()).sortedBy { it.id }
        }
    }

    override suspend fun loadCards(): List<CardDefinition> = withContext(Dispatchers.IO) {
        context.assets.open("catalog/cards.json").bufferedReader().use { reader ->
            json.decodeFromString<List<CardDefinition>>(reader.readText()).sortedBy { it.id }
        }
    }
}

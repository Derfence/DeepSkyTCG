package fr.aumombelli.dstcg.data

import android.content.Context
import fr.aumombelli.dstcg.model.AstronomyDetails
import fr.aumombelli.dstcg.model.CardDefinition
import fr.aumombelli.dstcg.model.EquipmentCardDefinition
import fr.aumombelli.dstcg.model.EquipmentSettingsDefinition
import fr.aumombelli.dstcg.model.ExtensionDefinition
import fr.aumombelli.dstcg.model.GameBalanceDefinition
import fr.aumombelli.dstcg.model.VariantProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class GameCatalogRepository(
    private val context: Context,
) : CatalogGateway {
    private val astronomyDetailsSerializersModule = SerializersModule {
        polymorphic(AstronomyDetails::class) {
            subclass(fr.aumombelli.dstcg.model.DeepSkyDetails::class)
            subclass(fr.aumombelli.dstcg.model.StarDetails::class)
            subclass(fr.aumombelli.dstcg.model.ConstellationDetails::class)
            subclass(fr.aumombelli.dstcg.model.SkyEventDetails::class)
            subclass(fr.aumombelli.dstcg.model.SolarSystemDetails::class)
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "detailType"
        serializersModule = astronomyDetailsSerializersModule
    }
    @Volatile
    private var extensionsCache: List<ExtensionDefinition>? = null
    @Volatile
    private var cardsCache: List<CardDefinition>? = null
    @Volatile
    private var variantProfilesCache: List<VariantProfile>? = null
    @Volatile
    private var gameBalanceCache: GameBalanceDefinition? = null
    @Volatile
    private var equipmentCardsCache: List<EquipmentCardDefinition>? = null
    @Volatile
    private var equipmentSettingsCache: EquipmentSettingsDefinition? = null

    override suspend fun loadExtensions(): List<ExtensionDefinition> = withContext(Dispatchers.IO) {
        extensionsCache ?: context.assets.open("catalog/extensions.json").bufferedReader().use { reader ->
            json.decodeFromString<List<ExtensionDefinition>>(reader.readText()).sortedBy { it.id }
                .also { extensionsCache = it }
        }
    }

    override suspend fun loadCards(): List<CardDefinition> = withContext(Dispatchers.IO) {
        cardsCache ?: context.assets.open("catalog/cards.json").bufferedReader().use { reader ->
            json.decodeFromString<List<CardDefinition>>(reader.readText()).sortedBy { it.id }
                .also { cardsCache = it }
        }
    }

    override suspend fun loadVariantProfiles(): List<VariantProfile> = withContext(Dispatchers.IO) {
        variantProfilesCache ?: context.assets.open("catalog/variant_profiles.json").bufferedReader().use { reader ->
            json.decodeFromString<List<VariantProfile>>(reader.readText()).sortedBy { it.id }
                .also { variantProfilesCache = it }
        }
    }

    override suspend fun loadGameBalance(): GameBalanceDefinition = withContext(Dispatchers.IO) {
        gameBalanceCache ?: context.assets.open("catalog/game_balance.json").bufferedReader().use { reader ->
            json.decodeFromString<GameBalanceDefinition>(reader.readText())
                .also { gameBalanceCache = it }
        }
    }

    override suspend fun loadEquipmentCards(): List<EquipmentCardDefinition> = withContext(Dispatchers.IO) {
        equipmentCardsCache ?: context.assets.open("catalog/equipment_cards.json").bufferedReader().use { reader ->
            json.decodeFromString<List<EquipmentCardDefinition>>(reader.readText())
                .sortedBy { it.id }
                .also { equipmentCardsCache = it }
        }
    }

    override suspend fun loadEquipmentSettings(): EquipmentSettingsDefinition = withContext(Dispatchers.IO) {
        equipmentSettingsCache ?: context.assets.open("catalog/equipment_settings.json").bufferedReader().use { reader ->
            json.decodeFromString<EquipmentSettingsDefinition>(reader.readText())
                .also { equipmentSettingsCache = it }
        }
    }
}

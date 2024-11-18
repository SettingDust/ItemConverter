package settingdust.item_converter

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.minecraftforge.fml.loading.FMLPaths
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

internal val json = Json {
    encodeDefaults = true
    prettyPrint = true
}

@Serializable
data class ClientConfig(val pressTicks: Int = 20) {
    companion object {
        private val path = FMLPaths.CONFIGDIR.get() / "${ItemConverter.ID}.client.json"

        var config = ClientConfig()

        @OptIn(ExperimentalSerializationApi::class)
        fun reload() {
            runCatching {
                path.createFile()
                path.writeText("{}")
            }
            config = json.decodeFromStream(path.inputStream())
            json.encodeToStream(config, path.outputStream())
        }
    }
}
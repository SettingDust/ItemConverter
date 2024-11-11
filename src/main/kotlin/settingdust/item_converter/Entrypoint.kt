package settingdust.item_converter

import com.mojang.serialization.JsonOps
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.ResourceKeyArgument
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.loading.FMLPaths
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.jvm.optionals.getOrNull

@Mod(ItemConverter.ID)
object ItemConverter {
    const val ID = "item_converter"
    val exportPath = FMLPaths.GAMEDIR.get() / ".item_converter_generated"

    init {

    }

    fun id(path: String) = ResourceLocation(ID, path)

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        event.dispatcher.register(Commands.literal(ID).apply {
            then(Commands.literal("generate").apply {
                then(Commands.argument("generator", ResourceKeyArgument.key(RuleGenerators.KEY)).apply {
                    executes { context ->
                        val key =
                            context.getArgument("generator", ResourceKey::class.java) as ResourceKey<RuleGenerator>
                        val registry = context.source.registryAccess().registry(RuleGenerators.KEY).getOrNull()
                            ?: error("No registry ${RuleGenerators.KEY.location()}")
                        val generator = registry.get(key) ?: error("No generator ${key.location()}")
                        val result = generator.generate(context.source.level)
                        for (entry in result) {
                            val path =
                                exportPath / entry.key.location().namespace / entry.key.registry().namespace / entry.key.registry().path / "${entry.key.location().path}.json"
                            val result = ConvertRule.CODEC.encodeStart(JsonOps.INSTANCE, entry.value)
                            path.writeText(
                                GsonHelper.toStableString(
                                    result.result().getOrNull() ?: error(
                                        result.error().get().message()
                                    )
                                )
                            )
                        }
                        context.source.sendSuccess(
                            Component.translatable(
                                "command.item_converter.generate.success",
                                exportPath
                            ), true
                        )
                        result.size
                    }
                })
            })
        })
    }
}
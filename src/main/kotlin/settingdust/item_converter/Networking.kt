package settingdust.item_converter

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.chat.Component
import net.minecraft.util.StringRepresentable
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import java.util.function.Supplier

object Networking {
    private const val VERSION = "1"
    val ID = ItemConverter.id("main")
    val channel = NetworkRegistry.newSimpleChannel(ID, { VERSION }, VERSION::equals, VERSION::equals)

    init {
        @Suppress("DEPRECATION")
        channel.registerMessage(
            0,
            C2SConvertItemPacket::class.java,
            { message, buf -> buf.writeWithCodec(C2SConvertItemPacket.CODEC, message) },
            { it.readWithCodec(C2SConvertItemPacket.CODEC) },
            C2SConvertItemPacket::handle
        )
    }
}

data class C2SConvertItemPacket(val slot: Int, val target: Int, val mode: Mode) {
    companion object {
        val CODEC = RecordCodecBuilder.create<C2SConvertItemPacket> { instance ->
            instance.group(
                Codec.INT.fieldOf("slot").forGetter { it.slot },
                Codec.INT.fieldOf("target").forGetter { it.target },
                StringRepresentable.fromEnum { Mode.values() }.fieldOf("mode").forGetter { it.mode }
            ).apply(instance, ::C2SConvertItemPacket)
        }

        fun handle(packet: C2SConvertItemPacket, context: Supplier<NetworkEvent.Context>) {
            context.get().enqueueWork {
                val player = context.get().sender
                if (player == null) {
                    ItemConverter.LOGGER.warn("Received C2SConvertItemPacket from null player.")
                    return@enqueueWork
                }
                val container = player.containerMenu
                if (container == null) {
                    ItemConverter.LOGGER.warn("Received C2SConvertItemPacket of null container from ${player.displayName}.")
                    return@enqueueWork
                }
                val slot = container.getSlot(packet.slot)
                val from = slot.item
                val registry = player.level.registryAccess().registryOrThrow(ConvertRules.KEY)
                val rule = registry.firstOrNull { it.input.matches(from) }
                if (rule == null) {
                    player.sendSystemMessage(
                        Component.translatable(
                            "messages.${ItemConverter.ID}.no_rule",
                            from.displayName
                        )
                    )
                    return@enqueueWork
                }
                val target = rule.output[packet.target].copy()
                when (packet.mode) {
                    Mode.ONE -> {
                        slot.safeTake(1, 1, player)
                        if (!player.inventory.add(target)) {
                            player.drop(target, true)
                        }
                    }

                    Mode.ALL -> {
                        slot.safeTake(from.count, from.count, player)
                        for (i in 0 until target.count) {
                            if (!player.inventory.add(target.copy())) {
                                player.drop(target, true)
                            }
                        }
                    }
                }
            }
        }
    }

    enum class Mode(private val serialName: String) : StringRepresentable {
        ONE("one"),
        ALL("all");

        override fun getSerializedName() = serialName
    }
}
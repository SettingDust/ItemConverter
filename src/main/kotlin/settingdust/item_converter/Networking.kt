package settingdust.item_converter

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.network.chat.Component
import net.minecraft.util.StringRepresentable
import net.minecraft.world.item.ItemStack
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import org.apache.commons.lang3.math.Fraction
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
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

data class C2SConvertItemPacket(val slot: Int, val target: ItemStack, val mode: Mode) {
    companion object {
        val CODEC = RecordCodecBuilder.create<C2SConvertItemPacket> { instance ->
            instance.group(
                Codec.INT.fieldOf("slot").forGetter { it.slot },
                MoreCodecs.ITEM_STACK.fieldOf("target").forGetter { it.target },
                StringRepresentable.fromEnum { Mode.values() }.fieldOf("mode").forGetter { it.mode }
            ).apply(instance, ::C2SConvertItemPacket)
        }

        fun handle(packet: C2SConvertItemPacket, context: Supplier<NetworkEvent.Context>) {
            val player = context.get().sender
            if (player == null) {
                ItemConverter.LOGGER.warn("Received C2SConvertItemPacket from null player.")
                return
            }
            val container = player.containerMenu
            if (container == null) {
                ItemConverter.LOGGER.warn("Received C2SConvertItemPacket of null container from ${player.displayName}.")
                return
            }
            val slot = container.getSlot(packet.slot)
            val fromItem = slot.item
            val from = ConvertRules.graph.vertexSet().first { it.test(fromItem) }
            if (from == null) {
                player.sendSystemMessage(
                    Component.translatable(
                        "messages.${ItemConverter.ID}.no_rule",
                        fromItem.displayName
                    )
                )
                return
            }
            val to = ConvertRules.graph.vertexSet().firstOrNull { it == SimpleItemPredicate(packet.target) }
            if (to == null) {
                ItemConverter.LOGGER.error("${player.displayName.string} trying to convert ${fromItem.displayName.string} to target not in graph ${packet.target}")
                return
            }
            val path = DijkstraShortestPath.findPathBetween(ConvertRules.graph, from, to)
            if (path == null) {
                ItemConverter.LOGGER.error("${player.displayName.string} trying to convert ${fromItem.displayName.string} to unreachable target ${packet.target}")
                return
            }
            val ratio = path.edgeList.fold(Fraction.ONE) { acc, edge -> edge.fraction.multiplyBy(acc) }
            context.get().enqueueWork {
                when (packet.mode) {
                    Mode.ONE -> {
                        if (ratio.denominator > fromItem.count) {
                            return@enqueueWork
                        }
                        slot.safeTake(ratio.denominator, ratio.denominator, player)
                        val itemToInsert = to.predicate.copy().also {
                            it.count = ratio.numerator
                        }
                        if (!player.inventory.add(itemToInsert)) {
                            player.drop(itemToInsert, true)
                        }
                    }

                    Mode.ALL -> {
                        val times = fromItem.count / ratio.denominator
                        val amount = ratio.denominator * times
                        slot.safeTake(amount, amount, player)
                        val itemToInsert = to.predicate.copy().also {
                            it.count = ratio.numerator * times
                        }
                        if (!player.inventory.add(itemToInsert)) {
                            player.drop(itemToInsert, true)
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
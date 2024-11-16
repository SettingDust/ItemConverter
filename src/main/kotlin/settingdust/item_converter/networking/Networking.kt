package settingdust.item_converter.networking

import net.minecraftforge.network.NetworkRegistry
import settingdust.item_converter.ItemConverter

@Suppress("DEPRECATION")
object Networking {
    private const val VERSION = "1"
    val ID = ItemConverter.id("main")
    val channel = NetworkRegistry.newSimpleChannel(ID, { VERSION }, VERSION::equals, VERSION::equals)

    init {
        channel.registerMessage(
            0,
            C2SConvertItemPacket::class.java,
            { message, buf -> buf.writeWithCodec(C2SConvertItemPacket.CODEC, message) },
            { it.readWithCodec(C2SConvertItemPacket.CODEC) },
            { packet, context -> C2SConvertItemPacket.handle(packet, context) }
        )
        channel.registerMessage(
            1,
            C2SConvertTargetPacket::class.java,
            { _, _ ->  },
            { C2SConvertTargetPacket },
            { _, context -> C2SConvertTargetPacket.handle(context) }
        )
    }
}
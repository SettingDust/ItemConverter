package settingdust.item_converter

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.item.ItemStack
import org.apache.commons.lang3.math.Fraction
import org.jgrapht.graph.SimpleDirectedWeightedGraph

object ConvertRules {
    val KEY = ResourceKey.createRegistryKey<ConvertRule>(ItemConverter.id("rules"))

    var graph =
        SimpleDirectedWeightedGraph<SimpleItemPredicate, FractionUnweightedEdge>(null) { FractionUnweightedEdge(Fraction.ZERO) }

    fun key(name: ResourceLocation) = ResourceKey.create(KEY, name)
}

data class ConvertRule(
    val input: ItemStack,
    val output: List<ItemStack>,
    val sound: SoundEvent,
    val pitch: Float,
    val volume: Float,
    val bidirectional: Boolean = false
) {
    companion object {
        val CODEC = RecordCodecBuilder.create<ConvertRule> { instance ->
            instance.group(
                MoreCodecs.ITEM_STACK.fieldOf("input").forGetter { it.input },
                MoreCodecs.ITEM_STACK.listOf().fieldOf("output").forGetter { it.output },
                SoundEvent.CODEC.fieldOf("sound").forGetter { it.sound },
                Codec.FLOAT.fieldOf("pitch").forGetter { it.pitch },
                Codec.FLOAT.fieldOf("volume").forGetter { it.volume },
                Codec.BOOL.optionalFieldOf("bidirectional", false).forGetter { it.bidirectional }
            ).apply(instance, ::ConvertRule)
        }
    }
}

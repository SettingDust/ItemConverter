package settingdust.item_converter

import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
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
    val output: List<ItemStack>
) {
    companion object {
        val CODEC = RecordCodecBuilder.create<ConvertRule> { instance ->
            instance.group(
                MoreCodecs.ITEM_STACK.fieldOf("input").forGetter { it.input },
                MoreCodecs.ITEM_STACK.listOf().fieldOf("output").forGetter { it.output }
            ).apply(instance, ::ConvertRule)
        }
    }
}

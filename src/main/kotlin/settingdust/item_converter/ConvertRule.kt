package settingdust.item_converter

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.IForgeRegistry
import java.util.function.Supplier

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
object ConvertRules {
    val KEY = ResourceKey.createRegistryKey<ConvertRule>(ItemConverter.id("rules"))
    lateinit var REGISTRY: Supplier<IForgeRegistry<ConvertRule>>
        internal set

    fun key(name: ResourceLocation) = ResourceKey.create(KEY, name)
}

data class ConvertRule(
    val input: ItemPredicate,
    val output: List<ItemStack>,
    val consumeDamage: Boolean = false
) {
    companion object {
        val CODEC = RecordCodecBuilder.create<ConvertRule> { instance ->
            instance.group(
                MoreCodecs.ITEM_PREDICATE.fieldOf("input").forGetter { it.input },
                MoreCodecs.ITEM_STACK.listOf().fieldOf("output").forGetter { it.output },
                Codec.BOOL.optionalFieldOf("consume_damage", false).forGetter { it.consumeDamage }
            ).apply(instance, ::ConvertRule)
        }
    }
}

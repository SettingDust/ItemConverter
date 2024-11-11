package settingdust.item_converter

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.IForgeRegistry
import net.minecraftforge.registries.NewRegistryEvent
import net.minecraftforge.registries.RegistryBuilder
import java.util.function.Supplier

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
object ConvertRules {
    val KEY = ResourceKey.createRegistryKey<ConvertRule>(ItemConverter.id("rules"))
    lateinit var REGISTRY: Supplier<IForgeRegistry<ConvertRule>>
        private set

    fun key(name: ResourceLocation) = ResourceKey.create(KEY, name)

    @SubscribeEvent
    internal fun onNewRegistry(event: NewRegistryEvent) {
        REGISTRY = event.create(RegistryBuilder<ConvertRule>().dataPackRegistry(ConvertRule.CODEC, ConvertRule.CODEC))
    }
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

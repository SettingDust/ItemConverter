package settingdust.item_converter

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.Hash.Strategy
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenCustomHashMap
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.Container
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.IForgeRegistry
import java.util.function.Supplier

typealias RuleGeneratorType = Codec<out RuleGenerator>

object RuleGeneratorTypes {
    val KEY = ResourceKey.createRegistryKey<RuleGeneratorType>(ItemConverter.id("rule_generator_type"))

    val RECIPE = RecipeRuleGenerator.CODEC

    lateinit var REGISTRY: Supplier<IForgeRegistry<RuleGeneratorType>>
        internal set

    fun key(name: ResourceLocation) = ResourceKey.create(KEY, name)
}

object RuleGenerators {
    val KEY = ResourceKey.createRegistryKey<RuleGenerator>(ItemConverter.id("rule_generator"))
    lateinit var REGISTRY: Supplier<IForgeRegistry<RuleGenerator>>
        internal set

    fun key(name: ResourceLocation) = ResourceKey.create(KEY, name)
}

interface RuleGenerator {
    companion object {
        val CODEC = ExtraCodecs.lazyInitializedCodec {
            RuleGeneratorTypes.REGISTRY.get().codec.dispatch({ it.codec }, { it })
        }
    }

    val codec: Codec<out RuleGenerator>

    fun generate(level: Level): Map<ResourceKey<ConvertRule>, ConvertRule>
}

data class RecipeRuleGenerator(
    val recipeType: ResourceKey<RecipeType<*>>
) : RuleGenerator {
    companion object {
        val CODEC = RecordCodecBuilder.create<RecipeRuleGenerator> { instance ->
            instance.group(
                ResourceKey.codec(Registry.RECIPE_TYPE_REGISTRY).fieldOf("recipe_type").forGetter { it.recipeType }
            ).apply(instance, ::RecipeRuleGenerator)
        }
    }

    val type = ForgeRegistries.RECIPE_TYPES.getDelegateOrThrow(recipeType).get()

    override val codec = CODEC

    override fun generate(level: Level): Map<ResourceKey<ConvertRule>, ConvertRule> {
        val recipeManager = level.recipeManager
        val recipes = recipeManager.getAllRecipesFor(type as RecipeType<Recipe<Container>>)
        var itemCounter = mutableMapOf<Item, Int>().withDefault { 0 }
        val inputToOutput = recipes.flatMap { recipe ->
            recipe.ingredients.singleOrNull()?.items?.map { it to recipe.resultItem } ?: emptySet()
        }
        val predicatesToOutputs =
            Object2ReferenceOpenCustomHashMap<Pair<ItemPredicate, ResourceKey<ConvertRule>>, MutableList<ItemStack>>(
                inputToOutput.size,
                object : Strategy<Pair<ItemPredicate, ResourceKey<ConvertRule>>> {
                    override fun hashCode(o: Pair<ItemPredicate, ResourceKey<ConvertRule>>?): Int {
                        return o?.first?.serializeToJson()?.hashCode() ?: 0
                    }

                    override fun equals(
                        a: Pair<ItemPredicate, ResourceKey<ConvertRule>>?,
                        b: Pair<ItemPredicate, ResourceKey<ConvertRule>>?
                    ): Boolean {
                        return a?.first?.serializeToJson()?.equals(b?.first?.serializeToJson()) == true
                    }
                })
        for ((input, output) in inputToOutput) {
            val itemKey = ForgeRegistries.ITEMS.getKey(input.item)!!
            predicatesToOutputs.getOrPut(
                input.toItemPredicate() to ConvertRules.key(
                    ItemConverter.id(
                        "${itemKey.namespace}/${itemKey.path}_${itemCounter.getValue(input.item)}"
                    )
                )
            ) {
                itemCounter[input.item] = itemCounter.getOrDefault(input.item, -1) + 1
                mutableListOf()
            } += output
        }
        return predicatesToOutputs
            .map { entry -> entry.key.second to ConvertRule(entry.key.first, entry.value) }.toMap()
    }
}
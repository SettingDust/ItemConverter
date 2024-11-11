package settingdust.item_converter

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.Container
import net.minecraft.world.item.Item
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
        return recipes.flatMap { recipe ->
            recipe.ingredients.singleOrNull()?.items?.map { it to recipe.resultItem } ?: emptySet()
        }.fold(mutableMapOf<ResourceKey<ConvertRule>, ConvertRule>()) { map, (input, output) ->
            val itemKey = ForgeRegistries.ITEMS.getKey(input.item)!!
            val key = ConvertRules.key(
                ItemConverter.id(
                    "${itemKey.namespace}/${itemKey.path}_${
                        itemCounter.getValue(input.item).also { itemCounter[input.item] = it + 1 }
                    }"
                )
            )
            map[key] = ConvertRule(
                input.toItemPredicate(),
                listOf(output)
            )
            map
        }
    }
}
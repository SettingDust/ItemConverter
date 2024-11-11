package settingdust.item_converter

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.Container
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.IForgeRegistry
import net.minecraftforge.registries.NewRegistryEvent
import net.minecraftforge.registries.RegistryBuilder
import java.util.function.Supplier

typealias RuleGeneratorType = Codec<out RuleGenerator>

object RuleGeneratorTypes {
    val KEY = ResourceKey.createRegistryKey<RuleGeneratorType>(ItemConverter.id("rule_generator_type"))
    private val REGISTER = DeferredRegister.create(KEY, ItemConverter.ID)

    val REGISTRY = REGISTER.makeRegistry {
        RegistryBuilder<RuleGeneratorType>()
    }

    val RECIPE = REGISTER.register("recipe") { RecipeRuleGenerator.CODEC }

    fun key(name: ResourceLocation) = ResourceKey.create(KEY, name)
}

object RuleGenerators {
    val KEY = ResourceKey.createRegistryKey<RuleGenerator>(ItemConverter.id("rule_generator"))
    lateinit var REGISTRY: Supplier<IForgeRegistry<RuleGenerator>>
        private set

    fun key(name: ResourceLocation) = ResourceKey.create(KEY, name)

    @SubscribeEvent
    internal fun onNewRegistry(event: NewRegistryEvent) {
        REGISTRY = event.create(RegistryBuilder<RuleGenerator>().dataPackRegistry(RuleGenerator.CODEC))
    }
}

interface RuleGenerator {
    companion object {
        val CODEC by lazy {
            RuleGeneratorTypes.REGISTRY.get().codec.dispatch({ it.codec }, { it })
        }
    }

    val codec: Codec<out RuleGenerator>

    fun generate(level: Level): Map<ResourceKey<ConvertRule>, ConvertRule>
}

data class RecipeRuleGenerator(
    val typeKey: ResourceKey<RecipeType<*>>
) : RuleGenerator {
    companion object {
        val CODEC = RecordCodecBuilder.create<RecipeRuleGenerator> { instance ->
            instance.group(
                ResourceKey.codec(Registry.RECIPE_TYPE_REGISTRY).fieldOf("type").forGetter { it.typeKey }
            ).apply(instance, ::RecipeRuleGenerator)
        }
    }

    val type = ForgeRegistries.RECIPE_TYPES.getDelegateOrThrow(typeKey).get()

    override val codec = CODEC

    override fun generate(level: Level): Map<ResourceKey<ConvertRule>, ConvertRule> {
        val recipeManager = level.recipeManager
        val recipes = recipeManager.getAllRecipesFor(type as RecipeType<Recipe<Container>>)
        var itemCounter = mutableMapOf<Item, Int>().withDefault { 0 }
        return recipes.flatMap { recipe ->
            recipe.ingredients.singleOrNull()?.items?.map { it to recipe.resultItem } ?: emptySet()
        }.fold(mutableMapOf<ResourceKey<ConvertRule>, ConvertRule>()) { map, (input, output) ->
            val key = ConvertRules.key(
                ItemConverter.id(
                    "${
                        ForgeRegistries.ITEMS.getKey(input.item)!!
                    }_${
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
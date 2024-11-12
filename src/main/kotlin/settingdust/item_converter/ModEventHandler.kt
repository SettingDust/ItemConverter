package settingdust.item_converter

import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.registries.NewRegistryEvent
import net.minecraftforge.registries.RegistryBuilder
import org.apache.commons.lang3.math.Fraction
import settingdust.item_converter.ConvertRules.KEY
import settingdust.item_converter.ItemConverter.id

object ModEventHandler {
    @SubscribeEvent
    internal fun onNewRegistry(event: NewRegistryEvent) {
        RuleGeneratorTypes.REGISTRY = event.create(
            RegistryBuilder<RuleGeneratorType>().setName(RuleGeneratorTypes.KEY.location()).disableSync()
        ) { it.register(id("recipe"), RuleGeneratorTypes.RECIPE) }
        RuleGenerators.REGISTRY = event.create(
            RegistryBuilder<RuleGenerator>().setName(RuleGenerators.KEY.location())
                .dataPackRegistry(RuleGenerator.CODEC).disableSync()
        )
        ConvertRules.REGISTRY = event.create(
            RegistryBuilder<ConvertRule>().setName(KEY.location())
                .dataPackRegistry(ConvertRule.CODEC, ConvertRule.CODEC)
                .onBake { internal, manager ->
                    ConvertRules.graph.removeAllVertices(ConvertRules.graph.vertexSet())
                    for (rule in internal.values) {
                        val input = rule.input
                        val inputPredicate = SimpleItemPredicate(input.copy().also { it.count = 1 })
                        ConvertRules.graph.addVertex(inputPredicate)
                        for (output in rule.output) {
                            val outputPredicate = SimpleItemPredicate(output.copy().also { it.count = 1 })
                            val fraction = Fraction.getReducedFraction(output.count, input.count)
                            ConvertRules.graph.addEdge(
                                inputPredicate,
                                outputPredicate,
                                FractionUnweightedEdge(fraction)
                            )
                            ConvertRules.graph.addEdge(
                                outputPredicate,
                                inputPredicate,
                                FractionUnweightedEdge(fraction.invert())
                            )
                        }
                    }
                }
        )
    }
}
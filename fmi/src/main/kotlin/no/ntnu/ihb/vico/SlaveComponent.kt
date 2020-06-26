package no.ntnu.ihb.vico

import no.ntnu.ihb.acco.core.*
import no.ntnu.ihb.acco.util.ObservableSet
import no.ntnu.ihb.fmi4j.SlaveInstance
import no.ntnu.ihb.fmi4j.modeldescription.CoSimulationModelDescription
import no.ntnu.ihb.fmi4j.modeldescription.ValueReference
import no.ntnu.ihb.fmi4j.modeldescription.variables.Causality
import no.ntnu.ihb.vico.model.SlaveProvider
import no.ntnu.ihb.vico.structure.Parameter
import no.ntnu.ihb.vico.structure.ParameterSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private typealias Cache<E> = HashMap<ValueReference, E>

open class SlaveComponent(
    private val slaveProvider: SlaveProvider,
    val instanceName: String,
    val stepSizeHint: Double? = null
) : Component() {

    var stepCount = 0L
        internal set

    val modelDescription: CoSimulationModelDescription
        get() = slaveProvider.modelDescription

    val variablesMarkedForReading: ObservableSet<String> = ObservableSet(mutableSetOf())

    internal val integerSetCache by lazy { Cache<Int>() }
    internal val realSetCache by lazy { Cache<Double>() }
    internal val booleanSetCache by lazy { Cache<Boolean>() }
    internal val stringSetCache by lazy { Cache<String>() }

    internal val integerGetCache by lazy { Cache<Int>() }
    internal val realGetCache by lazy { Cache<Double>() }
    internal val booleanGetCache by lazy { Cache<Boolean>() }
    internal val stringGetCache by lazy { Cache<String>() }

    private val parameterSets: MutableMap<String, ParameterSet> = mutableMapOf()

    init {

        val modelVariables = modelDescription.modelVariables

        val ints = (modelVariables.integers + modelVariables.enumerations).map { v ->
            IntLambdaProperty(
                v.name, 1,
                getter = {
                    variablesMarkedForReading.add(v.name)
                    it[0] = integerGetCache[v.valueReference] ?: 0
                },
                setter = { integerSetCache[v.valueReference] = it.first() },
                causality = v.causality.convert()
            )
        }
        val reals = modelVariables.reals.map { v ->
            RealLambdaProperty(
                v.name, 1,
                getter = {
                    variablesMarkedForReading.add(v.name)
                    it[0] = realGetCache[v.valueReference] ?: 0.0
                },
                setter = { realSetCache[v.valueReference] = it.first() },
                causality = v.causality.convert()
            )
        }
        val strings = modelVariables.strings.map { v ->
            StrLambdaProperty(
                v.name, 1,
                getter = {
                    variablesMarkedForReading.add(v.name)
                    it[0] = stringGetCache[v.valueReference] ?: ""
                },
                setter = { stringSetCache[v.valueReference] = it.first() },
                causality = v.causality.convert()
            )
        }
        val booleans = modelVariables.booleans.map { v ->
            BoolLambdaProperty(
                v.name, 1,
                getter = {
                    variablesMarkedForReading.add(v.name)
                    it[0] = booleanGetCache[v.valueReference] ?: false
                },
                setter = { booleanSetCache[v.valueReference] = it.first() },
                causality = v.causality.convert()
            )
        }

        registerProperties(ints + reals + strings + booleans)

    }

    fun instantiate(): SlaveInstance = slaveProvider.instantiate(instanceName)

    fun getParameterSet(name: String): ParameterSet? {
        return parameterSets[name]
    }

    fun addParameterSet(name: String, parameters: List<Parameter>) = apply {
        addParameterSet(ParameterSet(name, parameters))
    }

    fun addParameterSet(parameterSet: ParameterSet) = apply {
        check(parameterSet.name !in parameterSets) {
            "ParameterSet named '${parameterSet.name}' has already been added to component '${instanceName}'!"
        }
        parameterSets[parameterSet.name] = parameterSet
    }

    internal fun clearCaches() {
        listOf(realGetCache, integerGetCache, booleanGetCache, stringGetCache).forEach { it.clear() }
        listOf(realSetCache, integerSetCache, booleanSetCache, stringSetCache).forEach { it.clear() }
    }

    override fun toString(): String {
        return "SlaveComponent(instanceName=$instanceName)"
    }

    private companion object {
        private val LOG: Logger = LoggerFactory.getLogger(SlaveComponent::class.java)
    }

}

private fun Causality?.convert(): no.ntnu.ihb.acco.core.Causality {
    return when (this) {
        Causality.INPUT -> no.ntnu.ihb.acco.core.Causality.INPUT
        Causality.OUTPUT -> no.ntnu.ihb.acco.core.Causality.OUTPUT
        Causality.LOCAL -> no.ntnu.ihb.acco.core.Causality.LOCAL
        Causality.PARAMETER -> no.ntnu.ihb.acco.core.Causality.PARAMETER
        Causality.CALCULATED_PARAMETER -> no.ntnu.ihb.acco.core.Causality.CALCULATED_PARAMETER
        else -> no.ntnu.ihb.acco.core.Causality.UNKNOWN
    }
}


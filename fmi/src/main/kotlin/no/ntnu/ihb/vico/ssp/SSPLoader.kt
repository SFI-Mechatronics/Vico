package no.ntnu.ihb.vico.ssp

import no.ntnu.ihb.fmi4j.modeldescription.variables.*
import no.ntnu.ihb.fmi4j.util.extractContentTo
import no.ntnu.ihb.vico.core.LinearTransform
import no.ntnu.ihb.vico.model.ModelResolver
import no.ntnu.ihb.vico.ssp.jaxb.SystemStructureDescription
import no.ntnu.ihb.vico.ssp.jaxb.TComponent
import no.ntnu.ihb.vico.structure.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.util.stream.Collectors
import javax.xml.bind.JAXB

private typealias Components = Map<String, Component>

private const val DEFAULT_SSD_FILENAME = "SystemStructure.ssd"
private const val VICO_NAMESPACE = "com.github.ntnu-ihb.vico"
private const val OSP_NAMESPACE = "com.opensimulationplatform"

class SSPLoader @JvmOverloads constructor(
    sspFile: File,
    customSsdName: String? = null
) {

    val ssdFile: File

    init {
        require(sspFile.exists()) { "No such file: '$sspFile'" }

        val systemStructureFile = customSsdName ?: DEFAULT_SSD_FILENAME

        this.ssdFile = when {
            sspFile.extension == "ssd" -> {
                sspFile
            }
            sspFile.extension == "ssp" -> {
                val temp = Files.createTempDirectory("vico_").toFile().also {
                    it.deleteOnExit()
                }
                sspFile.extractContentTo(temp)
                File(temp, systemStructureFile)
            }
            sspFile.isDirectory -> {
                File(sspFile, systemStructureFile)
            }
            else -> throw IllegalArgumentException("Unsupported input file: ${sspFile.absolutePath}")
        }

        require(this.ssdFile.exists()) { "No such file: '${sspFile.absolutePath}'" }
    }

    fun load(): SystemStructure {

        val ssd = JAXB.unmarshal(ssdFile, SystemStructureDescription::class.java)
        val components = parseComponents(ssd)
        return SystemStructure(ssd.name).apply {
            components.values.forEach { component ->
                addComponent(component)
            }
            parseConnections(ssd, components).forEach { connection ->
                addConnection(connection)
            }
            parseDefaultExperiment(ssd)?.also { defaultExperiment = it }
        }.also {
            LOG.info("Loaded SSP config '${ssd.name}'")
        }
    }

    private fun parseComponents(ssd: SystemStructureDescription): Components {
        return ssd.system.elements.components.parallelStream().map { c ->
            parseComponent(c).also { component ->
                c.connectors?.connector?.onEach { sspConnector ->
                    val connector = when {
                        sspConnector.integer != null -> IntegerConnectorInfo(
                            sspConnector.name,
                            ConnectorKind.valueOf(sspConnector.kind.toUpperCase())
                        )
                        sspConnector.real != null -> RealConnectorInfo(
                            sspConnector.name,
                            ConnectorKind.valueOf(sspConnector.kind.toUpperCase()),
                            sspConnector.real.unit
                        )
                        sspConnector.boolean != null -> BooleanConnectorInfo(
                            sspConnector.name,
                            ConnectorKind.valueOf(sspConnector.kind.toUpperCase())
                        )
                        sspConnector.string != null -> StringConnectorInfo(
                            sspConnector.name,
                            ConnectorKind.valueOf(sspConnector.kind.toUpperCase())
                        )
                        sspConnector.binary != null -> {
                            throw UnsupportedOperationException("Binary connector is currently unsupported!")
                        }
                        sspConnector.enumeration != null -> {
                            throw UnsupportedOperationException("Enumeration connector is currently unsupported!")
                        }
                        else -> throw AssertionError()
                    }
                    component.addConnectorInfo(connector)
                }
                parseParameterBindings(c, component)
            }
        }.collect(Collectors.toList()).associateBy { it.instanceName }
    }

    private fun parseParameterBindings(c: TComponent, component: Component) {
        c.parameterBindings?.parameterBinding?.forEach { binding ->
            if (binding.source != null) {
                val parameterSet = JAXB.unmarshal(
                    URI("${ssdFile.parentFile.toURI()}/${binding.source}"),
                    no.ntnu.ihb.vico.ssp.jaxb.ParameterSet::class.java
                )
                component.addParameterSet(parseParameterSet(parameterSet))
            } else {
                binding.parameterValues.parameterSet.forEach { parameterSet ->
                    component.addParameterSet(parseParameterSet(parameterSet))
                }
            }
        }
    }

    private fun parseParameterSet(p: no.ntnu.ihb.vico.ssp.jaxb.ParameterSet): ParameterSet {
        val parameters = p.parameters.parameter.mapNotNull {
            when {
                it.integer != null -> IntegerParameter(it.name, it.integer.value)
                it.real != null -> RealParameter(it.name, it.real.value)
                it.boolean != null -> BooleanParameter(it.name, it.boolean.isValue)
                it.string != null -> StringParameter(it.name, it.string.value)
                it.enumeration != null -> {
                    LOG.error("Enumerations parameters are unsupported")
                    null
                }
                else -> throw UnsupportedOperationException("Unable to parse parameter: $it")
            }
        }
        return ParameterSet(p.name, parameters)
    }

    private fun parseComponent(c: TComponent): Component {
        var uri = URI(c.source)
        if (!uri.isAbsolute) {
            uri = URI("${ssdFile.parentFile.absoluteFile.toURI()}/${c.source}")
        }
        var stepSizeHint: Double? = null
        c.annotations?.annotation?.forEach { annotation ->
            when (annotation.type) {
                VICO_NAMESPACE, OSP_NAMESPACE -> {
                    val elem = annotation.any as Element
                    when (elem.nodeName) {
                        "vico:StepSizeHint", "osp:StepSizeHint" -> {
                            stepSizeHint = elem.getAttribute("value").toDoubleOrNull()
                        }
                    }
                }
            }
        }
        val model = ModelResolver.resolve(ssdFile.parentFile, uri)
        return Component(model, c.name, stepSizeHint)
    }

    private fun parseConnections(
        ssd: SystemStructureDescription,
        components: Components
    ): List<ConnectionInfo<*>> {
        return ssd.system.connections?.connection?.parallelStream()?.map { c ->

            val startComponent = components[c.startElement]
                ?: throw RuntimeException("No component named '${c.endElement}'")
            val startConnector = startComponent.getConnectorInfo(c.startConnector)

            val endComponent = components[c.endElement]
                ?: throw RuntimeException("No component named '${c.endElement}'")
            val endConnector = endComponent.getConnectorInfo(c.endConnector)

            val startVariable = startComponent.modelDescription.getVariableByName(startConnector.name)
            val endVariable = endComponent.modelDescription.getVariableByName(endConnector.name)

            require(startVariable.type == endVariable.type)

            when (startVariable.type) {
                VariableType.INTEGER, VariableType.ENUMERATION -> IntegerConnectionInfo(
                    startComponent, startVariable as IntegerVariable,
                    endComponent, endVariable as IntegerVariable
                )
                VariableType.REAL -> RealConnectionInfo(
                    startComponent, startVariable as RealVariable,
                    endComponent, endVariable as RealVariable,
                    c.linearTransformation?.let { t -> LinearTransform(t.factor, t.offset) }
                )
                VariableType.STRING -> StringConnectionInfo(
                    startComponent, startVariable as StringVariable,
                    endComponent, endVariable as StringVariable
                )
                VariableType.BOOLEAN -> BooleanConnectionInfo(
                    startComponent, startVariable as BooleanVariable,
                    endComponent, endVariable as BooleanVariable
                )
            }

        }?.collect(Collectors.toList()) ?: emptyList()
    }

    private fun parseDefaultExperiment(ssd: SystemStructureDescription): DefaultExperiment? {
        return ssd.defaultExperiment?.let {
            val startTime = it.startTime ?: 0.0
            val stopTime: Double? = it.stopTime
            DefaultExperiment(startTime, stopTime)
        }
    }

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(SSPLoader::class.java)
    }

}

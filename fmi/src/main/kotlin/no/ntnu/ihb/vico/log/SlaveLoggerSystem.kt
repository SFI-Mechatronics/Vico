package no.ntnu.ihb.vico.log

import no.ntnu.ihb.vico.SlaveComponent
import no.ntnu.ihb.vico.core.*
import no.ntnu.ihb.vico.core.Properties
import no.ntnu.ihb.vico.log.jaxb.TLogConfig
import no.ntnu.ihb.vico.util.formatForOutput
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.bind.JAXB

class SlaveLoggerSystem(
    private val logConfig: TLogConfig? = null,
    targetDir: File? = null
) : EventSystem(Family.all(SlaveComponent::class.java).build()) {

    var separator: String = ", "
    var decimalPoints: Int = 8
    var bufferSize: Int = 8 * 1024 * 4
    val targetDir: File = targetDir ?: File(".")
    private val loggers: MutableMap<String, SlaveLogger> = mutableMapOf()

    constructor(configFile: File, targetDir: File? = null) : this(
        JAXB.unmarshal(configFile, TLogConfig::class.java), targetDir
    )

    init {

        priority = Int.MAX_VALUE

        this.targetDir.mkdirs()

        logConfig?.components?.component?.also {
            mutableSetOf<String>().apply {
                for (key in it) {
                    check(add(key.name)) { "Duplicate component in log configuration: '${key.name}'" }
                }
            }
        }

        listen(Properties.PROPERTIES_CHANGED)
    }

    override fun entityAdded(entity: Entity) {
        val slave = entity.get<SlaveComponent>()
        if (logConfig == null) {
            loggers[slave.instanceName] = SlaveLogger(slave, emptyList(), 1)
        } else {
            val logInfo = logConfig.components?.component?.associateBy { it.name } ?: mutableMapOf()
            logInfo[slave.instanceName]?.also { component ->
                val decimationFactor = component.decimationFactor
                require(decimationFactor >= 1)
                val variables = component.variable.map { v ->
                    slave.getProperty(v.name)
                }
                loggers[slave.instanceName] =
                    SlaveLogger(slave, variables, decimationFactor, logConfig.isStaticFileNames)
            }
        }
        loggers[slave.instanceName]?.also {
            it.writeHeader()
        }
    }

    override fun entityRemoved(entity: Entity) {
        loggers.remove(entity.name)?.also {
            it.close()
        }
    }

    override fun init(engine: Engine) {
        loggers.values.forEach { logger ->
            logger.writeLine(engine.currentTime)
        }
    }

    override fun eventReceived(evt: Event) {
        when (evt.type) {
            Properties.PROPERTIES_CHANGED -> {
                val (currentTime, slave) = evt.value<Pair<Double, SlaveComponent>>()
                loggers[slave.instanceName]?.also { logger ->
                    logger.writeLine(currentTime)
                }
            }
        }
    }

    override fun close() {
        loggers.values.forEach { it.close() }
    }

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(SlaveLoggerSystem::class.java)
    }

    private inner class SlaveLogger(
        private val slave: SlaveComponent,
        variables: List<Property>,
        private val decimationFactor: Int,
        staticFileNames: Boolean = false
    ) : Closeable {

        private val writer: BufferedWriter
        private val variables: Collection<Property>

        init {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileName = if (staticFileNames) slave.instanceName else "${slave.instanceName}_$dateFormat"
            this.writer = FileWriter(File(targetDir, "${fileName}.csv")).buffered(bufferSize)
            this.variables = if (variables.isEmpty()) slave.properties.getAllProperties() else variables
            LOG.info("Logging ${this.variables.size} variables from '${slave.instanceName}'")
        }

        fun writeHeader() {
            variables.joinToString(separator, "time, stepNumber, ", "\n") {
                val causality = it.causality
                "${it.name} [${it.type} $causality]"
            }.also {
                writer.write(it)
            }
        }

        @Suppress("IMPLICIT_CAST_TO_ANY")
        fun writeLine(currentTime: Double) {
            if (slave.stepCount % decimationFactor == 0L) {
                variables.asSequence().map {
                    when (it) {
                        is IntProperty -> it.read()
                        is RealProperty -> it.read()
                        is StrProperty -> it.read()
                        is BoolProperty -> it.read()
                    }
                }.joinToString(
                    separator,
                    "${currentTime.formatForOutput(decimalPoints)}$separator${slave.stepCount}$separator",
                    "\n"
                ).also {
                    writer.write(it)
                }
            }
        }

        override fun close() {
            writer.flush()
            writer.close()
            LOG.debug("Wrote csv data for slave '${slave.instanceName} to folder '${targetDir.absolutePath}'")
        }

    }

}

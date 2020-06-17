package no.ntnu.ihb.vico.log

import no.ntnu.ihb.acco.core.Entity
import no.ntnu.ihb.acco.core.Event
import no.ntnu.ihb.acco.core.EventSystem
import no.ntnu.ihb.acco.core.Family
import no.ntnu.ihb.acco.util.formatForOutput
import no.ntnu.ihb.fmi4j.modeldescription.variables.ScalarVariable
import no.ntnu.ihb.fmi4j.modeldescription.variables.VariableType
import no.ntnu.ihb.fmi4j.readBoolean
import no.ntnu.ihb.fmi4j.readInteger
import no.ntnu.ihb.fmi4j.readReal
import no.ntnu.ihb.fmi4j.readString
import no.ntnu.ihb.vico.SlaveComponent
import no.ntnu.ihb.vico.SlaveSystem
import no.ntnu.ihb.vico.log.jaxb.TLogConfig
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.bind.JAXB

class SlaveLoggerSystem(
    private val logConfig: TLogConfig? = null,
    targetDir: File? = null
) : EventSystem(Family.all(SlaveComponent::class.java).build()) {

    var separator: String = ", "
    var decimalPoints: Int = 6
    val targetDir: File = targetDir ?: File(".")
    private val loggers: MutableMap<String, Logger> = mutableMapOf()

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

        listen(SlaveSystem.SLAVE_STEPPED)
    }

    override fun entityAdded(entity: Entity) {
        val slave = entity.getComponent(SlaveComponent::class.java)
        if (logConfig == null) {
            loggers[slave.instanceName] = Logger(slave, emptyList(), 1)
        } else {
            val logInfo = logConfig.components?.component?.associateBy { it.name } ?: mutableMapOf()
            logInfo[slave.instanceName]?.also { component ->
                val decimationFactor = component.decimationFactor
                require(decimationFactor >= 1)
                val variables = component.variable.map { v ->
                    slave.modelDescription.getVariableByName(v.name)
                }
                loggers[slave.instanceName] = Logger(slave, variables, decimationFactor, logConfig.isStaticFileNames)
            }
        }
        loggers[slave.instanceName]?.also {
            it.writeHeader()
        }
    }

    override fun init(currentTime: Double) {
        loggers.values.forEach { logger ->
            logger.writeLine(currentTime)
        }
    }

    override fun eventReceived(evt: Event) {
        when (evt.type) {
            SlaveSystem.SLAVE_STEPPED -> {
                val (currentTime, slave) = evt.target<Pair<Double, SlaveComponent>>()
                loggers[slave.instanceName]?.also { logger ->
                    logger.writeLine(currentTime)
                }
            }
        }
    }

    override fun close() {
        loggers.values.forEach { it.close() }
    }

    private inner class Logger(
        private val slave: SlaveComponent,
        variables: List<ScalarVariable>,
        private val decimationFactor: Int,
        staticFileNames: Boolean = false
    ) : Closeable {

        private val writer: BufferedWriter
        private val variables: List<ScalarVariable>

        init {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileName = if (staticFileNames) slave.instanceName else "${slave.instanceName}_$dateFormat"
            this.writer = FileOutputStream(File(targetDir, "${fileName}.csv")).bufferedWriter()
            this.variables = if (variables.isEmpty()) slave.modelVariables.toList() else variables
            this.variables.forEach {
                slave.markForReading(it.name)
            }
        }

        fun writeHeader() {
            variables.joinToString(separator, "time, stepNumber, ", "\n") {
                "${it.name} [${it.valueReference} ${it.type} ${it.causality}]"
            }.also {
                writer.write(it)
            }
        }

        @Suppress("IMPLICIT_CAST_TO_ANY")
        fun writeLine(currentTime: Double) {
            if (slave.stepCount % decimationFactor == 0L) {
                variables.map {
                    when (it.type) {
                        VariableType.INTEGER, VariableType.ENUMERATION -> slave.readInteger(it.valueReference).value
                        VariableType.REAL -> slave.readReal(it.valueReference).value.formatForOutput(decimalPoints)
                        VariableType.BOOLEAN -> slave.readBoolean(it.valueReference).value
                        VariableType.STRING -> slave.readString(it.valueReference).value
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
        }

    }

}

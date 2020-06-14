package no.ntnu.ihb.vico

import no.ntnu.ihb.acco.core.Engine
import no.ntnu.ihb.acco.core.Entity
import no.ntnu.ihb.fmi4j.readReal
import no.ntnu.ihb.vico.log.SlaveLoggerSystem
import no.ntnu.ihb.vico.master.FixedStepMaster
import no.ntnu.ihb.vico.model.ModelResolver
import no.ntnu.ihb.vico.ssp.SSPLoader
import no.ntnu.ihb.vico.structure.RealParameter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

internal class SlaveSystemTest {

    @Test
    fun slaveSystemTest() {

        Engine(1.0 / 100).use { engine ->

            val slaveSystem = SlaveSystem(FixedStepMaster())
            engine.addSystem(slaveSystem)

            val resultDir = File("build/results").also {
                it.deleteRecursively()
                engine.addSystem(SlaveLoggerSystem(null, it))
            }

            val slaveEntity = Entity("BouncingBall")
            val model = ModelResolver.resolve(TestFmus.get("1.0/BouncingBall.fmu"))
            SlaveComponent(model.instantiate("bouncingBall")).apply {
                addParameterSet("default", listOf(RealParameter("h", 2.0)))
                slaveEntity.addComponent(this)
            }
            engine.addEntity(slaveEntity)

            val slave = slaveSystem.getSlave("bouncingBall").apply {
                markForReading("h")
            }

            engine.init()

            Assertions.assertEquals(2.0, slave.readReal("h").value, 1e-6)
            engine.step(100)
            Assertions.assertTrue(slave.readReal("h").value > 0)

            Assertions.assertTrue(resultDir.listFiles()?.size ?: 0 > 0)

        }

    }

    @Test
    fun testSSP() {

        Engine(1.0 / 100).use { engine ->

            SSPLoader(TestSsp.get("ControlledDriveTrain.ssp")).load().apply(engine)
            val resultDir = File("build/results").also {
                it.deleteRecursively()
                engine.addSystem(SlaveLoggerSystem(null, it))
            }

            engine.step(100)

            Assertions.assertTrue(resultDir.listFiles()?.size ?: 0 > 0)

        }

    }

}

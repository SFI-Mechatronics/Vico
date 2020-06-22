package no.ntnu.ihb.acco.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SystemManagerTest {

    class ComponentA : Component()

    companion object {
        val family = Family.all(ComponentA::class.java).build()
    }

    abstract class TestSystem(
        family: Family
    ) : SimulationSystem(family)

    class SystemA : TestSystem(family) {

        init {
            priority = 1
        }

        override fun step(currentTime: Double, stepSize: Double) {
            println("A")
        }
    }

    class SystemB : TestSystem(family) {

        init {
            priority = 2
        }

        override fun step(currentTime: Double, stepSize: Double) {
            println("B")
        }
    }

    class SystemC : TestSystem(family) {

        init {
            priority = 3
        }

        override fun step(currentTime: Double, stepSize: Double) {
            println("C")
        }
    }

    @Test
    fun testSystemManager() {
        Engine().use { engine ->
            val manager = engine.systemManager

            val systems = listOf(
                SystemB(), SystemB(), SystemC()
            )
            systems.forEach { engine.addSystem(it) }

            assertEquals(3, manager.systems.size)
            assertEquals(systems.sorted(), manager.systems)

            engine.step(1)

        }
    }
}

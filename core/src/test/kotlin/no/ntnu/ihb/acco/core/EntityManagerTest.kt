package no.ntnu.ihb.acco.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class EntityManagerTest {

    @Test
    fun getByName() {
        Engine().use { engine ->
            val e1 = Entity("entity1")
            val e2 = Entity("entity2")
            engine.addEntity(e1, e2)
            assertSame(e1, engine.getEntityByName(e1.name))
            assertSame(e2, engine.getEntityByName(e2.name))
            assertThrows<IllegalArgumentException> { engine.getEntityByName("") }
        }
    }

    @Test
    fun getByTag() {
        Engine().use { engine ->
            val tag = Tag("aTag")
            val e1 = Entity("entity1").apply { this.tag = tag }
            val e2 = Entity("entity2").apply { this.tag = tag }
            engine.addEntity(e1, e2)
            assertEquals(listOf(e1, e2), engine.getEntityByTag(tag))
            assertTrue(engine.getEntityByTag("").isEmpty())
        }
    }

    @Test
    fun getFor() {

        class ComponentA : Component()
        class ComponentB : Component()
        class ComponentC : Component()


        Engine().use { engine ->

            Family.one(ComponentA::class.java, ComponentB::class.java).build().also { family ->
                Entity("e1").also { e ->
                    e.addComponent(ComponentA())
                    engine.addEntity(e)
                    assertEquals(1, engine.getEntitiesFor(family).size)
                }
                Entity("e2").also { e ->
                    e.addComponent(ComponentB())
                    engine.addEntity(e)
                    assertEquals(2, engine.getEntitiesFor(family).size)
                }
            }
            Family.all(ComponentC::class.java).build().also { family ->
                val entities = engine.getEntitiesFor(family)
                assertTrue(entities.isEmpty())
                Entity("e1").also { e ->
                    e.addComponent(ComponentC())
                    engine.addEntity(e)
                    assertTrue(entities.isNotEmpty())
                    engine.removeEntity(e)
                    assertTrue(entities.isEmpty())
                }

            }

        }
    }

}

package no.ntnu.ihb.acco.render.jme

import no.ntnu.ihb.acco.components.TransformComponent
import no.ntnu.ihb.acco.core.*
import no.ntnu.ihb.acco.render.Color
import no.ntnu.ihb.acco.render.GeometryComponent
import no.ntnu.ihb.acco.render.shape.BoxShape
import no.ntnu.ihb.acco.render.shape.SphereShape
import no.ntnu.ihb.acco.systems.IteratingSystem
import org.joml.Vector3d
import kotlin.math.PI
import kotlin.math.sin


private data class SineMoverComponent(
    var A: Double = 1.0,
    var f: Double = 0.1,
    var phi: Double = 0.0
) : Component {

    fun compute(t: Double) = A * sin(TWO_PHI * f * t + phi)

    private companion object {
        private const val TWO_PHI = 2 * PI
    }


}

private class SineMoverSystem : IteratingSystem(
    Family.all(TransformComponent::class.java, SineMoverComponent::class.java).build()
) {

    private val tmp = Vector3d()

    override fun processEntity(entity: Entity, currentTime: Double, stepSize: Double) {

        val tc = entity.transform
        val sc = entity.getComponent<SineMoverComponent>()
        tc.setLocalTranslation(tc.getLocalTranslation(tmp).apply { x = sc.compute(currentTime) })

    }

}

private fun e1(): Entity {
    return Entity("e1").also { e ->

        e.transform.setLocalTranslation(-1.0, 0.0, 0.0)

        e.addComponent(GeometryComponent(BoxShape()).apply {
            color.set(Color.green)
        })
        e.addComponent(SineMoverComponent())
    }
}

fun main() {

    Engine().also { engine ->

        val e1 = e1()
        engine.addEntity(e1)
        Entity("e2").also { e ->

            e.transform.setLocalTranslation(1.0, 0.0, 0.0)

            e.addComponent(GeometryComponent(SphereShape()).apply {
                color.set(Color.yellow)
            })
            e.addComponent(SineMoverComponent(f = 0.5))
            e1.addEntity(e)
            engine.getEntityByName("e1")
        }

        engine.addSystem(SineMoverSystem())
        engine.addSystem(JmeRenderSystem())

        EngineRunner(engine).apply {
            start()
        }

        Thread.sleep(1000)
        engine.getEntityByName("e1").getComponent<GeometryComponent>().visible = false
        engine.getEntityByName("e1.e2").getComponent<GeometryComponent>().wireframe = true
        Thread.sleep(1000)
        engine.getEntityByName("e1").getComponent<GeometryComponent>().visible = true
        engine.getEntityByName("e1.e2").getComponent<GeometryComponent>().wireframe = false

    }

}

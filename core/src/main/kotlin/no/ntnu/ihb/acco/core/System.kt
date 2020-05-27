package no.ntnu.ihb.acco.core

import java.io.Closeable

abstract class System(
    private val family: Family,
    val decimationFactor: Int = 1,
    val priority: Int = 0
) : Comparable<System>, Closeable {

    private var _engine: Engine? = null
    protected val engine: Engine
        get() = _engine ?: throw IllegalStateException("System is not affiliated with an Engine!")

    var enabled = true

    val interval: Double
        get() = engine.baseStepSize * decimationFactor

    protected lateinit var entities: Set<Entity>

    fun addedToEngine(engine: Engine) {
        this._engine = engine
        entities = engine.entityManager.getEntitiesFor(family).apply {
            addObserver = {
                entityAdded(it)
            }
            removeObserver = {
                entityRemoved(it)
            }
            forEach { entityAdded(it) }
        }
    }

    internal fun initialize(currentTime: Double) {
        init(currentTime)
    }

    protected open fun init(currentTime: Double) {}

    protected open fun entityAdded(entity: Entity) {}

    protected open fun entityRemoved(entity: Entity) {}

    abstract fun step(currentTime: Double, stepSize: Double)

    override fun close() {}

    override fun compareTo(other: System): Int {
        val compare = other.decimationFactor.compareTo(decimationFactor)
        return if (compare == 0) priority.compareTo(other.priority) else compare
    }

}

abstract class IteratingSystem(
    family: Family,
    decimationFactor: Int = 1,
    priority: Int = 0
) : System(family, decimationFactor, priority) {

    override fun step(currentTime: Double, stepSize: Double) {
        entities.forEach { e ->
            processEntity(e, currentTime, stepSize)
        }
    }

    protected abstract fun processEntity(entity: Entity, currentTime: Double, stepSize: Double)

}

abstract class ParallelIteratingSystem(
    family: Family,
    decimationFactor: Int = 1,
    priority: Int = 0
) : System(family, decimationFactor, priority) {

    override fun step(currentTime: Double, stepSize: Double) {
        entities.parallelStream().forEach { e ->
            processEntity(e, currentTime, stepSize)
        }
    }

    protected abstract fun processEntity(entity: Entity, currentTime: Double, stepSize: Double)
}

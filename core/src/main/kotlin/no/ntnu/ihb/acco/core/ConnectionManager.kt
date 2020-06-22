package no.ntnu.ihb.acco.core

class ConnectionManager {

    private val connections: MutableMap<Component, MutableList<Connection>> = mutableMapOf()

    fun update() {
        forEachConnection { c ->
            c.transferData()
        }
    }

    fun updateConnection(key: Component) {
        connections[key]?.forEach { it.transferData() }
    }

    fun addConnection(connection: Connection) {
        connections.computeIfAbsent(connection.source) {
            mutableListOf()
        }.add(connection)
    }

    fun onComponentRemoved(component: Component) {
        for (connection in connections.values.flatten()) {
            if (connection.source == component) {
                connections.remove(connection.source)
            }
            for (targetComponent in connection.targets) {
                if (targetComponent == component) {
                    connections.remove(connection.source)
                }
            }
        }
    }

    private inline fun forEachConnection(f: (Connection) -> Unit) {
        connections.values.forEach { list ->
            list.forEach {
                f.invoke(it)
            }
        }
    }

}

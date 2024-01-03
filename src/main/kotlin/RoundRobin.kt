package org.example

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class RoundRobin(
    private val servers: List<Server>,
    private val healthCheckPeriodInSeconds: Int,
) : LoadBalancingMethod {
    private val ioScope = CoroutineScope(Dispatchers.IO) + SupervisorJob()
    private val serverIndex = AtomicInteger(0)

    fun startHealthCheck() {
        ioScope.launch {
            while (true) {
                servers.forEach { server ->
                    launch {
                        server.ping()
                    }
                }
                delay(healthCheckPeriodInSeconds.toDuration(DurationUnit.SECONDS))
            }
        }
    }

    override fun getServer(): Server {
        var server = getNextServer()
        while (!server.isHealthy) {
            println("$server is not healthy, trying another")
            server = getNextServer()
        }
        return server
    }

    private fun getNextServer(): Server {
        val serverIndexToUse = serverIndex.getAndUpdate { index ->
            if (index == servers.size - 1) {
                0
            } else {
                index + 1
            }
        }

        return servers[serverIndexToUse]
    }
}
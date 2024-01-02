package org.example

import java.util.concurrent.atomic.AtomicInteger

class RoundRobin(
    private val servers: List<Server>,
) : LoadBalancingMethod {
    private val serverIndex = AtomicInteger(0)

    override fun getServer(): Server {
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
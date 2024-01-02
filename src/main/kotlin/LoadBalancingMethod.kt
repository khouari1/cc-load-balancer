package org.example

interface LoadBalancingMethod {
    fun getServer(): Server
}

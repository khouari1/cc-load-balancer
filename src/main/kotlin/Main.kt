package org.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket

fun main(args: Array<String>) = runBlocking {
    if (args.isEmpty()) {
        throw IllegalArgumentException("Expected servers to be provided")
    }
    val configuration = getConfiguration(args)
    val serverSocket = ServerSocket(9999)
    val loadBalancer = createLoadBalancer(configuration)
    loadBalancer.startHealthCheck()

    while (true) {
        val accept = serverSocket.accept()
        launch(Dispatchers.Default) {
            accept.use { socket ->
                socket.getReader().use { reader ->
                    val clientRequest = mutableListOf<String>()
                    var line = reader.readLine()
                    while (line != "") {
                        clientRequest.add(line)
                        line = reader.readLine()
                    }
                    println("Received request...")
                    println(clientRequest.joinToString(separator = "\n"))

                    val server = loadBalancer.getServer()
                    val destinationResponse = server.send(clientRequest)
                    socket.getWriter().use { writer ->
                        destinationResponse.forEach { line ->
                            writer.write("$line\n")
                        }
                        writer.write("\r\n")
                    }
                }
            }
        }
    }
}

fun getConfiguration(args: Array<String>): Configuration {
    val servers = getServers(args)
    val healthCheckPeriodInSeconds = if (args.size > 1) {
        args[1].toInt()
    } else {
        10
    }
    return Configuration(servers, healthCheckPeriodInSeconds)
}

data class Configuration(
    val servers: List<Server>,
    val healthCheckPeriodInSeconds: Int = 10,
)

private fun getServers(args: Array<String>) = args.first().split(",").map { server ->
    val (host, port) = server.split(":(?=\\d)".toRegex())
    Server(host, port.toInt())
}

private fun createLoadBalancer(configuration: Configuration) =
    RoundRobin(configuration.servers, configuration.healthCheckPeriodInSeconds)

fun Socket.getWriter() = BufferedWriter(OutputStreamWriter(getOutputStream()))
fun Socket.getReader() = BufferedReader(InputStreamReader(getInputStream()))
package org.example

import java.io.IOException
import java.net.Socket

typealias Request = List<String>
typealias Response = List<String>

data class Server(
    val host: String,
    val port: Int,
) {
    var isHealthy: Boolean = false

    fun ping() {
        println("Pinging $this")
        val response = send(listOf("GET / HTTP/1.1", "Host: localhost"))
        if (response.isEmpty()) {
            isHealthy = false
        } else {
            val (_, status, _) = response.first().split(" ")
            println("Server: $host:$port Status = $status")
            isHealthy = status.toInt() == 200
        }
    }

    fun send(content: Request): Response {
        return try {
            Socket(host, port).use { destinationSocket ->
                val destinationOutput = destinationSocket.getWriter()
                destinationOutput.use { output ->
                    val destinationInput = destinationSocket.getReader()
                    content.forEach { line ->
                        output.write("$line\r\n")
                    }
                    output.write("\r\n")
                    output.flush()

                    destinationInput.use { input ->
                        val destinationResponse = mutableListOf<String>()
                        var inputLine = input.readLine()
                        while (inputLine != null) {
                            destinationResponse.add(inputLine)
                            inputLine = input.readLine()
                        }
                        destinationResponse
                    }
                }
            }
        } catch (e: IOException) {
            println("Error ${e.message}")
            emptyList()
        }
    }

    override fun toString(): String {
        return "Server(host='$host', port=$port)"
    }
}

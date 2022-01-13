package org.pedrofelix.pc.coroutines

import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

fun main() {
    val server = ServerUsingThreads()
    val th = server.start();
    th.join()
}

class ServerUsingThreads {

    fun start(): Thread {
        val th = Thread { serverLoop() }
        th.start()
        return th
    }

    private fun serverLoop() {
        val serverSocket = ServerSocket()
        serverSocket.bind(InetSocketAddress("0.0.0.0", 8080))
        var clientId = 0
        while (true) {
            val socket: Socket = serverSocket.accept()
            logger.info("Socket accepted")
            Thread {
                clientLoop(socket, ++clientId)
            }.start()
        }
    }

    private fun clientLoop(socket: Socket, clientId: Int) {

        try {
            logger.info("{}: Starting client", clientId)
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()
            val buffer = ByteArray(2)
            while (true) {
                logger.info("{}: reading", clientId)
                val readLen = inputStream.read(buffer)
                logger.info("{}: read {} bytes", clientId, readLen)
                if (readLen == -1) {
                    break
                }
                logger.info("{}: writing", clientId)
                outputStream.write(buffer, 0, readLen)
            }
        } catch (ex: Exception) {
            logger.error("{}: exception {}", clientId, ex.message)
        } finally {
            logger.info("{}: ending", clientId)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServerUsingThreads::class.java)
    }
}
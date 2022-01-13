package org.pedrofelix.pc.coroutines

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.nio.channels.CompletionHandler
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val dispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

val singleThreadDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()

val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

suspend fun myDelay(ms: Long): Unit {

    logger.info("Before suspendCoroutine")
    try {
        suspendCancellableCoroutine<Unit> { continuation ->
            logger.info("Before schedule")
            val future = scheduledExecutor.schedule(
                {
                    // callback/Runnable called after the time elapses
                    // call the continuation
                    logger.info("schedule callback called")
                    continuation.resume(Unit)

                },
                ms, TimeUnit.MILLISECONDS
            )
        }
    }catch(ex: CancellationException) {
        logger.info("CancellationException")
        throw ex
    }
    logger.info("After suspendCoroutine")
}


fun main() {

    runBlocking {
        supervisorScope {
            val job = launch {
                logger.info("Before myDelay")
                myDelay(1000)
                logger.info("After myDelay")
            }

            launch {
                while (!job.isCompleted) {
                    logger.info("active={}, cancelled={}, completed={}", job.isActive, job.isCancelled, job.isCompleted)
                    delay(50)
                }
            }
            delay(500)
            job.cancel()
        }
    }
}

class ServerUsingCoroutines {

    suspend fun serverLoop() {
        // NIO2 (New IO 2)
        val serverSocket = AsynchronousServerSocketChannel.open()
        serverSocket.bind(InetSocketAddress("0.0.0.0", 8080))
        var clientId = 0
        // Structured Concurrency
        coroutineScope {
            try {
                while (true) {
                    val socket = serverSocket.acceptAsync()
                    delay(1)
                    logger.info("client socket accepted")
                    launch {
                        clientLoop(socket, ++clientId)
                    }
                }
            } finally {
                logger.info("accept loop ending")
            }
        }
        logger.info("after coroutineScope")
    }

    private suspend fun clientLoop(socket: AsynchronousSocketChannel, clientId: Int) {
        try {
            logger.info("{}: Starting client", clientId)
            val buffer = ByteBuffer.allocate(2)
            while (true) {
                logger.info("{}: reading", clientId)
                val readLen = socket.readAsync(buffer)
                logger.info("{}: read {} bytes", clientId, readLen)
                if (readLen == -1) {
                    break
                }
                buffer.flip()
                logger.info("{}: writing", clientId)
                socket.writeAllAsync(buffer)
                buffer.clear()
            }
        } catch (ex: AsynchronousCloseException) {
            logger.info("{}: socket closed, ending client", clientId)
        } catch (ex: CancellationException) {
            logger.info("Cancellation detected")
        } finally {
            logger.info("{}: ending", clientId)
            socket.close()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServerUsingCoroutines::class.java)
    }

}

suspend fun AsynchronousServerSocketChannel.acceptAsync() =
    suspendCoroutine<AsynchronousSocketChannel> { continuation ->

        this.accept(Unit, object : CompletionHandler<AsynchronousSocketChannel, Unit> {
            override fun completed(channel: AsynchronousSocketChannel, attachment: Unit) {
                continuation.resume(channel)
            }

            override fun failed(exception: Throwable, attachment: Unit) {
                continuation.resumeWithException(exception)
            }
        })
    }


private val logger = LoggerFactory.getLogger("Utils")

suspend fun AsynchronousByteChannel.readAsync(buffer: ByteBuffer) =
    suspendCoroutine<Int> { continuation ->
        this.read(buffer, Unit, object : CompletionHandler<Int, Unit> {
            override fun completed(result: Int, attachment: Unit) {
                continuation.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Unit) {
                continuation.resumeWithException(exc)
            }
        })
    }

suspend fun AsynchronousByteChannel.writeAllAsync(buffer: ByteBuffer) {
    do {
        this.writeAsync(buffer)
    } while (buffer.remaining() != 0)
}

suspend fun AsynchronousByteChannel.writeAsync(buffer: ByteBuffer) =
    suspendCoroutine<Int> { continuation ->
        this.write(buffer, Unit, object : CompletionHandler<Int, Unit> {

            override fun completed(result: Int, attachment: Unit) {
                continuation.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Unit) {
                continuation.resumeWithException(exc)
            }
        })
    }
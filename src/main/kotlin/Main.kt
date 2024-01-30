package dev.verbosemode

import com.google.api.gax.batching.BatchingSettings
import com.google.api.gax.batching.FlowControlSettings
import com.google.api.gax.batching.FlowController
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import org.threeten.bp.Duration
import java.util.concurrent.CompletableFuture
import kotlin.time.measureTime


private val GCP_PROJECT = System.getenv("GCP_PROJECT")
private const val GCP_TOPIC = "test-topic"

private val delayTreshold = listOf(10, 100L, 1000L)
private val elementCountThreshold = listOf(5L, 10L, 50L)
private val requestByteThreshold = listOf(1024L, 2048, 4096L)
val messagesToSend = listOf(1000)
const val testRuns = 25
const val pauseBetweenRuns = 2000L

data class BatchSettings(val delayTreshold: Long, val elementCountThreshold: Long, val requestByteThreshold: Long)

fun generateBatchSettings(): List<BatchSettings> {
    return delayTreshold.flatMap { delay ->
        elementCountThreshold.flatMap { elementCount ->
            requestByteThreshold.map { requestByte ->
                BatchSettings(delay, elementCount, requestByte)
            }
        }
    }
}

fun main() {

    measureTime {
        val bs = generateBatchSettings().also { println(it.size * testRuns * messagesToSend.size) }
        var cnt = 4
        println("| Row |BatchSettings | Messages | Median Time (ms) | Messages per Second | Time to Publish One Million")
        println("| --- | ---| --- | --- | --- | --- | --- | --- |")
        experiment(batchSettings = null).statsAsMarkdownTable(cnt++, null)
        bs.forEach {
            experiment(batchSettings = it).statsAsMarkdownTable(cnt++, it)
        }
    }.also { println("Total time: $it") }


}

private fun experiment(batchSettings: BatchSettings?): List<Pair<Int, List<kotlin.time.Duration>>> {


    val r1 = messagesToSend.map { msgAmnt ->

        msgAmnt to (1..testRuns).map { run ->
            val publisher = publisher(batchSettings)
            measureTime {
                val futures = publishMessages(msgAmnt, publisher)
                CompletableFuture.allOf(*futures.toTypedArray<CompletableFuture<Void>>()).get()
                publisher.shutdown()

            }
        }
    }

    Thread.sleep(pauseBetweenRuns)
    return r1


}

private fun publishMessages(
    msgAmnt: Int,
    publisher: Publisher
) = (1..msgAmnt).map {
    simulateWaiting()
    CompletableFuture.runAsync { publisher.publish(PubsubMessage.newBuilder().setData(data).build()).get() }
}

private fun publisher(batching: BatchSettings?): Publisher {
    val builder = Publisher.newBuilder("projects/$GCP_PROJECT/topics/$GCP_TOPIC")
    if (batching != null) {
        val batchingSettings = BatchingSettings.newBuilder()
            .setIsEnabled(true)
            .setDelayThreshold(Duration.ofMillis(batching.delayTreshold))
            .setElementCountThreshold(batching.elementCountThreshold)
            .setRequestByteThreshold(batching.requestByteThreshold)
            .setFlowControlSettings(
                FlowControlSettings.newBuilder().setMaxOutstandingRequestBytes(10000000)
                    .setMaxOutstandingElementCount(10000)
                    .setLimitExceededBehavior(FlowController.LimitExceededBehavior.Block).build()
            )
            .build()
        builder.setBatchingSettings(batchingSettings)
    }
    return builder.build()
}


private const val EVENT = """{ "id" : "1234", "name" : "test" }"""
private val data = ByteString.copyFromUtf8(EVENT)
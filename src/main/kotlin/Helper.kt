package dev.verbosemode

import kotlin.random.Random
import kotlin.text.Typography.times
import kotlin.time.Duration

fun timeToProcessOneMillionMessages(amount: Int, times: List<Duration>): Duration {
    val median = times.median()
    return ((median * 1000000) / amount)
}

fun simulateWaiting() {

    val rand = Random.nextDouble()
    val toLong = when {
        rand < 0.6 -> 0L
        rand < 0.9 -> 1L
        rand < 0.99 -> 2L
        else -> 5L
    }
    Thread.sleep(toLong)
}

fun List<Duration>.median(): Duration {
    return sorted().let { sorted ->
        val middle = sorted.size / 2
        if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2
        } else {
            sorted[middle]
        }
    }
}

fun List<Pair<Int, List<Duration>>>.statsAsMarkdownTable(row:Int,batchSettings: BatchSettings?) {

    forEach { (amount, times) ->
        val median = times.median()
        val itemsPerSecond = (amount * 1000 / median.inWholeMilliseconds)
        val timeToProcessOneMillionMessages = timeToProcessOneMillionMessages(amount, times)
        println(
            "| $row | $batchSettings | $amount |${median.inWholeMilliseconds} | $itemsPerSecond | $timeToProcessOneMillionMessages",
        )
    }
}
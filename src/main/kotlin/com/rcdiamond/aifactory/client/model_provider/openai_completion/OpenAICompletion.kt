package com.rcdiamond.aifactory.client.model_provider.openai_completion

import com.rcdiamond.aifactory.client.llm.AssistantEvent
import com.rcdiamond.aifactory.client.llm.LLMAPIError
import com.rcdiamond.aifactory.client.llm.LLMTransportError
import com.rcdiamond.aifactory.client.llm.GenerateResponse
import com.rcdiamond.aifactory.client.llm.ModelProvider
import com.rcdiamond.aifactory.client.llm.LLMContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.Flow as HttpFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** OpenAI-compatible Chat Completions. No retries or automatic tool execution. */
class OpenAICompletion(
    baseUrl: String,
    private val apiKey: String,
    private val model: String,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build(),
    private val requestTimeout: Duration = Duration.ofSeconds(120),
) : ModelProvider {
    private val endpoint: URI

    init {
        val base = URI(baseUrl)
        require(base.scheme == "http" || base.scheme == "https") { "baseUrl must use HTTP or HTTPS" }
        require(base.host != null && base.rawUserInfo == null && base.rawQuery == null && base.rawFragment == null) {
            "baseUrl must be an API root without credentials, query or fragment"
        }
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        require(model.isNotBlank()) { "model must not be blank" }
        require(!requestTimeout.isNegative && !requestTimeout.isZero) { "requestTimeout must be positive" }
        endpoint = URI(baseUrl.trimEnd('/') + "/chat/completions")
    }

    override suspend fun generate(prompt: LLMContext): GenerateResponse {
        val request = HttpRequest.newBuilder(endpoint)
            .timeout(requestTimeout)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(OpenAICompletionCodec.encode(model, prompt)))
            .build()
        val response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        if (response.statusCode() !in 200..299) {
            // Do not expose response bodies: upstream errors can echo secrets or prompt contents.
            throw OpenAIHttpException(response.statusCode())
        }
        return OpenAICompletionCodec.decode(response.body())
    }

    override suspend fun generate_stream(prompt: LLMContext): Result<Flow<Result<AssistantEvent>>> {
        val request = try {
            HttpRequest.newBuilder(endpoint)
                .timeout(requestTimeout)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(OpenAICompletionCodec.encode(model, prompt, stream = true)))
                .build()
        } catch (_: IllegalArgumentException) {
            return Result.failure(OpenAIProtocolException("Invalid streaming request"))
        }
        val events = flow<Result<AssistantEvent>> {
            val subscriber = StreamingBodySubscriber()
            try {
                val response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofPublisher()).await()
                // Subscribe even to error responses so cancellation releases the response body.
                if (response.statusCode() !in 200..299) {
                    subscriber.cancel()
                    response.body().subscribe(subscriber)
                    throw OpenAIHttpException(response.statusCode())
                }
                response.body().subscribe(subscriber)
                val contentType = response.headers().firstValue("Content-Type").orElse("")
                if (!contentType.substringBefore(';').trim().equals("text/event-stream", ignoreCase = true)) {
                    throw OpenAIProtocolException("Streaming response must use text/event-stream")
                }
                val framing = ServerSentEvents()
                val decoder = OpenAICompletionStreamDecoder()
                var done = false
                for (buffers in subscriber.chunks) {
                    for (buffer in buffers) {
                        while (buffer.hasRemaining()) {
                            val data = framing.accept(buffer.get()) ?: continue
                            if (data.trim() == "[DONE]") {
                                decoder.finish()
                                done = true
                                break
                            }
                            for (event in decoder.decode(data)) emit(Result.success(event))
                        }
                        if (done) break
                    }
                    if (done) break
                    subscriber.requestNext()
                }
                if (!done) throw OpenAIProtocolException("Streaming response ended before [DONE]")
            } finally {
                subscriber.cancel()
            }
        }.catch { failure ->
            when (failure) {
                is CancellationException -> throw failure
                is LLMAPIError -> emit(Result.failure(failure))
                is IOException -> emit(Result.failure(LLMTransportError()))
                else -> throw failure
            }
        }
        return Result.success(events)
    }
}

class OpenAIHttpException(val statusCode: Int) : LLMAPIError("OpenAI Chat Completions returned HTTP $statusCode")

class OpenAIProtocolException(message: String) : LLMAPIError(message)

/** One outstanding publisher batch; collecting a Flow suspends without blocking a socket thread. */
private class StreamingBodySubscriber : HttpFlow.Subscriber<List<ByteBuffer>> {
    val chunks = Channel<List<ByteBuffer>>(1)
    private val subscription = AtomicReference<HttpFlow.Subscription?>()
    private val cancelled = AtomicBoolean()

    override fun onSubscribe(value: HttpFlow.Subscription) {
        if (!subscription.compareAndSet(null, value) || cancelled.get()) {
            value.cancel()
        } else {
            value.request(1)
        }
    }

    override fun onNext(item: List<ByteBuffer>) {
        if (chunks.trySend(item).isFailure) {
            cancel()
        }
    }

    override fun onError(throwable: Throwable) {
        chunks.close(LLMTransportError())
    }

    override fun onComplete() {
        chunks.close()
    }

    fun requestNext() {
        subscription.get()?.request(1)
    }

    fun cancel() {
        cancelled.set(true)
        subscription.get()?.cancel()
        chunks.cancel()
    }
}

/** SSE byte framing preserves UTF-8 across HTTP batches and supports LF, CRLF and CR. */
private class ServerSentEvents {
    private val line = ByteArrayOutputStream()
    private val data = StringBuilder()
    private var hasData = false
    private var afterCr = false
    private var firstLine = true

    fun accept(byte: Byte): String? {
        val value = byte.toInt() and 0xff
        if (afterCr) {
            afterCr = false
            if (value == 10) return null
        }
        if (value != 10 && value != 13) {
            line.write(value)
            return null
        }
        afterCr = value == 13
        var text = line.toString(StandardCharsets.UTF_8)
        line.reset()
        if (firstLine) {
            text = text.removePrefix("\uFEFF")
            firstLine = false
        }
        if (text.isEmpty()) {
            if (!hasData) return null
            val event = data.toString()
            data.setLength(0)
            hasData = false
            return event
        }
        val colon = text.indexOf(':')
        val field = if (colon < 0) text else text.substring(0, colon)
        if (field == "data") {
            if (hasData) data.append('\n')
            data.append(if (colon < 0) "" else text.substring(colon + 1).removePrefix(" "))
            hasData = true
        }
        return null
    }
}

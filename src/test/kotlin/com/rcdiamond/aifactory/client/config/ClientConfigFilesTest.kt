package com.rcdiamond.aifactory.client.config

import com.rcdiamond.aifactory.client.llm.AgentSettings
import com.rcdiamond.aifactory.client.llm.ModelSettings
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientConfigFilesTest {
    @Test
    fun `missing files are created as commented starters and the blank key is reported`(@TempDir directory: Path) {
        val failure = assertFailsWith<ClientConfigurationException> { ClientConfigFiles.load(directory) }

        assertContains(failure.message.orEmpty(), ClientConfigFiles.MODEL_FILE_NAME)
        assertContains(failure.message.orEmpty(), "'api_key'")

        assertTrue(Files.exists(directory.resolve(ClientConfigFiles.MODEL_FILE_NAME)))
        assertTrue(Files.exists(directory.resolve(ClientConfigFiles.AGENT_FILE_NAME)))
    }

    @Test
    fun `a partially existing directory keeps its file and creates only the missing one`(@TempDir directory: Path) {
        writeModel(directory, validModel(apiKey = "key-abc", model = "custom-model"))
        val modelBefore = Files.readAllBytes(directory.resolve(ClientConfigFiles.MODEL_FILE_NAME))

        val configuration = ClientConfigFiles.load(directory)

        assertEquals("custom-model", configuration.model.model)
        assertEquals("key-abc", configuration.model.apiKey)
        assertContentEquals(modelBefore, Files.readAllBytes(directory.resolve(ClientConfigFiles.MODEL_FILE_NAME)))
        assertTrue(Files.exists(directory.resolve(ClientConfigFiles.AGENT_FILE_NAME)))
    }

    @Test
    fun `an existing configuration is returned typed and left byte for byte unchanged`(@TempDir directory: Path) {
        writeModel(directory, validModel(apiKey = "key-xyz", baseUrl = "https://example.invalid/v1", model = "custom-model"))
        writeAgent(directory, agentConfig(prompt = "\"be brief\"", maxSteps = "3", requestTimeout = "30", toolTimeout = "5"))
        val modelBefore = Files.readAllBytes(directory.resolve(ClientConfigFiles.MODEL_FILE_NAME))
        val agentBefore = Files.readAllBytes(directory.resolve(ClientConfigFiles.AGENT_FILE_NAME))

        val configuration = ClientConfigFiles.load(directory)

        assertEquals(
            ModelSettings(baseUrl = "https://example.invalid/v1", apiKey = "key-xyz", model = "custom-model"),
            configuration.model,
        )
        assertEquals(
            AgentSettings(
                systemPrompt = "be brief",
                maxSteps = 3,
                requestTimeoutSeconds = 30,
                toolTimeoutSeconds = 5,
            ),
            configuration.agent,
        )
        assertContentEquals(modelBefore, Files.readAllBytes(directory.resolve(ClientConfigFiles.MODEL_FILE_NAME)))
        assertContentEquals(agentBefore, Files.readAllBytes(directory.resolve(ClientConfigFiles.AGENT_FILE_NAME)))
    }

    @Test
    fun `range boundaries are inclusive`(@TempDir directory: Path) {
        val lowest = loadAgent(directory, maxSteps = "1", requestTimeout = "1", toolTimeout = "1")
        assertEquals(1, lowest.maxSteps)
        assertEquals(1L, lowest.requestTimeoutSeconds)
        assertEquals(1L, lowest.toolTimeoutSeconds)

        val highest = loadAgent(directory, maxSteps = "32", requestTimeout = "600", toolTimeout = "120")
        assertEquals(32, highest.maxSteps)
        assertEquals(600L, highest.requestTimeoutSeconds)
        assertEquals(120L, highest.toolTimeoutSeconds)
    }

    @Test
    fun `a blank api key is rejected with an actionable message`(@TempDir directory: Path) {
        writeModel(directory, validModel(apiKey = "   "))
        writeAgent(directory, agentConfig())

        val failure = assertFailsWith<ClientConfigurationException> { ClientConfigFiles.load(directory) }
        val message = failure.message.orEmpty()

        assertContains(message, ClientConfigFiles.MODEL_FILE_NAME)
        assertContains(message, "'api_key'")
        assertNull(failure.cause)
    }

    @Test
    fun `a missing key is reported with its file and name`(@TempDir directory: Path) {
        writeModel(directory, "base_url = \"https://example.invalid/v1\"\napi_key = \"key-xyz\"\n")
        writeAgent(directory, agentConfig())

        val failure = assertFailsWith<ClientConfigurationException> { ClientConfigFiles.load(directory) }

        assertContains(failure.message.orEmpty(), ClientConfigFiles.MODEL_FILE_NAME)
        assertContains(failure.message.orEmpty(), "'model'")
    }

    @ParameterizedTest(name = "{1} is rejected")
    @MethodSource("invalidAgentValues")
    fun `wrong value types and out-of-range numbers are rejected`(agentContent: String, key: String, @TempDir directory: Path) {
        writeModel(directory, validModel(apiKey = "key-xyz"))
        writeAgent(directory, agentContent)

        val failure = assertFailsWith<ClientConfigurationException> { ClientConfigFiles.load(directory) }

        assertContains(failure.message.orEmpty(), ClientConfigFiles.AGENT_FILE_NAME)
        assertContains(failure.message.orEmpty(), "'$key'")
        assertNull(failure.cause)
    }

    @Test
    fun `a parse failure names only the file and never chains the toml exception`(@TempDir directory: Path) {
        writeModel(
            directory,
            "api_key = \"sk-live-super-secret\"\nbase_url = \"https://example.invalid/v1\n",
        )

        val failure = assertFailsWith<ClientConfigurationException> { ClientConfigFiles.load(directory) }

        assertContains(failure.message.orEmpty(), ClientConfigFiles.MODEL_FILE_NAME)
        assertFalse(failure.message.orEmpty().contains("sk-live-super-secret"))
        assertNull(failure.cause)
    }

    @Test
    fun `a wrong typed value never echoes the secret it contains`(@TempDir directory: Path) {
        writeModel(directory, validModel(apiKey = "key-xyz"))
        writeAgent(directory, agentConfig(maxSteps = "\"sk-live-super-secret\""))

        val failure = assertFailsWith<ClientConfigurationException> { ClientConfigFiles.load(directory) }

        assertContains(failure.message.orEmpty(), "'max_steps'")
        assertFalse(failure.message.orEmpty().contains("sk-live-super-secret"))
        assertNull(failure.cause)
    }

    companion object {
        @JvmStatic
        fun invalidAgentValues(): Stream<Arguments> = Stream.of(
            Arguments.of(agentConfig(maxSteps = "40"), "max_steps"),
            Arguments.of(agentConfig(maxSteps = "0"), "max_steps"),
            Arguments.of(agentConfig(maxSteps = "\"eight\""), "max_steps"),
            Arguments.of(agentConfig(maxSteps = "1.5"), "max_steps"),
            Arguments.of(agentConfig(maxSteps = "true"), "max_steps"),
            Arguments.of(agentConfig(requestTimeout = "601"), "request_timeout_seconds"),
            Arguments.of(agentConfig(requestTimeout = "0"), "request_timeout_seconds"),
            Arguments.of(agentConfig(toolTimeout = "121"), "tool_timeout_seconds"),
            Arguments.of(agentConfig(toolTimeout = "0"), "tool_timeout_seconds"),
            Arguments.of(agentConfig(prompt = "\"\""), "system_prompt"),
            Arguments.of(agentConfig(prompt = "42"), "system_prompt"),
        )
    }
}

private fun loadAgent(directory: Path, maxSteps: String, requestTimeout: String, toolTimeout: String): AgentSettings {
    writeModel(directory, validModel(apiKey = "key-xyz"))
    writeAgent(directory, agentConfig(maxSteps = maxSteps, requestTimeout = requestTimeout, toolTimeout = toolTimeout))
    return ClientConfigFiles.load(directory).agent
}

private fun writeModel(directory: Path, content: String) {
    Files.writeString(directory.resolve(ClientConfigFiles.MODEL_FILE_NAME), content)
}

private fun writeAgent(directory: Path, content: String) {
    Files.writeString(directory.resolve(ClientConfigFiles.AGENT_FILE_NAME), content)
}

private fun validModel(
    apiKey: String = "key-xyz",
    baseUrl: String = "https://example.invalid/v1",
    model: String = "custom-model",
): String = "base_url = \"$baseUrl\"\napi_key = \"$apiKey\"\nmodel = \"$model\"\n"

private fun agentConfig(
    prompt: String = "\"be brief\"",
    maxSteps: String = "8",
    requestTimeout: String = "30",
    toolTimeout: String = "5",
): String = buildString {
    appendLine("system_prompt = $prompt")
    appendLine("max_steps = $maxSteps")
    appendLine("request_timeout_seconds = $requestTimeout")
    appendLine("tool_timeout_seconds = $toolTimeout")
}

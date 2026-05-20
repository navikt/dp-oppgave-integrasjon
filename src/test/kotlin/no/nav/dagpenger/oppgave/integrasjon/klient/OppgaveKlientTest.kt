package no.nav.dagpenger.oppgave.integrasjon.klient

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson3.jackson
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class OppgaveKlientTest {
    private val baseUrl = "http://oppgave.test"

    @Test
    fun `skal tagge oppgave med DP-sak naar ingen nokkelord finnes`() {
        val requests = mutableListOf<Pair<HttpMethod, String>>()
        val mockEngine =
            MockEngine { request ->
                requests.add(request.method to request.url.toString())
                when (request.method) {
                    HttpMethod.Get ->
                        respond(
                            content = """{"id": 123, "versjon": 1, "nokkelord": []}""",
                            headers = jsonHeaders(),
                        )
                    HttpMethod.Patch ->
                        respond(
                            content = """{"id": 123, "versjon": 2, "nokkelord": ["DP-sak"]}""",
                            headers = jsonHeaders(),
                        )
                    else -> error("Unexpected request: ${request.method}")
                }
            }

        val klient = OppgaveKlient(baseUrl = baseUrl, httpClient = httpClient(mockEngine))
        val tagget = runBlocking { klient.taggMedDpSak(123) }

        tagget shouldBe true
        requests.size shouldBe 2
        requests[0] shouldBe (HttpMethod.Get to "$baseUrl/api/v2/oppgaver/123")
        requests[1].first shouldBe HttpMethod.Patch
    }

    @Test
    fun `skal returnere false naar DP-sak allerede finnes`() {
        val mockEngine =
            MockEngine { request ->
                respond(
                    content = """{"id": 456, "versjon": 3, "nokkelord": ["DP-sak"]}""",
                    headers = jsonHeaders(),
                )
            }

        val klient = OppgaveKlient(baseUrl = baseUrl, httpClient = httpClient(mockEngine))
        val tagget = runBlocking { klient.taggMedDpSak(456) }

        tagget shouldBe false
    }

    @Test
    fun `skal kaste exception naar oppgaven allerede har 2 nokkelord`() {
        val mockEngine =
            MockEngine { request ->
                respond(
                    content = """{"id": 789, "versjon": 2, "nokkelord": ["Foo", "Bar"]}""",
                    headers = jsonHeaders(),
                )
            }

        val klient = OppgaveKlient(baseUrl = baseUrl, httpClient = httpClient(mockEngine))
        val exception = shouldThrow<IllegalStateException> { runBlocking { klient.taggMedDpSak(789) } }

        exception.message shouldBe "Oppgave 789 har allerede 2 nøkkelord, kan ikke tagge med DP-sak"
    }

    @Test
    fun `skal retry ved 409 Conflict og lykkes paa andre forsoek`() {
        var patchCount = 0
        val mockEngine =
            MockEngine { request ->
                when (request.method) {
                    HttpMethod.Get ->
                        respond(
                            content = """{"id": 100, "versjon": ${1 + patchCount}, "nokkelord": []}""",
                            headers = jsonHeaders(),
                        )
                    HttpMethod.Patch -> {
                        patchCount++
                        if (patchCount == 1) {
                            respond(content = "", status = HttpStatusCode.Conflict)
                        } else {
                            respond(
                                content = """{"id": 100, "versjon": 3, "nokkelord": ["DP-sak"]}""",
                                headers = jsonHeaders(),
                            )
                        }
                    }
                    else -> error("Unexpected")
                }
            }

        val klient = OppgaveKlient(baseUrl = baseUrl, httpClient = httpClient(mockEngine))
        val tagget = runBlocking { klient.taggMedDpSak(100) }

        tagget shouldBe true
        patchCount shouldBe 2
    }

    @Test
    fun `skal kaste exception etter 3 retries med 409`() {
        val mockEngine =
            MockEngine { request ->
                when (request.method) {
                    HttpMethod.Get ->
                        respond(
                            content = """{"id": 200, "versjon": 1, "nokkelord": []}""",
                            headers = jsonHeaders(),
                        )
                    HttpMethod.Patch -> respond(content = "", status = HttpStatusCode.Conflict)
                    else -> error("Unexpected")
                }
            }

        val klient = OppgaveKlient(baseUrl = baseUrl, httpClient = httpClient(mockEngine))

        shouldThrow<IllegalStateException> { runBlocking { klient.taggMedDpSak(200) } }
    }

    @Test
    fun `skal kaste exception naar GET feiler`() {
        val mockEngine =
            MockEngine { request ->
                respond(
                    content = """{"title":"Not Found"}""",
                    status = HttpStatusCode.NotFound,
                    headers = jsonHeaders(),
                )
            }

        val klient = OppgaveKlient(baseUrl = baseUrl, httpClient = httpClient(mockEngine))

        shouldThrow<IllegalStateException> { runBlocking { klient.taggMedDpSak(999) } }
    }

    @Test
    fun `skal appende DP-sak til eksisterende nokkelord`() {
        var patchBody: String? = null
        val mockEngine =
            MockEngine { request ->
                when (request.method) {
                    HttpMethod.Get ->
                        respond(
                            content = """{"id": 300, "versjon": 4, "nokkelord": ["Annet"]}""",
                            headers = jsonHeaders(),
                        )
                    HttpMethod.Patch -> {
                        patchBody = request.body.toByteArray().toString(Charsets.UTF_8)
                        respond(
                            content = """{"id": 300, "versjon": 5, "nokkelord": ["Annet", "DP-sak"]}""",
                            headers = jsonHeaders(),
                        )
                    }
                    else -> error("Unexpected")
                }
            }

        val klient = OppgaveKlient(baseUrl = baseUrl, httpClient = httpClient(mockEngine))
        val tagget = runBlocking { klient.taggMedDpSak(300) }

        tagget shouldBe true
        patchBody!! shouldBe """{"nokkelord":["Annet","DP-sak"],"meta":{"versjon":4}}"""
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun httpClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                jackson()
            }
        }
}

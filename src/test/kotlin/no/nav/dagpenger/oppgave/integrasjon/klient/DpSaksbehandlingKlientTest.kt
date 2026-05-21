package no.nav.dagpenger.oppgave.integrasjon.klient

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class DpSaksbehandlingKlientTest {
    private val baseUrl = "http://dp-saksbehandling.test"

    @Test
    fun `returnerer true naar person har dagpenger-sak`() {
        val sakId = "019e49a9-b522-7483-8217-980a3a48a9a7"
        val mockEngine =
            MockEngine { request ->
                request.method shouldBe HttpMethod.Post
                request.url.toString() shouldBe "$baseUrl/person/siste-dagpenger-sak"
                val body = String(request.body.toByteArray())
                body shouldBe """{"ident":"12345678901"}"""
                respond(
                    content = sakId,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
                )
            }

        val klient = lagKlient(mockEngine)

        runBlocking {
            klient.harSak("12345678901") shouldBe true
        }
    }

    @Test
    fun `returnerer false naar person ikke har dagpenger-sak`() {
        val mockEngine =
            MockEngine {
                respond(
                    content = "",
                    status = HttpStatusCode.NotFound,
                )
            }

        val klient = lagKlient(mockEngine)

        runBlocking {
            klient.harSak("99999999999") shouldBe false
        }
    }

    @Test
    fun `kaster exception ved uventet statuskode`() {
        val mockEngine =
            MockEngine {
                respond(
                    content = "Internal Server Error",
                    status = HttpStatusCode.InternalServerError,
                )
            }

        val klient = lagKlient(mockEngine)

        runBlocking {
            shouldThrow<DpSaksbehandlingKlientException> {
                klient.harSak("12345678901")
            }
        }
    }

    private fun lagKlient(mockEngine: MockEngine): DpSaksbehandlingKlient =
        DpSaksbehandlingKlient(baseUrl, tokenProvider = { "test-token" }, httpClient = createHttpClient(mockEngine))
}

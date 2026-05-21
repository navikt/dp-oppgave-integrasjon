package no.nav.dagpenger.oppgave.integrasjon.klient

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

internal class DpSaksbehandlingKlient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
) {
    suspend fun harSak(ident: String): Boolean {
        val response =
            httpClient.post("$baseUrl/person/siste-dagpenger-sak") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("ident" to ident))
            }
        return when (response.status) {
            HttpStatusCode.OK -> true
            HttpStatusCode.NotFound -> false
            else -> throw DpSaksbehandlingKlientException(
                "Uventet statuskode fra dp-saksbehandling: ${response.status}",
            )
        }
    }
}

class DpSaksbehandlingKlientException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

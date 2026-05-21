package no.nav.dagpenger.oppgave.integrasjon.klient

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson3.jackson

internal fun createHttpClient(engine: HttpClientEngine = CIO.create {}) =
    HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) {
            jackson()
        }
    }

package no.nav.dagpenger.oppgave.integrasjon

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

internal class DpSaksbehandlingKlient(
    config: Map<String, String>,
) {
    private val baseUrl = config.getValue("DP_SAKSBEHANDLING_URL")
    private val scope = config.getValue("DP_SAKSBEHANDLING_SCOPE")

    // TODO: 🔴 Red zone — implementer HTTP-klient med Azure CC token
    // GET $baseUrl/intern/person/{ident}/har-sak
    // → 200 = true, 404 = false, andre statuskoder = throw
    fun harSak(ident: String): Boolean {
        TODO("Implementer REST-kall til dp-saksbehandling med Azure client_credentials (scope=$scope)")
    }
}

package no.nav.dagpenger.oppgave.integrasjon.klient

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class OppgaveKlient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
) {
    companion object {
        private const val NOKKELORD = "DP-sak"
        private const val MAX_RETRIES = 3
        private const val MAX_NOKKELORD = 2
    }

    /**
     * Tagger oppgave med "DP-sak" nøkkelord.
     * Kaster exception ved feil slik at meldingen retryes.
     * @return true hvis tagget, false hvis allerede tagget
     */
    suspend fun taggMedDpSak(oppgaveId: Long): Boolean {
        val oppgave = hentOppgave(oppgaveId)

        if (NOKKELORD in oppgave.nokkelord) {
            log.info { "Oppgave $oppgaveId er allerede tagget med $NOKKELORD" }
            return false
        }

        check(oppgave.nokkelord.size < MAX_NOKKELORD) {
            "Oppgave $oppgaveId har allerede ${oppgave.nokkelord.size} nøkkelord, kan ikke tagge med $NOKKELORD"
        }

        patchMedRetry(oppgaveId, oppgave.nokkelord + NOKKELORD, oppgave.versjon)
        log.info { "Oppgave $oppgaveId tagget med $NOKKELORD" }
        return true
    }

    private suspend fun patchMedRetry(
        oppgaveId: Long,
        nokkelord: List<String>,
        startVersjon: Int,
    ) {
        var versjon = startVersjon
        repeat(MAX_RETRIES) { forsøk ->
            val response =
                httpClient.patch("$baseUrl/api/v2/oppgaver/$oppgaveId") {
                    contentType(ContentType.Application.Json)
                    setBody(PatchOppgaveRequest(nokkelord = nokkelord, meta = PatchMeta(versjon = versjon)))
                }

            when (response.status) {
                HttpStatusCode.OK -> return
                HttpStatusCode.Conflict -> {
                    log.info { "409 Conflict for oppgave $oppgaveId, forsøk ${forsøk + 1}/$MAX_RETRIES" }
                    val fersk = hentOppgave(oppgaveId)
                    if (NOKKELORD in fersk.nokkelord) return
                    versjon = fersk.versjon
                }
                else -> {
                    val body = response.bodyAsText()
                    sikkerlogg.error { "PATCH oppgave $oppgaveId feilet: $body" }
                    error("PATCH oppgave $oppgaveId feilet med ${response.status}")
                }
            }
        }
        error("Ga opp etter $MAX_RETRIES forsøk med 409 Conflict for oppgave $oppgaveId")
    }

    private suspend fun hentOppgave(oppgaveId: Long): OppgaveResponse {
        val response = httpClient.get("$baseUrl/api/v2/oppgaver/$oppgaveId")
        if (response.status == HttpStatusCode.OK) return response.body<OppgaveResponse>()

        val body = response.bodyAsText()
        sikkerlogg.error { "GET oppgave $oppgaveId feilet: $body" }
        error("GET oppgave $oppgaveId feilet med ${response.status}")
    }
}

internal data class OppgaveResponse(
    val id: Long,
    val versjon: Int,
    val nokkelord: List<String> = emptyList(),
)

internal data class PatchOppgaveRequest(
    val nokkelord: List<String>,
    val meta: PatchMeta,
)

internal data class PatchMeta(
    val versjon: Int,
)

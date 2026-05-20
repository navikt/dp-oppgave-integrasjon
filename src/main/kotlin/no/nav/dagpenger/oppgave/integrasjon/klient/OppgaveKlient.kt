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

    sealed class Resultat {
        data object Tagget : Resultat()

        data object AlleredeTagget : Resultat()

        data object NokkelordFull : Resultat()

        data class Feil(
            val melding: String,
        ) : Resultat()
    }

    suspend fun taggMedDpSak(oppgaveId: Long): Resultat {
        val oppgave =
            hentOppgave(oppgaveId)
                ?: return Resultat.Feil("Kunne ikke hente oppgave $oppgaveId")

        return when {
            NOKKELORD in oppgave.nokkelord -> Resultat.AlleredeTagget
            oppgave.nokkelord.size >= MAX_NOKKELORD -> {
                log.error { "Oppgave $oppgaveId har allerede ${oppgave.nokkelord.size} nøkkelord, kan ikke tagge" }
                Resultat.NokkelordFull
            }
            else -> patchMedRetry(oppgaveId, oppgave.nokkelord + NOKKELORD, oppgave.versjon)
        }
    }

    private suspend fun patchMedRetry(
        oppgaveId: Long,
        nokkelord: List<String>,
        startVersjon: Int,
    ): Resultat {
        var versjon = startVersjon
        repeat(MAX_RETRIES) { forsøk ->
            when (val result = patchNokkelord(oppgaveId, nokkelord, versjon)) {
                is PatchResult.Ok -> {
                    log.info { "Oppgave $oppgaveId tagget med $NOKKELORD" }
                    return Resultat.Tagget
                }
                is PatchResult.Conflict -> {
                    log.info { "409 Conflict for oppgave $oppgaveId, forsøk ${forsøk + 1}/$MAX_RETRIES" }
                    val fersk =
                        hentOppgave(oppgaveId)
                            ?: return Resultat.Feil("Kunne ikke hente oppgave $oppgaveId ved retry")
                    if (NOKKELORD in fersk.nokkelord) return Resultat.AlleredeTagget
                    versjon = fersk.versjon
                }
                is PatchResult.Feil -> return Resultat.Feil(result.melding)
            }
        }
        return Resultat.Feil("Ga opp etter $MAX_RETRIES forsøk med 409 Conflict for oppgave $oppgaveId")
    }

    private suspend fun hentOppgave(oppgaveId: Long): OppgaveResponse? {
        val response = httpClient.get("$baseUrl/api/v2/oppgaver/$oppgaveId")
        if (response.status == HttpStatusCode.OK) return response.body<OppgaveResponse>()

        val body = response.bodyAsText()
        log.error { "GET oppgave $oppgaveId feilet med ${response.status}" }
        sikkerlogg.error { "GET oppgave $oppgaveId feilet: $body" }
        return null
    }

    private sealed class PatchResult {
        data object Ok : PatchResult()

        data object Conflict : PatchResult()

        data class Feil(
            val melding: String,
        ) : PatchResult()
    }

    private suspend fun patchNokkelord(
        oppgaveId: Long,
        nokkelord: List<String>,
        versjon: Int,
    ): PatchResult {
        val response =
            httpClient.patch("$baseUrl/api/v2/oppgaver/$oppgaveId") {
                contentType(ContentType.Application.Json)
                setBody(PatchOppgaveRequest(nokkelord = nokkelord, meta = PatchMeta(versjon = versjon)))
            }
        return when (response.status) {
            HttpStatusCode.OK -> PatchResult.Ok
            HttpStatusCode.Conflict -> PatchResult.Conflict
            else -> {
                val body = response.bodyAsText()
                log.error { "PATCH oppgave $oppgaveId feilet med ${response.status}" }
                sikkerlogg.error { "PATCH oppgave $oppgaveId feilet: $body" }
                PatchResult.Feil("PATCH feilet med ${response.status}")
            }
        }
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

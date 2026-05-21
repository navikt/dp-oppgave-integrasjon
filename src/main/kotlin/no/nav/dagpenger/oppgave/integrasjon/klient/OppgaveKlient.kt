package no.nav.dagpenger.oppgave.integrasjon.klient

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

private val log = KotlinLogging.logger {}

class OppgaveKlientException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

internal class OppgaveKlient(
    private val baseUrl: String,
    private val tokenProvider: () -> String,
    private val httpClient: HttpClient = createHttpClient(),
) {
    companion object {
        private const val NOKKELORD = "DP-sak"
        private const val MAX_RETRIES = 3
    }

    sealed class PatchStatus {
        object OK : PatchStatus()

        object Conflict : PatchStatus()
    }

    suspend fun taggMedDpSak(oppgaveId: Long) {
        var status: PatchStatus? = null
        var retries = 0
        while (status !is PatchStatus.OK && retries++ < MAX_RETRIES) {
            status = merkOppgave(oppgaveId)
        }
        if (status !is PatchStatus.OK) {
            throw OppgaveKlientException("Kunne ikke merke oppgave $oppgaveId etter $MAX_RETRIES forsøk")
        }
    }

    private suspend fun merkOppgave(oppgaveId: Long): PatchStatus {
        val oppgave = hentOppgave(oppgaveId)
        val oppdatert = oppgave.nokkelord.toSet() + NOKKELORD

        return if (oppdatert == oppgave.nokkelord.toSet()) {
            PatchStatus.OK
        } else {
            patchOppgave(oppgave, oppdatert.toList())
        }
    }

    private suspend fun patchOppgave(
        oppgave: OppgaveResponse,
        nokkelord: List<String>,
    ): PatchStatus =
        runCatching {
            httpClient
                .patch("$baseUrl/api/v2/oppgaver/${oppgave.id}") {
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PatchOppgaveRequest(
                            nokkelord = nokkelord,
                            meta = PatchMeta(versjon = oppgave.versjon),
                        ),
                    )
                }.let { response ->
                    when (response.status) {
                        HttpStatusCode.OK -> PatchStatus.OK
                        HttpStatusCode.Conflict -> PatchStatus.Conflict
                        else -> throw OppgaveKlientException("PATCH oppgave ${oppgave.id} feilet med ${response.status}")
                    }
                }
        }.getOrElse {
            throw OppgaveKlientException("PATCH oppgave ${oppgave.id} feilet", it)
        }

    private suspend fun hentOppgave(oppgaveId: Long): OppgaveResponse =
        runCatching {
            httpClient
                .get("$baseUrl/api/v2/oppgaver/$oppgaveId") {
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                    expectSuccess = true
                }.body<OppgaveResponse>()
        }.onFailure {
            log.error { "GET oppgave $oppgaveId feilet: $it" }
        }.getOrElse {
            throw OppgaveKlientException("GET oppgave $oppgaveId feilet", it)
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

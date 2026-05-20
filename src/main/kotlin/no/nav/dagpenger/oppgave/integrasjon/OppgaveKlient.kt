package no.nav.dagpenger.oppgave.integrasjon

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

internal class OppgaveKlient(
    config: Map<String, String>,
) {
    private val baseUrl = config.getValue("OPPGAVE_URL")
    private val scope = config.getValue("OPPGAVE_SCOPE")

    // TODO: 🔴 Red zone — implementer HTTP-klient med Azure CC token
    // Steg 1: GET /api/v2/oppgaver/{oppgaveId} → les nøkkelord + versjon
    // Steg 2: Append "DP-sak" til nøkkelord-listen (skip hvis allerede der, eller maks 2)
    // Steg 3: PATCH /api/v2/oppgaver/{oppgaveId} med full nøkkelord-liste + meta.versjon
    // Ved 409 Conflict → retry GET+PATCH (maks 3 forsøk)
    fun taggMedDpSak(
        oppgaveId: Long,
        versjon: Int,
    ) {
        TODO("Implementer GET+PATCH nøkkelord mot Gosys v2 (scope=$scope)")
    }
}

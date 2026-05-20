package no.nav.dagpenger.oppgave.integrasjon

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class LoggOppgaveMarkerer : OppgaveMarkerer {
    override fun markerOppgave(
        ident: String,
        oppgaveId: Long,
        versjon: Int,
    ) {
        log.info { "Ville tagget oppgave $oppgaveId (versjon=$versjon) med nøkkelord DP-sak" }
        sikkerlogg.info { "Ville tagget oppgave $oppgaveId for ident=$ident" }
    }
}

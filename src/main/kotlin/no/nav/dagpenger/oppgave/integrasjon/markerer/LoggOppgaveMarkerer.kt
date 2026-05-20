package no.nav.dagpenger.oppgave.integrasjon.markerer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.oppgave.integrasjon.Oppgave

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class LoggOppgaveMarkerer : OppgaveMarkerer {
    override fun markerOppgave(oppgave: Oppgave) {
        log.info { "Ville tagget oppgave ${oppgave.oppgaveId} (versjon=${oppgave.versjon}) med nøkkelord DP-sak" }
        sikkerlogg.info { "Ville tagget oppgave ${oppgave.oppgaveId} for ident=${oppgave.ident}" }
    }
}

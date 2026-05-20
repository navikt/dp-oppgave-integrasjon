package no.nav.dagpenger.oppgave.integrasjon.markerer

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.oppgave.integrasjon.Oppgave
import no.nav.dagpenger.oppgave.integrasjon.klient.DpSaksbehandlingKlient
import no.nav.dagpenger.oppgave.integrasjon.klient.OppgaveKlient

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class DpSakOppgaveMarkerer(
    private val dpSaksbehandlingKlient: DpSaksbehandlingKlient,
    private val oppgaveKlient: OppgaveKlient,
    private val meterRegistry: MeterRegistry,
) : OppgaveMarkerer {
    override fun markerOppgave(oppgave: Oppgave) {
        val harSak = dpSaksbehandlingKlient.harSak(oppgave.ident)
        if (!harSak) {
            log.info { "Person har ingen sak i dp-saksbehandling, skipper oppgave ${oppgave.oppgaveId}" }
            return
        }

        log.info { "Person har sak — tagger oppgave ${oppgave.oppgaveId} med nøkkelord DP-sak" }
        sikkerlogg.info { "Tagger oppgave ${oppgave.oppgaveId} for ident=${oppgave.ident}" }

        val resultat = runBlocking { oppgaveKlient.taggMedDpSak(oppgave.oppgaveId) }

        when (resultat) {
            is OppgaveKlient.Resultat.Tagget -> meterRegistry.counter("oppgaver_tagget_total").increment()
            is OppgaveKlient.Resultat.AlleredeTagget -> log.info { "Oppgave ${oppgave.oppgaveId} var allerede tagget" }
            is OppgaveKlient.Resultat.NokkelordFull -> meterRegistry.counter("oppgaver_fullt_nokkelord_total").increment()
            is OppgaveKlient.Resultat.Feil -> {
                log.error { "Feil ved tagging av oppgave ${oppgave.oppgaveId}: ${resultat.melding}" }
                meterRegistry.counter("oppgaver_tagging_feilet_total").increment()
            }
        }
    }
}

package no.nav.dagpenger.oppgave.integrasjon.markerer

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.oppgave.integrasjon.Oppgave
import no.nav.dagpenger.oppgave.integrasjon.klient.DpSaksbehandlingKlient
import no.nav.dagpenger.oppgave.integrasjon.klient.OppgaveKlient

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

// TODO: 🔴 Red zone — implementer forretningslogikken selv
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

        // TODO: Avklar om GET er nødvendig (nyopprettede oppgaver har kanskje 0 nøkkelord)
        oppgaveKlient.taggMedDpSak(oppgave.oppgaveId, oppgave.versjon)

        meterRegistry.counter("oppgaver_tagget_total").increment()
    }
}

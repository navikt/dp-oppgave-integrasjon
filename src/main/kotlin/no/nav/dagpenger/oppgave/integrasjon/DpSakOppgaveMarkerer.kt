package no.nav.dagpenger.oppgave.integrasjon

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

// TODO: 🔴 Red zone — implementer forretningslogikken selv
internal class DpSakOppgaveMarkerer(
    private val dpSaksbehandlingKlient: DpSaksbehandlingKlient,
    private val oppgaveKlient: OppgaveKlient,
    private val meterRegistry: MeterRegistry,
) : OppgaveMarkerer {
    override fun markerOppgave(
        ident: String,
        oppgaveId: Long,
        versjon: Int,
    ) {
        val harSak = dpSaksbehandlingKlient.harSak(ident)
        if (!harSak) {
            log.info { "Person har ingen sak i dp-saksbehandling, skipper oppgave $oppgaveId" }
            return
        }

        log.info { "Person har sak — tagger oppgave $oppgaveId med nøkkelord DP-sak" }
        sikkerlogg.info { "Tagger oppgave $oppgaveId for ident=$ident" }

        // TODO: Avklar om GET er nødvendig (nyopprettede oppgaver har kanskje 0 nøkkelord)
        oppgaveKlient.taggMedDpSak(oppgaveId, versjon)

        meterRegistry.counter("oppgaver_tagget_total").increment()
    }
}

package no.nav.dagpenger.oppgave.integrasjon.markerer

import no.nav.dagpenger.oppgave.integrasjon.Oppgave

internal interface OppgaveMarkerer {
    fun markerOppgave(oppgave: Oppgave)
}

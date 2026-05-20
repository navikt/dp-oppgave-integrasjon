package no.nav.dagpenger.oppgave.integrasjon

internal interface OppgaveMarkerer {
    fun markerOppgave(
        ident: String,
        oppgaveId: Long,
        versjon: Int,
    )
}

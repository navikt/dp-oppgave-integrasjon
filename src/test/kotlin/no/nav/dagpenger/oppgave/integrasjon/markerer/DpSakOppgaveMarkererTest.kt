package no.nav.dagpenger.oppgave.integrasjon.markerer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.dagpenger.oppgave.integrasjon.Oppgave
import no.nav.dagpenger.oppgave.integrasjon.klient.DpSaksbehandlingKlient
import no.nav.dagpenger.oppgave.integrasjon.klient.DpSaksbehandlingKlientException
import no.nav.dagpenger.oppgave.integrasjon.klient.OppgaveKlient
import no.nav.dagpenger.oppgave.integrasjon.klient.OppgaveKlientException
import org.junit.jupiter.api.Test

internal class DpSakOppgaveMarkererTest {
    private val dpSaksbehandlingKlient = mockk<DpSaksbehandlingKlient>()
    private val oppgaveKlient = mockk<OppgaveKlient>(relaxed = true)
    private val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    private val markerer =
        DpSakOppgaveMarkerer(
            dpSaksbehandlingKlient = dpSaksbehandlingKlient,
            oppgaveKlient = oppgaveKlient,
            meterRegistry = meterRegistry,
        )

    @Test
    fun `tagger oppgave naar person har sak i dp-saksbehandling`() {
        coEvery { dpSaksbehandlingKlient.harSak("12345678901") } returns true

        markerer.markerOppgave(Oppgave(oppgaveId = 123, versjon = 1, ident = "12345678901"))

        coVerify(exactly = 1) { oppgaveKlient.taggMedDpSak(123) }
        meterRegistry.counter("oppgaver_tagget_total").count() shouldBe 1.0
    }

    @Test
    fun `tagger ikke oppgave naar person ikke har sak`() {
        coEvery { dpSaksbehandlingKlient.harSak("99999999999") } returns false

        markerer.markerOppgave(Oppgave(oppgaveId = 456, versjon = 2, ident = "99999999999"))

        coVerify(exactly = 0) { oppgaveKlient.taggMedDpSak(any()) }
    }

    @Test
    fun `propagerer exception fra dpSaksbehandlingKlient slik at R&R retrier`() {
        coEvery { dpSaksbehandlingKlient.harSak(any()) } throws
            DpSaksbehandlingKlientException("503 Service Unavailable")

        shouldThrow<DpSaksbehandlingKlientException> {
            markerer.markerOppgave(Oppgave(oppgaveId = 789, versjon = 1, ident = "12345678901"))
        }
    }

    @Test
    fun `propagerer exception fra oppgaveKlient slik at R&R retrier`() {
        coEvery { dpSaksbehandlingKlient.harSak(any()) } returns true
        coEvery { oppgaveKlient.taggMedDpSak(any()) } throws
            OppgaveKlientException("PATCH feilet med 500")

        shouldThrow<OppgaveKlientException> {
            markerer.markerOppgave(Oppgave(oppgaveId = 101, versjon = 1, ident = "12345678901"))
        }
    }
}

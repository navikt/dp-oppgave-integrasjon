package no.nav.dagpenger.oppgave.integrasjon

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.oppgave.integrasjon.markerer.OppgaveMarkerer
import no.nav.dagpenger.oppgave.integrasjon.mottak.OppgaveOpprettetMottak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppgaveOpprettetMottakTest {
    private val oppgaveMarkerer = mockk<OppgaveMarkerer>(relaxed = true)
    private val testRapid = TestRapid()

    @BeforeEach
    fun setup() {
        OppgaveOpprettetMottak(
            rapidsConnection = testRapid,
            oppgaveMarkerer = oppgaveMarkerer,
        )
    }

    @Test
    fun `skal kalle oppgaveMarkerer for DAG-oppgave med FOLKEREGISTERIDENT`() {
        testRapid.sendTestMessage(
            oppgaveOpprettetMelding(
                oppgaveId = 123456,
                versjon = 1,
                tema = "DAG",
                ident = "12345678901",
                identType = "FOLKEREGISTERIDENT",
            ),
        )

        verify(exactly = 1) { oppgaveMarkerer.markerOppgave(Oppgave(oppgaveId = 123456, versjon = 1, ident = "12345678901")) }
    }

    @Test
    fun `skal ignorere oppgaver med annet tema enn DAG`() {
        testRapid.sendTestMessage(
            oppgaveOpprettetMelding(
                oppgaveId = 123456,
                versjon = 1,
                tema = "SYK",
                ident = "12345678901",
                identType = "FOLKEREGISTERIDENT",
            ),
        )

        verify(exactly = 0) { oppgaveMarkerer.markerOppgave(any()) }
    }

    @Test
    fun `skal ignorere oppgaver med identType som ikke er FOLKEREGISTERIDENT`() {
        testRapid.sendTestMessage(
            oppgaveOpprettetMelding(
                oppgaveId = 123456,
                versjon = 1,
                tema = "DAG",
                ident = "999888777",
                identType = "ORGNR",
            ),
        )

        verify(exactly = 0) { oppgaveMarkerer.markerOppgave(any()) }
    }

    @Test
    fun `skal ignorere meldinger fra rapid-topic (har @event_name)`() {
        testRapid.sendTestMessage(
            //language=JSON
            """
            {
              "@event_name": "noe_annet",
              "hendelse": { "hendelsestype": "OPPGAVE_OPPRETTET", "tidspunkt": "2026-05-20T10:00:00" },
              "oppgave": {
                "oppgaveId": 123456,
                "versjon": 1,
                "kategorisering": { "tema": "DAG", "oppgavetype": "VUR_KONS_YTE" },
                "bruker": { "ident": "12345678901", "identType": "FOLKEREGISTERIDENT" },
                "status": "OPPRETTET",
                "behandlingsperiode": { "aktiv": "2026-05-20", "frist": "2026-05-27" },
                "tilordning": { "enhetsnr": "4450", "navIdent": null }
              }
            }
            """.trimIndent(),
        )

        verify(exactly = 0) { oppgaveMarkerer.markerOppgave(any()) }
    }

    private fun oppgaveOpprettetMelding(
        oppgaveId: Long,
        versjon: Int,
        tema: String,
        ident: String,
        identType: String,
    ): String =
        //language=JSON
        """
        {
          "hendelse": {
            "hendelsestype": "OPPGAVE_OPPRETTET",
            "tidspunkt": "2026-05-20T10:00:00"
          },
          "oppgave": {
            "oppgaveId": $oppgaveId,
            "versjon": $versjon,
            "kategorisering": {
              "tema": "$tema",
              "oppgavetype": "VUR_KONS_YTE"
            },
            "bruker": {
              "ident": "$ident",
              "identType": "$identType"
            },
            "status": "OPPRETTET",
            "behandlingsperiode": {
              "aktiv": "2026-05-20",
              "frist": "2026-05-27"
            },
            "tilordning": {
              "enhetsnr": "4450",
              "navIdent": null
            }
          }
        }
        """.trimIndent()
}

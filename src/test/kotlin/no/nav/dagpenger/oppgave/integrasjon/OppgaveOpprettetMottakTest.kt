package no.nav.dagpenger.oppgave.integrasjon

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OppgaveOpprettetMottakTest {
    private val dpSaksbehandlingKlient = mockk<DpSaksbehandlingKlient>()
    private val oppgaveKlient = mockk<OppgaveKlient>(relaxed = true)
    private val testRapid = TestRapid()

    @BeforeEach
    fun setup() {
        OppgaveOpprettetMottak(
            rapidsConnection = testRapid,
            dpSaksbehandlingKlient = dpSaksbehandlingKlient,
            oppgaveKlient = oppgaveKlient,
        )
    }

    @Test
    fun `skal tagge oppgave når person har sak`() {
        every { dpSaksbehandlingKlient.harSak("12345678901") } returns true

        testRapid.sendTestMessage(
            oppgaveOpprettetMelding(
                oppgaveId = 123456,
                versjon = 1,
                tema = "DAG",
                ident = "12345678901",
                identType = "FOLKEREGISTERIDENT",
            ),
            OppgaveOpprettetMottak.OPPGAVEHENDELSE_TOPIC,
        )

        verify(exactly = 1) { oppgaveKlient.taggMedDpSak(123456, 1) }
    }

    @Test
    fun `skal ikke tagge oppgave når person ikke har sak`() {
        every { dpSaksbehandlingKlient.harSak("12345678901") } returns false

        testRapid.sendTestMessage(
            oppgaveOpprettetMelding(
                oppgaveId = 123456,
                versjon = 1,
                tema = "DAG",
                ident = "12345678901",
                identType = "FOLKEREGISTERIDENT",
            ),
            OppgaveOpprettetMottak.OPPGAVEHENDELSE_TOPIC,
        )

        verify(exactly = 0) { oppgaveKlient.taggMedDpSak(any(), any()) }
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
            OppgaveOpprettetMottak.OPPGAVEHENDELSE_TOPIC,
        )

        verify(exactly = 0) { dpSaksbehandlingKlient.harSak(any()) }
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
            OppgaveOpprettetMottak.OPPGAVEHENDELSE_TOPIC,
        )

        verify(exactly = 0) { dpSaksbehandlingKlient.harSak(any()) }
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

        verify(exactly = 0) { dpSaksbehandlingKlient.harSak(any()) }
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

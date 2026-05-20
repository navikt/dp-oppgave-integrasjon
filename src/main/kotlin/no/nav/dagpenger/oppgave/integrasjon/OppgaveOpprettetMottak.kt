package no.nav.dagpenger.oppgave.integrasjon

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class OppgaveOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val dpSaksbehandlingKlient: DpSaksbehandlingKlient,
    private val oppgaveKlient: OppgaveKlient,
) : River.PacketListener {
    companion object {
        const val OPPGAVEHENDELSE_TOPIC = "oppgavehandtering.oppgavehendelse-v1"

        internal val rapidFilter: River.() -> Unit = {
            precondition {
                it.forbid("@event_name") // Kun meldinger fra ekstern topic (ikke rapid)
                it.requireValue("hendelse.hendelsestype", "OPPGAVE_OPPRETTET")
            }
            validate {
                it.requireKey(
                    "oppgave.oppgaveId",
                    "oppgave.versjon",
                    "oppgave.kategorisering.tema",
                    "oppgave.bruker.ident",
                    "oppgave.bruker.identType",
                )
            }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val tema = packet["oppgave.kategorisering.tema"].asText()
        if (tema != "DAG") return

        val identType = packet["oppgave.bruker.identType"].asText()
        if (identType != "FOLKEREGISTERIDENT") {
            log.info { "Ignorerer oppgave med identType=$identType" }
            return
        }

        val ident = packet["oppgave.bruker.ident"].asText()
        val oppgaveId = packet["oppgave.oppgaveId"].asLong()
        val versjon = packet["oppgave.versjon"].asInt()

        meterRegistry.counter("oppgaver_mottatt_total").increment()

        // TODO: 🔴 Red zone — implementer forretningslogikken selv
        // 1. Sjekk om personen har sak i dp-saksbehandling
        // 2. Hvis ja: GET oppgave for å lese nøkkelord + versjon
        // 3. PATCH med appended "DP-sak" nøkkelord
        taggOppgaveHvisHarSak(ident, oppgaveId, versjon, meterRegistry)
    }

    private fun taggOppgaveHvisHarSak(
        ident: String,
        oppgaveId: Long,
        versjon: Int,
        meterRegistry: MeterRegistry,
    ) {
        val harSak = dpSaksbehandlingKlient.harSak(ident)
        if (!harSak) {
            log.info { "Person har ingen sak i dp-saksbehandling, skipper oppgave $oppgaveId" }
            return
        }

        log.info { "Person har sak — tagger oppgave $oppgaveId med nøkkelord DP-sak" }
        sikkerlogg.info { "Tagger oppgave $oppgaveId for ident (se securelog)" }

        // TODO: Avklar om GET er nødvendig (nyopprettede oppgaver har kanskje 0 nøkkelord)
        oppgaveKlient.taggMedDpSak(oppgaveId, versjon)

        meterRegistry.counter("oppgaver_tagget_total").increment()
    }
}

package no.nav.dagpenger.oppgave.integrasjon.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.oppgave.integrasjon.Oppgave
import no.nav.dagpenger.oppgave.integrasjon.markerer.OppgaveMarkerer

private val log = KotlinLogging.logger {}

internal class OppgaveOpprettetMottak(
    rapidsConnection: RapidsConnection,
    private val oppgaveMarkerer: OppgaveMarkerer,
) : River.PacketListener {
    companion object {
        internal val rapidFilter: River.() -> Unit = {
            precondition {
                it.forbid("@event_name")
                it.requireValue("hendelse.hendelsestype", "OPPGAVE_OPPRETTET")
                it.requireValue("oppgave.kategorisering.tema", "DAG")
            }
            validate {
                it.requireKey(
                    "oppgave.oppgaveId",
                    "oppgave.versjon",
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
        val identType = packet["oppgave.bruker.identType"].asString()
        if (identType != "FOLKEREGISTERIDENT") {
            log.warn { "Uventet identType=$identType for DAG-oppgave ${packet["oppgave.oppgaveId"].asLong()}" }
            return
        }

        val ident = packet["oppgave.bruker.ident"].asString()
        val oppgaveId = packet["oppgave.oppgaveId"].asLong()
        val versjon = packet["oppgave.versjon"].asInt()

        meterRegistry.counter("oppgaver_mottatt_total").increment()

        oppgaveMarkerer.markerOppgave(Oppgave(oppgaveId = oppgaveId, versjon = versjon, ident = ident))
    }
}

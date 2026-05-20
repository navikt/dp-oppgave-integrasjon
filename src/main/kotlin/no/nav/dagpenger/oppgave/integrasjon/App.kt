package no.nav.dagpenger.oppgave.integrasjon

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.RapidApplication

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        sikkerlogg.error(e) { e.message }
    }
    App.start()
}

internal object App : RapidsConnection.StatusListener {
    private val rapidsConnection =
        RapidApplication.create(Configuration.config).also {
            OppgaveOpprettetMottak(
                rapidsConnection = it,
                dpSaksbehandlingKlient = DpSaksbehandlingKlient(Configuration.config),
                oppgaveKlient = OppgaveKlient(Configuration.config),
            )
        }

    init {
        rapidsConnection.register(this)
    }

    fun start() = rapidsConnection.start()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        log.info { "Starter dp-oppgave-integrasjon" }
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        log.info { "Stopper dp-oppgave-integrasjon" }
    }
}

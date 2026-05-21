package no.nav.dagpenger.oppgave.integrasjon

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.dagpenger.oppgave.integrasjon.klient.DpSaksbehandlingKlient
import no.nav.dagpenger.oppgave.integrasjon.klient.OppgaveKlient
import no.nav.dagpenger.oppgave.integrasjon.klient.createHttpClient
import no.nav.dagpenger.oppgave.integrasjon.markerer.DpSakOppgaveMarkerer
import no.nav.dagpenger.oppgave.integrasjon.mottak.OppgaveOpprettetMottak
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
    private val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    private val dpSaksbehandlingKlient =
        DpSaksbehandlingKlient(
            baseUrl = Configuration.dpSaksbehandlingUrl,
            tokenProvider = Configuration.dpSaksbehandlingTokenProvider,
            httpClient = createHttpClient(),
        )

    private val oppgaveKlient =
        OppgaveKlient(
            baseUrl = Configuration.oppgaveUrl,
            tokenProvider = Configuration.oppgaveTokenProvider,
            httpClient = createHttpClient(),
        )

    private val rapidsConnection =
        RapidApplication
            .create(
                env = Configuration.config,
                meterRegistry = meterRegistry,
            ).also {
                OppgaveOpprettetMottak(
                    rapidsConnection = it,
                    oppgaveMarkerer =
                        DpSakOppgaveMarkerer(
                            dpSaksbehandlingKlient = dpSaksbehandlingKlient,
                            oppgaveKlient = oppgaveKlient,
                            meterRegistry = meterRegistry,
                        ),
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

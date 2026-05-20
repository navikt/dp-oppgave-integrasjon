package no.nav.dagpenger.oppgave.integrasjon

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding

internal object Configuration {
    private const val APP_NAME = "dp-oppgave-integrasjon"

    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "RAPID_APP_NAME" to APP_NAME,
                "KAFKA_CONSUMER_GROUP_ID" to "$APP_NAME-v1",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_RESET_POLICY" to "latest",
                "KAFKA_EXTRA_TOPIC" to "oppgavehandtering.oppgavehendelse-v1",
                "DP_SAKSBEHANDLING_URL" to "http://dp-saksbehandling",
                "DP_SAKSBEHANDLING_SCOPE" to "",
                "OPPGAVE_URL" to "https://oppgave-q2.intern.dev.nav.no",
                "OPPGAVE_SCOPE" to "",
            ),
        )

    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val config: Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }
}

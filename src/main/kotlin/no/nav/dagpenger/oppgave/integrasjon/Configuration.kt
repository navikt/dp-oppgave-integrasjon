package no.nav.dagpenger.oppgave.integrasjon

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config

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
                "DP_SAKSBEHANDLING_SCOPE" to "api://dev-gcp.teamdagpenger.dp-saksbehandling/.default",
                "OPPGAVE_URL" to "https://oppgave-q2.intern.dev.nav.no",
                "OPPGAVE_SCOPE" to "api://dev-fss.oppgavehandtering.oppgave-q2/.default",
            ),
        )

    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val config: Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }

    val dpSaksbehandlingUrl: String = properties[Key("DP_SAKSBEHANDLING_URL", stringType)]
    val dpSaksbehandlingScope: String = properties[Key("DP_SAKSBEHANDLING_SCOPE", stringType)]
    val dpSaksbehandlingTokenProvider = { clientCredentialsTokenProvider(dpSaksbehandlingScope) }

    val oppgaveUrl: String = properties[Key("OPPGAVE_URL", stringType)]
    val oppgaveScope: String = properties[Key("OPPGAVE_SCOPE", stringType)]
    val oppgaveTokenProvider = { clientCredentialsTokenProvider(oppgaveScope) }

    private val clientCredentialsTokenProvider = { scope: String ->
        azureAdClient.clientCredentials(scope).access_token
            ?: throw RuntimeException("Failed to get access token")
    }

    val azureAdClient: CachedOauth2Client by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(properties)
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }
}

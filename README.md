# dp-oppgave-integrasjon

Automatisk tagging av Gosys-oppgaver med nøkkelord "DP-sak" for dagpengebrukere.

## Hva gjør appen?

Lytter på Kafka-hendelser fra Gosys (oppgavehendelse-v1). Når en ny oppgave opprettes med tema=DAG:
1. Sjekker om personen har en dagpengesak i dp-saksbehandling
2. Hvis ja: tagger oppgaven med nøkkelordet "DP-sak" via Gosys v2 API

Saksbehandlere kan deretter filtrere på "DP-sak" i Gosys 2 for å finne relevante oppgaver.

## Arkitektur

```
Gosys Kafka (EXTRA_TOPIC)
  → OppgaveOpprettetMottak (River: OPPGAVE_OPPRETTET + tema=DAG)
    → DpSaksbehandlingKlient: GET /intern/person/{ident}/har-sak
    → OppgaveKlient: PATCH /api/v2/oppgaver/{id} (nøkkelord)
```

- **Stateless** — ingen database
- **Rapids & Rivers** — for health, metrics, graceful shutdown
- **Azure AD client_credentials** — for maskin-til-maskin-kall

## Utvikling

```bash
./gradlew build        # Bygg + test
./gradlew test         # Kun tester
./gradlew installDist  # Lag distribuerbar app
```

## Miljøvariabler

| Variabel | Beskrivelse |
|----------|-------------|
| `KAFKA_EXTRA_TOPIC` | Gosys oppgavehendelse-topic |
| `DP_SAKSBEHANDLING_URL` | URL til dp-saksbehandling |
| `DP_SAKSBEHANDLING_SCOPE` | Azure scope for dp-saksbehandling |
| `OPPGAVE_URL` | URL til Gosys oppgave API |
| `OPPGAVE_SCOPE` | Azure scope for Gosys |

## Avhengigheter

- **dp-saksbehandling** — for har-sak-sjekk
- **oppgave (Gosys)** — for å lese og tagge oppgaver
- **Kafka** — `oppgavehandtering.oppgavehendelse-v1` (lesetilgang via PR #660)

# Documentazione Nuove Funzionalità: Storico Dati ed Esportazione CSV

## 1. Introduzione
L'obiettivo di questo aggiornamento è stato trasformare la dashboard di monitoraggio in tempo reale in uno strumento di analisi completo. Gli utenti ora possono non solo vedere l'ultimo dato ricevuto, ma anche consultare l'andamento temporale delle letture e scaricare i dati per analisi esterne.

---

## 2. Descrizione delle Funzionalità

### A. Navigazione e Interattività
I box dei dispositivi (Tauanito) nella schermata principale sono stati resi **interattivi**. 
*   **Azione:** Cliccando su una card di un dispositivo, l'app effettua una navigazione verso una nuova schermata di dettaglio.
*   **Implementazione:** È stata aggiunta una lambda `onDeviceClick` al componente `DeviceCard` che sfrutta il sistema di navigazione di Jetpack Compose per passare l'ID univoco del dispositivo alla rotta dello storico.

### B. Schermata Storico Dati (`HistoryScreen`)
Questa nuova interfaccia mostra un elenco cronologico di tutte le letture registrate dal sensore.
*   **Visualizzazione:** I dati sono presentati in card eleganti. Il titolo della pagina è stato ingrandito (**21.sp**) per una migliore leggibilità del nome del dispositivo.
*   **Gestione Stato:** Utilizza un `HistoryViewModel` per gestire il caricamento asincrono, mostrando un indicatore di progresso durante il recupero dei dati.

### C. Esportazione CSV
È stata integrata la funzione di download dei dati con un'interfaccia utente discreta ed elegante.
*   **Design del Pulsante:** Il pulsante "CSV" è posizionato all'estrema destra nella barra superiore (**actions** della TopAppBar). Presenta un design a badge con un **gradiente azzurrino** luminoso.
*   **Indicatore Unico:** Durante il download, il pulsante CSV viene sostituito da un unico piccolo indicatore di caricamento azzurro, eliminando la ridondanza visiva e mantenendo pulita l'interfaccia.
*   **Integrazione Web Avanzata (AJAX):** Per garantire la corretta ricezione dei dati, l'app utilizza l'endpoint dedicato `esportaUltimi_ajax.php`. Questo approccio permette di specificare analiticamente i sensori da includere (temperatura, umidità, pressione, CO2, IAQ, ecc.) e di bypassare i reindirizzamenti HTML dei form standard.
*   **Sicurezza e Sessione:** La richiesta include gli header **Referer** e **Accept** per simulare perfettamente il comportamento del browser, assicurando che la sessione rimanga attiva e che le autorizzazioni siano corrette.
*   **Salvataggio su Android:** Utilizza lo *Storage Access Framework* (SAF) per permettere all'utente di scegliere il nome e la cartella di destinazione del file sul dispositivo.

---

## 3. Dettagli Tecnici

### Componenti Creati/Modificati:
1.  **`TauanitoWebClient.kt`**: Implementazione del download tramite endpoint AJAX con parametri di query specifici per ogni tipologia di sensore.
2.  **`SensorRepository.kt`**: Implementato il parser per trasformare l'HTML dello storico in oggetti dati strutturati.
3.  **`Device.kt`**: Aggiornato il modello dati per supportare la cronologia delle letture.
4.  **`HistoryScreen.kt` & `HistoryViewModel.kt`**: Implementata la nuova UI con componenti personalizzati (Surface, Gradient Brush, shadow dinamiche).
5.  **`MainActivity.kt`**: Configurato il routing per la gestione della navigazione verso lo storico.

---

## 4. Guida all'Uso
1.  **Apri l'app** ed effettua il login.
2.  **Tocca il box** di un dispositivo Tauanito.
3.  Nella schermata dello storico, consulta le letture passate.
4.  **Clicca sul badge "CSV"** in alto a destra. L'app recupererà il file in modo sicuro.
5.  **Scegli la destinazione** sul tuo telefono quando richiesto dal sistema Android.

---

*Documento aggiornato al 3 Marzo 2026 (v1.0.8).*

---

## 6. Sicurezza e Accesso Biometrico (v6)

### A. Login con Impronta Digitale (Keystore AES-256)
L'applicazione integra un sistema di autenticazione biometrica allo stato dell'arte:
*   **Accesso Blindato:** Le credenziali sono salvate in un'area criptata hardware inaccessibile dall'esterno.
*   **Login Fulmineo (Cookie Persistence):** L'app memorizza in modo sicuro i cookie di sessione. All'apertura, se la sessione è ancora valida, l'accesso tramite impronta digitale è istantaneo (salta l'intero processo di login web).
*   **Isolamento Totale (v6):** È stato implementato un nuovo database di sicurezza (v6) che azzera ogni residuo precedente. Il sistema garantisce che al primo avvio assoluto l'accesso biometrico sia forzatamente **disattivato**.
*   **Esclusione Backup Cloud:** Per massima sicurezza, i file delle credenziali sono esplicitamente esclusi dal backup automatico di Android (Google Cloud), impedendo il ripristino di dati sensibili su nuovi dispositivi senza autorizzazione manuale.

### B. Gestione dal Menù Laterale
È stata aggiunta la voce **"Accesso Biometrico"** nel drawer:
*   **Switch di Stato:** Permette di attivare/disattivare la blindatura in tempo reale.
*   **Onboarding Manuale:** La funzione può essere attivata solo dopo un primo login manuale riuscito, garantendo che l'app abbia dati validi da criptare.

---

## 7. Stabilità e Connessione (Fix Errori 500)

### A. Gestione Sessione e Redirect
Per garantire la massima compatibilità con i server Tauanito:
*   **Simulazione Browser Mobile:** L'app utilizza un User-Agent Android moderno per evitare blocchi di sicurezza.
*   **Redirect Intelligenti:** Il sistema segue i reindirizzamenti del server (`302 Found`) in modo fluido, stabilizzando la sessione tra il login e il caricamento dei sensori.
*   **Caricamento Differito:** Il refresh dei dati avviene solo dopo l'ingresso nella Dashboard, eliminando conflitti di sincronizzazione durante la fase di autenticazione.

---

## 8. Personalizzazione e Design Avanzato

### A. Tema Dinamico e Glassmorphism
*   **Modalità Chiara "Vivida":** Utilizza box semi-trasparenti (**Glassmorphism**) per i dati, mantenendo visibile l'immagine di sfondo Tauanito.
*   **Phantom Shadows:** Ombre nere al 15% per garantire la leggibilità dei testi chiari su sfondi luminosi senza appesantire il design.

---

## 10. Compatibilità Google Pixel 9 / GrapheneOS (Fix Crash al Avvio)

### Problema Risolto
L'app crashava immediatamente all'avvio su **Google Pixel 9** con sistema operativo **GrapheneOS**. Il crash si presentava prima ancora che apparisse qualsiasi schermata, rendendo l'app completamente inutilizzabile. Sul Motorola con Android standard il problema non si verificava.

### Causa Radice
GrapheneOS **non include Google Play Services**. La libreria `firebase-messaging` che l'app utilizza per le notifiche push contiene un componente chiamato `FirebaseInitProvider` — un `ContentProvider` Android che si avvia **automaticamente prima di `onCreate()`**, fuori da qualsiasi `try/catch`. Su un dispositivo senza Play Services, questo provider andava in crash immediatamente, abbattendo l'intera app prima che potesse mostrare anche solo la schermata di benvenuto.

### Soluzioni Applicate

#### A. Disabilitazione Firebase Auto-Init (AndroidManifest.xml)
Aggiunto nel manifest tre flag per impedire l'auto-inizializzazione di Firebase e dei suoi servizi correlati:

```xml
<meta-data android:name="firebase_auto_init_enabled"            android:value="false" />
<meta-data android:name="firebase_messaging_auto_init_enabled"  android:value="false" />
<meta-data android:name="firebase_analytics_collection_enabled" android:value="false" />
```

Questi flag disattivano il `FirebaseInitProvider` prima che possa causare un crash.

#### B. Inizializzazione Condizionale (MainActivity.kt)
Aggiunto il metodo `isPlayServicesAvailable()` che verifica la presenza del pacchetto `com.google.android.gms` prima di tentare qualsiasi operazione Firebase:

```kotlin
private fun isPlayServicesAvailable(): Boolean {
    return try {
        packageManager.getPackageInfo("com.google.android.gms", 0)
        true
    } catch (e: Exception) { false }
}
```

Firebase (init, token FCM, notifiche push) viene ora inizializzato **solo se Play Services è presente**. Su GrapheneOS l'app si avvia normalmente, ignorando silenziosamente tutto il blocco Firebase.

#### C. Aggiornamento Dipendenze (app/build.gradle.kts)
| Dipendenza | Prima | Dopo | Motivo |
|---|---|---|---|
| `compileSdk` | 34 | **35** | Compatibilità Android 15 (Pixel 9) |
| `targetSdk` | 34 | **35** | Supporto esplicito Android 15 |
| `biometric-ktx:1.2.0-alpha05` | alpha | **`biometric:1.1.0`** (stabile) | Versione alpha instabile su Android 15 |
| `security-crypto:1.1.0-alpha06` | alpha, inutilizzata | **rimossa** | Causava problemi su Android 15, non usata |

### Impatto Funzionale
*   **GrapheneOS (senza Play Services):** l'app funziona completamente — login, dashboard sensori, storico, biometria. Le sole notifiche push FCM non sono disponibili (impossibile senza Play Services).
*   **Android standard (con Play Services):** nessuna variazione, Firebase continua a funzionare normalmente.

### File Modificati
1.  **`app/build.gradle.kts`** — compileSdk 35, targetSdk 35, dipendenze aggiornate/rimosse.
2.  **`AndroidManifest.xml`** — aggiunti 3 flag di disabilitazione Firebase auto-init.
3.  **`MainActivity.kt`** — aggiunto `isPlayServicesAvailable()`, Firebase condizionato alla sua presenza.

---

## 11. Fix Biometrica e Ordinamento — Sprint Marzo 2026

### A. Reset Biometria ad Ogni Avvio (Fix Definitivo)

#### Problema
L'accesso biometrico compariva al primo avvio dell'app anche quando non avrebbe dovuto essere disponibile. Le `SharedPreferences` persistono tra una sessione e l'altra (e tra aggiornamenti APK senza disinstallazione), quindi il flag `isBiometricEnabled = true` sopravviveva al riavvio dell'app, mostrando il pulsante o il popup biometrico inaspettatamente.

#### Soluzione — Reset in `MainActivity.onCreate()`
All'avvio dell'app (prima che Compose venga inizializzato) viene eseguito un reset esplicito:

```kotlin
// La biometria parte sempre disabilitata ad ogni avvio.
// L'utente la riattiva manualmente dopo aver effettuato il login.
SecurePreferences.setBiometricEnabled(this, false)
```

Questo garantisce che:
- Il toggle nel drawer parte sempre su **OFF** ad ogni avvio a freddo
- Il pulsante biometrico nella schermata di login non compare mai al primo accesso

---

### B. Biometrica Basata su Sessione (Flag `sessionLoginCompleted`)

#### Problema
Anche nascondendo il pulsante all'avvio, il pulsante tornava a comparire in scenari di logout/rientro nella stessa sessione basandosi sulle prefs, che potevano essere obsolete.

#### Soluzione — Companion Object in `LoginViewModel`
È stato introdotto un flag **in memoria** che si azzera ad ogni riavvio del processo:

```kotlin
companion object {
    // false ad ogni avvio a freddo, true dopo il primo login manuale nella sessione
    private var sessionLoginCompleted = false
}
```

**Flusso completo:**

| Momento | `sessionLoginCompleted` | `isBiometricEnabled` (prefs) | Pulsante biometrico |
|---|---|---|---|
| Avvio app (freddo) | `false` | `false` (resettato in `onCreate`) | **Nascosto** |
| Dopo login email+password | `true` | `false` (default) | Nascosto |
| Utente attiva biometria nel drawer | `true` | `true` | **Visibile** |
| Logout e ritorno al login | `true` (persiste nel processo) | `true` | **Visibile** |
| App chiusa e riaperta | `false` (nuovo processo) | `false` (reset in `onCreate`) | **Nascosto** |

#### Campo aggiunto a `LoginUiState`
```kotlin
data class LoginUiState(
    ...
    val showBiometricButton: Boolean = false  // gestito dal ViewModel, non dalle prefs
)
```
La visibilità del pulsante viene letta da `state.showBiometricButton` e non più direttamente dalle `SharedPreferences`.

---

### C. Rimosso Auto-Trigger Biometrico alla Schermata di Login

#### Problema
Un `LaunchedEffect(Unit)` in `LoginRoute` mostrava automaticamente il popup biometrico ogni volta che l'utente arrivava alla schermata di login, senza che l'utente avesse toccato nulla.

#### Soluzione
Il blocco `LaunchedEffect` è stato rimosso completamente. La biometria è ora **solo su richiesta esplicita** dell'utente tramite il pulsante dedicato, che compare solo nelle condizioni corrette (post-login, biometria attiva).

---

### D. Ordinamento per Data in Filtri Dashboard

#### Problema
I dispositivi mostrati con il filtro **"Batteria Scarica"** e con la **ricerca per cliente** non erano ordinati per data di invio dati, rendendo difficile identificare i dispositivi più recenti.

#### Causa
Il pattern di timestamp dalla pagina web è `"27.02.2026 - 12:39"` (punti come separatori della data, trattino tra data e ora), ma `parseTimestamp()` usava il formato `"dd/MM/yyyy HH:mm"` (slash, nessun trattino), che non corrispondeva mai → tutti i timestamp restituivano `0L` → ordinamento casuale.

#### Soluzione — `SensorViewModel.kt`
```kotlin
// Prima (errato):
SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

// Dopo (corretto):
SimpleDateFormat("dd.MM.yyyy - HH:mm", Locale.getDefault())
```

L'ordinamento discendente per data (`sortedByDescending`) è stato esteso anche al filtro cliente:
```kotlin
return if (showLowBatteryOnly || selectedCustomer != null) {
    result.sortedByDescending { parseTimestamp(it.timestamp) }
} else {
    result
}
```

---

### E. Messaggi di Errore di Rete Leggibili

#### Problema
In caso di assenza di connessione internet, l'app mostrava il messaggio tecnico grezzo dell'eccezione Java: `"Unable to resolve host "www.tauanito.it": No address associated with hostname"`.

#### Soluzione — `LoginViewModel.kt`
```kotlin
val msg = when {
    e is java.net.UnknownHostException ->
        "Impossibile raggiungere il server. Verifica la connessione internet."
    e.message?.contains("timeout", ignoreCase = true) == true ->
        "Connessione lenta. Riprova."
    e.message?.contains("Email o password") == true -> e.message!!
    else -> "Errore di connessione. Riprova."
}
```

---

### F. Aggiornamento Versione Prefs (`v8` → `v9`)

Il nome delle `SharedPreferences` è stato aggiornato da `tauanito_prefs_v8` a `tauanito_prefs_v9` per invalidare lo stato biometrico ereditato da sessioni precedenti (approccio consistente con le versioni precedenti v6→v7→v8).

---

### File Modificati in questo Sprint
| File | Modifica |
|---|---|
| `MainActivity.kt` | Reset `isBiometricEnabled = false` in `onCreate()` |
| `LoginViewModel.kt` | `companion object sessionLoginCompleted`, `showBiometricButton` in state, messaggi errore rete |
| `LoginScreen.kt` | Rimosso `LaunchedEffect` auto-trigger, pulsante legge `state.showBiometricButton` |
| `SensorViewModel.kt` | Fix formato `parseTimestamp`, ordinamento per cliente esteso |
| `SecurePreferences.kt` | Prefs rinominate `tauanito_prefs_v9` |

---

## 9. Dettagli Tecnici Aggiornati

### Nuovi Componenti Core:
1.  **`SecurePreferences.kt` (v6)**: Gestore della memoria criptata e della persistenza dei cookie di sessione.
2.  **`PersistentCookieJar.kt`**: Implementazione personalizzata di OkHttp per il salvataggio dei cookie su disco sicuro.
3.  **`NetworkModule.kt`**: Configurazione ottimizzata della rete con timeout ricalibrati e gestione redirect.
4.  **`BiometricHelper.kt`**: Interfaccia ufficiale verso i sensori biometrici di Android.
5.  **`SensorViewModel.kt`**: Logica di monitoraggio e ordinamento cronologico dinamico.
6.  **`MainActivity` (FragmentActivity)**: Supporto necessario per la biometria.

---

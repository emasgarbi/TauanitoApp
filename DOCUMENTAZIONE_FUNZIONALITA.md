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

*Documento aggiornato al 3 Marzo 2026.*

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

## 9. Dettagli Tecnici Aggiornati

### Nuovi Componenti Core:
1.  **`SecurePreferences.kt` (v6)**: Gestore della memoria criptata e della persistenza dei cookie di sessione.
2.  **`PersistentCookieJar.kt`**: Implementazione personalizzata di OkHttp per il salvataggio dei cookie su disco sicuro.
3.  **`NetworkModule.kt`**: Configurazione ottimizzata della rete con timeout ricalibrati e gestione redirect.
4.  **`BiometricHelper.kt`**: Interfaccia ufficiale verso i sensori biometrici di Android.
5.  **`SensorViewModel.kt`**: Logica di monitoraggio e ordinamento cronologico dinamico.
6.  **`MainActivity` (FragmentActivity)**: Supporto necessario per la biometria.

---

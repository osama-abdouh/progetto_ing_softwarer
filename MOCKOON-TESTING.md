# Test API con Mockoon

## Cos'è Mockoon?
Mockoon è uno strumento gratuito e open-source per creare mock API server in pochi secondi, senza bisogno di codice. Perfetto per testare il frontend senza il backend reale.

## Installazione

### Opzione 1: Desktop App (Consigliato)
1. Scarica Mockoon da: https://mockoon.com/download/
2. Installa l'applicazione desktop
3. Avvia Mockoon

### Opzione 2: CLI (Per sviluppatori esperti)
```bash
npm install -g @mockoon/cli
```

## Come usare il file di configurazione

### Con Desktop App
1. Apri Mockoon Desktop
2. Clicca su **File** → **Open environment**
3. Seleziona il file `mockoon-api-config.json` dalla root del progetto
4. Clicca sul pulsante **Start** (play verde) per avviare il mock server
5. Il server mock sarà disponibile su `http://localhost:8080`

### Con CLI
```bash
mockoon-cli start --data mockoon-api-config.json
```

## API Endpoints Disponibili

Il file di configurazione include i seguenti endpoint mockati:

### Autenticazione
- **POST** `/api/auth/login` - Login utente
  - Body: `{ "email": "mario.rossi@email.com", "password": "password123" }`
  - Risposta: Token JWT + dati utente

- **POST** `/api/auth/register` - Registrazione nuovo utente
  - Body: `{ "nome": "Mario", "cognome": "Rossi", "email": "...", "username": "...", "password": "..." }`
  - Risposta: Conferma registrazione

### Catalogo Prodotti
- **GET** `/api/catalogo/prodotti` - Lista tutti i prodotti
  - Risposta: Array di prodotti con dettagli

- **GET** `/api/catalogo/prodotto/:id` - Dettaglio singolo prodotto
  - Esempio: `/api/catalogo/prodotto/1`
  - Risposta: Oggetto prodotto con tutti i dettagli

### Carrello
- **GET** `/api/carrello` - Ottieni carrello utente
  - Header: `Authorization: Bearer <token>`
  - Risposta: Array prodotti nel carrello

- **POST** `/api/carrello` - Aggiungi prodotto al carrello
  - Header: `Authorization: Bearer <token>`
  - Body: `{ "id_prodotto": 1, "quantita": 1 }`
  - Risposta: Conferma aggiunta

### Wishlist
- **GET** `/api/wishlist` - Ottieni wishlist utente
  - Header: `Authorization: Bearer <token>`
  - Risposta: Array prodotti preferiti

### Ordini
- **GET** `/api/orders/user/:userId` - Ottieni ordini utente
  - Esempio: `/api/orders/user/1`
  - Risposta: Array ordini con dettagli

### Checkout
- **POST** `/api/acquisti/checkout` - Completa ordine
  - Header: `Authorization: Bearer <token>`
  - Body: Dati pagamento e indirizzo
  - Risposta: Conferma ordine + tracking number

### Admin
- **GET** `/api/products/load` - Carica tutti i prodotti (Admin)
  - Risposta: Array completo prodotti per gestione

## Testare con Postman o Insomnia

Puoi importare gli endpoint anche in Postman:

1. In Postman, vai su **Import**
2. Seleziona il file `mockoon-api-config.json`
3. Gli endpoint verranno importati come collection

## Modificare le Risposte Mock

### Con Desktop App
1. Apri l'endpoint che vuoi modificare
2. Nella sezione **Response**, modifica il JSON
3. Salva le modifiche
4. Le nuove risposte saranno immediate

### Nel file JSON
Puoi modificare direttamente `mockoon-api-config.json`:
- Trova l'endpoint in `routes[]`
- Modifica il campo `responses[].body`
- Riavvia il server mock

## Usare il Mock Server nel Frontend

Nel tuo Angular app, puoi switchare tra backend reale e mock:

```typescript
// src/app/config/api.config.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',  // Può puntare a Mockoon
  useMockServer: true  // Toggle per mock/real backend
};
```

## CORS Configuration

Il mock server è già configurato con CORS abilitato per accettare richieste da qualsiasi origine (`Access-Control-Allow-Origin: *`). Questo permette al frontend Angular di comunicare senza problemi.

## Vantaggi del Testing con Mockoon

✅ **Sviluppo Frontend Indipendente** - Non serve il backend Java avviato
✅ **Testing Rapido** - Risposte immediate senza latenza database
✅ **Dati Consistenti** - Sempre le stesse risposte per testing affidabile
✅ **Simulazione Errori** - Puoi creare risposte di errore per testare edge cases
✅ **No Database** - Nessun rischio di corrompere dati reali

## Workflow Consigliato

1. **Fase 1**: Usa Mockoon per sviluppare UI e logica frontend
2. **Fase 2**: Testa con backend reale quando la feature è completa
3. **Fase 3**: Usa Mockoon per regression testing veloce

## Troubleshooting

### Porta 8080 già in uso
Se il backend Java è già avviato sulla porta 8080:
1. Ferma il backend Java
2. Avvia Mockoon sulla porta 8080
OPPURE
1. Cambia la porta di Mockoon (es. 8081) nel file config
2. Aggiorna `api.config.ts` nel frontend

### CORS Errors
Se vedi errori CORS:
1. Verifica che CORS sia abilitato in Mockoon (già impostato nel config)
2. Controlla che l'URL nel frontend sia corretto

## Risorse Utili

- 📖 Documentazione: https://mockoon.com/docs/
- 🎥 Tutorial video: https://mockoon.com/tutorials/
- 💬 Community: https://github.com/mockoon/mockoon/discussions

## Prossimi Passi

Dopo aver familiarizzato con Mockoon, considera:
- Aggiungere più endpoint per pacchetti, coupon, indirizzi
- Creare multiple risposte (success, error 404, error 500)
- Utilizzare variabili dinamiche con template helpers
- Simulare latenza per testare loading states

---

**Nota**: Questo è un ambiente di testing. I dati sono fittizi e non influenzano il database reale.

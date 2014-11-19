ZimbraFlow
========
ZimbraFlow è un'estensione di Zimbra che utilizza la libreria [OpenZAL](http://openzal.org) e non va in conflitto con altre estensioni ZAL-based eventualmente presenti nel sistema.  
E' compatibile con OpenZAL 1.5 e Zimbra dalla version 6.0.7 alla versione 8.5.1.

La versione di OpenZAL compatibile piu' recente puo' essere trovata all'indirizzo http://openzal.org/1.5/zal-1.5-{zimbraVersion}.jar - es. la versione di OpenZAL compilata per Zimbra 8.5.0 è disponibile all'indirizzo [http://openzal.org/1.5/zal-1.5-8.5.0.jar](http://openzal.org/1.5/zal-1.5-8.5.0.jar)

La zimlet manda una richiesta all'estensione ZimbraFlow contenente l'Id del messaggio selezionato e l'indirizzo del servizio SOAP.  
L'estensione estrae il messaggio da Zimbra, anche se contenuto in cartelle condivise, e spedisce una richiesta SOAP contenente alcuni attributi del messaggio.

Nel file ZimbraFlowHandler.java è possibile configurare gli attributi del messaggio da inviare nella richiesta SOAP.  
Modificando il file config_template.xml è possibile impostare l'indirizzo del servizio SOAP: per impostare il file di configurazione per una zimlet utilizzare il comando  
'zmzimletctl configure config_template.xml'

Installazione
-------
* Crea la cartella per l'estensione ZimbraFlow:
```
mkdir /opt/zimbra/lib/ext/ZimbraFlow
```
* Sposta i jar zal.jar (per la tua versione di zimbra) e ZimbraFlow.jar nella cartella:
```
mv zal.jar ZimbraFlow.jar /opt/zimbra/lib/ext/ZimbraFlow/
```
* Riavvia il servizio mailbox dei server conivolti:
```
zmmailboxdctl restart
```
* Installa la zimlet com_ZimbraFlow.zip direttamente dalla console amministrativa oppure via CLI con il comando:
```
zmzimletctl deploy com_ZimbraFlow.zip
```

Esempio
-------
Nell'implementazione di esempio i seguenti campi sono inclusi nella richiesta SOAP:

* "cUserEmail": indirizzo dell'utente che effettua la richiesta (\*)
* "cEmailUniqueID": Id zimbra del messaggio
* "cEmailMessageID": Message-ID del messaggio
* "cDateTime": data di ricezione del messaggio (\*)
* "cFrom": mittente del messaggio
* "cTO": destinatario del messaggio 
* "cCC": destinatari del messaggio
* "cCCN": destinatari nascosti del messaggio
* "cSubject": soggetto del messaggio
* "cBody": corpo del messaggio
* "StreamBase64": tutto il messaggio (compresi eventuali allegati) encodato in base64 (\*)


\[NB: i campi contrassegnati con \* devono essere necessariamente presenti nel messaggio per poter costruire la richiesta.


Quando la richiesta causa un'eccezione il contenuto di questa viene mostrato in un alert dal browser.  
Se la richiesta viene inviata correttamente ma la riposta inizia con la sigla KO il messaggio è interpretato come un errore e mostrato in un popup all'interno del WebClient.  
Se la risposta inizia con la sigla OK il messaggio viene mostrato in un popup all'interno del WebClient che segnala il successo della richiesta SOAP.

Richiesta creata dall'estensione:
```
<?xml version="1.0" encoding="utf-8"?>
  <soap12:Envelope xmlnssi="http://www.w3.org/2001/XMLSchema-instance" xmlnssd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
  <soap12:Body>
    <EmailInfo xmlns=" http://tempuri.org/ ">
      <cUserEmail> string </cUserEmail>
      <cDateTime> string </cDateTime>
      <cFrom> string </cFrom>
      <cTO> string </cTO>
      <cCC> string </cCC>
      <cCCN> string </cCCN>
      <cSubject> string </cSubject>
      <cBody> string </cBody>
      <StreamBase64> string </StreamBase64>
    </EmailInfo>
  </soap12:Body>
</soap12:Envelope>
```

Risposta di esempio di un SOAP service:
```
<?xml version="1.0" encoding="utf-8"?>
  <soap12:Envelope xmlnssi=" http://www.w3.org/2001/XMLSchema-instance" xmlnssd="http://www.w3.org/2001/XMLSchema" xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
  <soap12:Body>
    <EmailInfoResponse xmlns=" http://tempuri.org/ ">
      <EmailInfoResult>OKMessaggio di conferma</EmailInfoResult>
    </EmailInfoResponse>
  </soap12:Body>
</soap12:Envelope>
```

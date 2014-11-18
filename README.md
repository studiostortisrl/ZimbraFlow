ZimbraFlow
========
ZimbraFlow è un'estensione di Zimbra che utilizza la libreria [OpenZAL](http://openzal.org).  
E' compatibile con OpenZAL 1.5 e Zimbra dalla version 6.0.7 alla versione 8.5.1.

La versione di OpenZAL compatibile piu' recente puo' essere trovata all'indirizzo http://openzal.org/1.5/zal-1.5-{zimbraVersion}.jar - es. la versione di OpenZAL compilata per Zimbra 8.5.0 è disponibile all'indirizzo [http://openzal.org/1.5/zal-1.5-8.5.0.jar](http://openzal.org/1.5/zal-1.5-8.5.0.jar)

La zimlet manda una richiesta all'estensione ZimbraFlow contenente l'Id del messaggio selezionato e l'indirizzo del servizio SOAP.  
L'estensione estrae il messaggio da Zimbra, e spedisce una richiesta SOAP contenente alcuni attributi del messaggio.

Nel file ZimbraFlowHandler.java è possibile configurare gli attributi del messaggio da inviare nella richiesta SOAP.  
Modificando il file config_template.xml è possibile impostare l'indirizzo del servizio SOAP: per impostare il file di configurazione per una zimlet utilizzare il comando  
'zmzimletctl configure config_template.xml'

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

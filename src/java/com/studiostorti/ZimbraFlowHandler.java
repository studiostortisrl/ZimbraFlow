/*
 * ZimbraFlow send an email to SOAP services
 * Copyright (C) 2014 Studio Storti S.r.l.
 *
 * This file is part of ZimbraFlow.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, version 2 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ZimbraFlow. If not, see <http://www.gnu.org/licenses/>.
 */

package com.studiostorti;

import org.openzal.zal.Account;
import org.openzal.zal.Mailbox;
import org.openzal.zal.MailboxManager;
import org.openzal.zal.Message;
import org.openzal.zal.OperationContext;
import org.openzal.zal.Provisioning;
import org.openzal.zal.log.ZimbraLog;
import org.openzal.zal.soap.QName;
import org.openzal.zal.soap.SoapHandler;
import org.openzal.zal.soap.SoapResponse;
import org.openzal.zal.soap.ZimbraContext;
import org.openzal.zal.soap.ZimbraExceptionContainer;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.codec.binary.Base64;
import javax.xml.soap.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ZimbraFlowHandler implements SoapHandler
{
  static final private String sNAMESPACE     = "urn:zimbraAccount";
  static final         QName  sREQUEST_QNAME = new QName("ZimbraFlow", sNAMESPACE);
  private final Provisioning   mProvisioning;
  private final MailboxManager mMailboxManager;

  public ZimbraFlowHandler()
  {
    mProvisioning = new Provisioning();
    mMailboxManager = new MailboxManager();
  }

  private String getText(Part mimeMessage) throws MessagingException, IOException
  {
    if (mimeMessage != null)
    {
      if (mimeMessage.isMimeType("text/plain"))
      {
        return (String) mimeMessage.getContent();
      }

      if (mimeMessage.isMimeType("multipart/alternative"))
      {
        Multipart multipart = (Multipart) mimeMessage.getContent();
        return getText(multipart.getBodyPart(0));
      }
      else if (mimeMessage.isMimeType("multipart/*"))
      {
        Multipart multipart = (Multipart) mimeMessage.getContent();
        for (int i = 0; i < multipart.getCount(); i++)
        {
          String body = getText(multipart.getBodyPart(i));
          if (!body.equals(""))
            return body;
        }
      }
    }

    return "";
  }

  @Override
  public void handleRequest(ZimbraContext zimbraContext, SoapResponse soapResponse, ZimbraExceptionContainer zimbraExceptionContainer)
  {
    String requesterId = zimbraContext.getAuthenticatedAccontId();
    Mailbox mailbox = mMailboxManager.getMailboxByAccountId(requesterId);
    Account account = mProvisioning.getAccountById(requesterId);
    Map<String, String> args = new HashMap<String, String>();

    if (account == null)
    {
      soapResponse.setValue("reply", "KONon e' stato possibile trovare l'account : " + requesterId);

      return;
    }

    OperationContext octxt = new OperationContext(account);
    String msgId = zimbraContext.getParameter("id", "");
    String namespace = zimbraContext.getParameter("namespace", "");
    String url = zimbraContext.getParameter("url", "");
    String user = account.getName();
    ZimbraLog.extensions.info("Azione ZimbraFlow richiesta da '" + user + "' per il messaggio: '" + msgId + "'");
    args.put("cUserEmail", user);

    Message item;
    if (! msgId.contains(":"))
    {
      try
      {
        item = mailbox.getMessageById(octxt, Integer.parseInt(msgId));
      }
      catch (Exception e)
      {
        soapResponse.setValue("reply", "KONon e' stato possibile recuperare il messaggio ("+msgId+"): " + e.getMessage());

        return;
      }

      ZimbraLog.mailbox.info("ZimbraFlow mail id : " + msgId);
      args.put("cEmailUniqueID", account.getId() + ":" + msgId);
    }
    else
    {
      ZimbraLog.mailbox.info("ZimbraFlow il messaggio e' una cartella condivisa");
      String accountId = msgId.substring(0, msgId.indexOf(':'));
      String itemId = msgId.substring(msgId.indexOf(':') + 1);

      try
      {
        Mailbox ownerMailbox = mMailboxManager.getMailboxByAccountId(accountId);
        item = ownerMailbox.getMessageById(octxt, Integer.parseInt(itemId));
      }
      catch (Exception e)
      {
        soapResponse.setValue("reply", "KONon e' stato possibile recuperare il messaggio ("+msgId+"): " + e.getMessage());

        return;
      }

      args.put("cEmailUniqueID", msgId);
    }
    if (item == null) {
      soapResponse.setValue("reply", "KONon e' stato possibile recuperare il messaggio ("+msgId+".");

      return;
    }

    MimeMessage mimeMessage = null;
    try {
      mimeMessage = item.getMimeMessage();
      args.put("cEmailMessageID", mimeMessage.getMessageID());
    } catch (MessagingException e) {
      ZimbraLog.mailbox.warn("ZimbraFlow errore cEmailMessageID: " + e.getMessage());
    }

    byte[] mime = item.getContent();

    args.put("StreamBase64", Base64.encodeBase64String(mime));

    String subject;
    subject = item.getSubject();
    args.put("cSubject", subject);

    String body;
    try {
      body = getText(mimeMessage);
    } catch (MessagingException e) {
      ZimbraLog.mailbox.warn("ZimbraFlow errore cBody: " + e.getMessage());
      body = "";
    } catch (IOException e) {
      ZimbraLog.mailbox.warn("ZimbraFlow errore cBody: " + e.getMessage());
      body = "";
    }
    args.put("cBody", body);

    try {
      Address from = mimeMessage.getFrom()[0];
      args.put("cFrom", ((InternetAddress) from).getAddress());
    } catch (NullPointerException ne) {
      ZimbraLog.mailbox.warn("ZimbraFlow errore cFrom: " + ne.getMessage());
      args.put("cFrom", "");
    } catch (MessagingException e) {
      ZimbraLog.mailbox.warn("ZimbraFlow errore cFrom: " + e.getMessage());
      args.put("cFrom", "");
    }

    try {
      Address[] toRecipients = mimeMessage.getRecipients(javax.mail.Message.RecipientType.TO);
      String toString = "";
      for (Address to : toRecipients)
      {
        toString += ((InternetAddress) to).getAddress() + ",";
      }
      if (toString.length()>0)
        args.put("cTO", toString.substring(0, toString.length() - 1));
      else
        args.put("cTO", toString);
    } catch (MessagingException ignored) {}
    catch (NullPointerException ne) {
      ZimbraLog.mailbox.warn("ZimbraFlow errore cTo: " + ne.getMessage());
      args.put("cTO", "");
    }

    try {
      Address[] ccRecipients = mimeMessage.getRecipients(javax.mail.Message.RecipientType.CC);
      String ccString = "";
      if (ccRecipients != null)
      {
        for (Address cc : ccRecipients)
        {
          ccString += ((InternetAddress) cc).getAddress() + ",";
        }
      }
      if (ccString.length()>0)
        args.put("cCC", ccString.substring(0, ccString.length() - 1));
      else
        args.put("cCC", ccString);
    } catch (MessagingException ignored) {}
    catch (NullPointerException ne) {
      ZimbraLog.mailbox.warn("ZimbraFlow errore cCC: " + ne.getMessage());
      args.put("cCC", "");
    }

    try {
      Address[] bccRecipients = mimeMessage.getRecipients(javax.mail.Message.RecipientType.BCC);
      String bccString = "";
      if (bccRecipients != null)
      {
        for (Address bcc : bccRecipients)
        {
          bccString += ((InternetAddress) bcc).getAddress() + ",";
        }
      }
      if (bccString.length()>0)
        args.put("cCCN", bccString.substring(0, bccString.length() - 1));
      else
        args.put("cCCN", bccString);

    } catch (MessagingException ignored) {}
    catch (NullPointerException ne) {
      ZimbraLog.mailbox.warn("ZimbraFlow errore cCCN: " + ne.getMessage());
      args.put("cCCN", "");
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd hh:mm:ss");
    long date = item.getDate();
    args.put("cDateTime", dateFormat.format(new Date(date)));

    if (args.get("cDateTime") == null || args.get("cUserEmail") == null || args.get("StreamBase64") == null)
    {
      String message = "KONon sono stati trovati tutti i campi obbligatori:";
      if (args.get("cDateTime") == null)
        message += "\nManca il campo cDateTime";
      else if (args.get("cUserEmail") == null)
        message += "\nManca  il campo cUserEmail";
      else if (args.get("StreamBase64") == null)
        message += "\nManca il campo StreamBase64";

      soapResponse.setValue("reply", message);

      return;
    }

    SOAPClient soapClient = new SOAPClient(url, namespace);
    try {
      String res = soapClient.sendRequest(args);
      soapResponse.setValue("reply", res);
    } catch (SOAPException e) {
      ZimbraLog.mailbox.error("ZimbraFlow SOAP call exception: "
                                + e.getMessage());
      soapResponse.setValue("reply", "KO" + e.getMessage());
    }
  }

  @Override
  public boolean needsAdminAuthentication(ZimbraContext zimbraContext)
  {
    return false;
  }

  @Override
  public boolean needsAuthentication(ZimbraContext zimbraContext)
  {
    return false;
  }

  public static class SOAPClient {

    private final String mUrl;
    private final String mNamespace;

    public SOAPClient(String url, String namespace)
    {
      mUrl = url;
      mNamespace = namespace;
    }

    public String sendRequest(Map<String, String> args) throws SOAPException
    {
      // Create SOAP Connection
      SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
      SOAPConnection soapConnection = soapConnectionFactory.createConnection();

      // Send SOAP Message to SOAP Server
      SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(args), mUrl);

      soapConnection.close();

      return readResponse(soapResponse);
    }

    public SOAPMessage createSOAPRequest(Map<String, String> args) throws SOAPException
    {
      MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
      SOAPMessage soapMessage = messageFactory.createMessage();
      soapMessage.getSOAPBody().setPrefix("soap12");

      soapMessage.setProperty(SOAPMessage.CHARACTER_SET_ENCODING, "utf-8");
      soapMessage.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");

      SOAPPart soapPart = soapMessage.getSOAPPart();
      // SOAP Envelope
      SOAPEnvelope envelope = soapPart.getEnvelope();
      envelope.setPrefix("soap12");
      envelope.removeNamespaceDeclaration("env");
      envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
      envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
      envelope.addNamespaceDeclaration("soap12", "http://www.w3.org/2003/05/soap-envelope");

      // SOAP Body
      SOAPBody soapBody = envelope.getBody();
      SOAPElement soapBodyElem = soapBody.addChildElement("EmailInfo", "", mNamespace);
      String userEmail = args.get("cUserEmail");
      SOAPElement soapBodyElemUser = soapBodyElem.addChildElement("cUserEmail");
      soapBodyElemUser.addTextNode(userEmail);

      String uniqueId = args.get("cEmailUniqueID");
      if (uniqueId != null && uniqueId.length() > 0)
      {
        SOAPElement soapBodyElemUniqueId = soapBodyElem.addChildElement("cEmailUniqueID");
        soapBodyElemUniqueId.addTextNode(uniqueId);
      }

      String messageId = args.get("cEmailMessageID");
      if (messageId != null && messageId.length() > 0)
      {
        SOAPElement soapBodyElemMessageId = soapBodyElem.addChildElement("cEmailMessageID");
        soapBodyElemMessageId.addTextNode(messageId);
      }

      String dateTime = args.get("cDateTime");
      SOAPElement soapBodyElemDate = soapBodyElem.addChildElement("cDateTime");
      soapBodyElemDate.addTextNode(dateTime);

      String from = args.get("cFrom");
      if (from != null && from.length() > 0)
      {
        SOAPElement soapBodyElemFrom = soapBodyElem.addChildElement("cFrom");
        soapBodyElemFrom.addTextNode(from);
      }

      String to = args.get("cTO");
      if (to != null && to.length() > 0)
      {
        SOAPElement soapBodyElemTo = soapBodyElem.addChildElement("cTO");
        soapBodyElemTo.addTextNode(to);
      }

      String cc = args.get("cCC");
      if (cc != null && cc.length() > 0)
      {
        SOAPElement soapBodyElemCC = soapBodyElem.addChildElement("cCC");
        soapBodyElemCC.addTextNode(cc);
      }

      String ccn = args.get("cCCN");
      if (ccn != null && ccn.length() > 0)
      {
        SOAPElement soapBodyElemCCN = soapBodyElem.addChildElement("cCCN");
        soapBodyElemCCN.addTextNode(ccn);
      }

      String subject = args.get("cSubject");
      if (subject != null && subject.length() > 0)
      {
        SOAPElement soapBodyElemSubject = soapBodyElem.addChildElement("cSubject");
        soapBodyElemSubject.addTextNode(subject);
      }

      String body = args.get("cBody");
      if (body != null && body.length() > 0)
      {
        SOAPElement soapBodyElemBody = soapBodyElem.addChildElement("cBody");
        soapBodyElemBody.addTextNode(body);
      }

      String streamBase64 = args.get("StreamBase64");
      SOAPElement soapBodyElemMime = soapBodyElem.addChildElement("StreamBase64");
      soapBodyElemMime.addTextNode(streamBase64);

      MimeHeaders headers = soapMessage.getMimeHeaders();
      headers.setHeader("SOAPAction", mNamespace + "/EmailInfo");
      headers.setHeader("Content-Type", "text/xml");

      soapMessage.saveChanges();

      return soapMessage;
    }

    private String readResponse(SOAPMessage soapResponse) throws SOAPException
    {
      SOAPBody body = soapResponse.getSOAPBody();
      return body.getTextContent();
    }
  }
}


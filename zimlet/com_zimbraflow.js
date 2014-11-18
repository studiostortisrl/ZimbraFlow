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

function com_zimbraflow_HandlerObject() {}

com_zimbraflow_HandlerObject.prototype = new ZmZimletBase();
com_zimbraflow_HandlerObject.prototype.constructor = com_zimbraflow_HandlerObject;

var ZimbraFlowZimlet = com_zimbraflow_HandlerObject;

ZimbraFlowZimlet.is_array = function (obj) {
  return Object.prototype.toString.apply(obj) === '[object Array]';
};

ZimbraFlowZimlet.prototype.doDrop = function (zmObject) {
  if (ZimbraFlowZimlet.is_array(zmObject)) {
    appCtxt.getAppController().setStatusMsg(
      'ZimbraFlow: Selezionare un solo messaggio di posta',
      ZmStatusView.LEVEL_WARNING
    );
  } else if (zmObject.TYPE === 'ZmConv') {
    this._displayDialog(zmObject.srcObj.getFirstHotMsg());
  } else if (zmObject.TYPE === 'ZmMailMsg') {
    var msgObj = zmObject.srcObj;//get access to source-object
    this._displayDialog(msgObj);
  }
};

ZimbraFlowZimlet.prototype._sendToZimbraFlow = function (id) {
  var url = this._zimletContext.getConfig('url'),
    soapDoc = AjxSoapDoc.create('ZimbraFlow', 'urn:zimbraAccount'),
    params;

  soapDoc.set('id', id);
  soapDoc.set('url', url);

  params = {
    soapDoc: soapDoc,
    asyncMode: true,
    callback: new AjxCallback(this, this._displayDialogOK),
    errorCallback: new AjxCallback(this, this._handleSOAPErrorResponseJSON)
  };

  return appCtxt.getAppController().sendRequest(params);
};

ZimbraFlowZimlet.prototype._handleSOAPErrorResponseJSON = function (result) {
  alert('Il server ' + this._zimletContext.getConfig('url') + ' ha risposto con un\'eccezione:\n' + result);
};

ZimbraFlowZimlet.prototype._displayDialogOK = function (result) {
  var msg = result._data.response.reply;

  if (msg.substring(0, 2) === 'OK') {
    appCtxt.getAppController().setStatusMsg('ZimbraFlow: ' + msg.substring(2));
  } else if (msg.substring(0, 2) === 'KO') {
    appCtxt.getAppController().setStatusMsg('ZimbraFlow: ' + 'error', ZmStatusView.LEVEL_CRITICAL);
    alert(msg.substring(2));
  } else {
    appCtxt.getAppController().setStatusMsg('ZimbraFlow: ' + msg, ZmStatusView.LEVEL_WARNING);
  }
};

/**
 * This method gets called by the Zimlet framework when a toolbar is created.
 *
 * @param {ZmApp} app
 * @param {ZmButtonToolBar} toolbar
 * @param {ZmController} controller
 * @param {String} viewId
 *
 */
ZimbraFlowZimlet.prototype.initializeToolbar = function (app, toolbar, controller, viewId) {
  var buttonIndex = 0,
    i,
    buttonParams,
    button;

  // get the index of "View" menu so we can display the button after that
  if (viewId !== 'CLV-main' && viewId !== 'TV-main' && viewId.substring(0, 4) !== 'MSG-') {
    return;
  }

  for (i = 0; i < toolbar.opList.length; i += 1) {
    if (toolbar.opList[i] === ZmOperation.VIEW_MENU) {
      buttonIndex = i + 1;
      break;
    }
  }

  buttonParams = {
    text: this.getMessage('label'),
    tooltip: this.getMessage('tooltip'),
    index: buttonIndex
  };

  // creates the button with an id and params containing the button details
  button = toolbar.createOp('ZIMBRAFLOW_BUTTON', buttonParams);
  button.addSelectionListener(new AjxListener(this, this._manageSelectedMail, [controller]));
};

ZimbraFlowZimlet.prototype._manageSelectedMail = function (controller) {
  var selectedItms = controller.getCurrentView().getSelection();

  if (selectedItms.length > 0) {
    this.srcMsgObj = selectedItms[0];
    if (this.srcMsgObj.type === 'CONV') {
      this.srcMsgObj = this.srcMsgObj.getFirstHotMsg();
    }
    this._displayDialog(this.srcMsgObj);
  }
};

ZimbraFlowZimlet.prototype._displayDialog = function (msg) {
  var id = msg.id;
  if (this.pbDialog) { //if zimlet dialog already exists...
    this.pView.getHtmlElement().innerHTML = this._createDialogView(msg); // update content
    this.pbDialog.setButtonListener(DwtDialog.OK_BUTTON, new AjxListener(this, this._okBtnListener, id));
    this.pbDialog.popup(); //simply popup the dialog
    return;
  }

  this.pView = new DwtComposite(this.getShell()); //creates an empty div as a child of main shell div
  //this.pView.setSize("250", "150"); // set width and height
  this.pView.setSize(250); // set width
  this.pView.getHtmlElement().style.overflow = 'auto'; // adds scrollbar
  this.pView.getHtmlElement().innerHTML = this._createDialogView(msg); // insert html to the dialogbox

  // pass the title, view and buttons information and create dialog box
  this.pbDialog = new ZmDialog({
    title: this.getMessage('dialog_title'),
    view: this.pView,
    parent: this.getShell(),
    standardButtons: [ DwtDialog.DISMISS_BUTTON, DwtDialog.OK_BUTTON ]
  });

  this.pbDialog.setButtonListener(DwtDialog.DISMISS_BUTTON, new AjxListener(this, this._dismissBtnListener));
  this.pbDialog.setButtonListener(DwtDialog.OK_BUTTON, new AjxListener(this, this._okBtnListener, id));

  this.pbDialog.popup(); //show the dialog
};

ZimbraFlowZimlet.prototype._createDialogView = function (msg) {
  var ccemails = [],
    bccemails = [],
    toemails = [],
    sender = '',
    participants = msg.participants.getArray(),
    i,
    view = [],
    date = new Date(msg.date),
    dateString = date.getDate() + '/' + (date.getMonth() + 1) + '/' + date.getFullYear(),
    subject = (msg.subject && msg.subject.length > 0) ? msg.subject : 'Dato non trovato';

  for (i = 0; i < participants.length; i += 1) {
    if (i === 0) {
      sender = participants[i].address;
    } else if (participants[i].type === AjxEmailAddress.TO) {
      toemails.push(participants[i].address);
    } else if (participants[i].type === AjxEmailAddress.CC) {
      ccemails.push(participants[i].address);
    } else if (participants[i].type === AjxEmailAddress.BCC) {
      bccemails.push(participants[i].address);
    }
  }

  dateString += ' ' + date.getHours() + ':' + date.getMinutes();
  sender = (sender.length > 0) ? sender : 'Dato non trovato';
  toemails = (toemails.length > 0) ? toemails : 'Dato non trovato';
  ccemails = (ccemails.length > 0) ? ccemails : 'Dato non trovato';
  bccemails = (bccemails.length > 0) ? bccemails : 'Dato non trovato';

  view.push('<div class="preview_date">');
  view.push('<b>Data di ricezione</b>: ');
  view.push(dateString);
  view.push('</div>');
  view.push('<div class="preview_subject">');
  view.push('<b>Oggetto</b>: ');
  view.push(subject);
  view.push('</div>');
  view.push('<div class="preview_from">');
  view.push('<b>Mittente</b>: ');
  view.push(sender);
  view.push('</div>');
  view.push('<div class="preview_to">');
  view.push('<b>Destinatari</b>: ');
  view.push(toemails);
  view.push('</div>');
  view.push('<div class="preview_cc">');
  view.push('<b>CC</b>: ');
  view.push(ccemails);
  view.push('</div>');
  view.push('<div class="preview_bcc">');
  view.push('<b>CCN</b>: ');
  view.push(bccemails);
  view.push('</div>');

  return view.join('');
};

ZimbraFlowZimlet.prototype._dismissBtnListener = function () {
  this.pbDialog.popdown(); // hide the dialog
};

ZimbraFlowZimlet.prototype._okBtnListener = function (id) {
  this.pbDialog.popdown(); // hide the dialog
  appCtxt.getAppController().setStatusMsg(this.getMessage('status_launch'));
  this._sendToZimbraFlow(id);
};

ZimbraFlowZimlet.prototype.singleClicked = function () {
  return;
};

ZimbraFlowZimlet.prototype.doubleClicked = ZimbraFlowZimlet.prototype.singleClicked;

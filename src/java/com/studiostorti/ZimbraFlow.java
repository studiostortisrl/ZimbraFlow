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

import org.openzal.zal.extension.ZalExtension;
import org.openzal.zal.extension.ZalExtensionController;
import org.openzal.zal.extension.Zimbra;
import org.openzal.zal.log.ZimbraLog;
import org.openzal.zal.soap.SoapServiceManager;

public class ZimbraFlow implements ZalExtension
{
  private final SoapServiceManager mSoapServiceManager;

  public ZimbraFlow()
  {
    mSoapServiceManager = new SoapServiceManager();
  }

  @Override
  public String getBuildId()
  {
    return "1";
  }

  public String getName()
  {
    return "ZimbraFlow";
  }

  @Override
  public void startup(ZalExtensionController zalExtensionController, Zimbra zimbra)
  {
    mSoapServiceManager.register(new ZimbraFlowService());
    ZimbraLog.mailbox.info("Loaded extension ZimbraFlow.");
  }

  @Override
  public void shutdown()
  {

  }
}

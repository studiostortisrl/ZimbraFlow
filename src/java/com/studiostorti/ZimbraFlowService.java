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

import org.openzal.zal.soap.QName;
import org.openzal.zal.soap.SoapHandler;
import org.openzal.zal.soap.SoapService;

import java.util.HashMap;
import java.util.Map;

public class ZimbraFlowService implements SoapService
{
  @Override
  public Map<QName, ? extends SoapHandler> getServices()
  {
    return new HashMap<QName, SoapHandler>()
    {{
      put(ZimbraFlowHandler.sREQUEST_QNAME, new ZimbraFlowHandler());
    }};
  }

  @Override
  public String getServiceName()
  {
    return "SoapServlet";
  }

  @Override
  public boolean isAdminService()
  {
    return false;
  }
}


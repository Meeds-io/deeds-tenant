/**
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2023 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

export function getConfiguration() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/deed/tenant/configuration`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error(resp.status);
    }
  });
}

export function getHubStatus() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/deed/tenant/status`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error(resp.status);
    }
  });
}

export function isTenantManager(address, nftId) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/deed/tenant/manager?address=${address}&nftId=${nftId}`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.ok) {
      return resp.text()
        .then(data => data === 'true');
    } else {
      throw new Error(resp.status);
    }
  });
}

export function getDeedTenant(nftId) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/deed/tenant?nftId=${nftId}`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      throw new Error(resp.status);
    }
  });
}

export function connectToWoM(request) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/deed/tenant/connect`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request),
  }).then((resp) => {
    if (!resp?.ok) {
      if (resp.status === 503) {
        return resp.text()
          .then(error => {
            throw new Error(error.split(':')[0]);
          });
      } else {
        throw new Error('wom.errorResponse');
      }
    }
  });
}
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

export function getHub(nftId) {
  const formData = new FormData();
  if (nftId) {
    formData.append('nftId', nftId);
  }
  const params = new URLSearchParams(formData).toString();
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/deed/tenant?${params}`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else if (resp?.status === 404) {
      return null;
    } else {
      return handleResponseError(resp);
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
      return handleResponseError(resp);
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
      return handleResponseError(resp);
    }
  });
}

export function disconnectFromWoM(request) {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/deed/tenant/disconnect`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request),
  }).then((resp) => {
    if (!resp?.ok) {
      return handleResponseError(resp);
    }
  });
}

export function generateToken() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/deed/tenant/token`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.ok) {
      return resp.text();
    } else {
      return handleResponseError(resp);
    }
  });
}

export function getConfiguration() {
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/deed/tenant/configuration`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      return handleResponseError(resp);
    }
  });
}

export function saveHubAvatar(paramsObj) {
  const formData = new FormData();
  if (paramsObj) {
    Object.keys(paramsObj).forEach(key => {
      const value = paramsObj[key];
      if (window.Array && Array.isArray && Array.isArray(value)) {
        value.forEach(val => formData.append(key, val));
      } else {
        formData.append(key, value);
      }
    });
  }
  const params = new URLSearchParams(formData).toString();
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/deed/tenant/avatar`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params,
  }).then((resp) => {
    if (!resp?.ok) {
      return handleResponseError(resp);
    }
  });
}

export function saveHubBanner(paramsObj) {
  const formData = new FormData();
  if (paramsObj) {
    Object.keys(paramsObj).forEach(key => {
      const value = paramsObj[key];
      if (window.Array && Array.isArray && Array.isArray(value)) {
        value.forEach(val => formData.append(key, val));
      } else {
        formData.append(key, value);
      }
    });
  }
  const params = new URLSearchParams(formData).toString();
  return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/deed/tenant/banner`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: params,
  }).then((resp) => {
    if (!resp?.ok) {
      return handleResponseError(resp);
    }
  });
}

function handleResponseError(resp) {
  if (resp.status === 503 || resp.status === 400 || resp.status === 401) {
    return resp.text()
      .then(error => {
        let messageKey = null;
        try {
          messageKey = JSON.parse(error).messageKey.split(':')[0];
        } catch (e) {
          messageKey = error.split(':')[0];
        }
        throw new Error(messageKey.split(':')[0]);
      });
  } else {
    throw new Error('wom.errorResponse');
  }
}
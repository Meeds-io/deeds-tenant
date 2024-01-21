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

export function getHub(nftId, forceRefresh) {
  const formData = new FormData();
  if (nftId) {
    formData.append('nftId', nftId);
  }
  if (forceRefresh === true) {
    formData.append('forceRefresh', true);
  }
  const params = new URLSearchParams(formData).toString();
  return fetch(`/deeds-tenant/rest/hub?${params}`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else if (resp?.status === 404) {
      return null;
    } else {
      return Vue.prototype.$tenantUtils.handleResponseError(resp);
    }
  });
}

export function connectToWoM(request) {
  return fetch('/deeds-tenant/rest/hub', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request),
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      return Vue.prototype.$tenantUtils.handleResponseError(resp);
    }
  });
}

export function disconnectFromWoM(request) {
  return fetch('/deeds-tenant/rest/hub', {
    method: 'DELETE',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request),
  }).then((resp) => {
    if (!resp?.ok) {
      return Vue.prototype.$tenantUtils.handleResponseError(resp);
    }
  });
}

export function generateToken() {
  return fetch('/deeds-tenant/rest/hub/token', {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.ok) {
      return resp.text();
    } else {
      return Vue.prototype.$tenantUtils.handleResponseError(resp);
    }
  });
}

export function getConfiguration() {
  return fetch('/deeds-tenant/rest/hub/configuration', {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.ok) {
      return resp.json();
    } else {
      return Vue.prototype.$tenantUtils.handleResponseError(resp);
    }
  });
}

export function getManagedDeeds(womServerUrl, address) {
  const url = `${womServerUrl}/api/hubs/managed-deeds/${address}`.replace(/\/\//g, '/').replace(':/', '://');
  return fetch(url, {method: 'Get'})
    .then((resp) => {
      if (resp?.ok) {
        return resp.json();
      } else {
        return Vue.prototype.$tenantUtils.handleResponseError(resp);
      }
    });
}

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
  return fetch(`/deeds-tenant/rest/wom?${params}`, {
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
  return fetch('/deeds-tenant/rest/wom/connect', {
    method: 'POST',
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

export function disconnectFromWoM(request) {
  return fetch('/deeds-tenant/rest/wom/disconnect', {
    method: 'POST',
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
  return fetch('/deeds-tenant/rest/wom/token', {
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
  return fetch('/deeds-tenant/rest/wom/configuration', {
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

export function getOwnedDeeds(womServerUrl, address) {
  const formData = new FormData();
  formData.append('address', address);
  const params = new URLSearchParams(formData).toString();
  const url = `${womServerUrl}/api/tenants?${params}`.replace(/\/\//g, '/').replace(':/', '://');
  return fetch(url, {method: 'Get'})
    .then((resp) => {
      if (resp?.ok) {
        return resp.json();
      } else {
        return Vue.prototype.$tenantUtils.handleResponseError(resp);
      }
    });
}

export function getLeases(womServerUrl, address, page, size) {
  const formData = new FormData();
  formData.append('address', address);
  formData.append('onlyConfirmed', true);
  formData.append('owner', false);
  formData.append('page', page || 0);
  formData.append('size', size || 50);
  const params = new URLSearchParams(formData).toString();
  const url = `${womServerUrl}/api/leases?${params}`.replace(/\/\//g, '/').replace(':/', '://');
  return fetch(url, {method: 'Get'})
    .then((resp) => {
      if (resp?.ok) {
        return resp.json();
      } else {
        return Vue.prototype.$tenantUtils.handleResponseError(resp);
      }
    });
}

export function getDeed(womServerUrl, deedId) {
  const url = `${womServerUrl}/api/deeds/${deedId}`.replace(/\/\//g, '/').replace(':/', '://');
  return fetch(url, {method: 'Get'})
    .then((resp) => {
      if (resp?.ok) {
        return resp.json();
      } else {
        return Vue.prototype.$tenantUtils.handleResponseError(resp);
      }
    });
}

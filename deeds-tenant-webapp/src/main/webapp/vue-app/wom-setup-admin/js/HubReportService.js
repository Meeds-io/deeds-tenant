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

export function getReports(page, size) {
  return fetch(`/deeds-tenant/rest/reports?page=${page || 0}&size=${size || 10}`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.status === 200) {
      return resp.json();
    } else if (resp?.status === 404) {
      return null;
    } else {
      return Vue.prototype.$tenantUtils.handleResponseError(resp);
    }
  });
}

export function getReport(id, refreshFromWoM) {
  return fetch(`/deeds-tenant/rest/reports/${id}?refresh=${refreshFromWoM || false}`, {
    method: 'GET',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.status === 200) {
      return resp.json();
    } else if (resp?.status === 404) {
      return null;
    } else {
      return Vue.prototype.$tenantUtils.handleResponseError(resp);
    }
  });
}

export function getLocalRewardDetails(period) {
  return fetch('/wallet/rest/reward/period/compute', {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      id: period?.id,
      rewardPeriodType: period?.rewardPeriodType,
      timeZone: period?.timeZone,
      startDateInSeconds: period?.startDateInSeconds,
      endDateInSeconds: period?.endDateInSeconds
    })
  }).then((resp) => {
    if (resp) {
      try {
        return resp.json().catch(() => {
          throw new Error('Error computing rewards');
        });
      } catch (e) {
        throw new Error('Error computing rewards');
      }
    } else {
      throw new Error('Error computing rewards');
    }
  });
}

export function sendReport(id) {
  return fetch(`/deeds-tenant/rest/reports/${id}`, {
    method: 'PUT',
    credentials: 'include',
  }).then((resp) => {
    if (resp?.status === 200) {
      return resp.json();
    } else if (resp?.status === 404) {
      return null;
    } else {
      return Vue.prototype.$tenantUtils.handleResponseError(resp);
    }
  });
}

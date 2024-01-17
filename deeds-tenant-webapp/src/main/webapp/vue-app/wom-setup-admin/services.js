/**
 *
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
 *
 */
import * as metamaskUtils from '../js/MetamaskUtils.js';
import * as hubService from './js/HubService.js';
import * as hubReportService from './js/HubReportService.js';

if (!Vue.prototype.$metamaskUtils) {
  window.Object.defineProperty(Vue.prototype, '$metamaskUtils', {
    value: metamaskUtils,
  });
}

if (!Vue.prototype.$hubService) {
  window.Object.defineProperty(Vue.prototype, '$hubService', {
    value: hubService,
  });
}

if (!Vue.prototype.$hubReportService) {
  window.Object.defineProperty(Vue.prototype, '$hubReportService', {
    value: hubReportService,
  });
}

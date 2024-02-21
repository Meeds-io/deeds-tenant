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

export function handleResponseError(resp) {
  if (resp.status === 503 || resp.status === 400 || resp.status === 401) {
    return resp.text()
      .then(error => {
        let messageKey = '';
        if (error?.includes('{') && error?.includes('}')) {
          error = JSON.parse(error);
          messageKey = error?.message || '';
        } else if (error) {
          messageKey = error.split(':')[0];
        }
        throw new Error(messageKey);
      });
  } else {
    throw new Error('wom.errorResponse');
  }
}

export function formatNumber(value, minFrac, maxFrac) {
  const number = new Intl.NumberFormat(eXo.env.portal.language, {
    style: 'decimal',
    minimumFractionDigits: minFrac || 0,
    maximumFractionDigits: maxFrac || 0,
  }).format(value || 0);
  return number !== 'NaN' && number || value || 0;
}

export function sendTransaction(provider, contract, method, options, params) {
  const signer = provider && contract && contract.connect(provider.getSigner());
  if (signer) {
    const estimationOptions = JSON.parse(JSON.stringify(options));
    delete estimationOptions.gasLimit;
    return signer.estimateGas[method](
      ...params,
      estimationOptions
    )
      .then(estimatedGasLimit => {
        if (estimatedGasLimit && estimatedGasLimit.toNumber && estimatedGasLimit.toNumber() > 0) {
          options.gasLimit = parseInt(estimatedGasLimit.toNumber() * 1.2);
        }
      })
      .then(() => 
        signer[method](
          ...params,
          options
        )
      ).catch(e => {
        if (e?.code !== 4001) { // User denied transaction signature
          throw e;
        }
      });
  }
  return Promise.resolve(null);
}

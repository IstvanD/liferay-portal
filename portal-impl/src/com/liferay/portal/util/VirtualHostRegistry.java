/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.util;

import com.liferay.petra.concurrent.DCLSingleton;
import com.liferay.portal.db.partition.util.DBPartitionUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.VirtualHost;
import com.liferay.portal.kernel.service.VirtualHostLocalServiceUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author István András Dézsi
 */
public class VirtualHostRegistry {

	public static VirtualHost fetchVirtualHost(String hostname) {
		if (Validator.isNull(hostname)) {
			return null;
		}

		Map<String, VirtualHost> virtualHosts = _getVirtualHosts();

		return virtualHosts.get(StringUtil.toLowerCase(hostname));
	}

	public static void register(VirtualHost virtualHost) {
		_put(_getVirtualHosts(), virtualHost);
	}

	public static void unregister(String hostname) {
		if (Validator.isNull(hostname)) {
			return;
		}

		Map<String, VirtualHost> virtualHosts = _getVirtualHosts();

		virtualHosts.remove(StringUtil.toLowerCase(hostname));
	}

	private static Map<String, VirtualHost> _createVirtualHosts() {
		Map<String, VirtualHost> virtualHosts = new ConcurrentHashMap<>();

		try {
			DBPartitionUtil.forEachCompanyId(
				companyId -> {
					if (companyId == null) {
						return;
					}

					for (VirtualHost virtualHost :
							VirtualHostLocalServiceUtil.getVirtualHosts(
								companyId)) {

						_put(virtualHosts, virtualHost);
					}
				});
		}
		catch (Exception exception) {
			_log.error(exception);
		}

		return virtualHosts;
	}

	private static Map<String, VirtualHost> _getVirtualHosts() {
		return _virtualHostsDCLSingleton.getSingleton(
			VirtualHostRegistry::_createVirtualHosts);
	}

	private static void _put(
		Map<String, VirtualHost> virtualHosts, VirtualHost virtualHost) {

		String hostname = virtualHost.getHostname();

		if (Validator.isNull(hostname)) {
			return;
		}

		virtualHosts.put(StringUtil.toLowerCase(hostname), virtualHost);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		VirtualHostRegistry.class);

	private static final DCLSingleton<Map<String, VirtualHost>>
		_virtualHostsDCLSingleton = new DCLSingleton<>();

}
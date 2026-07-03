/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.util;

import com.liferay.portal.kernel.model.VirtualHost;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.net.IDN;

import java.util.Collection;
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

		VirtualHost virtualHost = _virtualHosts.get(
			StringUtil.toLowerCase(hostname));

		if ((virtualHost == null) && hostname.contains("xn--")) {
			virtualHost = _virtualHosts.get(
				StringUtil.toLowerCase(IDN.toUnicode(hostname)));
		}

		return virtualHost;
	}

	public static void register(VirtualHost virtualHost) {
		String hostname = virtualHost.getHostname();

		if (Validator.isNull(hostname)) {
			return;
		}

		_virtualHosts.put(StringUtil.toLowerCase(hostname), virtualHost);
	}

	public static void unregister(String hostname) {
		if (Validator.isNull(hostname)) {
			return;
		}

		_virtualHosts.remove(StringUtil.toLowerCase(hostname));
	}

	public static void unregisterVirtualHosts(long companyId) {
		Collection<VirtualHost> virtualHosts = _virtualHosts.values();

		virtualHosts.removeIf(
			virtualHost -> virtualHost.getCompanyId() == companyId);
	}

	private static final Map<String, VirtualHost> _virtualHosts =
		new ConcurrentHashMap<>();

}
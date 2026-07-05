/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.util;

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

	public static long fetchCompanyId(String hostname) {
		if (Validator.isNull(hostname)) {
			return 0;
		}

		Long companyId = _companyIds.get(StringUtil.toLowerCase(hostname));

		if ((companyId == null) && hostname.contains("xn--")) {
			companyId = _companyIds.get(
				StringUtil.toLowerCase(IDN.toUnicode(hostname)));
		}

		if (companyId == null) {
			return 0;
		}

		return companyId;
	}

	public static void register(String hostname, long companyId) {
		if (Validator.isNull(hostname)) {
			return;
		}

		_companyIds.put(StringUtil.toLowerCase(hostname), companyId);
	}

	public static void unregister(String hostname) {
		if (Validator.isNull(hostname)) {
			return;
		}

		_companyIds.remove(StringUtil.toLowerCase(hostname));
	}

	public static void unregisterVirtualHosts(long companyId) {
		Collection<Long> companyIds = _companyIds.values();

		companyIds.removeIf(curCompanyId -> curCompanyId == companyId);
	}

	private static final Map<String, Long> _companyIds =
		new ConcurrentHashMap<>();

}
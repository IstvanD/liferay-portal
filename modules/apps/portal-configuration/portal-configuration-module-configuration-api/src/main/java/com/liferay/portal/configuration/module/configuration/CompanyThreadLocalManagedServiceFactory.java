/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.configuration.module.configuration;

import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.util.GetterUtil;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.cm.ManagedServiceFactory;

/**
 * @author István András Dézsi
 */
public abstract class CompanyThreadLocalManagedServiceFactory
	implements ManagedServiceFactory {

	@Override
	public final void deleted(String pid) {
		Long companyId = companyIds.remove(pid);

		if (companyId == null) {
			companyId = CompanyConstants.SYSTEM;
		}

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(companyId)) {

			doDeleted(companyId, pid);
		}
	}

	@Override
	public final void updated(String pid, Dictionary<String, ?> properties) {
		long companyId = GetterUtil.getLong(
			properties.get("companyId"), CompanyConstants.SYSTEM);

		companyIds.put(pid, companyId);

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(companyId)) {

			doUpdated(companyId, pid, properties);
		}
	}

	protected abstract void doDeleted(long companyId, String pid);

	protected abstract void doUpdated(
		long companyId, String pid, Dictionary<String, ?> properties);

	protected final Map<String, Long> companyIds = new ConcurrentHashMap<>();

}
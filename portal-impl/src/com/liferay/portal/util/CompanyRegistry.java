/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.util;

import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.util.Validator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author István András Dézsi
 */
public class CompanyRegistry {

	public static Company fetchCompany(long companyId) {
		return _companies.get(companyId);
	}

	public static Company fetchCompanyByWebId(String webId) {
		if (Validator.isNull(webId)) {
			return null;
		}

		for (Company company : _companies.values()) {
			if (webId.equals(company.getWebId())) {
				return company;
			}
		}

		return null;
	}

	public static void register(Company company) {
		_companies.put(company.getCompanyId(), company);
	}

	public static void unregister(long companyId) {
		_companies.remove(companyId);
	}

	private static final Map<Long, Company> _companies =
		new ConcurrentHashMap<>();

}
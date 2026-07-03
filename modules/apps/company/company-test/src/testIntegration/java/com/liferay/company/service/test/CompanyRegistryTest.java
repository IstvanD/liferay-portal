/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.company.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.db.partition.test.util.BaseDBPartitionTestCase;
import com.liferay.portal.kernel.instance.PortalInstancePool;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.AssumeTestRule;
import com.liferay.portal.kernel.test.rule.DataGuard;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.portal.util.CompanyRegistry;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author István András Dézsi
 */
@DataGuard(scope = DataGuard.Scope.NONE)
@RunWith(Arquillian.class)
public class CompanyRegistryTest extends BaseDBPartitionTestCase {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new AssumeTestRule("assume"), new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Test
	public void testAddAndDeleteCompany() throws Exception {
		_company = CompanyTestUtil.addCompany();

		long companyId = _company.getCompanyId();

		Company company = CompanyRegistry.fetchCompany(companyId);

		Assert.assertEquals(_company.getWebId(), company.getWebId());

		_companyLocalService.deleteCompany(_company);

		Assert.assertNull(CompanyRegistry.fetchCompany(companyId));
	}

	@Test
	public void testFetchCompany() throws Exception {
		long companyId = PortalInstancePool.getDefaultCompanyId();

		Company defaultCompany = _companyLocalService.getCompany(companyId);

		Company company = CompanyRegistry.fetchCompany(companyId);

		Assert.assertEquals(defaultCompany.getWebId(), company.getWebId());
	}

	@Test
	public void testFetchCompanyByWebId() throws Exception {
		Company defaultCompany = _companyLocalService.getCompany(
			PortalInstancePool.getDefaultCompanyId());

		Company company = CompanyRegistry.fetchCompanyByWebId(
			defaultCompany.getWebId());

		Assert.assertEquals(
			defaultCompany.getCompanyId(), company.getCompanyId());
	}

	@Test
	public void testUpdateCompany() throws Exception {
		_company = CompanyTestUtil.addCompany();

		int maxUsers = RandomTestUtil.randomInt();

		_companyLocalService.updateCompany(
			_company.getCompanyId(), _company.getVirtualHostname(),
			_company.getMx(), maxUsers, _company.isActive());

		Company company = CompanyRegistry.fetchCompany(_company.getCompanyId());

		Assert.assertEquals(maxUsers, company.getMaxUsers());
	}

	@DeleteAfterTestRun
	private Company _company;

	@Inject
	private CompanyLocalService _companyLocalService;

}
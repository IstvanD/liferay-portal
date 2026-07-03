/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.company.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.db.partition.test.util.BaseDBPartitionTestCase;
import com.liferay.portal.kernel.instance.PortalInstancePool;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.VirtualHost;
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
import com.liferay.portal.util.VirtualHostRegistry;

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
public class VirtualHostRegistryTest extends BaseDBPartitionTestCase {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new AssumeTestRule("assume"), new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Test
	public void testAddAndDeleteCompany() throws Exception {
		_company = CompanyTestUtil.addCompany();

		String virtualHostname = _company.getVirtualHostname();

		VirtualHost virtualHost = VirtualHostRegistry.fetchVirtualHost(
			virtualHostname);

		Assert.assertEquals(
			_company.getCompanyId(), virtualHost.getCompanyId());

		_companyLocalService.deleteCompany(_company);

		Assert.assertNull(
			VirtualHostRegistry.fetchVirtualHost(virtualHostname));
	}

	@Test
	public void testFetchVirtualHost() throws Exception {
		Company company = _companyLocalService.getCompany(
			PortalInstancePool.getDefaultCompanyId());

		VirtualHost virtualHost = VirtualHostRegistry.fetchVirtualHost(
			company.getVirtualHostname());

		Assert.assertEquals(company.getCompanyId(), virtualHost.getCompanyId());
	}

	@Test
	public void testUpdateVirtualHostname() throws Exception {
		_company = CompanyTestUtil.addCompany();

		String virtualHostname = _company.getVirtualHostname();

		String newVirtualHostname =
			RandomTestUtil.randomString() + StringPool.PERIOD +
				RandomTestUtil.randomString(3);

		_companyLocalService.updateCompany(
			_company.getCompanyId(), newVirtualHostname, _company.getMx(),
			_company.getMaxUsers(), _company.isActive());

		Assert.assertNull(
			VirtualHostRegistry.fetchVirtualHost(virtualHostname));

		VirtualHost virtualHost = VirtualHostRegistry.fetchVirtualHost(
			newVirtualHostname);

		Assert.assertEquals(
			_company.getCompanyId(), virtualHost.getCompanyId());
	}

	@DeleteAfterTestRun
	private Company _company;

	@Inject
	private CompanyLocalService _companyLocalService;

}
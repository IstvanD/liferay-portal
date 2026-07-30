/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.company.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.db.partition.test.util.BaseDBPartitionTestCase;
import com.liferay.portal.kernel.exception.CompanyVirtualHostException;
import com.liferay.portal.kernel.exception.LayoutSetVirtualHostException;
import com.liferay.portal.kernel.instance.PortalInstancePool;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.VirtualHost;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutSetLocalService;
import com.liferay.portal.kernel.service.VirtualHostLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.AssumeTestRule;
import com.liferay.portal.kernel.test.rule.DataGuard;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.TreeMapBuilder;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;

import java.util.TreeMap;

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
		_company1 = CompanyTestUtil.addCompany();

		String virtualHostname = _company1.getVirtualHostname();

		Assert.assertEquals(
			_company1.getCompanyId(),
			_fetchVirtualHostCompanyId(virtualHostname));

		_companyLocalService.deleteCompany(_company1);

		Assert.assertEquals(0, _fetchVirtualHostCompanyId(virtualHostname));
	}

	@Test
	public void testDeleteGroup() throws Exception {
		Group group = GroupTestUtil.addGroup();

		String virtualHostname = StringUtil.toLowerCase(
			RandomTestUtil.randomString() + StringPool.PERIOD +
				RandomTestUtil.randomString(3));

		_layoutSetLocalService.updateVirtualHosts(
			group.getGroupId(), false,
			TreeMapBuilder.put(
				virtualHostname, StringPool.BLANK
			).build());

		Assert.assertEquals(
			group.getCompanyId(), _fetchVirtualHostCompanyId(virtualHostname));

		_groupLocalService.deleteGroup(group);

		_company1 = CompanyTestUtil.addCompany();

		_companyLocalService.updateCompany(
			_company1.getCompanyId(), virtualHostname, _company1.getMx(),
			_company1.getMaxUsers(), _company1.isActive());

		Assert.assertEquals(
			_company1.getCompanyId(),
			_fetchVirtualHostCompanyId(virtualHostname));
	}

	@Test
	public void testFetchCompanyByVirtualHost() throws Exception {
		_company1 = CompanyTestUtil.addCompany();

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					PortalInstancePool.getDefaultCompanyId())) {

			Company company = _companyLocalService.fetchCompanyByVirtualHost(
				_company1.getVirtualHostname());

			Assert.assertEquals(
				_company1.getCompanyId(), company.getCompanyId());
		}
	}

	@Test
	public void testFetchVirtualHost() throws Exception {
		Company company = _companyLocalService.getCompany(
			PortalInstancePool.getDefaultCompanyId());

		Assert.assertEquals(
			company.getCompanyId(),
			_fetchVirtualHostCompanyId(company.getVirtualHostname()));
	}

	@Test
	public void testUpdateVirtualHostname() throws Exception {
		_company1 = CompanyTestUtil.addCompany();

		String virtualHostname = _company1.getVirtualHostname();

		String newVirtualHostname = StringUtil.toLowerCase(
			RandomTestUtil.randomString() + StringPool.PERIOD +
				RandomTestUtil.randomString(3));

		_companyLocalService.updateCompany(
			_company1.getCompanyId(), newVirtualHostname, _company1.getMx(),
			_company1.getMaxUsers(), _company1.isActive());

		Assert.assertEquals(0, _fetchVirtualHostCompanyId(virtualHostname));
		Assert.assertEquals(
			_company1.getCompanyId(),
			_fetchVirtualHostCompanyId(newVirtualHostname));
	}

	@Test(expected = CompanyVirtualHostException.class)
	public void testUpdateVirtualHostnameWhenHostnameIsDuplicate()
		throws Exception {

		_company1 = CompanyTestUtil.addCompany();

		_company2 = CompanyTestUtil.addCompany();

		_companyLocalService.updateCompany(
			_company2.getCompanyId(), _company1.getVirtualHostname(),
			_company2.getMx(), _company2.getMaxUsers(), _company2.isActive());
	}

	@Test
	public void testUpdateVirtualHostsWithDuplicateVirtualHostname()
		throws Exception {

		_company1 = CompanyTestUtil.addCompany();

		String virtualHostname1 = StringUtil.toLowerCase(
			StringBundler.concat(
				"a", RandomTestUtil.randomString(), StringPool.PERIOD,
				RandomTestUtil.randomString(3)));

		String virtualHostname2 = StringUtil.toLowerCase(
			StringBundler.concat(
				"b", RandomTestUtil.randomString(), StringPool.PERIOD,
				RandomTestUtil.randomString(3)));

		_companyLocalService.updateCompany(
			_company1.getCompanyId(), virtualHostname2, _company1.getMx(),
			_company1.getMaxUsers(), _company1.isActive());

		_group = GroupTestUtil.addGroup();

		try {
			_layoutSetLocalService.updateVirtualHosts(
				_group.getGroupId(), false,
				TreeMapBuilder.put(
					virtualHostname1, StringPool.BLANK
				).put(
					virtualHostname2, StringPool.BLANK
				).build());

			Assert.fail();
		}
		catch (LayoutSetVirtualHostException layoutSetVirtualHostException) {
			Assert.assertEquals(
				"Duplicate virtual hostname " + virtualHostname2,
				layoutSetVirtualHostException.getMessage());
		}

		_company2 = CompanyTestUtil.addCompany();

		_companyLocalService.updateCompany(
			_company2.getCompanyId(), virtualHostname1, _company2.getMx(),
			_company2.getMaxUsers(), _company2.isActive());

		Assert.assertEquals(
			_company2.getCompanyId(),
			_fetchVirtualHostCompanyId(virtualHostname1));
	}

	@Test
	public void testUpdateVirtualHostsWithEmptyVirtualHostnames()
		throws Exception {

		_group = GroupTestUtil.addGroup();

		String virtualHostname = StringUtil.toLowerCase(
			RandomTestUtil.randomString() + StringPool.PERIOD +
				RandomTestUtil.randomString(3));

		_layoutSetLocalService.updateVirtualHosts(
			_group.getGroupId(), false,
			TreeMapBuilder.put(
				virtualHostname, StringPool.BLANK
			).build());

		Assert.assertEquals(
			_group.getCompanyId(), _fetchVirtualHostCompanyId(virtualHostname));

		_layoutSetLocalService.updateVirtualHosts(
			_group.getGroupId(), false, new TreeMap<String, String>());

		_company1 = CompanyTestUtil.addCompany();

		_companyLocalService.updateCompany(
			_company1.getCompanyId(), virtualHostname, _company1.getMx(),
			_company1.getMaxUsers(), _company1.isActive());

		Assert.assertEquals(
			_company1.getCompanyId(),
			_fetchVirtualHostCompanyId(virtualHostname));
	}

	private long _fetchVirtualHostCompanyId(String hostname) {
		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					PortalInstancePool.getDefaultCompanyId())) {

			VirtualHost virtualHost = _virtualHostLocalService.fetchVirtualHost(
				hostname);

			if (virtualHost == null) {
				return 0;
			}

			return virtualHost.getCompanyId();
		}
	}

	@DeleteAfterTestRun
	private Company _company1;

	@DeleteAfterTestRun
	private Company _company2;

	@Inject
	private CompanyLocalService _companyLocalService;

	@DeleteAfterTestRun
	private Group _group;

	@Inject
	private GroupLocalService _groupLocalService;

	@Inject
	private LayoutSetLocalService _layoutSetLocalService;

	@Inject
	private VirtualHostLocalService _virtualHostLocalService;

}
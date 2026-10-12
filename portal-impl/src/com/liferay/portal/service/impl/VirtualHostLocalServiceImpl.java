/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.service.impl;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.db.partition.util.DBPartitionUtil;
import com.liferay.portal.kernel.bean.BeanReference;
import com.liferay.portal.kernel.change.tracking.CTCollectionThreadLocal;
import com.liferay.portal.kernel.cluster.Clusterable;
import com.liferay.portal.kernel.exception.AvailableLocaleException;
import com.liferay.portal.kernel.exception.LayoutSetVirtualHostException;
import com.liferay.portal.kernel.exception.NoSuchVirtualHostException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.model.VirtualHost;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.persistence.CompanyPersistence;
import com.liferay.portal.kernel.service.persistence.GroupPersistence;
import com.liferay.portal.kernel.service.persistence.LayoutSetPersistence;
import com.liferay.portal.kernel.transaction.TransactionCallbackUtil;
import com.liferay.portal.kernel.transaction.Transactional;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.base.VirtualHostLocalServiceBaseImpl;

import java.net.IDN;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Alexander Chow
 * @author Raymond Augé
 */
public class VirtualHostLocalServiceImpl
	extends VirtualHostLocalServiceBaseImpl {

	@Override
	public VirtualHost fetchCompanyDefaultVirtualHost(long companyId) {
		List<VirtualHost> virtualHosts = getVirtualHosts(companyId, 0);

		if (virtualHosts.isEmpty()) {
			return null;
		}

		if (virtualHosts.size() == 1) {
			VirtualHost virtualHost = virtualHosts.get(0);

			if (virtualHost.isDefaultVirtualHost()) {
				return virtualHost;
			}

			return null;
		}

		List<VirtualHost> defaultVirtualHosts = new ArrayList<>();

		for (VirtualHost virtualHost : virtualHosts) {
			if (virtualHost.isDefaultVirtualHost()) {
				defaultVirtualHosts.add(virtualHost);
			}
		}

		if (defaultVirtualHosts.isEmpty()) {
			return null;
		}

		if ((defaultVirtualHosts.size() > 1) && _log.isWarnEnabled()) {
			_log.error(
				"More than one default virtual host uses company ID " +
					companyId);
		}

		return defaultVirtualHosts.get(defaultVirtualHosts.size() - 1);
	}

	@Override
	public VirtualHost fetchVirtualHost(String virtualHostname) {
		virtualHostname = StringUtil.toLowerCase(
			StringUtil.trim(virtualHostname));

		if (Validator.isIPv6Address(virtualHostname)) {
			try {
				Inet6Address inet6Address = (Inet6Address)InetAddress.getByName(
					virtualHostname);

				virtualHostname = inet6Address.getHostAddress();
			}
			catch (UnknownHostException unknownHostException) {
				if (_log.isDebugEnabled()) {
					_log.debug(unknownHostException);
				}
			}
		}

		long companyId = _virtualHostPool.fetchCompanyId(virtualHostname);

		if (companyId != 0) {
			try (SafeCloseable safeCloseable =
					CompanyThreadLocal.setCompanyIdWithSafeCloseable(
						companyId,
						CTCollectionThreadLocal.getCTCollectionId())) {

				return _fetchVirtualHost(virtualHostname);
			}
		}

		return _fetchVirtualHost(virtualHostname);
	}

	@Override
	public VirtualHost getVirtualHost(String virtualHostname)
		throws PortalException {

		VirtualHost virtualHost = fetchVirtualHost(virtualHostname);

		if (virtualHost == null) {
			throw new NoSuchVirtualHostException(
				"{hostname=" + virtualHostname + "}");
		}

		return virtualHost;
	}

	@Override
	public List<VirtualHost> getVirtualHosts(long companyId) {
		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					companyId, CTCollectionThreadLocal.getCTCollectionId())) {

			return virtualHostPersistence.findByCompanyId(companyId);
		}
	}

	@Override
	public List<VirtualHost> getVirtualHosts(long companyId, long layoutSetId) {
		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					companyId, CTCollectionThreadLocal.getCTCollectionId())) {

			return _getVirtualHosts(companyId, layoutSetId);
		}
	}

	@Override
	public long getVirtualHostsCount(
		long excludedLayoutSetId, String[] virtualHostnames) {

		return virtualHostPersistence.countByNotL_H(
			excludedLayoutSetId, virtualHostnames);
	}

	@Clusterable
	@Override
	@Transactional(enabled = false)
	public void registerVirtualHost(long companyId, String hostname) {
		_virtualHostPool.register(companyId, hostname);
	}

	@Clusterable
	@Override
	@Transactional(enabled = false)
	public void unregisterVirtualHost(String hostname) {
		_virtualHostPool.unregister(hostname);
	}

	@Clusterable
	@Override
	@Transactional(enabled = false)
	public void unregisterVirtualHosts(long companyId) {
		_virtualHostPool.unregister(companyId);
	}

	@Override
	public VirtualHost updateVirtualHost(
		long companyId, String virtualHostname) {

		VirtualHost virtualHost = null;

		List<VirtualHost> virtualHosts = virtualHostPersistence.findByC_L(
			companyId, 0);

		if (virtualHosts.isEmpty()) {
			virtualHost = virtualHostPersistence.create(
				DBPartitionUtil.incrementCounter());

			virtualHost.setCompanyId(companyId);
		}
		else {
			virtualHost = virtualHosts.get(0);

			for (int i = 1; i < virtualHosts.size(); i++) {
				virtualHostPersistence.remove(virtualHosts.get(i));
			}
		}

		virtualHost.setHostname(virtualHostname);
		virtualHost.setDefaultVirtualHost(true);
		virtualHost.setLanguageId(StringPool.BLANK);

		virtualHost = virtualHostPersistence.update(virtualHost);

		Company company = _companyPersistence.fetchByPrimaryKey(companyId);

		if (company != null) {
			_companyPersistence.clearCache(company);
		}

		if (Validator.isNotNull(PropsValues.VIRTUAL_HOSTS_DEFAULT_SITE_NAME)) {
			Group group = _groupPersistence.fetchByC_GK(
				companyId, PropsValues.VIRTUAL_HOSTS_DEFAULT_SITE_NAME);

			if (group != null) {
				LayoutSet layoutSet = _layoutSetPersistence.fetchByG_P(
					group.getGroupId(), false);

				if (layoutSet != null) {
					_layoutSetPersistence.clearCache(layoutSet);
				}
			}
		}

		return virtualHost;
	}

	@Override
	public List<VirtualHost> updateVirtualHosts(
			LayoutSet layoutSet, TreeMap<String, String> virtualHostnames)
		throws PortalException {

		try (SafeCloseable safeCloseable =
				CompanyThreadLocal.setCompanyIdWithSafeCloseable(
					layoutSet.getCompanyId(),
					CTCollectionThreadLocal.getCTCollectionId())) {

			return _updateVirtualHosts(layoutSet, virtualHostnames);
		}
		catch (DuplicateVirtualHostnameException
					duplicateVirtualHostnameException) {

			throw new LayoutSetVirtualHostException(
				duplicateVirtualHostnameException.getMessage(),
				duplicateVirtualHostnameException);
		}
	}

	private VirtualHost _fetchVirtualHost(String virtualHostname) {
		VirtualHost virtualHost = virtualHostPersistence.fetchByHostname(
			virtualHostname);

		if ((virtualHost == null) && virtualHostname.contains("xn--")) {
			virtualHost = virtualHostPersistence.fetchByHostname(
				IDN.toUnicode(virtualHostname));
		}

		return virtualHost;
	}

	private List<VirtualHost> _getVirtualHosts(
		long companyId, long layoutSetId) {

		if (_cacheableQueryLimitLPD27353 <= 0) {
			return virtualHostPersistence.findByC_L(companyId, layoutSetId);
		}

		List<VirtualHost> virtualHosts = virtualHostPersistence.findByCompanyId(
			companyId);

		if (virtualHosts.size() > _cacheableQueryLimitLPD27353) {
			_cacheableQueryLimitLPD27353 = 0;
		}

		List<VirtualHost> filteredVirtualHosts = null;

		for (VirtualHost virtualHost : virtualHosts) {
			if (virtualHost.getLayoutSetId() != layoutSetId) {
				continue;
			}

			if (filteredVirtualHosts == null) {
				filteredVirtualHosts = new ArrayList<>(virtualHosts.size());
			}

			filteredVirtualHosts.add(virtualHost);
		}

		if (filteredVirtualHosts == null) {
			return Collections.emptyList();
		}

		return filteredVirtualHosts;
	}

	private void _unregisterVirtualHostsOnCommit(List<String> hostnames) {
		if (!_virtualHostPool.isEnabled()) {
			return;
		}

		TransactionCallbackUtil.registerCommitCallback(
			() -> {
				for (String hostname : hostnames) {
					virtualHostLocalService.unregisterVirtualHost(hostname);
				}

				return null;
			});
	}

	private List<VirtualHost> _updateVirtualHosts(
			LayoutSet layoutSet, TreeMap<String, String> virtualHostnames)
		throws PortalException {

		if (virtualHostnames.isEmpty()) {
			_unregisterVirtualHostsOnCommit(
				TransformUtil.transform(
					virtualHostPersistence.findByC_L(
						layoutSet.getCompanyId(), layoutSet.getLayoutSetId()),
					VirtualHost::getHostname));

			virtualHostPersistence.removeByC_L(
				layoutSet.getCompanyId(), layoutSet.getLayoutSetId());

			_layoutSetPersistence.clearCache(layoutSet);

			return Collections.emptyList();
		}

		long count = virtualHostPersistence.countByNotL_H(
			layoutSet.getLayoutSetId(),
			ArrayUtil.toStringArray(virtualHostnames.keySet()));

		if (count > 0) {
			throw new LayoutSetVirtualHostException();
		}

		Set<Locale> availableLocales = LanguageUtil.getAvailableLocales(
			layoutSet.getGroupId());

		List<VirtualHost> virtualHosts = new ArrayList<>(
			virtualHostPersistence.findByC_L(
				layoutSet.getCompanyId(), layoutSet.getLayoutSetId()));

		List<String> reservedHostnames = new ArrayList<>();

		if (_virtualHostPool.isEnabled()) {
			TransactionCallbackUtil.registerRollbackCallback(
				() -> {
					for (String reservedHostname : reservedHostnames) {
						virtualHostLocalService.unregisterVirtualHost(
							reservedHostname);
					}

					return null;
				});
		}

		boolean first = true;

		for (String curVirtualHostname : virtualHostnames.navigableKeySet()) {
			VirtualHost virtualHost = null;

			for (VirtualHost curVirtualHost : virtualHosts) {
				if (curVirtualHostname.equals(curVirtualHost.getHostname())) {
					virtualHost = curVirtualHost;

					break;
				}
			}

			if (virtualHost == null) {
				if (_virtualHostPool.isEnabled()) {
					Long virtualHostCompanyId =
						_virtualHostPool.registerIfAbsent(
							layoutSet.getCompanyId(), curVirtualHostname);

					if ((virtualHostCompanyId != null) &&
						(virtualHostCompanyId != layoutSet.getCompanyId())) {

						throw new DuplicateVirtualHostnameException(
							curVirtualHostname);
					}

					if (virtualHostCompanyId == null) {
						reservedHostnames.add(curVirtualHostname);

						virtualHostLocalService.registerVirtualHost(
							layoutSet.getCompanyId(), curVirtualHostname);
					}
				}

				long virtualHostId = DBPartitionUtil.incrementCounter();

				virtualHost = virtualHostPersistence.create(virtualHostId);

				virtualHost.setCompanyId(layoutSet.getCompanyId());
				virtualHost.setLayoutSetId(layoutSet.getLayoutSetId());
				virtualHost.setHostname(curVirtualHostname);

				virtualHosts.add(virtualHost);
			}

			String languageId = virtualHostnames.get(curVirtualHostname);

			Locale locale = LocaleUtil.fromLanguageId(languageId, true, false);

			if (locale == null) {
				locale = LocaleUtil.getSiteDefault();
			}

			if (!availableLocales.contains(locale)) {
				ReflectionUtil.throwException(
					new AvailableLocaleException(languageId));
			}

			virtualHost.setDefaultVirtualHost(first);
			virtualHost.setLanguageId(languageId);

			first = false;

			virtualHostPersistence.update(virtualHost);
		}

		List<String> removedHostnames = new ArrayList<>();

		Iterator<VirtualHost> iterator = virtualHosts.iterator();

		while (iterator.hasNext()) {
			VirtualHost virtualHost = iterator.next();

			if (!virtualHostnames.containsKey(virtualHost.getHostname())) {
				iterator.remove();

				removedHostnames.add(virtualHost.getHostname());

				virtualHostPersistence.remove(virtualHost);
			}
		}

		virtualHostPersistence.cacheResult(virtualHosts);

		_unregisterVirtualHostsOnCommit(removedHostnames);

		_layoutSetPersistence.clearCache(layoutSet);

		return virtualHosts;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		VirtualHostLocalServiceImpl.class);

	private volatile int _cacheableQueryLimitLPD27353 = GetterUtil.getInteger(
		PropsUtil.get("cacheable.query.limit.LPD-27353"));

	@BeanReference(type = CompanyPersistence.class)
	private CompanyPersistence _companyPersistence;

	@BeanReference(type = GroupPersistence.class)
	private GroupPersistence _groupPersistence;

	@BeanReference(type = LayoutSetPersistence.class)
	private LayoutSetPersistence _layoutSetPersistence;

	private final VirtualHostPool _virtualHostPool = new VirtualHostPool();

	private class VirtualHostPool {

		public long fetchCompanyId(String hostname) {
			if (!isEnabled() || !CompanyThreadLocal.isDefaultCompany() ||
				Validator.isNull(hostname)) {

				return 0;
			}

			Long companyId = _companyIdsByHostnameMap.get(
				StringUtil.toLowerCase(hostname));

			if ((companyId == null) && hostname.contains("xn--")) {
				companyId = _companyIdsByHostnameMap.get(
					StringUtil.toLowerCase(IDN.toUnicode(hostname)));
			}

			return GetterUtil.getLong(companyId);
		}

		public boolean isEnabled() {
			return PropsValues.DATABASE_PARTITION_ENABLED;
		}

		public void register(long companyId, String hostname) {
			if (Validator.isNull(hostname)) {
				return;
			}

			_companyIdsByHostnameMap.put(
				StringUtil.toLowerCase(hostname), companyId);
		}

		public Long registerIfAbsent(long companyId, String hostname) {
			return _companyIdsByHostnameMap.putIfAbsent(
				StringUtil.toLowerCase(hostname), companyId);
		}

		public void reset(Map<String, Long> companyIdsByHostnameMap) {
			_companyIdsByHostnameMap = new ConcurrentHashMap<>(
				companyIdsByHostnameMap);
		}

		public void unregister(long companyId) {
			Collection<Long> companyIds = _companyIdsByHostnameMap.values();

			companyIds.removeIf(curCompanyId -> curCompanyId == companyId);
		}

		public void unregister(String hostname) {
			if (Validator.isNull(hostname)) {
				return;
			}

			_companyIdsByHostnameMap.remove(StringUtil.toLowerCase(hostname));
		}

		private volatile Map<String, Long> _companyIdsByHostnameMap =
			new ConcurrentHashMap<>();

	}

}
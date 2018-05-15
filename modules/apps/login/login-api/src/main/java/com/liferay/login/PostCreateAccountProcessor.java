/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.login;

import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerList;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerListFactory;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

/**
 * @author Roberto Díaz
 */
public class PostCreateAccountProcessor {

	public void process(
		HttpServletRequest request, HttpServletResponse response) {

		for (PostCreateAccountProcess postCreateAccountProcess :
				_serviceTrackerList) {

			try {
				postCreateAccountProcess.process(request, response);
			}
			catch (PortalException pe) {
				if (_log.isWarnEnabled()) {
					_log.warn(pe);
				}
			}
		}
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_serviceTrackerList = ServiceTrackerListFactory.open(
			bundleContext, PostCreateAccountProcess.class);
	}

	@Deactivate
	protected void deactivate() {
		_serviceTrackerList.close();

		_serviceTrackerList = null;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		PostCreateAccountProcessor.class);

	private static
		ServiceTrackerList<PostCreateAccountProcess, PostCreateAccountProcess>
			_serviceTrackerList;

}
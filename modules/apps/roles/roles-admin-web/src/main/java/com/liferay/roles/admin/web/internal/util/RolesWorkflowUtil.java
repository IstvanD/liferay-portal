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

package com.liferay.roles.admin.web.internal.util;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.workflow.kaleo.model.KaleoDefinition;
import com.liferay.portal.workflow.kaleo.model.KaleoDefinitionVersion;
import com.liferay.portal.workflow.kaleo.model.KaleoTaskAssignment;
import com.liferay.portal.workflow.kaleo.service.KaleoDefinitionVersionLocalServiceUtil;
import com.liferay.portal.workflow.kaleo.service.KaleoInstanceLocalServiceUtil;
import com.liferay.portal.workflow.kaleo.service.KaleoTaskAssignmentLocalServiceUtil;
import com.liferay.roles.admin.web.internal.exception.RequiredWorkflowRoleException;

import java.util.List;

/**
 * @author István András Dézsi
 */
public class RolesWorkflowUtil {

	public static void checkWorkflow(long roleId) throws PortalException {
		KaleoDefinitionVersion kaleoDefinitionVersion = null;
		KaleoDefinition kaleoDefinition = null;

		int count = 0;

		List<KaleoTaskAssignment> kaleoTaskAssignments =
			KaleoTaskAssignmentLocalServiceUtil.getKaleoTaskAssignments(
				QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		for (KaleoTaskAssignment kaleoTaskAssignment : kaleoTaskAssignments) {
			if (StringUtil.equals(
					kaleoTaskAssignment.getAssigneeClassName(),
					_ROLE_CLASS_NAME) &&
				(kaleoTaskAssignment.getAssigneeClassPK() == roleId)) {

				kaleoDefinitionVersion =
					KaleoDefinitionVersionLocalServiceUtil.
						getKaleoDefinitionVersion(
							kaleoTaskAssignment.getKaleoDefinitionVersionId());

				kaleoDefinition = kaleoDefinitionVersion.getKaleoDefinition();

				if (kaleoDefinition.isActive()) {
					throw new RequiredWorkflowRoleException.
						MustNotDeleteRoleReferencedByActiveWorkflowDefinition(
							roleId);
				}

				count = KaleoInstanceLocalServiceUtil.getKaleoInstancesCount(
					kaleoDefinitionVersion.getKaleoDefinitionVersionId(),
					false);

				if (count > 0) {
					throw new RequiredWorkflowRoleException.
						MustNotDeleteRoleReferencedByCurrentWorkflowTask(
							roleId);
				}
			}
		}
	}

	private static final String _ROLE_CLASS_NAME = Role.class.getName();

	private static final Log _log = LogFactoryUtil.getLog(
		RolesWorkflowUtil.class);

}
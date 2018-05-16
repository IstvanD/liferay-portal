/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.invitation.invite.members.internal.events;

import com.liferay.invitation.invite.members.constants.InviteMembersPortletKeys;
import com.liferay.invitation.invite.members.service.MemberRequestLocalService;
import com.liferay.login.events.CreateAccountActionProcess;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Roberto Díaz
 */
@Component(
	immediate = true,
	property = "javax.portlet.name=" + InviteMembersPortletKeys.INVITE_MEMBERS,
	service = CreateAccountActionProcess.class
)
public class InviteMembersCreateAccountActionProcess
	implements CreateAccountActionProcess {

	@Override
	public void process(
			HttpServletRequest request, HttpServletResponse response,
			ThemeDisplay themeDisplay, User user, String password)
		throws PortalException {

		String ppid = ParamUtil.getString(request, "p_p_id");

		String portletNamespace = _portal.getPortletNamespace(ppid);

		String memberRequestKey = ParamUtil.getString(
			request, portletNamespace.concat("key"));

		if (Validator.isNull(memberRequestKey)) {
			String redirect = ParamUtil.getString(request, "redirect");

			ppid = _http.getParameter(redirect, "p_p_id", false);

			portletNamespace = _portal.getPortletNamespace(ppid);

			memberRequestKey = _http.getParameter(
				redirect, portletNamespace.concat("key"), false);

			if (Validator.isNull(memberRequestKey)) {
				return;
			}
		}

		_memberRequestLocalService.updateMemberRequest(
			memberRequestKey, user.getUserId());
	}

	@Reference
	private Http _http;

	@Reference
	private MemberRequestLocalService _memberRequestLocalService;

	@Reference
	private Portal _portal;

}
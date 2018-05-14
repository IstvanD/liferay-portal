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

package com.liferay.invitation.invite.members.internal.events;

import com.liferay.invitation.invite.members.constants.InviteMembersPortletKeys;
import com.liferay.login.events.CreateAccountActionProcess;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.LifecycleAction;
import com.liferay.portal.kernel.events.LifecycleEvent;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PropsKeys;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Eduardo Garcia
 * @author Roberto Díaz
 */
@Component(
	immediate = true, property = "key=" + PropsKeys.LOGIN_EVENTS_POST,
	service = LifecycleAction.class
)
public class LoginPostAction implements LifecycleAction {

	@Override
	public void processLifecycleEvent(LifecycleEvent lifecycleEvent)
		throws ActionException {

		try {
			User user = _portal.getUser(lifecycleEvent.getRequest());

			_createAccountActionProcess.process(
				lifecycleEvent.getRequest(), lifecycleEvent.getResponse(), null,
				user, null);
		}
		catch (Exception e) {
			throw new ActionException(e);
		}
	}

	@Reference(
		target = "(javax.portlet.name=" + InviteMembersPortletKeys.INVITE_MEMBERS +
			")",
		unbind = "-"
	)
	private CreateAccountActionProcess _createAccountActionProcess;

	@Reference
	private Portal _portal;

}
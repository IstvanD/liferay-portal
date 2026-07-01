/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.dao.db;

import com.liferay.portal.kernel.instance.PortalInstancePool;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.PropsUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Mariano Álvaro Sáiz
 */
public class BaseDBProcessTest {

	@Test
	public void testGetFixedThreadPoolSize() throws Exception {
		_testGetFixedThreadPoolSize(DBType.MYSQL, 1, 1);

		Runtime runtime = Runtime.getRuntime();

		_testGetFixedThreadPoolSize(
			DBType.MYSQL, runtime.availableProcessors(), 1000);

		_testGetFixedThreadPoolSize(DBType.HYPERSONIC, 1, 1000);
	}

	@Test
	public void testProcessConcurrentlyReadsResultSetOnCreatingThread()
		throws Exception {

		ReflectionTestUtil.setFieldValue(
			BaseDBProcess.class, "_fixedThreadPoolSize", new AtomicInteger(2));

		try {
			Connection connection = Mockito.mock(Connection.class);

			Statement statement = Mockito.mock(Statement.class);

			Mockito.when(
				connection.createStatement()
			).thenReturn(
				statement
			);

			AtomicReference<Thread> creatingThread = new AtomicReference<>();

			ResultSet resultSet = Mockito.mock(ResultSet.class);

			Mockito.when(
				statement.executeQuery(Mockito.anyString())
			).thenAnswer(
				invocation -> {
					creatingThread.set(Thread.currentThread());

					return resultSet;
				}
			);

			Set<Thread> nextThreads = Collections.newSetFromMap(
				new ConcurrentHashMap<>());

			AtomicInteger remainingRows = new AtomicInteger(1);

			Mockito.when(
				resultSet.next()
			).thenAnswer(
				invocation -> {
					nextThreads.add(Thread.currentThread());

					return remainingRows.getAndDecrement() > 0;
				}
			);

			BaseDBProcess baseDBProcess = new BaseDBProcess() {
			};

			baseDBProcess.connection = connection;

			baseDBProcess.processConcurrently(
				RandomTestUtil.randomString(),
				currentResultSet -> new Object[0],
				values -> {
				},
				null);

			Assert.assertEquals(
				Collections.singleton(creatingThread.get()), nextThreads);
		}
		finally {
			ReflectionTestUtil.setFieldValue(
				BaseDBProcess.class, "_fixedThreadPoolSize",
				new AtomicInteger(0));
		}
	}

	private void _testGetFixedThreadPoolSize(
			DBType dbType, int expectedFixedThreadPoolSize, int maximumPoolSize)
		throws Exception {

		Runtime runtime = Runtime.getRuntime();

		try (MockedStatic<DBManagerUtil> dbManagerUtilMockedStatic =
				Mockito.mockStatic(DBManagerUtil.class);
			MockedStatic<PortalInstancePool> portalInstancePoolMockedStatic =
				Mockito.mockStatic(PortalInstancePool.class);
			MockedStatic<PropsUtil> propsUtilMockedStatic = Mockito.mockStatic(
				PropsUtil.class)) {

			DB db = Mockito.mock(DB.class);

			dbManagerUtilMockedStatic.when(
				DBManagerUtil::getDB
			).thenReturn(
				db
			);

			Mockito.when(
				db.getDBType()
			).thenReturn(
				dbType
			);

			portalInstancePoolMockedStatic.when(
				PortalInstancePool::getCompanyIds
			).thenReturn(
				new long[runtime.availableProcessors() + 2]
			);

			propsUtilMockedStatic.when(
				() -> PropsUtil.get("jdbc.default.maximumPoolSize")
			).thenReturn(
				String.valueOf(maximumPoolSize)
			);

			BaseDBProcess baseDBProcess = new BaseDBProcess() {
			};

			ReflectionTestUtil.setFieldValue(
				BaseDBProcess.class, "_fixedThreadPoolSize",
				new AtomicInteger(0));

			int fixedThreadPoolSize = ReflectionTestUtil.invoke(
				baseDBProcess, "_getFixedThreadPoolSize", new Class<?>[0]);

			Assert.assertEquals(
				expectedFixedThreadPoolSize, fixedThreadPoolSize);
		}
	}

}
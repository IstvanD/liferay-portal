/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.db.migration.importer.jdbc;

import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.TestInfo;
import com.liferay.portal.kernel.test.util.RandomTestUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author István András Dézsi
 */
public class AutoBatchPreparedStatementUtilTest {

	@Before
	public void setUp() throws Exception {
		AutoBatchPreparedStatementUtil.start();
	}

	@After
	public void tearDown() throws Exception {
		AutoBatchPreparedStatementUtil.stop();
	}

	@Test
	@TestInfo("LPD-110959")
	public void testConcurrentAutoBatch() throws Exception {
		Connection connection = Mockito.mock(Connection.class);
		PreparedStatement preparedStatement = Mockito.mock(
			PreparedStatement.class);

		Mockito.when(
			connection.prepareStatement(Mockito.anyString())
		).thenReturn(
			preparedStatement
		);

		try (PreparedStatement autoBatchPreparedStatement =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection, RandomTestUtil.randomString())) {

			autoBatchPreparedStatement.addBatch();
			autoBatchPreparedStatement.executeBatch();
		}

		Mockito.verify(
			preparedStatement
		).close();
	}

	@Test
	@TestInfo("LPD-110959")
	public void testStart() {
		ThreadPoolExecutor threadPoolExecutor =
			ReflectionTestUtil.getFieldValue(
				AutoBatchPreparedStatementUtil.class, "_executorService");

		BlockingQueue<Runnable> blockingQueue = threadPoolExecutor.getQueue();

		int threadPoolSize = ReflectionTestUtil.getFieldValue(
			AutoBatchPreparedStatementUtil.class, "_THREAD_POOL_SIZE");

		Assert.assertEquals(threadPoolSize, blockingQueue.remainingCapacity());

		Assert.assertTrue(
			threadPoolExecutor.getRejectedExecutionHandler() instanceof
				ThreadPoolExecutor.CallerRunsPolicy);
	}

}
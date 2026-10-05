/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.db.index;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.db.DBResourceUtil;
import com.liferay.portal.kernel.dependency.manager.DependencyManagerSyncUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Map;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Mariano Álvaro Sáiz
 */
public class IndexUpdaterUtilTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetTableIndexesSQLMap() {
		String tablesSQL =
			"create table TestTable1 (field1 INT, field2 INT);\n\n" +
				"create table TestTable2 (field1 INT, field2 INT);";
		String indexesSQL = "create index IX_TEST1 on TestTable1 (field2);";

		Map<String, String> tableIndexesSQLMap = ReflectionTestUtil.invoke(
			IndexUpdaterUtil.class, "_getTableIndexesSQLMap",
			new Class<?>[] {String.class, String.class}, tablesSQL, indexesSQL);

		Assert.assertEquals(
			tableIndexesSQLMap.toString(), 2, tableIndexesSQLMap.size());
		Assert.assertEquals(tableIndexesSQLMap.get("TestTable1"), indexesSQL);
		Assert.assertEquals(
			tableIndexesSQLMap.get("TestTable2"), StringPool.BLANK);
	}

	@Test
	public void testUpdateAllIndexes() {
		try (MockedStatic<DBResourceUtil> dbResourceUtilMockedStatic =
				Mockito.mockStatic(DBResourceUtil.class);
			MockedStatic<DependencyManagerSyncUtil>
				dependencyManagerSyncUtilMockedStatic = Mockito.mockStatic(
					DependencyManagerSyncUtil.class)) {

			IndexUpdaterUtil.updateAllIndexes();

			dependencyManagerSyncUtilMockedStatic.verify(
				() -> DependencyManagerSyncUtil.registerSyncCallable(
					Mockito.any()));
		}
	}

}
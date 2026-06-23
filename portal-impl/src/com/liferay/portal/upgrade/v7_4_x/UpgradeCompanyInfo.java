/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.v7_4_x;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.db.partition.util.DBPartitionUtil;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.UpgradeProcessFactory;
import com.liferay.portal.kernel.upgrade.UpgradeStep;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author István András Dézsi
 */
public class UpgradeCompanyInfo extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		DBPartitionUtil.forEachCompanyId(
			companyId -> {
				_addCompanyInfoColumns();
				_copyCompanyInfo(companyId);
			});
	}

	@Override
	protected UpgradeStep[] getPostUpgradeSteps() {
		return new UpgradeStep[] {
			UpgradeProcessFactory.dropColumns("Company", _COMPANY_COLUMN_NAMES)
		};
	}

	private void _addCompanyInfoColumns() throws Exception {
		for (String[] column : _COMPANY_INFO_COLUMNS) {
			if (!hasColumn("CompanyInfo", column[0])) {
				alterTableAddColumn("CompanyInfo", column[0], column[1]);
			}
		}
	}

	private void _copyCompanyInfo(Long companyId) throws Exception {
		if (!hasColumn("Company", "homeURL")) {
			return;
		}

		try (PreparedStatement preparedStatement1 = connection.prepareStatement(
				_getSelectCompanySQL(companyId));
			PreparedStatement preparedStatement2 =
				AutoBatchPreparedStatementUtil.autoBatch(
					connection,
					StringBundler.concat(
						"update CompanyInfo set homeURL = ?, logoId = ?, name ",
						"= ?, legalName = ?, legalId = ?, legalType = ?, ",
						"sicCode = ?, tickerSymbol = ?, industry = ?, type_ = ",
						"?, size_ = ?, indexNameCurrent = ?, indexNameNext = ",
						"? where companyId = ?"))) {

			if (companyId != null) {
				preparedStatement1.setLong(1, companyId);
			}

			try (ResultSet resultSet = preparedStatement1.executeQuery()) {
				while (resultSet.next()) {
					int parameterIndex = _setCompanyInfoValues(
						preparedStatement2, resultSet, 1);

					preparedStatement2.setLong(
						parameterIndex, resultSet.getLong("companyId"));

					preparedStatement2.addBatch();
				}

				preparedStatement2.executeBatch();
			}
		}
	}

	private String _getSelectCompanySQL(Long companyId) {
		String sql = StringBundler.concat(
			"select companyId, homeURL, logoId, name, legalName, legalId, ",
			"legalType, sicCode, tickerSymbol, industry, type_, size_, ",
			"indexNameCurrent, indexNameNext from Company");

		if (companyId == null) {
			return sql;
		}

		return sql + " where companyId = ?";
	}

	private int _setCompanyInfoValues(
			PreparedStatement preparedStatement, ResultSet resultSet,
			int parameterIndex)
		throws SQLException {

		preparedStatement.setString(
			parameterIndex++, resultSet.getString("homeURL"));
		preparedStatement.setLong(
			parameterIndex++, resultSet.getLong("logoId"));
		preparedStatement.setString(
			parameterIndex++, resultSet.getString("name"));
		preparedStatement.setString(
			parameterIndex++, resultSet.getString("legalName"));
		preparedStatement.setString(
			parameterIndex++, resultSet.getString("legalId"));
		preparedStatement.setString(
			parameterIndex++, resultSet.getString("legalType"));
		preparedStatement.setString(
			parameterIndex++, resultSet.getString("sicCode"));
		preparedStatement.setString(
			parameterIndex++, resultSet.getString("tickerSymbol"));
		preparedStatement.setString(
			parameterIndex++, resultSet.getString("industry"));
		preparedStatement.setString(
			parameterIndex++, resultSet.getString("type_"));
		preparedStatement.setString(
			parameterIndex++, resultSet.getString("size_"));
		preparedStatement.setString(
			parameterIndex++, resultSet.getString("indexNameCurrent"));
		preparedStatement.setString(
			parameterIndex++, resultSet.getString("indexNameNext"));

		return parameterIndex;
	}

	private static final String[] _COMPANY_COLUMN_NAMES = {
		"homeURL", "logoId", "name", "legalName", "legalId", "legalType",
		"sicCode", "tickerSymbol", "industry", "type_", "size_",
		"indexNameCurrent", "indexNameNext"
	};

	private static final String[][] _COMPANY_INFO_COLUMNS = {
		{"homeURL", "STRING null"}, {"logoId", "LONG"},
		{"name", "VARCHAR(75) null"}, {"legalName", "VARCHAR(75) null"},
		{"legalId", "VARCHAR(75) null"}, {"legalType", "VARCHAR(75) null"},
		{"sicCode", "VARCHAR(75) null"}, {"tickerSymbol", "VARCHAR(75) null"},
		{"industry", "VARCHAR(75) null"}, {"type_", "VARCHAR(75) null"},
		{"size_", "VARCHAR(75) null"}, {"indexNameCurrent", "VARCHAR(75) null"},
		{"indexNameNext", "VARCHAR(75) null"}
	};

}
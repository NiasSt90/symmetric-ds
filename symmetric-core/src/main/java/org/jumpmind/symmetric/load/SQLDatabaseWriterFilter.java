package org.jumpmind.symmetric.load;

import bsh.TargetError;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.sql.*;
import org.jumpmind.symmetric.*;
import org.jumpmind.symmetric.common.Constants;
import org.jumpmind.symmetric.io.data.*;
import org.jumpmind.symmetric.model.*;
import org.jumpmind.util.*;

import java.util.*;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * User: Markus Schulz <msc@antzsystem.de>
 * Date: 24.08.15
 * Time: 10:53
 */
public class SQLDatabaseWriterFilter extends DynamicDatabaseWriterFilter {

	protected static final ISqlRowMapper<Boolean> lookupColumnRowMapper = new ISqlRowMapper<Boolean>() {
		@Override
		public Boolean mapRow(Row row) {
			return Boolean.TRUE.equals(row.values().iterator().next());
		}
	};

	private static final String OLD_ = "OLD_";

	public SQLDatabaseWriterFilter(ISymmetricEngine engine, Map<String, List<LoadFilter>> loadFilters) {
		super(engine, loadFilters);
	}

	@Override
	protected boolean processLoadFilters(DataContext context, Table table, CsvData data, Exception error,
		WriteMethod writeMethod, List<LoadFilter> loadFiltersForTable) {

		boolean writeRow = true;
		LoadFilter currentFilter = null;
		List<Boolean> values = null;
		try {
			LinkedCaseInsensitiveMap<Object> namedParams = null;
			for (LoadFilter filter : loadFiltersForTable) {
				currentFilter = filter;
				values = null;
				if (filter.isFilterOnDelete() && data.getDataEventType().equals(DataEventType.DELETE)
					|| filter.isFilterOnInsert() && data.getDataEventType().equals(DataEventType.INSERT)
					|| filter.isFilterOnUpdate() && data.getDataEventType().equals(DataEventType.UPDATE)) {
					String sql = null;
					if (writeMethod.equals(WriteMethod.BEFORE_WRITE) && filter.getBeforeWriteScript() != null) {
						sql = doTokenReplacementOnSql(context, filter.getBeforeWriteScript());
					}
					else if (writeMethod.equals(WriteMethod.AFTER_WRITE) && filter.getAfterWriteScript() != null) {
						sql = doTokenReplacementOnSql(context, filter.getAfterWriteScript());
					}
					else if (writeMethod.equals(WriteMethod.HANDLE_ERROR) && filter.getHandleErrorScript() != null) {
						sql = doTokenReplacementOnSql(context, filter.getHandleErrorScript());
					}
					if (sql != null && !sql.trim().isEmpty()) {
						if (namedParams == null) {
							namedParams = getVariablesMap(table, data);
						}
						ISqlTransaction transaction = context.findTransaction();
						values = transaction.query(sql, lookupColumnRowMapper, namedParams);
					}

					if (values != null && values.size() > 0) {
						writeRow = values.get(0);
					}
				}
			}
		}
		catch (Exception ex) {
			processError(currentFilter, table, ex);
		}
		return writeRow;
	}

	private LinkedCaseInsensitiveMap<Object> getVariablesMap(Table table, CsvData data) {
		LinkedCaseInsensitiveMap<Object> namedParams = new LinkedCaseInsensitiveMap<Object>();
		if (data != null) {
			Map<String, String> sourceValues = data.toColumnNameValuePairs(table.getColumnNames(), CsvData.ROW_DATA);
			if (sourceValues.size() > 0) {
				for (String columnName : sourceValues.keySet()) {
					namedParams.put(columnName, sourceValues.get(columnName));
					namedParams.put(columnName.toUpperCase(), sourceValues.get(columnName));
				}
			}
			else {
				Map<String, String> pkValues = data.toColumnNameValuePairs(
					table.getPrimaryKeyColumnNames(), CsvData.PK_DATA);
				for (String columnName : pkValues.keySet()) {
					namedParams.put(columnName, pkValues.get(columnName));
					namedParams.put(columnName.toUpperCase(), pkValues.get(columnName));
				}
			}

			Map<String, String> oldValues = data.toColumnNameValuePairs(table.getColumnNames(),
				CsvData.OLD_DATA);
			for (String columnName : oldValues.keySet()) {
				namedParams.put(OLD_ + columnName, oldValues.get(columnName));
				namedParams.put(OLD_ + columnName.toUpperCase(), oldValues.get(columnName));
			}
		}
		return namedParams;
	}

	@Override
	protected void executeScripts(DataContext context, String key, Set<String> scripts, boolean isFailOnError) {
		if (scripts != null) {
			try {
				ISqlTransaction transaction = context.findTransaction();
				for (String script : scripts) {
					String sql = doTokenReplacementOnSql(context, script);
					transaction.query(sql, lookupColumnRowMapper, null);
				}
			}
			catch (Exception e) {
				if (isFailOnError) {
					throw (RuntimeException) e;
				}
				else {
					log.error("Failed while executing sql script", e);
				}
			}
		}
	}

	protected String doTokenReplacementOnSql(DataContext context, String sql) {
		if (isNotBlank(sql)) {
			Data csvData = (Data) context.get(Constants.DATA_CONTEXT_CURRENT_CSV_DATA);

			if (csvData != null && csvData.getTriggerHistory() != null) {
				sql = FormatUtils
					.replaceToken(sql, "sourceCatalogName", csvData.getTriggerHistory().getSourceCatalogName(), true);
			}

			if (csvData != null && csvData.getTriggerHistory() != null) {
				sql = FormatUtils
					.replaceToken(sql, "sourceSchemaName", csvData.getTriggerHistory().getSourceSchemaName(), true);
			}
		}
		return sql;
	}


	protected void processError(LoadFilter currentFilter, Table table, Throwable ex) {
		if (ex instanceof TargetError) {
			ex = ((TargetError) ex).getTarget();
		}
		String formattedMessage = String
			.format("Error executing sql script for load filter %s on table %s. The error was: %s",
				new Object[]{currentFilter != null ? currentFilter.getLoadFilterId() : "N/A", table.getName(),
								 ex.getMessage()});
		log.error(formattedMessage);
		if (currentFilter.isFailOnError()) {
			throw new SymmetricException(formattedMessage, ex);
		}
	}

}

package com.server.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.server.framework.common.DateUtil;
import com.server.framework.strategy.QueryStrategyContext;
import com.server.framework.factory.DatabaseConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class AdminDBService {
    private static final Logger logger = LoggerFactory.getLogger(AdminDBService.class);

    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private QueryStrategyContext queryStrategyContext;
    
    @Autowired
    private DatabaseConnectionFactory connectionFactory;

    @Transactional
    public Object executeQuery(String query, boolean needTable, boolean needColumn, String tableName) {
        try {
            Session session = entityManager.unwrap(Session.class);
            SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) session.getSessionFactory();
            Connection connection = sessionFactory.getServiceRegistry()
                .getService(ConnectionProvider.class)
                .getConnection();

            try {
                if (needTable) {
                    return getTableList(connection);
                }

                if (needColumn) {
                    if (tableName == null || tableName.trim().isEmpty()) {
                        tableName = "HttpLogs";
                    }
                    String singleTableName = tableName.split(",")[0].trim();
                    return getColumnMetadata(connection, singleTableName);
                }

                return executeQueryWithStrategy(connection, query);
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        } catch (Exception e) {
            logger.error("Error executing query: " + e.getMessage(), e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Error: " + e.getMessage());
            return error;
        }
    }

    private List<String> getTableList(Connection connection) throws SQLException {
        List<String> tableList = new ArrayList<>();
        try {
            ResultSet rs = connection.createStatement().executeQuery("Show tables");
            while (rs.next()) {
                tableList.add(rs.getString(1));
            }
        } catch (Exception e) {
            logger.warn("Error getting tables with 'Show tables', trying metadata approach", e);
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                tableList.add(rs.getString("TABLE_NAME"));
            }
        }

        tableList.add("Throttle");
        return tableList;
    }

    private Map<String, Object> getColumnMetadata(Connection connection, String table) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();

        if (StringUtils.equals(table, "Throttle")) {
            result.put("columns", Arrays.asList("IP", "URI", "REQUEST_COUNT", "TIME_FRAME_START"));
            result.put("pk", "");
            return result;
        }

        DatabaseMetaData metaData = connection.getMetaData();
        List<String> columnList = new ArrayList<>();
        List<String> bigIntColumns = new ArrayList<>();
        String pkName = "";

        ResultSet columnResultSet = metaData.getColumns(null, null, table, null);
        while (columnResultSet.next()) {
            String columnName = columnResultSet.getString("COLUMN_NAME").toUpperCase();
            columnList.add(columnName);
            if (columnResultSet.getInt("DATA_TYPE") == Types.BIGINT) {
                bigIntColumns.add(columnName);
            }
            if (StringUtils.equals("CreatedTime".toUpperCase(), columnName)) {
                columnList.add("FormattedTime");
            }
        }

        if (!bigIntColumns.isEmpty()) {
            ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, table);
            while (primaryKeys.next()) {
                String pk = primaryKeys.getString("COLUMN_NAME").toUpperCase();
                if (bigIntColumns.contains(pk)) {
                    pkName = pk;
                    break;
                }
            }
        }

        result.put("columns", columnList);
        result.put("pk", pkName);
        return result;
    }

    private Map<String, Object> executeQueryWithStrategy(Connection connection, String query) throws SQLException {
        try {
            List<Map<String, String>> queryResult = queryStrategyContext.executeQuery(connection, query, null);
            
            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("query_output", queryResult);
            return resultMap;
        } catch (SQLException e) {
            logger.error("Strategy-based query execution failed, falling back to direct execution: " + e.getMessage());
            return executeRegularQuery(connection, query);
        }
    }
    
    private Map<String, Object> executeRegularQuery(Connection connection, String query) throws SQLException {
        Map<String, Object> resultMap = new LinkedHashMap<>();

        if (StringUtils.isBlank(StringUtils.deleteWhitespace(query))) {
            return resultMap;
        }

        query = query.replaceAll("\n", StringUtils.SPACE);
        query = query.replaceAll("([.;])$", StringUtils.EMPTY);

        String[] queryParts = parseQuery(query);
        String startQuery = queryParts[0];
        String endQuery = queryParts[1];

        PreparedStatement preparedStatement = connection.prepareStatement(startQuery + endQuery);
        preparedStatement.execute();

        if (!query.matches("(?i)select(.*)")) {
            resultMap.put("query_output", "Executed successfully");
            return resultMap;
        }

        ResultSet resultSet = preparedStatement.getResultSet();
        ResultSetMetaData metaData = resultSet.getMetaData();

        List<Map<String, String>> queryOutput = new ArrayList<>();
        boolean hasRows = false;

        while (resultSet.next()) {
            hasRows = true;
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i).toUpperCase();
                row.put(columnName, resultSet.getString(i));

                if (StringUtils.equals("CreatedTime", metaData.getColumnName(i))) {
                    Long timeValue = resultSet.getLong(i);
                    row.put("FORMATTEDTIME", DateUtil.getFormattedTime(timeValue, DateUtil.DATE_WITH_TIME_FORMAT));
                }
            }
            queryOutput.add(row);
        }

        if (!hasRows) {
            Map<String, String> emptyRow = new LinkedHashMap<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                emptyRow.put(metaData.getColumnName(i).toUpperCase(), "<EMPTY>");
            }
            queryOutput.add(emptyRow);
        }

        resultMap.put("query_output", queryOutput);
        return resultMap;
    }


    private String[] parseQuery(String query) {
        Pattern orderByPattern = Pattern.compile("(.*)(?i)(order)\\s+(?i)(by)(.*)");
        Pattern groupByPattern = Pattern.compile("(.*)(?i)(group)\\s+(?i)(by)(.*)");
        Pattern havingPattern = Pattern.compile("(.*)(?i)(having)(.*)");
        Pattern limitPattern = Pattern.compile("(.*)(?i)(limit)(.*)");

        String startQuery = query;
        String endQuery = "";

        Matcher matcher;
        if ((matcher = orderByPattern.matcher(query)).matches()) {
            startQuery = query.substring(0, matcher.start(2));
            endQuery = query.substring(matcher.start(2));
        } else if ((matcher = groupByPattern.matcher(query)).matches()) {
            startQuery = query.substring(0, matcher.start(2));
            endQuery = query.substring(matcher.start(2));
        } else if ((matcher = havingPattern.matcher(query)).matches()) {
            startQuery = query.substring(0, matcher.start(2));
            endQuery = query.substring(matcher.start(2));
        } else if ((matcher = limitPattern.matcher(query)).matches()) {
            startQuery = query.substring(0, matcher.start(2));
            endQuery = query.substring(matcher.start(2));
        }

        return new String[]{startQuery, endQuery};
    }
}

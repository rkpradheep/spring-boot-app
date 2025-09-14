package com.server.zoho;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

import com.server.framework.common.AppProperties;
import com.server.framework.common.CommonService;
import com.server.framework.common.DateUtil;
import com.server.framework.error.AppException;

import org.springframework.stereotype.Service;

@Service
public class SASService {
    private static final ThreadLocal<AtomicInteger> CONNECTION_RETRY_ATTEMPT = ThreadLocal.withInitial(() -> new AtomicInteger(0));

    public long[] getLimits(Long spaceID) {
        long rangeId = spaceID % 9000000L;
        long startId = (rangeId - 1L) * 1000000000000L;
        long endId = rangeId * 1000000000000L - 1L;
        return new long[] {startId, endId};
    }

    public String getSpaceIDFromPK(Long pk) {
        pk = (pk / 1000000000000L) + 1;
        return pk.toString();
    }

    public String getZSIDFromPK(Connection connection, String pk) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(AppProperties.getProperty("sas.zsid.from.pk.query"));
        statement.setLong(1, Long.parseLong(getSpaceIDFromPK(Long.parseLong(pk))));
        statement.execute();
        ResultSet resultSet = statement.getResultSet();
        resultSet.next();
        return resultSet.getString("LOGINNAME");
    }

    public boolean isMultiGrid(Connection connection) throws Exception {
        PreparedStatement statement = connection.prepareStatement(AppProperties.getProperty("sas.multigrid.query"));
        statement.setString(1, "multigrid");
        statement.execute();
        ResultSet resultSet = statement.getResultSet();
        if (!resultSet.next()) {
            return false;
        }
        return resultSet.getBoolean("PROPVAL");
    }

    public String getClusterIP(Connection connection, String clusterID) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(AppProperties.getProperty("sas.cluster.ip.query"));
        statement.setInt(1, Integer.parseInt(clusterID));
        statement.execute();
        ResultSet resultSet = statement.getResultSet();
        resultSet.next();
        return resultSet.getString("ADDRESS");
    }

    public Pair<String, String> getMasterSlaveIPPair(Connection connection, String clusterIP) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(AppProperties.getProperty("sas.master.slave.ip.fetch.query"));
        statement.setObject(1, clusterIP);
        statement.execute();
        ResultSet resultSet = statement.getResultSet();
        resultSet.next();
        return new ImmutablePair<>(resultSet.getString("masterip"), resultSet.getString("slaveip"));
    }

    public Connection getDBConnection(String server, String ip, String db, String user, String password) throws Exception {
        try {
            Connection conn;
            if (StringUtils.equals("mysql", server)) {
                Class.forName("com.mysql.jdbc.Driver");
                conn = DriverManager.getConnection(MessageFormat.format("jdbc:mysql://{0}:3306/{1}?connectTimeout=5000&useSSL=false", ip, db), user, password);
            } else {
                Class.forName("org.postgresql.Driver");
                conn = DriverManager.getConnection(MessageFormat.format("jdbc:postgresql://{0}:5432/sasdb?currentSchema={1}&connectTimeout=20&useSSL=false", ip, db), user, password);
            }
            conn.setAutoCommit(false);
            return conn;
        } catch (Exception e) {
            CONNECTION_RETRY_ATTEMPT.get().incrementAndGet();
            if(CONNECTION_RETRY_ATTEMPT.get().get() >= 3)
            {
                throw e;
            }
            else
            {
                Thread.sleep(DateUtil.ONE_SECOND_IN_MILLISECOND * 3);
                return getDBConnection(server, ip, db, user, password);
            }
        } finally {
            CONNECTION_RETRY_ATTEMPT.get().set(0);
        }
    }

    public void addUserDetails(String server, String ip, String db, String user, String password, Map<String, Object> resultMap, Long sasStartRange, Long sasEndRange) {
        if (getUserDetails("UserID", server, ip, db, user, password, resultMap, sasStartRange, sasEndRange)) {
            return;
        }
        getUserDetails("ID", server, ip, db, user, password, resultMap, sasStartRange, sasEndRange);
    }

    private boolean getUserDetails(String userPk, String server, String ip, String db, String user, String password, Map<String, Object> resultMap, Long sasStartRange, Long sasEndRange) {
        try (Connection connection = getDBConnection(server, ip, db, user, password)) {
            PreparedStatement preparedStatement = connection.prepareStatement(MessageFormat.format(AppProperties.getProperty("service.user.details.query"), userPk));
            preparedStatement.setObject(1, sasStartRange);
            preparedStatement.setObject(2, sasEndRange);

            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();

            List<Map<String, String>> userList = new ArrayList<>();
            while (resultSet.next()) {
                Map<String, String> userDetails = new LinkedHashMap<>();

                userDetails.put("name", resultSet.getString("Name"));
                userDetails.put("email", resultSet.getString("Email"));
                userDetails.put("zuid", resultSet.getString("ZUID"));

                userList.add(userDetails);
            }
            resultMap.put("users", userList);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void handleQuery(String query, String server, String ip, String db, String user, String password, Map<String, Object> resultMap, Long sasStartRange, Long sasEndRange, boolean skipScoping) {
        String pkName = null;
        if (query == null || query.replaceAll(" ", "").length() == 0) {
            return;
        }
        query = query.replaceAll("\n", " ").trim();
        query = query.replaceAll("(\\.|;)$", "");
        boolean isUpdateOrDelete = false;
        try (Connection connection = getDBConnection(server, ip, db, user, password)) {
            PreparedStatement preparedStatement;

            Pattern selectQuerPattern = Pattern.compile("(?i)(select)(.*)(from)\\s+(\\w+)(.*)");
            Matcher selectQueryMatcher = selectQuerPattern.matcher(query);
            Pattern updatePattern = Pattern.compile("(?i)(Update)\\s+(\\w+)\\s+(set)(.*)\\s+(where)\\s+(.*)");
            Matcher updateMatcher = updatePattern.matcher(query);
            Pattern deletePattern = Pattern.compile("(?i)(Delete)\\s+(from)\\s+(\\w+)\\s+(where)\\s+(.*)");
            Matcher deleteMatcher = deletePattern.matcher(query);
            if (selectQueryMatcher.matches()) {
                ResultSet primaryKeys = connection.getMetaData().getPrimaryKeys(null, "jbossdb", selectQueryMatcher.group(4));
                while (primaryKeys.next()) {
                    pkName = primaryKeys.getString("COLUMN_NAME");
                    if (pkName.matches("(.*)(?i)(id)")) {
                        Pattern allisaPattern = Pattern.compile("(?i)(select)(.*)(from)\\s+(\\w+)\\s+AS\\s+(\\w+)\\s+(.*)");
                        Matcher aliasMatcher = allisaPattern.matcher(query);
                        pkName = (aliasMatcher.matches() ? aliasMatcher.group(5) : selectQueryMatcher.group(4)).concat(".").concat(pkName);
                        break;
                    }
                }
            } else if (updateMatcher.matches() || deleteMatcher.matches()) {
                isUpdateOrDelete = true;
                String tableName = updateMatcher.matches() ? updateMatcher.group(2) : deleteMatcher.group(3);
                ZohoService.doAuthentication();
                ResultSet primaryKeys = connection.getMetaData().getPrimaryKeys(null, "jbossdb", tableName);
                while (primaryKeys.next()) {
                    pkName = primaryKeys.getString("COLUMN_NAME");
                    if (pkName.matches("(.*)(?i)(id)")) {
                        break;
                    } else {
                        pkName = null;
                    }
                }

                int datType = -1;
                ResultSet resultSet = connection.getMetaData().getColumns(null, "jbossdb", tableName, null);
                while (resultSet.next()) {
                    if (StringUtils.equals(pkName, resultSet.getString("COLUMN_NAME"))) {
                        datType = Integer.parseInt(resultSet.getString("DATA_TYPE"));
                    }
                }
                if (StringUtils.isEmpty(pkName) || datType != Types.BIGINT) {
                    throw new AppException("Update query cannot be performed as PK column with BIGINT type is not found for this table!");
                }
            }

            Matcher orderByMatcher = Pattern.compile("(.*)(?i)(order)\\s+(?i)(by)(.*)").matcher(query);
            Matcher groupByMatcher = Pattern.compile("(.*)(?i)(group)\\s+(?i)(by)(.*)").matcher(query);
            Matcher havingMatcher = Pattern.compile("(.*)(?i)(having)(.*)").matcher(query);
            Matcher limitMatcher = Pattern.compile("(.*)(?i)(limit)(.*)").matcher(query);

            String startQuery = query;
            String endQuery = "";
            if (orderByMatcher.matches()) {
                startQuery = query.substring(0, orderByMatcher.start(2));
                endQuery = query.substring(orderByMatcher.start(2));
            } else if (groupByMatcher.matches()) {
                startQuery = query.substring(0, groupByMatcher.start(2));
                endQuery = query.substring(groupByMatcher.start(2));
            } else if (havingMatcher.matches()) {
                startQuery = query.substring(0, havingMatcher.start(2));
                endQuery = query.substring(havingMatcher.start(2));
            } else if (limitMatcher.matches()) {
                startQuery = query.substring(0, limitMatcher.start(2));
                endQuery = query.substring(limitMatcher.start(2));
            }

            if (skipScoping || pkName == null || !pkName.matches("(.*)(?i)(id)") || pkName.equalsIgnoreCase("zsid") || pkName.equalsIgnoreCase("zuid")) {
                preparedStatement = connection.prepareStatement(startQuery + endQuery);
            } else {
                if (query.matches("(.*)(?i)(where)(.*)")) {
                    preparedStatement = connection.prepareStatement(startQuery + MessageFormat.format(" AND {0}>= ? AND {0}<=? ", pkName).concat(endQuery));
                } else {
                    preparedStatement = connection.prepareStatement(startQuery + MessageFormat.format(" where {0}>= ? AND {0}<=? ", pkName).concat(endQuery));
                }
                preparedStatement.setObject(1, sasStartRange);
                preparedStatement.setObject(2, sasEndRange);
            }

            if (isUpdateOrDelete) {
                int updatedRecords = preparedStatement.executeUpdate();
                if (updatedRecords > 1) {
                    connection.rollback();
                    throw new AppException("Query cannot be executed as it affects more than one record");
                }

                connection.commit();
                resultMap.put("query_output", (updateMatcher.matches() ? "Update" : "Delete") + " query executed successfully");
                return;
            }

            preparedStatement.executeQuery();
            List<Map<String, String>> queryOutput = getQueryOutput(preparedStatement);
            resultMap.put("query_output", queryOutput);
        }
        catch (AppException ae) {
            throw ae;
        }
        catch (Exception e) {
            resultMap.put("query_output", e.getMessage());
        }
    }

    List<Map<String, String>> getQueryOutput(PreparedStatement preparedStatement) throws SQLException {
        ResultSet resultSet = preparedStatement.getResultSet();
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

        List<Map<String, String>> queryOutput = new ArrayList<>();
        while (resultSet.next()) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                row.put(resultSetMetaData.getColumnLabel(i).toUpperCase(), resultSet.getString(i));
            }
            queryOutput.add(row);
        }
        if (queryOutput.isEmpty()) {
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                row.put(resultSetMetaData.getColumnName(i).toUpperCase(), "<EMPTY>");
            }
            queryOutput.add(row);
        }
        return queryOutput;
    }

    void handleDecryption(jakarta.servlet.http.HttpServletRequest request, JSONObject credentials) throws Exception {
        try {
            java.security.PrivateKey privateKey = (java.security.PrivateKey) request.getSession().getAttribute("private_key");
            if (privateKey == null) {
                throw new Exception();
            }

            if (!credentials.has("ip") || !credentials.has("user") || !credentials.has("password")) {
                throw new Exception();
            }
            
            credentials.put("ip", CommonService.decryptData(privateKey, credentials.getString("ip")));
            credentials.put("user", CommonService.decryptData(privateKey, credentials.getString("user")));
            credentials.put("password", CommonService.decryptData(privateKey, credentials.getString("password")));
        } catch (Exception e) {
            throw new AppException("key_expired", "Key expired");
        }
    }
}


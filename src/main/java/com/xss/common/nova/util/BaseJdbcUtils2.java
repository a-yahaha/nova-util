package com.xss.common.nova.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @author poi
 * @version 1.0.0
 * @description
 * @date created in 14:13 2019-05-08
 */
public class BaseJdbcUtils2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseJdbcUtils2.class);

    public BaseJdbcUtils2() {
    }

    public static BaseJdbcUtils2.JdbcResult getSelect(String tableName, Class<?> entityClass, Map<String, String> entityToDbMap, Map<String, Object> criteria, Map<String, Object> notEqualCriteria) {
        StringBuilder sql = new StringBuilder();
        if (entityClass != null) {
            sql.append("SELECT ");
            int index = 0;
            Field[] var7 = entityClass.getDeclaredFields();
            int var8 = var7.length;

            for(int var9 = 0; var9 < var8; ++var9) {
                Field entityField = var7[var9];
                if (entityToDbMap.get(entityField.getName()) != null) {
                    if (index != 0) {
                        sql.append(",");
                    }

                    sql.append((String)entityToDbMap.get(entityField.getName()));
                    ++index;
                } else {
                    LOGGER.warn("entity中的属性'{}'找不到对应的数据库'{}'字段.", entityField.getName(), tableName);
                }
            }

            sql.append(" FROM ").append(tableName);
        } else {
            sql.append("SELECT * FROM ").append(tableName);
        }

        BaseJdbcUtils2.JdbcResult jdbcResult = getWhereClause(tableName, criteria, notEqualCriteria);
        return jdbcResult != null ? new BaseJdbcUtils2.JdbcResult(sql.append(jdbcResult.getSql()).toString(), jdbcResult.getParams()) : new BaseJdbcUtils2.JdbcResult(sql.toString(), (Object[])null);
    }

    public static BaseJdbcUtils2.JdbcResult getSelectForCount(String tableName, Map<String, Object> criteria, Map<String, Object> notEqualCriteria) {
        StringBuilder sql = (new StringBuilder("SELECT count(*) FROM ")).append(tableName);
        BaseJdbcUtils2.JdbcResult jdbcResult = getWhereClause(tableName, criteria, notEqualCriteria);
        return jdbcResult != null ? new BaseJdbcUtils2.JdbcResult(sql.append(jdbcResult.getSql()).toString(), jdbcResult.getParams()) : new BaseJdbcUtils2.JdbcResult(sql.toString(), (Object[])null);
    }

    public static BaseJdbcUtils2.JdbcResult getSelectForSum(String tableName, String dbColumToBeSummed, Map<String, Object> criteria, Map<String, Object> notEqualCriteria) {
        StringBuilder sql = (new StringBuilder("SELECT sum(")).append(dbColumToBeSummed).append(") FROM ").append(tableName);
        BaseJdbcUtils2.JdbcResult jdbcResult = getWhereClause(tableName, criteria, notEqualCriteria);
        return jdbcResult != null ? new BaseJdbcUtils2.JdbcResult(sql.append(jdbcResult.getSql()).toString(), jdbcResult.getParams()) : new BaseJdbcUtils2.JdbcResult(sql.toString(), (Object[])null);
    }

    public static BaseJdbcUtils2.JdbcResult getInsert(String tableName, Object entity, Map<String, String> entityToDbMap) {
        StringBuilder sql = (new StringBuilder("INSERT INTO ")).append(tableName);
        Map<String, Object> dbMap = transferEntityToDbMap(entity, entityToDbMap);
        Object[] params = null;
        if (dbMap != null && dbMap.size() > 0) {
            params = new Object[dbMap.size()];
            int index = 0;
            sql.append("(");

            Iterator var7;
            Map.Entry entry;
            for(var7 = dbMap.entrySet().iterator(); var7.hasNext(); ++index) {
                entry = (Map.Entry)var7.next();
                if (index != 0) {
                    sql.append(",");
                }

                sql.append((String)entry.getKey());
            }

            sql.append(") VALUES (");
            index = 0;

            for(var7 = dbMap.entrySet().iterator(); var7.hasNext(); ++index) {
                entry = (Map.Entry)var7.next();
                if (index != 0) {
                    sql.append(",");
                }

                sql.append("?");
                params[index] = entry.getValue();
            }

            sql.append(")");
            return new BaseJdbcUtils2.JdbcResult(sql.toString(), params);
        } else {
            return null;
        }
    }

    public static String getNamedInsert(String tableName, Class<?> cls, Map<String, String> entityToDbMap) {
        List<String> props = new ArrayList();
        StringBuilder sql = (new StringBuilder("INSERT INTO ")).append(tableName).append("(");
        int index = 0;
        Field[] var6 = cls.getDeclaredFields();
        int var7 = var6.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            Field field = var6[var8];
            String fieldName = field.getName();
            String colName = (String)entityToDbMap.get(fieldName);
            if (StringUtils.isBlank(colName)) {
                LOGGER.warn("类[{}]field[{}]没有对应的数据库字段", cls.getSimpleName(), fieldName);
            } else {
                props.add(fieldName);
                if (index != 0) {
                    sql.append(",");
                }

                sql.append(colName);
                ++index;
            }
        }

        sql.append(") VALUES (");
        index = 0;

        for(Iterator var12 = props.iterator(); var12.hasNext(); ++index) {
            String prop = (String)var12.next();
            if (index != 0) {
                sql.append(",");
            }

            sql.append(":").append(prop);
        }

        sql.append(")");
        return sql.toString();
    }

    public static String getNamedUpdate(String tableName, Class<?> cls, Map<String, String> entityToDbMap) {
        StringBuilder sql = (new StringBuilder("UPDATE ")).append(tableName).append(" SET ");
        int index = 0;
        Field[] var5 = cls.getDeclaredFields();
        int var6 = var5.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            Field field = var5[var7];
            String fieldName = field.getName();
            String colName = (String)entityToDbMap.get(fieldName);
            if (StringUtils.isBlank(colName)) {
                LOGGER.warn("类[{}]field[{}]没有对应的数据库字段", cls.getSimpleName(), fieldName);
            } else {
                if (index != 0) {
                    sql.append(",");
                }

                sql.append(colName).append("=:").append(fieldName);
                ++index;
            }
        }

        return sql.toString();
    }

    public static String getInsertSql(String tableName, Map<String, String> entityToDbMap) {
        StringBuilder sql = (new StringBuilder("INSERT INTO ")).append(tableName).append(" (");
        int size = entityToDbMap.size();
        int index = 0;

        for(Iterator var5 = entityToDbMap.entrySet().iterator(); var5.hasNext(); ++index) {
            Map.Entry<String, String> entry = (Map.Entry)var5.next();
            sql.append((String)entry.getValue());
            if (index != size - 1) {
                sql.append(",");
            }
        }

        sql.append(") VALUES (");

        for(int i = 0; i < size; ++i) {
            sql.append("?");
            if (i != size - 1) {
                sql.append(",");
            }
        }

        sql.append(")");
        return sql.toString();
    }

    public static Object[] getInsertParam(Object entity, Map<String, String> entityToDbMap) {
        int size = entityToDbMap.size();
        Class entityClass = entity.getClass();

        try {
            Object[] params = new Object[size];
            int index = 0;

            for(Iterator var6 = entityToDbMap.entrySet().iterator(); var6.hasNext(); ++index) {
                Map.Entry<String, String> entry = (Map.Entry)var6.next();
                Field field = entityClass.getDeclaredField((String)entry.getKey());
                field.setAccessible(true);
                params[index] = field.get(entity);
            }

            return params;
        } catch (Exception var9) {
            throw new RuntimeException("获取sql插入语句的参数出错, entity=" + entity.toString() + ";entityToDbMap=" + entityToDbMap, var9);
        }
    }

    public static BaseJdbcUtils2.JdbcResult getUpdate(String tableName, Object entity, Map<String, String> entityToDbMap, Map<String, Object> criteria, Map<String, Object> notEqualCriteria) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> dbMap = transferEntityToDbMap(entity, entityToDbMap);
        if (dbMap != null && dbMap.size() > 0) {
            Object[] paramsForSet = new Object[dbMap.size()];
            sql.append("UPDATE ").append(tableName).append(" SET ");
            int index = 0;

            for(Iterator var9 = dbMap.entrySet().iterator(); var9.hasNext(); ++index) {
                Map.Entry<String, Object> entry = (Map.Entry)var9.next();
                if (index != 0) {
                    sql.append(",");
                }

                sql.append((String)entry.getKey()).append("=?");
                paramsForSet[index] = entry.getValue();
            }

            Object[] paramsForWhere = null;
            BaseJdbcUtils2.JdbcResult jdbcResult = getWhereClause(tableName, criteria, notEqualCriteria);
            if (jdbcResult != null) {
                sql.append(jdbcResult.getSql());
                paramsForWhere = jdbcResult.getParams();
            }

            Object[] params = new Object[paramsForSet.length + (paramsForWhere == null ? 0 : paramsForWhere.length)];
            index = 0;
            Object[] var12 = paramsForSet;
            int var13 = paramsForSet.length;

            int var14;
            Object obj;
            for(var14 = 0; var14 < var13; ++var14) {
                obj = var12[var14];
                params[index] = obj;
                ++index;
            }

            if (paramsForWhere != null) {
                var12 = paramsForWhere;
                var13 = paramsForWhere.length;

                for(var14 = 0; var14 < var13; ++var14) {
                    obj = var12[var14];
                    params[index] = obj;
                    ++index;
                }
            }

            return new BaseJdbcUtils2.JdbcResult(sql.toString(), params);
        } else {
            return null;
        }
    }

    public static <T> T transferDbResultToEntity(Map<String, Object> dbResult, Map<String, String> entityToDbMap, Class<T> entityClass) {
        if (dbResult != null && dbResult.size() != 0) {
            Object entity = null;

            try {
                entity = entityClass.newInstance();
                Field[] fields = entityClass.getDeclaredFields();
                Field[] var5 = fields;
                int var6 = fields.length;

                for(int var7 = 0; var7 < var6; ++var7) {
                    Field f = var5[var7];
                    f.setAccessible(true);
                    String dbColumn = (String)entityToDbMap.get(f.getName());
                    if (dbColumn != null) {
                        Object value = dbResult.get(dbColumn);
                        if (value != null) {
                            if (f.getType() != String.class && value.getClass() == String.class) {
                                PropertyEditor editor = PropertyEditorManager.findEditor(f.getType());
                                editor.setAsText(value.toString());
                                value = editor.getValue();
                            }

                            f.set(entity, value);
                        }
                    } else {
                        LOGGER.warn("类'{}'中属性'{}'没有对应的数据库字段", entityClass.getSimpleName(), f.getName());
                    }
                }

                return (T) entity;
            } catch (Exception var12) {
                throw new RuntimeException("将从数据库查询出来的结果'" + dbResult + "'转换为类'" + entityClass.getSimpleName() + "'的对象失败.", var12);
            }
        } else {
            return null;
        }
    }

    public static String printEntityToDbMap(Class<?> cls) {
        StringBuilder buf = new StringBuilder();
        Field[] var2 = cls.getDeclaredFields();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Field field = var2[var4];
            buf.append("entityToDbMap.put(\"").append(field.getName()).append("\", \"").append(field.getName()).append("\");\n");
        }

        return buf.toString();
    }

    private static BaseJdbcUtils2.JdbcResult getWhereClause(String tableName, Map<String, Object> criteria, Map<String, Object> notEqualCriteria) {
        StringBuilder sql = new StringBuilder();
        int cirteriaSize = criteria == null ? 0 : criteria.size();
        int notEqualCriteriaSize = notEqualCriteria == null ? 0 : notEqualCriteria.size();
        List<Object> params = new ArrayList();
        if (cirteriaSize + notEqualCriteriaSize <= 0) {
            return null;
        } else {
            sql.append(" WHERE (");
            int index = 0;
            Iterator var8;
            Map.Entry entry;
            String dbColumn;
            Object value;
            int innerIndex;
            Iterator var13;
            Object obj;
            if (criteria != null && criteria.size() > 0) {
                for(var8 = criteria.entrySet().iterator(); var8.hasNext(); ++index) {
                    entry = (Map.Entry)var8.next();
                    if (index != 0) {
                        sql.append(" AND ");
                    }

                    dbColumn = (String)entry.getKey();
                    value = entry.getValue();
                    if (!List.class.isAssignableFrom(value.getClass())) {
                        sql.append(dbColumn).append("=?");
                        params.add(entry.getValue());
                    } else {
                        sql.append(dbColumn).append(" IN (");
                        innerIndex = 0;

                        for(var13 = ((List)value).iterator(); var13.hasNext(); ++innerIndex) {
                            obj = var13.next();
                            if (innerIndex != 0) {
                                sql.append(",");
                            }

                            sql.append("?");
                            params.add(obj);
                        }

                        sql.append(")");
                    }
                }
            }

            if (notEqualCriteria != null && notEqualCriteria.size() > 0) {
                for(var8 = notEqualCriteria.entrySet().iterator(); var8.hasNext(); ++index) {
                    entry = (Map.Entry)var8.next();
                    if (index != 0) {
                        sql.append(" AND ");
                    }

                    dbColumn = (String)entry.getKey();
                    value = entry.getValue();
                    if (!List.class.isAssignableFrom(value.getClass())) {
                        sql.append(dbColumn).append("!=?");
                        params.add(entry.getValue());
                    } else {
                        sql.append(dbColumn).append(" NOT IN (");
                        innerIndex = 0;

                        for(var13 = ((List)value).iterator(); var13.hasNext(); ++innerIndex) {
                            obj = var13.next();
                            if (innerIndex != 0) {
                                sql.append(",");
                            }

                            sql.append("?");
                            params.add(obj);
                        }

                        sql.append(")");
                    }
                }
            }

            sql.append(")");
            return new BaseJdbcUtils2.JdbcResult(sql.toString(), params.toArray());
        }
    }

    public static Map<String, Object> transferEntityToDbMap(Object obj, Map<String, String> entityToDbMap) {
        HashMap map = new HashMap();

        try {
            Field[] fields = obj.getClass().getDeclaredFields();
            Field[] var4 = fields;
            int var5 = fields.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                Field f = var4[var6];
                f.setAccessible(true);
                if (f.get(obj) != null) {
                    String dbColumn = (String)entityToDbMap.get(f.getName());
                    if (dbColumn == null) {
                        LOGGER.warn("类'{}'中属性'{}'没有对应的数据库字段", obj.getClass().getSimpleName(), f.getName());
                    } else {
                        map.put(dbColumn, f.get(obj));
                    }
                }
            }

            return map;
        } catch (Exception var9) {
            throw new RuntimeException("将对象转换为数据库map异常", var9);
        }
    }

    public static BaseJdbcUtils2.JdbcResult getTimeSpanJdbcResult(Date from, Date to, String column) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList();
        if (from != null && to == null) {
            sql.append(" AND " + column + ">=?");
            params.add(from);
        } else if (from == null && to != null) {
            sql.append(" AND " + column + "<=?");
            params.add(to);
        } else if (from != null && to != null) {
            sql.append(" AND " + column + ">=? AND " + column + "<=?");
            params.add(from);
            params.add(to);
        }

        return new BaseJdbcUtils2.JdbcResult(sql.toString(), params.toArray());
    }

    public static BaseJdbcUtils2.JdbcResult getPagingJdbcResult(Date from, Date to, String column, Integer page, Integer limit) {
        BaseJdbcUtils2.JdbcResult jdbcResult = getTimeSpanJdbcResult(from, to, column);
        String sql = jdbcResult.getSql();
        if (limit != null) {
            if (page == null) {
                page = 1;
            }

            sql = sql + " LIMIT " + (page - 1) * limit + ", " + limit;
        }

        return new BaseJdbcUtils2.JdbcResult(sql, jdbcResult.getParams());
    }

    public static class JdbcResult {
        private String sql;
        private Object[] params;

        public JdbcResult(String sql, Object[] params) {
            this.sql = sql;
            this.params = params;
        }

        public String getSql() {
            return this.sql;
        }

        public Object[] getParams() {
            return this.params;
        }

        public String toString() {
            return BaseJsonUtils.writeValue(this);
        }
    }
}

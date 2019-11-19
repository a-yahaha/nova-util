package com.xss.common.nova.util;

import com.google.common.base.Preconditions;
import com.xss.common.nova.annotation.PrimaryKey;
import com.xss.common.nova.annotation.UpdateIgnore;
import com.xss.common.nova.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 生成sql语句及相应参数的帮助类.
 * 内部有大量的重复方法, 如参数类型为Criteria及List<Criteria>, 之所以不使用Criteria...就是要强迫使用者在有多个Criterion的情况下使用
 * 更可读的List<Criteria>
 */
@Slf4j
public class BaseJdbcUtils {
    private static Map<Class, List<String>> updateFieldsMap = new ConcurrentHashMap<>();
    private static DbType dbType = DbType.MYSQL;

    public static void defaultDbType(DbType dbType) {
        BaseJdbcUtils.dbType = dbType;
    }

    public static JdbcResult getInsert(String table, Object entity, Map<String, String> dbMapping) {
        Preconditions.checkArgument(entity != null, "数据库插入数据时entity对象不能为空");
        return insertSql(table, Arrays.asList(entity), dbMapping, false);
    }

    @Deprecated
    public static JdbcResult getInsertIgnore(String table, Object entity, Map<String, String> dbMapping) {
        Preconditions.checkArgument(entity != null, "数据库插入数据时entity对象不能为空");
        return insertSql(table, Arrays.asList(entity), dbMapping, true);
    }

    public static JdbcResult getInsertOrUpdate(String table, Object entity, Map<String, String> dbMapping) {
        Preconditions.checkArgument(entity != null, "数据库插入数据时entity对象不能为空");
        return getBatchInsertOrUpdate(table, Arrays.asList(entity), dbMapping);
    }

    public static JdbcResult getInsertOrUpdate(String table, Object entity, Map<String, String> dbMapping, Map<String, UpdateOper> updateOperations) {
        Preconditions.checkArgument(entity != null, "数据库插入数据时entity对象不能为空");
        return getBatchInsertOrUpdate(table, Arrays.asList(entity), dbMapping, updateOperations);
    }

    public static JdbcResult getBatchInsert(String table, List<? extends Object> entities, Map<String, String> dbMapping) {
        return insertSql(table, entities, dbMapping, false);
    }

    public static JdbcResult getBatchInsertIgnore(String table, List<? extends Object> entities, Map<String, String> dbMapping) {
        return insertSql(table, entities, dbMapping, true);
    }

    public static JdbcResult getBatchInsertOrUpdate(String table, List<? extends Object> entities, Map<String, String> dbMapping) {
        return getBatchInsertOrUpdate(table, entities, dbMapping, null);
    }

    public static JdbcResult getBatchInsertOrUpdate(String table, List<? extends Object> entities, Map<String, String> dbMapping, Map<String, UpdateOper> updateOperations) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(entities), "数据库插入数据时entity对象不能为空");
        JdbcResult jdbcResult = insertSql(table, entities, dbMapping, false);
        StringBuilder sql = new StringBuilder(jdbcResult.getSql());
        sql.append(" ON DUPLICATE KEY UPDATE ");


        //获取需要更新的列
        List<String> updateFields = updateFields(entities.get(0).getClass(), dbMapping);

        //组装sql参数
        List<Object[]> paramList = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            Object[] params = jdbcResult.getParamsList().get(i);
            Map<String, Object> entityMap = BaseBeanUtils.beanToMap(entities.get(i));

            if (i == 0) {
                for (int index = 0; index < updateFields.size(); index++) {
                    if (index != 0) {
                        sql.append(",");
                    }
                    String fieldName = updateFields.get(index);
                    sql.append(colUpdateSql(dbMapping.get(fieldName), updateOperations));
                }
            }

            List<Object> updateParams = new ArrayList();
            for (int index = 0; index < updateFields.size(); index++) {
                String fieldName = updateFields.get(index);
                String colName = dbMapping.get(fieldName);
                updateParams.add(colUpdateValue(colName, fieldName, entityMap, updateOperations));
            }

            paramList.add(ArrayUtils.addAll(params, updateParams.toArray()));
        }

        jdbcResult.setSql(sql.toString());
        jdbcResult.setParamsList(paramList);
        return jdbcResult;
    }

    public static JdbcResult getSelect(String table, Criteria criterion) {
        List<Criteria> criteria = criterion == null ? Collections.emptyList() : Arrays.asList(criterion);
        return getSelect(table, criteria, null);
    }

    public static JdbcResult getSelect(String table, List<Criteria> criteria) {
        return getSelect(table, criteria, null);
    }

    public static JdbcResult getSelect(String table, Criteria criterion, PageRequest pageRequest) {
        List<Criteria> criteria = criterion == null ? Collections.emptyList() : Arrays.asList(criterion);
        return getSelect(table, criteria, pageRequest);
    }

    public static JdbcResult getSelect(String table, List<Criteria> criteria, PageRequest pageRequest) {
        Preconditions.checkArgument(StringUtils.isNotBlank(table), "表名不能为空");
        JdbcResult jdbcResult;
        if(pageRequest != null && pageRequest.getPage() != null && pageRequest.getPage() != null && dbType == DbType.ORACLE) {
            JdbcResult whereSql = whereSql(criteria);
            StringBuilder sql = new StringBuilder("SELECT * FROM (SELECT rownum rnum, tmp.* FROM (SELECT * FROM ")
                    .append(wrapTable(table)).append(whereSql.getSql()).append(") tmp WHERE rownum<=")
                    .append(pageRequest.getPage() * pageRequest.getPageSize())
                    .append(") WHERE rnum>").append((pageRequest.getPage()-1) * pageRequest.getPageSize());
            jdbcResult = new JdbcResult(sql.toString(), whereSql.getParams());
        } else {
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(wrapTable(table));
            JdbcResult whereSql = whereSql(criteria);
            jdbcResult = new JdbcResult(sql.append(whereSql.getSql()).toString(), whereSql.getParams());

            //添加分页参数
            if (pageRequest != null && pageRequest.getPage() != null) {
                String pageSql = pageSql(pageRequest);
                jdbcResult.setSql(jdbcResult.getSql() + pageSql);
            }
        }

        return jdbcResult;
    }

    public static JdbcResult getSelectForCount(String table, Criteria criterion) {
        List<Criteria> criteria = criterion == null ? Collections.emptyList() : Arrays.asList(criterion);
        return getSelectForCount(table, criteria);
    }

    public static JdbcResult getSelectForCount(String table, List<Criteria> criteria) {
        Preconditions.checkArgument(StringUtils.isNotBlank(table), "表名不能为空");
        StringBuilder sql = new StringBuilder("SELECT count(*) FROM ").append(wrapTable(table));
        JdbcResult whereSql = whereSql(criteria);
        return new JdbcResult(sql.append(whereSql.getSql()).toString(), whereSql.getParams());
    }

    public static JdbcResult getSelectForSum(String table, Criteria criterion, String... dbColumns) {
        List<Criteria> criteria = criterion == null ? Collections.emptyList() : Arrays.asList(criterion);
        return getSelectForSum(table, criteria, dbColumns);
    }

    /**
     * @param dbColumns 需要进行sum操作的数据库列名
     */
    public static JdbcResult getSelectForSum(String table, List<Criteria> criteria, String... dbColumns) {
        Preconditions.checkArgument(StringUtils.isNotBlank(table), "表名不能为空");
        Preconditions.checkArgument(dbColumns != null && dbColumns.length > 0, "表[" + table + "]进行sum运算的列名不能为空");
        StringBuilder sql = new StringBuilder("SELECT ");
        IntStream.range(0, dbColumns.length).forEach(index -> {
            if (index != 0) {
                sql.append(",");
            }
            sql.append("sum(").append(wrapCol(dbColumns[index])).append(")");
        });
        sql.append(" FROM ").append(wrapTable(table));

        JdbcResult whereSql = whereSql(criteria);
        return new JdbcResult(sql.append(whereSql.getSql()).toString(), whereSql.getParams());
    }

    public static JdbcResult getPatch(String table, Object entity, Map<String, String> dbMapping, String... dbColumns) {
        Preconditions.checkArgument(dbColumns != null && dbColumns.length > 0, "更新数据时必须指定dbColumns条件");
        return getPatch(table, entity, dbMapping, null, Arrays.asList(dbColumns));
    }

    public static JdbcResult getPatch(String table, Object entity, Map<String, String> dbMapping,
                                      Map<String, UpdateOper> updateOperations, String... dbColumns) {
        Preconditions.checkArgument(dbColumns != null && dbColumns.length > 0, "更新数据时必须指定dbColumns条件");
        return getPatch(table, entity, dbMapping, updateOperations, Arrays.asList(dbColumns));
    }

    public static JdbcResult getPatch(String table, Object entity, Map<String, String> dbMapping, Criteria criterion) {
        Preconditions.checkArgument(criterion != null, "更新数据时criteria不能为空");
        return getPatch(table, entity, dbMapping, null, Arrays.asList(criterion));
    }

    public static JdbcResult getPatch(String table, Object entity, Map<String, String> dbMapping,
                                      Map<String, UpdateOper> updateOperations, Criteria criterion) {
        Preconditions.checkArgument(criterion != null, "更新数据时criteria不能为空");
        return getPatch(table, entity, dbMapping, updateOperations, Arrays.asList(criterion));
    }

    public static JdbcResult getPatch(String table, Object entity, Map<String, String> dbMapping, List<? extends Object> criteria) {
        return getPatch(table, entity, dbMapping, null, criteria);
    }

    /**
     * 只更新entity中不为null的属性
     */
    public static JdbcResult getPatch(String table, Object entity, Map<String, String> dbMapping, Map<String, UpdateOper> updateOperations, List<? extends Object> criteria) {
        Preconditions.checkArgument(StringUtils.isNotBlank(table), "表名不能为空");
        Preconditions.checkArgument(entity != null, "数据库插入数据时entity对象不能为空");
        Preconditions.checkArgument(dbMapping != null && dbMapping.size() > 0, "数据库字段名和实体属性名的对应关系不能为空");
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(criteria), "更新数据时criteria不能为空");

        List<String> updateFields = updateFields(entity.getClass(), dbMapping);
        Map<String, Object> fieldValueMap = fieldValues(entity, updateFields);
        StringBuilder sql = new StringBuilder("UPDATE ").append(wrapTable(table)).append(" SET ");
        List<String> fieldNames = new ArrayList<>(fieldValueMap.keySet());

        //col1=?,col2=?,..
        IntStream.range(0, fieldNames.size()).forEach(index -> {
            if (index != 0) {
                sql.append(",");
            }
            sql.append(colUpdateSql(dbMapping.get(fieldNames.get(index)), updateOperations));
        });

        //where sql
        Map<String, Object> entityMap = BaseBeanUtils.beanToMapNonNull(entity);
        Map<String, Object> colValueMap = entityMap.entrySet().stream().filter(entry -> dbMapping.containsKey(entry.getKey()))
                .collect(Collectors.toMap(entry -> dbMapping.get(entry.getKey()), entry -> entry.getValue()));
        JdbcResult whereSql = whereSql(criteria, colValueMap);
        sql.append(whereSql.getSql());

        //组装params
        List<Object> params = new ArrayList<>();
        IntStream.range(0, fieldNames.size()).forEach(index ->
                params.add(colUpdateValue(dbMapping.get(fieldNames.get(index)), fieldNames.get(index), fieldValueMap, updateOperations))
        );
        IntStream.range(0, whereSql.getParams().length).forEach(index ->
                params.add(whereSql.getParams()[index])
        );

        return new JdbcResult(sql.toString(), params.toArray());
    }

    public static JdbcResult getUpdate(String table, Object entity, Map<String, String> dbMapping, Criteria criterion) {
        Preconditions.checkArgument(criterion != null, "更新数据时criteria不能为空");
        return getUpdate(table, entity, dbMapping, Arrays.asList(criterion));
    }

    public static JdbcResult getUpdate(String table, Object entity, Map<String, String> dbMapping,
                                       Map<String, UpdateOper> updateOperations, Criteria criterion) {
        Preconditions.checkArgument(criterion != null, "更新数据时criteria不能为空");
        return getUpdate(table, entity, dbMapping, updateOperations, Arrays.asList(criterion));
    }

    public static JdbcResult getUpdate(String table, Object entity, Map<String, String> dbMapping, String... dbColumns) {
        Preconditions.checkArgument(dbColumns != null && dbColumns.length > 0, "更新数据时必须指定dbColumns条件");
        return getUpdate(table, entity, dbMapping, Arrays.asList(dbColumns));
    }

    public static JdbcResult getUpdate(String table, Object entity, Map<String, String> dbMapping,
                                       Map<String, UpdateOper> updateOperations, String... dbColumns) {
        Preconditions.checkArgument(dbColumns != null && dbColumns.length > 0, "更新数据时必须指定dbColumns条件");
        return getUpdate(table, entity, dbMapping, updateOperations, Arrays.asList(dbColumns));
    }

    public static JdbcResult getUpdate(String table, Object entity, Map<String, String> dbMapping, List<? extends Object> criteria) {
        return getBatchUpdate(table, Arrays.asList(entity), dbMapping, null, criteria);
    }

    public static JdbcResult getUpdate(String table, Object entity, Map<String, String> dbMapping,
                                       Map<String, UpdateOper> updateOperations, List<? extends Object> criteria) {
        return getBatchUpdate(table, Arrays.asList(entity), dbMapping, updateOperations, criteria);
    }

    public static JdbcResult getBatchUpdate(String table, List<? extends Object> entities, Map<String, String> dbMapping, String... dbColumns) {
        Preconditions.checkArgument(dbColumns != null && dbColumns.length > 0, "更新数据时必须指定dbColumn条件");
        return getBatchUpdate(table, entities, dbMapping, null, Arrays.asList(dbColumns));
    }

    public static JdbcResult getBatchUpdate(String table, List<? extends Object> entities, Map<String, String> dbMapping,
                                            Map<String, UpdateOper> updateOperations, String... dbColumns) {
        Preconditions.checkArgument(dbColumns != null && dbColumns.length > 0, "更新数据时必须指定dbColumn条件");
        return getBatchUpdate(table, entities, dbMapping, updateOperations, Arrays.asList(dbColumns));
    }

    public static JdbcResult getBatchUpdate(String table, List<? extends Object> entities, Map<String, String> dbMapping,
                                            List<? extends Object> criteria) {
        return getBatchUpdate(table, entities, dbMapping, null, criteria);
    }

    /**
     * @param criteria 如果Object类型为String, 则表示实体类字段; 如果为Criteria表示数据库字段
     */
    public static JdbcResult getBatchUpdate(String table, List<? extends Object> entities, Map<String, String> dbMapping,
                                            Map<String, UpdateOper> updateOperations, List<? extends Object> criteria) {
        Preconditions.checkArgument(StringUtils.isNotBlank(table), "表名不能为空");
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(entities), "数据库插入数据时entity对象不能为空");
        Preconditions.checkArgument(dbMapping != null && dbMapping.size() > 0, "数据库字段名和实体属性名的对应关系不能为空");

        StringBuilder sql = new StringBuilder("UPDATE ").append(wrapTable(table)).append(" SET ");
        List<String> fieldNames = updateFields(entities.get(0).getClass(), dbMapping);

        //set col1=?,col2=?,..
        IntStream.range(0, fieldNames.size()).forEach(index -> {
            if (index != 0) {
                sql.append(",");
            }

            sql.append(colUpdateSql(dbMapping.get(fieldNames.get(index)), updateOperations));
        });

        //where sql
        sql.append(" WHERE ");
        IntStream.range(0, criteria.size()).forEach(index -> {
            Object column = criteria.get(index);
            if (index != 0) {
                if (column instanceof Criteria && ((Criteria) column).isOr()) {
                    sql.append(" OR ");
                } else {
                    sql.append(" AND ");
                }
            }

            if (column instanceof String) {
                sql.append(wrapCol((String) column)).append("=?");
            } else {
                Criteria criterion = (Criteria) column;
                sql.append(whereSql(criterion));
            }
        });

        //组装params
        List<Object[]> paramsList = new ArrayList<>();
        entities.forEach(entity -> {
            List<Object> params = new ArrayList();

            Map<String, Object> entityMap = BaseBeanUtils.beanToMap(entity);
            IntStream.range(0, fieldNames.size()).forEach(index -> {
                String fieldName = fieldNames.get(index);
                params.add(colUpdateValue(dbMapping.get(fieldName), fieldName, entityMap, updateOperations));
            });

            //where params
            IntStream.range(0, criteria.size()).forEach(index -> {
                Object column = criteria.get(index);
                if (column instanceof String) {
                    Map.Entry ele = dbMapping.entrySet().stream().filter(entry -> entry.getValue().equalsIgnoreCase((String) column))
                            .findAny().orElse(null);
                    String field = (String) ele.getKey();
                    params.add(entityMap.get(field));
                } else if (column instanceof Criteria) {
                    params.addAll(getParams((Criteria) column));
                }
            });

            paramsList.add(params.toArray());
        });

        return new JdbcResult(sql.toString(), paramsList);
    }

    public static JdbcResult getDelete(String table, Criteria criterion) {
        Preconditions.checkArgument(criterion != null, "数据库删除必须要指定条件");
        return getDelete(table, Arrays.asList(criterion));
    }

    public static JdbcResult getDelete(String table, List<Criteria> criteria) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(criteria), "数据库删除必须要指定条件");
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(wrapTable(table));
        JdbcResult whereSql = whereSql(criteria);
        return new JdbcResult(sql.append(whereSql.getSql()).toString(), whereSql.getParams());
    }

    public static <T> T dbRowToPo(Map<String, Object> dbRow, Map<String, String> dbMapping, Class<T> poClass) {
        return dbRowToEntity(dbRow, dbMapping, poClass);
    }

    @Deprecated
    public static <T> T dbRowToEntity(Map<String, Object> dbRow, Map<String, String> dbMapping, Class<T> entityClass) {
        if (dbRow == null || dbRow.size() == 0) {
            return null;
        }

        Object entity = BaseBeanUtils.newInstance(entityClass);
        if (entity == null) {
            return null;
        }

        Arrays.stream(entityClass.getDeclaredFields()).forEach(f -> {
            try {
                f.setAccessible(true);
                String col = dbMapping.get(f.getName());
                if (col != null) {
                    Object value = dbRow.get(col);
                    if (value != null) {
                        if (value instanceof java.sql.Date || value instanceof java.sql.Timestamp || value instanceof java.sql.Time) {
                            value = new Date(((Date) value).getTime());
                        }
                        if (f.getType() != String.class && value.getClass() == String.class) {
                            PropertyEditor editor = PropertyEditorManager.findEditor(f.getType());
                            editor.setAsText(value.toString());
                            value = editor.getValue();
                        }

                        if(dbType == DbType.ORACLE) {
                            if(f.getType() == Integer.class) {
                                value = ((BigDecimal)value).intValue();
                            } else if(f.getType() == Long.class) {
                                value = ((BigDecimal)value).longValue();
                            }
                        }

                        f.set(entity, value);
                    }
                } else {
                    log.warn("类'{}'中属性'{}'没有对应的数据库字段", entityClass.getSimpleName(), f.getName());
                }
            } catch (Throwable e) {
                log.error("用数据库返回的数据设置类[{}]字段[{}]异常", entityClass.getSimpleName(), f.getName(), ExceptionUtils.getStackTrace(e));
            }
        });

        return (T) entity;
    }

    private static JdbcResult insertSql(String table, List<? extends Object> entities, Map<String, String> dbMapping, boolean ignore) {
        Preconditions.checkArgument(StringUtils.isNotBlank(table), "表名不能为空");
        Preconditions.checkArgument(entities != null && entities.size() > 0, "数据库插入数据时entity对象不能为空");
        Preconditions.checkArgument(dbMapping != null && dbMapping.size() > 0, "数据库字段名和实体属性名的对应关系不能为空");

        StringBuilder sql = new StringBuilder("INSERT ");
        if (ignore && dbType == DbType.MYSQL) {
            sql.append("IGNORE INTO ").append(wrapTable(table));
        } else {
            sql.append("INTO ").append(wrapTable(table));
        }

        //(col1,col2,...)
        List<String> fieldNames = new ArrayList<>(dbMapping.keySet());
        sql.append(" (");
        IntStream.range(0, dbMapping.size()).forEach(index -> {
            if (index != 0) {
                sql.append(",");
            }
            sql.append(wrapCol(dbMapping.get(fieldNames.get(index))));
        });

        //values(?,?,..)
        sql.append(") VALUES (");
        IntStream.range(0, dbMapping.size()).forEach(index -> {
            if (index != 0) {
                sql.append(",");
            }
            sql.append("?");
        });
        sql.append(")");

        List<Object[]> paramsList = new ArrayList<>();
        entities.forEach(entity -> {
            Object[] params = new Object[dbMapping.size()];
            paramsList.add(params);

            Map<String, Object> entityMap = BaseBeanUtils.beanToMap(entity);
            IntStream.range(0, dbMapping.size()).forEach(index ->
                    params[index] = entityMap.get(fieldNames.get(index))
            );
        });

        return new JdbcResult(sql.toString(), paramsList);
    }

    private static JdbcResult whereSql(List<? extends Object> criteria) {
        return whereSql(criteria, null);
    }

    private static JdbcResult whereSql(List<? extends Object> criteria, Map<String, Object> colValues) {
        if (CollectionUtils.isEmpty(criteria)) {
            return new JdbcResult("", new Object[]{});
        }

        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(" WHERE ");
        IntStream.range(0, criteria.size()).forEach(index -> {
            Object criterion = criteria.get(index);
            if (index != 0) {
                if (criterion instanceof Criteria && ((Criteria) criterion).isOr()) {
                    sql.append(" OR ");
                } else {
                    sql.append(" AND ");
                }
            }

            if (criterion instanceof String) {
                sql.append(wrapCol((String) criterion)).append("=?");
                params.add(colValues.get(criterion));
            } else {
                Criteria criterion1 = (Criteria) criterion;
                sql.append(whereSql((criterion1)));
                params.addAll(getParams(criterion1));
            }
        });

        return new JdbcResult(sql.toString(), params.toArray());
    }

    private static String whereSql(Criteria criterion) {
        StringBuilder sql = new StringBuilder("");
        if (criterion instanceof CombinedCriteria) {
            sql.append("(");
            IntStream.range(0, ((CombinedCriteria) criterion).getCriteria().size()).forEach(index -> {
                Criteria crtCriterion = ((CombinedCriteria) criterion).getCriteria().get(index);
                if (index != 0) {
                    if (crtCriterion.isOr()) {
                        sql.append(" OR ");
                    } else {
                        sql.append(" AND ");
                    }
                }
                sql.append(whereSql(crtCriterion));
            });
            sql.append(")");
        } else {
            if (criterion.getType() == Criteria.Type.EQ) {
                sql.append(wrapCol(criterion.getColName())).append("=?");
            } else if (criterion.getType() == Criteria.Type.NE) {
                sql.append(wrapCol(criterion.getColName())).append("!=?");
            } else if (criterion.getType() == Criteria.Type.GT) {
                sql.append(wrapCol(criterion.getColName())).append(">?");
            } else if (criterion.getType() == Criteria.Type.GE) {
                sql.append(wrapCol(criterion.getColName())).append(">=?");
            } else if (criterion.getType() == Criteria.Type.LT) {
                sql.append(wrapCol(criterion.getColName())).append("<?");
            } else if (criterion.getType() == Criteria.Type.LE) {
                sql.append(wrapCol(criterion.getColName())).append("<=?");
            } else if (criterion.getType() == Criteria.Type.IN || criterion.getType() == Criteria.Type.NIN) {
                char[] temp = new char[((List) criterion.getValue()).size()];
                Arrays.fill(temp, '?');
                String innerSql = StringUtils.join(temp, ',');
                if (criterion.getType() == Criteria.Type.IN) {
                    sql.append(wrapCol(criterion.getColName())).append(" IN(").append(innerSql).append(")");
                } else {
                    sql.append(wrapCol(criterion.getColName())).append(" NOT IN(").append(innerSql).append(")");
                }
            } else if (criterion.getType() == Criteria.Type.LIKE) {
                sql.append(wrapCol(criterion.getColName())).append(" LIKE ?");
            } else if (criterion.getType() == Criteria.Type.IS_NULL) {
                sql.append("ISNULL(").append(wrapCol(criterion.getColName())).append(")");
            } else if (criterion.getType() == Criteria.Type.IS_NOT_NULL) {
                sql.append(wrapCol(criterion.getColName())).append(" IS NOT NULL");
            }
        }
        return sql.toString();
    }

    private static String pageSql(PageRequest pageRequest) {
        if (pageRequest == null) {
            return "";
        }
        Integer page = pageRequest.getPage();
        Integer pageSize = pageRequest.getPageSize();
        String pageSql = "";
        if (StringUtils.isNotBlank(pageRequest.getOrderedCol())) {
            pageSql = " ORDER BY " + wrapCol(pageRequest.getOrderedCol()) + " " + pageRequest.getOrder().name();
        }
        return pageSql + " LIMIT " + (page - 1) * pageSize + "," + pageSize;
    }

    private static List<String> updateFields(Class entityCls, Map<String, String> dbMapping) {
        //获取需要更新的列
        List<String> updateFields = new ArrayList<>();
        if (updateFieldsMap.containsKey(entityCls)) {
            updateFields = updateFieldsMap.get(entityCls);
        } else {
            for (String fieldName : dbMapping.keySet()) {
                try {
                    Field field = entityCls.getDeclaredField(fieldName);
                    if (field.getAnnotation(PrimaryKey.class) == null &&
                            //   field.getAnnotation(UniqueIndex.class) == null &&
                            field.getAnnotation(UpdateIgnore.class) == null) {
                        updateFields.add(fieldName);
                    }
                } catch (Throwable e) {
                    log.error("dbMapping中存在无法识别的entity属性{}", fieldName);
                }
            }
            updateFieldsMap.put(entityCls, updateFields);
        }
        return updateFields;
    }

    private static Map<String, Object> fieldValues(Object entity, List<String> fields) {
        Map<String, Object> entityMap = BaseBeanUtils.beanToMapNonNull(entity);
        Map<String, Object> fieldValues = new HashMap<>();
        entityMap.entrySet().forEach(entry -> {
            if (fields.contains(entry.getKey())) {
                fieldValues.put(entry.getKey(), entry.getValue());
            }
        });
        return fieldValues;
    }

    private static List<Object> getParams(Criteria criteria) {
        List<Object> params = new ArrayList<>();
        if (criteria instanceof CombinedCriteria) {
            ((CombinedCriteria) criteria).getCriteria().forEach(criterion -> params.addAll(getParams(criterion)));
        } else {
            if (criteria.getType() == Criteria.Type.IN || criteria.getType() == Criteria.Type.NIN) {
                params.addAll((List) criteria.getValue());
            } else if (criteria.getType() != Criteria.Type.IS_NULL && criteria.getType() != Criteria.Type.IS_NOT_NULL) {
                params.add(criteria.getValue());
            }
        }
        return params;
    }

    private static String sql(CombinedCriteria combinedCriteria) {
        if (combinedCriteria == null || BaseCollectionUtils.isEmpty(combinedCriteria.getCriteria())) {
            return "";
        }

        StringBuilder buf = new StringBuilder();
        IntStream.range(0, combinedCriteria.getCriteria().size()).forEach(index -> {
            Object criterion = combinedCriteria.getCriteria().get(index);
            if (index != 0) {
                if (criterion instanceof Criteria && ((Criteria) criterion).isOr()) {
                    buf.append(" OR ");
                } else {
                    buf.append(" AND ");
                }
            }
        });

        return buf.toString();
    }

    private static String colUpdateSql(String colName, Map<String, UpdateOper> updateOperations) {
        String sql = wrapCol(colName);
        if (updateOperations == null || !updateOperations.containsKey(colName)) {
            sql += "=?";
        } else {
            sql += "=" + wrapCol(colName);
            UpdateOper updateOper = updateOperations.get(colName);
            switch (updateOper.getType()) {
                case ADD:
                    sql += "+?";
                    break;
                case SUBTRACT:
                    sql += "-?";
                    break;
                case MULTIPLY:
                    sql += "*?";
                    break;
                case DIVIDE:
                    sql += "/?";
                    break;
                default:
                    log.error("不支持的UpdateOper.Type类型");
            }
        }
        return sql;
    }

    private static Object colUpdateValue(String colName, String fieldName, Map<String, Object> entityMap, Map<String, UpdateOper> updateOperations) {
        if (updateOperations == null || !updateOperations.containsKey(colName)) {
            return entityMap.get(fieldName);
        } else {
            return updateOperations.get(colName).getValue();
        }
    }

    private static String wrapCol(String colName) {
        if (dbType == DbType.MYSQL && !colName.startsWith("`")) {
            return "`" + colName + "`";
        } else if (dbType == DbType.ORACLE && !colName.startsWith("\"")) {
            return "\"" + colName + "\"";
        } else {
            return colName;
        }
    }

    private static String wrapTable(String table) {
        if(dbType == DbType.ORACLE) {
            return "\"" + table + "\"";
        }
        return table;
    }

    public enum DbType {
        MYSQL,
        ORACLE
    }
}

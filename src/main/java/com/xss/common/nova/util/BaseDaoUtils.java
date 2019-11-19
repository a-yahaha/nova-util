package com.xss.common.nova.util;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.xss.common.nova.model.DbInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

@Slf4j
public class BaseDaoUtils {
    private static final String NAME = "COLUMN_NAME";
    private static final String TYPE = "TYPE_NAME";
    private static final String REMARKS = "REMARKS";
    private static final Map<String, String> typeMap = new HashMap<>();
    private static String basePath;
    private static Map<String, List<String>> primaryKeysMap = new HashMap<>();
    private static Map<String, List<String>> uniqueIndexMap = new HashMap<>();
    private static Map<String, List<String>> indexMap = new HashMap<>();
    private static List<String> UPDATE_IGNORE_COLS = Arrays.asList(
            "create_time", "create_at", "created_at", "createTime", "createAt", "createdAt",
            "create_by", "created_by", "createBy", "createdBy");
    //数据库关键字desc、range、match、delayed
    private static List<String> SQL_KEY_WORDS = Arrays.asList("");

    static {
        URL baseUrl = BaseDaoUtils.class.getClassLoader().getResource(".");
        try {
            basePath = URLDecoder.decode(baseUrl.getPath(), "UTF-8");
            basePath = StringUtils.substringBefore(basePath, "/target");
        } catch (Exception e) {
            log.error("获取项目路径异常", e);
        }

        typeMap.put("CHAR", "String");
        typeMap.put("VARCHAR", "String");
        typeMap.put("BLOB", "String");
        typeMap.put("MEDIUMBLOB", "String");
        typeMap.put("LONGBLOB", "String");
        typeMap.put("TEXT", "String");
        typeMap.put("TINYTEXT", "String");
        typeMap.put("MEDIUMTEXT", "String");
        typeMap.put("LONGTEXT", "String");
        typeMap.put("ENUM", "String");
        typeMap.put("SET", "String");
        typeMap.put("FLOAT", "BigDecimal");
        typeMap.put("REAL", "BigDecimal");
        typeMap.put("DOUBLE", "BigDecimal");
        typeMap.put("NUMERIC", "BigDecimal");
        typeMap.put("DECIMAL", "BigDecimal");
        typeMap.put("BOOLEAN", "Boolean");
        typeMap.put("BIT", "Boolean");
        typeMap.put("TINYINT", "Integer");
        typeMap.put("SMALLINT", "Integer");
        typeMap.put("MEDIUMINT", "Integer");
        typeMap.put("INT", "Integer");
        typeMap.put("INTEGER", "Integer");
        typeMap.put("INT UNSIGNED", "Integer");
        typeMap.put("BIGINT", "Long");
        typeMap.put("DATE", "Date");
        typeMap.put("TIME", "Date");
        typeMap.put("DATETIME", "Date");
        typeMap.put("TIMESTAMP", "Date");
    }

    public static void generateCode(DbInfo dbInfo, List<String> tables, String basePackage) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotBlank(dbInfo.getUrl()), "数据库url不能为空");
        Preconditions.checkArgument(StringUtils.isNotBlank(dbInfo.getUser()), "数据库用户名不能为空");
        Preconditions.checkArgument(StringUtils.isNotBlank(basePackage), "basePackage不能为空");
        generateCode(dbInfo, tables, basePackage, false);
    }

    public static void generateCode(DbInfo dbInfo, List<String> tables, String basePackage, boolean override) throws Exception {
        DataSource ds = dataSource(dbInfo);
        Connection conn = null;
        try {
            conn = ds.getConnection();
            DatabaseMetaData meta = conn.getMetaData();

            //生成 META-INF/spring.factories文件
            generateCode4SpringFactoryFile(basePackage);

            //生成DaoConfig.java
            String schema = StringUtils.substringAfterLast(dbInfo.getUrl(), "/");
            schema = StringUtils.substringBefore(schema, "?");
            schema = BaseStringUtils.underScoreToCamel(schema, false);
            generateCode4Config(schema, basePackage);

            //生成DaoAspect.java
            generateCode4Aspect(basePackage);

            //如果没有指定tables,将遍历当前schema下所有表
            if (BaseCollectionUtils.isEmpty(tables)) {
                tables = new ArrayList<>();
                ResultSet rs = meta.getTables(null, null, "%", null);
                if (rs != null) {
                    while (rs.next()) {
                        tables.add(rs.getString(3));
                    }
                } else {
                    log.error("当前数据库schema下未发现任务表存在");
                }
            }

            String finalSchema = schema;
            //生成dao及entity文件
            tables.forEach(table -> generateCode4Table(finalSchema, meta, table, basePackage, override));
        } catch (Throwable e) {
            log.error("dao代码生成异常", e);
        } finally {
            try {
                conn.close();
            } catch (Throwable ex) {
            }
        }
    }

    private static void generateCode4SpringFactoryFile(String basePackage) throws Exception {
        File springFactoryFile = ensureFile(Type.factory, null, null, false);
        if (springFactoryFile == null) {
            return;
        }

        StringBuilder buf = new StringBuilder();
        buf.append("org.springframework.boot.autoconfigure.EnableAutoConfiguration=\\\n");
        buf.append("  ").append(basePackage).append(".").append(Type.config.name()).append(".DaoConfig");
        FileUtils.writeByteArrayToFile(springFactoryFile, buf.toString().getBytes("UTF-8"));
    }

    private static void generateCode4Config(String schema, String basePackage) throws Exception {
        File configFile = ensureFile(Type.config, null, basePackage, false);
        String fileContent = Files.asCharSource(configFile, Charsets.UTF_8).read();
        if (fileContent.contains(schema + ".mysql.url")) {
            return;
        }

        StringBuilder buf = new StringBuilder();
        if (fileContent.length() > 0) {
            for (String line : FileUtils.readLines(configFile, "UTF-8")) {
                if (line.contains("@ConditionalOnProperty")) {
                    int index = line.lastIndexOf("\"");
                    buf.append(line.substring(0, index + 1)).append(", \"").append(schema).append(".mysql.url\"")
                            .append(line.substring(index + 1)).append("\n");
                } else if (line.contains("@PostConstruct")) {
                    buf.deleteCharAt(buf.lastIndexOf("\n"));
                    buf.append("    @Value(\"${").append(schema).append(".mysql.url}\")\n");
                    buf.append("    private String ").append(schema).append("Url;\n\n");
                    buf.append(line).append("\n");
                } else if (line.contains("private Map<String, Object> dbProps")) {
                    buf.append(generateCode4ConfigOfOneScheme(schema, false));
                    buf.append(line).append("\n");
                } else {
                    buf.append(line).append("\n");
                }
            }
        } else {
            buf.append("package ").append(basePackage).append(".config;").append("\n\n");
            buf.append("import com.alibaba.druid.pool.DruidDataSourceFactory;\n");
            buf.append("import lombok.extern.slf4j.Slf4j;\n");
            buf.append("import org.apache.commons.lang3.StringUtils;\n");
            buf.append("import org.apache.commons.lang3.exception.ExceptionUtils;\n");
            buf.append("import org.springframework.beans.factory.annotation.Value;\n");
            buf.append("import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;\n");
            buf.append("import org.springframework.context.annotation.Bean;\n");
            buf.append("import org.springframework.context.annotation.ComponentScan;\n");
            buf.append("import org.springframework.context.annotation.Primary;\n");
            buf.append("import org.springframework.context.annotation.Configuration;\n");
            buf.append("import org.springframework.jdbc.core.JdbcTemplate;\n");
            buf.append("import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;\n");
            buf.append("import org.springframework.jdbc.datasource.DataSourceTransactionManager;\n\n");
            buf.append("import javax.annotation.PostConstruct;\n");
            buf.append("import javax.sql.DataSource;\n");
            buf.append("import java.util.HashMap;\n");
            buf.append("import java.util.Map;\n\n");
            buf.append("@Slf4j\n");
            buf.append("@Configuration\n");
            buf.append("@ConditionalOnProperty({\"").append(schema).append(".mysql.url\"})\n");
            buf.append("@ComponentScan(basePackages = {\"").append(basePackage).append(".dao\", \"").append(basePackage).append(".aspect\"})\n");
            buf.append("public class DaoConfig {\n");
            buf.append("    private Map<String, Object> initMap;\n");
            buf.append("    @Value(\"${mysql.user}\")\n");
            buf.append("    private String user;\n");
            buf.append("    @Value(\"${mysql.pass}\")\n");
            buf.append("    private String pass;\n");
            buf.append("    @Value(\"${mysql.max.active:10}\")\n");
            buf.append("    private String maxActive;\n");
            buf.append("    @Value(\"${").append(schema).append(".mysql.url}\")\n");
            buf.append("    private String ").append(schema).append("Url;\n\n");
            buf.append("    @PostConstruct\n");
            buf.append("    public void init() {\n");
            buf.append("        initMap = new HashMap<>(16);\n");
            buf.append("        initMap.put(\"driverClassName\", \"com.mysql.jdbc.Driver\");\n");
            buf.append("        initMap.put(\"initialSize\", \"1\");\n");
            buf.append("        initMap.put(\"minIdle\", \"1\");\n");
            buf.append("        initMap.put(\"maxWait\", \"20000\");\n");
            buf.append("        initMap.put(\"removeAbandoned\", \"true\");\n");
            buf.append("        initMap.put(\"removeAbandonedTimeout\", \"180\");\n");
            buf.append("        initMap.put(\"timeBetweenEvictionRunsMillis\", \"60000\");\n");
            buf.append("        initMap.put(\"minEvictableIdleTimeMillis\", \"300000\");\n");
            buf.append("        initMap.put(\"validationQuery\", \"SELECT 1\");\n");
            buf.append("        initMap.put(\"testWhileIdle\", \"true\");\n");
            buf.append("        initMap.put(\"testOnBorrow\", \"false\");\n");
            buf.append("        initMap.put(\"testOnReturn\", \"false\");\n");
            buf.append("        initMap.put(\"poolPreparedStatements\", \"true\");\n");
            buf.append("        initMap.put(\"maxPoolPreparedStatementPerConnectionSize\", \"50\");\n");
            buf.append("        initMap.put(\"initConnectionSqls\", \"SELECT 1\");\n");
            buf.append("        initMap.put(\"maxActive\", maxActive + \"\");\n");
            buf.append("    }\n\n");
            buf.append(generateCode4ConfigOfOneScheme(schema, true));
            buf.append("    private Map<String, Object> dbProps(String url) {\n");
            buf.append("        Map<String, Object> dbProperties = new HashMap<>(initMap);\n");
            buf.append("        dbProperties.put(\"url\", url);\n");
            buf.append("        dbProperties.put(\"username\", user);\n");
            buf.append("        if (StringUtils.isNotBlank(pass)) {\n");
            buf.append("            dbProperties.put(\"password\", pass);\n");
            buf.append("        }\n");
            buf.append("        return dbProperties;\n");
            buf.append("    }\n");
            buf.append("}");
        }

        FileUtils.writeByteArrayToFile(configFile, buf.toString().getBytes("UTF-8"));
    }

    private static String generateCode4ConfigOfOneScheme(String schema, boolean firstSchema) {
        String schemaFirstUpper = schema.substring(0, 1).toUpperCase() + schema.substring(1);
        StringBuilder buf = new StringBuilder();
        if (firstSchema) {
            buf.append("    @Primary\n");
        }
        buf.append("    @Bean(name = \"ds").append(schemaFirstUpper).append("\")\n");
        buf.append("    public DataSource ds").append(schemaFirstUpper).append("() {\n");
        buf.append("        log.info(\"初始化").append(schema).append("数据源\");\n");
        buf.append("        try {\n");
        buf.append("            return DruidDataSourceFactory.createDataSource(dbProps(").append(schema)
                .append("Url));\n");
        buf.append("        } catch (Exception e) {\n");
        buf.append("            log.error(\"无法获得数据源[{}]:{}\", ").append(schema).append("Url, ExceptionUtils.getStackTrace(e));\n");
        buf.append("            throw new RuntimeException(\"无法获得数据源\");\n");
        buf.append("        }\n");
        buf.append("    }\n\n");
        buf.append("    @Bean(name = \"template").append(schemaFirstUpper).append("\")\n");
        buf.append("    public JdbcTemplate template").append(schemaFirstUpper).append("() {\n");
        buf.append("        return new JdbcTemplate(this.ds").append(schemaFirstUpper).append("());\n");
        buf.append("    }\n\n");
        buf.append("    @Bean(name = \"namedTemplate").append(schemaFirstUpper).append("\")\n");
        buf.append("    public NamedParameterJdbcTemplate namedTemplate").append(schemaFirstUpper).append("() {\n");
        buf.append("        return new NamedParameterJdbcTemplate(this.ds").append(schemaFirstUpper).append("());\n");
        buf.append("    }\n\n");
        buf.append("    @Bean(name = \"tm").append(schemaFirstUpper).append("\")\n");
        buf.append("    public DataSourceTransactionManager ts").append(schemaFirstUpper).append("() {\n");
        buf.append("        return new DataSourceTransactionManager(this.ds").append(schemaFirstUpper).append("());\n");
        buf.append("    }\n\n");
        return buf.toString();
    }

    private static void generateCode4Table(String schema, DatabaseMetaData meta, String table, String basePackage, boolean override) {
        try {
            ResultSet rs = meta.getColumns(null, null, table, null);
            if (rs != null) {
                Map<String, String> nameTypes = new LinkedHashMap<>();
                Map<String, String> remarks = new HashMap<>();
                List errorList = new ArrayList<>();
                while (rs.next()) {
                    String name = rs.getString(NAME);
                    if (SQL_KEY_WORDS.contains(name.toLowerCase())) {
                        errorList.add("表中含数据库关键字:" + name);
                    }
                    nameTypes.put(name, rs.getString(TYPE));
                    remarks.put(name, rs.getString(REMARKS));
                }

                if (BaseCollectionUtils.isNotEmpty(errorList)) {
                    throw new Exception("异常:" + errorList);
                }

                //获取表的主键
                ResultSet primaryKeyRs = meta.getPrimaryKeys(null, null, table);//PK_NAME KEY_SEQ
                String primaryKeyName = null;
                while (primaryKeyRs.next()) {
                    if (primaryKeyName == null) {
                        primaryKeyName = StringUtils.defaultString(primaryKeyRs.getString("PK_NAME"), "primary");
                    }
                    List<String> primaryKeys = primaryKeysMap.get(table);
                    if (primaryKeys == null) {
                        primaryKeys = new ArrayList<>();
                        primaryKeysMap.put(table, primaryKeys);
                    }
                    BaseCollectionUtils.add(primaryKeys, primaryKeyRs.getShort("KEY_SEQ") - 1, primaryKeyRs.getString("COLUMN_NAME"));
                }

                //获取表的其它索引
                ResultSet indexInfoRs = meta.getIndexInfo(null, null, table, false, false);
                String uniqueIndexName = null;
                String otherIndexName = null;
                while (indexInfoRs.next()) {
                    String indexName = indexInfoRs.getString("INDEX_NAME");
                    if (indexName == null || indexName.equals(primaryKeyName)) {
                        continue;
                    }

                    boolean unique = !indexInfoRs.getBoolean("NON_UNIQUE");
                    //获取表的唯一索引(只取一个)
                    List<String> uniqueCols = uniqueIndexMap.get(table);
                    if (unique && (uniqueCols == null || indexName.equalsIgnoreCase(uniqueIndexName))) {
                        if (uniqueCols == null) {
                            uniqueCols = new ArrayList<>();
                            uniqueIndexMap.put(table, uniqueCols);
                        }
                        uniqueIndexName = indexName;
                        BaseCollectionUtils.add(uniqueCols, indexInfoRs.getShort("ORDINAL_POSITION") - 1, indexInfoRs.getString("COLUMN_NAME"));
                    }

                    //获取表的其它索引(只取一个)
                    List<String> indexCols = indexMap.get(table);
                    if (!unique && (indexCols == null || indexName.equalsIgnoreCase(otherIndexName))) {
                        if (indexCols == null) {
                            indexCols = new ArrayList<>(8);
                            indexMap.put(table, indexCols);
                        }
                        otherIndexName = indexName;
                        BaseCollectionUtils.add(indexCols, indexInfoRs.getShort("ORDINAL_POSITION") - 1, indexInfoRs.getString("COLUMN_NAME"));
                    }
                }

                String entityName = entityName(table);
                generateEntityFile(table, entityName, basePackage, nameTypes, remarks, override);
                generateDaoFile(schema, entityName, basePackage, table, nameTypes, override);
            }
        } catch (Throwable e) {
            log.error("为表[{}]生成dao代码时异常: {}", table, ExceptionUtils.getStackTrace(e));
        }
    }

    private static void generateCode4Aspect(String basePackage) throws Exception {
        File aspectFile = ensureFile(Type.aspect, null, basePackage, false);
        if (aspectFile == null) {
            return;
        }

        StringBuilder buf = new StringBuilder();
        buf.append("package ").append(basePackage).append(".aspect;\n\n");

        FileUtils.writeByteArrayToFile(aspectFile, buf.toString().getBytes("UTF-8"));
    }

    private static void generateEntityFile(String table, String entityName, String basePackage, Map<String, String> nameTypes, Map<String, String> remarks, boolean override) throws Exception {
        File file = ensureFile(Type.po, entityName, basePackage, override);
        if (file != null) {
            InputStream in = new ByteArrayInputStream(entityContent(table, basePackage, entityName, nameTypes, remarks).getBytes("UTF-8"));
            FileUtils.copyInputStreamToFile(in, file);
        }
    }

    private static void generateDaoFile(String schema, String entityName, String basePackage, String table, Map<String, String> nameTypes, boolean override) throws Exception {
        File file = ensureFile(Type.dao, entityName, basePackage, override);
        if (file != null) {
            InputStream in = new ByteArrayInputStream(daoContent(schema, basePackage, entityName, table, nameTypes).getBytes("UTF-8"));
            FileUtils.copyInputStreamToFile(in, file);
        }
    }

    private static File ensureFile(Type type, String entityName, String basePackage, boolean override) throws Exception {
        File file;
        if (type == Type.factory) {
            File dir = new File(basePath + "/src/main/resources/META-INF");
            FileUtils.forceMkdir(dir);
            file = new File(dir, "spring.factories");
            if (file.exists()) {
                return null;
            }
        } else {
            File dir = new File(basePath + "/src/main/java/" + basePackage.replace('.', '/') + "/" + type.name());
            FileUtils.forceMkdir(dir);

            if (type == Type.dao || type == Type.po) {
                String suffix = (type == Type.dao) ? "Dao" : "Po";
                file = new File(dir, entityName + suffix + ".java");
                if (!override && file.exists()) {
                    return null;
                }

                if (file.exists()) {
                    FileUtils.forceDelete(file);
                }
            } else if (type == Type.aspect) {
                file = new File(dir, "DaoAspect.java");
                if (file.exists()) {
                    return null;
                }
            } else { //config
                file = new File(dir, "DaoConfig.java");
            }
        }

        FileUtils.touch(file);
        return file;
    }

    private static String entityContent(String table, String basePackage, String entityName, Map<String, String> nameTypes, Map<String, String> remarks) {
        List<String> primaryKeys = primaryKeysMap.get(table);
        List<String> uniqueKeys = uniqueIndexMap.get(table);

        StringBuilder bufHeader = new StringBuilder("package ").append(basePackage).append(".po;").append("\n\n");
        bufHeader.append("import lombok.Data;").append("\n\n");
        AtomicBoolean containAnno = new AtomicBoolean(false);
        AtomicBoolean containBigDecimal = new AtomicBoolean(false);

        StringBuilder buf = new StringBuilder();
        buf.append("import java.util.*;").append("\n\n");
        buf.append("@Data").append("\n");
        buf.append("public class ").append(entityName).append("Po {").append("\n");
        nameTypes.entrySet().forEach(entry -> {
            if (StringUtils.isNotBlank(remarks.get(entry.getKey()))) {
                buf.append("    /**\n");
                buf.append("     * ").append(remarks.get(entry.getKey())).append("\n");
                buf.append("     */\n");
            }

            if (BaseCollectionUtils.contains(primaryKeys, entry.getKey())) {
                buf.append("    @PrimaryKey\n");
                containAnno.set(true);
            } else if (BaseCollectionUtils.contains(uniqueKeys, entry.getKey())) {
                buf.append("    @UniqueIndex\n");
                containAnno.set(true);
            } else if (BaseCollectionUtils.containsIgnoreCase(UPDATE_IGNORE_COLS, entry.getKey())) {
                buf.append("    @UpdateIgnore\n");
                containAnno.set(true);
            }

            String javaType = javaType(entry.getValue());
            buf.append("    private ").append(javaType).append(" ").append(toCamel(entry.getKey(), false)).append(";\n");
            if("BigDecimal".equalsIgnoreCase(javaType)) {
                containBigDecimal.set(true);
            }
        });
        buf.append("}");

        if (containAnno.get()) {
            bufHeader.append("import com.xss.common.nova.annotation.*;\n");
        }
        if(containBigDecimal.get()) {
            bufHeader.append("import java.math.BigDecimal;\n");
        }
        return bufHeader.toString() + buf.toString();
    }

    private static String daoContent(String schema, String basePackage, String entityName, String table, Map<String, String> nameTypes) {
        String schemaFirstUpper = schema.substring(0, 1).toUpperCase() + schema.substring(1);
        String entityCamelName = toCamel(entityName, false);

        StringBuilder buf = new StringBuilder("package ").append(basePackage).append(".dao;").append("\n\n");
        buf.append("import com.xss.common.nova.util.BaseJdbcUtils;").append("\n");
        buf.append("import com.xss.common.nova.model.*;").append("\n");
        buf.append("import " + basePackage + ".po.").append(entityName).append("Po;").append("\n");
        buf.append("import org.springframework.dao.EmptyResultDataAccessException;").append("\n");
        buf.append("import org.springframework.jdbc.core.JdbcTemplate;").append("\n");
        buf.append("import org.springframework.stereotype.Repository;").append("\n\n");
        buf.append("import javax.annotation.PostConstruct;").append("\n");
        buf.append("import javax.annotation.Resource;").append("\n");
        buf.append("import java.util.*;").append("\n");
        buf.append("import java.util.stream.Collectors;").append("\n");
        buf.append("import java.util.stream.IntStream;").append("\n\n");

        buf.append("@Repository").append("\n");
        buf.append("public class ").append(entityName).append("Dao {").append("\n");
        buf.append("    private final static String TABLE_NAME = \"").append(table).append("\";\n");
        buf.append("    private Map<String, String> dbMapping = new HashMap<>();").append("\n");
        buf.append("    @Resource(name = \"template").append(schemaFirstUpper).append("\")").append("\n");
        buf.append("    private JdbcTemplate template;").append("\n\n");

        //init
        Map<String, String> dbMapping = new HashMap<>();
        buf.append("    @PostConstruct").append("\n");
        buf.append("    public void init() {").append("\n");
        nameTypes.entrySet().forEach(entry -> {
            buf.append("        dbMapping.put(\"").append(toCamel(entry.getKey(), false)).append("\", \"").append(entry.getKey()).append("\");\n");
            dbMapping.put(toCamel(entry.getKey(), false), entry.getKey());
        });
        buf.append("    }\n\n");

        //insert
        buf.append("    public boolean insert(").append(entityName).append("Po ").append(ensure(entityCamelName)).append(") {\n");
        buf.append("        JdbcResult jdbcResult = BaseJdbcUtils.getInsert(getTable(), ").append(ensure(entityCamelName)).append(", dbMapping);\n");
        buf.append("        return template.update(jdbcResult.getSql(), jdbcResult.getParams()) == 1;\n");
        buf.append("    }\n\n");

        if (primaryKeysMap.containsKey(table)) {
            //insert ignore
            buf.append("    public boolean insertIgnore(").append(entityName).append("Po ").append(ensure(entityCamelName)).append(") {\n");
            buf.append("        JdbcResult jdbcResult = BaseJdbcUtils.getInsertIgnore(getTable(), ").append(ensure(entityCamelName)).append(", dbMapping);\n");
            buf.append("        return template.update(jdbcResult.getSql(), jdbcResult.getParams()) == 1;\n");
            buf.append("    }\n\n");

            //insertOrUpdate
            buf.append("    /**\n");
            buf.append("     * @return true when insert\n");
            buf.append("     */\n");
            buf.append("    public boolean insertOrUpdate(").append(entityName).append("Po ").append(ensure(entityCamelName)).append(") {\n");
            buf.append("        JdbcResult jdbcResult = BaseJdbcUtils.getInsertOrUpdate(getTable(), ").append(ensure(entityCamelName)).append(", dbMapping);\n");
            buf.append("        return template.update(jdbcResult.getSql(), jdbcResult.getParams()) == 1;\n");
            buf.append("    }\n\n");
        }

        //batch insert
        buf.append("    public int batchInsert(List<").append(entityName).append("Po> ").append(entityCamelName).append("s) {\n");
        buf.append("        JdbcResult jdbcResult = BaseJdbcUtils.getBatchInsert(getTable(), ").append(entityCamelName).append("s, dbMapping);\n");
        buf.append("        return IntStream.of(template.batchUpdate(jdbcResult.getSql(), jdbcResult.getBatchParams())).sum();\n");
        buf.append("    }\n\n");

        //update
        buf.append("    public boolean update(").append(entityName).append("Po ").append(ensure(entityCamelName)).append(") {\n");
        buf.append("        JdbcResult jdbcResult = BaseJdbcUtils.getUpdate(getTable(), ").append(ensure(entityCamelName)).append(", dbMapping, ")
                .append(primaryKeySimpleArgs(table, nameTypes)).append(");\n");
        buf.append("        return template.update(jdbcResult.getSql(), jdbcResult.getParams()) == 1;\n");
        buf.append("    }\n\n");

        //patch
        buf.append("    public boolean patch(").append(entityName).append("Po ").append(ensure(entityCamelName)).append(") {\n");
        buf.append("        JdbcResult jdbcResult = BaseJdbcUtils.getPatch(getTable(), ").append(ensure(entityCamelName)).append(", dbMapping, ")
                .append(primaryKeySimpleArgs(table, nameTypes)).append(");\n");
        buf.append("        return template.update(jdbcResult.getSql(), jdbcResult.getParams()) == 1;\n");
        buf.append("    }\n\n");

        //get by primary key or unique key
        if (BaseCollectionUtils.isNotEmpty(primaryKeysMap.get(table))) {
            buf.append(getByCols(primaryKeysMap.get(table), nameTypes, entityName, true));
        }

        if (BaseCollectionUtils.isNotEmpty(uniqueIndexMap.get(table))) {
            buf.append(getByCols(uniqueIndexMap.get(table), nameTypes, entityName, false));
        }


        //get list by index or one of unique key
        if (indexMap.containsKey(table) ||
                (primaryKeysMap.containsKey(table) && primaryKeysMap.get(table).size() > 1) ||
                (uniqueIndexMap.containsKey(table) && uniqueIndexMap.get(table).size() > 1)) {
            //list
            List<String> cols = indexMap.get(table);
            if (cols == null && (primaryKeysMap.containsKey(table) && primaryKeysMap.get(table).size() > 1)) {
                cols = Arrays.asList(primaryKeysMap.get(table).get(0));
            } else if (cols == null) {
                cols = Arrays.asList(uniqueIndexMap.get(table).get(0));
            }
            buf.append("    public List<").append(entityName).append("Po> list(").append(queryArgs(cols, nameTypes)).append(") {\n");
            buf.append("        JdbcResult jdbcResult = BaseJdbcUtils.getSelect(getTable(), ");
            buf.append(criteriaArgs(16, cols)).append(");\n");
            buf.append("        return template.queryForList(jdbcResult.getSql(), jdbcResult.getParams()).stream()\n");
            buf.append("                ").append(".map(dbRow -> BaseJdbcUtils.dbRowToPo(dbRow, dbMapping, ").append(entityName).append("Po.class))\n");
            buf.append("                ").append(".collect(Collectors.toList());\n");
            buf.append("    }\n\n");
        }

        if (primaryKeysMap.containsKey(table)) {
            //getOrInsert
            String argsName = ensure(entityCamelName);
            String getArgs = getArgs(argsName, uniqueIndexMap.containsKey(table) ? uniqueIndexMap.get(table) : primaryKeysMap.get(table), dbMapping);
            buf.append("    public ").append(entityName).append("Po getOrInsert(").append(entityName).append("Po ").append(argsName).append(") {\n");
            buf.append("        ").append(entityName).append("Po po = this.get").append(uniqueIndexMap.containsKey(table) ? "ByIndex" : "").append("(").append(getArgs).append(");\n");
            buf.append("        if (po == null) {\n");
            buf.append("            if (!this.insertIgnore(").append(argsName).append(")) {\n");
            buf.append("                return this.get").append(uniqueIndexMap.containsKey(table) ? "ByIndex" : "").append("(").append(getArgs).append(");\n");
            buf.append("            }\n");
            buf.append("            return ").append(argsName).append(";\n");
            buf.append("        }\n");
            buf.append("        return po;\n");
            buf.append("    }\n\n");

            //getAfterPut
            if (uniqueIndexMap.containsKey(table)) {
                buf.append("    public ").append(entityName).append("Po getAfterPut(").append(entityName).append("Po ").append(argsName).append(") {\n");
                buf.append("        if (this.insertOrUpdate(").append(argsName).append(")) {\n");
                buf.append("            return ").append(argsName).append(";\n");
                buf.append("        } else {\n");
                buf.append("            return this.getByIndex(").append(getArgs).append(");\n");
                buf.append("        }\n");
                buf.append("    }\n\n");
            }
        }

        //batch get with paging and response
        buf.append("    public PageResponse<").append(entityName).append("Po> getPage(PageRequest pageRequest) {\n");
        buf.append("        JdbcResult jdbcResult = BaseJdbcUtils.getSelectForCount(getTable(), (Criteria) null);\n");
        buf.append("        Integer total = template.queryForObject(jdbcResult.getSql(), jdbcResult.getParams(), Integer.class);\n");
        buf.append("        if(total == 0) {\n");
        buf.append("            return new PageResponse<>(0, null);\n");
        buf.append("        }\n\n");
        buf.append("        jdbcResult = BaseJdbcUtils.getSelect(getTable(), (Criteria) null, pageRequest);\n");
        buf.append("        List<").append(entityName).append("Po> datas = template.queryForList(jdbcResult.getSql(), jdbcResult.getParams()).stream()\n");
        buf.append("                .map(dbRow -> BaseJdbcUtils.dbRowToPo(dbRow, dbMapping, ").append(entityName).append("Po.class))\n");
        buf.append("                .collect(Collectors.toList());\n");
        buf.append("        return new PageResponse<>(total, datas);\n");
        buf.append("    }\n\n");

        //delete
        List<String> keyCols = primaryKeys(table, nameTypes);
        String primaryKeyArgs = keyCols == null ? "String id" : queryArgs(keyCols, nameTypes);
        buf.append("    public int delete(").append(primaryKeyArgs).append(") {\n");
        buf.append("        JdbcResult jdbcResult = BaseJdbcUtils.getDelete(getTable(), ").append(criteriaArgs(16, keyCols)).append(");\n");
        buf.append("        return template.update(jdbcResult.getSql(), jdbcResult.getParams());\n");
        buf.append("    }\n\n");

        //getTable
        buf.append("    private String getTable() {\n");
        buf.append("        return TABLE_NAME;\n");
        buf.append("    }\n");

        buf.append("}");
        return buf.toString();
    }

    private static String getByCols(List<String> keyCols, Map<String, String> nameTypes, String entityName, boolean primaryKey) {
        String keyArgs = keyCols == null ? "String id" : queryArgs(keyCols, nameTypes);
        StringBuilder buf = new StringBuilder();
        if (primaryKey) {
            buf.append("    public ").append(entityName).append("Po get(").append(keyArgs).append(") {\n");
        } else {
            buf.append("    public ").append(entityName).append("Po getByIndex(").append(keyArgs).append(") {\n");
        }
        buf.append("        JdbcResult jdbcResult = BaseJdbcUtils.getSelect(getTable(), ");
        buf.append(criteriaArgs(16, keyCols)).append(");\n");
        buf.append("        try {\n");
        buf.append("            Map<String, Object> dbRow = template.queryForMap(jdbcResult.getSql(), jdbcResult.getParams());\n");
        buf.append("            return BaseJdbcUtils.dbRowToPo(dbRow, dbMapping, ").append(entityName).append("Po.class);\n");
        buf.append("        } catch (EmptyResultDataAccessException e) {\n");
        buf.append("            return null;\n");
        buf.append("        }\n");
        buf.append("    }\n\n");
        return buf.toString();
    }

    private static String javaType(String sqlType) {
        sqlType = sqlType.toUpperCase();
        if (sqlType.contains("UNSIGNED")) {
            sqlType = sqlType.replace("UNSIGNED", "").trim();
        }

        if (typeMap.containsKey(sqlType.toUpperCase())) {
            return typeMap.get(sqlType.toUpperCase());
        } else {
            log.warn("无法识别的mysql数据类型[{}]将映射为String", sqlType);
            return "String";
        }
    }

    private static String toCamel(String src, boolean firstUpper) {
        return BaseStringUtils.underScoreToCamel(src, firstUpper);
    }

    private static String entityName(String table) {
        int index = table.indexOf("t_");
        if (index == 0) {
            table = table.substring(2);
        }
        index = table.indexOf("T_");
        if (index == 0) {
            table = table.substring(2);
        }

        table = table.replaceAll("_\\d{1,}$", "");
        return toCamel(table, true);
    }

    private static DataSource dataSource(DbInfo dbInfo) throws Exception {
        Map<String, Object> datasourceMap = new HashMap<>();
        datasourceMap.put("url", dbInfo.getUrl());
        datasourceMap.put("username", dbInfo.getUser());
        if (StringUtils.isNotBlank(dbInfo.getPass())) {
            datasourceMap.put("password", dbInfo.getPass());
        }

        return DruidDataSourceFactory.createDataSource(datasourceMap);
    }

    private static String criteriaArgs(int space, List<String> cols) {
        StringBuilder buf = new StringBuilder();
        if (BaseCollectionUtils.isEmpty(cols)) {
            buf.append("Criteria.column(\"").append("id").append("\").eq(").append("id").append(")");
        } else if (cols.size() == 1) {
            buf.append("Criteria.column(\"").append(cols.get(0)).append("\").eq(").append(toCamel(cols.get(0), false)).append(")");
        } else {
            buf.append("CriteriaBuilder.newBuilder()\n");
            IntStream.range(0, cols.size()).forEach(index -> {
                buf.append(StringUtils.repeat(" ", space)).append(".column(\"").append(cols.get(index)).append("\").eq(").append(toCamel(cols.get(index), false)).append(")\n");
            });
            buf.append(StringUtils.repeat(" ", space)).append(".build()");
        }
        return buf.toString();
    }

    private static List<String> primaryKeys(String table, Map<String, String> nameTypes) {
        if (BaseCollectionUtils.isNotEmpty(primaryKeysMap.get(table))) {
            return primaryKeysMap.get(table);
        } else if (BaseCollectionUtils.isNotEmpty(uniqueIndexMap.get(table))) {
            return uniqueIndexMap.get(table);
        } else if (BaseCollectionUtils.isNotEmpty(indexMap.get(table))) {
            return indexMap.get(table);
        } else {
            return Arrays.asList(nameTypes.entrySet().stream()
                    .filter(entry -> StringUtils.isNotBlank(entry.getKey())).findFirst().get().getKey());
        }
    }

    private static String primaryKeySimpleArgs(String table, Map<String, String> nameTypes) {
        StringBuilder buf = new StringBuilder();
        List<String> cols = primaryKeys(table, nameTypes);
        IntStream.range(0, cols.size()).forEach(index -> {
            if (index != 0) {
                buf.append(", ");
            }
            buf.append("\"").append(cols.get(index)).append("\"");
        });
        return buf.toString();
    }

    private static String primaryKeyArgs(String table, Map<String, String> nameTypes) {
        List<String> cols;
        if (BaseCollectionUtils.isNotEmpty(primaryKeysMap.get(table))) {
            cols = primaryKeysMap.get(table);
        } else if (BaseCollectionUtils.isNotEmpty(uniqueIndexMap.get(table))) {
            cols = uniqueIndexMap.get(table);
        } else {
            return "String id";
        }

        return queryArgs(cols, nameTypes);
    }

    private static String queryArgs(List<String> cols, Map<String, String> nameTypes) {
        StringBuilder buf = new StringBuilder();
        IntStream.range(0, cols.size()).forEach(index -> {
            if (index != 0) {
                buf.append(", ");
            }
            String colName = cols.get(index);
            buf.append(javaType(nameTypes.get(colName))).append(" ").append(toCamel(colName, false));
        });

        return buf.toString();
    }

    private static String getArgs(String prefix, List<String> cols, Map<String, String> dbMapping) {
        StringBuilder buf = new StringBuilder();
        IntStream.range(0, cols.size()).forEach(index -> {
            if (index != 0) {
                buf.append(", ");
            }
            buf.append(prefix).append(".get");
            String colName = cols.get(index);
            Map.Entry<String, String> entry = dbMapping.entrySet().stream().filter(item -> item.getValue().equals(colName)).findAny().orElse(null);
            String fieldName = entry.getKey();
            buf.append(fieldName.substring(0, 1).toUpperCase()).append(fieldName.substring(1)).append("()");
        });
        return buf.toString();
    }

    private static String ensure(String entityCamelName) {
        if (entityCamelName.equals("case")) {
            return "casePo";
        } else {
            return entityCamelName;
        }
    }

    enum Type {
        dao,
        po,
        config,
        aspect,
        factory
    }
}

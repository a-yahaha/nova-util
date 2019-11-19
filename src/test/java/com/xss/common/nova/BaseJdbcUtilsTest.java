package com.xss.common.nova;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.google.common.collect.ImmutableMap;
import com.xss.common.nova.model.*;
import com.xss.common.nova.util.BaseDateUtils;
import com.xss.common.nova.util.BaseJdbcUtils;
import com.xss.common.nova.util.BaseStringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.IntStream;

import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.Assert.*;

@Slf4j
public class BaseJdbcUtilsTest {
    private static String table = "t_user";
    private static Map<String, String> dbMapping = ImmutableMap.of(
            "id", "id",
            "name", "name",
            "age", "age",
            "lastModifyTime", "last_update_at");
    private JdbcTemplate template;

    @BeforeClass
    public static void init(){
        BaseJdbcUtils.defaultDbType(BaseJdbcUtils.DbType.MYSQL);
    }

    @Test
    public void basicTest() {
        UserEntity user = new UserEntity();
        user.setId(BaseStringUtils.uuidSimple());
        user.setName("wyb");
        user.setAge(30);
        user.setLastModifyTime(new Date());

        UserEntity user2 = new UserEntity();
        user2.setId(BaseStringUtils.uuidSimple());
        user2.setName("wyl");
        user2.setAge(4);
        user2.setLastModifyTime(new Date());

        //insert
        JdbcResult jdbcResult = BaseJdbcUtils.getInsert(table, user, dbMapping);
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("insert into t_user (`id`,`name`,`age`,`last_update_at`) values (?,?,?,?)"));
        assertTrue(jdbcResult.getParams().length == 4);
        assertTrue(jdbcResult.getParams()[2].equals(30));
        assertTrue(jdbcResult.getParams()[3].equals(user.getLastModifyTime()));

        //insert ignore
        jdbcResult = BaseJdbcUtils.getInsertIgnore(table, user, dbMapping);
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("insert ignore into t_user (`id`,`name`,`age`,`last_update_at`) values (?,?,?,?)"));

        //batch insert
        jdbcResult = BaseJdbcUtils.getBatchInsert(table, Arrays.asList(user, user2), dbMapping);
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("insert into t_user (`id`,`name`,`age`,`last_update_at`) values (?,?,?,?)"));
        assertTrue(jdbcResult.getBatchParams().size() == 2);
        assertTrue(jdbcResult.getParams()[2].equals(30));

        //batch insert ignore
        jdbcResult = BaseJdbcUtils.getBatchInsertIgnore(table, Arrays.asList(user, user2), dbMapping);
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("insert ignore into t_user (`id`,`name`,`age`,`last_update_at`) values (?,?,?,?)"));

        //select
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("name").eq("wyb"));
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("select * from t_user where `name`=?"));
        assertTrue(jdbcResult.getParams().length == 1);
        assertTrue(jdbcResult.getParams()[0].equals("wyb"));

        //select with paging
        PageRequest pageRequest = PageRequest.of(1, 10).orderBy("age");
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("age").eq(20), pageRequest);
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("select * from t_user where `age`=? order by `age` desc limit 0,10"));

        //select for count
        jdbcResult = BaseJdbcUtils.getSelectForCount(table, Criteria.column("age").eq(20));
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("select count(*) from t_user where `age`=?"));

        //select for sum
        jdbcResult = BaseJdbcUtils.getSelectForSum(table, Criteria.column("name").eq("wyb"), "age");
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("select sum(`age`) from t_user where `name`=?"));

        jdbcResult = BaseJdbcUtils.getSelectForSum(table, Criteria.column("name").eq("wyb"), "age", "name");
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("select sum(`age`),sum(`name`) from t_user where `name`=?"));

        //patch
        UserEntity patchUser = new UserEntity();
        patchUser.setAge(31);
        jdbcResult = BaseJdbcUtils.getPatch(table, patchUser, dbMapping, Criteria.column("id").eq(user.getId()));
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("update t_user set `age`=? where `id`=?"));

        //update
        UserEntity updateUser = new UserEntity();
        updateUser.setId(user.getId());
        updateUser.setName(user.getName());
        updateUser.setLastModifyTime(new Date());
        jdbcResult = BaseJdbcUtils.getUpdate(table, updateUser, dbMapping, "id");
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("update t_user set `name`=?,`age`=?,`last_update_at`=? where `id`=?"));
        assertTrue(jdbcResult.getParams().length == 4);
        jdbcResult = BaseJdbcUtils.getUpdate(table, updateUser, dbMapping, Criteria.column("id").eq(updateUser.getId()));
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("update t_user set `name`=?,`age`=?,`last_update_at`=? where `id`=?"));
        assertTrue(jdbcResult.getParams().length == 4);
        jdbcResult = BaseJdbcUtils.getUpdate(table, updateUser, dbMapping, Arrays.asList("id", Criteria.column("name").eq("wyl")));
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("update t_user set `name`=?,`age`=?,`last_update_at`=? where `id`=? and `name`=?"));
        assertTrue(jdbcResult.getParams().length == 5);
        assertEquals(jdbcResult.getParams()[3], updateUser.getId());
        assertEquals(jdbcResult.getParams()[4], "wyl");

        //batch update
        UserEntity updateUser2 = new UserEntity();
        updateUser2.setId(user2.getId());
        updateUser2.setName(user2.getName());
        updateUser2.setLastModifyTime(new Date());
        jdbcResult = BaseJdbcUtils.getBatchUpdate(table, Arrays.asList(updateUser, updateUser2), dbMapping, Arrays.asList("id", Criteria.column("age").le(100)));
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("update t_user set `name`=?,`age`=?,`last_update_at`=? where `id`=? and `age`<=?"));
        assertTrue(jdbcResult.getBatchParams().size() == 2);
        Object[] params2 = jdbcResult.getBatchParams().get(1);
        assertTrue(params2.length == 5);
        assertEquals(params2[0], "wyl");
        assertEquals(params2[3], user2.getId());
        jdbcResult = BaseJdbcUtils.getBatchUpdate(table, Arrays.asList(updateUser, updateUser2), dbMapping, Arrays.asList("id", Criteria.column("name").eq("wyl")));
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("update t_user set `name`=?,`age`=?,`last_update_at`=? where `id`=? and `name`=?"));
        assertTrue(jdbcResult.getBatchParams().size() == 2);
        params2 = jdbcResult.getBatchParams().get(1);
        assertTrue(params2.length == 5);
        assertEquals(params2[4], "wyl");

        //db row to entity
        UserEntity userEntity = BaseJdbcUtils.dbRowToEntity(ImmutableMap.of("id", "123", "name", "wyb", "age", 20, "last_update_at",
                BaseDateUtils.fromDateFormat("2000-01-01")), dbMapping, UserEntity.class);
        assertEquals(userEntity.getId(), "123");
        assertEquals(userEntity.getName(), "wyb");
        assertEquals((long) userEntity.getAge(), 20);
        assertEquals(userEntity.getId(), "123");
        assertEquals(BaseDateUtils.toDateFormat(userEntity.getLastModifyTime()), "2000-01-01");

        //or
        jdbcResult = BaseJdbcUtils.getSelect(table, CriteriaBuilder.newBuilder()
                .column("name").eq("wyb").or("age").eq(20).build());
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("select * from t_user where `name`=? or `age`=?"));
        assertTrue(jdbcResult.getParams().length == 2);
        assertTrue(jdbcResult.getParams()[0].equals("wyb"));
        assertTrue(jdbcResult.getParams()[1].equals(20));

        //combined criteria
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.column("name").eq("wyb"));
        criteriaList.add(CriteriaBuilder.newBuilder().or("name").eq("lll").column("age").eq(20).buildCombined());
        jdbcResult = BaseJdbcUtils.getSelect(table, criteriaList);
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("select * from t_user where `name`=? or (`name`=? and `age`=?)"));
        assertTrue(jdbcResult.getParams().length == 3);
        assertTrue(jdbcResult.getParams()[0].equals("wyb"));
        assertTrue(jdbcResult.getParams()[1].equals("lll"));
        assertTrue(jdbcResult.getParams()[2].equals(20));

        Criteria oneCondition = CriteriaBuilder.newBuilder().column("id_card").isNull().column("real_name_status").eq("Y").buildCombined();
        Criteria secondCondition = CriteriaBuilder.newBuilder().or("id_card").eq("36233019841126").column("real_name_status").eq("N").buildCombined();
        Criteria combined = CriteriaBuilder.newBuilder().criteria(oneCondition).criteria(secondCondition).buildCombined();
        List<Criteria> finalCriteria = CriteriaBuilder.newBuilder().column("id").eq("123").criteria(combined).build();
        jdbcResult = BaseJdbcUtils.getSelect(table, finalCriteria);
        assertThat(jdbcResult.getSql(), equalToIgnoringCase("select * from t_user where `id`=? and ((isnull(`id_card`) and `real_name_status`=?) or (`id_card`=? and `real_name_status`=?))"));
        assertTrue(jdbcResult.getParams().length == 4);
        assertTrue(jdbcResult.getParams()[0].equals("123"));
        assertTrue(jdbcResult.getParams()[1].equals("Y"));
        assertTrue(jdbcResult.getParams()[2].equals("36233019841126"));
        assertTrue(jdbcResult.getParams()[3].equals("N"));
    }

    @Test
    public void testUsingDb() {
        Assume.assumeTrue(dbExist());
        log.info("开始利用数据库进行测试...");

        //删除年龄0-100的所有数据
        JdbcResult jdbcResult = BaseJdbcUtils.getDelete(table, Arrays.asList(Criteria.column("age").ge(0), Criteria.column("age").le(100)));
        template.update(jdbcResult.getSql(), jdbcResult.getParams());

        //准备用户数据
        UserEntity user1 = new UserEntity();
        user1.setId(BaseStringUtils.uuidSimple());
        user1.setName("wyb");
        user1.setAge(33);
        user1.setLastModifyTime(new Date());
        UserEntity user2 = new UserEntity();
        user2.setId(BaseStringUtils.uuidSimple());
        user2.setName("wyl");
        user2.setAge(4);
        user2.setLastModifyTime(DateUtils.addSeconds(user1.getLastModifyTime(), 1));
        UserEntity user3 = new UserEntity();
        user3.setId(BaseStringUtils.uuidSimple());
        user3.setName("lll");
        user3.setAge(30);
        user3.setLastModifyTime(DateUtils.addSeconds(user1.getLastModifyTime(), 2));

        //插入一条数据
        jdbcResult = BaseJdbcUtils.getInsert(table, user1, dbMapping);
        int row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 1);

        //再次插入重复主键数据
        jdbcResult = BaseJdbcUtils.getInsert(table, user1, dbMapping);
        try {
            template.update(jdbcResult.getSql(), jdbcResult.getParams());
            assertTrue(false);
        } catch (Throwable e) {
            assertTrue(e instanceof DuplicateKeyException);
        }

        //使用ignore再次插入重复主键数据
        jdbcResult = BaseJdbcUtils.getInsertIgnore(table, user1, dbMapping);
        row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 0);

        //批量插入两条数据
        jdbcResult = BaseJdbcUtils.getBatchInsert(table, Arrays.asList(user2, user3), dbMapping);
        row = IntStream.of(template.batchUpdate(jdbcResult.getSql(), jdbcResult.getBatchParams())).sum();
        assertTrue(row == 2);

        //根据id查一条数据
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("id").eq(user2.getId()));
        Map<String, Object> dbRow = template.queryForMap(jdbcResult.getSql(), jdbcResult.getParams());
        UserEntity user = BaseJdbcUtils.dbRowToEntity(dbRow, dbMapping, UserEntity.class);
        assertEquals(user.getName(), user2.getName());
        assertEquals(user.getAge(), user2.getAge());
        assertEquals(user.getLastModifyTime(), user2.getLastModifyTime());

        //根据条件查询多条数据
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("age").gt(4));
        List<Map<String, Object>> dbRows = template.queryForList(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(dbRows.size() == 2);
        dbRows.forEach(dbResult -> {
            UserEntity entity = BaseJdbcUtils.dbRowToEntity(dbResult, dbMapping, UserEntity.class);
            if (entity.getId().equals(user1.getId())) {
                assertEquals(entity.getName(), user1.getName());
                assertEquals(entity.getAge(), user1.getAge());
            }
        });

        //分页查询, 默认倒序
        jdbcResult = BaseJdbcUtils.getSelect(table, (Criteria) null, PageRequest.of(1, 2).orderBy("age"));
        dbRows = template.queryForList(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(dbRows.size() == 2);
        user = BaseJdbcUtils.dbRowToEntity(dbRows.get(0), dbMapping, UserEntity.class);
        assertEquals(user.getAge(), Integer.valueOf(33));

        //分页查询, 升序
        jdbcResult = BaseJdbcUtils.getSelect(table, (Criteria) null, PageRequest.of(2, 2).orderBy("last_update_at", PageRequest.Order.ASC));
        dbRows = template.queryForList(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(dbRows.size() == 1);
        user = BaseJdbcUtils.dbRowToEntity(dbRows.get(0), dbMapping, UserEntity.class);
        assertEquals(user.getName(), "lll");

        //count and in
        jdbcResult = BaseJdbcUtils.getSelectForCount(table, Criteria.column("age").in(Arrays.asList(4, 33)));
        assertEquals(template.queryForObject(jdbcResult.getSql(), jdbcResult.getParams(), Integer.class), Integer.valueOf(2));

        //sum age
        jdbcResult = BaseJdbcUtils.getSelectForSum(table, Criteria.column("age").ge(4), "age");
        assertEquals(template.queryForObject(jdbcResult.getSql(), jdbcResult.getParams(), Integer.class), Integer.valueOf(67));

        //patch by field
        user = new UserEntity();
        user.setAge(34);
        user.setId(user1.getId());
        jdbcResult = BaseJdbcUtils.getPatch(table, user, dbMapping, "id");
        row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 1);
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("id").eq(user1.getId()));
        dbRow = template.queryForMap(jdbcResult.getSql(), jdbcResult.getParams());
        UserEntity userEntity = BaseJdbcUtils.dbRowToEntity(dbRow, dbMapping, UserEntity.class);
        assertEquals(userEntity.getAge(), Integer.valueOf(34));

        //patch age-2
        jdbcResult = BaseJdbcUtils.getPatch(table, user, dbMapping, ImmutableMap.of("age", new UpdateOper(UpdateOper.Type.SUBTRACT, 2)),  "id");
        row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 1);
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("id").eq(user1.getId()));
        dbRow = template.queryForMap(jdbcResult.getSql(), jdbcResult.getParams());
        userEntity = BaseJdbcUtils.dbRowToEntity(dbRow, dbMapping, UserEntity.class);
        assertEquals(userEntity.getAge(), Integer.valueOf(32));

        //patch by criteria
        user.setAge(33);
        jdbcResult = BaseJdbcUtils.getPatch(table, user, dbMapping, Criteria.column("id").eq(user1.getId()));
        row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 1);
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("id").eq(user1.getId()));
        dbRow = template.queryForMap(jdbcResult.getSql(), jdbcResult.getParams());
        userEntity = BaseJdbcUtils.dbRowToEntity(dbRow, dbMapping, UserEntity.class);
        assertEquals(userEntity.getAge(), Integer.valueOf(33));

        //patch by list
        user.setAge(34);
        jdbcResult = BaseJdbcUtils.getPatch(table, user, dbMapping, Arrays.asList(
                "id", Criteria.column("age").in(Arrays.asList(1, 2, 3))));
        row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 0);
        user.setAge(35);
        jdbcResult = BaseJdbcUtils.getPatch(table, user, dbMapping, Arrays.asList(
                "id", Criteria.column("age").in(Arrays.asList(33, 34))));
        row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 1);
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("id").eq(user1.getId()));
        dbRow = template.queryForMap(jdbcResult.getSql(), jdbcResult.getParams());
        userEntity = BaseJdbcUtils.dbRowToEntity(dbRow, dbMapping, UserEntity.class);
        assertEquals(userEntity.getAge(), Integer.valueOf(35));

        //update by field
        user.setAge(33);
        jdbcResult = BaseJdbcUtils.getUpdate(table, user, dbMapping, "id");
        row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 1);
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("id").eq(user1.getId()));
        dbRow = template.queryForMap(jdbcResult.getSql(), jdbcResult.getParams());
        userEntity = BaseJdbcUtils.dbRowToEntity(dbRow, dbMapping, UserEntity.class);
        assertEquals(userEntity.getAge(), Integer.valueOf(33));

        //update age+1
        jdbcResult = BaseJdbcUtils.getUpdate(table, user, dbMapping, ImmutableMap.of("age", new UpdateOper(UpdateOper.Type.ADD, 2)), "id");
        row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 1);
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("id").eq(user1.getId()));
        dbRow = template.queryForMap(jdbcResult.getSql(), jdbcResult.getParams());
        userEntity = BaseJdbcUtils.dbRowToEntity(dbRow, dbMapping, UserEntity.class);
        assertEquals(userEntity.getAge(), Integer.valueOf(35));

        //update by criteria
        user.setAge(34);
        jdbcResult = BaseJdbcUtils.getUpdate(table, user, dbMapping, Criteria.column("id").eq(user1.getId()));
        row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 1);
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("id").eq(user1.getId()));
        dbRow = template.queryForMap(jdbcResult.getSql(), jdbcResult.getParams());
        userEntity = BaseJdbcUtils.dbRowToEntity(dbRow, dbMapping, UserEntity.class);
        assertEquals(userEntity.getAge(), Integer.valueOf(34));

        //update by list
        user.setAge(35);
        jdbcResult = BaseJdbcUtils.getPatch(table, user, dbMapping, Arrays.asList(
                "id", Criteria.column("age").in(Arrays.asList(33, 32))));
        row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 0);
        jdbcResult = BaseJdbcUtils.getPatch(table, user, dbMapping, Arrays.asList(
                "id", Criteria.column("age").in(Arrays.asList(34))));
        row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 1);
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("id").eq(user1.getId()));
        dbRow = template.queryForMap(jdbcResult.getSql(), jdbcResult.getParams());
        userEntity = BaseJdbcUtils.dbRowToEntity(dbRow, dbMapping, UserEntity.class);
        assertEquals(userEntity.getAge(), Integer.valueOf(35));

        //update time to null
        user.setLastModifyTime(null);
        jdbcResult = BaseJdbcUtils.getUpdate(table, user, dbMapping, "id");
        row = template.update(jdbcResult.getSql(), jdbcResult.getParams());
        assertTrue(row == 1);
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("id").eq(user1.getId()));
        dbRow = template.queryForMap(jdbcResult.getSql(), jdbcResult.getParams());
        user = BaseJdbcUtils.dbRowToEntity(dbRow, dbMapping, UserEntity.class);
        assertTrue(user.getLastModifyTime() == null);

        //batch update
        Date date = BaseDateUtils.getDayStart(new Date());
        user1.setLastModifyTime(date);
        user2.setLastModifyTime(date);
        user3.setLastModifyTime(date);
        jdbcResult = BaseJdbcUtils.getBatchUpdate(table, Arrays.asList(user1, user2, user3), dbMapping, Arrays.asList("id", Criteria.column("age").le(100)));
        row = (int) IntStream.of(template.batchUpdate(jdbcResult.getSql(), jdbcResult.getBatchParams())).count();
        assertTrue(row == 3);

        //insertOrUpdate
        UserEntity user4 = new UserEntity();
        user4.setId(BaseStringUtils.uuidSimple());
        user4.setName("wyb");
        user4.setAge(28);
        user4.setLastModifyTime(new Date());
        jdbcResult = BaseJdbcUtils.getInsertOrUpdate(table, user4, dbMapping);
        assertTrue(template.update(jdbcResult.getSql(), jdbcResult.getParams()) == 2);

        //get
        jdbcResult = BaseJdbcUtils.getSelect(table, Criteria.column("id").eq(user1.getId()));
        assertNotNull(template.queryForMap(jdbcResult.getSql(), jdbcResult.getParams()));
    }

    private boolean dbExist() {
        if (template != null) {
            return true;
        }

        try {
            HashMap datasourceMap = new HashMap();
            datasourceMap.put("driverClassName", "com.mysql.jdbc.Driver");
            datasourceMap.put("url", "jdbc:mysql://localhost:3306/test");
            datasourceMap.put("username", "root");
            datasourceMap.put("init", "true");
            DruidDataSource dataSource = (DruidDataSource)DruidDataSourceFactory.createDataSource(datasourceMap);

            //检查t_user表是否存在
            Connection conn = dataSource.createPhysicalConnection();
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, "t_user", null);
            if (rs == null || !rs.next()) {
                return false;
            }

            template = new JdbcTemplate(dataSource);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}

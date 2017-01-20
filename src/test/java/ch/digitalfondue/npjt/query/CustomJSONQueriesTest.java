/**
 * Copyright (C) 2015 digitalfondue (info@digitalfondue.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.digitalfondue.npjt.query;

import ch.digitalfondue.npjt.*;
import ch.digitalfondue.npjt.mapper.ColumnMapper;
import ch.digitalfondue.npjt.mapper.ColumnMapperFactory;
import ch.digitalfondue.npjt.mapper.ParameterConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.annotation.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestJdbcConfiguration.class)
public class CustomJSONQueriesTest {

    @Autowired
    DataSource dataSource;

    private static Gson JSON = new GsonBuilder().create();

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface AsJson {
    }

    private static class JsonColumnMapper extends ColumnMapper {

        JsonColumnMapper(String name, Class<?> paramType) {
            super(name, paramType);
        }

        @Override
        public Object getObject(ResultSet rs) throws SQLException {
            return JSON.fromJson(rs.getString(name), paramType);
        }
    }

    private static boolean containAsJsonAnnotation(Annotation[] annotations) {
        if(annotations == null) {
            return false;
        }
        for(Annotation annotation : annotations) {
            if(annotation.annotationType() == AsJson.class) {
                return true;
            }
        }
        return false;
    }

    private static class JsonColumnMapperFactory implements ColumnMapperFactory {

        @Override
        public ColumnMapper build(String name, Class<?> paramType) {
            return new JsonColumnMapper(name, paramType);
        }

        @Override
        public int order() {
            return 0;
        }

        @Override
        public boolean accept(Class<?> paramType, Annotation[] annotations) {
            return containAsJsonAnnotation(annotations);
        }

        @Override
        public RowMapper<Object> getSingleColumnRowMapper(final Class<Object> clzz) {
            return new RowMapper<Object>() {
                @Override
                public Object mapRow(ResultSet resultSet, int rowNum) throws SQLException {
                    return JSON.fromJson(resultSet.getString(1), clzz);
                }
            };
        }
    }

    private static class JsonParameterConverter implements ParameterConverter {

        @Override
        public boolean accept(Class<?> parameterType, Annotation[] annotations) {
            return containAsJsonAnnotation(annotations);
        }

        @Override
        public void processParameter(String parameterName, Object arg, Class<?> parameterType, MapSqlParameterSource ps) {
            ps.addValue(parameterName, JSON.toJson(arg, parameterType));
        }

        @Override
        public int order() {
            return 0;
        }
    }

    @Test
    public void simpleQueriesTest() {
        QueryFactory qf = new QueryFactory("hsqldb", new JdbcTemplate(dataSource));
        qf.addColumnMapperFactory(new JsonColumnMapperFactory());
        qf.addParameterConverters(new JsonParameterConverter());

        JsonQueries jq = qf.from(JsonQueries.class);

        jq.createTable();

        Map<String, String> map = Collections.singletonMap("MY_KEY", "MY_VALUE");
        jq.insertValue("TEST", map);

        JsonConf conf = jq.findByKey("TEST");
        Assert.assertTrue(conf.conf.equals(map));

        Assert.assertTrue(jq.findConfBoolByKey("TEST").equals(map));

    }

    public static class JsonConf {

        final String key;
        final Map<String, String> conf;

        public JsonConf(@ConstructorAnnotationRowMapper.Column("CONF_KEY") String key,
                        @ConstructorAnnotationRowMapper.Column("CONF_JSON") @AsJson Map<String, String> conf) {
            this.key = key;
            this.conf = conf;
        }

    }

    public interface JsonQueries {
        @Query("CREATE TABLE LA_CONF_JSON (CONF_KEY VARCHAR(64) PRIMARY KEY NOT NULL, CONF_JSON CLOB NOT NULL)")
        void createTable();

        @Query("INSERT INTO LA_CONF_JSON(CONF_KEY, CONF_JSON) VALUES(:key, :confJson)")
        int insertValue(@Bind("key") String key,  @Bind("confJson") @AsJson Map<String, String> conf);

        @Query("SELECT * FROM LA_CONF_JSON WHERE CONF_KEY = :key")
        JsonConf findByKey(@Bind("key") String key);

        @Query("SELECT CONF_JSON FROM LA_CONF_JSON WHERE CONF_KEY = :key")
        @AsJson
        Map<String, String> findConfBoolByKey(@Bind("key") String key);
    }
}

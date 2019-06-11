npjt-extra
==========

[![Build Status](https://travis-ci.org/digitalfondue/npjt-extra.svg?branch=master)](https://travis-ci.org/digitalfondue/npjt-extra) [![Coverage Status](https://coveralls.io/repos/digitalfondue/npjt-extra/badge.svg?branch=master)](https://coveralls.io/r/digitalfondue/npjt-extra?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/ch.digitalfondue.npjt-extra/npjt-extra.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ch.digitalfondue.npjt-extra%22%20AND%20a%3A%22npjt-extra%22)

A small layer over Spring's NamedParameterJdbcTemplate, it provide a similar interface as:

 - the jdbi object api (http://jdbi.org/sql_object_api_queries/)
 - the spring-data-jpa named parameters api (http://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.named-parameters)
 
With some extra goodies. Requires java8 or later.

Note: this project predates spring-data-jdbc.

See the Use and examples section.

## Download

### Maven

```xml
<dependency>
	<groupId>ch.digitalfondue.npjt-extra</groupId>
	<artifactId>npjt-extra</artifactId>
	<version>2.0.0</version>
</dependency>
```

### Gradle

```
compile "ch.digitalfondue.npjt-extra:npjt-extra:1.1.5"
```

## Use and examples

npjt-extra is composed of 3 parts: 

 - a default RowMapper (annotation based)
 - an interface based query repository
 - the configuration classes

### RowMapping definition

For mapping a row to a class, npjt-extra offer a default row mapper that require the following restriction:

 - the class must have only **one public constructor** (this restriction could be lifted off).
 - each of the constructor argument must have a @Column annotation that map the column name to the parameter.

This constructor approach is for promoting an immutable model.

*IF* you cannot map using this strategy, you can specify the mapper class in the @Query and @QueryOverride annotation.
Your class must implement `org.springframework.jdbc.core.RowMapper<T>`.
See below in the "Basic use" section.

Example:

```java

import ch.digitalfondue.npjt.ConstructorAnnotationRowMapper.Column;

public static class Conf {

  final String key;
  final String value;

  public Conf(@Column("CONF_KEY") String key, @Column("CONF_VALUE") String value) {
    this.key = key;
    this.value = value;
  }
}

```

As you can probably guess, this class will map a ResultSet containing 2 column named "CONF_KEY" and "CONF_VALUE".

### Query repository definition

npjt-extra generate a proxy from the interface defined by the user.

The rules are simple:

 - you can define a method without parameter that return NamedParameterJdbcTemplate: the proxy will return the underlying NamedParameterJdbcTemplate.
 - you can define default methods
 - all the others methods must contain the @Query(...) annotation
 
#### Basic use

A basic "query repository" will be similar to (using the Conf class defined before):

```java

import java.util.List;
import ch.digitalfondue.npjt.Bind;
import ch.digitalfondue.npjt.Query;

public interface MySimpleQueries {

  /** insert a key,value pair, return the number of affected rows */
  @Query("INSERT INTO LA_CONF(CONF_KEY, CONF_VALUE) VALUES(:key, :value)")
  int insertValue(@Bind("key") String key, @Bind("value") String value);

  /** 
   * find a single element, as the underlying NamedParameterJdbcTemplate, 
   * it will launch an exception if there are 0 or more than 1 object
   */
  @Query("SELECT * FROM LA_CONF WHERE CONF_KEY = :key")
  Conf findByKey(@Bind("key") String key);

  /** It will map multiple values too */
  @Query("SELECT * FROM LA_CONF")
  List<Conf> findAll();
  
  
  /** You can override the default mapper and specify your own */
  @Query(value = "SELECT * FROM LA_CONF", mapper = MyConfMapper.class)
  List<MyConf> findAllCustomMapper();
  
  
  /** You can search "simple" types too if they are supported by spring jdbc */
  @Query("SELECT CONF_VALUE FROM LA_CONF WHERE CONF_KEY = 'BLA'")
  String findBla();

  /** You can search Lists of "simple" types too if they are supported by spring jdbc */
  @Query("SELECT CONF_KEY FROM LA_CONF")
  List<String> findAllKeys();

}
 
```

The custom mapper specified in the annotation of the method findAllCustomMapper must implement `org.springframework.jdbc.core.RowMapper<T>`. 

#### Query override

If you want to support multiple DB which have some slightly different syntax you can override a query:

For example:

```java

import ch.digitalfondue.npjt.Query;
import ch.digitalfondue.npjt.QueryOverride;
import ch.digitalfondue.npjt.QueriesOverride;

public interface QueryTest {
  @Query("SELECT * FROM LA_CONF")
  @QueriesOverride({
  	@QueryOverride(db = "MYSQL", value = "SELECT * FROM LA_CONF_MYSQL"),
  	@QueryOverride(db = "PGSQL", value = "SELECT * FROM LA_CONF_PGSQL")
  })
  List<Conf> findAll();
}
```

When creating the ch.digitalfondue.npjt.QueryFactory that will generate the repositories, you must specify
2 parameters: first the **DB** name and second the DataSource.


If the db name match the db parameter specified in a @QueryOverride, the associated String value will be used.

*Note*: if you are defining a custom mapper in the @Query annotation, _you_ must specify it in the @QueryOverride too!

#### Fetch generated keys

If you have a table with an auto generated key and you want to get the value after your insert, you can set the return
type to `AffectedRowCountAndKey<T>`.

Example:

You have a table (HSQLDB) defined as:

```sql
CREATE TABLE LA_AUTO (ID INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY NOT NULL, VALUE CLOB NOT NULL);
``` 

In your QueryRepository you define:

```java

public interface AutogeneratedKeyQueries {

  @Query("INSERT INTO LA_AUTO(VALUE) VALUES (:value)")
  AffectedRowCountAndKey<Integer> insert(@Bind("value") String value);

}
```

The returned AffectedRowCountAndKey will contain the ID of the inserted element. You can see in action in the test AutogeneratedKeyQueriesTest.java

Notes:
 
 - in some cases (for example if you are using pgsql), you must define the column name of the generated identifier with the annotation `@AutoGeneratedKey("COLUMN_NAME")` as the DB even though there is a single generated key it will return more than one.
 - it's currently a basic implementation that support the simplest use cases: pull request or test cases that highlight a missing feature are welcome! 

#### Query templates

If you only require to generate a query string which depend from the db type, you can define a query template.

You need to define the type in the @Query annotation to QueryType.TEMPLATE and the return type of the query as String.

For example:

```java

public interface QueryTest {

  @Query(type = QueryType.TEMPLATE, value = "MY_TEMPLATE")
  @QueriesOverride({
  	@QueryOverride(db = "MYSQL", value = "SELECT * FROM MY_TEMPLATE_MYSQL"),
  	@QueryOverride(db = "PGSQL", value = "SELECT * FROM MY_TEMPLATE_PGSQL")
  })
  String template();
}
```

Calling template() will return "MY_TEMPLATE" (or the overridden values).

##### Optional

You can wrap the returned object in a Optional. For example:

```java
@Query("SELECT * FROM LA_CONF WHERE CONF_KEY = :key")
Optional<Conf> findByKey(@Bind("key") String key);
```

Will work as expected. If the query return more than one object it will launch an exception like the unwrapped
version.

##### Default methods in the interface

You can add default methods too, for example, if you need some custom query directly with
the NamedParameterJdbcTemplate:

```java
public interface MySimpleQueries {

  
  @Query("INSERT INTO LA_CONF(CONF_KEY, CONF_VALUE) VALUES(:key, :value)")
  int insertValue(@Bind("key") String key, @Bind("value") String value);
  
  /** any method that return NamedParameterJdbcTemplate and has 0 arguments will return the underlying NamedParameterJdbcTemplate*/
  NamedParameterJdbcTemplate getNamedParameterJdbcTemplate();

  /** here your default method */
  default String defaultMethod(String key) {
    return getNamedParameterJdbcTemplate()
			.queryForObject("SELECT CONF_VALUE FROM LA_CONF WHERE CONF_KEY = :key", Collections.singletonMap("key", key), String.class);
  }
}
```

##### LocalDate, LocalDateTime, Instant support

npjt-extra support out of the box LocalDate, LocalDateTime and Instant both as a parameter of a interface method and as a mapped value.


### Configuration

You can configure it in 2 ways.

#### Using @EnableNpjt annotation

First you need to annotate your query repository with the ch.digitalfondue.npjt.QueryRepository annotation.

Note, you will need a DataSource configured.

Then you only need to configure the packages to scan:

```java

/** scan the packages "ch.digitalfondue.npjt.query" and "ch.digitalfondue.npjt.columnmapper" */
@EnableNpjt(basePackages = {"ch.digitalfondue.npjt.query", "ch.digitalfondue.npjt.columnmapper"})
public class MyConfig2 {
}

```
 
#### Manual instantiation

Using the JavaConfig:

```java

public class MyConfig {
   
  /** instantiate the interface MySimpleQueries */
  /** the db type could be another parameter :) */
  @Bean
  public MySimpleQueries getMySimpleQueries(DataSource dataSource) {
    return QueryFactory.from(MySimpleQueries.class, "HSQLDB", dataSource);
  }
  
}

```



All the annotated interfaces will be available in your spring context.

### Data type mapping

By default, npjt-extra has the following input parameters/result set mapping facilities enabled:

 - a default parameter/result set mapper which use the same logic as the one from the jdbctemplate
 - a enum mapper that convert from/to a string representation
 - support ZonedDateTimeMapper, LocalDate, LocalDateTime and Instant

You can add new mappers by exposing as a bean a `List<ColumnMapperFactory>` and a `List<ParameterConverter>` (see example at: https://github.com/digitalfondue/npjt-extra/blob/master/src/test/java/ch/digitalfondue/npjt/query/CustomJSONQueriesTest.java#L125).

If you want to configure from an empty list, you can define your custom `QueryFactory`: see full example at https://github.com/digitalfondue/npjt-extra/blob/master/src/test/java/ch/digitalfondue/npjt/query/customfactory/CustomJSONQueriesWithCustomQueryFactoryTest.java#L131 
where instead of calling `super.getDefaultFactories();` you can begin from an empty List.

Then when using the @EnableNpjt you will need to specify the Factory:

`@EnableNpjt(queryFactory = CustomQueryFactory.class, basePackages = {"ch.digitalfondue.npjt.query.customfactory"})` 
 

For both mappers, the application order is defined by the `order()` method. The smallest the `int` returned, the higher the priority.
 

#### Input parameters mapping

You must implement the `ch.digitalfondue.npjt.mapper.ParameterConverter` interface and register the converter in the queryFactory calling the `QueryFactory.addParameterConverters(ParameterConverter parameterConverter)` method.

#### Result set objects mapping 

You must implement the `ch.digitalfondue.npjt.mapper.ColumnMapperFactory` interface and register the converter in the queryFactory calling the `addColumnMapperFactory(ColumnMapperFactory columnMapperFactory)` method.
 

## Javadoc

 - http://javadoc.io/doc/ch.digitalfondue.npjt-extra/npjt-extra/

## License

The library is under The Apache Software License, Version 2.0

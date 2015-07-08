npjt-extra
==========

[![Build Status](https://travis-ci.org/digitalfondue/npjt-extra.svg?branch=master)](https://travis-ci.org/digitalfondue/npjt-extra) [![Coverage Status](https://coveralls.io/repos/digitalfondue/npjt-extra/badge.svg?branch=master)](https://coveralls.io/r/digitalfondue/npjt-extra?branch=master)

A small layer over Spring's NamedParameterJdbcTemplate, it provide a similar interface as :

 - the jdbi object api (http://jdbi.org/sql_object_api_queries/)
 - the spring-data-jpa named parameters api (http://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.named-parameters)
 
With some extra goodies.

See the Use and examples section.

## Download

### Maven

```xml
<dependency>
	<groupId>ch.digitalfondue.npjt-extra</groupId>
	<artifactId>npjt-extra</artifactId>
	<version>1.0.2</version>
</dependency>
```

### Gradle

```
compile "ch.digitalfondue.npjt-extra:npjt-extra:1.0.2"
```

## Use and examples

npjt-extra is composed of 3 parts: 

 - an annotation based RowMapper
 - an interface based query repository
 - the configuration classes

### RowMapping definition

For mapping a row to a class, npjt-extra require the following restriction:

 - the class must have only **one public constructor** (this restriction could be lifted off).
 - each of the constructor argument must have a @Column annotation that map the column name to the parameter.

This constructor approach is for promoting an immutable model. 

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
 - if you use java8: you can define default methods
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
  
  
  /** You can search "simple" types too if they are supported by spring jdbc */
  @Query("SELECT CONF_VALUE FROM LA_CONF WHERE CONF_KEY = 'BLA'")
  String findBla();

  /** You can search Lists of "simple" types too if they are supported by spring jdbc */
  @Query("SELECT CONF_KEY FROM LA_CONF")
  List<String> findAllKeys();

}
 
```

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

#### Fetch generated keys

TBD

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

#### Java 8 support

##### Optional

If you use java8, you can wrap the returned object in a Optional. For example:

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

If you don't need the support, you can remove them calling `QueryFactory.removeColumnMapperFactory` and `QueryFactory.removeParameterConverter`
with the following classes:

 - LocalDate: `LocalDateMapper.Factory.class` and `LocalDateMapper.Parameter.class`
 - LocalDateTime: `LocalDateTimeMapper.Factory.class` and `LocalDateTimeMapper.Parameter.class`
 - Instant: `InstantMapper.Factory.class` and `InstantMapper.Parameter.class`


### Configuration

npjt-extra require that you configure the ch.digitalfondue.npjt.QueryFactory, then you can 

 - instantiate manually the various query repositories with ch.digitalfondue.npjt.QueryFactory.from(final Class<T> clazz);
 - scan the package(s) with ch.digitalfondue.npjt.QueryRepositoryScanner
 
#### Manual instantiation

Using the JavaConfig:

```java

public class MyConfig {


  /** the db type could be another parameter :) */
  @Bean
  public QueryFactory queryFactory(DataSource dataSource) {
    return new QueryFactory("hsqldb", dataSource);
  }
  
  
  /** instantiate the interface MySimpleQueries */
  @Bean
  public MySimpleQueries getMySimpleQueries(QueryFactory queryFactory) {
    return queryFactory.from(MySimpleQueries.class);
  }
  
}

```

#### Using the QueryRepositoryScanner

First you need to annotate your query repository with the ch.digitalfondue.npjt.QueryRepository annotation.

Then you only need to configure the packages to scan:

```java

public class MyConfig2 {


  /** the db type could be another parameter :) */
  @Bean
  public QueryFactory queryFactory(DataSource dataSource) {
    return new QueryFactory("hsqldb", dataSource);
  }
  
  
  /** scan the packages "ch.digitalfondue.npjt.query" and "ch.digitalfondue.npjt.columnmapper" */
  @Bean
  public QueryRepositoryScanner getScanner(QueryFactory queryFactory) {
    return new QueryRepositoryScanner(queryFactory, 
      "ch.digitalfondue.npjt.query",
      "ch.digitalfondue.npjt.columnmapper");
  }
}

```

All the annotated interfaces will be available in your spring context.

### Custom data type mapping

[TODO]

## Javadoc

[TODO]

## License

The library is under The Apache Software License, Version 2.0
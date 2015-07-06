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
	<version>1.0.1</version>
</dependency>
```

### Gradle

```
compile "ch.digitalfondue.npjt-extra:npjt-extra:1.0.1"
```

## Use and examples

npjt-extra is composed of 3 parts: 

 - an annotation based driven RowMapper
 - the interface based query repository definition
 - the configuration classes

### RowMapping definition

For mapping a row to class, npjt-extra require the following restriction:

 - the class must have only **one public constructor**.
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
 
[TODO: TBD]

### Configuration

[TODO: TBD]

## Javadoc

[TODO: TBD]

## License

The library is under the The Apache Software License, Version 2.0
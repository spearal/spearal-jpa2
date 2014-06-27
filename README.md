Spearal JPA2
============

## What is Spearal?

Spearal is a compact binary format for exchanging arbitrary complex data between various endpoints such as Java EE, JavaScript / HTML, Android and iOS applications.

Spearal-Jpa2 is an extension of Spearal-Java which supports JPA2+ entities and deals with uninitialized properties.

## How to get and build the project?

First, you need to get, build and install Spearal-Java:

````sh
$ git clone https://github.com/spearal/spearal-jpa2.git
$ cd spearal-java
$ ./gradlew install
````

Then, you can build Spearal JPA2:

````sh
$ cd ..
$ git clone https://github.com/spearal/spearal-jpa2.git
$ cd spearal-java
$ ./gradlew build
````

The built library can then be found in the `build/libs/` directory.

## How to use the library?

You don't need to do anything particular. Just add the spearal-jpa2-*.jar library to yopur classpath, it will be automatically loaded as a service.

````java
SpearalFactory factory = new SpearalFactory();
ByteArrayOutputStream baos = new ByteArrayOutputStream();
SpearalEncoder encoder = factory.newEncoder(baos);
encoder.writeAny(entity);
````

All uninitialized properties in `entity` will be skipped.

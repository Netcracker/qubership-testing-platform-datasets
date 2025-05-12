# ATP Dataset

## How to start service
(in some case with flag -DskipTests)
1. Build project (mvn clean package) with profiles:
    * db-postgresql
    * generate-q-classes-pg
    * migration-on-build-pg
    
    Example: 
    ````
    mvn clean package -P db-postgresql,generate-q-classes-pg,migration-on-build-pg
    ````
   
   Example with skip tests:
   ````
   mvn clean package -DskipTests=true -P db-postgresql,generate-q-classes-pg,migration-on-build-pg
   ````

2. Change value of _spring.resources.static-locations_ in application.properties from module atp-dataset: 
    
    ````properties
    spring.resources.static-locations=file:./atp-dataset/web/
    ````

2. Main class: org.qubership.atp.dataset.Main
3. VM options: 
    ````properties
    -Dspring.config.location=target/config/application.properties
    -Dspring.cloud.bootstrap.location=target/config/bootstrap.properties
    -Dlogging.level.org.qubership.atp.common.logging.interceptor.RestTemplateLogInterceptor=debug
    -Dlogging.level.org.qubership.atp.catalogue.service.client.feign.DatasetFeignClient=debug
    -Dds.logger.level=DEBUG
    -Dlogging.level.root=DEBUG
    ````
4. Use classpath of module: atp-dataset
5. Workdir (directory with project, example): atp-dataset

## How to prepare local DB before application running
* Create database
    ````
    CREATE DATABASE datasets;
    ````
* Create extension
    ````
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    ````
* Create user
    ````
    CREATE USER postgres WITH PASSWORD 'admin';
    ````
* Grant privileges on database to user
    ````
    GRANT ALL PRIVILEGES ON DATABASE "datasets" to postgres;
    ````
  
# How build project (mvn clean package) with profiles:
    db-postgresql
    generate-q-classes-pg
    migration-on-build-pg


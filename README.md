# Qubership Testing Platform Datasets Service

## Description
Datasets Service is used to store and manage test case data.

### Key entities

1. Data set list (DSL)
2. Data set (DS)
3. Variable
    1. Key-value
    2. A list of lines
    3. File variables
    4. Complex - it deals with storing complex data sets, for example, Ben's cases.

### Basic functions

1. Storing, creating, editing, removing, cloning of key entities;
2. A tool that creates and stores Data Set is a storage of a test case running first and AT start initiator;
3. Access functions should provide access via REST and from Java;
4. Support of macros and formulas:
    1. RAND - Generates a random number;
    2. DATE - Returns current date;
    3. TIME - Returns current time;
    4. There should be an option to move from one variable to another;
    5. There should be an option to operate data and time, for example, to add one day to the date;
    6. There should be an option to concatenate several variables, lines, numbers in one variable;
    7. The list of Excel formulas used in DS can be found here: atp_dataset_formulas_from_excel.txt
    8. Explicitly describe the types of variables and possibly the behavior and format of writing.
5. Export/import support in Excel format is required;
    1. DSL is exported completely with all its DS.
       Availability of multiple DSL in one Excel file is required:  
       it is mandatory if there is a logical grouping of lists as a tree (folders)
    2. Relationship between the main entities in different systems:
        1. CERM:
            1. Configuration (1 Project = 1 DSL = 1..N DS)
            2. Execution (1 TestCase = 1 DSL = 1 DS)
        2. ATP:
            1. Configuration (1 Project = 1..N DSL)
            2. Configuration (1 TestCase = 1..N DSL)
            3. Execution (1 TestCase = 1 DSL = 1 DS)
        3. ITF:
            1. Configuration (1 Call chain = 1..N DSL)
            2. Execution (1 TestCase = 1 DSL = 1 DS)
6. Add an option of blocking Data set entity. Block time should be limited, so that a block can be removed on the second day after creation.
7. Getting a list of outdated DSL with an option to clean these DSL.
8. It should enable importing DSL into already existing DSL
9. It should enable export and import of multiple DSL at a time.

### Entities 

- **VA**
  - DSL visibility area. Allows to separate the projects (Currently it's the same as project. So, 1 Visibility area = 1 project).
- **DSL**
  - Defines the set of attributes for specific datasets.
- **DS**
  - Contains the specific set of values for attributes in the dataset.
- **Attribute**
  - Defines the type of values (currently, the following are supported: list values, text values, links to DS).
- **Parameter**
  - Defines the attribute value in the specific dataset.

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


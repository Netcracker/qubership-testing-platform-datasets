cd %~d0%~p0

rm console.log

:: TECHNICAL INFORMATION _ DO NOT MODIFY
SET jdbc.Dialect=com.querydsl.sql.PostgreSQLTemplates
SET jdbc.Driver=org.postgresql.Driver

:: DATABASE CONNECTION SETTINGS
::DATASET SITE URL: http://mb-datasets:9000/ui/index.html
SET jdbc.Url=jdbc:postgresql://mb-datasets/datasets
SET jdbc.User=postgres
SET jdbc.Password=Qwe54321
SET jdbc_type=pg
SET gridfs.database=gridfs
SET gridfs.host=localhost
SET gridfs.port=27017
SET gridfs.user=
SET gridfs.password=

::JENKINS CI TEST DATABASE
::SET jdbc.Url=jdbc:postgresql://kube01nd04cn:5433/ci_db
::SET jdbc.User=ci_db_user
::SET jdbc.Password=VHeE4BBV2

:: full file path to folder with excel files
SET excel_folder=C:\SVN\ATP_2.0\dataset_2_attempt\atp-dataset-import\TEST_DATA\test_data_1
:: excel file name with "parent" data
SET parent_file_name=Parent_SMALL.xlsx
:: excel file name with "child" data sets
SET child_file=Child_SMALL.xlsx
::visibility area name to store data set lists
SET va_name=TEST_VA
::data set list name where all child datasets are stored
SET dsl_name=TEST_DSL
::data set name for parameter groups (for PARENT groups and child groups parameters)
SET default_group_dataset_name=DEFAULT_TEST
::for ability to create attributes in root
SET root_attr=FALSE
::===================================Attention please====================================
::In case you got an error: Exception in thread "main" java.lang.OutOfMemoryError: GC overhead limit exceede d at
::You need allocate more RAM for java process.
::=======================================================================================
SET q_classes_cp=q-classes/atp-dataset-q-classes-generation-${project.version}-%jdbc_type%.jar
java -Dgridfs.port=%gridfs.port% -Dgridfs.host=%gridfs.host% -Dgridfs.database=%gridfs.database% -Dgridfs.user=%gridfs.user% -Dgridfs.password=%gridfs.password% -Djdbc.Dialect=%jdbc.Dialect% -Djdbc.Driver=%jdbc.Driver% "-Djdbc.Url=%jdbc.Url%" "-Djdbc.User=%jdbc.User%" "-Djdbc.Password=%jdbc.Password%" "-Dexcel_folder=%excel_folder%" "-Dparent_file_name=%parent_file_name%" "-Dchild_file=%child_file%" "-Dva_name=%va_name%" "-Ddsl_name=%dsl_name%" "-Ddefault_group_dataset_name=%default_group_dataset_name%" "-Droot_attr=%root_attr%" -cp "lib/*;%q_classes_cp%" migration.org.qubership.atp.dataset.DataSetImporter %* --spring.main.web-application-type=none --spring.main.allow-bean-definition-overriding=true
pause

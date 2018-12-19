
THIS IS AN UNFINISHED DRAFT
==========================

sample.oauth.store
=======================

This sample project provides example implementations of `com.ibm.websphere.security.oauth20.store.OAuthStore` for the WebSphere Liberty profile.

There are two sample custom store projects. The first project, `sample.bell`, contains an implementation that is packaged and loaded into Liberty using the `bells-1.0` feature, while the second project, `sample.user.feature`, contains an implementation that is packaged and loaded into WebSphere Liberty as a user feature. The samples are otherwise functionally equivalent and use a MongoDB database to store the clients, tokens and consents. 

Quick Start
===========
1. Clone the `sample.oauth.store` project:
   > git clone https://github.com/WASdev/sample.oauth.store
   
   > cd sample.oauth.store

1. Start WebSphere Liberty in either the `sample.bell` or `sample.user.feature` projects. This command will build the required libraries and install them into the WebSphere Liberty instance and then start a WebSphere Liberty server that is configured with the OAuthStore implementation.
   > ./gradlew sample.bell:start
   
   OR
   
   > ./gradlew sample.user.feature.start

1. Run the functional tests in the `sample.test` project. These tests will download and start up a MongoDB instance and run the tests against the running WebSphere Liberty server.
   > ./gradlew sample.test:test

1. Stop the WebSphere Liberty Server.
   > ./gradlew sample.bell:stop
   
   OR
   
   > ./gradlew sample.user.feature:stop

More Detailed
=============
1. Optional: Download, install and start mongoDB if you do not wish to use the testing MongoDB instance. See https://www.mongodb.com/
   - If installed on Windows, go to the installation location bin directory (example: `Program Files/mongoDB/Server/40`)
   - Start the mongoDB server: mongod.exe
   - You will need to update the mongo properties files to not start a temporary MongoDB instance. See instructions below to edit the `SupportFiles/mongoDB.props` file.

1. Bring down the sample projects with git: 
   > git clone https://github.com/WASdev/sample.oauth.store
   
   > cd sample.oauth.store
   
1. If you started your own mongoDB instance, edit the `SupportFiles/mongoDB.props` file.
   - Change `START_MONGODB=true` to `START_MONGODB=false`
   - Optionally change the `HOST`, `PORT`, or `DBNAME`
   - If you need to add a user and password, uncomment the `USER` and `PWD` lines and fill in your username and password.
      - If you are using the user feature project, also edit `sample.user.feature/src/liberty/config/server.xml`. Add the `user="${user}" password="${password}"` attributes to the `customStoreMongoDBConfig` element.

1. To build and start a server running one of the custom OAuthStore samples, run on of the following commands:

    > ./gradlew sample.bell:start

    OR

    > ./gradlew sample.user.feature:start
   
1. To check if your BELL or User Feature loaded, check the messages.log
   - For the BELL, check `sample.oauth.store\sample.bell\build\wlp\usr\servers\server1\logs\messages.log` for `I CustomStoreSample Bell initialized.`
   - For the User Feature, check `sample.oauth.store\sample.user.feature\build\wlp\usr\servers\server1\logs\messages.log` for `I CustomStoreSample User Feature initialized.`
   
1. To check if the CustomStoreSample connected to MongoDB, check the messages.log for the following message (should appear after 30 seconds): `I Connected to the database oauthSample`

1. Run the functional tests in the `sample.test` project. These tests will download and start up a MongoDB instance and run the tests against the running WebSphere Liberty server.
   > ./gradlew sample.test:test

1. Stop the WebSphere Liberty Server.
   > ./gradlew sample.bell:stop
   
   OR
   
   > ./gradlew sample.user.feature:stop

Developing in Eclipse
=====================
1. If you did not do the quick start steps, bring down the sample projects with git: 
   > git clone https://github.com/WASdev/sample.oauth.store
   
   > cd sample.oauth.store

1. Generate the Eclipse project and classpath files for all the sub-projects.
   > ./gradlew cleanEclipse eclipse

1. Acquire and install an Eclipse IDE. See http://www.eclipse.org/downloads/ For example,  Eclipse Photon for Java EE Developers ( 4.8 ) https://www.eclipse.org/downloads/packages/release/photon/r/eclipse-ide-java-ee-developers

1. Open Eclipse and create a new workspace

1. Import sample projects into Eclipse as existing projects. In your Eclipse workspace go to, File > Import > General > Existing projects into workspace. Select `sample.bell`, `sample.user.feature`, and/or `sample.test`.

1. To build and run your changes, run either
   > ./gradlew sample.bell:start

    OR

   > ./gradlew sample.user.feature:start


Connecting to your mongoDB database with customized configuration
=================================================================
The CustomStoreSample loads the information about the database from the `SupportFiles/mongoDB.props` file. It is configured to use localhost:27017 (default MongoDB port) by default. To update the database name, hostname, port or add a user and password, update the  `SupportFiles/mongoDB.props`. Whenever you change the mongoDB.props, do a gradlew build on the project to copy the file over. The `SupportFiles/mongoDB.props` is copied into the server, overwriting the existing file, every time a build is done.

To use an existing MongoDB instance with the `sample.test` tests instead of the testing instance that the `sample.test` project starts up, set `START_MONGODB=false` in the `mongoDB.props` file.

Running with user/password authentication:
   - If authentication is enabled on the mongoDB database, uncomment and set the `USER` and `PWD` fields in the mongoDB.props file.
   - If you will be using the user feature project, edit `sample.user.feature/src/liberty/config/server.xml`. Add the `user="${user}" password="${password}"` attributes to the `customStoreMongoDBConfig` element.
   > The testing MongoDB instance that `sample.test` starts up does not have authentication enabled. Setting credentials while `START_MONGODB=true` will result in the tests failing.
   

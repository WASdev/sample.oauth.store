
THIS IS AN UNFINISHED DRAFT
==========================

sample.oauth.store
=======================

This sample project provides example implementations of com.ibm.websphere.security.oauth20.store.OAuthStore for the WebSphere Liberty profile.

The first project, `sample.bell` contains an implementation that is packaged and loaded into Liberty using the `bells-1.0` feature, while the second project, `sample.user.feature`, contains an implementation that is packaged and loaded into WebSphere Liberty as a user feature. The samples are otherwise functionally equivalent and use a MongoDB database to store the clients, tokens and consents. 

Quick Start
===========
1. Clone the `sample.oauth.store` project:
   > git clone https://github.com/WASdev/sample.oauth.store
   > cd sample.oauth.store

1. Start WebSphere Liberty in either the `sample.bell` or `sample.user.feature` projects. This command will build the required libraries and install them into the WebSphere Liberty instance and then start a WebSphere Liberty server that is configured with the OAuthStore implementation.
   > ./gradlew sample.bell:start
   
   OR
   
   > ./gradlew sample.user.feature.start

1. Run the functional tests in the `sample.test` project. These tests will start up a MongoDB instance and run the tests against the running WebSphere Liberty server.

1. Stop the WebSphere Liberty Server.
   > ./gradlew sample.bell:stop
   
   OR
   
   > ./gradlew sample.user.feature:stop

More Detailed
=============
1. Download, install and start mongoDB if you do not wish to use the testing MongoDB instance. See https://www.mongodb.com/
   - If installed on Windows, go to the installation location bin directory (Program Files/mongoDB/Server/versionNum)
   - Start the mongoDB server: mongod.exe

1. Bring down the sample projects with git: 
   > git clone https://github.com/WASdev/sample.oauth.store

1. To build and start a server running one of the custom OAuthStore samples, run on of the following commands:

    > ./gradlew sample.bell:start

    OR

    > ./gradlew sample.user.feature:start

   - Pre-GA: For Windows, edit the build.gradle file and change `commandLine "${wlpRoot}/bin/installUtility"` to commandLine `"${wlpRoot}/bin/installUtility.bat"` (add the .bat).
   
1. To check if your BELL or User Feature loaded, check the messages.log
   - For the BELL, check `sample.oauth.store\sample.bell\build\wlp\usr\servers\server1\logs\messages.log` for `I CustomStoreSample Bell initialized.`
   - For the User Feature, check `sample.oauth.store\sample.user.feature\build\wlp\usr\servers\server1\logs\messages.log` for `I CustomStoreSample User Feature initialized.`
   
1. To check if the CustomStoreSample connected to MongoDB, check the messages.log for the following message (should appear after 30 seconds): `I Connected to the database oauthSample`

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


Connecting to your mongoDB database with customized configuration
=================================================================
The CustomStoreSample loads the information about the database from the `SupportFiles/mongoDB.props` file. If you are done building, update the `mongoDB.props` in your server directory. If you plan to rebuilds, then update the `SupportFiles/mongoDB.props`. Do a gradlew build on the project to copy the file over and restart. The `SupportFiles/mongoDB.props` is copied into the server, overwriting the existing file, every time a build is done.

To change the database name, host or port, edit values in the mongoDB.props file.

To use an existing MongoDB instance with the `sample.test` tests instead of the testing instance that the `sample.test` project starts up, set "START_MONGODB=false" in the `mongoDB.props` file.

If authentication is enabled on the mongoDB database, uncomment and set the "USER" and "PASSWORD" fields in the mongoDB.props file.
   - NOTE: The testing MongoDB instance that `sample.test` starts up does not have authentication enabled. Setting credentials while "START_MONGODB=true" will result in the tests failing.


THIS IS AN UNFINISHED DRAFT
==========================

sample.oauth.store
=======================

This sample project provides an example implementation of com.ibm.websphere.security.oauth20.store.OAuthStore for the WebSphere Liberty profile. It provides a Bells and a user feature example jar. This sample uses a mongoDB database to store the information. 

The projects contains a user feature and a Bell using the implementation class, CustomStoreSample. It provides the instructions for creating a user feature and a Bell. You only need to follow the directions for one or the other.

Quick Start
===========
1. Download, install and start mongoDB. See https://www.mongodb.com/
   - If installed on Windows, go to the installation location bin directory (Program Files/mongoDB/Server/versionNum)
   - Start the mongoDB server: mongod.exe

1. Bring down the sample projects with git: `git clone https://github.com/WASdev/sample.oauth.store`

1. To build and start a server running one of the custom OAuthStore samples, run on of the following commands:

    > ./gradlew sample.bell:start

    OR

    > ./gradlew sample.user.feature:start

   - Pre-GA: For Windows, edit the build.gradle file and change `commandLine "${wlpRoot}/bin/installUtility"` to commandLine `"${wlpRoot}/bin/installUtility.bat"` (add the .bat).

Develop in Eclipse
==============
1. If you did not do the quick start steps, bring down the sample projects with git: `git clone https://github.com/WASdev/sample.oauth.store`

1. Generate the eclipse artifacts in the bell and/or user feature projects. Run
    > ./gradlew sample.bell:eclipse

    OR

    > ./gradlew sample.user.feature:eclipse

1. Acquire and install an Eclipse IDE. See http://www.eclipse.org/downloads/ For example,  Eclipse Photon for Java EE Developers ( 4.8 ) https://www.eclipse.org/downloads/packages/release/photon/r/eclipse-ide-java-ee-developers

1. Open Eclipse and create a new workspace

1. Import sample projects into Eclipse as existing projects. In your Eclipse workspace go to, File > Import > General > Existing projects into workspace. Select sample.user.feature or sample.bell


Connecting to your mongoDB database with customized configuration
=================================================================
The CustomStoreSample loads the information about the database from a mongoDB.props file. If you are done building, update the mongoDB.props in your server directory. If you are continuing to make changes and doing builds, then update the mongoDB.props in the Support Files directory. Do a gradlew build on the project to copy it over and restart. The mongoDB.props is copied over everytime a build is done.

To change the database name, host or port, edit values in the mongoDB.props file.   

Create a database requiring authorization. MongoDB standalone server tips: If installed on Windows, go to the installation location bin directory (Program Files/mongoDB/Server/versionNum)
1. Start the mongoDB server: run mongod.exe
1. Access the mongoDB CLI: run mongo.exe
1. In the CLI window opened by mongo.exe, create the database and a user (replace values as needed, oauthSample is the default database name).
      - Create a database named default: 
      > use oauthSample
      - Create a user named testUser and a pwd of fancyPassword: 
      > db.createUser( {    user: "testUser",    pwd: "fancyPassword",    roles: [      { role: "readWrite", db: "default" }    ]  } )

Add a user and password to log into mongoDB
   - Uncomment the "USER" and "PWD" options in the mongoDB.props. Add your database user/pwd.





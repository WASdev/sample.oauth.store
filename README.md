# sample.oauth.store
Sample implementations of the WebSphere Liberty com.ibm.websphere.security.oauth20.store.OAuthStore API, which allows a customer to implement their own persistent store for for clients, consents, and tokens.

THIS IS AN UNFINISHED DRAFT
==========================

sample.oauth.store
=======================

This sample project provides an example implementation of com.ibm.websphere.security.oauth20.store.OAuthStore for the WebSphere Liberty profile. It provides a Bells and a user feature example jar. This sample uses a mongoDB database to store the information. 

The projects contains a user feature and a Bell using the implementation class, CustomStoreSample. It provides the instructions for creating a user feature and a Bell. You only need to follow the directions for one or the other.

INCOMPLETE: Things you will need:
==============
- A recent copy of the Eclipse IDE. See http://www.eclipse.org/downloads/ For example,  Eclipse Photon for Java EE Developers ( 4.8 ) https://www.eclipse.org/downloads/packages/release/photon/r/eclipse-ide-java-ee-developers
- A compatible Websphere Developer Tools for WebSphere® Application Server Liberty -- for example, https://developer.ibm.com/wasdev/downloads/#asset/tools-IBM_Liberty_Developer_Tools_for_Eclipse_Photon  See here for general install options: https://www.ibmdw.net/wasdev/downloads/
- An IBM Liberty Runtime at V18.0.0.4 or later.
- A mongoDB install. See https://www.mongodb.com/
- A mongoDB java driver.

INCOMPLETE: Setup instructions:
============
1. Install Eclipse (if not installed already)
1. Open Eclipse and create a new workspace
1. Install Websphere Developer Tools for WebSphere® Application Server Liberty into Eclipse.
   - In Eclipse, go to Help > Install New Software. Click Add. If you downloaded a zip file, point to the location of the zip file. Or enter the URL to the download site. Follow directions and restart eclipse.
1. Create a WebSphere Liberty Profile runtime in Eclipse
   - In Eclipse, go to Window > Preferences > Server > Runtime Environments > Add > IBM > Liberty Runtime. Check the "Create a new local server" box.
   - Click next
   -  Point to your Liberty 18.0.0.4 directory.
1. Bring down the sample projects with git: `git clone https://github.com/WASdev/sample.oauth.store`
1. Import sample projects into Eclipse. In your Eclipse workspace go to, File > Import > Existing projects into workspace. Select  OAuthCustomStoreUF (User Feature example) and/or OAuthCustomStoreBell (Bell example)
1. Add the mongoDB java driver to the class path for the project. Right click on  OAuthCustomStoreUF and/or OAuthCustomStoreBell. Select Java Build Path > Libraries tab > Add external jar. Select your mongoDB java driver jar.
1. Pre-18.0.0.4 GA: Add the com.ibm.ws.security.oauth.*.jar to the class path for the project. Right click on  OAuthCustomStoreUF and/or OAuthCustomStoreBell. Select Java Build Path > Libraries tab > Add external jar. Navigate to your liberty install and add wlp/lib/com.ibm.ws.security.oauth.*.jar.
1. Set up your Target Platform: Windows > Preferences > Plug-in Development > Target Platform, and select Liberty.
1. Create jar to install on server
   - Create User Feature jar : Right click on the OAuthCustomStoreUF project and select Export  > OSGI Bundle or Fragment. Select a location to save the jar.
   - Create Bell jar: Right click on the OAuthCustomStoreBell project and select Export  > OSGI Bundle or Fragment. Select a location to save the jar.
1. Create a new Liberty server. In this example, it will be referenced as server1.
1. Copy artifacts to your liberty install
   - User feature
      - Copy user feature jar you created to usr/extensions/lib (create if it doesn't exist).. 
      - Copy customStoreSample-1.0.mf (provided in the SupportFiles directory) to usr/extensions/lib/features (create if it doesn't exist) .
      - Copy the mongoDB driver jar to ${shared.config.dir}/lib/global (create if doesn't exist) to use as a global library. You could also package it as part of the user feature.
   - Bell
      - Copy bell jar you created to the ${shared.config.dir} directory (wlp/usr/shared)
      - Copy the mongoDB driver jar to the ${shared.config.dir} directory (wlp/usr/shared). If you select a different location, change the server.xml to point to the correct location for the mongoDB driver.
1. Set up your server. Edit your server.xml and add the following features:
   - Add features to `<featureManager>` tag
       - `<feature>oauth-2.0</feature>`
      - For User Feature, add add `<feature>usr:customStoreSample-1.0</feature>`
      - Run with Bell, add `<feature>bells-1.0</feature>`
   - Add libraries
      -Library for Bell
``` 
<library id="customStoreLib">
        <fileset dir="${wlp.user.dir}/shared" includes="security.custom.store.bell_1.0.0.201812031435.jar,mongo-java-driver-2.14.2.jar"/>
    </library>
```
   - For Bell implementation, add Bell tag:
`<bell libraryRef="customStoreLib" service="com.ibm.websphere.security.oauth20.store.OAuthStore" />`
   - Add oauthProvider:
```
<oauthProvider id="OAuthConfig" filter="request-url%=ssodemo" oauthOnly="false">
		<customStore storeId="mongoDbStore" cleanupExpiredInterval="15"/>
		<autoAuthorizeClient>dclient01</autoAuthorizeClient>
		<autoAuthorizeClient>dclient02</autoAuthorizeClient>
	</oauthProvider>
```
1. Copy the mongoDB.props to your server directory (wlp/usr/servers/server1) and update it with the correct values for your mongoDB install.





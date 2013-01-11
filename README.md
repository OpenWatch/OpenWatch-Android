OpenWatch for Android
=================

A next generation [OpenWatch](http://openwatch.net) client for Android devices.

Downloading the Source
----------------------
When downloading the source make sure to clone the repository with:

    $ git clone git@github.com:OpenWatch/OpenWatch-Android.git --recursive
    
### Ignition
In case I stick with Ignition, here's how you do it:

1. Make sure you have maven 3 installed:

		$ mvn --version
		Apache Maven 3.0.3 (r1075438; 2011-02-28 09:31:09-0800)
		...
2. Set $ANDROID_HOME to your Android sdk dir and run `mvn clean install` in /ignition

		$ EXPORT ANDROID_HOME=/Path/to/android-sdk-macosx
		$ mvn clean install
		
3. Install [m2eclipse](http://eclipse.org/m2e/download/) and [m2e-android](http://rgladwell.github.com/m2e-android/) plugins for Eclipse

	**(OSX) In Eclipse**: 
	+ **m2eclipse**:
	+ Help -> Install New Software -> Add…
	+ Enter 'http://download.eclipse.org/technology/m2e/releases' as Location and Name as you wish. Follow the wizard through completion.
	+ **m2e-android**:
	+ Help -> Eclipse MarketPlace
	+ Make sure the "Eclipse MarketPlace" icon is selected
	+ Search for "android m2e" and select "Android Configurator for M2E"
		
	
4. Import the ignition support Android library project
 		 
		Import -> Maven -> Existing Maven Project -> Point to /ignition/ignition-support

5. Import the ignition core Java library project

		Import -> Maven -> Existing Maven Project -> Point to /ignition/ignition-core

6. Add ignition support project as an Android Library project dependency of OpenWatch

		R-Click OpenWatch project -> properties -> Android -> Libraries Pane -> Add -> Select ignition-core

7. Add ignition core as a Java project dependency of OpenWatch

		R-Click OpenWatch project -> properties -> Java Build Path -> Projects -> Add -> Select ignition-support

8. That was fun, friendly, and fascinating!




Running
----------------------

To run the software you'll need a device running Android 2.2+ with a camera and an ARMv7 processor with NEON support.

### Google Maps API v2

The new Google Maps API v2 requires you to piss yourself off registering each apk signing key you use (debug, production) with the [)Google API Console(https://code.google.com/apis/console]). 

On OSX:

1. cd ~/.android
2. keytool -list -v -keystore ./debug.keystore
3. when prompted for password, enter 'android'
4. copy the SHA1 fingerprint and append `:net.openwatch.reporter`
5. Follow [these steps](https://developers.google.com/maps/documentation/android/start#adding_the_api_key_to_your_application) to add the key to your Android application

TODO
----
+ Adjust bitrate of LQ stream per sendVideoChunk() network performance


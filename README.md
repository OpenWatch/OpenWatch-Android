OpenWatch for Android
=================

[Download on Google Play](https://play.google.com/store/apps/details?id=org.ale.openwatch&hl=en)

![Screenshot](https://s3.amazonaws.com/openwatch-static/static/assets/img/android.png)

[OpenWatch](http://openwatch.net) is a global citizen journalism project with the goal of building a more transparent and less corrupt society.

This app will allow you to stream video and photos directly to the web! It's the easiest possible way to get your media online. Use this application to record any events you witness, your encounters with the police, border agents, or other authority figures, or to just record anything you find interesting. Your recordings will appear online to be used in public interest investigations and news stories.

A high quality version is synced online once you're finished recording, giving you the advantages of a streaming service like uStream, but all of the high-quality video ability of your phone's local video camera.

OpenWatch can also send you alerts and special assignments, so you can become the top reporter for events in your local community. Top contributors may even be offered special missions and paid opportunities.

This is the first version of the application, so please let us know if you find any bugs or have any feedback to give us. OpenWatch is Free and Open Source software.

Downloading the Source
----------------------
When downloading the source make sure to clone the repository with:

    $ git clone git@github.com:OpenWatch/OpenWatch-Android.git --recursive
    
#### SSL Certificate KeyStore

OpenWatch-Android uses a bundled KeyStore to verify ssl connections against. To adapt this to your application you need to generate a BKS format KeyStore, place it in res/raw/, and modify HttpClient.java appropriately with the keystore filename. Place the keystore password in SECRETS.java:

##### Creating a KeyStore from your SSL certificates:

 1. Get your server's X509 format ssl certificates. You'll need the root and all intermediate certificates, but not your local cert. [In my experience this was easiest to do in Firefox](http://superuser.com/a/97203/185405)
 2. [Generate a BKS format keystore with your certificates](http://blog.antoine.li/2010/10/22/android-trusting-ssl-certificates/)
 3. Place your keystore in /res/raw and update `HttpClient` with the appropriate filename

 
#### SECRETS.java

Create a file named `SECRETS.java` in /src/org/ale/openwatch with the following content:


	package net.openwatch.reporter;

	public class SECRETS {
		public static final String SSL_KEYSTORE_PASS = "your_keystore_password";
		public static final String BUGSENSE_API_KEY = "your_bugsense_key_or_leave_blank";
	}

Building
-------
The top level directory of this project contains a folder that should be imported into your IDE as an Android project. All but `OpenWatch` should be imported as library projects, and added as dependencies to OpenWatch.

OpenWatch recommends building this project with [Android Studio and Gradle](http://developer.android.com/sdk/installing/studio.html), but this repository contains Eclipse project files as well.


API Keys
--------

### Google Maps API v2

OpenWatch-Android uses The new Google Maps API v2 requires you to register each apk signing key you use (debug, production) with the [Google API Console](https://code.google.com/apis/console). 

On OSX:

1. $ cd ~/.android
2. $ keytool -list -v -keystore ./debug.keystore 
3. When prompted for password, enter 'android'
4. Copy the SHA1 fingerprint and append `:org.ale.openwatch`. Enter this in the API Console under the "API Access" section. Look for "Key for Android apps".
5. Follow [these steps](https://developers.google.com/maps/documentation/android/start#adding_the_api_key_to_your_application) to add the key to your Android application



Running
-------

To run the software you'll need a device running Android 2.2+ (API Level 8) with a camera.

License
=========

	Software License Agreement (GPLv3+)
	
	Copyright (c) 2013, The OpenWatch Corporation, Inc. All rights reserved.
	
	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.

If you would like to relicense this code, 
please contact us at [team@openwatch.net](mailto:team@openwatch.net).

This software additionally references or incorporates the following sources
of intellectual property, the license terms for which are set forth
in the sources themselves (check the Submodules directory for more information):

* ActionBarSherlock
* android-async-http
* Android-Universal-Image-Loader
* Android-ViewPagerIndicator
* androrm
* TouchImageView

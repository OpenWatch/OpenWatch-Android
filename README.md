OpenWatch for Android
=================

A next generation [OpenWatch](http://openwatch.net) client for Android devices.

Downloading the Source
----------------------
When downloading the source make sure to clone the repository with:

    $ git clone git@github.com:OpenWatch/OpenWatch-Android.git
    
### Temporary Dependency
This project is currently configured to depend on a Library project [android-async-http](https://github.com/OnlyInAmerica/android-async-http). This is temporary as we contribute changes upstream to this project. To setup, clone the project, import "Existing Android Code" in Eclipse, right click on the OpenWatch project -> Android -> Libraries -> Add Class Folder -> Point to android-async-http root.


Running
----------------------

To run the software you'll need a device running Android 2.2+ with a camera and an ARMv7 processor with NEON support.


TODO
----
+ Send available meta data during MediaCapture calls
+ Adjust bitrate of LQ stream per sendVideoChunk() network performance
+ 
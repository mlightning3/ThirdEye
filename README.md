ThirdEye
========

Android app for controling the flask-video-streaming server. Both are projects
for the University of Michigan.

### Features

Currently the app supports most of the features available on the server's web-view

* Viewing the video stream
* Saving pictures and video
* Setting a default filename for pictures and video
* Downloading files from server
* Camera and image controls

Installing
==========

This app required Android version 4.4 (aka Kit Kat) or higher. Otherwise you
will have an error during installation.

Currently the only way to install the app is to sideload it. Either you need
to build the app from source, or you grab a finished apk from src/releases
which ideally will have tagged releases in. On the device you wish to install
the app on, you need to enable Unknown Sources in device settings (the exact
location of this setting will change depending on the phone you have, Google
is your friend here, but also the reason it isn't standard). Then copy the
apk file onto the device and locate it with a file browser on the device.
Selecting the file should start the installation process, and once you are
done, you can turn off Unknown Sources. The app will now be installed and ready
to be used, and can be uninstalled like any other app.

Building
========

To the poor soul that is tasked with updating this app, I am sorry. While the
process of setting up the build environment isn't too painful, understanding
how everything works together is more involved. All you need to do to get
started is install Android Studio, and follow it's instructions about how to
use it (such as loading development builds onto a real device).

Any time you need to make a new release build (aka publish an apk) you will
need a copy of the the keystore with the android development key for this app
(Which will be kept in a different location than this repository). Then you
can follow the steps for building a signed apk in Android Studio.

Here is a short summation of how the project is layed out. The app currently
uses different activities for each of the different main parts (Main view,
video stream view, setting view, filelisting view). As such, there is at
least one java file for each of the activities, as well as a resource file
that defines the layout that will be displayed on the screen. Some things,
like the filelisting activity, also call on other third party libraries, so
be prepared to look in other places than Google's Android development pages.

Copyright
=========
University of Michigan 2018

All rights reserved.
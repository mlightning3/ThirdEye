ThirdEye
========

Android app for controling the flask-video-streaming server. Both are projects
for the University of Michigan.

### Features

Currently the app supports all of the features available on the server's web-view
such as:

* Viewing the video stream
* Saving pictures and video
* Setting a default filename for pictures and video
* Downloading files from server
* Camera and image controls

Installing
==========

This app required Android version 4.4 (aka Kit Kat) or higher. Otherwise you
will have an error during installation.

Also note that if you are going to use this on Kit Kat, the built in web view
does not support the streaming format. To get around this, the app launches a
web browser when you go to view the stream, giving you the normal web-view from
the server.

Currently the only way to install the app is to sideload it. Either you need
to build the app from source, or you grab a finished apk from app/releases
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
started is install [Android Studio](https://developer.android.com/studio/install)
, and follow it's instructions about how to
use it (such as loading development builds onto a real device).

Any time you need to make a new release build (aka publish an apk) you will
need a copy of the the keystore with the android development key for this app
(Which will be kept in a different location than this repository). Then you
can follow the steps for [building a signed apk in Android Studio](https://developer.android.com/studio/publish/app-signing).

Here is a short summation of how the project is layed out. The app currently
uses one main [activity](https://developer.android.com/guide/components/activities/intro-activities) for everything to live in. Then each of the different
main parts (device control, video stream view, setting view, filelisting view),
are implemented as [fragments](https://developer.android.com/guide/components/fragments) (mini activities that can be swapped out). As such,
there is at least one java file for each of the fragments, as well as a resource
file that defines the layout that will be displayed on the screen. Some things,
like the filelisting activity, also call on other third party libraries, so
be prepared to look in other places than Google's Android development pages.

Copyright
=========
University of Michigan 2018

All rights reserved.

### Licenses

Vector icon assests from Google under Apache License 2.0

[Flexable Adapter](https://github.com/davideas/FlexibleAdapter) from davidea
under Apache License 2.0

[Glide](https://github.com/bumptech/glide) from bumptech under Apache License 2.0, BSD and MIT

[Android Material Color Picker Dialog](https://github.com/Pes8/android-material-color-picker-dialog) from Pes8 under MIT

ThirdEye
========

Android app for controlling the [flask-video-streaming server](https://github.com/mlightning3/flask-video-streaming). Both are projects
for the University of Michigan.

### Features

Currently the app supports all of the features available on the server's web-view
such as:

* Viewing the video stream
* Saving pictures and video
* Setting a default filename for pictures and video
* Downloading files from server
* Camera and image controls

### Hardware Requirements

* Android 4.4 or higher
* Wifi connectivity

When viewing a video stream, the processing power of the device running the app will make a difference
in framerate. It is recommended that you use a more modern device, ideally something that came with
Android 5 (Lollipop) or newer when it launched.

Installing
==========

Note that if you are going to use this on Kit Kat, the built in web view
does not support the streaming format. To get around this, the app launches a
web browser when you go to view the stream, giving you the normal web-view from
the server. *As such, support for Kit Kat may be dropped eventually*

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

Copyright and Licenses
======================
2018 - 2019, University of Michigan

All rights reserved.

### Libraries and Resources Used

Vector icon assets from Google under Apache License 2.0

[Flexible Adapter](https://github.com/davideas/FlexibleAdapter) from davidea
under Apache License 2.0

[Glide](https://github.com/bumptech/glide) from bumptech under Apache License 2.0, BSD and MIT

[Android Material Color Picker Dialog](https://github.com/Pes8/android-material-color-picker-dialog) from Pes8 under MIT

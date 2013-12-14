See also https://git.wikimedia.org/summary/apps%2Fandroid%2Fjava-mwapi.git


== Updating icons from SVG ==

Many of our icons are maintained as SVG originals, rasterized to PNG at the
various output resolutions via a script. This rasterization is not part of
the main build process, so needs to be re-run when adding new icons.

Ensure you have librsvg and the 'rsvg-convert' command:

* On Ubuntu, run "sudo apt-get install librsvg2-bin"
* On Mac OS X using Homebrew, run "brew install librsvg"

In "wikipedia" project subdirectory, run:
* "./convertify.bash"

Original files from icon-sources/*.svg are rendered and copied into the res/
subdirectories. Note that they are not automatically added to git!

== Steps to Get Up and Developing on a Mac ==

These instructions should help a Mac owner download the Wikipedia for Android source code and get the latest - sometimes not yet tested - version running in an emulator and on a real Android device. If you don't know anything about how to navigate around a UNIX/Linux/Mac filesystem and edit files with a program like pico, emacs, or vim, these instructions probably won't make any sense. And as noted below, you probably need to understand git in order to ever contribute source code updates to the new app (new release ~first quarter of 2014). And regarding source code, you will need to probably be pretty good at Java or iOS programming, but more ideally the Android-flavored Java used in Android development to really get down to work or play quickly. But anyway, here we go for those who enjoy a fun side project for a good cause...

1. Download the "ADT Bundle for Mac" from https://developer.android.com/sdk/index.html. This includes Eclipse. However, some other developers are currently using IntelliJ IDEA as their primary Android development IDE. Never hurts to have a couple of IDEs for different purposes.

2. Extract the ADT Bundle compressed file you just downloaded, into a folder of its own inside of /Applications/ (e.g., /Applications/adt/).

3. On a Mac, you'll want to update your ~/.bash_profile file to set the $ANDROID_HOME variable to point to the 'sdk' folder contained in the compressed file you just extracted and to update your system path to point at some binaries. 

export ANDROID_HOME=/Applications/adt/sdk
export PATH=$ANDROID_HOME/tools:$PATH

Now, any time you open a new Terminal, the $ANDROID_HOME variable will be set automatically and you'll be able to run command line tools pertinent to Android software development, such as the tool aptly named "android".

4. Speaking of which, in a new Terminal window, run the following command:

android sdk

This will load a GUI tool called "Android SDK Manager" for managing the Android-related packages necessary for Android programming.

5. The dependencies among the packages in order to support older and newer versions of Android devices can require some effort. But the following selection set is an okay starting point at first (later on you may need to add more stuff to test on different devices and properly compile the app).

Tools:
 Android SDK Tools (Rev 22.3)
 Android SDK Platform-tools (Rev 19)
 Android SDK Build-tools (Rev 19)
 Android SDK Build-tools (Rev 18.1)

Android 4.3 (API 18):
 SDK Platform (API 18, Rev 1)
 ARM EABI v7a System Image (API 18, Rev 1)

Android 4.2.2 (API 17):
 SDK Platform (API 17, Rev 2)

Extras:
 Android Support Repository (Rev 3)
 Android Support Library (Rev 19)

So, using Android SDK Manager, install that stuff, and once you're satisfied that everything has been downloaded and installed, feel free to close the Android SDK Manager.


6. Download and install Maven from http://maven.apache.org/download.cgi. Follow the instructions in the section "Unix-based Operating Systems (Linux, Solaris and Mac OS X)". Note again how the instructions talk about setting environment variables. Assuming, you created /usr/local/apache-maven and planted a folder in there called, for example, "maven-3.1.1", you would want to add the following to your ~/.bash_profile:

export M2_HOME=/usr/local/apache-maven/apache-maven-3.1.1
export M2=$M2_HOME/bin
export MAVEN_OPTS="-Xms256m -Xmx512m"
export PATH=$M2:$PATH

Maven relies upon Java. If in the Terminal you can type the command "java -version" and you yet you don't have a $JAVA_HOME environment variable, Maven will probably Just Work on Mac, so you don't necessarily need to add and export JAVA_HOME in ~/.bash_profile like the instructions suggest. If, on the other hand you don't have Java, download and install the latest version of the Java SDK in the 1.6 series (or if unsupported, maybe 1.7, ...) and ensure you can get "java -version" to work.

Now, in a new Terminal window, type the following command to ensure Maven is working.

mvn --version


7. In the parent directory of your programming projects, run the following commands:

git clone https://git.wikimedia.org/git/apps/android/java-mwapi.git
git clone https://git.wikimedia.org/git/apps/android/wikipedia.git

If you've never used git, you will need to search the web on how to add git to the system from Apple Xcode, and then study on how to use git (look for material on https://mediawiki.org for git convention as practiced by MediaWiki programmers).

Assuming the git clone operations worked, you will have two new folders representing the freshly cloned repositories, "java-mwapi" and "wikipedia".

8. Rename the folder for the "wikipedia" repository to ensure you know which OS it's for (there's an iOS app by the name of "wikipedia", too, so it's good to have different folder names at the root of the repos).

mv wikipedia/ android-wikipedia/

9. cd into the "java-mwapi" folder and run the following command:

mvn install

10. Install IntelliJ IDEA Community Edition from http://www.jetbrains.com/idea/download/index.html.

11. Open IntelliJ IDEA, and choose to Import Project. Select the "android-wikipedia" folder and click OK.

In the Import Project dialog box, allow the "Import project from external model" radio button be selected and "Maven" to be highlighted, then click Next.

The defaults on the next screen need one tweak: check the checkbox labeled "Import Maven Projects automatically". Then click Next.

On the next screen, there should be one checkbox checked for the project to import. Leave it as is, and then click Next.

In the next screen, on the lefthand pane, highlight the "Android API 18 Platform", ensure that Build Target of 4.3 and and Java SDK of 1.6 are set before clicking Next. If you didn't see the "Android API 18 Platform" option, you will probably need to (1) click the "+" symbol and choose "JDK" on this dialog box to first add the base SDK '1.6' value; IntelliJ kind of just figures out where the Java SDK is based for you, so don't be surprised if it has dug several folders deep...then (2) click the "+" symbol again choose "Android SDK", then navigate to the "sdk" folder of the ADT folder.

On the next screen accept the default of "wikipedia-parent" and click Finish.

12. Now wait - it can take a while! - IntelliJ IDEA will try to download project dependencies with its Maven integration. Sometimes you need to cd into the android-wikimedia folder do a mvn install to jumpstart the process, alternating back and forth to figure out the source of dependency problems and to get things downloaded.

13. Assuming all dependencies were downloaded cleanly, in IntelliJ IDEA go to Build > Make Project.

14. After a little while of trying to compile everything, IntelliJ will probably bark at you with something like the following:

cannot find symbol
symbol: class Javascriptinterface


You can double click on that error to see the part of the code causing problems.

15. To resolve this error, go to File > Project Structure, click on Modules, ensure the "wikipedia" module is highlighted in the middle pane, in the rightmost pane click on the "Dependencies" tab, then ensure that "Android API 18 Platform" is chosen for the "Module SDK". Now highlight the "wikipedia-it" module in the middle pane and set its "Module SDK" to "Android API 18 Platform" as well.

WARNING. Sometimes you'll get a build error about some "v4" library that seems to be clearly from Google. In that case, you'll want go to the bottom of the Dependencies tab click the "+" symbol and choose "Jars or libraries" for the each module. And in the file picker dialog box, navigate into the "sdk" folder of the ADT you added to the system earlier, then go into extras/android/support/v4/ and select "android-support-v4.jar" and then click OK and try building again. Search engines are your friend when these sorts of errors arise. Usually, if Maven stuff breaks down it is a problem someone has experienced...sometimes it's just that the downloads are brittle when your internet connection is unstable or slow.

16. If you're having a really good day, everything will have compiled neatly. There may be a few warnings, but no fatal errors.

Now, it's time to either run the app on an Android device or in an emulator. The emulator is slow due to being a full stack implementation. So most people seem to prefer to push apps to real Android devices.

17. To setup a configuration for a real Android device, first, on your Android device, go into Settings. Depending on your version of Android OS, you may need to take a different approach, but usually you can go to "About <phone/table>" and tap repeatedly on the "Build number" cell until it tells you that you've put the device in developer mode. Now that developer mode is turned on, from the Settings app go into "Developer options" and turn on USB debugging (you may want to turn this feature off later on when you don't need it). Now, connect your Android device to your Mac. You'll be prompted with a message about allowing USB debugging for a particular RSA key fingerprint. Click OK to that.

Next, in IntelliJ

A. In IntelliJ, go to Run > Edit Configurations.
B. Click the "+" symbol and choose Android Application.
C. Choose "Wikipedia" for the Module.
D. Change the Target Device from Emulator to USB Device.
E. Type a descriptive name in the Name field such as "Nexus 4 Physical Device".
F. Click OK

Now up in the top right part of IntelliJ you should see a green colored play button. Also if you go to the Run menu you should see an option to, for example "Run Nexus 4 Physical Device". Use either option to pop the app onto your device and run it! The Wikipedia apps uses an internet connection, so you'll want to probably turn on internet access before launching the app via the play button or run menu.

One of IntelliJ's standout features is debugging. If you want to do that, you just need to click the Debug (green bug) button. To set breakpoints in your code, as with other IDEs, click in the gutter to the left of the source code in the IDE and notice that a little red circle is added. For example, if you set a breakpoint in the first line of public CommunicationBridge(final WebView webView, final String baseURL) in CommunicationBridge.java -

this.webView = webView;

- you'll get a glimpse into what actually happens the moment after tapping on an article title from search results. With the debugger, you can step through the code one line at a time, jump over methods, manipulate variables to see what would happen, and so on. Refer to online documentation to learn more about how the debugger works.

18. Now, if you wanted to use the emulator, first you'd want to run the following command in Terminal:

android avd

This will launch the Android Virtual Device Manager application where you can create different virtual machines with specs representative of devices in the wild. Again, the emulator is slow, but it's nice to be able to see how things work out on different screen sizes when you don't have lots of devices handy. To create a device, just go to the Device Definitions tab of Android Virtual Device Manager, click on one of the profiles, and then click "Create AVD". And in the ensuing dialog box set the Target to, for example, Android 4.3 - API Level 18, then click OK. You'll now see the virtual machine in the Android Virtual Devices tab.

Note: you can also go to the Tools > Android > AVD Manager in IntelliJ to get to the same interface.

Now that you have an emulator virtual machine ready, you can go to Run > Edit Configurations in IntelliJ again, click the '+' symbol, choose Android Application,  set the Module to 'wikipedia', and ensure Emulator is chosen, choosing the preferred emulator from the list of eligible emulators, setting a descriptive Name, then clicking OK. Then click the Play button or use the Run menu to get it started. A window will ensue for you to verify the configuration, to which you click OK. Then wait a while for everything to fire up and enjoy the emulator.


19. Enjoy.

20. Help make it better!


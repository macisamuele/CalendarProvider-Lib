# CalendarProvider-Lib [ ![Download](https://api.bintray.com/packages/macisamuele/maven/calendarprovider-lib/images/download.svg) ](https://bintray.com/macisamuele/maven/calendarprovider-lib/_latestVersion)
An utility library for the simplification of calendar's and event's management on Android.

### Description
This library simplify the life of the Android Developers that have to manage the calendars provided by the Android Content Provider without the requirement to know the exact management of the events performed by Android.

### Usage
Two main classes are defined:
- `CalendarInfo` which contains a set of static methods to manage the calendars
- `EventInfo` which contains a set of static methods to manage the events

### Use into Android Studio
Add the repository to your `build.gradle` file
```gradle
repositories {
 maven {
  url  "http://dl.bintray.com/macisamuele/maven" 
 }
}
```
 and add the dependency to your module
 ```
 compile 'it.macisamuele:calendarprovider-lib:0.0.1'
 ```
You can also add manually the library in aar format, [download link](https://bintray.com/artifact/download/macisamuele/maven/it/macisamuele/calendarprovider-lib/0.0.1/calendarprovider-lib-0.0.1.aar) **not suggested**

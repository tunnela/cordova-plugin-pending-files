# cordova-plugin-pending-files

## Installation

```
cordova plugin add cordova-plugin-pending-files
```

It is also possible to install via repo url directly (unstable):

```
cordova plugin add https://github.com/tunnela/cordova-plugin-pending-files.git
```

## Why to use

Android uses intent to launch the native file picker activity on the device when selecting files, and on phones with low memory, the Cordova activity may be killed. In this scenario, the result from the plugin call will be delivered via the `resume` event of `document` element. See the [Android Lifecycle guide](https://developer.android.com/guide/components/activities/activity-lifecycle) for more information. 

## How to use

In the `resume` event, the `pendingResult.result.multiple` will tell if a multi-file picker was used. If the `multiple` is `true`, the `pendingResult.result.files` will contain the selected file URIs. If `multiple` is `false`, the `pendingResult.result.file` will contain the selected file URI. Check the `pendingResult.pluginStatus` to determine whether or not the call was successful.

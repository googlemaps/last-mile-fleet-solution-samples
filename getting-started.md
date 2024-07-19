# Getting Started

## Introduction

This document describes the setup and initial testing process for the LMFS
Sample Apps.

## Prerequisites

This document assumes the following:

-   Your workstation has the following pieces of software installed:

    -   Java SDK 17 (support for newer SDKs is work in progress)
    -   Gradle 8 (latest version)
    -   The `gcloud` command line utility for Google Cloud
        ([instructions](https://cloud.google.com/sdk/docs/install))

-   You have a working Google Cloud project that has been granted access for
    LMFS and related APIs, and followed the
    [Authentication and Authorization](https://developers.google.com/maps/documentation/transportation-logistics/last-mile-fleet-solution/shipment-tracking/fleet-engine/auth)
    guide. You should enable the following services on your Google Cloud project:

    -   [Local Rides and Deliveries API](https://console.cloud.google.com/apis/library/fleetengine.googleapis.com)
    -   Maps
        [SDK for Android](https://console.cloud.google.com/apis/library/maps-android-backend.googleapis.com),
        [SDK for iOS](https://console.cloud.google.com/apis/library/maps-ios-backend.googleapis.com)
        and
        [JavaScript API](https://console.cloud.google.com/apis/library/maps-backend.googleapis.com)
    -   [Navigation SDK](https://console.cloud.google.com/apis/library/navigationsdkusage.googleapis.com)
    -   [Identity and Access Management (IAM) API](https://console.cloud.google.com/apis/library/iam.googleapis.com)
        and
        [IAM Service Account Credentials API](https://console.cloud.google.com/apis/library/iamcredentials.googleapis.com)

-   You have created the following service accounts. Setup
    [instructions](https://developers.google.com/maps/documentation/transportation-logistics/last-mile-fleet-solution/shipment-tracking/fleet-engine/auth#creating_a_service_account).

    -   A Driver service account with the roles "Fleet Engine Delivery Trusted
        Driver" or "Fleet Engine Delivery Untrusted Driver" and "Service Account
        Token Creator"
    -   A Consumer service account with the roles "Fleet Engine Delivery
        Consumer" and "Service Account Token Creator"
    -   A Server service account with the roles "Fleet Engine Delivery Super
        User" and "Service Account Token Creator"
    -   A Fleet Reader service account with the roles "Fleet Engine Delivery
        Fleet Reader" and "Service Account Token Creator"

-   You have created API Keys. Setup
    [instructions](https://developers.google.com/maps/documentation/javascript/get-api-key).

This document requires the use of the following pieces of information:

-   `PROJECT_ID`: The Google Cloud project ID.
-   `ANDROID_API_KEY`: An API key that can be used from Android. This key should
    be able to access the Navigation and Maps SDKs for Android.
    -   Required only if you will build and run the Android application.
-   `IOS_API_KEY`: An API key that can be used from iOS. This key should
    be able to access the Navigation and Maps SDK for iOS.
    -   Required only if you will build and run the iOS application.
-   `JS_API_KEY`: An API key that can be used from web applications. This key
    should be able to access the Maps JavaScript API.
-   `DRIVER_SERVICE_ACCOUNT_EMAIL`: The email address of a service account that
    has the roles "Fleet Engine Delivery Driver" and "Service Account Token
    Creator".
-   `CONSUMER_SERVICE_ACCOUNT_EMAIL`: The email address of a service account
    that has the roles "Fleet Engine Delivery Consumer" and "Service Account
    Token Creator".
-   `SERVER_SERVICE_ACCOUNT_EMAIL`: The email address of a service account that
    has the roles "Fleet Engine Delivery Super User" and "Service Account Token
    Creator".
-   `FLEET_READER_ACCOUNT_EMAIL`: The email address of a service account that
    has the roles "Fleet Engine Fleet Reader" and "Service Account Token
    Creator".
-   `ANDROID_SDK_PATH`: The path to the installed Android SDK. For example,
    `/Users/<user>/Library/Android/sdk` on a Mac computer.
    -   Required only if you will build and run the Android application.

## Setup

### Step 1. Authenticate with `gcloud`

1.  Log into Google Cloud on your command line:

    ```sh
    $ gcloud auth login
    ```

    You will be prompted to sign into your Google account and grant access to
    the Google Cloud SDK. Accept and continue. The account you use must have the
    "Service Account Token Creator" role on this project.

1.  Enable your Google account to be used as Application Default Credentials.
    This should only be done in a testing scenario, not production!

    ```sh
    $ gcloud auth application-default login
    ```

1.  Set the project ID in your environment, replacing `PROJECT_ID` with your
    project ID:

    ```sh
    $ gcloud config set project PROJECT_ID
    ```

### Step 2. Initial setup, testing

1.  Obtain the source of the sample apps from GitHub:

    ```sh
    $ git clone https://github.com/googlemaps/last-mile-fleet-solution-samples
    ```

1.  Update the configuration by running the script below. The script will prompt
    you for settings used when setting up the Google Cloud project, and update
    the source files accordingly. For the list of settings, see the
    [Prerequisites](#prerequisites) section above.

    ```sh
    $ tools/update_configuration.sh
    ```

    This script updates some or all of the following files with the required configuration.

    -   `backend/src/main/resources/config.properties`
    -   `android_driverapp_samples/app/src/main/AndroidManifest.xml`
    -   `android_driverapp_samples/local.properties`
    -   `ios_driverapp_samples/LMFSDriverSampleApp/LocalOverrides/ApplicationDefaults.json`

    NOTE:
    [Google recommends](https://developers.google.com/maps/api-security-best-practices?hl=en#mobile-ws-static-web)
    handling your API keys carefully. You likely should set your source code
    control system to ignore these files to avoid checking in your API keys.

1.  Generate a gradle wrapper for the backend. You should only need to do this
    once.

    ```sh
    $ cd backend
    $ gradle wrapper
    ```

1.  Run the tests to verify that the configuration is correct. If it returns
    SUCCESS at the end, your system is functional. See
    [Common issues](#common-issues) for some possible errors.

    ```sh
    $ ./gradlew test
    ```

    At this point you have verified that the sources have been correctly set up,
    permissions are working, that you are able to sign Fleet Engine Java Web
    Tokens (JWTs) of various kinds, and that you can create `DeliveryVehicle`
    and `Task` entities in Fleet Engine.

## Launch the apps

### Start the backend host

Start the backend host first. By default, this runs on your workstation on port
8080.

```sh
$ cd backend
$ ./gradlew appengineRun
```

Open a browser to <http://localhost:8080/> and verify that the backend has
started.

Do not use `ctrl-c` to stop the backend host, as this may cause it to retain the
port and cause re-starts to fail.

To stop the backend, open another terminal window and run:

```sh
$ cd backend
$ ./gradlew appengineStop
```

### Load delivery configuration

1.  In a browser window, go to
    <http://localhost:8080/upload-backend-config.html>.

1.  Select the file you want to upload. You can use the example delivery
    configuration at `backend/src/test/resources/test.json`.

1.  After the delivery configuration is uploaded, the vehicle and task entities
    are created on Fleet Engine. You can examine the status of the requests via
    the web page.

Note: after you upload the delivery configuration, the vehicle, task and
tracking IDs of the created entities change format from how they are entered in
the configuration file.

Specifically, for an entity in the configuration file with ID of the form
`sample_id`, the actual ID of the correspondingly created Fleet Engine entity
will look like `sample_id_<timestamp-millis>`, where `<timestamp-millis>` is the
millisecond timestamp when the file was uploaded. This ensures that you can
upload the same delivery configuration file each time you restart the backend
without ID clashes between new entities and entities created in previous runs of
the backend. You should always refer to the IDs with timestamps when entering
them into the shipment tracking and fleet tracking web apps, as well as in API
calls to the backend. Using the IDs without timestamps results in an `entities
not found` error.

### Import and start the mobile apps

Follow the instructions in the [Android](android_driverapp_samples/README.md) or
[iOS](ios_driverapp_samples/README.md) driver app README file to import and
start a mobile app.

### Start the web app

Follow the instructions for the [web apps](web-apps.md) to perform shipment or
delivery vehicle tracking.

## Common issues

-   [The test](#step-2-initial-setup-testing) fails with errors indicating that
    tokens could not be signed.

    The tests are designed to exercise the token-signing credential set up.

    -   Verify that you have successfully authenticated against your Google
        Cloud project via the `gcloud` command, in
        [Step 1](#step-1-authenticate-with-gcloud) above.
    -   Verify that `backend/src/main/resources/config.properties` contain
        correct service account email addresses and the correct project ID.
    -   In the Google Cloud Console, verify that the service accounts have the
        correct roles. The roles are listed in the
        [Prerequisites](#prerequisites) section above.

Other common issues are listed in the
[Android](android_driverapp_samples/README.md#common-issues),
[iOS](ios_driverapp_samples/README.md#common-issues) and
[web](web-apps.md#common-issues) README files.

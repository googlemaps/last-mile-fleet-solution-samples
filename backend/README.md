# LMFS Sample Apps Backend

## Introduction

The LMFS sample backend is a reference implementation of the endpoints needed by
the LMFS Sample Apps. It provides endpoints used to ingest delivery scenarios,
create JSON Web Tokens (JWTs) needed by the sample apps to access Fleet Engine,
assign stops and tasks to vehicles, and facilitate vehicle and task updates.

For an overview of the system architecture, refer to the
[System Architecture section of the main README file](../README.md#system-architecture).

## Using the backend

To launch the backend, follow
[the steps in the Getting Started document](../getting-started.md#launch-the-apps).

## Web pages and apps

The sample backend ships with a few pages in `src/main/webapp/html/`. When the
backend is deployed, you can access these pages via `/<page-name>`. These pages
help start a deployment, and illustrate the use of the various web SDKs.

-   `/index.html`: lists all the available pages.
-   `/upload-delivery-config.html`: This page allows you to upload a delivery
    configuration file. The file is ingested by the `POST /delivery_config`
    endpoint, and used to populate the list of vehicles, stops and tasks.
-   `shipment_tracking.html`: This page contains a sample
    [JavaScript Shipment Tracking](https://developers.google.com/maps/documentation/transportation-logistics/last-mile-fleet-solution/shipment-tracking/how-to/track_shipment)
    app, and allows you to view the status of a particular shipment.
-   `fleet_tracking.html`: This page contains a sample
    [JavaScript Fleet Tracking](https://developers.google.com/maps/documentation/transportation-logistics/last-mile-fleet-solution/fleet-performance/how-to/track_fleet)
    app, and allows you to view the status of a delivery vehicle in a fleet.

The web apps are described in detail in a [separate document](../web-apps.md).

## API endpoints

The backend API exposes functionality to the sample mobile apps, as well as the
reference shipment tracking and delivery vehicle tracking integrations.

### Token issuance: `GET /token/:type/[:id]`

This endpoint creates a signed token used by a SDK identified by the token type.
Important: the sample backend implements **no** form of authentication on this
endpoint; all requests result in valid, unexpired tokens. Be careful to
implement an authentication scheme in your system to protect tokens from misuse.
See the
[Fleet Engine Authentication and Authorization guide](https://developers.google.com/maps/documentation/transportation-logistics/last-mile-fleet-solution/shipment-tracking/fleet-engine/auth).

This endpoint uses the
[Fleet Engine Authentication Library](https://github.com/googlemaps/java-fleetengine-auth).

**Request**

Params | Type   | Description
------ | ------ | ----------------------------------------------------------
`type` | enum   | The type of token to sign and return; required. Valid values are `delivery_driver`, `delivery_consumer`, and `fleet_reader`.
`id`   | string | The ID for which the token is to be created.<ul><li>If the type is `delivery_driver`, this must be the id of the delivery vehicle. <li>If the type is `delivery_consumer`, this must be the tracking ID of the package.<li>If the type is `fleet_reader`, this field must not be specified.</ul>

**Response**

The response is a JSON object with the following format:

```ts
{
  // The Unix timestamp at which the token was created (in milliseconds).
  creation_timestamp_ms: number,
  // The Unix timestamp at which time the token will expire (in milliseconds).
  expiration_timestamp_ms: number,
  // The token in base-64 encoded format.
  token: string,
};
```

### Upload delivery configuration: `POST /delivery_config`

Uploads the delivery configuration via multipart form data. Creates Fleet Engine
entities (vehicles, tasks).

**Request**

This request takes a body of multipart form data, consisting of the delivery
configuration file to be uploaded. See
[Delivery Configuration file](#delivery-configuration-file) for details.

**Response**

The response is plain-text output of the created entities.

### Manifest

#### `POST /manifest/[:vehicleId]`

Updates the manifest for the given vehicle ID. This endpoint is also used to
assign an unassigned manifest to a vehicle.

Note: this would ideally be done with the `PATCH` method, rather than `POST`.
However, the sample backend is implemented with
[Java’s HttpServlet](https://javaee.github.io/javaee-spec/javadocs/javax/servlet/http/HttpServlet.html)
which does not support `PATCH`. As such, for this operation, updates are
supported via `POST`.

**Request**

Params                          | Type            | Description
------------------------------- | --------------- | -----------
`vehicle_id`                    | string          | The ID of the delivery vehicle. Required, unless `<body>.client_id` is specified.
`<body>`                        | JSON object     | The field(s) to update.
`<body>.client_id`              | string          | The client ID requesting the manifest assignment.<p>If this field is specified, you must not specify any other field for update. <p>If this field is specified, `vehicle_id` can be omitted; the backend will select any available manifest for assignment. <p>If the client identified by `client_id` is already assigned a particular manifest, that manifest will be returned. <p>If the client identified by `client_id` is assigned a particular manifest *and* `vehicle_id` is specified, the backend verifies that the requested `vehicle_id` matches the assignment, then returns the manifest. An error is returned if a mismatch is found. Note that as part of the delivery configuration upload process, the backend generates `vehicle_id`s with timestamps.
`<body>.current_stop_state`     | enum            | Indicates one of three valid states for the next stop for the delivery vehicle. <p>Valid values are `STATE_UNSPECIFIED`, `NEW`, `ENROUTE`, and `ARRIVED`.
`<body>.remaining_stop_id_list` | List of stop ID | Use this field to update the list of remaining stops.<p>To mark a stop as closed, remove the corresponding stop ID from this list. Any stop IDs that are removed from the list are considered closed, and tasks associated with the stop will be marked `CLOSED`.<p>To reorder the sequence of stops that the driver will navigate through, reorder the IDs in this list.<p>Do not update this field while the vehicle is navigating to the next stop (i.e. the stop state is `ENROUTE`.)

**Response**

The response is the updated `DeliveryConfig.Manifest` for the delivery vehicle
with the given `vehicle_id`. See
[Delivery Configuration file](#delivery-configuration-file) for details.

#### `GET /manifest/:vehicle_id`

Returns the manifest for the given vehicle ID.

**Request**

Params       | Type   | Description
------------ | ------ | -----------------------------------------
`vehicle_id` | string | The ID of the delivery vehicle. Required.

**Response**

The response is a `DeliveryConfig.Manifest` for the given `vehicle_id`. See
[Delivery Configuration file](#delivery-configuration-file) for details.

### Task

#### `POST /task/:id`

Updates the task with the given ID.

Note: this would ideally be done with the `PATCH` method, rather than `POST`.
However, the sample backend is implemented with
[Java’s HttpServlet](https://javaee.github.io/javaee-spec/javadocs/javax/servlet/http/HttpServlet.html)
which does not support `PATCH`. As such, for this operation, updates are
supported via `POST`.

**Request**

Params                | Type        | Description
--------------------- | ----------- | -----------
`id`                  | string      | The task ID. Required.
`<body>`              | JSON object | The field(s) to update.
`<body>.task_outcome` | enum        | The outcome to which the task should be updated. Valid values are `TASK_OUTCOME_UNSPECIFIED`, `SUCCEEDED` and `FAILED`.

**Response**

The response is the updated `DeliveryConfig.Task` for the task with the given
`id`.

#### `GET /task/:id`

Returns the task with the given ID.

**Request**

Params | Type   | Description
------ | ------ | ----------------------
`id`   | string | The task ID. Required.

**Response**

The response is a `DeliveryConfig.Task` for the task with the given `id`.

#### `GET /tasks?vehicleId=:id`

Returns the tasks associated with the delivery vehicle with the given `id`.

**Request**

Params | Type   | Description
------ | ------ | ----------------------------------
`id`   | string | The delivery vehicle ID. Required.

**Response**

The response is a list of `DeliveryConfig.Task` objects, assigned to the
delivery vehicle with the given `id`.

#### `GET /taskInfoByTrackingId/:trackingId`

Returns manifest information for the task with the given Tracking ID.

**Request**

Params         | Type   | Description
-------------- | ------ | --------------------------
`trackingId`   | string | The tracking ID. Required.

**Response**

The response is a `DeliveryConfig.Task` for the task with the given
`trackingId`.

## Delivery configuration file

```js
// Unless specified with a ?, all fields are required.
// This is defined using TypeScript types for the sake of human readers (we don’t
// actually use this in TypeScript).

typedef TaskId = string;
typedef StopId = string;

// A point on the map
typedef Waypoint = {
  // Optional; a description of the location of the point. It could be an address
  // of the point, for example.
  description?: string,

  lat: number,
  lng: number,
};

typedef Task = {
{
  // The ID of the task. This ID must be unique across all tasks defined in the
  // same file.
  task_id: TaskId,

  // The consumer-facing, public tracking ID of the task. The combination of this
  // task's tracking ID and its type (task_type field below) must be unique
  // across all tasks defined in the same file. In other words, there cannot be
  // two or more tasks with tracking_id "xyz" and type "PICKUP".
  tracking_id: string,

  // The intended destination of the task.
  planned_waypoint: Waypoint,

  // The name of the contact person for this task (typically the addressee for a
  // delivery, or the sender for a pickup). This may not be populated for all
  // tasks.
  contact_name?: string,

  // Optional; the intended completion time of the task. If specified, it must be
  // an ISO 8601 date, e.g. 2021-05-01T15:00:00.000-07:00. The backend may
  // consider the task definition file to be invalid otherwise.
  planned_completion_time?: string,

  // Optional, and only used if planned_completion_time is specified above. The
  // time window when completion could occur, in seconds. For example, given the
  // time in planned_completion_time above, a value of 7200 would mean that
  // completion could take place between 15:00 and 17:00.
  planned_completion_time_range_seconds?: number,

  // Estimated time for accomplishing the task.
  duration_seconds: number,

  // The type of the task. For details on the different task types, refer to the
  // Delivery API user guide
  // (https://developers.google.com/maps/documentation/transportation-logistics/last-mile-fleet-solution/shipment-tracking/fleet-engine/deliveries_api#use_cases).
  task_type: 'PICKUP' | 'DELIVERY' | 'SCHEDULED_STOP' | 'UNAVAILABLE_TASK',

  // Optional; a description of the task.
  description?: string,
};

// The set of stops and tasks assigned to a single delivery vehicle.
typedef Manifest = {

  // Vehicle definition.
  vehicle: {
    // The ID of the vehicle. This ID must be unique across all vehicles defined in
    // the same file.
    vehicle_id: string,

    // The provider ID of the customer, which is the GCP project ID. Ignored if
    // specified in the delivery configuration file; will be filled in for
    // /manifest calls.
    provider_id: string,

    // (Optional) Where the vehicle is located initially for simulation.
    start_location?: Waypoint,
  },

  // (Optional) The client ID to which the manifest and the vehicle are currently
  // assigned if it has been assigned to a client. Setting this field on an initial
  // load indicates that this manifest should ONLY be assigned to a client who
  // supplies that client ID.
  client_id?: string,

  // The current state of the vehicle as it travels to the next stop. Setting this
  // field to 'ENROUTE' or 'ARRIVED' specifies that the vehicle is en-route or has
  // arrived at the first stop in remaining_stop_ids, respectively.
  // Setting this field on an initial load (as part of the json file) will be
  // ignored.
  current_stop_state?: 'STATE_UNSPECIFIED' | 'NEW' | 'ENROUTE' | 'ARRIVED',

  // The list of IDs of stops that the vehicle has not yet, but will travel
  // through, in order.
  // Each entry in stop_ids must be found in a stop below. When a vehicle finishes
  // serving one stop, update this field to remove the finished stop to begin
  // serving the next stop.
  // To rearrange the order of the remaining stops, rearrange this list of IDs.
  // Setting this field on an initial load (as part of the JSON file) will be
  // ignored. The initial sequence of stops is the sequence order given in the
  // stops field.
  remaining_stop_id_list?: Array<StopId>,

  // A list of tasks for the vehicle to complete, not necessarily in this order.
  // Each item in the array is of type ProviderConfig.Task.
  tasks: Array<Task>,

  // A list of stops for the vehicle to travel through, not necessarily in this
  // order.
  stops: Array<{

    // An ID used to uniquely identify the stop.
    stop_id: StopId,

    // One stop for the vehicle.
    planned_waypoint: Waypoint,

    // Multiple nearby tasks may be done at this stop, e.g. delivering many
    // packages to apartments in a building. Note that the tasks do not
    // necessarily need to be performed at this order.
    task_id_list: Array<TaskId>,
  }>
}

typedef DeliveryConfig = {
  // Optional; a description of the config.
  description: string,
  manifests: Array<Manifest>
};
```

/*
 * Copyright 2022 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/**
 * @fileoverview Demo for delivery vehicle tracking with Journey Sharing in Maps
 * JS API.
 */

/**
 * Callback that starts up the delivery vehicle tracking demo app after the Maps
 * JS API bootstrap has loaded.
 */
function initializeJourneySharing() {
  const optionsModal =
    new DeliveryVehicleTrackingOptionsModal('options-container');
  const app = DeliveryVehicleTrackingApp.createInstance(optionsModal);

  document.getElementById('show-options-button').onclick = (e) => {
    optionsModal.showOptionsModal();
  };

  document.getElementById('reset-button').onclick = (e) => {
    optionsModal.reset();
  };

  if (optionsModal.options.deliveryVehicleId) {
    document.getElementById('delivery-vehicle-id-input').value =
      optionsModal.options.deliveryVehicleId;
    document.getElementById('delivery-vehicle-text-field').classList.add(
      'mdc-text-field--label-floating');
    document.getElementById('delivery-vehicle-id-input-label').classList.add(
      'mdc-floating-label--float-above');
  }

  document.getElementById('eta-value').textContent = '';

  const startTracking = debounce((e) => {
    const deliveryVehicleId =
      document.getElementById('delivery-vehicle-id-input').value;
    if (deliveryVehicleId !== app.deliveryVehicleId) {
      optionsModal.options.deliveryVehicleId = deliveryVehicleId;
      optionsModal.saveToHistory();
      app.deliveryVehicleId = deliveryVehicleId;
    }
    e.preventDefault();
  }, 500);

  document.getElementById('tracking-button').onclick = startTracking;
  document.getElementById('delivery-vehicle-id-input').onkeyup = (e) => {
    if (e.key === 'Enter') {
      startTracking(e);
    }
  };

  app.start();
  optionsModal.setMapView(app.mapView);
}

class DeliveryVehicleTrackingApp {
  /**
   * @param {!DeliveryVehicleTrackingOptionsModal} optionsModal
   */
  constructor(optionsModal) {
    this.isLoadingResults_ = false;

    this.deliveryVehicleId_ = optionsModal.options.deliveryVehicleId;
    this.optionsModal = optionsModal;

    // map of latlng to task
    this.tasksMap = {};
    // map of latlng to task marker
    this.taskMarkersMap = {};
    // reference to the card on the map
    this.taskDetailsCard = null;
  }

  /**
   * Creates the singleton app instance
   *
   * @param {?DeliveryVehicleTrackingOptionsModal} optionsModal
   * @return {!DeliveryVehicleTrackingApp}
   */
  static createInstance(optionsModal) {
    this.instance = new DeliveryVehicleTrackingApp(optionsModal || {});
    return this.instance;
  }

  /**
   * Returns or creates the singleton app instance
   * @return {!DeliveryVehicleTrackingApp}
   */
  static getInstance() {
    if (!this.instance) {
      return DeliveryVehicleTrackingApp.createInstance();
    }

    return this.instance;
  }

  /**
   * Returns the tracking ID
   * @return {string} Tracking ID
   */
  get deliveryVehicleId() {
    return this.deliveryVehicleId_;
  }

  /**
   * Sets the tracking ID
   */
  set deliveryVehicleId(newDeliveryVehicleId) {
    if (this.deliveryVehicleId_ === newDeliveryVehicleId) {
      return;
    }

    this.resetDeliveryVehicleDetailsDisplay();
    this.isLoadingResults = !!newDeliveryVehicleId;

    this.deliveryVehicleId_ = newDeliveryVehicleId;
    this.locationProvider.deliveryVehicleId = newDeliveryVehicleId;
  }

  /**
   * Disables inputs and shows a loading message depending on the value of
   * `newIsLoadingResults`
   */
  set isLoadingResults(newIsLoadingResults) {
    if (this.isLoadingResults_ === newIsLoadingResults) {
      return;
    }

    this.isLoadingResults_ = newIsLoadingResults;
    setInputsDisabled(newIsLoadingResults);
    showHideElementById('loading', newIsLoadingResults);
  }

  /**
   * Creates a FleetEngineLocationProvider and JourneySharingMapView. Also
   * sets LocationProvider event listeners.
   */
  start() {
    this.locationProvider =
        new google.maps.journeySharing
            .FleetEngineDeliveryVehicleLocationProvider({
              projectId: PROJECT_ID,
              deliveryVehicleId: this.deliveryVehicleId_,
              authTokenFetcher: this.authTokenFetcher,
              pollingIntervalMillis:
                  this.optionsModal.options.pollingIntervalMillis,

              // Show all incomplete tasks as well as complete ones
              taskFilterOptions: {},
              shouldShowTasks: this.optionsModal.options.showTasks,
              shouldShowOutcomeLocations:
                  this.optionsModal.options.showOutcomeLocations,
              taskMarkerCustomization: (params) => {
                let icon = this.optionsModal.getIcon(
                    this.optionsModal.options.destinationIcon);
                if (params.task.outcome === 'SUCCEEDED') {
                  icon = this.optionsModal.getIcon(
                      this.optionsModal.options.successfulTaskIcon);
                } else if (params.task.outcome === 'FAILED') {
                  icon = this.optionsModal.getIcon(
                      this.optionsModal.options.unsuccessfulTaskIcon);
                }
                params.marker.setIcon(icon);
              },
              taskOutcomeMarkerCustomization: {
                icon: this.optionsModal.getIcon(
                    this.optionsModal.options.taskOutcomeIcon),
              },
              plannedStopMarkerCustomization: {
                icon: this.optionsModal.getIcon(
                    this.optionsModal.options.waypointIcon),
              },
              deliveryVehicleMarkerCustomization: {
                icon: this.optionsModal.getIcon(
                    this.optionsModal.options.vehicleIcon),
              },
            });

    const mapViewOptions = {
      element: document.getElementById('map_canvas'),
      locationProvider: this.locationProvider,
      anticipatedRoutePolylineSetup:
        { visible: this.optionsModal.options.showAnticipatedRoutePolyline },
      takenRoutePolylineSetup:
        { visible: this.optionsModal.options.showTakenRoutePolyline },
    };

    this.mapView =
      new google.maps.journeySharing.JourneySharingMapView(mapViewOptions);

    this.mapView.map.setOptions(
      { center: { lat: 37.424069, lng: -122.0916944 }, zoom: 14 });

    google.maps.event.addListenerOnce(this.mapView.map, 'tilesloaded', () => {
      setInputsDisabled(false);
    });

    this.locationProvider.addListener('update', e => {
      this.isLoadingResults = false;
      const vehicle = e.deliveryVehicle;
      const tasks = e.tasks;

      if (vehicle && vehicle.hasOwnProperty('name')) {
        this.vehicle_ = vehicle;
        showHideElementById('delivery-vehicle-id-error', false);

        document.getElementById('delivery-vehicle-id-value').textContent =
          vehicle.name;

        document.getElementById('stops-remaining-value').textContent =
          vehicle.remainingVehicleJourneySegments ?
            vehicle.remainingVehicleJourneySegments.length :
            '0';

        document.getElementById('navigation-status-value').textContent =
          vehicle.navigationStatus;

        document.getElementById('eta-value').textContent =
          `${vehicle.remainingDurationMillis / 1000}s`;

        document.getElementById('distance-value').textContent =
          `${vehicle.remainingDistanceMeters / 1000}km`;
      }

      if (tasks && tasks.length) {
        this.tasks_ = tasks;
        document.getElementById('completed-tasks-value').textContent =
          tasks.map((t) => t.name).sort().join('\n');
      }

      this.updateData();
      this.updateTaskTable();
      this.updateMarkers();
      if (this.taskDetailsCard != null && this.highlightedTask_ != null) {
        this.refreshTaskDetail(this.highlightedTask_);
      }
    });

    this.locationProvider.addListener('error', e => {
      console.error(e);
      const error = e.error;

      this.isLoadingResults = false;

      document.getElementById('delivery-vehicle-id-error').textContent =
        `Error: ${error.message}`;
      showHideElementById('delivery-vehicle-id-error', true);
    });
  }

  /**
   * Resets DOM elements and restarts the delivery vehicle tracking demo app.
   */
  restart() {
    setInputsDisabled(true);
    this.resetErrorDisplay();
    this.resetDeliveryVehicleDetailsDisplay();
    this.start();
  }

  /**
   * Resets the DOM elements that display delivery vehicles details.
   */
  resetDeliveryVehicleDetailsDisplay() {
    this.resetErrorDisplay();

    document.getElementById('delivery-vehicle-id-value').textContent = '';
    document.getElementById('stops-remaining-value').textContent = '';
    document.getElementById('navigation-status-value').textContent = '';
    document.getElementById('eta-value').textContent = '';
    document.getElementById('distance-value').textContent = '';
    document.getElementById('completed-tasks-value').textContent = '';
  }

  /**
   * Resets the error message display
   */
  resetErrorDisplay() {
    document.getElementById('delivery-vehicle-id-error').textContent = '';
    showHideElementById('delivery-vehicle-id-error', false);
  }

  plannedTaskLatLng(t) {
    return `${t.plannedLocation.lat},${t.plannedLocation.lng}`;
  }

  markerLatLng(m) {
    return `${m.position.lat()},${m.position.lng()}`;
  }

  badgeName(task) {
    if (task.outcome === 'SUCCEEDED') {
      return 'success';
    } else if (task.outcome === 'FAILED') {
      return 'attempted';
    } else if (task.status === 'OPEN') {
      return 'open';
    } else {
      return 'unknown';
    }
  }

  updateData() {
    if (!this.tasks_) {
      return;
    }
    for (const task of this.tasks_) {
      this.tasksMap[this.plannedTaskLatLng(task)] = task;
    }
    for (const marker of this.mapView.unsuccessfulTaskMarkers) {
      if (this.markerLatLng(marker) in this.tasksMap) {
        this.taskMarkersMap[this.markerLatLng(marker)] = marker;
      }
    }
  }

  updateTaskTable() {
    if (!this.vehicle_ || !this.tasks_) {
      return;
    }

    // Build a map of tasks.
    const taskIdsMap = {};
    for (const t of this.tasks_) {
      taskIdsMap[t.name.split('/').pop()] = t;
    }

    let taskCounter = 0;
    document.getElementById('task-table-body').innerHTML = '';
    this.taskIndex_ = {};

    const typeString = (task) => {
      if (task.type === 'DELIVERY') {
        return 'Delivery';
      } else if (task.type === 'PICKUP') {
        return 'Pick up';
      } else {
        return 'Other';
      }
    };

    const statusIcon = (task) => {
      if (task.outcome === 'SUCCEEDED') {
        return '<span class="material-icons" style="color:#1E8E3E;">check_circle</span>';
      } else if (task.outcome === 'FAILED') {
        return '<span class="material-icons" style="color:#F29900;">error</span>';
      } else if (task.status === 'OPEN') {
        return '<span class="material-icons-outlined" style="color:#3B78E7;">arrow_circle_right</span>';
      } else {
        return '<span class="material-icons" style="color:#80868B;">help</span>';
      }
    };


    // sort the tasks with outcomes by outcome time
    const tasksWithOutcomes = this.tasks_.filter(
        task => task.outcome === 'SUCCEEDED' || task.outcome === 'FAILED');
    tasksWithOutcomes.sort((t1, t2) => t1.name > t2.name ? 1 : -1);

    // Tasks with outcomes.
    for (const task of tasksWithOutcomes) {
      taskCounter++;
      const taskId = task.name.split('/').pop();
      this.taskIndex_[taskId] = taskCounter;
      const n = document.getElementById('table-row-template').cloneNode(true);
      n.id = `task-row-${task.id}`;
      n.getElementsByClassName('stop_index')[0].innerText = `${taskCounter}`;
      n.getElementsByClassName('tracking_id')[0].innerText = task.trackingId;
      n.getElementsByClassName('type')[0].innerText = typeString(task);
      n.getElementsByClassName('status')[0].innerHTML = statusIcon(task);

      const outcomeTime = new Date(task.outcomeTime);
      let hours = outcomeTime.getHours();
      const ampm = hours >= 12 ? 'pm' : 'am';
      hours = (hours === 0 ? 12 : (hours > 12 ? hours - 12 : hours));
      let minutes = outcomeTime.getMinutes();
      if (minutes < 10) {
        minutes = '0' + minutes;
      }
      const timeString = `${hours}:${minutes} ${ampm}`;
      n.getElementsByClassName('time')[0].innerText = timeString;
      n.style.display = "table-row";
      if (this.highlightedTask_ != null && this.highlightedTask_.name.split('/').pop() == taskId) {
        n.classList.add('highlight');
      }
      n.onclick = () => { this.showTaskDetail(task); };
      document.getElementById('task-table-body').appendChild(n);
    }

    // Tasks without outcomes.
    for (const vjs of this.vehicle_.remainingVehicleJourneySegments) {
      for (const t of vjs.stop.tasks) {
        const task = taskIdsMap[t.id];
        if (task.outcome != null) {
          continue;
        }

        taskCounter++;
        this.taskIndex_[t.id] = taskCounter;
        const n = document.getElementById('table-row-template').cloneNode(true);
        n.id = `task-row-${t.id}`;
        n.getElementsByClassName('stop_index')[0].innerText = `${taskCounter}`;
        n.getElementsByClassName('tracking_id')[0].innerText = task.trackingId;
        n.getElementsByClassName('type')[0].innerText = typeString(task);
        n.getElementsByClassName('status')[0].innerHTML = statusIcon(task);
        n.getElementsByClassName('time')[0].innerText = '-';
        n.style.display = "table-row";
        if (this.highlightedTask_ != null && this.highlightedTask_.name.split('/').pop() == t.id) {
          n.classList.add('highlight');
        }
        n.onclick = () => { this.showTaskDetail(task); };
        document.getElementById('task-table-body').appendChild(n);
      }
    }
  }

  updateMarkers() {
    if (this.taskIndex_ == null) {
      return;
    }

    for (const marker of this.mapView.unsuccessfulTaskMarkers) {
      if (this.markerLatLng(marker) in this.tasksMap) {
        const task = this.tasksMap[this.markerLatLng(marker)];
        const markerSymbol = marker.getIcon();

        // Customize the marker symbol.
        markerSymbol.path =
            'M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7';
        markerSymbol.labelOrigin = { x: 12, y: 10 };

        const markerIndex = this.taskIndex_[task.name.split('/').pop()];
        marker.setLabel({
          text: `${markerIndex}`,
        });
        google.maps.event.clearListeners(marker, 'click');
        google.maps.event.addListener(marker, 'click', () => {
          this.showTaskDetail(task);
        });
      }
    }

    for (const marker of this.mapView.successfulTaskMarkers) {
      if (this.markerLatLng(marker) in this.tasksMap) {
        const task = this.tasksMap[this.markerLatLng(marker)];
        google.maps.event.clearListeners(marker, 'click');
        google.maps.event.addListener(marker, 'click', () => {
          this.showTaskDetail(task);
        })
      }
    }
  }

  showTaskDetail(task) {
    if (task == null) {
      return;
    }
    const marker = this.taskMarkersMap[this.plannedTaskLatLng(task)];

    this.resetTaskDetail();
    this.highlightedMarker_ = marker;
    this.highlightedTask_ = task;
    console.log(task, marker);
    const symbol = marker.getIcon();
    symbol.strokeWeight = 3;
    marker.setIcon(symbol);

    // Create a card with task details, and attach it to the map.
    this.taskDetailsCard = document.getElementById('task-details-popup-template').cloneNode(true);
    this.refreshTaskDetail(task);
    this.mapView.map.controls[google.maps.ControlPosition.TOP_LEFT].push(this.taskDetailsCard);

    document.getElementById(`task-row-${task.name.split('/').pop()}`).classList.add('highlight');
  }

  refreshTaskDetail(task) {
    const taskId = task.name.split('/').pop();
    this.taskDetailsCard.getElementsByClassName('task-index')[0].innerText = `${this.taskIndex_[taskId]}`;
    this.taskDetailsCard.getElementsByClassName('task-status')[0].innerHTML = `<span class="badge ${this.badgeName(task)}-badge"></span>`;
    this.taskDetailsCard.getElementsByClassName('tracking-id')[0].innerText = task.trackingId;
    this.taskDetailsCard.style.display = 'block';

    // Fetch additional data from the manifest endpoint.
    fetch(`/task/${taskId}?manifestDataRequested=true`)
      .then((response) => response.json())
      .then((d) => {
        if (d == null) {
          this.taskDetailsCard.getElementsByClassName('address')[0].innerText = '-';
          return;
        }
        if (d['planned_waypoint']['description'] != null) {
          this.taskDetailsCard.getElementsByClassName('address')[0].innerText = d['planned_waypoint']['description'];
        } else {
          this.taskDetailsCard.getElementsByClassName('address')[0].innerText = '-';
        }
      });
  }

  resetTaskDetail() {
    if (this.highlightedMarker_ != null) {
      const symbol = this.highlightedMarker_.getIcon();
      symbol.strokeWeight = 1;
      this.highlightedMarker_.setIcon(symbol);
    }
    this.highlightedMarker_ = null;

    if (this.mapView.map.controls[google.maps.ControlPosition.TOP_LEFT].length > 0) {
      this.mapView.map.controls[google.maps.ControlPosition.TOP_LEFT].pop();
    }

    for (const row of document.getElementsByClassName('task-table-row')) {
      row.classList.remove('highlight');
    }
    this.highlightedTask_ = null;
    this.taskDetailsCard = null;
  }

  /**
   * Fetcher to get auth tokens from backend.
   */
  async authTokenFetcher(options) {
    const url = `${BACKEND_HOST}/token/fleet_reader`;
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(response.statusText);
    }
    const data = await response.json();
    const expiresInSeconds = Math.floor((data.expiration_timestamp_ms - Date.now()) / 1000);
    if (expiresInSeconds < 0) {
      throw new Error('Auth token already expired');
    }
    return {
      token: data.token,
      expiresInSeconds: data.expiration_timestamp_ms - Date.now(),
    };
  }
}

/**
 * Returns a function that will fire once at the start of [wait] ms. The
 * function will not fire again for subsequent clicks within [wait] ms of each
 * other.
 *
 * @param {!Function} func The function to debounce
 * @param {number} wait The number of ms to wait for
 * @return {!Function} The debounced function
 */
function debounce(func, wait) {
  let timeout;
  return function () {
    const context = this;
    const args = arguments;
    const callNow = !timeout;
    clearTimeout(timeout);
    timeout = setTimeout(() => timeout = null, wait);
    if (callNow) {
      func.apply(context, args);
    }
  };
}

/**
 * Sets the tracking id input field and button to disabled or enabled
 *
 * @param {boolean} disabled Whether or not to disable the inputs
 */
function setInputsDisabled(disabled) {
  if (disabled) {
    document.getElementById('delivery-vehicle-id-input')
      .setAttribute('disabled', true);
    document.getElementById('tracking-button').setAttribute('disabled', true);
  } else {
    document.getElementById('delivery-vehicle-id-input')
      .removeAttribute('disabled');
    document.getElementById('tracking-button').removeAttribute('disabled');
  }
}

/**
 * Shows or hides the DOM element with the given id
 *
 * @param {string} id
 * @param {boolean} show
 */
function showHideElementById(id, show) {
  const el = document.getElementById(id);
  if (el) {
    el.style.display = show ? 'block' : 'none';
  }
}

/**
 * Switches the tab to show either the task details or the vehicle details.
 *
 * @param {number} tabIndex 0 for tasks, 1 for details.
 */
function switchTab(tabIndex) {
  if (tabIndex === 0) {
    document.querySelector('.shipments-table-container').style.display = 'flex';
    document.querySelector('.shipment-details-table').style.display = 'none';
  }
  else if (tabIndex === 1) {
    document.querySelector('.shipments-table-container').style.display = 'none';
    document.querySelector('.shipment-details-table').style.display = 'table';
  }
}

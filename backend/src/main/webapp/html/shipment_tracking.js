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
 * @fileoverview Demo for shipment tracking with JourneySharing in Maps JS API
 */

/**
 * Callback that starts up the shipment tracking demo app after the Maps JS API
 * bootstrap has loaded.
 */
function initializeJourneySharing() {
  const optionsModal = new ShipmentTrackingOptionsModal('options-container');
  const app = ShipmentTrackingApp.createInstance(optionsModal);

  document.getElementById('show-options-button').onclick = (e) => {
    optionsModal.showOptionsModal();
  };

  document.getElementById('reset-button').onclick = (e) => {
    optionsModal.reset();
  };

  const menu = document.getElementById('menu');
  for (const button of document.getElementsByClassName('menu-button')) {
    button.onclick = (e) => {
      if (menu.style.display === 'none') {
        menu.style.display = 'block';
      } else {
        menu.style.display = 'none';
      }
    };
  }

  document.getElementById('tracking-id-input').value =
      optionsModal.options.trackingId || '';

  document.getElementById('eta-value').textContent = '';

  const startTracking = debounce((e) => {
    const trackingId = document.getElementById('tracking-id-input').value;
    if (trackingId !== app.trackingId) {
      optionsModal.options.trackingId = trackingId;
      optionsModal.saveToHistory();
      app.trackingId = trackingId;
    }
    e.preventDefault();
  }, 500);

  document.getElementById('tracking-button').onclick = startTracking;
  document.getElementById('tracking-id-input').onkeyup = (e) => {
    if (e.key === 'Enter') {
      startTracking(e);
    }
  };

  app.start();
  optionsModal.setMapView(app.mapView);
}

class ShipmentTrackingApp {
  /**
   * @param {!ShipmentTrackingOptionsModal} optionsModal
   */
  constructor(optionsModal) {
    this.isLoadingResults_ = false;

    this.trackingId_ = optionsModal.options.trackingId;
    this.optionsModal = optionsModal;
  }

  /**
   * Creates the singleton app instance
   *
   * @param {?ShipmentTrackingOptionsModal} optionsModal
   * @return {!ShipmentTrackingApp}
   */
  static createInstance(optionsModal) {
    this.instance = new ShipmentTrackingApp(optionsModal || {});
    return this.instance;
  }

  /**
   * Returns or creates the singleton app instance
   * @return {!ShipmentTrackingApp}
   */
  static getInstance() {
    if (!this.instance) {
      return ShipmentTrackingApp.createInstance();
    }

    return this.instance;
  }

  /**
   * Returns the tracking ID
   * @return {string} Tracking ID
   */
  get trackingId() {
    return this.trackingId_;
  }

  /**
   * Sets the tracking ID
   */
  set trackingId(newTrackingId) {
    if (this.trackingId_ === newTrackingId) {
      return;
    }

    this.resetShipmentDetailsDisplay();
    this.isLoadingResults = !!newTrackingId;

    this.trackingId_ = newTrackingId;
    this.locationProvider.trackingId = newTrackingId;
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
        new google.maps.journeySharing.FleetEngineShipmentLocationProvider({
          projectId: PROJECT_ID,
          authTokenFetcher: this.authTokenFetcher,
          trackingId: this.trackingId_,
          pollingIntervalMillis:
              this.optionsModal.options.pollingIntervalMillis,
          deliveryVehicleMarkerCustomization: {
            icon: this.optionsModal.getIcon(
                this.optionsModal.options.vehicleIcon),
          },
          destinationMarkerCustomization: {
            icon: this.optionsModal.getIcon(
                this.optionsModal.options.destinationIcon),
          },
        });

    const mapViewOptions = {
      element: document.getElementById('map_canvas'),
      locationProvider: this.locationProvider,
      anticipatedRoutePolylineSetup:
          {visible: this.optionsModal.options.showAnticipatedRoutePolyline},
      takenRoutePolylineSetup:
          {visible: this.optionsModal.options.showTakenRoutePolyline},
    };

    this.mapView =
        new google.maps.journeySharing.JourneySharingMapView(mapViewOptions);

    this.mapView.map.setOptions(
        {center: {lat: 37.424069, lng: -122.0916944}, zoom: 14});

    google.maps.event.addListenerOnce(this.mapView.map, 'tilesloaded', () => {
      setInputsDisabled(false);
    });

    this.locationProvider.addListener('update', e => {
      const taskTrackingInfo = e.taskTrackingInfo;
      console.log(taskTrackingInfo);
      this.isLoadingResults = false;

      if (!taskTrackingInfo) {
        return;
      }

      if (!taskTrackingInfo.hasOwnProperty('state')) {
        document.getElementById('tracking-id-error').textContent =
            `No shipment found for tracking id '${this.trackingId_}'`;
        showHideElementById('tracking-id-error', true);
        return;
      }

      document.getElementById('details-container').style.display = 'block';

      // Tracking ID
      document.getElementById('tracking-id-value').textContent =
          this.trackingId_;

      // Task status
      document.getElementById('task-status-value').textContent =
          taskTrackingInfo.state;

      // Task outcome
      document.getElementById('task-outcome-value').textContent =
          taskTrackingInfo.taskOutcome;

      if (taskTrackingInfo.state === 'CLOSED') {
        document.getElementById('stops-remaining-value').textContent = '';
        if (taskTrackingInfo.outcome === 'SUCCEEDED') {
          document.getElementById('stops-count').innerText = 'Completed';
        } else {
          document.getElementById('stops-count').innerText = 'Attempted';
        }
      } else {
        document.getElementById('stops-remaining-value').textContent =
            `${taskTrackingInfo.remainingStopCount}`;
        if (taskTrackingInfo.remainingStopCount >= 2) {
          document.getElementById('stops-count').innerText =
          `${taskTrackingInfo.remainingStopCount} stops away`;
        } else if (taskTrackingInfo.remainingStopCount === 1) {
          document.getElementById('stops-remaining-value').textContent = '';
          if (taskTrackingInfo.taskOutcome === 'SUCCEEDED') {
            document.getElementById('stops-count').innerText = 'Completed';
          } else if (taskTrackingInfo.taskOutcome === 'FAILED') {
            document.getElementById('stops-count').innerText = 'Attempted';
          } else {
            document.getElementById('stops-count').innerText =
                `You are the next stop`;
          }
        }
      }

      // ETA
      document.getElementById('eta-value').textContent =
          taskTrackingInfo.estimatedTaskCompletionTime;

      // Fetch data from manifest
      const taskId = taskTrackingInfo.name.split('/').pop();
      fetch(`/taskInfoByTrackingId/${taskId}`)
          .then((response) => response.json())
          .then((d) => {
            if (d == null || d['status'] === 404) {
              document.getElementById('eta-time').innerText = 'n/a';
              document.getElementById('address').innerText = 'n/a';
              return;
            }
            if (d['planned_completion_time'] != '') {
              const completionTime = new Date(d['planned_completion_time']);
              let timeString = getTimeString(completionTime);
              if (d['planned_completion_time_range_seconds'] > 0) {
                timeString = timeString + ' - ' +
                    getTimeString(new Date(
                        completionTime.getTime() +
                        d['planned_completion_time_range_seconds'] * 1000));
              }
              document.getElementById('eta-time').innerText = timeString;
            } else {
              document.getElementById('eta-time').innerText = 'n/a';
            }
            if (d['planned_waypoint']['description'] != null) {
              document.getElementById('address').innerText =
                  d['planned_waypoint']['description'];
            } else {
              document.getElementById('address').innerText = 'n/a';
            }
          });
    });

    this.locationProvider.addListener('error', e => {
      console.error(e);
      const error = e.error;

      this.isLoadingResults = false;

      document.getElementById('tracking-id-error').textContent =
          `Error: ${error.message}`;
      showHideElementById('tracking-id-error', true);
    });
  }

  /**
   * Resets DOM elements and restarts the shipment tracking demo app.
   */
  restart() {
    setInputsDisabled(true);
    this.resetErrorDisplay();
    this.resetShipmentDetailsDisplay();
    this.start();
  }

  /**
   * Resets the DOM elements that display shipment details.
   */
  resetShipmentDetailsDisplay() {
    this.resetErrorDisplay();

    document.getElementById('tracking-id-value').textContent = '';
    document.getElementById('task-status-value').textContent = '';
    document.getElementById('task-outcome-value').textContent = '';
    document.getElementById('stops-remaining-value').textContent = '';
    document.getElementById('eta-value').textContent = '';
  }

  /**
   * Resets the error message display
   */
  resetErrorDisplay() {
    document.getElementById('tracking-id-error').textContent = '';
    showHideElementById('tracking-id-error', false);
  }

  /**
   * Fetcher to get auth tokens from backend.
   */
  async authTokenFetcher(options) {
    const url =
        `${BACKEND_HOST}/token/delivery_consumer/${options.context.trackingId}`;
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(response.statusText);
    }
    const data = await response.json();
    const expiresInSeconds =
        Math.floor((data.expiration_timestamp_ms - Date.now()) / 1000);
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
  return function() {
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
    document.getElementById('tracking-id-input').setAttribute('disabled', true);
    document.getElementById('tracking-button').setAttribute('disabled', true);
  } else {
    document.getElementById('tracking-id-input').removeAttribute('disabled');
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
 * Formats a Date object for display.
 * @param {!Date} date
 * @return {string} The formatted time string.
 */
function getTimeString(date) {
  let hours = date.getHours();
  const ampm = hours >= 12 ? 'pm' : 'am';
  hours = (hours === 0 ? 12 : (hours > 12 ? hours - 12 : hours));
  let minutes = date.getMinutes();
  if (minutes < 10) {
    minutes = '0' + minutes;
  }
  return `${hours}:${minutes} ${ampm}`;
}

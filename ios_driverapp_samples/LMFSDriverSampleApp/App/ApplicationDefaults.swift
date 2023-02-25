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

import Foundation
import UIKit

/// This enum holds persistent app settings.
///
/// When getting a value, the value may come from one of three sources:
/// * If in a previous run of the application, an explicit value was set, that value will be
///   returned.
/// * If there is an override file in LocalOverrides/ApplicationDefaults.json, and the type
///   provided in that file is compatible with the type declared for the value, that value will be
///   returned.
/// * Otherwise the programmatic default value listed below will be returned.
enum ApplicationDefaults {
  /// The Google Maps API Key matching the bundleID of this app.
  static let apiKey: Value<String> = Value(
    key: "apiKey",
    defaultValue: "***This value must be configured, either in code or via a local override***")

  /// The URL for requests to the backend.
  static let backendBaseURLString: Value<String> = Value(
    key: "backendBaseURL",
    defaultValue: "http://localhost:8080")

  /// Whether we should disable using simulated location when navigation.
  ///
  /// If this is disabled, the real device location will be used.
  static let disableLocationSimulation: Value<Bool> = Value(
    key: "disableLocationSimulation",
    defaultValue: false)

  /// Whether we should disable reporting location via DriverSDK.
  static let disableLocationReporting: Value<Bool> = Value(
    key: "disableLocationReporting",
    defaultValue: false)

  /// Client ID to use when contacting the backend.
  static let clientId: Value<String> = Value(
    key: "clientId",
    /// Try to use the vendor ID as it is stable across app launches; use UUID as a backup if
    /// vendor ID is not available.
    defaultValue: UIDevice.current.identifierForVendor?.uuidString ?? NSUUID().uuidString)

  /// Instances of this class represent a particular value persisted via a UserDefaults object.
  ///
  /// T must be a class which is representable in UserDefaults; see Apple documentation.
  class Value<T> {
    /// The key for storing this value in userDefaults.
    private let key: String
    /// The UserDefaults instance to persist this value in.
    private let defaults: UserDefaults

    init(key: String, defaults: UserDefaults = .standard, defaultValue: T) {
      self.key = key
      self.defaults = defaults

      // See if the default of this value was overridden in a JSON file.
      var localDefaultValue = defaultValue
      if let localOverrideValue = localOverrides[key] {
        if let typedLocalOverrideValue = localOverrideValue as? T {
          localDefaultValue = typedLocalOverrideValue
        } else {
          // Error just prints, since this is an error in the local overrides file placed by the
          // programmer.
          print("Local override for key \(key) was of type \(type(of:localOverrideValue)) " +
              "but expected type was \(type(of:defaultValue)).")
        }
      }

      // Now establish this default at the UserDefaults level.
      defaults.register(defaults: [key: localDefaultValue])
    }

    /// The value is available via this property. It will automatically persist any values set in
    /// UserDefaults.
    var value: T {
      get {
        /// Because we registered a default value for this key, .object should never return nil.
        return self.defaults.object(forKey: self.key) as! T
      }
      set(newValue) {
        self.defaults.set(newValue, forKey: self.key)
      }
    }
  }

  /// Dictionary of locally-set overrides for default values.
  ///
  /// See the README.md file in the LocalOverrides directory for more information on the
  /// LocalOverrides mechanism.
  private static let localOverrides: [String: Any] = readLocalOverrides()

  /// Static function to read a JSON-format file that allows locally overriding the default values
  /// for any application defaults setting.
  private static func readLocalOverrides() -> [String: Any] {
    let defaultsOverridesURL = NSURL.fileURL(withPath: Bundle.main.resourcePath!)
        .appendingPathComponent("LocalOverrides", isDirectory: true)
        .appendingPathComponent("ApplicationDefaults.json", isDirectory: false)
    do {
      let contents = try Data(contentsOf: defaultsOverridesURL)
      if let dict = try JSONSerialization.jsonObject(with: contents) as? [String: Any] {
         return dict
      }

    } catch {
    }
    return [:]
  }
}

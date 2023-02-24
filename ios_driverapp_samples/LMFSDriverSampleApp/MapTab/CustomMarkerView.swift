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
import GoogleMaps
import SwiftUI
import UIKit

/// A custom view which can be used to provide map markers that have a pin graphic and a textual
/// label inside the pin.
class CustomMarkerView: UIView {
  let pinLabel: String?
  let image: UIImage?

  init(imageName: String, pinLabel: String?) {
    self.image = UIImage(named: imageName)
    self.pinLabel = pinLabel
    super.init(
      frame: CGRect(origin: CGPoint(x: 0, y: 0), size: image?.size ?? CGSize(width: 10, height: 10))
    )
    setupViews()
  }

  private func setupViews() {
    let imageView = UIImageView(image: image)
    imageView.translatesAutoresizingMaskIntoConstraints = false
    addSubview(imageView)
    imageView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
    imageView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
    imageView.topAnchor.constraint(equalTo: topAnchor).isActive = true
    imageView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    imageView.clipsToBounds = true

    if pinLabel != nil {
      let label = UILabel()
      label.translatesAutoresizingMaskIntoConstraints = false
      self.insertSubview(label, aboveSubview: imageView)
      label.textColor = .white
      label.text = pinLabel
      label.font = .systemFont(ofSize: 14, weight: .regular)
      label.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
      label.topAnchor.constraint(equalTo: imageView.topAnchor, constant: 6).isActive = true
    }
  }

  required init?(coder aDecoder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }
}

struct CustomMarker {
  /// Helper function to create the marker for a ModelData.Stop.
  ///
  /// The userData of the returned marker will be the stopId for the given Stop.
  static func makeMarker(
    stop: ModelData.Stop,
    showLabel: Bool = true
  ) -> GMSMarker {
    let waypoint = stop.stopInfo.plannedWaypoint
    let position = CLLocationCoordinate2D(latitude: waypoint.lat, longitude: waypoint.lng)
    let marker = GMSMarker()
    let customMarker = CustomMarkerView(
      imageName: "deep_blue_pin",
      pinLabel: showLabel ? String(stop.order) : nil)
    marker.iconView = customMarker
    marker.position = position
    marker.infoWindowAnchor = CGPoint(x: 0.5, y: 0)
    marker.zIndex = Int32(stop.order)
    marker.userData = stop.stopInfo.stopId
    return marker
  }

  /// Helper function to create the marker for a ModelData.Task.
  ///
  /// The userData for the returned marker will be the taskId for the given Task.
  static func makeMarker(task: ModelData.Task, showLabel: Bool = true) -> GMSMarker {
    let waypoint = task.taskInfo.plannedWaypoint
    let position = CLLocationCoordinate2D(latitude: waypoint.lat, longitude: waypoint.lng)
    let marker = GMSMarker()
    let imageName = task.taskInfo.taskType == .DELIVERY ? "red_pin" : "pink_pin"
    let customMarker = CustomMarkerView(
      imageName: imageName,
      pinLabel: showLabel ? String(task.sequenceString) : nil)
    marker.iconView = customMarker
    marker.position = position
    marker.infoWindowAnchor = CGPoint(x: 0.5, y: 0)
    marker.zIndex = Int32(task.sequence)
    marker.userData = task.taskInfo.taskId
    return marker
  }

}

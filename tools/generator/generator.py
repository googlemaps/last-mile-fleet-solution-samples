"""A utility to generate configuration files used by the backend."""

# Copyright 2022 Google LLC. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import argparse
import dataclasses
import datetime
import json
import random

from typing import Dict, Iterator, List, Sequence

import pytz

TASK_TYPES = ['PICKUP', 'DELIVERY']


class CustomArgParseHelpFormatter(argparse.RawDescriptionHelpFormatter,
                                  argparse.ArgumentDefaultsHelpFormatter):
  pass


# Parser declaration.
parser = argparse.ArgumentParser(
    formatter_class=CustomArgParseHelpFormatter,
    description="""\
    Generate a configuration file of tasks and vehicles.

    Specify a circle via its center (in latitude and longitude) and radius
    (in degrees lat/lng) to randomly generate tasks and vehicles in this circle.

    Outputs to the terminal; use > to save as a file.
    """)
parser.add_argument(
    '--numVehicles',
    '-v',
    dest='num_vehicles',
    type=int,
    default=3,
    help='The number of vehicles to generate.')
parser.add_argument(
    '--numStopsPerVehicle',
    '-s',
    dest='num_stops',
    type=int,
    default=5,
    help='The number of stops to generate, per vehicle.')
parser.add_argument(
    '--numTasksPerStop',
    '-t',
    dest='num_tasks',
    type=int,
    default=3,
    help='The number of tasks to generate, per stop.')
parser.add_argument(
    '--epiLat',
    '-lat',
    dest='lat',
    type=float,
    default=37.8,
    help='The latitude of the circle\'s center.')
parser.add_argument(
    '--epiLng',
    '-lng',
    dest='lng',
    type=float,
    default=-122.4,
    help='The longitude of the circle\'s center.')
parser.add_argument(
    '--radius',
    '-r',
    dest='radius',
    type=float,
    default=0.05,
    help='The radius of the circle, in degrees lat/lng.')
parser.add_argument(
    '--stopRadius',
    '-sr',
    dest='stop_radius',
    type=float,
    default=0.001,
    help='The radius for generating tasks around a stop, in degrees lat/lng.')
parser.add_argument(
    '--taskTypes',
    dest='task_types',
    nargs='+',
    choices=TASK_TYPES,
    default=TASK_TYPES,
    help='Types of tasks to generate.')
parser.add_argument(
    '--planned_completion_time_range',
    dest='planned_completion_time_range',
    type=float,
    default=3600,
    help='The range of time allowed for completion, in seconds.')
parser.add_argument(
    '--timezone',
    dest='timezone',
    type=str,
    default='America/Los_Angeles',
    help='A timezone identifier for generated timestamps.')


@dataclasses.dataclass
class Waypoint:
  """The structure for a waypoint.

  Contains coordinates, and an optional description.
  """
  lat: float
  lng: float
  description: str = None


@dataclasses.dataclass
class Vehicle:
  """The structure for a vehicle."""

  vehicle_id: str
  description: str = None


@dataclasses.dataclass
class Task:
  """The structure for a task."""

  task_id: str
  tracking_id: str
  planned_waypoint: Waypoint
  task_type: str
  planned_completion_time: datetime.datetime = None
  planned_completion_time_range: int = 3600
  duration_seconds: int = 60
  contact_name: str = None
  description: str = None


@dataclasses.dataclass
class Stop:
  """The structure for a stop. May contain multiple tasks."""

  stop_id: str
  planned_waypoint: Waypoint
  tasks: Sequence[str]


@dataclasses.dataclass
class Manifest:
  """The structure for a vehicle manifest.

  Contains a vehicle and its stops and tasks.
  """

  vehicle: Vehicle
  tasks: Sequence[Task]
  stops: Sequence[Stop]


@dataclasses.dataclass
class BackendConfig:
  """The structure for the overall format. Contains multiple Manifests."""

  manifests: Sequence[Manifest]
  description: str = None


def get_random_waypoint(lat: float, lng: float, radius: float) -> Waypoint:
  # Generate a random offset in the box centered at origin with side length 2;
  # then check to see if it's in the unit circle.
  offset = (2, 2)
  while offset[0]**2 + offset[1]**2 > 1:
    offset = (2 * random.random() - 1, 2 * random.random() - 1)

  return Waypoint(lat + offset[0] * radius, lng + offset[1] * radius)


def yield_sequential_id(prefix: str, divider: str = '_') -> Iterator[str]:
  id_seq = 0
  while True:
    id_seq += 1
    yield f'{prefix}{divider}{id_seq}'


vehicle_id_iterator = yield_sequential_id('vehicle')
stop_id_iterator = yield_sequential_id('stop')
task_id_iterator = yield_sequential_id('task')
tracking_id_iterator = yield_sequential_id('tracking')
contact_name_iterator = yield_sequential_id('Customer', ' ')


def gen_vehicle() -> Vehicle:
  return Vehicle(next(vehicle_id_iterator))


def gen_stops_and_tasks() -> (Sequence[Stop], Sequence[Task]):
  """Creates the stops and tasks."""

  stops: List[Stop] = []
  tasks: List[Task] = []
  tz = pytz.timezone(args.timezone)
  start_time = datetime.datetime.now().astimezone(tz)
  for _ in range(args.num_stops):
    waypoint = get_random_waypoint(args.lat, args.lng, args.radius)
    stop_tasks = gen_stop_tasks(waypoint.lat, waypoint.lng,
                                args.stop_radius, start_time)
    tasks = tasks + stop_tasks
    start_time = start_time + datetime.timedelta(hours=len(stop_tasks))
    stop_id = next(stop_id_iterator)
    stops.append(Stop(stop_id, waypoint, [t.task_id for t in stop_tasks]))
  return (stops, tasks)


def gen_stop_tasks(lat: float, lng: float, radius: float,
                   start_time: datetime.datetime) -> Sequence[Task]:
  """Creates the tasks for a certain stop."""

  stop_tasks = []
  for i in range(args.num_tasks):
    stop_tasks.append(
        Task(
            task_id=next(task_id_iterator),
            tracking_id=next(tracking_id_iterator),
            planned_waypoint=get_random_waypoint(lat, lng, radius),
            task_type=random.choice(args.task_types),
            planned_completion_time=start_time + datetime.timedelta(hours=i),
            planned_completion_time_range=args.planned_completion_time_range,
            contact_name=next(contact_name_iterator)))
  return stop_tasks


def dataclass_dict_factory(data: object) -> Dict[str, object]:
  """Converts a dataclass object into a dict, removing all keys with None values."""
  d = dict(data)

  # Materialize the keys of d beforehand, so it doesn't complain about dict
  # size changing.
  keys = list(d.keys())
  for key in keys:
    if d[key] is None:
      del d[key]
  return d


class BackendConfigJsonEncoder(json.JSONEncoder):
  """JSON Encoder for BackendConfig.

  Specifies serialization for datetime and dataclass objects.
  """

  def default(self, obj: object) -> object:
    """Provides a JSON-serializable object for datetime and dataclass objects.

    Args:
      obj: the object to be JSON-serialized.

    Returns:
      a JSON-serializable object for the input.
    """

    if isinstance(obj, datetime.datetime):
      return obj.isoformat()
    elif dataclasses.is_dataclass(obj):
      return dataclasses.asdict(obj, dict_factory=dataclass_dict_factory)
    return json.JSONEncoder.default(self, obj)


if __name__ == '__main__':
  args = parser.parse_args()
  generated_manifests = []
  for _ in range(args.num_vehicles):
    generated_vehicle = gen_vehicle()
    generated_stops, generated_tasks = gen_stops_and_tasks()
    generated_manifests.append(
        Manifest(generated_vehicle, generated_tasks, generated_stops))
  backend_config = BackendConfig(generated_manifests)
  print(json.dumps(backend_config, indent=2, cls=BackendConfigJsonEncoder))

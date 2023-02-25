#!/usr/local/bin/python3

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
import json
import pathlib
import re

CONFIRMATION_RE = re.compile(r'^y(e(s)?)?$', re.IGNORECASE)
"""Regex for a positive response to the confirmation prompt."""

LMFS_ROOT = pathlib.Path(__file__).parents[1]
"""File path for the root of the last_mile_fleet_solution_samples repo."""

BACKEND_CONFIG_FILE = 'backend/src/main/resources/config.properties'
"""Backend config file path relative to LMFS_ROOT."""

ANDROID_MANIFEST_FILE = 'android_driverapp_samples/app/src/main/AndroidManifest.xml'
"""Android Manifest.xml file path relative to LMFS_ROOT."""

ANDROID_CONFIG_FILE = 'android_driverapp_samples/local.properties'
"""Android config file path relative to LMFS_ROOT."""

IOS_CONFIG_TEMPLATE = 'tools/ios_ApplicationDefaults_template.json'
"""iOS local override template file path relative to LMFS_ROOT."""

IOS_CONFIG_FILE = 'ios_driverapp_samples/LMFSDriverSampleApp/LocalOverrides/ApplicationDefaults.json'
"""iOS local override file path relative to LMFS_ROOT."""

class Item():
  """Represents one value to be edited in a configuration file."""
  name: str
  """Name of this item.

  Also defines the target string in the input file, see the update_re
  regular expression in the implementation code.
  """
  file: str
  """Path-like object for the input or input/output file."""
  output_file: str
  """If specified, path-like object for the output file.

  If not specified, file will be used for both input and output."""

  def __init__(self, name: str, file: str, output_file: str = None):
    self.name = name
    self.file = file
    self.output_file = output_file

  def enabled(self) -> bool:
    """Returns whether the utility can attempt to set the item.

    Verifies the input file and output directory exist.
    """
    if LMFS_ROOT.joinpath(self.file).is_file():
      if self.output_file is None:
        return True  # Same as self.file, which exists.
      else:
        return LMFS_ROOT.joinpath(self.output_file).parents[0].is_dir()
    else:
      return False

  def update(self, new_value: str) -> None:
    """Updates this item to the new value."""
    if not self.enabled():
      print(f'Skipping {self.name} due to missing file or directory.')
      return
    print(f'Updating {self.name}...', end='')
    update_re = re.compile(r'\*{5}UPDATE_WITH_' + self.name + r'\*{5}')
    with LMFS_ROOT.joinpath(self.file).open('r') as f:
      contents = f.read()
    new_contents = update_re.sub(new_value, contents)
    if new_contents == contents:
      print(f'\nWarning: {self.name} not found in {self.file}!')
    else:
      with LMFS_ROOT.joinpath(self.output_file or self.file).open('w') as outf:
        outf.write(new_contents)
      print(f'Done.')

  def interactive_update(self) -> None:
    """Interactively asks for a new value and updates if provided."""
    if not self.enabled():
      print(f'Skipping {self.name} due to missing file or directory.')
      return
    confirmed = False
    while(not confirmed):
      new_value = input(f'Input value for {self.name} (hit return to skip): ')
      if len(new_value) == 0:
        print(f'Skipping {self.name}.\n')
        return
      confirmation = input(f'About to update {self.name} with "{new_value}". '
                           f'Are you sure? [y/N] ')
      # If the user did not confirm the edit, we'll go around the loop and
      # prompt for the new value again.
      if CONFIRMATION_RE.match(confirmation):
        confirmed = True
        self.update(new_value)
      print('')


ITEMS = [
  Item('PROJECT_ID', BACKEND_CONFIG_FILE),
  Item('SERVER_SERVICE_ACCOUNT_EMAIL', BACKEND_CONFIG_FILE),
  Item('DRIVER_SERVICE_ACCOUNT_EMAIL', BACKEND_CONFIG_FILE),
  Item('CONSUMER_SERVICE_ACCOUNT_EMAIL', BACKEND_CONFIG_FILE),
  Item('FLEET_READER_SERVICE_ACCOUNT_EMAIL', BACKEND_CONFIG_FILE),
  Item('JS_API_KEY', BACKEND_CONFIG_FILE),
  Item('ANDROID_API_KEY', ANDROID_MANIFEST_FILE),
  Item('ANDROID_SDK_PATH', ANDROID_CONFIG_FILE),
  Item('IOS_API_KEY', IOS_CONFIG_TEMPLATE, IOS_CONFIG_FILE),
]
"""List of configuration items that may be updated."""

def main(command_file: argparse.FileType = None):
  """Main function when this script is run directly."""
  if command_file:
    commands = json.load(command_file)
    items_by_name = { i.name: i for i in ITEMS }
    for (key, val) in commands.items():
      if key in items_by_name:
        items_by_name[key].update(val)
      else:
        print(f'Command file specified unknown configuration "{key}".')
  else:
    for item in ITEMS:
      item.interactive_update()

if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  parser.add_argument('command_file', nargs='?',
    type=argparse.FileType('r', encoding='utf-8'),
    help='JSON file. Top-level keys are item names, value is the new value.'
    ' If not provided, will interactively prompt for values.')
  main(command_file=parser.parse_args().command_file)



# Local Overrides

Other than the contents of this file, files in this directory should be
ignored by source code management systems. This directory can be used
to provide development-specific local overrides.

Currently supported contents for this directory:

* **`ApplicationDefaults.json`**

  A JSON file by this name may be placed in this directory. The top-level
  keys in the file should correspond to static properties of the
  `ApplicationDefaults.swift` class, and the values of such keys should match
  the type of those properties. Any values established in the JSON file
  will become the runtime default values in `ApplicationDefaults`.

  Note that even when the default value is overridden here, users may set an
  explicit value which will be persisted by the `ApplicationDefaults` class.
  See the documentation of the class for more details.


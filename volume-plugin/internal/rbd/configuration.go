package dockerVolumeRbd

import (
	"os"
	"strings"
)



// Read
func (d *rbdDriver) configure() {

	// set default confs:
	d.conf["pool"] = "ssd"
	d.conf["cluster"] = "ceph"
	d.conf["device_map_root"] = "/dev/rbd"
	d.conf["namespace"] = ""

	d.loadEnvironmentRbdConfigVars();

}


// Get only the env vars starting by RBD_CONF_*
// i.e. RBD_CONF_GLOBAL_MON_HOST is saved in d.conf[global_mon_host]
//
// An empty value never overrides a default. Docker exports every env var
// declared in config.json, so a setting left blank on a host arrives here as
// an empty string: on a production worker a blank RBD_CONF_DEVICE_MAP_ROOT
// voided the /dev/rbd default, and every create and mount then resolved to the
// relative path <pool>/<image> instead of a device node (2026-08-31).
// Unsetting a default that has one is not a supported operation; the only
// config default that is legitimately empty (namespace) is already empty.
func (d *rbdDriver) loadEnvironmentRbdConfigVars() {
	for _, e := range os.Environ() {
		pair := strings.SplitN(e, "=", 2)

		if (strings.HasPrefix(pair[0], "RBD_CONF_")) {
			if (pair[1] == "") {
				continue
			}
			configPair := strings.Split(pair[0], "RBD_CONF_")
			d.conf[strings.ToLower(configPair[1])] = pair[1]
		}
	}

}


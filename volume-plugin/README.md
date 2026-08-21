MyDocker Docker Volume Plugin
=============================

How to develop
--------------

Run `make all` to build the plugin and install it, then `make enable` to enable it.

You can select arm64 plateform :

```bash
make PLATFORM=linux/arm64 all
```


How to use
----------

1. Create mandatory directories :
```bash
mkdir -p /etc/ceph
mkdir -p /mnt/mydocker-fs
```

2. Install the plugin :

```bash
docker plugin install --disable centralesupelec/mydockervolume:latest
```

3. Configure the plugin :

   1. For Local volumes :
    ```bash
    docker plugin set centralesupelec/mydockervolume:latest DRIVER_MODE=FS
    ```

    If you want NFS, you need an NFS export with option `no_root_squash`. This can lead to [security issues](https://book.hacktricks.xyz/linux-hardening/privilege-escalation/nfs-no_root_squash-misconfiguration-pe) but is mandatory for some docker images that do chown/chmod on the volume.  
    
    Mount your NFS backend to `/mnt/mydocker-fs`, e.g. `mount my-nfs-server:/mnt/exported-directory/directory-for-volumes-of-this-client /mnt/mydocker-fs`
    
    The mounting needs to be done _before_ enabling the plugin.

    2. For RBD volumes :
    ```bash
    docker plugin set centralesupelec/mydockervolume:latest DRIVER_MODE=RBD
    ```

And set other options following [upstream docs](https://github.com/CentraleSupelec/docker-volume-rbd) (mirrored fork from [wetopi/rbd](https://github.com/wetopi/docker-volume-rbd)).


4. Enable the plugin:
```bash
docker plugin enable --timeout 120 centralesupelec/mydockervolume
```

Always pass `--timeout`. It sets the HTTP client timeout dockerd applies to
every driver call. Under the daemon default, concurrent creates were observed
failing at ~40s on preprod while the volumes were in fact created, so callers
were told a create failed for a volume that exists; that is also shorter than
the driver's own retry budget (`SHELL_TIMEOUT_SECONDS` x `SHELL_RETRIES`, 110s
by default), so a create exhausting its retries can never report the real
error. 120 matches the request deadline dockerd already grants
`VolumeDriver.Create`.

Note that `docker plugin install` enables the plugin itself and has no
`--timeout` flag, so a fresh install lands on the default. Install with
`--disable`, then enable explicitly as above.

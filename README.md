# Movecraft-Repair Addon

Home of the code for the following features:
 - Craft repair signs
 - Region repair (for Movecraft-Warfare)
 
 ## Download

This plugin supports Minecraft versions 1.10.2 to 1.16.5, and Movecraft 7.0.0 Beta 9+ (or Eirik's fork Beta 38+).

Devevlopment builds can be found on the [GitHub Actions tab](https://github.com/APDevTeam/Movecraft-Repair/actions) of this repository.

Stable builds can be found on [our SpigotMC page](tbd).

## Building
This plugin requires that the user setup and build their [Movecraft](https://github.com/APDevTeam/Movecraft) development environment, and then clone this into the same folder as your Movecraft development environment such that both Movecraft-Repair and Movecraft are contained in the same folder.
This plugin also requires you to build the latest version of 1.13.2 using build tools.

```
java -jar BuildTools.jar --rev 1.13.2
```

Then, run the following to build Movecraft-Warfare through `maven`.
```
mvn clean install
```
Jars are located in `/target`.


## Support
[Github Issues](https://github.com/APDevTeam/Movecraft-Repair/issues)

[Discord](http://bit.ly/JoinAP-Dev)

The plugin is released here under the GNU General Public License v3.

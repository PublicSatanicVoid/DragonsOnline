# Dragons Core
This module defines the plugin `DragonsCore`, which provides core functionality for Dragons Online.

Notably, the following features and interfaces are provided:

* `Dragons` singleton instance, containing central data registries
* Game object definitions, loaders, and implementations (see `GameObject`, `GameObjectLoader`, and subclasses such as `User` and `UserLoader`)
* Lightweight object registry and loader (see `AbstractLightweightLoader`)
* Persistent and non-persistent data accessors and managers (see `StorageAccess`, `StorageManager`, and implementing classes)
* Cross-server communication protocols (see `MessageDispatcher`, `MessageHandler`, and subclasses) 
* Custom logging system (see `DragonsLogger` and `LogLevel`)
* Utilities to simplify common tasks, such as `StringUtil` for string manipulation and `BukkitUtil` for simplifying Bukkit task scheduling
* Much more!

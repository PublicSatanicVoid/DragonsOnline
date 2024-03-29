# Dragons Online

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7514213e041246c08a7e69285f3dabef)](https://app.codacy.com/manual/adam.priebe.812/DragonsOnline?utm_source=github.com&utm_medium=referral&utm_content=UniverseCraft/DragonsOnline&utm_campaign=Badge_Grade_Dashboard)

Dragons Online is a Minecraft-based MMORPG created by UniverseCraft and licensed under the GNU GPL v2.
 
## Where to dive in
If you're new to the repository, here are the most important files to check out (in this order!)
- `dragons-core: mc.dragons.core.Dragons` is the main class and the entry point. It contains all the high-level object managers, service providers, loaders, and configuration data necessary to start a server instance.
- `dragons-core: mc.dragons.core.gameobject.GameObject` defines the interface for `GameObject`s, the main unit of content within Dragons Online. Users, NPCs, items, etc. are all `GameObject`s.
- `dragons-core: mc.dragons.core.gameobject.GameObjectType` defines the various types of `GameObject`s.
  
- `dragons-core: mc.dragons.core.gameobject.user.User` defines the `User` object, which represents a player in the game.
- `dragons-moderationtools: mc.dragons.tools.moderation.InfoCommand` defines a console command to fetch and display a user's data. This class illustrates the basic structure of a command and demonstrates how data is fetched from the database at a high level, as well as how various components tie together.

- `dragons-core: mc.dragons.core.gameobject` - all classes in subpackages of this are at the heart of Dragons Online, and define the key elements of the game. If you want to take a deep dive into the workings of Dragons Online, these are a great place to start.

## Basic Code Structure
The codebase is divided into several plugins, each with a specific purpose. The core plugin is `dragons-core` and contains all necessary functionality to create a minimally working game. All other plugins depend either directly or indirectly on `dragons-core`.

A full listing of current plugins:
- `dragons-core` provides the base functionality for the game. Simple mobs/NPCs and items can function fully with just this plugin.

- `dragons-npcs` extends mob/NPC functionality to allow for custom models and behavior, such as particle auras. This functionality hooks into the core by extending the `NPCAddon` abstract class and registering each addon with the `AddonRegistry`.

- `dragons-spells` allows specific abilities to be bound to capable items, such as damaging all nearby entities after using a certain combination of clicks such as R-L-L.

- `dragons-dev` implements a routine backup protocol, periodic announcements for the content team, and a tasks system to assign and track various projects. Highly recommended for build or development servers.

- `dragons-devtools` further expands on `dragons-dev` by providing a wide range of command-based utilities to test, debug, and inspect server performance and locate the cause of errors without requiring attachment of a debugger. Whereas `dragons-dev` is geared more toward content teams and management, `dragons-devtools` is geared toward developers and administration.

- `dragons-contenttools` provides many useful features for game masters or designers to implement aspects of the game, such as creating NPC and item classes, defining regions, and more.

- `dragons-moderationtools` provides commands for moderating user activity. This includes warning, kicking, muting, and banning players, optionally for a specified time frame. Commands are also provided for viewing and modifying a user's punishment history and carrying out other moderation-related tasks such as becoming invisible or invincible.

- `dragons-anticheat` is an experimental module that seeks to model (and someday enforce) vanilla behavior, accounting for modifications by the game.
- `dragons-res` implements a residence system. This allows users to purchase physical space in the game and customize it in various ways. Residences are virtual and instanced separately from the actual game worlds, allowing for multiple users to independently purchase and customize the same area. That is, there is a many-to-many relationship between areas in the game ('res points') which are typically enclosed spaces such as houses, stores, or apartments, and the actual residence of each player for a given res point.

- `dragons-social` defines social functionality such as guilds, messaging, and broadcasting, which allow users to communicate across server instances. Functionality such as friend requests and trades will also go in this module, once implemented.

- `dragons-spells` implements `ItemAddon`s to allow custom functionality to be bound to an item upon a certain sequence of clicks being performed.
 
**All Dragons plugins other than DragonsCore must depend on DragonsCore and their main classes must extend DragonsJavaPlugin! Things will break if this does not happen!**
 
## Game Objects and Storage
Major aspects of the game are defined as game objects. Every game object extends the abstract `GameObject` class to provide specific functionality. The `GameObject` class itself provides the necessary methods for storing and retrieving data. Certain game objects are backed by a persistent data storage system such as a MongoDB database, while other (more transient) game objects are only stored in-memory. These various ways of storage are standardized and abstracted via the `StorageManager` and `StorageAccess` interfaces. All `GameObject`s have a `StorageAccess`, which is unique to that `GameObject` and handles the storage of that object's data, and a `StorageManager`, which provides methods for creating, loading, storing, removing, and batch-modifying `StorageAccess`es.
 
The two 'flavors' of storage are `Mongo` and `Local`, implemented as `MongoStorageManager`/`MongoStorageAccess` and `LocalStorageManager`/`LocalStorageAccess`. Local storage does not persist past the game server instance and may often last only for a few seconds or minutes. This is the flavor used for transient objects such as mobs. Mongo storage access does persist past game server instances and is backed by a MongoDB database. This is used for permanent NPCs (e.g. quest givers), items, regions, etc.

**A MongoDB instance is required for Dragons Online to load. It must be configured as a replica set, due to extensive use of change streams throughout the codebase.**

All game objects have a unique identifier, which consists of the game object's type and a UUID. Combined with the flavor of storage for the game object, this can be used to retrieve any game object subject to scope constraints (e.g. the `LocalStorageAccess` of a transient object can only be accessed on the server on which it resides and only while it is live).

The current game object types are:
- `User`: A player in the game. Each user is associated with a player and contains additional properties such as the player's quest progress, saved inventory (as opposed to the online-only inventory of the Bukkit `Player`), and rank.
- `Floor`: A world in the game. Each floor contains a short name (no spaces), a display name, a minimum level, and a world file. The short name is used by the content team or developers; the display name is shown to players. For nonzero minimum levels, there should be a one-to-one relationship between minimum level and floor (level-zero floors being reserved for various development and testing functions and thus not user-facing). Floors are persistent game objects.

- `Region`: A cubic area in the game. Regions serve two main purposes: to give users a sense of where they are and a way of communicating this easily to others, and to restrict certain functionality to specific places. For example, a specific region may have an extremely high spawn rate of a particular mob. This spawn rate is tied to a specific region and is only applicable in that region.

- `Item`: A specific instance of an item in the game. Items exist either as drops in a game world at a particular location or as items in a player's inventory. As they must persist in inventories across user logins, items are persistent game objects. Every item is associated with an item class, which defines the default traits for that item, such as its material type, minimum required level to use, lore, etc. Notably, bound spells (if using the `dragons-spells` plugin) are not derived from the item class.

- `ItemClass`: A type of item. Each item class serves as a template for all items of that class. When an item is created, its traits are derived from its item class, but many of these traits may be overridden on a specific item.

- `NPC`: A specific instance of a mob in the game. Both transient mobs and permanent NPCs use this class, as the differences in most regards are nominal and non-specific. As with items, NPCs inherit most of their traits from their NPC class. Notably, location is not derived from the NPC class.

- `NPCClass`: A type of NPC. This is more specific than the Bukkit `EntityType`. An NPC class defines the entity type in addition to the displayed name, level, health, and much more. Many modifiers are available via NPC classes, such as custom behavior on left-clicking (attacking) or right-clicking (interacting with) an NPC of that class, as well as attributes such as walk speed, knockback resistance, etc.

- `Quest`: A named series of objectives leading to a specific goal. Simpler quests use a linear chain of objectives, while more complex quests may branch differently based on a player's choices (e.g. obeying or disobeying an NPC's instructions). Quests are also associated with a minimum required level.

- `Structure`: (Not implemented yet) A generated structure in the game. These are similar to schematics, and indeed may rely upon schematics, but are also associated with the user who placed it and may have restrictions on where, how frequently, and for how long they may be placed. Examples of structures would be towers, siege engines, and cannons. Most if not all structures will decay or disappear after a certain amount of time, allowing for a more dynamic gameplay experience.
 
Game objects are created and loaded via the associated generic `GameObjectLoader` of their type, for example `FloorLoader` which extends `GameObjectLoader<Floor>` and loads `Floor`s. This is true for all game objects: in general, a game object of class `X` is loaded through the class `XLoader` which extends `GameObjectLoader<X>`. Various methods exist for loading a game object; however, it is guaranteed that a non-abstract subclass of `GameObjectLoader` will contain the public method `loadObject(StorageAccess)` which returns a game object associated with the specified storage access. Specific loaders may provide additional methods for loading by UUID or name, for creating new objects, or for marshalling between the game object and the associated Bukkit object, if applicable. Outside of `GameObjectLoader`s, it is highly recommended to use those loaders to construct game objects rather than directly calling their constructors.

Game object types are stored in the `GameObjectType` enum, through which their associated loader may be accessed as `GameObjectType.getLoader(XLoader.class)` or `GameObjectType.X.getLoader()` e.g. `GameObjectType.getLoader(RegionLoader.class)` or `GameObjectType.REGION.getLoader()`.

## Lightweight Objects
Lightweight objects are other aspects of the game which are generally less complex than `GameObject`s. For example, residences are lightweight objects as their only data is the owner of the residence, the associated res point, a list of customizations applied to the residence, and whether or not the residence has been locked by an administrator. Lightweight objects may be any object, as they are not required to extend any particular class (such a class would be entirely empty as lightweight objects are highly diverse and flexible in nature and generally share few if any common features).

All lightweight objects are loaded through a generic `AbstractLightweightLoader`, such as a `ResLoader` which extends `AbstractLightweightLoader<Residence>`. All lightweight object loaders must be registered with the `LightweightLoaderRegistry`.

## Intra-Server Connectivity
Dragons Online is designed to support a multi-server environment, in which key data is synced in real time and secondary data reaches eventual consistency. Eventual consistency is reached through the use of a common MongoDB connection through which non-transient data is persisted. Immediate synchronization is achieved when needed through the use of `MessageHandler`s which are managed through a central `MessageDispatcher`.

Each `MessageHandler` handles a separate type of sync operation. For example, punishments issued on one server which need to be applied to a player on another server go through `PunishMessageHandler` implemented in `dragons-moderationtools`. These message handlers rely on MongoDB change streams for real-time communication.

## Permissions
Various components of Dragons Online may provide access to sensitive features such as moderation and game object manipulation. In most Minecraft plugins, this access is controlled through permission nodes, which are strings associated with a user. This approach is flawed for three reasons. Firstly, these strings must be standardized - any small discrepancy, e.g. "gameobject.npc.delete" vs "gameobjects.npc.delete" can lead to errors, thus suggesting that permission strings be stored as (preferably) static and final constants. Secondly, in traditional implementations these are associated with a specific player. This issue introduces a crucial security vulnerability; if a Minecraft account with elevated permissions is compromised, these permissions are available to that account without qualification, unless other security measures are introduced downstream. Thirdly, if a staff member logs in with an alternate Minecraft account they will be unable to access their elevated permissions unless they have registered those permissions to their alternate account. Conventionally, this registration has proven difficult to manage.

To avoid the first problem, Dragons Online uses permission levels and permission flags. A permission level functions similarly to a rank in that it is named, represents a specific role, and exists in a one-to-many relationship with users. A permission flag functions similarly to a permission string but differs in that it is implemented in an enum (`SystemProfileFlag`, to be specific) rather than as a string. Permission levels control primary access to functionality and are hierarchical in nature; profile flags control access to additional functionality and are not hierarchical. For example, while all moderators need to be able to ban users, not all admins need this ability as some work solely on the development side. Thus, there is a separate permission flag for access to sensitive moderation tools which admins may or may not have.

To avoid the second and third problems, these levels and flags are not associated directly with users but rather with a `SystemProfile`. System profiles have a name and password separate from a Minecraft account, and require a player to log in to them upon joining. This significantly thwarts an attacker's likelihood of gaining elevated permissions as knowledge of a user's Minecraft credentials is insufficient for gaining access to elevated permissions. This adds another layer on top of Minecraft's authentication system. System profile passwords are stored only in hashed form (via passing a salted input to the SHA-256 hash function) for added security. Furthermore, each system profile is locked to a list of Minecraft accounts, so privileged users must register alternate accounts beforehand if they wish to access elevated permissions on those accounts. However, once an alt account is registered its permissions access is automatically identical to that of its main account.

## Chat Channels
A custom chat system is implemented in the `dragons-core` plugin. In addition to creating a nicer and more informative format for the chat, this system implements a channelled approach. Specifically, each user "speaks" on one channel and "listens" on one or more channels. When a user sends a message on a channel, only the users listening on that channel receive the message.

Channels are often context-specific. For example, the Local channel is world-specific; only users listening to the Local channel _and_ currently located in the same world as the sender will receive the message.

This system significantly cuts down on clutter by allowing users to choose which messages they receive.

For even more user-specific communications, a private-messaging system is implemented as well. Moderators can view this as well as see communications on channels in other contexts via the /chatspy command.

## Quests
Perhaps the most complex part of Dragons Online is the quest system. As mentioned above, a quest is implemented as a series of objectives. Each objective, or step, has a trigger and a list of actions. Upon advancing to an objective, the user will (usually) see the name of the objective. Upon fulfilling the trigger condition, the actions of that step will be executed sequentially.

Some more complicated quests may trigger different kinds of actions by branching. To implement branching, the step must have the trigger type `TriggerType.BRANCH_CONDITIONAL`. The trigger is then associated with various branch points, each of which contains a trigger and a step to jump to if that trigger is met. Triggers in the branch conditional are checked sequentially to ensure consistent results.

Some examples of triggers include checking whether the player has entered or exited a region, clicked on or killed an NPC, or has a certain item. A trigger may also be `INSTANT`, which results in the actions executing immediately, or `NEVER`, which results in the actions never executing (i.e. a dead-end).

Some examples of actions include teleporting the player, beginning dialogue between an NPC and the player, spawning an NPC, giving XP to the player, sending the player to a different step of the quest, teleporting an NPC to the player, making an NPC walk to the player, giving or taking an item to/from the player, adding or removing a potion effect to/from the player, giving the player a series of choices (each of which sends the player to a specific quest step), pausing quest execution for a certain amount of time, or displaying a standardized completion message to the player upon finishing the quest.

## Tasks
Dragons Online implements a robust tasks system to allow developers, builders, and game masters to collaboratively and centrally manage assignments and projects. This system is implemented in `dragons-dev` and can optionally use Discord webhooks to send task notifications to a channel.

The task lifecycle is as follows: First, a task is created. Then, a staff member with the `SystemProfileFlag.TASK_MANAGER` flag can approve or deny the task. If denied, no further action is taken. If approved, the task must then be assigned to one or more members. This can be carried out by a task manager or on individual initiative through self-assignment. Once the task is completed, an assignee marks it as done. A task manager reviews it and either re-opens the task if more work is needed, or closes it if the task is satisfactorily completed. Erroneous tasks can be permanently deleted, but closed tasks are not automatically deleted so they can be referenced later.

**For proper searching behavior within tasks, a text index must be created on the tasks collection on the columns `name` and optionally `notes`.**

## Moderation System
Dragons Online features an extensive moderation system, mostly contained within the `dragons-moderationtools` module. The system integrates thoroughly with other areas of the codebase, taking snapshots of user data when a report is filed and implementing strict permission requirements to compartmentalize access. It is also depended on by the anticheat to issue and track automated violations.

Violations are classified into different codes, which can be viewed with the `/pcodes` command. A standing level system is used to apply stricter punishments the more times a player breaks a rule.

### Central Entry Point for Moderation Actions
There are different ways to initiate a moderation action. Regular players can use the `/report <player> <reason>` command, or `/chatreport <player>`. This files a central report which is placed in a moderation queue for review. Authorized staff members can use `/report <player1 [player2 ...] <code> [extra info]` to report one or more players at once. Staff-issued reports guide the issuer through a process of choosing the appropriate action to take.

### Types of Moderation Actions
Moderation actions can be initiated more directly with `/hold <players ...> <code> [extra info]`, which files a report and places a 48-hour suspension on the targeted accounts. This suspension takes the form of either a temporary mute or ban, depending on the code specified.

If a staff member does not want to take any immediate action and needs a senior staff member to review the issue, they can use `/escalate <players ...> <code> [extra info]`. A report will be filed and flagged for senior staff review.

A player can be placed on the watchlist if there is insufficient evidence but high suspicion. This generates a report and notifies staff members when the player joins. Watchlist reports are automatically closed if the player is banned, and can also be closed manually.

An punishment can be applied instantly with `/punish <players ...> <code> [extra info]`, which immediately applies a punishment if possible. If the issuing staff member does not have permission to apply the specified code, an escalation report is applied instead and a hold is placed. Punishments of permanent duration also generate an escalation report, but are applied immediately if authorized.

If a staff member is uncertain which moderation action to take, they should use `/report` which will guide them through the process of choosing one.

### Reviewing Moderation Actions
Moderation actions can then be reviewed with the `/modqueue` command, which shows the staff member the next report in the queue. Helpers can only view or take action on chat reports. The report is displayed, along with clickable options to confirm the report, mark it as insufficient evidence, escalate for senior staff review, skip the report, etc.

### Administration of Reports and Punishments
A punishment can be revoked for various reasons, each of which is given a revocation code. Helpers and moderators can revoke punishments that they have issued; appeals team staff and admins can revoke any punishment.

A player's punishment history can be viewed with `/viewpunishments <player>`. A punishment can be removed with `/removepunishment <player> <id> <revocation code>`. The punishment ID can be found by viewing the punishment history.

Admins have access to purge a player's punishment history and permanently delete reports or punishments.

There are many other features of the moderation system which are too numerous to cover here. All moderation commands can be found in the `plugin.yml` of `dragons-moderationtools`.

## Custom Logging
The default Java logging system provides many options for extension. Unfortunately, the underlying framework of CraftBukkit and its reliance upon log4j as a backend breaks much of this functionality, in particular debug logging. To address this, we provide numerous logging utilities which replace many key components of the logging system to enable debug logging and implement custom, more intuitive log levels. This is managed through a `CustomLoggingProvider` within `DragonsJavaPlugin`, and the `DragonsLogger` class is provided to allow easy access to these additional logging facilities.

It is preferred to use the provided class `LogLevel` over Java's `Level` as the former has additional levels and features designed to improve usefulness to this project.

Correlation ID-based logging is also implemented in `CorrelationLogger` which extends `AbstractLightweightLoader<CorrelationLogEntry>`. Correlation IDs allow logs related to transactions or complex operations to be tracked and associated across multiple modules, so that all relevant log entries can be fetched and read in one place if the correlation ID is known. Rather than relying directly on this class, however, it is preferred to use the wrapper methods in `DragonsLogger`.

## Singleton Usage
Many key classes implement the `Singleton` interface, indicating that they enforce that at most one instance of that class exist in the application's lifecycle. Singletons are registered centrally with the `Singletons` class and can be retrieved with `Singletons.getInstance(Class<? extends Singleton>)`. Many singleton classes will also implement a static `getInstance()` method of their own. These static getters are all provided for convenience and are intended as a last resort if instances are unavailable at a given location in code - for example, in add-ons or in some specific static utility classes.

It is strongly recommended that dependency injection be preferred whenever possible, to minimize the incentive for static abuse and to keep dependencies between code straightforward. Generally, any module or class should have at most one call to `getInstance()` the result of which should then be cached.

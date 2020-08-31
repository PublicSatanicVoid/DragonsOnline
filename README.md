# DragonsOnline
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7514213e041246c08a7e69285f3dabef)](https://app.codacy.com/manual/adam.priebe.812/DragonsOnline?utm_source=github.com&utm_medium=referral&utm_content=UniverseCraft/DragonsOnline&utm_campaign=Badge_Grade_Dashboard)

Dragons Online is a Minecraft-based MMORPG created by DragonRider747 (UniverseCraft) and licensed under the GNU GPL v2.

## Basic Code Structure
The codebase is divided into several plugins, each with a specific purpose. The core plugin is `dragons-core` and contains all necessary functionality to create a minimally working game. All other plugins depend either directly or indirectly on `dragons-core`.

Build servers or development servers should include `dragons-dev` which implements a routine backup protocol, announcements, and a tasks system to assign and track various projects. Further functionality for creating the game is included in `dragons-contenttools` which includes many useful GM commands. It is also recommended that both dev and production servers include `dragons-devtools` which contains many commands which allow for testing, debugging, and inspecting the server performance and locating the cause of errors without requiring a debugger to be attached.

A full listing of current plugins:
- `dragons-core` provides the base functionality for the game. Simple (vanilla) mobs/NPCs and items can function fully with just this plugin.

- `dragons-npcs` extends mob/NPC functionality to allow for custom models and behavior, such as particle auras. This functionality hooks into the core by extending the `NPCAddon` abstract class and registering each addon with the `AddonRegistry`.

- `dragons-spells` allows specific abilities to be bound to capable items, such as damaging all nearby entities after using a certain combination of clicks such as R-L-L.

- `dragons-dev` implements a routine backup protocol, periodic announcements for the content team, and a tasks system to assign and track various projects. Highly recommended for build or development servers.

- `dragons-devtools` further expands on `dragons-dev` by providing a wide range of command-based utilities to test, debug, and inspect server performance and locate the cause of errors without requiring attachment of a debugger.

- `dragons-contenttools` provides many useful features for game masters or designers to implement aspects of the game, such as creating NPC and item classes, defining regions, and more.

- `dragons-moderationtools` provides commands for moderating user activity. This includes warning, kicking, muting, and banning players, optionally for a specified time frame. Commands are also provided for viewing and modifying a user's punishment history and carrying out other moderation-related tasks such as becoming invisible or invincible.

- `dragons-res` implements a residence system. This allows users to purchase physical space in the game and customize it in various ways. Residences are virtual and instanced separately from the actual game worlds, allowing for multiple users to independently purchase and customize the same area. That is, there is a many-to-many relationship between areas in the game ('res points') which are typically enclosed spaces such as houses, stores, or apartments, and the actual residence of each player for a given res point.
 
 ## Game Objects and Storage
 Major aspects of the game are defined as game objects. Every game object extends the abstract `GameObject` class to provide specific functionality. The `GameObject` class itself provides the necessary methods for storing and retrieving data. Certain game objects are backed by a persistent data storage system such as a MongoDB database, while other (more transient) game objects are only stored in-memory. These various ways of storage are standardized and abstracted via the `StorageManager` and `StorageAccess` interfaces. All `GameObject`s have a `StorageAccess`, which is unique to that `GameObject` and handles the storage of that object's data, and a `StorageManager`, which provides methods for creating, loading, storing, removing, and batch-modifying `StorageAccess`es.
 
The two 'flavors' of storage are `Mongo` and `Local`, implemented as `MongoStorageManager`/`MongoStorageAccess` and `LocalStorageManager`/`LocalStorageAccess`. Local storage does not persist past the game server instance and may often last only for a few seconds or minutes. This is the flavor used for transient objects such as mobs. Mongo storage access does persist past game server instances and is backed by a MongoDB database. This is used for permanent NPCs (e.g. quest givers), items, regions, etc.

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
 
Game objects are created and loaded via the associated generic `GameObjectLoader` of their type, for example `FloorLoader` which extends `GameObjectLoader<Floor>` and loads `Floor`s. This is true for all game objects: in general, a game object of class `X` is loaded through the class `XLoader` which extends `GameObjectLoader<X>`. Various methods exist for loading a game object; however, it is guaranteed that a non-abstract subclass of `GameObjectLoader` will contain the public method `loadObject(StorageAccess)` which returns a game object associated with the specified storage access. Specific loaders may provide additional methods for loading by UUID or name, for creating new objects, or for marshalling between the game object and the associated Bukkit object, if applicable.

Game object types are stored in the `GameObjectType` enum, through which their associated loader may be accessed as `GameObjectType.X.<X, XLoader>getLoader()` e.g. `GameObjectType.REGION.<Region, RegionLoader>getLoader()`.

## Lightweight Objects
Lightweight objects are other aspects of the game which are generally less complex than `GameObject`s. For example, residences are lightweight objects as their only data is the owner of the residence, the associated res point, a list of customizations applied to the residence, and whether or not the residence has been locked by an administrator. Lightweight objects may be any object, as they are not required to extend any particular class (such a class would be entirely empty as lightweight objects are highly diverse and flexible in nature and generally share few if any common features).

All lightweight objects are loaded through a generic `AbstractLightweightLoader`, such as a `ResLoader` which extends `AbstractLightweightLoader<Residence>`. All lightweight object loaders must be registered with the `LightweightLoaderRegistry`.

## Permissions
Various components of Dragons Online may provide access to sensitive features such as moderation and game object manipulation. Typically, this access is controlled through permissions, which are strings associated with a user. This approach is flawed for three reasons. Firstly, these strings must be standardized - any small discrepancy, e.g. "gameobject.npc.delete" vs "gameobjects.npc.delete" can lead to errors, thus suggesting that permission strings be stored as (preferably) static and final constants. Secondly, in traditional implementations these are associated with a specific player. This issue introduces a crucial security vulnerability; if a Minecraft account with elevated permissions is compromised, these permissions are available to that account without qualification, unless other security measures are introduced downstream. Thirdly, if a staff member logs in with an alternate Minecraft account they will be unable to access their elevated permissions.

To avoid the first problem, Dragons Online uses permission levels and permission flags. A permission level functions similarly to a rank in that it is named, represents a specific role, and exists in a one-to-many relationship with users. A permission flag functions similarly to a permission string but differs in that it is implemented in an enum (`SystemProfileFlag`, to be specific) rather than as a string. This significantly signifies the problem of using permission strings, as the fact that permission strings ought to be standardized via static and final constants suggests a better implementation in the form of an enum.

To avoid the second and third problems, these levels and flags are not associated directly with users but rather with a `SystemProfile`. System profiles have a name and password separate from a Minecraft account, and require a player to log in to them upon joining. This significantly thwarts an attacker's likelihood of gaining elevated permissions as knowledge of a user's Minecraft credentials is insufficient for gaining access to elevated permissions. Neither is it necessary: any user with the credentials of the system profile can log into that system profile and gain the associated permissions until logging out. System profile passwords are stored only in hashed form (via passing a salted input to the SHA-256 hash function) for added security. This brings the critical security requirement under the roof of the server and associated database(s) rather than relying on a potentially compromisable third party. (In poorly designed systems this could theoretically reduce security; we assume, as does every developer of every secure system, that sysadmins will take care to properly secure and compartmentalize access to their databases and servers. Given such security, we are at worst introducing no new vulnerability to the system.)

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

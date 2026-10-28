## https://integrateddynamics.rubensworks.net/book/manual/diagnostics.html

Network Diagnostics
Players with operator status have access to the
Network Diagnostics
tool, which allows the tick time across all networks on the server to be inspected.
To enable this tool, the command
\integrateddynamics networkdiagnostics
can be executed, which will start a temporary local web server.
When clicking on the link shown after executing the command, your Web browser will open, and will show a table containing all networks and their elements. You can click on any row to highlight them in the world, or click on the
Teleport
button to teleport your player to them.

## https://integrateddynamics.rubensworks.net/book/manual/networks.html

Networks
Integrated Dynamics
networks
are the basis of any good automation system. A network consists of a collection of connected
Logic Cables
and parts can be placed on them. These cables should be managed using a
Wrench
. Shift+right-clicking a cable or part will remove it, while a regular right-click will disconnect it from other cables.
Even though
Logic Cables
may look nice, in some cases you’ll probably want to hide them. You can do this using
Facades
, which can resemble any material. Furthermore, these cables are also compatible with
MC MultiPart
, which means that you can hide your cables in more complex facade structures.
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Stick]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Redstone Dust]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Stick]
[Crystallized Menril Chunk]
→
[Logic Cable]
3
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Redstone Dust]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
→
[Wrench]
Crafting Table
[Crafting Table]
[Slimeball]
[Slimeball]
[Crystallized Menril Chunk]
[Slimeball]
[Slimeball]
→
[Facade]
32

## https://integrateddynamics.rubensworks.net/book/manual/parts/index.html

Parts
Introduction
Settings
Offsets
Readers
Writers
Others

## https://integrateddynamics.rubensworks.net/book/manual/parts/introduction.html

Introduction
Parts can be placed on each side of a
Logic Cable
. This section describes parts that can read
Values
and make them available with a
Variable Card
, and parts that can perform actions based on the
Values
within
Variable Cards
.
Most parts have configurable settings, which can be accessed using the top-left button in the GUI of a part. This allows you to change the part
Ticks/Operation
, meaning per how many ticks this part should perform its actions. Secondly, the
Priority
of part can be changed, which indicates the order in which this part can act relative to the other parts in the same network in the same tick. If part A has a priority of -1, and part B has a priority of 1, then part B will always perform its actions before part A. If priorities of parts are the same, then the relative order is random.
A part's
Energy Channel
determines where it can draw its energy from. -1 is the wildcard channel (it will match all channels), and 0 is the default channel (used by parts such as energy cells that don't have a channel setting). Finally,
Target Side
determines which side of its target block will be interacted with. This defaults to the side that it's actually placed against.
Some reader and writer parts only contain aspects when certain other mods are present. If no aspects are present for a certain part, a warning will be added to the item tooltip whenever you attempt to craft it.

## https://integrateddynamics.rubensworks.net/book/manual/parts/offsets.html

Offsets
By default, parts will target the direct neighboring position. However, when
Part Enhancements
are applied, the target position can be changed through the part's
Offset
screen.
Offsets are always relative to the part's target position, and are limited by the number of Part Enhancements that have been applied.
Offsets can either be set manually, or they can be set visually using the
Wrench
. When setting the
Wrench
to offset-mode, a position can be stored by shift + right-clicking. This position can be saved into parts by right-clicking them with that
Wrench
.
Offsets can not only be statically defined, buy they can also be
dynamic
using
Variable Cards
.
Crafting Table
[Crafting Table]
[Input Variable Transformer]
[Crystallized Menril Chunk]
[Output Variable Transformer]
[Input Variable Transformer]
[Crystallized Menril Chunk]
[Output Variable Transformer]
[Diamond]
→
[Part Enhancement: Offset]

## https://integrateddynamics.rubensworks.net/book/manual/parts/other/connector_mono_directional.html

Mono-Directional Connector
Sometimes, your logic networks will span large distances. While regular cables will do the trick, it may not be very convenient to place cables across a long distance.
Mono-Directional Connectors
allow you to connect networks wirelessly over a long distance. This can be done by placing a first connector on one network, and a second connector on another network. These two connectors must be in a straight line to each other, with a distance less than or equal to 512 blocks. The connectors will appear active when a valid connection has been made.
Crafting Table
[Crafting Table]
[Output Variable Transformer]
[Logic Cable]
[Logic Cable]
[Logic Cable]
[Input Variable Transformer]
→
[Mono-Directional Connector]

## https://integrateddynamics.rubensworks.net/book/manual/parts/other/connector_omni_directional.html

Omni-Directional Connector
While
Mono-Directional
connectors are useful for long-distance connections in a single direction, in some cases this might be a bit too limited.
You may for example want to connect more than two networks together using a single connector or connect with networks in other dimensions. For these cases, you can use the
Omni-Directional
connectors.
These connectors will have a unique group id when first crafted, which identifies the group in which networks can be connected. Each connector group has a unique color of particles when placed in the world, the group id can be seen in the item tooltip or by right-clicking on the connector when placed. Additional connectors can be added to a group by crafting a new one using an existing connector within a group.
You can also change the group of existing connectors in two ways. Crafting one alone in a crafting table will assign it to a new group with no other members, and crafting two together will copy the first one's group to the second one.
Crafting Table
[Crafting Table]
[Logic Director]
[Mono-Directional Connector]
[Logic Cable]
[Mono-Directional Connector]
[Logic Director]
→
[Omni-Directional Connector]
2
Crafting Table
[Crafting Table]
[Logic Director]
[Mono-Directional Connector]
[Omni-Directional Connector]
[Mono-Directional Connector]
[Logic Director]
→
[Omni-Directional Connector]
3
Crafting Table
[Crafting Table]
[Omni-Directional Connector]
[Omni-Directional Connector]
→
[Omni-Directional Connector]
2

## https://integrateddynamics.rubensworks.net/book/manual/parts/other/display.html

Display
A useful monitoring or debugging tool that simply shows the value of a
Variable Card
.
Displays
can be rotated by right-clicking with a
Wrench
.
Inside the gui, a
Copy
button is available to copy the currently displayed value to your clipboard so that it can be used outside of the game. You can also just use the default copy hotkey of your OS to copy this value.
Crafting Table
[Crafting Table]
[Menril Berries]
[Static Light Panel]
[Output Variable Transformer]
[Static Light Panel]
[Menril Berries]
→
[Display Panel]

## https://integrateddynamics.rubensworks.net/book/manual/parts/other/index.html

Others
Light
Display
Mono-Directional Connector
Omni-Directional Connector

## https://integrateddynamics.rubensworks.net/book/manual/parts/other/light.html

Light
A simple light that is either always on, or can be controlled using a
Boolean
or
Integer
Variable Card
.
Crafting Table
[Crafting Table]
[Menril Berries]
[Menril Berries]
[Output Variable Transformer]
[Redstone Dust]
[Menril Berries]
→
[Static Light Panel]
Crafting Table
[Crafting Table]
[Redstone Dust]
[Redstone Dust]
[Static Light Panel]
[Redstone Dust]
[Redstone Dust]
→
[Dynamic Light Panel]

## https://integrateddynamics.rubensworks.net/book/manual/parts/reader/audio.html

Audio
Information about music and audio that is playing in the enviroment.
Crafting Table
[Crafting Table]
[Note Block]
[Note Block]
[Input Variable Transformer]
[Note Block]
[Note Block]
→
[Audio Reader]
Aspect
Harp Note
Reads a harp note, expected range [0, 24]
Output:
Integer
Properties:
Range
Base Drum Note
Reads a base drum note, expected range [0, 24]
Output:
Integer
Properties:
Range
Snare Note
Reads a snare note, expected range [0, 24]
Output:
Integer
Properties:
Range
Hat Note
Reads a hat note, expected range [0, 24]
Output:
Integer
Properties:
Range
Bass Note
Reads a bass note, expected range [0, 24]
Output:
Integer
Properties:
Range
Flute Note
Reads a flute note, expected range [0, 24]
Output:
Integer
Properties:
Range
Bell Note
Reads a bell note, expected range [0, 24]
Output:
Integer
Properties:
Range
Guitar Note
Reads a guitar note, expected range [0, 24]
Output:
Integer
Properties:
Range
Chime Note
Reads a chime note, expected range [0, 24]
Output:
Integer
Properties:
Range
Xylophone Note
Reads a xylophone note, expected range [0, 24]
Output:
Integer
Properties:
Range
Iron Xylophone Note
Reads an iron xylophone note, expected range [0, 24]
Output:
Integer
Properties:
Range
Cow Bell Note
Reads a cow bell note, expected range [0, 24]
Output:
Integer
Properties:
Range
Didgeridoo Note
Reads a didgeridoo note, expected range [0, 24]
Output:
Integer
Properties:
Range
Bit Note
Reads a bit note, expected range [0, 24]
Output:
Integer
Properties:
Range
Banjo Note
Reads a banjo note, expected range [0, 24]
Output:
Integer
Properties:
Range
Pling Note
Reads a pling note, expected range [0, 24]
Output:
Integer
Properties:
Range
Zombie Note
Reads a zombie note, expected range [0, 24]
Output:
Integer
Properties:
Range
Skeleton Note
Reads a skeleton note, expected range [0, 24]
Output:
Integer
Properties:
Range
Creeper Note
Reads a creeper note, expected range [0, 24]
Output:
Integer
Properties:
Range
Dragon Note
Reads a dragon note, expected range [0, 24]
Output:
Integer
Properties:
Range
Wither Skeleton Note
Reads a wither skeleton note, expected range [0, 24]
Output:
Integer
Properties:
Range
Piglin Note
Reads a piglin note, expected range [0, 24]
Output:
Integer
Properties:
Range
Head Note
Reads a head note, expected range [0, 24]
Output:
Integer
Properties:
Range

## https://integrateddynamics.rubensworks.net/book/manual/parts/reader/block.html

Block
Information about the block the part is facing.
Crafting Table
[Crafting Table]
[Cobblestone]
[Infested Cobblestone]
[Mossy Cobblestone]
[Cobbled Deepslate]
[Dirt]
[Cobblestone]
[Infested Cobblestone]
[Mossy Cobblestone]
[Cobbled Deepslate]
[Dirt]
[Input Variable Transformer]
[Dirt]
[Cobblestone]
[Infested Cobblestone]
[Mossy Cobblestone]
[Cobbled Deepslate]
[Dirt]
[Cobblestone]
[Infested Cobblestone]
[Mossy Cobblestone]
[Cobbled Deepslate]
→
[Block Reader]
Aspect
Has Block
If the target space has a block
Output:
Boolean
Dimension
The id of the target world dimension
Output:
String
X Coordinate
The X coordinate of the target
Output:
Integer
Y Coordinate
The Y coordinate of the target
Output:
Integer
Z Coordinate
The Z coordinate of the target
Output:
Integer
Block
The target block
Output:
Block
Tile Entity NBT
The target Tile Entity as NBT
Output:
NBT
Biome
The biome name at the target
Output:
String
Light Level
The light level at the target
Output:
Integer

## https://integrateddynamics.rubensworks.net/book/manual/parts/reader/entity.html

Entity
Information about the entity or entities that are inside the square that the part is facing.
Crafting Table
[Crafting Table]
[Raw Porkchop]
[Raw Porkchop]
[Input Variable Transformer]
[Raw Porkchop]
[Raw Porkchop]
→
[Entity Reader]
Aspect
Item Frame Rotation
The index of the rotation the item in the attached Item Frame has.
Output:
Integer
Entities
The entities in the target space
Output:
List
Players
The players that are in the target space
Output:
List
Entity
The selected entity in the target space
Output:
Entity
Properties:
List Element
Item Frame
The item in an Item Frame attached to the reader
Output:
Item

## https://integrateddynamics.rubensworks.net/book/manual/parts/reader/extra_dimensional.html

Extra-Dimensional
Information about the server, such as the list of online players.
Crafting Table
[Crafting Table]
[Eye of Ender]
[Ender Pearl]
[Input Variable Transformer]
[Ender Pearl]
[Eye of Ender]
→
[Extra-Dimensional Reader]
Aspect
Random
A random integer value
Output:
Integer
Player Count
The amount of players in the server
Output:
Integer
Tick time
The average tick time in nanoseconds for the server
Output:
Integer
TPS
The average number of ticks per second over all worlds
Output:
Double
Players
The players that are online on this server
Output:
List

## https://integrateddynamics.rubensworks.net/book/manual/parts/reader/fluid.html

Fluid
Read values from the fluid tank this part is facing.
Crafting Table
[Crafting Table]
[Water Bucket]
[Bucket]
[Input Variable Transformer]
[Bucket]
[Water Bucket]
→
[Fluid Reader]
Aspect
Tank Full
If the target fluid tank is full
Output:
Boolean
Tank Empty
If the target fluid tank is empty
Output:
Boolean
Tank Not Empty
If the target fluid tank has something in it
Output:
Boolean
Is Tank
If the target has a fluid tank
Output:
Boolean
Fluid Amount
The amount of fluid in mB in the active tank
Output:
Integer
Properties:
Tank ID
Total Fluid Amount
The total amount of fluid in mB
Output:
Integer
Fluid Capacity
Total capacity of the active tank
Output:
Integer
Properties:
Tank ID
Total Fluid Capacity
Total capacity of the tank
Output:
Integer
Tanks
The amount of tanks found at the target
Output:
Integer
Fluid Fill Ratio
The amount of fluid in the active tank divided by its capacity
Output:
Double
Properties:
Tank ID
Tank Fluids
The list of fluids in all tanks
Output:
List
Tank Capacities
The list of capacities of all tanks
Output:
List
Tank Fluid
The fluid in the target tank
Output:
Fluid
Properties:
Tank ID
Block Fluid
The fluid in the target block
Output:
Fluid
RS network fluids
A list of all the fluids in the RS network.
Output:
List

## https://integrateddynamics.rubensworks.net/book/manual/parts/reader/index.html

Readers
Introduction
Audio
Block
Entity
Extra-Dimensional
Fluid
Inventory
Machine
Network
Redstone
World

## https://integrateddynamics.rubensworks.net/book/manual/parts/reader/introduction.html

Introduction
The simplest method for creating a dynamic variable is done using
Readers
. Different types of readers exist for reading information from the world and storing it inside a
Variable Card
. Each
Reader
contains one or more
Aspects
for reading different things with different
Value Types
.

## https://integrateddynamics.rubensworks.net/book/manual/parts/reader/inventory.html

Inventory
Read information from the facing inventory, like the amount of slots and items. This will not transfer items itself.
Crafting Table
[Crafting Table]
[Chest]
[Trapped Chest]
[Chest]
[Trapped Chest]
[Input Variable Transformer]
[Chest]
[Trapped Chest]
→
[Inventory Reader]
Aspect
Inventory Full
If all slots have at least one item
Output:
Boolean
Inventory Empty
If all slots are empty
Output:
Boolean
Inventory Not Empty
If there is at least one item
Output:
Boolean
Is Inventory
If the target has an inventory
Output:
Boolean
Inventory Count
Get the total amount of items
Output:
Integer
Slots
The number of slots in the inventory
Output:
Integer
Slots Filled
The number of slots that have an item
Output:
Integer
Fill Ratio
The number of non-empty slots divided by the total number of slots
Output:
Double
Items
Get a list of the items in the inventory
Output:
List
Slot Item
Get the Item in the active slot
Output:
Item
Properties:
Slot ID
RS network items
A list of all the items in the RS network.
Output:
List
RS craftable items
A list of all the craftable items in the RS network.
Output:
List
RS crafting items
A list of all the items that are currently being crafted in the RS network.
Output:
List

## https://integrateddynamics.rubensworks.net/book/manual/parts/reader/machine.html

Machine
The machine this part is facing can be read, such as the work they are doing.
Crafting Table
[Crafting Table]
[Furnace]
[Iron Ingot]
[Input Variable Transformer]
[Iron Ingot]
[Crafting Table]
→
[Machine Reader]
Aspect
Is Worker
If the target is a worker machine
Output:
Boolean
Has Work
If the target has work to process
Output:
Boolean
Can Work
If the target would be able to start working in its current state
Output:
Boolean
Is Working
If the target is currently working
Output:
Boolean
Has Temperature
If the target has a certain temperature
Output:
Boolean
Temperature
The current temperature of the target in Kelvin
Output:
Double
Maximum Temperature
The maximum temperature of the target in Kelvin
Output:
Double
Minimum Temperature
The minimum temperature of the target in Kelvin
Output:
Double
Default Temperature
The default temperature of the target in Kelvin
Output:
Double
Is Recipe Handler
If the target can handle recipes
Output:
Boolean
Recipes
Recipes that can be handled by the target
Output:
List
Recipe By Input
An operator that returns a recipe for a given input.
Output:
Operator
Recipe By Output
An operator that returns a recipe for a given output.
Output:
Operator
Recipes By Input
An operator that returns all possible recipes for a given input.
Output:
Operator
Recipes By Output
An operator that returns all possible recipes for a given output.
Output:
Operator
Recipe Output By Input
An operator that returns the resulting recipe output for a given input.
Output:
Operator
Recipe Inputs By Output
An operator that returns all possible recipe inputs for a given output.
Output:
Operator
Is FE Handler
If the target in some way handles FE
Output:
Boolean
Is FE Receiver
If the target can receive FE
Output:
Boolean
Is FE Provider
If the target can provide FE
Output:
Boolean
Can Extract FE
If FE can really be extracted from the target, takes storage into account
Output:
Boolean
Can Insert FE
If FE can really be inserted into the target, takes storage and capacity into account
Output:
Boolean
Is FE Buffer Full
If the target's FE buffer is completely full
Output:
Boolean
Is FE Buffer Empty
If the target's FE buffer is completely empty
Output:
Boolean
Is FE Buffer Not Empty
If the target's FE buffer is not empty
Output:
Boolean
Stored FE
The amount of FE stored in the target
Output:
Integer
FE Capacity
The FE capacity of the target
Output:
Integer
FE Fill Ratio
The amount of FE in the target divided by its capacity
Output:
Double
Is RS Network
If the target has a Refined Storage network.
Output:
Boolean
Fission Reactor Damage
Damage factor of the given Fission Reactor
Output:
Double
Fission Reactor Actual Burn Rate
The actual burn rate of the given Fission Reactor
Output:
Double
Fission Reactor Maximum Burn Rate
The maximum possible burn rate of the given Fission Reactor
Output:
Double
Fission Reactor Set Burn Rate
The configured burn rate of the given Fission Reactor
Output:
Double
Fission Reactor Heating Rate
The amount of coolant being heated (mB/t) of the given Fission Reactor
Output:
Long
Fission Reactor Environmental Loss
The environmental loss of the given Fission Reactor
Output:
Double

## https://integrateddynamics.rubensworks.net/book/manual/parts/reader/network.html

Network
Read information about the
Integrated Dynamics
network this part is facing. This does not have to be the same network as the network this part is on.
Crafting Table
[Crafting Table]
[Logic Cable]
[Logic Cable]
[Input Variable Transformer]
[Logic Cable]
[Logic Cable]
→
[Network Reader]
Aspect
Is Network
If the target has a network
Output:
Boolean
Elements
The amount of elements in the target network
Output:
Integer
Energy Batteries
The amount of energy batteries in the target network
Output:
Integer
Energy Stored
The amount of energy available in the target network
Output:
Integer
Properties:
Channel
Energy Capacity
The energy capacity of the network
Output:
Integer
Properties:
Channel
Energy Consumption Rate
The current energy consumption rate of all ID parts within the target network per tick
Output:
Integer
Value
The value that is being exposed by the target
Output:
Any
Variable Value By ID
An operator that returns the value of a variable by its integer identifier
Output:
Operator
Network Item Count
Get the total amount of items in the network
Output:
Long
Properties:
Channel
Network Item Max Count
The upper limit of the total amount of items that can be stored in the network
Output:
Long
Properties:
Channel
Network Items
Get a list of all items in the network
Output:
List
Properties:
Channel
Network Count Of Item
Get the total count of the given item in the network
Output:
Operator
Properties:
Channel
Network Item Interfaces
Get number of valid interfaces for item storage
Output:
Integer
Properties:
Channel
Network Fluid Count
Get the total amount of fluids in the network
Output:
Long
Properties:
Channel
Network Fluid Max Count
The upper limit of the total amount of fluids that can be stored in the network
Output:
Long
Properties:
Channel
Network Fluids
Get a list of all fluids in the network
Output:
List
Properties:
Channel
Network Count Of Fluid
Get the total count of the given fluid in the network
Output:
Operator
Properties:
Channel
Network Fluid Interfaces
Get number of valid interfaces for fluid storage
Output:
Integer
Properties:
Channel
Network Chemical Count
Get the total amount of chemicals in the network
Output:
Long
Properties:
Channel
Network Chemical Max Count
The upper limit of the total amount of chemicals that can be stored in the network
Output:
Long
Properties:
Channel
Network Chemicals
Get a list of all chemicals in the network
Output:
List
Properties:
Channel
Network Count Of Chemical
Get the total count of the given chemical in the network
Output:
Operator
Properties:
Channel
Network Chemical Interfaces
Get the number of valid interfaces for chemical storage
Output:
Integer
Properties:
Channel
Recipes
A list of all available recipes.
Output:
List
Properties:
Channel
Active Crafting Jobs
A list of all active crafting jobs as recipes.
Output:
List
Properties:
Channel
Crafting Ingredients
A list of all ingredients that are being crafted.
Output:
List
Properties:
Channel

## https://integrateddynamics.rubensworks.net/book/manual/parts/reader/redstone.html

Redstone
Read redstone information from the facing block.
Crafting Table
[Crafting Table]
[Block of Redstone]
[Redstone Dust]
[Input Variable Transformer]
[Redstone Dust]
→
[Redstone Reader]
Aspect
Redstone Low
If there is no redstone level
Output:
Boolean
Redstone
If there is a redstone level
Output:
Boolean
Redstone High
If the redstone level is at its maximum
Output:
Boolean
Redstone Clock
Pulses with a configurable length and interval.
Output:
Boolean
Properties:
Pulse Interval
Pulse Length
Pulse Time Offset
Redstone
Get the exact redstone level
Output:
Integer
Comparator
Get the comparator value
Output:
Integer

## https://integrateddynamics.rubensworks.net/book/manual/parts/reader/world.html

World
Information about the world this part is in, like the current weather and time.
Crafting Table
[Crafting Table]
[Dirt]
[Obsidian]
[Crying Obsidian]
[Dirt]
[Obsidian]
[Crying Obsidian]
[Input Variable Transformer]
[Obsidian]
[Crying Obsidian]
[Dirt]
[Obsidian]
[Crying Obsidian]
[Dirt]
→
[World Reader]
Aspect
Weather: Clear
If there is a clear weather in the world
Output:
Boolean
Weather: Rain
If it is raining in the world
Output:
Boolean
Weather: Thunder
If there is a thunderstorm in the world
Output:
Boolean
Is Day
If it is day in the target world
Output:
Boolean
Is Night
If it is night in the target world
Output:
Boolean
Rain Countdown
The amount of ticks until it will rain in this world
Output:
Integer
Tick time
The average tick time in milliseconds for this world
Output:
Integer
Day Time
The time that has passed in the target world day
Output:
Integer
Light Level
The light level at the target
Output:
Integer
TPS
The number of ticks per second in this world
Output:
Double
Time
The world time dependent of the set time command
Output:
Long
Total Time
The world time independent of the set time command
Output:
Long
World Name
The target world name
Output:
String
Players
The players that are in this world
Output:
List
Radiation
Get the radiation level at the target (in Sv/h)
Output:
Double

## https://integrateddynamics.rubensworks.net/book/manual/parts/settings.html

Settings
Most parts have a
Settings
screen using which basic part settings can be modified.
The Target Side is the side that this part will interact with. It defaults to the opposite of the side of the cable this part was placed upon.
The Ticks/Operation allows you to configure the ticking frequency of this part. The higher, the slower it operates.
The Priority determines the order in which this part is executed within a single network tick.
The Energy Channel indicates the channel from which energy should be consumed when this part ticks. This is only applicable if energy consumption for networks is enabled.
The configuration of a part can be copied to other parts using a
Wrench
. Shift+right-clicking a part copies it into the Wrench, and right-clicking another part pastes it. The Wrench mode picks what is copied:
Copy All
takes everything and only pastes onto a part of the same type,
Copy Settings
takes the update interval, priority, channel, side and offsets, and
Copy Aspect
takes the active aspect with its variable card and all aspect settings. The last two can be pasted onto any part. Only settings that you changed yourself are copied, so pasting never resets anything you left alone. Each pasted variable card consumes one blank
Variable Card
from your inventory, and raising the maximum offset of a part consumes
Offset Enhancements
.
Right-clicking with a
Wrench
that holds a configuration, while not pointing at a part, opens a screen that lists everything it is going to paste: the part settings, the aspect that the copied part was writing with its variable card, and every aspect setting that was changed. Anything in that list can be switched off, so that pasting leaves that part of the target alone, and the button next to the screen switches everything on or off at once. A variable card that the target part already holds is left as it is, so it costs you no blank card.

## https://integrateddynamics.rubensworks.net/book/manual/parts/writer/audio.html

Audio
Play sounds and music.
Crafting Table
[Crafting Table]
[Note Block]
[Note Block]
[Output Variable Transformer]
[Note Block]
[Note Block]
→
[Audio Writer]
Aspect
Harp Note
Output a harp note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Base Drum Note
Output a base drum note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Snare Note
Output a snare note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Hat Note
Output a hat note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Bass Note
Output a bass note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Flute Note
Output a flute note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Bell Note
Output a bell note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Guitar Note
Output a guitar note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Chime Note
Output a chime note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Xylophone Note
Output a xylophone note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Iron Xylophone Note
Output an iron xylophone note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Cow Bell Note
Output a cow bell note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Didgeridoo Note
Output a didgeridoo note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Bit Note
Output a bit note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Banjo Note
Output a banjo note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Pling Note
Output a pling note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Zombie Note
Output a zombie note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Skeleton Note
Output a skeleton note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Creeper Note
Output a creeper note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Dragon Note
Output a dragon note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Wither Skeleton Note
Output a wither skeleton note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Piglin Note
Output a piglin note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Head Note
Output a head note, expected range [0, 24]
Input:
Integer
Properties:
Volume
Sound
Play the given sound name
Input:
String
Properties:
Volume
Frequency
Text
Convert the given text to speech
Input:
String
Properties:
Range

## https://integrateddynamics.rubensworks.net/book/manual/parts/writer/effect.html

Effect
Emit particles based on certain values and settings.
Crafting Table
[Crafting Table]
[Fire Charge]
[Fire Charge]
[Output Variable Transformer]
[Fire Charge]
[Fire Charge]
→
[Effect Writer]
Aspect
Particle: Angry Villager
Emit an angry villager particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Bubble
Emit a bubble particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Cloud
Emit a cloud particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Crit
Emit a crit particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Damage Indicator
Emit a damage indicator particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Dragon Breath
Emit a dragon breath particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Dripping Lava
Emit a dripping lava particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Falling Lava
Emit a falling lava particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Landing Lava
Emit a landing lava particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Dripping Water
Emit a dripping water particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Falling Water
Emit a falling water particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Effect
Emit an effect particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Elder Guardian
Emit an elder guardian particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Enchanted Hit
Emit an enchanted hit particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Enchanted
Emit an enchanted particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: End Rod
Emit an end rod particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Explosion Emitter
Emit an explosion emitter particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Explosion
Emit an explosion particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Sonic Boom
Emit a sonic boom particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Firework
Emit a firework particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Fishing
Emit a fishing particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Flame
Emit a flame particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Sculk Soul
Emit a Sculk Soul particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Sculk Charge Pop
Emit a Sculk Charge Pop particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Soul Fire Flame
Emit a Soul Fire Flame particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Soul
Emit a Soul particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Flash
Emit a Flash particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Happy Villager
Emit a happy villager particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Composter
Emit a composter particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Heart
Emit a heart particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Instant Effect
Emit an instant effect particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Slime
Emit a slime particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Snowball
Emit a snowball particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Large Smoke
Emit a large smoke particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Lava
Emit a lava particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Mycelium
Emit a mycelium particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Note
Emit a note particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Poof
Emit a poof particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Portal
Emit a portal particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Rain
Emit a rain particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Smoke
Emit a smoke particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Sneeze
Emit a sneeze particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Spit
Emit a spit particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Squid Ink
Emit a squid ink particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Sweep Attack
Emit a sweep attack particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Totem of Undying
Emit a totem of undying particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Underwater
Emit an underwater particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Splash
Emit a splash particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Witch
Emit a witch particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Bubble Pop
Emit a bubble pop particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Downwards Current
Emit a downwards current particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Upwards Current
Emit an upwards current particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Nautilus
Emit a nautilus particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Dolphin
Emit a dolphin particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Cosy Campfire Smoke
Emit a cosy campfire smoke particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Signal Campfire Smoke
Emit a signal campfire smoke particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Dripping Honey
Emit a dripping honey particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Falling Honey
Emit a falling honey particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Landing Honey
Emit a landing honey particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Falling Nectar
Emit a falling nectar particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Falling Spore Blossom
Emit a falling spore blossom particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Ash
Emit an ash particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Crimson Spore
Emit a crimson spore particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Warped Spore
Emit a warped spore particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Spore Blossom Air
Emit a spore blossom air particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Dripping Obsidian Tear
Emit a dripping obsidian tear particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Falling Obsidian Tear
Emit a falling obsidian tear particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Landing Obsidian Tear
Emit a landing obsidian tear particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Reverse Portal
Emit a reverse portal particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: White Ash
Emit a white ash particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Small Flame
Emit a small flame particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Snowflake
Emit a snowflake particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Dripping Dripstone Lava
Emit a dripping dripstone lava particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Falling Dripstone Lava
Emit a falling dripstone lava particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Dripping Dripstone Water
Emit a dripping dripstone water particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Falling Dripstone Water
Emit a falling dripstone water particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Glow Squid Ink
Emit a glow squid ink particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Glow
Emit a glow particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Wax On
Emit a wax on particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Wax Off
Emit a wax off particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Electric Spark
Emit an electric spark particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread
Particle: Scrape
Emit a scrape particle with a certain velocity.
Input:
Double
Properties:
X Offset
Y Offset
Z Offset
Particle Count
X Spread
Y Spread
Z Spread

## https://integrateddynamics.rubensworks.net/book/manual/parts/writer/entity.html

Entity
Configure and control entities.
Crafting Table
[Crafting Table]
[Raw Porkchop]
[Raw Porkchop]
[Output Variable Transformer]
[Raw Porkchop]
[Raw Porkchop]
→
[Entity Writer]
Aspect

## https://integrateddynamics.rubensworks.net/book/manual/parts/writer/index.html

Writers
Audio
Effect
Entity
Machine
Inventory
Redstone

## https://integrateddynamics.rubensworks.net/book/manual/parts/writer/inventory.html

Inventory
Perform actions related to inventories and pipes. This will not transfer items itself.
Crafting Table
[Crafting Table]
[Chest]
[Trapped Chest]
[Chest]
[Trapped Chest]
[Output Variable Transformer]
[Chest]
[Trapped Chest]
→
[Inventory Writer]
Aspect
RS craft item
Trigger a crafting job for the given item.
Input:
Item
Properties:
Skip If In Storage
Crafting Count
RS craft items
Trigger a crafting job for all the items in the given list.
Input:
List
Properties:
Skip If In Storage
Crafting Count
RS cancel all crafting jobs
Cancel all running crafting jobs in the RS network.
Input:
Boolean
RS cancel crafting jobs
Cancel all running crafting jobs for the given item in the RS network.
Input:
Item
RS cancel crafting jobs
Cancel all running crafting jobs for the given items in the RS network.
Input:
List

## https://integrateddynamics.rubensworks.net/book/manual/parts/writer/machine.html

Machine
Configure and control machines.
Crafting Table
[Crafting Table]
[Furnace]
[Iron Ingot]
[Output Variable Transformer]
[Iron Ingot]
[Crafting Table]
→
[Machine Writer]
Aspect

## https://integrateddynamics.rubensworks.net/book/manual/parts/writer/redstone.html

Redstone
Output a certain redstone level.
Crafting Table
[Crafting Table]
[Block of Redstone]
[Redstone Dust]
[Output Variable Transformer]
[Redstone Dust]
→
[Redstone Writer]
Aspect
Redstone
Outputs a full redstone signal if true
Input:
Boolean
Properties:
Strong Power
Redstone
Outputs the value as exact redstone signal
Input:
Integer
Properties:
Strong Power
Redstone Pulse
Outputs a pulse with the configured redstone value if the given value changes.
Input:
Boolean
Properties:
Strong Power
Pulse Value
Pulse Length
Redstone Pulse
Outputs a pulse with the configured redstone value if the given value changes.
Input:
Integer
Properties:
Strong Power
Pulse Value
Pulse Length

## https://integrateddynamics.rubensworks.net/book/tutorials/advancedNetworkComponents.html

Advanced Network Components
Now that you are familiar with basic logic, we can have a look at the more advanced network components: the
Materializer
and the
Proxy
.
As you may remember, the
Logic Programmer
allows you to create variables with
static
values, or you can create more complex variables based on
dynamic
value evaluation.
The
Materializer
can be used for converting a
dynamic
value to a
static
one. This can be done by inserting the
dynamic
Variable Card
in the top slot of the
Materializer
. Every
Variable Card
that is inserted into the left slot, will be set with the value the top variable has at that moment.
The
Proxy
allows you to add an extra level of indirection for your variables. You may for example have a logic program that requires a
static
value in there. If at any point in the future, you want to change that value, you would have to rewrite your program, unless that value was
proxied
, in which case you would simply have to update the value in the
Proxy
.
For this tutorial, create materialized and proxied
Variable Cards
.
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Variable Card]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Variable Card]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Redstone Dust]
[Crystallized Menril Chunk]
→
[Materializer]
Crafting Table
[Crafting Table]
[Variable Card]
[Crystallized Menril Chunk]
[Variable Card]
[Crystallized Menril Chunk]
[Variable Store]
[Crystallized Menril Chunk]
[Variable Card]
[Crystallized Menril Chunk]
[Variable Card]
→
[Proxy]
Advancements
Variable Materialization
Materialize a variable in a §lMaterializer§r.
Variable Proxying
Create a valid proxy variable in a §lProxy§r.
Rewards
[Materializer]
5
[Proxy]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/aspects.html

Aspects
As a simple example, let’s try reading in a redstone value and displaying it.
Do this by attaching a
Redstone Reader
to a
Logic Cable
in the world. Make sure that the reader is targeting something with a redstone value, like a
Redstone Torch
.
After that, right-click on the
Redstone Reader
to open the gui, after which you have to insert a
Variable Card
into the
Integer
Redstone Value
aspect, to bind the target’s redstone value to that
Variable Card
.
Next, place a
Display Panel
in the same network as the
Redstone Reader
, and insert your bound
Variable Card
to display its value.
As a next step, craft a
Redstone Writer
and let it write a maximal integer redstone value that was read by a Redstone Reader.
Crafting Table
[Crafting Table]
[Block of Redstone]
[Redstone Dust]
[Output Variable Transformer]
[Redstone Dust]
→
[Redstone Writer]
Aspect
Redstone
Get the exact redstone level
Output:
Integer
Aspect
Redstone
Outputs the value as exact redstone signal
Input:
Integer
Properties:
Strong Power
Advancements
Redstone Capturing
Make a variable card for a redstone §6Integer§7 value.
Redstone Observement
Place an §6Integer§7 variable card in a Display Panel.
Redstone Transmission
Let a Redstone Writer write a maximal integer redstone level that was read by a Redstone Reader.
Rewards
[Redstone Reader]
5
[Redstone Writer]
5
[Variable Card]
16

## https://integrateddynamics.rubensworks.net/book/tutorials/basicNetworkComponents.html

Basic Network Components
One of the methods for binding
Variable Cards
to certain values, is by reading in values from
Readers
. Let’s start by crafting some basic readers. After that, craft a
Display Panel
, which is useful for displaying the values of
Variable Cards
.
Crafting Table
[Crafting Table]
[Block of Redstone]
[Redstone Dust]
[Input Variable Transformer]
[Redstone Dust]
→
[Redstone Reader]
Crafting Table
[Crafting Table]
[Cobblestone]
[Infested Cobblestone]
[Mossy Cobblestone]
[Cobbled Deepslate]
[Dirt]
[Cobblestone]
[Infested Cobblestone]
[Mossy Cobblestone]
[Cobbled Deepslate]
[Dirt]
[Input Variable Transformer]
[Dirt]
[Cobblestone]
[Infested Cobblestone]
[Mossy Cobblestone]
[Cobbled Deepslate]
[Dirt]
[Cobblestone]
[Infested Cobblestone]
[Mossy Cobblestone]
[Cobbled Deepslate]
→
[Block Reader]
Crafting Table
[Crafting Table]
[Chest]
[Trapped Chest]
[Chest]
[Trapped Chest]
[Input Variable Transformer]
[Chest]
[Trapped Chest]
→
[Inventory Reader]
Crafting Table
[Crafting Table]
[Furnace]
[Iron Ingot]
[Input Variable Transformer]
[Iron Ingot]
[Crafting Table]
→
[Machine Reader]
Crafting Table
[Crafting Table]
[Menril Berries]
[Menril Berries]
[Output Variable Transformer]
[Redstone Dust]
[Menril Berries]
→
[Static Light Panel]
Crafting Table
[Crafting Table]
[Menril Berries]
[Static Light Panel]
[Output Variable Transformer]
[Static Light Panel]
[Menril Berries]
→
[Display Panel]
Advancements
Redstone Reading
Craft a Redstone Reader
Block Reading
Craft a Block Reader
Inventory Reading
Craft an Inventory Reader
Machine Reading
Craft a Machine Reader
Value Displaying
Craft a Display Panel
Rewards
[Variable Card]
10
[Display Panel]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/networkFoundations.html

Network Foundations
The main goal of this mod is for building control networks for automation. This is done using
Logic Cables
. Try making a network with ten of those cables.
The next step will be to craft some
Variable Cards
and
Variable Transformers
.
Variable Cards
are used to store references to values in the network.
Variable Transformers
are crafting elements for components that can be placed on
Logic Cables
for reading and writing these values.
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Stick]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Redstone Dust]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Stick]
[Crystallized Menril Chunk]
→
[Logic Cable]
3
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Redstone Dust]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
→
[Wrench]
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Paper]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
→
[Variable Card]
24
Crafting Table
[Crafting Table]
[Variable Card]
[Crystallized Menril Chunk]
[Sticky Piston]
[Crystallized Menril Chunk]
[Variable Card]
→
[Input Variable Transformer]
4
Crafting Table
[Crafting Table]
[Variable Card]
[Crystallized Menril Chunk]
[Piston]
[Crystallized Menril Chunk]
[Variable Card]
→
[Output Variable Transformer]
4
Advancements
Cables, for logic
Craft a Logic Cable
Networks, for logic
Build a logic network with at least 10 connected Logic Cables.
Menril Wrenching
Craft a Wrench
Variables, For Values
Craft a Variable Card
Variable Input Transformation
Craft a Variable Input Transformer
Variable Output Transformation
Craft a Variable Output Transformer
Rewards
[Logic Cable]
64
[Input Variable Transformer]
16
[Output Variable Transformer]
16

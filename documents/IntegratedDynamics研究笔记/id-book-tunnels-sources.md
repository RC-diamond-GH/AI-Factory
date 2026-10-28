## https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/concepts.html

Concepts
Integrated Tunnels
adds 4 part types for the different transport types, namely
Importers
,
Exporters
and two types of
Interfaces
.
Importers (blue) always try to pull from a target, to Interfaces (white) in the same network.
Exporters (orange) always try to push to a target, from Interfaces (white) in the same network. Items in this case will always transfer from left to right.
You could add multiple interfaces to your network, and assign priorities to specify which should be filled/emptied first.
If you want to specify what ingredients can go through an interface, you can use a filtering interface. This can be useful if you for example want to lock inventories to only contain certain items.
Furthermore, you can also add multiple Importers and Exporters, again with relative priorities.
Using
Integrated Dynamics
variables, you have to specify what items should be moved. If you want to keep it simple, and just move everything, simply use a blank
Variable Card
.
Note that if you have an autocrafting mod such as
Integrated Crafting
installed, then you will be able to use the
Craft
setting in exporters. This will make it so that if an ingredient is not available for export, it will be autocrafted first.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/energy.html

Energy
Move energy over the network. Import energy into the network using
Energy Importers
into
Energy Interfaces
. Export energy from the network using
Energy Exporters
from
Energy Interfaces
. Dynamically filter if energy can go through an interface using a
Filtering Energy Interface
.
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Energy Battery]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
→
[Energy Interface]
4
Crafting Table
[Crafting Table]
[Energy Interface]
[Output Variable Transformer]
→
[Energy Exporter]
Crafting Table
[Crafting Table]
[Energy Interface]
[Input Variable Transformer]
→
[Energy Importer]
Crafting Table
[Crafting Table]
[Energy Interface]
[Energy Interface]
→
[Filtering Energy Interface]
Aspect
Export Energy
Export energy if the boolean is true
Input:
Boolean
Properties:
Channel
Energy Transfer Rate
Craft
Passive Interaction
Passive Interaction Ignores Filter
Check Energy Amount
Export Energy Amount
Export the given amount of energy each tick.
Input:
Long
Properties:
Channel
Energy Transfer Rate
Craft
Passive Interaction
Passive Interaction Ignores Filter
Check Energy Amount
Aspect
Import Energy
Import energy if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Energy Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Energy Amount
Import Energy Amount
Import the given amount of energy each tick.
Input:
Long
Properties:
Channel
Round-Robin
Passive Interaction
Passive Interaction Ignores Filter
Check Energy Amount
Aspect
Filter All Energy
Pass energy if the boolean is true
Input:
Boolean
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/fluid.html

Fluids
Move fluids over the network. Import fluids into the network using
Fluid Importers
into
Fluid Interfaces
. Export fluids from the network using
Fluid Exporters
from
Fluid Interfaces
. Dynamically filter what fluids can go through an interface using a
Filtering Fluid Interface
.
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Bucket]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
→
[Fluid Interface]
4
Crafting Table
[Crafting Table]
[Fluid Interface]
[Output Variable Transformer]
→
[Fluid Exporter]
Crafting Table
[Crafting Table]
[Fluid Interface]
[Input Variable Transformer]
→
[Fluid Importer]
Crafting Table
[Crafting Table]
[Fluid Interface]
[Fluid Interface]
→
[Filtering Fluid Interface]
Aspect
Export All Fluids
Export fluids if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Fluid Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Fluid Amount
Export Fluid Amount
Export the given amount of fluids each tick.
Input:
Integer
Properties:
Channel
Passive Interaction
Passive Interaction Ignores Filter
Check Fluid Amount
Export Fluid
Export the given fluid.
Input:
Fluid
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Fluid Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Fluid Amount
Check NBT
Craft
Export Fluids
Export the given fluids.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Fluid Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Fluid Amount
Check NBT
Export Fluids Predicate
Export all fluids that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Fluid Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Fluid Amount
Slot-based
Export Fluids NBT
Export all fluids that match the given NBT tag.
Input:
NBT
Properties:
Channel
Fluid Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
NBT subset
NBT superset
Require Fluid NBT
Check NBT Recursively
Aspect
Import All Fluids
Import fluids if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Fluid Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Fluid Amount
Import Fluid Amount
Import the given amount of fluids each tick.
Input:
Integer
Properties:
Channel
Passive Interaction
Passive Interaction Ignores Filter
Check Fluid Amount
Import Fluid
Import the given fluid.
Input:
Fluid
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Fluid Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Fluid Amount
Check NBT
Import Fluids
Import the given fluids.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Fluid Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Fluid Amount
Check NBT
Import Fluids Predicate
Import all fluids that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Fluid Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Fluid Amount
Slot-based
Import Fluids NBT
Import all fluids that match the given NBT tag.
Input:
NBT
Properties:
Channel
Fluid Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
NBT subset
NBT superset
Require Fluid NBT
Check NBT Recursively
Aspect
Filter All Fluids
Pass fluids if the boolean is true
Input:
Boolean
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Filter Fluid
Only pass the given fluid.
Input:
Fluid
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Blacklist
Fluid Transfer Rate
Check Fluid Amount
Check NBT
Filter Fluids
Pass the given fluids.
Input:
List
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Blacklist
Fluid Transfer Rate
Check Fluid Amount
Check NBT
Filter Fluids Predicate
Pass all fluids that match the given predicate.
Input:
Operator
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Filter Fluids NBT
Pass all fluids that match the given NBT tag.
Input:
NBT
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Blacklist
NBT subset
NBT superset
Require Fluid NBT
Check NBT Recursively

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/index.html

Tunnels
Introduction
Concepts
Items
Fluids
Energy
World Interaction
Player Simulation

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/introduction.html

Introduction
Integrated Tunnels
is an extension to
Integrated Dynamics
to transfer items, fluids and energy over
Integrated Dynamics
networks.
You can use
Integrated Dynamics
logic to configure filters, which can be as complex as you desire.
"I want to send 3 enchantable pickaxes with a damage value less than 10 to this chest every tick"
or
"I want to fill this tank until it has 1000mB of oil, but only when my machine is working and if I have less than 10 iron ingots in this chest"
No problem!
Integrated Tunnels
can do that for you, and it will do it efficiently as well!

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/item.html

Items
Move items over the network. Import items into the network using
Item Importers
into
Item Interfaces
. Export items from the network using
Item Exporters
from
Item Interfaces
. Dynamically filter what items can go through an interface using a
Filtering Item Interface
.
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Chest]
[Trapped Chest]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
→
[Item Interface]
4
Crafting Table
[Crafting Table]
[Item Interface]
[Output Variable Transformer]
→
[Item Exporter]
Crafting Table
[Crafting Table]
[Item Interface]
[Input Variable Transformer]
→
[Item Importer]
Crafting Table
[Crafting Table]
[Item Interface]
[Item Interface]
→
[Filtering Item Interface]
Aspect
Export All Items
Export items if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Export Item Amount
Export the given amount of items each tick.
Input:
Integer
Properties:
Channel
Round-Robin
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Export Item Slot
Export items to the given slot. -1 is any slot, -2 or less is no slot.
Input:
Integer
Properties:
Channel
Round-Robin
Item Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Export Item
Export the given item.
Input:
Item
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Check NBT
Empty is Wildcard
Craft
Export Items
Export the given items.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Check NBT
Craft
Export Items Predicate
Export all items that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Slot-based
Export Items NBT
Export all items that match the given NBT tag.
Input:
NBT
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
NBT subset
NBT superset
Require Item NBT
Check NBT Recursively
Aspect
Import All Items
Import items if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Import Item Amount
Import the given amount of items each tick.
Input:
Integer
Properties:
Channel
Round-Robin
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Import Item Slot
Import items from the given slot. -1 is any slot, -2 or less is no slot.
Input:
Integer
Properties:
Channel
Round-Robin
Item Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Import Item
Import the given item.
Input:
Item
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Check NBT
Empty is Wildcard
Import Items
Import the given items.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Check NBT
Craft
Import Items Predicate
Import all items that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Slot-based
Import Items NBT
Import all items that match the given NBT tag.
Input:
NBT
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
NBT subset
NBT superset
Require Item NBT
Check NBT Recursively
Aspect
Filter All Items
Pass items if the boolean is true
Input:
Boolean
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Filter Item
Only pass the given item.
Input:
Item
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Blacklist
Item Transfer Rate
Check Stack Size
Check NBT
Empty is Wildcard
Filter Items
Pass the given items.
Input:
List
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Blacklist
Item Transfer Rate
Check Stack Size
Check NBT
Empty is Wildcard
Filter Items Predicate
Pass all items that match the given predicate.
Input:
Operator
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Filter Items NBT
Pass all items that match the given NBT tag.
Input:
NBT
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Blacklist
NBT subset
NBT superset
Require Item NBT
Check NBT Recursively

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/player.html

Player Simulation
Any experienced Minecrafter knows that the less you have to click, the better. With the
Player Simulator
, you can automate away a large part of this tedious clicking. It allows you to automate clicking with any hand and item. Advanced properties allow you to change things like click positions and so on.
Crafting Table
[Crafting Table]
[World Item Exporter]
[World Block Importer]
[World Item Importer]
[Emerald]
[World Block Exporter]
→
[Player Simulator]
Aspect
Click Empty
Click with an empty hand.
Input:
Boolean
Properties:
Channel
Round-Robin
Right Click
Right Hand
Continuous Click
Sneak
Right Click Duration
Network Inventory
Entity Index
Offset X
Offset Y
Offset Z
Click
Click with any item.
Input:
Boolean
Properties:
Channel
Round-Robin
Right Click
Right Hand
Continuous Click
Sneak
Right Click Duration
Network Inventory
Item Transfer Rate
Entity Index
Offset X
Offset Y
Offset Z
Click Amount
Click with the given amount of items.
Input:
Integer
Properties:
Channel
Round-Robin
Right Click
Right Hand
Continuous Click
Sneak
Right Click Duration
Network Inventory
Entity Index
Offset X
Offset Y
Offset Z
Click Item
Click with the given item.
Input:
Item
Properties:
Channel
Round-Robin
Blacklist
Craft
Right Click
Right Hand
Continuous Click
Sneak
Right Click Duration
Network Inventory
Check Stack Size
Check NBT
Item Transfer Rate
Entity Index
Offset X
Offset Y
Offset Z
Click Items
Click with the given items.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Right Click
Empty is Wildcard
Right Hand
Continuous Click
Sneak
Right Click Duration
Network Inventory
Check Stack Size
Check NBT
Item Transfer Rate
Entity Index
Offset X
Offset Y
Offset Z
Click Item Predicate
Click with the items that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Right Click
Right Hand
Continuous Click
Sneak
Right Click Duration
Network Inventory
Item Transfer Rate
Entity Index
Offset X
Offset Y
Offset Z
Click Item NBT
Click with the items that match the given NBT tag.
Input:
NBT
Properties:
Channel
Round-Robin
Right Click
Right Hand
Continuous Click
Sneak
Right Click Duration
Network Inventory
Item Transfer Rate
Entity Index
Offset X
Offset Y
Offset Z
NBT subset
NBT superset
Require Item NBT
Check NBT Recursively

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/world/block.html

Block
The
World Block Importers
and
World Block Exporters
allow you to place blocks and break them. Various aspect properties such as silk touching are available.
Crafting Table
[Crafting Table]
[Item Exporter]
[Logic Director]
→
[World Block Exporter]
Crafting Table
[Crafting Table]
[Item Importer]
[Logic Director]
→
[World Block Importer]
Aspect
Place All Item Blocks
Place all item blocks in the world.
Input:
Boolean
Properties:
Channel
Round-Robin
Cause Block Update
Right Hand
Ignore Replacable
Place Item Block
Place the given item block in the world.
Input:
Item
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Check NBT
Cause Block Update
Right Hand
Ignore Replacable
Craft
Place Item Blocks
Place the given item blocks in the world.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Check NBT
Cause Block Update
Right Hand
Ignore Replacable
Craft
Place Item Blocks Predicate
Place the item blocks in the world that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Cause Block Update
Right Hand
Ignore Replacable
Place Item Blocks NBT
Place the item blocks in the world that match the given NBT tag.
Input:
NBT
Properties:
Channel
Round-Robin
Blacklist
Cause Block Update
Right Hand
Ignore Replacable
NBT subset
NBT superset
Require Item NBT
Check NBT Recursively
Place Block
Place the given block in the world.
Input:
Block
Properties:
Channel
Round-Robin
Cause Block Update
Right Hand
Ignore Replacable
Blacklist
Empty is Wildcard
Craft
Place Blocks
Place the given blocks in the world.
Input:
List
Properties:
Channel
Round-Robin
Cause Block Update
Right Hand
Ignore Replacable
Blacklist
Place Blocks Predicate
Place the blocks in the world that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Cause Block Update
Right Hand
Ignore Replacable
Aspect
Pick Up All Item Blocks
Pick up all item blocks in the world.
Input:
Boolean
Properties:
Channel
Round-Robin
Cause Block Update
Right Hand
Silk Touch
Ignore Replacable
Break Block When No Drops
Match Block
Pick Up Item Block
Pick up the given item block in the world.
Input:
Item
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Check NBT
Cause Block Update
Right Hand
Silk Touch
Ignore Replacable
Break Block When No Drops
Match Block
Pick Up Item Blocks
Pick up the given item blocks in the world.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Check NBT
Cause Block Update
Right Hand
Silk Touch
Ignore Replacable
Break Block When No Drops
Match Block
Pick Up Item Blocks Predicate
Pick up the item blocks in the world that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Cause Block Update
Right Hand
Silk Touch
Ignore Replacable
Break Block When No Drops
Match Block
Pick Up Item Blocks NBT
Pick up the item blocks in the world that match the given NBT tag.
Input:
NBT
Properties:
Channel
Round-Robin
Blacklist
Cause Block Update
Right Hand
Silk Touch
Ignore Replacable
Break Block When No Drops
Match Block
NBT subset
NBT superset
Require Item NBT
Check NBT Recursively
Pick Up Block
Pick up the given block in the world.
Input:
Block
Properties:
Channel
Round-Robin
Cause Block Update
Right Hand
Silk Touch
Ignore Replacable
Break Block When No Drops
Match Block
Blacklist
Empty is Wildcard
Pick Up Blocks
Pick up the given blocks in the world.
Input:
List
Properties:
Channel
Round-Robin
Cause Block Update
Right Hand
Silk Touch
Ignore Replacable
Break Block When No Drops
Match Block
Blacklist
Pick Up Blocks Predicate
Pick up the blocks in the world that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Cause Block Update
Right Hand
Silk Touch
Ignore Replacable
Break Block When No Drops
Match Block

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/world/energy.html

Energy
The
World Energy Importers
and
World Energy Exporters
help you interact with entities that have an energy buffer, such as dropped batteries.
Crafting Table
[Crafting Table]
[Energy Exporter]
[Logic Director]
→
[World Energy Exporter]
Crafting Table
[Crafting Table]
[Energy Importer]
[Logic Director]
→
[World Energy Importer]
Aspect
Export All Energy To Entities
Export energy to the facing entities if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Energy Transfer Rate
Check Energy Amount
Entity Index
Export Energy Amount To Entities
Export the given amount of energy to the facing entities each tick.
Input:
Long
Properties:
Channel
Entity Index
Craft
Aspect
Import All Energy From Entities
Import energy from the facing entities if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Energy Transfer Rate
Check Energy Amount
Entity Index
Import Energy Amount From Entities
Import the given amount of energy from the facing entities each tick.
Input:
Long
Properties:
Channel
Entity Index

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/world/fluid.html

Fluids
With
World Fluid Importers
and
World Fluid Exporters
, you can place fluid source blocks, pick them up and interact with entities that are a tank, such as dropped tank items. Various aspect properties exist to change things like whether or not a block update should be caused.
Crafting Table
[Crafting Table]
[Fluid Exporter]
[Logic Director]
→
[World Fluid Exporter]
Crafting Table
[Crafting Table]
[Fluid Importer]
[Logic Director]
→
[World Fluid Importer]
Aspect
Place All Fluids
Place all fluids in the world.
Input:
Boolean
Properties:
Channel
Round-Robin
Cause Block Update
Ignore Replacable
Place Fluid
Place the given fluid in the world.
Input:
Fluid
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Check NBT
Cause Block Update
Ignore Replacable
Craft
Place Fluids
Place the given fluids in the world.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Check NBT
Cause Block Update
Ignore Replacable
Place Fluids Predicate
Place the given fluids in the world that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Cause Block Update
Ignore Replacable
Slot-based
Place Fluids NBT
Place the given fluids in the world that match the given NBT tag.
Input:
NBT
Properties:
Channel
Cause Block Update
Ignore Replacable
NBT subset
NBT superset
Require Fluid NBT
Check NBT Recursively
Export All Fluid To Entities
Export fluids to the facing entities if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Fluid Transfer Rate
Check Fluid Amount
Entity Index
Export Fluid Amount To Entities
Export the given amount of fluid to the facing entities each tick.
Input:
Integer
Properties:
Channel
Round-Robin
Export Fluid To Entities
Export the given fluid to the facing entities.
Input:
Fluid
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Fluid Transfer Rate
Check Fluid Amount
Check NBT
Craft
Entity Index
Export Fluids To Entities
Export the given fluids to the facing entities.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Fluid Transfer Rate
Check Fluid Amount
Check NBT
Entity Index
Export Fluids Predicate To Entities
Export all fluids that match the given predicate to the facing entities.
Input:
Operator
Properties:
Channel
Round-Robin
Fluid Transfer Rate
Check Fluid Amount
Entity Index
Slot-based
Export Fluids NBT To Entities
Export all fluids that match the given NBT tag to the facing entities.
Input:
NBT
Properties:
Channel
NBT subset
NBT superset
Require Fluid NBT
Check NBT Recursively
Aspect
Pick Up All Fluids
Pick up all fluids in the world.
Input:
Boolean
Properties:
Channel
Round-Robin
Pick Up Fluid
Pick up the given fluid in the world.
Input:
Fluid
Properties:
Channel
Round-Robin
Empty is Wildcard
Check NBT
Blacklist
Pick Up Fluids
Pick up the given fluids in the world.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Check NBT
Pick Up Fluids Predicate
Pick up the given fluids in the world that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Slot-based
Pick Up Fluids NBT
Pick up the fluids in the world that match the given NBT tag.
Input:
NBT
Properties:
Channel
NBT subset
NBT superset
Require Fluid NBT
Check NBT Recursively
Import All Fluid From Entities
Import fluid from the facing entities if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Fluid Transfer Rate
Check Fluid Amount
Entity Index
Import Fluid Amount From Entities
Import the given amount of fluid from the facing entities each tick.
Input:
Integer
Properties:
Channel
Round-Robin
Import Fluid From Entities
Import the given fluid from the facing entities.
Input:
Fluid
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Fluid Transfer Rate
Check Fluid Amount
Check NBT
Entity Index
Import Fluids From Entities
Import the given fluids from the facing entities.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Fluid Transfer Rate
Check Fluid Amount
Check NBT
Entity Index
Import Fluids Predicate From Entities
Import all fluids that match the given predicate from the facing entities.
Input:
Operator
Properties:
Channel
Round-Robin
Fluid Transfer Rate
Check Fluid Amount
Entity Index
Slot-based
Import Fluids NBT From Entities
Import all fluids that match the given NBT tag from the facing entities.
Input:
NBT
Properties:
Channel
NBT subset
NBT superset
Require Fluid NBT
Check NBT Recursively

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/world/index.html

World Interaction
Items
Fluids
Energy
Block

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/world/item.html

Items
World Item Importers
and
World Item Exporters
can be used to place items in the world, pick them up, and interact with entities that have an inventory, such as dropped Shulker Boxes and Donkeys. Various aspect properties exist to change things like dispensing, throw-velocity, pickup delay, pitch and yaw.
Crafting Table
[Crafting Table]
[Item Exporter]
[Logic Director]
→
[World Item Exporter]
Crafting Table
[Crafting Table]
[Item Importer]
[Logic Director]
→
[World Item Importer]
Aspect
Place All Item Entities
Place all items into the world.
Input:
Boolean
Properties:
Channel
Round-Robin
Dispense
Offset X
Offset Y
Offset Z
Lifespan (ticks)
Pickup Delay (ticks)
Velocity
Yaw
Pitch
Item Transfer Rate
Place Item Entities
Place the given amount of items into the world.
Input:
Integer
Properties:
Channel
Round-Robin
Dispense
Offset X
Offset Y
Offset Z
Lifespan (ticks)
Pickup Delay (ticks)
Velocity
Yaw
Pitch
Place Item Entity
Place the given item into the world.
Input:
Item
Properties:
Channel
Round-Robin
Dispense
Offset X
Offset Y
Offset Z
Lifespan (ticks)
Pickup Delay (ticks)
Velocity
Yaw
Pitch
Item Transfer Rate
Check Stack Size
Check NBT
Blacklist
Empty is Wildcard
Craft
Place Item Entities
Place the given items into the world.
Input:
List
Properties:
Channel
Round-Robin
Dispense
Offset X
Offset Y
Offset Z
Lifespan (ticks)
Pickup Delay (ticks)
Velocity
Yaw
Pitch
Item Transfer Rate
Check Stack Size
Check NBT
Blacklist
Empty is Wildcard
Craft
Place Item Entities Predicate
Place the item entities into the world that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Dispense
Offset X
Offset Y
Offset Z
Lifespan (ticks)
Pickup Delay (ticks)
Velocity
Yaw
Pitch
Item Transfer Rate
Slot-based
Place Item Entities NBT
Place the item entities into the world that match the given NBT tag.
Input:
NBT
Properties:
Channel
Round-Robin
Dispense
Offset X
Offset Y
Offset Z
Lifespan (ticks)
Pickup Delay (ticks)
Velocity
Yaw
Pitch
Item Transfer Rate
Blacklist
NBT subset
NBT superset
Require Item NBT
Check NBT Recursively
Export All Items To Entities
Export items to the facing entities if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Item Transfer Rate
Item Slot
Check Stack Size
Entity Index
Export Item Amount To Entities
Export the given amount of items to the facing entities each tick.
Input:
Integer
Properties:
Channel
Round-Robin
Item Slot
Check Stack Size
Entity Index
Export Item Slot To Entities
Export items to the given slot of the facing entities. -1 is any slot, -2 or less is no slot.
Input:
Integer
Properties:
Channel
Round-Robin
Item Transfer Rate
Check Stack Size
Entity Index
Export Item To Entities
Export the given item to the facing entities.
Input:
Item
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
Check Stack Size
Check NBT
Empty is Wildcard
Craft
Entity Index
Export Items To Entities
Export the given items to the facing entities.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
Check Stack Size
Check NBT
Empty is Wildcard
Entity Index
Export Items Predicate To Entities
Export all items that match the given predicate to the facing entities.
Input:
Operator
Properties:
Channel
Round-Robin
Item Transfer Rate
Item Slot
Check Stack Size
Slot-based
Entity Index
Export Items NBT To Entities
Export all items that match the given NBT tag to the facing entities.
Input:
NBT
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
NBT subset
NBT superset
Require Item NBT
Check NBT Recursively
Entity Index
Aspect
Pick Up All Item Entities
Pick up all item entities from the world.
Input:
Boolean
Properties:
Channel
Round-Robin
Ignore Pickup Delay
Item Transfer Rate
Pick Up Item Entities
Pick up the given amount of items from the world.
Input:
Integer
Properties:
Channel
Round-Robin
Check Stack Size
Check NBT
Ignore Pickup Delay
Pick Up Item Entity
Pick up the given item entity from the world.
Input:
Item
Properties:
Channel
Round-Robin
Check Stack Size
Check NBT
Ignore Pickup Delay
Item Transfer Rate
Blacklist
Empty is Wildcard
Pick Up Item Entities
Pick up the given item entities from the world.
Input:
List
Properties:
Channel
Round-Robin
Check Stack Size
Check NBT
Ignore Pickup Delay
Item Transfer Rate
Blacklist
Empty is Wildcard
Pick Up Item Entities Predicate
Pick up the item entities from the world that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Ignore Pickup Delay
Item Transfer Rate
Slot-based
Pick Up Item Entities NBT
Pick up the item entities from the world that match the given NBT tag.
Input:
NBT
Properties:
Channel
Round-Robin
Blacklist
Ignore Pickup Delay
Item Transfer Rate
NBT subset
NBT superset
Require Item NBT
Check NBT Recursively
Import All Items From Entities
Import items from the facing entities if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Item Transfer Rate
Item Slot
Check Stack Size
Entity Index
Import Item Amount From Entities
Import the given amount of items from the facing entities each tick.
Input:
Integer
Properties:
Channel
Round-Robin
Item Slot
Check Stack Size
Entity Index
Import Item Slot From Entities
Import items from the given slot of the facing entities. -1 is any slot, -2 or less is no slot.
Input:
Integer
Properties:
Channel
Round-Robin
Item Transfer Rate
Check Stack Size
Entity Index
Import Item From Entities
Import the given item from the facing entities.
Input:
Item
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
Check Stack Size
Check NBT
Empty is Wildcard
Craft
Entity Index
Import Items From Entities
Import the given items from the facing entities.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
Check Stack Size
Check NBT
Empty is Wildcard
Entity Index
Import Items Predicate From Entities
Import all items that match the given predicate from the facing entities.
Input:
Operator
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
Check Stack Size
Check NBT
Empty is Wildcard
Entity Index
Slot-based
Import Items NBT From Entities
Import all items that match the given NBT tag from the facing entities.
Input:
NBT
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
NBT subset
NBT superset
Require Item NBT
Check NBT Recursively
Entity Index

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/break_stone.html

Break Only Stone Blocks
Make a
Variable Card
containing a reference to the
Stone
item value. Place this inside a
World Block Importer
to break only Stone blocks. Also have a look at the various aspect properties to see if you can manage to silk touch the Stone to avoid receiving just Cobblestone.
Aspect
Pick Up Block
Pick up the given block in the world.
Input:
Block
Properties:
Channel
Round-Robin
Cause Block Update
Right Hand
Silk Touch
Ignore Replacable
Break Block When No Drops
Match Block
Blacklist
Empty is Wildcard
Advancements
Break Stone
Break Stone blocks using a World Block Importer
Rewards
[World Block Importer]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/click_sword.html

Click with Sword
All this clicking and such can be quite boring, why not let a
Player Simulator
do it for you? Start by crafting one, and insert a
Variable Card
containing a
Diamond Sword
to let it attack autonomously using
Diamond Swords
from your item network.
Crafting Table
[Crafting Table]
[World Item Exporter]
[World Block Importer]
[World Item Importer]
[Emerald]
[World Block Exporter]
→
[Player Simulator]
Aspect
Click Item
Click with the given item.
Input:
Item
Properties:
Channel
Round-Robin
Blacklist
Craft
Right Click
Right Hand
Continuous Click
Sneak
Right Click Duration
Network Inventory
Check Stack Size
Check NBT
Item Transfer Rate
Entity Index
Offset X
Offset Y
Offset Z
Advancements
Player Simulation
Craft a Player Simulator
Click with a Diamond Sword
Click with a Diamond Sword using a Player Simulator
Rewards
[Player Simulator]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/drop_all_items.html

Drop All Items
Place an empty
Variable Card
inside a
World Item Exporter
to drop all items from the network on the ground.
Aspect
Place All Item Entities
Place all items into the world.
Input:
Boolean
Properties:
Channel
Round-Robin
Dispense
Offset X
Offset Y
Offset Z
Lifespan (ticks)
Pickup Delay (ticks)
Velocity
Yaw
Pitch
Item Transfer Rate
Advancements
Omni Item Dropping
Drop all items using a World Item Exporter
Rewards
[Item Exporter]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/export_enchantable_items.html

Export Enchantable Items
Export all enchantable items to the target inventory. This can be done using a certain
Operator
.
Operator
Enchantable
enchantable
If the item can be enchanted
Item
Boolean
Global name: itemstackIsEnchantable(
Item
) →
Boolean
Method name:
Item
.isEnchantable() →
Boolean
Aspect
Export Items Predicate
Export all items that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Slot-based
Advancements
Export Enchantable Items
Use the Enchantable Operator to export all enchantable items into an inventory using an Item Exporter
Rewards
[Item Interface]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/export_items_limit.html

Export Items with Limit
Read the number of items in the target inventory using an
Inventory Reader
, and only move items until the total number of items is larger than
10
.
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
Inventory Count
Get the total amount of items
Output:
Integer
Operator
Less Than
<
Number
Number
Boolean
Global name: numberLessThan(
Number
,
Number
) →
Boolean
Method name:
Number
.lessThan(
Number
) →
Boolean
Aspect
Export All Items
Export items if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Advancements
Limited Item Exporting
Export items into an inventory until 10 items are in that inventory using an Item Exporter
Rewards
[Item Interface]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/filter_storage_day.html

Filter Item Storage if Day
Using the
Filtering Item Interface
, attach a
Chest
to your network, and only make its contents available to the network if it's day (and not when it's night).
Aspect
Is Day
If it is day in the target world
Output:
Boolean
Aspect
Filter All Items
Pass items if the boolean is true
Input:
Boolean
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Advancements
Filter Item Storage if Day
Use a Filtering Item Interface to only expose an inventory's contents during daytime
Rewards
[Filtering Item Interface]
1
[Filtering Fluid Interface]
1
[Filtering Energy Interface]
1

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/filter_storage_mod.html

Filter Item Storage by Mod
Using the
Filtering Item Interface
, attach a
Chest
to your network, and configure a predicate-based filter that only passes items from the
Integrated Dynamics
mod. To do this, we will create a new operator that will take an
Item
as input, and returns a
Boolean
as output.
To do this, first create an
Operator
value containing the
Relational Equals
operator. Next, create a
String
containing the
IntegratedDynamics
value (without space). And then, use the
Apply
operator to partially apply your
Relational Equals
operator to your
String
value.
As a next step, we have to create another
Operator
value, but this time containing the
Item Mod
operator. And now, we can use the
Pipe
operator to pipe our
Item Mod
operator into our partially applied operator from the previous step.
This result from the
Pipe
operator can be placed into the predicate-based aspect of your
Filtering Item Interface
, and all variables we depend on need to be placed into a
Variable Store
. If all steps are followed correctly, the filter will only pass items from the
Integrated Dynamics
mod, which can be customized by changing our
String
constant.
Operator
Mod
mod
The name of the mod owning the given item
Item
String
Global name: itemstackMod(
Item
) →
String
Method name:
Item
.mod() →
String
Operator
Equals
==
Tests whether two values are equal. The types of both must be the same.
Any
Any
Boolean
Global name: anyEquals(
Any
,
Any
) →
Boolean
Method name:
Any
.equals(
Any
) →
Boolean
Operator
Apply
apply
Apply for a given operator a given value.
Operator
Any
Any
Global name: operatorApply(
Operator
,
Any
) →
Any
Method name:
Operator
.apply(
Any
) →
Any
Operator
Pipe
.
Create a new operator that pipes the output from the first operator to the second operator.
Operator
Operator
Operator
Global name: operatorPipe(
Operator
,
Operator
) →
Operator
Method name:
Operator
.pipe(
Operator
) →
Operator
Aspect
Filter Items Predicate
Pass all items that match the given predicate.
Input:
Operator
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Advancements
Filter Item Storage by Mod
Use a Filtering Item Interface to only pass items from a certain mod
Rewards
[Filtering Item Interface]
5
[Filtering Fluid Interface]
5
[Filtering Energy Interface]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/import_all_items.html

Import All Items
Place an empty
Variable Card
inside an
Item Importer
to import all items into the network from a target inventory.
Aspect
Import All Items
Import items if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Advancements
Omni Item Importing
Import all items using an Item Importer
Rewards
[Item Interface]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/import_items_list.html

Import Item List
Import all items in a certain list to the target inventory. An item list can be created by reading a list from any
Inventory Reader
.
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
Items
Get a list of the items in the inventory
Output:
List
Aspect
Import Items
Import the given items.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Item Transfer Rate
Item Slot
Passive Interaction
Passive Interaction Ignores Filter
Check Stack Size
Check NBT
Craft
Advancements
List-based Item Importing
Import all items from a list into an inventory using an Item Importer
Rewards
[Item Exporter]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/importer_exporter.html

Importer Exporter
Item Importers
will move items from their target towards one of the
Item Interfaces
.
Item Exporters
do the opposite, they will move items from any of the
Item Interfaces
to their target.
Crafting Table
[Crafting Table]
[Item Interface]
[Output Variable Transformer]
→
[Item Exporter]
Crafting Table
[Crafting Table]
[Item Interface]
[Input Variable Transformer]
→
[Item Importer]
Advancements
Advanced Item Exporting
Craft an Item Exporter
Advanced Item Importing
Craft an Item Importer
Rewards
[Item Exporter]
2
[Item Importer]
2

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/index.html

Tunnel Tutorials
Introduction
Interfaces
Importer Exporter
Import All Items
Export Items with Limit
Import Item List
Export Enchantable Items
Filter Item Storage if Day
Filter Item Storage by Mod
World Item Importer Exporter
Drop All Items
Pick up Items with Limit
World Block Importer Exporter
Break Only Stone Blocks
Place Items by Tag
Click with Sword

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/interfaces.html

Interfaces
Interfaces
are parts that can be attached to
Cables
. An item, fluid and energy version exists for them, which respectively connect to inventories, tanks and energy machines.
Interfaces
make their target accessible to the network.
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Chest]
[Trapped Chest]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
→
[Item Interface]
4
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Bucket]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
→
[Fluid Interface]
4
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Energy Battery]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
→
[Energy Interface]
4
Advancements
Item Interfacing
Craft an Item Interface
Fluid Interfacing
Craft a Fluid Interface
Energy Interfacing
Craft an Energy Interface
Rewards
[Item Interface]
2
[Fluid Interface]
2
[Energy Interface]
2

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/introduction.html

Introduction
This section contains several tutorials that help you to learn
Integrated Tunnels
, the
Integrated Dynamics
add-on for item, fluid and energy transfer.

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/pickup_items_limit.html

Pick up Items with Limit
Write a constant number
5
to a
Variable Card
, and place it inside a
World Item Importer
to pick up 5 items at a time from the target space.
Aspect
Pick Up Item Entities
Pick up the given amount of items from the world.
Input:
Integer
Properties:
Channel
Round-Robin
Check Stack Size
Check NBT
Ignore Pickup Delay
Advancements
Limited Item Pickup
Pick up 5 items at a time using a World Item Importer
Rewards
[Item Importer]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/place_logwood.html

Place Items by Tag
Try creating a system using the
World Block Exporter
that only places wooden logs. Do this using the
Tag Values
operator to retrieve a list of all items in the
logs
tag.
Operator
Item Tag Values
item_tag_values
The Tag values (items) of the given item tag name
String
List
Global name: stringItemsByTag(
String
) →
List
Method name:
String
.itemsByTag() →
List
Aspect
Place Item Blocks
Place the given item blocks in the world.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Check NBT
Cause Block Update
Right Hand
Ignore Replacable
Craft
Advancements
Place Logs
Place any log using a World Block Exporter by using the 'logs' tag list
Rewards
[World Block Exporter]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/world_block_importer_exporter.html

World Block Importer Exporter
Instead of dropping items in the world or picking them up, you can also break and place blocks using the
World Block Importer
and
World Block Exporter
. Various aspect properties exist to change things such as whether or not the blocks should be silk touched when breaking.
Crafting Table
[Crafting Table]
[Item Exporter]
[Logic Director]
→
[World Block Exporter]
Crafting Table
[Crafting Table]
[Item Importer]
[Logic Director]
→
[World Block Importer]
Advancements
Advanced Block World Exporting
Craft a World Block Exporter
Advanced Block World Importing
Craft a World Block Importer
Rewards
[World Block Exporter]
2
[World Block Importer]
2

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/world_item_importer_exporter.html

World Item Importer Exporter
Simular to
Item Importers
and
Item Exporters
,
World Item Importers
and
World Item Exporters
can respectively pick up items from the world into your item network and place items into the world from your item network. Furthermore, these can also be used to interact with dropped items that have inventories, such as
Shulker Boxes
.
Crafting Table
[Crafting Table]
[Item Exporter]
[Logic Director]
→
[World Item Exporter]
Crafting Table
[Crafting Table]
[Item Importer]
[Logic Director]
→
[World Item Importer]
Advancements
Advanced Item World Exporting
Craft a World Item Exporter
Advanced Item World Importing
Craft a World Item Importer
Rewards
[World Item Exporter]
2
[World Item Importer]
2

## https://integrateddynamics.rubensworks.net/book/manual/logic/advanced/index.html

Advanced
NBT Path

## https://integrateddynamics.rubensworks.net/book/manual/logic/advanced/nbt_path.html

NBT Path
When working with complex
NBT
tags, you can quickly require complex operator chains for reading deeply nested NBT values. In order to simplify this task,
String
-based
NBT Path expressions
can be used to determine the path within an
NBT
tag.
The simplest NBT Path expression is made up of a chain of
field selectors
, such as
.fieldName
or
[”fieldName”]
.
For example, applying the expression
”.root.child1.child2”
on the
NBT
tag
{ root: { child1: { child2: ”some value” }}}
will output
”some value”
.
Multiple fields can be selected by providing an array of field names or a wildcard, for example
[fieldA,fieldB]
or
*
For example, applying the expression
”.a[b1,b2]*”
on the
NBT
tag
{ a: { b1: { c: ”some value” }}}
will output
”some value”
.
When working with NBT lists, specific entries can be selected by specifying an index, such as
[1]
.
For example, applying the expression
”.a[1]”
on the
NBT
tag
{ a: [10,20] }
will output
”20”
.
Multiple list indexes can be selected using the slice operator:
[start:end:step]
. (Only start is required, the other elements may be omitted)
For example, applying the expression
”.a[1:4:2]”
on the
NBT
tag
{ a: [0,1,2,3,4,5] }
will output
[1,3]
.
More advanced filter expressions are also possible using the filter syntax, for example
[?(@.childName < 10)]
(@ refers to the current tag, .. refers to the parent tag, $ refers to the root tag)
For example, applying the expression
”$.a[?(@ == 3)]”
on the
NBT
tag
{ a: [0,1,2,3,4,5] }
will output
[3]
.
Expressions can be combined using ”§§” (and), ”||” (or), and ”!” (not).
For example, applying the expression
”$.a[?(@ == 3 || (@ == 5))]”
on the
NBT
tag
{ a: [0,1,2,3,4,5] }
will output
[3,5]
.
Operator
NBT Path Match First
NBT.path_match_first
Apply the given NBT Path expression on the given NBT value, and return the first match
String
NBT
NBT
Global name: stringNbtPathMatchFirst(
String
,
NBT
) →
NBT
Method name:
String
.nbtPathMatchFirst(
NBT
) →
NBT
Operator
NBT Path Match All
NBT.path_match_all
Apply the given NBT Path expression on the given NBT value, and return all matches as a list
String
NBT
List
Global name: stringNbtPathMatchAll(
String
,
NBT
) →
List
Method name:
String
.nbtPathMatchAll(
NBT
) →
List
Operator
NBT Path Test
NBT.path_test
Test if the given NBT Path expression matches with the given NBT value
String
NBT
Boolean
Global name: stringNbtPathTest(
String
,
NBT
) →
Boolean
Method name:
String
.nbtPathTest(
NBT
) →
Boolean

## https://integrateddynamics.rubensworks.net/book/manual/logic/index.html

Logic
Value Types
Variables
Advanced

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/block.html

Block
A reference to a
Minecraft
block. For example
Dirt
,
Cobblestone
,
Red Stained Clay
,
Chest
, ....

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/boolean.html

Boolean
The simplest value type, its values can be either
True
or
False
.

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/double.html

Double
Real numbers, which can also be positive or negative. For example
3.1415
,
4815162342
,
3.33333333
, ...
This value type is part of the
Number
category.

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/entity.html

Entity
A reference to a certain
Minecraft
entity. For example
Zombie
,
Cow
,
Player_abc
....

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/fluid.html

Fluid
A reference to a
Minecraft
fluid. For example
Lava@1000mB
,
Water@3mB
,
Menril Resin@3030mB
, ....

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/index.html

Value Types
Boolean
Integer
Double
Long
String
List
Operator
NBT
Block
Item
Entity
Fluid
Ingredients
Recipe

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/ingredients.html

Ingredients
A collection of
Items
,
Fluids
and
Energy amounts
(
Integer
). Its main use is for representing the input and output of
Recipes
.

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/integer.html

Integer
A value type for representing positive and negative numbers. For example
-20
,
0
,
1000
,
333
, ...
The values can range between
–2147483648
and
2147483647
.
This value type is part of the
Number
category.

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/item.html

Item
A reference to a
Minecraft
item. For example
Apple
,
Cobblestone
,
Diamond Pickaxe
,
Enchanted Sword
, ....

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/list.html

List
A list containing values of a certain
Value Type
. All elements in a certain list can only contain values of the same value types. For example
(0, 1, 2, 3)
,
(“a”, “b”, “c”)
,
(3.33, 1.14, 5, 6)

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/long.html

Long
The same as
Integers
, but with a larger value range, which goes from
-9223372036854775808
to
9223372036854775807
This value type is part of the
Number
category.

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/nbt.html

NBT
Certain things in
Minecraft
internally use the
NBT
datastructure. An
NBT
tag is a special kind of object that can hold multiple values of different types. For example the NBT representation of a
Chest
containing all its items, the enchantment information of an
Pickaxe
, ...
Variables can hold references to such
NBT
tags, and corresponding operators exist to read or extend these tags.
NBT
tags are typically used in this mod as a low-level fallback mechanism, in case you can not read certain information using dedicated operators.

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/operator.html

Operator
A value containing a reference to an
Operator
. Operators can be partially applied for more flexible evaluations based on the concept of
Currying
. This is part of the concept of
metaprogramming
(look it up, it's a thing!), where operators are treated as data by other operators. This basically allows you to create new operators yourself, which can then be used for more complex operations.
You can for example store the addition
Operator
in a variable card. Do this by starting to type the operator name and selecting it from the autocomplete dropdown-list and clicking it. You can then partially apply it with one
Integer
Variable Card
, which will result in a new virtual operator with one input type and one output type, which internally contains a reference to that applied
Integer
. Applying another
Integer
will result in an
Integer
.
Other operations of
Operators
are present in the
Logic Programmer
for more advanced evaluations, such as reducing a list to a single value, or filtering a list based on a
Predicate
.

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/recipe.html

Recipe
An abstract crafting recipe, that takes
Ingredients
as input and outputs another
Ingredients
value.
Recipes can be created visually using the
Logic Programmer
, or they can be read from (supported) machines using the
Machine Reader
.
These recipes can be used for autocrafting mods such as
Integrated Crafting
.
To quickly find specific recipes among existing
Variable Cards
inside a GUI, you can hold shift to temporarily display what the output item of the recipe is.
Shift-clicking on an input slot inside the
Logic Programmer
allows you to configure detailed properties of this input slot. For example, it allows you to indicate if NBT should be considered or not, if this item can be reused across multiple recipes (e.g. if it will not be fully consumed after the crafting job), and if tag-based alternatives should be considered for this item.

## https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/string.html

String
A sequence of characters, for example
”aaa”
,
“Hi!”
,
“x!(àà)ç”
, ...

## https://integrateddynamics.rubensworks.net/book/manual/logic/variables/index.html

Variables
Variable Card
Variable Store

## https://integrateddynamics.rubensworks.net/book/manual/logic/variables/variable_card.html

Variable Card
Variable Cards
are the primary mode of logic transport in this mod. A variable contains a reference to a certain
Value
with a certain
Value Type
.
Variables can either be static or dynamic. Static variables are defined once, and will never change afterwards. Dynamic variables are the result of some form of
Logic Evaluation
, which are done by
Operators
or
Reader Aspects
. An example of a dynamic variable is the time in the
Minecraft
world, which changes continuously.
Dynamic variables will only be evaluated when their value is requested in some way, such as displaying the value in a
Display Panel
, or being requested as part of the evaluation of another dynamic variable.
Each
Variable Card
has a unique ID, which is used internally in networks for
Logic Evaluation
. When an error occurs in the network, these ID’s will be shown to the player to help with debugging. For better usability, a label can be given to
Variable Cards
using a
Labeler
, which will allow players to see the label instead of the ID’s.
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
[Crystallized Menril Chunk]
[Book and Quill]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
→
[Labeller]
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
Crafting Table
[Crafting Table]
[Crystallized Chorus Chunk]
[Crystallized Menril Chunk]
[Crystallized Chorus Chunk]
[Crystallized Chorus Chunk]
[Diamond]
[Crystallized Chorus Chunk]
[Crystallized Chorus Chunk]
[Crystallized Menril Chunk]
[Crystallized Chorus Chunk]
→
[Logic Director]
4

## https://integrateddynamics.rubensworks.net/book/manual/logic/variables/variable_store.html

Variable Store
Some dynamic variables are evaluated based on other variables, where access to those referenced variables must be possible. A dynamic variable representing the addition of two other variables must be able to access those two other variables when it is evaluated. Access to those other variables must be possible within the same network as where the dynamic variable is being evaluated, which can be done by placing the other variable cards in a
Variable Store
. Multiple
Variable Stores
can be present in a network. Those other variables may also reference certain variables, which should also be present in that same network.
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Block of Crystallized Menril]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Chest]
[Trapped Chest]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Block of Crystallized Menril]
[Crystallized Menril Chunk]
→
[Variable Store]

## https://integrateddynamics.rubensworks.net/book/manual/machines/delay.html

Delayer
In some situations, you might need to maintain some kind of history of variable values. When you for instance want to take the average or difference over the last values.
For this, you can use a
Delayer
, with which you can maintain the history of the values of a given variable. You can do this by inserting a
Variable Card
in the top slot for which you want to maintain the history. You can now create
Variable Cards
that will refer to a list of values, which corresponds to the history. The referenced (or any other)
Variable Card
must remain in the top slot for the delayer to work.
The newest values correspond to the last elements in the list. You can choose the maximum length of the history from within the gui.
Crafting Table
[Crafting Table]
[Variable Card]
[Crystallized Menril Chunk]
[Variable Card]
[Clock]
[Variable Store]
[Clock]
[Variable Card]
[Crystallized Menril Chunk]
[Variable Card]
→
[Delayer]

## https://integrateddynamics.rubensworks.net/book/manual/machines/materializer.html

Materializer
In order to make a dynamic
Variable Card
static, a
Materializer
can be used. The dynamic
Variable Card
must be inserted, and a new empty
Variable Card
must be inserted, which will cause it to contain the current value of the dynamic
Variable Card
as a static value.
This can be useful in situations where you want to log the values of a certain dynamic
Variable Card
, or when you don’t need the dynamic evaluation of a
Variable Card
anymore.
Variable Cards
can be piped in and out the
Materializer
.
The two encode buttons next to the
Materializer
place its materialized value on your clipboard as an encoded string, so that you can share it with other players, or use it in another world. The first button gives a readable encoding, the second a shorter one that is easier to send around.
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

## https://integrateddynamics.rubensworks.net/book/manual/machines/proxy.html

Proxy
A
Proxy
can be used to make a dynamic reference to a certain
Variable Card
. It allows you to insert a
Variable Card
in the top slot, and write a new
Variable Card
, which will dynamically take the value of the
Variable Card
that is present in the proxy. Note that the referenced (or any other)
Variable Card
must remain in the top slot for the proxy to work.
This is useful in situations where you have a chain of
Variable Cards
, but you still would like to be able to change one value manually. You could then proxy that variable, which will allow it to be swapped with another variable at any time.
Just as with the
Materializer
,
Variable Cards
can be piped in and out of it.
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

## https://integrateddynamics.rubensworks.net/book/tutorials/advancedOperations.html

Advanced Operations
As you may have noticed during the last tutorial, the
Logic Programmer
allows you to do way more than just adding numbers. In this tutorial, we’ll look into the more advanced datastructures and chaining of operators.
Let’s start by creating a static
List
of the
Integers
1
,
10
and
100
, and visualize it in a
Display Panel
.
Items
are one of the possible value types in this mod. Let’s make a static
Item
Variable Card
, dynamically retrieve the mod name from that item, and visualize it in a
Display Panel
.
Remember that you’ll have to use a
Variable Store
to store your referenced variable.
For the final part of this tutorial, we’ll combine dynamic variables with operators. Attach an
Entity Reader
to a network, read the target
Entity
, retrieve the
Block
the target
Entity
is looking at, and visualize that
Block
in a
Display Panel
.
Crafting Table
[Crafting Table]
[Raw Porkchop]
[Raw Porkchop]
[Input Variable Transformer]
[Raw Porkchop]
[Raw Porkchop]
→
[Entity Reader]
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
Aspect
Entity
The selected entity in the target space
Output:
Entity
Properties:
List Element
Operator
Target Block
target_block
The block the given entity is currently looking at.
Entity
Block
Global name: entityTargetBlock(
Entity
) →
Block
Method name:
Entity
.targetBlock() →
Block
Advancements
Logical List Building
Construct a static List with §6Integers§7 1, 10 and 100.
Item Origin Identification
Get the mod name of an Item.
What Would I Be Looking At?
Get the Block you are looking at.
Rewards
[Variable Card]
64
[Variable Card]
64
[Variable Store]
32

## https://integrateddynamics.rubensworks.net/book/tutorials/challenges.html

Challenges
This final tutorial section contains several non-trivial challenges to show that you understand the basics of
Integrated Dynamics
. No further information will be given for each challenge, so good luck!
Advancements
Creeper Taming
Show the HP of a Creeper in a Display Panel
Sponge Step Sound
Play the step sound of a Wet Sponge block
Recursive Recursion
Create an infinite List, and display it in a Display Panel
Tools for Obsidian?
Check if a Stone Pickaxe can break an Obsidian block, and show the result in a Display Panel.
Smart Pressure Plate
Create a system that automatically opens a door when you pass by an Entity Reader based on entity name matching, using a Redstone Writer directly pointing to the door. (Hint: pass an entity name as first argument into the equals operator)
Rewards
[Variable Card]
64
[Variable Card]
64
[Variable Card]
64
[Variable Card]
64
[Variable Store]
64
[Display Panel]
64

## https://integrateddynamics.rubensworks.net/book/tutorials/index.html

Tutorials
Introduction
Menril Basics
Network Foundations
Basic Network Components
Aspects
Logic Operations
Advanced Network Components
Advanced Operations
The Value of Operators
Saving State
Recipe Handling
NBT
Challenges
Tunnel Tutorials
Crafting Tutorials
Terminal Tutorials
Scripting Tutorials
NBT Tutorials
Mekanism Tutorials

## https://integrateddynamics.rubensworks.net/book/tutorials/introduction.html

Introduction
This part of the book contains several tutorials that will help you understand the basics of the mod. Furthermore, it will allow you to earn some
rewards
while accomplishing the achievements of each tutorial.

## https://integrateddynamics.rubensworks.net/book/tutorials/logicOperations.html

Logic Operations
While displaying values that originate from readers can be useful, things get really interesting when you start combining certain values with other values using
Operators
. All
Operators
can be found in the
Logic Programmer
.
Start by crafting a
Logic Programmer
, which exists in both item and block-form. Open its gui to see the list of all
operators
.
Next, search for the
Integer
operator, and click on it. This will open a window in which you should type a number. When you have done this, insert an empty
Variable Card
to bind that
Integer
to the
Variable Card
. When you’re done, do that once more for another
Integer
.
Finally, search for the
Addition
operator and open it. This allows you to add two
Numbers
. Insert your two
Integer
Variable Cards
into these slots, and bind it to a new
Variable Card
.
After writing a value to a
Variable Card
, you can always modify it again later by inserting it again in the
Logic Programmer
.
As
Integrated Dynamics
evaluates variables in a
lazy
manner, this
Addition
Variable Card
does not contain the addition of these
Integers
directly, but the addition will only be performed when the value is needed in the network, based on the variable references. For this, the network has to be able to find these referenced
Variable Cards
. For this, you will need to craft and place a
Variable Store
. Insert the two
Integer
Variable Cards
into this store to make them available to the network.
Finally, you can now connect a cable to the
Variable Store
, attach a
Display Panel
, and insert the
Addition
Variable Card
to display the addition.
The
Logic Programmer
and
Variable Stores
are going to be your best friends when you want to do more complex automation.
Crafting Table
[Crafting Table]
[Block of Crystallized Menril]
[Crafting Table]
→
[Logic Programmer]
Crafting Table
[Crafting Table]
[Logic Programmer]
→
[Portable Logic Programmer]
Crafting Table
[Crafting Table]
[Portable Logic Programmer]
→
[Logic Programmer]
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Block of Crystallized Menril]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Chest]
[Trapped Chest]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Block of Crystallized Menril]
[Crystallized Menril Chunk]
→
[Variable Store]
Operator
Addition
+
Number
Number
Number
Global name: numberAdd(
Number
,
Number
) →
Number
Method name:
Number
.add(
Number
) →
Number
Advancements
Logic Programming
Open a Logic Programmer's GUI
Constant Definition
Define a constant §6Integer§7 Variable Card.
Arithmetic Addition
Add two §eNumbers§7 and display the result.
Rewards
[Variable Store]
5
[Variable Card]
32

## https://integrateddynamics.rubensworks.net/book/tutorials/menrilBasics.html

Menril Basics
To get started with this mod, you will need to find some
Menril Trees
which can most easily be found in
Meneglin Biomes
.
After that, you’ll need to craft a
Squeezer
and
Drying Basin
for processing
Menril Logs
. This can be done by placing a block of wood in the
Squeezer
and jumping on it until it's flat. Once that has been done, the
Menril Resin
will try to go to the two sides of the
Squeezer
. Next, place a
Drying Basin
next to the
Squeezer
to make it fill up with the
Menril Resin
you just squeezed.
If you want to repeat the process, you can reset the
Squeezer
by applying a redstone pulse.
In the future, you will also be able to craft upgraded variants of these machines that are more easily automatable, but are more costly: the
Mechanical Squeezer
and the
Mechanical Drying Basin
.
Crafting Table
[Crafting Table]
[Stick]
[Block of Iron]
[Stick]
[Stick]
[Stick]
[Oak Planks]
[Spruce Planks]
[Birch Planks]
[Jungle Planks]
[Acacia Planks]
[Dark Oak Planks]
[Crimson Planks]
[Warped Planks]
[Mangrove Planks]
[Bamboo Planks]
[Cherry Planks]
[Menril Planks]
[Iron Ingot]
[Oak Planks]
[Spruce Planks]
[Birch Planks]
[Jungle Planks]
[Acacia Planks]
[Dark Oak Planks]
[Crimson Planks]
[Warped Planks]
[Mangrove Planks]
[Bamboo Planks]
[Cherry Planks]
[Menril Planks]
→
[Squeezer]
Crafting Table
[Crafting Table]
[Dark Oak Log]
[Dark Oak Wood]
[Stripped Dark Oak Log]
[Stripped Dark Oak Wood]
[Oak Log]
[Oak Wood]
[Stripped Oak Log]
[Stripped Oak Wood]
[Acacia Log]
[Acacia Wood]
[Stripped Acacia Log]
[Stripped Acacia Wood]
[Birch Log]
[Birch Wood]
[Stripped Birch Log]
[Stripped Birch Wood]
[Jungle Log]
[Jungle Wood]
[Stripped Jungle Log]
[Stripped Jungle Wood]
[Spruce Log]
[Spruce Wood]
[Stripped Spruce Log]
[Stripped Spruce Wood]
[Mangrove Log]
[Mangrove Wood]
[Stripped Mangrove Log]
[Stripped Mangrove Wood]
[Cherry Log]
[Cherry Wood]
[Stripped Cherry Log]
[Stripped Cherry Wood]
[Menril Log]
[Enriched Menril Log]
[Stripped Menril Log]
[Menril Wood]
[Stripped Menril Wood]
[Crimson Stem]
[Stripped Crimson Stem]
[Crimson Hyphae]
[Stripped Crimson Hyphae]
[Warped Stem]
[Stripped Warped Stem]
[Warped Hyphae]
[Stripped Warped Hyphae]
[Black Dye]
[Dark Oak Log]
[Dark Oak Wood]
[Stripped Dark Oak Log]
[Stripped Dark Oak Wood]
[Oak Log]
[Oak Wood]
[Stripped Oak Log]
[Stripped Oak Wood]
[Acacia Log]
[Acacia Wood]
[Stripped Acacia Log]
[Stripped Acacia Wood]
[Birch Log]
[Birch Wood]
[Stripped Birch Log]
[Stripped Birch Wood]
[Jungle Log]
[Jungle Wood]
[Stripped Jungle Log]
[Stripped Jungle Wood]
[Spruce Log]
[Spruce Wood]
[Stripped Spruce Log]
[Stripped Spruce Wood]
[Mangrove Log]
[Mangrove Wood]
[Stripped Mangrove Log]
[Stripped Mangrove Wood]
[Cherry Log]
[Cherry Wood]
[Stripped Cherry Log]
[Stripped Cherry Wood]
[Menril Log]
[Enriched Menril Log]
[Stripped Menril Log]
[Menril Wood]
[Stripped Menril Wood]
[Crimson Stem]
[Stripped Crimson Stem]
[Crimson Hyphae]
[Stripped Crimson Hyphae]
[Warped Stem]
[Stripped Warped Stem]
[Warped Hyphae]
[Stripped Warped Hyphae]
[Iron Ingot]
[Iron Ingot]
[Dark Oak Log]
[Dark Oak Wood]
[Stripped Dark Oak Log]
[Stripped Dark Oak Wood]
[Oak Log]
[Oak Wood]
[Stripped Oak Log]
[Stripped Oak Wood]
[Acacia Log]
[Acacia Wood]
[Stripped Acacia Log]
[Stripped Acacia Wood]
[Birch Log]
[Birch Wood]
[Stripped Birch Log]
[Stripped Birch Wood]
[Jungle Log]
[Jungle Wood]
[Stripped Jungle Log]
[Stripped Jungle Wood]
[Spruce Log]
[Spruce Wood]
[Stripped Spruce Log]
[Stripped Spruce Wood]
[Mangrove Log]
[Mangrove Wood]
[Stripped Mangrove Log]
[Stripped Mangrove Wood]
[Cherry Log]
[Cherry Wood]
[Stripped Cherry Log]
[Stripped Cherry Wood]
[Menril Log]
[Enriched Menril Log]
[Stripped Menril Log]
[Menril Wood]
[Stripped Menril Wood]
[Crimson Stem]
[Stripped Crimson Stem]
[Crimson Hyphae]
[Stripped Crimson Hyphae]
[Warped Stem]
[Stripped Warped Stem]
[Warped Hyphae]
[Stripped Warped Hyphae]
[Black Dye]
[Dark Oak Log]
[Dark Oak Wood]
[Stripped Dark Oak Log]
[Stripped Dark Oak Wood]
[Oak Log]
[Oak Wood]
[Stripped Oak Log]
[Stripped Oak Wood]
[Acacia Log]
[Acacia Wood]
[Stripped Acacia Log]
[Stripped Acacia Wood]
[Birch Log]
[Birch Wood]
[Stripped Birch Log]
[Stripped Birch Wood]
[Jungle Log]
[Jungle Wood]
[Stripped Jungle Log]
[Stripped Jungle Wood]
[Spruce Log]
[Spruce Wood]
[Stripped Spruce Log]
[Stripped Spruce Wood]
[Mangrove Log]
[Mangrove Wood]
[Stripped Mangrove Log]
[Stripped Mangrove Wood]
[Cherry Log]
[Cherry Wood]
[Stripped Cherry Log]
[Stripped Cherry Wood]
[Menril Log]
[Enriched Menril Log]
[Stripped Menril Log]
[Menril Wood]
[Stripped Menril Wood]
[Crimson Stem]
[Stripped Crimson Stem]
[Crimson Hyphae]
[Stripped Crimson Hyphae]
[Warped Stem]
[Stripped Warped Stem]
[Warped Hyphae]
[Stripped Warped Hyphae]
→
[Drying Basin]
Squeezer
[Squeezer]
[Menril Log]
→
[Menril Resin]
1000
Drying Basin
[Drying Basin]
7.5s
[Menril Resin]
1000
→
[Block of Crystallized Menril]
Advancements
Meneglin Discovery
Chop down a Menril tree, which can most easily be found in Meneglin biomes.
Squeezing like a boss
Craft a Squeezer.
Drying like a boss
Craft a Drying Basin.
Menril Production
Process Menril Log into Menril Resin and dry it to obtain Crystallized Menril.
Rewards
[Menril Log]
64
[Block of Crystallized Menril]
10
[Crystallized Menril Brick]
10
[Menril Berries]
20

## https://integrateddynamics.rubensworks.net/book/tutorials/nbt.html

NBT
NBT
tags are used to represent complex datastructures. These are typically used internally in Minecraft to store machine and entity information. In this tutorial, we will go over simple NBT tag reading and manipulation
As a first step, use a
Block Reader
on a lectern, read its
NBT
data, and visualize it in a
Display Panel
.
Next, from the read
Lectern
NBT
tag, read the
Book
sub-tag as a compound tag, and visualize it.
As a final step, we will read the
id
field from this
Book
sub-tag. However, instead of starting from our last read sub-tag, we will start from the root, and apply an
NBT Path
expression. Such expressions are useful to get deeply nested
NBT
values. For example, applying the expression
”.root.child1.child2”
on the
NBT
tag
{ root: { child1: { child2: ”some value” }}}
will output
”some value”
. More advanced features of
NBT Path
are described later in the advanced part of the manual.
To accomplish the final advancement, create an
NBT Path
expression that selects the
Book
/
id
field, apply it on our
Lectern
tag, and visualize it in a
Display Panel
.
Aspect
Tile Entity NBT
The target Tile Entity as NBT
Output:
NBT
Operator
NBT Compound Value Compound
NBT{}.get_compound
The Compound value in the given NBT compound tag with the given key
NBT
String
NBT
Global name: nbtGetCompound(
NBT
,
String
) →
NBT
Method name:
NBT
.getCompound(
String
) →
NBT
Operator
NBT Path Match First
NBT.path_match_first
Apply the given NBT Path expression on the given NBT value, and return the first match
String
NBT
NBT
Global name: stringNbtPathMatchFirst(
String
,
NBT
) →
NBT
Method name:
String
.nbtPathMatchFirst(
NBT
) →
NBT
Advancements
Lectern NBT
Read the NBT tag from a machine using a Block Reader, such as the Lectern.
Lectern NBT Book
Read the Book NBT field inside a Lectern block on the NBT tag read from a Block Reader.
Lectern NBT Book Name
Read the name (id) of a book inside a Lectern block by applying an NBT Path expression on the NBT tag read from a Block Reader.
Rewards
[Variable Card]
30
[Machine Reader]
5
[Display Panel]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/recipe_handling.html

Recipe Handling
Recipes
are a big part of
Minecraft
. That is why this mod offers first-class support for them via
Recipe
values, and
Ingredients
values to represent their input and output collections. In this tutorial, we will have a look at how recipes can be obtained and used.
The easiest way to obtain
Recipes
is simply by reading it from a machine that can handle recipes. So let's start by reading all crafting recipes by placing a
Machine Reader
against a
Crafting Table
and reading the list of recipes.
If you want some more fine-grained control over recipes, or if you are working with a machine that does not expose its recipes via the
Machine Reader
, you can visually create a
Recipe
yourself in the
Logic Programmer
. Let's try creating a simple recipe, for example for crafting a
Chest
.
In this last part of the tutorial, we'll take recipe reading one step further. Using the
Recipe By Output
aspect, we can retrieve an operator that, when applied to an
Ingredients
instance, will return the first
Recipe
that returns the given
Ingredients
as output. We will use this to look up the recipe of a
Chest
.
Let's start by creating an
Ingredients
instance with a single
Chest
in your
Logic Programmer
. Next, read the
Recipe By Output
aspect from a
Machine Reader
that targets a
Crafting Table
. After that, use the
Apply
operator to apply your
Chest
Ingredient
to the operator you got from the
Machine Reader
. Finally, insert the resulting variable into a
Display Panel
to see the
Chest
's recipe. (Don't forget to insert all your used variables into a
Variable Store
).
Obviously, there is much more you can do with
Recipes
and
Ingredients
. Have a look in the
Machine Reader
and
Logic Programmer
to find out what other advanced functionality is available to you.
Aspect
Recipes
Recipes that can be handled by the target
Output:
List
Aspect
Recipe By Output
An operator that returns a recipe for a given output.
Output:
Operator
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
Advancements
Recipe Reading
Read the list of recipes from a machine (like the Crafting Table) using a Machine Reader.
Recipe Creation
Visually create a §8Recipe§7 using the Logic Programmer.
Recipe Lookup
Dynamically lookup the recipe of a Chest using a Machine Reader.
Rewards
[Variable Card]
15
[Machine Reader]
5
[Display Panel]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html

Saving State
Everything you have built so far is
stateless
: a variable is always recomputed from the current values of its inputs, so a network has no memory of what happened before. Sometimes you need exactly that memory, for example a lamp that stays on after a button was pressed. In this tutorial, we will use the
Delayer
to save state.
A
Delayer
remembers the previous values of the
Variable Card
in its top slot, and exposes them as a
List
. Start by crafting one, attach it to a network with a
Redstone Reader
that is targeting a
Button
, and insert the reader's
Boolean
card of the aspect that is simply called
Redstone
, which is
True
while there is a signal. Next, set the
Capacity
to
1
in its gui, and leave
Ticks/Operation
at
1
.
The
newest
value is always the
last
element of the
List
, so a
Capacity
of
1
makes the history hold exactly one element: the value of the previous tick. With a larger
Capacity
, the first element would be an older value instead. Now write a new
Variable Card
using the bottom slots. This card refers to the
Delayer
's history.
The trick for saving state is a
feedback loop
: put a
Variable Card
in the
Delayer
whose value depends on the
Delayer
's own history. Each tick, the
Delayer
evaluates that card and stores the result, which becomes the previous value on the next tick. This dependency must be
indirect
: putting the history card itself in the top slot only gives an error that the variable is referring to itself. Let's use this to build a
latch
: a lamp that is switched on by one
Button
, and switched off by another.
Every operator you apply in the
Logic Programmer
results in its
own
Variable Card
, so we will build the latch from several small cards. You can find each operator by typing its name in the search field. Start by reading the two
Buttons
with
Redstone Readers
as
Boolean
Redstone
cards named
On
and
Off
. As the
Get Or Default
operator takes all of its inputs as cards, also create an
Integer
card with value
0
and a
Boolean
card with value
False
. Now define
Previous
as
Get Or Default
of the history card, that
Integer
and that
Boolean
, so that it also works while the history is still empty.
Next, define
NotOff
as
Not
Off
, then
Keep
as
Previous
And
NotOff
, and finally
State
as
On
Or
Keep
. Take the reader card out of the
Delayer
's top slot, and place
State
there instead. All other cards, including the intermediate ones, must be placed in a
Variable Store
, as the network can otherwise not find them. Finally, create a second
Variable Card
named
State
by building that same
Or
expression once more, as the first one has to stay in the
Delayer
, and insert it into the
Redstone
aspect of a
Redstone Writer
that is targeting a
Redstone Lamp
. The lamp will now stay on after pressing
On
, until you press
Off
.
For the final part, we will make a
toggle
that flips on every press of a
single
Button
: the
On
one. If we would simply invert
Previous
while the
Button
is pressed, the state would flip on every tick of the press. We therefore need to detect the exact tick in which the
Button
becomes
pressed. Place a second
Delayer
with
Capacity
2
, so that its history holds the two most recent values. Write a new
Redstone
card of that
Button
into its top slot, as the earlier
On
card has to stay in the
Variable Store
, and write its history card. This history is exactly
[False, True]
in the tick after the
Button
was pressed.
Create a static
List
of the
Booleans
False
and
True
, in that order, and define
Edge
as the second
Delayer
's history card
Equals
that
List
. Next, define
NotPrevious
as
Not
Previous
, and
State
as
Choice
of
Edge
,
NotPrevious
and
Previous
. Place it in the top slot of the first
Delayer
, which must still have
Capacity
1
, insert a second copy into the
Redstone Writer
, and put the new cards in the
Variable Store
. Every press of the
Button
now toggles the lamp. Feedback loops like these can save any kind of value, not just
Booleans
, so you can now build counters, timers and much more.
Crafting Table
[Crafting Table]
[Variable Card]
[Crystallized Menril Chunk]
[Variable Card]
[Clock]
[Variable Store]
[Clock]
[Variable Card]
[Crystallized Menril Chunk]
[Variable Card]
→
[Delayer]
Operator
Get Or Default
get_or_default
Safely get the list element at the given position, if that element is not available, return the given default value.
List
Integer
Any
Any
Global name: listGetOrDefault(
List
,
Integer
,
Any
) →
Any
Method name:
List
.getOrDefault(
Integer
,
Any
) →
Any
Operator
Not
!
Boolean
Boolean
Global name: booleanNot(
Boolean
) →
Boolean
Method name:
Boolean
.not() →
Boolean
Operator
And
&&
Boolean
Boolean
Boolean
Global name: booleanAnd(
Boolean
,
Boolean
) →
Boolean
Method name:
Boolean
.and(
Boolean
) →
Boolean
Operator
Or
||
Boolean
Boolean
Boolean
Global name: booleanOr(
Boolean
,
Boolean
) →
Boolean
Method name:
Boolean
.or(
Boolean
) →
Boolean
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
Choice
?
If the first value is true, the second value is taken, otherwise the third value.\nType two and three must be equal.
Boolean
Any
Any
Any
Global name: booleanChoice(
Boolean
,
Any
,
Any
) →
Any
Method name:
Boolean
.choice(
Any
,
Any
) →
Any
Advancements
Variable Delaying
Write a history variable in a §lDelayer§r.
State Latching
Build a latch that is set by one button and reset by another, by feeding a variable that depends on its own §lDelayer§r history back into that §lDelayer§r, and write it to a Redstone Writer.
State Toggling
Build a toggle that flips on every button press, by detecting the rising edge of the button in a second §lDelayer§r, and write it to a Redstone Writer.
Rewards
[Delayer]
2
[Redstone Reader]
5
[Redstone Writer]
5
[Variable Card]
64

## https://integrateddynamics.rubensworks.net/book/tutorials/theValueOfOperators.html

The Value of Operators
In
Integrated Dynamics
,
Operators
are first-class citizens. Up until now, we have only used them manually using the
Logic Programmer
, but in this tutorial, we’ll look into how they can be used in a more dynamic way, by storing
Operators
in
Variable Cards
, and invoking them dynamically.
As a first step, go to the
Logic Programmer
, and create a new static
Operator
, by starting to type
Arithmetic Addition
Operator
in the autocomplete dropdown-list and clicking it, and write it to a
Variable Card
. After that, create two
Integer
variables. Next, find the
Apply
operator, and apply the first
Integer
to the
Operator
, which will
bind
the first input argument of the addition
Operator
to that
Integer
, and will output a new
Operator
with one remaining input argument. Next, use the
Apply
operator again to
bind
the final input argument of the newly created
Operator
. Finally, use a
Display Panel
to visualize your dynamically invoked addition.
Next to applying
Operators
, there are a lot of other advanced
Operator
operations. We’ll now look into using
Operators
on
Lists
, more specifically the
Filter
Operator
that takes a
List
and a
Predicate
(an
Operator
with one input argument and a
Boolean
output value). Start by creating a
List
of the
Integers
1
,
10
and
100
. Next, store the
Relational Equals
Operator
in a variable. Next, apply this operator on the
Integer
10
. Next, use the
Filter
Operator
with your
List
and the applied
Operator
. This will filter away all the items from the
List
that return
False
by applying them on the given operator, which in this case means that all elements that are not equal to
10
will be removed from the list. Visualize the result in a
Display Panel
.
There are plenty of other operations on
Operators
, such as combining
Predicates
, flipping
Operator
arguments, and more operations on
Lists
.
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
Filter
filter
Filter a list of elements by matching them all with the given predicate.
Operator
List
List
Global name: operatorFilter(
Operator
,
List
) →
List
Method name:
Operator
.filter(
List
) →
List
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
Advancements
Dynamic Additions
Invoke the addition Operator dynamically
Dynamic List Filtering
Filter an Integer list 1, 10 and 100 to 10.
Rewards
[Variable Card]
64
[Variable Card]
64
[Variable Card]
64
[Variable Card]
64

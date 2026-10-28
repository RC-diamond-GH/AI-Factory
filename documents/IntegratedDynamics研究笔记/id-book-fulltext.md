# On the Dynamics of Integration — complete English text snapshot

Source: https://integrateddynamics.rubensworks.net/book/index.html

197 unique contents; directory aliases recorded separately in id-book-manifest.json. Generated environment is MC 1.21.1 / NeoForge, not the 1.20.1 target. This file preserves book descriptions, not verified target behavior.

## https://integrateddynamics.rubensworks.net/book/index.html

On the Dynamics of Integration
Introduction
Tutorials
Manual
Index
This is the Web-version of the
On the Dynamics of Integration
book, which is part of the
Integrated Dynamics
mod for Minecraft.
All information in this Web book is based on the default configuration of this mod, and may be different from ingame experience depending on your configuration.
This Web book was generated with the following mods:
Mekanism-1.21.1-10.7.18.84-all
commoncapabilities-1.21.1-neoforge-2.11.6-376
cyclopscore-1.21.1-neoforge-1.30.0-1139
integratedcrafting-1.21.1-neoforge-1.7.2-840
integrateddynamics-1.21.1-neoforge-1.38.0-2266
integratedmekanism-1.21.1-neoforge-1.1.3-151
integratednbt-1.21.1-neoforge-1.6.3-79
integratedrest-1.21.1-neoforge-1.1.15-210
integratedscripting-1.21.1-neoforge-1.0.27-483
integratedterminals-1.21.1-neoforge-1.10.0-1059
integratedtunnels-1.21.1-neoforge-1.13.0-801
refinedstorage-neoforge-2.0.9

## https://integrateddynamics.rubensworks.net/book/introduction.html

Introduction
For too long, automation possibilities have been too limited.
Integrated Dynamics
aims to be the go-to mod for automating your logic systems. It does this by providing an expressive immersive logic programming experience with hooks and support for most elements of
Minecraft
and its modifications.
Redstone circuits have always been a very basic, but workable and easily understandable method for automation. The basicness of redstone is however also an inconvenience. For automation networks with higher complexities, redstone circuits can become very large and consequently also difficult to manage. Furthermore, one small water-leak can cause major headaches to every redstoner.
The goal of this book is twofold. First, a
tutorial
is provided for learning the most important concepts behind this mod. Secondly, a detailed
manual
explains all elements and mechanisms for players that are not not interested in the tutorial.
You can navigate through this book by clicking on the arrows at the bottom, shift clicking will flip through whole sections instead of single pages. At the start of large sections you will see a content table which will contain hyperlinks to its subsections for faster browsing. When you come across recipes, the items that are explained in this book are clickable as a fast reference to the section they are explained in. You will also notice a button at the top left of this book, which will take you to the section above your current one. The button at the top right corner will go back to the previously visited page each time when clicked.

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

## https://integrateddynamics.rubensworks.net/book/manual/index.html

Manual
Introduction
Prerequisites
Networks
Logic
Parts
Machines
Logic Programming
Network Diagnostics
Tunnels
Crafting
Terminals
REST
Scripting
NBT
Mekanism

## https://integrateddynamics.rubensworks.net/book/manual/introduction.html

Introduction
This part of the book contains detailed descriptions of all parts of the mod.

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

## https://integrateddynamics.rubensworks.net/book/manual/logic_programming.html

Logic Programming
Another method for creating dynamic variable cards, is by applying an
Operator
on one or more
Variable Cards
to produce a new
Variable Card
. An example of an
Operator
is the addition of two
Numbers
, resulting in a new
Number
.
An
Operator
can have one or more input values, and has one output value. In most cases the inputs and output have a certain
Value Type
, otherwise, they will have the
Any
value type. An
Operator
with a single input and one output
Boolean
is called a
Predicate
.
The
Logic Programmer
contains a large collection of
Operators
. It also allows you to create static
Variable Cards
for all
Value Types
. After selecting an
Operator
, valid
Variable Cards
should be inserted in the expected input slots. Finally, another
Variable Card
should be inserted in the output slot. If the
Operator
is valid, a new dynamic variable will be written to the card. Remember that the referenced
Variable Cards
have to be present in a
Variable Store
in the same network where the
Operator
will be evaluated.
Besides writing values to
Variable Cards
, the
Logic Programmer
can also be used to modify existing
Variable Cards
. The value of an existing
Variable Card
can be loaded into the
Logic Programmer
by inserting it into the slot on the right-hand side while making sure no
Operators
or
Value Type
on the left-hand side is selected (which is the case when first opening the
Logic Programmer
, or the current element can be cleared).
The
Logic Programmer
allows you to search through the list of
Operators
by name using the textbox on the top. It also allows you to filter on input or output value types, by inserting a
Variable Card
of those types in the bottom-left slots.
If a
Labeller
is present in the player’s inventory, the resulting
Variable Card
can be labelled immediately by clicking on the ‘E’ button next to the output slot.
The
Logic Programmer
exists both in block-form and item-form. Both can be interchanged by placing them in a crafting grid.
The
Decode Value
element turns a value that was encoded in a
Materializer
into a Variable Card, when it is pasted into its text field. This way, values can be shared between players and worlds. Inside a network, the
Encode
,
Encode Compressed
and
Decode
operators do the same with strings.
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
Key Binding
Focus Search Logic Programmer
Alt + F
Key Binding
Rename Variable Logic Programmer
Alt + R
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
Nand
!&&
Boolean
Boolean
Boolean
Global name: booleanNand(
Boolean
,
Boolean
) →
Boolean
Method name:
Boolean
.nand(
Boolean
) →
Boolean
Nor
!||
Boolean
Boolean
Boolean
Global name: booleanNor(
Boolean
,
Boolean
) →
Boolean
Method name:
Boolean
.nor(
Boolean
) →
Boolean
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
Subtraction
-
Number
Number
Number
Global name: numberSubtract(
Number
,
Number
) →
Number
Method name:
Number
.subtract(
Number
) →
Number
Multiplication
*
Number
Number
Number
Global name: numberMultiply(
Number
,
Number
) →
Number
Method name:
Number
.multiply(
Number
) →
Number
Division
/
Number
Number
Number
Global name: numberDivide(
Number
,
Number
) →
Number
Method name:
Number
.divide(
Number
) →
Number
Maximum
max
Takes the largest of two values.
Number
Number
Number
Global name: numberMax(
Number
,
Number
) →
Number
Method name:
Number
.max(
Number
) →
Number
Minimum
min
Takes the smallest of two values.
Number
Number
Number
Global name: numberMin(
Number
,
Number
) →
Number
Method name:
Number
.min(
Number
) →
Number
Increment
++
Number
Number
Global name: numberIncrement(
Number
) →
Number
Method name:
Number
.increment() →
Number
Decrement
--
Number
Number
Global name: numberDecrement(
Number
) →
Number
Method name:
Number
.decrement() →
Number
Modulus
%
Number
Number
Number
Global name: numberModulus(
Number
,
Number
) →
Number
Method name:
Number
.modulus(
Number
) →
Number
Square Root
sqrt
Double
Double
Global name: doubleSqrt(
Double
) →
Double
Method name:
Double
.sqrt() →
Double
Power
pow
Double
Double
Double
Global name: doublePow(
Double
,
Double
) →
Double
Method name:
Double
.pow(
Double
) →
Double
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
Greater Than
>
Number
Number
Boolean
Global name: numberGreaterThan(
Number
,
Number
) →
Boolean
Method name:
Number
.greaterThan(
Number
) →
Boolean
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
Not Equals
!=
Tests whether two values are unequal. The types of both must be the same.
Any
Any
Boolean
Global name: anyNotEquals(
Any
,
Any
) →
Boolean
Method name:
Any
.notEquals(
Any
) →
Boolean
Greater Than or Equal
>=
Any
Any
Boolean
Global name: anyGreaterThanOrEquals(
Any
,
Any
) →
Boolean
Method name:
Any
.greaterThanOrEquals(
Any
) →
Boolean
Less Than or Equal
<=
Any
Any
Boolean
Global name: anyLessThanOrEquals(
Any
,
Any
) →
Boolean
Method name:
Any
.lessThanOrEquals(
Any
) →
Boolean
And
&
Integer
Integer
Integer
Global name: integerBinaryAnd(
Integer
,
Integer
) →
Integer
Method name:
Integer
.binaryAnd(
Integer
) →
Integer
Or
|
Integer
Integer
Integer
Global name: integerBinaryOr(
Integer
,
Integer
) →
Integer
Method name:
Integer
.binaryOr(
Integer
) →
Integer
Xor
^
Integer
Integer
Integer
Global name: integerXor(
Integer
,
Integer
) →
Integer
Method name:
Integer
.xor(
Integer
) →
Integer
Complement
~
Integer
Integer
Global name: integerComplement(
Integer
) →
Integer
Method name:
Integer
.complement() →
Integer
Left Shift
<<
Integer
Integer
Integer
Global name: integerLeftShift(
Integer
,
Integer
) →
Integer
Method name:
Integer
.leftShift(
Integer
) →
Integer
Right Shift
>>
Right Shift with ones fill
Integer
Integer
Integer
Global name: integerRightShift(
Integer
,
Integer
) →
Integer
Method name:
Integer
.rightShift(
Integer
) →
Integer
Right Zero Shift
>>>
Right Shift with zeros fill
Integer
Integer
Integer
Global name: integerUnsignedRightShift(
Integer
,
Integer
) →
Integer
Method name:
Integer
.unsignedRightShift(
Integer
) →
Integer
Length
len
The length of the given String
String
Integer
Global name: stringLength(
String
) →
Integer
Method name:
String
.length() →
Integer
Concat
+
Concatenate the two given Strings to one
String
String
String
Global name: stringConcat(
String
,
String
) →
String
Method name:
String
.concat(
String
) →
String
String Contains
contains
If the given substring is contained within the given string.
String
String
Boolean
Global name: stringContains(
String
,
String
) →
Boolean
Method name:
String
.contains(
String
) →
Boolean
Contains Regex
contains_regex
If the regular expression given matches any substring of the given string.
String
String
Boolean
Global name: stringContainsRegex(
String
,
String
) →
Boolean
Method name:
String
.containsRegex(
String
) →
Boolean
Matches Regex
matches_regex
If the regular expression given matches the given string.
String
String
Boolean
Global name: stringMatchesRegex(
String
,
String
) →
Boolean
Method name:
String
.matchesRegex(
String
) →
Boolean
Index Of
index_of
Get the index of the first occurrence of a substring matching the search term in the given string.
String
String
Integer
Global name: stringIndexOf(
String
,
String
) →
Integer
Method name:
String
.indexOf(
String
) →
Integer
Index Of Regex
index_of_regex
Get the index of the first occurrence of a substring matching the pattern in the given string.
String
String
Integer
Global name: stringIndexOfRegex(
String
,
String
) →
Integer
Method name:
String
.indexOfRegex(
String
) →
Integer
Starts With
starts_with
If the given substring matches the start of the given string.
String
String
Boolean
Global name: stringStartsWith(
String
,
String
) →
Boolean
Method name:
String
.startsWith(
String
) →
Boolean
Ends With
ends_with
If the string given matches the end of the given string.
String
String
Boolean
Global name: stringEndsWith(
String
,
String
) →
Boolean
Method name:
String
.endsWith(
String
) →
Boolean
Split On
split_on
Get a list containing pieces, split on the given (literal) delimiter, of the given string.
String
String
List
Global name: stringSplitOn(
String
,
String
) →
List
Method name:
String
.splitOn(
String
) →
List
Split On Regex
split_on_regex
Get a list containing pieces, split on the given regular expression, of the given string.
String
String
List
Global name: stringSplitOnRegex(
String
,
String
) →
List
Method name:
String
.splitOnRegex(
String
) →
List
Substring
substring
Get a substring, from the given index (inclusive) to the given index (exclusive), of the given string.
Integer
Integer
String
String
Global name: integerSubstring(
Integer
,
Integer
,
String
) →
String
Method name:
Integer
.substring(
Integer
,
String
) →
String
Regex Group
regex_group
Search for the given regular expression and return in that match the group at the given index, in the given string.
String
Integer
String
String
Global name: stringRegexGroup(
String
,
Integer
,
String
) →
String
Method name:
String
.regexGroup(
Integer
,
String
) →
String
Regex Groups
regex_groups
Search for the given regular expression and return in that match a list of all its groups, in the given string.
String
String
List
Global name: stringRegexGroups(
String
,
String
) →
List
Method name:
String
.regexGroups(
String
) →
List
Regex Scan
regex_scan
Search for all matches of the given regular expression and return the group at the given index, in the given string.
String
Integer
String
List
Global name: stringRegexScan(
String
,
Integer
,
String
) →
List
Method name:
String
.regexScan(
Integer
,
String
) →
List
Replace
replace
Find all instances of the search term and replace them with the given string, in the given string.
String
String
String
String
Global name: stringReplace(
String
,
String
,
String
) →
String
Method name:
String
.replace(
String
,
String
) →
String
Replace Regex
replace_regex
Find all matches of the search pattern and replace them with the given string, in the given string.
String
String
String
String
Global name: stringReplaceRegex(
String
,
String
,
String
) →
String
Method name:
String
.replaceRegex(
String
,
String
) →
String
Join
join
Create a joined string with the given string delimiter and the given list of strings.
String
List
String
Global name: stringJoin(
String
,
List
) →
String
Method name:
String
.join(
List
) →
String
To String
name
Get the string or name of a named object or converts it to a string
Named
String
Global name: namedName(
Named
) →
String
Method name:
Named
.name() →
String
Unique Name
uname
Get the unique name of an object
Uniquely Named
String
Global name: uniquely_namedUniqueName(
Uniquely Named
) →
String
Method name:
Uniquely Named
.uniqueName() →
String
Error
error
Throw a custom error
String
String
Global name: stringStringError(
String
) →
String
Method name:
String
.stringError() →
String
Round
|| ||
Round to the nearest Integer
Number
Integer
Global name: numberRound(
Number
) →
Integer
Method name:
Number
.round() →
Integer
Ceil
⌈ ⌉
Round up to the nearest Integer
Number
Integer
Global name: numberCeil(
Number
) →
Integer
Method name:
Number
.ceil() →
Integer
Floor
⌊ ⌋
Round down to the nearest Integer
Number
Integer
Global name: numberFloor(
Number
) →
Integer
Method name:
Number
.floor() →
Integer
Compact
compact
Show a shorter, less precise representation of a Number
Number
String
Global name: numberCompact(
Number
) →
String
Method name:
Number
.compact() →
String
Cast Number to Double
(double)
Cast any Number to a Double
Number
Double
Global name: numberToDouble(
Number
) →
Double
Method name:
Number
.toDouble() →
Double
Cast Number to Long
(long)
Cast any Number to a Long
Number
Long
Global name: numberToLong(
Number
) →
Long
Method name:
Number
.toLong() →
Long
Cast Number to Integer
(integer)
Cast any Number to an Integer
Number
Integer
Global name: numberToInteger(
Number
) →
Integer
Method name:
Number
.toInteger() →
Integer
Is Null
o
If the given value is null
Any
Boolean
Global name: anyIsNull(
Any
) →
Boolean
Method name:
Any
.isNull() →
Boolean
Is Not Null
∅
If the given value is not null
Any
Boolean
Global name: anyIsNotNull(
Any
) →
Boolean
Method name:
Any
.isNotNull() →
Boolean
Length
| |
The length of the given list
List
Integer
Global name: listLength(
List
) →
Integer
Method name:
List
.length() →
Integer
Empty
∅
If the list is empty
List
Boolean
Global name: listIsEmpty(
List
) →
Boolean
Method name:
List
.isEmpty() →
Boolean
Not Empty
o
If the list is not empty
List
Boolean
Global name: listIsNotEmpty(
List
) →
Boolean
Method name:
List
.isNotEmpty() →
Boolean
Get
get
Get the list element at the given position, throws an error if the index is out of bounds.
List
Integer
Any
Global name: listGet(
List
,
Integer
) →
Any
Method name:
List
.get(
Integer
) →
Any
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
Contains
contains
If the list contains the given element.
List
Any
Boolean
Global name: listContains(
List
,
Any
) →
Boolean
Method name:
List
.contains(
Any
) →
Boolean
Contains Predicate
contains_p
If at least one of the elements of the list returns true for the given predicate.
List
Operator
Boolean
Global name: listContainsPredicate(
List
,
Operator
) →
Boolean
Method name:
List
.containsPredicate(
Operator
) →
Boolean
Count
count
The number of times the given element is found in the list.
List
Any
Integer
Global name: listCount(
List
,
Any
) →
Integer
Method name:
List
.count(
Any
) →
Integer
Count Predicate
count_p
The number of times the given predicate returns true for the elements in the list.
List
Operator
Integer
Global name: listCountPredicate(
List
,
Operator
) →
Integer
Method name:
List
.countPredicate(
Operator
) →
Integer
Append
append
Append to the given list the given item.
List
Any
List
Global name: listAppend(
List
,
Any
) →
List
Method name:
List
.append(
Any
) →
List
Concat
concat
Concatenate the two lists to each other.
List
List
List
Global name: listConcat(
List
,
List
) →
List
Method name:
List
.concat(
List
) →
List
Lazy List Builder
lazybuilt
Build a list lazily using a start value and an operator that is applied to the previous element to get a next element.
Any
Operator
List
Global name: anyLazyBuilt(
Any
,
Operator
) →
List
Method name:
Any
.lazyBuilt(
Operator
) →
List
Head
head
Get the first element of the given list.
List
Any
Global name: listHead(
List
) →
Any
Method name:
List
.head() →
Any
Tail
tail
Remove the first element from the list.
List
List
Global name: listTail(
List
) →
List
Method name:
List
.tail() →
List
Unique Predicate
uniq_p
Deduplicate all elements in the list based on the given comparator predicate.
List
Operator
List
Global name: listUniquePredicate(
List
,
Operator
) →
List
Method name:
List
.uniquePredicate(
Operator
) →
List
Uniq
uniq
Deduplicate all elements in the list.
List
List
Global name: listUnique(
List
) →
List
Method name:
List
.unique() →
List
Slice
slice
Take a subset of the given list from the given index (inclusive) to the given index (exclusive).
List
Integer
Integer
List
Global name: listSlice(
List
,
Integer
,
Integer
) →
List
Method name:
List
.slice(
Integer
,
Integer
) →
List
Intersection
∩
Returns a list of items present in both supplied lists
List
List
List
Global name: listIntersection(
List
,
List
) →
List
Method name:
List
.intersection(
List
) →
List
List Equals (Set)
=set=
Check if two lists are equal according to set semantics.
List
List
Boolean
Global name: listEquals_set(
List
,
List
) →
Boolean
Method name:
List
.equals_set(
List
) →
Boolean
List Equals (Multiset)
=multiset=
Check if two lists are equal according to multiset semantics.
List
List
Boolean
Global name: listEquals_multiset(
List
,
List
) →
Boolean
Method name:
List
.equals_multiset(
List
) →
Boolean
Opaque
opaque
If the given block is opaque, non-transparent
Block
Boolean
Global name: blockIsOpaque(
Block
) →
Boolean
Method name:
Block
.isOpaque() →
Boolean
Item
itemstack
The item representation of the given block
Block
Item
Global name: blockItemStack(
Block
) →
Item
Method name:
Block
.itemStack() →
Item
Mod
mod
The name of the mod owning the given block
Block
String
Global name: blockMod(
Block
) →
String
Method name:
Block
.mod() →
String
Block Break Sound
break_sound
The break sound of the given block
Block
String
Global name: blockBreakSound(
Block
) →
String
Method name:
Block
.breakSound() →
String
Block Place Sound
place_sound
The place sound of the given block
Block
String
Global name: blockPlaceSound(
Block
) →
String
Method name:
Block
.placeSound() →
String
Block Step Sound
step_sound
The step sound of the given block
Block
String
Global name: blockStepSound(
Block
) →
String
Method name:
Block
.stepSound() →
String
Block Is Shearable
is_shearable
If the given block is shearable
Block
Boolean
Global name: blockIsShearable(
Block
) →
Boolean
Method name:
Block
.isShearable() →
Boolean
Block Plant Age
plant_age
The age of the given plant block
Block
Integer
Global name: blockPlantAge(
Block
) →
Integer
Method name:
Block
.plantAge() →
Integer
Block By Name
block_by_name
Get the block corresponding to the given unique name.
String
Block
Global name: stringBlockByName(
String
) →
Block
Method name:
String
.blockByName() →
Block
Block Properties
block_props
Get the block properties as NBT compound tag.
Block
NBT
Global name: blockProperties(
Block
) →
NBT
Method name:
Block
.properties() →
NBT
Block Properties
block_with_props
Get the given block applied with the given properties.
Block
NBT
Block
Global name: blockWithProperties(
Block
,
NBT
) →
Block
Method name:
Block
.withProperties(
NBT
) →
Block
Block Properties
block_all_props
Get all possible block properties as an NBT compound tag with list values.
Block
NBT
Global name: blockPossibleProperties(
Block
) →
NBT
Method name:
Block
.possibleProperties() →
NBT
Block Tag Names
block_tag_names
The Tag names (strings) of the given block
Block
List
Global name: blockTags(
Block
) →
List
Method name:
Block
.tags() →
List
Block Tag Values
block_tag_values
The Tag values (blocks) of the given block tag name
String
List
Global name: stringBlocksByTag(
String
) →
List
Method name:
String
.blocksByTag() →
List
Size
size
The current stack size
Item
Integer
Global name: itemstackSize(
Item
) →
Integer
Method name:
Item
.size() →
Integer
Max size
maxsize
The maximum stack size
Item
Integer
Global name: itemstackMaxSize(
Item
) →
Integer
Method name:
Item
.maxSize() →
Integer
Stackable
stackable
If the item stack can hold at least two items.
Item
Boolean
Global name: itemstackIsStackable(
Item
) →
Boolean
Method name:
Item
.isStackable() →
Boolean
Damageable
damageable
If the item can take damage.
Item
Boolean
Global name: itemstackIsDamageable(
Item
) →
Boolean
Method name:
Item
.isDamageable() →
Boolean
Damage
damage
The current item damage
Item
Integer
Global name: itemstackDamage(
Item
) →
Integer
Method name:
Item
.damage() →
Integer
Max Damage
max_damage
The maximum item damage
Item
Integer
Global name: itemstackMaxDamage(
Item
) →
Integer
Method name:
Item
.maxDamage() →
Integer
Enchanted
enchanted
If the item is enchanted
Item
Boolean
Global name: itemstackIsEnchanted(
Item
) →
Boolean
Method name:
Item
.isEnchanted() →
Boolean
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
Repair Cost
repair_cost
The repair cost of the item
Item
Integer
Global name: itemstackRepairCost(
Item
) →
Integer
Method name:
Item
.repairCost() →
Integer
Rarity
rarity
The rarity of the item
Item
String
Global name: itemstackRarity(
Item
) →
String
Method name:
Item
.rarity() →
String
Strength vs Block
strength
The strength this item has against the given block
Item
Block
Double
Global name: itemstackStrength(
Item
,
Block
) →
Double
Method name:
Item
.strength(
Block
) →
Double
Can Harvest
can_harvest
If the item can harvest the given block
Item
Block
Boolean
Global name: itemstackCanHarvest(
Item
,
Block
) →
Boolean
Method name:
Item
.canHarvest(
Block
) →
Boolean
Block
block
The block from the given item
Item
Block
Global name: itemstackBlock(
Item
) →
Block
Method name:
Item
.block() →
Block
Has Fluid
is_fluidstack
If the given item has a fluid
Item
Boolean
Global name: itemstackIsFluidStack(
Item
) →
Boolean
Method name:
Item
.isFluidStack() →
Boolean
Fluid
fluidstack
The fluid from the given item
Item
Fluid
Global name: itemstackFluidStack(
Item
) →
Fluid
Method name:
Item
.fluidStack() →
Fluid
Fluid Capacity
fluidstack_capacity
The fluid capacity of the given item in mB
Item
Integer
Global name: itemstackFluidCapacity(
Item
) →
Integer
Method name:
Item
.fluidCapacity() →
Integer
NBT equals
=NBT=
If the data components of the given items are equal
Item
Item
Boolean
Global name: itemstackIsNbtEqual(
Item
,
Item
) →
Boolean
Method name:
Item
.isNbtEqual(
Item
) →
Boolean
Raw item equals no NBT
=NoNBT=
If the raw items of the given stacks are equal, ignoring data components and item count but including damage values.
Item
Item
Boolean
Global name: itemstackIsEqualNonNbt(
Item
,
Item
) →
Boolean
Method name:
Item
.isEqualNonNbt(
Item
) →
Boolean
Raw item equals
=Raw=
If the raw items of the given items are equal, ignoring NBT, item count, and damage values.
Item
Item
Boolean
Global name: itemstackIsEqualRaw(
Item
,
Item
) →
Boolean
Method name:
Item
.isEqualRaw(
Item
) →
Boolean
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
Fuel Burn Time
burn_time
The fuel burn time in ticks of the given item
Item
Integer
Global name: itemstackBurnTime(
Item
) →
Integer
Method name:
Item
.burnTime() →
Integer
Is Fuel
can_burn
If the given item can be used as fuel
Item
Boolean
Global name: itemstackCanBurn(
Item
) →
Boolean
Method name:
Item
.canBurn() →
Boolean
Item Tag Names
item_tag_names
The Tag names (strings) of the given item
Item
List
Global name: itemstackTags(
Item
) →
List
Method name:
Item
.tags() →
List
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
Item With Stacksize
with_size
Set the stacksize for the given item
Item
Integer
Item
Global name: itemstackWithSize(
Item
,
Integer
) →
Item
Method name:
Item
.withSize(
Integer
) →
Item
Is FE Container
is_fe_container
If the given item can hold FE
Item
Boolean
Global name: itemstackIsFeContainer(
Item
) →
Boolean
Method name:
Item
.isFeContainer() →
Boolean
FE Stored
stored_fe
The amount of FE stored in this item
Item
Integer
Global name: itemstackFeStored(
Item
) →
Integer
Method name:
Item
.feStored() →
Integer
FE Capacity
capacity_fe
The maximum amount of FE that can be stored in this item
Item
Integer
Global name: itemstackFeCapacity(
Item
) →
Integer
Method name:
Item
.feCapacity() →
Integer
Item Has Inventory
has_inventory
If the given item has an inventory
Item
Boolean
Global name: itemstackHasInventory(
Item
) →
Boolean
Method name:
Item
.hasInventory() →
Boolean
Item Inventory Size
inventory_size
Retrieve the inventory size of the given item handler contents
Item
Integer
Global name: itemstackInventorySize(
Item
) →
Integer
Method name:
Item
.inventorySize() →
Integer
Item Inventory
inventory
Retrieve the inventory of the given item handler contents
Item
List
Global name: itemstackInventory(
Item
) →
List
Method name:
Item
.inventory() →
List
Item By Name
item_by_name
Get the item corresponding to the given unique name.
String
Item
Global name: stringItemByName(
String
) →
Item
Method name:
String
.itemByName() →
Item
Item List Count
item_list_count
Get the total item count of exactly the given item in a list.
List
Item
Integer
Global name: listItemListCount(
List
,
Item
) →
Integer
Method name:
List
.itemListCount(
Item
) →
Integer
Item NBT
NBT()
Get the data of the given item as NBT.
Item
NBT
Global name: itemstackNbt(
Item
) →
NBT
Method name:
Item
.nbt() →
NBT
Has NBT
has_nbt
If the item stack has data components.
Item
Boolean
Global name: itemstackHasNbt(
Item
) →
Boolean
Method name:
Item
.hasNbt() →
Boolean
Data Keys
data_keys
Get all data component keys of the item stack.
Item
List
Global name: itemstackDataKeys(
Item
) →
List
Method name:
Item
.dataKeys() →
List
Data Value
data_value
Get the value of the given data component key.
Item
String
NBT
Global name: itemstackDataValue(
Item
,
String
) →
NBT
Method name:
Item
.dataValue(
String
) →
NBT
With Data
with_data
Add the given data component key and value to the given item stack.
Fluid
String
NBT
NBT
Global name: fluidstackWithData(
Fluid
,
String
,
NBT
) →
NBT
Method name:
Fluid
.withData(
String
,
NBT
) →
NBT
Tooltip
tooltip
Get the tooltip of the given item as a list of strings.
Item
List
Global name: itemstackTooltip(
Item
) →
List
Method name:
Item
.tooltip() →
List
Entity Tooltip
entity_item_tooltip
In the context of the given Player Entity, get the tooltip of the given item as a list of strings.
Entity
Item
List
Global name: entityEntityItemTooltip(
Entity
,
Item
) →
List
Method name:
Entity
.entityItemTooltip(
Item
) →
List
Is Mob
is_mob
If the entity is a mob
Entity
Boolean
Global name: entityIsMob(
Entity
) →
Boolean
Method name:
Entity
.isMob() →
Boolean
Is Animal
is_animal
If the entity is an animal
Entity
Boolean
Global name: entityIsAnimal(
Entity
) →
Boolean
Method name:
Entity
.isAnimal() →
Boolean
Is Item
is_item
If the entity is an item
Entity
Boolean
Global name: entityIsItem(
Entity
) →
Boolean
Method name:
Entity
.isItem() →
Boolean
Is Player
is_player
If the entity is a player
Entity
Boolean
Global name: entityIsPlayer(
Entity
) →
Boolean
Method name:
Entity
.isPlayer() →
Boolean
Is Minecart
is_minecart
If the entity is a minecart
Entity
Boolean
Global name: entityIsMinecart(
Entity
) →
Boolean
Method name:
Entity
.isMinecart() →
Boolean
Item
item
The item from the item entity
Entity
Item
Global name: entityItem(
Entity
) →
Item
Method name:
Entity
.item() →
Item
Health
health
The entity health
Entity
Double
Global name: entityHealth(
Entity
) →
Double
Method name:
Entity
.health() →
Double
Width
width
The entity width
Entity
Double
Global name: entityWidth(
Entity
) →
Double
Method name:
Entity
.width() →
Double
Height
height
The entity height
Entity
Double
Global name: entityHeight(
Entity
) →
Double
Method name:
Entity
.height() →
Double
Is Burning
is_burning
If the entity is burning
Entity
Boolean
Global name: entityEntityIsBurning(
Entity
) →
Boolean
Method name:
Entity
.entityIsBurning() →
Boolean
Is Wet
is_wet
If the entity is wet
Entity
Boolean
Global name: entityIsWet(
Entity
) →
Boolean
Method name:
Entity
.isWet() →
Boolean
Is Crouching
is_crouching
If the entity is crouching
Entity
Boolean
Global name: entityIsCrouching(
Entity
) →
Boolean
Method name:
Entity
.isCrouching() →
Boolean
Is Eating
is_eating
If the entity is eating
Entity
Boolean
Global name: entityIsEating(
Entity
) →
Boolean
Method name:
Entity
.isEating() →
Boolean
Armor Inventory
armor_inventory
The list of items the entity is carrying as armor.
Entity
List
Global name: entityArmorInventory(
Entity
) →
List
Method name:
Entity
.armorInventory() →
List
Player Inventory
inventory
The list of items the player is carrying.
Entity
List
Global name: entityInventory(
Entity
) →
List
Method name:
Entity
.inventory() →
List
Mod
mod
The name of the mod owning the given entity
Entity
String
Global name: entityMod(
Entity
) →
String
Method name:
Entity
.mod() →
String
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
Target Entity
target_entity
The entity the given entity is currently looking at.
Entity
Entity
Global name: entityTargetEntity(
Entity
) →
Entity
Method name:
Entity
.targetEntity() →
Entity
Has Gui Open
has_gui_open
If the given player has an external gui open.
Entity
Boolean
Global name: entityHasGuiOpen(
Entity
) →
Boolean
Method name:
Entity
.hasGuiOpen() →
Boolean
Held Item Mainhand
held_item_1
The item the given entity is currently holding in its main hand.
Entity
Item
Global name: entityHeldItem(
Entity
) →
Item
Method name:
Entity
.heldItem() →
Item
Held Item Offhand
held_item_2
The item the given entity is currently holding in its off-hand.
Entity
Item
Global name: entityHeldItemOffHand(
Entity
) →
Item
Method name:
Entity
.heldItemOffHand() →
Item
Mounted Entities
mounted
The entities that are currently mounted on the given entity.
Entity
List
Global name: entityMounted(
Entity
) →
List
Method name:
Entity
.mounted() →
List
Item Frame Contents
itemframe_contents
The contents from the given Item Frame.
Entity
Item
Global name: entityItemFrameContents(
Entity
) →
Item
Method name:
Entity
.itemFrameContents() →
Item
Item Frame Rotation
itemframe_rotation
The rotation from the given Item Frame.
Entity
Integer
Global name: entityItemFrameRotation(
Entity
) →
Integer
Method name:
Entity
.itemFrameRotation() →
Integer
Entity Hurt Sound
hurtsound
The hurt sound of the given entity.
Entity
String
Global name: entityHurtSound(
Entity
) →
String
Method name:
Entity
.hurtSound() →
String
Entity Death Sound
deathsound
The death sound of the given entity.
Entity
String
Global name: entityDeathSound(
Entity
) →
String
Method name:
Entity
.deathSound() →
String
Entity Age
age
The age of the given entity.
Entity
Integer
Global name: entityAge(
Entity
) →
Integer
Method name:
Entity
.age() →
Integer
Entity Is Child
is_child
If the given entity is a child.
Entity
Boolean
Global name: entityIsChild(
Entity
) →
Boolean
Method name:
Entity
.isChild() →
Boolean
Entity Can Breed
canbreed
If the given entity is ready to be bred.
Entity
Boolean
Global name: entityCanBreed(
Entity
) →
Boolean
Method name:
Entity
.canBreed() →
Boolean
Entity Is In Love
is_in_love
If the given entity is in love and is ready to breed.
Entity
Boolean
Global name: entityIsInLove(
Entity
) →
Boolean
Method name:
Entity
.isInLove() →
Boolean
Entity Can Breed With
can_breed_with
If the given entity can be bred using the given item.
Entity
Item
Boolean
Global name: entityCanBreedWith(
Entity
,
Item
) →
Boolean
Method name:
Entity
.canBreedWith(
Item
) →
Boolean
Entity Is Shearable
is_shearable
If the given entity is shearable
Entity
Boolean
Global name: entityIsShearable(
Entity
) →
Boolean
Method name:
Entity
.isShearable() →
Boolean
Entity NBT
NBT()
Get the given entity as NBT.
Entity
NBT
Global name: entityNbt(
Entity
) →
NBT
Method name:
Entity
.nbt() →
NBT
Entity Type
entity_type
The entity type name.
Entity
String
Global name: entityType(
Entity
) →
String
Method name:
Entity
.type() →
String
Entity Items
entity_items
The items that are contained within this entity.
Entity
List
Global name: entityItems(
Entity
) →
List
Method name:
Entity
.items() →
List
Entity Fluids
entity_fluids
The fluids that are contained within this entity.
Entity
List
Global name: entityFluids(
Entity
) →
List
Method name:
Entity
.fluids() →
List
Entity Energy Stored
entity_stored_fe
The amount of energy that is stored in this entity.
Entity
Integer
Global name: entityEnergy(
Entity
) →
Integer
Method name:
Entity
.energy() →
Integer
Entity Energy Capacity
entity_capacity_fe
The energy capacity of this entity.
Entity
Integer
Global name: entityEnergyCapacity(
Entity
) →
Integer
Method name:
Entity
.energyCapacity() →
Integer
Amount
amount
The amount of mB
Fluid
Integer
Global name: fluidstackAmount(
Fluid
) →
Integer
Method name:
Fluid
.amount() →
Integer
Block
block
The block of the fluid
Fluid
Block
Global name: fluidstackBlock(
Fluid
) →
Block
Method name:
Fluid
.block() →
Block
Light level
light_level
The light level emitted by the fluid
Fluid
Integer
Global name: fluidstackLightLevel(
Fluid
) →
Integer
Method name:
Fluid
.lightLevel() →
Integer
Density
density
The fluid density
Fluid
Integer
Global name: fluidstackDensity(
Fluid
) →
Integer
Method name:
Fluid
.density() →
Integer
Temperature
temperature
The fluid temperature
Fluid
Integer
Global name: fluidstackTemperature(
Fluid
) →
Integer
Method name:
Fluid
.temperature() →
Integer
Viscosity
viscosity
The fluid viscosity
Fluid
Integer
Global name: fluidstackViscosity(
Fluid
) →
Integer
Method name:
Fluid
.viscosity() →
Integer
Is Lighter Than Air
lighter_than_air
If the fluid is lighter than air
Fluid
Boolean
Global name: fluidstackIsLighterThanAir(
Fluid
) →
Boolean
Method name:
Fluid
.isLighterThanAir() →
Boolean
Rarity
rarity
The fluid rarity
Fluid
String
Global name: fluidstackRarity(
Fluid
) →
String
Method name:
Fluid
.rarity() →
String
Bucket empty sound
sound_bucket_empty
The bucket empty sound for the fluid
Fluid
String
Global name: fluidstackBucketEmptySound(
Fluid
) →
String
Method name:
Fluid
.bucketEmptySound() →
String
Fluid vaporize sound
sound_fluid_vaporize
The fluid vaporize sound for the fluid
Fluid
String
Global name: fluidstackFluidVaporizeSound(
Fluid
) →
String
Method name:
Fluid
.fluidVaporizeSound() →
String
Bucket fill sound
sound_bucket_fill
The bucket fill sound for the fluid
Fluid
String
Global name: fluidstackBucketFillSound(
Fluid
) →
String
Method name:
Fluid
.bucketFillSound() →
String
Bucket
bucket
The filled bucket for the fluid
Fluid
Item
Global name: fluidstackBucket(
Fluid
) →
Item
Method name:
Fluid
.bucket() →
Item
Raw fluid equals
=Raw=
If the raw fluids are equal, ignoring quantity
Fluid
Fluid
Boolean
Global name: fluidstackIsRawEqual(
Fluid
,
Fluid
) →
Boolean
Method name:
Fluid
.isRawEqual(
Fluid
) →
Boolean
Mod
mod
The name of the mod owning the given fluid
Fluid
String
Global name: fluidstackMod(
Fluid
) →
String
Method name:
Fluid
.mod() →
String
Fluid NBT
NBT()
Get the data of the given fluid as NBT.
Fluid
NBT
Global name: fluidstackNbt(
Fluid
) →
NBT
Method name:
Fluid
.nbt() →
NBT
Fluid With Amount
with_amount
Copy the given fluid with the given amount
Fluid
Integer
Fluid
Global name: fluidstackWithAmount(
Fluid
,
Integer
) →
Fluid
Method name:
Fluid
.withAmount(
Integer
) →
Fluid
Data Keys
data_keys
Get all data component keys of the fluid stack.
Fluid
List
Global name: fluidstackDataKeys(
Fluid
) →
List
Method name:
Fluid
.dataKeys() →
List
Data Value
data_value
Get the value of the given data component key.
Fluid
String
NBT
Global name: fluidstackDataValue(
Fluid
,
String
) →
NBT
Method name:
Fluid
.dataValue(
String
) →
NBT
Fluid Tag Names
fluid_tag_names
The Tag names (strings) of the given fluid
Fluid
List
Global name: fluidstackTags(
Fluid
) →
List
Method name:
Fluid
.tags() →
List
Fluid Tag Values
fluid_tag_values
The Tag values (fluids) of the given fluid tag name
String
List
Global name: stringFluidsByTag(
String
) →
List
Method name:
String
.fluidsByTag() →
List
Fluid By Name
fluid_by_name
Get the fluid corresponding to the given unique name.
String
Fluid
Global name: stringFluidByName(
String
) →
Fluid
Method name:
String
.fluidByName() →
Fluid
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
Apply 2
apply2
Apply for a given operator the given two values.
Operator
Any
Any
Any
Global name: operatorApply2(
Operator
,
Any
,
Any
) →
Any
Method name:
Operator
.apply2(
Any
,
Any
) →
Any
Apply 3
apply3
Apply for a given operator the given three values.
Operator
Any
Any
Any
Any
Global name: operatorApply3(
Operator
,
Any
,
Any
,
Any
) →
Any
Method name:
Operator
.apply3(
Any
,
Any
,
Any
) →
Any
Apply N
apply_n
Apply for a given operator the given list of values.
Operator
List
Any
Global name: operatorApply_n(
Operator
,
List
) →
Any
Method name:
Operator
.apply_n(
List
) →
Any
Apply 0
apply0
Apply for a given operator without arguments.
Operator
Any
Global name: operatorApply0(
Operator
) →
Any
Method name:
Operator
.apply0() →
Any
Map
map
Apply the given operator on all elements of a list, resulting in a new list of mapped values.
Operator
List
List
Global name: operatorMap(
Operator
,
List
) →
List
Method name:
Operator
.map(
List
) →
List
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
Conjunction
.&&.
Takes the conjunction of two predicates.
Operator
Operator
Operator
Global name: operatorConjunction(
Operator
,
Operator
) →
Operator
Method name:
Operator
.conjunction(
Operator
) →
Operator
Disjunction
.||.
Takes the disjunction of two predicates.
Operator
Operator
Operator
Global name: operatorDisjunction(
Operator
,
Operator
) →
Operator
Method name:
Operator
.disjunction(
Operator
) →
Operator
Negation
!.
Takes the negation of a predicate.
Operator
Operator
Global name: operatorNegation(
Operator
) →
Operator
Method name:
Operator
.negation() →
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
Pipe 2
.2
Create a new operator that gives its input to the first and second operators, and pipes the outputs from both of them to the third operator.
Operator
Operator
Operator
Operator
Global name: operatorPipe2(
Operator
,
Operator
,
Operator
) →
Operator
Method name:
Operator
.pipe2(
Operator
,
Operator
) →
Operator
Flip
flip
Flip the two first input parameters of an operator.
Operator
Operator
Global name: operatorFlip(
Operator
) →
Operator
Method name:
Operator
.flip() →
Operator
Reduce
reduce
Apply the given operator on all elements of a list to reduce the list to one value.
Operator
List
Any
Any
Global name: operatorReduce(
Operator
,
List
,
Any
) →
Any
Method name:
Operator
.reduce(
List
,
Any
) →
Any
Reduce 1
reduce1
Apply the given operator on all elements of a list to reduce the list to one value. reduce1(op, list) is equivalent to reduce(op, tail(list), head(list)).
Operator
List
Any
Global name: operatorReduce1(
Operator
,
List
) →
Any
Method name:
Operator
.reduce1(
List
) →
Any
Operator By Name
op_by_name
Get the operator that has the given unique name.
String
Operator
Global name: stringOperatorByName(
String
) →
Operator
Method name:
String
.operatorByName() →
Operator
NBT Compound Size
NBT{}.size
The number of entries inside the given NBT compound tag
NBT
Integer
Global name: nbtSize(
NBT
) →
Integer
Method name:
NBT
.size() →
Integer
NBT Compound Keys
NBT{}.keys
The list of keys inside the given NBT compound tag
NBT
List
Global name: nbtKeys(
NBT
) →
List
Method name:
NBT
.keys() →
List
NBT Compound Has Key
NBT{}.has_key
If the given NBT compound tag contains the given key
NBT
String
Boolean
Global name: nbtHasKey(
NBT
,
String
) →
Boolean
Method name:
NBT
.hasKey(
String
) →
Boolean
NBT Compound Entry Type
NBT{}.type
The value type in the given NBT compound tag corresponding to the given key
NBT
String
String
Global name: nbtType(
NBT
,
String
) →
String
Method name:
NBT
.type(
String
) →
String
NBT Compound Value
NBT{}.get_tag
The value of any type in the given NBT compound tag with the given key
NBT
String
NBT
Global name: nbtGetTag(
NBT
,
String
) →
NBT
Method name:
NBT
.getTag(
String
) →
NBT
NBT Compound Value Boolean
NBT{}.get_boolean
The Boolean value in the given NBT compound tag with the given key
NBT
String
Boolean
Global name: nbtGetBoolean(
NBT
,
String
) →
Boolean
Method name:
NBT
.getBoolean(
String
) →
Boolean
NBT Compound Value Integer
NBT{}.get_integer
The Integer value in the given NBT compound tag with the given key
NBT
String
Integer
Global name: nbtGetInteger(
NBT
,
String
) →
Integer
Method name:
NBT
.getInteger(
String
) →
Integer
NBT Compound Value Long
NBT{}.get_long
The Long value in the given NBT compound tag with the given key
NBT
String
Long
Global name: nbtGetLong(
NBT
,
String
) →
Long
Method name:
NBT
.getLong(
String
) →
Long
NBT Compound Value Double
NBT{}.get_double
The Double value in the given NBT compound tag with the given key
NBT
String
Double
Global name: nbtGetDouble(
NBT
,
String
) →
Double
Method name:
NBT
.getDouble(
String
) →
Double
NBT Compound Value String
NBT{}.get_string
The String value in the given NBT compound tag with the given key
NBT
String
String
Global name: nbtGetString(
NBT
,
String
) →
String
Method name:
NBT
.getString(
String
) →
String
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
NBT Compound Value List NBT
NBT{}.get_list_tag
The NBT List value in the given NBT compound tag with the given key
NBT
String
List
Global name: nbtGetListTag(
NBT
,
String
) →
List
Method name:
NBT
.getListTag(
String
) →
List
NBT Compound Value Byte Array
NBT{}.get_list_byte
The Byte Array in the given NBT compound tag with the given key as Integer List
NBT
String
List
Global name: nbtGetListByte(
NBT
,
String
) →
List
Method name:
NBT
.getListByte(
String
) →
List
NBT Compound Value Integer Array
NBT{}.get_list_int
The Integer Array in the given NBT compound tag with the given key as Integer List
NBT
String
List
Global name: nbtGetListInt(
NBT
,
String
) →
List
Method name:
NBT
.getListInt(
String
) →
List
NBT Compound Value Long Array
NBT{}.get_list_long
The Long Array in the given NBT compound tag with the given key as Long List
NBT
String
List
Global name: nbtGetListLong(
NBT
,
String
) →
List
Method name:
NBT
.getListLong(
String
) →
List
NBT Compound Without
NBT{}.without
Get a copy of the given NBT compound tag without the given key
NBT
String
NBT
Global name: nbtWithout(
NBT
,
String
) →
NBT
Method name:
NBT
.without(
String
) →
NBT
NBT Compound With Boolean
NBT{}.with_boolean
Get a copy of the given NBT compound tag with the given Boolean entry
NBT
String
Boolean
NBT
Global name: nbtWithBoolean(
NBT
,
String
,
Boolean
) →
NBT
Method name:
NBT
.withBoolean(
String
,
Boolean
) →
NBT
NBT Compound With Short
NBT{}.with_short
Get a copy of the given NBT compound tag with the given Integer as a short entry
NBT
String
Integer
NBT
Global name: nbtWithShort(
NBT
,
String
,
Integer
) →
NBT
Method name:
NBT
.withShort(
String
,
Integer
) →
NBT
NBT Compound With Integer
NBT{}.with_integer
Get a copy of the given NBT compound tag with the given Integer entry
NBT
String
Integer
NBT
Global name: nbtWithInteger(
NBT
,
String
,
Integer
) →
NBT
Method name:
NBT
.withInteger(
String
,
Integer
) →
NBT
NBT Compound With Long
NBT{}.with_long
Get a copy of the given NBT compound tag with the given Long entry
NBT
String
Long
NBT
Global name: nbtWithLong(
NBT
,
String
,
Long
) →
NBT
Method name:
NBT
.withLong(
String
,
Long
) →
NBT
NBT Compound With Double
NBT{}.with_double
Get a copy of the given NBT compound tag with the given Double entry
NBT
String
Double
NBT
Global name: nbtWithDouble(
NBT
,
String
,
Double
) →
NBT
Method name:
NBT
.withDouble(
String
,
Double
) →
NBT
NBT Compound With Float
NBT{}.with_float
Get a copy of the given NBT compound tag with the given Double as a float entry
NBT
String
Double
NBT
Global name: nbtWithFloat(
NBT
,
String
,
Double
) →
NBT
Method name:
NBT
.withFloat(
String
,
Double
) →
NBT
NBT Compound With String
NBT{}.with_string
Get a copy of the given NBT compound tag with the given String entry
NBT
String
String
NBT
Global name: nbtWithString(
NBT
,
String
,
String
) →
NBT
Method name:
NBT
.withString(
String
,
String
) →
NBT
NBT Compound With NBT
NBT{}.with_tag
Get a copy of the given NBT compound tag with the given NBT entry
NBT
String
NBT
NBT
Global name: nbtWithTag(
NBT
,
String
,
NBT
) →
NBT
Method name:
NBT
.withTag(
String
,
NBT
) →
NBT
NBT Compound With NBT List
NBT{}.with_tag_list
Get a copy of the given NBT compound tag with the given NBT List entry
NBT
String
List
NBT
Global name: nbtWithTagList(
NBT
,
String
,
List
) →
NBT
Method name:
NBT
.withTagList(
String
,
List
) →
NBT
NBT Compound With Byte List
NBT{}.with_byte_list
Get a copy of the given NBT compound tag with the given Integer List as an NBT Byte Array entry
NBT
String
List
NBT
Global name: nbtWithByteList(
NBT
,
String
,
List
) →
NBT
Method name:
NBT
.withByteList(
String
,
List
) →
NBT
NBT Compound With Int List
NBT{}.with_int_list
Get a copy of the given NBT compound tag with the given NBT Integer Array entry
NBT
String
List
NBT
Global name: nbtWithIntList(
NBT
,
String
,
List
) →
NBT
Method name:
NBT
.withIntList(
String
,
List
) →
NBT
NBT Compound With Long List
NBT{}.with_list_long
Get a copy of the given NBT compound tag with the given NBT Long Array entry
NBT
String
List
NBT
Global name: nbtWithListLong(
NBT
,
String
,
List
) →
NBT
Method name:
NBT
.withListLong(
String
,
List
) →
NBT
NBT Compound Subset
NBT{}.⊆
If the first NBT compound tag is a subset of, or equal to the second NBT compound tag. This will recursively check nested tags.
NBT
NBT
Boolean
Global name: nbtIsSubset(
NBT
,
NBT
) →
Boolean
Method name:
NBT
.isSubset(
NBT
) →
Boolean
NBT Compound Union
NBT{}.∪
The union of the given NBT compound tags. Nested tags will be joined recursively.
NBT
NBT
NBT
Global name: nbtUnion(
NBT
,
NBT
) →
NBT
Method name:
NBT
.union(
NBT
) →
NBT
NBT Compound Intersection
NBT{}.∩
The intersection of the given NBT compound tags. Nested tags will be intersected recursively.
NBT
NBT
NBT
Global name: nbtIntersection(
NBT
,
NBT
) →
NBT
Method name:
NBT
.intersection(
NBT
) →
NBT
NBT Compound Minus
NBT{}.∖
The difference of the given NBT compound tags. Nested tags will be subtracted recursively.
NBT
NBT
NBT
Global name: nbtMinus(
NBT
,
NBT
) →
NBT
Method name:
NBT
.minus(
NBT
) →
NBT
NBT Boolean As Boolean
NBT.as_boolean
Get the Boolean value of the given NBT Byte tag
NBT
Boolean
Global name: nbtAsBoolean(
NBT
) →
Boolean
Method name:
NBT
.asBoolean() →
Boolean
NBT Byte As Integer
NBT.as_byte
Get the Integer value of the given NBT Byte tag
NBT
Integer
Global name: nbtAsByte(
NBT
) →
Integer
Method name:
NBT
.asByte() →
Integer
NBT Short as Integer
NBT.as_short
Get the Integer value of the given NBT Short tag
NBT
Integer
Global name: nbtAsShort(
NBT
) →
Integer
Method name:
NBT
.asShort() →
Integer
NBT Integer As Integer
NBT.as_int
Get the Integer value of the given NBT Integer tag
NBT
Integer
Global name: nbtAsInt(
NBT
) →
Integer
Method name:
NBT
.asInt() →
Integer
NBT Long As Long
NBT.as_long
Get the Long value of the given NBT Long tag
NBT
Long
Global name: nbtAsLong(
NBT
) →
Long
Method name:
NBT
.asLong() →
Long
NBT Double As Double
NBT.as_double
Get the Double value of the given NBT Double tag
NBT
Double
Global name: nbtAsDouble(
NBT
) →
Double
Method name:
NBT
.asDouble() →
Double
NBT Float As Double
NBT.as_float
Get the Double value of the given NBT Float tag
NBT
Double
Global name: nbtAsFloat(
NBT
) →
Double
Method name:
NBT
.asFloat() →
Double
NBT String As String
NBT.as_string
Get the String value of the given NBT String tag
NBT
String
Global name: nbtAsString(
NBT
) →
String
Method name:
NBT
.asString() →
String
NBT List As NBT List
NBT.as_tag_list
Get the NBT List value of the given NBT List tag
NBT
List
Global name: nbtAsTagList(
NBT
) →
List
Method name:
NBT
.asTagList() →
List
NBT Byte Array As Byte List
NBT.as_byte_list
Get the Byte List value of the given NBT Byte Array tag
NBT
List
Global name: nbtAsByteList(
NBT
) →
List
Method name:
NBT
.asByteList() →
List
NBT Integer Array As Integer List
NBT.as_int_list
Get the Integer List value of the given NBT Integer Array tag
NBT
List
Global name: nbtAsIntList(
NBT
) →
List
Method name:
NBT
.asIntList() →
List
NBT Long Array As Long List
NBT.as_long_list
Get the Long List value of the given NBT Long Array tag
NBT
List
Global name: nbtAsLongList(
NBT
) →
List
Method name:
NBT
.asLongList() →
List
NBT Byte From Boolean
NBT.from_boolean
Create an NBT Byte tag from the given Boolean value
Boolean
NBT
Global name: booleanAsNbt(
Boolean
) →
NBT
Method name:
Boolean
.asNbt() →
NBT
NBT Short From Integer
NBT.from_short
Create an NBT Short tag from the given Integer value
Integer
NBT
Global name: shortAsNbt(
Integer
) →
NBT
Method name:
Integer
.shortAsNbt() →
NBT
NBT Byte From Integer
NBT.from_byte
Create an NBT Byte tag from the given Integer value
Integer
NBT
Global name: byteAsNbt(
Integer
) →
NBT
Method name:
Integer
.byteAsNbt() →
NBT
NBT Integer From Integer
NBT.from_int
Create an NBT Integer tag from the given Integer value
Integer
NBT
Global name: integerAsNbt(
Integer
) →
NBT
Method name:
Integer
.asNbt() →
NBT
NBT Long From Long
NBT.from_long
Create an NBT Long tag from the given Long value
Long
NBT
Global name: longAsNbt(
Long
) →
NBT
Method name:
Long
.asNbt() →
NBT
NBT Double From Double
NBT.from_double
Create an NBT Double tag from the given Double value
Double
NBT
Global name: doubleAsNbt(
Double
) →
NBT
Method name:
Double
.asNbt() →
NBT
NBT Float From Double
NBT.from_float
Create an NBT Float tag from the given Double value
Double
NBT
Global name: floatAsNbt(
Double
) →
NBT
Method name:
Double
.floatAsNbt() →
NBT
NBT String From String
NBT.from_string
Create an NBT String tag from the given String value
String
NBT
Global name: stringAsNbt(
String
) →
NBT
Method name:
String
.asNbt() →
NBT
NBT List From Tag List
NBT.from_tag_list
Create an NBT List tag from the given NBT List value
List
NBT
Global name: tagListAsNbt(
List
) →
NBT
Method name:
List
.tagListAsNbt() →
NBT
NBT Byte Array From Byte List
NBT.from_byte_list
Create an NBT Byte Array tag from the given Integer List value
List
NBT
Global name: byteListAsNbt(
List
) →
NBT
Method name:
List
.byteListAsNbt() →
NBT
NBT Integer Array From Integer List
NBT.from_int_list
Create an NBT Integer Array tag from the given Integer List value
List
NBT
Global name: intListAsNbt(
List
) →
NBT
Method name:
List
.intListAsNbt() →
NBT
NBT Long Array From Long List
NBT.from_long_list
Create an NBT Long Array tag from the given Long List value
List
NBT
Global name: longListAsNbt(
List
) →
NBT
Method name:
List
.longListAsNbt() →
NBT
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
Ingredient items
Ingr.items
The list of items
Ingredients
List
Global name: ingredientsItems(
Ingredients
) →
List
Method name:
Ingredients
.items() →
List
Ingredient fluids
Ingr.fluids
The list of fluids
Ingredients
List
Global name: ingredientsFluids(
Ingredients
) →
List
Method name:
Ingredients
.fluids() →
List
Ingredient energy elements
Ingr.energies
The list of energy elements
Ingredients
List
Global name: ingredientsEnergies(
Ingredients
) →
List
Method name:
Ingredients
.energies() →
List
Ingredients With Item
Ingr.with_item
Get a copy of the given ingredients with the given item at the given ingredient position
Ingredients
Integer
Item
Ingredients
Global name: ingredientsWithItem(
Ingredients
,
Integer
,
Item
) →
Ingredients
Method name:
Ingredients
.withItem(
Integer
,
Item
) →
Ingredients
Ingredients With Fluid
Ingr.with_fluid
Get a copy of the given ingredients with the given fluid at the given ingredient position
Ingredients
Integer
Fluid
Ingredients
Global name: ingredientsWithFluid(
Ingredients
,
Integer
,
Fluid
) →
Ingredients
Method name:
Ingredients
.withFluid(
Integer
,
Fluid
) →
Ingredients
Ingredients With Energy
Ingr.with_energy
Get a copy of the given ingredients with the given energy at the given ingredient position
Ingredients
Integer
Long
Ingredients
Global name: ingredientsWithEnergy(
Ingredients
,
Integer
,
Long
) →
Ingredients
Method name:
Ingredients
.withEnergy(
Integer
,
Long
) →
Ingredients
Ingredients With Items
Ingr.with_items
Get a copy of the given ingredients with the given list of items at the given ingredient position
Ingredients
List
Ingredients
Global name: ingredientsWithItems(
Ingredients
,
List
) →
Ingredients
Method name:
Ingredients
.withItems(
List
) →
Ingredients
Ingredients With Fluids
Ingr.with_fluids
Get a copy of the given ingredients with the given list of fluids at the given ingredient position
Ingredients
List
Ingredients
Global name: ingredientsWithFluids(
Ingredients
,
List
) →
Ingredients
Method name:
Ingredients
.withFluids(
List
) →
Ingredients
Ingredients With Energy Elements
Ingr.with_energies
Get a copy of the given ingredients with the given list of energy elements at the given ingredient position
Ingredients
List
Ingredients
Global name: ingredientsWithEnergies(
Ingredients
,
List
) →
Ingredients
Method name:
Ingredients
.withEnergies(
List
) →
Ingredients
Recipe Input Ingredients
recipe_in
The input ingredients of the given recipe
Recipe
Ingredients
Global name: recipeInput(
Recipe
) →
Ingredients
Method name:
Recipe
.input() →
Ingredients
Recipe Output Ingredients
recipe_out
The output ingredients of the given recipe
Recipe
Ingredients
Global name: recipeOutput(
Recipe
) →
Ingredients
Method name:
Recipe
.output() →
Ingredients
Recipe With Input Ingredients
Recipe.with_in
Get a copy of the given recipe with the given ingredients as input
Recipe
Ingredients
Recipe
Global name: recipeWithInput(
Recipe
,
Ingredients
) →
Recipe
Method name:
Recipe
.withInput(
Ingredients
) →
Recipe
Recipe With Output Ingredients
Recipe.with_out
Get a copy of the given recipe with the given ingredients as output
Recipe
Ingredients
Recipe
Global name: recipeWithOutput(
Recipe
,
Ingredients
) →
Recipe
Method name:
Recipe
.withOutput(
Ingredients
) →
Recipe
Recipe With Input and Output Ingredients
Recipe.with_io
Create a recipe with the first ingredients as input, and the second ingredients as output
Ingredients
Ingredients
Recipe
Global name: ingredientsWithInputOutput(
Ingredients
,
Ingredients
) →
Recipe
Method name:
Ingredients
.withInputOutput(
Ingredients
) →
Recipe
Parse
parse_boolean
String
Boolean
Global name: stringParseAsBoolean(
String
) →
Boolean
Method name:
String
.parseAsBoolean() →
Boolean
Parse
parse_double
String
Double
Global name: stringParseAsDouble(
String
) →
Double
Method name:
String
.parseAsDouble() →
Double
Parse
parse_integer
String
Integer
Global name: stringParseAsInteger(
String
) →
Integer
Method name:
String
.parseAsInteger() →
Integer
Parse
parse_long
String
Long
Global name: stringParseAsLong(
String
) →
Long
Method name:
String
.parseAsLong() →
Long
Parse
parse_nbt
String
NBT
Global name: stringParseAsNbt(
String
) →
NBT
Method name:
String
.parseAsNbt() →
NBT
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
Identity
id
A copy of the input value.
Any
Any
Global name: anyIdentity(
Any
) →
Any
Method name:
Any
.identity() →
Any
Constant
K
A copy of the first input value.
Any
Any
Any
Global name: anyConstant(
Any
,
Any
) →
Any
Method name:
Any
.constant(
Any
) →
Any
Encode
encode
A materialized copy of the input value, encoded as a readable string.
Any
String
Global name: anyEncode(
Any
) →
String
Method name:
Any
.encode() →
String
Encode Compressed
encode_compressed
A materialized copy of the input value, encoded as a compressed string.
Any
String
Global name: anyEncodeCompressed(
Any
) →
String
Method name:
Any
.encodeCompressed() →
String
Decode
decode
The value that was encoded in the given string, by the Encode operator or a Materializer, in either its readable or its compressed form.
String
Any
Global name: stringDecode(
String
) →
Any
Method name:
String
.decode() →
Any
Cast
()
Integer
Double
Global name: integerIntegerToDouble(
Integer
) →
Double
Method name:
Integer
.integerToDouble() →
Double
Cast
()
Integer
Long
Global name: integerIntegerToLong(
Integer
) →
Long
Method name:
Integer
.integerToLong() →
Long
Cast
()
Double
Integer
Global name: doubleDoubleToInteger(
Double
) →
Integer
Method name:
Double
.doubleToInteger() →
Integer
Cast
()
Double
Long
Global name: doubleDoubleToLong(
Double
) →
Long
Method name:
Double
.doubleToLong() →
Long
Cast
()
Long
Integer
Global name: longLongToInteger(
Long
) →
Integer
Method name:
Long
.longToInteger() →
Integer
Cast
()
Long
Double
Global name: longLongToDouble(
Long
) →
Double
Method name:
Long
.longToDouble() →
Double
Has Chemical
is_chemicalstack
If the given item has a chemical
Item
Boolean
Global name: itemstackIsChemicalStack(
Item
) →
Boolean
Method name:
Item
.isChemicalStack() →
Boolean
Chemical
chemicalstack
The chemical from the given item
Item
Chemical
Global name: itemstackChemicalStack(
Item
) →
Chemical
Method name:
Item
.chemicalStack() →
Chemical
Chemical Capacity
chemicalstack_capacity
The chemical capacity of the given item in mB
Item
Long
Global name: itemstackChemicalCapacity(
Item
) →
Long
Method name:
Item
.chemicalCapacity() →
Long
Amount
amount
The amount of mB
Chemical
Long
Global name: chemicalstackAmount(
Chemical
) →
Long
Method name:
Chemical
.amount() →
Long
Tint
tint
The tint color of the chemical type
Chemical
Integer
Global name: chemicalstackTint(
Chemical
) →
Integer
Method name:
Chemical
.tint() →
Integer
Is Radioactive
is_radioactive
If the given chemical is radioactive
Chemical
Boolean
Global name: chemicalstackIsRadioactive(
Chemical
) →
Boolean
Method name:
Chemical
.isRadioactive() →
Boolean
Radioactivity
radioactivity
The radioactivity of the given chemical
Chemical
Double
Global name: chemicalstackRadioactivity(
Chemical
) →
Double
Method name:
Chemical
.radioactivity() →
Double
Is Coolant
is_coolant
If the given chemical is a cooled coolant or heated coolant
Chemical
Boolean
Global name: chemicalstackIsCoolant(
Chemical
) →
Boolean
Method name:
Chemical
.isCoolant() →
Boolean
Thermal Enthalpy
thermal_enthalpy
The thermal enthalpy of the given coolant chemical
Chemical
Double
Global name: chemicalstackThermalEnthalpy(
Chemical
) →
Double
Method name:
Chemical
.thermalEnthalpy() →
Double
Conductivity
conductivity
The conductivity of the given coolant chemical
Chemical
Double
Global name: chemicalstackConductivity(
Chemical
) →
Double
Method name:
Chemical
.conductivity() →
Double
Is Cooled Coolant
is_cooled_coolant
If the given chemical is a cooled coolant
Chemical
Boolean
Global name: chemicalstackIsCooledCoolant(
Chemical
) →
Boolean
Method name:
Chemical
.isCooledCoolant() →
Boolean
Cooled Coolant Of
cooled_coolant_of
Get the heated chemical of the given cooled coolant
Chemical
Chemical
Global name: chemicalstackCooledCoolantOf(
Chemical
) →
Chemical
Method name:
Chemical
.cooledCoolantOf() →
Chemical
Is Heated Coolant
is_heated_coolant
If the given chemical is a heated coolant
Chemical
Boolean
Global name: chemicalstackIsHeatedCoolant(
Chemical
) →
Boolean
Method name:
Chemical
.isHeatedCoolant() →
Boolean
Heated Coolant Of
heated_coolant_of
Get the cooled chemical of the given heated coolant
Chemical
Chemical
Global name: chemicalstackHeatedCoolantOf(
Chemical
) →
Chemical
Method name:
Chemical
.heatedCoolantOf() →
Chemical
Is Fuel
is_fuel
If the given chemical can be used as fuel
Chemical
Boolean
Global name: chemicalstackIsFuel(
Chemical
) →
Boolean
Method name:
Chemical
.isFuel() →
Boolean
Burn Ticks
burn_ticks
The number of ticks this chemical can burn as fuel
Chemical
Integer
Global name: chemicalstackBurnTicks(
Chemical
) →
Integer
Method name:
Chemical
.burnTicks() →
Integer
Energy per Tick
energy_per_tick
The amount of energy this chemical can produce when burnt as fuel
Chemical
Long
Global name: chemicalstackEnergyPerTick(
Chemical
) →
Long
Method name:
Chemical
.energyPerTick() →
Long
Raw chemical equals
=Raw=
If the raw chemicals are equal
Chemical
Chemical
Boolean
Global name: chemicalstackIsRawEqual(
Chemical
,
Chemical
) →
Boolean
Method name:
Chemical
.isRawEqual(
Chemical
) →
Boolean
Mod
mod
The name of the mod owning the given chemical
Chemical
String
Global name: chemicalstackMod(
Chemical
) →
String
Method name:
Chemical
.mod() →
String
With Amount
with_amount
Copy the given chemical with the given amount
Chemical
Long
Chemical
Global name: chemicalstackWithAmount(
Chemical
,
Long
) →
Chemical
Method name:
Chemical
.withAmount(
Long
) →
Chemical
Tag Names
chemical_tag_names
The Tag names (strings) of the given chemical
Chemical
List
Global name: chemicalstackTags(
Chemical
) →
List
Method name:
Chemical
.tags() →
List
Tag Values
chemical_tag_values
The Tag values (chemicals) of the given chemical tag name
String
List
Global name: stringChemicalsByTag(
String
) →
List
Method name:
String
.chemicalsByTag() →
List
Chemical By Name
chemical_by_name
Get the chemical corresponding to the given unique name.
String
Chemical
Global name: stringChemicalByName(
String
) →
Chemical
Method name:
String
.chemicalByName() →
Chemical
Chemicals
Ingr.chemicals
The list of chemicals
Ingredients
List
Global name: ingredientsChemicals(
Ingredients
) →
List
Method name:
Ingredients
.chemicals() →
List
With Chemical
Ingr.with_chemical
Get a copy of the given ingredients with the given chemical at the given ingredient position
Ingredients
Integer
Chemical
Ingredients
Global name: ingredientsWithChemical(
Ingredients
,
Integer
,
Chemical
) →
Ingredients
Method name:
Ingredients
.withChemical(
Integer
,
Chemical
) →
Ingredients
With Chemicals
Ingr.with_chemicals
Get a copy of the given ingredients with the given list of chemicals at the given ingredient position
Ingredients
List
Ingredients
Global name: ingredientsWithChemicals(
Ingredients
,
List
) →
Ingredients
Method name:
Ingredients
.withChemicals(
List
) →
Ingredients
Radiation
radiation
Get the radiation level of the given entity (in Sv/h)
Entity
Double
Global name: entityRadiation(
Entity
) →
Double
Method name:
Entity
.radiation() →
Double

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

## https://integrateddynamics.rubensworks.net/book/manual/machines/energy_battery.html

Energy Battery
Integrated Dynamics
networks can be configured to consume energy. For this, you can attach
Energy Batteries
to the network to add energy storage capacity for it.
These batteries accept both
RF
-based and
Tesla
power.
Applying a redstone signal will enable auto-export mode of energy to all sides.
When this battery is held in your inventory,
auto-supply
mode can be enabled by shift+right clicking. When enabled, this battery will automatically try to fill the items you are holding in your two hands with energy.
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Block of Crystallized Menril]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Block of Redstone]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Block of Crystallized Menril]
[Crystallized Menril Chunk]
→
[Energy Battery]

## https://integrateddynamics.rubensworks.net/book/manual/machines/generator.html

Generator
When you need
RF
or
Tesla
energy, you can use the
Generator
to create energy from
burnable
items.
Crafting Table
[Crafting Table]
[Energy Battery]
[Furnace]
→
[Generator]

## https://integrateddynamics.rubensworks.net/book/manual/machines/index.html

Machines
Materializer
Proxy
Delayer
Energy Battery
Generator
Mechanical Squeezer
Mechanical Drying Basin

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

## https://integrateddynamics.rubensworks.net/book/manual/machines/mechanical_drying_basin.html

Mechanical Drying Basin
Drying with the
Drying Basin
can take a while. If you want things to go faster, or if you want to be able to dry more things, you can use the
Mechanical Drying Basin
, which requires energy to run.
Crafting Table
[Crafting Table]
[Obsidian]
[Crying Obsidian]
[Energy Battery]
[Drying Basin]
[Energy Battery]
[Obsidian]
[Crying Obsidian]
→
[Mechanical Drying Basin]
Mechanical Drying Basin
[Mechanical Drying Basin]
0.75s
[Menril Resin]
1000
→
[Block of Crystallized Menril]
[Mechanical Drying Basin]
1.5s
[Liquid Chorus]
1000
→
[Block of Crystallized Chorus]
Mechanical Drying Basin
[Mechanical Drying Basin]
0.5s
[Blue Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Lime Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
2s
[Clay]
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Light Blue Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
1s
[Wet Sponge]
→
[Sponge]
[Mechanical Drying Basin]
0.5s
[Cyan Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Brown Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
5s
[Lava]
1000
→
[Magma Block]
[Mechanical Drying Basin]
0.5s
[Red Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Yellow Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Gray Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Rotten Flesh]
→
[Leather]
[Mechanical Drying Basin]
0.75s
[Oak Sapling]
[Spruce Sapling]
[Birch Sapling]
[Jungle Sapling]
[Acacia Sapling]
[Dark Oak Sapling]
[Azalea]
[Flowering Azalea]
[Mangrove Propagule]
[Cherry Sapling]
[Menril Sapling]
→
[Dead Bush]
[Mechanical Drying Basin]
0.5s
[White Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Magma Block]
→
[Netherrack]
[Mechanical Drying Basin]
0.5s
[Dirt]
→
[Coarse Dirt]
[Mechanical Drying Basin]
0.5s
[Pink Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Kelp]
→
[Dried Kelp]
[Mechanical Drying Basin]
0.5s
[Mud]
→
[Dirt]
[Mechanical Drying Basin]
0.5s
[Black Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Purple Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Light Gray Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Green Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Magenta Terracotta]
[Water]
250
→
[Terracotta]
[Mechanical Drying Basin]
0.5s
[Orange Terracotta]
[Water]
250
→
[Terracotta]

## https://integrateddynamics.rubensworks.net/book/manual/machines/mechanical_squeezer.html

Mechanical Squeezer
There will come a time, when you will have had enough of all this jumping on
Squeezers
. If you haven't automated your
Squeezers
by then, perhaps it's time to look into making a
Mechanical Squeezer
. It does not require any manual effort, but it requires energy to run. This also makes it significantly faster than the regular
Squeezers
and allows more things to be squeezed with a higher yield.
Crafting Table
[Crafting Table]
[Diamond]
[Energy Battery]
[Squeezer]
[Energy Battery]
[Obsidian]
[Crying Obsidian]
→
[Mechanical Squeezer]
Mechanical Squeezer
[Mechanical Squeezer]
0.75s
[Popped Chorus Fruit]
→
[Crystallized Chorus Chunk]
2
[Crystallized Chorus Chunk]
0.5
[Crystallized Chorus Chunk]
0.5
[Liquid Chorus]
125
[Mechanical Squeezer]
0.75s
[Menril Planks]
→
[Crystallized Menril Chunk]
[Menril Resin]
250
[Mechanical Squeezer]
0.75s
[Menril Log]
→
[Crystallized Menril Chunk]
2
[Crystallized Menril Chunk]
0.5
[Crystallized Menril Chunk]
0.5
[Menril Resin]
1000
[Mechanical Squeezer]
0.75s
[Proto Chorus]
→
[Crystallized Chorus Chunk]
0.5
[Crystallized Chorus Chunk]
0.5
[Liquid Chorus]
125
Mechanical Squeezer
[Mechanical Squeezer]
2s
[Breeze Rod]
→
[Wind Charge]
6
[Mechanical Squeezer]
0.25s
[White Tulip]
→
[White Dye]
4
[White Dye]
0.5
2
[Mechanical Squeezer]
0.25s
[Lily of the Valley]
→
[White Dye]
4
[White Dye]
0.5
2
[Mechanical Squeezer]
0.5s
[Snowball]
→
[Water]
250
[Mechanical Squeezer]
1s
[Bricks]
→
[Brick]
4
[Mechanical Squeezer]
0.5s
[Glass]
[White Stained Glass]
[Orange Stained Glass]
[Magenta Stained Glass]
[Light Blue Stained Glass]
[Yellow Stained Glass]
[Lime Stained Glass]
[Pink Stained Glass]
[Gray Stained Glass]
[Light Gray Stained Glass]
[Cyan Stained Glass]
[Purple Stained Glass]
[Blue Stained Glass]
[Brown Stained Glass]
[Green Stained Glass]
[Red Stained Glass]
[Black Stained Glass]
[Tinted Glass]
→
[Sand]
[Mechanical Squeezer]
0.25s
[Rose Bush]
→
[Red Dye]
8
[Red Dye]
0.5
2
[Red Dye]
0.5
2
[Mechanical Squeezer]
0.25s
[Red Tulip]
→
[Red Dye]
4
[Red Dye]
0.5
2
[Mechanical Squeezer]
0.25s
[Poppy]
→
[Red Dye]
4
[Red Dye]
0.5
2
[Mechanical Squeezer]
0.25s
[Blue Orchid]
→
[Light Blue Dye]
4
[Light Blue Dye]
0.5
2
[Mechanical Squeezer]
2s
[Blaze Rod]
→
[Blaze Powder]
5
[Mechanical Squeezer]
0.25s
[Peony]
→
[Pink Dye]
8
[Pink Dye]
0.5
2
[Pink Dye]
0.5
2
[Mechanical Squeezer]
0.25s
[Pink Tulip]
→
[Pink Dye]
4
[Pink Dye]
0.5
2
[Mechanical Squeezer]
1s
[Blaze Powder]
→
[Lava]
250
[Mechanical Squeezer]
1s
[Facade]
→
[Facade]
[Mechanical Squeezer]
5s
[Magma Cream]
→
[Lava]
250
[Mechanical Squeezer]
0.25s
[Orange Tulip]
→
[Orange Dye]
4
[Orange Dye]
0.5
2
[Mechanical Squeezer]
0.25s
[Azure Bluet]
→
[Light Gray Dye]
4
[Light Gray Dye]
0.5
2
[Mechanical Squeezer]
0.25s
[Oxeye Daisy]
→
[Light Gray Dye]
4
[Light Gray Dye]
0.5
2
[Mechanical Squeezer]
37.5s
[Netherrack]
→
[Lava]
1000
[Mechanical Squeezer]
1s
[Ice]
→
[Water]
1000
[Mechanical Squeezer]
2s
[Sugar Cane]
→
[Sugar]
4
[Sugar]
0.5
2
[Mechanical Squeezer]
0.5s
[Bone]
→
[White Dye]
6
[White Dye]
0.5
2
[White Dye]
0.5
2
[Mechanical Squeezer]
1s
[Snow]
→
[Water]
1000
[Mechanical Squeezer]
0.25s
[Sunflower]
→
[Yellow Dye]
8
[Yellow Dye]
0.5
2
[Yellow Dye]
0.5
2
[Mechanical Squeezer]
0.25s
[Dandelion]
→
[Yellow Dye]
4
[Yellow Dye]
0.5
2
[Mechanical Squeezer]
0.25s
[Cornflower]
→
[Blue Dye]
4
[Blue Dye]
0.5
2
[Mechanical Squeezer]
1s
[Cactus]
→
[Green Dye]
4
[Green Dye]
0.5
2
[Water]
500
[Mechanical Squeezer]
0.25s
[Wither Rose]
→
[Black Dye]
4
[Black Dye]
0.5
2
[Mechanical Squeezer]
1s
[Honey Bottle]
→
[Sugar]
5
[Sugar]
0.75
[Mechanical Squeezer]
0.25s
[Allium]
→
[Magenta Dye]
4
[Magenta Dye]
0.5
2
[Mechanical Squeezer]
0.25s
[Lilac]
→
[Magenta Dye]
8
[Magenta Dye]
0.5
2
[Magenta Dye]
0.5
2
[Mechanical Squeezer]
1s
[Mud]
→
[Dirt]
[Water]
250
[Mechanical Squeezer]
0.75s
[Wet Sponge]
→
[Sponge]
[Water]
50
[Mechanical Squeezer]
0.25s
[White Wool]
[Orange Wool]
[Magenta Wool]
[Light Blue Wool]
[Yellow Wool]
[Lime Wool]
[Pink Wool]
[Gray Wool]
[Light Gray Wool]
[Cyan Wool]
[Purple Wool]
[Blue Wool]
[Brown Wool]
[Green Wool]
[Red Wool]
[Black Wool]
→
[String]
4
Mechanical Squeezer
[Mechanical Squeezer]
2s
[Lead Ore]
[Deepslate Lead Ore]
→
[Raw Lead]
3
[Raw Lead]
0.5
2
[Raw Lead]
0.5
2
[Mechanical Squeezer]
2s
[Stone]
→
[Cobblestone]
[Mechanical Squeezer]
2s
[Coal Ore]
[Deepslate Coal Ore]
→
[Coal]
6
[Coal]
0.5
[Mechanical Squeezer]
2s
[Emerald Ore]
[Deepslate Emerald Ore]
→
[Emerald]
[Emerald]
0.5
[Mechanical Squeezer]
1.5s
[Gilded Blackstone]
→
[Gold Nugget]
4
[Gold Nugget]
0.5
5
[Mechanical Squeezer]
1.5s
[Prismarine Shard]
→
[Prismarine Crystals]
[Mechanical Squeezer]
2s
[Iron Ore]
[Deepslate Iron Ore]
→
[Raw Iron]
3
[Raw Iron]
0.5
[Raw Iron]
0.5
[Mechanical Squeezer]
2s
[Lapis Lazuli Ore]
[Deepslate Lapis Lazuli Ore]
→
[Lapis Lazuli]
[Lapis Lazuli]
0.5
2
[Mechanical Squeezer]
2s
[Redstone Ore]
[Deepslate Redstone Ore]
→
[Redstone Dust]
12
[Redstone Dust]
0.5
2
[Mechanical Squeezer]
2s
[Magma Block]
→
[Magma Cream]
4
[Mechanical Squeezer]
1.5s
[Glowstone]
→
[Glowstone Dust]
4
[Mechanical Squeezer]
2s
[Uranium Ore]
[Deepslate Uranium Ore]
→
[Raw Uranium]
3
[Raw Uranium]
0.5
2
[Raw Uranium]
0.5
2
[Mechanical Squeezer]
2s
[Diamond Ore]
[Deepslate Diamond Ore]
→
[Diamond]
[Diamond]
0.5
[Mechanical Squeezer]
2s
[Ancient Debris]
→
[Netherite Scrap]
[Netherite Scrap]
0.75
[Mechanical Squeezer]
2s
[Tin Ore]
[Deepslate Tin Ore]
→
[Raw Tin]
3
[Raw Tin]
0.5
2
[Raw Tin]
0.5
2
[Mechanical Squeezer]
1.5s
[Sandstone]
→
[Sand]
2
[Mechanical Squeezer]
2s
[Osmium Ore]
[Deepslate Osmium Ore]
→
[Raw Osmium]
3
[Raw Osmium]
0.5
2
[Raw Osmium]
0.5
2
[Mechanical Squeezer]
2s
[Stone Bricks]
→
[Cracked Stone Bricks]
[Mechanical Squeezer]
2s
[Cobblestone]
→
[Gravel]
[Flint]
0.5
[Mechanical Squeezer]
1.5s
[Red Sandstone]
→
[Red Sand]
2
[Mechanical Squeezer]
2s
[Nether Quartz Ore]
→
[Nether Quartz]
3
[Nether Quartz]
0.5
[Mechanical Squeezer]
1.5s
[Blackstone]
→
[Gravel]
2
[Mechanical Squeezer]
0.5s
[Coal]
→
[Coal Dust]
[Mechanical Squeezer]
3s
[Obsidian]
→
[Obsidian Dust]
2
[Mechanical Squeezer]
2s
[Gold Ore]
[Nether Gold Ore]
[Deepslate Gold Ore]
→
[Raw Gold]
3
[Raw Gold]
0.5
2
[Raw Gold]
0.5
2
[Mechanical Squeezer]
2s
[Copper Ore]
[Deepslate Copper Ore]
→
[Raw Copper]
8
[Raw Copper]
0.5
2
[Raw Copper]
0.25
2
[Mechanical Squeezer]
1.5s
[Gravel]
→
[Flint]
[Flint]
0.5

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

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/attuned_recipes.html

Attuned Recipes
Since the
Attuned Crafting Interface
exposes
all
recipes of its machine, it may hand the network more recipes than you want it to use: a Crafting Table alone accounts for hundreds. Its GUI therefore shows every recipe of the bound machine as a grid of output icons, where you decide which ones the network is allowed to craft.
Recipes with a
green border
are exposed to the network. Clicking one gives it a
red border
and greys it out, which removes it from the network, so that no new crafting job uses it. Clicking it again brings it back. Hovering over a recipe shows its output, its recipe name and the ingredients it needs.
To change many recipes at once, hold down your mouse button and drag over them. The recipe you start on decides whether the drag enables or disables, so one gesture does both.
The search field matches recipe names, mod names and recipe ids. The
Enable
,
Disable
and
Invert
buttons apply to whatever the search currently matches, which is what makes a machine with thousands of recipes workable: searching for
minecraft:
and pressing
Disable
hides every vanilla recipe in one go.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/basics.html

Basics
The
Crafting Interface
is used to tell the network
where
and
how
things can be crafted.
By pointing a
Crafting Interface
to a machine
(like a Crafting table or Furnace)
, this interface will be bound to this machine to handle recipes.
When opening the GUI of a
Crafting Interface
, you can insert
Variable Cards
that hold
Recipes
. This will tell the interface that the given recipe can be crafted using the bound machine.
When you've progressed later in the game, you will be able to craft the
Attuned Crafting Interface
. It does not require you to manually add recipes to it, as it will read and expose all recipes that are available in the target machine. These are the same recipes that are listed in the
Machine Reader
. Some modded machines may not be supported, which will be visualized by a red border when placing the
Attuned Crafting Interface
.
The
Enumerated Crafting Interface
takes a single variable holding a
list of recipes
, and exposes every recipe in that list to the network. This allows you to derive the recipes of an interface using logic, for example by pointing the
Machine Reader
at a machine, or by filtering a list of recipes. Infinite lists are not supported, and very long lists are cut off at a limit that can be changed in the config.
Crafting Table
[Crafting Table]
[Iron Ingot]
[Crafting Table]
[Iron Ingot]
[Output Variable Transformer]
[Block of Crystallized Menril]
[Input Variable Transformer]
[Iron Ingot]
[Crafting Table]
[Iron Ingot]
→
[Crafting Interface]
Crafting Table
[Crafting Table]
[Emerald]
[Crafting Interface]
[Block of Crystallized Chorus]
[Crafting Interface]
[Emerald]
→
[Attuned Crafting Interface]
Crafting Table
[Crafting Table]
[Diamond]
[Crafting Interface]
[Block of Crystallized Chorus]
[Crafting Interface]
[Diamond]
→
[Enumerated Crafting Interface]

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/blocking.html

Blocking Mode
By default, the
Crafting Interface
will have
blocking mode
enabled. This will cause the machine to only accept one recipe at a time. It will only start with the next recipe if the previous one has completed.
For machines that can process multiple inputs in parallel, you may disable
blocking mode
via the settings GUI of the
Crafting Interface
. When disabling
blocking mode
, as many inputs as possible will be processed by the machine if you have a multi-amount crafting job.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/configuration.html

Configuration
Like most
Integrated Dynamics
parts, you can configure things in the
Crafting Interface
such as the channel, priority and tick rate.
Additionally, you can change the target side separately
for each ingredient type
. This is useful if the bound machine accepts different ingredient types in different sides. For example, items may be accepted from the sides, while fluids may be accepted from the top.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/crafting.html

Crafting Process
This section will explain how the crafting process is being handled by
Crafting Interfaces
. This information is essential to get the most out of this mod, and to debug any issues you may encounter.
When a crafting job is started (for example with the
Crafting Writer
), one (or more)
Crafting Interface(s)
will be used that have a recipe corresponding to the given job. All ingredients that are necessary for the recipes will be pulled from the network and held within the crafting jobs in the
Crafting Interface(s)
, to avoid ingredients being used elsewhere before the crafting jobs are completed. The
Crafting Interface
is responsible for inserting all inputs of the recipe into the bound machine. It will do this based on the storage network contents, as provided by
Integrated Tunnels interfaces
.
A
Crafting Interface
will however
not
import the crafting results from the bound machine. The player is responsible for making sure that the crafting results end up back into the network. The Crafting Table is an exception to this, only a
Crafting Interface
is required to craft with this. The
Crafting Interface
will only mark the crafting job as completed once the expected outputs are
inserted into
the storage network. This can be achieved by either placing an active
Integrated Tunnels importer
onto the machine, or by letting the machine automatically output into the
Crafting Interface
. Outputs that reach an attached storage by any other route, such as a pipe or hopper from another mod writing straight into a chest or drawer, will
not
be detected, and the crafting job will keep waiting even though the outputs are visible in a terminal. Everything (items, fluids, energy) that is inserted into the
Crafting Interface
will be inserted into the storage network for the configured channel.
The following figures show examples of valid minimal setups for autocrafting with a Crafting Table and Furnace.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/debugging_jobs.html

Debugging Crafting Jobs
While setting up autocrafting, you may run into situations where your crafting job does not work properly due to misconfiguration.
You can debug these cases using crafting-related aspects from the
Network Reader
, such as getting the list of all available recipes in the network, getting all running crafting jobs, and getting all missing crafting job ingredients. Visualizing these lists in
Display Panels
can help you pinpoint the problem you are experiencing.
Note that if you have
Integrated Terminals
installed, you can use the
Crafting Job Terminal
to visualize all running crafting jobs, visualize any missing ingredients, or cancel the jobs.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/distribution.html

Distributed Crafting Jobs
If you are working with slow machines, or if you want to improve the efficiency of certain crafting jobs, you can
distribute
a crafting job over multiple
Crafting Interfaces
. For example, this feature allows you to distribute the crafting of 40 iron ingots evenly over 10 furnaces.
The only thing that is needed to make this work, is to make sure that all machines that you want to distribute over, have a
Crafting Interface
that (at least) exposes exactly the same recipe that you want to distribute. In the case of our previous example, you would have to make sure that all 10 furnaces have a
Crafting Interface
containing (at least) exactly the same recipe for smelting iron ingots.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/dynamic_recipes.html

Dynamic Recipes
Following the dynamic nature of
Integrated Dynamics
Variable Cards
, recipes that are exposed through
Crafting Interfaces
can also be fully dynamic. For example, you could expose a recipe that changes based on it being day or night, or the availability of a certain item in the network.
Crafting Interfaces
will automatically detect when a recipe is updated, and all new crafting jobs will immediately use the new recipe. However, all running crafting jobs will still use the old recipe.
Note that the more your recipe changes, the more lag may be produced. So use this feature with caution.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/index.html

Crafting Interface
Introduction
Basics
Attuned Recipes
Crafting Process
Configuration
Recipe Feedback
Distributed Crafting Jobs
Blocking Mode
Reusable ingredients
Dynamic Recipes
Debugging Crafting Jobs

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/introduction.html

Introduction
In this section, we will go over the basics, crafting process, configuration and special features of the
Crafting Interface
.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/recipe_feedback.html

Recipe Feedback
When inserting
Variable Cards
into the
Crafting Interface
, you will either see a
green checkmark
or a
red cross
to indicate if the given recipe is valid for the bound machine. You can hover over these symbols to get more information.
This is an early-feedback system in cases you accidentally insert the wrong recipe into an interface, for example if you insert the crafting recipe for a chest into a
Crafting Interface
that is bound to a furnace.
If you see a
red cross
, this means that the
Variable Card
either doesn't expose a valid recipe, or that the recipe cannot be crafted in the bound machine.
A
green checkmark
indicates that the
Variable Card
contains a valid recipe,
and
that the machine can (probably) handle the given recipe.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/reusable.html

Reusable ingredients
If you want to configure recipes that do not fully consume input items, but for example only consume part of the durability of that item, you may configure those recipe ingredients as
reusable
. This can be done during recipe
Variable Card
creation by shift-clicking on an input slot inside the
Logic Programmer
and enabling the
Reusable
checkbox.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_writer.html

Crafting Writer
The
Crafting Writer
allows you to automatically start crafting jobs based on certain conditions using the recipes that are exposed by
Crafting Interfaces
.
Using the following aspects, you can start crafting jobs for the given recipe, output item, output fluid or output energy. Using the aspect settings, you can configure the crafting channel, whether or not already-running crafting jobs should block any new jobs, if crafting jobs for any missing inputs should also be started, and whether or not the presence of ingredients in the storage network should block any new crafting jobs.
If you want to insert a
Variable Card
for telling that
nothing
should be crafted, just insert an empty value, such as a
Variable Card
for the
Item
value type without an actual item in it.
Crafting Table
[Crafting Table]
[Crafting Table]
[Output Variable Transformer]
[Crafting Table]
→
[Crafting Writer]
Aspect
Craft Recipe
Craft the given recipe
Input:
Recipe
Properties:
Channel
Ignore crafting jobs
Craft missing ingredients
Craft amount
Craft Item
Craft the given item
Input:
Item
Properties:
Channel
Ignore storage contents
Ignore crafting jobs
Craft missing ingredients
Storage contents craft delay
Craft Fluid
Craft the given fluid
Input:
Fluid
Properties:
Channel
Ignore storage contents
Ignore crafting jobs
Craft missing ingredients
Storage contents craft delay
Craft Energy
Craft the given energy amount
Input:
Integer
Properties:
Channel
Ignore storage contents
Ignore crafting jobs
Craft missing ingredients
Storage contents craft delay
Craft Recipe from Operator
Craft the recipe returned by the given operator without inputs. If the operator returns an empty recipe, nothing is crafted.
Input:
Operator
Properties:
Channel
Ignore crafting jobs
Craft missing ingredients
Craft amount

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/index.html

Crafting
Introduction
Crafting Interface
Crafting Writer

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/introduction.html

Introduction
Integrated Crafting
is an extension to
Integrated Dynamics
for autocrafting within
Integrated Dynamics
networks. Autocrafting can happen with any combination of
items
,
fluids
and
energy
. The two main parts that are required for this are the
Crafting Interface
and the
Crafting Writer
.
The
Crafting Interface
can be placed next to any kind of crafting machine, and can be used to provide crafting options to the network based on
Variable Cards
containing
Recipes
.
The
Crafting Writer
can then be used to automatically start crafting jobs based on the recipes that are exposed by all
Crafting Interfaces
on the network.
The following sections explain how this autocrafting process works.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratednbt/index.html

NBT
Introduction
NBT Extractor
NBT Extractor Remote
Usage

## https://integrateddynamics.rubensworks.net/book/manual/main/integratednbt/introduction.html

Introduction
Integrated NBT
is an extension to
Integrated Dynamics
that adds tools for working with
NBT
(Named Binary Tag) data. It allows you to visually navigate, inspect, and extract deeply nested values from any
NBT
-typed variable on your
Integrated Dynamics
network.
Whether you need to pull a single integer out of a complex item tag, extract a string buried inside a nested compound, or encode a reusable extraction path for use in operators —
Integrated NBT
makes it straightforward, with no extra wiring required.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratednbt/nbt_extractor.html

NBT Extractor
The
NBT Extractor
is the centerpiece of this add-on. It is a block that presents a graphical tree browser for any
NBT
-typed variable. Place the block, insert an
NBT
Variable Card
into its left slot, and the block will load the full NBT tree of that variable so you can explore it. Once you have located the value you need, a single left click selects its path and lets you write it to an output card.
Crafting Table
[Crafting Table]
[Redstone Dust]
[Logic Programmer]
[Portable Logic Programmer]
[Redstone Dust]
[Crystallized Menril Chunk]
[Block of Crystallized Menril]
[Crystallized Menril Chunk]
→
[NBT Extractor]

## https://integrateddynamics.rubensworks.net/book/manual/main/integratednbt/nbt_extractor_remote.html

NBT Extractor Remote
The
NBT Extractor Remote
is an item-form companion to the
NBT Extractor
block. Right click any placed
NBT Extractor
to bind the remote to it. From that point on you can open the same NBT browsing interface from anywhere in the same dimension without having to stand next to the block. The remote requires line-of-sight loading: the target chunk must be loaded.
Crafting Table
[Crafting Table]
[Redstone Dust]
[Ender Pearl]
[Redstone Dust]
[Crystallized Menril Chunk]
[Logic Programmer]
[Portable Logic Programmer]
[Crystallized Menril Chunk]
→
[NBT Extractor Remote]

## https://integrateddynamics.rubensworks.net/book/manual/main/integratednbt/usage.html

Usage
To get started, place an
NBT
-typed
Variable Card
into the
left slot
of the
NBT Extractor
. The block will read the variable and display its contents as a collapsible tree.
Right click
any compound tag or list entry in the tree to expand or collapse it. Keep drilling down until you reach the value you want to extract.
Left click
the target entry to select its path. Once a path is selected, place a compatible
Variable Card
into the
right slot
. The extractor will write the extracted value to that card according to the active
Output Mode
.
The
Output Mode
controls how the output value is created. In
Reference Mode
, the output variable holds a live reference to the extracted value and updates whenever the source NBT variable changes. In
Operator Mode
, the extraction path is encoded as a reusable
Operator
that accepts any NBT input and returns the sub-value. In
Value Mode
, the current value is written to the card as a fixed constant. In
NBT Path Mode
, the path itself is stored as a
CyclopsCore NBT Path
string.
For most use cases,
Reference Mode
is the right choice. If you want a snapshot that does not change, or if you intend to apply the same extraction to many different NBT values, consider
Value Mode
or
Operator Mode
respectively.
The
Auto Refresh
toggle in the GUI controls whether the displayed NBT tree updates automatically when the source variable is updated. Turn it off to freeze the view at its current state, which is useful when inspecting rapidly changing data.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedrest/http.html

HTTP Proxy
The HTTP Proxy has an internal value state that can be updated via HTTP POST requests.
A certain value type must be selected in the HTTP Proxy, and only requests of that type will be accepted.
More information can be found on the GitHub wiki
Crafting Table
[Crafting Table]
[Logic Director]
[Crystallized Menril Chunk]
[Proxy]
[Crystallized Menril Chunk]
→
[HTTP Proxy]

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedrest/index.html

REST
Introduction
HTTP Proxy

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedrest/introduction.html

Introduction
Integrated REST
is a mod that allows you to read from and write to your
Integrated Dynamics
networks via a REST API.
This makes it possible to read the status of your networks and worlds via Web-protocols, and interact with them by POST-ing values to HTTP Proxy blocks in your world.
Check the server console to see where your server is running and see what kind of information you can find. (Default is
http://localhost:3000/
)

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/advanced/external_editing.html

External Editing
If you have access to the file system on which your singleplayer or multiplayer game is running, you can access script files through any external editor. This allows you to create scripts using your favorite IDE (such as Vi, but definitely not Emacs).
Script files are accessible via the
world/integratedscripting/
directory (see
saves/
for singleplayer worlds). In here, you will find directories for all
Scripting Disks
by their id. Inside of each
Scripting Disk
directory, you will find all script files stored in that disk.
You can safely modify these script files through an external editor, even if the game is running. After externally saving the file, they will be updated automatically in the game, and dependent variables will be updated as well.
If your external editor supports this (modern IDEs do), modifying a script file through the
Scripting Terminal
will also cause the files to be updated in your external editor.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/advanced/index.html

Advanced
Transient Storage
External Editing
Logging
TypeScript

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/advanced/logging.html

Logging
For complex scripts, the need may arise to debug them by temporarily writing to external log files.
JavaScript allows you to call functions such as
console.log()
and
console.error()
to write messages to respectively the
Standard Out
and
Standard Error
output streams.
When calling any of these functions, log messages will respectively be written to script-dependent
.stdout
and
.stderr
files.
For example, if your script is located in
myfile.js
, log messages will be written to
myfile.js.stdout
.
These
.stdout
and
.stderr
files are accessible via your
Scripting Terminal
or through external editing.
To avoid log files to become too large, they are capped in their size. By default, they are limited to 2096 lines. Consult your server admin if you are in need of more lines.
function myComplexFunc(value) {
console.log('Hello world!');
console.log(`I received value ${value}`);
return value + 42;
}

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/advanced/transient_storage.html

Transient Storage
Since Integrated Dynamics is based on functional programming principles, keeping track of states is mainly non-existent, except through external approaches such as the
Delayer
.
As the JavaScript language offers a mix of both functional and imperative programming, keeping track of states becomes partially possible.
Concretely, if you declare a mutable variable outside of functions (for example using
let
), you can modify its values through function invocations.
This way, you can keep track of states, such as counting the number of function calls, or calculating the running average of specific values.
An important catch to these variables is that their state is only transient. Concretely, this means that their values can be reset any time a variable card in the network is re-inserted, or when the server is restarted.
As such, you can not assume these values to always remain present. If you need persistent data storage, it is better to make use of the
Delayer
.
let state = 0;
function getAndIncr(value) {
return state += value;
}
let sum = 0;
let amount = 0;
function getRunningAvg(value) {
amount++;
sum += value;
return sum / amount;
}

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/advanced/typescript.html

TypeScript
The TypeScript language is a well-known superset of the JavaScript language that adds typing support. This allows developers to write type-safe JavaScript, by defining the types of variables and functions.
While Integrated Scripting only supports pure JavaScript, a TypeScript
typings file
is available for all available global functions, value types, and their methods.
When editing your scripts through an external IDE, this typings file could be loaded to create type-safe scripts. This is only recommended if you know TypeScript and know how to use it.
The typings file is available at
world/integratedscripting/integratedscripting.d.ts
(see
saves/
for singleplayer worlds). This file is automatically re-generated upon every world restart, so you should not manually adjust this file.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/concepts.html

Concepts
Editing, storing, and serving scripts to the network requires 3 elements: the
Scripting Terminal
,
Scripting Disk
, and
Scripting Drive
.
The
Scripting Disk
is used to store raw script contents, but can not do anything on its own.
The
Scripting Drive
allows you to insert a single
Scripting Disk
, which will expose its contents to the network.
The
Scripting Terminal
allows you to view, manage, and edit all scripts in the current network that are exposed by
Scripting Drives
.
Each of these elements will be discussed in more detail hereafter.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/disk.html

Scripting Disk
Scripting Disks
are responsible for storing scripts, and are to be inserted into
Scripting Drives
. They can be created by combining some
Variable Cards
with
Mendesite
.
Once a
Scripting Disk
has been inserted into a
Scripting Drive
, they will obtain a unique numerical identifier. This identifier will be used to modify scripts on this disk from within the
Scripting Terminal
. The identifier is also used to link script elements to
Variable Cards
.
A
Scripting Disk
can safely be moved to another
Scripting Drive
without loss of script data, even when on a different network.
Variable Cards
that refer to this script's identifier will however start throwing errors when not on the same network anymore.
A
Scripting Disk
has no limit in terms of its storage size, but splitting up scripts across multiple disks is recommended for management reasons.
Crafting Table
[Crafting Table]
[Variable Card]
[Variable Card]
[Mendesite]
[Variable Card]
[Variable Card]
→
[Scripting Disk]
8

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/drive.html

Scripting Drive
Each
Scripting Drive
is a block that is connected to your
Integrated Dynamics
network that accepts a single
Scripting Disk
, which allows the disk's script contents to be exposed to the network.
It is crafted by combining a
Variable Store
and
Jukebox
with some
Scripting Disks
.
Crafting Table
[Crafting Table]
[Scripting Disk]
[Scripting Disk]
[Variable Store]
[Scripting Disk]
[Jukebox]
→
[Scripting Drive]

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/index.html

Scripting
Introduction
Materials
Concepts
Scripting Disk
Scripting Drive
Scripting Terminal
Writing Scripts
Advanced

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/introduction.html

Introduction
Integrated Scripting
is an extension to
Integrated Dynamics
for creating scripts to handle complex operations in
Integrated Dynamics
networks. By default, this mod ships with support for the JavaScript scripting language.
It allows you to define
Integrated Dynamics
logic operations using a convenient scripting syntax instead of the standard visual variable card-based method, which can become cumbersome for advanced chains of operations.
For example, it allows you to easily define complex item list filters such as
"items that are damageable, are owned by mod X, and correspond to blocks that can only be mined with a diamond pickaxe."
. Or
"get the item in this list with the second-largest stack size."

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/materials.html

Materials
Before getting started with scripting, you will need some
Mendesite
blocks. These blocks are very malleable, and form a good basis for the storage and handling of scripts.
Mendesite
is created by combining
Menril Resin
and
Andesite
in a
Drying Basin
.
Drying Basin
[Drying Basin]
15s
[Andesite]
[Menril Resin]
1000
→
[Mendesite]
Mechanical Drying Basin
[Mechanical Drying Basin]
0.75s
[Andesite]
[Menril Resin]
1000
→
[Mendesite]

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/terminal.html

Scripting Terminal
The
Scripting Terminal
is a part that must be connected to your
Integrated Dynamics
network to manage the scripts stored in the
Scripting Disks
of this network.
When right-clicking on a placed terminal, a simplified
Integrated Development Environment
(IDE) will be shown using which you can edit scripts.
On the top-left side of the screen, you can select the identifier of a
Scripting Disk
that is connected to the network, after which all script files stored in this disk will be shown in the script file list below.
If the disk does not contain any script files yet, you can create them by clicking on the "+"-button on the bottom-left side of the screen.
The script file list on the left-hand side of the screen is used to select a script for editing. When clicking on a script, its editable contents will show up on the right-hand-side of the screen.
The text field on the right-hand side of the screen allows you to edit the contents of script files, which will be saved automatically after each modification.
While the editor will perform limited syntax highlighting, it is not able to detect syntax errors at this time. Syntax errors will be reported when executing scripts via
Variable Cards
.
In order to use a script member as
Integrated Dynamics
logic, you can bind it to a
Variable Card
.
You can do this by first selecting a script member in the text editor by selecting it with your cursor, such as a variable name or function name.
After that, you can insert a
Variable Card
into the slot on the right-hand side of the screen to bind it to this script member.
This
Variable Card
can now be used elsewhere in the network, by for example showing a variable member inside a
Display Panel
, or by using a function member as an operator in an item list filter.
The next chapter will provide more details on how to write scripts.
Crafting Table
[Crafting Table]
[Redstone Dust]
[Mendesite]
[Redstone Dust]
[Output Variable Transformer]
[Display Panel]
[Input Variable Transformer]
[Redstone Dust]
[Redstone Dust]
→
[Scripting Terminal]

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/writing/functions.html

Functions
As
Operators
are first-class citizens in Integrated Dynamics logic, they can also be created as JavaScript functions.
Any way of creating JavaScript functions can be used to create
Operators
, such as via the
function
keyword, or by creating custom lambdas.
Functions accept a number of arguments, which –when executed through Integrated Dynamics logic– will receive regular Integrated Dynamics values that are translated into JavaScript values.
Binding a JavaScript function to a
Variable Card
as an
Operator
, is done just like with constants and variables, by selecting the function member name in the Scripting Terminal text editor and binding it to a
Variable Card
. This value can now be used anywhere an
Operator
is accepted.
For example, you can create a function with a single
Item
argument that returns a
Boolean
to use as a filter
Operator
for filtering a list of items that you read from a Chest.
Below, you can find examples of different types of functions that could be created.
function isEven(value) {
return value % 2 === 0;
}
const isOdd = (value) => value % 2 !== 0;
function fibonacci(limit) {
const values = [];
let n1 = 0;
let n2 = 1;
let nextTerm = 0;
for (let i = 1; i <= number; i++) {
values.push(n1);
nextTerm = n1 + n2;
n1 = n2;
n2 = nextTerm;
}
return values;
}

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/writing/globals.html

Global functions
When writing JavaScript code, you can make use of the global
idContext
variable to access all Integrated Dynamics operators as functions through the
ops
field. This allows you to make use of built-in operators when creating new ones.
For example, you can write an
Item
filter that checks if an item is stackable and if it has a stack size of at least 16, as shown below.
To know what the name is of the operator you want to execute, you can look at their global name via the operator tooltips inside the
Logic Programmer
or in the list of operators in the Logic Programming section of this book.
Since invoking operators via
idContext
can become quite verbose, you can choose to store operators in a custom constant variable, as shown in the second example. Alternatively, you can make use of
object methods
, which are discussed in the next section.
function filterItem(item) {
return idContext.ops.itemstackStackable(item) && idContext.ops.itemstackStacksize(item) >= 16;
}
const isStackable = idContext.ops.itemstackIsStackable;
const size = idContext.ops.itemstackSize;
function filterItemCompact(item) {
return isStackable(item) && size(item) >= 16;
}

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/writing/index.html

Writing Scripts
JavaScript
Constants and Variables
Functions
Global functions
Object methods

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/writing/js.html

JavaScript
This mod allows scripts to be written in the JavaScript syntax. All features allowed by ECMAScript (ECMA-262) are available.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/writing/methods.html

Object methods
To counter the verbosity of global functions, you can write code more compactly using
object methods
.
Object value types such as
Blocks
,
Items
,
Fluids
, ... will have
methods
attached to them when used in JavaScript.
Object methods are just plain functions, but their first argument is tied to the object value.
For example, the global function
itemstackStackable
takes a single
Item
argument and outputs a
Boolean
. This function is available as a method on
Item
values via the name
stackable
, which takes zero arguments.
Global functions that accept two or more arguments will be available as methods on object values, with all arguments shifted by one. For example, the global function
itemstackStrength
accepts an
Item
and a
Block
argument, but is also available as method on
Items
with a single
Block
argument.
function filterItemMethods(item) {
return item.isStackable() && item.size() >= 16;
}
function filterItemMethods2(item, block) {
return item.strength(block) > 10;
}
function filterItemMethods3(item) {
return item.nbt().Damage === 0;
}

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/writing/variables.html

Constants and Variables
All Integrated Dynamics value types that you're used to (such as
Integers
,
Booleans
,
Items
, ...) are available in JavaScript, and vice versa.
The most straightforward way to create a value in JavaScript and make it available in a static
Variable Card
value, is by defining a variable member in JavaScript using
let
or
const
.
let
defines variables that are re-assignable later in the script, such as counters.
const
defines variables that can not be re-assigned.
In order to use this value in a
Variable Card
, you need to select the variable member name inside the Scripting Terminal text editor, and then insert an empty
Variable Card
in the right-hand slot. You can now use this
Variable Card
as you're used to, such as visualizing its value in a
Display Panel
.
Creating variables via JavaScript is mainly useful if you need to create complex values that are not easy to create using the
Logic Programmer
, such as long
Strings
with a specific value. Below, you can find some examples of how to create such values.
const myBoolean = true;
let myInt = 123 + 456;
let myString = "";
for (let i = 0; i < 50; i++) {
myString =+ ".";
}
const myList = [ 1, 1, 2, 3, 5 ];

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/crafting_job_terminal.html

Crafting Job Terminal
Note: this section only applies if you have an autocrafting mod such as Integrated Crafting installed.
The
Crafting Job Terminal
allows you to see and cancel all running crafting jobs in the network.
When first opening the terminal gui, you will see a list of all running jobs. You can click on each job to see a more detailed overview of its status.
In this gui, you will be able to see the following information of each job: remaining sub-jobs, status, duration, owner, channel. You can hover over each sub-job to get more information on its status.
Crafting Table
[Crafting Table]
[Glowstone Dust]
[Menril Glass]
[Glowstone Dust]
[Display Panel]
[Glowstone Dust]
[Crafting Table]
[Glowstone Dust]
→
[Crafting Job Terminal]

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/index.html

Terminals
Introduction
Storage Terminal
Crafting Job Terminal

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/introduction.html

Introduction
Integrated Terminals
is an extension to
Integrated Dynamics
to manage and overview
Integrated Dynamics
networks.
For instance, the
Storage Terminal
allows you to interact with your
storage networks
, such as your attached
Energy Batteries
. When other mods are installed that add storage capabilities, such as the
item
and
fluid
capabilities from
Integrated Tunnels
, then you will be able to interact with these as well.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/autocrafting.html

Autocrafting
Note: this section only applies if you have an autocrafting mod such as Integrated Crafting installed.
If your network contains crafting recipes, then you will be able to start crafting jobs right from within your
Storage Terminal
.
All available recipes will show up in their respective tabs and channels. Clicking on a recipe will open a gui to select the amount of times the recipe should be executed. After that, a crafting plan will be shown that lists all required or missing ingredients. After that, the job will be started. All running crafting jobs will be visible in the
Crafting Job Terminal
.
Power users can shift-click on recipes in the storage terminal to skip the crafting amount gui, and go immediately to the crafting plan overview.
Ingredients that are being produced by a running crafting job are marked with a spinner in the
Storage Terminal
. The color of this spinner indicates the status of the crafting job, which is also shown when hovering over the ingredient, together with the quantity that is still being crafted.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/crafting.html

Crafting
In order to craft the
Storage Terminal
, you will first need a special type of self-illuminating
glass
, i.e.,
Menril Glass
, which you can craft in a
Drying Basin
.
In a similar way, you can craft
Chorus Glass
, which enables entities to pass through it.
Drying Basin
[Drying Basin]
10s
[Glass]
[Menril Resin]
1000
→
[Menril Glass]
Drying Basin
[Drying Basin]
10s
[Glass]
[Liquid Chorus]
1000
→
[Chorus Glass]
Crafting Table
[Crafting Table]
[Glowstone Dust]
[Menril Glass]
[Glowstone Dust]
[Output Variable Transformer]
[Display Panel]
[Input Variable Transformer]
[Glowstone Dust]
[Glowstone Dust]
→
[Storage Terminal]

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/ender_storage.html

Ender Storage
Storage Terminals
can be
ender-upgraded
by right-clicking them with an
Eye of Ender
. Optionally, upgraded terminals can emit
ender particles
so that you can tell them apart at a glance. This can be enabled in the server-side config file, where it is disabled by default.
An ender-upgraded terminal gains an additional
Ender Storage
tab, which shows the contents of your personal
Ender Chest
. Just like with a regular
Ender Chest
, you can insert and extract items there, including by shift-clicking.
Because the
Ender Chest
is bound to you instead of to the network, this tab has no channel selector, search field or
Variable
filter slots.
Breaking an ender-upgraded
Storage Terminal
will give you back the
Eye of Ender
that was used to upgrade it.
Portable Storage Terminals
can be ender-upgraded as well, by combining them with an
Eye of Ender
in a crafting grid. This retains the network the terminal was linked to.
Crafting Table
[Crafting Table]
[Portable Storage Terminal]
[Eye of Ender]
→
[Portable Storage Terminal]

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/index.html

Storage Terminal
Storage Terminal Introduction
Crafting
Usage
Autocrafting
Power Users
Ender Storage
Portable Terminal

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/introduction.html

Storage Terminal Introduction
The
Storage Terminal
is a part that can be placed on a
Cable
to enable gui-based storage management in the attached network.
The following sections will explain how it can be crafted, used, and power-used.

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/portable.html

Portable Terminal
By attaching some
Crystalized Menril Chunks
to your
Storage Terminal
, you can create a portable variant of the terminal.
In order to link your
Portable Storage Terminal
to a certain network, your network will need an
Omni-Directional Connector
. Linking to it can be done by just right-clicking with your
Portable Storage Terminal
on the
Omni-Directional Connector
.
For power users, it is possible to configure a hotkey to open the first
Portable Storage Terminal
that is available in your inventory.
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Storage Terminal]
[Crystallized Menril Chunk]
→
[Portable Storage Terminal]
Key Binding
Open first Portable Storage Terminal
C

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/power_usage.html

Power Users
Power users tend to dislike clicking too much as it takes too much time. The
Storage Terminal
offers a couple of
hotkeys
to speed up the usage of common tasks such as selecting the search field, browsing through tabs, clearing the crafting grid, and balancing crafting grid slots. These hotkeys are listed at the end of this section.
When you have the JEI mod installed, you will be able to exploit JEI's recipe shift-clicking ability to quickly fill the crafting grid. The configured source of the auto-refill mode determines where the items come from. Furthermore, you can enable synchronization between the JEI and
Storage Terminal
search fields.
If you are not satisfied with the provided text-based filtering options, you can insert your own
Predicates
via
Variable Cards
. These should accept the ingredient type as input, and a
Boolean
as ouput.
Key Binding
Focus Search Logic Programmer
Alt + F
Key Binding
Go to next terminal tab
Tab
Key Binding
Go to previous terminal tab
Shift + Tabb
Key Binding
Clear Crafting Grid into Inventory
C
Key Binding
Clear Crafting Grid into Storage
Shift + C
Key Binding
Balance Crafting Grid
B

## https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/usage.html

Usage
The
Storage Terminal
contains tabs with all your active
storage networks
in the current attached network, optionally separated by channel. For example, if you have several
Energy Batteries
attached, you will see the sum of all energy in the energy tab. When other mods are installed that add storage capabilities, such as the
item
and
fluid
capabilities from
Integrated Tunnels
, then you will be able to interact with these as well. The bottom of each tab displays the total and maximum quantities. For the remainder of this book, we will refer to
things
that can be stored (such as items, fluids and energy) as
ingredients
.
Next to viewing ingredients, you can also interact with them. Like with any inventory, this is done by clicking to pick up something, and clicking again to place something. For certain ingredient types such as energy and fluids, you will need to click on a container item when clicking in your inventory, such as batteries or tanks. Next to clicking, you can also shift-click to move in bulk.
When a storage network ingredient is selected, you can increase it by a small amount by clicking on the storage network slot again. Shift-clicking will increase it with a larger amount. In the same way, you can decrease the quantity by right-clicking. The same can be done by scrolling over a storage ingredient.
When a storage network ingredient is selected, you can move a small quantity by right-clicking, everything that is held by left-clicking, and half of what is held by middle-clicking.
Using the search box, you can filter the displayed ingredients by name. Deeper searching through results can be done by defining further parameters separated by a space.
or
-conditions can be defined with '|'.
Via the left-hand buttons, you can toggle through different sorting options, such as ordering by name and quantity.
Different prefixes are possible to define your search target. By default, you will search within the ingredient name. By prefixing the string with
'@'
, you can search by mod. With
'#'
, tooltips can be searched. Finally,
'$'
queries item tags. For instance, the search condition
'$plank @minecraft'
will show all planks added by Minecraft.
When an item storage network is available, you will also get an additional crafting tab. This is just like the item tab, but with an integrated crafting grid. It has several useful integrated functionalities, such as clearing the grid into your inventory or network, balancing the grid, and configuring the grid auto-refill behavior after crafting.
By default, the terminal will take up as much space as possible. Using the
Grid Size
button, it is possible to change this to only scale in height, width, or use a fixed number of rows and columns (small, medium, or large). If you want to display even more slots, try tweaking your
GUI Scale
from the Minecraft options. The behavior of the different grid sizes can be configured using your client-side config file.
All terminal settings (buttons, searchboxes, tabs, ...) are persisted per player and per terminal. In order to configure what settings will be loaded for newly placed terminals, you can press the anvil button on the bottom right of your terminal gui.

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

## https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/crafting.html

Chemical Crafting
Auto-crafting with chemicals works in exactly the same way as for items, fluids, and energy. You can create recipes within the
Logic Programmer
(Chemical Recipe), or read all available recipes in a machine using the
Machine Reader
. These recipes can be inserted in the
Crafting Interface
for autocrafting in places such as the
Crafting Writer
or the
Storage Terminal
.
WARNING:
The
Chemical Tanks
from
Mekanism
do not allow chemical interaction from all sides by default. So you may have to explicitly allow
Input/Output
for the connected side within the
Chemical Tank
GUI.

## https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/index.html

Mekanism
Introduction
Chemical Logic
Chemical Tunnels
Chemical Terminals
Chemical Crafting

## https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/introduction.html

Introduction
Integrated Mekanism
is an extension to
Integrated Dynamics
that adds deep integration with the
Mekanism
mod.
This mod allows you to read properties of chemical-related machines, apply operators over them, transfer chemicals over networks, access chemicals using the
Storage Terminal
, and perform autocrafting using chemicals.

## https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/logic/index.html

Chemical Logic
Chemical Value Type
Chemical Reader
New read aspects
Chemical Operators

## https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/logic/operators.html

Chemical Operators
Chemical
values can be used within a variety of operators, for example to check if something is radioactive, or reading its conductivity.
Operator
Has Chemical
is_chemicalstack
If the given item has a chemical
Item
Boolean
Global name: itemstackIsChemicalStack(
Item
) →
Boolean
Method name:
Item
.isChemicalStack() →
Boolean
Chemical
chemicalstack
The chemical from the given item
Item
Chemical
Global name: itemstackChemicalStack(
Item
) →
Chemical
Method name:
Item
.chemicalStack() →
Chemical
Chemical Capacity
chemicalstack_capacity
The chemical capacity of the given item in mB
Item
Long
Global name: itemstackChemicalCapacity(
Item
) →
Long
Method name:
Item
.chemicalCapacity() →
Long
Amount
amount
The amount of mB
Chemical
Long
Global name: chemicalstackAmount(
Chemical
) →
Long
Method name:
Chemical
.amount() →
Long
Tint
tint
The tint color of the chemical type
Chemical
Integer
Global name: chemicalstackTint(
Chemical
) →
Integer
Method name:
Chemical
.tint() →
Integer
Is Radioactive
is_radioactive
If the given chemical is radioactive
Chemical
Boolean
Global name: chemicalstackIsRadioactive(
Chemical
) →
Boolean
Method name:
Chemical
.isRadioactive() →
Boolean
Radioactivity
radioactivity
The radioactivity of the given chemical
Chemical
Double
Global name: chemicalstackRadioactivity(
Chemical
) →
Double
Method name:
Chemical
.radioactivity() →
Double
Is Coolant
is_coolant
If the given chemical is a cooled coolant or heated coolant
Chemical
Boolean
Global name: chemicalstackIsCoolant(
Chemical
) →
Boolean
Method name:
Chemical
.isCoolant() →
Boolean
Thermal Enthalpy
thermal_enthalpy
The thermal enthalpy of the given coolant chemical
Chemical
Double
Global name: chemicalstackThermalEnthalpy(
Chemical
) →
Double
Method name:
Chemical
.thermalEnthalpy() →
Double
Conductivity
conductivity
The conductivity of the given coolant chemical
Chemical
Double
Global name: chemicalstackConductivity(
Chemical
) →
Double
Method name:
Chemical
.conductivity() →
Double
Is Cooled Coolant
is_cooled_coolant
If the given chemical is a cooled coolant
Chemical
Boolean
Global name: chemicalstackIsCooledCoolant(
Chemical
) →
Boolean
Method name:
Chemical
.isCooledCoolant() →
Boolean
Cooled Coolant Of
cooled_coolant_of
Get the heated chemical of the given cooled coolant
Chemical
Chemical
Global name: chemicalstackCooledCoolantOf(
Chemical
) →
Chemical
Method name:
Chemical
.cooledCoolantOf() →
Chemical
Is Heated Coolant
is_heated_coolant
If the given chemical is a heated coolant
Chemical
Boolean
Global name: chemicalstackIsHeatedCoolant(
Chemical
) →
Boolean
Method name:
Chemical
.isHeatedCoolant() →
Boolean
Heated Coolant Of
heated_coolant_of
Get the cooled chemical of the given heated coolant
Chemical
Chemical
Global name: chemicalstackHeatedCoolantOf(
Chemical
) →
Chemical
Method name:
Chemical
.heatedCoolantOf() →
Chemical
Is Fuel
is_fuel
If the given chemical can be used as fuel
Chemical
Boolean
Global name: chemicalstackIsFuel(
Chemical
) →
Boolean
Method name:
Chemical
.isFuel() →
Boolean
Burn Ticks
burn_ticks
The number of ticks this chemical can burn as fuel
Chemical
Integer
Global name: chemicalstackBurnTicks(
Chemical
) →
Integer
Method name:
Chemical
.burnTicks() →
Integer
Energy per Tick
energy_per_tick
The amount of energy this chemical can produce when burnt as fuel
Chemical
Long
Global name: chemicalstackEnergyPerTick(
Chemical
) →
Long
Method name:
Chemical
.energyPerTick() →
Long
Raw chemical equals
=Raw=
If the raw chemicals are equal
Chemical
Chemical
Boolean
Global name: chemicalstackIsRawEqual(
Chemical
,
Chemical
) →
Boolean
Method name:
Chemical
.isRawEqual(
Chemical
) →
Boolean
Mod
mod
The name of the mod owning the given chemical
Chemical
String
Global name: chemicalstackMod(
Chemical
) →
String
Method name:
Chemical
.mod() →
String
With Amount
with_amount
Copy the given chemical with the given amount
Chemical
Long
Chemical
Global name: chemicalstackWithAmount(
Chemical
,
Long
) →
Chemical
Method name:
Chemical
.withAmount(
Long
) →
Chemical
Tag Names
chemical_tag_names
The Tag names (strings) of the given chemical
Chemical
List
Global name: chemicalstackTags(
Chemical
) →
List
Method name:
Chemical
.tags() →
List
Tag Values
chemical_tag_values
The Tag values (chemicals) of the given chemical tag name
String
List
Global name: stringChemicalsByTag(
String
) →
List
Method name:
String
.chemicalsByTag() →
List
Chemical By Name
chemical_by_name
Get the chemical corresponding to the given unique name.
String
Chemical
Global name: stringChemicalByName(
String
) →
Chemical
Method name:
String
.chemicalByName() →
Chemical
Chemicals
Ingr.chemicals
The list of chemicals
Ingredients
List
Global name: ingredientsChemicals(
Ingredients
) →
List
Method name:
Ingredients
.chemicals() →
List
With Chemical
Ingr.with_chemical
Get a copy of the given ingredients with the given chemical at the given ingredient position
Ingredients
Integer
Chemical
Ingredients
Global name: ingredientsWithChemical(
Ingredients
,
Integer
,
Chemical
) →
Ingredients
Method name:
Ingredients
.withChemical(
Integer
,
Chemical
) →
Ingredients
With Chemicals
Ingr.with_chemicals
Get a copy of the given ingredients with the given list of chemicals at the given ingredient position
Ingredients
List
Ingredients
Global name: ingredientsWithChemicals(
Ingredients
,
List
) →
Ingredients
Method name:
Ingredients
.withChemicals(
List
) →
Ingredients
Radiation
radiation
Get the radiation level of the given entity (in Sv/h)
Entity
Double
Global name: entityRadiation(
Entity
) →
Double
Method name:
Entity
.radiation() →
Double

## https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/logic/part_reader_chemical.html

Chemical Reader
A part that adds information about the chemical machine the part is facing.
Crafting Table
[Crafting Table]
[Basic Chemical Tank]
[Basic Chemical Tank]
[Input Variable Transformer]
[Basic Chemical Tank]
→
[Chemical Reader]
Aspect
Tank Full
If the target chemical tank is full
Output:
Boolean
Tank Empty
If the target chemical tank is empty
Output:
Boolean
Tank Not Empty
If the target chemical tank has something in it
Output:
Boolean
Is Tank
If the target has a chemical tank
Output:
Boolean
Chemical Amount
The amount of chemical in mB in the active tank
Output:
Long
Properties:
Tank ID
Total Chemical Amount
The total amount of chemical in mB
Output:
Long
Chemical Capacity
Total capacity of the active tank
Output:
Long
Properties:
Tank ID
Total Chemical Capacity
Total capacity of the tank
Output:
Long
Tanks
The amount of tanks found at the target
Output:
Integer
Chemical Fill Ratio
The amount of chemical in the active tank divided by its capacity
Output:
Double
Properties:
Tank ID
Tank Chemicals
The list of chemicals in all tanks
Output:
List
Tank Capacities
The list of capacities of all tanks
Output:
List
Tank Chemical
The chemical in the target tank
Output:
Chemical
Properties:
Tank ID

## https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/logic/part_readers.html

New read aspects
Parts such as the
World Reader
and
Machine Reader
have some additional aspects to read information about things such as radiation and fission reactor statistics.
Aspect
Radiation
Get the radiation level at the target (in Sv/h)
Output:
Double
Aspect
Fission Reactor Damage
Damage factor of the given Fission Reactor
Output:
Double
Aspect
Fission Reactor Actual Burn Rate
The actual burn rate of the given Fission Reactor
Output:
Double
Aspect
Fission Reactor Maximum Burn Rate
The maximum possible burn rate of the given Fission Reactor
Output:
Double
Aspect
Fission Reactor Set Burn Rate
The configured burn rate of the given Fission Reactor
Output:
Double
Aspect
Fission Reactor Heating Rate
The amount of coolant being heated (mB/t) of the given Fission Reactor
Output:
Long
Aspect
Fission Reactor Environmental Loss
The environmental loss of the given Fission Reactor
Output:
Double

## https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/logic/value_type_chemical.html

Chemical Value Type
This mod adds a dedicated value type to represent references to a
Mekanism
chemical. For example
Sodium
,
Oxygen
,
Dirty Gold Slurry
,
Biomass
, ....
Chemicals are also supported in
Recipes
and
Ingredients
. For chemical-based recipes, a more convenient entry in the
Logic Programmer
is available under the name Chemical Recipe. This has support for mods such as JEI.
Chemical values are also fully supported within mods such as
Integrated Scripting
and
Integrated REST
.

## https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/terminals.html

Chemical Terminals
When at least one valid
Chemical Interface
is attached to your network, the
Storage Terminal
will allow you to interact with all chemicals in your network, in the same way you are used to interacting with items, fluids, and energy.

## https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/tunnels.html

Chemical Tunnels
Just like you're used to transferring items, fluids, and energy using
Integrated Tunnels
, you can transfer chemicals in the same way.
WARNING:
The
Chemical Tanks
from
Mekanism
do not allow chemical interaction from all sides by default. So you may have to explicitly allow
Input/Output
for the connected side within the
Chemical Tank
GUI.
The
Chemical Interface
exposes chemical tanks to the network, and the
Chemical Importer
and
Chemical Exporter
respectively import from and export to them. For more advanced scenarios, you can use the
World Chemical Importer
,
World Chemical Exporter
, and
Filtering Chemical Interface
.
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Basic Chemical Tank]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
→
[Chemical Interface]
4
Crafting Table
[Crafting Table]
[Chemical Interface]
[Output Variable Transformer]
→
[Chemical Exporter]
Crafting Table
[Crafting Table]
[Chemical Interface]
[Input Variable Transformer]
→
[Chemical Importer]
Crafting Table
[Crafting Table]
[Chemical Exporter]
[Logic Director]
→
[World Chemical Exporter]
Crafting Table
[Crafting Table]
[Chemical Importer]
[Logic Director]
→
[World Chemical Importer]
Crafting Table
[Crafting Table]
[Chemical Interface]
[Chemical Interface]
→
[Filtering Chemical Interface]
Aspect
Export All Chemicals
Export chemicals if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Chemical Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Chemical Amount
Export Chemical Amount
Export the given amount of chemicals each tick.
Input:
Long
Properties:
Channel
Passive Interaction
Passive Interaction Ignores Filter
Check Chemical Amount
Export Chemical
Export the given chemical.
Input:
Chemical
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Chemical Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Chemical Amount
Craft
Export Chemicals
Export the given chemicals.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Chemical Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Chemical Amount
Export Chemicals Predicate
Export all chemicals that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Chemical Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Chemical Amount
Aspect
Import All Chemicals
Import chemicals if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Chemical Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Chemical Amount
Import Chemical Amount
Import the given amount of chemicals each tick.
Input:
Long
Properties:
Channel
Passive Interaction
Passive Interaction Ignores Filter
Check Chemical Amount
Import Chemical
Import the given chemical.
Input:
Chemical
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Chemical Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Chemical Amount
Import Chemicals
Import the given chemicals.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Chemical Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Chemical Amount
Import Chemicals Predicate
Import all chemicals that match the given predicate.
Input:
Operator
Properties:
Channel
Round-Robin
Chemical Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Chemical Amount
Aspect
Export All Chemicals To Entities
Export chemicals to the facing entities if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Chemical Transfer Rate
Check Chemical Amount
Entity Index
Export Chemical Amount To Entities
Export the given amount of chemical to the facing entities each tick.
Input:
Long
Properties:
Channel
Round-Robin
Export Chemical To Entities
Export the given chemical to the facing entities.
Input:
Chemical
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Chemical Transfer Rate
Check Chemical Amount
Craft
Entity Index
Export Chemical To Entities
Export the given chemical to the facing entities.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Chemical Transfer Rate
Check Chemical Amount
Entity Index
Export Chemicals Predicate To Entities
Export all chemicals that match the given predicate to the facing entities.
Input:
Operator
Properties:
Channel
Round-Robin
Chemical Transfer Rate
Check Chemical Amount
Entity Index
Aspect
Import All Chemicals From Entities
Import chemicals from the facing entities if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Chemical Transfer Rate
Check Chemical Amount
Entity Index
Import Chemical Amount From Entities
Import the given amount of chemical from the facing entities each tick.
Input:
Long
Properties:
Channel
Round-Robin
Import Chemical From Entities
Import the given chemical from the facing entities.
Input:
Chemical
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Chemical Transfer Rate
Check Chemical Amount
Entity Index
Import Chemical From Entities
Import the given chemical from the facing entities.
Input:
List
Properties:
Channel
Round-Robin
Blacklist
Chemical Transfer Rate
Check Chemical Amount
Entity Index
Import Chemicals Predicate From Entities
Import all chemicals that match the given predicate from the facing entities.
Input:
Operator
Properties:
Channel
Round-Robin
Chemical Transfer Rate
Check Chemical Amount
Entity Index
Aspect
Filter All Chemicals
Pass chemicals if the boolean is true
Input:
Boolean
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Filter Chemical
Only pass the given chemical.
Input:
Chemical
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Blacklist
Chemical Transfer Rate
Check Chemical Amount
Filter Chemicals
Pass the given chemicals.
Input:
List
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied
Blacklist
Chemical Transfer Rate
Check Chemical Amount
Filter Chemicals Predicate
Pass all chemicals that match the given predicate.
Input:
Operator
Properties:
Apply to Insertions
Apply to Extractions
Allow if not applied

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

## https://integrateddynamics.rubensworks.net/book/manual/prerequisites/drying_basin.html

Drying Basin
Drying out blocks and items can be done using the
Drying Basin
. This can for instance be useful when you want to let your
Menril Resin
dry into
Crystallized Menril
.
Whatever you do, don’t insert hot fluids!
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
Crafting Table
[Crafting Table]
[Block of Crystallized Menril]
[Block of Crystallized Menril]
[Block of Crystallized Menril]
[Block of Crystallized Menril]
→
[Crystallized Menril Brick]
4
Crafting Table
[Crafting Table]
[Block of Crystallized Menril]
→
[Crystallized Menril Chunk]
9
Crafting Table
[Crafting Table]
[Block of Crystallized Chorus]
[Block of Crystallized Chorus]
[Block of Crystallized Chorus]
[Block of Crystallized Chorus]
→
[Crystallized Chorus Brick]
4
Crafting Table
[Crafting Table]
[Block of Crystallized Chorus]
→
[Crystallized Chorus Chunk]
9
Drying Basin
[Drying Basin]
15s
[Liquid Chorus]
1000
→
[Block of Crystallized Chorus]
[Drying Basin]
7.5s
[Menril Resin]
1000
→
[Block of Crystallized Menril]
Drying Basin
[Drying Basin]
5s
[White Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
5s
[Pink Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
5s
[Dirt]
→
[Coarse Dirt]
[Drying Basin]
5s
[Brown Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
5s
[Blue Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
5s
[Purple Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
5s
[Lime Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
5s
[Light Gray Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
5s
[Gray Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
10s
[Wet Sponge]
→
[Sponge]
[Drying Basin]
5s
[Orange Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
20s
[Clay]
→
[Terracotta]
[Drying Basin]
5s
[Light Blue Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
5s
[Magenta Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
5s
[Cyan Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
5s
[Mud]
→
[Dirt]
[Drying Basin]
5s
[Black Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
5s
[Green Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
7.5s
[Oak Sapling]
[Spruce Sapling]
[Birch Sapling]
[Jungle Sapling]
[Acacia Sapling]
[Dark Oak Sapling]
[Azalea]
[Flowering Azalea]
[Mangrove Propagule]
[Cherry Sapling]
[Menril Sapling]
→
[Dead Bush]
[Drying Basin]
5s
[Red Terracotta]
[Water]
250
→
[Terracotta]
[Drying Basin]
5s
[Rotten Flesh]
→
[Leather]
[Drying Basin]
5s
[Kelp]
→
[Dried Kelp]
[Drying Basin]
5s
[Yellow Terracotta]
[Water]
250
→
[Terracotta]

## https://integrateddynamics.rubensworks.net/book/manual/prerequisites/index.html

Prerequisites
Menril Trees
Meneglin Biome
Squeezer
Drying Basin

## https://integrateddynamics.rubensworks.net/book/manual/prerequisites/meneglin_biome.html

Meneglin Biome
Menril Trees
are unfortunately quite hard to find. Luckily, there is one biome, the
Meneglin Biome
in which these trees thrive. The large amount of
Menril Trees
causes some things in this biome to become blue, which is because of the
Menril Resin
that is given off by these trees to the environment.

## https://integrateddynamics.rubensworks.net/book/manual/prerequisites/menril_trees.html

Menril Trees
You may occasionally encounter tall blue trees. Such trees are called
Menril Trees
, and have some very interesting properties. First, the leaves give off light. This is caused by the illuminating berries, which are also very tasty! Secondly, the nice-looking wood of the tree contains a lot of resin, which can be used for crafting most of the things in this mod.
Crafting Table
[Crafting Table]
[Menril Log]
[Enriched Menril Log]
[Stripped Menril Log]
[Menril Wood]
[Stripped Menril Wood]
→
[Menril Planks]
4

## https://integrateddynamics.rubensworks.net/book/manual/prerequisites/squeezer.html

Squeezer
The
Squeezer
is an early-game machine that allows you to jump on blocks and items to break or squeeze them. Not only players, but any kind of entity can jump or fall on it to perform the squeezing process. Resulting items will either fall into any adjacent inventory or on the ground. Resulting fluids will remain in the machine and will flow to one of the two sides when there is a target tank. The machine contains a small suspension that can be enabled with a redstone pulse to reset it. Most notably, this machine can be used to squeeze the resin out of
Menril Logs
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
[Menril Berries]
[Menril Berries]
[Ender Pearl]
[Ender Pearl]
→
[Proto Chorus]
Squeezer
[Squeezer]
[Menril Planks]
→
[Menril Resin]
250
[Squeezer]
[Popped Chorus Fruit]
→
[Liquid Chorus]
125
[Squeezer]
[Menril Log]
→
[Menril Resin]
1000
[Squeezer]
[Proto Chorus]
→
[Liquid Chorus]
125
Squeezer
[Squeezer]
[Blaze Rod]
→
[Blaze Powder]
3
[Blaze Powder]
[Squeezer]
[Facade]
→
[Facade]
[Squeezer]
[Oxeye Daisy]
→
[Light Gray Dye]
4
[Squeezer]
[Azure Bluet]
→
[Light Gray Dye]
4
[Squeezer]
[Rose Bush]
→
[Red Dye]
8
[Squeezer]
[Red Tulip]
→
[Red Dye]
4
[Squeezer]
[Poppy]
→
[Red Dye]
4
[Squeezer]
[Wet Sponge]
→
[Sponge]
[Water]
50
[Squeezer]
[Wither Rose]
→
[Black Dye]
4
[Squeezer]
[Bricks]
→
[Brick]
4
[Squeezer]
[Snow]
→
[Water]
1000
[Squeezer]
[Cactus]
→
[Green Dye]
4
[Green Dye]
0.5
2
[Water]
500
[Squeezer]
[Lily of the Valley]
→
[White Dye]
4
[Squeezer]
[White Tulip]
→
[White Dye]
4
[Squeezer]
[Sugar Cane]
→
[Sugar]
4
[Sugar]
0.5
2
[Squeezer]
[Glass]
[White Stained Glass]
[Orange Stained Glass]
[Magenta Stained Glass]
[Light Blue Stained Glass]
[Yellow Stained Glass]
[Lime Stained Glass]
[Pink Stained Glass]
[Gray Stained Glass]
[Light Gray Stained Glass]
[Cyan Stained Glass]
[Purple Stained Glass]
[Blue Stained Glass]
[Brown Stained Glass]
[Green Stained Glass]
[Red Stained Glass]
[Black Stained Glass]
[Tinted Glass]
→
[Sand]
[Squeezer]
[Mud]
→
[Dirt]
[Water]
250
[Squeezer]
[Allium]
→
[Magenta Dye]
4
[Squeezer]
[Lilac]
→
[Magenta Dye]
8
[Squeezer]
[Honey Bottle]
→
[Sugar]
4
[Sugar]
0.75
[Squeezer]
[Orange Tulip]
→
[Orange Dye]
4
[Squeezer]
[Snowball]
→
[Water]
250
[Squeezer]
[Cornflower]
→
[Blue Dye]
4
[Squeezer]
[Bone]
→
[White Dye]
6
[White Dye]
0.5
2
[White Dye]
0.5
2
[Squeezer]
[Sunflower]
→
[Yellow Dye]
8
[Squeezer]
[Dandelion]
→
[Yellow Dye]
4
[Squeezer]
[Breeze Rod]
→
[Wind Charge]
5
[Wind Charge]
[Squeezer]
[White Wool]
[Orange Wool]
[Magenta Wool]
[Light Blue Wool]
[Yellow Wool]
[Lime Wool]
[Pink Wool]
[Gray Wool]
[Light Gray Wool]
[Cyan Wool]
[Purple Wool]
[Blue Wool]
[Brown Wool]
[Green Wool]
[Red Wool]
[Black Wool]
→
[String]
2
[String]
0.5
[String]
0.5
[Squeezer]
[Blaze Powder]
→
[Lava]
250
[Squeezer]
[Ice]
→
[Water]
1000
[Squeezer]
[Pink Tulip]
→
[Pink Dye]
4
[Squeezer]
[Peony]
→
[Pink Dye]
8
[Squeezer]
[Blue Orchid]
→
[Light Blue Dye]
4
Squeezer
[Squeezer]
[Osmium Ore]
[Deepslate Osmium Ore]
→
[Raw Osmium]
2
[Raw Osmium]
0.75
[Squeezer]
[Glowstone]
→
[Glowstone Dust]
4
[Squeezer]
[Tin Ore]
[Deepslate Tin Ore]
→
[Raw Tin]
2
[Raw Tin]
0.75
[Squeezer]
[Copper Ore]
[Deepslate Copper Ore]
→
[Raw Copper]
4
[Raw Copper]
0.5
2
[Raw Copper]
0.5
2
[Squeezer]
[Coal]
→
[Coal Dust]
[Squeezer]
[Coal Ore]
[Deepslate Coal Ore]
→
[Coal]
4
[Coal]
0.5
[Coal]
0.5
[Squeezer]
[Gold Ore]
[Nether Gold Ore]
[Deepslate Gold Ore]
→
[Raw Gold]
2
[Raw Gold]
0.75
[Squeezer]
[Nether Quartz Ore]
→
[Nether Quartz]
2
[Nether Quartz]
0.5
[Squeezer]
[Gravel]
→
[Flint]
[Flint]
0.5
[Squeezer]
[Lead Ore]
[Deepslate Lead Ore]
→
[Raw Lead]
2
[Raw Lead]
0.75
[Squeezer]
[Cobblestone]
→
[Gravel]
[Squeezer]
[Lapis Lazuli Ore]
[Deepslate Lapis Lazuli Ore]
→
[Lapis Lazuli]
[Lapis Lazuli]
0.5
2
[Lapis Lazuli]
0.5
2
[Squeezer]
[Stone]
→
[Cobblestone]
[Squeezer]
[Uranium Ore]
[Deepslate Uranium Ore]
→
[Raw Uranium]
2
[Raw Uranium]
0.75
[Squeezer]
[Diamond Ore]
[Deepslate Diamond Ore]
→
[Diamond]
[Diamond]
0.75
[Squeezer]
[Ancient Debris]
→
[Netherite Scrap]
[Netherite Scrap]
0.75
[Squeezer]
[Iron Ore]
[Deepslate Iron Ore]
→
[Raw Iron]
2
[Raw Iron]
0.75
[Squeezer]
[Gilded Blackstone]
→
[Gold Nugget]
4
[Gold Nugget]
0.5
5
[Squeezer]
[Redstone Ore]
[Deepslate Redstone Ore]
→
[Redstone Dust]
8
[Redstone Dust]
0.5
2
[Redstone Dust]
0.5
2
[Squeezer]
[Stone Bricks]
→
[Cracked Stone Bricks]
[Squeezer]
[Blackstone]
→
[Gravel]
2
[Squeezer]
[Sandstone]
→
[Sand]
2
[Squeezer]
[Emerald Ore]
[Deepslate Emerald Ore]
→
[Emerald]
[Emerald]
0.75
[Squeezer]
[Obsidian]
→
[Obsidian Dust]
[Squeezer]
[Nether Quartz Ore]
→
[Nether Quartz]
2
[Nether Quartz]
0.5

## https://integrateddynamics.rubensworks.net/book/tag_index.html

Index
[Attuned Crafting Interface]
Attuned Crafting Interface
[Audio Reader]
Audio Reader
[Audio Writer]
Audio Writer
[Block of Crystallized Chorus]
Block of Crystallized Chorus
[Block of Crystallized Menril]
Block of Crystallized Menril
[Block Reader]
Block Reader
[Chemical Exporter]
Chemical Exporter
[Chemical Importer]
Chemical Importer
[Chemical Interface]
Chemical Interface
[Chemical Reader]
Chemical Reader
[Chorus Glass]
Chorus Glass
[Crafting Interface]
Crafting Interface
[Crafting Job Terminal]
Crafting Job Terminal
[Crafting Writer]
Crafting Writer
[Crystallized Chorus Brick]
Crystallized Chorus Brick
[Crystallized Chorus Chunk]
Crystallized Chorus Chunk
[Crystallized Menril Brick]
Crystallized Menril Brick
[Crystallized Menril Chunk]
Crystallized Menril Chunk
[Delayer]
Delayer
[Display Panel]
Display Panel
[Drying Basin]
Drying Basin
[Dynamic Light Panel]
Dynamic Light Panel
[Effect Writer]
Effect Writer
[Energy Battery]
Energy Battery
[Energy Exporter]
Energy Exporter
[Energy Importer]
Energy Importer
[Energy Interface]
Energy Interface
[Entity Reader]
Entity Reader
[Entity Writer]
Entity Writer
[Enumerated Crafting Interface]
Enumerated Crafting Interface
[Extra-Dimensional Reader]
Extra-Dimensional Reader
[Facade]
Facade
[Filtering Chemical Interface]
Filtering Chemical Interface
[Filtering Energy Interface]
Filtering Energy Interface
[Filtering Fluid Interface]
Filtering Fluid Interface
[Filtering Item Interface]
Filtering Item Interface
[Fluid Exporter]
Fluid Exporter
[Fluid Importer]
Fluid Importer
[Fluid Interface]
Fluid Interface
[Fluid Reader]
Fluid Reader
[Generator]
Generator
[HTTP Proxy]
HTTP Proxy
[Input Variable Transformer]
Input Variable Transformer
[Inventory Reader]
Inventory Reader
[Inventory Writer]
Inventory Writer
[Item Exporter]
Item Exporter
[Item Importer]
Item Importer
[Item Interface]
Item Interface
[Labeller]
Labeller
[Liquid Chorus]
Liquid Chorus
[Logic Cable]
Logic Cable
[Logic Director]
Logic Director
[Logic Programmer]
Logic Programmer
[Machine Reader]
Machine Reader
[Machine Writer]
Machine Writer
[Materializer]
Materializer
[Mechanical Drying Basin]
Mechanical Drying Basin
[Mechanical Squeezer]
Mechanical Squeezer
[Mendesite]
Mendesite
[Menril Berries]
Menril Berries
[Menril Glass]
Menril Glass
[Menril Log]
Menril Log
[Menril Planks]
Menril Planks
[Menril Resin]
Menril Resin
[Menril Sapling]
Menril Sapling
[Mono-Directional Connector]
Mono-Directional Connector
[NBT Extractor]
NBT Extractor
[NBT Extractor Remote]
NBT Extractor Remote
[Network Reader]
Network Reader
[Omni-Directional Connector]
Omni-Directional Connector
[Output Variable Transformer]
Output Variable Transformer
[Player Simulator]
Player Simulator
[Proto Chorus]
Proto Chorus
[Proxy]
Proxy
[Redstone Reader]
Redstone Reader
[Redstone Writer]
Redstone Writer
[Scripting Disk]
Scripting Disk
[Scripting Drive]
Scripting Drive
[Scripting Terminal]
Scripting Terminal
[Squeezer]
Squeezer
[Static Light Panel]
Static Light Panel
[Storage Terminal]
Storage Terminal
[Variable Card]
Variable Card
[Variable Store]
Variable Store
[World Block Exporter]
World Block Exporter
[World Block Importer]
World Block Importer
[World Chemical Exporter]
World Chemical Exporter
[World Chemical Importer]
World Chemical Importer
[World Energy Exporter]
World Energy Exporter
[World Energy Importer]
World Energy Importer
[World Fluid Exporter]
World Fluid Exporter
[World Fluid Importer]
World Fluid Importer
[World Item Exporter]
World Item Exporter
[World Item Importer]
World Item Importer
[World Reader]
World Reader
[Wrench]
Wrench

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

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedcrafting/autocrafting_setup.html

Setup Autocrafting
In this first
Integrated Crafting
tutorial, we are going to cover the basic setup of autocrafting.
The first requirement for autocrafting is the
Crafting Interface
, which is a network part that must be facing a machine, such as a Crafting Table or Furnace. First, craft such a
Crafting Interface
, and make it face a
Crafting Table
.
Next, we are going to tell the
Crafting Interface
how it can craft Oak Planks. Do this by opening up your
Logic Programmer
, and write a
Variable Card
that contains a
Recipe
with as input an Oak Log, and as output 4 Oak Planks
(do not change the default item match settings)
.
This recipe MUST be created manually, and not auto-filled using mods such as JEI.
Finally, let's insert your newly created recipe into your
Crafting Interface
to finish this first tutorial. If a
green checkmark
is shown, this means that your recipe is valid, and that the recipe is now available to the network for autocrafting.
Crafting Table
[Crafting Table]
[Iron Ingot]
[Crafting Table]
[Iron Ingot]
[Output Variable Transformer]
[Block of Crystallized Menril]
[Input Variable Transformer]
[Iron Ingot]
[Crafting Table]
[Iron Ingot]
→
[Crafting Interface]
Advancements
Crafting² Interface
Craft a Crafting Interface
Auto-Planking
Insert the manually-created recipe for Oak Planks into a Crafting Interface
Rewards
[Crafting Interface]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedcrafting/autocrafting_trigger.html

Trigger Autocrafting
In this second
Integrated Crafting
tutorial, we will start a crafting job for Oak Planks using the
Crafting Writer
.
First let's start by crafting a
Crafting Writer
. This will allow you to automatically start crafting jobs for a given recipe, item, fluid or energy. Once you have this, place the writer anywhere on the network that has your
Crafting Interface
from the previous tutorial.
Before we tell the
Crafting Writer
to craft Oak Planks, we first need to insert some Oak Logs into the network. In order to do this, attach some storage (like a Chest) to the network using an
Integrated Tunnels
Item Interface
, and make sure that the Chest contains at least one Oak Log.
Now, we tell the
Crafting Writer
to start crafting Oak Planks. Do this by creating a
Variable Card
containing Oak Planks using your
Logic Programmer
, and insert this into the
Crafting Writer
.
If you configured everything correctly, you will be able to see that one Oak Log has been converted into 4 Oak Planks. Once you remove these planks, 4 new planks will be crafted if other logs were available. You can play around with the
Crafting Writer
aspect settings to for example ignore the storage contents, and craft as many planks as possible.
The figure at the end of this section shows an example the required parts of a setup for completing this tutorial.
Crafting Table
[Crafting Table]
[Crafting Table]
[Output Variable Transformer]
[Crafting Table]
→
[Crafting Writer]
Aspect
Craft Item
Craft the given item
Input:
Item
Properties:
Channel
Ignore storage contents
Ignore crafting jobs
Craft missing ingredients
Storage contents craft delay
Advancements
Crafting² Writer
Craft a Crafting Writer
Effective Auto-Planking
Use a Crafting Writer to start a crafting job for Oak Planks
Rewards
[Crafting Writer]
5

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedcrafting/index.html

Crafting Tutorials
Introduction
Setup Autocrafting
Trigger Autocrafting

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedcrafting/introduction.html

Introduction
This section contains several tutorials that help you to learn
Integrated Crafting
, the
Integrated Dynamics
add-on for autocrafting within
Integrated Dynamics
networks.

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedmekanism/index.html

Mekanism Tutorials
Introduction
Chemical Reading
Chemical Tunneling

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedmekanism/introduction.html

Introduction
This section contains several tutorials that help you to learn
Integrated Mekanism
, the
Integrated Dynamics
add-on that adds deep integration with the
Mekanism
mod.

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedmekanism/reading.html

Chemical Reading
Let's read some information within a
Chemical Tank
! For this, we first need to craft a
Chemical Reader
, and attach it to a
Cable
so it faces a
Chemical Tank
.
Next, try reading out whether or not the tank is empty using a
Variable Card
, and display it in a
Display Panel
.
Then, try reading the Chemical contents of the tank, and display it in a
Display Panel
.
Finally, apply the
Is Radioactive
operator within the
Logic Programmer
to this Chemical value, and read out the final value by placing the resulting
Variable Card
in a
Display Panel
. (don't forget to place all intermediary variables within a
Variable Store
!)
Crafting Table
[Crafting Table]
[Basic Chemical Tank]
[Basic Chemical Tank]
[Input Variable Transformer]
[Basic Chemical Tank]
→
[Chemical Reader]
Aspect
Tank Empty
If the target chemical tank is empty
Output:
Boolean
Aspect
Tank Chemical
The chemical in the target tank
Output:
Chemical
Properties:
Tank ID
Operator
Is Radioactive
is_radioactive
If the given chemical is radioactive
Chemical
Boolean
Global name: chemicalstackIsRadioactive(
Chemical
) →
Boolean
Method name:
Chemical
.isRadioactive() →
Boolean
Advancements
Chemical Reading
Get started with Chemical automation using the Chemical Reader.
Reaction vessel dry
Read if a tank is empty using the Chemical Reader.
What does the tank have?
Read the chemical content of a tank using the Chemical Reader.
Logical Dosimeter
Check if a chemical is radioactive.
Rewards
[Chemical Reader]
2
[Variable Card]
32

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedmekanism/tunnels.html

Chemical Tunneling
Now, we're going to try moving some chemicals over our network. For this, we first need to craft a
Chemical Interface
, and attach it to a
Cable
so it faces a
Chemical Tank
.
WARNING:
The
Chemical Tanks
from
Mekanism
do not allow chemical interaction from all sides by default. So you may have to explicitly allow
Input/Output
for the connected side within the
Chemical Tank
GUI.
Next, craft a
Chemical Importer
and place it on a cable so it faces another
Chemical Tank
, and import ALL chemicals.
Finally, craft a
Chemical Exporter
and place it on a cable so it faces another
Chemical Tank
, and export one specific chemical (e.g. Sodium).
There are multiple ways to create a
Chemical
value. You can for example create one directly within the
Logic Programmer
, searching for "Chemical", and dragging and dropping a tank containing a chemical from your inventory or JEI.
Crafting Table
[Crafting Table]
[Crystallized Menril Chunk]
[Basic Chemical Tank]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
[Crystallized Menril Chunk]
→
[Chemical Interface]
4
Crafting Table
[Crafting Table]
[Chemical Interface]
[Input Variable Transformer]
→
[Chemical Importer]
Crafting Table
[Crafting Table]
[Chemical Interface]
[Output Variable Transformer]
→
[Chemical Exporter]
Aspect
Import All Chemicals
Import chemicals if the boolean is true
Input:
Boolean
Properties:
Channel
Round-Robin
Chemical Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Chemical Amount
Aspect
Export Chemical
Export the given chemical.
Input:
Chemical
Properties:
Channel
Round-Robin
Blacklist
Empty is Wildcard
Chemical Transfer Rate
Passive Interaction
Passive Interaction Ignores Filter
Check Chemical Amount
Craft
Advancements
Chemical Interfacing
Get started with Chemical transfers using the Chemical Interface.
ALL THE CHEMICALS!
Import all chemicals using the Chemical Importer.
Precise Chemical Exports
Export one specific chemical type using the Chemical Exporter.
Rewards
[Chemical Interface]
4
[Chemical Importer]
2
[Chemical Exporter]
2

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratednbt/display_entity_position.html

Sneaky Entity Tracking
With the entity NBT reference variable from the previous step, open your
NBT Extractor
again, and place a blank
Variable Card
into the right slot. Expand the
Pos
list, which stores the entity's X, Y and Z coordinates as doubles. Left click entry
#0
to select the X position and to write the extracted value.
Place a
Display Panel
on a
Cable
in the same network and insert the extracted X position
Variable Card
. The panel will display the entity's current X coordinate in real time. As long as
Auto Refresh
is enabled on the
NBT Extractor
, the value updates whenever the entity moves.
Advancements
Sneaky Entity Tracking
Place an NBT-extracted variable within a Display Panel to show an entity's X coordinate.
Rewards
[Variable Card]
4

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratednbt/entity_nbt_variable.html

Walking the Entity NBT Tree
Start by attaching an
Entity Reader
to a
Cable
, facing the space where your target entity will stand. Open the
Logic Programmer
and write the
Entity
aspect to a
Variable Card
. This card holds a live reference to the entity in that space.
Next, use the
Entity NBT
operator in the
Logic Programmer
to convert the entity value into an NBT compound. Create an
Operator
variable that applies
Entity NBT
to your
Entity
card. The resulting card holds the entity's full NBT data as a
NBT
-typed variable.
Place the
NBT
Variable Card
into the left slot of your
NBT Extractor
. The block will load and display the entity's full NBT tree. Make sure
Reference Mode
is active, place a blank
Variable Card
into the right slot, navigate the tree, and then left click a value to select its extraction path to write the extracted reference.
Aspect
Entity
The selected entity in the target space
Output:
Entity
Properties:
List Element
Operator
Entity NBT
NBT()
Get the given entity as NBT.
Entity
NBT
Global name: entityNbt(
Entity
) →
NBT
Method name:
Entity
.nbt() →
NBT
Advancements
Walking the Entity NBT Tree
Use the NBT Extractor to extract a value from an entity's NBT variable in Reference Mode.
Rewards
[NBT Extractor]
1
[Variable Card]
8

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratednbt/index.html

NBT Tutorials
Introduction
Walking the Entity NBT Tree
Sneaky Entity Tracking

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratednbt/introduction.html

Introduction
This section contains tutorials that walk you through working with
Integrated NBT
. The goal is to set up a live view of a specific value inside an entity's NBT data, then display it on screen using an
Integrated Dynamics
network.

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedscripting/basics.html

The basics of Scripting
Mendesite
blocks are the foundational crafting materials for scripting-related items. Let's create some using the
Drying Basin
!
Next, we will create some
Scripting Disks
and
Scripting Drives
using these
Mendesite
blocks.
Scripting Disks
will be able to store scripting data, and
Scripting Drives
will allow you to insert these disks into your network.
Drying Basin
[Drying Basin]
15s
[Andesite]
[Menril Resin]
1000
→
[Mendesite]
Crafting Table
[Crafting Table]
[Variable Card]
[Variable Card]
[Mendesite]
[Variable Card]
[Variable Card]
→
[Scripting Disk]
8
Crafting Table
[Crafting Table]
[Scripting Disk]
[Scripting Disk]
[Variable Store]
[Scripting Disk]
[Jukebox]
→
[Scripting Drive]
Advancements
Mendesite Galore
Craft a Mendesite block, which is a basic resource for Integrated Scripting.
Not a floppy disk!
Craft a Scripting Disk, which is used to store scripting data.
They see me drivin'
Craft a Scripting Drive, so you can load scripts into your network.
Rewards
[Mendesite]
16
[Scripting Disk]
4
[Scripting Drive]
2

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedscripting/functions.html

Scripting functions
While creating constant values through JavaScript might be fun, the real power of these scripts become apparent when creating
functions
.
Functions created through JavaScript can be bound to
Variable Cards
as
Operators
, which can then be used in Integrated Dynamics logic as usual. For example, you could use them for defining Integrated Tunnels exporter filters, or for calculating complex values such as the median stack size of items in a list.
In this tutorial, we'll create a function that accepts a single
Item
argument, and returns a
Boolean
as output. This function will act as a filter operator, which we can use when for example filtering a list of items.
Concretely, we will read the list of items using the
Inventory Reader
, and
Filter
it using our newly created function. You can bind the function to a
Variable Card
by selecting it in the editor and inserting an empty
Variable Card
, which will store it as an
Operator
.
You are free to use any implementation of the filter function for this tutorial. Hereafter, you can find a valid example script which you are free to copy.
To complete the tutorial, show the result of the filtered list in a
Display Panel
.
In this example script, you will see that you can invoke built-in methods on items such as
isStackable()
and
stacksize()
. More details on which methods are available can be found later in the full manual.
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
Aspect
Items
Get a list of the items in the inventory
Output:
List
function filterItemMethods(item) {
return item.isStackable() && item.size() >= 16;
}
Advancements
Overpowered item list filtering
Filter a list of items read from a chest using a script-based function.
Rewards
[Scripting Disk]
4
[Variable Card]
16

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedscripting/index.html

Scripting Tutorials
Introduction
The basics of Scripting
Using the Scripting Terminal
Scripting functions

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedscripting/introduction.html

Introduction
This section contains several tutorials that help you to learn
Integrated Scripting
, the
Integrated Dynamics
add-on for creating scripts to handle complex operations.

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedscripting/terminal.html

Using the Scripting Terminal
In the previous step, we created and placed a
Scripting Disk
and
Scripting Drive
. In this tutorial, we will see how we can actually
write
scripts and bind them to
Variable Cards
.
First, let us craft a
Scripting Terminal
, place it on a cable connected to the
Scripting Drive
, and right-click on the
Scripting Terminal
to open it.
You will see an editor in which you can access all script files stored in the different
Scripting Disks
that are accessible to the network. You can toggle through the different
Scripting Disks
by clicking on the arrows on the top-left of the screen.
At this stage, you will not have any script files in your network yet, so let's create a new file by clicking on the "+" button on the bottom-left of the screen. Now click on the new file to open it, and click on the text field at the right-hand side of the screen to edit the file and write
JavaScript
code.
To keep it simple for now, we will create a constant value. Let's write something like
const myVar = 123;
As a final step, let's bind this constant to a
Variable Card
by inserting an empty
Variable Card
into the slot at the right-hand side of the screen, and select the variable by double-clicking it with your cursor. Your
Variable Card
will now be bound to the constant value. You can now use this
Variable Card
anywhere in your network. Let's try showing it in a
Display Panel
!
Crafting Table
[Crafting Table]
[Redstone Dust]
[Mendesite]
[Redstone Dust]
[Output Variable Transformer]
[Display Panel]
[Input Variable Transformer]
[Redstone Dust]
[Redstone Dust]
→
[Scripting Terminal]
Advancements
The Discovery of the Scripts
Open the Scripting Terminal GUI.
The Rise of the Scripts
Create a script, and bind it to a Variable Card.
The Unleashing of the Scripts
Display a scripted value in a Display Panel
Rewards
[Scripting Terminal]
1
[Variable Card]
16

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/index.html

Terminal Tutorials
Introduction
Storage Terminal
Ender-upgraded Storage Terminal
Storage Terminal Filtering

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/introduction.html

Introduction
This section contains several tutorials that help you to learn
Integrated Terminals
, the
Integrated Dynamics
add-on for managing and overviewing
Integrated Dynamics
networks.

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/storage_terminal.html

Storage Terminal
The
Storage Terminal
allows you to show the storage contents of the attached network and interact with its contents. Different ingredient types are available in separate tabs, and each tab can be filtered by channel, search queries and
Variable Cards
. They can also be sorted in various ways. Refer to the manual for further details.
This tutorial is simple, craft
Menril Glass
, a
Storage Terminal
, and open its gui after attaching it to a network.
Drying Basin
[Drying Basin]
10s
[Glass]
[Menril Resin]
1000
→
[Menril Glass]
Crafting Table
[Crafting Table]
[Glowstone Dust]
[Menril Glass]
[Glowstone Dust]
[Output Variable Transformer]
[Display Panel]
[Input Variable Transformer]
[Glowstone Dust]
[Glowstone Dust]
→
[Storage Terminal]
Advancements
The Glass of Illumination
Dry Menril Resin onto Glass to obtain Menril Glass.
The creation of the Storage Terminal
Craft a Storage Terminal
GUI Simulator 9000
Open the gui of a Storage Terminal
Rewards
[Menril Glass]
10
[Storage Terminal]
3

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/storage_terminal_ender.html

Ender-upgraded Storage Terminal
Next to the storage of your network, a
Storage Terminal
can also give you access to your personal
Ender Chest
. For that, the terminal first needs to be
ender-upgraded
.
In order to complete this tutorial, place a
Storage Terminal
on a
Cable
, and right-click it with an
Eye of Ender
. The
Eye of Ender
will be consumed, and a new
Ender Storage
tab will show up in its gui. You will get the
Eye of Ender
back when you break the terminal again.
The same can be done with a
Portable Storage Terminal
, by combining it with an
Eye of Ender
in a crafting grid.
Crafting Table
[Crafting Table]
[Portable Storage Terminal]
[Eye of Ender]
→
[Portable Storage Terminal]
Advancements
Storage from the End
Ender-upgrade a Storage Terminal by right-clicking it with an Eye of Ender
Rewards
[Eye of Ender]
2
[Portable Storage Terminal]
1

## https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/storage_terminal_filtering.html

Storage Terminal Filtering
One of the many useful features of the
Storage Terminal
is its ability to
filter
through ingredients. In this tutorial, we will explore two different filtering methods: text-based filtering and variable-based filtering (only the second one will give you an advancement). This tutorial will use item-based storage as provided by Item Interfaces from Integrated Tunnels.
For the first part of this tutorial, we will filter through items by typing in a certain string in the search field. Note that you can use a hotkey to select this search field more quickly. And if you have JEI installed, you can synchronize its contents with the JEI search field. By default, you will search within the ingredient name. By prefixing the string with
'@'
, you can search by mod. With
'#'
, tooltips can be searched. Finally,
'$'
queries item tags. In order to complete the first part of this tutorial, type
'$plank @minecraft'
into the search field to show all planks added by Minecraft.
The second part of this tutorial uses variable-based filtering using the bottom-right
Variable
slots in the
Storage Terminal
gui. You can insert
Variables
that contain a certain
Predicate
as configured in the
Integrated Dynamics Logic Programmer
that takes the ingredient type as input, and a
Boolean
as ouput that indicates whether or not the given ingredient should be shown. For this tutorial, let's try filtering all items that are
enchantable
. To do this, open up a
Logic Programmer
, go to the
Predicate
type, search for the
"Item Enchantable"
predicate, and write it to a
Variable
. After that, insert your
Variable
into one of the
Variable
slots inside the
Storage Terminal
when the item tab is selected.
Key Binding
Focus Search Logic Programmer
Alt + F
Advancements
Dynamic Storage Terminal Filtering
Dynamically filter enchantable items in the Storage Terminal using a Variable
Rewards
[Variable Card]
64
[Storage Terminal]
3

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

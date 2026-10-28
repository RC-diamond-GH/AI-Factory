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

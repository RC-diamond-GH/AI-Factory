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

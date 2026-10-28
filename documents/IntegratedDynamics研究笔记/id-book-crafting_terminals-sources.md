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

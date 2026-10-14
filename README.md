# AI-Factory

English | [简体中文](README_cn.md)

AI-Factory brings an LLM-powered assistant into Minecraft: chat in-game, request permission-checked game actions, and optionally work with an Applied Energistics 2 (AE2) network through an ME Intelligent Terminal.

**Early beta.** The basic chat panel and AE2 integration features have been manually tested by the maintainer. Multiplayer, dedicated-server operation, and edge cases have **not** been manually tested. Back up your world before trying it; beta updates do not promise save compatibility.

## Getting started

### 1. Install the mod and dependencies

| Component | Version / requirement |
| --- | --- |
| Minecraft | **1.20.1** |
| Mod loader | **Forge**; development target: **47.4.10** |
| Java | **17** |
| Kotlin for Forge (KFF) | **Required**; use **4.12.0** for Minecraft 1.20.1 |
| Applied Energistics 2 | **Optional**; integration targets the Forge version **15.4.11** |
| GuideME | Install **20.1.7** when using the AE2 integration |
| Just Enough Items (JEI) | **Optional, client-side**; Forge 1.20.1 **15.62.0 or newer in the 15.x line**; development target **15.62.0.219** |

1. Set up a Minecraft 1.20.1 Forge instance in your launcher.
2. Download the AI-Factory release JAR from [GitHub Releases](https://github.com/RC-diamond-GH/AI-Factory/releases) and place it in that instance's `mods` directory. Choose the `.jar` attachment, not GitHub's source archives or a `-plain.jar` build.
3. Install [Kotlin for Forge](https://github.com/thedarkcolour/KotlinForForge) for Minecraft 1.20.1. KFF is **not bundled** with AI-Factory; it is needed even without AE2.
4. For the ME features, also install [AE2](https://github.com/AppliedEnergistics/Applied-Energistics-2) 15.4.11 and [GuideME](https://github.com/AppliedEnergistics/GuideME) 20.1.7, using their Minecraft 1.20.1 Forge builds.
5. For recipe queries, install the Minecraft 1.20.1 Forge build of [JEI](https://github.com/mezz/JustEnoughItems) on the client. JEI is not bundled and is not required on the server for these tools.

Without AE2, the regular chat assistant remains available; ME items and terminal features are not registered. This is a Forge mod, not a Fabric or NeoForge build.

The current networking design requires AI-Factory and KFF on **both the client and server**, with matching AI-Factory versions; AE2 integration also needs AE2 and GuideME on both sides. This describes installation requirements, **not verified multiplayer or dedicated-server support**.

### 2. Configure LLM API access

You need your own API endpoint, API key, and model. AI-Factory uses an **OpenAI-compatible Chat Completions API**; the model and provider must support streaming and tool/function calling to use the game tools. Compatibility with every provider is not guaranteed. No API subscription or credit is included.

1. Start the game, enter a world, and press **G** to open the assistant. You can change the key in Minecraft's controls.
2. On first open, the mod creates these files under your **client game instance**:

   ```text
   config/ai-factory/model.toml
   config/ai-factory/agent.toml
   ```

   The default API key is empty, so an initial configuration error is expected until you fill it in.
3. Edit `model.toml`, for example:

   ```toml
   base_url = "https://api.openai.com/v1"
   api_key = "YOUR_API_KEY"
   model = "gpt-4o-mini"
   ```

   Replace the key and model with values available from your provider. For another compatible service, replace `base_url` too. It is the API root, not the full completion URL: the mod appends `/chat/completions` and does not add `/v1` automatically.
4. Return to the chat panel and click **Reload config**, then send a message.

`agent.toml` controls the system prompt, maximum agent steps, model-request timeout, and tool timeout. You can leave its generated values unchanged to start. Existing files are not overwritten automatically. Reloading the regular G-key assistant resets its conversation; reloading an ME conversation retains its shared history. 

LLM requests run on the client using that player's configuration; there is no server-side LLM provider setup. API calls may cost money, including an extra request used to generate an automatic ME conversation title. Conversation history and relevant tool results are sent to the configured provider. **The API key is stored in plain text**: never commit or share `model.toml`. Shared ME histories are stored in the world and cached locally; do not treat them as private notes or include them in a public upload without reviewing their contents.

### 3. Try the AE2 integration (optional)

Install an **ME Intelligent Cell** in an ME Drive, then attach an **ME Intelligent Terminal** to a free face of an AE2 cable. The terminal needs network power and a channel. Right-click it to create or open a conversation; it uses the same client LLM configuration as the regular assistant.

Intelligent cells come in **1K / 4K / 16K / 64K / 256K** tiers. Each holds up to **8 conversations**, sharing that cell's capacity. These store conversation context, not ordinary item stacks. Storage is counted locally with `cl100k_base`; it is not your provider's billed token usage or a promise that the model can accept the entire stored history.

## Current capabilities

### Regular assistant — G key

- In-game chat with streamed responses and a step-limited agent loop that can request tools and use their results.
- `give_item`: grants registered items to the requesting player, subject to server permission equivalent to `/give` (permission level 2 or above).
- `execute_command`: runs a server command as the requesting player, with that player's permissions and context; it does not grant operator privileges.

Commands can affect the world and other players. Closing the regular chat cancels the current turn, but **cancellation does not undo actions already executed**. This is an assistant with specific tools, not unrestricted control of every Minecraft feature.

### ME Intelligent Terminal — optional AE2 integration

- Persistent conversations associated with intelligent cells, with a conversation list, automatic titles, history clearing, and deletion.
- World-backed context storage and a local cache. The design uses exclusive conversation locks; multi-user contention has not been manually tested.
- Background turns and a status HUD: closing the ME interface does **not** cancel a running turn. The conversation lock is released after saving is confirmed, the turn has ended, and the interface is closed.

| ME tool | Capability |
| --- | --- |
| `ae2_read_inventory` | Read network inventory and craftable entries. |
| `ae2_item_details` | Inspect an entry's identity, quantity, and bounded NBT details. |
| `ae2_crafting_plan` | Preview a crafting plan and missing resources without submitting a job. |
| `ae2_request_crafting` | Submit an actual AE2 crafting job using existing patterns; output stays in the network. |
| `ae2_manage_patterns` | Inspect pattern containers; encode and install supported crafting, processing, stonecutting, and smithing patterns. |

### Recipe queries — optional JEI integration

When JEI is installed on the client, both the regular assistant and the ME assistant receive these read-only tools. Without JEI, neither tool is registered; all existing non-JEI features remain available.

| JEI tool | Capability |
| --- | --- |
| `jei_find_ingredients` | Search visible registered items, fluids and custom ingredient types by localized name or resource ID; return a bounded page with ingredient keys. |
| `jei_query_recipes` | Query an ingredient's production recipes, input uses or catalyst uses, optionally limited to a JEI category; return displayed slots and category workstations/catalysts. |

Try asking “Use JEI to find how to make a piston” or “Which JEI recipes use iron ingots?” The model must support tools and actually call them; an ordinary answer alone is not evidence of a query.

Ingredient keys select JEI-defined subtypes, not every NBT field. They expire when JEI reloads or their bounded reference cache evicts them; search again after `unknown_key`. A direct item/fluid ID selects a default stack. A temporarily unavailable runtime returns `jei_not_ready` without changing the registered tool list.

Results are JEI **display data**, not executable recipes or proof that the player/ME network can craft them. Rotation candidates can be correlated; lookup-only invisible ingredients are absent from the slot view. Machine energy, duration, probability and execution conditions are not extracted. Missing/failed layouts, hidden candidates and truncation are reported explicitly.


## Testing status and limitations

**Manually tested by the maintainer:**

- The basic in-game conversation panel.
- The AE2 integration features.

These are feature-level checks, not exhaustive coverage of every operation or environment. They are maintainer-reported results, not a claim that this documentation update ran the game.

**Not manually tested:**

- JEI recipe queries in an actual game, including plugin discovery, third-party categories and runtime reloads.
- Multiplayer, including concurrent access to shared conversations.
- Dedicated-server startup and operation.
- The full range of edge cases: large histories, capacity limits, disconnects, timeouts, cancellation races, damaged saves, and interactions with other mods.

## Feedback and issues

Bug reports, suggestions, and experience reports are welcome at [GitHub Issues](https://github.com/RC-diamond-GH/AI-Factory/issues). Please include:

- AI-Factory, Minecraft, Forge, KFF, and (if installed) AE2 / GuideME versions.
- Whether it happened in single-player, multiplayer, or on a dedicated server.
- Reproduction steps, expected behavior, and what actually happened.
- Relevant **redacted** logs or screenshots; for API problems, the provider and model name if you can share them.

Never attach API keys, authorization headers, unreviewed configuration files, or sensitive conversation history. Remove secrets from logs and screenshots before posting.

## Development and license

Build setup, packaging pitfalls, provider contracts, and historical verification notes are in [Development notes (Chinese)](documents/开发与踩坑记录.md). Further implementation details are in the [AE2 delivery notes (Chinese)](documents/AE2智能存储拓展交付说明.md) and [client agent/tool design (Chinese)](documents/客户端ReAct与Tool架构设计.md).

AI-Factory is licensed under [Apache-2.0](LICENSE). The release JAR bundles JTokkit under its [MIT license](src/main/resources/META-INF/licenses/jtokkit-LICENSE.txt); Kotlin for Forge, AE2, GuideME and JEI are installed separately.

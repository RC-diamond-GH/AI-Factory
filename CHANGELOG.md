# Changelog

## Unreleased — Integrated Dynamics DSL and native card tools

- Reuse the shared functional-language compiler for a typed Haskell-like ID DSL: explicit inputs, monomorphic bindings, higher-order native operators, partial application, composition, lists and conditional card graphs.
- Add `id_compile` for previews and `id_make_cards` for explicit creation. Source-free mechanism RPCs carry only bindings or bounded plans; conversation history can still contain source.
- Independently validate schema, topology, input proofs, concrete signatures, finiteness, resources and SNBT before allocating IDs. Create real native facades with native handlers; consume truly blank cards and reserve main-inventory space without overwriting existing cards.
- Separate 8192 typed IR nodes from 256 physical definitions; enforce distinct-value JSON and encoded-plan budgets before recursive serialization. Share duplicate literals/aspects without double charging. Drain smaller blank-card stacks first.
- Reject statically invalid slice bounds with source-mapped `E_LITERAL_RANGE`, including direct plans and full/partial native apply chains. Report a curried error once at its chain tip. Preserve runtime-native `get`/`getOr` index behavior.
- Memoize successful existing-facade proofs within each request without bypassing depth checks; cap total expansions at 4096. Preserve infinite-list metadata through native identity/constant calls so full-list consumers reject them.
- Report phase/code/node/input, client-mapped source positions, actionable hints and truthful partial-creation results. Functions have no data equality; concrete constraint violations are type errors, not requests for more annotations. Writes are not transactionally rolled back.
- Upgrade shared-session protocol to **7** and add English/Chinese tool summaries. Keep native evaluation, deployment, logistics and Delay/Proxy wiring under player/ID control.
- Provide the [DSL design and extension API](documents/IntegratedDynamicsDSL设计.md), [user guide](documents/IntegratedDynamicsDSL使用指南.md) and updated player/developer documentation.
- Verification: the complete integration `build` passed **458** tests, including **108** language cases, **51** DSL cases, **7** native graph/finiteness cases and **4** inventory-planning cases, with zero failures/errors/skips. Offline compiler/ToolManager/error mapping, deep-plan/inventory probes, direct decoder limits and real native Integer/NBT facade serialization plus higher-order contracts were exercised. Bounded same-path SDK probes reproduced and verified the facade/finiteness fixes; partial-slice function exports and map consumers were exercised. No Minecraft client/server, actual-world card creation, game UI or paid model API was started.

## Unreleased — shared functional language framework

- Add the Minecraft/Forge-independent `lang` module: source-aware parser, monomorphic types, host-injected primitive contracts, bounded dependency checking and deterministic typed graphs.
- Provide an extensible backend contract and a strict pure evaluator with higher-order lists, partial application and language-level conditional/Boolean short-circuiting.
- Document syntax, extension APIs, semantics and limits in [共享类 Haskell 解释器](documents/共享类Haskell解释器.md).
- Review remediation bounds cumulative AST depth, cross-let checking/lowering traversal, name-diagnostic work, closure applications and list/string materialization; rejects function equality and defines JVM floating min/max behavior.
- Verification: original and adversarial CLI cases passed after the first fixes; cross-let forward/reversed stack-overflow cases were additionally reproduced and covered by regression tests. `build` passed **396** tests including **108** language cases, with zero failures/errors/skips. No game or paid model API was started.

## Unreleased — CI and development loop

- Build every pull request and main-branch merge with JDK 21 / Java 17, run default tests without paid API calls, inspect the reobfuscated release JAR, and upload build artifacts.
- Document the required plan → commit → CI → Code Review → merge → CI loop in [开发闭环](documents/开发闭环.md) and the PR checklist.

## Unreleased — independent Integrated Dynamics Agent

- Add the optional `ai_factory:integrated_dynamics_terminal` block, item, creative tab, recipe, models, texture and UUID-preserving loot.
- Join ID through its native cable-connectable block/entity and network-element provider lifecycle; do not infer membership from an adjacent cable.
- Persist terminal/session UUID references on the block and store conversation bodies separately from ME cells. Add independent ID leases, session CRUD, client history, background turns and HUD.
- Reuse the shared client lifecycle through per-addon composition roots; register only `id_network_info`, `id_list_parts` and `id_list_variables` for the ID agent.
- Inspect loaded native network metadata and definition references with strict bounded pagination; do not evaluate/read values, compile DSL, write cards or change logistics.
- Target Forge 1.20.1 / ID **1.31.3**, CyclopsCore **1.20.0**, CommonCapabilities **2.9.7**; optional ID dependency, no AE2 requirement and no bundled ID libraries.
- Shared-session protocol is now **6**; both sides must use the same build. ME storage and authorization remain separate.
- Verification: `build`, `jarJar` and `reobfJarJar` succeeded; **288** local tests passed, including **14** ID regressions, with no failures/errors/skips. Standalone session/file-reload and argument smoke passed; five common ID classes linked with AE2/GuideME removed from the classpath. Release checks confirmed Java 17, expanded metadata, complete ID resources and translation parity.
- No Minecraft client/server or paid model API was started. Actual network formation/splitting, wireless links, block drops/replacement, rendering and multiplayer remain unverified. Forge's standard `BlockEntityType.Builder.build(null)` produces one Kotlin Java-nullness warning; Gradle also reports its existing Gradle 9 deprecation notice.

## Unreleased — optional JEI recipe queries

- Add `jei_find_ingredients` and `jei_query_recipes` to both regular and ME agents only when JEI is installed on the client.
- Integrate through a public no-argument `@JeiPlugin`; isolate JEI API types from the Tool/gateway contracts and Agent composition roots.
- Support localized ingredient search, item/fluid IDs and custom-type search keys, production/input/catalyst queries, category filtering and bounded pagination.
- Invalidate runtime-scoped ingredient keys across reloads, reject newly hidden ingredients, preserve rotation indices and explicitly report incomplete/truncated display data.
- Add English/Chinese tool summaries. Recipe display data is not crafting authorization; machine energy, duration, probability and execution conditions are not extracted.
- Target JEI **15.62.0.219**; optional client dependency range **[15.62.0,16)**. JEI and MezzConfig are not bundled in AI-Factory.
- Verification: `build` and reobfuscated packaging succeeded; **274** local tests passed (including **9** JEI regressions), with no failures, errors or skips. Offline public-API fixtures exercised the production ToolManager/gateway/backend and missing-JEI class linkage; package checks confirmed Java 17, the plugin annotation, optional client metadata and translation-key parity.
- No Minecraft client/server or paid LLM API was started/called. Game loading, actual chat rendering and third-party JEI recipes remain unverified.

## 0.0.1-beta.1 — prepared for GitHub pre-release

First public beta of AI-Factory. This version has not been published yet.

### Features

- In-game chat panel with streaming LLM responses and a step-limited tool-using agent.
- Permission-checked item grants and server commands executed as the requesting player.
- Optional AE2 integration: five tiers of ME Intelligent Cells, an ME Intelligent Terminal, persistent conversations, automatic titles, and background-turn status.
- ME tools for inventory inspection, item/NBT details, crafting previews, actual crafting requests, and pattern management.

### Installation

- Minecraft **1.20.1**, **Forge** (development target **47.4.10**), and **Java 17**.
- **Kotlin for Forge 4.12.0** for Minecraft 1.20.1 is required and is not bundled.
- Optional ME integration requires Forge **AE2 15.4.11** and **GuideME 20.1.7**.
- Install the release attachment **`ai_factory-0.0.1-beta.1.jar`** in the game instance's `mods` directory. Do not install the `-plain.jar` build or GitHub's source archives.
- Configure your own OpenAI-compatible Chat Completions endpoint, API key, and model in `config/ai-factory/model.toml`. Press **G** in a world to create the configuration, then edit it and click **Reload config**.
- The networking design requires matching AI-Factory versions and dependencies on both sides; this is not a claim of tested multiplayer or dedicated-server support.

Full setup instructions: [English](https://github.com/RC-diamond-GH/AI-Factory/blob/v0.0.1-beta.1/README.md) / [简体中文](https://github.com/RC-diamond-GH/AI-Factory/blob/v0.0.1-beta.1/README_cn.md). These versioned links become available when the release tag is pushed or created on GitHub.

### Build and package checks

- `clean build` succeeded; **265 local automated tests** passed with no failures, errors, or skipped tests.
- The final JAR's Mod metadata and manifest identify **0.0.1-beta.1**; its Minecraft dependency is restricted to **1.20.1**.
- Mod classes target Java 17. The package contains its resources, Apache-2.0 license, and JTokkit 1.1.0 with its MIT license; KFF, AE2, and GuideME are not bundled.
- An accompanying `ai_factory-0.0.1-beta.1.jar.sha256` records the checksum of this build.
- These checks inspect the built artifact; they do **not** validate loading it in Minecraft, multiplayer, or a dedicated server.

### Testing status

The maintainer has manually tested the **basic chat panel and AE2 integration features**. These are feature-level checks, not exhaustive verification of every tool or environment.

**Not manually tested:** multiplayer, dedicated-server startup/operation, and the full range of edge cases, including concurrency, disconnects, capacity limits, and damaged saves.

### Beta precautions

- Back up your world and use a test save. Save compatibility across beta versions is not promised.
- Commands and ME operations may change the world or network. Cancellation does not undo completed actions.
- API calls may incur charges. Conversation history and relevant tool results are sent to your configured provider.
- API keys are stored in plain text locally. Do not share configuration files or unredacted logs/history.

Bug reports and feedback are welcome at [GitHub Issues](https://github.com/RC-diamond-GH/AI-Factory/issues). Include versions, environment, reproduction steps, expected/actual behavior, and redacted logs.

### License

AI-Factory: [Apache-2.0](https://github.com/RC-diamond-GH/AI-Factory/blob/v0.0.1-beta.1/LICENSE). The release embeds JTokkit 1.1.0 under its [MIT license](https://github.com/RC-diamond-GH/AI-Factory/blob/v0.0.1-beta.1/src/main/resources/META-INF/licenses/jtokkit-LICENSE.txt). KFF, AE2, and GuideME are installed separately.

# 👀 Clairvoyant Anti-X-ray

**Clairvoyant** is a powerful, modern anti-x-ray plugin designed for PaperMC servers. It moves beyond simple ore obfuscation and instead focuses on **intelligent data analysis** to identify suspicious player behavior. By meticulously logging every mining action and analyzing patterns, Clairvoyant provides server administrators with the clear, evidence-based insights needed to catch cheaters and maintain a fair gameplay environment.

The goal of Clairvoyant is not to automatically ban players, but to empower administrators. It calculates a "Suspicion Score" for each player, allowing you to make informed decisions based on detailed reports and interactive in-game visualizations.

-----

## ✨ Features

* **Detailed Action Logging**: Logs every block break, block place, and specific interactions to a local SQLite database for later analysis.
* **Advanced Heuristic Engine**: Analyzes player data against multiple configurable heuristics (e.g., ore-to-stone ratio, tunneling patterns, mining at suspicious Y-levels) to calculate an overall suspicion score.
* **Interactive In-Game Visualization**: The `/cv trace` command generates interactive, admin-only visual markers in the world, showing exactly where a player has mined. Right-click a marker to get detailed information about the action.
* **Comprehensive Reports**: The `/cv check` command provides a detailed report, breaking down the player's suspicion score by each heuristic, complete with color-coded status levels ([NORMAL], [SUSPICIOUS], [DANGEROUS]).
* **Highly Configurable**: Almost every aspect of the analysis engine—from heuristic weights to what ores are considered valuable—can be customized in the `config.yml` file. This includes full support for adding modded or custom items.
* **Performance-Focused**: Built with an asynchronous architecture to minimize impact on server performance (TPS).

-----

## 🛠️ Commands & Permissions

The main command is `/clairvoyant`, with aliases `/cv` and `/xray`. All commands require the permission `clairvoyant.admin` by default.

| Command | Arguments | Description |
| :--- | :--- | :--- |
| `/cv stats` | `<player>` | Shows detailed mining statistics for the specified player, including counts for each block type broken. |
| `/cv trace` | `<player>` | Generates an interactive, in-game visualization of the player's recent mining activity. Only you can see the markers. |
| `/cv check`| `<player>` | Runs a full heuristic analysis on the player and displays a detailed suspicion report with an overall score and status. |
| `/cv help` | | Displays the list of available commands. |

-----

## ⚙️ Installation

1.  Download the latest `Clairvoyant-vX.X.X.jar` from the releases page.
2.  Place the JAR file into your server's `/plugins` directory.
3.  Start or restart your server.
4.  The plugin will generate a `Clairvoyant` folder containing the `config.yml` and `clairvoyant.db` files.

-----

## 🔧 Configuration Guide (`config.yml`)

All configuration is handled in the `plugins/Clairvoyant/config.yml` file. This guide explains each section in detail.

### `weights`

This section controls how much influence each heuristic has on the final suspicion score. It's recommended to keep the sum of all weights equal to `1.0`.

* **Example**: If you think `tunneling-pattern` is a more reliable indicator of cheating than `torch-usage`, you can increase its weight and decrease the other.

<!-- end list -->

```yaml
weights:
  high-value-ore-ratio: 0.15
  tunneling-pattern: 0.25 # Increased weight
  torch-usage: 0.05
  # ... and so on
```

### `ore-lists`

This is where you define which blocks the plugin should care about. **You can add items from mods or custom plugins here** using their unique namespaced ID (e.g., `mod_id:item_id`).

* `high-value`: A list of rare and valuable ores. Mining these ores in unusual ways will significantly impact the suspicion score.
* `common`: A map of common ores. Used by some heuristics to detect if a player is abnormally focusing on one type of common ore.
* `stones`: A list of common blocks that are typically ignored during mining (like stone, dirt, deepslate). This list is crucial for calculating ore-to-stone ratios.

### `visualization-mapping`

This section customizes the visual feedback for the `/cv trace` command.

* `default-marker`: The block that will be used for any mined block not specified in the `ores` list below.
* `ores`: A map that defines which block to show for a specific mined ore. For example, when a `DIAMOND_ORE` is mined, the trace will show a `DIAMOND_BLOCK`.

### `thresholds`

This section allows you to fine-tune the sensitivity of each heuristic.

* `min-blocks-for-analysis`: The minimum number of blocks a player must break before their data can be analyzed. This prevents inaccurate reports for new players.
* `high-value-ratio`: The ore-to-stone ratio above which the heuristic will start adding to the suspicion score.
* `suspicious-y-levels`: Defines the Y-level ranges that are considered optimal for finding valuable ores. Players who spend an unusual amount of time mining in these specific zones will get a higher score.
* And many others to control the sensitivity of each specific analysis.

### `suspicion-levels`

This section sets the score boundaries for the status levels displayed in the `/cv check` report.

* `suspicious`: Any score above this value will be flagged as **[SUSPICIOUS]**.
* `dangerous`: Any score above this value will be flagged as **[DANGEROUS]**.

<!-- end list -->

```yaml
suspicion-levels:
  suspicious: 40.0
  dangerous: 70.0
```
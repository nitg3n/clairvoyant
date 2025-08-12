package com.nitg3n.clairvoyant.models

import java.util.UUID

/**
 * Defines the types of player actions that are logged.
 */
enum class ActionType {
    BLOCK_BREAK,
    BLOCK_PLACE,
    INTERACT,
    ZONE_ENTRY
}

/**
 * A data class that encapsulates all information about a single player action.
 *
 * @property playerUUID The UUID of the player.
 * @property playerName The name of the player.
 * @property actionType The type of the action.
 * @property material The material associated with the action (e.g., block type).
 * @property world The name of the world.
 * @property x The X coordinate.
 * @property y The Y coordinate.
 * @property z The Z coordinate.
 * @property timestamp The time the action occurred, defaults to the current time.
 */
data class ActionData(
    val playerUUID: UUID,
    val playerName: String,
    val actionType: ActionType,
    val material: String,
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val timestamp: Long = System.currentTimeMillis()
)
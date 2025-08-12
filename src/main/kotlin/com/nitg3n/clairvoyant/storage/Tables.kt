package com.nitg3n.clairvoyant.storage

import org.jetbrains.exposed.sql.Table

/**
 * Database table definition for storing all player actions.
 */
object PlayerActions : Table("player_actions") {
    /** A unique auto-incrementing ID for each action record. */
    val id = integer("id").autoIncrement()
    /** The UUID of the player who performed the action. */
    val playerUUID = varchar("player_uuid", 36)
    /** The name of the player at the time of the action. */
    val playerName = varchar("player_name", 16)
    /** The type of action performed (e.g., BLOCK_BREAK, INTERACT). */
    val actionType = varchar("action_type", 20)
    /** The material involved in the action (e.g., DIAMOND_ORE, TORCH). */
    val material = varchar("material", 50)
    /** The name of the world where the action occurred. */
    val world = varchar("world", 50)
    /** The coordinates of the action. */
    val x = integer("x")
    val y = integer("y")
    val z = integer("z")
    /** The timestamp of the action in milliseconds. */
    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(id)
}
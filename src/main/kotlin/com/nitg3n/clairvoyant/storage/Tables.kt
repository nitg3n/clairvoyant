package com.nitg3n.clairvoyant.storage

import org.jetbrains.exposed.sql.Table

object PlayerActions : Table("player_actions") {
    val id = integer("id").autoIncrement()
    val playerUuid = varchar("player_uuid", 36)
    val timestamp = long("timestamp")
    val actionType = varchar("action_type", 16)
    val world = varchar("world", 64)
    val x = integer("x")
    val y = integer("y")
    val z = integer("z")
    val blockMaterial = varchar("block_material", 64).nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, playerUuid, timestamp)
    }
}

object PlayerStats : Table("player_stats") {
    val playerUuid = varchar("player_uuid", 36)
    val oreSummary = text("ore_summary")
    override val primaryKey = PrimaryKey(playerUuid)
}
package com.nitg3n.clairvoyant.models

import java.util.UUID

sealed class ActionData {
    abstract val playerUuid: UUID
    abstract val timestamp: Long
}

data class MinedBlockAction(
    override val playerUuid: UUID,
    override val timestamp: Long,
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val blockMaterial: String
) : ActionData()

data class PlayerMoveAction(
    override val playerUuid: UUID, override val timestamp: Long, val world: String, val x: Int, val y: Int, val z: Int
) : ActionData()

package com.nitg3n.clairvoyant.models

import java.util.UUID

/**
 * 플레이어의 행동 유형을 정의하는 Enum 클래스.
 */
enum class ActionType {
    BLOCK_BREAK,
    BLOCK_PLACE,
    INTERACT,     // 상자 열기 등 상호작용 이벤트를 위한 유형
    ZONE_ENTRY
}

/**
 * 플레이어의 단일 행동에 대한 모든 정보를 담는 데이터 클래스.
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

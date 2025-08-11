package com.nitg3n.clairvoyant.storage

import org.jetbrains.exposed.sql.Table

/**
 * 플레이어의 모든 행동 로그를 저장하는 데이터베이스 테이블 정의.
 * UUID를 문자열로 저장하도록 수정되었습니다.
 */
object PlayerActions : Table("player_actions") {
    val id = integer("id").autoIncrement()
    val playerUUID = varchar("player_uuid", 36)
    val playerName = varchar("player_name", 16)
    val actionType = varchar("action_type", 20)
    val material = varchar("material", 50)
    val world = varchar("world", 50)
    val x = integer("x")
    val y = integer("y")
    val z = integer("z")
    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(id)
}

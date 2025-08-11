package com.nitg3n.clairvoyant.services

import com.nitg3n.clairvoyant.Clairvoyant
import com.nitg3n.clairvoyant.models.ActionData
import com.nitg3n.clairvoyant.models.ActionType
import com.nitg3n.clairvoyant.storage.PlayerActions
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

/**
 * 데이터베이스 연결 및 쿼리를 관리합니다.
 * (오류 수정: Deprecated된 Exposed API를 최신 버전으로 수정)
 */
class DatabaseManager(private val plugin: Clairvoyant) {

    private lateinit var db: Database

    init {
        connect()
        initializeSchema()
    }

    private fun connect() {
        val dbFile = File(plugin.dataFolder, "clairvoyant.db")
        if (!dbFile.exists()) {
            dbFile.parentFile.mkdirs()
        }
        db = Database.connect("jdbc:sqlite:${dbFile.path}", "org.sqlite.JDBC")
    }

    private fun initializeSchema() {
        transaction(db) {
            SchemaUtils.create(PlayerActions)
        }
    }

    /**
     * 플레이어의 행동 데이터를 데이터베이스에 저장합니다.
     */
    fun logAction(actionData: ActionData) {
        transaction(db) {
            PlayerActions.insert {
                it[playerUUID] = actionData.playerUUID.toString()
                it[playerName] = actionData.playerName
                it[actionType] = actionData.actionType.name
                it[material] = actionData.material
                it[world] = actionData.world
                it[x] = actionData.x
                it[y] = actionData.y
                it[z] = actionData.z
                it[timestamp] = actionData.timestamp
            }
        }
    }

    /**
     * 특정 플레이어의 모든 행동 로그를 가져옵니다.
     */
    fun getPlayerActions(playerUUID: UUID): List<ActionData> {
        return transaction(db) {
            // Deprecated 수정: `selectAll().where { ... }` 를 사용하여 최신 API로 변경
            PlayerActions.selectAll().where { PlayerActions.playerUUID eq playerUUID.toString() }
                .orderBy(PlayerActions.timestamp)
                .map { row ->
                    ActionData(
                        playerUUID = UUID.fromString(row[PlayerActions.playerUUID]),
                        playerName = row[PlayerActions.playerName],
                        actionType = ActionType.valueOf(row[PlayerActions.actionType]),
                        material = row[PlayerActions.material],
                        world = row[PlayerActions.world],
                        x = row[PlayerActions.x],
                        y = row[PlayerActions.y],
                        z = row[PlayerActions.z],
                        timestamp = row[PlayerActions.timestamp]
                    )
                }
        }
    }

    /**
     * 특정 플레이어의 광물 채굴 통계를 가져옵니다.
     */
    fun getPlayerMiningStats(playerUUID: UUID): Map<String, Int> {
        return transaction(db) {
            val materialCol = PlayerActions.material
            val countCol = materialCol.count()
            // Deprecated 수정: `slice` 대신 `select`에 직접 컬럼을 지정하고, `where`를 사용하는 최신 API로 변경
            PlayerActions
                .select(materialCol, countCol)
                .where {
                    (PlayerActions.playerUUID eq playerUUID.toString()) and
                            (PlayerActions.actionType eq ActionType.BLOCK_BREAK.name)
                }
                .groupBy(materialCol)
                .associate {
                    it[materialCol] to it[countCol].toInt()
                }
        }
    }
}

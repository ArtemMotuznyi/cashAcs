package ua.developer.artemmotuznyi.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    private var _dataSource: HikariDataSource? = null
    private val lock = Any()

    val dataSource: HikariDataSource
        get() = _dataSource ?: throw IllegalStateException("Database not initialized. Call init() first.")

    fun init(jdbcUrl: String, user: String, password: String) {
        if (_dataSource != null) {
            return
        }

        synchronized(lock) {
            if (_dataSource == null) {
                val config = HikariConfig().apply {
                    this.jdbcUrl = jdbcUrl
                    this.username = user
                    this.password = password
                    maximumPoolSize = 10
                    isAutoCommit = true
                    transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                    validate()
                }
                val ds = HikariDataSource(config)
                Database.Companion.connect(ds)
                _dataSource = ds
            }
        }
    }
}
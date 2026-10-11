package com.echo.inbox.data.local

import android.content.Context
import androidx.room.Room
import com.echo.inbox.security.DatabaseKeyManager
import net.zetetic.database.Logger
import net.zetetic.database.NoopTarget
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.util.Arrays

object DatabaseFactory {
    fun create(context: Context): EchoDatabase {
        // SQLCipher routes Java-client diagnostics to Logcat by default. Disable that sink before
        // loading or opening the database so sensitive context can never be emitted by the driver.
        Logger.setTarget(NoopTarget())
        System.loadLibrary("sqlcipher")
        val passphrase = DatabaseKeyManager(context).getOrCreatePassphrase()
        return try {
            val factory = SupportOpenHelperFactory(passphrase.copyOf())
            Room.databaseBuilder(
                context.applicationContext,
                EchoDatabase::class.java,
                DATABASE_NAME,
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        } finally {
            Arrays.fill(passphrase, 0)
        }
    }

    const val DATABASE_NAME = "echo_inbox.db"
}

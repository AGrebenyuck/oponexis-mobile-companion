package com.oponexis.companion.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "foundation_metadata")
data class FoundationMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Database(
    entities = [FoundationMetadataEntity::class, CallOutcomeEntity::class, EventOutboxEntity::class, SmsActivityEntity::class, DirectSmsMessageEntity::class],
    version = 10,
    exportSchema = false,
)
abstract class SkeletonDatabase : RoomDatabase() {
    abstract fun callOutcomeDao(): CallOutcomeDao
    abstract fun eventOutboxDao(): EventOutboxDao
    abstract fun smsActivityDao(): SmsActivityDao
    abstract fun directSmsDao(): DirectSmsDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `call_outcomes` (
                `call_ref` TEXT NOT NULL,
                `observed_at_epoch_millis` INTEGER NOT NULL,
                `disconnect_category` TEXT NOT NULL,
                `duration_bucket` TEXT NOT NULL,
                `outcome_code` TEXT,
                `resolved_at_epoch_millis` INTEGER,
                `dismissed_at_epoch_millis` INTEGER,
                PRIMARY KEY(`call_ref`)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `call_outcomes` ADD COLUMN `phone_number` TEXT")
        db.execSQL("ALTER TABLE `call_outcomes` ADD COLUMN `display_name` TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `call_outcomes` ADD COLUMN `history_phone_number` TEXT")
        db.execSQL("ALTER TABLE `call_outcomes` ADD COLUMN `history_display_name` TEXT")
        db.execSQL("ALTER TABLE `call_outcomes` ADD COLUMN `customer_ref` TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `event_outbox` (
                `event_id` TEXT NOT NULL,
                `call_ref` TEXT NOT NULL,
                `schema_version` INTEGER NOT NULL,
                `event_type` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `confidence` TEXT NOT NULL,
                `observed_at_epoch_millis` INTEGER NOT NULL,
                `resolved_at_epoch_millis` INTEGER NOT NULL,
                `disconnect_category` TEXT NOT NULL,
                `duration_bucket` TEXT NOT NULL,
                `outcome_code` TEXT NOT NULL,
                `phone_number` TEXT,
                `customer_ref` TEXT,
                `status` TEXT NOT NULL,
                `attempt_count` INTEGER NOT NULL,
                `next_attempt_at_epoch_millis` INTEGER NOT NULL,
                `lease_until_epoch_millis` INTEGER,
                `last_error_category` TEXT,
                `server_receipt_id` TEXT,
                `delivered_at_epoch_millis` INTEGER,
                `created_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`event_id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_event_outbox_call_ref` ON `event_outbox` (`call_ref`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_event_outbox_status` ON `event_outbox` (`status`)",
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `call_outcomes` ADD COLUMN `sms_receipt_id` TEXT")
        db.execSQL("ALTER TABLE `call_outcomes` ADD COLUMN `sms_receipt_type` TEXT")
        db.execSQL("ALTER TABLE `call_outcomes` ADD COLUMN `sms_delivery_status` TEXT")
        db.execSQL("ALTER TABLE `call_outcomes` ADD COLUMN `sms_delivery_detail` TEXT")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE `call_outcomes` ADD COLUMN `direction` TEXT NOT NULL DEFAULT 'INCOMING'")
	}
}

val MIGRATION_6_7 = object : Migration(6, 7) {
	override fun migrate(db: SupportSQLiteDatabase) {
		db.execSQL("ALTER TABLE `call_outcomes` ADD COLUMN `is_returning_customer` INTEGER NOT NULL DEFAULT 0")
	}
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sms_activity` (
                `id` TEXT NOT NULL,
                `provider_message_id` TEXT,
                `status` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `phone_number` TEXT,
                `detail` TEXT,
                `occurred_at_epoch_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_sms_activity_provider_message_id` " +
                "ON `sms_activity` (`provider_message_id`)",
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `call_outcomes` ADD COLUMN `topic_label` TEXT")
        db.execSQL("ALTER TABLE `event_outbox` ADD COLUMN `topic_label` TEXT")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `direct_sms_messages` (
                `id` TEXT NOT NULL,
                `direction` TEXT NOT NULL,
                `phone_number` TEXT NOT NULL,
                `body` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `created_at_epoch_millis` INTEGER NOT NULL,
                `updated_at_epoch_millis` INTEGER NOT NULL,
                `total_parts` INTEGER NOT NULL DEFAULT 1,
                `sent_parts` INTEGER NOT NULL DEFAULT 0,
                `delivered_parts` INTEGER NOT NULL DEFAULT 0,
                `error` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_direct_sms_messages_created_at_epoch_millis` ON `direct_sms_messages` (`created_at_epoch_millis`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_direct_sms_messages_direction` ON `direct_sms_messages` (`direction`)")
    }
}

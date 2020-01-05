package com.aopro.wordlink.database

import com.aopro.wordlink.ApplicationConfig
import com.mongodb.MongoClient
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import com.mongodb.client.MongoDatabase
import org.litote.kmongo.KMongo

object DatabaseHandler {
    lateinit var clientSession: MongoClient
    lateinit var databaseSession: MongoDatabase

    /**
     * データベースに接続する
     */
    fun initialize() {
        clientSession = KMongo.createClient(
            addr = ServerAddress(ApplicationConfig.DATABASE_HOST, ApplicationConfig.DATABASE_PORT.toInt()),
            credentialsList = listOf(MongoCredential.createCredential(
                ApplicationConfig.DATABASE_USER,
                ApplicationConfig.DATABASE_NAME,
                ApplicationConfig.DATABASE_PASSWORD.toCharArray())
            )
        )

        databaseSession = clientSession.getDatabase("wordlink")
    }

}
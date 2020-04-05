package com.aopro.wordlink.database

import com.aopro.wordlink.ApplicationConfig
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
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
        clientSession = KMongo
            .createClient(MongoClientURI(ApplicationConfig.DATABASE_URL))
        databaseSession = clientSession.getDatabase("mojingo-v2")
    }

}
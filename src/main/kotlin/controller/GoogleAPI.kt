package me.mojingo.v2.backend.controller

import me.mojingo.v2.backend.ApplicationConfig
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import java.io.File
import java.io.InputStreamReader
import java.util.*


object GoogleAPI {

    fun initialize() {
        setUpSheet
    }

    val setUpSheet: Sheets by lazy {
//        val dataStoreDir = File("./credentials")
//        val dataStoreFactory = FileDataStoreFactory(dataStoreDir)
        val jsonFactory = JacksonFactory.getDefaultInstance()
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
//
//        val scopes = Arrays.asList(SheetsScopes.SPREADSHEETS)
//
//        val clientSecret = GoogleClientSecrets.load(jsonFactory,
//            InputStreamReader(ApplicationConfig::class.java.getResourceAsStream("/mojingo-v2-prod-9adb6e75f461.json")))
//
//        val flow = GoogleAuthorizationCodeFlow.Builder(
//            httpTransport,
//            jsonFactory,
//            clientSecret,
//            scopes
//        ).setDataStoreFactory(dataStoreFactory).setAccessType("offline").build()
//        val credential = AuthorizationCodeInstalledApp(flow,  LocalServerReceiver()).authorize("user")

        val credential = GoogleCredential.Builder().setTransport(httpTransport)
            .setJsonFactory(jsonFactory)
            .setServiceAccountId("sheet-request@mojingo-v2-prod.iam.gserviceaccount.com")
            .setServiceAccountPrivateKeyFromP12File(File(ApplicationConfig.GOOGLE_P12_LOCATION))
            .setServiceAccountScopes(arrayListOf(SheetsScopes.SPREADSHEETS_READONLY))
            .build()
        Sheets.Builder(httpTransport, jsonFactory, credential).setApplicationName("Mojingo").build()
    }


}
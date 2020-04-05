package com.aopro.wordlink.controller

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.Sheet
import java.io.InputStreamReader
import java.util.*


object GoogleAPI {

    fun initialize() {
        setUpSheet
    }

    val setUpSheet: Sheets by lazy {
        val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory: JsonFactory = JacksonFactory()

        val clientSecret = GoogleClientSecrets.load(jsonFactory,
            InputStreamReader(this::class.java.getResourceAsStream("client-secret.json")))

        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecret,
            SheetsScopes.all()
//            arrayListOf("https://docs.google.com/feeds", "https://spreadsheets.google.com/feeds")
        ).build()

        val credential =  AuthorizationCodeInstalledApp(flow, LocalServerReceiver()).authorize("user")
        Sheets.Builder(httpTransport, jsonFactory, credential).setApplicationName("Mojingo").build()
    }


}
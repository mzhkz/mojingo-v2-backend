package com.aopro.wordlink

import com.aopro.wordlink.utilities.generateRandomSHA512
import java.lang.Exception
import java.lang.IllegalArgumentException

object ApplicationConfig {

    private const val ENV_PREFIX = "WORDLINK"

    /** 環境変数名を取得*/
    val env: (String) -> String = { value ->
        "${ENV_PREFIX}_${value}"
    }

    val PRODUCTION by lazy {
        KTOR_ENV == "prod"
    }



    val KTOR_ENV by lazy {
        System.getenv(env("KTOR_ENV"))?: "dev"
    }

    /** 接続を許可するフロントエンドのドメイン (CORS ) */
    val FRONTEND_APP_DOMAIN by lazy {
        System.getenv(env("FRONTEND_DOMAIN")) ?: throw IllegalAccessException("接続を許可するフロントエンドのドメインの環境変数を設定してください。")
    }

    /** MongoデータベースのURL*/
    val DATABASE_URL by lazy {
        System.getenv(env("DATABASE_URL")) ?: throw IllegalAccessException("データベースURLの環境変数を設定してください。")
    }

    /** Mongoデータベースのユーザ */
    val ALLOWED_ROOT by lazy {
        System.getenv(env("ALLOW_ROOT")) == "1"
    }

    /** Mongoデータベースのユーザ */
    val ROOT_PASSWORD by lazy {
        System.getenv(env("ROOT_PASSWORD"))
            ?: if (ALLOWED_ROOT) throw IllegalAccessException("ROOTアカウントを有効化するためには、環境変数にてパスワードの登録が必要です。") else ""
    }

    /** JWT Secret */
    val SESSION_SECRET by lazy {
        System.getenv(env("SESSION_SECRET")) ?: throw IllegalAccessException("セッションシークレットの環境変数を設定してください。")
    }
}
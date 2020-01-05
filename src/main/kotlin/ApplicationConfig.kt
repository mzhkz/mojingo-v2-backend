package com.aopro.wordlink

object ApplicationConfig {

    private const val ENV_PREFIX = "WORDLINK"

    /** 環境変数名を取得*/
    val env: (String) -> String = { value ->
        println("${ENV_PREFIX}_${value}")
        "${ENV_PREFIX}_${value}"
    }

    val RUNNNING_PROFUCTION by lazy {
        System.getenv(env("PRODUCTION"))
    }

    val TOP_LEVEL_APP_DOMAIN by lazy {
        System.getenv(env("TOP_LEVEL_APP_DOMAIN"))
    }

    val FRONTEND_APP_DOMAIN by lazy {
        System.getenv(env("FRONTEND_APP_DOMAIN"))
    }

    val BACKEND_APP_DOMAIN by lazy {
        System.getenv(env("BACKEND_APP_DOMAIN"))
    }

    /** Mongoデータベースのホスト*/
    val DATABASE_HOST by lazy {
        System.getenv(env("DATABASE_HOST"))
    }

    /** Mongoデータベースのポート */
    val DATABASE_PORT by lazy {
        System.getenv(env("DATABASE_PORT"))
    }

    /** Mongoデータベース名 */
    val DATABASE_NAME by lazy {
        System.getenv(env("DATABASE_NAME")) ?: "wordlink"
    }

    /** Mongoデータベースのユーザ */
    val DATABASE_USER by lazy {
        System.getenv(env("DATABASE_USER"))
    }

    /** MySQLデータベースのパスワード */
    val DATABASE_PASSWORD by lazy {
        System.getenv(env("DATABASE_PASSWORD"))
    }
}
package me.mojingo.v2.backend

object ApplicationConfig {

    private const val ENV_PREFIX = "MOJINGOV2"

    /** 環境変数名を取得*/
    val env: (String) -> String = { value ->
        "${ENV_PREFIX}_${value}"
    }

    val PRODUCTION by lazy {
        KTOR_ENV == "prod"
    }



    val KTOR_ENV by lazy {
        System.getenv("KTOR_ENV")?: "dev"
    }

    /** 接続を許可するフロントエンドのドメイン (CORS ) */
    val FRONTEND_APP_DOMAIN by lazy {
        System.getenv(env("FRONTEND_DOMAIN")) ?: throw IllegalAccessException("接続を許可するフロントエンドのドメインの環境変数を設定してください。")
    }

    /** 接続を許可するフロントエンドのドメイン (CORS ) */
    val GOOGLE_P12_LOCATION by lazy {
        System.getenv(env("GOOGLE_P12_LOCATION")) ?: throw IllegalAccessException("P12の環境変数を設定してください。")
    }


    /** MongoデータベースのURL*/
    val DATABASE_URL by lazy {
        System.getenv(env("DATABASE_URL")) ?: throw IllegalAccessException("データベースURLの環境変数を設定してください。")
    }

    /** Mongoデータベースのユーザ */
    val ALLOW_ROOT by lazy {
        System.getenv(env("ALLOW_ROOT")) == "1"
    }

    /** Mongoデータベースのユーザ */
    val ROOT_PASSWORD by lazy {
        System.getenv(env("ROOT_PASSWORD"))
            ?: if (ALLOW_ROOT) throw IllegalAccessException("ROOTアカウントを有効化するためには、環境変数にてパスワードの登録が必要です。") else ""
    }

    /** パスワードSALT*/
    val PASSWORD_SECRET by lazy {
        System.getenv(env("PASSWORD_SECRET")) ?: throw IllegalAccessException("パスワードシークレットの環境変数を設定してください。")
    }

    /** JWT Secret */
    val SESSION_SECRET by lazy {
        System.getenv(env("SESSION_SECRET")) ?: throw IllegalAccessException("セッションシークレットの環境変数を設定してください。")
    }

    const val SYSTEM_ROOT_NAME = "system_root" //ルートユーザーで予約されるユーザ名

    const val REVIEW_OF_RECOMMENDED_MAX_SIZE = 100 //復習テストの最大サイズ数
}
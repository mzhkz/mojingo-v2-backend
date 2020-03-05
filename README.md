## WordLink-BackEnd 

依頼。

[![CircleCI](https://circleci.com/gh/mozow470/Mojingo-Frontend.svg?style=svg)](https://circleci.com/gh/mozow470/Mojingo-Frontend)


### Using Language, framework and libraries.
- Kotlin (1.3.61)
- Ktor (1.2.6)
- MongoDB (4.0)
- MongoDB Driver (3.11.2)

### Environments Settings
|ENV|Type|Description|DEFAULT|
|:---|:---|:---|:---|
|WORDLINK_DATABASE_URL|string|データベース接続のURL|localhost|
|WORDLINK_FRONTEND_DOMAIN|string|接続を許可するフロントエンドのドメイン（CORS）|****|
|WORDLINK_SESSION_SECRET|string|JsonWebTokenの秘密文字列|****|
|WORDLINK_ALLOWED_ROOT|int|ROOTアカウントを有効にするかどうか|0|
|WORDLINK_ROOT_PASSWORD|string|ROOTアカウントのパスワード|****|
|KTOR_ENV|string|本番環境フラグ|


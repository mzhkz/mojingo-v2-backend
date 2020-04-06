## Mojingo-V2-BackEnd 

### Using Language, framework and libraries.
- Kotlin (1.3.61)
- Ktor (1.2.6)
- MongoDB (4.0)
- MongoDB Driver (3.11.2)

### Environments Settings
|ENV|Type|Description|DEFAULT|
|:---|:---|:---|:---|
|MOJINGOV2_DATABASE_URL|string|データベース接続のURL|localhost|
|MOJINGOV2_FRONTEND_DOMAIN|string|接続を許可するフロントエンドのドメイン（CORS）|****|
|MOJINGOV2_SESSION_SECRET|string|JsonWebTokenの秘密文字列|****|
|MOJINGOV2_PASSWORD_SECRET|string|Passwordの暗号化ハッシュトークン|****|
|MOJINGOV2_ALLOW_ROOT|int|ROOTアカウントを有効にするかどうか|0|
|MOJINGOV2_ROOT_PASSWORD|string|ROOTアカウントのパスワード|****|
|MOJINGOV2_GOOGLE_P12_LOCATION|string|P12ファイルのロケーション|****|
|KTOR_ENV|string|本番環境フラグ|false|


keytool -genkey -alias poker -storetype PKCS12 -keyalg RSA -keysize 2048 -keystore keystore.p12 -validity 4000 -ext SAN=dns:localhost,ip:127.0.0.1


pause
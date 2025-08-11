#!/usr/bin/env kotlin
@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("org.springframework.security:spring-security-crypto:6.2.1")
@file:DependsOn("org.springframework:spring-jcl:6.1.12")
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

if (args.size != 2) {
    println("Usage: ./scripts/hash-password.main.kts <username> <password>")
    kotlin.system.exitProcess(1)
}

val username = args[0]
val password = args[1]

require(username.length <= 100 && password.length <= 100) { "Error: max length 100" }
require(Regex("^[a-zA-Z0-9_.-]+$").matches(username)) {
    "Error: Username can contain letters, numbers, dots, hyphens, underscores only"
}

val cost = 12
val hash = BCryptPasswordEncoder(cost).encode(password)

println("Hashed credentials for admin_credentials.txt:")
println(username)
println(hash)
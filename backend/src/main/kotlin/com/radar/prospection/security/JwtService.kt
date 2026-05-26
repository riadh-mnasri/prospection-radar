package com.radar.prospection.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService {

    @Value("\${security.jwt.secret}")
    private lateinit var secret: String

    @Value("\${security.jwt.expiration-ms}")
    private var expirationMs: Long = 0

    private fun key(): SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun generate(user: UserDetails): String = Jwts.builder()
        .subject(user.username)
        .issuedAt(Date())
        .expiration(Date(System.currentTimeMillis() + expirationMs))
        .signWith(key())
        .compact()

    fun extractEmail(token: String): String = claims(token).subject

    fun isValid(token: String, user: UserDetails): Boolean = try {
        extractEmail(token) == user.username && !isExpired(token)
    } catch (_: Exception) {
        false
    }

    private fun isExpired(token: String): Boolean = claims(token).expiration.before(Date())

    private fun claims(token: String) = Jwts.parser().verifyWith(key()).build()
        .parseSignedClaims(token).payload
}

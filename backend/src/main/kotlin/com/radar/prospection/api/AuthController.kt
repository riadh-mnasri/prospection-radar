package com.radar.prospection.api

import com.radar.prospection.domain.User
import com.radar.prospection.repository.UserRepository
import com.radar.prospection.security.JwtService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authManager: AuthenticationManager
) {

    @PostMapping("/register")
    fun register(@RequestBody body: Map<String, String>): ResponseEntity<*> {
        val email = body["email"] ?: return ResponseEntity.badRequest().body(mapOf("error" to "Champs requis manquants"))
        val name = body["name"] ?: return ResponseEntity.badRequest().body(mapOf("error" to "Champs requis manquants"))
        val pass = body["password"] ?: return ResponseEntity.badRequest().body(mapOf("error" to "Champs requis manquants"))

        if (userRepository.existsByEmail(email))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "Cet email est déjà utilisé"))

        val user = userRepository.save(User(email = email, name = name).withPassword(passwordEncoder.encode(pass)))

        return ResponseEntity.ok(mapOf(
            "token" to jwtService.generate(user),
            "name" to user.name,
            "email" to user.email
        ))
    }

    @PostMapping("/login")
    fun login(@RequestBody body: Map<String, String>): ResponseEntity<*> {
        val email = body["email"] ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Email ou mot de passe incorrect"))
        val pass = body["password"] ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Email ou mot de passe incorrect"))

        try {
            authManager.authenticate(UsernamePasswordAuthenticationToken(email, pass))
        } catch (_: Exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Email ou mot de passe incorrect"))
        }

        val user = userRepository.findByEmail(email) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
        return ResponseEntity.ok(mapOf(
            "token" to jwtService.generate(user),
            "name" to user.name,
            "email" to user.email
        ))
    }

    @GetMapping("/me")
    fun me(@RequestHeader("Authorization") header: String): ResponseEntity<*> {
        val token = header.substring(7)
        val email = jwtService.extractEmail(token)
        return userRepository.findByEmail(email)
            ?.let { u -> ResponseEntity.ok(mapOf("name" to u.name, "email" to u.email)) }
            ?: ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
    }
}

package com.radar.prospection.security

import com.radar.prospection.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails =
        userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("Utilisateur introuvable: $email")
}

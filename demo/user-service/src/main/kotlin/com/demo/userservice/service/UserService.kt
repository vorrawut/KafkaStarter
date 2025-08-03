package com.demo.userservice.service

import com.demo.userservice.model.*
import com.demo.userservice.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class UserService {
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Autowired
    private lateinit var userEventPublisher: UserEventPublisher
    
    private val passwordEncoder = BCryptPasswordEncoder()
    private val logger = org.slf4j.LoggerFactory.getLogger(UserService::class.java)
    
    fun registerUser(request: UserRegistrationRequest): UserResponse {
        logger.info("Registering new user: ${request.username}")
        
        // Check if user already exists
        if (userRepository.findByUsername(request.username) != null) {
            throw IllegalArgumentException("Username already exists: ${request.username}")
        }
        
        if (userRepository.findByEmail(request.email) != null) {
            throw IllegalArgumentException("Email already exists: ${request.email}")
        }
        
        // Create new user
        val user = User(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName,
            phoneNumber = request.phoneNumber,
            status = UserStatus.ACTIVE,
            createdAt = Instant.now()
        )
        
        val savedUser = userRepository.save(user)
        
        // Publish user registration event
        val registrationEvent = UserRegisteredEvent(
            eventId = "user-reg-${UUID.randomUUID()}",
            userId = savedUser.id,
            username = savedUser.username,
            email = savedUser.email,
            firstName = savedUser.firstName,
            lastName = savedUser.lastName,
            phoneNumber = savedUser.phoneNumber,
            registrationSource = "WEB"
        )
        
        userEventPublisher.publishUserRegistered(registrationEvent)
        
        // Publish general user event
        val userEvent = UserEvent(
            eventId = "user-event-${UUID.randomUUID()}",
            eventType = "REGISTERED",
            userId = savedUser.id,
            username = savedUser.username,
            email = savedUser.email,
            metadata = mapOf(
                "firstName" to savedUser.firstName,
                "lastName" to savedUser.lastName,
                "registrationSource" to "WEB"
            )
        )
        
        userEventPublisher.publishUserEvent(userEvent)
        
        logger.info("User registered successfully: ${savedUser.username}")
        
        return UserResponse(
            id = savedUser.id,
            username = savedUser.username,
            email = savedUser.email,
            firstName = savedUser.firstName,
            lastName = savedUser.lastName,
            phoneNumber = savedUser.phoneNumber,
            status = savedUser.status,
            createdAt = savedUser.createdAt,
            lastLoginAt = savedUser.lastLoginAt
        )
    }
    
    fun authenticateUser(request: UserLoginRequest, ipAddress: String, userAgent: String): UserResponse {
        logger.info("Authenticating user: ${request.username}")
        
        val user = userRepository.findByUsername(request.username)
            ?: throw IllegalArgumentException("Invalid username or password")
        
        if (!passwordEncoder.matches(request.password, user.password)) {
            logger.warn("Failed login attempt for user: ${request.username}")
            throw IllegalArgumentException("Invalid username or password")
        }
        
        if (user.status != UserStatus.ACTIVE) {
            logger.warn("Login attempt for inactive user: ${request.username}")
            throw IllegalArgumentException("User account is not active")
        }
        
        // Update last login time
        val updatedUser = user.copy(lastLoginAt = Instant.now())
        userRepository.save(updatedUser)
        
        // Publish login event
        val loginEvent = UserLoginEvent(
            eventId = "user-login-${UUID.randomUUID()}",
            userId = updatedUser.id,
            username = updatedUser.username,
            loginMethod = "PASSWORD",
            ipAddress = ipAddress,
            userAgent = userAgent,
            sessionId = UUID.randomUUID().toString()
        )
        
        userEventPublisher.publishUserLogin(loginEvent)
        
        // Publish general user event
        val userEvent = UserEvent(
            eventId = "user-event-${UUID.randomUUID()}",
            eventType = "LOGIN",
            userId = updatedUser.id,
            username = updatedUser.username,
            email = updatedUser.email,
            metadata = mapOf(
                "ipAddress" to ipAddress,
                "userAgent" to userAgent,
                "sessionId" to loginEvent.sessionId
            )
        )
        
        userEventPublisher.publishUserEvent(userEvent)
        
        logger.info("User authenticated successfully: ${updatedUser.username}")
        
        return UserResponse(
            id = updatedUser.id,
            username = updatedUser.username,
            email = updatedUser.email,
            firstName = updatedUser.firstName,
            lastName = updatedUser.lastName,
            phoneNumber = updatedUser.phoneNumber,
            status = updatedUser.status,
            createdAt = updatedUser.createdAt,
            lastLoginAt = updatedUser.lastLoginAt
        )
    }
    
    fun getUserById(id: Long): UserResponse? {
        val user = userRepository.findById(id).orElse(null) ?: return null
        
        return UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            phoneNumber = user.phoneNumber,
            status = user.status,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt
        )
    }
    
    fun getUserByUsername(username: String): UserResponse? {
        val user = userRepository.findByUsername(username) ?: return null
        
        return UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            phoneNumber = user.phoneNumber,
            status = user.status,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt
        )
    }
    
    fun updateUserStatus(userId: Long, newStatus: UserStatus): UserResponse {
        logger.info("Updating user status: userId=$userId, newStatus=$newStatus")
        
        val user = userRepository.findById(userId).orElse(null)
            ?: throw IllegalArgumentException("User not found: $userId")
        
        val updatedUser = user.copy(status = newStatus)
        userRepository.save(updatedUser)
        
        // Publish status change event
        val userEvent = UserEvent(
            eventId = "user-event-${UUID.randomUUID()}",
            eventType = "STATUS_CHANGED",
            userId = updatedUser.id,
            username = updatedUser.username,
            email = updatedUser.email,
            metadata = mapOf(
                "oldStatus" to user.status.name,
                "newStatus" to newStatus.name
            )
        )
        
        userEventPublisher.publishUserEvent(userEvent)
        
        logger.info("User status updated successfully: userId=$userId, newStatus=$newStatus")
        
        return UserResponse(
            id = updatedUser.id,
            username = updatedUser.username,
            email = updatedUser.email,
            firstName = updatedUser.firstName,
            lastName = updatedUser.lastName,
            phoneNumber = updatedUser.phoneNumber,
            status = updatedUser.status,
            createdAt = updatedUser.createdAt,
            lastLoginAt = updatedUser.lastLoginAt
        )
    }
    
    fun getAllUsers(): List<UserResponse> {
        return userRepository.findAll().map { user ->
            UserResponse(
                id = user.id,
                username = user.username,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                phoneNumber = user.phoneNumber,
                status = user.status,
                createdAt = user.createdAt,
                lastLoginAt = user.lastLoginAt
            )
        }
    }
}
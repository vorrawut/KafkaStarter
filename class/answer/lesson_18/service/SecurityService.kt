package com.learning.KafkaStarter.service

import com.learning.KafkaStarter.model.UserCredentials
import com.learning.KafkaStarter.model.AclEntry
import com.learning.KafkaStarter.model.SecurityAuditEvent
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewAcl
import org.apache.kafka.clients.admin.AclBindingFilter
import org.apache.kafka.common.acl.*
import org.apache.kafka.common.resource.ResourcePattern
import org.apache.kafka.common.resource.ResourceType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

@Service
class SecurityService {
    
    @Autowired
    private lateinit var kafkaAdmin: KafkaAdmin
    
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, SecurityAuditEvent>
    
    @Value("\${app.security.audit.enabled:true}")
    private var auditEnabled: Boolean = true
    
    @Value("\${app.security.audit.topic:security-audit-events}")
    private var auditTopic: String = "security-audit-events"
    
    private val passwordEncoder = BCryptPasswordEncoder()
    private val logger = org.slf4j.LoggerFactory.getLogger(SecurityService::class.java)
    
    // In-memory user store (in production, use database or LDAP)
    private val userStore = ConcurrentHashMap<String, UserCredentials>()
    
    // Encryption key (in production, use secure key management)
    private val encryptionKey: SecretKey by lazy {
        val keySpec = SecretKeySpec(
            "MySecretKey123456789012345678901234".toByteArray().sliceArray(0..31),
            "AES"
        )
        keySpec
    }
    
    // Statistics
    private val authenticationAttempts = AtomicLong(0)
    private val successfulAuthentications = AtomicLong(0)
    private val failedAuthentications = AtomicLong(0)
    private val authorizationChecks = AtomicLong(0)
    private val accessDenials = AtomicLong(0)
    
    init {
        // Initialize sample users
        initializeSampleUsers()
    }
    
    fun authenticateUser(username: String, password: String): UserCredentials? {
        authenticationAttempts.incrementAndGet()
        
        try {
            logger.debug("Attempting authentication for user: $username")
            
            val user = userStore[username]
            if (user == null) {
                logger.warn("User not found: $username")
                failedAuthentications.incrementAndGet()
                auditSecurityEvent(createAuditEvent("AUTHENTICATION", username, "USER_NOT_FOUND", "FAILURE"))
                return null
            }
            
            // Check if credentials are expired
            if (user.expiresAt < System.currentTimeMillis()) {
                logger.warn("Credentials expired for user: $username")
                failedAuthentications.incrementAndGet()
                auditSecurityEvent(createAuditEvent("AUTHENTICATION", username, "CREDENTIALS_EXPIRED", "FAILURE"))
                return null
            }
            
            // Verify password
            if (passwordEncoder.matches(password, user.password)) {
                logger.info("Authentication successful for user: $username")
                successfulAuthentications.incrementAndGet()
                auditSecurityEvent(createAuditEvent("AUTHENTICATION", username, "LOGIN", "SUCCESS"))
                return user
            } else {
                logger.warn("Invalid password for user: $username")
                failedAuthentications.incrementAndGet()
                auditSecurityEvent(createAuditEvent("AUTHENTICATION", username, "INVALID_PASSWORD", "FAILURE"))
                return null
            }
            
        } catch (e: Exception) {
            logger.error("Authentication error for user: $username", e)
            failedAuthentications.incrementAndGet()
            auditSecurityEvent(createAuditEvent("AUTHENTICATION", username, "AUTHENTICATION_ERROR", "FAILURE"))
            return null
        }
    }
    
    fun authorizeOperation(principal: String, resource: String, operation: String): Boolean {
        authorizationChecks.incrementAndGet()
        
        try {
            logger.debug("Checking authorization: principal=$principal, resource=$resource, operation=$operation")
            
            // Get user credentials
            val user = userStore[principal]
            if (user == null) {
                logger.warn("Principal not found for authorization: $principal")
                accessDenials.incrementAndGet()
                auditSecurityEvent(createAuditEvent("AUTHORIZATION", principal, resource, "FAILURE", 
                    mapOf("operation" to operation, "reason" to "PRINCIPAL_NOT_FOUND")))
                return false
            }
            
            // Check role-based permissions
            val hasRolePermission = checkRolePermission(user.roles, resource, operation)
            if (!hasRolePermission) {
                logger.warn("Insufficient role permissions: principal=$principal, roles=${user.roles}")
                accessDenials.incrementAndGet()
                auditSecurityEvent(createAuditEvent("AUTHORIZATION", principal, resource, "FAILURE",
                    mapOf("operation" to operation, "reason" to "INSUFFICIENT_ROLE_PERMISSIONS")))
                return false
            }
            
            // Check explicit permissions
            val hasExplicitPermission = user.permissions.contains("$resource:$operation") || 
                                      user.permissions.contains("*:*") ||
                                      user.permissions.contains("$resource:*")
            
            if (!hasExplicitPermission) {
                logger.warn("No explicit permission: principal=$principal, resource=$resource, operation=$operation")
                accessDenials.incrementAndGet()
                auditSecurityEvent(createAuditEvent("AUTHORIZATION", principal, resource, "FAILURE",
                    mapOf("operation" to operation, "reason" to "NO_EXPLICIT_PERMISSION")))
                return false
            }
            
            logger.info("Authorization successful: principal=$principal, resource=$resource, operation=$operation")
            auditSecurityEvent(createAuditEvent("AUTHORIZATION", principal, resource, "SUCCESS",
                mapOf("operation" to operation)))
            return true
            
        } catch (e: Exception) {
            logger.error("Authorization error: principal=$principal, resource=$resource, operation=$operation", e)
            accessDenials.incrementAndGet()
            auditSecurityEvent(createAuditEvent("AUTHORIZATION", principal, resource, "FAILURE",
                mapOf("operation" to operation, "error" to (e.message ?: "Unknown error"))))
            return false
        }
    }
    
    fun createAclEntry(aclEntry: AclEntry): Boolean {
        try {
            logger.info("Creating ACL entry: $aclEntry")
            
            val adminClient = AdminClient.create(kafkaAdmin.configurationProperties)
            
            val resourcePattern = ResourcePattern(
                ResourceType.valueOf(aclEntry.resourceType),
                aclEntry.resourceName,
                org.apache.kafka.common.resource.PatternType.LITERAL
            )
            
            val accessControlEntry = AccessControlEntry(
                aclEntry.principal,
                aclEntry.host,
                AclOperation.valueOf(aclEntry.operation),
                AclPermissionType.valueOf(aclEntry.permission)
            )
            
            val aclBinding = AclBinding(resourcePattern, accessControlEntry)
            val newAcl = NewAcl(aclBinding)
            
            val result = adminClient.createAcls(listOf(newAcl))
            result.all().get() // Wait for completion
            
            adminClient.close()
            
            logger.info("ACL entry created successfully: $aclEntry")
            auditSecurityEvent(createAuditEvent("ACL_MANAGEMENT", aclEntry.principal, 
                aclEntry.resourceName, "SUCCESS", mapOf("operation" to "CREATE_ACL", "aclEntry" to aclEntry)))
            
            return true
            
        } catch (e: Exception) {
            logger.error("Failed to create ACL entry: $aclEntry", e)
            auditSecurityEvent(createAuditEvent("ACL_MANAGEMENT", aclEntry.principal,
                aclEntry.resourceName, "FAILURE", mapOf("operation" to "CREATE_ACL", "error" to (e.message ?: "Unknown error"))))
            return false
        }
    }
    
    fun listAcls(resourceType: String?, principal: String?): List<AclEntry> {
        try {
            logger.debug("Listing ACLs: resourceType=$resourceType, principal=$principal")
            
            val adminClient = AdminClient.create(kafkaAdmin.configurationProperties)
            
            // Create filter
            val resourceFilter = if (resourceType != null) {
                org.apache.kafka.common.resource.ResourcePatternFilter(
                    ResourceType.valueOf(resourceType),
                    null,
                    org.apache.kafka.common.resource.PatternType.ANY
                )
            } else {
                org.apache.kafka.common.resource.ResourcePatternFilter.ANY
            }
            
            val accessControlEntryFilter = if (principal != null) {
                AccessControlEntryFilter(principal, null, AclOperation.ANY, AclPermissionType.ANY)
            } else {
                AccessControlEntryFilter.ANY
            }
            
            val aclBindingFilter = AclBindingFilter(resourceFilter, accessControlEntryFilter)
            
            val result = adminClient.describeAcls(aclBindingFilter)
            val aclBindings = result.values().get()
            
            adminClient.close()
            
            val aclEntries = aclBindings.map { binding ->
                AclEntry(
                    principal = binding.entry().principal(),
                    resourceType = binding.pattern().resourceType().name,
                    resourceName = binding.pattern().name(),
                    operation = binding.entry().operation().name,
                    permission = binding.entry().permissionType().name,
                    host = binding.entry().host(),
                    createdAt = System.currentTimeMillis()
                )
            }
            
            logger.debug("Listed ${aclEntries.size} ACL entries")
            return aclEntries
            
        } catch (e: Exception) {
            logger.error("Failed to list ACLs: resourceType=$resourceType, principal=$principal", e)
            return emptyList()
        }
    }
    
    fun deleteAclEntry(aclEntry: AclEntry): Boolean {
        try {
            logger.info("Deleting ACL entry: $aclEntry")
            
            val adminClient = AdminClient.create(kafkaAdmin.configurationProperties)
            
            val resourcePattern = ResourcePattern(
                ResourceType.valueOf(aclEntry.resourceType),
                aclEntry.resourceName,
                org.apache.kafka.common.resource.PatternType.LITERAL
            )
            
            val accessControlEntry = AccessControlEntry(
                aclEntry.principal,
                aclEntry.host,
                AclOperation.valueOf(aclEntry.operation),
                AclPermissionType.valueOf(aclEntry.permission)
            )
            
            val aclBinding = AclBinding(resourcePattern, accessControlEntry)
            val aclBindingFilter = AclBindingFilter(aclBinding.pattern().toFilter(), aclBinding.entry().toFilter())
            
            val result = adminClient.deleteAcls(listOf(aclBindingFilter))
            result.all().get() // Wait for completion
            
            adminClient.close()
            
            logger.info("ACL entry deleted successfully: $aclEntry")
            auditSecurityEvent(createAuditEvent("ACL_MANAGEMENT", aclEntry.principal,
                aclEntry.resourceName, "SUCCESS", mapOf("operation" to "DELETE_ACL", "aclEntry" to aclEntry)))
            
            return true
            
        } catch (e: Exception) {
            logger.error("Failed to delete ACL entry: $aclEntry", e)
            auditSecurityEvent(createAuditEvent("ACL_MANAGEMENT", aclEntry.principal,
                aclEntry.resourceName, "FAILURE", mapOf("operation" to "DELETE_ACL", "error" to (e.message ?: "Unknown error"))))
            return false
        }
    }
    
    fun auditSecurityEvent(event: SecurityAuditEvent) {
        if (!auditEnabled) return
        
        try {
            kafkaTemplate.send(auditTopic, event.principal, event)
            logger.debug("Security event audited: ${event.eventType} for ${event.principal}")
        } catch (e: Exception) {
            logger.error("Failed to audit security event: $event", e)
        }
    }
    
    fun validateMessageClassification(classification: String, userRole: String): Boolean {
        // Get classification levels
        val classificationLevels = mapOf(
            "PUBLIC" to 0,
            "INTERNAL" to 1,
            "CONFIDENTIAL" to 2,
            "SECRET" to 3
        )
        
        val roleClearance = mapOf(
            "USER" to 0,
            "EMPLOYEE" to 1,
            "MANAGER" to 2,
            "ADMIN" to 3
        )
        
        val messageLevel = classificationLevels[classification] ?: return false
        val userLevel = roleClearance[userRole] ?: return false
        
        val hasAccess = userLevel >= messageLevel
        
        logger.debug("Classification validation: classification=$classification (level=$messageLevel), " +
            "userRole=$userRole (level=$userLevel), hasAccess=$hasAccess")
        
        return hasAccess
    }
    
    fun encryptSensitiveData(data: String): String {
        try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16)
            Random.nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, ivSpec)
            val encryptedData = cipher.doFinal(data.toByteArray())
            
            // Prepend IV to encrypted data
            val result = iv + encryptedData
            return Base64.getEncoder().encodeToString(result)
            
        } catch (e: Exception) {
            logger.error("Failed to encrypt data", e)
            throw RuntimeException("Encryption failed", e)
        }
    }
    
    fun decryptSensitiveData(encryptedData: String): String {
        try {
            val data = Base64.getDecoder().decode(encryptedData)
            
            // Extract IV and encrypted data
            val iv = data.sliceArray(0..15)
            val encrypted = data.sliceArray(16 until data.size)
            
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, ivSpec)
            val decryptedData = cipher.doFinal(encrypted)
            
            return String(decryptedData)
            
        } catch (e: Exception) {
            logger.error("Failed to decrypt data", e)
            throw RuntimeException("Decryption failed", e)
        }
    }
    
    // Utility methods
    private fun initializeSampleUsers() {
        val users = listOf(
            UserCredentials(
                username = "admin",
                password = passwordEncoder.encode("admin123"),
                roles = listOf("ADMIN"),
                permissions = listOf("*:*"),
                expiresAt = System.currentTimeMillis() + 86400000 * 30 // 30 days
            ),
            UserCredentials(
                username = "manager",
                password = passwordEncoder.encode("manager123"),
                roles = listOf("MANAGER"),
                permissions = listOf("secure-messages:READ", "secure-messages:WRITE", "confidential-messages:READ"),
                expiresAt = System.currentTimeMillis() + 86400000 * 30
            ),
            UserCredentials(
                username = "employee",
                password = passwordEncoder.encode("employee123"),
                roles = listOf("EMPLOYEE"),
                permissions = listOf("secure-messages:READ", "secure-messages:WRITE"),
                expiresAt = System.currentTimeMillis() + 86400000 * 30
            ),
            UserCredentials(
                username = "user",
                password = passwordEncoder.encode("user123"),
                roles = listOf("USER"),
                permissions = listOf("secure-messages:READ"),
                expiresAt = System.currentTimeMillis() + 86400000 * 30
            )
        )
        
        users.forEach { user ->
            userStore[user.username] = user
        }
        
        logger.info("Initialized ${users.size} sample users")
    }
    
    private fun checkRolePermission(roles: List<String>, resource: String, operation: String): Boolean {
        // Define role-based permissions
        val rolePermissions = mapOf(
            "ADMIN" to setOf("*:*"),
            "MANAGER" to setOf("secure-messages:*", "confidential-messages:READ", "confidential-messages:WRITE"),
            "EMPLOYEE" to setOf("secure-messages:READ", "secure-messages:WRITE"),
            "USER" to setOf("secure-messages:READ")
        )
        
        return roles.any { role ->
            val permissions = rolePermissions[role] ?: emptySet()
            permissions.any { permission ->
                permission == "*:*" || 
                permission == "$resource:*" || 
                permission == "$resource:$operation"
            }
        }
    }
    
    private fun createAuditEvent(
        eventType: String, 
        principal: String, 
        resource: String, 
        result: String,
        details: Map<String, Any> = emptyMap()
    ): SecurityAuditEvent {
        return SecurityAuditEvent(
            eventId = "audit-${UUID.randomUUID()}",
            eventType = eventType,
            principal = principal,
            resource = resource,
            operation = details["operation"]?.toString() ?: "UNKNOWN",
            result = result,
            clientIp = "127.0.0.1", // In production, get from request
            timestamp = Instant.now().toEpochMilli(),
            details = details
        )
    }
    
    // Statistics methods
    fun getSecurityStatistics(): Map<String, Any> {
        return mapOf(
            "authentication" to mapOf(
                "totalAttempts" to authenticationAttempts.get(),
                "successful" to successfulAuthentications.get(),
                "failed" to failedAuthentications.get(),
                "successRate" to if (authenticationAttempts.get() > 0) {
                    (successfulAuthentications.get().toDouble() / authenticationAttempts.get()) * 100
                } else 0.0
            ),
            "authorization" to mapOf(
                "totalChecks" to authorizationChecks.get(),
                "accessDenials" to accessDenials.get(),
                "accessGranted" to authorizationChecks.get() - accessDenials.get(),
                "denialRate" to if (authorizationChecks.get() > 0) {
                    (accessDenials.get().toDouble() / authorizationChecks.get()) * 100
                } else 0.0
            ),
            "users" to mapOf(
                "totalUsers" to userStore.size,
                "activeUsers" to userStore.values.count { it.expiresAt > System.currentTimeMillis() }
            )
        )
    }
    
    fun resetStatistics() {
        authenticationAttempts.set(0)
        successfulAuthentications.set(0)
        failedAuthentications.set(0)
        authorizationChecks.set(0)
        accessDenials.set(0)
        logger.info("Security statistics reset")
    }
}
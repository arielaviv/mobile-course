package com.field.survey.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val tokenProvider: TokenProvider,
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val user = auth.currentUser ?: return Result.failure(Exception("Login failed"))
            tokenProvider.saveTokens(
                user.uid,
                user.getIdToken(false).await()?.token ?: "",
            )
            tokenProvider.saveUserInfo(
                user.uid,
                user.displayName ?: "",
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<Unit> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            val user = auth.currentUser ?: return Result.failure(Exception("Registration failed"))

            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build(),
            ).await()

            firestore.collection("users").document(user.uid).set(
                mapOf(
                    "name" to name,
                    "email" to email,
                    "profileImageUrl" to "",
                    "createdAt" to System.currentTimeMillis(),
                ),
            ).await()

            tokenProvider.saveTokens(
                user.uid,
                user.getIdToken(false).await()?.token ?: "",
            )
            tokenProvider.saveUserInfo(user.uid, name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun getUserName(): String = auth.currentUser?.displayName ?: tokenProvider.getUserName()

    fun getUserId(): String = auth.currentUser?.uid ?: tokenProvider.getUserId()

    fun getUserEmail(): String = auth.currentUser?.email ?: ""

    suspend fun getUserProfile(): Result<UserProfile> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            Result.success(
                UserProfile(
                    name = doc.getString("name") ?: getUserName(),
                    email = getUserEmail(),
                    photoBase64 = doc.getString("photoBase64") ?: "",
                ),
            )
        } catch (e: Exception) {
            Result.success(
                UserProfile(
                    name = getUserName(),
                    email = getUserEmail(),
                    photoBase64 = "",
                ),
            )
        }
    }

    suspend fun updateProfile(name: String, photoBase64: String?): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
        return try {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build(),
            ).await()

            val updates = mutableMapOf<String, Any>(
                "name" to name,
            )
            if (photoBase64 != null) {
                updates["photoBase64"] = photoBase64
            }
            firestore.collection("users").document(user.uid).update(updates).await()

            tokenProvider.saveUserInfo(user.uid, name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
        tokenProvider.clear()
    }
}

data class UserProfile(
    val name: String,
    val email: String,
    val photoBase64: String,
)

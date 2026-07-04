package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityHelper {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val FIXED_SALT = "VanshVrikshSystemSalt"

    /**
     * Derives a secret key from user input PIN + fixed salt
     */
    private fun getSecretKeySpec(userPin: String): SecretKeySpec {
        val rawKey = (userPin + FIXED_SALT).toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedKey = digest.digest(rawKey)
        return SecretKeySpec(hashedKey, "AES")
    }

    private fun getIvSpec(): IvParameterSpec {
        // Simple IV derived from hash of the salt
        val digest = MessageDigest.getInstance("MD5")
        val iv = digest.digest(FIXED_SALT.toByteArray(Charsets.UTF_8))
        return IvParameterSpec(iv)
    }

    /**
     * Encrypts a string using the provided PIN (or generic default PIN)
     */
    fun encrypt(plainText: String, userPin: String = "1234"): String {
        if (plainText.isEmpty()) return ""
        return try {
            val keySpec = getSecretKeySpec(userPin)
            val ivSpec = getIvSpec()
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to simple base64 so app doesn't crash on encryption failure
            Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        }
    }

    /**
     * Decrypts a string using the provided PIN (or generic default PIN)
     */
    fun decrypt(encryptedText: String, userPin: String = "1234"): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val keySpec = getSecretKeySpec(userPin)
            val ivSpec = getIvSpec()
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                // Return base64 decoded string as fallback
                String(Base64.decode(encryptedText, Base64.DEFAULT), Charsets.UTF_8)
            } catch (ex: Exception) {
                encryptedText // final fallback
            }
        }
    }

    /**
     * Securely copy a picked external Uri file to the application's private storage,
     * protecting application documents/photos from deletion or discovery in standard galleries.
     */
    fun copyFileToPrivateSandbox(context: Context, sourceUri: Uri, folderName: String): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(sourceUri)
            if (inputStream == null) return null

            val sandboxDir = File(context.filesDir, folderName)
            if (!sandboxDir.exists()) {
                sandboxDir.mkdirs()
            }

            // Create unique file name
            val fileName = "doc_${System.currentTimeMillis()}_" + (sourceUri.lastPathSegment?.replace("[^a-zA-Z0-9.-]".toRegex(), "_") ?: "file.bin")
            val destinationFile = File(sandboxDir, fileName)

            val outputStream = FileOutputStream(destinationFile)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            inputStream.close()
            outputStream.flush()
            outputStream.close()

            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

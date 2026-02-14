package com.example.eastsyria.SignUp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.random.Random

class EmailService {

    companion object {
        private const val TAG = "EmailService"
        private const val SENDER_EMAIL = "sup.east.syria@gmail.com"
        private const val SENDER_PASSWORD = "krya wxzn gmnw bsrz" // Fixed: removed \n
        private const val SMTP_HOST = "smtp.gmail.com"
        private const val SMTP_PORT = "465" // SSL port
    }

    fun generateVerificationCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    suspend fun sendVerificationCode(
        recipientEmail: String,
        recipientName: String,
        verificationCode: String
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Sending email to: $recipientEmail")

            val props = Properties().apply {
                put("mail.smtp.host", SMTP_HOST)
                put("mail.smtp.port", SMTP_PORT)
                put("mail.smtp.auth", "true")
                put("mail.smtp.socketFactory.port", SMTP_PORT)
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.ssl.protocols", "TLSv1.2")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(SENDER_EMAIL, "East Syria Explorer"))
                addRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                subject = "Your Verification Code - East Syria Explorer"
                setContent(createEmailBody(recipientName, verificationCode), "text/html; charset=utf-8")
            }

            Transport.send(message)

            Log.d(TAG, "✅ Email sent successfully to $recipientEmail")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send email: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    private fun createEmailBody(name: String, code: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f5f5f5;">
            <table width="100%" cellpadding="0" cellspacing="0" border="0" style="padding: 20px;">
                <tr>
                    <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" border="0" style="background-color: #ffffff; border-radius: 12px; overflow: hidden;">
                            
                            <!-- Header -->
                            <tr>
                                <td style="background-color: #FF6B35; padding: 40px 30px; text-align: center;">
                                    <h1 style="color: #ffffff; margin: 0; font-size: 28px;">East Syria Explorer</h1>
                                </td>
                            </tr>
                            
                            <!-- Content -->
                            <tr>
                                <td style="padding: 40px 30px;">
                                    <h2 style="color: #333333; margin: 0 0 20px 0;">Hello $name! 👋</h2>
                                    <p style="color: #666666; font-size: 16px; line-height: 1.6; margin: 0 0 30px 0;">
                                        Thank you for signing up. Use the verification code below:
                                    </p>
                                    
                                    <!-- Code Box -->
                                    <div style="background-color: #FFF8F5; border: 3px solid #FF6B35; border-radius: 12px; padding: 25px; text-align: center; margin: 30px 0;">
                                        <p style="font-size: 42px; font-weight: bold; color: #FF6B35; margin: 0; letter-spacing: 8px;">$code</p>
                                    </div>
                                    
                                    <p style="color: #999999; font-size: 14px; text-align: center; margin: 20px 0;">
                                        ⏱️ This code expires in 10 minutes
                                    </p>
                                    
                                    <p style="color: #999999; font-size: 13px; margin-top: 20px;">
                                        If you didn't request this, please ignore this email.
                                    </p>
                                </td>
                            </tr>
                            
                            <!-- Footer -->
                            <tr>
                                <td style="background-color: #f8f8f8; padding: 20px; text-align: center;">
                                    <p style="color: #999999; font-size: 12px; margin: 0;">
                                        © 2026 East Syria Explorer
                                    </p>
                                </td>
                            </tr>
                            
                        </table>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.trimIndent()
    }
}
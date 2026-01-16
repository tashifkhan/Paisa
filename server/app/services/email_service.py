"""
Email service - handles sending emails via SMTP.
Async implementation using aiosmtplib.
"""

import aiosmtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

from ..core.config import settings
from ..core.logger import logger


class EmailService:
    """Async email service using aiosmtplib."""

    @staticmethod
    async def send_email(to_email: str, subject: str, html_content: str) -> bool:
        """Send an email using SMTP settings from config."""
        try:
            # Create message
            message = MIMEMultipart("alternative")
            message["From"] = settings.SMTP_FROM or settings.SMTP_USER
            message["To"] = to_email
            message["Subject"] = subject

            # Attach HTML content
            html_part = MIMEText(html_content, "html")
            message.attach(html_part)

            # Send email
            await aiosmtplib.send(
                message,
                hostname=settings.SMTP_HOST,
                port=settings.SMTP_PORT,
                username=settings.SMTP_USER,
                password=settings.SMTP_PASS,
                start_tls=not settings.SMTP_SECURE,  # Use STARTTLS for port 587
            )

            logger.info(f"Email sent successfully to {to_email}")
            return True

        except Exception as e:
            logger.error(f"Failed to send email to {to_email}: {str(e)}")
            return False

    @staticmethod
    async def send_otp_email(to_email: str, otp_code: str) -> bool:
        """Send OTP verification email."""
        subject = "Your Paisa Verification Code"

        html_content = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f4f4f5; margin: 0; padding: 40px 20px;">
            <div style="max-width: 400px; margin: 0 auto; background: white; border-radius: 16px; padding: 40px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                <div style="text-align: center; margin-bottom: 32px;">
                    <h1 style="color: #18181b; font-size: 24px; margin: 0 0 8px 0;">Verification Code</h1>
                    <p style="color: #71717a; font-size: 14px; margin: 0;">Enter this code to verify your email</p>
                </div>
                
                <div style="background: linear-gradient(135deg, #10b981 0%, #059669 100%); border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 24px;">
                    <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: white;">{otp_code}</span>
                </div>
                
                <p style="color: #71717a; font-size: 13px; text-align: center; margin: 0;">
                    This code will expire in <strong>10 minutes</strong>.<br>
                    If you didn't request this code, please ignore this email.
                </p>
                
                <hr style="border: none; border-top: 1px solid #e4e4e7; margin: 32px 0;">
                
                <p style="color: #a1a1aa; font-size: 12px; text-align: center; margin: 0;">
                    © Paisa - Your Personal Finance Manager
                </p>
            </div>
        </body>
        </html>
        """

        return await EmailService.send_email(to_email, subject, html_content)


# Singleton instance
email_service = EmailService()

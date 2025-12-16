<?php
/**
 * Email Helper Functions
 * File: /api/email_helper.php
 */

function send_reset_email($to_email, $user_name, $reset_link) {
    $subject = "Reset Password - Kamus Korea";
    
    // Email body
    $message = "
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; line-height: 1.6; }
            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
            .button { 
                background-color: #6650a4; 
                color: white; 
                padding: 12px 24px; 
                text-decoration: none; 
                border-radius: 5px; 
                display: inline-block;
                margin: 20px 0;
            }
            .footer { 
                margin-top: 30px; 
                font-size: 12px; 
                color: #666; 
                border-top: 1px solid #ddd;
                padding-top: 20px;
            }
        </style>
    </head>
    <body>
        <div class='container'>
            <h2>Reset Password</h2>
            <p>Halo " . htmlspecialchars($user_name) . ",</p>
            <p>Kami menerima permintaan untuk reset password akun Anda.</p>
            <p>Klik tombol di bawah ini untuk membuat password baru:</p>
            
            <a href='" . htmlspecialchars($reset_link) . "' class='button'>Reset Password</a>
            
            <p>Atau copy link ini ke browser Anda:</p>
            <p style='word-break: break-all; color: #6650a4;'>" . htmlspecialchars($reset_link) . "</p>
            
            <p><strong>Link ini akan kadaluarsa dalam 15 menit.</strong></p>
            
            <p>Jika Anda tidak meminta reset password, abaikan email ini.</p>
            
            <div class='footer'>
                <p>Email ini dikirim otomatis oleh sistem Kamus Korea.</p>
                <p>Jangan balas email ini.</p>
            </div>
        </div>
    </body>
    </html>
    ";
    
    // Headers
    $headers = "MIME-Version: 1.0" . "\r\n";
    $headers .= "Content-type:text/html;charset=UTF-8" . "\r\n";
    $headers .= "From: Kamus Korea <noreply@webtechsolution.my.id>" . "\r\n";
    
    // Send email
    return mail($to_email, $subject, $message, $headers);
}
?>
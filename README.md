# ChiaraMail
ChiaraMail is an open source project based on the ChiaraMail for Android email client V. 4.56, available from Google Play 
(https://play.google.com/store/apps/details?id=com.chiaramail.chiaramailforandroid&hl=en_US). This project was originally 
built using the Eclipse IDE. ChiaraMail is an Android email client, based on the Open Source project K-9 Mail. As such, it is 
a functional superset of K-9 Mail. What sets ChiaraMail apart is its use of Envelope-Content Splitting (ECS) technology 
which gives it the following advantages:

1. Secure email in transit without requiring encryption. 
2. Enables the user to change, at any time, the content of an email message that had been sent using ECS. 
3. Send large (> 25 MB) attachments directly to recipients, without requiring any third-party storage facilities. 
4. Prevent unauthorized parties from reading your mail, even if they have access to your email password. 
5. Enables ephemeral messaging (disappearing email content). 
6. Protects against email spoofing and phishing. 
7. Control whether recipients can forward a message you sent them. 

Access to ECS messages is provided via a content server, owned and operated by ChiaraMail Corp. When the user configures an 
email account using an ECS-enabled client, such as this one, they are assigned an ECS content server password, which the
email client sends to the content server whenever an ECS operation (send, view, update, etc.) is performed. ECS message 
content, including the message body and any attachments, is stored in the content server. When an ECS message is sent, only 
the message header and a small 'canned' message, are sent through the email network; for this reason, no encryption is 
required in order to secure the message while it is "in the air". However, the client has the capability to encrypt the 
message content in the server, using AES encryption. See https://www.chiaramailcorp.com/about/works/ for details.
The content server operated by ChiaraMail Corp. is currently hosted on Amazon Web Services and is extremely secure: it 
received an "A+" security rating from Qualys SSL Labs (https://www.ssllabs.com/ssltest/analyze.html?d=chiaramail.com).

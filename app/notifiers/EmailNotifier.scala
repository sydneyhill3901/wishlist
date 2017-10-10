package notifiers

import com.google.inject.ImplementedBy
import javax.inject.Singleton
import scala.concurrent.Future
import models.Recipient
// import play.api.libs.mailer._
// import play.api.{Mode, Play}
// import play.core.Router
import controllers.WithLogging


@ImplementedBy(classOf[DefaultEmailNotifier])
trait EmailNotifier extends WithLogging {

   def sendContactEmail(name: String, email: String, username: Option[String],
                     subject: Option[String], message: String,
                     currentRecipient: Option[Recipient]): Future[Unit] = {
      // TODO !
      logger.warn("TODO")
      Future.successful(())
   }

   def sendNewRegistrationAlert(recipient: Recipient): Future[Unit] = {
      // TODO !
      logger.warn("TODO")
      Future.successful(())
   }

   def sendEmailVerification(recipient: Recipient, verificationHash: String): Future[Unit] = {
      // TODO !
      logger.warn("TODO")
      logger.info(s"Verification: " + controllers.routes.RecipientController.verifyEmail(recipient.username, verificationHash).url)
      Future.successful(())
   }

   def sendPasswordResetEmail( recipient: Recipient): Future[Unit] = {
      // TODO !
      logger.warn("TODO")
      Future.successful(())
   }

   def sendPasswordChangedNotification(recipient: Recipient): Future[Unit] = {
      // TODO !
      logger.warn("TODO")
      Future.successful(())
   }

}

@Singleton
class DefaultEmailNotifier extends EmailNotifier

/*

object EmailConfiguration {

  def hostname = Play.configuration.getString("net.hostname").getOrElse("localhost")
  def emailFrom = Play.configuration.getString("mail.from").getOrElse("wish@example.com")
  def alertRecipient = Play.configuration.getString("mail.alerts").getOrElse("wish@example.org")

}

trait EmailDispatcher {

  def sendEmail(recipient:String,subjectAndBody:(String,String))

  def sendAlertEmail(subjectAndBody:(String,String)) {
    sendEmail(EmailConfiguration.alertRecipient,subjectAndBody)
  }

}

object MockEmailDispatcher extends EmailDispatcher {

  override def sendEmail(recipient:String,subjectAndBody:(String,String)) {
    Logger.info("Email sent (mock): [%s] to [%s]" .format(subjectAndBody._1,recipient))
    Logger.info("%s" .format(subjectAndBody._2))
  }

}


trait MailerComponent {

  val mailerClient = new CommonsMailer(Play.configuration)
}


object SmtpEmailDispatcher extends EmailDispatcher with MailerComponent{

  override def sendEmail(recipient:String, subjectAndBody:(String,String)) {
    val mail = Email(
                    EmailTemplate.subjectPrefix + subjectAndBody._1,
                    EmailConfiguration.emailFrom,
                    Seq(recipient),
                    bodyText = Some(subjectAndBody._2 + EmailTemplate.footer) )
    mailerClient.send(mail)
    Logger.info("Email sent: [%s] to [%s]" .format(subjectAndBody._1,recipient))
  }

}



trait EmailService {

  private def noSmtpHostDefinedException = throw new NullPointerException("No SMTP host defined")

  def dispatcher:EmailDispatcher = {
    if (Play.mode == Mode.Prod) {
      Play.configuration.getString("smtp.host") match {
        case None => noSmtpHostDefinedException
        case Some("mock") => MockEmailDispatcher
        case _ => SmtpEmailDispatcher
      }
    } else MockEmailDispatcher
  }

}



object EmailNotifier extends EmailService {

  def sendPasswordResetEmail(recipient: Recipient, newPassword: String) {
    dispatcher.sendEmail(recipient.email,EmailTemplate.newPasswordText(recipient, newPassword))
  }

  def sendPasswordChangeEmail(recipient: Recipient) {
    dispatcher.sendEmail(recipient.email,EmailTemplate.changePasswordText(recipient))
  }


  def sendRecipientDeletedNotification(recipient: Recipient) {
    dispatcher.sendEmail(recipient.email,EmailTemplate.deleteRecipientNotificationText(recipient))
  }

  def sendEmailVerificationEmail(recipient:Recipient, verificationHash: String) {
    val verificationUrl = EmailConfiguration.hostname + "/recipient/" + recipient.username + "/verify/" + verificationHash +"/"
    dispatcher.sendEmail(recipient.email, EmailTemplate.emailVerificationText(recipient.username, verificationUrl))
  }

}




object EmailAlerter extends EmailService {

  def sendNewRegistrationAlert(recipient: Recipient) {
    dispatcher.sendAlertEmail(EmailTemplate.registrationText(recipient))
  }

  def sendRecipientDeletedAlert(recipient: Recipient) {
    dispatcher.sendAlertEmail(EmailTemplate.deleteRecipientAlertText(recipient))
  }

  def sendContactMessage(name:String,email:String,username:Option[String],subject:Option[String],message:String,currentRecipient:Option[Recipient]) {
    dispatcher.sendAlertEmail(EmailTemplate.contactMessageText(name,email,username,subject,message,currentRecipient))
  }

}
*/

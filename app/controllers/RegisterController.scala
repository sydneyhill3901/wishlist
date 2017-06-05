package controllers

// import play.api.Play.current
// import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import models._
// import notifiers._
// import java.math.BigInteger
// import java.security.SecureRandom
import scala.concurrent.Future


trait RegisterForm {

   // val ValidEmailAddress = """^([0-9a-zA-Z_]+[-_\.0-9a-zA-Z]*\+?[-_\.0-9a-zA-Z]*)@([0-9a-zA-Z][-\w]*[0-9a-zA-Z]\.)+[a-zA-Z]{2,9}$""".r
   val ValidEmailAddresses =
      List(
         // """^[-\._A-Za-z0-9]+(\+[-\._A-Za-z0-9]+)?@[A-Za-z0-9][-\.A-Za-z0-9]+[A-Za-z0-9]$""".r,
         """^[\+\-\._A-Za-z0-9]+@[-\.A-Za-z0-9]{2,}$""".r,  // expected characters
         """^.+(\+.+)?@.+$""".r,                            // can have plus in local alias
         """^.+@[^\-].*[^\-]$""".r                          // domain does not end or start with -
      )

   val InvalidEmailAddress =
         """\.\.""".r                                       // no double dotting


   val ValidUsername = """^[a-zA-Z0-9\-_]{3,99}$""".r

	val simpleRegisterForm = Form {
		"email" -> optional(text(maxLength = 99))
 	}

 	val registerForm = Form(
    tuple(
      "username" -> nonEmptyText(minLength = 3, maxLength = 99),
      "fullname" -> optional(text(maxLength = 99)),
      "email" -> nonEmptyText(maxLength = 99),
      "password" -> nonEmptyText(minLength = 4, maxLength = 99),
      "confirm" -> nonEmptyText(minLength = 4, maxLength = 99)
    ) verifying("Registration failed. Passwords do not match", fields => fields match {
      case (_, _, _, password, confirmPassword) =>
        password.trim == confirmPassword.trim
    })  verifying("Registration failed. Email address is not valid", fields => fields match {
      case (_, _, email, _, _) =>
         ValidEmailAddresses.filterNot( r => r.findFirstIn(email.trim).isDefined ).isEmpty &&
            InvalidEmailAddress.findFirstIn(email.trim).isEmpty
    }) verifying("Registration failed. Username is not valid. A to Z and numbers only", fields => fields match {
      case (username, _, _, _, _) =>
         ValidUsername.findFirstIn(username.trim).isDefined
    })
  )
}



@Singleton
class RegisterController @Inject() (val configuration: Configuration, val recipientFactory: RecipientFactory, val recipientLookup: RecipientLookup)
extends Controller with Secured with WithAnalytics with RegisterForm {

  	def register =
     (UsernameAction andThen MaybeCurrentRecipientAction).async { implicit request =>
  		registerForm.bindFromRequest.fold(
        errors => {
          Logger.warn("Registration failed: " + errors.errors.headOption.map( e => s"${e.key}: ${e.message}").getOrElse(""))
          Future.successful(BadRequest(views.html.register(errors)))
        },
   	   registeredForm => {
            recipientLookup.findRecipient(registeredForm._1.trim.toLowerCase()) flatMap {
               case None =>
                  recipientFactory.newRecipient( registeredForm ).save.map {
                     case Right(recipient) =>
                        Logger.info("New registration: " + registeredForm._1)
                  //  EmailAlerter.sendNewRegistrationAlert(recipient)

                        if(true){
                           //  if(Recipient.emailVerificationRequired){
                           val verificationHash = "hash"
                           // val verificationHash = recipient.findVerificationHash.getOrElse(recipient.generateVerificationHash)
                           // EmailNotifier.sendEmailVerificationEmail(recipient, verificationHash)
                           Redirect(routes.Application.index()).withNewSession.flashing("messageSuccess"->
                              """
                              Welcome, you have successfully registered.<br/>
                              Please click on the verification link in the email we just sent to you
                              """)
                        } else {
                           Redirect(routes.Application.index()).withSession(
                              "username" -> registeredForm._1).flashing("messageSuccess"-> "Welcome, you have successfully registered")
                        }
                     case Left(e) =>
                        throw new IllegalStateException("Not able to save new registration")
                  }
               case _ =>
                  Future.successful(
                     BadRequest(views.html.register(
                           registerForm.fill((registeredForm._1, registeredForm._2, registeredForm._3, "", ""))))
                        .flashing("messageError" -> "Registration failed. Username already registered"))
            }
      	}
      )
  }

   def redirectToRegisterForm =
     (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
      simpleRegisterForm.bindFromRequest.fold(
         errors => {
        BadRequest(views.html.register(registerForm))
         },
         emailInForm => {
            emailInForm match {
               case None => Ok(views.html.register(registerForm))
               case Some(email) =>
                  Ok(views.html.register(
                     registerForm.fill( email, None, email, "", "") ) )
            }
         }
      )
   }

   def showRegisterForm = (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.register(registerForm))
   }
}

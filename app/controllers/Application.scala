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


trait WithAnalytics {

   def configuration: Configuration

   implicit def analyticsDetails: Option[String] = configuration.getString("analytics.id")

}

trait RegisterForm {

	val simpleRegisterForm = Form {
		"email" -> optional(text(maxLength = 99))
 	}

 	val registerForm = Form(
    tuple(
      "username" -> nonEmptyText(maxLength = 99),
      "fullname" -> optional(text(maxLength = 99)),
      "email" -> nonEmptyText(maxLength = 99),
      "password" -> nonEmptyText(minLength = 4, maxLength = 99),
      "confirm" -> nonEmptyText(minLength = 4, maxLength = 99)
    ) verifying("Passwords do not match", fields => fields match {
      case (username, fullname, email, password, confirmPassword) => {
        password.trim == confirmPassword.trim
     }
    })  verifying("Email address is not valid", fields => fields match {
      case (username, fullname, email, password, confirmPassword) => {
         true
      //   RecipientController.ValidEmailAddress.findFirstIn(email.trim).isDefined
      }
    }) verifying("Username is not valid. A to Z and numbers only", fields => fields match {
      case (username, fullname, email, password, confirmPassword) => {
         true
      //   RecipientController.ValidUsername.findFirstIn(username.trim).isDefined
      }
    }) verifying("Username is already taken", fields => fields match {
      case (username, fullname, email, password, confirmPassword) => {
         true
      //   !Recipient.findByUsername(username.trim).isDefined
      }
    })
  )
}

trait LoginForm {

   // def recipientLookup: RecipientLookup

   val ValidUsernamePattern = """^[a-zA-Z0-9\-_]{3,99}$""".r

	val loginForm = Form(
	   tuple(
	      "username" -> nonEmptyText(minLength = 4, maxLength = 99),
	      "password" -> nonEmptyText(minLength = 4, maxLength = 99),
      	"source" -> optional(text)
      ) verifying("Log in failed. Username invalid", fields => fields match {
           case (username, _, _) =>  ValidUsernamePattern.unapplySeq(username).isDefined
      })
	)
}

@Singleton
class Application @Inject() (val configuration: Configuration, val recipientFactory: RecipientFactory, val recipientLookup: RecipientLookup)
extends Controller with Secured with WithAnalytics with WishForm with RegisterForm {

  val contactForm = Form(
    tuple(
      "name" -> nonEmptyText(maxLength = 99),
      "email" -> nonEmptyText(maxLength = 99),
      "username" -> optional(text(maxLength = 99)),
      "subject" -> optional(text(maxLength = 200)),
      "message" -> nonEmptyText(maxLength = 2000)
    ) verifying("Email address is not valid", fields => fields match {
      case (name, email, username, subject, message) => {
         false
      //   RecipientController.ValidEmailAddress.findFirstIn(email.trim).isDefined
      }
    })
  )

   def index = Action.async { implicit request =>
      findCurrentRecipient map { implicit currentRecipient =>
         currentRecipient match {
         case Some(recipient) => {
            Logger.debug("yay already logged in")
            val wishlists: Seq[Wishlist] = Seq.empty // Wishlist.findByRecipient(recipient)
            // Ok(views.html.indexanon()).withSession( request.session )
            Ok(views.html.indexrecipient(
               editWishlistForm, wishlists)).withSession( request.session )
         }
         case None =>
            Logger.debug("not logged in")
            Ok(views.html.indexanon())
      }
   } }

   def redirectToIndex = Action { implicit request =>
      Redirect(routes.Application.index())
   }

   def about = (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.about())
   }

   def contact = (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.contact(contactForm))
   }

   def redirectToContact = Action { implicit request =>
      Redirect(routes.Application.contact())
   }

  def sendContact =  (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
    contactForm.bindFromRequest.fold(
      errors => {
          Logger.warn("Contact failed: " + errors)
          BadRequest(views.html.contact(errors))
      },
      contactFields => {

      //   EmailAl erter.sendContactMessage(contactFields._1, contactFields._2, contactFields._3, contactFields._4, contactFields._5, findCurrentRecipient)

        Redirect(routes.Application.index()).flashing("message"->"Your message was sent")

      }
    )
  }

   def logout = Action {
      Redirect(routes.Application.index).withNewSession.flashing("message"->"You have been logged out")
   }

}


@Singleton
class RegisterController @Inject() (val configuration: Configuration, val recipientFactory: RecipientFactory, val recipientLookup: RecipientLookup)
extends Controller with Secured with WithAnalytics with RegisterForm {

  	def register =
     (UsernameAction andThen MaybeCurrentRecipientAction).async { implicit request =>
  		registerForm.bindFromRequest.fold(
        errors => {
          Logger.warn("Registration failed: " + errors)
          Future.successful(BadRequest(views.html.register(errors)))
        },
   	   registeredForm => {
	      	Logger.info("New registration: " + registeredForm)
            recipientFactory.newRecipient( registeredForm ).save.map {
               case Right(recipient) =>
            //  EmailAlerter.sendNewRegistrationAlert(recipient)

                  if(true){
                     //  if(Recipient.emailVerificationRequired){
                     val verificationHash = "hash"
                     // val verificationHash = recipient.findVerificationHash.getOrElse(recipient.generateVerificationHash)
                     // EmailNotifier.sendEmailVerificationEmail(recipient, verificationHash)
                     Redirect(routes.Application.index()).withNewSession.flashing("messageSuccess"->
                        """
                        Welcome, you have successfully registered.<br/>
                        Please click on the link in the email we just sent to you
                        """)
                  } else {
                     Redirect(routes.Application.index()).withSession(
                        "username" -> registeredForm._1).flashing("messageSuccess"-> "Welcome, you have successfully registered")
                  }
               case Left(e) =>
                  throw new IllegalStateException("Not able to save new registration")
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


@Singleton
class LoginController @Inject() (val configuration: Configuration, val recipientFactory: RecipientFactory, val recipientLookup: RecipientLookup)
extends Controller with Secured with WithAnalytics with LoginForm {

   def redirectToLoginForm = Action { implicit request =>
      Redirect(routes.LoginController.showLoginForm())
   }

   def showLoginForm = (UsernameAction andThen MaybeCurrentRecipientAction) { implicit request =>
      Ok(views.html.login(loginForm))
   }

   def login = (UsernameAction andThen MaybeCurrentRecipientAction).async { implicit request =>

      def badLogin(username: String, errorMessage: String) =
         BadRequest(views.html.login(
               loginForm.fill((username, "", None))))
            .flashing("messageError" -> errorMessage)

      def loginFailed(username: String) =
         Future.successful(
            badLogin(username, "Log in failed. Username does not exist or password is invalid") )

      def notVerified(username: String) =
         badLogin(username, "Log in failed. Email not verified. Please check your email")

      def loginSuccess(username: String) =
         Redirect(routes.Application.index())
            .withSession("username" -> username)
            .flashing("message"->"You have logged in")

      loginForm.bindFromRequest.fold(
         errors => {
            Logger.info("Log in failed:"+ errors)
            Future.successful( BadRequest(views.html.login(errors)) )
         },{
         case (username, password, source) =>
            recipientLookup.findRecipient(username) flatMap {
               case Some(recipient) =>
                  recipient.authenticate(password) flatMap {
                     case true =>
                        recipient.isVerified map {
                           case true  =>
                              Logger.debug("Login success: " + username)
                              loginSuccess(username)
                           case false =>
                              Logger.warn("Login failed. Not verified: " + username)
                              notVerified(username)
                        }
                     case false =>
                        Logger.warn("Login failed. Credentials not correct: " + username)
                        loginFailed(username)
                  }
               case _ =>
                  Logger.warn("Login failed. Recipient not found: " + username)
                  loginFailed(username)
            }
         }
      )
   }
}

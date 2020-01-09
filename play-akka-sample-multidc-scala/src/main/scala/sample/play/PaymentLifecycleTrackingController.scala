package sample.play

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import sample.model.PaymentLifecycle
import sample.play.GatewayInterface.{AuthorizeRequest, _}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentLifecycleTrackingController @Inject()(
    val controllerComponents: ControllerComponents,
    val lifecycleTrackerShardRegion: ActorRef[PaymentLifecycle.Command]
)(implicit scheduler: Scheduler, ec: ExecutionContext)
    extends BaseController {

  implicit val timeout = Timeout(10.seconds)

  def authorize(): Action[AnyContent] = Action.async { implicit request =>
    {
      request.body.asJson
        .flatMap(_.validate[AuthorizeRequest].asOpt.map { cmd  =>

          //lifecycleTrackerShardRegion ! cmd // TODO use ask and map the result.

          val authResult : Future[PaymentLifecycle.SetBalanceResponse] = lifecycleTrackerShardRegion.ask[PaymentLifecycle.SetBalanceResponse]({ ref =>
            PaymentLifecycle.Authorize(cmd.id, cmd.amount, ref)
          }).mapTo[PaymentLifecycle.SetBalanceResponse]

          // TODO: is this a sensible Play future pattern?
         authResult.map { r =>
           r.status match {
             case PaymentLifecycle.ReceivedSuccessfully => Ok(Accepted.toString()) // TODO: Json format?
             case error: PaymentLifecycle.BalanceCommandError => BadRequest(error.message)
           }
         }

        })
        .getOrElse(Future.successful(BadRequest))
    }
  }

//  def settle(): Action[AnyContent] = Action.async { implicit request =>
//    {
//    request.body.asJson
//      .flatMap(_.validate[Settle].asOpt.map { cmd  =>
//
//        lifecycleTrackerShardRegion ! cmd
//
//        Future.successful(Accepted)
//      })
//      .getOrElse(Future.successful(BadRequest))
//    }
//  }
//  def refund(): Action[AnyContent] = Action.async { implicit request =>
//    {
//    request.body.asJson
//      .flatMap(_.validate[Refund].asOpt.map { cmd  =>
//
//        lifecycleTrackerShardRegion ! cmd
//
//        Future.successful(Accepted)
//      })
//      .getOrElse(Future.successful(BadRequest))
//    }
//  }
//  def chargeback(): Action[AnyContent] = Action.async { implicit request =>
//    {
//    request.body.asJson
//      .flatMap(_.validate[Chargeback].asOpt.map { cmd  =>
//
//        lifecycleTrackerShardRegion ! cmd
//
//        Future.successful(Accepted)
//      })
//      .getOrElse(Future.successful(BadRequest))
//    }
//  }

  // TODO : add getBalance for each balance type
}


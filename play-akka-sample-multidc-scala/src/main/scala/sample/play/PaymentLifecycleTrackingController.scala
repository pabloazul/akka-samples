package sample.play

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import javax.inject.{Inject, Singleton}
import play.api.mvc._
import sample.model.PaymentLifecycle._
import sample.model.PaymentLifecycleJson._
import sample.play.GatewayInterface.AuthorizeRequest

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentLifecycleTrackingController @Inject()(
    cc: ControllerComponents,
    val lifecycleTrackerShardRegion: ActorRef[Command]
)(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext)
    extends AbstractController(cc) {

  def authorize(): Action[AnyContent] = Action.async { implicit request =>
    {
      request.body.asJson
        .flatMap(_.validate[AuthorizeRequest].asOpt.map { cmd  =>

          //lifecycleTrackerShardRegion ! cmd // TODO use ask and map the result.

          val authResult : Future[SetBalanceResponse] = lifecycleTrackerShardRegion.ask[SetBalanceResponse]({ ref =>
              Authorize(cmd.id, cmd.amount, ref)
          }).mapTo[SetBalanceResponse]

          // TODO: is this a sensible Play future pattern?
         authResult.map { r =>
           r.status match {
             case ReceivedSuccessfully => Ok(Accepted.toString()) // TODO: Json format?
             case error: BalanceCommandError => BadRequest(error.message)
           }
         }

        })
        .getOrElse(Future.successful(BadRequest))
    }
  }

  def settle(): Action[AnyContent] = Action.async { implicit request =>
    {
    request.body.asJson
      .flatMap(_.validate[Settle].asOpt.map { cmd  =>

        lifecycleTrackerShardRegion ! cmd

        Future.successful(Accepted)
      })
      .getOrElse(Future.successful(BadRequest))
    }
  }
  def refund(): Action[AnyContent] = Action.async { implicit request =>
    {
    request.body.asJson
      .flatMap(_.validate[Refund].asOpt.map { cmd  =>

        lifecycleTrackerShardRegion ! cmd

        Future.successful(Accepted)
      })
      .getOrElse(Future.successful(BadRequest))
    }
  }
  def chargeback(): Action[AnyContent] = Action.async { implicit request =>
    {
    request.body.asJson
      .flatMap(_.validate[Chargeback].asOpt.map { cmd  =>

        lifecycleTrackerShardRegion ! cmd

        Future.successful(Accepted)
      })
      .getOrElse(Future.successful(BadRequest))
    }
  }

  // TODO : add getBalance for each balance type
}


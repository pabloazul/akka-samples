package sample.play

import akka.actor.Scheduler
import akka.util.Timeout
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc._
import play.mvc.Http.HeaderNames
import sample.model.PaymentLifecycle._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentLifecycleTrackingController @Inject()(
    cc: ControllerComponents,
    val lifecycleTrackerShardRegion: akka.actor.ActorRef
)(implicit timeout: Timeout, scheduler: Scheduler, ec: ExecutionContext)
    extends AbstractController(cc) {

  def authorize(): Action[AnyContent] = Action.async { implicit request =>
    {
      request.body.asJson
        .flatMap(_.validate[Authorize].asOpt.map { cmd  =>

          lifecycleTrackerShardRegion ! cmd

          Future.successful(Accepted)
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

}


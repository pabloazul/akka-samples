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

  def authorize(): Action[AnyContent] = Action.async { implicit request => {

    request.body.asJson
      .flatMap(_.validate[AuthorizeRequest].asOpt.map { cmd =>

        val authResult: Future[PaymentLifecycle.SetBalanceResponse] = lifecycleTrackerShardRegion.ask[PaymentLifecycle.SetBalanceResponse]({ ref =>
          PaymentLifecycle.Authorize(cmd.id, cmd.amount, ref)
        }).mapTo[PaymentLifecycle.SetBalanceResponse]

        authResult.map { r =>
          r.status match {
            case success: PaymentLifecycle.ReceivedSuccessfully => Ok(success.toString)
            case error: PaymentLifecycle.BalanceCommandError => BadRequest(error.message)
          }
        }

      })
      .getOrElse(Future.successful(BadRequest))
  }
  }

  def authorizationBalance(): Action[AnyContent] = Action.async { implicit request => {

    request.body.asJson
      .flatMap(_.validate[GetBalances].asOpt.map { cmd =>

        val authorizationAmountQueryResult: Future[PaymentLifecycle.BalanceResponse] = lifecycleTrackerShardRegion.ask[PaymentLifecycle.BalanceResponse]({ ref =>
          PaymentLifecycle.GetAuthorizationBalance(cmd.id, ref)
        }).mapTo[PaymentLifecycle.BalanceResponse]

        authorizationAmountQueryResult.map { ar =>
          Ok(ar.balanceEvents.toString())
        }
      }).getOrElse(Future.successful(BadRequest))
  }
  }

  def settle(): Action[AnyContent] = Action.async { implicit request => {

    request.body.asJson
      .flatMap(_.validate[SettleRequest].asOpt.map { cmd =>

        val authResult: Future[PaymentLifecycle.SetBalanceResponse] = lifecycleTrackerShardRegion.ask[PaymentLifecycle.SetBalanceResponse]({ ref =>
          PaymentLifecycle.Settle(cmd.id, cmd.amount, ref)
        }).mapTo[PaymentLifecycle.SetBalanceResponse]

        authResult.map { r =>
          r.status match {
            case success: PaymentLifecycle.ReceivedSuccessfully => Ok(success.toString)
            case error: PaymentLifecycle.BalanceCommandError => BadRequest(error.message)
          }
        }

      })
      .getOrElse(Future.successful(BadRequest))
  }
  }

  def settleBalance(): Action[AnyContent] = Action.async { implicit request => {

    request.body.asJson
      .flatMap(_.validate[GetBalances].asOpt.map { cmd =>

        val settlementAmountQueryResult: Future[PaymentLifecycle.BalanceResponse] = lifecycleTrackerShardRegion.ask[PaymentLifecycle.BalanceResponse]({ ref =>
          PaymentLifecycle.GetSettledBalance(cmd.id, ref)
        }).mapTo[PaymentLifecycle.BalanceResponse]

        settlementAmountQueryResult.map { ar =>
          Ok(ar.balanceEvents.toString())
        }
      }).getOrElse(Future.successful(BadRequest))
  }
  }

  def refund(): Action[AnyContent] = Action.async { implicit request => {

    request.body.asJson
      .flatMap(_.validate[RefundRequest].asOpt.map { cmd =>

        val authResult: Future[PaymentLifecycle.SetBalanceResponse] = lifecycleTrackerShardRegion.ask[PaymentLifecycle.SetBalanceResponse]({ ref =>
          PaymentLifecycle.Refund(cmd.id, cmd.amount, ref)
        }).mapTo[PaymentLifecycle.SetBalanceResponse]

        authResult.map { r =>
          r.status match {
            case success: PaymentLifecycle.ReceivedSuccessfully => Ok(success.toString)
            case error: PaymentLifecycle.BalanceCommandError => BadRequest(error.message)
          }
        }

      })
      .getOrElse(Future.successful(BadRequest))
  }
  }

  def refundBalance(): Action[AnyContent] = Action.async { implicit request => {

    request.body.asJson
      .flatMap(_.validate[GetBalances].asOpt.map { cmd =>

        val settlementAmountQueryResult: Future[PaymentLifecycle.BalanceResponse] = lifecycleTrackerShardRegion.ask[PaymentLifecycle.BalanceResponse]({ ref =>
          PaymentLifecycle.GetRefundedBalance(cmd.id, ref)
        }).mapTo[PaymentLifecycle.BalanceResponse]

        settlementAmountQueryResult.map { ar =>
          Ok(ar.balanceEvents.toString())
        }
      }).getOrElse(Future.successful(BadRequest))
  }
  }

  def chargeback(): Action[AnyContent] = Action.async { implicit request => {

    request.body.asJson
      .flatMap(_.validate[RefundRequest].asOpt.map { cmd =>

        val authResult: Future[PaymentLifecycle.SetBalanceResponse] = lifecycleTrackerShardRegion.ask[PaymentLifecycle.SetBalanceResponse]({ ref =>
          PaymentLifecycle.Chargeback(cmd.id, cmd.amount, ref)
        }).mapTo[PaymentLifecycle.SetBalanceResponse]

        authResult.map { r =>
          r.status match {
            case success: PaymentLifecycle.ReceivedSuccessfully => Ok(success.toString)
            case error: PaymentLifecycle.BalanceCommandError => BadRequest(error.message)
          }
        }

      })
      .getOrElse(Future.successful(BadRequest))
  }
  }

  def chargebackBalance(): Action[AnyContent] = Action.async { implicit request => {

    request.body.asJson
      .flatMap(_.validate[GetBalances].asOpt.map { cmd =>

        val settlementAmountQueryResult: Future[PaymentLifecycle.BalanceResponse] = lifecycleTrackerShardRegion.ask[PaymentLifecycle.BalanceResponse]({ ref =>
          PaymentLifecycle.GetChargebackBalance(cmd.id, ref)
        }).mapTo[PaymentLifecycle.BalanceResponse]

        settlementAmountQueryResult.map { ar =>
          Ok(ar.balanceEvents.toString())
        }
      }).getOrElse(Future.successful(BadRequest))
  }
  }

  def transactionState(): Action[AnyContent] = Action.async { implicit request => {

    request.body.asJson
      .flatMap(_.validate[GetBalances].asOpt.map { cmd =>

        val stateQueryResult: Future[PaymentLifecycle.TransactionStateReply] = lifecycleTrackerShardRegion.ask[PaymentLifecycle.TransactionStateReply]({ ref =>
          PaymentLifecycle.GetTransactionState(cmd.id, ref)
        }).mapTo[PaymentLifecycle.TransactionStateReply]

        stateQueryResult.map { ar =>
          Ok(s"${ar.id} ${ar.state} ${ar.events}")
        }
      }).getOrElse(Future.successful(BadRequest))
  }
  }
}


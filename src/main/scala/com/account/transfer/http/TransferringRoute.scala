package com.account.transfer.http

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import com.account.transfer.http.model.TransferRequest
import com.account.transfer.repository.model.TransactionStatus
import com.account.transfer.service.TransferringService
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
/**
  * Created by Alexander on 05.08.2017.
  */
class TransferringRoute(transferringService: TransferringService) {

  import FailFastCirceSupport._
  import io.circe.generic.auto._
  import io.circe.syntax._
  import TransactionStatus.encodeTransaction

  val route =
    pathPrefix("plain") {
      path("transfer") {
        post {
          entity(as[TransferRequest]) { request =>
            onComplete(transferringService.transfer(request.accountFrom, request.accountTo, request.amount)) { transactionTry =>
              complete(HttpEntity(ContentTypes.`application/json`,
                transactionTry.map { transaction =>
                  transaction.asJson.noSpaces
                }.getOrElse("")))
            }
          }
        }
      }
    }
}

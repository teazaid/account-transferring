package com.account.transfer.http

import java.time.LocalDateTime

import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import com.account.transfer.http.model.TransferRequest
import com.account.transfer.repository.model.{Transaction, TransactionStatus}
import com.account.transfer.service.TransferringService
import org.scalatest.{Matchers, WordSpec}
import io.circe.syntax._
import io.circe.generic.auto._
import org.mockito.{Mock, Mockito}
import org.mockito.Mockito._

import scala.concurrent.Future
/**
  * Created by Alexander on 05.08.2017.
  */
class TransferringRouteSpec extends WordSpec with Matchers with ScalatestRouteTest {

  private val accountFrom = "accountFrom"
  private val accountTo = "accountFrom"
  private val amount = 5550
  private val transferringServiceMock = mock(classOf[TransferringService])
  private val now = LocalDateTime.now

  when(transferringServiceMock.transfer(accountFrom, accountTo, amount)).thenReturn(
    Future.successful(Transaction(accountFrom,accountTo, amount, TransactionStatus.Success, Some(0), now)))

  private val route = new TransferringRoute(transferringServiceMock).route

  "TransferringRoute" should {
    "make successful transfer" in {
      val transferRequest = ByteString(TransferRequest(accountFrom, accountTo, amount).asJson.noSpaces)

      Post("/plain/transfer",
        HttpEntity(MediaTypes.`application/json`, transferRequest)) ~> route ~> check {
        status.intValue() shouldEqual 200
        responseAs[String] shouldEqual s"""{"from":"accountFrom","to":"accountFrom","amount":5550,"status":"Success","id":0,"created":"${now.toString}"}"""
      }
      verify(transferringServiceMock).transfer(accountFrom, accountTo, amount)
    }
  }
}

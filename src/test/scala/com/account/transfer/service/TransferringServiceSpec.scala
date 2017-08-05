package com.account.transfer.service

import java.time.LocalDateTime

import com.account.transfer.repository.model.{AccountInfo, Transaction, TransactionStatus}
import com.account.transfer.repository.{AccountRepository, TransactionRepository}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import scalikejdbc.{DB, _}
import scalikejdbc.config.DBs
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by Alexander on 05.08.2017.
  */
class TransferringServiceSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  private val accountRepository = new AccountRepository
  private val transactionRepository = new TransactionRepository

  private val timeout = 1.second

  private val accountOne = "abc"
  private val accountTwo = "efg"
  private val nonExistingAccount = s"${accountOne}as"

  private val now = LocalDateTime.now()

  "TransferringService" should {
    s"make successful transfer from ${accountOne} to ${accountTwo}" in {
      val transferringService = new TransferringService(accountRepository, transactionRepository)
      Await.result(transferringService.transfer(accountOne, accountTwo, 500),
        timeout).copy(transactionId = None, created = now) shouldEqual(Transaction(accountOne, accountTwo, 500, TransactionStatus.Success, None, now))

      verifyAmountForAccount(accountOne, 4500)
      verifyAmountForAccount(accountTwo, 5500)
    }

    s"make successful transfer from ${accountOne} to ${accountOne}" in {
      val transferringService = new TransferringService(accountRepository, transactionRepository)
      Await.result(transferringService.transfer(accountOne, accountOne, 500),
        timeout).copy(transactionId = None, created = now) shouldEqual(Transaction(accountOne, accountOne, 500, TransactionStatus.Success, None, now))

      verifyAmountForAccount(accountOne, 4500)
      verifyAmountForAccount(accountTwo, 5500)
    }

    s"make successful concurrent transfer from ${accountOne} to ${accountTwo} and ${accountTwo} to ${accountOne}" in {
      val transferringService = new TransferringService(accountRepository, transactionRepository) {
        override protected def firstProcessing(): Unit = Thread.sleep(timeout.toMillis)
      }

      val transferOneF = transferringService.transfer(accountOne, accountTwo, 1000)
      val transferTwoF = transferringService.transfer(accountTwo, accountOne, 2000)

      val combinedResult = for {
        transferOne <- transferOneF
        transferTwo <- transferTwoF
      } yield (transferOne, transferTwo)

      val (transactionOne, transactionTwo) = Await.result(combinedResult, timeout*3)

      transactionOne.copy(transactionId = None, created = now) shouldEqual(Transaction(accountOne, accountTwo, 1000, TransactionStatus.Success, None, now))
      transactionTwo.copy(transactionId = None, created = now) shouldEqual(Transaction(accountTwo, accountOne, 2000, TransactionStatus.Success, None, now))

      verifyAmountForAccount(accountOne, 5500)
      verifyAmountForAccount(accountTwo, 4500)
    }

    "transfer more funds than present" in {
      val transferringService = new TransferringService(accountRepository, transactionRepository)
      Await.result(transferringService.transfer(accountOne, accountTwo, 6000),
        timeout).copy(transactionId = None, created = now) shouldEqual(Transaction(accountOne, accountTwo, 6000, TransactionStatus.Failure, None, now))

      verifyAmountForAccount(accountOne, 5500)
      verifyAmountForAccount(accountTwo, 4500)
    }

    "transfer to non existing account" in {
      val nonExistingAccount = s"${accountOne}as"
      val transferringService = new TransferringService(accountRepository, transactionRepository)
      Await.result(transferringService.transfer(accountOne, nonExistingAccount, 500),
        timeout).copy(transactionId = None, created = now) shouldEqual(Transaction(accountOne, nonExistingAccount, 500, TransactionStatus.Failure, None, now))

      verifyAmountForAccount(accountOne, 5500)
      verifyAmountForAccount(accountTwo, 4500)
    }

    "transfer from non existing account" in {
      val transferringService = new TransferringService(accountRepository, transactionRepository)
      Await.result(transferringService.transfer(nonExistingAccount, accountOne, 500),
        timeout).copy(transactionId = None, created = now) shouldEqual(Transaction(nonExistingAccount, accountOne, 500, TransactionStatus.Failure, None, now))

      verifyAmountForAccount(accountOne, 5500)
      verifyAmountForAccount(accountTwo, 4500)
    }
  }

  override protected def beforeAll(): Unit = {
    DBs.setupAll()

    AccountRepository.init()

    DB autoCommit { implicit session =>
      sql"insert into accounts(account_number, amount) values ('abc', 5000)".execute().apply()
      sql"insert into accounts(account_number, amount) values ('efg', 5000)".execute().apply()
    }

    TransactionRepository.init()
  }

  override protected def afterAll(): Unit = {
    DBs.closeAll()
  }

  private def verifyAmountForAccount(accountNumber: String, amount: Long): Unit = {
    Await.result(accountRepository.find(accountNumber),
      timeout).get.copy(id = None) shouldEqual(AccountInfo(accountNumber, amount, None))
  }
}

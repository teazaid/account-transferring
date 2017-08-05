package com.account.transfer.repository

import com.account.transfer.repository.model.{Transaction, TransactionStatus}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import scalikejdbc.{DB, _}
import scalikejdbc.config.DBs

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by Alexander on 05.08.2017.
  */
class TransactionRepositorySpec extends WordSpec with Matchers with BeforeAndAfterAll {

  private val transactionRepository = new TransactionRepository()
  private val timeout = 1.second

  "TransactionRepository" should {
    "create transaction and read it" in {
      TransactionStatus.values.map { transactionStatus =>
        val transactionToCreate = Transaction("1", "2", 5000, transactionStatus)

        val result = for {
          createdTransaction <- transactionRepository.insert(transactionToCreate)
          transaction <- transactionRepository.findByTransactionId(createdTransaction.transactionId.get)
        } yield {
          transaction.get.copy(transactionId = None) shouldEqual (transactionToCreate)
        }

        Await.result(result, timeout)
      }
    }

    "find transaction by accountFromId" in {
      val transactions = Await.result(transactionRepository.findByAccountFromId(1), timeout)
      transactions.size shouldEqual(2)
      transactions.collect {
        case t if(t.status != TransactionStatus.Failure) => t.amount
      }.sum shouldEqual(5000)

    }
  }

  override def beforeAll() {
    DBs.setupAll()
    AccountRepository.init()

    DB localTx  { implicit session =>
      sql"insert into accounts(account_number, amount) values ('1', 500);".execute().apply()
      sql"insert into accounts(account_number, amount) values ('2', 500);".execute().apply()
    }

    TransactionRepository.init()
  }

  override def afterAll() {

    TransactionRepository.destroy()
    AccountRepository.destroy()
    DBs.closeAll()
  }
}

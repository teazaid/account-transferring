package com.account.transfer.repository

import com.account.transfer.repository.model.{AccountInfo, Transfer}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import scalikejdbc.config.DBs
import scalikejdbc._

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Alexander on 05.08.2017.
  */
class AccountRepositorySpec extends WordSpec with Matchers with BeforeAndAfterAll {
  private val accountRepository = new AccountRepository()
  private val timeout = 1.second
  private val accountOne = "abc"
  private val accountTwo = "efg"

  "AccountRepository" should {
    "read account info" in {
      val accountInfoOpt = Await.result(accountRepository.find(accountOne), timeout)
      accountInfoOpt.get shouldEqual(AccountInfo(accountOne, 5000, Some(1)))
    }

    "transfer money from A to B accounts" in {
      val transfer = Transfer(accountOne, accountTwo, 500)
      Await.result(accountRepository.transfer(transfer), timeout)

      Await.result(accountRepository.find(accountOne), timeout).get shouldEqual(AccountInfo(accountOne, 4500, Some(1)))
      Await.result(accountRepository.find(accountTwo), timeout).get shouldEqual(AccountInfo(accountTwo, 5500, Some(2)))
    }
  }

  override protected def beforeAll(): Unit = {
    DBs.setupAll()

    AccountRepository.init()

    DB autoCommit { implicit session =>
      sql"insert into accounts(account_number, amount) values ('abc', 5000)".execute().apply()
      sql"insert into accounts(account_number, amount) values ('efg', 5000)".execute().apply()
    }

  }

  override protected def afterAll(): Unit = {
    AccountRepository.destroy()
    DBs.closeAll()
  }
}

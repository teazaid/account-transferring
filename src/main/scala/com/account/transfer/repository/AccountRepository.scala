package com.account.transfer.repository

import com.account.transfer.repository.model.{AccountInfo, Transfer}
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Alexander on 05.08.2017.
  */
class AccountRepository extends LazyLogging {
  def find(accountNumber: String): Future[Option[AccountInfo]] = Future {
    DB autoCommit { implicit session =>
      sql"""
        select * from accounts where account_number = ${accountNumber};
         """.map(transactionsMapper).single().apply()
    }
  }

  def transfer(transfer: Transfer): Future[Unit] = Future {
    DB localTx  { implicit session =>
      sql"update accounts set amount = amount + ${transfer.amount} where account_number = ${transfer.toAccount}".executeUpdate().apply()
      sql"update accounts set amount = amount - ${transfer.amount} where account_number = ${transfer.fromAccount}".executeUpdate().apply()
    }
    ()
  }

  private val transactionsMapper: WrappedResultSet => AccountInfo = {
    rs: WrappedResultSet =>
      AccountInfo(
        rs.string("account_number"),
        rs.long("amount"),
        rs.longOpt("id")
      )
  }
}

object AccountRepository {

  def init(): Unit = {
    DB autoCommit { implicit session =>
      sql"""
         |create table if not exists accounts(
         |id bigint AUTO_INCREMENT,
         |account_number varchar(255) not null,
         |amount bigint default 0 not null,
         |PRIMARY KEY (id));
         """.stripMargin.execute().apply()
    }
  }

  def destroy(): Unit = {
    DB autoCommit { implicit session =>
      sql"drop table accounts;".execute().apply()
    }

  }

}

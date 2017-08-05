package com.account.transfer.repository

import com.account.transfer.repository.model.{Transaction, TransactionStatus}
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.{DB, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Alexander on 05.08.2017.
  */
class TransactionRepository extends LazyLogging {

  def findByTransactionId(transactionId: Long): Future[Option[Transaction]] = Future {
    DB autoCommit { implicit session =>
      sql"""
        select * from transactions where id = ${transactionId};
         """.map(transactionsMapper).single().apply()
    }
  }

  def findByAccountFromId(accountFromId: Long): Future[List[Transaction]] = Future {
    DB autoCommit { implicit session =>
      sql"""
        select * from transactions where account_from = ${accountFromId};
         """.map(transactionsMapper).list().apply()
    }
  }

  def insert(transaction: Transaction): Future[Transaction] = Future {
    val id = DB autoCommit { implicit session =>
      sql"""
        insert into transactions(account_from, account_to, amount, status, created)
           values (${transaction.accountFrom}, ${transaction.accountTo}, ${transaction.amount}, ${transaction.status.toString}, ${transaction.created});
         """.updateAndReturnGeneratedKey().apply()
    }
    transaction.copy(transactionId = Some(id))
  }

  private val transactionsMapper: WrappedResultSet => Transaction = {
    rs: WrappedResultSet =>
      Transaction(
        rs.string("account_from"),
        rs.string("account_to"),
        rs.long("amount"),
        TransactionStatus.withName(rs.string("status")),
        rs.longOpt("id"),
        rs.localDateTime("created")
      )
  }

}

object TransactionRepository {
  def init(): Unit = {
    DB autoCommit { implicit session =>
      sql"""
         |create table if not exists transactions(
         |id bigint AUTO_INCREMENT,
         |account_from varchar(255) not null,
         |account_to varchar(255) not null,
         |amount bigint default 0 not null ,
         |status varchar(255) not null,
         |created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
         |PRIMARY KEY (id));
         """.stripMargin.execute().apply()
    }
  }

  def destroy(): Unit = {
    DB autoCommit { implicit session =>
      sql"drop table transactions;".execute().apply()
    }
  }
}
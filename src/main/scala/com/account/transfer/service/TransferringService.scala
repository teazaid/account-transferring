package com.account.transfer.service

import com.account.transfer.repository.model.{AccountInfo, Transaction, TransactionStatus, Transfer}
import com.account.transfer.repository.{AccountRepository, TransactionRepository}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Alexander on 05.08.2017.
  */
class TransferringService(accountRepository: AccountRepository,
                          transactionRepository: TransactionRepository) extends LazyLogging {

  def transfer(fromAccount: String, toAccount: String, amountToTransfer: Long): Future[Transaction] = {
    val sorted = List(fromAccount, toAccount).sorted
    val first = sorted(0).toLowerCaseIntern
    val second = sorted(1).toLowerCaseIntern

    logger.debug("acquiring lock for first account {}", first)
    first.synchronized {
      logger.debug("acquired lock for first account {}", first)
      firstProcessing()
      logger.debug("acquiring lock for second account {}", second)
      second.synchronized {
        logger.debug("acquired lock for second account {}", second)

        val accountFromDbF = accountRepository.find(fromAccount)
        val accountToDbF = accountRepository.find(toAccount)

        val accountsDbF = for {
          accountFromDb <- accountFromDbF
          accountToDb <- accountToDbF
        } yield (accountFromDb, accountToDb)

        process(accountsDbF, fromAccount, toAccount, amountToTransfer)
      }
    }
  }

  private def process(accountsDbF: Future[(Option[AccountInfo], Option[AccountInfo])],
                      fromAccount: String,
                      toAccount: String,
                      amountToTransfer: Long): Future[Transaction] = {
    accountsDbF.flatMap { case (accountFromOpt, accountToOpt) =>
      val r = for {
        accountFrom <- accountFromOpt if accountFrom.amount >= amountToTransfer
        accountTo <- accountToOpt
      } yield {
        for {
          _ <- accountRepository.transfer(Transfer(accountFrom.accountNumber, accountTo.accountNumber, amountToTransfer))
          t <- transactionRepository.insert(
            Transaction(accountFrom.accountNumber, accountTo.accountNumber, amountToTransfer, TransactionStatus.Success))
        } yield t
      }
      r.getOrElse(transactionRepository.insert(
        Transaction(fromAccount, toAccount, amountToTransfer, TransactionStatus.Failure)))
    }
  }

  protected def firstProcessing(): Unit = {}

  private implicit class StrRith(str: String) {
    def toLowerCaseIntern(): String = {
      str.toLowerCase.intern()
    }
  }

}

package com.account.transfer.repository.model

import java.time.LocalDateTime

import com.account.transfer.repository.model.TransactionStatus.TransactionStatus
import io.circe.Encoder

/**
  * Created by Alexander on 05.08.2017.
  */
case class Transaction(accountFrom: String,
                       accountTo: String,
                       amount: Long,
                       status: TransactionStatus,
                       transactionId: Option[Long] = None,
                       created: LocalDateTime = LocalDateTime.now)

object TransactionStatus extends Enumeration {
  type TransactionStatus = Value
  val Success, Failure = Value

  implicit val encodeTransaction: Encoder[Transaction] =
    Encoder.forProduct6("from", "to", "amount", "status", "id", "created")(tx =>
      (tx.accountFrom, tx.accountTo, tx.amount, tx.status.toString, tx.transactionId, tx.created.toString)
    )
}

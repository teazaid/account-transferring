package com.account.transfer.http.model

/**
  * Created by Alexander on 05.08.2017.
  */
case class TransferRequest(accountFrom: String, accountTo: String, amount: Long)

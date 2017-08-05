import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.account.transfer.http.TransferringRoute
import com.account.transfer.repository.{AccountRepository, TransactionRepository}
import com.account.transfer.service.TransferringService
import scalikejdbc.config.DBs
import scalikejdbc.{DB, _}

import scala.io.StdIn

/**
  * Created by Alexander on 05.08.2017.
  */
object Runner {
  def main(args: Array[String]) {
    DBs.setupAll()

    AccountRepository.init()
    TransactionRepository.init()

    DB autoCommit { implicit session =>
      sql"insert into accounts(account_number, amount) values ('abc', 5000)".execute().apply()
      sql"insert into accounts(account_number, amount) values ('efg', 5000)".execute().apply()
      sql"insert into accounts(account_number, amount) values ('zyx', 5000)".execute().apply()
    }

    val accountRepository = new AccountRepository()
    val transactionRepository = new TransactionRepository()

    val tranferringService = new TransferringService(accountRepository, transactionRepository)

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val transferringRoute = new TransferringRoute(tranferringService).route

    val bindingFuture = Http().bindAndHandle(transferringRoute, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete { _ =>
        DBs.closeAll()
        system.terminate()
      }

  }

}

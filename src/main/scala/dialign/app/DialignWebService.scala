package dialign.app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import spray.json._
import dialign.online.{DialogueHistory, Utterance}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scala.concurrent.ExecutionContextExecutor
import spray.json.RootJsonFormat
import scala.concurrent.Future
import scala.util.{Success, Failure}
import akka.http.scaladsl.model.StatusCodes

// Case classes for JSON serialization
case class DialogueTurn(
  locutor: String,
  utterance: String
)

case class AnalysisRequest(
  dialogue: Seq[DialogueTurn]
)

case class TurnAnalysis(
  locutor: String,
  utterance: String,
  der: Double,
  dser: Double,
  sharedExpressions: Seq[String],
  establishedSharedExpressions: Seq[String],
  selfExpressions: Seq[String]
)

case class AnalysisResponse(
  turns: Seq[TurnAnalysis]
)

// Web service implementation
object DialignWebService extends App with SprayJsonSupport with DefaultJsonProtocol {
  // JSON formats
  implicit val dialogueTurnFormat: RootJsonFormat[DialogueTurn] = jsonFormat2(DialogueTurn)
  implicit val analysisRequestFormat: RootJsonFormat[AnalysisRequest] = jsonFormat1(AnalysisRequest)
  implicit val turnAnalysisFormat: RootJsonFormat[TurnAnalysis] = jsonFormat7(TurnAnalysis)
  implicit val analysisResponseFormat: RootJsonFormat[AnalysisResponse] = jsonFormat1(AnalysisResponse)

  // Updated Actor system setup
  implicit val system: ActorSystem = ActorSystem("dialign-web-service")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val route =
    path("analyze") {
      post {
        entity(as[AnalysisRequest]) { request =>
          println(s"Processing analysis request with ${request.dialogue.length} dialogue turns")
          
          // Convert request dialogue to utterances
          val utterances = request.dialogue.map { turn =>
            Utterance(turn.locutor, turn.utterance)
          }.toIndexedSeq

          // Create dialogue history
          val dialogueHistory = DialogueHistory(utterances)

          // Process analysis in parallel using Future
          val analysisResultsFuture = Future.traverse(utterances) { utterance =>
            Future {
              val scoring = dialogueHistory.score(utterance)
              TurnAnalysis(
                locutor = utterance.locutor,
                utterance = utterance.text,
                der = scoring.der,
                dser = scoring.dser,
                sharedExpressions = scoring.sharedExpressions.map(_.content.mkString(" ")).toSeq,
                establishedSharedExpressions = scoring.establishedSharedExpressions.map(_.content.mkString(" ")).toSeq,
                selfExpressions = scoring.selfExpressions.map(_.content.mkString(" ")).toSeq
              )
            }
          }

          // Complete with the future result
          onComplete(analysisResultsFuture) {
            case Success(results) => complete(AnalysisResponse(results))
            case Failure(ex) => 
              println(s"Analysis failed: ${ex.getMessage}")
              complete(StatusCodes.InternalServerError -> "Analysis failed")
          }
        }
      }
    }

  // Updated server binding
  val bindingFuture = Http()
    .newServerAt("0.0.0.0", 8080)
    .bind(route)

  println(s"Server online at http://0.0.0.0:8080/")
  
  // Keep the server running and handle shutdown
  sys.addShutdownHook {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  // Block until the server is stopped
  scala.io.StdIn.readLine("Press ENTER to stop the server...\n")
  
  // Cleanup
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
} 
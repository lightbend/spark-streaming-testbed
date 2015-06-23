package controllers

import play.api._
import play.api.mvc._
import com.typesafe.spark.testbed.TestPlan
import com.typesafe.spark.testbed.Actors
import play.api.Play.current
import com.typesafe.spark.testbed.DataGeneratorActor

object Application extends Controller {

  val logger: Logger = Logger(this.getClass)

  def index = Action {
    Ok(views.html.index())
  }
  
  def postPlan = Action { request =>
    request.body.asText match {
      case Some(plan) =>
        val testPlan = TestPlan.parse(plan)
        logger.info(s"Test plan received: $testPlan")
        Actors.dataGenerator ! DataGeneratorActor.TestPlanMsg(testPlan)
      case None =>
        logger.info("No plan received")
    }
    Ok("all clear")
  }

}
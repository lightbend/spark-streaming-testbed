package controllers

import play.api._
import play.api.mvc._
import com.typesafe.spark.testbed.TestPlan
import com.typesafe.spark.testbed.Actors
import play.api.Play.current
import com.typesafe.spark.testbed.DataGeneratorActor

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }
  
  def postPlan = Action { request =>
    request.body.asText match {
      case Some(plan) =>
        val testPlan = TestPlan.parse(plan)
        println(testPlan)
        Actors.dataGenerator ! DataGeneratorActor.TestPlanMsg(testPlan)
      case None =>
        println("No plan received")
    }
    Ok("all clear")
  }

}
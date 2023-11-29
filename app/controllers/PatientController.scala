package controllers

import play.api._
import play.api.mvc._

import javax.inject._

@Singleton
class PatientController @Inject()(val controllerComponents: ControllerComponents) extends BaseController{
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.patient())
  }

}
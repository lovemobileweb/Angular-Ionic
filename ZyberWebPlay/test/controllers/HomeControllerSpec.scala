package controllers

import models.extra.SharingSubmission
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import play.api.libs.json.Json
import play.api.test.{WithApplication, _}
import services.TestData
import zyber.server.ZyberTestSession

class HomeControllerSpec extends Specification with TestData with BeforeEach {
  val application =
    new FakeApplication()

  implicit val shareSubmitFormat = Json.writes[SharingSubmission]

  def before = {
  }

//  "HomeController" should {
//    "Do something" in new WithApplication(application) {
//      //TODO make it make some assertions
//    }
//  }
}
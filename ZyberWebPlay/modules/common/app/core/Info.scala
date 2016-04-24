package core

import java.io.{BufferedReader, InputStreamReader}

object Info {
  var info:String = ""
  def init() = {

    try {
      val name: Class[_] = Class.forName("hello.BuildInfo")
      val version = name.getMethod("version")
      val time = name.getMethod("builtAtString")
      info = version.invoke(null) + " - " + time.invoke(null) + " (UTC) "

    } catch {
      case _:Throwable => //Ignored
    }
    try {
      val ps = new ProcessBuilder("git", "describe","--always")
      ps.redirectErrorStream(true)

      val pr = ps.start()

      val in = new BufferedReader(new InputStreamReader(pr.getInputStream))
      val line: String = in.readLine()
      if(line != null && !line.contains("fatal")) {
        info += " Hash - " + line
      }
      pr.waitFor()
    } catch {
      case _:Throwable => //Ignored
    }
  }

}
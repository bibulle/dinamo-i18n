import sbt._
import Keys._
import play.Project._
import com.typesafe.config._

object ApplicationBuild extends Build {

	val conf = ConfigFactory.parseFile(new File("conf/application.conf")).resolve()
	//val conf = play.api.Configuration.load(new File("."))
	val appName    = conf.getString("app.name")
	val appVersion = conf.getString("app.version")

    val appDependencies = Seq(
			javaCore, 
			javaJdbc, 
			javaEbean, 
			anorm ,
  		"com.memetix" % "microsoft-translator-java-api" % "0.6.2",
  		"com.dropbox.core" % "dropbox-core-sdk" % "[1.6,1.7)"
    )

	val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
    )

}

name := "CoapApps"

version := "1.0"

resolvers += "Luebeck repository" at
  "http://maven.itm.uni-luebeck.de/content/repositories/releases"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-log4j12" % "1.6.6",
  "de.uniluebeck.itm" % "ncoap" % "1.0.3"
)
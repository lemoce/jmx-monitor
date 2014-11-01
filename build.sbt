name := "jmx-monitor"

version := "1.0"

scalaVersion := "2.11.1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.6"

unmanagedJars in Compile += file ("/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home/lib/tools.jar")

runMain := "org.cerencio.jmx.Main"
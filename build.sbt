resolvers += Resolver.mavenLocal

libraryDependencies += "org.teavm" % "teavm-tooling" % "0.6.0-SNAPSHOT"
 
lazy val root = (project in file(".")).
  settings(
    name := "sbt-teavm",
    version := "0.1.0",
    organization := "de.tuda.stg",
    sbtPlugin := true,
    scalaVersion := "2.10.6",
    sbtVersion := "0.13.11"
  )

/*
artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.withClassifier(Some("assembly"))
}

addArtifact(artifact in (Compile, assembly), assembly)
*/

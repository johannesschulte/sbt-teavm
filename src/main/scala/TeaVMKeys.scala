package sbtteavm

import sbt._

trait TeaVMKeys {
  lazy val teaVMWasm = taskKey[Unit]("Compiles Wasm Peers to Wasm code")
}

package sbtteavm

import sbt.Keys.{sLog, target, classDirectory, compile, `package`}
import sbt._
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMTargetType;
import java.io.File

import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.ConsoleTeaVMToolLog;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.tooling.builder.InProcessBuildStrategy;
import org.teavm.tooling.builder.ClassLoaderFactory;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets

import org.teavm.tooling.builder.BuildResult;
import java.net.URLClassLoader;

import scala.io.Source
import java.nio.file.Paths 



object TeaVMPlugin extends AutoPlugin {

  override val trigger: PluginTrigger = noTrigger

  override val requires: Plugins = plugins.JvmPlugin

  object autoImport extends TeaVMKeys

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] =Seq(
    teaVMWasm := wasmTask.dependsOn(`package` in Compile).value
  )

  private def recFileList(dir : File) : List[File] = {
    var fileList : List[File]= dir.listFiles.filter(_.isFile).toList
    val dirList : List[File]= dir.listFiles.filter(_.isDirectory).toList
    for (curDir <- dirList) {
      fileList ++= recFileList(curDir)
    }
    fileList
  }

  object LoaderFactory extends ClassLoaderFactory {
    def create(urls: Array[URL], inner: ClassLoader) : ClassLoader = new URLClassLoader(urls, inner)
  }

  private def wasmTask =  Def.task {
    (compile in Compile).value
    val log = sLog.value

    val cd = (classDirectory in Compile).value
    val fileList = recFileList(cd)

    val wasmFiles = fileList.filter(f => f.getPath().endsWith("$Wasm$WasmExported$.class"))

    val logTV : ConsoleTeaVMToolLog = new ConsoleTeaVMToolLog(true);

    if (wasmFiles.length != 0) {
      val cu = wasmFiles.head
      val relPath = cu.getPath().drop(cd.getPath().length+1)
      // 6 for ".class"
      val className = relPath.dropRight(6).replaceAll("/", ".")

      val builder : InProcessBuildStrategy = new InProcessBuildStrategy(LoaderFactory)

      val outFile = className+".wasm"
      builder.init()
      val cpe = new java.util.ArrayList[String]()
      cpe.add(cd.toString())
      builder.setClassPathEntries(cpe);
      builder.setTargetDirectory(cd.toString())
      builder.setIncremental(false)
      builder.setMinifying(true);
      builder.setDebugInformationGenerated(false);
      builder.setHeapSize(8 * 1024 * 1024);
      builder.setLog(logTV);


      builder.setMainClass(className)
      builder.setEntryPointName("main");
      builder.setOptimizationLevel(TeaVMOptimizationLevel.FULL);
      builder.setFastDependencyAnalysis(false);
      builder.setTargetType(TeaVMTargetType.WEBASSEMBLY);
      val result = builder.build();
      
      //TeaVMProblemRenderer.describeProblems(result.getCallGraph(), result.getProblems(), logTV);

      val wasmCode : Array[Byte] = Files.readAllBytes(Paths.get(cd.toString, "classes.wasm"))

      val wasmString : String = wasmCode.mkString(",")

      val wasmRTIn = Source.fromFile(cd.toString + "/client.jsm").getLines.mkString("\n")

      val wasmRTOut  = wasmRTIn.replace("__WASMCODE__", wasmString)+"\n"

      Files.write(Paths.get(cd.toString, "client.jsm"), wasmRTOut.getBytes(StandardCharsets.UTF_8))
    }
  }
}

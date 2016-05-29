
import org.clapper.sbt.editsource.EditSourcePlugin
import org.clapper.sbt.editsource.EditSourcePlugin.autoImport._
import sbt.Keys._
import sbt.{IO, Project}
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.ReleasePlugin.autoImport._

object ReleaseProcess {
  lazy val generateDoc = ReleaseStep(action = st => {
    val proj = Project.extract(st)
    val vcs = proj.get(releaseVcs).getOrElse(sys.error("Aborting release. Working directory is not a repository of a recognized VCS."))
    val ref = proj.get(thisProjectRef)
    val base = vcs.baseDir

    val (cst, _) = proj.runTask(EditSourcePlugin.autoImport.clean in EditSource in ref, st)
    val (nst, files) = proj.runTask(edit in EditSource in ref, cst)
    vcs.add(files.flatMap(IO.relativize(base, _)): _*) !! st.log
    nst
  })

  lazy val steps = Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    generateDoc,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
}
package org.scalaide.core
package hyperlink

import testsetup.SDTTestUtils
import testsetup.TestProjectSetup
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.runtime.NullProgressMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.scalaide.core.internal.hyperlink.ScalaDeclarationHyperlinkComputer
import org.junit.BeforeClass

object HyperlinkDetectorTests extends TestProjectSetup("hyperlinks") with HyperlinkTester {
  @BeforeClass
  def initializeSubProject(): Unit = {
    SDTTestUtils.enableAutoBuild(false) // make sure no auto-building is happening

    object hyperlinksSubProject extends TestProjectSetup("hyperlinks-sub")
    hyperlinksSubProject.project // force initialization of this project
    hyperlinksSubProject.project.underlying.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor)

    val markers = SDTTestUtils.findProblemMarkers(hyperlinksSubProject.compilationUnit("util/Box.scala")).toList
    val errorMessages: List[String] = for (p <- markers) yield p.getAttribute(IMarker.MESSAGE).toString

    assertTrue("No build errors expected: " + errorMessages, errorMessages.isEmpty)

    // since auto-building is off, we need to do this manually
    // and make sure the classpath is up to date
    project.presentationCompiler.askRestart()
  }
}

class HyperlinkDetectorTests {
  import HyperlinkDetectorTests._

  @Test
  def simpleHyperlinks() = FlakyTest.retry("simpleHyperlinks", "no links found for `Tpe`") {
    val oracle = List(
      Link("type scala.Predef.Set"),
      Link("type hyperlinks.SimpleHyperlinking.Tpe"),
      Link("object scala.Array", "method scala.Array.apply"),
      Link("method scala.collection.TraversableOnce.sum"),
      Link("type scala.Predef.String"),
      Link("object scala.Some"),
      Link("class scala.Option"),
      Link("type scala.Predef.String"),
      Link("value hyperlinks.SimpleHyperlinking.arr"))

    loadTestUnit("hyperlinks/SimpleHyperlinking.scala").andCheckAgainst(oracle)
  }

  @Test
  def scalaPackageLinks() = FlakyTest.retry("simpleHyperlinks", "expected 2 link, found 1 expected:<2> but was:<1>") {
    val oracle = List(
        Link("object scala.collection.immutable.List", "method scala.collection.immutable.List.apply"),
        Link("object scala.collection.immutable.List"),
        Link("object scala.collection.Seq", "method scala.collection.generic.GenericCompanion.apply"),
        Link("object scala.collection.Seq"),
        Link("object scala.collection.immutable.Nil"),
        Link("value scalalinks.Foo.xs", "method scala.collection.LinearSeqOptimized.apply")
    )

    loadTestUnit("scalalinks/ScalaListLinks.scala").andCheckAgainst(oracle)
  }

  @Test
  def bug1000560(): Unit = {
    val oracle = List(Link("object bug1000560.Outer"),
                Link("value bug1000560.Outer.bbb"),
                Link("value bug1000560.Outer.a"),
                Link("object bug1000560.Outer")
  )

    loadTestUnit("bug1000560/Test1.scala").andCheckAgainst(oracle)
  }

  @Test @Ignore
  def bug1000560_2(): Unit = {
    val oracle = List(Link("value bug1000560.Test2.foo"),
                      Link("method bug1000560.Foo.bar"))

    loadTestUnit("bug1000560/Test2.scala").andCheckAgainst(oracle)
  }

  @Test
  def test1000656(): Unit = {
    val oracle = List(Link("type util.Box.myInt"), Link("object util.Full", "method util.Full.apply"))
    loadTestUnit("bug1000656/Client.scala").andCheckAgainst(oracle)
  }

  @Test @Ignore("This test is flaky because of askTypeAt's issues with overloading. See SI-7548")
  def testJavaLinks(): Unit = {
    val oracle = List(Link("util.JavaMethods.nArray"),
        Link("util.JavaMethods.nArray"),
        Link("util.JavaMethods.typeparam"),
        Link("util.JavaMethods.typeparam2"))
    loadTestUnit("javalinks/JavaLinks.scala").andCheckAgainst(oracle, checkJavaElements)
  }

  @Test
  def hyperlinkOnList_t1001215(): Unit = {
    val oracle = List(Link("object scala.collection.immutable.List", "method scala.collection.immutable.List.apply"))

    loadTestUnit("t1001215/A.scala").andCheckAgainst(oracle)
  }

  @Ignore("Enable this once Scalac ticket SI-7915 is fixed")
  @Test
  def t1001921(): Unit = {
    val oracle = List(Link("method t1001921.Bar.bar"))

    loadTestUnit("t1001921/Ticket1001921.scala", forceTypeChecking = true).andCheckAgainst(oracle)
  }
}

package speedith.remix

import isabelle.Term._
import speedith.core.lang.Zone
import speedith.remix.Sets._
import speedith.remix.NormalForms._
import speedith.remix.HOLSetConversions._
import speedith.remix.ShadedZoneTranslator._
import scala.collection.mutable
import scala.collection
import scala.collection.JavaConversions._


case class ShadedZoneTranslator(shadingSpecificationTerms: Seq[Term],
                                numberOfSpiders: Int,
                                contourNames: scala.collection.Set[String]) {

  val (shadedZones: Seq[Zone], termsWithoutShading: Seq[Term]) = extractShadedZonesAndInvalidTerms()

  private def extractShadedZonesAndInvalidTerms(): Pair[Seq[Zone], Seq[Term]] = {
    val notSubsetSpecTerms = mutable.ListBuffer[Term]()
    var shadedZones = mutable.ListBuffer[Zone]()
    for (term <- shadingSpecificationTerms) {
      extractShadingSpecificationTerms(term) match {
        case Some((setSpecTerm, spidersSetTerm)) =>
          assertAllSpidersSpecified(spidersSetTerm)
          shadedZones ++= extractShadedZones(setSpecTerm)
        case None =>
          notSubsetSpecTerms += term
      }
    }
    (shadedZones.result(), notSubsetSpecTerms.result())
  }

  private def extractShadedZones(setSpecTerm: Term): Iterable[Zone] = {
    val setSpecFormula = holSetToBooleanFormula(setSpecTerm)
    assertHOLSetSpecificationIsValid(setSpecFormula)
    val setSpecDNF = toDNF(setSpecFormula)
    val disjuncts = extractDistinctDisjuncts(setSpecDNF).map(d => extractDistinctConjuncts(d))
    val nonContradictoryDisjuncts = disjuncts.filter(disjunct => disjunct.forall {
      case Neg(s) => !disjunct.contains(s)
      case _ => true
    })
    nonContradictoryDisjuncts.flatMap(toZones)
  }

  private def toZones(contours: collection.Set[Formula[String]]): Seq[Zone] = {
    val inContours = contours.collect {
      case Atom(contour) => contour
    }
    val outContours = contours.collect {
      case Neg(Atom(contour)) => contour
    }
    allZones(contourNames, inContours, outContours)
  }

  private def allZones(allContours: collection.Set[String],
                       inContours: collection.Set[String],
                       outContours: collection.Set[String]): Seq[Zone] = {
    def allZonesImpl(remainingContours: List[String], refinedInContours: List[String], refinedOutContours: List[String]): List[Zone] = {
      remainingContours match {
        case Nil =>
          List(new Zone(refinedInContours, refinedOutContours))
        case contour :: tailingContours =>
          allZonesImpl(tailingContours, contour :: refinedInContours, refinedOutContours) :::
            allZonesImpl(tailingContours, refinedInContours, contour :: refinedOutContours)
      }
    }
    val missingZones = (allContours -- inContours) -- outContours
    allZonesImpl(missingZones.toList, inContours.toList, outContours.toList)
  }

  private def assertHOLSetSpecificationIsValid(setSpecFormula: Formula[_]): Unit = {
    if (setSpecFormula == null) {
      throw new IllegalArgumentException("The left-hand side of the shading specification must be a HOL set expression.")
    }
  }

  private def assertAllSpidersSpecified(spidersSetTerm: Term): Unit = {
    val spiderIndices = Sets.extractInsertedSetElements(spidersSetTerm).map {
      case Bound(index) => index
      case _ => throw new IllegalArgumentException("Shaded zone specification contains non-spider elements.")
    }.toSet
    val allSpidersFound = (0 until numberOfSpiders).forall(spiderIndices.contains)
    if (!allSpidersFound) {
      throw new IllegalArgumentException("Not all spiders were specified in the shaded zone specification.")
    }
  }
}

object ShadedZoneTranslator {

  private def extractShadingSpecificationTerms(shadingSpecificationTerm: Term): Option[(Term, Term)] = {
    shadingSpecificationTerm match {
      case App(App(HOL_SUBSET_CONST, set), spiders) =>
        Some((set, spiders))
      case _ =>
        None
    }
  }
}

package speedith.remix

import isabelle.Term._
import speedith.core.lang._
import speedith.core.lang.reader.ReadingException
import speedith.core.lang.Operator
import speedith.core.lang.Operator._
import speedith.remix.TermUtils._
import speedith.remix.NormalForms._
import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import scala.collection.JavaConversions._

object Translations {

  /**
   * Takes an Isabelle term and tries to translate it to a spider diagram.
   *
   * @throws an exception is thrown if the translation fails for any reason.
   */
  @throws(classOf[ReadingException])
  def termToSpiderDiagram(t: Term): SpiderDiagram = recognise(t, null)._1

  /**
   * Takes an Isabelle term and tries to translate it to a spider diagram.
   *
   * <p>The premises must be `Trueprop`s.</p>
   *
   * @throws an exception is thrown if the translation fails for any reason.
   */
  @throws(classOf[ReadingException])
  def termToSpiderDiagram(premises: java.util.List[Term], spiders: java.util.List[Free]): SpiderDiagram = {
    if (premises == null || premises.size() == 0) {
      throw new ReadingException("The list of premises must not be empty.")
    }
    val premisesHOL = extractHOLPremises(premises)
    // Handle the case where each premise is a PSD itself (just try each
    // premise) and then conjunctively connect these primary spider diagrams
    // with the rest of the premises.
    val sd = sdFromConjuncts(premisesHOL)
    if (premisesHOL == null || premisesHOL.length < 1)
      sd
    else {
      // There are still some premises left. Extract a spider diagram from them:
      val psd = if (spiders == null || spiders.size() == 0) {
        convertToPSD(ArrayBuffer[Free](), null, premisesHOL)._1
      } else {
        convertToPSD(spiders.toIterable.toIndexedSeq, spiders.get(0).typ, premisesHOL)._1
      }
      if (psd == null)
        null
      else if (sd != null)
        SpiderDiagrams.createCompoundSD(Operator.Conjunction, sd, psd)
      else
        psd
    }
  }


  private def extractHOLPremises(premises: java.util.List[Term]): mutable.Buffer[Term] = {
    premises.map {
      case App(Const(HOL_TRUEPROP, typ), body) => body
      case t => throw new ReadingException("The list of premises contains a term that is not a Trueprop: '%s;.".format(t.toString))
    }
  }

  private type RecogniserIn = ( /*term:*/ Term, /*spiderType:*/ Typ)
  private type RecogniserOut = (SpiderDiagram, /*spiderType:*/ Typ)
  private type Recogniser = PartialFunction[RecogniserIn, RecogniserOut]

  private val recogniseBinaryHOLOperator: Recogniser = {
    case (App(App(Const(operator, typ), lhs), rhs), spiderType) if BinaryOperators contains operator =>
      // If this recognises the meta implications, then it will be without
      // quantified spiders. Therefore the LHS and the RHS must be just normal
      // spider diagrams in the SNF form.
      val (lhsSD, spiderType1) = recognise(lhs, spiderType)
      val (rhsSD, spiderType2) = recognise(rhs, spiderType1)
      (SpiderDiagrams.createCompoundSD(operatorsIsaToSD(operator), lhsSD, rhsSD), spiderType2)
  }

  private val recogniseNegation: Recogniser = {
    case (App(Const(HOL_NOT, typ), body), spiderType) =>
      val (negSD, spiderType1) = recognise(body, spiderType)
      (SpiderDiagrams.createCompoundSD(Negation, negSD), spiderType1)
  }

  private val recogniseExistential: Recogniser = {
    case (term@App(Const(HOL_EXISTENTIAL, typ), Abs(spider, spiderType1, body)), spiderType) =>
      if (!checkSpiderType(spiderType1, spiderType)) throw new ReadingException("Not all spiders are of the same type.")

      // Extract all spiders and the conjuncts of the body:
      val spiders = ArrayBuffer[Free]()
      val conjuncts = ArrayBuffer[Term]()
      extractConjuncts(extractSpidersAndBody(term, spiders), conjuncts)

      // Make sure that all spiders have the same type:
      if (!spiders.forall {
        spider => checkSpiderType(spider.typ, spiderType1)
      }) throw new ReadingException("Not all spiders are of the same type.")

      // Now extract the unitary spider diagram out of the data:
      convertToPSD(spiders, spiderType1, conjuncts)
  }

  private val recogniseTrueprop: Recogniser = {
    case (App(Const(HOL_TRUEPROP, typ), body), spiderType) => recognise(body, spiderType)
  }

  private val recogniseMetaAll: Recogniser = {
    case (term@App(Const(ISA_META_ALL, typ), Abs(spider, spiderType1, body)), spiderType) =>
      if (!checkSpiderType(spiderType1, spiderType)) throw new ReadingException("Not all spiders are of the same type.")

      // We have to extract all quantified spiders.
      val spiders = ArrayBuffer[Free]()
      val body = findQuantifiedVarsAndBody(term, spiders)
      // Make sure that all spiders have the same type:
      if (!spiders.forall {
        spiders => checkSpiderType(spiders.typ, spiderType1)
      }) throw new ReadingException("Not all spiders are of the same type.")

      // Now find all premises and the conclusion:
      val premises = ArrayBuffer[Term]()
      val conclusion = findPremisesAndConclusion(body, premises)

      // Now extract all conjuncts from the premises:
      val conjuncts = ArrayBuffer[Term]()
      extractConjuncts(premises, conjuncts)

      // We've got enough info to extract a spider diagram from the premises:
      val (lhsSD, spiderType2) = convertToPSD(spiders, spiderType1, conjuncts)
      val (rhsSD, spiderType3) = recognise(conclusion, spiderType2)

      (SpiderDiagrams.createCompoundSD(Implication, lhsSD, rhsSD), spiderType3)
  }

  private val recognise: Recogniser = {
    case x => (recogniseBinaryHOLOperator
      orElse recogniseMetaAll
      orElse recogniseTrueprop
      orElse recogniseExistential
      orElse recogniseNegation
      orElse {
      case _ => throw new ReadingException("Not an SNF formula. Found an unknown term '%s'.".format(x))
    }: Recogniser)(x)
  }

  private val BinaryOperators = Set(HOL_CONJUNCTION, HOL_DISJUNCTION, HOL_IMPLICATION, HOL_EQUALITY, ISA_META_IMPLICATION)
  private val HOLListDistinct = "List.distinct"
  private val HOLSetMember = "Set.member"

  /**
   * Extracts conjunctively connected spider diagrams from the list of premises.
   */
  private def sdFromConjuncts(conjuncts: mutable.Buffer[Term]): SpiderDiagram = {
    if (conjuncts == null || conjuncts.isEmpty)
      return null
    var typ: Typ = null
    var sds: SpiderDiagram = null
    for (i <- (conjuncts.length - 1) to 0 by -1) {
      try {
        val (sd, typTmp) = recognise(conjuncts(i), typ)
        typ = typTmp
        if (sds == null) sds = sd
        else sds = SpiderDiagrams.createCompoundSD(Operator.Conjunction, sd, sds)
        conjuncts.remove(i)
      } catch {
        case e: Throwable => println(e)
      }
    }
    sds
  }

  private def getAndRemove[A, T <: A, B](buffer: mutable.Buffer[T], filter: A => Option[B]): ArrayBuffer[B] = {
    val retVal = ArrayBuffer[B]()
    var i = 0
    while (i < buffer.length) {
      filter(buffer(i)) match {
        case Some(x) =>
          buffer.remove(i)
          retVal += x
        case None =>
          i = i + 1
      }
    }
    retVal
  }

  private def rlRemoveWhere[A, T <: A](buffer: mutable.Buffer[T], filter: A => Boolean): Unit = {
    var i = buffer.length - 1
    while (i >= 0) {
      if (filter(buffer(i))) {
        buffer.remove(i)
      }
      i = i - 1
    }
  }

  private def operatorsIsaToSD = HashMap(
    HOL_CONJUNCTION -> Conjunction,
    HOL_DISJUNCTION -> Disjunction,
    HOL_IMPLICATION -> Implication,
    HOL_EQUALITY -> Equivalence,
    ISA_META_IMPLICATION -> Implication,
    HOL_NOT -> Negation)

  private def extractConjuncts(term: Term, conjuncts: mutable.Buffer[Term]): Unit = {
    term match {
      case App(App(Const(HOL_CONJUNCTION, _), lhs), rhs) =>
        extractConjuncts(lhs, conjuncts)
        extractConjuncts(rhs, conjuncts)
      case App(Const(HOL_TRUEPROP, typ), body) => extractConjuncts(body, conjuncts)
      case x => conjuncts += x
    }
  }

  private def extractConjuncts(terms: Traversable[Term], conjuncts: mutable.Buffer[Term]): Unit = {
    terms foreach {
      term => extractConjuncts(term, conjuncts)
    }
  }

  /**
   * Extracts HOL existentially quantified variables from the term, puts them
   * into the given array list packed as separate `Free`s, and returns the
   * body.
   */
  private def extractSpidersAndBody(t: Term, variables: mutable.Buffer[Free]): Term = {
    t match {
      case App(Const(HOL_EXISTENTIAL, _), Abs(varName, varType, body)) =>
        variables += Free(varName, varType)
        extractSpidersAndBody(body, variables)
      case _ => t
    }
  }

  private def checkSpiderType(newSpiderType: Typ, oldSpiderType: Typ): Boolean = {
    if (oldSpiderType == null) true else oldSpiderType.equals(newSpiderType)
  }

  private def extractDistinctTerm(conjuncts: mutable.Buffer[Term]): App = {
    val distinctTerms = getAndRemove(conjuncts, (t: Term) => t match {
      case x@App(Const(HOLListDistinct, _), _) => Some(x)
      case _ => None
    })
    if (distinctTerms.length > 1) throw new ReadingException("More than one 'distinct' term found in a unitary SNF formula.")
    else if (distinctTerms.length == 1) {
      // We've got the single distinct term, return it:
      return distinctTerms(0)
    }
    null
  }

  private def extractSpiderInequalities(conjuncts: mutable.Buffer[Term], spiders: IndexedSeq[Free]): Boolean = {
    if (spiders != null && spiders.length > 1) {
      val inequalities = new Array[Boolean](spiders.length * spiders.length)
      val spiderType = spiders(0).typ
      rlRemoveWhere(conjuncts, (t: Term) => t match {
        case App(Const(HOL_NOT, _), App(App(Const(HOL_EQUALITY, Type(_, List(spiderType1, Type(_, List(spiderType2, _))))), Bound(spider1)), Bound(spider2))) if spiderType1.equals(spiderType2) && checkSpiderType(spiderType1, spiderType) =>
          inequalities(scala.math.min(spider1, spider2) * spiders.length + scala.math.max(spider1, spider2)) = true
          true
        case _ => false
      })
      for (i <- 0 to spiders.length - 1) {
        for (j <- i + 1 to spiders.length - 1) {
          if (!inequalities(i * spiders.length + j)) return false
        }
      }
      true
    } else false
  }

  private def checkSpiderInequalities(spiders: IndexedSeq[Free], conjuncts: mutable.Buffer[Term]): Unit = {
    if (spiders != null && spiders.length > 1) {
      // We need spider inequalities only if there are at least two spiders...
      // Is there a distinct term present?
      val inequalitiesPresent = extractSpiderInequalities(conjuncts, spiders)
      // If the user specified inequalities, then that's fine. But if
      // inequalities are not specified, then let's look for the distinct clause:
      if (!inequalitiesPresent) {
        val distinctTerm: App = extractDistinctTerm(conjuncts)
        if (distinctTerm == null) {
          // There is no `distinct` term and no equalities. This is not allowed:
          throw new ReadingException("Did not find a 'distinct' assertion. This is needed to tell that spiders are distinct elements.")
        } else {
          // If there is a `distinct` term, check that it contains all spiders:
          val distinctSpiders = TermUtils.getListElements(distinctTerm.arg).map {
            case Bound(i) => spiders(spiders.length - i - 1)
            case _ => throw new ReadingException("The 'distinct' term does not contain only spiders.")
          }.toSet
          if (spiders exists (s => !distinctSpiders.contains(s))) throw new ReadingException("The 'distinct' assertion does not contain all spiders.")
          // Okay, all spiders are distinct...
        }
      }
    }
  }

  private def findContoursInTerm(term: Term, spiderType: Typ, outContours: mutable.HashSet[Free] = mutable.HashSet[Free]()): Typ = {
    term match {
      case t@Free(predicateName, Type(fun, List(spiderType1))) =>
        if (!checkSpiderType(spiderType1, spiderType)) throw new ReadingException("A contour's type '%s' does not match the spider's type '%s'.".format(spiderType1, spiderType))
        outContours += t
        spiderType1
      case Abs(_, _, body) => findContoursInTerm(body, spiderType, outContours)
      case App(lhs, rhs) =>
        findContours(List(lhs, rhs), spiderType, outContours)._2
      case _ => spiderType
    }
  }

  private def findContours(conjuncts: Seq[Term], spiderType: Typ, outContours: mutable.HashSet[Free] = mutable.HashSet[Free]()): (mutable.HashSet[Free], Typ) = {
    var spiderType1 = spiderType
    for (t <- conjuncts) {
      spiderType1 = findContoursInTerm(t, spiderType1, outContours)
    }
    (outContours, spiderType)
  }

  /**
   * Returns the spider at the 'from-behind' index (i.e.: index `0` maps to
   * `length - 1` and index `length - 1` maps to `0`).
   */
  private def getSpiderWithBoundIndex(boundIndex: Int, spiders: IndexedSeq[Free]): Free = spiders(spiders.length - 1 - boundIndex)

  private def addSubsetsFromTo[A](fromSet: Seq[A], mustContain: mutable.HashSet[A], toSet: mutable.HashSet[mutable.HashSet[A]], out: mutable.HashSet[A] = mutable.HashSet[A](), startIndex: Int = 0): Unit = {
    var i = startIndex
    toSet += mustContain ++ out
    while (i < fromSet.length) {
      out += fromSet(i)
      addSubsetsFromTo(fromSet, mustContain, toSet, out, i + 1)
      out -= fromSet(i)
      i = i + 1
    }
  }

  private def fromInZonesToSDRegion(inZones: mutable.HashSet[mutable.HashSet[Free]], contours: mutable.HashSet[Free]): Region = {
    val zones = new java.util.TreeSet[Zone]()
    for (z <- inZones) {
      val inContours = new java.util.TreeSet[String]()
      val outContours = new java.util.TreeSet[String]()
      z.foreach(contour => inContours.add(contour.name))
      contours.foreach(contour => if (!z.contains(contour)) outContours.add(contour.name))
      zones.add(new Zone(inContours, outContours))
    }
    new Region(zones)
  }

  private def habitatSpecificationTermToFormula(spiderIndex: Int, term: Term): Formula[Free] = {
    term match {
      case App(App(Const(HOL_CONJUNCTION, _), lhs), rhs) =>
        val flhs = habitatSpecificationTermToFormula(spiderIndex, lhs)
        if (flhs == null) return null
        val frhs = habitatSpecificationTermToFormula(spiderIndex, rhs)
        if (frhs == null) return null
        Inf(flhs, frhs)
      case App(App(Const(HOL_DISJUNCTION, _), lhs), rhs) =>
        val flhs = habitatSpecificationTermToFormula(spiderIndex, lhs)
        if (flhs == null) return null
        val frhs = habitatSpecificationTermToFormula(spiderIndex, rhs)
        if (frhs == null) return null
        Sup(flhs, frhs)
      case App(Const(HOL_NOT, _), region) =>
        val f = habitatSpecificationTermToFormula(spiderIndex, region)
        if (f == null) null else Neg(f)
      case App(App(Const(HOLSetMember, _), Bound(boundIndex)), region) if boundIndex == spiderIndex =>
        HOLSetConversions.holSetSpecToFormula(region)
      case _ => null
    }
  }

  private def isaHabitatSpecifiersToFormula(spiderIndex: Int, conjuncts: mutable.Buffer[Term]): Formula[Free] = {
    var formulae = ArrayBuffer[Formula[Free]]()
    var i = conjuncts.length - 1
    while (i >= 0) {
      val f = habitatSpecificationTermToFormula(spiderIndex, conjuncts(i))
      if (f != null) {
        formulae += f
        conjuncts.remove(i)
      }
      i = i - 1
    }
    if (formulae.length == 0) null else toConjuncts(formulae, (f: Formula[Free]) => f)
  }

  private def extractHabitats(conjuncts: mutable.Buffer[Term], spiders: IndexedSeq[Free], contours: mutable.HashSet[Free], spiderType: Typ, habitats: java.util.HashMap[String, Region] = new java.util.HashMap()): (java.util.HashMap[String, Region], Typ) = {
    // For each spider, find all the terms that talk about its set membership:
    for (spiderIndex <- 0 to spiders.length - 1) {
      // This set will contain all zones of this spider's habitat:
      val inZones = new mutable.HashSet[mutable.HashSet[Free]]()
      // First fetch all the 'habitat-specifying' terms for this spider and
      // convert them into a 'set-lattice' formula, which we will normalise:
      val habitatTerms = isaHabitatSpecifiersToFormula(spiderIndex, conjuncts)
      // If there are no habitat-specifying terms, then the spider can live anywhere:
      if (habitatTerms == null) {
        addSubsetsFromTo(contours.toSeq, mutable.HashSet.empty, inZones)
      } else {
        // There are some habitat-specifying terms. Calculate the disjunctive
        // normal form of the habitat-specifying formula (this makes it then
        // easy to find all zones of the habitat):
        val disjuncts = extractDistinctDisjuncts(toDNF(habitatTerms)).map(d => extractDistinctConjuncts(d))
        // Remove all self-contradicting disjuncts:
        disjuncts.retain(d => d.forall {
          case Neg(s) => !d.contains(s)
          case _ => true
        })

        // A disjunct may not be fully specified (which means that in each clause
        // some contours might be missing). In this case, we have to calculate
        // for each clause all its fully-specified zones that are its subsets:
        for (d <- disjuncts) {
          // We calculate the fully-specified zones in the following way:
          //	-	if 'specified' contains all contours that are mentioned in the clause 'd',
          //		'positive' is the set of all positive contour literals in
          //		'd', and 'other' are all contours not mentioned in 'd',
          //		then '{x in P(other) | positive union x}' is the set of all
          //		sub zones of the region specified in clause 'd':
          val positive = d.collect {
            case Atom(s) => s
          }
          val specified = d.map {
            case Atom(s) => s
            case Neg(Atom(s)) => s
            case _ => throw new RuntimeException("Found an unknown term in a set which should contain only contour literals.")
          }
          val other = contours.filter(a => !specified.contains(a))
          addSubsetsFromTo(other.toBuffer, positive, inZones)
        }
      }
      //println("Habitat of spider %s: %s".format(getSpiderWithBoundIndex(spiderIndex, spiders).name, inZones.map(a => a.map(b => b.name))));
      habitats.put(getSpiderWithBoundIndex(spiderIndex, spiders).name, fromInZonesToSDRegion(inZones, contours))
    }
    (habitats, spiderType)
  }

  private def convertToPSD(spiders: IndexedSeq[Free], spiderType: Typ, conjuncts: mutable.Buffer[Term]): (PrimarySpiderDiagram, Typ) = {
    checkSpiderInequalities(spiders, conjuncts)

    // Get all contour names mentioned in this unitary spider diagram, the
    // predicates are of this form:
    //		Free(B,Type(fun,List(<spiderType>, Type(HOL.bool,List()))))
    val (contours, spiderType1) = findContours(conjuncts, spiderType)

    val (habitats, _) = extractHabitats(conjuncts, spiders, contours, spiderType1)

    val shadedZoneTranslator = ShadedZoneTranslator(conjuncts, spiders.length, contours.map(_.name))
    val shadedZones = shadedZoneTranslator.shadedZones
    val remainingConjunctsAfterShading = shadedZoneTranslator.termsWithoutShading

    assertNoExtraneousTerms(remainingConjunctsAfterShading)

    Pair(
      SpiderDiagrams.createPrimarySD(
        spiders.map(_.name),
        habitats,
        shadedZones,
        null
      ),
      spiderType1
    )
  }

  private def assertNoExtraneousTerms(remainingConjunctsAfterShading: Seq[Term]) {
    if (remainingConjunctsAfterShading.length != 0) {
      throw new ReadingException("The formula is not in the SNF form. There is an unknown term in the specification of a unitary spider diagram: %s".format(remainingConjunctsAfterShading.head))
    }
  }
}

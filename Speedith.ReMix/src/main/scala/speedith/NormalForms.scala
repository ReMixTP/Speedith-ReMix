package speedith.remix

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

object NormalForms {

  def toConjuncts[A, B](conjuncts: Seq[B], converter: B => Formula[A]): Formula[A] = {
    if (conjuncts == null || conjuncts.length < 1) {
      null
    } else {
      ((null: Formula[A]) /: conjuncts)((x, y) => if (x == null) converter(y) else Inf(x, converter(y)))
    }
  }

  def toNNF[A](formula: Formula[A]): Formula[A] = {
    formula match {
      case Inf(lhs, rhs) => Inf(toNNF(lhs), toNNF(rhs))
      case Sup(lhs, rhs) => Sup(toNNF(lhs), toNNF(rhs))
      case Neg(Inf(lhs, rhs)) => Sup(toNNF(Neg(lhs)), toNNF(Neg(rhs)))
      case Neg(Sup(lhs, rhs)) => Inf(toNNF(Neg(lhs)), toNNF(Neg(rhs)))
      case Neg(Neg(body)) => toNNF(body)
      case _ => formula
    }
  }

  def toCNF[A](formula: Formula[A]): Formula[A] = {
    val nnf = toNNF(formula)
    def toCNFImpl(nnfFormula: Formula[A]): Formula[A] = {
      nnfFormula match {
        case Sup(Inf(a, b), Inf(c, d)) => Inf(Inf(toCNFImpl(Sup(a, c)), toCNFImpl(Sup(a, d))), Inf(toCNFImpl(Sup(b, c)), toCNFImpl(Sup(b, d))))
        case Sup(a, Inf(b, c)) => Inf(toCNFImpl(Sup(a, b)), toCNFImpl(Sup(a, c)))
        case Sup(Inf(b, c), a) => Inf(toCNFImpl(Sup(b, a)), toCNFImpl(Sup(c, a)))
        case t@Sup(a, b) if !t.isCNF => toCNFImpl(Sup(toCNFImpl(a), toCNFImpl(b)))
        case t@Inf(a, b) if !t.isCNF => Inf(toCNFImpl(a), toCNFImpl(b))
        case x => x
      }
    }
    toCNFImpl(nnf)
  }

  def toDNF[A](formula: Formula[A]): Formula[A] = {
    val nnf = toNNF(formula)
    def toDNFImpl(nnfFormula: Formula[A]): Formula[A] = {
      nnfFormula match {
        case Inf(Sup(a, b), Sup(c, d)) => Sup(Sup(toDNFImpl(Inf(a, c)), toDNFImpl(Inf(a, d))), Sup(toDNFImpl(Inf(b, c)), toDNFImpl(Inf(b, d))))
        case Inf(a, Sup(b, c)) => Sup(toDNFImpl(Inf(a, b)), toDNFImpl(Inf(a, c)))
        case Inf(Sup(b, c), a) => Sup(toDNFImpl(Inf(b, a)), toDNFImpl(Inf(c, a)))
        case t@Inf(a, b) if !t.isDNF => toDNFImpl(Inf(toDNFImpl(a), toDNFImpl(b)))
        case t@Sup(a, b) if !t.isDNF => Sup(toDNFImpl(a), toDNFImpl(b))
        case x => x
      }
    }
    toDNFImpl(nnf)
  }

  def extractDistinctDisjuncts[A](formula: Formula[A], extractedDisjuncts: mutable.HashSet[Formula[A]] = mutable.HashSet[Formula[A]]()): mutable.HashSet[Formula[A]] = {
    formula match {
      case Sup(Sup(a, b), c) =>
        extractDistinctDisjuncts(a, extractedDisjuncts)
        extractDistinctDisjuncts(b, extractedDisjuncts)
        extractDistinctDisjuncts(c, extractedDisjuncts)
      case Sup(a, Sup(b, c)) =>
        extractDistinctDisjuncts(a, extractedDisjuncts)
        extractDistinctDisjuncts(b, extractedDisjuncts)
        extractDistinctDisjuncts(c, extractedDisjuncts)
      case Sup(lhs, rhs) =>
        extractedDisjuncts += lhs
        extractedDisjuncts += rhs
      case x =>
        extractedDisjuncts += x
    }
    extractedDisjuncts
  }

  def extractDisjuncts[A](formula: Formula[A], terms: ArrayBuffer[Formula[A]] = ArrayBuffer[Formula[A]]()): ArrayBuffer[Formula[A]] = {
    formula match {
      case Sup(Sup(a, b), c) =>
        extractDisjuncts(a, terms)
        extractDisjuncts(b, terms)
        extractDisjuncts(c, terms)
      case Sup(a, Sup(b, c)) =>
        extractDisjuncts(a, terms)
        extractDisjuncts(b, terms)
        extractDisjuncts(c, terms)
      case Sup(lhs, rhs) =>
        terms += lhs
        terms += rhs
      case x =>
        terms += x
    }
    terms
  }

  def extractDistinctConjuncts[A](formula: Formula[A], terms: mutable.HashSet[Formula[A]] = mutable.HashSet[Formula[A]]()): mutable.HashSet[Formula[A]] = {
    formula match {
      case Inf(Inf(a, b), c) =>
        extractDistinctConjuncts(a, terms)
        extractDistinctConjuncts(b, terms)
        extractDistinctConjuncts(c, terms)
      case Inf(a, Inf(b, c)) =>
        extractDistinctConjuncts(a, terms)
        extractDistinctConjuncts(b, terms)
        extractDistinctConjuncts(c, terms)
      case Inf(lhs, rhs) =>
        terms += lhs
        terms += rhs
      case x =>
        terms += x
    }
    terms
  }

  def extractConjuncts[A](formula: Formula[A], terms: ArrayBuffer[Formula[A]] = ArrayBuffer[Formula[A]]()): ArrayBuffer[Formula[A]] = {
    formula match {
      case Inf(Inf(a, b), c) =>
        extractConjuncts(a, terms)
        extractConjuncts(b, terms)
        extractConjuncts(c, terms)
      case Inf(a, Inf(b, c)) =>
        extractConjuncts(a, terms)
        extractConjuncts(b, terms)
        extractConjuncts(c, terms)
      case Inf(lhs, rhs) =>
        terms += lhs
        terms += rhs
      case x =>
        terms += x
    }
    terms
  }

}

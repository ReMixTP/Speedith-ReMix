package speedith.remix

sealed abstract class Formula[+A] {

  final def +[B >: A](that: Formula[B]): Formula[B] = Sup(this, that)

  final def *[B >: A](that: Formula[B]): Formula[B] = Inf(this, that)

  final def unary_-(): Formula[A] = Neg(this)

  final def unary_!(): Formula[A] = Neg(this)

  final def ∧[B >: A](that: Formula[B]): Formula[B] = this * that

  final def ∨[B >: A](that: Formula[B]): Formula[B] = this + that

  final def &&[B >: A](that: Formula[B]): Formula[B] = this * that

  final def ||[B >: A](that: Formula[B]): Formula[B] = this + that

  final def &[B >: A](that: Formula[B]): Formula[B] = this * that

  final def |[B >: A](that: Formula[B]): Formula[B] = this + that

  def isLiteral: Boolean = false

  def isCNF: Boolean

  def isDNF: Boolean

  def isNNF: Boolean
}

object Formula {
  implicit def string2atom(str: String): Formula[String] = Atom(str)

  implicit def symbol2atom(atomSymbol: Symbol): Formula[Symbol] = Atom(atomSymbol)
}


case class Atom[+A](atom: A) extends Formula[A] {
  override def toString: String = atom.toString

  override def isLiteral: Boolean = true

  val isCNF: Boolean = true

  val isDNF: Boolean = true

  val isNNF: Boolean = true
}

case class Inf[+A](lhs: Formula[A], rhs: Formula[A]) extends Formula[A] {

  /**
   * This formula is in CNF if and only if its children are in CNF.
   */
  def isCNF: Boolean = {
    lhs.isCNF && rhs.isCNF
  }

  /**
   * This formula is in DNF if and only if it contains no Sup(_, _) children
   * and all children must be in DNF.
   */
  def isDNF: Boolean = this match {
    case Inf(Sup(_, _), _) | Inf(_, Sup(_, _)) => false
    case _ => lhs.isDNF && rhs.isDNF
  }

  def isNNF: Boolean = {
    lhs.isNNF && rhs.isNNF
  }

  override def toString: String = {
    parenthesize(lhs) + " \u2227 " + parenthesize(rhs)
  }

  private def parenthesize(nestedTerm: Formula[_]): String = {
    nestedTerm match {
      case Sup(_, _) => "(" + nestedTerm + ")"
      case _ => nestedTerm.toString
    }
  }
}

case class Neg[+A](body: Formula[A]) extends Formula[A] {
  override def toString: String = {
    body match {
      case Neg(_) | Atom(_) =>
        "!" + body
      case _ =>
        "!(%s)".format(body)
    }
  }

  override def isLiteral: Boolean = {
    body match {
      case Atom(_) => true
      case _ => false
    }
  }

  def isNNF: Boolean = isLiteral

  def isCNF: Boolean = isLiteral

  def isDNF: Boolean = isLiteral
}

case class Sup[+A](lhs: Formula[A], rhs: Formula[A]) extends Formula[A] {

  override def toString: String = {
    "%s \u2228 %s".format(lhs, rhs)
  }

  /**
   * This formula is in CNF if and only if it contains no Inf(_, _) children
   * and all children must be in CNF.
   */
  def isCNF: Boolean = this match {
    case Sup(Inf(_, _), _) | Sup(_, Inf(_, _)) => false
    case _ => lhs.isCNF && rhs.isCNF
  }

  /**
   * This formula is in DNF if and only if its children are in DNF.
   */
  def isDNF: Boolean = {
    lhs.isDNF && rhs.isDNF
  }

  def isNNF: Boolean = {
    lhs.isNNF && rhs.isNNF
  }
}

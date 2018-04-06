package speedith.remix

import isabelle.Term.{Const, App, Free, Term}
import speedith.remix.Sets._

object HOLSetConversions {

  def holSetSpecToFormula(term: Term): Formula[Free] = {
    term match {
      case App(App(Const(HOL_SET_UNION, _), lhs), rhs) => Sup(holSetSpecToFormula(lhs), holSetSpecToFormula(rhs))
      case App(App(Const(HOL_SET_INTERSECTION, _), lhs), rhs) => Inf(holSetSpecToFormula(lhs), holSetSpecToFormula(rhs))
      case App(Const(HOL_SET_COMPLEMENT, _), lhs) => Neg(holSetSpecToFormula(lhs))
      case App(App(Const(HOL_SET_DIFFERENCE, _), lhs), rhs) => Inf(holSetSpecToFormula(lhs), Neg(holSetSpecToFormula(rhs)))
      case t@Free(_, _) => Atom(t)
      case _ => null
    }
  }

  /**
   * Converts HOL set expressions such as `A - ((B ∪ C) ∩ D)` to Boolean formulae. The sets must be free variables.
   */
  def holSetToBooleanFormula(term: Term): Formula[String] = {
    term match {
      case App(App(Const(HOL_SET_UNION, _), lhs), rhs) => Sup(holSetToBooleanFormula(lhs), holSetToBooleanFormula(rhs))
      case App(App(Const(HOL_SET_INTERSECTION, _), lhs), rhs) => Inf(holSetToBooleanFormula(lhs), holSetToBooleanFormula(rhs))
      case App(Const(HOL_SET_COMPLEMENT, _), lhs) => Neg(holSetToBooleanFormula(lhs))
      case App(App(Const(HOL_SET_DIFFERENCE, _), lhs), rhs) => Inf(holSetToBooleanFormula(lhs), Neg(holSetToBooleanFormula(rhs)))
      case t@Free(name, _) => Atom(name)
      case _ => null
    }
  }
}

package speedith.remix

import isabelle.Term._

object Sets {

  val HOL_SET_DIFFERENCE = "Groups.minus_class.minus"
  val HOL_SET_COMPLEMENT = "Groups.uminus_class.uminus"
  val HOL_SET_INTERSECTION = "Lattices.inf_class.inf"
  val HOL_SET_UNION = "Lattices.sup_class.sup"
  val HOL_EMPTY_SET_CONST = Const("Orderings.bot_class.bot", Type("Set.set", List(TFree("'a", TermUtils.HOL_LIST_TYPE))))
  val HOL_SET_INSERT_CONST = Const("Set.insert", Type("fun", List(TFree("'a", TermUtils.HOL_LIST_TYPE), Type("fun", List(Type("Set.set", List(TFree("'a", TermUtils.HOL_LIST_TYPE))), Type("Set.set", List(TFree("'a", TermUtils.HOL_LIST_TYPE))))))))
  val HOL_SUBSET_CONST = Const("Orderings.ord_class.less_eq", Type("fun", List(Type("Set.set", List(TFree("'a", TermUtils.HOL_LIST_TYPE))), Type("fun", List(Type("Set.set", List(TFree("'a", TermUtils.HOL_LIST_TYPE))), Type("HOL.bool", List()))))))

  def extractInsertedSetElements(setSpecification: Term): List[Term] = {
    def impl(setSpecificationInner: Term, accumulator: List[Term]): List[Term] = setSpecificationInner match {
      case HOL_EMPTY_SET_CONST =>
        accumulator
      case App(App(HOL_SET_INSERT_CONST, element), rest) =>
        impl(rest, element :: accumulator)
      case _ =>
        throw new IllegalArgumentException("The given term is not a set literal, such as: '{a,b,c,d}'.")
    }
    impl(setSpecification, Nil).reverse
  }

}

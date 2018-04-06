package speedith.remix

import isabelle.Term._
import scala.collection.JavaConversions
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

/**
 * Provides a bunch of methods for
 */
object TermUtils {

  val ISA_META_IMPLICATION = "==>"
  val ISA_META_ALL = "all"
  val HOL_CONJUNCTION = "HOL.conj"
  val HOL_DISJUNCTION = "HOL.disj"
  val HOL_IMPLICATION = "HOL.implies"
  val HOL_EQUALITY = "HOL.eq"
  val HOL_EXISTENTIAL = "HOL.Ex"
  val HOL_ALL = "HOL.All"
  val HOL_NOT = "HOL.Not"
  val HOL_TRUEPROP = "HOL.Trueprop"
  val HOL_TRUE = "HOL.True"
  val HOL_FALSE = "HOL.False"

  /**
   * The constant name for the HOL list Cons constructor.
   */
  val HOL_LIST_CONS = "List.list.Cons"

  /**
   * The constant name for the HOL list Nil constructor.
   */
  val HOL_LIST_NIL = "List.list.Nil"

  private val ISA_FUN = "fun"
  private val HOL_STRING_CHAR = "String.char.Char"
  private val HOL_LIST = "List.list"
  private val HOL_NIBBLE_TYPE = Type("String.nibble", List())
  private val HOL_CHAR_TYPE = Type("String.char", List())
  private val HOL_CHAR_LIST_TYPE = Type(HOL_LIST, List(HOL_CHAR_TYPE))
  private val HOL_EMPTY_STRING_TERM = Const(HOL_LIST_NIL, HOL_CHAR_LIST_TYPE)
  private val HOL_BOOL_TYPE = Type("HOL.bool", List())
  private val HOL_NON_EMPTY_STRING_TERM = Const(HOL_LIST_CONS, Type(ISA_FUN, List(HOL_CHAR_TYPE, Type(ISA_FUN, List(HOL_CHAR_LIST_TYPE, HOL_CHAR_LIST_TYPE)))))
  val HOL_LIST_TYPE = List("HOL.type")
  private val HOL_LIST_FREE_TYPE = TFree("'a", HOL_LIST_TYPE)
  private val PLACEHOLDER_MIXR = "IsaMixR.MixRNoVars"
  private val PLACEHOLDER_MIXRVARS = "IsaMixR.MixR"
  private val PLACEHOLDER_VARS = "IsaMixR.mixr_var"
  private val TYPE_PLACEHOLDERVARS = Type(PLACEHOLDER_VARS, List())
  private val TYPE_PLACEHOLDERVARS_LIST = List(TYPE_PLACEHOLDERVARS)
  private val ISAMIXR_ABOUT = "IsaMixR.About"
  private val MIXR_PLACEHOLDER_VARS_LIST_TYPE = Type(HOL_LIST, TYPE_PLACEHOLDERVARS_LIST)

  /**
   * Extracts the premises and the conclusion from the given term `t`. This
   * method puts the premises into the given array and returns the conclusion.
   */
  def findPremisesAndConclusion(t: Term, premises: java.util.List[Term]): Term = {
    t match {
      case App(App(Const(ISA_META_IMPLICATION, _), lhsTerm), rhsTerm) => {
        premises.add(lhsTerm)
        findPremisesAndConclusion(rhsTerm, premises)
      }
      case _ => t
    }
  }

  /**
   * Extracts the premises and the conclusion from the given term `t`. This
   * method puts the premises into the given array and returns the conclusion.
   */
  def findPremisesAndConclusion(t: Term, premises: mutable.Buffer[Term]): Term = {
    findPremisesAndConclusion(t, JavaConversions.bufferAsJavaList(premises))
  }

  /**
   * Extracts the globally meta-quantified variables from the term, puts them
   * into the given array list packed as separate `Term.Free`, and returns the
   * body.
   *
   * For example, from the formula `⋀ x y z. ⟦ A(x, y, z) ⟧ ⟹ B(x, y, z)`, we
   * get `⟦ A(x, y, z) ⟧ ⟹ B(x, y, z)` as the body, and the list `[x, y, z]`
   * in the variables list.
   *
   * The quantified variables in the body are still retained as bound.
   */
  def findQuantifiedVarsAndBody(t: Term, variables: java.util.List[Free]): Term = {
    t match {
      case App(Const(ISA_META_ALL, _), Abs(varName, varType, body)) => {
        variables.add(Free(varName, varType))
        findQuantifiedVarsAndBody(body, variables)
      }
      case _ => t
    }
  }

  /**
   * Extracts the globally meta-quantified variables from the term, puts them
   * into the given array list packed as separate `Term.Free`, and returns the
   * body.
   *
   * For example, from the formula `⋀ x y z. ⟦ A(x, y, z) ⟧ ⟹ B(x, y, z)`, we
   * get `⟦ A(x, y, z) ⟧ ⟹ B(x, y, z)` as the body, and the list `[x, y, z]`
   * in the variables list.
   *
   * The quantified variables in the body are still retained as bound.
   */
  def findQuantifiedVarsAndBody(t: Term, variables: mutable.Buffer[Free]): Term = {
    findQuantifiedVarsAndBody(t, JavaConversions.bufferAsJavaList(variables))
  }

  /**
   * Returns the terms that represent the elements of the `listTerm` which must
   * be an HOL list.
   */
  def getListElements(listTerm: Term, outElements: java.util.List[Term]): java.util.List[Term] = {
    listTerm match {
      case App(App(Const(HOL_LIST_CONS, _), element), rest) => {
        outElements.add(element)
        getListElements(rest, outElements)
      }
      case Const(HOL_LIST_NIL, _) => {}
      case x => throw new IllegalArgumentException("The given term is not an HOL list. It contained the term '%s'.".format(x))
    }
    outElements
  }

  def traverseListElements(listTerm: Term, visitor: Term => Unit): Unit = {
    listTerm match {
      case App(App(Const(HOL_LIST_CONS, _), element), rest) => {
        visitor(element)
        traverseListElements(rest, visitor)
      }
      case Const(HOL_LIST_NIL, _) => {}
      case _ => throw new IllegalArgumentException("The given term is not an HOL list. It contained the term '%s'.".format(listTerm))
    }
  }

  /**
   * Returns the terms that represent the elements of the `listTerm` which must
   * be an HOL list.
   */
  def getListElements(listTerm: Term, outElements: mutable.Buffer[Term] = ArrayBuffer[Term]()): mutable.Buffer[Term] = {
    getListElements(listTerm, JavaConversions.bufferAsJavaList(outElements))
    outElements
  }

  /**
   * Visits every term in the given term tree.
   */
  def traverseTermTree(t: Term, visitor: Term => Boolean): Boolean = {
    if (visitor(t)) return true
    t match {
      case Abs(_, _, t1) =>
        traverseTermTree(t1, visitor)
      case App(t1, t2) => {
        if (traverseTermTree(t1, visitor)) return true
        traverseTermTree(t2, visitor)
      }
      case _ => false
    }
  }

  /**
   * If the term represents a string it will extract the string from it. Otherwise it will return `null`.
   */
  def extractString(t: Term): String = {
    t match {
      case App(App(HOL_NON_EMPTY_STRING_TERM, firstChar), otherChars) => {
        val sb = new StringBuilder()
        sb.append(extractChar(firstChar))
        traverseListElements(otherChars, ch => {
          sb.append(extractChar(ch))
        })
        sb.toString()
      }
      case HOL_EMPTY_STRING_TERM => ""
      case _ => null
    }
  }

  /**
   * If the term is a placeholder then this method extracts and returns it. If it is not a placeholder, it returns `null`.
   */
  // def extractPlaceholder(t: Term): Placeholder = {
  //   t match {
  //     case App(Const(HOL_TRUEPROP, Type(ISA_FUN, _)), innerTerm) => extractPlaceholder(innerTerm)
  //     case App(Const(PLACEHOLDER_MIXR, Type(ISA_FUN, List(HOL_CHAR_LIST_TYPE, HOL_BOOL_TYPE))), payload) => {
  //       val (formulaFormatName, payloadFormula) = extractPayloadLanguageAndFormula(payload)
  //       new PlaceholderWithoutVars(formulaFormatName, payloadFormula)
  //     }
  //     case App(App(Const(PLACEHOLDER_MIXRVARS, Type(ISA_FUN, List(MIXR_PLACEHOLDER_VARS_LIST_TYPE, Type(ISA_FUN, List(HOL_CHAR_LIST_TYPE, HOL_BOOL_TYPE))))), vars), payload) => {
  //       val (formulaFormatName, payloadFormula) = extractPayloadLanguageAndFormula(payload)
  //       val varList = extractPlaceholderVars(vars)
  //       PlaceholderWithVars(varList, formulaFormatName, payloadFormula)
  //     }
  //     case _ => null
  //   }
  // }


  // def extractPlaceholderVars(t: Term): java.util.List[FreeVar] = {
  //   t match {
  //     case App(App(Const(HOL_LIST_CONS, Type(ISA_FUN, List(TYPE_PLACEHOLDERVARS, Type(ISA_FUN, List(Type(HOL_LIST, TYPE_PLACEHOLDERVARS_LIST), Type(HOL_LIST, TYPE_PLACEHOLDERVARS_LIST)))))), first), other) => {
  //       val outVars = mutable.Buffer[FreeVar]()
  //       extractPlaceholderVarGroup(first, outVars)
  //       traverseListElements(other, t => {
  //         extractPlaceholderVarGroup(t, outVars)
  //       })
  //       JavaConversions.bufferAsJavaList(outVars)
  //     }
  //     case _ => throw new IllegalArgumentException("Not a valid list of placeholder variables.")
  //   }
  // }

  // def extractPlaceholderVarGroup(t: Term, outVars: mutable.Buffer[FreeVar]): Unit = {
  //   t match {
  //     case App(Const(ISAMIXR_ABOUT, Type(ISA_FUN, List(Type(HOL_LIST, List(_)), TYPE_PLACEHOLDERVARS))), vars) => {
  //       traverseListElements(vars, {
  //         case Free(varName, varType) =>
  //           outVars += FreeVar(varName, varType)
  //         case Const(varName, varType) =>
  //           outVars += FreeVar(varName, varType)
  //         case tAny => throw new IllegalArgumentException("Not a valid group of placeholder variables. The offending term: " + tAny)
  //       })
  //     }
  //     case _ =>
  //       throw new IllegalArgumentException("Not a valid group of placeholder variables.")
  //   }
  // }

  private def extractPayloadLanguageAndFormula(payload: Term): (String, String) = {
    val payloadString = extractString(payload)
    val delimiterIndex = payloadString.indexOf(':')
    if (delimiterIndex < 0) {
      throw new IllegalArgumentException("The placeholders is not formatted correctly. The payload must be of the format: LANG_NAME:FORMULA")
    }
    val formulaFormatName = payloadString.substring(0, delimiterIndex)
    val payloadFormula = payloadString.substring(delimiterIndex + 1)
    (formulaFormatName, payloadFormula)
  }

  private def extractChar(t: Term): Char = {
    t match {
      case App(App(Const(HOL_STRING_CHAR, Type(ISA_FUN, List(HOL_NIBBLE_TYPE, Type(ISA_FUN, List(HOL_NIBBLE_TYPE, HOL_CHAR_TYPE))))), Const(n1, HOL_NIBBLE_TYPE)), Const(n2, HOL_NIBBLE_TYPE)) =>
        extractCharFromNibbles(n1, n2)
      case _ =>
        throw new IllegalArgumentException("Expected a character term. Got '%s'.".format(t))
    }
  }

  private def extractCharFromNibbles(n1: String, n2: String): Char = {
    val hexChar1 = n1.charAt(n1.length() - 1)
    val hexChar2 = n2.charAt(n2.length() - 1)
    ((hexCharToInt(hexChar1) << 4) + hexCharToInt(hexChar2)).toChar
  }

  private def hexCharToInt(hex: Char): Int = {
    if (hex >= '0' && hex <= '9')
      hex - '0'
    else if (hex >= 'A' && hex <= 'F')
      hex - 'A' + 10
    else
      throw new IllegalArgumentException("Not a hexadecimal character.")
  }
}

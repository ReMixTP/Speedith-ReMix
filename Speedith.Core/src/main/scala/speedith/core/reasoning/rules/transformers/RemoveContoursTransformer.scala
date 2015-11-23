package speedith.core.reasoning.rules.transformers

import speedith.core.reasoning.args.ContourArg
import speedith.core.lang._
import scala.collection.JavaConversions._

case class RemoveContoursTransformer(contourArgs: java.util.List[ContourArg]) extends IdTransformer {

  val subDiagramIndex = contourArgs(0).getSubDiagramIndex
  val contoursToRemove = contourArgs.map(_.getContour).toSet

  private def regionWithoutContours(region: Iterable[Zone]): Set[Zone] = {
    region.map(zone => new Zone(zone.getInContours -- contoursToRemove, zone.getOutContours -- contoursToRemove)).toSet
  }

  private def shadedRegionWithoutContours(region: Set[Zone]): Set[Zone] = {
    var shadedRegion = region
    for (contourToRemove <- contoursToRemove) {
      shadedRegion = shadedRegion.filter(zone =>
        zone.getInContours.contains(contourToRemove) &&
          shadedRegion.contains(new Zone(zone.getInContours - contourToRemove, zone.getOutContours + contourToRemove))
      )
      shadedRegion = shadedRegion.map(zone => new Zone(zone.getInContours - contourToRemove, zone.getOutContours - contourToRemove)).toSet
    }
    shadedRegion
  }

  override def transform(psd: PrimarySpiderDiagram,
                         diagramIndex: Int,
                         parents: java.util.ArrayList[CompoundSpiderDiagram],
                         childIndices: java.util.ArrayList[java.lang.Integer]): SpiderDiagram = {
    if (subDiagramIndex == diagramIndex) {
      try {
        val normalised = normalize(psd)
        SpiderDiagrams.createPrimarySD(
          normalised.getSpiders,
          normalised.getHabitats.map {
            case (spider, habitat) => (spider, new Region(regionWithoutContours(habitat.zones)))
          },
          shadedRegionWithoutContours(normalised.getShadedZones.toSet),
          regionWithoutContours(normalised  .getPresentZones)
        )
      }
      catch {
        case e: Throwable =>
          println(e)
          e.printStackTrace()
          throw e
      }
    } else {
      null
    }
  }

  private def normalize (psd : PrimarySpiderDiagram): PrimarySpiderDiagram= {
    val possibleZones: Set[Zone] = Zones.allZonesForContours(psd.getAllContours.toSeq:_*).toSet
    SpiderDiagrams.createPrimarySD(psd.getSpiders, psd.getHabitats, psd.getShadedZones, possibleZones -- (psd.getShadedZones -- psd.getPresentZones))
  }
}

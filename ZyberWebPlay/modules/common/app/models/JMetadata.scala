package models

import zyber.server.dao.MetaData
import scala.collection.JavaConverters._
//import scala.collection.mutable.Set
import java.util.UUID

case class JMetadata (
    key: String,
    value: Seq[String]
    ){
  
  def to(uuid: UUID): MetaData = 
    new MetaData(key, value.toSet.asJava, uuid)
}

    
object JMetadata{
  def from(md: MetaData) = 
    JMetadata(
        md.getKey,
        Option(md.getValue).map(_.asScala.toSeq).getOrElse(Seq())
        )
        
}
    
    
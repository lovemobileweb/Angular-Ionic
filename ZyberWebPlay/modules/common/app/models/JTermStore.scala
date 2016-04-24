package models

import zyber.server.dao.TermStore
import java.util.UUID

case class JTermStore(
    name: String,
    description: String,
    allowCustomTerms: Boolean,
    uuid: Option[UUID] = None) {

  def toTermStore(uuid: UUID): TermStore = {
    val termStore = new TermStore(name, description, allowCustomTerms)
    termStore.setTermStoreId(uuid)
    termStore
  }
  def toTermStore(): TermStore = {
    val termStore = new TermStore(name, description, allowCustomTerms)
    if (uuid.isDefined)
      termStore.setTermStoreId(uuid.get)
    termStore
  }
}

object JTermStore {
  def from(ts: TermStore): JTermStore =
    JTermStore(
      ts.getName,
      ts.getDescription,
      ts.getAllowCustomTerms,
      Option(ts.getTermStoreId)
    )
}
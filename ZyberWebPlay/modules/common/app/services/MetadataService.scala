package services

import java.util.UUID
import javax.inject.Inject

import com.google.inject.ImplementedBy
import core.ApiErrors
import models.JMetadata
import play.api.http.Status
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import zyber.server.{CassandraMapperDelegate, ZyberSession}
import zyber.server.dao._

import scala.collection.JavaConverters._
import scala.util.Try

@ImplementedBy(classOf[MetadataServiceImp])
trait MetadataService {

  //TODO this should probably be in its own service
  def getApplicationSetting(key:String)(implicit user:User):Option[ApplicationSetting]
  def putApplicationSetting(key:String, value:String)(implicit user:User):ApplicationSetting

  def getPathMetadata(pathId: UUID)(implicit u: User): Seq[MetaData]

  def getTermStore(implicit u: User): Seq[TermStore]

  def getTerms(termStoreName: String)(implicit u: User): Seq[TermStoreTerm]

  def updateMetadata(md: MetaData)(implicit user: User): Either[ApiErrors, Unit]

  def updateMetadata(pathId: UUID, md: Seq[JMetadata])(implicit u: User): Try[Unit]

  def deletePathMetadata(pathId: UUID, key: String)(implicit user: User): Either[ApiErrors, Unit]

  def addTermstore(termStore: TermStore, terms: Seq[String])(implicit user: User): Either[ApiErrors, Unit]

  def updateTermstore(termStore: TermStore, terms: Seq[String])(implicit user: User): Either[ApiErrors, Unit]

  def deleteTermstore(termstoreId: UUID)(implicit user: User): Either[ApiErrors, Unit]
}

class MetadataServiceImp @Inject() (
    val session: ZyberSession,
    val messagesApi: MessagesApi) extends MetadataService with MultitenancySupport with I18nSupport {


  //TODO this should probably be in its own service
  override def getApplicationSetting(key: String)(implicit user:User): Option[ApplicationSetting] = {
    Option(applicationSettingAccessor.getTenantSetting(key))
  }


  override def putApplicationSetting(key: String, value: String)(implicit user: User): ApplicationSetting = {
    val setting: ApplicationSetting = new ApplicationSetting(key,value)
    setting.setTenantId(user.getTenantId)
    applicationSettingMapper.save(setting)
    setting
  }

  def applicationSettingAccessor(implicit user:User): ApplicationSettingsAccessor = userSession.accessor(classOf[ApplicationSettingsAccessor])
  def applicationSettingMapper(implicit user:User): CassandraMapperDelegate[ApplicationSetting] = userSession.mapper(classOf[ApplicationSetting])

  def metadataMapper(implicit user: User): CassandraMapperDelegate[MetaData] =
    userSession.mapper(classOf[MetaData])
  def metadataAccessor(implicit user: User): MetaDataAccessor =
    userSession.accessor(classOf[MetaDataAccessor])

  def termStoreMapper(implicit user: User): CassandraMapperDelegate[TermStore] =
    userSession.mapper(classOf[TermStore])

  def termStoreAccessor(implicit user: User): TermStoreAccessor =
    userSession.accessor(classOf[TermStoreAccessor])

  def termStoreTermMapper(implicit user: User): CassandraMapperDelegate[TermStoreTerm] =
    userSession.mapper(classOf[TermStoreTerm])

  def termStoreTermAccessor(implicit user: User): TermStoreTermAccessor =
    userSession.accessor(classOf[TermStoreTermAccessor])

  def getPathMetadata(pathId: UUID)(implicit user: User): Seq[MetaData] =
    metadataAccessor.getPathMetadata(pathId).all().asScala

  def getTermStore(implicit user: User): Seq[TermStore] = {
    termStoreAccessor.getTermStore.all().asScala

  }

  def getTerms(termStoreName: String)(implicit user: User): Seq[TermStoreTerm] = {
    Option(termStoreAccessor.getTermStoreByName(termStoreName))
      .map { ts =>
        termStoreTermAccessor.getTerms(ts.getTermStoreId).all().asScala
      }
      .getOrElse(Seq())
  }

  def updateMetadata(md: MetaData)(implicit user: User): Either[ApiErrors, Unit] = {
    Right { metadataMapper.save(md) }
  }

  def updateMetadata(pathId: UUID, md: Seq[JMetadata])(implicit user: User): Try[Unit] = Try {
    metadataAccessor.deletePathMetadata(pathId)
    md.foreach { x =>
      metadataMapper.save(x.to(pathId))
    }
  }

  def deletePathMetadata(pathId: UUID, key: String)(implicit user: User): Either[ApiErrors, Unit] = {
    val vbp = metadataAccessor.getValueByPathID(key, pathId)
    if (null == vbp) {
      Left(ApiErrors.single("", Messages("api.metadata_not_found"), Status.NOT_FOUND))
    } else Right {
      metadataAccessor.deletePathMetadataByKey(pathId, key)
    }
  }

  override def addTermstore(termStore: TermStore, terms: Seq[String])(implicit user: User): Either[ApiErrors, Unit] = {
    if (null != termStoreAccessor.getTermStoreByName(termStore.getName))
      Left(ApiErrors.single("Term store already exists", Messages("existent_termstore"), Status.BAD_REQUEST))
    else {
      Right {
        val uuid = UUID.randomUUID()

        termStore.setTermStoreId(uuid)
        termStoreMapper.save(termStore)

        if (!termStore.getAllowCustomTerms)
          saveTerms(uuid, terms)
      }
    }
  }

  override def updateTermstore(termStore: TermStore, terms: Seq[String])(implicit user: User): Either[ApiErrors, Unit] = {
    val termStoreDB = termStoreAccessor.getTermStoreById(termStore.getTermStoreId)

    if (null == termStoreDB)
      Left(ApiErrors.single("Invalid term store", Messages("invalid_termstore"), Status.BAD_REQUEST))
    else if (termStore.getName != termStoreDB.getName && null != termStoreAccessor.getTermStoreByName(termStore.getName))
      Left(ApiErrors.single("Term store already exists", Messages("existent_termstore"), Status.CONFLICT))
    else {
      Right {
        termStoreMapper.save(termStore)

        termStoreTermAccessor.deleteTermStoreTerms(termStore.getTermStoreId)

        if (!termStore.getAllowCustomTerms)
          saveTerms(termStore.getTermStoreId, terms)
      }
    }
  }

  override def deleteTermstore(termstoreId: UUID)(implicit user: User): Either[ApiErrors, Unit] = {
    val termStoreDB = termStoreAccessor.getTermStoreById(termstoreId)
    if (null == termStoreDB)
      Left(ApiErrors.single("Invalid term store", Messages("invalid_termstore"), Status.BAD_REQUEST))
    else {
      Right {
        termStoreTermAccessor.deleteTermStoreTerms(termstoreId)
        termStoreAccessor.deleteTermStoreTerms(termstoreId)
      }
    }
  }

  private def saveTerms(termStoreId: UUID,
                        terms: Seq[String])(implicit user: User) = {
    terms.map(str =>
      new TermStoreTerm(termStoreId, UUID.randomUUID(), str))
      .foreach(termStoreTermMapper.save(_))
  }

}
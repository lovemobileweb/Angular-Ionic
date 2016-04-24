package services

import org.apache.commons.io.IOUtils
import zyber.server.ZyberTestSession
import org.specs2.mutable.Specification
import com.datastax.driver.core.Session
import zyber.server.dao._
import java.text.SimpleDateFormat
import org.specs2.specification.AfterAll
import play.api.test.WithApplication
import zyber.server.ZyberUserSession
import play.api.i18n.DefaultMessagesApi
import com.datastax.driver.core.querybuilder.QueryBuilder
import scala.collection.JavaConverters._
import play.api.inject.guice.GuiceApplicationBuilder
import zyber.server.ZyberSession
import play.api.inject.bind
import zyber.server.Abilities
import java.util.UUID
import scala.util.Random
import play.mvc.Http.Status
import core.ZyberCacheManagerProvider
import net.sf.ehcache.CacheManager
import org.specs2.specification.BeforeEach

class FileServiceSpec extends Specification with PathsData with BeforeEach {

  lazy val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
  override lazy val zyberSession = zyberUserSession.session

  //  def zyberFakeApp = new GuiceApplicationBuilder().
  //    overrides(bind[ZyberSession].to(zyberSession)).
  //    overrides(bind[CacheManager].toProvider[ZyberCacheManagerProvider]).
  //    build()

  def before = {
    //    cleanDB(ZyberTestSession.getTestUserSession.session.getSession)
    deleteForTestingTenant(zyberSession.getSession, classOf[Path])
    deleteForTestingTenant(zyberSession.getSession, classOf[User])
    deleteForTestingTenant(zyberSession.getSession, classOf[UserKeys])
  }

  "FileService" should {
    "Return None on invalid path" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      implicit val user = zyberUserSession.user

      val folders = "folder1/folder2/folder3".split("/").toList
      val rootPath = zyberUserSession.getRootPath
      createTreeRec(rootPath, folders)

      val fileService = app.injector.instanceOf(classOf[FileServiceImpl])
      val retPath = fileService.getPath(rootPath, "folder1/folder3".split("/").toList)
      retPath must beNone
    }

    "Upload and restore multiple versions" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      val zyberSession = zyberUserSession.session
      implicit val user = zyberUserSession.user
      user.getRootPath(zyberUserSession)

      val activityService = app.injector.instanceOf(classOf[ActivityService])
      val fileService: FileServiceImpl = app.injector.instanceOf(classOf[FileServiceImpl])

      val output = fileService.getOSPath("tmp", user, user.getHomeFolder)
      output.get.write("Hi there".getBytes())
      output.get.close()

      var files = fileService.listFiles(user, user.getHomeFolder.toString).get
      files.size shouldEqual 1
      files.foreach(_.setZus(zyberUserSession))
      val theFile = files.head
      val version = theFile.getCurrentVersion
      val original: Long = version.getTime
      IOUtils.toString(files.head.getInputStream) shouldEqual "Hi there"

      val output2 = fileService.getOSPath("tmp", user, user.getHomeFolder)
      output2.get.write("Hi there2".getBytes())
      output2.get.close()
      files = fileService.listFiles(user, user.getHomeFolder.toString).get
      files.size shouldEqual 1
      files.foreach(_.setZus(zyberUserSession))
      IOUtils.toString(files.last.getInputStream) shouldEqual "Hi there2"

      fileService.restoreVersion(files.head.getPathId, original, user)

      files = fileService.listFiles(user, user.getHomeFolder.toString).get
      files.size shouldEqual 1
      files.foreach(_.setZus(zyberUserSession))
      IOUtils.toString(files.last.getInputStream) shouldEqual "Hi there"

      val activity = activityService.listActivityByPath(files.head.getPathId)
      activity.head.getActivity shouldEqual "Created"
      activity(1).getActivity shouldEqual "Edited"
      activity(2).getActivity shouldEqual "Restored"
    }

    "Get path from root and given url path" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      val zyberSession = zyberUserSession.session
      implicit val user = zyberUserSession.user

      val folders = "folder1/folder2/folder3".split("/").toList
      val rootPath = zyberUserSession.getRootPath

      createTreeRec(rootPath, folders)

      val fileService = app.injector.instanceOf(classOf[FileServiceImpl])

      val retPath = fileService.getPath(rootPath, folders)

      retPath must beSome

      val path = retPath.get
      path.getName mustEqual folders.last
    }

    "Find paths by id" in new WithApplication(zyberFakeApp) {
      val fileService = app.injector.instanceOf(classOf[FileService])
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      implicit val user = zyberUserSession.getUser
      val urp = user.getRootPath(zyberUserSession)

      val maybeuserPath = fileService.getPathByUUID(user.getHomeFolder)

      maybeuserPath must beSome

      val userPath = maybeuserPath.get
      userPath.getPathId mustEqual urp.getPathId
      userPath.getName mustEqual urp.getName

      val folder1 = urp.createDirectory("folder1")

      val maybeF1Path = fileService.getPathByUUID(folder1.getPathId)
      maybeF1Path must beSome

      val f1 = maybeF1Path.get
      f1.getPathId mustEqual folder1.getPathId
      f1.getName mustEqual folder1.getName

      val nonePath = fileService.getPathByUUID(UUID.randomUUID)
      nonePath must beNone
    }
  }

  "FileService#listFiles" should {

    "Return paths ordered by name" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      val zyberSession = zyberUserSession.session
      implicit val user = zyberUserSession.user
      user.getRootPath(zyberUserSession)

      val secService = app.injector.instanceOf(classOf[SecurityService])
      val rootPath = zyberUserSession.getRootPath

      val folder1 = rootPath.createDirectory("folder1")
      secService.setOwnerPermissionsFor(folder1, user.getUserId)
      val folder2 = rootPath.createDirectory("folder2")
      secService.setOwnerPermissionsFor(folder2, user.getUserId)

      val file1 = rootPath.createFile("file1")
      secService.setOwnerPermissionsFor(file1, user.getUserId)
      file1.getOutputStream.write("file1".getBytes)

      val file2 = rootPath.createFile("file2")
      secService.setOwnerPermissionsFor(file2, user.getUserId)
      file2.getOutputStream.write("file2".getBytes)

      val file3 = rootPath.createFile("file3")
      secService.setOwnerPermissionsFor(file3, user.getUserId)
      file3.getOutputStream.write("file3".getBytes)

      val expectedPaths = List(folder1, folder2, file1, file2, file3)

      val fileService = app.injector.instanceOf(classOf[FileServiceImpl])

      val mpaths = fileService.listFiles(zyberUserSession.user, user.getHomeFolder.toString, false, Some("name"), None)

      mpaths must beSome

      val paths = mpaths.get

      paths.size mustEqual expectedPaths.size
      paths.map(_.getPathId) must contain(exactly(expectedPaths.map(_.getPathId): _*)).inOrder
    }

    "Return paths ordered by name desc" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      val zyberSession = zyberUserSession.session
      implicit val user = zyberUserSession.user
      user.getRootPath(zyberUserSession)

      val secService = app.injector.instanceOf(classOf[SecurityService])
      val rootPath = zyberUserSession.getRootPath

      val folder1 = rootPath.createDirectory("folder1")
      secService.setOwnerPermissionsFor(folder1, user.getUserId)

      val folder2 = rootPath.createDirectory("folder2")
      secService.setOwnerPermissionsFor(folder2, user.getUserId)

      val file1 = rootPath.createFile("file1")
      secService.setOwnerPermissionsFor(file1, user.getUserId)
      file1.getOutputStream.write("file1".getBytes)

      val file2 = rootPath.createFile("file2")
      secService.setOwnerPermissionsFor(file2, user.getUserId)
      file2.getOutputStream.write("file2".getBytes)

      val file3 = rootPath.createFile("file3")
      secService.setOwnerPermissionsFor(file3, user.getUserId)
      file3.getOutputStream.write("file3".getBytes)

      val expectedPaths = List(file3, file2, file1, folder2, folder1)

      val fileService = app.injector.instanceOf(classOf[FileServiceImpl])

      val mpaths = fileService.listFiles(zyberUserSession.user, user.getHomeFolder.toString, false, Some("name"), Some("desc"))

      mpaths must beSome

      val paths = mpaths.get

      paths.size mustEqual expectedPaths.size
      paths.map(_.getPathId) must contain(exactly(expectedPaths.map(_.getPathId): _*)).inOrder
    }

    "Return paths ordered by modified date" in new WithApplication(zyberFakeApp) {

      val sf = new SimpleDateFormat("dd/MM/yyyy hh:mm")

      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      val zyberSession = zyberUserSession.session
      implicit val user = zyberUserSession.user
      user.getRootPath(zyberUserSession)

      //      val loginService = new LoginServiceImp(zyberSession, messagesApi)
      val secService = app.injector.instanceOf(classOf[SecurityService])
      val rootPath = zyberUserSession.getRootPath

      val mapper = zyberUserSession.mapper(classOf[Path])

      val folder1 = rootPath.createDirectory("folder1")
      secService.setOwnerPermissionsFor(folder1, user.getUserId)

      val folder2 = rootPath.createDirectory("folder2")
      secService.setOwnerPermissionsFor(folder2, user.getUserId)

      val file1 = rootPath.createFile("file1")
      secService.setOwnerPermissionsFor(file1, user.getUserId)
      file1.getOutputStream.write("file1".getBytes)

      val file2 = rootPath.createFile("file2")
      secService.setOwnerPermissionsFor(file2, user.getUserId)
      file2.getOutputStream.write("file2".getBytes)
      file2.setModifiedDate(sf.parse("20/06/2015 11:00"))
      mapper.save(file2)

      val file3 = rootPath.createFile("file3")
      secService.setOwnerPermissionsFor(file3, user.getUserId)
      file3.getOutputStream.write("file3".getBytes)
      file3.setModifiedDate(sf.parse("20/06/2014 11:00"))
      mapper.save(file3)

      val expectedPaths = List(folder1, folder2, file3, file2, file1)

      val fileService = app.injector.instanceOf(classOf[FileServiceImpl])

      val mpaths = fileService.listFiles(zyberUserSession.user, user.getHomeFolder.toString, false, Some("modified"), None)

      mpaths must beSome

      val paths = mpaths.get

      paths.size mustEqual expectedPaths.size
      paths.map(_.getPathId) must contain(exactly(expectedPaths.map(_.getPathId): _*)).inOrder
    }

    "Return paths ordered by modified date desc" in new WithApplication(zyberFakeApp) {
      val sf = new SimpleDateFormat("dd/MM/yyyy hh:mm")

      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      val zyberSession = zyberUserSession.session
      implicit val user = zyberUserSession.user
      user.getRootPath(zyberUserSession)

      val secService = app.injector.instanceOf(classOf[SecurityService])
      val rootPath = zyberUserSession.getRootPath

      val mapper = zyberUserSession.mapper(classOf[Path])

      val folder1 = rootPath.createDirectory("folder1")
      secService.setOwnerPermissionsFor(folder1, user.getUserId)

      val folder2 = rootPath.createDirectory("folder2")
      secService.setOwnerPermissionsFor(folder2, user.getUserId)

      val file1 = rootPath.createFile("Zfile1")
      secService.setOwnerPermissionsFor(file1, user.getUserId)
      file1.getOutputStream.write("file1".getBytes)

      val file2 = rootPath.createFile("Gfile2")
      secService.setOwnerPermissionsFor(file2, user.getUserId)
      file2.getOutputStream.write("file2".getBytes)
      file2.setModifiedDate(sf.parse("20/06/2015 11:00"))
      mapper.save(file2)

      val file3 = rootPath.createFile("Pfile3")
      secService.setOwnerPermissionsFor(file3, user.getUserId)
      file3.getOutputStream.write("file3".getBytes)
      file3.setModifiedDate(sf.parse("20/06/2014 11:00"))
      mapper.save(file3)

      val expectedPaths = List(file1, file2, file3, folder2, folder1)

      val fileService = app.injector.instanceOf(classOf[FileServiceImpl])

      val mpaths = fileService.listFiles(zyberUserSession.user, user.getHomeFolder.toString, false, Some("modified"), Some("desc"))

      mpaths must beSome

      val paths = mpaths.get

      paths.size mustEqual expectedPaths.size
      paths.map(_.getPathId) must contain(exactly(expectedPaths.map(_.getPathId): _*)).inOrder
    }

    "Return paths ordered by size" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      val zyberSession = zyberUserSession.session
      implicit val user = zyberUserSession.user

      //      val loginService = new LoginServiceImp(zyberSession, messagesApi)
      val rootPath = zyberUserSession.getRootPath
      val secService = app.injector.instanceOf(classOf[SecurityService])

      val folder1 = rootPath.createDirectory("folder1")
      secService.setOwnerPermissionsFor(folder1, user.getUserId)

      val folder2 = rootPath.createDirectory("folder2")
      secService.setOwnerPermissionsFor(folder2, user.getUserId)

      val file1 = rootPath.createFile("file1")
      secService.setOwnerPermissionsFor(file1, user.getUserId)
      val f1os = file1.getOutputStream
      f1os.write("f".getBytes)
      f1os.close()

      val file2 = rootPath.createFile("file2")
      secService.setOwnerPermissionsFor(file2, user.getUserId)
      val f2os = file2.getOutputStream
      f2os.write("file2file2file2file2file2file2file2file2".getBytes)
      f2os.close()

      val file3 = rootPath.createFile("file3")
      secService.setOwnerPermissionsFor(file3, user.getUserId)
      val f3os = file3.getOutputStream
      f3os.write("file3file3file3file3file3".getBytes)
      f3os.close()

      val expectedPaths = List(folder1, folder2, file1, file3, file2)

      val fileService = app.injector.instanceOf(classOf[FileServiceImpl])

      val mpaths = fileService.listFiles(zyberUserSession.user, user.getHomeFolder.toString, false, Some("size"), None)

      mpaths must beSome

      val paths = mpaths.get

      paths.size mustEqual expectedPaths.size
      paths.map(_.getPathId) must contain(exactly(expectedPaths.map(_.getPathId): _*)).inOrder
    }

    "Return paths ordered by size desc" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      val zyberSession = zyberUserSession.session
      implicit val user = zyberUserSession.user

      val secService = app.injector.instanceOf(classOf[SecurityService])
      val rootPath = zyberUserSession.getRootPath

      val folder1 = rootPath.createDirectory("folder1")
      secService.setOwnerPermissionsFor(folder1, user.getUserId)

      val folder2 = rootPath.createDirectory("folder2")
      secService.setOwnerPermissionsFor(folder2, user.getUserId)

      val file1 = rootPath.createFile("file1")
      secService.setOwnerPermissionsFor(file1, user.getUserId)
      val f1os = file1.getOutputStream
      f1os.write("f".getBytes)
      f1os.close()

      val file2 = rootPath.createFile("file2")
      secService.setOwnerPermissionsFor(file2, user.getUserId)
      val f2os = file2.getOutputStream
      f2os.write("file2file2file2file2file2file2file2file2".getBytes)
      f2os.close()

      val file3 = rootPath.createFile("file3")
      secService.setOwnerPermissionsFor(file3, user.getUserId)
      val f3os = file3.getOutputStream
      f3os.write("file3file3file3file3file3".getBytes)
      f3os.close()

      val expectedPaths = List(file2, file3, file1, folder2, folder1)

      val fileService = app.injector.instanceOf(classOf[FileServiceImpl])

      val mpaths = fileService.listFiles(zyberUserSession.user, user.getHomeFolder.toString, false, Some("size"), Some("desc"))

      mpaths must beSome

      val paths = mpaths.get

      paths.size mustEqual expectedPaths.size
      paths.map(_.getPathId) must contain(exactly(expectedPaths.map(_.getPathId): _*)).inOrder
    }
  }

  "FileService#createFolder" should {
    "Not allow creating repeated folders and with invalid name" in new WithApplication(zyberFakeApp) {
      val fileService = app.injector.instanceOf(classOf[FileService])
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      implicit val user = zyberUserSession.getUser
      val userRootPath = user.getRootPath(zyberUserSession)

      var fname = "1invalidName2/_[]:??" + Random.nextInt
      var res = fileService.createFolder(userRootPath, fname)
      res must beLeft
      var error = res.left.get
      error.statusCode mustEqual Status.BAD_REQUEST

      fname = "folder" + Random.nextInt
      fileService.createFolder(userRootPath, fname)
      res = fileService.createFolder(userRootPath, fname)

      res must beLeft
      error = res.left.get
      error.statusCode mustEqual Status.CONFLICT
    }

    "Create new folders" in new WithApplication(zyberFakeApp) {
      val fileService = app.injector.instanceOf(classOf[FileService])
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      implicit val user = zyberUserSession.getUser
      val userRootPath = user.getRootPath(zyberUserSession)

      val fname = "folder" + Random.nextInt
      val res = fileService.createFolder(userRootPath, fname)
      res must beRight
      val cf = res.right.get
      cf.getName mustEqual fname
    }
  }

  "FileService#rename" should {
    "Rename files" in new WithApplication(zyberFakeApp) {
      val fileService = app.injector.instanceOf(classOf[FileService])
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      implicit val user = zyberUserSession.getUser
      val userRootPath = user.getRootPath(zyberUserSession)
      val folderName = "initialFolder"
      val initPath = userRootPath.createDirectory(folderName)

      val newFolderName = "newFolderName"
      var res = fileService.rename(initPath, newFolderName)
      res must beRight
      var renamed = res.right.get
      renamed.getName mustEqual newFolderName
      val accessor = zyberUserSession.accessor(classOf[PathAccessor])
      var retPath = accessor.getPath(initPath.getPathId)
      retPath.getName mustEqual newFolderName

      val fileName = "fileName"
      val initFile = userRootPath.createFile(fileName)
      val newFileName = "newFileName"
      res = fileService.rename(initFile, newFileName)
      res must beRight
      renamed = res.right.get
      renamed.getName mustEqual newFileName
      retPath = accessor.getPath(initFile.getPathId)
      retPath.getName mustEqual newFileName
    }

    "Not allow renaming with existing name in the same parent folder" in new WithApplication(zyberFakeApp) {
      val fileService = app.injector.instanceOf(classOf[FileService])
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      implicit val user = zyberUserSession.getUser
      val userRootPath = user.getRootPath(zyberUserSession)
      val folderName = "initialFolder"
      val initPath = userRootPath.createDirectory(folderName)

      val res = fileService.rename(initPath, folderName)
      res must beLeft
      val errors = res.left.get
      errors.statusCode mustEqual Status.CONFLICT
    }

    "Not allow invalid names" in new WithApplication(zyberFakeApp) {
      val fileService = app.injector.instanceOf(classOf[FileService])
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      implicit val user = zyberUserSession.getUser
      val userRootPath = user.getRootPath(zyberUserSession)
      val folderName = "initialFolder"
      val initPath = userRootPath.createDirectory(folderName)

      val newFolderName = "invalid://???[¨P[*][}"
      val res = fileService.rename(initPath, folderName)
      res must beLeft
      val errors = res.left.get
      errors.statusCode mustEqual Status.CONFLICT
    }
  }

  "FileService#delete" should {
    "Mark paths as removed" in new WithApplication(zyberFakeApp) {
      pending
    }
  }

  "FileService#moveRenamingPaths" should {
    "Move files/folders and keep content the same" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      val fileService = app.injector.instanceOf(classOf[FileService])
      implicit val user = zyberUserSession.getUser

      val userRootPath = user.getRootPath(zyberUserSession)
      val folderName = "initialFolder"

      val initPath = fileService.createFolder(userRootPath, folderName).right.get
      val dstPath = fileService.createFolder(userRootPath, "destFolder").right.get

      val content = "Hi there"
      val output = fileService.getOSPath("tmp", user, initPath.getPathId).get
      output.write(content.getBytes())
      output.close()

      var moveResult = fileService.moveRenamingPaths(Seq(output.getPath.getPathId), dstPath)
      moveResult must beRight(1)

      var files = fileService.listFiles(user, initPath.getPathId.toString).get
      files.size shouldEqual 0

      files = fileService.listFiles(user, dstPath.getPathId.toString).get
      files.size shouldEqual 1
      val movedFile = files.head
      movedFile.setZus(zus)
      movedFile.getPathId mustEqual output.getPath.getPathId
      IOUtils.toString(movedFile.getInputStream) shouldEqual content

      val newFolderName = "newFolderName"
      var newFolder = fileService.createFolder(userRootPath, newFolderName).right.get

      val filesToMove = Seq(newFolder, dstPath)
      moveResult = fileService.moveRenamingPaths(filesToMove.map(_.getPathId), initPath)
      moveResult must beRight(2)

      files = fileService.listFiles(user, initPath.getPathId.toString).get
      files.size shouldEqual filesToMove.size

      files.map(_.getPathId) must contain(exactly(filesToMove.map(_.getPathId): _*))
    }

    "Not allow moving folders inside inner folders" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)

      val fileService = app.injector.instanceOf(classOf[FileService])
      implicit val user = zyberUserSession.getUser

      val userRootPath = user.getRootPath(zyberUserSession)
      val folderName = "initialFolder"

      val initFolder = fileService.createFolder(userRootPath, folderName).right.get
      val innerFolder = fileService.createFolder(initFolder, "destFolder").right.get

      val content = "Hi there"
      val output = fileService.getOSPath("tmp", user, innerFolder.getPathId).get
      output.write(content.getBytes())
      output.close()

      var moveResult = fileService.moveRenamingPaths(Seq(initFolder.getPathId), innerFolder)
      moveResult must beLeft

      var files = fileService.listFiles(user, initFolder.getPathId.toString).get
      files.size shouldEqual 1
      val movedFolder = files.head
      movedFolder.getPathId mustEqual innerFolder.getPathId
    }

    "Rename folder/file on move when name is already taken by other path" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)

      val fileService = app.injector.instanceOf(classOf[FileService])
      implicit val user = zyberUserSession.getUser

      val userRootPath = user.getRootPath(zyberUserSession)
      val folderName = "initialFolder"

      val initFolder = fileService.createFolder(userRootPath, folderName).right.get
      val dstFolder = fileService.createFolder(userRootPath, "destFolder").right.get
      val existentFolder = fileService.createFolder(dstFolder, folderName).right.get

      val content = "Hi there"
      val output = fileService.getOSPath(folderName, user, initFolder.getPathId).get
      output.write(content.getBytes())
      output.close()

      var moveResult = fileService.moveRenamingPaths(Seq(initFolder.getPathId), dstFolder)
      moveResult must beRight(1)

      var files = fileService.listFiles(user, dstFolder.getPathId.toString).get
      files.size shouldEqual 2
      files.map(_.getName) must contain(exactly(folderName, s"${folderName}(1)"))

      moveResult = fileService.moveRenamingPaths(Seq(output.getPath.getPathId), dstFolder)
      moveResult must beRight(1)
      files = fileService.listFiles(user, dstFolder.getPathId.toString).get
      files.size shouldEqual 3
      files.map(_.getName) must contain(exactly(folderName, s"${folderName}(1)", s"${folderName}(2)"))
    }
  }

  "FileService#copyRenamingPaths" should {
    "Copy files or folders by creating new paths with same content in destination" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)
      val fileService = app.injector.instanceOf(classOf[FileService])
      implicit val user = zyberUserSession.getUser

      val userRootPath = user.getRootPath(zyberUserSession)
      val folderName = "initialFolder"

      val initPath = fileService.createFolder(userRootPath, folderName).right.get
      val dstPath = fileService.createFolder(userRootPath, "destFolder").right.get

      val content = "Hi there"
      val output = fileService.getOSPath("tmp", user, initPath.getPathId).get
      output.write(content.getBytes())
      output.close()

      var copyResult = fileService.copyRenamingPaths(Seq(output.getPath.getPathId), dstPath)
      copyResult must beRight(1)

      var files = fileService.listFiles(user, initPath.getPathId.toString).get
      files.size shouldEqual 1

      files = fileService.listFiles(user, dstPath.getPathId.toString).get
      files.size shouldEqual 1
      val copiedFile = files.head
      copiedFile.getName mustEqual "tmp"
      copiedFile.setZus(zus)

      IOUtils.toString(copiedFile.getInputStream) shouldEqual content

      val newFolderName = "newFolderName"
      var newFolder = fileService.createFolder(userRootPath, newFolderName).right.get

      val filesToCopy = Seq(initPath, dstPath)
      copyResult = fileService.copyRenamingPaths(filesToCopy.map(_.getPathId), newFolder)
      copyResult must beRight(2)

      files = fileService.listFiles(user, newFolder.getPathId.toString).get
      files.size shouldEqual filesToCopy.size
      files.map(_.getName) must contain(exactly(filesToCopy.map(_.getName): _*))

      files.foreach { f =>
        val af = fileService.listFiles(user, f.getPathId.toString).get
        af.foreach { ff =>
          ff.setZus(zus)
          IOUtils.toString(ff.getInputStream) shouldEqual content
        }
      }
    }

    "Not allow coyping folders inside inner folders" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)

      val fileService = app.injector.instanceOf(classOf[FileService])
      implicit val user = zyberUserSession.getUser

      val userRootPath = user.getRootPath(zyberUserSession)
      val folderName = "initialFolder"

      val initFolder = fileService.createFolder(userRootPath, folderName).right.get
      val innerFolder = fileService.createFolder(initFolder, "destFolder").right.get

      val content = "Hi there"
      val output = fileService.getOSPath("tmp", user, innerFolder.getPathId).get
      output.write(content.getBytes())
      output.close()

      var copyResult = fileService.copyRenamingPaths(Seq(initFolder.getPathId), innerFolder)
      copyResult must beLeft

      var files = fileService.listFiles(user, innerFolder.getPathId.toString).get
      files.size shouldEqual 1
      
    }

    "Rename folder/file on copy when name is already taken by other path" in new WithApplication(zyberFakeApp) {
      val zyberUserSession = ZyberTestSession.getTestUserSession(testingTenantId)

      val fileService = app.injector.instanceOf(classOf[FileService])
      implicit val user = zyberUserSession.getUser

      val userRootPath = user.getRootPath(zyberUserSession)
      val folderName = "initialFolder"

      val initFolder = fileService.createFolder(userRootPath, folderName).right.get
      val dstFolder = fileService.createFolder(userRootPath, "destFolder").right.get
      val existentFolder = fileService.createFolder(dstFolder, folderName).right.get

      val content = "Hi there"
      val output = fileService.getOSPath(folderName, user, initFolder.getPathId).get
      output.write(content.getBytes())
      output.close()

      var copyResult = fileService.copyRenamingPaths(Seq(initFolder.getPathId), dstFolder)
      copyResult must beRight(1)

      var files = fileService.listFiles(user, dstFolder.getPathId.toString).get
      files.size shouldEqual 2
      files.map(_.getName) must contain(exactly(folderName, s"${folderName}(1)"))

      copyResult = fileService.copyRenamingPaths(Seq(output.getPath.getPathId), dstFolder)
      copyResult must beRight(1)
      files = fileService.listFiles(user, dstFolder.getPathId.toString).get
      files.size shouldEqual 3
      files.map(_.getName) must contain(exactly(folderName, s"${folderName}(1)", s"${folderName}(2)"))
    }
  }
}

trait PathsData extends TestData {

  def createTree(rootPath: Path, path: String) = {
    createTreeRec(rootPath, path.split("/"))
  }

  def createTreeRec(rootPath: Path, path: Seq[String]): Unit = {
    path match {
      case Nil          =>
      case name :: Nil  => rootPath.createDirectory(name)
      case name :: rest => createTreeRec(rootPath.createDirectory(name), rest)
    }
  }
}
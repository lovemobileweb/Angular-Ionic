package models.extra

import zyber.server.dao.Path

case class FolderTree(path: Path, folders: Seq[FolderTree])
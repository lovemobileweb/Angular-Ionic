package models.extra

case class FlowChunkInfo(
  flowChunkNumber: Int,
  flowChunkSize: Int,
  flowCurrentChunkSize: Int,
  flowTotalSize: Long,
  flowTotalChunks: Int,
  flowIdentifier: String,
  flowFilename: String)
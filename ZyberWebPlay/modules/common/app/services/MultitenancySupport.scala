package services

import zyber.server.ZyberUserSession
import zyber.server.dao.User
import zyber.server.ZyberSession
import java.util.UUID

trait MultitenancySupport { 
  def session: ZyberSession
  
  def userSession(implicit user: User): ZyberUserSession = new ZyberUserSession(session, user)
  
  def tenantSession(implicit tenantId: UUID): ZyberUserSession = new ZyberUserSession(session, tenantId)
}
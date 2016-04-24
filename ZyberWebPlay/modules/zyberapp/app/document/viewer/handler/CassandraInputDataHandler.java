package document.viewer.handler;

//import static java.util.concurrent.TimeUnit.MILLISECONDS;
//import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.InputStream;
//import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import play.Logger;
import zyber.server.ZyberSession;
import zyber.server.ZyberUserSession;
import zyber.server.dao.Path;
import zyber.server.dao.User;
import zyber.server.dao.ViewerInfo;
import zyber.server.dao.rawaccessors.PathAccessor;
import zyber.server.dao.rawaccessors.UserAccessor;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.groupdocs.viewer.domain.GroupDocsFileDescription;
import com.groupdocs.viewer.handlers.input.InputDataHandler;

public class CassandraInputDataHandler extends InputDataHandler {

	private ZyberSession zyberSession;

	private Mapper<ViewerInfo> viewerMapper;

	private MappingManager mapingManager;

	private PathAccessor pathAccessor;

	private UserAccessor userAccessor;

	public CassandraInputDataHandler() {
		try {
			zyberSession = ZyberSession.getZyberSession();
			mapingManager = zyberSession.getMappingManager();
			viewerMapper = mapingManager.mapper(ViewerInfo.class);
			pathAccessor = mapingManager.createAccessor(PathAccessor.class);
			userAccessor = mapingManager.createAccessor(UserAccessor.class);
		} catch (Exception ex) {
			Logger.error("Error in Init:", ex);
			throw ex;
		}
	}

	private ViewerInfo getViewerInfo(String tokenId) {
		ViewerInfo viewerInfo = viewerMapper.get(UUID.fromString(tokenId));
		return viewerInfo;
	}

	private Path getPath(String tokenId) {

		ViewerInfo viewerInfo = getViewerInfo(tokenId);

		if (null == viewerInfo) {
			throw new RuntimeException("Invalid or expired token");
		}
		// Token is remove by cassandra automatically (ttl)
		// long MAX_DURATION = MILLISECONDS.convert(5, MINUTES);
		// long duration = new Date().getTime() -
		// viewerInfo.getCreatedTime().getTime();
		// if (duration >= MAX_DURATION) {
		// throw new RuntimeException("Invalid token");
		// }
		User user = userAccessor.getById(viewerInfo.getTenantId(),
				viewerInfo.getUserId());
		ZyberUserSession zus = new ZyberUserSession(zyberSession, user,
				viewerInfo.getTenantId());
		Path path = pathAccessor.getPath(viewerInfo.getFileId());
		path.setZus(zus);
		return path;
	}

	@Override
	public InputStream getFile(String uuid) throws Exception {
		try {
			Logger.debug("At CassandraInputDataHandler#getFile");
			return getPath(uuid).getInputStream();
		} catch (Exception ex) {
			Logger.error("Error in Init:", ex);
			throw ex;
		}

	}

	@Override
	public GroupDocsFileDescription getFileDescription(String uuid)
			throws Exception {
		try {
			Logger.debug("At CassandraInputDataHandler#getFileDescription");

			Path path = getPath(uuid);

			GroupDocsFileDescription fileDescription = new GroupDocsFileDescription();
			fileDescription.setGuid(uuid);
			fileDescription.setName(path.getName());
			fileDescription.setLastModified(path.getModifiedDate().getTime());
			fileDescription.setSize(path.getSize());

			return fileDescription;
		} catch (Exception ex) {
			Logger.error("Error in Init:", ex);
			throw ex;
		}

	}

	@Override
	public List<GroupDocsFileDescription> getFileDescriptionList(String arg0)
			throws Exception {
		return new LinkedList<GroupDocsFileDescription>();
	}

	@Override
	public String saveFile(InputStream arg0, String arg1, Integer arg2,
			String arg3) throws Exception {
		throw new UnsupportedOperationException();
	}

}

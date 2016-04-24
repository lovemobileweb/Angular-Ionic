package zyber;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import services.DBTests;
import zyber.server.ZyberTestSession;
import zyber.server.ZyberUserSession;
import zyber.server.dao.Path;
import zyber.server.dao.PathType;

public class TestPaths extends TestCase {
	static Path createFile(Path p, String name, String body) throws IOException {
		Path ret = p.createFile(name);
		try (java.io.OutputStream os = ret.getOutputStream()) {
			os.write(body.getBytes());			
		}
		return ret;
		
	}

	public void testCreateAndListFolders() throws IOException {
		// ZyberSession z= ZyberSession.getInstance();
		ZyberUserSession u1 = ZyberTestSession.getTestUserSession(DBTests.testingTenantId(), "u1");
		ZyberUserSession u2 = ZyberTestSession.getTestUserSession(DBTests.testingTenantId(), "u2");

		Path p1 = u1.getUser().getRootPath(u2);
		assertEquals(true, p1.isDirectory());

		assertEquals(false, p1.isFile());
		assertEquals(u1.getUser().getEmail(), p1.getName());
		Path earth = p1.createChild("earth", PathType.Directory);
		Path animals = earth.createChild("animals", PathType.Directory);
		Path ds = animals.createChild("deepsea", PathType.Directory);
		createFile(ds, "polychaete Worm.txt", "some file.");

		Path bird = createFile(animals, "bird.txt", "Bird file.");
		Path cat = createFile(animals, "cat.txt", "Cat file.");
		Path fish = createFile(animals, "fish.txt", "Fish file.");
		/*OutputStream os = fish.getOutputStream();
		os.write("Tenctaceles: He is a ninety year old , but nimble, octopus. ".getBytes());
		os.write("Tenctaceles: He is a ninety year old , but nimble, octopus. ".getBytes());
		os.write("Tenctaceles: He is a ninety year old , but nimble, octopus. ".getBytes());
		os.flush();*/
	//	animals.delete();
	    // cat.delete();
		HashMap <String,String> metadata = new HashMap<String,String>();
		metadata.put("mime-type","Mime");
		metadata.put("applicable-security-policy","FOX");
		System.out.println("Before");
		fish.setMetaData("mime-type","Mime");
		List<Path> all = animals.getChildren().all();
		Collections.sort(all,Path.BY_NAME);
		//Last should be fish
		Path path = all.get(all.size() - 1);
		assertEquals("fish.txt",path.getName());
		fish.rename("fish2.txt");
		List<Path> allAfter = animals.getChildren().all();
		Collections.sort(allAfter,Path.BY_NAME);
		Path renamedAfter = allAfter.get(all.size() - 1);

		assertEquals("fish2.txt",renamedAfter.getName());

		// These should return the root path.
		assertEquals("should be root path", p1, p1.findChild("/"));
		assertEquals("should be root path", p1, p1.findChild(""));
  
		animals.rename("animals1");
	//	cat.move("/earth");
		cat.setMetaData(metadata);		
		Set<String> value = new HashSet<String>();
		value.add("FOX");
		value.add("SOX");
		bird.setMetaData("applicable-security-policy", value);
		System.out.println("after");
	//	fish.getMetaData("mime-type");
		
      //  fish.delete();

		assertEquals(4, animals.getChildren().all().size());
		assertEquals(true, animals.isDirectory());
		assertEquals(false, animals.isFile());
		assertEquals("earth", earth.getName());

		assertEquals(0, bird.getChildren().all().size());
		assertEquals(false, bird.isDirectory());
		assertEquals(true, bird.isFile());

		Path p2 = u2.getRootPath();
		assertNotSame(p1, p2);
		assertEquals(0, p2.getChildren().all().size());

		System.out.println("Tree at 1:\n" + printTree(p1, null));

		//Test moving
		fish.move(ds.getPathId());
		List<Path> inDs = ds.getChildren().all();
		Collections.sort(inDs,Path.BY_NAME);
		assertEquals(2, inDs.size());
		assertEquals("fish2.txt",inDs.get(0).getName());

		List<Path> inAnimals = animals.getChildren().all();
		assertEquals(3, inAnimals.size());

		//Test deletion
		fish.delete();
		inDs = ds.getChildren().all();
		assertEquals(1, inDs.size());

//		assertContains(u1.getRootPath(), "/earth", "animals");
//		assertContains(u1.getRootPath(), "/earth/animals", "bird.txt");
//		assertContains(u1.getRootPath(), "/earth/animals", "cat.txt");
//		assertContains(u1.getRootPath(), "/earth/animals", "fish1.txt");
//		assertContains(u1.getRootPath(), "/earth/animals/deepsea", "Polychaete Worm.txt");
		
	}

	public String printTree(Path p, String prefixSoFar) {
		if (p.isFile())
			return prefixSoFar + " / '" + p.getName() + "'\n";
		if (p.isDirectory()) {
			String r = "";
			r += (prefixSoFar == null ? "" : prefixSoFar + " / ") + "'" + p.getName() + "' /\n";
			for (Path ch : p.getChildren()) {
				r += printTree(ch, (prefixSoFar == null ? "" : prefixSoFar + " / ") + "'" + p.getName() + "'");
			}
			return r;
		}
		throw new IllegalStateException();

	}

	public void assertContains(Path root, String dirname, String matchfile) {
		System.out.println("looking for " + matchfile + " in folder " + dirname);
		Path parent = root.findChild(dirname);
		assertNotNull("Could not find path: " + dirname, parent);
		Path child = parent.getFirstChild(matchfile);
		assertNotNull("Could not find " + matchfile + " at path: " + dirname, child);
		assertEquals("Expected exactly 1 child " + matchfile + " in " + dirname, 1,
				parent.getChild(matchfile).all().size());
		System.out.println("found for " + matchfile + " in folder " + dirname);
	}
}

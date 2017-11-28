package aQute.service.library;

import java.security.*;
import java.util.*;

import aQute.service.library.Library.Revision;
import aQute.struct.*;

public class Revisions extends struct {
	public byte[]		_id;
	public List<byte[]>	content	= list();

	public static byte[] checksum(Revisions rs) throws NoSuchAlgorithmException {
		Collections.sort(rs.content, new Comparator<byte[]>() {

			@Override
			public int compare(byte[] o1, byte[] o2) {
				for (int i = 0; i < o1.length; i++) {
					if (i >= o2.length)
						return 1;

					int a = o1[i];
					int b = o2[i];
					if (a > b)
						return 1;
					if (a < b)
						return -1;
				}
				if (o1.length != o2.length)
					return -1;
				return 0;
			}

		});

		//
		// A single repository has the id of the single member's id
		// This makes any revision its own repository
		//
		
		if ( rs.content.size() == 1)
			return rs.content.get(0);

		MessageDigest digester = MessageDigest.getInstance("SHA1");
		byte[] last = null;
		for (Iterator<byte[]> i = rs.content.iterator(); i.hasNext();) {
			byte[] ba = i.next();
			// Skip duplicates
			if (ba != last) {
				digester.update(ba);
				last = ba;
			} else
				i.remove();
		}
		return digester.digest();
	}

	public static Revisions singleton(Revision r) throws NoSuchAlgorithmException {
		Revisions revisions = new Revisions();
		revisions.content.add(r._id);
		revisions._id = r._id;
		return revisions;
	}

	public static Revisions from(Collection<? extends Revision> responses) throws NoSuchAlgorithmException {
		Revisions revs = new Revisions();
		for ( Revision r : responses)
			revs.content.add(r._id);
		revs._id = checksum(revs);
		return revs;
	}
}

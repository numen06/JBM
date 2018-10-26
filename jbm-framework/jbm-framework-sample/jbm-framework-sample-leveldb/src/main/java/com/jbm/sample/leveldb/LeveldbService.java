package com.jbm.sample.leveldb;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.annotation.PostConstruct;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.springframework.stereotype.Service;

@Service
public class LeveldbService {
	private final File databaseDir = new File("leveldb");
	private DB db;

	@PostConstruct
	public void init() throws IOException {
		// open();
		test();
	}

	public void open() throws IOException {
		Options options = new Options();
		options.createIfMissing(true);
		db = factory.open(databaseDir, options);
	}

	public void test() throws IOException {
		open();
		System.out.println("Adding");
		for (int i = 0; i < 1000 * 1000; i++) {
			if (i % 100000 == 0) {
				System.out.println("  at: " + i);
			}
			db.put(bytes("key" + i), bytes("value" + i));
		}

		db.close();
		open();

		System.out.println("Search");
		for (int i = 0; i < 1000 * 1000; i++) {
			if (i % 100000 == 0) {
				System.out.println("  at: " + i);
			}
			System.out.println(new String(db.get(bytes("key" + i))));
		}

		db.close();
		open();

		System.out.println("Deleting");
		for (int i = 0; i < 1000 * 1000; i++) {
			if (i % 100000 == 0) {
				System.out.println("  at: " + i);
			}
			db.delete(bytes("key" + i));
		}

		db.close();

	}

	public static byte[] bytes(String value) {
		if (value == null) {
			return null;
		}
		try {
			return value.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String asString(byte[] value) {
		if (value == null) {
			return null;
		}
		try {
			return new String(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private final DBFactory factory = Iq80DBFactory.factory;

	File getTestDirectory(String name) throws IOException {
		File rc = new File(databaseDir, name);
		factory.destroy(rc, new Options().createIfMissing(true));
		rc.mkdirs();
		return rc;
	}

}

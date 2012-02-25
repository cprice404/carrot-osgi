package com.carrotgarden.osgi.anno.scr.make;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotgarden.osgi.anno.scr.case01.Comp_01_empty;
import com.carrotgarden.osgi.anno.scr.case01.Comp_01_factory;
import com.carrotgarden.osgi.anno.scr.case01.Comp_01_reference;
import com.carrotgarden.osgi.anno.scr.case01.Comp_01_reference_dynamic;
import com.carrotgarden.osgi.anno.scr.case01.Comp_01_service;
import com.carrotgarden.osgi.anno.scr.case01.Comp_01_with_name;
import com.carrotgarden.osgi.anno.scr.case02.Comp_02_0;
import com.carrotgarden.osgi.anno.scr.case02.Comp_02_1;
import com.carrotgarden.osgi.anno.scr.case02.Comp_02_2;
import com.carrotgarden.osgi.anno.scr.case02.Comp_02_3;

public class TestMaker1 {

	static final Logger log = LoggerFactory.getLogger(TestMaker1.class);

	static String convertStreamToString(final InputStream input) {

		if (input == null) {
			return null;
		}

		try {
			return new java.util.Scanner(input).useDelimiter("\\A").next();
		} catch (final Exception e) {
			return "";
		}
	}

	static void testClass(final Class<?> klaz) {

		log.debug("######################################");
		log.debug("test class : {}", klaz.getName());

		final Maker maker = new Maker();

		final String source = maker.make(klaz);
		log.debug("source : \n{}", source);

		final String fileName = klaz.getSimpleName() + ".xml";
		final InputStream input = klaz.getResourceAsStream(fileName);
		final String target = convertStreamToString(input);
		log.debug("target : \n{}", target);

		assertEquals(source, target);

	}

	@Test
	public void test01() {

		testClass(Comp_01_empty.class);

		testClass(Comp_01_with_name.class);

		testClass(Comp_01_service.class);

		testClass(Comp_01_reference.class);

		testClass(Comp_01_reference_dynamic.class);

		testClass(Comp_01_factory.class);

	}

	@Test
	public void test02() {

		testClass(Comp_02_0.class); // missing

		testClass(Comp_02_1.class);

		testClass(Comp_02_2.class);

		testClass(Comp_02_3.class); // missing

	}

}
